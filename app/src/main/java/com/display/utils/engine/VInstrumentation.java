package com.display.utils.engine;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.os.IBinder;
import android.util.Log;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

public class VInstrumentation {
    private static final String TAG = "VI";
    private static VInstrumentation s;
    private boolean hooked;
    private VInstrumentation() {}
    public static VInstrumentation get() { if (s == null) s = new VInstrumentation(); return s; }

    public void launch(Activity stub, Intent realIntent, ActivityInfo ti,
                       VActivityManager.SandboxRecord rec, int slot) {
        ensureHooked();
        String cls = realIntent.getComponent() != null ?
            realIntent.getComponent().getClassName() : ti.name;
        Log.i(TAG, "Launching " + cls + " in stub[" + slot + "]");

        try {
            ClassLoader tcl = VClassLoader.getCL();
            if (tcl == null) { Log.e(TAG, "No CL"); stub.finish(); return; }

            // ActivityThread al
            Class<?> atClass = Class.forName("android.app.ActivityThread");
            Method curAT = atClass.getDeclaredMethod("currentActivityThread");
            curAT.setAccessible(true);
            Object at = curAT.invoke(null);

            // mActivities map
            Field mActField = atClass.getDeclaredField("mActivities");
            mActField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<IBinder, Object> mActivities = (Map<IBinder, Object>) mActField.get(at);

            // Stub token al (hidden field)
            Field tokenField = Activity.class.getDeclaredField("mToken");
            tokenField.setAccessible(true);
            IBinder token = (IBinder) tokenField.get(stub);

            Object acr = mActivities.get(token);
            if (acr == null) {
                Log.e(TAG, "ACR not found for token");
                stub.finish();
                return;
            }

            Class<?> acrClass = acr.getClass();

            // Intent override
            Field intentField = acrClass.getDeclaredField("intent");
            intentField.setAccessible(true);
            intentField.set(acr, realIntent);

            // ActivityInfo override
            Field aiField = acrClass.getDeclaredField("activityInfo");
            aiField.setAccessible(true);
            ActivityInfo ai = new ActivityInfo();
            ai.name = cls;
            ai.packageName = rec.packageName;
            ai.applicationInfo = new ApplicationInfo(rec.appInfo);
            ai.applicationInfo.dataDir = rec.dataDir;
            ai.applicationInfo.nativeLibraryDir = rec.libDir;
            aiField.set(acr, ai);

            // Instrumentation zaten VIW ile replace edildi (ensureHooked)
            // Sistem şimdi performLaunchActivity çağırdığında
            // VIW.newActivity() target CL'den yükleyecek

            Log.i(TAG, "ACR patched OK: " + cls);

        } catch (Exception e) {
            Log.e(TAG, "FAIL: " + e);
            e.printStackTrace();
            stub.finish();
        }
    }

    public void ensureHookedPublic() {
        if (hooked) return;
        try {
            Class<?> atClass = Class.forName("android.app.ActivityThread");
            Method curAT = atClass.getDeclaredMethod("currentActivityThread");
            curAT.setAccessible(true);
            Object at = curAT.invoke(null);

            Field instrField = atClass.getDeclaredField("mInstrumentation");
            instrField.setAccessible(true);
            Instrumentation orig = (Instrumentation) instrField.get(at);

            if (!(orig instanceof VIW)) {
                instrField.set(at, new VIW(orig));
            }
            hooked = true;
            Log.i(TAG, "Instrumentation hooked with VIW");
        } catch (Exception e) {
            Log.e(TAG, "Hook fail: " + e);
        }
    }
}
