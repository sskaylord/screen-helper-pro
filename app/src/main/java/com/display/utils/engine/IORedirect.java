package com.display.utils.engine;

import android.util.Log;
import java.util.HashMap;
import java.util.Map;

public class IORedirect {
    private static final String TAG = "IOR";
    private static boolean sNativeLoaded;
    private static final Map<String, String[]> sRedirects = new HashMap<>();

    public static void init() {
        if (!sNativeLoaded) {
            try {
                System.loadLibrary("display_utils");
                sNativeLoaded = true;
            } catch (Exception e) {
                Log.e(TAG, "Native load failed: " + e);
            }
        }
    }

    public static synchronized void activateForPackage(String dataDir, String libDir, String pkg) {
        if (sRedirects.containsKey(pkg)) return;
        sRedirects.put(pkg, new String[]{dataDir, libDir});
        if (sNativeLoaded) {
            try {
                nativeAddRedirect(pkg, dataDir, libDir);
                Log.i(TAG, "Redirect active: " + pkg);
            } catch (Exception e) {
                Log.e(TAG, "Native redirect failed: " + e);
            }
        }
    }

    public static String redirectPath(String path, String pkg) {
        String[] dirs = sRedirects.get(pkg);
        if (dirs == null || path == null) return path;

        String orig1 = "/data/data/" + pkg;
        if (path.startsWith(orig1)) return dirs[0] + path.substring(orig1.length());

        String orig2 = "/data/user/0/" + pkg;
        if (path.startsWith(orig2)) return dirs[0] + path.substring(orig2.length());

        return path;
    }

    public static boolean isActive(String pkg) { return sRedirects.containsKey(pkg); }

    private static native void nativeAddRedirect(String pkg, String dataDir, String libDir);
}
