package com.display.utils;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class ProfileManager extends Activity {
    private static final String TAG = "PM";
    private static final int BG_DARK = 0xFF0A0E1A;
    private static final int CARD_BG = 0xFF111827;
    private static final int ACCENT = 0xFF3B82F6;
    private static final int PINK = 0xFFEC4899;
    private static final int TEXT_WHITE = 0xFFE0E8F0;
    private static final int TEXT_DIM = 0xFF8899AA;

    private LinearLayout appList;

    private void log(String msg) {
        Log.e(TAG, msg);
        try {
            FileWriter fw = new FileWriter(
                Environment.getExternalStorageDirectory() + "/Download/crash_display.log", true);
            fw.write(System.currentTimeMillis() + " " + msg + "\n");
            fw.close();
        } catch (Exception ignored) {}
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log("onCreate start");
        try {
            getWindow().setStatusBarColor(BG_DARK);
            getWindow().setNavigationBarColor(BG_DARK);

            LinearLayout root = new LinearLayout(this);
            root.setOrientation(LinearLayout.VERTICAL);
            root.setBackgroundColor(BG_DARK);
            root.addView(makeTopBar());

            ScrollView scroll = new ScrollView(this);
            scroll.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));

            LinearLayout content = new LinearLayout(this);
            content.setOrientation(LinearLayout.VERTICAL);
            content.setPadding(40, 20, 40, 40);

            content.addView(makeHeader());
            content.addView(makeSpacer(20));

            appList = new LinearLayout(this);
            appList.setOrientation(LinearLayout.VERTICAL);

            addApp("Play Store", "com.android.vending", true);
            addApp("Google", "com.google.android.googlequicksearchbox", true);
            addApp("Standoff 2", "com.axlebolt.standoff2", true);
            addApp("WhatsApp", "com.whatsapp", false);
            addApp("Instagram", "com.instagram.android", false);
            addApp("TikTok", "com.zhiliaoapp.musically", false);
            addApp("Telegram", "org.telegram.messenger", false);
            addApp("Discord", "com.discord", false);

            content.addView(appList);
            scroll.addView(content);
            root.addView(scroll);
            setContentView(root);
            log("onCreate done");
        } catch (Exception e) {
            log("onCreate CRASH: " + e.toString());
            Toast.makeText(this, "Hata: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private View makeTopBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(20, 16, 20, 16);
        bar.setBackgroundColor(BG_DARK);

        TextView back = new TextView(this);
        back.setText("\u2190");
        back.setTextColor(TEXT_WHITE);
        back.setTextSize(24);
        back.setPadding(10, 0, 20, 0);
        back.setOnClickListener(v -> finish());
        bar.addView(back);

        TextView title = new TextView(this);
        title.setText("Uygulama Yöneticisi");
        title.setTextColor(TEXT_WHITE);
        title.setTextSize(20);
        title.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        bar.addView(title);
        return bar;
    }

    private View makeHeader() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(30, 24, 30, 24);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD_BG);
        bg.setCornerRadius(16);
        card.setBackground(bg);

        TextView icon = new TextView(this);
        icon.setText("\uD83D\uDCCB");
        icon.setTextSize(28);
        icon.setPadding(0, 0, 20, 0);
        card.addView(icon);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);

        TextView t1 = new TextView(this);
        t1.setText("Uygulamalarınızı yönetin");
        t1.setTextColor(TEXT_WHITE);
        t1.setTextSize(16);
        t1.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        texts.addView(t1);

        TextView t2 = new TextView(this);
        t2.setText("Hızlı erişim ve hesap yönetimi.");
        t2.setTextColor(TEXT_DIM);
        t2.setTextSize(13);
        texts.addView(t2);

        card.addView(texts);
        return card;
    }

    private void addApp(String name, String pkg, boolean isPrimary) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(20, 20, 20, 20);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = 12;
        row.setLayoutParams(lp);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD_BG);
        bg.setCornerRadius(12);
        row.setBackground(bg);

        TextView icon = new TextView(this);
        icon.setText("\uD83D\uDCE6");
        icon.setTextSize(28);
        icon.setPadding(0, 0, 20, 0);
        row.addView(icon);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setLayoutParams(new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView t1 = new TextView(this);
        t1.setText(name);
        t1.setTextColor(TEXT_WHITE);
        t1.setTextSize(16);
        t1.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        texts.addView(t1);

        TextView t2 = new TextView(this);
        t2.setText(pkg);
        t2.setTextColor(TEXT_DIM);
        t2.setTextSize(12);
        texts.addView(t2);

        row.addView(texts);

        TextView actionBtn = new TextView(this);
        actionBtn.setText("Aç");
        actionBtn.setTextColor(Color.WHITE);
        actionBtn.setTextSize(14);
        actionBtn.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        actionBtn.setPadding(30, 12, 30, 12);
        actionBtn.setGravity(Gravity.CENTER);

        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(isPrimary ? ACCENT : PINK);
        btnBg.setCornerRadius(20);
        actionBtn.setBackground(btnBg);
        actionBtn.setOnClickListener(v -> onAppAction(name, pkg));
        row.addView(actionBtn);

        appList.addView(row);
    }

    private void onAppAction(String name, String pkg) {
        log("onAppAction: " + name + " / " + pkg);
        Toast.makeText(this, "Yükleniyor " + name + "...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                if (pkg.equals("com.android.vending") ||
                    pkg.equals("com.google.android.googlequicksearchbox")) {
                    openServiceApp(pkg);
                } else if (pkg.equals("com.axlebolt.standoff2")) {
                    openTargetApp();
                } else {
                    runOnUiThread(() ->
                        Toast.makeText(this, name + " henüz desteklenmiyor", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                log("onAppAction ERROR: " + e.toString());
                runOnUiThread(() ->
                    Toast.makeText(this, "Hata: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void openServiceApp(String pkg) throws Exception {
        log("openServiceApp: " + pkg);

        CompatLoader loader = new CompatLoader(this);
        boolean ready = loader.init();
        log("CompatLoader.init = " + ready);

        PathHelper ph = new PathHelper(this);
        ph.activate(pkg);

        AssetLoader.loadTarget(this, pkg);

        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(pkg);
        if (launchIntent == null) {
            runOnUiThread(() ->
                Toast.makeText(this, pkg + " yüklü değil", Toast.LENGTH_SHORT).show());
            return;
        }

        runOnUiThread(() -> {
            try {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launchIntent);
                Toast.makeText(this, "Açıldı", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                log("openServiceApp launch error: " + e.toString());
                Toast.makeText(this, "Açılamadı: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void openTargetApp() throws Exception {
        String pkg = "com.axlebolt.standoff2";
        log("openTargetApp start");

        CompatLoader loader = new CompatLoader(this);
        boolean gmsReady = loader.init();
        log("CompatLoader.init = " + gmsReady);

        boolean initOk = DisplayCore.initialize(this, pkg);
        log("DisplayCore.initialize = " + initOk);

        if (!initOk) {
            runOnUiThread(() ->
                Toast.makeText(this, "Başlatma hatası", Toast.LENGTH_LONG).show());
            return;
        }

        Intent launchIntent = getPackageManager().getLaunchIntentForPackage(pkg);
        if (launchIntent == null) {
            runOnUiThread(() ->
                Toast.makeText(this, "Standoff 2 yüklü değil", Toast.LENGTH_SHORT).show());
            DisplayCore.cleanup();
            return;
        }

        runOnUiThread(() -> {
            try {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launchIntent);

                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    try {
                        DisplayPanel panel = DisplayCore.getOverlayPanel();
                        if (panel != null) panel.show();
                    } catch (Exception ignored) {}
                }, 2000);

                Toast.makeText(this, "Standoff 2 açıldı", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                log("openTargetApp launch error: " + e.toString());
                Toast.makeText(this, "Hata: " + e.getMessage(), Toast.LENGTH_LONG).show();
                DisplayCore.cleanup();
            }
        });
    }

    private View makeSpacer(int h) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, h));
        return v;
    }
}
