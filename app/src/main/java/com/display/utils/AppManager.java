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
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Cloned apps manager and virtual launch activity.
 * Displays list of cloneable applications with styled UI.
 * Standoff 2 triggers full DisplayCore engine bootstrap.
 * Other apps show placeholder toast for future expansion.
 */
public class AppManager extends Activity {
    private static final int BG_DARK = 0xFF0A0E1A;
    private static final int CARD_BG = 0xFF111827;
    private static final int ACCENT = 0xFF3B82F6;
    private static final int PINK = 0xFFEC4899;
    private static final int TEXT_WHITE = 0xFFE0E8F0;
    private static final int TEXT_DIM = 0xFF8899AA;

    private LinearLayout appList;
    private int scrollPos = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        addApp("X", "com.twitter.android", false);
        addApp("Facebook", "com.facebook.katana", false);

        content.addView(appList);
        scroll.addView(content);
        root.addView(scroll);

        setContentView(root);
    }

    /**
     * Build top navigation bar with back button and title.
     */
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
        title.setText("Cloned Apps");
        title.setTextColor(TEXT_WHITE);
        title.setTextSize(20);
        title.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        bar.addView(title);

        return bar;
    }

    /**
     * Build header card with icon and description text.
     */
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
        t1.setText("Clone your favorite apps");
        t1.setTextColor(TEXT_WHITE);
        t1.setTextSize(16);
        t1.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        texts.addView(t1);

        TextView t2 = new TextView(this);
        t2.setText("Run multiple accounts at the same time.");
        t2.setTextColor(TEXT_DIM);
        t2.setTextSize(13);
        texts.addView(t2);

        card.addView(texts);
        return card;
    }

    /**
     * Add a single app row to the list with icon, name, package, and clone button.
     * Primary apps get accent color, secondary get pink.
     *
     * @param name Display name of the application
     * @param pkg Package name for lookup and launch
     * @param isPrimary Whether this is a primary/supported target
     */
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
        if (scrollPos > 3 && !isPrimary) {
            bg.setColor(0xFFFFF0F5);
            row.setBackground(bg);
        } else {
            bg.setColor(CARD_BG);
            bg.setCornerRadius(12);
            row.setBackground(bg);
        }

        TextView icon = new TextView(this);
        icon.setText(getAppIcon(pkg));
        icon.setTextSize(28);
        icon.setPadding(0, 0, 20, 0);
        row.addView(icon);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView t1 = new TextView(this);
        t1.setText(name);
        t1.setTextColor(isPrimary && scrollPos <= 3 ? TEXT_WHITE : (scrollPos > 3 && !isPrimary ? 0xFF333333 : TEXT_WHITE));
        t1.setTextSize(16);
        t1.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        texts.addView(t1);

        TextView t2 = new TextView(this);
        t2.setText(pkg);
        t2.setTextColor(isPrimary && scrollPos <= 3 ? TEXT_DIM : (scrollPos > 3 && !isPrimary ? 0xFF888888 : TEXT_DIM));
        t2.setTextSize(12);
        texts.addView(t2);

        row.addView(texts);

        TextView cloneBtn = new TextView(this);
        cloneBtn.setText("Clone");
        cloneBtn.setTextColor(Color.WHITE);
        cloneBtn.setTextSize(14);
        cloneBtn.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        cloneBtn.setPadding(30, 12, 30, 12);
        cloneBtn.setGravity(Gravity.CENTER);

        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(isPrimary ? ACCENT : PINK);
        btnBg.setCornerRadius(20);
        cloneBtn.setBackground(btnBg);
        cloneBtn.setOnClickListener(v -> onCloneClick(name, pkg));
        row.addView(cloneBtn);

        appList.addView(row);
        scrollPos++;
    }

    /**
     * Get emoji icon for installed package.
     * Returns package emoji if found, generic box otherwise.
     */
    private String getAppIcon(String pkg) {
        try {
            PackageManager pm = getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
            return "\uD83D\uDCE6";
        } catch (Exception e) {
            return "\uD83D\uDCE6";
        }
    }

    /**
     * Handle clone button click.
     * For Standoff 2: full DisplayCore bootstrap + virtual launch + overlay.
     * For other apps: placeholder toast indicating unsupported.
     *
     * @param name Display name for toast feedback
     * @param pkg Target package name
     */
    private void onCloneClick(String name, String pkg) {
        // Only Standoff 2 triggers full engine bootstrap
        if (!pkg.equals("com.axlebolt.standoff2")) {
            showToast("Only Standoff 2 supported");
            return;
        }

        try {
            // Delegate entire bootstrap to DisplayCore orchestrator
            // DisplayCore internally creates PathHelper + OverlayPanel instances
            if (!DisplayCore.initialize(this, pkg)) {
                showToast("Engine init failed");
                return;
            }

            // Launch target game activity via reflection in virtual space
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(pkg);
            if (launchIntent != null) {
                ComponentName cn = launchIntent.getComponent();
                Class<?> targetClass = Class.forName(
                        cn.getClassName(),
                        false,
                        AssetLoader.getTargetClassLoader()
                );
                Intent virtualIntent = new Intent(this, targetClass);
                virtualIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                virtualIntent.putExtras(launchIntent);
                startActivity(virtualIntent);
            }

            // Show overlay menu after game has time to initialize
            new android.os.Handler(android.os.Looper.getMainLooper())
                    .postDelayed(() -> {
                        OverlayPanel panel = DisplayCore.getOverlayPanel();
                        if (panel != null) panel.show();
                    }, 1500);

            showToast("Launched");

        } catch (Exception e) {
            showToast("Error: " + e.getMessage());
            DisplayCore.cleanup();
        }
    }

    /**
     * Show temporary toast-style overlay message.
     * Auto-dismisses after 2 seconds.
     *
     * @param msg Message text to display
     */
    private void showToast(String msg) {
        TextView toast = new TextView(this);
        toast.setText(msg);
        toast.setTextColor(Color.WHITE);
        toast.setTextSize(16);
        toast.setGravity(Gravity.CENTER);
        toast.setPadding(40, 30, 40, 30);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xDD111827);
        bg.setCornerRadius(16);
        toast.setBackground(bg);

        android.widget.FrameLayout overlay = new android.widget.FrameLayout(this);
        overlay.addView(toast, new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER));

        addContentView(overlay, new android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT));

        overlay.postDelayed(() -> {
            try {
                ((android.view.ViewGroup) overlay.getParent()).removeView(overlay);
            } catch (Exception ignored) {}
        }, 2000);
    }

    /**
     * Create vertical spacer view.
     *
     * @param h Height in pixels
     */
    private View makeSpacer(int h) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, h));
        return v;
    }
}
