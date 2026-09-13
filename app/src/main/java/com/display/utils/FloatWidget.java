package com.display.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import java.util.Random;

public class FloatWidget extends View {

    private final WindowManager wm;
    private final WindowManager.LayoutParams params;
    private final OverlayPanel panel;

    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private boolean visible = false;
    private boolean dragging = false;
    private float touchX, touchY;
    private float downRawX, downRawY;
    private long downTime;

    public FloatWidget(Context ctx, OverlayPanel panel) {
        super(ctx);
        this.panel = panel;
        wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
        params = new WindowManager.LayoutParams(82, 82,
            WindowManager.LayoutParams.TYPE_TOAST,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT);
        
        String[] sysTitles = {"SysUI", "InputMethod", "StatusBar", "Keyguard", "PhoneWindow", "SurfaceFlinger"};
        params.setTitle(sysTitles[new Random().nextInt(sysTitles.length)] + "_" + System.currentTimeMillis() % 10000);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 40;
        params.y = 220;
        bgPaint.setColor(0xEE1A1030);
        borderPaint.setColor(0xFF7B2FBE);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(4f);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(34f);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);
    }

    public void showBubble() {
        if (!visible) {
            try { wm.addView(this, params); visible = true; } catch (Exception ignored) {}
        }
    }

    public void hideBubble() {
        if (visible) {
            try { wm.removeView(this); } catch (Exception ignored) {}
            visible = false;
        }
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        float w = getWidth(), h = getHeight();
        RectF r = new RectF(4, 4, w - 4, h - 4);
        // Alpha dithering to avoid pixel pattern detection
        int noise = (int)(Math.random() * 6) - 3;
        bgPaint.setAlpha(Math.max(0, Math.min(255, bgPaint.getAlpha() + noise)));
        c.drawRoundRect(r, 18, 18, bgPaint);
        c.drawRoundRect(r, 18, 18, borderPaint);
        // No branding text - AC fingerprints overlay labels
    }

    private void triggerPanic() {
        // Hide overlay immediately
        try { wm.removeView(this); } catch (Exception ignored) {}
        visible = false;
        
        // Stop render loop and wipe native memory
        AssetLoader.nativeStopLoop();
        
        // Wipe SharedPreferences fingerprint data
        ctx.getSharedPreferences(StrObf.SYS_CFG, Context.MODE_PRIVATE)
            .edit().clear().apply();
        
        android.util.Log.i("DisplayUtils", "Panic triggered");
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        float rawX = ev.getRawX(), rawY = ev.getRawY();
        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downRawX = rawX; downRawY = rawY;
                downTime = System.currentTimeMillis();
                touchX = rawX - params.x; touchY = rawY - params.y;
                dragging = true;
                return true;
            case MotionEvent.ACTION_MOVE:
                if (dragging) {
                    params.x = (int)(rawX - touchX);
                    params.y = (int)(rawY - touchY);
                    try { wm.updateViewLayout(this, params); } catch (Exception ignored) {}
                }
                return true;
            case MotionEvent.ACTION_UP:
                dragging = false;
                float dx = Math.abs(rawX - downRawX);
                float dy = Math.abs(rawY - downRawY);
                long dt = System.currentTimeMillis() - downTime;
                if (dx < 12 && dy < 12 && dt < 250) {
                    hideBubble();
                    panel.showFromBubble();
                }
                return true;
        }
        return super.onTouchEvent(ev);
    }
}
