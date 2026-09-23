package com.display.utils.engine;
import android.app.Activity;
import android.os.Bundle;
public class StubP0 extends Activity {
    @Override protected void onCreate(Bundle s) {
        android.util.Log.i("STUB", "StubP0.onCreate ENTERED slot=" + (s != null ? "hasBundle" : "noBundle"));
        try {
            int slot = getIntent().getIntExtra("_vs", -1);
            android.util.Log.i("STUB", "StubP0 slot=" + slot + " intent=" + getIntent());
        } catch (Exception e) {
            android.util.Log.e("STUB", "StubP0 intent read fail: " + e);
        }
        super.onCreate(s);
        VActivityManager.get().onStubCreate(this, s);
    }
    @Override protected void onDestroy() { super.onDestroy(); VActivityManager.get().onStubDestroy(this); }
}
