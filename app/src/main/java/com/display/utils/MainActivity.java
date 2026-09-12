package com.display.utils;

import android.app.Activity;
import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private FrameLayout contentFrame;
    private TextView tabHome, tabSpoofer, tabUI;
    private int currentTab = 2;

    private static final int BG_DARK = 0xFF0A0E1A;
    private static final int NAV_BG = 0xFF111827;
    private static final int ACCENT = 0xFF3B82F6;
    private static final int TEXT_DIM = 0xFF8899AA;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().setStatusBarColor(BG_DARK);
        getWindow().setNavigationBarColor(NAV_BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG_DARK);

        contentFrame = new FrameLayout(this);
        contentFrame.setId(View.generateViewId());
        contentFrame.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        root.addView(contentFrame);

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setBackgroundColor(NAV_BG);
        nav.setPadding(0, 12, 0, 12);
        nav.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT));

        tabUI = makeTab("UI", "\uD83C\uDFA8", 0);
        tabSpoofer = makeTab("Spoofer", "\uD83D\uDC46", 1);
        tabHome = makeTab("Home", "\uD83C\uDFE0", 2);

        nav.addView(tabUI, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        nav.addView(tabSpoofer, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        nav.addView(tabHome, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        root.addView(nav);
        setContentView(root);
        switchTab(2);
    }

    private TextView makeTab(String label, String icon, final int idx) {
        TextView tv = new TextView(this);
        tv.setText(icon + "\n" + label);
        tv.setTextColor(TEXT_DIM);
        tv.setTextSize(12);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, 8, 0, 8);
        tv.setOnClickListener(v -> switchTab(idx));
        return tv;
    }

    private void switchTab(int index) {
    public void switchTabPublic(int index) { switchTab(index); }
        currentTab = index;
        tabHome.setTextColor(index == 2 ? ACCENT : TEXT_DIM);
        tabSpoofer.setTextColor(index == 1 ? ACCENT : TEXT_DIM);
        tabUI.setTextColor(index == 0 ? ACCENT : TEXT_DIM);

        Fragment f;
        switch (index) {
            case 0: f = new HomeFragment(); break;
            case 1: f = new SpooferFragment(); break;
            default: f = new HomeFragment(); break;
        }

        getFragmentManager().beginTransaction()
            .replace(contentFrame.getId(), f)
            .commitAllowingStateLoss();
    }
}
