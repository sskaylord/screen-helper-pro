package com.display.utils;

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
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import java.util.UUID;

public class DisplayFragment extends Fragment {
    private static final int BG = 0xFF1A0A1E;
    private static final int CARD = 0xFF2D1233;
    private static final int PINK = 0xFFEC4899;
    private static final int TW = 0xFFFCE7F3;
    private static final int TD = 0xFFD4A0B0;

    private TextView androidIdText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ScrollView scroll = new ScrollView(requireContext());
        scroll.setBackgroundColor(BG);

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(16), dp(16), dp(16), dp(16));

        // Title
        TextView title = new TextView(requireContext());
        title.setText("Spoofer");
        title.setTextColor(TW);
        title.setTextSize(20);
        title.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        title.setPadding(0, 0, 0, dp(16));
        root.addView(title);

        // Random Android ID card
        LinearLayout idCard = makeCard();
        idCard.setOrientation(LinearLayout.HORIZONTAL);
        idCard.setGravity(Gravity.CENTER_VERTICAL);

        TextView idIcon = new TextView(requireContext());
        idIcon.setText("\uD83D\uDE08");
        idIcon.setTextSize(24);
        idIcon.setPadding(0, 0, dp(12), 0);
        idCard.addView(idIcon);

        LinearLayout idTexts = new LinearLayout(requireContext());
        idTexts.setOrientation(LinearLayout.VERTICAL);
        idTexts.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView idTitle = new TextView(requireContext());
        idTitle.setText("Rastgele Android ID");
        idTitle.setTextColor(TW);
        idTitle.setTextSize(14);
        idTitle.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        idTexts.addView(idTitle);

        TextView idSub = new TextView(requireContext());
        idSub.setText("Her a\u00e7\u0131l\u0131\u015fta yeni ID olu\u015fturur");
        idSub.setTextColor(TD);
        idSub.setTextSize(11);
        idTexts.addView(idSub);

        idCard.addView(idTexts);

        // Toggle placeholder
        TextView toggle = new TextView(requireContext());
        toggle.setText("\u25CF");
        toggle.setTextColor(PINK);
        toggle.setTextSize(20);
        idCard.addView(toggle);

        root.addView(idCard);

        // Android ID display
        LinearLayout idRow = makeCard();
        idRow.setOrientation(LinearLayout.HORIZONTAL);
        idRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView idLabel = new TextView(requireContext());
        idLabel.setText("Android ID");
        idLabel.setTextColor(TD);
        idLabel.setTextSize(13);
        idLabel.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        idRow.addView(idLabel);

        androidIdText = new TextView(requireContext());
        androidIdText.setText(UUID.randomUUID().toString().substring(0, 18) + "...");
        androidIdText.setTextColor(TW);
        androidIdText.setTextSize(12);
        idRow.addView(androidIdText);

        root.addView(idRow);

        // Generate button
        TextView genBtn = makeButton("+ Rastgele ID Olu\u015ftur");
        genBtn.setOnClickListener(v -> {
            androidIdText.setText(UUID.randomUUID().toString().substring(0, 18) + "...");
            Toast.makeText(requireContext(), "Yeni ID olu\u015fturuldu", Toast.LENGTH_SHORT).show();
        });
        root.addView(genBtn);

        // Device info rows
        String[][] rows = {
            {"Cihaz Modeli", "Random"},
            {"Manufacturer", "Random"},
            {"Android Versiyon", "Random"},
            {"IMEI (SIM)", "Random"},
            {"MAC Adresi", "Random"}
        };

        for (String[] row : rows) {
            LinearLayout r = makeCard();
            r.setOrientation(LinearLayout.HORIZONTAL);
            r.setGravity(Gravity.CENTER_VERTICAL);

            TextView name = new TextView(requireContext());
            name.setText(row[0]);
            name.setTextColor(TW);
            name.setTextSize(13);
            name.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            r.addView(name);

            TextView val = new TextView(requireContext());
            val.setText(row[1]);
            val.setTextColor(TD);
            val.setTextSize(12);
            r.addView(val);

            TextView arr = new TextView(requireContext());
            arr.setText("\u203A");
            arr.setTextColor(TD);
            arr.setTextSize(16);
            arr.setPadding(dp(8), 0, 0, 0);
            r.addView(arr);

            root.addView(r);
        }

        // Apply button
        TextView applyBtn = makeButton("Uygula");
        applyBtn.setOnClickListener(v -> Toast.makeText(requireContext(), "Spoofing uyguland\u0131", Toast.LENGTH_SHORT).show());
        root.addView(applyBtn);

        scroll.addView(root);
        return scroll;
    }

    private LinearLayout makeCard() {
        LinearLayout card = new LinearLayout(requireContext());
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(8);
        card.setLayoutParams(lp);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD);
        bg.setCornerRadius(dp(12));
        card.setBackground(bg);
        return card;
    }

    private TextView makeButton(String text) {
        TextView btn = new TextView(requireContext());
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(15);
        btn.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(0, dp(14), 0, dp(14));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(12);
        btn.setLayoutParams(lp);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(PINK);
        bg.setCornerRadius(dp(14));
        btn.setBackground(bg);
        return btn;
    }

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density); }
}
