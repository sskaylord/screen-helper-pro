package com.display.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.Log;
import java.io.File;
import java.lang.reflect.Method;
import dalvik.system.DexClassLoader;

/**
 * Loads real GMS Core APK via DexClassLoader for account authentication.
 * No system install required - runs entirely within local cache.
 */
public class CompatLoader {
    private static final String TAG = StrObf.d("\u00c2\u00d3\u00d4\u0051\u00c4\u00d7\u00d7"); // "CompatLoader" XOR'd
    
    public static boolean loadGms(android.content.Context ctx) {
        if (sLoaded) return true;
        try {
            java.io.File apk = new java.io.File(ctx.getFilesDir(), "cache/gms.apk");
            if (!apk.exists()) {
                // Try assets
                java.io.InputStream is = ctx.getAssets().open("gms-core.apk");
                apk.getParentFile().mkdirs();
                java.io.FileOutputStream fos = new java.io.FileOutputStream(apk);
                byte[] buf = new byte[8192];
                int n;
                while ((n = is.read(buf)) > 0) fos.write(buf, 0, n);
                fos.close();
                is.close();
            }
            if (apk.exists()) {
                sGmsClassLoader = new dalvik.system.DexClassLoader(
                    apk.getAbsolutePath(),
                    ctx.getCacheDir().getAbsolutePath(),
                    null, ctx.getClassLoader());
                sLoaded = true;
                return true;
            }
        } catch (Exception e) {
            android.util.Log.e("CompatLoader", "loadGms: " + e);
        }
        return false;
    }

private static ClassLoader sGmsClassLoader = null;
    private static boolean sLoaded = false;

    /**
     * Load GMS Core from installed system or sideloaded APK.
     * Searches standard GMS paths first, then fallback to extracted copy.
     */
    public static synchronized boolean load(Context ctx) {
        if (sLoaded) return true;

        String gmsApkPath = findGmsApk(ctx);
        if (gmsApkPath == null) {
            Log.e(TAG, "GMS Core APK not found");
            return false;
        }

        try {
            // Extract dex from GMS APK
            String dexOutDir = ctx.getFilesDir().getAbsolutePath() + "/cache/gms";
            new File(dexOutDir).mkdirs();

            // Use BootClassLoader parent - no host leak
            ClassLoader bootParent;
            try {
                bootParent = (ClassLoader) Class.forName("java.lang.BootClassLoader")
                    .getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                bootParent = ClassLoader.getSystemClassLoader();
            }

            sGmsClassLoader = new DexClassLoader(
                gmsApkPath,
                dexOutDir,
                null,
                bootParent
            );

            // Verify core class loads
            Class<?> gmsClass = sGmsClassLoader.loadClass(
                "com.google.android.gms.common.GoogleApiAvailability"
            );
            
            sLoaded = true;
            Log.i(TAG, "GMS Core loaded successfully");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "GMS load failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Find GMS Core APK path - system or sideloaded.
     */
    private static String findGmsApk(Context ctx) {
        // Try system GMS first
        try {
            ApplicationInfo info = ctx.getPackageManager()
                .getApplicationInfo("com.google.android.gms", 0);
            if (info.sourceDir != null && new File(info.sourceDir).exists()) {
                return info.sourceDir;
            }
        } catch (Exception ignored) {}

        // Try sideloaded in local cache
        String[] candidates = {
            ctx.getFilesDir().getAbsolutePath() + "/cache/gms/base.apk",
            ctx.getFilesDir().getAbsolutePath() + "/cache/gms/gms-core.apk",
            "/data/local/tmp/gms-core.apk"
        };

        for (String path : candidates) {
            if (new File(path).exists()) return path;
        }

        return null;
    }

    /** Get GMS ClassLoader for auth operations */
    public static ClassLoader getClassLoader() { return sGmsClassLoader; }
    
    /** Check if GMS is ready */
    public static boolean isReady() { return sLoaded; }
}
