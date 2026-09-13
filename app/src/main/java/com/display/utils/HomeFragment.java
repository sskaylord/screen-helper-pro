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

public class HomeFragment extends Fragment {

    private static final int BG_DARK = 0xFF1A0A1E;
    private static final int CARD_BG = 0xFF2D1233;
    private static final int ACCENT = 0xFFEC4899;
    private static final int PURPLE = 0xFF7B2FBE;
    private static final int TEXT_WHITE = 0xFFFCE7F3;
    private static final int TEXT_DIM = 0xFFD4A0B0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ScrollView scroll = new ScrollView(getActivity());
        scroll.setBackgroundColor(BG_DARK);

        LinearLayout root = new LinearLayout(getActivity());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(40, 40, 40, 40);

        root.addView(makeHeader());
        root.addView(makeBanner());
        root.addView(makeWelcomeCard());
        root.addView(makeSpacer(30));
        root.addView(makeActionButton("Uyg Klonla", "Uygulamaları klonla ve yönet", "\uD83D\uDCCB", () -> {
            startActivity(new Intent(getActivity(), ProfileManager.class));
        }));
        root.addView(makeSpacer(20));
        root.addView(makeActionButton("Ayarlar", "Cihaz bilgilerini değiştir", "\uD83D\uDC46", () -> {
            ((MainActivity) getActivity()).switchTabPublic(1);
        }));
        root.addView(makeSpacer(30));
        root.addView(makeQuickActions());

        scroll.addView(root);
        return scroll;
    }

    private View makeHeader() {
        LinearLayout header = new LinearLayout(getActivity());
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(getActivity());
        title.setText("AZUREHUB");
        title.setTextColor(Color.WHITE);
        title.setTextSize(28);
        title.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        title.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        header.addView(title);

        TextView crown = new TextView(getActivity());
        crown.setText("\uD83D\uDC51");
        crown.setTextSize(24);
        header.addView(crown);

        TextView gear = new TextView(getActivity());
        gear.setText(" \u2699");
        gear.setTextSize(24);
        gear.setTextColor(TEXT_DIM);
        gear.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), DeviceConfig.class));
        });
        header.addView(gear);

        TextView sub = new TextView(getActivity());
        sub.setText("Uyg Klonla");
        sub.setTextColor(TEXT_DIM);
        sub.setTextSize(14);

        LinearLayout wrap = new LinearLayout(getActivity());
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.addView(header);
        wrap.addView(sub);
        return wrap;
    }

    private View makeBanner() {
        TextView banner = new TextView(getActivity());
        banner.setText("\n\n\uD83C\uDFAE  AZUREHUB  \uD83C\uDFAE\n\n");
        banner.setTextColor(PURPLE);
        banner.setTextSize(20);
        banner.setGravity(Gravity.CENTER);
        banner.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF1A1040);
        bg.setCornerRadius(24);
        banner.setBackground(bg);
        banner.setPadding(20, 40, 20, 40);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = 30;
        banner.setLayoutParams(lp);
        return banner;
    }

    private View makeWelcomeCard() {
        LinearLayout card = new LinearLayout(getActivity());
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(30, 24, 30, 24);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD_BG);
        bg.setCornerRadius(16);
        bg.setStroke(1, 0xFF2A3A5C);
        card.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = 20;
        card.setLayoutParams(lp);

        TextView icon = new TextView(getActivity());
        icon.setText("\u2744");
        icon.setTextColor(ACCENT);
        icon.setTextSize(28);
        icon.setPadding(0, 0, 20, 0);
        card.addView(icon);

        LinearLayout texts = new LinearLayout(getActivity());
        texts.setOrientation(LinearLayout.VERTICAL);

        TextView t1 = new TextView(getActivity());
        t1.setText("Welcome to AZUREHUB");
        t1.setTextColor(TEXT_WHITE);
        t1.setTextSize(16);
        t1.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        texts.addView(t1);

        TextView t2 = new TextView(getActivity());
        t2.setText("Uygulama yöneticisi. Klonla, Oyna.");
        t2.setTextColor(TEXT_DIM);
        t2.setTextSize(13);
        texts.addView(t2);

        card.addView(texts);
        return card;
    }

    private View makeActionButton(String title, String subtitle, String icon, Runnable action) {
        LinearLayout card = new LinearLayout(getActivity());
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(30, 28, 30, 28);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD_BG);
        bg.setCornerRadius(16);
        bg.setStroke(1, PURPLE);
        card.setBackground(bg);
        card.setOnClickListener(v -> action.run());

        TextView ic = new TextView(getActivity());
        ic.setText(icon);
        ic.setTextSize(28);
        ic.setPadding(0, 0, 24, 0);
        card.addView(ic);

        LinearLayout texts = new LinearLayout(getActivity());
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView t1 = new TextView(getActivity());
        t1.setText(title);
        t1.setTextColor(TEXT_WHITE);
        t1.setTextSize(17);
        t1.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        texts.addView(t1);

        TextView t2 = new TextView(getActivity());
        t2.setText(subtitle);
        t2.setTextColor(TEXT_DIM);
        t2.setTextSize(13);
        texts.addView(t2);

        card.addView(texts);

        TextView arrow = new TextView(getActivity());
        arrow.setText("\u203A");
        arrow.setTextColor(TEXT_DIM);
        arrow.setTextSize(28);
        card.addView(arrow);

        return card;
    }

    private View makeQuickActions() {
        LinearLayout row = new LinearLayout(getActivity());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);

        row.addView(makeQuickBtn("Play Store", "\uD83D\uDED2"));
        row.addView(makeQuickBtn("Google", "\uD83D\uDD0D"));
        row.addView(makeQuickBtn("Settings", "\u2699"));
        row.addView(makeQuickBtn("Help", "\u2753"));

        LinearLayout wrap = new LinearLayout(getActivity());
        wrap.setOrientation(LinearLayout.VERTICAL);

        TextView label = new TextView(getActivity());
        label.setText("Quick Actions");
        label.setTextColor(TEXT_DIM);
        label.setTextSize(14);
        label.setPadding(0, 0, 0, 16);
        wrap.addView(label);
        wrap.addView(row);
        return wrap;
    }

    private View makeQuickBtn(String label, String icon) {
        LinearLayout btn = new LinearLayout(getActivity());
        btn.setOrientation(LinearLayout.VERTICAL);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(20, 16, 20, 16);
        btn.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView ic = new TextView(getActivity());
        ic.setText(icon);
        ic.setTextSize(24);
        ic.setGravity(Gravity.CENTER);
        btn.addView(ic);

        TextView tv = new TextView(getActivity());
        tv.setText(label);
        tv.setTextColor(TEXT_DIM);
        tv.setTextSize(11);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, 8, 0, 0);
        btn.addView(tv);

        return btn;
    }

    private View makeSpacer(int h) {
        View v = new View(getActivity());
        v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, h));
        return v;
    }
}
