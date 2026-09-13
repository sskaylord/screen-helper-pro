package com.display.utils.engine;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;

public class VIW extends Instrumentation {
    private final Instrumentation base;
    public VIW(Instrumentation b) { base = b; }

    @Override public Activity newActivity(ClassLoader cl, String cn, Intent intent)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        ClassLoader tcl = VClassLoader.getCL();
        if (tcl != null) {
            try {
                Class<?> c = tcl.loadClass(cn);
                if (Activity.class.isAssignableFrom(c)) return (Activity) c.newInstance();
            } catch (ClassNotFoundException ignored) {}
        }
        return base.newActivity(cl, cn, intent);
    }
}
