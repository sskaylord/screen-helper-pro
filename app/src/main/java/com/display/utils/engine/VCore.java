package com.display.utils.engine;

import android.app.Application;
import android.content.Context;
import android.util.Log;

public class VCore {
    private static final String TAG = "VCore";
    private static VCore sInstance;
    private Context mContext;
    private boolean mInitialized;

    private VCore() {}

    public static VCore get() {
        if (sInstance == null) sInstance = new VCore();
        return sInstance;
    }

    public void init(Context ctx) {
        if (mInitialized) return;
        mContext = ctx.getApplicationContext();
        VActivityManager.get().init(mContext);
        VPackageManager.get().init(mContext);
        IORedirect.init();
        mInitialized = true;
        Log.i(TAG, "VCore initialized");
    }

    public boolean isInitialized() { return mInitialized; }
    public Context getContext() { return mContext; }

    public boolean installApp(String pkg) {
        if (!mInitialized) init(mContext);
        return VActivityManager.get().installApp(pkg);
    }

    public void launchApp(String pkg) {
        if (!mInitialized) init(mContext);
        VActivityManager.get().launchApp(pkg);
    }

    public boolean isInstalled(String pkg) {
        return VActivityManager.get().isInstalled(pkg);
    }

    public void uninstallApp(String pkg) {
        VActivityManager.get().uninstallApp(pkg);
    }
}
