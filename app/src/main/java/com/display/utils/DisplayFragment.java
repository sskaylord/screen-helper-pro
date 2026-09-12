package com.display.utils;

import android.app.Fragment;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * Display and overlay settings fragment.
 * Shows ESP toggle controls and visual customization options.
 * Stealth-named to appear as screen configuration utility.
 */
public class DisplayFragment extends Fragment {

    private static final int BG_DARK = 0xFF0A0E1A;
    private static final int CARD_BG = 0xFF111827;
    private static final int ACCENT = 0xFF3B82F6;
    private static final int TEXT_WHITE = 0xFFE0E8F0;
    private static final int TEXT_DIM = 0xFF8899AA;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ScrollView scroll = new ScrollView(getActivity());
        scroll.setBackgroundColor(BG_DARK);

        LinearLayout content = new LinearLayout(getActivity());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(40, 30, 40, 40);

        // Header card
        content.addView(makeHeader());
        content.addView(makeSpacer(20));

        // Feature toggles placeholder
        String[] features = {
            "Box Display", "Health Indicator", "Skeleton View", "Name Label",
            "Distance Info", "Weapon Icon", "Charm Highlight", "Crosshair",
            "Snap Line", "Team Filter", "Visible Only", "Auto Track"
        };

        for (String feature : features) {
            content.addView(makeToggleRow(feature));
            content.addView(makeSpacer(8));
        }

        scroll.addView(content);
        return scroll;
    }

    /**
     * Build header card with icon and description.
     */
    private View makeHeader() {
        LinearLayout card = new LinearLayout(getActivity());
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(30, 24, 30, 24);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD_BG);
        bg.setCornerRadius(16);
        card.setBackground(bg);

        TextView icon = new TextView(getActivity());
        icon.setText("\uD83C\uDFA8");
        icon.setTextSize(28);
        icon.setPadding(0, 0, 20, 0);
        card.addView(icon);

        LinearLayout texts = new LinearLayout(getActivity());
        texts.setOrientation(LinearLayout.VERTICAL);

        TextView t1 = new TextView(getActivity());
        t1.setText("Display Settings");
        t1.setTextColor(TEXT_WHITE);
        t1.setTextSize(16);
        t1.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        texts.addView(t1);

        TextView t2 = new TextView(getActivity());
        t2.setText("Configure visual overlay options.");
        t2.setTextColor(TEXT_DIM);
        t2.setTextSize(13);
        texts.addView(t2);

        card.addView(texts);
        return card;
    }

    /**
     * Create a single toggle row with label.
     * Actual toggle state managed by OverlayPanel native bridge.
     */
    private View makeToggleRow(String label) {
        LinearLayout row = new LinearLayout(getActivity());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(24, 18, 24, 18);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD_BG);
        bg.setCornerRadius(12);
        row.setBackground(bg);

        TextView tv = new TextView(getActivity());
        tv.setText(label);
        tv.setTextColor(TEXT_WHITE);
        tv.setTextSize(15);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(tv);

        TextView status = new TextView(getActivity());
        status.setText("OFF");
        status.setTextColor(TEXT_DIM);
        status.setTextSize(13);
        row.addView(status);

        return row;
    }

    private View makeSpacer(int h) {
        View v = new View(getActivity());
        v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, h));
        return v;
    }
}
