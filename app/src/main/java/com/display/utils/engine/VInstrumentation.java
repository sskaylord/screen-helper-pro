package com.display.utils.engine;

import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class VInstrumentation {
    private static final String TAG = "VI";
    private static VInstrumentation s;
    private boolean hooked;
    private VInstrumentation() {}
    public static VInstrumentation get() { if (s == null) s = new VInstrumentation(); return s; }

    public void launch(Activity stub, Intent realIntent, ActivityInfo ti,
                       VActivityManager.SandboxRecord rec, int slot) {
        ensureHooked();
        String cls = realIntent.getComponent() != null ? realIntent.getComponent().getClassName() : ti.name;
        Log.i(TAG, "Launching " + cls + " in stub[" + slot + "]");
        try {
            ClassLoader tcl = VClassLoader.getCL();
            if (tcl == null) { stub.finish(); return; }
            Class<?> rc = tcl.loadClass(cls);
            Class<?> at = Class.forName("android.app.ActivityThread");
            Object thread = at.getMethod("currentActivityThread").invoke(null);
            Field af = at.getDeclaredField("mActivities"); af.setAccessible(true);
            Object map = af.get(thread);
            IBinder token = (IBinder) Activity.class.getMethod("getActivityToken").invoke(stub);
            Object acr = map.getClass().getMethod("get", Object.class).invoke(map, token);
            if (acr == null) { stub.finish(); return; }
            Field actF = acr.getClass().getDeclaredField("activity"); actF.setAccessible(true);
            Instrumentation instr = (Instrumentation) at.getDeclaredField("mInstrumentation").get(thread);
            // Use reflection to get the field value properly
            Field instrF = at.getDeclaredField("mInstrumentation"); instrF.setAccessible(true);
            instr = (Instrumentation) instrF.get(thread);
            Activity real = instr.newActivity(tcl, cls, realIntent);
            if (real == null) { stub.finish(); return; }
            actF.set(acr, real);
            Method oc = Activity.class.getDeclaredMethod("onCreate", Bundle.class);
            oc.setAccessible(true); oc.invoke(real, (Bundle)null);
            Method os = Activity.class.getDeclaredMethod("onStart");
            os.setAccessible(true); os.invoke(real);
            Method or = Activity.class.getDeclaredMethod("onResume");
            or.setAccessible(true); or.invoke(real);
            Log.i(TAG, "OK: " + cls);
        } catch (Exception e) {
            Log.e(TAG, "FAIL: " + e);
            e.printStackTrace();
            stub.finish();
        }
    }

    private void ensureHooked() {
        if (hooked) return;
        try {
            Class<?> at = Class.forName("android.app.ActivityThread");
            Object thread = at.getMethod("currentActivityThread").invoke(null);
            Field f = at.getDeclaredField("mInstrumentation"); f.setAccessible(true);
            Instrumentation orig = (Instrumentation) f.get(thread);
            f.set(thread, new VIW(orig));
            hooked = true;
        } catch (Exception e) { Log.e(TAG, "Hook fail: " + e); }
    }
}
