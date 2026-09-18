package com.display.utils.engine;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.util.Log;
import dalvik.system.DexClassLoader;
import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.Enumeration;

public class VClassLoader {
    private static final String TAG = "VCL";
    private static DexClassLoader cl;

    public static synchronized void load(Context ctx, String pkg,
            String apk, String libDir, String cacheDir) throws Exception {
        if (cl != null) return;
        new File(cacheDir).mkdirs();

        // Split APK topla
        StringBuilder paths = new StringBuilder(apk);
        try {
            ApplicationInfo ai = ctx.getPackageManager().getApplicationInfo(pkg, 0);
            if (ai.splitSourceDirs != null) {
                for (String split : ai.splitSourceDirs) {
                    paths.append(File.pathSeparator).append(split);
                    Log.i(TAG, "Split: " + split);
                }
            }
        } catch (Exception e) { Log.w(TAG, "Split detect: " + e); }

        cl = new DexClassLoader(paths.toString(), cacheDir, libDir, ctx.getClassLoader());
        Log.i(TAG, "Loaded: " + paths);
    }

    public static ClassLoader getCL() { return cl; }
    public static synchronized void reset() { cl = null; }

    public static void extractLibs(String apk, String dest) throws Exception {
        File d = new File(dest); d.mkdirs();
        try (ZipFile z = new ZipFile(apk)) {
            String arch = null;
            for (String a : new String[]{"arm64-v8a", "armeabi-v7a"}) {
                Enumeration<? extends ZipEntry> en = z.entries();
                while (en.hasMoreElements()) {
                    if (en.nextElement().getName().startsWith("lib/" + a + "/")) { arch = a; break; }
                }
                if (arch != null) break;
            }
            if (arch == null) return;
            Enumeration<? extends ZipEntry> en = z.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                String n = e.getName();
                if (n.startsWith("lib/" + arch + "/") && n.endsWith(".so") && !e.isDirectory()) {
                    File out = new File(d, n.substring(n.lastIndexOf('/') + 1));
                    if (!out.exists()) {
                        InputStream is = z.getInputStream(e);
                        FileOutputStream fos = new FileOutputStream(out);
                        byte[] b = new byte[8192]; int r;
                        while ((r = is.read(b)) > 0) fos.write(b, 0, r);
                        fos.close(); is.close();
                        out.setReadable(true, false); out.setExecutable(true, false);
                    }
                }
            }
        }
    }
}
