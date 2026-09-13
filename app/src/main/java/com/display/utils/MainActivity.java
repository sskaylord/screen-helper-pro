package com.display.utils;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

public class MainActivity extends AppCompatActivity {
    static final int BG = 0xFF1A0A1E, PINK = 0xFFEC4899, TW = 0xFFFCE7F3, TD = 0xFFD4A0B0, CARD = 0xFF2D1233;
    private FrameLayout content;
    private TextView[] tabs;

    @Override protected void onCreate(Bundle s) {
        super.onCreate(s);
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG);

        content = new FrameLayout(this); content.setId(View.generateViewId());
        content.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1f));
        root.addView(content);

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL); nav.setBackgroundColor(CARD); nav.setPadding(0,12,0,12);
        String[][] t = {{"\u2302","Home"},{"\uD83D\uDE08","Spoofer"},{"\u2637","Uyglar"}};
        tabs = new TextView[3];
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            LinearLayout ti = new LinearLayout(this);
            ti.setOrientation(LinearLayout.VERTICAL); ti.setGravity(Gravity.CENTER);
            ti.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f)); ti.setPadding(0,8,0,4);
            TextView ic = new TextView(this); ic.setText(t[i][0]); ic.setTextSize(22); ic.setGravity(Gravity.CENTER);
            ic.setTextColor(i==0?PINK:TD); ti.addView(ic);
            TextView lb = new TextView(this); lb.setText(t[i][1]); lb.setTextSize(11); lb.setGravity(Gravity.CENTER);
            lb.setTextColor(i==0?PINK:TD); lb.setTypeface(Typeface.DEFAULT_BOLD); ti.addView(lb);
            tabs[i] = lb; ti.setTag(ic);
            ti.setOnClickListener(v -> switchTab(idx));
            nav.addView(ti);
        }
        root.addView(nav);
        setContentView(root);
        reqPerms();
        switchTab(0);
    }

    public void switchTabPublic(int i) { switchTab(i); }

    private void switchTab(int i) {
        Fragment f;
        switch(i) { case 1: f=new DisplayFragment(); break; case 2: f=new ProfilesFragment(); break; default: f=new HomeFragment(); }
        getSupportFragmentManager().beginTransaction().replace(content.getId(), f).commitAllowingStateLoss();
        for (int j=0;j<tabs.length;j++) {
            tabs[j].setTextColor(j==i?PINK:TD);
            View p=(View)tabs[j].getParent();
            if(p instanceof LinearLayout){View ic=((LinearLayout)p).getChildAt(0);if(ic instanceof TextView)((TextView)ic).setTextColor(j==i?PINK:TD);}
        }
    }

    private void reqPerms() {
        String[] ps={Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.SYSTEM_ALERT_WINDOW,Manifest.permission.FOREGROUND_SERVICE};
        java.util.List<String> need=new java.util.ArrayList<>();
        for(String p:ps) if(ActivityCompat.checkSelfPermission(this,p)!=PackageManager.PERMISSION_GRANTED) need.add(p);
        if(!need.isEmpty()) ActivityCompat.requestPermissions(this,need.toArray(new String[0]),100);
        if(Build.VERSION.SDK_INT>=30&&!Environment.isExternalStorageManager()){
            try{startActivity(new android.content.Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                android.net.Uri.parse("package:"+getPackageName())));}catch(Exception ignored){}}
        if(Build.VERSION.SDK_INT>=26&&!getPackageManager().canRequestPackageInstalls()){
            try{startActivity(new android.content.Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                android.net.Uri.parse("package:"+getPackageName())));}catch(Exception ignored){}}
    }
}
