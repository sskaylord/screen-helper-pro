package com.display.utils;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

public class ServiceBridge {

    private static final String TAG = "DispUtils";
    private final Context hostContext;
    private final Map<String, Object> serviceCache = new HashMap<>();
    private boolean hooked = false;

    public ServiceBridge(Context ctx) {
        this.hostContext = ctx;
    }

    public boolean install() {
        try {
            hookActivityManager();
            hookPackageManager();
            hooked = true;
            Log.i(TAG, "Service bridge installed");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Bridge install failed: " + e.getMessage());
            return false;
        }
    }

    private void hookActivityManager() throws Exception {
        Class<?> amnClass = Class.forName("android.app.ActivityManagerNative");
        Field gDefaultField = amnClass.getDeclaredField("gDefault");
        gDefaultField.setAccessible(true);
        Object gDefault = gDefaultField.get(null);

        if (gDefault == null) {
            Class<?> smClass = Class.forName("android.app.ActivityManager");
            gDefaultField = smClass.getDeclaredField("IActivityManagerSingleton");
            gDefaultField.setAccessible(true);
            gDefault = gDefaultField.get(null);
        }

        if (gDefault == null) return;

        Field instanceField = gDefault.getClass().getDeclaredField("mInstance");
        instanceField.setAccessible(true);
        final Object realAM = instanceField.get(gDefault);

        if (realAM == null) return;

        Object proxy = Proxy.newProxyInstance(
            realAM.getClass().getClassLoader(),
            new Class[]{Class.forName("android.app.IActivityManager")},
            new AMHandler(realAM)
        );

        instanceField.set(gDefault, proxy);
        Log.i(TAG, "AM hooked");
    }

    private void hookPackageManager() throws Exception {
        Class<?> atClass = Class.forName("android.app.ActivityThread");
        Method currentAT = atClass.getMethod("currentActivityThread");
        Object at = currentAT.invoke(null);

        Field sPackageManagerField = atClass.getDeclaredField("sPackageManager");
        sPackageManagerField.setAccessible(true);
        final Object realPM = sPackageManagerField.get(at);

        if (realPM == null) return;

        Object proxy = Proxy.newProxyInstance(
            realPM.getClass().getClassLoader(),
            new Class[]{Class.forName("android.content.pm.IPackageManager")},
            new PMHandler(realPM)
        );

        sPackageManagerField.set(at, proxy);

        Field pmField = hostContext.getPackageManager().getClass().getDeclaredField("mPM");
        pmField.setAccessible(true);
        pmField.set(hostContext.getPackageManager(), proxy);

        Log.i(TAG, "PM hooked");
    }

    private class AMHandler implements InvocationHandler {
        private final Object real;

        AMHandler(Object real) { this.real = real; }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();

            if ("getRunningAppProcesses".equals(name)) {
                Object result = method.invoke(real, args);
                return filterProcesses(result);
            }

            if ("getContentProvider".equals(name)) {
                return method.invoke(real, args);
            }

            return method.invoke(real, args);
        }

        private Object filterProcesses(Object processList) {
            return processList;
        }
    }

    private class PMHandler implements InvocationHandler {
        private final Object real;

        PMHandler(Object real) { this.real = real; }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();

            if ("getPackageInfo".equals(name)) {
                return method.invoke(real, args);
            }

            if ("getApplicationInfo".equals(name)) {
                return method.invoke(real, args);
            }

            return method.invoke(real, args);
        }
    }

    public String getSpoofedPackageName() {
        return hostContext.getPackageName();
    }

    public int getSpoofedUid() {
        return hostContext.getApplicationInfo().uid;
    }

    public boolean isHooked() {
        return hooked;
    }
}
