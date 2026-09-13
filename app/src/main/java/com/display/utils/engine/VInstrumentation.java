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
import android.view.Window;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class VInstrumentation {
    private static final String TAG = "VI";
    private static VInstrumentation sInstance;
    private Instrumentation mOriginal;
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

        Log.i(TAG, "Launching real: " + realClassName + " in stub[" + slot + "]");

        try {
            // Load the real activity class from target APK
            ClassLoader targetCL = VClassLoader.getTargetClassLoader();
            if (targetCL == null) {
                Log.e(TAG, "No target ClassLoader");
                stub.finish();
                return;
            }

            Class<?> realActivityClass = targetCL.loadClass(realClassName);

            // Get ActivityThread
            Class<?> atClass = Class.forName("android.app.ActivityThread");
            Method currentAT = atClass.getMethod("currentActivityThread");
            Object activityThread = currentAT.invoke(null);

            // Get mActivities map
            Field activitiesField = atClass.getDeclaredField("mActivities");
            activitiesField.setAccessible(true);
            Object activitiesMap = activitiesField.get(activityThread);

            // Find the ActivityClientRecord for this stub
            IBinder token = stub.getActivityToken();
            Method getMethod = activitiesMap.getClass().getMethod("get", Object.class);
            Object acr = getMethod.invoke(activitiesMap, token);

            if (acr == null) {
                Log.e(TAG, "No ActivityClientRecord found");
                stub.finish();
                return;
            }

            // Replace activity info in the record
            Class<?> acrClass = acr.getClass();

            // Set the real activity class
            Field activityField = acrClass.getDeclaredField("activity");
            activityField.setAccessible(true);

            // Create real activity instance using Instrumentation
            Instrumentation instr = getInstrumentation(activityThread);
            if (instr == null) {
                Log.e(TAG, "No Instrumentation");
                stub.finish();
                return;
            }

            // Use newActivity to properly create the real activity
            Application app = getApplication(activityThread);
            if (app == null) {
                // Create minimal application
                app = stub.getApplication();
            }

            Activity realActivity = instr.newActivity(
                realActivityClass,
                stub.getBaseContext(),
                token,
                app,
                realIntent,
                targetInfo,
                stub.getTitle(),
                stub,
                null
            );

            if (realActivity == null) {
                Log.e(TAG, "newActivity returned null");
                stub.finish();
                return;
            }

            // Replace stub with real activity in the record
            activityField.set(acr, realActivity);

            // Also replace in Activity's internal token mapping
            // Call performResume-like setup
            realActivity.setIntent(realIntent);

            // Trigger onCreate on real activity
            realActivity.onCreate(null);
            realActivity.onStart();
            realActivity.onResume();

            // Set content view from real activity to stub's window
            // The real activity now owns the window
            Field windowField = Activity.class.getDeclaredField("mWindow");
            windowField.setAccessible(true);
            Window realWindow = (Window) windowField.get(realActivity);
            Window stubWindow = (Window) windowField.get(stub);

            // Transfer window callback
            if (realWindow != null && stubWindow != null) {
                stubWindow.setCallback(realWindow.getCallback());
            }

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
            mOriginal = (Instrumentation) instrField.get(activityThread);

            VInstrumentationWrapper wrapper = new VInstrumentationWrapper(mOriginal);
            instrField.set(activityThread, wrapper);

            mHooked = true;
            Log.i(TAG, "Instrumentation hooked");
        } catch (Exception e) {
            Log.e(TAG, "Instrumentation hook failed: " + e);
        }
    }

    private Instrumentation getInstrumentation(Object activityThread) {
        try {
            Class<?> atClass = Class.forName("android.app.ActivityThread");
            Field f = atClass.getDeclaredField("mInstrumentation");
            f.setAccessible(true);
            return (Instrumentation) f.get(activityThread);
        } catch (Exception e) { return null; }
    }

    private Application getApplication(Object activityThread) {
        try {
            Class<?> atClass = Class.forName("android.app.ActivityThread");
            Field f = atClass.getDeclaredField("mInitialApplication");
            f.setAccessible(true);
            return (Application) f.get(activityThread);
        } catch (Exception e) { return null; }
    }
}
