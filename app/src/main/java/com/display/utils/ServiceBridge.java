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

    public String getSpoofedSignature() {
        return "308201dd30820146a0030201020204";
    }

    public String getSpoofedBuildFingerprint() {
        String[] fps = {
            "samsung/beyond2ltexx/beyond2:12/SP1A.210812.016/G975FXXS9FVB1:user/release-keys",
            "google/raven/raven:14/AP2A.240605.024/11583682:user/release-keys",
            "OnePlus/OP594DL1/OP594DL1:14/UKQ1.230924.001/user/release-keys",
            "Xiaomi/aurora/aurora:14/UKQ1.231003.002/user/release-keys"
        };
        return fps[new java.util.Random().nextInt(fps.length)];
    }

    public String getSpoofedDeviceId() {
        StringBuilder sb = new StringBuilder();
        java.util.Random r = new java.util.Random(hostContext.getPackageName().hashCode());
        for (int i = 0; i < 16; i++) {
            sb.append(Integer.toHexString(r.nextInt(16)));
        }
        return sb.toString();

    public String getSpoofedSignature() {
        return "308201dd30820146a0030201020204";
    }

    public String getSpoofedBuildFingerprint() {
        String[] fps = {
            "samsung/beyond2ltexx/beyond2:12/SP1A.210812.016/user/release-keys",
            "google/raven/raven:14/AP2A.240605.024/user/release-keys",
            "OnePlus/OP594DL1/OP594DL1:14/UKQ1.230924.001/user/release-keys",
            "Xiaomi/aurora/aurora:14/UKQ1.231003.002/user/release-keys"
        };
        return fps[new java.util.Random().nextInt(fps.length)];
    }

    public String getSpoofedDeviceId() {
        StringBuilder sb = new StringBuilder();
        java.util.Random r = new java.util.Random(hostContext.getPackageName().hashCode());
        for (int i = 0; i < 16; i++) {
            sb.append(Integer.toHexString(r.nextInt(16)));
        }
        return sb.toString();
    }
}
