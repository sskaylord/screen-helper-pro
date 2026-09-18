package com.display.utils.engine;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.util.Log;

public class VIW extends Instrumentation {
    private static final String TAG = "VIW";
    private final Instrumentation base;

    public VIW(Instrumentation b) { base = b; }

    @Override
    public Activity newActivity(ClassLoader cl, String cn, Intent intent)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        ClassLoader tcl = VClassLoader.getCL();
        if (tcl != null) {
            try {
                Class<?> c = tcl.loadClass(cn);
                if (Activity.class.isAssignableFrom(c)) {
                    Log.i(TAG, "VCL hit: " + cn);
                    return (Activity) c.newInstance();
                }
            } catch (ClassNotFoundException ignored) {}
        }
        return base.newActivity(cl, cn, intent);
    }
}
