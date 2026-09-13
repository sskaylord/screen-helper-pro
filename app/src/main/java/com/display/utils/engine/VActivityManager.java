package com.display.utils.engine;

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
            Object singleton;
            try {
                Class<?> am = Class.forName("android.app.ActivityManager");
                Field f = am.getDeclaredField("IActivityManagerSingleton");
                f.setAccessible(true);
                singleton = f.get(null);
            } catch (Exception e) {
                Class<?> amn = Class.forName("android.app.ActivityManagerNative");
                Field f = amn.getDeclaredField("gDefault");
                f.setAccessible(true);
                singleton = f.get(null);
            }
            Class<?> sc = Class.forName("android.util.Singleton");
            Field inst = sc.getDeclaredField("mInstance");
            inst.setAccessible(true);
            Method get = sc.getDeclaredMethod("get");
            get.setAccessible(true);
            Object raw = get.invoke(singleton);
            Class<?> iams = Class.forName("android.app.IActivityManager");
            Object proxy = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class[]{iams}, new AMSH(raw));
            inst.set(singleton, proxy);
            Log.i(TAG, "AMS hooked");
        } catch (Exception e) { Log.e(TAG, "AMS hook fail: " + e); }
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
        if (slot < 0) { stub.finish(); return; }
        Intent ri = realIntents.get(slot);
        String pkg = targetPkgs.get(slot);
        ActivityInfo ti = targetInfos.get(slot);
        if (ri == null || pkg == null) { stub.finish(); return; }
        SandboxRecord rec = apps.get(pkg);
        if (rec == null) { stub.finish(); return; }
        IORedirect.activate(rec.dataDir, rec.libDir, pkg);
        try { VClassLoader.load(ctx, rec.apkPath, rec.libDir, rec.cacheDir); }
        catch (Exception e) { Log.e(TAG, "CL fail: " + e); stub.finish(); return; }
        VInstrumentation.get().launch(stub, ri, ti, rec, slot);
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
        ctx.startActivity(si);
    }

    public boolean isInstalled(String pkg) { SandboxRecord r = apps.get(pkg); return r != null && r.installed; }
    public List<SandboxRecord> getInstalledApps() { return new ArrayList<>(apps.values()); }
    public SandboxRecord getRecord(String pkg) { return apps.get(pkg); }
}
