package com.display.utils;

import android.app.Application;
import android.util.Log;
import com.display.utils.engine.VCore;

public class App extends Application {
    @Override public void onCreate() {
        super.onCreate();
        try { VCore.get().init(this); } catch (Exception e) { Log.e("APP", "" + e); }
    }
}
