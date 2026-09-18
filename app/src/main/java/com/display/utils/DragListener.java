package com.display.utils;

import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

public class DragListener implements View.OnTouchListener {
    private final WindowManager wm;
    private final View v;
    private final WindowManager.LayoutParams p;
    private float ix, iy, tx, ty;

    public DragListener(WindowManager wm, View v, WindowManager.LayoutParams p) {
        this.wm = wm; this.v = v; this.p = p;
    }

    @Override public boolean onTouch(View view, MotionEvent e) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN: ix=p.x; iy=p.y; tx=e.getRawX(); ty=e.getRawY(); return false;
            case MotionEvent.ACTION_MOVE: p.x=(int)(ix+e.getRawX()-tx); p.y=(int)(iy+e.getRawY()-ty);
                wm.updateViewLayout(v, p); return true;
        }
        return false;
    }
}
