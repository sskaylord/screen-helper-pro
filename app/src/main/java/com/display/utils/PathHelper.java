package com.display.utils;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class PathHelper {

    private static final String TAG = "MountService";
    private final Context hostContext;
    private final File virtualDataDir;
    private final Map<String, String> pathMap = new HashMap<>();
    private boolean active = false;

    public PathHelper(Context ctx) {
        this.hostContext = ctx;
        this.virtualDataDir = new File(ctx.getFilesDir(), "vs/data");
        this.virtualDataDir.mkdirs();
    }

    public boolean activate(String targetPackage) {
        try {
            String realDataPath = "/data/data/" + targetPackage;
            String virtualPath = virtualDataDir.getAbsolutePath();

            pathMap.put(realDataPath, virtualPath);
            pathMap.put("/data/user/0/" + targetPackage, virtualPath);
            
            // Google Play Store & Services virtual mapping
            String[] googlePackages = {
                "com.android.vending",
                "com.google.android.gms",
                "com.google.android.gsf"
            };
            for (String pkg : googlePackages) {
                String pkgVirtual = new File(virtualDataDir.getParentFile(), pkg).getAbsolutePath();
                new File(pkgVirtual).mkdirs();
                createSubDirs(pkgVirtual);
                pathMap.put("/data/data/" + pkg, pkgVirtual);
                pathMap.put("/data/user/0/" + pkg, pkgVirtual);
            }

            createSubDirs(virtualPath);
            hookNativeIO();
            active = true;

            Log.i(TAG, "Path resolver active for: " + targetPackage);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Activate failed: " + e.getMessage());
            return false;
        }
    }

    private void createSubDirs(String base) {
        new File(base, "shared_prefs").mkdirs();
        new File(base, "databases").mkdirs();
        new File(base, "files").mkdirs();
        new File(base, "cache").mkdirs();
        new File(base, "code_cache").mkdirs();
        new File(base, "app_webview").mkdirs();
        new File(base, "meta").mkdirs();
    }

    private void hookNativeIO() {
        try {
            Class<?> osClass = Class.forName("libcore.io.Os");
            Class<?> blockGuardClass = Class.forName("libcore.io.BlockGuardOs");

            Log.i(TAG, "Native IO hooks prepared");
        } catch (Exception e) {
            Log.w(TAG, "Native IO hook skipped: " + e.getMessage());
        }
    }

    public String resolve(String originalPath) {
        if (!active || originalPath == null) return originalPath;
        for (Map.Entry<String, String> entry : pathMap.entrySet()) {
            if (originalPath.startsWith(entry.getKey())) {
                return originalPath.replace(entry.getKey(), entry.getValue());
            }
        }
        return originalPath;
    }

    public File getVirtualDataDir() {
        return virtualDataDir;
    }

    public File getSharedPrefsDir() {
        return new File(virtualDataDir, "shared_prefs");
    }

    public File getDatabaseDir() {
        return new File(virtualDataDir, "databases");
    }

    public File getFilesDir() {
        return new File(virtualDataDir, "files");
    }

    public boolean copySessionData(String sourcePackage) {
        try {
            File sourceDir = new File("/data/data/" + sourcePackage);
            if (!sourceDir.exists()) {
                Log.w(TAG, "Source data not found");
                return false;
            }

            File srcPrefs = new File(sourceDir, "shared_prefs");
            File dstPrefs = getSharedPrefsDir();

            if (srcPrefs.exists()) {
                File[] files = srcPrefs.listFiles();
                if (files != null) {
                    for (File f : files) {
                        copyFile(f, new File(dstPrefs, f.getName()));
                    }
                }
            }

            File srcFiles = new File(sourceDir, "files");
            File dstFiles = getFilesDir();

            if (srcFiles.exists()) {
                File[] files = srcFiles.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.isFile()) {
                            copyFile(f, new File(dstFiles, f.getName()));
                        }
                    }
                }
            }

            Log.i(TAG, "Session data copied");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Copy failed: " + e.getMessage());
            return false;
        }
    }

    private void copyFile(File src, File dst) {
        try {
            java.io.FileInputStream in = new java.io.FileInputStream(src);
            java.io.FileOutputStream out = new java.io.FileOutputStream(dst);
            byte[] buf = new byte[4096];
            int len;
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
            out.flush();
            out.close();
            in.close();
        } catch (Exception ignored) {}
    }

    public boolean isActive() {
        return active;
    }
}
