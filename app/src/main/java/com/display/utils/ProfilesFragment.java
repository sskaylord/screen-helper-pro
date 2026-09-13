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

public class ProfilesFragment extends Fragment {
    private static final int BG = 0xFF1A0A1E;
    private static final int CARD = 0xFF2D1233;
    private static final int PINK = 0xFFEC4899;
    private static final int TW = 0xFFFCE7F3;
    private static final int TD = 0xFFD4A0B0;

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
        title.setText("Ekstra Spoofer Ayarlar\u0131");
        title.setTextColor(TW);
        title.setTextSize(20);
        title.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        title.setPadding(0, 0, 0, dp(16));
        root.addView(title);

        // Toggle rows
        String[][] toggles = {
            {"Konum Spoofing", "GPS konumunu de\u011fi\u015ftir", "true"},
            {"Dolan\u0131m (Roaming)", "Sanal a\u011f konumu", "false"},
            {"WiFi MAC Spoofing", "MAC adresini de\u011fi\u015ftir", "false"},
            {"Bluetooth MAC Spoofing", "MAC adresini de\u011fi\u015ftir", "false"},
            {"Cihaz Kimli\u011fi Spoofing", "Cihaz bilgilerini de\u011fi\u015ftir", "true"},
        };

        for (String[] t : toggles) {
            LinearLayout row = makeCard();
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);

            LinearLayout texts = new LinearLayout(requireContext());
            texts.setOrientation(LinearLayout.VERTICAL);
            texts.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView name = new TextView(requireContext());
            name.setText(t[0]);
            name.setTextColor(TW);
            name.setTextSize(14);
            name.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            texts.addView(name);

            TextView desc = new TextView(requireContext());
            desc.setText(t[1]);
            desc.setTextColor(TD);
            desc.setTextSize(11);
            texts.addView(desc);

            row.addView(texts);

            boolean on = "true".equals(t[2]);
            TextView toggle = new TextView(requireContext());
            toggle.setText(on ? "\u25CF" : "\u25CB");
            toggle.setTextColor(on ? PINK : TD);
            toggle.setTextSize(22);
            toggle.setTag(on);
            toggle.setOnClickListener(v -> {
                boolean isOn = Boolean.TRUE.equals(v.getTag());
                ((TextView) v).setText(isOn ? "\u25CB" : "\u25CF");
                ((TextView) v).setTextColor(isOn ? TD : PINK);
                v.setTag(!isOn);
            });
            row.addView(toggle);

            root.addView(row);
        }

        // Uygulama Verisi Temizle
        LinearLayout cleanRow = makeCard();
        cleanRow.setOrientation(LinearLayout.HORIZONTAL);
        cleanRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView cleanIcon = new TextView(requireContext());
        cleanIcon.setText("\uD83D\uDDD1");
        cleanIcon.setTextSize(20);
        cleanIcon.setPadding(0, 0, dp(12), 0);
        cleanRow.addView(cleanIcon);

        LinearLayout cleanTexts = new LinearLayout(requireContext());
        cleanTexts.setOrientation(LinearLayout.VERTICAL);
        cleanTexts.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView cleanName = new TextView(requireContext());
        cleanName.setText("Uygulama Verisi Temizle");
        cleanName.setTextColor(TW);
        cleanName.setTextSize(14);
        cleanTexts.addView(cleanName);

        TextView cleanDesc = new TextView(requireContext());
        cleanDesc.setText("\u00d6nbellek ve veri temizle");
        cleanDesc.setTextColor(TD);
        cleanDesc.setTextSize(11);
        cleanTexts.addView(cleanDesc);

        cleanRow.addView(cleanTexts);

        TextView cleanArr = new TextView(requireContext());
        cleanArr.setText("\u203A");
        cleanArr.setTextColor(TD);
        cleanArr.setTextSize(18);
        cleanRow.addView(cleanArr);

        cleanRow.setOnClickListener(v -> Toast.makeText(requireContext(), "Veriler temizlendi", Toast.LENGTH_SHORT).show());
        root.addView(cleanRow);

        // Apply all button
        TextView applyAll = new TextView(requireContext());
        applyAll.setText("T\u00fcm\u00fcn\u00fc Uygula");
        applyAll.setTextColor(Color.WHITE);
        applyAll.setTextSize(15);
        applyAll.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        applyAll.setGravity(Gravity.CENTER);
        applyAll.setPadding(0, dp(14), 0, dp(14));
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        alp.topMargin = dp(16);
        applyAll.setLayoutParams(alp);
        GradientDrawable abg = new GradientDrawable();
        abg.setColor(PINK);
        abg.setCornerRadius(dp(14));
        applyAll.setBackground(abg);
        applyAll.setOnClickListener(v -> Toast.makeText(requireContext(), "T\u00fcm ayarlar uyguland\u0131", Toast.LENGTH_SHORT).show());
        root.addView(applyAll);

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

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density); }
}
