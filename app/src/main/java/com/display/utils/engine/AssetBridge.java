package com.display.utils.engine;

import android.content.Context;
import android.util.Log;
import dalvik.system.DexClassLoader;
import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class AssetBridge {
    private static final String TAG = "AB";
    private static DexClassLoader sTargetCL;
    private static String sNativeLib;

    public static synchronized void loadTarget(Context ctx, String apkPath, String nativeLibDir) throws Exception {
        if (sTargetCL != null) return;

        // Extract native libs from APK
        extractNativeLibs(apkPath, nativeLibDir);

        // Create isolated ClassLoader
        String dexOpt = ctx.getCacheDir().getAbsolutePath() + "/dex_opt";
        new File(dexOpt).mkdirs();

        sTargetCL = new DexClassLoader(apkPath, dexOpt, nativeLibDir, ctx.getClassLoader());
        sNativeLib = nativeLibDir;
        Log.i(TAG, "Target loaded: " + apkPath);
    }

    public static ClassLoader getTargetClassLoader() { return sTargetCL; }

    private static void extractNativeLibs(String apkPath, String destDir) throws Exception {
        ZipFile zip = new ZipFile(apkPath);
        String arch = "arm64-v8a";
        try {
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.startsWith("lib/" + arch + "/") && name.endsWith(".so")) {
                    String soName = name.substring(name.lastIndexOf('/') + 1);
                    File out = new File(destDir, soName);
                    if (!out.exists()) {
                        InputStream is = zip.getInputStream(entry);
                        FileOutputStream fos = new FileOutputStream(out);
                        byte[] buf = new byte[8192];
                        int n;
                        while ((n = is.read(buf)) > 0) fos.write(buf, 0, n);
                        fos.close(); is.close();
                    }
                }
            }
        } finally { zip.close(); }
    }
}
