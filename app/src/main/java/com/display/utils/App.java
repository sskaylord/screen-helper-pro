package com.display.utils;

import android.annotation.SuppressLint;
import android.app.Application;
import android.util.Log;
import com.display.utils.engine.VCore;
import java.lang.reflect.Method;

public class App extends Application {

    static { bypassHiddenApi(); }

    @SuppressLint("PrivateApi")
    private static void bypassHiddenApi() {
        try {
            Class<?> vmRuntime = Class.forName("dalvik.system.VMRuntime");
            Method getRuntime = vmRuntime.getDeclaredMethod("getRuntime");
            getRuntime.setAccessible(true);
            Object runtime = getRuntime.invoke(null);
            Method setExemptions = vmRuntime.getDeclaredMethod(
                "setHiddenApiExemptions", String[].class);
            setExemptions.setAccessible(true);
            setExemptions.invoke(runtime, new Object[]{new String[]{"L"}});
            Log.i("AzureHub", "Hidden API bypass: OK");
        } catch (Exception e) {
            Log.e("AzureHub", "Hidden API bypass FAIL: " + e);
        }
    }

    @Override public void onCreate() {
        super.onCreate();
        try { VCore.get().init(this); } catch (Exception e) { Log.e("APP", "" + e); }
    }
}
