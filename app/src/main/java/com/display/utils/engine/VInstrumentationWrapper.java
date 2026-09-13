package com.display.utils.engine;

import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class VInstrumentationWrapper extends Instrumentation {
    private static final String TAG = "VIW";
    private final Instrumentation mBase;

    public VInstrumentationWrapper(Instrumentation base) { mBase = base; }

    @Override
    public Activity newActivity(ClassLoader cl, String className, Intent intent)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        // Check if this is a sandboxed app launch
        VActivityManager vam = VActivityManager.get();
        // Try loading from target ClassLoader first
        ClassLoader targetCL = VClassLoader.getTargetClassLoader();
        if (targetCL != null) {
            try {
                Class<?> cls = targetCL.loadClass(className);
                if (Activity.class.isAssignableFrom(cls)) {
                    return (Activity) cls.newInstance();
                }
            } catch (ClassNotFoundException ignored) {}
        }
        return mBase.newActivity(cl, className, intent);
    }

    @Override
    public void callApplicationOnCreate(Application app) {
        mBase.callApplicationOnCreate(app);
    }

    @Override
    public void callActivityOnCreate(Activity activity, Bundle icicle) {
        mBase.callActivityOnCreate(activity, icicle);
    }

    @Override
    public void callActivityOnResume(Activity activity) {
        mBase.callActivityOnResume(activity);
    }

    @Override
    public void callActivityOnPause(Activity activity) {
        mBase.callActivityOnPause(activity);
    }

    @Override
    public void callActivityOnDestroy(Activity activity) {
        mBase.callActivityOnDestroy(activity);
    }
}
