package com.wuzhanning.yougame.nativegames;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.*;
import android.widget.*;

/** TV launcher and explicit pause/results dialogs; legacy game remains a separate Activity. */
public final class NativeActivity extends Activity {
    static final String[] TITLES={"数字弹射", "叠塔工坊", "数字疾跑", "运算竞技场", "红蓝吞噬", "企鹅冰原"};
    private static final String[] SUBTITLES={"借墙反弹 · 一发连锁翻倍", "看准时机 · 完美落点修复塔宽", "三道冲刺 · 跳栏滑行选运算门", "自由走位 · 抢运算环击败对手", "追逐与避让 · 长大后撤离", "起伏冰坡 · 二段跳越过冰缝"};
    static final String[][] HINTS={
        {"左右瞄准 · 确认发射 · 上键换弹"},
        {"确认落塔 · 左右微调移动方块"},
        {"左右换道 · 上 / 确认跳跃 · 下滑行"},
        {"方向键持续移动 · 确认短距离冲刺"},
        {"方向键自由移动 · 确认冲刺 · 东侧出口"},
        {"确认二段跳 · 左刹车 / 右加速 · 按住下滑行"}
    };
    private static final String[] DETAILS={
        "左右键瞄准，确认发射，上键交换当前弹与备用弹。数字球可以从侧墙反弹。\n\n相同数字相碰会翻倍，合成后的球还会吸收紧贴的相同数字，触发连锁。每 4 发，顶部球群下压一次；压过警戒线或 90 秒耗尽时失败。\n\n目标从合成 32 逐步增加至 256。先观察球群，再选择直射、反弹或换弹，别用不同数字堵住好位置。",
        "方块自动左右移动，确认后落下。左右键可微调移动速度。只有与塔顶重叠的部分能够留下，落空则结束。\n\n中心误差小于 13 时是完美落点，可以恢复 18 点塔宽。相同数字落在一起会合并为翻倍数字，不同数字则增加塔高。\n\n完成指定次数的成功落塔即可过关。连续完美落点可加分，90 秒内完成目标。",
        "左右键切换三条跑道，上或确认跳跃，下键滑行。蓝色数字直接相加；选择 +10 或 ×2 运算门，避开 −5。\n\n实心路障必须换道；红白矮栏要跳过；高横梁要滑行通过。空中的星星只有跳起才能拿到。障碍每次消耗 1 点体力并损失四分之一数字。\n\n每关是可预读的障碍段落，抵达终点时数字达到要求，才能击破数字墙。满体力通关可获三颗评价星。",
        "方向键连续移动，确认可短距离加速，冲刺冷却 2.4 秒。\n\n收集 +3 小数字，抢占 +8 和 ×2 运算环。运算环使用后冷却 7 秒。红方大于你时会追击，变得比你小时会逃跑；数字更大时碰到对手即可击败它并吸收数字。\n\n石柱会挡住双方，利用地形绕行。被更大对手撞到损失体力和部分数字。80 秒内连续击败 3 名对手获胜。",
        "方向键在连续世界中自由移动，确认短暂冲刺。黄色数字提供成长，蓝色同伴可以直接吸收；红色对手只有数字小于你时才可吞噬。\n\n更大的红方会追赶你，小红方会逃跑。先在安全处收集数字，再选择可追上的猎物。被大红方碰到会损失体力和部分数字。\n\n达到目标数字后，去地图东侧的圆形传送门撤离。右下角小地图标出你和出口的位置。100 秒内完成任务。",
        "企鹅在起伏冰坡上自动滑行，确认或上键跳跃，空中再按一次可二段跳。左键刹车、右键加速；下键快速下落，按住下键可低身穿过冰拱。\n\n冰缝需要提前跳起，空中鱼群与星星奖励准确的起跳时机。比你小的冰块可以撞碎吸收，大冰块需要跳过。摔入冰缝或撞上危险损失体力。\n\n带够小鱼到达营地过关。星星跨关保存，每 5 颗换一种企鹅配色。"
    };
    private android.media.SoundPool audio;
    private final int[] sounds=new int[6];
    private void initSound() {
        android.media.AudioAttributes attr=new android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_GAME).setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION).build();
        audio=new android.media.SoundPool.Builder().setMaxStreams(4).setAudioAttributes(attr).build();
        String[] names={"", "collect", "launch", "hit", "merge", "win"};
        for(int i=1;i<names.length;i++) try { android.content.res.AssetFileDescriptor fd=getAssets().openFd("sounds/"+names[i]+".wav"); sounds[i]=audio.load(fd,1); fd.close(); } catch(java.io.IOException e) { android.util.Log.w("YouGame", "Missing sound",e); }
    }
    void sound(int event) { if(audio!=null && event>0 && event<sounds.length && !prefs.getBoolean("mute",false)) audio.play(sounds[event],.3f,.3f,1,0,1); }
    private SharedPreferences prefs;
    private GameView view;
    private AlertDialog dialog;
    private boolean foreground;
    private long lastSave;
    private int selected;
    private Button[] cards;
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(5894);
        prefs=getSharedPreferences("native-arcade-v2",MODE_PRIVATE);
        initSound();
        selected=prefs.getInt("selected",0);
        showLibrary();
        if(saved!=null && saved.getInt("mode",-1)>=0) {
            int mode=saved.getInt("mode");
            if(mode<6) {
                Game restored=Game.restore(prefs.getString("save-"+mode,null));
                if(restored!=null) { view=new GameView(this,restored); setContentView(view); view.requestFocus(); }
            }
        }
    }
    private int dp(float n) { return Math.round(n*getResources().getDisplayMetrics().density); }
    private GradientDrawable background(int color,boolean stroke) {
        GradientDrawable bg=new GradientDrawable(); bg.setColor(color); bg.setCornerRadius(dp(12));
        if(stroke) bg.setStroke(dp(3),0xffffd58b); return bg;
    }
    private TextView label(String text,int size,int color) {
        TextView t=new TextView(this); t.setText(text); t.setTextSize(size); t.setTextColor(color); return t;
    }
    private void showLibrary() {
        if(view!=null) { view.stop(); saveNow(); view=null; }
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(0xff0e1829);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(40),dp(24),dp(40),dp(20)); scroll.addView(root);
        root.addView(label("YOU GAME  /  迷你街机",14,0xff71cbd5));
        TextView title=label("六种挑战，马上开玩",29,0xffedf4fd); title.setTypeface(null,Typeface.BOLD); title.setPadding(0,dp(5),0,dp(4)); root.addView(title);
        root.addView(label("方向键选择 · 确认进入 · 连续动作玩法 · 无广告离线游玩",14,0xff9eb2ca));
        cards=new Button[6];
        for(int row=0;row<3;row++) {
            LinearLayout strip=new LinearLayout(this); root.addView(strip);
            for(int col=0;col<2;col++) {
                final int mode=row*2+col;
                int level=prefs.getInt("level-"+mode,1), best=prefs.getInt("best-"+mode,0);
                Button b=new Button(this); cards[mode]=b; b.setId(100+mode); b.setFocusableInTouchMode(true); b.setAllCaps(false); b.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL); b.setTextColor(0xffedf4fd); b.setTextSize(17); b.setPadding(dp(19),dp(8),dp(15),dp(8));
                b.setText("0"+(mode+1)+"  "+TITLES[mode]+"   /   第 "+level+" 关\n"+SUBTITLES[mode]);
                b.setDefaultFocusHighlightEnabled(false);
                b.setBackground(background(0xff18283d,false));
                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(0,dp(86),1); lp.setMargins(0,dp(10),dp(col==0?12:0),0); strip.addView(b,lp);
                b.setOnFocusChangeListener((v,focused)-> { b.setBackground(background(focused?0xff304761:0xff18283d,focused)); b.setTextColor(focused?0xffffdf9c:0xffedf4fd); if(focused) selected=mode; });
                b.setOnClickListener(v-> choose(mode));
                b.setNextFocusLeftId(100+row*2); b.setNextFocusRightId(100+row*2+1);
                b.setNextFocusUpId(row==0?100+col:100+mode-2);
                b.setNextFocusDownId(row==2?200:100+mode+2);
            }
        }
        Button legacy=new Button(this); legacy.setId(200); legacy.setAllCaps(false); legacy.setText("星球碰碰乐  ↗   打开原有游戏"); legacy.setTextSize(15); legacy.setTextColor(0xff71cbd5);
        legacy.setBackground(background(0xff18283d,false));
        legacy.setOnFocusChangeListener((v,f)->legacy.setBackground(background(f?0xff304761:0xff18283d,f)));
        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(48)); lp.topMargin=dp(12); root.addView(legacy,lp);
        legacy.setOnClickListener(v->startActivity(new Intent().setClassName(this,"com.wuzhanning.yougame.MainActivity")));
        legacy.setNextFocusUpId(104); legacy.setNextFocusDownId(100);
        setContentView(scroll); cards[Math.max(0,Math.min(5,selected))].requestFocus();
    }
    private void track(AlertDialog d) {
        dialog=d; d.setOnDismissListener(v->{ if(dialog==d) dialog=null; }); d.show();
        for(int which:new int[]{AlertDialog.BUTTON_POSITIVE,AlertDialog.BUTTON_NEGATIVE,AlertDialog.BUTTON_NEUTRAL}) {
            Button button=d.getButton(which);
            if(button!=null) {
                button.setFocusableInTouchMode(true);
                button.setOnFocusChangeListener((v,f)->{ button.setBackground(background(f?0xff53667c:0xff424242,f)); button.setTextColor(f?0xffffdf9c:0xff80cbc4); });
            }
        }
        d.getWindow().getDecorView().post(()->{
            if(d.getButton(AlertDialog.BUTTON_POSITIVE)!=null) d.getButton(AlertDialog.BUTTON_POSITIVE).requestFocus();
            else if(d.getListView()!=null) { d.getListView().requestFocus(); d.getListView().setSelection(0); }
        });
    }
    private void choose(int mode) {
        selected=mode; prefs.edit().putInt("selected",mode).apply();
        Game saved=Game.restore(prefs.getString("save-"+mode,null));
        String description=SUBTITLES[mode]+"\n\n已解锁至第 "+prefs.getInt("level-"+mode,1)+" 关。\n"+(saved!=null?"已有第 "+saved.level+" 关存档。":"还没有存档，从第一关开始吧。");
        track(new AlertDialog.Builder(this).setTitle(TITLES[mode]).setMessage(description)
            .setPositiveButton(saved==null?"开始游戏":"继续存档",(d,w)->launch(saved==null?fresh(mode,1):saved))
            .setNeutralButton("选择关卡",(d,w)->levelPicker(mode))
            .setNegativeButton("玩法说明",(d,w)->help(mode,()->choose(mode))).create());
    }
    private Game fresh(int mode,int level) {
        Game g=new Game(mode,level,System.nanoTime());
        if(mode==Game.PENGUIN) { g.stars=prefs.getInt("stars",0); g.skin=g.stars/5%3; }
        return g;
    }
    private void levelPicker(int mode) {
        NumberPicker picker=new NumberPicker(this); picker.setMinValue(1); picker.setMaxValue(prefs.getInt("level-"+mode,1)); picker.setWrapSelectorWheel(false); picker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        picker.setValue(prefs.getInt("level-"+mode,1));
        track(new AlertDialog.Builder(this).setTitle("选择已解锁关卡").setView(picker).setMessage("上下选择关卡。开始后替换本游戏的当前存档。")
            .setPositiveButton("开始",(d,w)->launch(fresh(mode,picker.getValue())))
            .setNegativeButton("返回",(d,w)->choose(mode)).create());
        picker.post(picker::requestFocus);
    }
    private void launch(Game g) {
        closeDialog();
        if(view!=null) view.stop();
        view=new GameView(this,g); setContentView(view); view.requestFocus(); saveNow();
        if(g.state==0 && foreground) view.start(); else if(g.state!=0) getWindow().getDecorView().post(this::finished);
    }
    void saveGame() {
        if(SystemClock.uptimeMillis()-lastSave>=750) saveNow();
    }
    private void saveNow() {
        if(view==null) return;
        Game g=view.game;
        SharedPreferences.Editor e=prefs.edit().putString("save-"+g.mode,g.save()).putInt("best-"+g.mode,Math.max(g.score,prefs.getInt("best-"+g.mode,0)));
        if(g.state==1) e.putInt("level-"+g.mode,Math.max(Math.min(60,g.level+1),prefs.getInt("level-"+g.mode,1)));
        if(g.mode==Game.PENGUIN) e.putInt("stars",Math.max(g.stars,prefs.getInt("stars",0)));
        if(g.state==1) e.putInt("rating-"+g.mode+"-"+g.level,Math.max(g.rating(),prefs.getInt("rating-"+g.mode+"-"+g.level,0)));
        e.apply(); lastSave=SystemClock.uptimeMillis();
    }
    void finished() {
        if(view==null) return;
        view.stop(); saveNow();
        if(dialog!=null || !foreground) return;
        Game g=view.game;
        AlertDialog d=new AlertDialog.Builder(this).setTitle(g.state==1?"过关啦！":"再挑战一次")
            .setMessage(g.message+"\n\n第 "+g.level+" 关 · 得分 "+g.score+(g.state==1?"\n本关评价  "+new String[]{"","★","★★","★★★"}[g.rating()]:"\n再来一局，试试另一条路线。"))
            .setPositiveButton(g.state==1 && g.level<60?"下一关":"再来一次",(a,w)->launch(fresh(g.mode,g.state==1?Math.min(60,g.level+1):g.level)))
            .setNeutralButton("重玩本关",(a,w)->launch(fresh(g.mode,g.level)))
            .setNegativeButton("游戏大厅",(a,w)->showLibrary()).create();
        d.setOnCancelListener(a->showLibrary()); track(d);
    }
    void pauseMenu() {
        if(view==null || dialog!=null) return;
        if(view.game.state!=0) { finished(); return; }
        view.stop(); saveNow();
        String[] options={"继续游戏", "玩法与关卡说明", "重新开始本关", "返回游戏大厅", prefs.getBoolean("mute",false)?"打开音效":"关闭音效"};
        AlertDialog d=new AlertDialog.Builder(this).setTitle("已暂停 · "+TITLES[view.game.mode]).setItems(options,(a,w)->{
            switch(w) {
                case 0: if(foreground) view.start(); view.requestFocus(); break;
                case 1: help(view.game.mode,this::backToPause); break;
                case 2: confirmRestart(); break;
                case 3: showLibrary(); break;
                case 4: prefs.edit().putBoolean("mute",!prefs.getBoolean("mute",false)).apply(); backToPause(); break;
            }
        }).create();
        d.setOnCancelListener(a->{ if(foreground && view!=null) { view.start(); view.requestFocus(); } }); track(d);
    }
    private void closeDialog() {
        if(dialog!=null) { AlertDialog old=dialog; dialog=null; old.dismiss(); }
    }
    private void backToPause() { closeDialog(); pauseMenu(); }
    private void confirmRestart() {
        track(new AlertDialog.Builder(this).setTitle("重新开始本关？").setMessage("将替换本关进度，已解锁关卡和最高分保留。")
            .setPositiveButton("重新开始",(a,w)->launch(fresh(view.game.mode,view.game.level)))
            .setNegativeButton("返回",(a,w)->backToPause()).setOnCancelListener(d->backToPause()).create());
    }
    private void help(int mode,Runnable after) {
        ScrollView scroll=new ScrollView(this); TextView text=label(DETAILS[mode],18,0xffedf4fd); text.setPadding(dp(24),dp(12),dp(24),dp(12)); scroll.addView(text);
        AlertDialog d=new AlertDialog.Builder(this).setTitle(TITLES[mode]+" · 玩法").setView(scroll).setPositiveButton("知道了",(a,w)->after.run()).create();
        d.setOnCancelListener(a->after.run()); track(d);
    }
    @Override public void onBackPressed() {
        if(view!=null) { pauseMenu(); return; }
        track(new AlertDialog.Builder(this).setTitle("退出数字游乐场？").setMessage("所有进度已保存在这台设备。")
            .setPositiveButton("退出",(d,w)->finish()).setNegativeButton("继续玩",null).create());
    }
    @Override protected void onResume() {
        super.onResume(); foreground=true; if(audio!=null) audio.autoResume();
        if(view!=null && dialog==null) { if(view.game.state==0) pauseMenu(); else finished(); }
    }
    @Override protected void onPause() {
        foreground=false;
        if(audio!=null) audio.autoPause();
        if(view!=null) { view.stop(); saveNow(); }
        super.onPause();
    }
    @Override protected void onSaveInstanceState(Bundle out) {
        saveNow(); out.putInt("mode",view==null?-1:view.game.mode); super.onSaveInstanceState(out);
    }
    @Override protected void onDestroy() {
        if(view!=null) view.stop(); if(dialog!=null) dialog.dismiss(); if(audio!=null) audio.release(); super.onDestroy();
    }
}
