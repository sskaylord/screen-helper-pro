package com.display.utils.engine;

import com.display.utils.DisplayCore;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

public class VActivityManager {
    private static final String TAG = "VAM";
    private static VActivityManager s;
    static final String HOST = "com.display.utils";
    static final int SLOTS = 10;
    static String[] stubs;

    final SparseArray<Intent> realIntents = new SparseArray<>();
    final SparseArray<String> targetPkgs = new SparseArray<>();
    final SparseArray<ActivityInfo> targetInfos = new SparseArray<>();
    final Map<String, SandboxRecord> apps = new HashMap<>();

    private Context ctx;
    private boolean hooked;

    public static class SandboxRecord {
        public String packageName, apkPath, dataDir, libDir, cacheDir;
        public ApplicationInfo appInfo;
        public boolean installed;
    }

    private VActivityManager() {}
    public static VActivityManager get() { if (s == null) s = new VActivityManager(); return s; }

    public void init(Context c) {
        if (hooked) return;
        ctx = c.getApplicationContext();
        stubs = new String[SLOTS];
        for (int i = 0; i < 8; i++) stubs[i] = HOST + ".engine.StubP" + i;
        for (int i = 0; i < 2; i++) stubs[8 + i] = HOST + ".engine.StubL" + i;
        hookAMS();
        hooked = true;
    }

    private void hookAMS() {
        try {
            Object singleton = null;
            // Android 10+: ActivityTaskManager
            try {
                Class<?> atm = Class.forName("android.app.ActivityTaskManager");
                Field f = atm.getDeclaredField("IActivityTaskManagerSingleton");
                f.setAccessible(true);
                singleton = f.get(null);
                Log.i(TAG, "Using ActivityTaskManager singleton");
            } catch (Exception e) {
                // Fallback: ActivityManager
                try {
                    Class<?> am = Class.forName("android.app.ActivityManager");
                    Field f = am.getDeclaredField("IActivityManagerSingleton");
                    f.setAccessible(true);
                    singleton = f.get(null);
                    Log.i(TAG, "Using ActivityManager singleton");
                } catch (Exception e2) {
                    Class<?> amn = Class.forName("android.app.ActivityManagerNative");
                    Field f = amn.getDeclaredField("gDefault");
                    f.setAccessible(true);
                    singleton = f.get(null);
                    Log.i(TAG, "Using ActivityManagerNative gDefault");
                }
            }
            if (singleton == null) { Log.e(TAG, "Singleton null"); return; }

            Class<?> sc = Class.forName("android.util.Singleton");
            Field inst = sc.getDeclaredField("mInstance");
            inst.setAccessible(true);
            Object raw = inst.get(singleton);
            if (raw == null) {
                Method get = sc.getDeclaredMethod("get");
                get.setAccessible(true);
                raw = get.invoke(singleton);
            }

            // IActivityTaskManager veya IActivityManager
            Class<?> iface;
            try { iface = Class.forName("android.app.IActivityTaskManager"); }
            catch (ClassNotFoundException ex) { iface = Class.forName("android.app.IActivityManager"); }

            Object proxy = Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class[]{iface}, new AMSH(raw));
            inst.set(singleton, proxy);
            Log.i(TAG, "AMS hooked with " + iface.getSimpleName());
        } catch (Exception e) { Log.e(TAG, "AMS hook fail: " + e); e.printStackTrace(); }
    }

    private class AMSH implements InvocationHandler {
        private final Object real;
        AMSH(Object r) { real = r; }
        @Override public Object invoke(Object p, Method m, Object[] a) throws Throwable {
            String n = m.getName();
            if (n.startsWith("startActivity")) return hookStart(m, a);
            return m.invoke(real, a);
        }
        private Object hookStart(Method m, Object[] a) throws Throwable {
            Intent intent = null; int idx = -1;
            for (int i = 0; i < a.length; i++) if (a[i] instanceof Intent) { intent = (Intent)a[i]; idx = i; break; }
            if (intent == null) return m.invoke(real, a);
            ComponentName comp = intent.getComponent();
            if (comp == null) {
                var ri = ctx.getPackageManager().resolveActivity(intent, 0);
                if (ri != null && ri.activityInfo != null) comp = new ComponentName(ri.activityInfo.packageName, ri.activityInfo.name);
            }
            if (comp == null) return m.invoke(real, a);
            SandboxRecord rec = apps.get(comp.getPackageName());
            if (rec == null || !rec.installed) return m.invoke(real, a);
            ActivityInfo ti = null;
            try { ti = ctx.getPackageManager().getActivityInfo(comp, 0); } catch (Exception ignored) {}
            int slot = allocSlot(ti);
            if (slot < 0) return m.invoke(real, a);
            realIntents.put(slot, new Intent(intent));
            targetPkgs.put(slot, comp.getPackageName());
            if (ti != null) targetInfos.put(slot, ti);
            Intent si = new Intent();
            si.setComponent(new ComponentName(HOST, stubs[slot]));
            si.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            si.putExtra("_vs", slot);
            a[idx] = si;
            Log.i(TAG, "Redirect " + comp.getPackageName() + " -> stub[" + slot + "]");
            return m.invoke(real, a);
        }
    }

    private int allocSlot(ActivityInfo ti) {
        boolean land = ti != null && (ti.screenOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            || ti.screenOrientation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        int s = land ? 8 : 0, e = land ? 10 : 8;
        for (int i = s; i < e; i++) if (realIntents.get(i) == null) return i;
        return s;
    }

    void freeSlot(int slot) { realIntents.remove(slot); targetPkgs.remove(slot); targetInfos.remove(slot); }

    public void onStubCreate(Activity stub, Bundle saved) {
        int slot = stub.getIntent().getIntExtra("_vs", -1);
        Log.i(TAG, "onStubCreate slot=" + slot);
        if (slot < 0) { stub.finish(); return; }
        Intent ri = realIntents.get(slot);
        String pkg = targetPkgs.get(slot);
        ActivityInfo ti = targetInfos.get(slot);
        if (ri == null || pkg == null) { Log.e(TAG, "No data for slot " + slot); stub.finish(); return; }
        SandboxRecord rec = apps.get(pkg);
        if (rec == null) { Log.e(TAG, "No record for " + pkg); stub.finish(); return; }

        // 1 — IO redirect
        IORedirect.activate(rec.dataDir, rec.libDir, pkg);

        // 2 — ClassLoader (split APK dahil)
        try {
            VClassLoader.load(ctx, pkg, rec.apkPath, rec.libDir, rec.cacheDir);
        } catch (Exception e) { Log.e(TAG, "CL fail: " + e); stub.finish(); return; }

        // 3 — Target Application lifecycle
        startTargetApplication(rec);

        // 4 — VIW hook (stub intercept için kritik)
        VInstrumentation.get().ensureHookedPublic();

        // 5 — ACR patch
        VInstrumentation.get().launch(stub, ri, ti, rec, slot);

        // 6 — Overlay inject (Standoff 2)
        if ("com.axlebolt.standoff2".equals(pkg)) {
            new android.os.Handler(android.os.Looper.getMainLooper())
                .postDelayed(() -> {
                    try { DisplayCore.initialize(stub, pkg); }
                    catch (Exception e) { Log.e(TAG, "DC init: " + e); }
                }, 3500);
        }
    }

    private void startTargetApplication(SandboxRecord rec) {
        try {
            ClassLoader tcl = VClassLoader.getCL();
            if (tcl == null) return;
            android.content.pm.PackageInfo pi = ctx.getPackageManager().getPackageInfo(rec.packageName, 0);
            String appCls = pi.applicationInfo.className;
            if (appCls == null) appCls = "android.app.Application";
            Class<?> clz = tcl.loadClass(appCls);
            android.app.Application app = (android.app.Application) clz.newInstance();
            java.lang.reflect.Method attach = android.app.Application.class.getDeclaredMethod("attach", android.content.Context.class);
            attach.setAccessible(true);
            attach.invoke(app, ctx);
            app.onCreate();
            Log.i(TAG, "Target App started: " + appCls);
        } catch (Exception e) { Log.w(TAG, "App lifecycle skip: " + e); }
    }

    public void onStubDestroy(Activity stub) {
        int slot = stub.getIntent().getIntExtra("_vs", -1);
        if (slot >= 0) freeSlot(slot);
    }

    public boolean installApp(String pkg) {
        if (apps.containsKey(pkg) && apps.get(pkg).installed) return true;
        try {
            PackageManager pm = ctx.getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
            SandboxRecord r = new SandboxRecord();
            r.packageName = pkg; r.apkPath = ai.sourceDir;
            r.dataDir = ctx.getFilesDir().getAbsolutePath() + "/sb/" + pkg;
            r.libDir = r.dataDir + "/lib"; r.cacheDir = r.dataDir + "/cache";
            r.appInfo = ai; r.installed = true;
            new File(r.dataDir).mkdirs(); new File(r.libDir).mkdirs(); new File(r.cacheDir).mkdirs();
            VClassLoader.extractLibs(r.apkPath, r.libDir);
            apps.put(pkg, r);
            Log.i(TAG, "Installed: " + pkg);
            return true;
        } catch (Exception e) { Log.e(TAG, "Install fail: " + e); return false; }
    }

    public void launchApp(String pkg) {
        SandboxRecord rec = apps.get(pkg);
        if (rec == null) { installApp(pkg); rec = apps.get(pkg); }
        if (rec == null) return;
        Intent ri = ctx.getPackageManager().getLaunchIntentForPackage(pkg);
        if (ri == null) return;
        ComponentName comp = ri.getComponent();
        if (comp == null) {
            var resolve = ctx.getPackageManager().resolveActivity(ri, 0);
            if (resolve != null && resolve.activityInfo != null)
                comp = new ComponentName(resolve.activityInfo.packageName, resolve.activityInfo.name);
        }
        if (comp == null) return;
        ActivityInfo ti = null;
        try { ti = ctx.getPackageManager().getActivityInfo(comp, 0); } catch (Exception ignored) {}
        int slot = allocSlot(ti);
        if (slot < 0) return;
        realIntents.put(slot, new Intent(ri));
        targetPkgs.put(slot, pkg);
        if (ti != null) targetInfos.put(slot, ti);
        Intent si = new Intent();
        si.setComponent(new ComponentName(HOST, stubs[slot]));
        si.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        si.putExtra("_vs", slot);
        Log.i(TAG, "Direct launch stub[" + slot + "] for " + pkg);
        Log.i(TAG, "Stub class: " + stubs[slot]);
        
        // FLAG_ACTIVITY_NEW_TASK zaten var, ek flag'ler ekle
        si.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        
        try {
            ctx.startActivity(si);
            Log.i(TAG, "startActivity OK");
        } catch (android.content.ActivityNotFoundException e) {
            Log.e(TAG, "ActivityNotFound: " + stubs[slot] + " — manifest'te yok mu?");
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException: " + e.getMessage());
            // Fallback: FLAG_ACTIVITY_NEW_TASK olmadan dene
            try {
                si.removeFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                si.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
                ctx.startActivity(si);
                Log.i(TAG, "startActivity retry OK");
            } catch (Exception e2) {
                Log.e(TAG, "Retry also failed: " + e2.getMessage());
            }
        } catch (Exception e) {
            Log.e(TAG, "startActivity FAILED: " + e.getClass().getName() + ": " + e.getMessage());
        }
    }


    public String getRealClass(int slot) {
        Intent ri = realIntents.get(slot);
        if (ri != null && ri.getComponent() != null) return ri.getComponent().getClassName();
        ActivityInfo ti = targetInfos.get(slot);
        if (ti != null) return ti.name;
        return null;
    }

    public String getRealPkg(int slot) { return targetPkgs.get(slot); }

    public boolean isInstalled(String pkg) { SandboxRecord r = apps.get(pkg); return r != null && r.installed; }
    public List<SandboxRecord> getInstalledApps() { return new ArrayList<>(apps.values()); }
    public SandboxRecord getRecord(String pkg) { return apps.get(pkg); }
}
