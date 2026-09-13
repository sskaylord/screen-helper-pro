package com.display.utils;

import android.app.Fragment;
import android.content.Intent;
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
 * Account profiles management fragment.
 * Launches ProfileManager activity for app cloning.
 * Stealth-named to appear as multi-account manager.
 */
public class ProfilesFragment extends Fragment {

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

        // Open ProfileManager button
        content.addView(makeOpenButton());
        content.addView(makeSpacer(20));

        // Info card
        content.addView(makeInfoCard());

        scroll.addView(content);
        return scroll;
    }

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
        icon.setText("\uD83D\uDC65");
        icon.setTextSize(28);
        icon.setPadding(0, 0, 20, 0);
        card.addView(icon);

        LinearLayout texts = new LinearLayout(getActivity());
        texts.setOrientation(LinearLayout.VERTICAL);

        TextView t1 = new TextView(getActivity());
        t1.setText("Account Profiles");
        t1.setTextColor(TEXT_WHITE);
        t1.setTextSize(16);
        t1.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        texts.addView(t1);

        TextView t2 = new TextView(getActivity());
        t2.setText("Manage multiple app instances.");
        t2.setTextColor(TEXT_DIM);
        t2.setTextSize(13);
        texts.addView(t2);

        card.addView(texts);
        return card;
    }

    private View makeOpenButton() {
        TextView btn = new TextView(getActivity());
        btn.setText("Open Profile Manager");
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(16);
        btn.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(0, 24, 0, 24);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ACCENT);
        bg.setCornerRadius(16);
        btn.setBackground(bg);

        btn.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ProfileManager.class);
            startActivity(intent);
        });

        return btn;
    }

    private View makeInfoCard() {
        LinearLayout card = new LinearLayout(getActivity());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(30, 24, 30, 24);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD_BG);
        bg.setCornerRadius(16);
        card.setBackground(bg);

        TextView t1 = new TextView(getActivity());
        t1.setText("\uD83D\uDCA1 How it works");
        t1.setTextColor(TEXT_WHITE);
        t1.setTextSize(15);
        t1.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        card.addView(t1);

        card.addView(makeSpacer(12));

        TextView t2 = new TextView(getActivity());
        t2.setText("Manage display configurations and preferences.");
        t2.setTextColor(TEXT_DIM);
        t2.setTextSize(13);
        t2.setLineSpacing(4, 1.2f);
        card.addView(t2);

        return card;
    }

    private View makeSpacer(int h) {
        View v = new View(getActivity());
        v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, h));
        return v;
    }
}
