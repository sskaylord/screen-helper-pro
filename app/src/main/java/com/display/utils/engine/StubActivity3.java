package com.display.utils.engine;
import android.app.Activity;
import android.os.Bundle;
public class StubActivity3 extends Activity {
    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        VActivityManager.get().onStubCreate(this, getIntent());
    }
    @Override protected void onResume() { super.onResume(); VActivityManager.get().onStubResume(this); }
    @Override protected void onPause() { super.onPause(); VActivityManager.get().onStubPause(this); }
    @Override protected void onDestroy() { super.onDestroy(); VActivityManager.get().onStubDestroy(this); }
    @Override public void onBackPressed() { VActivityManager.get().onStubBack(this); }
}
