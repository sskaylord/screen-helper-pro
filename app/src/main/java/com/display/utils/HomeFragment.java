package com.display.utils;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment {
    private static final int BG = 0xFF1A0A1E;
    private static final int CARD = 0xFF2D1233;
    private static final int PINK = 0xFFEC4899;
    private static final int PINK_LIGHT = 0xFFF9A8D4;
    private static final int TW = 0xFFFCE7F3;
    private static final int TD = 0xFFD4A0B0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ScrollView scroll = new ScrollView(requireContext());
        scroll.setBackgroundColor(BG);
        scroll.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        // === BANNER ===
        FrameLayout bannerFrame = new FrameLayout(requireContext());
        LinearLayout.LayoutParams bannerLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(450));
        bannerFrame.setLayoutParams(bannerLp);

        ImageView bannerImg = new ImageView(requireContext());
        try {
            bannerImg.setImageResource(R.drawable.splash_bg);
            bannerImg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        } catch (Exception ignored) {
            GradientDrawable fallback = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF3D1040, 0xFF1A0A1E});
            bannerImg.setBackground(fallback);
        }
        bannerFrame.addView(bannerImg, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        // Top bar overlay
        LinearLayout topBar = new LinearLayout(requireContext());
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(dpToPx(16), dpToPx(20), dpToPx(16), 0);
        FrameLayout.LayoutParams topLp = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        topLp.gravity = Gravity.TOP;
        topBar.setLayoutParams(topLp);

        TextView logo = new TextView(requireContext());
        logo.setText("\uD83D\uDC51 AZUREHUB");
        logo.setTextColor(Color.WHITE);
        logo.setTextSize(20);
        logo.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        logo.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        topBar.addView(logo);

        TextView bell = new TextView(requireContext());
        bell.setText("\uD83D\uDD14");
        bell.setTextSize(18);
        bell.setPadding(0, 0, dpToPx(12), 0);
        topBar.addView(bell);

        TextView gear = new TextView(requireContext());
        gear.setText("\u2699");
        gear.setTextColor(Color.WHITE);
        gear.setTextSize(18);
        topBar.addView(gear);

        bannerFrame.addView(topBar);

        // Subtitle
        LinearLayout sub = new LinearLayout(requireContext());
        sub.setOrientation(LinearLayout.VERTICAL);
        sub.setPadding(dpToPx(16), 0, 0, 0);
        FrameLayout.LayoutParams subLp = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        subLp.gravity = Gravity.TOP | Gravity.START;
        subLp.topMargin = dpToPx(50);
        sub.setLayoutParams(subLp);

        TextView s1 = new TextView(requireContext());
        s1.setText("\uD83D\uDCE6 Virtual App");
        s1.setTextColor(PINK_LIGHT);
        s1.setTextSize(12);
        sub.addView(s1);

        TextView s2 = new TextView(requireContext());
        s2.setText("More Apps  More Freedom");
        s2.setTextColor(TD);
        s2.setTextSize(11);
        sub.addView(s2);

        bannerFrame.addView(sub);

        // Play Clone Enjoy
        TextView pce = new TextView(requireContext());
        pce.setText("Play\nClone\nEnjoy \u2661");
        pce.setTextColor(Color.WHITE);
        pce.setTextSize(18);
        pce.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        pce.setPadding(dpToPx(16), 0, 0, 0);
        FrameLayout.LayoutParams pceLp = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        pceLp.gravity = Gravity.BOTTOM | Gravity.START;
        pceLp.bottomMargin = dpToPx(20);
        pce.setLayoutParams(pceLp);
        bannerFrame.addView(pce);

        root.addView(bannerFrame);

        // === SPOOFER CARD ===
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dpToPx(16), dpToPx(14), dpToPx(16), dpToPx(14));
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(dpToPx(16), dpToPx(12), dpToPx(16), 0);
        card.setLayoutParams(cardLp);

        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(CARD);
        cardBg.setCornerRadius(dpToPx(16));
        card.setBackground(cardBg);

        TextView cardIcon = new TextView(requireContext());
        cardIcon.setText("\uD83D\uDE08");
        cardIcon.setTextSize(26);
        cardIcon.setPadding(0, 0, dpToPx(12), 0);
        card.addView(cardIcon);

        LinearLayout cardTexts = new LinearLayout(requireContext());
        cardTexts.setOrientation(LinearLayout.VERTICAL);
        cardTexts.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView ct1 = new TextView(requireContext());
        ct1.setText("Spoofer");
        ct1.setTextColor(TW);
        ct1.setTextSize(15);
        ct1.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        cardTexts.addView(ct1);

        TextView ct2 = new TextView(requireContext());
        ct2.setText("Random Android ID\nDevice Spoofing");
        ct2.setTextColor(TD);
        ct2.setTextSize(11);
        cardTexts.addView(ct2);

        card.addView(cardTexts);

        TextView arrow = new TextView(requireContext());
        arrow.setText("\u203A");
        arrow.setTextColor(TD);
        arrow.setTextSize(22);
        card.addView(arrow);

        card.setOnClickListener(v -> {
            if (getActivity() != null) {
                ((MainActivity) getActivity()).switchTabPublic(1);
            }
        });
        root.addView(card);

        // === FAB ===
        FrameLayout fabFrame = new FrameLayout(requireContext());
        fabFrame.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(70)));

        TextView fab = new TextView(requireContext());
        fab.setText("+");
        fab.setTextColor(Color.WHITE);
        fab.setTextSize(26);
        fab.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        fab.setGravity(Gravity.CENTER);
        GradientDrawable fabBg = new GradientDrawable();
        fabBg.setColor(PINK);
        fabBg.setCornerRadius(dpToPx(30));
        fab.setBackground(fabBg);
        FrameLayout.LayoutParams fabLp = new FrameLayout.LayoutParams(dpToPx(56), dpToPx(56));
        fabLp.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
        fabLp.rightMargin = dpToPx(16);
        fab.setLayoutParams(fabLp);
        fab.setOnClickListener(v -> startActivity(new Intent(getActivity(), ProfileManager.class)));
        fabFrame.addView(fab);
        root.addView(fabFrame);

        // Spacer
        View spacer = new View(requireContext());
        spacer.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(30)));
        root.addView(spacer);

        scroll.addView(root);
        return scroll;
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
}
