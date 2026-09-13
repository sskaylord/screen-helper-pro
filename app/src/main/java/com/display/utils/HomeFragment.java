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

        // === BANNER (anime girl image) ===
        FrameLayout bannerFrame = new FrameLayout(requireContext());
        LinearLayout.LayoutParams bannerLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 900);
        bannerFrame.setLayoutParams(bannerLp);

        ImageView bannerImg = new ImageView(requireContext());
        try {
            bannerImg.setImageResource(R.drawable.splash_bg);
            bannerImg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        } catch (Exception ignored) {
            bannerImg.setBackgroundColor(0xFF3D1040);
        }
        bannerFrame.addView(bannerImg, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));

        // Top overlay: AZUREHUB title + icons
        LinearLayout topOverlay = new LinearLayout(requireContext());
        topOverlay.setOrientation(LinearLayout.HORIZONTAL);
        topOverlay.setGravity(Gravity.CENTER_VERTICAL);
        topOverlay.setPadding(30, 40, 30, 0);
        FrameLayout.LayoutParams topLp = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        topLp.gravity = Gravity.TOP;
        topOverlay.setLayoutParams(topLp);

        TextView logo = new TextView(requireContext());
        logo.setText("\uD83D\uDC51 AZUREHUB");
        logo.setTextColor(Color.WHITE);
        logo.setTextSize(22);
        logo.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        logo.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        topOverlay.addView(logo);

        TextView bell = new TextView(requireContext());
        bell.setText("\uD83D\uDD14");
        bell.setTextSize(20);
        bell.setPadding(0, 0, 20, 0);
        topOverlay.addView(bell);

        TextView gear = new TextView(requireContext());
        gear.setText("\u2699\uFE0F");
        gear.setTextSize(20);
        topOverlay.addView(gear);

        bannerFrame.addView(topOverlay);

        // Subtitle overlay
        LinearLayout subOverlay = new LinearLayout(requireContext());
        subOverlay.setOrientation(LinearLayout.VERTICAL);
        subOverlay.setPadding(30, 0, 0, 0);
        FrameLayout.LayoutParams subLp = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        subLp.gravity = Gravity.TOP | Gravity.START;
        subLp.topMargin = 80;
        subOverlay.setLayoutParams(subLp);

        TextView sub1 = new TextView(requireContext());
        sub1.setText("\uD83D\uDCE6 Virtual App");
        sub1.setTextColor(PINK_LIGHT);
        sub1.setTextSize(13);
        subOverlay.addView(sub1);

        TextView sub2 = new TextView(requireContext());
        sub2.setText("More Apps  More Freedom");
        sub2.setTextColor(TD);
        sub2.setTextSize(12);
        subOverlay.addView(sub2);

        bannerFrame.addView(subOverlay);

        // "Play Clone Enjoy" text
        TextView pce = new TextView(requireContext());
        pce.setText("Play\nClone\nEnjoy \u2661");
        pce.setTextColor(Color.WHITE);
        pce.setTextSize(20);
        pce.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        pce.setPadding(30, 0, 0, 0);
        FrameLayout.LayoutParams pceLp = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        pceLp.gravity = Gravity.BOTTOM | Gravity.START;
        pceLp.bottomMargin = 40;
        pce.setLayoutParams(pceLp);
        bannerFrame.addView(pce);

        root.addView(bannerFrame);

        // === SPOOFER CARD ===
        LinearLayout spooferCard = new LinearLayout(requireContext());
        spooferCard.setOrientation(LinearLayout.HORIZONTAL);
        spooferCard.setGravity(Gravity.CENTER_VERTICAL);
        spooferCard.setPadding(30, 24, 30, 24);
        LinearLayout.LayoutParams scLp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        scLp.setMargins(30, 20, 30, 0);
        spooferCard.setLayoutParams(scLp);

        GradientDrawable scBg = new GradientDrawable();
        scBg.setColor(CARD);
        scBg.setCornerRadius(20);
        spooferCard.setBackground(scBg);

        TextView spIcon = new TextView(requireContext());
        spIcon.setText("\uD83D\uDE08");
        spIcon.setTextSize(28);
        spIcon.setPadding(0, 0, 20, 0);
        spooferCard.addView(spIcon);

        LinearLayout spTexts = new LinearLayout(requireContext());
        spTexts.setOrientation(LinearLayout.VERTICAL);
        spTexts.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView sp1 = new TextView(requireContext());
        sp1.setText("Spoofer");
        sp1.setTextColor(TW);
        sp1.setTextSize(16);
        sp1.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        spTexts.addView(sp1);

        TextView sp2 = new TextView(requireContext());
        sp2.setText("Random Android ID\nDevice Spoofing");
        sp2.setTextColor(TD);
        sp2.setTextSize(12);
        spTexts.addView(sp2);

        spooferCard.addView(spTexts);

        TextView spArrow = new TextView(requireContext());
        spArrow.setText("\u203A");
        spArrow.setTextColor(TD);
        spArrow.setTextSize(24);
        spooferCard.addView(spArrow);

        spooferCard.setOnClickListener(v -> startActivity(new android.content.Intent(getActivity(), com.display.utils.ProfileManager.class)));
        root.addView(spooferCard);

        // === FAB (+ button) ===
        FrameLayout fabFrame = new FrameLayout(requireContext());
        fabFrame.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 120));

        TextView fab = new TextView(requireContext());
        fab.setText("+");
        fab.setTextColor(Color.WHITE);
        fab.setTextSize(28);
        fab.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        fab.setGravity(Gravity.CENTER);
        GradientDrawable fabBg = new GradientDrawable();
        fabBg.setColor(PINK);
        fabBg.setCornerRadius(60);
        fab.setBackground(fabBg);
        FrameLayout.LayoutParams fabLp = new FrameLayout.LayoutParams(110, 110);
        fabLp.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
        fabLp.rightMargin = 30;
        fab.setLayoutParams(fabLp);
        fab.setOnClickListener(v -> startActivity(new Intent(getActivity(), ProfileManager.class)));
        fabFrame.addView(fab);
        root.addView(fabFrame);

        // Bottom spacer
        View spacer = new View(requireContext());
        spacer.setLayoutParams(new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 60));
        root.addView(spacer);

        scroll.addView(root);
        return scroll;
    }
}
