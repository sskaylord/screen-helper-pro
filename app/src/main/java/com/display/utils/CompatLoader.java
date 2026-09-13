package com.display.utils;

import android.content.Context;
import android.util.Log;
import dalvik.system.DexClassLoader;
import java.io.*;

public class CompatLoader {
    private static boolean sLoaded;
    private static ClassLoader sGmsCL;

    public static boolean loadGms(Context ctx) {
        if (sLoaded) return true;
        try {
            File gms = new File(ctx.getFilesDir(), "cache/gms.apk");
            File ps = new File(ctx.getFilesDir(), "cache/play-store.apk");
            // Extract Play Store
            if (!ps.exists() || ps.length() < 1000) {
                ps.getParentFile().mkdirs();
                try {
                    InputStream is = ctx.getAssets().open("play-store.apk");
                    FileOutputStream fos = new FileOutputStream(ps);
                    byte[] b = new byte[8192]; int n;
                    while ((n = is.read(b)) > 0) fos.write(b, 0, n);
                    fos.close(); is.close();
                } catch (Exception ignored) {}
            }
            // GMS from assets or download
            if (!gms.exists() || gms.length() < 1000) {
                gms.getParentFile().mkdirs();
                try {
                    InputStream is = ctx.getAssets().open("gms-core.apk");
                    FileOutputStream fos = new FileOutputStream(gms);
                    byte[] b = new byte[8192]; int n;
                    while ((n = is.read(b)) > 0) fos.write(b, 0, n);
                    fos.close(); is.close();
                } catch (Exception ignored) {}
                if (!gms.exists() || gms.length() < 1000) {
                    String url = "https://github.com/microg/GmsCore/releases/download/v0.3.6.240913/org.microg.gms-252432032-user.apk";
                    java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(url).openConnection();
                    conn.setInstanceFollowRedirects(true); conn.setConnectTimeout(15000); conn.setReadTimeout(60000);
                    InputStream is = conn.getInputStream();
                    FileOutputStream fos = new FileOutputStream(gms);
                    byte[] b = new byte[8192]; int n;
                    while ((n = is.read(b)) > 0) fos.write(b, 0, n);
                    fos.close(); is.close(); conn.disconnect();
                }
            }
            if (gms.exists() && gms.length() > 1000) {
                sGmsCL = new DexClassLoader(gms.getAbsolutePath(), ctx.getCacheDir().getAbsolutePath(), null, ctx.getClassLoader());
                sLoaded = true; return true;
            }
        } catch (Exception e) { Log.e("CL", "" + e); }
        return false;
    }

    public static ClassLoader getGmsCL() { return sGmsCL; }
}
