package com.display.utils.engine;

import android.content.Context;
import android.util.Log;
import dalvik.system.DexClassLoader;
import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class VClassLoader {
    private static DexClassLoader cl;

    public static synchronized void load(Context ctx, String apk, String libDir, String cacheDir) throws Exception {
        if (cl != null) return;
        new File(cacheDir).mkdirs();
        cl = new DexClassLoader(apk, cacheDir, libDir, ctx.getClassLoader());
        Log.i("VCL", "Loaded: " + apk);
    }

    public static ClassLoader getCL() { return cl; }

    public static void extractLibs(String apk, String dest) throws Exception {
        File d = new File(dest); d.mkdirs();
        ZipFile z = new ZipFile(apk);
        String arch = null;
        for (String a : new String[]{"arm64-v8a", "armeabi-v7a"}) {
            var en = z.entries();
            while (en.hasMoreElements()) {
                if (en.nextElement().getName().startsWith("lib/" + a + "/")) { arch = a; break; }
            }
            if (arch != null) break;
        }
        if (arch == null) { z.close(); return; }
        var en = z.entries();
        while (en.hasMoreElements()) {
            ZipEntry e = en.nextElement();
            String n = e.getName();
            if (n.startsWith("lib/" + arch + "/") && n.endsWith(".so") && !e.isDirectory()) {
                File out = new File(dest, n.substring(n.lastIndexOf('/') + 1));
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
        z.close();
    }
}
