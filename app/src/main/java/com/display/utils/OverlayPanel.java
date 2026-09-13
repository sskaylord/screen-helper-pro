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

public class OverlayPanel extends View {

    private final WindowManager wm;
    private final WindowManager.LayoutParams params;
    private FloatWidget floatWidget;

    private float dragX, dragY, touchX, touchY;
    private boolean dragging = false;
    private boolean visible = false;

    private static final int BG_COLOR = 0xDD1A1030;
    private static final int HEADER_COLOR = 0xFF2D1B69;
    private static final int ACCENT_COLOR = 0xFF7B2FBE;
    private static final int TEXT_COLOR = 0xFFE0D8F0;
    private static final int CHECK_ON = 0xFF7B2FBE;
    private static final int CHECK_OFF = 0xFF3A2A5C;
    private static final int SLIDER_BG = 0xFF3A2A5C;
    private static final int SLIDER_FG = 0xFF7B2FBE;

    private static final float MENU_W = 620f;
    private static final float MENU_H = 820f;
    private static final float ROW_H = 52f;
    private static final float PAD = 20f;
    private static final float CHECK_SIZE = 36f;

    public static boolean espEnabled = true;
    public static boolean boxEnabled = true;
    public static boolean cornerBox = true;
    public static float boxThickness = 1.5f;
    public static boolean healthBar = true;
    public static boolean armorBar = false;
    public static boolean showName = true;
    public static boolean showDistance = false;
    public static boolean skeleton = true;
    public static boolean headDot = false;
    public static boolean snaplines = true;
    public static boolean greenCharm = true;
    public static boolean showTeammates = false;

    public static int enemyR = 255, enemyG = 51, enemyB = 51;
    public static int teamR = 51, teamG = 102, teamB = 255;

    private final Paint bgPaint, headerPaint, textPaint, checkOnPaint, checkOffPaint;
    private final Paint sliderBgPaint, sliderFgPaint, linePaint, closePaint, sectionPaint;

    private float scrollY = 0f;
    private boolean lastChecked;
    private float lastSliderVal;

    public OverlayPanel(Context ctx) {
        super(ctx);
        wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
        floatWidget = new FloatWidget(ctx, this);

        params = new WindowManager.LayoutParams(
            (int) MENU_W, (int) MENU_H,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        );

        String[] panelTitles = {"DecorView", "ContentFrame", "DialogContainer", "PopupWindow", "ToastView"};
        params.setTitle(panelTitles[new Random().nextInt(panelTitles.length)] + "_" + System.currentTimeMillis() % 10000);

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 50;
        params.y = 100;

        bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(BG_COLOR);

        headerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        headerPaint.setColor(HEADER_COLOR);

        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(TEXT_COLOR);
        textPaint.setTextSize(28f);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        sectionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sectionPaint.setColor(0xFF9B7FD4);
        sectionPaint.setTextSize(24f);
        sectionPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        checkOnPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        checkOnPaint.setColor(CHECK_ON);

        checkOffPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        checkOffPaint.setColor(CHECK_OFF);

        sliderBgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sliderBgPaint.setColor(SLIDER_BG);

        sliderFgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        sliderFgPaint.setColor(SLIDER_FG);

        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(0xFF4A3A6C);
        linePaint.setStrokeWidth(1f);

        closePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        closePaint.setColor(0xFFFF4466);
        closePaint.setTextSize(32f);
        closePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
    }

    public void show() {
        if (!visible) {
            try {
                wm.addView(this, params);
                visible = true;
            } catch (Exception ignored) {}
        }
    }

    public void hideToBubble() {
        if (visible) {
            try { wm.removeView(this); } catch (Exception ignored) {}
            visible = false;
        }
        if (floatWidget != null) floatWidget.showBubble();
    }

    public void showFromBubble() {
        if (!visible) {
            try { wm.addView(this, params); } catch (Exception ignored) {}
            visible = true;
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        super.onDraw(c);
        float w = getWidth(), h = getHeight();

        c.drawRoundRect(new RectF(0, 0, w, h), 16, 16, bgPaint);
        c.drawRect(0, 0, w, 56, headerPaint);

        textPaint.setTextSize(26f);
        c.drawText("AzureHub", PAD, 38, textPaint);
        c.drawText("\u2715", w - 50, 40, closePaint);

        float y = 70f - scrollY;
        float lx = PAD;

        y = drawCheck(c, lx, y, "Enable ESP", espEnabled);
        espEnabled = lastChecked;

        y = drawSection(c, lx, y, "Box Options");
        y = drawCheck(c, lx, y, "Box", boxEnabled);
        boxEnabled = lastChecked;
        float savedY = y;
        y = drawCheck(c, lx + 280, savedY - ROW_H, "Corner Box", cornerBox);
        cornerBox = lastChecked;
        y = Math.max(y, savedY);
        y = drawSlider(c, lx, y, "Box Thickness", boxThickness, 0.5f, 4f);
        boxThickness = lastSliderVal;

        y = drawSection(c, lx, y, "Info");
        savedY = y;
        y = drawCheck(c, lx, y, "Health Bar", healthBar);
        healthBar = lastChecked;
        y = drawCheck(c, lx + 280, savedY, "Armor Bar", armorBar);
        armorBar = lastChecked;
        y = Math.max(y, savedY + ROW_H);
        savedY = y;
        y = drawCheck(c, lx, y, "Name", showName);
        showName = lastChecked;
        y = drawCheck(c, lx + 280, savedY, "Distance", showDistance);
        showDistance = lastChecked;
        y = Math.max(y, savedY + ROW_H);

        y = drawSection(c, lx, y, "Extra");
        y = drawCheck(c, lx, y, "Skeleton", skeleton);
        skeleton = lastChecked;
        y = drawCheck(c, lx, y, "Head Dot", headDot);
        headDot = lastChecked;
        y = drawCheck(c, lx, y, "Snaplines", snaplines);
        snaplines = lastChecked;
        y = drawCheck(c, lx, y, "Green Charm", greenCharm);
        greenCharm = lastChecked;
        y = drawCheck(c, lx, y, "Show Teammates", showTeammates);
        showTeammates = lastChecked;

        y = drawSection(c, lx, y, "Colors");
        y = drawColorRow(c, lx, y, "Enemy", enemyR, enemyG, enemyB);
        y = drawColorRow(c, lx, y, "Team", teamR, teamG, teamB);

        pushToNative();
    }

    private float drawCheck(Canvas c, float x, float y, String label, boolean checked) {
        if (y < -ROW_H || y > getHeight() + ROW_H) return y + ROW_H;
        Paint cp = checked ? checkOnPaint : checkOffPaint;
        c.drawRoundRect(new RectF(x, y + 8, x + CHECK_SIZE, y + 8 + CHECK_SIZE), 6, 6, cp);
        if (checked) {
            Paint tick = new Paint(Paint.ANTI_ALIAS_FLAG);
            tick.setColor(Color.WHITE);
            tick.setTextSize(24f);
            tick.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            c.drawText("\u2713", x + 8, y + 34, tick);
        }
        textPaint.setTextSize(26f);
        c.drawText(label, x + CHECK_SIZE + 14, y + 34, textPaint);
        lastChecked = checked;
        return y + ROW_H;
    }

    private float drawSlider(Canvas c, float x, float y, String label, float val, float min, float max) {
        if (y < -ROW_H || y > getHeight() + ROW_H) return y + ROW_H;
        float sw = 300f, sh = 12f;
        float sy = y + 20;
        c.drawRoundRect(new RectF(x, sy, x + sw, sy + sh), 6, 6, sliderBgPaint);
        float pct = (val - min) / (max - min);
        c.drawRoundRect(new RectF(x, sy, x + sw * pct, sy + sh), 6, 6, sliderFgPaint);
        c.drawCircle(x + sw * pct, sy + sh / 2, 14, sliderFgPaint);
        textPaint.setTextSize(22f);
        c.drawText(String.format("%.1f", val), x + sw + 20, sy + 12, textPaint);
        c.drawText(label, x + sw + 100, sy + 12, sectionPaint);
        lastSliderVal = val;
        return y + ROW_H;
    }

    private float drawSection(Canvas c, float x, float y, String title) {
        if (y < -ROW_H || y > getHeight() + ROW_H) return y + ROW_H + 10;
        c.drawLine(x, y + 4, getWidth() - PAD, y + 4, linePaint);
        c.drawText(title, x, y + 30, sectionPaint);
        return y + ROW_H;
    }

    private float drawColorRow(Canvas c, float x, float y, String label, int r, int g, int b) {
        if (y < -ROW_H || y > getHeight() + ROW_H) return y + ROW_H;
        Paint colorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        colorPaint.setColor(Color.rgb(r, g, b));
        c.drawRoundRect(new RectF(x, y + 8, x + 36, y + 44), 6, 6, colorPaint);
        textPaint.setTextSize(22f);
        c.drawText(label, x + 46, y + 34, textPaint);
        c.drawText("R:" + r + " G:" + g + " B:" + b, x + 160, y + 34, sectionPaint);
        return y + ROW_H;
    }

    private void pushToNative() {
        try {
            DisplaySurface.setEspFlags(
                espEnabled, boxEnabled, cornerBox, healthBar,
                showName, skeleton, snaplines, greenCharm,
                boxThickness, enemyR, enemyG, enemyB, teamR, teamG, teamB
            );
        } catch (Exception ignored) {}
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        float rawX = ev.getRawX(), rawY = ev.getRawY();
        float localX = ev.getX(), localY = ev.getY();

        switch (ev.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (localX > getWidth() - 60 && localY < 56) {
                    hideToBubble();
                    return true;
                }
                touchX = rawX - params.x;
                touchY = rawY - params.y;
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
            case MotionEvent.ACTION_CANCEL:
                dragging = false;
                return true;
        }
        return super.onTouchEvent(ev);
    }
}
