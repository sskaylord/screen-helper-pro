package com.display.utils.engine;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

public class VPackageManager {
    private static final String TAG = "VPM";
    private static VPackageManager sInstance;
    private Context mContext;
    private boolean mHooked;
    private final Map<String, PackageInfo> mSpoofedPackages = new HashMap<>();

    private VPackageManager() {}

    public static VPackageManager get() {
        if (sInstance == null) sInstance = new VPackageManager();
        return sInstance;
    }

    public void init(Context ctx) {
        if (mHooked) return;
        mContext = ctx.getApplicationContext();
        hookPackageManager();
        mHooked = true;
    }

    private void hookPackageManager() {
        try {
            // Hook IPackageManager
            Class<?> atClass = Class.forName("android.app.ActivityThread");
            Method currentAT = atClass.getMethod("currentActivityThread");
            Object activityThread = currentAT.invoke(null);

            Field sPackageManagerField = atClass.getDeclaredField("sPackageManager");
            sPackageManagerField.setAccessible(true);
            Object rawIPM = sPackageManagerField.get(activityThread);

            if (rawIPM == null) return;

            Class<?> ipmClass = Class.forName("android.content.pm.IPackageManager");
            Object proxy = Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class[]{ipmClass},
                new PMHandler(rawIPM)
            );

            sPackageManagerField.set(activityThread, proxy);

            // Also hook in ApplicationPackageManager
            PackageManager pm = mContext.getPackageManager();
            Field mPMField = pm.getClass().getDeclaredField("mPM");
            mPMField.setAccessible(true);
            mPMField.set(pm, proxy);

            Log.i(TAG, "PackageManager hooked");
        } catch (Exception e) {
            Log.e(TAG, "PM hook failed: " + e);
        }
    }

    private class PMHandler implements InvocationHandler {
        private final Object mReal;
        PMHandler(Object real) { mReal = real; }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            try {
                if ("getPackageInfo".equals(name) && args != null && args.length > 0) {
                    String pkg = (String) args[0];
                    PackageInfo spoofed = mSpoofedPackages.get(pkg);
                    if (spoofed != null) return spoofed;
                }
                if ("getApplicationInfo".equals(name) && args != null && args.length > 0) {
                    String pkg = (String) args[0];
                    VActivityManager.SandboxRecord rec = VActivityManager.get().getRecord(pkg);
                    if (rec != null && rec.appInfo != null) {
                        // Redirect dataDir to sandbox
                        ApplicationInfo ai = new ApplicationInfo(rec.appInfo);
                        ai.dataDir = rec.dataDir;
                        ai.nativeLibraryDir = rec.nativeLibDir;
                        return ai;
                    }
                }
            } catch (Exception ignored) {}
            return method.invoke(mReal, args);
        }
    }

    public void addSpoofedPackage(PackageInfo pi) {
        mSpoofedPackages.put(pi.packageName, pi);
    }
}
