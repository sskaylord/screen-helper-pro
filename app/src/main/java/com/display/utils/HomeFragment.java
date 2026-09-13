package com.display.utils;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment {
    int dp(int v){return(int)(v*getResources().getDisplayMetrics().density);}

    @Override public View onCreateView(LayoutInflater li, ViewGroup c, Bundle s) {
        ScrollView sv=new ScrollView(requireContext());sv.setBackgroundColor(MainActivity.BG);
        sv.setLayoutParams(new ViewGroup.LayoutParams(-1,-1));
        LinearLayout r=new LinearLayout(requireContext());r.setOrientation(LinearLayout.VERTICAL);r.setBackgroundColor(MainActivity.BG);

        // Banner
        FrameLayout bf=new FrameLayout(requireContext());
        bf.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(450)));
        ImageView bi=new ImageView(requireContext());
        try{bi.setImageResource(R.drawable.splash_bg);bi.setScaleType(ImageView.ScaleType.CENTER_CROP);}
        catch(Exception e){bi.setBackgroundColor(0xFF3D1040);}
        bf.addView(bi,new FrameLayout.LayoutParams(-1,-1));

        // Top bar
        LinearLayout tb=new LinearLayout(requireContext());tb.setOrientation(LinearLayout.HORIZONTAL);
        tb.setGravity(Gravity.CENTER_VERTICAL);tb.setPadding(dp(16),dp(20),dp(16),0);
        FrameLayout.LayoutParams tlp=new FrameLayout.LayoutParams(-1,-2);tlp.gravity=Gravity.TOP;tb.setLayoutParams(tlp);
        TextView logo=new TextView(requireContext());logo.setText("\uD83D\uDC51 AZUREHUB");logo.setTextColor(Color.WHITE);
        logo.setTextSize(20);logo.setTypeface(Typeface.DEFAULT_BOLD);
        logo.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1f));tb.addView(logo);
        TextView bell=new TextView(requireContext());bell.setText("\uD83D\uDD14");bell.setTextSize(18);bell.setPadding(0,0,dp(12),0);tb.addView(bell);
        TextView gear=new TextView(requireContext());gear.setText("\u2699");gear.setTextColor(Color.WHITE);gear.setTextSize(18);tb.addView(gear);
        bf.addView(tb);

        // Subtitle
        LinearLayout sub=new LinearLayout(requireContext());sub.setOrientation(LinearLayout.VERTICAL);sub.setPadding(dp(16),0,0,0);
        FrameLayout.LayoutParams slp=new FrameLayout.LayoutParams(-2,-2);slp.gravity=Gravity.TOP|Gravity.START;slp.topMargin=dp(50);sub.setLayoutParams(slp);
        TextView s1=new TextView(requireContext());s1.setText("\uD83D\uDCE6 Virtual App");s1.setTextColor(0xFFF9A8D4);s1.setTextSize(12);sub.addView(s1);
        TextView s2=new TextView(requireContext());s2.setText("More Apps  More Freedom");s2.setTextColor(MainActivity.TD);s2.setTextSize(11);sub.addView(s2);
        bf.addView(sub);

        // PCE
        TextView pce=new TextView(requireContext());pce.setText("Play\nClone\nEnjoy \u2661");pce.setTextColor(Color.WHITE);
        pce.setTextSize(18);pce.setTypeface(Typeface.DEFAULT_BOLD);pce.setPadding(dp(16),0,0,0);
        FrameLayout.LayoutParams plp=new FrameLayout.LayoutParams(-2,-2);plp.gravity=Gravity.BOTTOM|Gravity.START;plp.bottomMargin=dp(20);pce.setLayoutParams(plp);
        bf.addView(pce);
        r.addView(bf);

        // Spoofer card
        LinearLayout card=new LinearLayout(requireContext());card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);card.setPadding(dp(16),dp(14),dp(16),dp(14));
        LinearLayout.LayoutParams clp=new LinearLayout.LayoutParams(-1,-2);clp.setMargins(dp(16),dp(12),dp(16),0);card.setLayoutParams(clp);
        GradientDrawable cbg=new GradientDrawable();cbg.setColor(MainActivity.CARD);cbg.setCornerRadius(dp(16));card.setBackground(cbg);
        TextView ci=new TextView(requireContext());ci.setText("\uD83D\uDE08");ci.setTextSize(26);ci.setPadding(0,0,dp(12),0);card.addView(ci);
        LinearLayout ct=new LinearLayout(requireContext());ct.setOrientation(LinearLayout.VERTICAL);
        ct.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1f));
        TextView c1=new TextView(requireContext());c1.setText("Spoofer");c1.setTextColor(MainActivity.TW);c1.setTextSize(15);c1.setTypeface(Typeface.DEFAULT_BOLD);ct.addView(c1);
        TextView c2=new TextView(requireContext());c2.setText("Random Android ID\nDevice Spoofing");c2.setTextColor(MainActivity.TD);c2.setTextSize(11);ct.addView(c2);
        card.addView(ct);
        TextView ca=new TextView(requireContext());ca.setText("\u203A");ca.setTextColor(MainActivity.TD);ca.setTextSize(22);card.addView(ca);
        card.setOnClickListener(v->{if(getActivity()!=null)((MainActivity)getActivity()).switchTabPublic(1);});
        r.addView(card);

        // FAB
        FrameLayout ff=new FrameLayout(requireContext());ff.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(70)));
        TextView fab=new TextView(requireContext());fab.setText("+");fab.setTextColor(Color.WHITE);fab.setTextSize(26);
        fab.setTypeface(Typeface.DEFAULT_BOLD);fab.setGravity(Gravity.CENTER);
        GradientDrawable fb=new GradientDrawable();fb.setColor(MainActivity.PINK);fb.setCornerRadius(dp(30));fab.setBackground(fb);
        FrameLayout.LayoutParams flp=new FrameLayout.LayoutParams(dp(56),dp(56));flp.gravity=Gravity.END|Gravity.CENTER_VERTICAL;flp.rightMargin=dp(16);fab.setLayoutParams(flp);
        fab.setOnClickListener(v->startActivity(new Intent(getActivity(),ProfileManager.class)));
        ff.addView(fab);r.addView(ff);

        View sp=new View(requireContext());sp.setLayoutParams(new LinearLayout.LayoutParams(-1,dp(30)));r.addView(sp);
        sv.addView(r);return sv;
    }
}
