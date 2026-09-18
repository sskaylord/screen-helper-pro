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

        // STUB INTERCEPT — beyaz ekranın asıl fix'i
        if (cn != null && cn.startsWith("com.display.utils.engine.Stub")) {
            int slot = (intent != null) ? intent.getIntExtra("_vs", -1) : -1;
            if (slot >= 0) {
                String realCls = VActivityManager.get().getRealClass(slot);
                if (realCls != null) {
                    ClassLoader tcl = VClassLoader.getCL();
                    if (tcl != null) {
                        try {
                            Class<?> c = tcl.loadClass(realCls);
                            if (Activity.class.isAssignableFrom(c)) {
                                Log.i(TAG, "STUB→REAL: " + cn + " → " + realCls);
                                return (Activity) c.newInstance();
                            }
                        } catch (ClassNotFoundException e) {
                            Log.e(TAG, "Real class not in VCL: " + realCls);
                        }
                    }
                }
            }
        }

        // Normal target class
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
