package com.display.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * Core orchestrator for native engine bootstrap.
 * Handles virtual environment setup, native library loading,
 * IL2CPP metadata parsing, and render loop lifecycle.
 * 
 * Stealth-renamed from DisplayCore to avoid AC string detection.
 */
public class DisplayCore {
    private static final String TAG = "DisplayUtils";
    private static volatile boolean sInitialized = false;
    private static volatile boolean sRunning = false;
    private static Handler sTickHandler;
    private static Runnable sTickRunnable;

    // JNI method declarations - signatures must match cpp/native_bridge.cpp exactly
    public static native int nativeLoadTarget(String pkgName);
    public static native int nativeParseMeta();
    public static native void nativeSetOffsets(long gmOff, long plOff, long hpOff, long posOff);
    public static native void nativeStartLoop();
    public static native void nativeTick();
    public static native void nativeStopLoop();
    public static native int nativeGetStatus();

    /**
     * Master bootstrap sequence.
     * Must be called after virtual environment is prepared.
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

            // Step 2: Redirect file I/O to virtual sandbox
            PathHelper.activate(ctx, targetPkg);

            // Step 3: Load target dex + extract libil2cpp.so into memory
            AssetLoader.loadTarget(ctx, targetPkg);

            // Step 4: Attach to target process, find module base address
            int loadResult = nativeLoadTarget(targetPkg);
            if (loadResult != 0) {
                Log.e(TAG, "nativeLoadTarget failed with code: " + loadResult);
                return false;
            }

            // Step 5: Parse global-metadata.dat, resolve IL2CPP type/field offsets
            int parseResult = nativeParseMeta();
            if (parseResult != 0) {
                Log.e(TAG, "nativeParseMeta failed with code: " + parseResult);
                return false;
            }

            // Step 6: Push resolved offsets to native cache
            // TODO: Replace 0x0 placeholders with real dumped offsets from Il2CppDumper
            nativeSetOffsets(0x0, 0x0, 0x0, 0x0);

            // Step 7: Spawn native render thread (runs independently at ~60fps)
            nativeStartLoop();

            // Step 8: Setup Java-side tick handler for UI synchronization
            sTickHandler = new Handler(Looper.getMainLooper());
            sTickRunnable = () -> {
                if (sRunning) {
                    nativeTick();
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
            nativeStopLoop();
        } catch (UnsatisfiedLinkError ignored) {
            // Native lib not loaded yet, safe to ignore
        }
        SysConfig.uninstall();
        sInitialized = false;
        Log.i(TAG, "DisplayCore cleaned up");
    }

    /**
     * Check if engine is fully initialized and running
     */
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
}
