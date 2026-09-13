package com.display.utils;

import android.app.Activity;
import android.content.Intent;
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
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import com.display.utils.engine.VCore;
import java.io.FileWriter;

public class ProfileManager extends Activity {
    private static final String TAG = "PM";
    private static final int BG = 0xFF1A0A1E;
    private static final int CARD = 0xFF2D1233;
    private static final int PINK = 0xFFEC4899;
    private static final int TW = 0xFFFCE7F3;
    private static final int TD = 0xFFD4A0B0;

    private void log(String m) {
        Log.e(TAG, m);
        try { FileWriter fw = new FileWriter(Environment.getExternalStorageDirectory() + "/Download/crash_display.log", true); fw.write(System.currentTimeMillis() + " " + m + "\n"); fw.close(); } catch (Exception ignored) {}
    }

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        log("onCreate");
        try {
            getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
            LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG);
            LinearLayout top = new LinearLayout(this); top.setOrientation(LinearLayout.HORIZONTAL); top.setGravity(Gravity.CENTER_VERTICAL); top.setPadding(20,16,20,16); top.setBackgroundColor(BG);
            TextView back = new TextView(this); back.setText("\u2190"); back.setTextColor(TW); back.setTextSize(24); back.setPadding(10,0,20,0); back.setOnClickListener(v -> finish()); top.addView(back);
            TextView title = new TextView(this); title.setText("Uyg Klonla"); title.setTextColor(TW); title.setTextSize(20); title.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD)); top.addView(title);
            root.addView(top);
            LinearLayout tabs = new LinearLayout(this); tabs.setOrientation(LinearLayout.HORIZONTAL); tabs.setPadding(20,10,20,10);
            String[] cats = {"T\u00fcm Uygular","Oyunlar","Sosyal","Di\u011fer"};
            for (int i = 0; i < cats.length; i++) { TextView tab = new TextView(this); tab.setText(cats[i]); tab.setTextColor(i==0?Color.WHITE:TD); tab.setTextSize(13); tab.setPadding(20,8,20,8); GradientDrawable tb = new GradientDrawable(); tb.setColor(i==0?PINK:CARD); tb.setCornerRadius(16); tab.setBackground(tb); LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT); tlp.rightMargin=12; tab.setLayoutParams(tlp); tabs.addView(tab); }
            root.addView(tabs);
            ScrollView scroll = new ScrollView(this); scroll.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
            LinearLayout list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); list.setPadding(20,10,20,20);
            addApp(list,"Play Store","com.android.vending","Google Play Store",true);
            addApp(list,"Google Chrome","com.google.android.googlequicksearchbox","Web Taray\u0131c\u0131",true);
            addApp(list,"WhatsApp","com.whatsapp","Sosyal",false);
            addApp(list,"Instagram","com.instagram.android","Sosyal",false);
            addApp(list,"TikTok","com.zhiliaoapp.musically","Sosyal",false);
            addApp(list,"PUBG MOBILE","com.tencent.ig","Oyun",true);
            addApp(list,"Standoff 2","com.axlebolt.standoff2","Oyun",true);
            addApp(list,"Valorant Mobile","com.riotgames.valorant","Oyun",true);
            addApp(list,"Telegram","org.telegram.messenger","Sosyal",false);
            addApp(list,"Facebook","com.facebook.katana","Sosyal",false);
            scroll.addView(list); root.addView(scroll); setContentView(root);
            log("onCreate done");
        } catch (Exception e) { log("CRASH: "+e); Toast.makeText(this,"Hata: "+e.getMessage(),Toast.LENGTH_LONG).show(); finish(); }
    }

    private void addApp(LinearLayout p, String name, String pkg, String cat, boolean pri) {
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(20,16,20,16);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT); lp.bottomMargin=10; row.setLayoutParams(lp);
        GradientDrawable bg = new GradientDrawable(); bg.setColor(CARD); bg.setCornerRadius(14); row.setBackground(bg);
        TextView icon = new TextView(this); icon.setText("\uD83D\uDCE6"); icon.setTextSize(26); icon.setPadding(0,0,16,0); row.addView(icon);
        LinearLayout texts = new LinearLayout(this); texts.setOrientation(LinearLayout.VERTICAL); texts.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView t1 = new TextView(this); t1.setText(name); t1.setTextColor(TW); t1.setTextSize(15); t1.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD)); texts.addView(t1);
        TextView t2 = new TextView(this); t2.setText(cat); t2.setTextColor(TD); t2.setTextSize(12); texts.addView(t2); row.addView(texts);
        TextView btn = new TextView(this); btn.setText("Klonla"); btn.setTextColor(Color.WHITE); btn.setTextSize(13); btn.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD)); btn.setPadding(24,10,24,10); btn.setGravity(Gravity.CENTER);
        GradientDrawable bb = new GradientDrawable(); bb.setColor(PINK); bb.setCornerRadius(18); btn.setBackground(bb);
        btn.setOnClickListener(v -> onAction(name, pkg)); row.addView(btn); p.addView(row);
    }

    private void onAction(String name, String pkg) {
        log("onAction: "+name+" / "+pkg);
        Toast.makeText(this, name+" ba\u015flat\u0131l\u0131yor...", Toast.LENGTH_SHORT).show();
        new Thread(() -> { try {
            if (pkg.equals("com.axlebolt.standoff2")) launchGame(pkg); else launchApp(pkg);
        } catch (Exception e) { log("ERROR: "+e); runOnUiThread(() -> Toast.makeText(this,"Hata: "+e.getMessage(),Toast.LENGTH_LONG).show()); } }).start();
    }

    private void launchApp(String pkg) throws Exception {
        log("launchApp: "+pkg);
        CompatLoader.loadGms(this);
        VCore.get().init(getApplicationContext());
        VCore.get().installApp(pkg);
        runOnUiThread(() -> VCore.get().launchApp(pkg));
    }

    private void launchGame(String pkg) throws Exception {
        log("launchGame: "+pkg);
        CompatLoader.loadGms(this);
        VCore.get().init(getApplicationContext());
        VCore.get().installApp(pkg);
        DisplayCore.initialize(this, pkg);
        runOnUiThread(() -> {
            VCore.get().launchApp(pkg);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try { OverlayPanel p = DisplayCore.getPanel(); if (p != null) p.show(); } catch (Exception ignored) {}
            }, 3000);
        });
    }
}