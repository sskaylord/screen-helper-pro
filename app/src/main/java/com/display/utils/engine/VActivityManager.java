package com.display.utils.engine;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.SparseArray;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class VActivityManager {
    private static final String TAG = "VAM";
    private static VActivityManager sInstance;
    static final String HOST_PKG = "com.display.utils";

    // 10 stub slots (8 portrait + 2 landscape)
    static final int STUB_COUNT = 10;
    private final AtomicInteger stubCounter = new AtomicInteger(0);

    // Stub class names
    static String[] sStubClasses;

    // Real intent storage: stubIndex → real Intent
    final SparseArray<Intent> mRealIntents = new SparseArray<>();
    // Stub index → target package
    final SparseArray<String> mTargetPkgs = new SparseArray<>();
    // Stub index → ActivityInfo of real target
    final SparseArray<ActivityInfo> mTargetInfos = new SparseArray<>();

    // Installed sandbox apps
    final Map<String, SandboxRecord> mInstalledApps = new HashMap<>();

    private Context mContext;
    private Handler mHandler;
    private boolean mHooked;

    public static class SandboxRecord {
        public String packageName;
        public String apkPath;
        public String dataDir;
        public String nativeLibDir;
        public String cacheDir;
        public ApplicationInfo appInfo;
        public PackageInfo pkgInfo;
        public boolean installed;
        public long installTime;
    }

    private VActivityManager() {}

    public static VActivityManager get() {
        if (sInstance == null) sInstance = new VActivityManager();
        return sInstance;
    }

    public void init(Context ctx) {
        if (mHooked) return;
        mContext = ctx.getApplicationContext();
        mHandler = new Handler(Looper.getMainLooper());

        sStubClasses = new String[STUB_COUNT];
        for (int i = 0; i < 8; i++) sStubClasses[i] = HOST_PKG + ".engine.StubActivityP" + i;
        for (int i = 0; i < 2; i++) sStubClasses[8 + i] = HOST_PKG + ".engine.StubActivityL" + i;

        hookActivityManager();
        mHooked = true;
        Log.i(TAG, "VActivityManager ready");
    }

    // ===================== AMS HOOK =====================
    private void hookActivityManager() {
        try {
            Object singleton;
            if (Build.VERSION.SDK_INT >= 26) {
                Class<?> amClass = Class.forName("android.app.ActivityManager");
                Field f = amClass.getDeclaredField("IActivityManagerSingleton");
                f.setAccessible(true);
                singleton = f.get(null);
            } else {
                Class<?> amnClass = Class.forName("android.app.ActivityManagerNative");
                Field f = amnClass.getDeclaredField("gDefault");
                f.setAccessible(true);
                singleton = f.get(null);
            }

            Class<?> singletonClass = Class.forName("android.util.Singleton");
            Field instanceField = singletonClass.getDeclaredField("mInstance");
            instanceField.setAccessible(true);

            // Force init
            Method getMethod = singletonClass.getDeclaredMethod("get");
            getMethod.setAccessible(true);
            Object rawAMS = getMethod.invoke(singleton);

            Class<?> iamsInterface = Class.forName("android.app.IActivityManager");

            Object proxy = Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class[]{iamsInterface},
                new AMSHandler(rawAMS)
            );

            instanceField.set(singleton, proxy);
            Log.i(TAG, "AMS proxy installed");
        } catch (Exception e) {
            Log.e(TAG, "AMS hook failed: " + e);
        }
    }

    private class AMSHandler implements InvocationHandler {
        private final Object mReal;
        AMSHandler(Object real) { mReal = real; }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            try {
                if ("startActivity".equals(name) || "startActivityAsUser".equals(name) ||
                    "startActivityForResult".equals(name)) {
                    return handleStartActivity(method, args);
                }
                if ("getIntentSender".equals(name)) {
                    return method.invoke(mReal, args);
                }
            } catch (Exception e) {
                Log.e(TAG, "Hook error in " + name + ": " + e);
            }
            return method.invoke(mReal, args);
        }

        private Object handleStartActivity(Method method, Object[] args) throws Throwable {
            Intent intent = null;
            int intentIdx = -1;
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof Intent) { intent = (Intent) args[i]; intentIdx = i; break; }
            }
            if (intent == null) return method.invoke(mReal, args);

            // Resolve target
            ComponentName comp = intent.getComponent();
            if (comp == null) {
                // Try to resolve
                PackageManager pm = mContext.getPackageManager();
                var ri = pm.resolveActivity(intent, 0);
                if (ri != null && ri.activityInfo != null) {
                    comp = new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name);
                }
            }
            if (comp == null) return method.invoke(mReal, args);

            String targetPkg = comp.getPackageName();
            SandboxRecord rec = mInstalledApps.get(targetPkg);
            if (rec == null || !rec.installed) {
                return method.invoke(mReal, args); // Not sandboxed
            }

            // Get target ActivityInfo
            ActivityInfo targetInfo = null;
            try {
                targetInfo = mContext.getPackageManager().getActivityInfo(comp, 0);
            } catch (Exception ignored) {}

            // Allocate stub
            boolean landscape = targetInfo != null &&
                (targetInfo.screenOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE ||
                 targetInfo.screenOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE ||
                 targetInfo.screenOrientation == ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);

            int slot = allocSlot(landscape);
            if (slot < 0) {
                Log.w(TAG, "No free stub slot");
                return method.invoke(mReal, args);
            }

            // Store real intent
            mRealIntents.put(slot, new Intent(intent));
            mTargetPkgs.put(slot, targetPkg);
            if (targetInfo != null) mTargetInfos.put(slot, targetInfo);

            // Build stub intent
            String stubClass = sStubClasses[slot];
            Intent stubIntent = new Intent();
            stubIntent.setComponent(new ComponentName(HOST_PKG, stubClass));
            stubIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            stubIntent.putExtra("_vs", slot);

            args[intentIdx] = stubIntent;
            Log.i(TAG, "Redirect " + targetPkg + "/" + comp.getClassName() + " → stub[" + slot + "]");
            return method.invoke(mReal, args);
        }
    }

    private int allocSlot(boolean landscape) {
        int start = landscape ? 8 : 0;
        int end = landscape ? 10 : 8;
        for (int i = start; i < end; i++) {
            if (mRealIntents.get(i) == null) return i;
        }
        // Reuse oldest
        int oldest = start;
        return oldest;
    }

    void freeSlot(int slot) {
        mRealIntents.remove(slot);
        mTargetPkgs.remove(slot);
        mTargetInfos.remove(slot);
    }

    // ===================== STUB LIFECYCLE =====================
    public void onStubCreate(Activity stub, Bundle savedInstanceState) {
        Intent stubIntent = stub.getIntent();
        int slot = stubIntent.getIntExtra("_vs", -1);
        if (slot < 0) { stub.finish(); return; }

        Intent realIntent = mRealIntents.get(slot);
        String targetPkg = mTargetPkgs.get(slot);
        ActivityInfo targetInfo = mTargetInfos.get(slot);

        if (realIntent == null || targetPkg == null) { stub.finish(); return; }

        SandboxRecord rec = mInstalledApps.get(targetPkg);
        if (rec == null) { stub.finish(); return; }

        Log.i(TAG, "Stub[" + slot + "] creating for " + targetPkg);

        // Activate IO redirect
        IORedirect.activateForPackage(rec.dataDir, rec.nativeLibDir, targetPkg);

        // Load target classes
        try {
            VClassLoader.loadApk(mContext, rec.apkPath, rec.nativeLibDir, rec.cacheDir);
        } catch (Exception e) {
            Log.e(TAG, "ClassLoad failed: " + e);
            stub.finish();
            return;
        }

        // Delegate to VInstrumentation
        VInstrumentation.get().handleStubActivity(stub, realIntent, targetInfo, rec, slot);
    }

    public void onStubDestroy(Activity stub) {
        int slot = stub.getIntent().getIntExtra("_vs", -1);
        if (slot >= 0) freeSlot(slot);
    }

    // ===================== APP MANAGEMENT =====================
    public boolean installApp(String pkg) {
        if (mInstalledApps.containsKey(pkg) && mInstalledApps.get(pkg).installed) return true;
        try {
            PackageManager pm = mContext.getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
            PackageInfo pi = pm.getPackageInfo(pkg, 0);

            SandboxRecord rec = new SandboxRecord();
            rec.packageName = pkg;
            rec.apkPath = ai.sourceDir;
            rec.dataDir = mContext.getFilesDir().getAbsolutePath() + "/sandbox/" + pkg;
            rec.nativeLibDir = rec.dataDir + "/lib";
            rec.cacheDir = rec.dataDir + "/cache";
            rec.appInfo = ai;
            rec.pkgInfo = pi;
            rec.installed = true;
            rec.installTime = System.currentTimeMillis();

            new File(rec.dataDir).mkdirs();
            new File(rec.nativeLibDir).mkdirs();
            new File(rec.cacheDir).mkdirs();

            // Extract native libs
            VClassLoader.extractNativeLibs(rec.apkPath, rec.nativeLibDir);

            mInstalledApps.put(pkg, rec);
            Log.i(TAG, "Installed: " + pkg + " from " + rec.apkPath);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Install failed: " + pkg + " - " + e);
            return false;
        }
    }

    public void launchApp(String pkg) {
        Log.i(TAG, "launchApp: " + pkg + " installed=" + mInstalledApps.containsKey(pkg));
        SandboxRecord rec = mInstalledApps.get(pkg);
        if (rec == null) { installApp(pkg); rec = mInstalledApps.get(pkg); }
        if (rec == null) { Log.e(TAG, "No record for " + pkg); return; }

        // Get real launch intent
        Intent realIntent = mContext.getPackageManager().getLaunchIntentForPackage(pkg);
        if (realIntent == null) {
            Log.e(TAG, "No launch intent for " + pkg);
            return;
        }

        // Resolve real activity
        ComponentName comp = realIntent.getComponent();
        if (comp == null) {
            var ri = mContext.getPackageManager().resolveActivity(realIntent, 0);
            if (ri != null && ri.activityInfo != null) {
                comp = new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name);
            }
        }
        if (comp == null) { Log.e(TAG, "Cannot resolve activity for " + pkg); return; }

        // Get ActivityInfo
        ActivityInfo targetInfo = null;
        try { targetInfo = mContext.getPackageManager().getActivityInfo(comp, 0); } catch (Exception ignored) {}

        // Allocate stub slot DIRECTLY (bypass AMS hook)
        int slot = allocSlot(false);
        if (slot < 0) { Log.e(TAG, "No free stub slot"); return; }

        mRealIntents.put(slot, new Intent(realIntent));
        mTargetPkgs.put(slot, pkg);
        if (targetInfo != null) mTargetInfos.put(slot, targetInfo);

        // Launch stub activity directly
        String stubClass = sStubClasses[slot];
        Intent stubIntent = new Intent();
        stubIntent.setComponent(new ComponentName(HOST_PKG, stubClass));
        stubIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        stubIntent.putExtra("_vs", slot);

        Log.i(TAG, "Launching stub[" + slot + "] for " + pkg + "/" + comp.getClassName());
        mContext.startActivity(stubIntent);
    }

    public void uninstallApp(String pkg) {
        mInstalledApps.remove(pkg);
        // Clean sandbox dir
        File dir = new File(mContext.getFilesDir(), "sandbox/" + pkg);
        deleteRecursive(dir);
    }

    public boolean isInstalled(String pkg) {
        SandboxRecord r = mInstalledApps.get(pkg);
        return r != null && r.installed;
    }

    public List<SandboxRecord> getInstalledApps() {
        return new ArrayList<>(mInstalledApps.values());
    }

    public SandboxRecord getRecord(String pkg) { return mInstalledApps.get(pkg); }

    private void deleteRecursive(File f) {
        if (f == null || !f.exists()) return;
        if (f.isDirectory()) for (File c : f.listFiles()) deleteRecursive(c);
        f.delete();
    }
}
