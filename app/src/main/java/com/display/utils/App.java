package com.display.utils;

import android.app.Application;
import android.util.Log;
import com.display.utils.engine.VCore;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("APP", "Application onCreate - initializing VCore");
        try {
            VCore.get().init(this);
            Log.i("APP", "VCore initialized in Application");
        } catch (Exception e) {
            Log.e("APP", "VCore init failed: " + e);
        }
    }
}
