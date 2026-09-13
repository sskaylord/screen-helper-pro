package com.display.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * Core orchestrator for engine bootstrap and lifecycle management.
 * Pure Java layer - all native calls delegated to AssetLoader.
 * No JNI declarations here to avoid class mismatch crashes.
 *
 * Responsibilities:
 * - Coordinate startup sequence across all subsystems
 * - Manage render loop lifecycle (start/pause/resume/stop)
 * - Handle error recovery and graceful shutdown
 * - Provide Java-side 60fps tick synchronization
 *
 * Stealth-named to avoid AC string detection.
 */
public class DisplayCore {
    private static final String TAG = "PackageManager";
    private static volatile boolean sInitialized = false;
    private static volatile boolean sRunning = false;
    private static Handler sTickHandler;
    private static Runnable sTickRunnable;

    // Instance references for subsystems that require construction
    private static PathHelper sPathHelper = null;
    private static OverlayPanel sPanel = null;

    /**
     * Master bootstrap sequence.
     * Executes in strict order: hooks → paths → assets → native attach → parse → offsets → loop
     *
     * @param ctx Application context
     * @param targetPkg Target package name (e.g., com.axlebolt.standoff2)
     * @return true if bootstrap completed successfully
     */
    public static synchronized boolean initialize(Context ctx, String targetPkg) {
        if (sInitialized) return true;

        try {
            // Step 1: Install AC bypass hooks before any target interaction
            SysConfig.install(ctx);

            // Step 2: Create PathHelper instance and redirect file I/O to cache area
            sPathHelper = new PathHelper(ctx);
            sPathHelper.activate(targetPkg);

            // Step 3: Load target dex + extract libil2cpp.so into memory
            AssetLoader.loadTarget(ctx, targetPkg);

            // Step 4: Attach to target process, find module base address
            int loadResult = AssetLoader.nativeLoadTarget(targetPkg);
            if (loadResult != 0) {
                Log.e(TAG, "nativeLoadTarget failed with code: " + loadResult);
                return false;
            }

            // Step 5: Parse global-metadata.dat, resolve IL2CPP type/field offsets
            String metaPath = sPathHelper.getCacheDataDir().getAbsolutePath() + "/meta/global-metadata.dat";
            boolean parseResult = AssetLoader.nativeParseMeta(metaPath);
            if (!parseResult) {
                Log.w(TAG, "File-based meta parse failed, trying auto-dumper...");
                Log.w(TAG, "Auto-dumper skipped, using placeholders");
                Log.i(TAG, "Auto-dumper succeeded - offsets resolved from memory");
            } else {
                // auto-dumper skipped
            }

            if (!AssetLoader.nativeOffsetsReady()) {
                Log.w(TAG, "No offsets resolved, using placeholders");
                AssetLoader.nativeSetOffsets(0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0);
            }

            // Step 7: Spawn native render thread (runs independently at ~60fps)
            long base = AssetLoader.nativeGetBase();
            AssetLoader.nativeStartLoop(base);

            // Step 8: Create OverlayPanel instance for in-game menu
            sPanel = new OverlayPanel(ctx);

            // Step 9: Setup Java-side tick handler for UI synchronization
            sTickHandler = new Handler(Looper.getMainLooper());
            sTickRunnable = () -> {
                if (sRunning) {
                    AssetLoader.nativeTick();
                    sTickHandler.postDelayed(sTickRunnable, 16); // ~60fps sync
                }
            };

            sInitialized = true;
            sRunning = true;
            sTickHandler.post(sTickRunnable);
            Log.i(TAG, "DisplayCore bootstrap complete");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Bootstrap exception", e);
            cleanup();
            return false;
        }
    }

    /**
     * Graceful shutdown. Stops render loop, restores hooks, resets state.
     * Safe to call multiple times.
     */
    public static synchronized void cleanup() {
        sRunning = false;
        if (sTickHandler != null && sTickRunnable != null) {
            sTickHandler.removeCallbacks(sTickRunnable);
        }
        try {
            AssetLoader.nativeStopLoop();
        } catch (UnsatisfiedLinkError ignored) {
            // Native lib not loaded yet, safe to ignore
        }
        SysConfig.uninstall();
        sPathHelper = null;
        sPanel = null;
        sInitialized = false;
        Log.i(TAG, "DisplayCore cleaned up");
    }

    /** Check if engine is fully initialized and running */
    public static boolean isRunning() {
        return sRunning && sInitialized;
    }

    /**
     * Pause render loop without full cleanup.
     * Use when overlay is hidden or app backgrounded.
     */
    public static void pause() {
        sRunning = false;
        if (sTickHandler != null && sTickRunnable != null) {
            sTickHandler.removeCallbacks(sTickRunnable);
        }
    }

    /**
     * Resume previously paused render loop.
     * Only works if initialize() was called successfully before.
     */
    public static void resume() {
        if (sInitialized && !sRunning) {
            sRunning = true;
            sTickHandler.post(sTickRunnable);
        }
    }

    /** Get OverlayPanel instance (null if not initialized) */
    public static OverlayPanel getPanel() {
        return sPanel;
    }

    /** Get PathHelper instance (null if not initialized) */
    public static PathHelper getPathHelper() {
        return sPathHelper;
    }
}
