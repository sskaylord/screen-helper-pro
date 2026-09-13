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

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class VInstrumentation {
    private static final String TAG = "VI";
    private static VInstrumentation sInstance;
    private boolean mHooked;

    private VInstrumentation() {}

    public static VInstrumentation get() {
        if (sInstance == null) sInstance = new VInstrumentation();
        return sInstance;
    }

    public void handleStubActivity(Activity stub, Intent realIntent, ActivityInfo targetInfo,
                                    VActivityManager.SandboxRecord record, int slot) {
        Log.i(TAG, "handleStubActivity: slot=" + slot + " pkg=" + record.packageName);

        String realClassName = realIntent.getComponent() != null ?
            realIntent.getComponent().getClassName() : targetInfo.name;

        Log.i(TAG, "Real class: " + realClassName);

        try {
            ClassLoader targetCL = VClassLoader.getTargetClassLoader();
            if (targetCL == null) {
                Log.e(TAG, "No target ClassLoader");
                stub.finish();
                return;
            }

            // Load real activity class
            Class<?> realClass = targetCL.loadClass(realClassName);
            Log.i(TAG, "Loaded class: " + realClass.getName());

            // Get ActivityThread
            Class<?> atClass = Class.forName("android.app.ActivityThread");
            Method currentAT = atClass.getMethod("currentActivityThread");
            Object activityThread = currentAT.invoke(null);

            // Get mActivities map
            Field activitiesField = atClass.getDeclaredField("mActivities");
            activitiesField.setAccessible(true);
            Object activitiesMap = activitiesField.get(activityThread);

            // Find ActivityClientRecord via token
            Method getTokenMethod = Activity.class.getMethod("getActivityToken");
            IBinder token = (IBinder) getTokenMethod.invoke(stub);

            Method mapGet = activitiesMap.getClass().getMethod("get", Object.class);
            Object acr = mapGet.invoke(activitiesMap, token);

            if (acr == null) {
                Log.e(TAG, "No ACR for token");
                stub.finish();
                return;
            }

            // Replace activity in ACR
            Field activityField = acr.getClass().getDeclaredField("activity");
            activityField.setAccessible(true);

            // Get Instrumentation
            Field instrField = atClass.getDeclaredField("mInstrumentation");
            instrField.setAccessible(true);
            Instrumentation instr = (Instrumentation) instrField.get(activityThread);

            // Create real activity
            Activity realActivity = instr.newActivity(targetCL, realClassName, realIntent);
            if (realActivity == null) {
                Log.e(TAG, "newActivity returned null");
                stub.finish();
                return;
            }

            // Put real activity into ACR
            activityField.set(acr, realActivity);

            // Call lifecycle via reflection
            Method onCreate = Activity.class.getDeclaredMethod("onCreate", Bundle.class);
            onCreate.setAccessible(true);
            onCreate.invoke(realActivity, (Bundle) null);

            Method onStart = Activity.class.getDeclaredMethod("onStart");
            onStart.setAccessible(true);
            onStart.invoke(realActivity);

            Method onResume = Activity.class.getDeclaredMethod("onResume");
            onResume.setAccessible(true);
            onResume.invoke(realActivity);

            Log.i(TAG, "SUCCESS: " + realClassName + " launched in sandbox");

        } catch (Exception e) {
            Log.e(TAG, "FAILED: " + e);
            e.printStackTrace();
            stub.finish();
        }
    }

    public void ensureHooked() {
        if (mHooked) return;
        try {
            Class<?> atClass = Class.forName("android.app.ActivityThread");
            Method currentAT = atClass.getMethod("currentActivityThread");
            Object activityThread = currentAT.invoke(null);

            Field instrField = atClass.getDeclaredField("mInstrumentation");
            instrField.setAccessible(true);
            Instrumentation original = (Instrumentation) instrField.get(activityThread);

            VInstrumentationWrapper wrapper = new VInstrumentationWrapper(original);
            instrField.set(activityThread, wrapper);

            mHooked = true;
            Log.i(TAG, "Instrumentation hooked");
        } catch (Exception e) {
            Log.e(TAG, "Hook failed: " + e);
        }
    }
}
