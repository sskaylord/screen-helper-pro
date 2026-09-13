package com.display.utils.engine;

import android.content.Context;
import android.util.Log;
import dalvik.system.DexClassLoader;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class VClassLoader {
    private static final String TAG = "VCL";
    private static DexClassLoader sTargetCL;
    private static String sCurrentPkg;

    public static synchronized void loadApk(Context ctx, String apkPath, String nativeLibDir, String cacheDir) throws Exception {
        if (sTargetCL != null) return;

        new File(cacheDir).mkdirs();

        sTargetCL = new DexClassLoader(apkPath, cacheDir, nativeLibDir, ctx.getClassLoader());
        Log.i(TAG, "Loaded: " + apkPath);
    }

    public static ClassLoader getTargetClassLoader() { return sTargetCL; }

    public static void reset() {
        sTargetCL = null;
        sCurrentPkg = null;
    }

    public static void extractNativeLibs(String apkPath, String destDir) throws Exception {
        File dest = new File(destDir);
        dest.mkdirs();

        ZipFile zip = new ZipFile(apkPath);
        String[] archs = {"arm64-v8a", "armeabi-v7a"};
        String arch = null;

        // Detect preferred arch
        for (String a : archs) {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                if (entries.nextElement().getName().startsWith("lib/" + a + "/")) {
                    arch = a; break;
                }
            }
            if (arch != null) break;
        }

        if (arch == null) { zip.close(); return; }

        var entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String name = entry.getName();
            if (name.startsWith("lib/" + arch + "/") && name.endsWith(".so") && !entry.isDirectory()) {
                String soName = name.substring(name.lastIndexOf('/') + 1);
                File out = new File(destDir, soName);
                if (!out.exists()) {
                    InputStream is = zip.getInputStream(entry);
                    FileOutputStream fos = new FileOutputStream(out);
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = is.read(buf)) > 0) fos.write(buf, 0, n);
                    fos.close(); is.close();
                    out.setReadable(true, false);
                    out.setExecutable(true, false);
                }
            }
        }
        zip.close();
        Log.i(TAG, "Native libs extracted to " + destDir);
    }
}
