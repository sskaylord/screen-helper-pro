package com.renderkit.support;

import android.content.Context;
import android.graphics.PixelFormat;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class RenderSurface extends SurfaceView implements SurfaceHolder.Callback {

    private long nativePtr;
    private volatile boolean espEnabled = true;
    private volatile boolean showMenu = false;
    private float menuX = 50f, menuY = 100f;
    private float dragOffX = 0f, dragOffY = 0f;
    private boolean dragging = false;
    private Thread renderThread;

    public static boolean espBox = true;
    public static boolean espSkeleton = true;
    public static boolean espGlow = true;
    public static boolean espHealth = true;
    public static boolean espName = true;

    static {
        System.loadLibrary("render_support");
    }

    public RenderSurface(Context ctx) {
        super(ctx);
        getHolder().addCallback(this);
        setZOrderOnTop(true);
        getHolder().setFormat(PixelFormat.TRANSLUCENT);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        nativePtr = nativeInit(holder.getSurface());
        renderThread = new Thread(() -> {
            while (!Thread.interrupted()) {
                if (espEnabled) {
                    nativeDrawFrame(nativePtr);
                }
                try {
                    Thread.sleep(16);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "RenderKit-Loop");
        renderThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int fmt, int w, int h) {
        if (nativePtr != 0) {
            nativeOnResize(nativePtr, w, h);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (renderThread != null) {
            renderThread.interrupt();
            try { renderThread.join(500); } catch (InterruptedException ignored) {}
        }
        if (nativePtr != 0) {
            nativeDestroy(nativePtr);
            nativePtr = 0;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        float x = ev.getX(), y = ev.getY();
        float mw = 220f;
        float btnH = 44f;

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (x > menuX && x < menuX + 160 &&
                    y > menuY && y < menuY + btnH) {
                    showMenu = !showMenu;
                    return true;
                }
                if (showMenu &&
                    x > menuX && x < menuX + mw &&
                    y > menuY + btnH + 8 && y < menuY + btnH + 8 + btnH) {
                    espEnabled = !espEnabled;
                    return true;
                }
                if (showMenu &&
                    x > menuX && x < menuX + mw &&
                    y > menuY && y < menuY + btnH) {
                    dragging = true;
                    dragOffX = x - menuX;
                    dragOffY = y - menuY;
                    return true;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (dragging) {
                    menuX = x - dragOffX;
                    menuY = y - dragOffY;
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                dragging = false;
                break;
        }
        return super.onTouchEvent(ev);
    }

    public boolean isEspEnabled() { return espEnabled; }
    public void setEspEnabled(boolean v) { this.espEnabled = v; }

    private native long nativeInit(android.view.Surface surface);
    private native void nativeDrawFrame(long ptr);
    private native void nativeOnResize(long ptr, int w, int h);
    private native void nativeDestroy(long ptr);
}
