package com.display.utils;

import android.content.Context;
import android.graphics.*;
import android.os.Handler;
import android.os.Looper;
import android.view.View;

public class EspView extends View {
    private final OverlayPanel panel;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean running;
    private final Paint boxP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint skelP = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hpFill = new Paint();
    private final Paint hpBg = new Paint();
    private float[][] entities = {};

    public EspView(Context ctx, OverlayPanel p) {
        super(ctx); panel = p; setLayerType(LAYER_TYPE_HARDWARE, null);
        boxP.setColor(0xFFEC4899); boxP.setStyle(Paint.Style.STROKE); boxP.setStrokeWidth(2f);
        skelP.setColor(0xCCFFFFFF); skelP.setStyle(Paint.Style.STROKE); skelP.setStrokeWidth(1.5f);
        hpFill.setStyle(Paint.Style.FILL); hpBg.setColor(0x88000000); hpBg.setStyle(Paint.Style.FILL);
    }

    @Override protected void onDraw(Canvas c) {
        if (!panel.espEnabled && !panel.skeletonEnabled && !panel.healthEnabled) return;
        for (float[] e : entities) {
            if (e.length < 6) continue;
            float x=e[0],y=e[1],w=e[2],h=e[3],hp=e[4],mhp=Math.max(e[5],1f);
            if (panel.espEnabled) c.drawRect(x,y,x+w,y+h,boxP);
            if (panel.healthEnabled) {
                float r=hp/mhp, bh=h*r, bx=x-7f;
                c.drawRect(bx,y,bx+4f,y+h,hpBg);
                hpFill.setColor(Color.rgb((int)(255*(1-r)),(int)(255*r),0));
                c.drawRect(bx,y+h-bh,bx+4f,y+h,hpFill);
            }
            if (panel.skeletonEnabled) drawSkel(c,x,y,w,h);
        }
    }

    private void drawSkel(Canvas c, float x, float y, float w, float h) {
        float cx=x+w/2f, hr=w*0.13f;
        c.drawCircle(cx,y+hr,hr,skelP);
        c.drawLine(cx,y+hr*2.2f,cx,y+h*0.54f,skelP);
        c.drawLine(x+w*0.15f,y+h*0.38f,x+w*0.85f,y+h*0.38f,skelP);
        c.drawLine(x+w*0.15f,y+h*0.38f,x+w*0.05f,y+h*0.5f,skelP);
        c.drawLine(x+w*0.85f,y+h*0.38f,x+w*0.95f,y+h*0.5f,skelP);
        c.drawLine(cx,y+h*0.54f,x+w*0.37f,y+h*0.73f,skelP);
        c.drawLine(cx,y+h*0.54f,x+w*0.63f,y+h*0.73f,skelP);
        c.drawLine(x+w*0.37f,y+h*0.73f,x+w*0.25f,y+h,skelP);
        c.drawLine(x+w*0.63f,y+h*0.73f,x+w*0.75f,y+h,skelP);
    }

    public void startRender() { running=true; handler.post(loop); }
    public void stopRender() { running=false; handler.removeCallbacks(loop); }
    private final Runnable loop = new Runnable() {
        public void run() { if (!running) return; invalidate(); handler.postDelayed(this, 16L); }
    };
}
