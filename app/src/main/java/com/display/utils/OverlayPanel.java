package com.display.utils;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.*;

public class OverlayPanel {
    private static final int PINK = 0xFFEC4899, BG = 0xCC1A0A1E, TW = 0xFFFCE7F3, DIM = 0xFFD4A0B0;
    private final WindowManager wm;
    private LinearLayout menuView;
    private EspView espView;
    private boolean menuShowing, espShowing;

    public boolean espEnabled=false, skeletonEnabled=false, healthEnabled=false;
    public boolean aimbotEnabled=false, charmEnabled=false, speedEnabled=false;
    public float aimFov=90f;

    public OverlayPanel(Context ctx) {
        wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
        espView = new EspView(ctx, this);
        buildMenu(ctx);
    }

    private void buildMenu(Context ctx) {
        menuView = new LinearLayout(ctx);
        menuView.setOrientation(LinearLayout.VERTICAL);
        menuView.setBackgroundColor(BG);
        menuView.setPadding(28, 24, 28, 24);

        TextView title = new TextView(ctx);
        title.setText("AzureHub \u25B8");
        title.setTextColor(PINK); title.setTextSize(17f);
        title.setTypeface(Typeface.DEFAULT_BOLD); title.setGravity(Gravity.CENTER);
        menuView.addView(title);
        addDiv(ctx);

        addTog(ctx, "ESP Box", v -> { espEnabled=tog(v); refresh(); });
        addTog(ctx, "Skeleton", v -> { skeletonEnabled=tog(v); refresh(); });
        addTog(ctx, "Health Bar", v -> { healthEnabled=tog(v); refresh(); });
        addTog(ctx, "Aimbot", v -> { aimbotEnabled=tog(v); });
        addTog(ctx, "Charm (NoClip)", v -> { charmEnabled=tog(v); });
        addTog(ctx, "Speed Hack", v -> { speedEnabled=tog(v); });

        // FOV
        TextView fovLabel = new TextView(ctx);
        fovLabel.setText("Aim FOV: 90\u00B0"); fovLabel.setTextColor(DIM); fovLabel.setTextSize(13f);
        fovLabel.setPadding(8, 10, 8, 2); menuView.addView(fovLabel);
        SeekBar seek = new SeekBar(ctx); seek.setMax(180); seek.setProgress(90);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) { aimFov=p; fovLabel.setText("Aim FOV: "+p+"\u00B0"); }
            public void onStartTrackingTouch(SeekBar s) {} public void onStopTrackingTouch(SeekBar s) {}
        });
        menuView.addView(seek);
        addDiv(ctx);

        // Dump
        TextView dump = new TextView(ctx);
        dump.setText("\u26A1 AUTO DUMP"); dump.setTextColor(0xFFFFFFFF); dump.setTextSize(14f);
        dump.setTypeface(Typeface.DEFAULT_BOLD); dump.setGravity(Gravity.CENTER); dump.setPadding(24, 14, 24, 14);
        GradientDrawable dbg = new GradientDrawable(); dbg.setColor(PINK); dbg.setCornerRadius(16f); dump.setBackground(dbg);
        dump.setOnClickListener(v -> {
            dump.setText("DUMPING...");
            new Thread(() -> {
                long r = DisplayCore.dumpOffsets();
                new Handler(Looper.getMainLooper()).post(() ->
                    dump.setText(r != 0 ? "\u2713 DUMP OK (0x"+Long.toHexString(r)+")" : "\u2717 DUMP FAIL"));
            }).start();
        });
        menuView.addView(dump);
        addDiv(ctx);

        TextView close = new TextView(ctx);
        close.setText("\u2715 MEN\u00DCY\u00dc G\u0130ZLE"); close.setTextColor(DIM); close.setTextSize(12f);
        close.setGravity(Gravity.CENTER); close.setPadding(0, 14, 0, 0);
        close.setOnClickListener(v -> hideMenu());
        menuView.addView(close);
    }

    private boolean tog(View v) { boolean on = !(v.getTag() != null && (Boolean)v.getTag()); v.setTag(on); return on; }
    private void addTog(Context ctx, String name, View.OnClickListener cb) {
        TextView b = new TextView(ctx); b.setText(name+"   OFF"); b.setTextColor(TW); b.setTextSize(14f);
        b.setPadding(8, 14, 8, 14); b.setTag(false);
        b.setOnClickListener(v -> { cb.onClick(v); boolean on=(Boolean)v.getTag();
            ((TextView)v).setText(name+"   "+(on?"ON":"OFF")); ((TextView)v).setTextColor(on?PINK:TW); });
        menuView.addView(b);
    }
    private void addDiv(Context ctx) {
        View d = new View(ctx); d.setBackgroundColor(0x33EC4899);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, 1); lp.topMargin=8; lp.bottomMargin=8;
        d.setLayoutParams(lp); menuView.addView(d);
    }
    private void refresh() { if (espView != null) espView.postInvalidate(); }

    private WindowManager.LayoutParams mp(int w, int h, boolean touch) {
        int type = Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        if (!touch) flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        return new WindowManager.LayoutParams(w, h, type, flags, PixelFormat.TRANSLUCENT);
    }

    public void show() { showMenu(); showEsp(); }
    public void showMenu() {
        if (menuShowing) return;
        WindowManager.LayoutParams p = mp(-2, -2, true);
        p.gravity = Gravity.TOP | Gravity.START; p.x = 20; p.y = 80;
        menuView.setOnTouchListener(new DragListener(wm, menuView, p));
        try { wm.addView(menuView, p); menuShowing = true; } catch (Exception e) { android.util.Log.e("OP", ""+e); }
    }
    public void showEsp() {
        if (espShowing) return;
        android.graphics.Point sz = new android.graphics.Point(); wm.getDefaultDisplay().getSize(sz);
        WindowManager.LayoutParams p = mp(sz.x, sz.y, false);
        p.gravity = Gravity.TOP | Gravity.START;
        try { wm.addView(espView, p); espShowing = true; espView.startRender(); } catch (Exception e) { android.util.Log.e("OP", ""+e); }
    }
    public void hideMenu() { if (!menuShowing) return; try { wm.removeView(menuView); } catch (Exception ignored) {} menuShowing = false; }
    public void hide() { hideMenu(); if (!espShowing) return; try { wm.removeView(espView); espView.stopRender(); } catch (Exception ignored) {} espShowing = false; }
}
