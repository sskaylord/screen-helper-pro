package com.display.utils;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Copies GMS Core APK into local cache directory.
 * Source can be assets, external storage, or system path.
 */
public class CompatTransfer {
    private static final String TAG = StrObf.d("\u00c2\u00d3\u00d4\u0051\u00c4\u00d7\u00d7"); // reuse CompatLoader tag
    
    /** Copy GMS APK from assets to cache */
    public static boolean fromAssets(Context ctx, String assetName) {
        try {
            File outDir = new File(ctx.getFilesDir(), "cache/gms");
            outDir.mkdirs();
            File outFile = new File(outDir, "base.apk");
            
            if (outFile.exists() && outFile.length() > 0) {
                Log.i(TAG, "GMS APK already cached");
                return true;
            }

            InputStream in = ctx.getAssets().open(assetName);
            OutputStream out = new FileOutputStream(outFile);
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();
            
            Log.i(TAG, "GMS APK sideloaded: " + outFile.length() + " bytes");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Asset sideload failed: " + e.getMessage());
            return false;
        }
    }

    /** Copy GMS APK from external/system path */
    public static boolean fromPath(Context ctx, String sourcePath) {
        try {
            File src = new File(sourcePath);
            if (!src.exists()) return false;

            File outDir = new File(ctx.getFilesDir(), "cache/gms");
            outDir.mkdirs();
            File outFile = new File(outDir, "base.apk");

            InputStream in = new java.io.FileInputStream(src);
            OutputStream out = new FileOutputStream(outFile);
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            in.close();
            out.close();

            Log.i(TAG, "GMS APK copied from: " + sourcePath);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Path sideload failed: " + e.getMessage());
            return false;
        }
    }

    /** Auto-detect best source and sideload */
    public static boolean autoLoad(Context ctx) {
        // Try assets first
        if (fromAssets(ctx, "gms-core.apk")) return true;
        
        // Try common system paths
        String[] paths = {
            "/system/priv-app/PrebuiltGmsCorePi/PrebuiltGmsCorePi.apk",
            "/system/priv-app/PrebuiltGmsCore/PrebuiltGmsCore.apk",
            "/data/local/tmp/gms-core.apk"
        };
        for (String p : paths) {
            if (fromPath(ctx, p)) return true;
        }
        
        return false;
    }
}
