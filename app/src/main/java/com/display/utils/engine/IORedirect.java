package com.display.utils.engine;

import android.util.Log;
import java.util.HashMap;
import java.util.Map;

public class IORedirect {
    private static boolean loaded;
    private static final Map<String, String[]> redirects = new HashMap<>();

    public static void init() {
        if (!loaded) { try { System.loadLibrary("display_utils"); loaded = true; } catch (Exception ignored) {} }
    }

    public static synchronized void activate(String dataDir, String libDir, String pkg) {
        if (redirects.containsKey(pkg)) return;
        redirects.put(pkg, new String[]{dataDir, libDir});
        if (loaded) { try { nativeAdd(pkg, dataDir, libDir); } catch (Exception ignored) {} }
    }

    private static native void nativeAdd(String pkg, String dataDir, String libDir);
}
