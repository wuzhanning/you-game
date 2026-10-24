package com.wuzhanning.yougame.nativegames;

import android.graphics.*;
import android.os.Bundle;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.Arrays;

/** Original vector artwork, a fixed-size effect pool, and a single 30 Hz Canvas surface. */
final class GameView extends View {
    private static final int INK=0xff20344b, WHITE=0xfffffcf3, TEAL=0xff48bdb5, GOLD=0xffffc55c, RED=0xffeb737d;
    private static final int[] COLORS={0xff69c8c3,0xff78b9e5,0xff9a91cf,0xffefaf76,0xffe88194,0xffa7bf6b,0xffc698d1};
    private static final String[] HEARTS={"", "♥", "♥ ♥", "♥ ♥ ♥"};
    private static final Typeface FONT=Typeface.create("sans-serif-medium",Typeface.NORMAL);
    final Game game;
    final NativeActivity host;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path=new Path();
    private final LinearGradient iceSky=new LinearGradient(0,100,0,630,0xff8ed9e5,0xffd6f2ec,Shader.TileMode.CLAMP);
    private final LinearGradient runSky=new LinearGradient(0,120,0,620,0xff83d7dc,0xffe7edcc,Shader.TileMode.CLAMP);
    private final LinearGradient towerSky=new LinearGradient(0,100,0,650,0xffa4a3d5,0xfff4c5b6,Shader.TileMode.CLAMP);
    private final String[] bodyLabels=new String[Game.CAPACITY];
    private final int[] bodyCodes=new int[Game.CAPACITY];
    private String number="",score="",clock="",objective="",progressText="",level="",next="",reserve="",combo="",cooldown="",heading="",targetNumber="";
    private String accessible="";
    private int oldValue=-1,oldScore=-1,oldTime=-1,oldNext=-1,oldReserve=-1,oldProgress=-1,oldCombo=-1,oldCooldown=-1;
    private float scale=1,offsetX,offsetY,touchX,touchY;
    private long previous;
    private int checkpoint;
    private boolean running,ready;
    private final Choreographer.FrameCallback frame=new Choreographer.FrameCallback() {
        @Override public void doFrame(long now) {
            if(!running) return;
            if(previous==0) previous=now;
            long elapsed=(now-previous)/1000000;
            if(elapsed>=30) {
                int ms=(int)Math.min(100,elapsed); previous=now;
                game.update(ms); refresh();
                if(game.sound!=0) { host.sound(game.sound); game.sound=0; }
                invalidate(); checkpoint+=ms;
                if(checkpoint>=5000) { checkpoint=0; host.saveGame(); }
                if(game.state!=0) { running=false; host.finished(); }
            }
            if(running) Choreographer.getInstance().postFrameCallback(this);
        }
    };
    GameView(NativeActivity host,Game game) {
        super(host); this.host=host; this.game=game; ready=game.elapsed==0 && game.state==0;
        setDefaultFocusHighlightEnabled(false); setFocusable(true); setFocusableInTouchMode(true);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        Arrays.fill(bodyCodes,Integer.MIN_VALUE); level="第 "+game.level+" 关";
        objective=game.mode==Game.MERGE?"合成 "+game.target:game.mode==Game.STACK?"完成 "+game.target+" 次落塔":game.mode==Game.RUN?"冲线时数字 ≥ "+game.target:game.mode==Game.DUEL?"击败 3 名对手":game.mode==Game.COLOR?"成长到 "+game.target+" · 东侧出口":"带回 ≥ "+game.target+" 条小鱼";
        heading=level+"  ·  "+objective; targetNumber=Integer.toString(game.target);
        setClickable(true); refresh();
    }
    void start() {
        if(ready || game.state!=0) { invalidate(); return; }
        if(!running) { running=true; previous=0; Choreographer.getInstance().postFrameCallback(frame); }
    }
    void stop() { running=false; Choreographer.getInstance().removeFrameCallback(frame); game.releaseAll(); heldDown=false; }
    @Override protected void onDetachedFromWindow() { stop(); super.onDetachedFromWindow(); }
    private void refresh() {
        boolean changed=oldValue!=game.value || oldScore!=game.score || oldTime!=(int)game.time;
        if(oldValue!=game.value) { oldValue=game.value; number=Integer.toString(game.value); }
        if(oldScore!=game.score) { oldScore=game.score; score="得分 "+game.score; }
        if(oldTime!=(int)game.time) { oldTime=(int)game.time; clock=oldTime+"s"; }
        if(oldNext!=game.next) { oldNext=game.next; next=Integer.toString(game.next); }
        if(oldReserve!=game.reserve) { oldReserve=game.reserve; reserve=Integer.toString(game.reserve); }
        int progress=game.mode==Game.STACK?game.drops:game.mode==Game.DUEL?game.enemies:game.mode==Game.COLOR?Math.min(game.target,game.value):(int)game.progress;
        if(oldProgress!=progress) { oldProgress=progress; progressText=game.mode==Game.STACK?progress+" / "+game.target:game.mode==Game.DUEL?progress+" / 3":game.mode==Game.COLOR?progress+" / "+game.target:(game.mode==Game.PENGUIN?progress/10:progress)+" m"; }
        if(oldCombo!=game.combo) { oldCombo=game.combo; combo="连击 ×"+game.combo; }
        if(oldCooldown!=(int)Math.ceil(game.cooldown)) { oldCooldown=(int)Math.ceil(game.cooldown); cooldown=oldCooldown==0?"确认：冲刺就绪":"冲刺冷却 "+oldCooldown+"s"; }
        for(int i=0;i<game.bodies.length;i++) {
            Game.Body b=game.bodies[i]; int code=b.number*32+b.kind;
            if(bodyCodes[i]!=code) { bodyCodes[i]=code; bodyLabels[i]=b.kind==Game.ADD?(b.number>=0?"+":"")+b.number:b.kind==Game.MULTIPLY?"×"+b.number:Integer.toString(b.number); }
        }
        if(changed) { accessible=NativeActivity.TITLES[game.mode]+"，"+objective+"，数字 "+number+"，"+score+"，体力 "+game.hp; setContentDescription(accessible); }
    }
    @Override protected void onSizeChanged(int w,int h,int ow,int oh) { scale=Math.min(w/1280f,h/720f); offsetX=(w-scale*1280)/2; offsetY=(h-scale*720)/2; }
    private void rect(Canvas c,float x,float y,float w,float h,int color,float radius) { p.setShader(null); p.setStyle(Paint.Style.FILL); p.setColor(color); c.drawRoundRect(x,y,x+w,y+h,radius,radius,p); }
    private void circle(Canvas c,float x,float y,float r,int color) { p.setShader(null); p.setColor(color); p.setStyle(Paint.Style.FILL); c.drawCircle(x,y,r,p); }
    private void oval(Canvas c,float x,float y,float w,float h,int color) { p.setShader(null); p.setColor(color); c.drawOval(x-w/2,y-h/2,x+w/2,y+h/2,p); }
    private void line(Canvas c,float x,float y,float x2,float y2,int color,float width) { p.setShader(null); p.setColor(color); p.setStrokeWidth(width); p.setStrokeCap(Paint.Cap.ROUND); c.drawLine(x,y,x2,y2,p); }
    private void text(Canvas c,String s,float x,float y,float size,int color,boolean center) {
        if(s==null) return; p.setShader(null); p.setTypeface(FONT); p.setTextSize(size); p.setColor(color); p.setTextAlign(center?Paint.Align.CENTER:Paint.Align.LEFT); c.drawText(s,x,y,p);
    }
    private void fitted(Canvas c,String s,float x,float y,float size,int color,float maxWidth) {
        p.setTextSize(size); float width=p.measureText(s); text(c,s,x,y,size*Math.min(1,maxWidth/Math.max(1,width)),color,true);
    }
    private void quad(Canvas c,int color,float x1,float y1,float x2,float y2,float x3,float y3,float x4,float y4) {
        p.setShader(null); p.setColor(color); path.reset(); path.moveTo(x1,y1); path.lineTo(x2,y2); path.lineTo(x3,y3); path.lineTo(x4,y4); path.close(); c.drawPath(path,p);
    }
    private int tint(int color,float factor) { return Color.rgb(Math.min(255,(int)(Color.red(color)*factor)),Math.min(255,(int)(Color.green(color)*factor)),Math.min(255,(int)(Color.blue(color)*factor))); }
    private int color(int n) { return COLORS[(31-Integer.numberOfLeadingZeros(Math.max(1,n)))%COLORS.length]; }
    @Override protected void onDraw(Canvas c) {
        c.drawColor(0xff15283c); c.save(); c.translate(offsetX,offsetY); c.scale(scale,scale);
        rect(c,0,0,1280,720,0xfff7f3e8,0);
        c.save(); c.clipRect(40,125,1240,635);
        if(game.shake>0) c.translate((float)Math.sin(game.elapsed*70)*game.shake*.4f,0);
        if(game.mode==Game.MERGE) blaster(c);
        else if(game.mode==Game.STACK) tower(c);
        else if(game.mode==Game.RUN) runner(c);
        else if(game.mode==Game.DUEL || game.mode==Game.COLOR) arena(c);
        else penguinWorld(c);
        effects(c); c.restore(); hud(c);
        if(ready) {
            rect(c,280,270,720,190,0xee20344b,28);
            text(c,NativeActivity.TITLES[game.mode],640,325,36,WHITE,true);
            text(c,"确认出发",640,384,30,GOLD,true);
            text(c,NativeActivity.HINTS[game.mode][0],640,426,21,0xffbcdbdd,true);
        }
        c.restore();
    }
    private void hud(Canvas c) {
        rect(c,40,25,1200,87,INK,24);
        text(c,NativeActivity.TITLES[game.mode],65,62,26,WHITE,false);
        text(c,heading,65,94,18,0xffb9d8d7,false);
        rect(c,650,40,205,13,0xff415367,7);
        float progress=game.mode==Game.MERGE?game.value/(float)game.target:game.mode==Game.STACK?game.drops/(float)game.target:game.mode==Game.DUEL?game.enemies/3f:game.mode==Game.COLOR?game.value/(float)game.target:game.progress/game.goal;
        rect(c,650,40,205*Math.min(1,progress),13,TEAL,7);
        text(c,game.mode==Game.MERGE?"合成进度":progressText,650,85,18,WHITE,false);
        text(c,clock,870,65,23,GOLD,false);
        text(c,HEARTS[game.hp],950,58,24,RED,false);
        text(c,score,950,91,18,WHITE,false);
        rect(c,1130,38,86,57,TEAL,16); text(c,number,1173,77,28,INK,true);
        rect(c,40,647,1200,49,INK,16);
        text(c,NativeActivity.HINTS[game.mode][0],59,678,18,WHITE,false);
        String hint=(game.mode==Game.DUEL || game.mode==Game.COLOR)?cooldown:"返回 / 菜单：暂停";
        p.setTypeface(FONT); p.setTextSize(18); p.setColor(GOLD); p.setTextAlign(Paint.Align.RIGHT); c.drawText(hint,1219,678,p);
        if(game.combo>1) { rect(c,1010,133,200,38,0xddfff6dd,15); text(c,combo,1110,160,22,INK,true); }
    }
    private void sphere(Canvas c,float x,float y,float r,int n,String label,boolean glow) {
        int color=color(n);
        if(glow) circle(c,x,y,r+7,0x40ffffff);
        oval(c,x+3,y+r*.83f,r*1.7f,r*.4f,0x220e2334);
        circle(c,x,y,r,color); circle(c,x-r*.25f,y-r*.28f,r*.66f,tint(color,1.08f));
        oval(c,x-r*.32f,y-r*.45f,r*.4f,r*.19f,0x99ffffff);
        fitted(c,label,x,y+r*.3f,r*.82f,INK,r*1.55f);
    }
    private void blaster(Canvas c) {
        rect(c,40,125,1200,510,0xfff4d6bc,24);
        circle(c,135,230,60,0xffeab9a5); circle(c,1120,470,105,0xfff0c5a5);
        for(int i=0;i<5;i++) { circle(c,120+i*35,420+(i%2)*45,9,0xffdca78e); circle(c,1100+i*18,250+(i%3)*25,6,0xffdca78e); }
        rect(c,290,135,700,465,0xffa97f79,28); rect(c,300,145,680,445,0xffffeed6,23);
        line(c,316,545,964,545,0xffd29583,2);
        text(c,"警戒线",1020,550,17,0xff9a6570,false);
        text(c,"借墙反弹",116,320,22,INK,true); text(c,"相同数字相碰",1120,355,21,INK,true); text(c,"就会翻倍",1120,386,21,INK,true);
        for(int i=0;i<game.bodies.length;i++) {
            Game.Body b=game.bodies[i]; if(b.active) sphere(c,b.x,b.y,b.w,b.number,bodyLabels[i],b.kind==Game.SHOT);
        }
        boolean shot=false; for(Game.Body b:game.bodies) shot|=b.active && b.kind==Game.SHOT;
        if(!shot) {
            float x=640,y=590,dx=(float)Math.sin(game.aim),dy=-(float)Math.cos(game.aim);
            for(int i=0;i<20;i++) {
                x+=dx*17; y+=dy*17;
                if(x<328 || x>952) { dx=-dx; x=Game.clamp(x,328,952); }
                if(y<174) break;
                circle(c,x,y,3,0xffceac91);
            }
        }
        oval(c,640,615,120,22,0x330e2334); rect(c,590,581,100,55,INK,22);
        c.save(); c.rotate((float)Math.toDegrees(game.aim),640,600); rect(c,623,550,34,58,TEAL,9); c.restore();
        sphere(c,640,591,27,game.next,next,true);
        sphere(c,1060,570,24,game.reserve,reserve,false); text(c,"↑ 换弹",1135,577,19,INK,true);
    }
    private void block(Canvas c,float x,float y,float width,float height,int n,String label,boolean active) {
        int color=color(n); float d=10;
        quad(c,tint(color,.72f),x+width/2,y-height/2,x+width/2+d,y-height/2-d,x+width/2+d,y+height/2-d,x+width/2,y+height/2);
        quad(c,tint(color,1.12f),x-width/2,y-height/2,x-width/2+d,y-height/2-d,x+width/2+d,y-height/2-d,x+width/2,y-height/2);
        rect(c,x-width/2,y-height/2,width,height,color,4);
        fitted(c,label,x,y+height*.23f,height*.65f,INK,width-12);
        if(active) line(c,x-width/2+6,y-height/2+3,x+width/2-6,y-height/2+3,0xccffffff,2);
    }
    private void tower(Canvas c) {
        p.setShader(towerSky); c.drawRect(40,125,1240,635,p); p.setShader(null);
        circle(c,1050,230,55,0xffffe6b0);
        for(int i=0;i<15;i++) {
            float x=45+i*90;
            rect(c,x,470-(i%4)*24,65,180,0xff999bc0,3);
            for(int j=0;j<3;j++) rect(c,x+12,490-(i%4)*24+j*22,9,10,0xffc3c0d6,2);
        }
        Game.Body top=game.topBlock(); float camera=top==null?0:Math.max(0,420-top.y);
        rect(c,315,610+camera,650,35,0xff53657e,10);
        if(top!=null) for(int j=0;j<7;j++) circle(c,top.x,235+j*45,2,0xfff9ead9);
        for(int i=0;i<game.bodies.length;i++) { Game.Body b=game.bodies[i]; if(b.active) block(c,b.x,b.y+camera,b.w,b.h,b.number,bodyLabels[i],false); }
        if(top!=null) block(c,game.px,game.py+camera,top.w,42,game.next,next,true);
        text(c,"完美重叠 = 恢复塔宽",640,160,21,INK,true);
        text(c,"左右微调 · 确认落塔",180,570,18,INK,true);
    }
    private float depth(float z) { return Game.clamp(1-z/90,0,1); }
    private float roadY(float z) { float t=depth(z); return 215+t*t*385; }
    private float roadX(float lane,float z) { float t=depth(z); return 640+(lane-1)*(35+t*t*275); }
    private void runner(Canvas c) {
        p.setShader(runSky); c.drawRect(40,125,1240,635,p); p.setShader(null);
        circle(c,1060,187,42,0xffffefb5);
        for(int i=0;i<9;i++) {
            float x=60+i*145;
            rect(c,x,180-(i%3)*18,85,130,0xff78b6b4,5);
            rect(c,x+15,192-(i%3)*18,55,6,0xffa5dbce,2);
        }
        quad(c,0xffc6d68b,40,270,1240,270,1240,635,40,635);
        quad(c,0xfff2c38e,585,215,695,215,1210,635,70,635);
        quad(c,0xffe8af7f,600,215,680,215,1155,635,125,635);
        for(int l=0;l<2;l++) for(int j=0;j<10;j++) {
            float z=j*10-game.progress%10;
            if(z<0 || z>89) continue;
            line(c,roadX(l+.5f,z),roadY(z),roadX(l+.5f,z+4),roadY(z+4),0xfff9e6c4,Math.max(1,depth(z)*5));
        }
        for(int i=game.bodies.length-1;i>=0;i--) {
            Game.Body b=game.bodies[i]; float z=b.y-game.progress;
            if(!b.active || z<0 || z>84) continue;
            float s=.16f+depth(z)*depth(z)*.84f,x=roadX(b.x,z),y=roadY(z);
            if(b.kind==Game.FOOD) { sphere(c,x,y-30*s,27*s,b.number,bodyLabels[i],false); }
            else if(b.kind==Game.STAR) star(c,x,y-95*s,21*s,GOLD);
            else if(b.kind==Game.WALL) {
                block(c,x,y-32*s,68*s,64*s,16,"",false);
                line(c,x-17*s,y-48*s,x+17*s,y-15*s,WHITE,5*s);
                line(c,x+17*s,y-48*s,x-17*s,y-15*s,WHITE,5*s);
            } else if(b.kind==Game.LOW) {
                rect(c,x-58*s,y-35*s,116*s,30*s,0xffe87874,4*s);
                for(int j=0;j<4;j++) line(c,x-48*s+j*30*s,y-30*s,x-30*s+j*30*s,y-10*s,WHITE,6*s);
            } else if(b.kind==Game.HIGH || b.kind==Game.ADD || b.kind==Game.MULTIPLY) {
                int color=b.kind==Game.HIGH?0xffa17fa9:b.kind==Game.ADD && b.number<0?RED:TEAL;
                rect(c,x-64*s,y-125*s,12*s,125*s,color,3*s); rect(c,x+52*s,y-125*s,12*s,125*s,color,3*s);
                rect(c,x-64*s,y-125*s,128*s,40*s,color,6*s);
                text(c,b.kind==Game.HIGH?"↓":bodyLabels[i],x,y-96*s,27*s,INK,true);
                if(b.kind!=Game.HIGH) rect(c,x-52*s,y-85*s,104*s,80*s,0x3063d6ce,0);
            }
        }
        float finish=game.goal-game.progress;
        if(finish<85) {
            float s=.16f+depth(finish)*depth(finish)*.84f,x=640,y=roadY(finish);
            rect(c,x-150*s,y-180*s,300*s,160*s,0xffead8a6,10*s);
            text(c,"终点数字墙",x,y-135*s,27*s,INK,true); text(c,targetNumber,x,y-65*s,48*s,INK,true);
        }
        float x=roadX(game.lane,0),y=590-game.jumpHeight()*.85f;
        oval(c,x,600,75-game.jumpHeight()*.2f,16,0x330e2334);
        if(game.invincible==0 || ((int)(game.elapsed*10))%2==0) {
            if(game.roll>0) {
                circle(c,x,y-15,28,TEAL); line(c,x-15,y-15,x+15,y-15,WHITE,4);
            } else {
                float gait=(float)Math.sin(game.elapsed*18)*12;
                line(c,x-11,y-17,x-19-gait,y+8,INK,10); line(c,x+11,y-17,x+19+gait,y+8,INK,10);
                line(c,x-18,y-57,x-34-gait,y-35,0xffffd5a7,9); line(c,x+18,y-57,x+34+gait,y-35,0xffffd5a7,9);
                rect(c,x-23,y-70,46,55,TEAL,18); circle(c,x,y-90,23,0xffffd5a7);
                rect(c,x-24,y-113,48,17,0xffe87874,9);
                circle(c,x-7,y-89,2.5f,INK); circle(c,x+7,y-89,2.5f,INK);
            }
            rect(c,x-44,y-157,88,36,INK,14); text(c,number,x,y-131,25,WHITE,true);
        }
    }
    private void blob(Canvas c,float x,float y,float r,int color,String n,boolean player,boolean danger) {
        oval(c,x,y+r*.9f,r*1.8f,r*.42f,0x250e2334);
        float wobble=1+(float)Math.sin(game.elapsed*5+x*.02f)*.035f;
        oval(c,x,y,r*2*wobble,r*2/wobble,color);
        oval(c,x-r*.25f,y-r*.35f,r*.8f,r*.4f,0x44ffffff);
        circle(c,x-r*.24f,y-r*.24f,r*.12f,WHITE); circle(c,x+r*.24f,y-r*.24f,r*.12f,WHITE);
        circle(c,x-r*.22f,y-r*.24f,r*.055f,INK); circle(c,x+r*.26f,y-r*.24f,r*.055f,INK);
        fitted(c,n,x,y+r*.48f,r*.75f,INK,r*1.6f);
        if(player) { line(c,x-r*.25f,y-r-13,x+r*.25f,y-r-13,INK,3); circle(c,x,y-r-18,3,INK); }
        if(danger) { p.setColor(0x66e96070); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(3); c.drawCircle(x,y,r+7+(float)Math.sin(game.elapsed*5)*3,p); p.setStyle(Paint.Style.FILL); }
    }
    private float cameraX() { return game.mode==Game.COLOR?Game.clamp(game.px-640,0,1120):game.mode==Game.PENGUIN?game.px-260:0; }
    private float cameraY() { return game.mode==Game.COLOR?Game.clamp(game.py-380,0,800):0; }
    private void arena(Canvas c) {
        boolean duel=game.mode==Game.DUEL; float cx=cameraX(),cy=cameraY();
        rect(c,40,125,1200,510,duel?0xffd8d9c3:0xffcde0b3,24);
        if(duel) {
            oval(c,640,414,1135,470,0xffb9cdbd); oval(c,640,414,1030,380,0xffcad7bd);
            p.setColor(0xfff4f0d2); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(3); c.drawOval(120,205,1160,598,p); c.drawCircle(640,400,95,p); p.setStyle(Paint.Style.FILL);
            text(c,"实时运算竞技场",640,173,19,INK,true);
        } else {
            for(int i=0;i<42;i++) {
                float x=(i*367)%2400-cx,y=180+(i*193)%1200-cy;
                if(x<40 || x>1240 || y<125 || y>635) continue;
                oval(c,x,y,20,8,0xffb8cf9d); line(c,x-3,y,x-5,y-8,0xffa4be8b,2); line(c,x+2,y,x+4,y-6,0xffa4be8b,2);
            }
            float gx=2280-cx,gy=780-cy; boolean unlocked=game.value>=game.target;
            if(gx<1270 && gy<700 && gy>80) {
                circle(c,gx,gy,75,unlocked?0xffa1d3b8:0xffc7cca7);
                p.setColor(unlocked?TEAL:0xff969c88); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(8); c.drawCircle(gx,gy,55,p); p.setStyle(Paint.Style.FILL);
                text(c,unlocked?"出口":"成长后开启",gx,gy+7,20,INK,true);
            }
        }
        for(int i=0;i<game.bodies.length;i++) {
            Game.Body b=game.bodies[i]; float x=b.x-cx,y=b.y-cy;
            if(!b.active || x<0 || x>1280 || y<90 || y>680 || b.kind==Game.FOOD && b.tag==1) continue;
            if(b.kind==Game.FOOD) {
                circle(c,x,y,b.w+3,0x40ffffff); circle(c,x,y,b.w,0xffefbf63); text(c,bodyLabels[i],x,y+5,b.w*1.2f,INK,true);
            } else if(b.kind==Game.ROCK) {
                oval(c,x,y+12,b.w*2,b.h*1.4f,0xff9ca99d); oval(c,x-5,y-2,b.w*1.7f,b.h*1.45f,0xffc2c6b4);
                line(c,x-16,y-15,x+10,y-20,0xffe2dfc7,4);
            } else if(b.kind==Game.ADD || b.kind==Game.MULTIPLY) {
                int color=b.tag==1?0xffa8b9a9:TEAL;
                circle(c,x,y,b.w+7,0x40ffffff); circle(c,x,y,b.w,color); text(c,b.tag==1?"冷却":bodyLabels[i],x,y+9,25,INK,true);
            } else {
                boolean danger=b.kind==Game.RIVAL && b.number>=game.value;
                blob(c,x,y,b.w,b.kind==Game.FRIEND?0xff71c8ce:danger?RED:0xffe9a975,bodyLabels[i],false,danger);
            }
        }
        float px=game.px-cx,py=game.py-cy;
        if(game.boost>0) for(int i=1;i<5;i++) circle(c,px-game.vx*.013f*i,py-game.vy*.013f*i,game.playerRadius()*(1-i*.13f),0x3360bcb7);
        if(game.invincible==0 || ((int)(game.elapsed*10))%2==0) blob(c,px,py,game.playerRadius(),0xff57c2c2,number,true,false);
        if(!duel) {
            rect(c,1055,532,160,85,0xddfff8df,15);
            rect(c,1065,541,140,67,0xffc7d6ad,7);
            circle(c,1065+game.px/2400*140,541+(game.py-150)/1300*67,4,INK);
            circle(c,1198,574,5,GOLD);
            text(c,game.value>=game.target?"→ 东侧出口已开启":"先吃小数字，再向东探索",640,616,19,INK,true);
        }
    }
    private void star(Canvas c,float x,float y,float r,int color) {
        p.setShader(null); p.setColor(color); path.reset();
        for(int i=0;i<10;i++) { double a=-Math.PI/2+i*Math.PI/5; float rr=i%2==0?r:r*.45f,px=x+(float)Math.cos(a)*rr,py=y+(float)Math.sin(a)*rr; if(i==0) path.moveTo(px,py); else path.lineTo(px,py); }
        path.close(); c.drawPath(path,p);
    }
    private void fish(Canvas c,float x,float y,float r,String label) {
        oval(c,x,y,r*2.2f,r*1.35f,0xffefb45f);
        quad(c,0xffe79d50,x-r,y,x-r*1.75f,y-r*.8f,x-r*1.75f,y+r*.8f,x-r,y);
        circle(c,x+r*.6f,y-r*.18f,r*.1f,INK);
        fitted(c,label,x-1,y+r*.29f,r*.85f,INK,r*1.6f);
    }
    private void penguin(Canvas c,float x,float y,float r,boolean sliding) {
        c.save(); c.translate(x,y);
        if(game.phase==2) c.rotate(Game.clamp((game.vy+590)/1240,0,1)*360);
        if(sliding) c.rotate(-70);
        int coat=game.skin==0?0xff26435a:game.skin==1?0xff765c99:0xff497b74;
        oval(c,0,3,r*1.65f,r*2.3f,coat); oval(c,0,r*.18f,r*1.15f,r*1.7f,WHITE);
        float flap=(float)Math.sin(game.elapsed*12)*10;
        oval(c,-r*.9f,4,r*.5f,r*1.3f,coat); oval(c,r*.9f,4+flap*.15f,r*.5f,r*1.3f,coat);
        circle(c,-r*.23f,-r*.48f,r*.22f,WHITE); circle(c,r*.23f,-r*.48f,r*.22f,WHITE);
        circle(c,-r*.18f,-r*.48f,r*.085f,INK); circle(c,r*.28f,-r*.48f,r*.085f,INK);
        quad(c,GOLD,-r*.17f,-r*.28f,r*.24f,-r*.28f,r*.04f,-r*.02f,-r*.17f,-r*.28f);
        rect(c,-r*.72f,-r*.08f,r*1.4f,r*.21f,0xffe97978,5);
        quad(c,0xffe97978,-r*.5f,0,-r*1.6f,-r*.1f-flap*.2f,-r*1.45f,r*.15f,-r*.5f,r*.18f);
        oval(c,-r*.4f,r*1.08f,r*.7f,r*.25f,GOLD); oval(c,r*.4f,r*1.08f,r*.7f,r*.25f,GOLD);
        c.restore();
    }
    private void penguinWorld(Canvas c) {
        float camera=cameraX(); p.setShader(iceSky); c.drawRect(40,125,1240,635,p); p.setShader(null);
        circle(c,1080,190,40,0xfffff0c8);
        for(int layer=0;layer<2;layer++) {
            path.reset(); path.moveTo(40,600);
            for(int i=-1;i<11;i++) {
                float x=i*170-(game.progress*(layer==0?.08f:.18f)%170),y=layer==0?330:410;
                path.lineTo(x,y+55); path.lineTo(x+85,y-65-(i%3)*20); path.lineTo(x+170,y+55);
            }
            path.lineTo(1240,635); path.lineTo(40,635); path.close(); p.setColor(layer==0?0xffb2dfe2:0xff91cad5); c.drawPath(path,p);
        }
        path.reset(); path.moveTo(40,635); path.lineTo(40,Game.ground(camera+40));
        for(int x=40;x<=1240;x+=24) path.lineTo(x,Game.ground(camera+x));
        path.lineTo(1240,635); path.close(); p.setColor(0xff77b8cb); c.drawPath(path,p);
        path.reset(); path.moveTo(40,Game.ground(camera+40));
        for(int x=64;x<=1240;x+=24) path.lineTo(x,Game.ground(camera+x));
        p.setColor(WHITE); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(18); c.drawPath(path,p); p.setStyle(Paint.Style.FILL);
        for(int chunk=0;chunk<4;chunk++) {
            float start=chunk*1300+600, end=start+125+Math.min(45,game.level*2),x=start-camera;
            if(x>1260 || end-camera<20) continue;
            float y=Game.ground(start);
            rect(c,x,y-8,end-start,635-y+10,0xff397b9d,0);
            line(c,x-5,y-5,x+2,y+45,0xffc6f1f5,7); line(c,end-camera+3,Game.ground(end)-6,end-camera-1,Game.ground(end)+40,0xffc6f1f5,7);
        }
        for(int i=0;i<game.bodies.length;i++) {
            Game.Body b=game.bodies[i]; float x=b.x-camera,y=b.y;
            if(!b.active || x<0 || x>1280) continue;
            if(b.kind==Game.FOOD) fish(c,x,y,b.w,bodyLabels[i]);
            else if(b.kind==Game.STAR) { circle(c,x,y,26+(float)Math.sin(game.elapsed*5)*3,0x44fff0c0); star(c,x,y,18,GOLD); }
            else if(b.kind==Game.LOW) {
                float floor=Game.ground(b.x);
                quad(c,0xff8bd3e2,x-34,floor,x-26,floor-56,x+15,floor-74,x+38,floor);
                quad(c,0xffb9f0f1,x-26,floor-56,x+15,floor-74,x+12,floor,x-5,floor);
                text(c,bodyLabels[i],x+2,floor-25,25,INK,true);
            } else if(b.kind==Game.HIGH) {
                float floor=Game.ground(b.x);
                rect(c,x-50,floor-92,18,92,0xffa1e2e6,4); rect(c,x+32,floor-92,18,92,0xffa1e2e6,4);
                rect(c,x-50,floor-92,100,30,0xffc0f1ef,8); text(c,"↓",x,floor-69,24,INK,true);
            }
        }
        if(game.goal-game.progress<900) {
            float x=game.goal+180-camera;
            line(c,x,Game.ground(game.goal+180),x,Game.ground(game.goal+180)-170,INK,6);
            quad(c,0xffe97978,x,Game.ground(game.goal+180)-170,x+70,Game.ground(game.goal+180)-150,x+70,Game.ground(game.goal+180)-105,x,Game.ground(game.goal+180)-125);
            text(c,"营地",x+40,Game.ground(game.goal+180)-137,18,WHITE,true);
        }
        float radius=Math.min(35,25+(float)Math.sqrt(game.value)*.5f);
        oval(c,260,Game.ground(game.px)+4,72,12,0x22416982);
        if(game.invincible==0 || ((int)(game.elapsed*10))%2==0) penguin(c,260,game.py,radius,heldDown);
        rect(c,222,game.py-radius-60,76,31,INK,12); text(c,number,260,game.py-radius-36,23,WHITE,true);
    }
    private boolean heldDown;
    private void effects(Canvas c) {
        float cx=cameraX(),cy=cameraY();
        float towerCamera=0;
        if(game.mode==Game.STACK && game.topBlock()!=null) towerCamera=Math.max(0,420-game.topBlock().y);
        for(int i=0;i<game.flife.length;i++) if(game.flife[i]>0) {
            float x=game.fx[i]-cx,y=game.fy[i]-cy+towerCamera;
            int alpha=(int)(Math.min(1,game.flife[i]*2)*255),color=(game.fcolor[i]&0x00ffffff)|(alpha<<24);
            rect(c,x-3,y-3,6,6,color,2);
        }
        if(game.floatingLife>0) {
            float x=game.floatingX-cx,y=game.floatingY-cy+towerCamera-42-(1.1f-game.floatingLife)*25;
            text(c,game.floating,x+1,y+2,25,WHITE,true); text(c,game.floating,x,y,25,INK,true);
        }
    }
    private void direction(int d,boolean repeat) { if(!ready) game.press(d,repeat); refresh(); invalidate(); }
    private void confirm() { if(ready) { ready=false; start(); } else game.confirm(); refresh(); invalidate(); host.saveGame(); }
    @Override public boolean onKeyDown(int code,KeyEvent event) {
        switch(code) {
            case KeyEvent.KEYCODE_DPAD_LEFT: direction(0,event.getRepeatCount()>0); return true;
            case KeyEvent.KEYCODE_DPAD_UP: direction(1,event.getRepeatCount()>0); return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT: direction(2,event.getRepeatCount()>0); return true;
            case KeyEvent.KEYCODE_DPAD_DOWN: heldDown=true; direction(3,event.getRepeatCount()>0); return true;
            case KeyEvent.KEYCODE_DPAD_CENTER: case KeyEvent.KEYCODE_ENTER: case KeyEvent.KEYCODE_BUTTON_A: if(event.getRepeatCount()==0) confirm(); return true;
            case KeyEvent.KEYCODE_BACK: case KeyEvent.KEYCODE_MENU: case KeyEvent.KEYCODE_BUTTON_START: case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE: if(event.getRepeatCount()==0) host.pauseMenu(); return true;
            default: return super.onKeyDown(code,event);
        }
    }
    @Override public boolean onKeyUp(int code,KeyEvent event) {
        int d=code==KeyEvent.KEYCODE_DPAD_LEFT?0:code==KeyEvent.KEYCODE_DPAD_UP?1:code==KeyEvent.KEYCODE_DPAD_RIGHT?2:code==KeyEvent.KEYCODE_DPAD_DOWN?3:-1;
        if(d>=0) { game.release(d); if(d==3) heldDown=false; return true; }
        return super.onKeyUp(code,event);
    }
    @Override public boolean onTouchEvent(MotionEvent e) {
        if(e.getAction()==MotionEvent.ACTION_DOWN) { touchX=e.getX(); touchY=e.getY(); return true; }
        if(e.getAction()==MotionEvent.ACTION_UP) {
            float dx=e.getX()-touchX,dy=e.getY()-touchY;
            if(Math.max(Math.abs(dx),Math.abs(dy))>30*getResources().getDisplayMetrics().density) {
                int d=Math.abs(dx)>Math.abs(dy)?(dx>0?2:0):(dy>0?3:1); direction(d,false); game.release(d);
            } else performClick(); return true;
        }
        return true;
    }
    @Override public boolean performClick() { super.performClick(); confirm(); return true; }
    @Override public void onInitializeAccessibilityNodeInfo(AccessibilityNodeInfo info) {
        super.onInitializeAccessibilityNodeInfo(info);
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_LEFT); info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_UP);
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_RIGHT); info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SCROLL_DOWN);
        info.addAction(new AccessibilityNodeInfo.AccessibilityAction(0x7f0a0001,"暂停游戏"));
    }
    @Override public boolean performAccessibilityAction(int action,Bundle args) {
        if(action==0x7f0a0001) { host.pauseMenu(); return true; }
        int d=action==android.R.id.accessibilityActionScrollLeft?0:action==android.R.id.accessibilityActionScrollUp?1:action==android.R.id.accessibilityActionScrollRight?2:action==android.R.id.accessibilityActionScrollDown?3:-1;
        if(d>=0) { direction(d,false); game.release(d); return true; }
        return super.performAccessibilityAction(action,args);
    }
}
