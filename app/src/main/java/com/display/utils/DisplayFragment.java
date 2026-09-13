package com.display.utils;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.fragment.app.Fragment;
import java.util.UUID;

public class DisplayFragment extends Fragment {
    int dp(int v){return(int)(v*getResources().getDisplayMetrics().density);}
    TextView idTxt;

    LinearLayout card(){
        LinearLayout c=new LinearLayout(requireContext());c.setPadding(dp(14),dp(12),dp(14),dp(12));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(8);c.setLayoutParams(lp);
        GradientDrawable bg=new GradientDrawable();bg.setColor(MainActivity.CARD);bg.setCornerRadius(dp(12));c.setBackground(bg);return c;
    }

    TextView btn(String t){
        TextView b=new TextView(requireContext());b.setText(t);b.setTextColor(Color.WHITE);b.setTextSize(15);
        b.setTypeface(Typeface.DEFAULT_BOLD);b.setGravity(Gravity.CENTER);b.setPadding(0,dp(14),0,dp(14));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(12);b.setLayoutParams(lp);
        GradientDrawable bg=new GradientDrawable();bg.setColor(MainActivity.PINK);bg.setCornerRadius(dp(14));b.setBackground(bg);return b;
    }

    @Override public View onCreateView(LayoutInflater li, ViewGroup c, Bundle s) {
        ScrollView sv=new ScrollView(requireContext());sv.setBackgroundColor(MainActivity.BG);
        LinearLayout r=new LinearLayout(requireContext());r.setOrientation(LinearLayout.VERTICAL);
        r.setBackgroundColor(MainActivity.BG);r.setPadding(dp(16),dp(16),dp(16),dp(16));

        TextView title=new TextView(requireContext());title.setText("Spoofer");title.setTextColor(MainActivity.TW);
        title.setTextSize(20);title.setTypeface(Typeface.DEFAULT_BOLD);title.setPadding(0,0,0,dp(16));r.addView(title);

        // ID card
        LinearLayout ic=card();ic.setOrientation(LinearLayout.HORIZONTAL);ic.setGravity(Gravity.CENTER_VERTICAL);
        TextView ii=new TextView(requireContext());ii.setText("\uD83D\uDE08");ii.setTextSize(24);ii.setPadding(0,0,dp(12),0);ic.addView(ii);
        LinearLayout it=new LinearLayout(requireContext());it.setOrientation(LinearLayout.VERTICAL);
        it.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1f));
        TextView i1=new TextView(requireContext());i1.setText("Rastgele Android ID");i1.setTextColor(MainActivity.TW);i1.setTextSize(14);i1.setTypeface(Typeface.DEFAULT_BOLD);it.addView(i1);
        TextView i2=new TextView(requireContext());i2.setText("Her a\u00e7\u0131l\u0131\u015fta yeni ID olu\u015fturur");i2.setTextColor(MainActivity.TD);i2.setTextSize(11);it.addView(i2);
        ic.addView(it);
        TextView tg=new TextView(requireContext());tg.setText("\u25CF");tg.setTextColor(MainActivity.PINK);tg.setTextSize(20);ic.addView(tg);
        r.addView(ic);

        // ID row
        LinearLayout ir=card();ir.setOrientation(LinearLayout.HORIZONTAL);ir.setGravity(Gravity.CENTER_VERTICAL);
        TextView il=new TextView(requireContext());il.setText("Android ID");il.setTextColor(MainActivity.TD);il.setTextSize(13);
        il.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1f));ir.addView(il);
        idTxt=new TextView(requireContext());idTxt.setText(UUID.randomUUID().toString().substring(0,18)+"...");
        idTxt.setTextColor(MainActivity.TW);idTxt.setTextSize(12);ir.addView(idTxt);r.addView(ir);

        TextView gb=btn("+ Rastgele ID Olu\u015ftur");
        gb.setOnClickListener(v->{idTxt.setText(UUID.randomUUID().toString().substring(0,18)+"...");
            Toast.makeText(requireContext(),"Yeni ID olu\u015fturuldu",Toast.LENGTH_SHORT).show();});
        r.addView(gb);

        String[][] rows={{"Cihaz Modeli","Random"},{"Manufacturer","Random"},{"Android Versiyon","Random"},{"IMEI (SIM)","Random"},{"MAC Adresi","Random"}};
        for(String[] row:rows){
            LinearLayout rr=card();rr.setOrientation(LinearLayout.HORIZONTAL);rr.setGravity(Gravity.CENTER_VERTICAL);
            TextView n=new TextView(requireContext());n.setText(row[0]);n.setTextColor(MainActivity.TW);n.setTextSize(13);
            n.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1f));rr.addView(n);
            TextView val=new TextView(requireContext());val.setText(row[1]);val.setTextColor(MainActivity.TD);val.setTextSize(12);rr.addView(val);
            TextView ar=new TextView(requireContext());ar.setText("\u203A");ar.setTextColor(MainActivity.TD);ar.setTextSize(16);ar.setPadding(dp(8),0,0,0);rr.addView(ar);
            r.addView(rr);
        }

        TextView ab=btn("Uygula");
        ab.setOnClickListener(v->Toast.makeText(requireContext(),"Spoofing uyguland\u0131",Toast.LENGTH_SHORT).show());
        r.addView(ab);
        sv.addView(r);return sv;
    }
}
