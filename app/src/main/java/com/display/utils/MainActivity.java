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
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class MainActivity extends AppCompatActivity {
    private static final int BG = 0xFF1A0A1E;
    private static final int PINK = 0xFFEC4899;
    private static final int TW = 0xFFFCE7F3;
    private static final int TD = 0xFFD4A0B0;
    private static final int CARD = 0xFF2D1233;

    private FrameLayout contentFrame;
    private TextView[] tabViews;
    private int currentTab = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        // Content area
        contentFrame = new FrameLayout(this);
        contentFrame.setId(View.generateViewId());
        contentFrame.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        root.addView(contentFrame);

        // Bottom nav
        LinearLayout bottomNav = new LinearLayout(this);
        bottomNav.setOrientation(LinearLayout.HORIZONTAL);
        bottomNav.setBackgroundColor(CARD);
        bottomNav.setPadding(0, 12, 0, 12);
        LinearLayout.LayoutParams bnvLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bottomNav.setLayoutParams(bnvLp);

        String[][] tabs = {{"\u2302", "Home"}, {"\uD83D\uDE08", "Spoofer"}, {"\u2637", "Uyglar"}};
        tabViews = new TextView[tabs.length];

        for (int i = 0; i < tabs.length; i++) {
            final int idx = i;
            LinearLayout tabItem = new LinearLayout(this);
            tabItem.setOrientation(LinearLayout.VERTICAL);
            tabItem.setGravity(Gravity.CENTER);
            tabItem.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            tabItem.setPadding(0, 8, 0, 4);

            TextView icon = new TextView(this);
            icon.setText(tabs[i][0]);
            icon.setTextSize(22);
            icon.setGravity(Gravity.CENTER);
            icon.setTextColor(i == 0 ? PINK : TD);
            tabItem.addView(icon);

            TextView label = new TextView(this);
            label.setText(tabs[i][1]);
            label.setTextSize(11);
            label.setGravity(Gravity.CENTER);
            label.setTextColor(i == 0 ? PINK : TD);
            label.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            tabItem.addView(label);

            tabViews[i] = label;
            tabItem.setTag(icon);
            tabItem.setOnClickListener(v -> switchTab(idx));
            bottomNav.addView(tabItem);
        }

        root.addView(bottomNav);
        setContentView(root);

        requestPermissions();
        switchTab(0);
    }

    public void switchTabPublic(int idx) { switchTab(idx); }

    private void switchTab(int idx) {
        currentTab = idx;
        Fragment f;
        switch (idx) {
            case 0: f = new HomeFragment(); break;
            case 1: f = new DisplayFragment(); break;
            case 2: f = new ProfilesFragment(); break;
            default: f = new HomeFragment(); break;
        }

        getSupportFragmentManager().beginTransaction()
            .replace(contentFrame.getId(), f)
            .commitAllowingStateLoss();

        // Update tab colors
        for (int i = 0; i < tabViews.length; i++) {
            tabViews[i].setTextColor(i == idx ? PINK : TD);
            View parent = (View) tabViews[i].getParent();
            if (parent instanceof LinearLayout) {
                View iconView = ((LinearLayout) parent).getChildAt(0);
                if (iconView instanceof TextView) {
                    ((TextView) iconView).setTextColor(i == idx ? PINK : TD);
                }
            }
        }
    }

    private void requestPermissions() {
        String[] perms = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.SYSTEM_ALERT_WINDOW,
            Manifest.permission.FOREGROUND_SERVICE
        };
        java.util.List<String> needed = new java.util.ArrayList<>();
        for (String p : perms) {
            if (ActivityCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                needed.add(p);
            }
        }
        if (!needed.isEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toArray(new String[0]), 100);
        }
        if (Build.VERSION.SDK_INT >= 30) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    startActivity(new android.content.Intent(
                        android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        android.net.Uri.parse("package:" + getPackageName())));
                } catch (Exception ignored) {}
            }
        }
        if (Build.VERSION.SDK_INT >= 26) {
            if (!getPackageManager().canRequestPackageInstalls()) {
                try {
                    startActivity(new android.content.Intent(
                        android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        android.net.Uri.parse("package:" + getPackageName())));
                } catch (Exception ignored) {}
            }
        }
    }
}
