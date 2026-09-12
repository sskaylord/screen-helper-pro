package com.display.utils;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import dalvik.system.DexClassLoader;

/**
 * Asset loading and native bridge layer.
 * Stealth-renamed from ResourceLoader to avoid AC string detection.
 *
 * Responsibilities:
 * - Extract target APK dex files into virtual sandbox
 * - Extract native libraries (libil2cpp.so) from target APK
 * - Create DexClassLoader for target package classes
 * - Provide reflection-based class/method lookup
 * - Declare all native JNI methods used by DisplayCore orchestrator
 *
 * Native method count: 22 (must match cpp JNI signatures exactly)
 * All extraction happens in app-private storage. No root required.
 */
public class AssetLoader {
    private static final String TAG = "DisplayUtils";
    private static volatile boolean sLoaded = false;
    private static DexClassLoader sTargetClassLoader = null;
    private static String sNativeLibPath = null;
    private static String sDexPath = null;

    // ========================================================================
    // NATIVE METHOD DECLARATIONS
    // These MUST match cpp JNI function names exactly.
    // Grouped by source file for maintainability.
    // ========================================================================

    // --- native_bridge.cpp (9 methods) ---

    /** Attach to target process memory space, find module base */
    public static native int nativeLoadTarget(String libPath);

    /** Get base address of loaded target module */
    public static native long nativeGetBase();

    /** Get size of loaded target module in bytes */
    public static native long nativeGetSize();

    /** Check if target module is loaded and ready */
    public static native boolean nativeIsReady();

    /** Find exported symbol address in target module */
    public static native long nativeFindSymbol(String symName);

    /** Read arbitrary memory from target process */
    public static native byte[] nativeReadMemory(long addr, int size);

    /** Clean suspicious entries from /proc/self/maps */
    public static native void nativeCleanMaps();

    /** Emergency restore all hooks to original state */
    public static native void nativeEmergencyRestore();

    /** Get current active hook count */
    public static native int nativeGetHookCount();

    // --- meta_parser.cpp (3 methods) ---

    /** Parse IL2CPP global-metadata.dat file */
    public static native boolean nativeParseMeta(String path);

    /** Get resolved offset by type index */
    public static native long nativeGetOffset(int type);

    /** Check if all offsets have been resolved */
    public static native boolean nativeOffsetsReady();

    // --- render_loop.cpp (7 methods) ---

    /** Start native render loop thread with module base address */
    public static native void nativeStartLoop(long base);

    /** Stop native render loop thread gracefully */
    public static native void nativeStopLoop();

    /** Single frame tick - read player data, update cache */
    public static native void nativeTick();

    /** Push resolved offsets to native cache (11 parameters) */
    public static native void nativeSetOffsets(long gm, long pl, long lp, long hp,
                                                long tm, long tr, long ps, long bn,
                                                long vm, long el, long ec);

    /** Get player data array by index (position, health, team, etc.) */
    public static native float[] nativeGetPlayerData(int idx);

    /** Get current view/projection matrix from game engine */
    public static native float[] nativeGetViewMatrix();

    /** Get number of players currently in cache */
    public static native int nativeGetPlayerCount();

    /** Check if render loop is actively running */
    public static native boolean nativeIsRunning();

    // --- env_check.cpp (3 methods) ---

    /** Full environment scan for debugging/tracing tools */
    public static native void nativeFullScan();

    /** Check if debugger is attached to process */
    public static native boolean nativeIsDebuggerPresent();

    // ========================================================================
    // JAVA-SIDE ASSET LOADING
    // ========================================================================

    /**
     * Load target package dex and native libraries.
     * Extracts from installed APK into app-private directories.
     * Creates DexClassLoader for target class resolution.
     * Loads libil2cpp.so via System.load().
     *
     * Note: Extraction uses app's own filesDir, NOT PathHelper virtual dirs.
     * PathHelper handles runtime I/O redirection separately.
     *
     * @param ctx Application context
     * @param targetPkg Target package name (e.g., com.axlebolt.standoff2)
     */
    public static synchronized void loadTarget(Context ctx, String targetPkg) {
        if (sLoaded) return;

        try {
            // Get target APK path from PackageManager
            String apkPath = ctx.getPackageManager()
                    .getApplicationInfo(targetPkg, 0).sourceDir;

            if (apkPath == null || !new File(apkPath).exists()) {
                Log.e(TAG, "Target APK not found: " + targetPkg);
                return;
            }

            // Use app-private directories for extraction
            String libExtractDir = ctx.getFilesDir().getAbsolutePath() + "/vs/lib";
            String dexExtractDir = ctx.getFilesDir().getAbsolutePath() + "/vs/dex";
            new File(libExtractDir).mkdirs();
            new File(dexExtractDir).mkdirs();

            // Extract native library (arm64-v8a preferred, fallback to armeabi-v7a)
            sNativeLibPath = extractNativeLib(apkPath, libExtractDir);

            // Extract dex files (supports multidex)
            sDexPath = extractDexFiles(apkPath, dexExtractDir);

            // Create DexClassLoader for target package class resolution
            if (sDexPath != null) {
                sTargetClassLoader = new DexClassLoader(
                        sDexPath,
                        ctx.getCodeCacheDir().getAbsolutePath(),
                        sNativeLibPath,
                        ctx.getClassLoader()
                );
                Log.d(TAG, "DexClassLoader created");
            }

            // Load extracted native library into current process
            if (sNativeLibPath != null) {
                System.load(sNativeLibPath + "/libil2cpp.so");
                Log.d(TAG, "Native library loaded");
            }

            sLoaded = true;
            Log.i(TAG, "AssetLoader: target loaded successfully");
        } catch (Exception e) {
            Log.e(TAG, "AssetLoader load failed", e);
        }
    }

    /**
     * Extract libil2cpp.so from target APK.
     * Searches for arm64-v8a architecture first, falls back to armeabi-v7a.
     *
     * @param apkPath Absolute path to target APK
     * @param destDir Destination directory for extracted .so
     * @return Directory containing extracted .so file, or null on failure
     */
    private static String extractNativeLib(String apkPath, String destDir) {
        String[] archPaths = {
            "lib/arm64-v8a/libil2cpp.so",
            "lib/armeabi-v7a/libil2cpp.so"
        };

        try (ZipFile zip = new ZipFile(apkPath)) {
            for (String archPath : archPaths) {
                ZipEntry entry = zip.getEntry(archPath);
                if (entry != null) {
                    File outFile = new File(destDir, "libil2cpp.so");
                    extractZipEntry(zip, entry, outFile);
                    Log.d(TAG, "Extracted native lib: " + archPath);
                    return destDir;
                }
            }
            Log.w(TAG, "No supported arch found in APK");
        } catch (Exception e) {
            Log.e(TAG, "Native lib extraction failed", e);
        }
        return null;
    }

    /**
     * Extract classes.dex (and multidex if present) from target APK.
     * Supports up to classes4.dex for large applications.
     *
     * @param apkPath Absolute path to target APK
     * @param destDir Destination directory for extracted dex files
     * @return Colon-separated dex paths for DexClassLoader, or null
     */
    private static String extractDexFiles(String apkPath, String destDir) {
        StringBuilder dexPaths = new StringBuilder();

        try (ZipFile zip = new ZipFile(apkPath)) {
            String[] dexNames = {"classes.dex", "classes2.dex", "classes3.dex", "classes4.dex"};

            for (String dexName : dexNames) {
                ZipEntry entry = zip.getEntry(dexName);
                if (entry != null) {
                    File outFile = new File(destDir, dexName);
                    extractZipEntry(zip, entry, outFile);

                    if (dexPaths.length() > 0) dexPaths.append(":");
                    dexPaths.append(outFile.getAbsolutePath());
                    Log.d(TAG, "Extracted dex: " + dexName);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Dex extraction failed", e);
            return null;
        }

        return dexPaths.length() > 0 ? dexPaths.toString() : null;
    }

    /**
     * Utility: Extract a single ZipEntry to file using buffered I/O.
     * Creates parent directories if they don't exist.
     *
     * @param zip Source ZipFile
     * @param entry ZipEntry to extract
     * @param outFile Destination file
     */
    private static void extractZipEntry(ZipFile zip, ZipEntry entry, File outFile) throws Exception {
        if (!outFile.getParentFile().exists()) {
            outFile.getParentFile().mkdirs();
        }
        try (InputStream is = zip.getInputStream(entry);
             FileOutputStream fos = new FileOutputStream(outFile)) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = is.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
        }
    }

    /**
     * Find a class in the target's loaded dex.
     *
     * @param className Fully qualified class name
     * @return Class object or null if not found
     */
    public static Class<?> findClass(String className) {
        if (sTargetClassLoader == null) return null;
        try {
            return sTargetClassLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            Log.w(TAG, "Class not found: " + className);
            return null;
        }
    }

    /**
     * Invoke a static method on a target class via reflection.
     *
     * @param className Fully qualified class name
     * @param methodName Method name
     * @param paramTypes Parameter types
     * @param args Arguments
     * @return Method return value or null
     */
    public static Object invokeStatic(String className, String methodName,
                                       Class<?>[] paramTypes, Object[] args) {
        try {
            Class<?> cls = findClass(className);
            if (cls == null) return null;
            java.lang.reflect.Method m = cls.getDeclaredMethod(methodName, paramTypes);
            m.setAccessible(true);
            return m.invoke(null, args);
        } catch (Exception e) {
            Log.e(TAG, "invokeStatic failed: " + className + "." + methodName, e);
            return null;
        }
    }

    /**
     * Create instance of target class via default constructor.
     *
     * @param className Fully qualified class name
     * @return New instance or null
     */
    public static Object createInstance(String className) {
        try {
            Class<?> cls = findClass(className);
            if (cls == null) return null;
            return cls.newInstance();
        } catch (Exception e) {
            Log.e(TAG, "createInstance failed: " + className, e);
            return null;
        }
    }

    /** Get the DexClassLoader for target package */
    public static DexClassLoader getTargetClassLoader() { return sTargetClassLoader; }

    /** Get path to extracted native library directory */
    public static String getNativeLibPath() { return sNativeLibPath; }

    /** Check if target assets are loaded */
    public static boolean isLoaded() { return sLoaded; }
}
