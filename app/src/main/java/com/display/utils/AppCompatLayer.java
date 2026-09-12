package com.display.utils;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.FrameLayout;

public class AppCompatLayer extends Activity {

    private DisplaySurface renderSurface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        FrameLayout layout = new FrameLayout(this);
        renderSurface = new DisplaySurface(this);
        layout.addView(renderSurface, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ));
        setContentView(layout);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (renderSurface != null) {
            renderSurface.setEspEnabled(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (renderSurface != null) {
            renderSurface.setEspEnabled(true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        renderSurface = null;
    }
}
