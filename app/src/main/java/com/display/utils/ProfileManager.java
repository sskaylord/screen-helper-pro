package com.display.utils;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.widget.*;
import com.display.utils.engine.VCore;
import java.io.FileWriter;
import java.util.*;

public class ProfileManager extends Activity {
    static final int BG=0xFF1A0A1E,CARD=0xFF2D1233,PINK=0xFFEC4899,TW=0xFFFCE7F3,TD=0xFFD4A0B0;
    String filter="all";

    void log(String m){Log.e("PM",m);try{FileWriter fw=new FileWriter(Environment.getExternalStorageDirectory()+"/Download/crash_display.log",true);fw.write(System.currentTimeMillis()+" "+m+"\n");fw.close();}catch(Exception ignored){}}

    @Override protected void onCreate(Bundle s){
        super.onCreate(s);log("onCreate");
        try{
            getWindow().setStatusBarColor(BG);getWindow().setNavigationBarColor(BG);
            LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(BG);
            LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.HORIZONTAL);top.setGravity(Gravity.CENTER_VERTICAL);top.setPadding(20,16,20,16);
            TextView back=new TextView(this);back.setText("\u2190");back.setTextColor(TW);back.setTextSize(24);back.setPadding(10,0,20,0);
            back.setOnClickListener(v->finish());top.addView(back);
            TextView title=new TextView(this);title.setText("Uyg Klonla");title.setTextColor(TW);title.setTextSize(20);title.setTypeface(Typeface.DEFAULT_BOLD);top.addView(title);
            root.addView(top);
            LinearLayout tabs=new LinearLayout(this);tabs.setOrientation(LinearLayout.HORIZONTAL);tabs.setPadding(20,10,20,10);
            String[][] cats={{"T\u00fcm Uygular","all"},{"Oyunlar","game"},{"Sosyal","social"},{"Di\u011fer","other"}};
            for(int i=0;i<cats.length;i++){
                final String f=cats[i][1];
                TextView tab=new TextView(this);tab.setText(cats[i][0]);tab.setTextColor(i==0?Color.WHITE:TD);tab.setTextSize(13);tab.setPadding(20,8,20,8);
                GradientDrawable tb=new GradientDrawable();tb.setColor(i==0?PINK:CARD);tb.setCornerRadius(16);tab.setBackground(tb);
                LinearLayout.LayoutParams tlp=new LinearLayout.LayoutParams(-2,-2);tlp.rightMargin=12;tab.setLayoutParams(tlp);
                tab.setOnClickListener(v->{filter=f;recreate();});tabs.addView(tab);
            }
            root.addView(tabs);
            try{ImageView banner=new ImageView(this);banner.setImageResource(R.drawable.splash_bg);banner.setScaleType(ImageView.ScaleType.CENTER_CROP);
                LinearLayout.LayoutParams blp=new LinearLayout.LayoutParams(-1,400);blp.leftMargin=20;blp.rightMargin=20;blp.bottomMargin=10;banner.setLayoutParams(blp);root.addView(banner);}catch(Exception ignored){}
            ScrollView scroll=new ScrollView(this);scroll.setLayoutParams(new LinearLayout.LayoutParams(-1,0,1f));
            LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);list.setPadding(20,10,20,20);
            List<AppInfo> apps=getApps();
            for(AppInfo a:apps){
                if(!"all".equals(filter)){if("game".equals(filter)&&!a.isGame)continue;if("social".equals(filter)&&!a.isSocial)continue;if("other".equals(filter)&&(a.isGame||a.isSocial))continue;}
                addRow(list,a);
            }
            scroll.addView(list);root.addView(scroll);setContentView(root);
            log("onCreate done apps="+apps.size());
        }catch(Exception e){log("CRASH: "+e);Toast.makeText(this,"Hata: "+e.getMessage(),Toast.LENGTH_LONG).show();finish();}
    }

    static class AppInfo{String name,pkg;android.graphics.drawable.Drawable icon;boolean isGame,isSocial;}

    List<AppInfo> getApps(){
        List<AppInfo> res=new ArrayList<>();
        PackageManager pm=getPackageManager();
        Intent mi=new Intent(Intent.ACTION_MAIN,null);mi.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> infos=pm.queryIntentActivities(mi,0);
        Set<String> social=new HashSet<>(Arrays.asList("com.whatsapp","com.instagram.android","com.zhiliaoapp.musically","org.telegram.messenger","com.facebook.katana","com.twitter.android","com.discord"));
        Set<String> games=new HashSet<>(Arrays.asList("com.axlebolt.standoff2","com.tencent.ig","com.riotgames.valorant","com.pubg.imobile","com.supercell.clashofclans","com.mojang.minecraftpe","com.roblox.client"));
        for(ResolveInfo ri:infos){
            if(ri.activityInfo==null)continue;
            String pkg=ri.activityInfo.packageName;
            if(pkg.equals(getPackageName()))continue;
            if(pkg.startsWith("com.android.")&&!pkg.equals("com.android.vending"))continue;
            AppInfo a=new AppInfo();a.name=ri.loadLabel(pm).toString();a.pkg=pkg;a.icon=ri.loadIcon(pm);
            a.isSocial=social.contains(pkg);a.isGame=games.contains(pkg);
            res.add(a);
        }
        Collections.sort(res,(a,b)->a.name.compareToIgnoreCase(b.name));
        return res;
    }

    void addRow(LinearLayout p,AppInfo a){
        LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(20,16,20,16);
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=10;row.setLayoutParams(lp);
        GradientDrawable bg=new GradientDrawable();bg.setColor(CARD);bg.setCornerRadius(14);row.setBackground(bg);
        ImageView iv=new ImageView(this);iv.setImageDrawable(a.icon);iv.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        LinearLayout.LayoutParams ilp=new LinearLayout.LayoutParams(80,80);ilp.rightMargin=16;iv.setLayoutParams(ilp);row.addView(iv);
        LinearLayout tx=new LinearLayout(this);tx.setOrientation(LinearLayout.VERTICAL);tx.setLayoutParams(new LinearLayout.LayoutParams(0,-2,1f));
        TextView t1=new TextView(this);t1.setText(a.name);t1.setTextColor(TW);t1.setTextSize(15);t1.setTypeface(Typeface.DEFAULT_BOLD);tx.addView(t1);
        TextView t2=new TextView(this);t2.setText(a.isGame?"Oyun":a.isSocial?"Sosyal":"Uygulama");t2.setTextColor(TD);t2.setTextSize(12);tx.addView(t2);
        row.addView(tx);
        TextView btn=new TextView(this);btn.setText("Klonla");btn.setTextColor(Color.WHITE);btn.setTextSize(13);btn.setTypeface(Typeface.DEFAULT_BOLD);btn.setPadding(24,10,24,10);btn.setGravity(Gravity.CENTER);
        GradientDrawable bb=new GradientDrawable();bb.setColor(PINK);bb.setCornerRadius(18);btn.setBackground(bb);
        btn.setOnClickListener(v->onAction(a.name,a.pkg));row.addView(btn);p.addView(row);
    }

    void onAction(String name,String pkg){
        log("onAction: "+name+" / "+pkg);
        Toast.makeText(this,name+" klonlan\u0131yor...",Toast.LENGTH_SHORT).show();
        try{
            log("step1: VCore.init");
            VCore.get().init(getApplicationContext());
            log("step2: installApp");
            boolean ok=VCore.get().installApp(pkg);
            log("step3: installed="+ok);
            if(!ok){Toast.makeText(this,"Kurulum ba\u015far\u0131s\u0131z",Toast.LENGTH_LONG).show();return;}
            log("step4: launchApp");
            VCore.get().launchApp(pkg);
            log("step5: launched OK");
            if(pkg.equals("com.axlebolt.standoff2")){
                new Handler(Looper.getMainLooper()).postDelayed(()->{
                    try{OverlayPanel p=DisplayCore.getPanel();if(p!=null){p.show();log("step6: overlay shown");}}
                    catch(Exception e){log("step6 err: "+e);}
                },3000);
            }
        }catch(Exception e){log("FATAL: "+e);Toast.makeText(this,"Hata: "+e.getMessage(),Toast.LENGTH_LONG).show();}
    }
}
