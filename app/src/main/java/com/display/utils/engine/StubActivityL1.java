package com.display.utils.engine;
import android.app.Activity;
import android.os.Bundle;
public class StubActivityL1 extends Activity {
    @Override protected void onCreate(Bundle s) { super.onCreate(s); VActivityManager.get().onStubCreate(this, s); }
    @Override protected void onDestroy() { super.onDestroy(); VActivityManager.get().onStubDestroy(this); }
}
