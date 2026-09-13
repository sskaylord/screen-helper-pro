package com.display.utils.engine;

import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.content.ComponentName;
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
        if (!mHooked) hookInstrumentation();

        String realClassName = realIntent.getComponent() != null ?
            realIntent.getComponent().getClassName() : targetInfo.name;

        Log.i(TAG, "Launching: " + realClassName + " in stub[" + slot + "]");

        try {
            ClassLoader targetCL = VClassLoader.getTargetClassLoader();
            if (targetCL == null) { stub.finish(); return; }

            // Load real activity class
            Class<?> realClass = targetCL.loadClass(realClassName);

            // Get ActivityThread
            Class<?> atClass = Class.forName("android.app.ActivityThread");
            Method currentAT = atClass.getMethod("currentActivityThread");
            Object activityThread = currentAT.invoke(null);

            // Get mActivities
            Field activitiesField = atClass.getDeclaredField("mActivities");
            activitiesField.setAccessible(true);
            Object activitiesMap = activitiesField.get(activityThread);

            // Find ActivityClientRecord for this stub
            java.lang.reflect.Method getTokenMethod = Activity.class.getMethod("getActivityToken");
            IBinder token = (IBinder) getTokenMethod.invoke(stub);
            Method mapGet = activitiesMap.getClass().getMethod("get", Object.class);
            Object acr = mapGet.invoke(activitiesMap, token);

            if (acr == null) { stub.finish(); return; }

            // Replace the activity in the record using reflection
            Class<?> acrClass = acr.getClass();
            Field activityField = acrClass.getDeclaredField("activity");
            activityField.setAccessible(true);

            // Get Instrumentation from ActivityThread
            Field instrField = atClass.getDeclaredField("mInstrumentation");
            instrField.setAccessible(true);
            Instrumentation instr = (Instrumentation) instrField.get(activityThread);

            // Get Application
            Field appField = atClass.getDeclaredField("mInitialApplication");
            appField.setAccessible(true);
            Application app = (Application) appField.get(activityThread);
            if (app == null) app = stub.getApplication();

            // Create real activity via Instrumentation.newActivity(ClassLoader, String, Intent)
            Activity realActivity = instr.newActivity(targetCL, realClassName, realIntent);

            if (realActivity == null) { stub.finish(); return; }

            
                                    activityField.set(acr, realActivity);

            // Use reflection to call protected lifecycle methods
            Method onCreateMethod = Activity.class.getDeclaredMethod("onCreate", Bundle.class);
            onCreateMethod.setAccessible(true);
            onCreateMethod.invoke(realActivity, (Bundle) null);

            Method onStartMethod = Activity.class.getDeclaredMethod("onStart");
            onStartMethod.setAccessible(true);
            onStartMethod.invoke(realActivity);

            Method onResumeMethod = Activity.class.getDeclaredMethod("onResume");
            onResumeMethod.setAccessible(true);
            onResumeMethod.invoke(realActivity);

            Log.i(TAG, "Real activity launched: " + realClassName);

        } catch (Exception e) {
            Log.e(TAG, "Launch failed: " + e);
            e.printStackTrace();
            stub.finish();
        }
    }

    private void hookInstrumentation() {
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
            Log.e(TAG, "Instrumentation hook failed: " + e);
        }
    }
}
