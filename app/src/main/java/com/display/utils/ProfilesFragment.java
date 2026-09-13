package com.display.utils;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.fragment.app.Fragment;

public class ProfilesFragment extends Fragment {
    int dp(int v){return(int)(v*getResources().getDisplayMetrics().density);}

    LinearLayout card(){
        LinearLayout c=new LinearLayout(requireContext());c.setPadding(dp(14),dp(12),dp(14),dp(12));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=dp(8);c.setLayoutParams(lp);
        GradientDrawable bg=new GradientDrawable();bg.setColor(MainActivity.CARD);bg.setCornerRadius(dp(12));c.setBackground(bg);return c;
    }

    @Override public View onCreateView(LayoutInflater li, ViewGroup c, Bundle s) {
        ScrollView sv=new ScrollView(requireContext());sv.setBackgroundColor(MainActivity.BG);
        LinearLayout r=new LinearLayout(requireContext());r.setOrientation(LinearLayout.VERTICAL);
        r.setBackgroundColor(MainActivity.BG);r.setPadding(dp(16),dp(16),dp(16),dp(16));

        TextView title=new TextView(requireContext());title.setText("Ekstra Spoofer Ayarlar\u0131");
        title.setTextColor(MainActivity.TW);title.setTextSize(20);title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(0,0,0,dp(16));r.addView(title);

        String[][] toggles={{"Konum Spoofing","GPS konumunu de\u011fi\u015ftir","1"},{"Dolan\u0131m (Roaming)","Sanal a\u011f konumu","0"},
            {"WiFi MAC Spoofing","MAC adresini de\u011fi\u015ftir","0"},{"Bluetooth MAC Spoofing","MAC adresini de\u011fi\u015ftir","0"},
            {"Cihaz Kimli\u011fi Spoofing","Cihaz bilgilerini de\u011fi\u015ftir","1"}};

        for(String[] t:toggles){
            LinearLayout row=card();row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);
            LinearLayout tx=new LinearLayout(requireContext());tx.setOrientation(LinearLayout.VERTICAL);
            tx.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1f));
            TextView nm=new TextView(requireContext());nm.setText(t[0]);nm.setTextColor(MainActivity.TW);nm.setTextSize(14);nm.setTypeface(Typeface.DEFAULT_BOLD);tx.addView(nm);
            TextView ds=new TextView(requireContext());ds.setText(t[1]);ds.setTextColor(MainActivity.TD);ds.setTextSize(11);tx.addView(ds);
            row.addView(tx);
            boolean on="1".equals(t[2]);
            TextView tg=new TextView(requireContext());tg.setText(on?"\u25CF":"\u25CB");tg.setTextColor(on?MainActivity.PINK:MainActivity.TD);tg.setTextSize(22);tg.setTag(on);
            tg.setOnClickListener(v->{boolean o=Boolean.TRUE.equals(v.getTag());((TextView)v).setText(o?"\u25CB":"\u25CF");
                ((TextView)v).setTextColor(o?MainActivity.TD:MainActivity.PINK);v.setTag(!o);});
            row.addView(tg);r.addView(row);
        }

        // Clean row
        LinearLayout cr=card();cr.setOrientation(LinearLayout.HORIZONTAL);cr.setGravity(Gravity.CENTER_VERTICAL);
        TextView ci=new TextView(requireContext());ci.setText("\uD83D\uDDD1");ci.setTextSize(20);ci.setPadding(0,0,dp(12),0);cr.addView(ci);
        LinearLayout ct=new LinearLayout(requireContext());ct.setOrientation(LinearLayout.VERTICAL);
        ct.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1f));
        TextView cn=new TextView(requireContext());cn.setText("Uygulama Verisi Temizle");cn.setTextColor(MainActivity.TW);cn.setTextSize(14);ct.addView(cn);
        TextView cd=new TextView(requireContext());cd.setText("\u00d6nbellek ve veri temizle");cd.setTextColor(MainActivity.TD);cd.setTextSize(11);ct.addView(cd);
        cr.addView(ct);
        TextView ca=new TextView(requireContext());ca.setText("\u203A");ca.setTextColor(MainActivity.TD);ca.setTextSize(18);cr.addView(ca);
        cr.setOnClickListener(v->Toast.makeText(requireContext(),"Veriler temizlendi",Toast.LENGTH_SHORT).show());
        r.addView(cr);

        // Apply all
        TextView aa=new TextView(requireContext());aa.setText("T\u00fcm\u00fcn\u00fc Uygula");aa.setTextColor(Color.WHITE);aa.setTextSize(15);
        aa.setTypeface(Typeface.DEFAULT_BOLD);aa.setGravity(Gravity.CENTER);aa.setPadding(0,dp(14),0,dp(14));
        LinearLayout.LayoutParams alp=new LinearLayout.LayoutParams(-1,-2);alp.topMargin=dp(16);aa.setLayoutParams(alp);
        GradientDrawable abg=new GradientDrawable();abg.setColor(MainActivity.PINK);abg.setCornerRadius(dp(14));aa.setBackground(abg);
        aa.setOnClickListener(v->Toast.makeText(requireContext(),"T\u00fcm ayarlar uyguland\u0131",Toast.LENGTH_SHORT).show());
        r.addView(aa);
        sv.addView(r);return sv;
    }
}
