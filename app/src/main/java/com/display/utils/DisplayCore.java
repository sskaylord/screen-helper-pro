package com.display.utils;

import android.content.Context;
import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class DisplayCore {
    private static final String TAG = "DC";
    private static OverlayPanel panel;
    private static boolean nativeReady;

    static {
        try { System.loadLibrary("env_check"); nativeReady = true; Log.i(TAG, "Native OK"); }
        catch (UnsatisfiedLinkError e) { Log.e(TAG, "Native fail: " + e); }
    }

    public static native void nativeInit(String il2cppPath);
    public static native long dumpOffsets();
    public static native boolean readMem(long addr, byte[] buf, int size);

    public static void initialize(Context ctx, String pkg) {
        String path = findIl2cpp(pkg);
        if (nativeReady && path != null) {
            nativeInit(path);
            Log.i(TAG, "nativeInit: " + path);
        } else {
            Log.w(TAG, "il2cpp not found or native unavailable");
        }
        if (panel == null) panel = new OverlayPanel(ctx);
        panel.show();
    }

    private static String findIl2cpp(String pkg) {
        String[] paths = {
            "/data/data/com.display.utils/sb/" + pkg + "/lib/libil2cpp.so",
            "/data/app/" + pkg + "-1/lib/arm64/libil2cpp.so",
            "/data/app/" + pkg + "-2/lib/arm64/libil2cpp.so"
        };
        for (String p : paths) if (new File(p).exists()) return p;
        try (BufferedReader br = new BufferedReader(new FileReader("/proc/self/maps"))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("libil2cpp.so")) {
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length >= 6) return parts[5];
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static OverlayPanel getPanel() { return panel; }
}
