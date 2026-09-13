package com.display.utils;

import android.content.Context;
import android.graphics.PixelFormat;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class DisplaySurface extends SurfaceView implements SurfaceHolder.Callback {

    private long nativePtr;
    private volatile boolean espEnabled = true;
    private Thread renderThread;

    static {
        System.loadLibrary(StrObf.DISPLAY_UTILS_LIB);
    }

    public DisplaySurface(Context ctx) {
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
                if (espEnabled) nativeDrawFrame(nativePtr);
                try { Thread.sleep(16); } catch (InterruptedException e) { break; }
            }
        }, "DisplayLoop");
        renderThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int fmt, int w, int h) {
        if (nativePtr != 0) nativeOnResize(nativePtr, w, h);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (renderThread != null) {
            renderThread.interrupt();
            try { renderThread.join(500); } catch (InterruptedException ignored) {}
        }
        if (nativePtr != 0) { nativeDestroy(nativePtr); nativePtr = 0; }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        return super.onTouchEvent(ev);
    }

    public boolean isEspEnabled() { return espEnabled; }
    public void setEspEnabled(boolean v) { this.espEnabled = v; }

    private native long nativeInit(android.view.Surface surface);
    private native void nativeDrawFrame(long ptr);
    private native void nativeOnResize(long ptr, int w, int h);
    private native void nativeDestroy(long ptr);
    private static native boolean isStealth();
    public static native void setEspFlags(boolean esp, boolean box, boolean corner, boolean hp, boolean name, boolean skel, boolean snap, boolean charm, float thick, int eR, int eG, int eB, int tR, int tG, int tB);
}
