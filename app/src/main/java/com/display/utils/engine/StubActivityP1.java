package com.display.utils.engine;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
public class StubActivityP1 extends Activity {
    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        Log.i("STUB", "StubActivityP1 onCreate");
        VInstrumentation.get().ensureHooked();
        VActivityManager.get().onStubCreate(this, s);
    }
    @Override protected void onDestroy() { super.onDestroy(); VActivityManager.get().onStubDestroy(this); }
}
