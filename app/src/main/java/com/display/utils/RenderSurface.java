package com.display.utils;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.FrameLayout;

public class RenderSurface extends Activity {

    private DisplaySurface displaySurface;
    private OverlayPanel displayPanel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        FrameLayout layout = new FrameLayout(this);
        displaySurface = new DisplaySurface(this);
        layout.addView(displaySurface, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ));
        setContentView(layout);

        displayPanel = new OverlayPanel(this);
        displayPanel.show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (displaySurface != null) {
            displaySurface.setRenderActive(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (displaySurface != null) {
            displaySurface.setRenderActive(true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        displaySurface = null;
        displayPanel = null;
    }
}
