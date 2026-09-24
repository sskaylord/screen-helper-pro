package com.display.utils.engine;

import android.app.Activity;
import android.content.Context;
import java.util.List;

public class VCore {
    private static VCore s;
    private Context ctx;
    private boolean ready;
    private VCore() {}
    public static VCore get() { if (s == null) s = new VCore(); return s; }

    public void init(Context c) {
        if (ready) return;
        ctx = c.getApplicationContext();
        VActivityManager.get().init(ctx);
        IORedirect.init();
        ready = true;
    }

    public boolean installApp(String pkg) { return VActivityManager.get().installApp(pkg); }

    // Activity context ile launch — background restriction bypass
    public void launchApp(Activity from, String pkg) {
        VActivityManager.get().launchApp(from, pkg);
    }

    // Eski signature fallback (ApplicationContext ile — çalışmayabilir)
    public void launchApp(String pkg) {
        VActivityManager.get().launchApp(null, pkg);
    }

    public boolean isInstalled(String pkg) { return VActivityManager.get().isInstalled(pkg); }
    public List<VActivityManager.SandboxRecord> getApps() { return VActivityManager.get().getInstalledApps(); }
}
