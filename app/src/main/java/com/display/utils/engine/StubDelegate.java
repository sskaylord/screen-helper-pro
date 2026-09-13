package com.display.utils.engine;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import java.lang.reflect.Method;
import java.util.WeakHashMap;

public class StubDelegate {
    private static final String TAG = "SD";
    private static final WeakHashMap<Activity, DelegateInfo> delegates = new WeakHashMap<>();

    static class DelegateInfo {
        Object realActivity;
        Class<?> realClass;
        Intent realIntent;
        VActivityManager.SandboxRecord record;
        boolean created;
    }

    public static void attach(Activity stub, Intent realIntent, Class<?> realClass,
                              VActivityManager.SandboxRecord record) {
        DelegateInfo info = new DelegateInfo();
        info.realIntent = realIntent;
        info.realClass = realClass;
        info.record = record;

        try {
            info.realActivity = realClass.newInstance();
            // Inject host activity reference
            Method attach = findMethod(realClass, "attach", android.content.Context.class);
            if (attach != null) {
                attach.setAccessible(true);
                attach.invoke(info.realActivity, stub.getBaseContext());
            }
            info.created = true;
            delegates.put(stub, info);

            // Call onCreate on real activity
            Method onCreate = findMethod(realClass, "onCreate", Bundle.class);
            if (onCreate != null) {
                onCreate.setAccessible(true);
                onCreate.invoke(info.realActivity, (Bundle) null);
            }
        } catch (Exception e) {
            Log.e(TAG, "Attach failed: " + e);
        }
    }

    public static void onResume(Activity stub) {
        DelegateInfo info = delegates.get(stub);
        if (info != null && info.realActivity != null) {
            try {
                Method m = findMethod(info.realClass, "onResume");
                if (m != null) { m.setAccessible(true); m.invoke(info.realActivity); }
            } catch (Exception ignored) {}
        }
    }

    public static void onPause(Activity stub) {
        DelegateInfo info = delegates.get(stub);
        if (info != null && info.realActivity != null) {
            try {
                Method m = findMethod(info.realClass, "onPause");
                if (m != null) { m.setAccessible(true); m.invoke(info.realActivity); }
            } catch (Exception ignored) {}
        }
    }

    public static void onDestroy(Activity stub) {
        DelegateInfo info = delegates.remove(stub);
        if (info != null && info.realActivity != null) {
            try {
                Method m = findMethod(info.realClass, "onDestroy");
                if (m != null) { m.setAccessible(true); m.invoke(info.realActivity); }
            } catch (Exception ignored) {}
        }
    }

    public static void onBack(Activity stub) {
        DelegateInfo info = delegates.get(stub);
        if (info != null && info.realActivity != null) {
            try {
                Method m = findMethod(info.realClass, "onBackPressed");
                if (m != null) { m.setAccessible(true); m.invoke(info.realActivity); }
                else stub.finish();
            } catch (Exception e) { stub.finish(); }
        } else { stub.finish(); }
    }

    private static Method findMethod(Class<?> cls, String name, Class<?>... params) {
        try { return cls.getDeclaredMethod(name, params); }
        catch (Exception e) {
            try { return cls.getMethod(name, params); }
            catch (Exception e2) { return null; }
        }
    }
}
