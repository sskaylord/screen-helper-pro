package com.display.utils;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class SettingsActivity extends Activity {

    private static final int BG_DARK = 0xFF0A0E1A;
    private static final int CARD_BG = 0xFF111827;
    private static final int ACCENT = 0xFF3B82F6;
    private static final int TEXT_WHITE = 0xFFE0E8F0;
    private static final int TEXT_DIM = 0xFF8899AA;

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

        content.addView(makeSection("Display"));
        content.addView(makeToggle("ESP Box", true));
        content.addView(makeToggle("Health Bar", true));
        content.addView(makeToggle("Snap Lines", true));
        content.addView(makeToggle("Skeleton", true));
        content.addView(makeToggle("Green Charm", true));
        content.addView(makeToggle("Show Teammates", false));
        content.addView(makeSpacer(20));
        content.addView(makeSection("Spoof"));
        content.addView(makeToggle("Auto Random ID", false));
        content.addView(makeToggle("Hide Root", true));
        content.addView(makeSpacer(20));
        content.addView(makeSection("About"));
        content.addView(makeInfoRow("Version", "1.0.0"));
        content.addView(makeInfoRow("Engine", "Custom Native"));
        content.addView(makeInfoRow("Architecture", "arm64-v8a"));

        scroll.addView(content);
        root.addView(scroll);
        setContentView(root);
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
        title.setText("Settings");
        title.setTextColor(TEXT_WHITE);
        title.setTextSize(20);
        title.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        bar.addView(title);

        return bar;
    }

    private View makeSection(String title) {
        TextView tv = new TextView(this);
        tv.setText(title);
        tv.setTextColor(ACCENT);
        tv.setTextSize(14);
        tv.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        tv.setPadding(0, 20, 0, 10);
        return tv;
    }

    private View makeToggle(String label, boolean defaultOn) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(20, 16, 20, 16);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD_BG);
        bg.setCornerRadius(12);
        row.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = 8;
        row.setLayoutParams(lp);

        TextView tv = new TextView(this);
        tv.setText(label);
        tv.setTextColor(TEXT_WHITE);
        tv.setTextSize(15);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(tv);

        final boolean[] state = {defaultOn};
        TextView toggle = new TextView(this);
        toggle.setText(state[0] ? "\u25CF ON" : "\u25CB OFF");
        toggle.setTextColor(state[0] ? 0xFF00FF41 : TEXT_DIM);
        toggle.setTextSize(14);
        toggle.setOnClickListener(v -> {
            state[0] = !state[0];
            toggle.setText(state[0] ? "\u25CF ON" : "\u25CB OFF");
            toggle.setTextColor(state[0] ? 0xFF00FF41 : TEXT_DIM);
        });
        row.addView(toggle);

        return row;
    }

    private View makeInfoRow(String label, String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(20, 12, 20, 12);

        TextView tv1 = new TextView(this);
        tv1.setText(label);
        tv1.setTextColor(TEXT_DIM);
        tv1.setTextSize(14);
        tv1.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(tv1);

        TextView tv2 = new TextView(this);
        tv2.setText(value);
        tv2.setTextColor(TEXT_WHITE);
        tv2.setTextSize(14);
        row.addView(tv2);

        return row;
    }

    private View makeSpacer(int h) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, h));
        return v;
    }
}
