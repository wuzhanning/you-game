package com.wuzhanning.yougame.nativegames;

import java.io.*;
import java.util.Base64;
import java.util.zip.CRC32;

/** Continuous action games. Coordinates are world units, never board cells. */
public final class Game {
    public static final int MERGE=0, STACK=1, RUN=2, DUEL=3, COLOR=4, PENGUIN=5, NUMBER_SHOOTER=6;
    public static final int ORB=1, SHOT=2, BLOCK=3, FOOD=4, WALL=5, LOW=6, HIGH=7, ADD=8, MULTIPLY=9, STAR=10, RIVAL=11, FRIEND=12, ROCK=13;
    public static final int LIMIT=99999, CAPACITY=96;
    public static final float EXIT_X=2280, EXIT_Y=780, EXIT_RADIUS=55;
    public static final float ICE_HEIGHT=74, ARCH_TOP=92, ARCH_HEIGHT=30;
    static final int SHOOTER_FINISH=370;
    private static final int[] REWARD_WALLS={2,4,8,15,25,40,63,90,125,175,240,350,500,750,1000,1564,2400};
    public static final class Body {
        public int kind, number, tag;
        public float x,y,vx,vy,w,h,age;
        public boolean active;
    }
    public final Body[] bodies=new Body[CAPACITY];
    public final float[] fx=new float[48],fy=new float[48],fvx=new float[48],fvy=new float[48],flife=new float[48];
    public final int[] fcolor=new int[48];
    private final boolean[] held=new boolean[4];
    public int mode,level,score,value=10,hp=3,state,target,goal,next=2,reserve=4,shots,drops,combo,bestCombo,stars,skin,enemies,phase;
    public float elapsed,progress,px=640,py=550,vx,vy,aim,lane=1,laneTarget=1,jump,roll,cooldown,invincible,pulse,shake,time=75,spawn,buffer,coyote,boost,flash;
    public float floatingX,floatingY,floatingLife;
    public String message="", floating="";
    public int sound; // 1 collect, 2 jump/launch, 3 hit, 4 merge, 5 win.
    public long seed;
    private float tapX,tapY,tapTime,stepRemainder;
    public Game(int mode,int level,long seed) {
        if(mode<0 || mode>NUMBER_SHOOTER || level<1 || level>60) throw new IllegalArgumentException("Invalid game");
        this.mode=mode; this.level=level; this.seed=seed;
        for(int i=0;i<CAPACITY;i++) bodies[i]=new Body();
        if(mode==MERGE) initBlaster();
        else if(mode==STACK) initTower();
        else if(mode==RUN) initRun();
        else if(mode==NUMBER_SHOOTER) initNumberShooter();
        else if(mode==DUEL || mode==COLOR) initArena();
        else initPenguin();
    }
    int random(int bound) { seed=seed*6364136223846793005L+1442695040888963407L; return (int)((seed>>>1)%bound); }
    public static int capped(long n) { return (int)Math.max(0,Math.min(LIMIT,n)); }
    public boolean animated() { return state==0; }
    public Body add(int kind,float x,float y,int number,float w,float h) {
        for(Body b:bodies) if(!b.active) { b.active=true; b.kind=kind; b.x=x; b.y=y; b.number=number; b.w=w; b.h=h; b.vx=0; b.vy=0; b.age=0; b.tag=0; return b; }
        return null;
    }
    public void releaseAll() { for(int i=0;i<4;i++) held[i]=false; tapTime=0; buffer=0; }
    public void release(int direction) { if(direction>=0 && direction<4) held[direction]=false; }
    public void press(int d,boolean repeat) {
        if(state!=0 || d<0 || d>3) return;
        held[d]=true;
        if(mode==MERGE && !repeat) {
            if(d==0) aim=Math.max(-1.05f,aim-.075f);
            if(d==2) aim=Math.min(1.05f,aim+.075f);
            if(d==1 && cooldown<=0) { int n=next; next=reserve; reserve=n; cooldown=.25f; }
        } else if(mode==RUN || mode==NUMBER_SHOOTER) {
            if(d==0 && (!repeat || cooldown<=0)) { laneTarget=Math.max(0,laneTarget-1); cooldown=.14f; }
            if(d==2 && (!repeat || cooldown<=0)) { laneTarget=Math.min(2,laneTarget+1); cooldown=.14f; }
            if(mode==RUN && !repeat && d==1) jumpAction();
            if(mode==RUN && !repeat && d==3) { roll=.65f; jump=0; }
        } else if(mode==PENGUIN && !repeat && d==1) jumpAction();
        else if((mode==DUEL || mode==COLOR) && !repeat) {
            tapX=d==0?-1:d==2?1:0; tapY=d==1?-1:d==3?1:0; tapTime=.18f;
        }
    }
    public void input(int d) { press(d,false); release(d); }
    public void confirm() {
        if(state!=0) return;
        if(mode==MERGE) shoot();
        else if(mode==STACK) dropTower();
        else if(mode==RUN || mode==PENGUIN) jumpAction();
        else if((mode==DUEL || mode==COLOR) && cooldown<=0) { boost=.28f; cooldown=2.4f; sound=2; burst(px,py,0xff63d6ce,8); }
    }
    private void jumpAction() {
        if(mode==RUN) { if(jump<=0) { jump=.84f; roll=0; sound=2; } return; }
        buffer=.13f;
        if(phase==0 && coyote>0 || phase==1) {
            vy=phase==0?-650:-590; phase++; coyote=0; buffer=0; sound=2;
            burst(px,py,0xffc4f1fc,7);
        }
    }
    public void update(int ms) {
        if(state!=0 || ms<=0) return;
        // Fixed 60 Hz rules, independently of the 30 Hz display; cap stalls and never catch up from background.
        stepRemainder+=Math.min(ms,100)/1000f;
        while(stepRemainder>=1f/60 && state==0) { step(1f/60); stepRemainder-=1f/60; }
    }
    private void step(float dt) {
        elapsed+=dt; time=Math.max(0,time-dt);
        cooldown=Math.max(0,cooldown-dt); invincible=Math.max(0,invincible-dt); boost=Math.max(0,boost-dt);
        pulse=Math.max(0,pulse-dt*2); shake=Math.max(0,shake-dt*25); flash=Math.max(0,flash-dt*2);
        floatingLife=Math.max(0,floatingLife-dt); tapTime=Math.max(0,tapTime-dt);
        for(int i=0;i<flife.length;i++) if(flife[i]>0) { flife[i]-=dt; fx[i]+=fvx[i]*dt; fy[i]+=fvy[i]*dt; fvy[i]+=150*dt; }
        for(Body b:bodies) if(b.active) b.age+=dt;
        if(mode==MERGE) updateBlaster(dt);
        else if(mode==STACK) updateTower(dt);
        else if(mode==RUN) updateRun(dt);
        else if(mode==NUMBER_SHOOTER) updateNumberShooter(dt);
        else if(mode==DUEL || mode==COLOR) updateArena(dt);
        else updatePenguin(dt);
        if(time<=0 && state==0) lose("时间到了，再试一次更好的路线");
    }
    private void score(int n) { score=capped((long)score+n); }
    private void event(String s,float x,float y,int color,boolean merge) {
        floating=s; floatingX=x; floatingY=y; floatingLife=1.1f; message=s; pulse=.4f; sound=merge?4:1; burst(x,y,color,merge?14:7);
    }
    private void win(String s) { if(state!=0) return; state=1; message=s; sound=5; burst(px,py,0xffffcf64,24); }
    private void lose(String s) { if(state!=0) return; state=2; message=s; sound=3; }
    private boolean hit(String s) {
        if(invincible>0 || state!=0) return false;
        hp--; invincible=1.3f; shake=7; flash=.3f; combo=0; sound=3; message=s;
        burst(px,py,0xffff7b78,12);
        if(hp<=0) lose(s+" · 体力用完了");
        return true;
    }
    public void burst(float x,float y,int color,int count) {
        int made=0;
        for(int i=0;i<flife.length && made<count;i++) if(flife[i]<=0) {
            float angle=made*2.399963f;
            flife[i]=.4f+(made%4)*.08f; fx[i]=x; fy[i]=y; fvx[i]=(float)Math.cos(angle)*(65+made*5); fvy[i]=(float)Math.sin(angle)*(65+made*5)-50; fcolor[i]=color; made++;
        }
    }
    private void initBlaster() {
        target=1<<(5+Math.min(3,(level-1)/6)); goal=target; value=2; time=90;
        for(int row=0;row<3;row++) for(int col=0;col<7;col++) {
            int n=1<<(1+(col/2+row)%3);
            add(ORB,415+col*73+(row%2)*22,200+row*66,n,31,31);
        }
        message="瞄准相同数字 · 反弹也能合成";
    }
    private void shoot() {
        if(cooldown>0) return;
        for(Body b:bodies) if(b.active && b.kind==SHOT) return;
        Body shot=add(SHOT,640,590,next,28,28);
        if(shot==null) { lose("空间用完了"); return; }
        shot.vx=(float)Math.sin(aim)*690; shot.vy=-(float)Math.cos(aim)*690;
        cooldown=.2f; shots++; next=reserve;
        int max=4; for(Body b:bodies) if(b.active && b.kind==ORB) max=Math.max(max,b.number);
        reserve=1<<(1+random(Math.min(4,Math.max(2,31-Integer.numberOfLeadingZeros(max)))));
        sound=2;
    }
    private void updateBlaster(float dt) {
        if(held[0]) aim=Math.max(-1.05f,aim-dt*1.2f);
        if(held[2]) aim=Math.min(1.05f,aim+dt*1.2f);
        for(Body shot:bodies) if(shot.active && shot.kind==SHOT) {
            float startX=shot.x,startY=shot.y;
            shot.x+=shot.vx*dt; shot.y+=shot.vy*dt;
            if(shot.x<300+shot.w) { shot.x=300+shot.w; shot.vx=Math.abs(shot.vx); }
            if(shot.x>980-shot.w) { shot.x=980-shot.w; shot.vx=-Math.abs(shot.vx); }
            Body contact=null; float first=2;
            for(Body b:bodies) if(b.active && b!=shot && b.kind==ORB) {
                float t=contactTime(startX,startY,shot.x-startX,shot.y-startY,b.x,b.y,shot.w+b.w);
                if(t<first) { first=t; contact=b; }
            }
            if(contact!=null) {
                shot.x=startX+(shot.x-startX)*first; shot.y=startY+(shot.y-startY)*first;
                float dx=shot.x-contact.x,dy=shot.y-contact.y, dist=(float)Math.sqrt(Math.max(1,dx*dx+dy*dy));
                shot.x=contact.x+dx/dist*(shot.w+contact.w+1); shot.y=contact.y+dy/dist*(shot.h+contact.h+1);
                shot.kind=ORB; shot.vx=shot.vy=0; combineOrbs(shot);
                finishShot();
            } else if(shot.y<165+shot.h) { shot.y=165+shot.h; shot.kind=ORB; shot.vx=shot.vy=0; combineOrbs(shot); finishShot(); }
        }
    }
    private static float contactTime(float x,float y,float dx,float dy,float cx,float cy,float radius) {
        float ox=x-cx,oy=y-cy,c=ox*ox+oy*oy-radius*radius;
        if(c<=0) return 0;
        float a=dx*dx+dy*dy,b=ox*dx+oy*dy,discriminant=b*b-a*c;
        if(a==0 || discriminant<0) return 2;
        float t=(-b-(float)Math.sqrt(discriminant))/a;
        return t>=0 && t<=1?t:2;
    }
    private boolean settleOrb(Body root) {
        for(int pass=0;pass<24;pass++) {
            boolean moved=false;
            for(Body b:bodies) if(b.active && b!=root && b.kind==ORB) {
                float dx=root.x-b.x,dy=root.y-b.y,min=root.w+b.w+1;
                if(dx*dx+dy*dy<min*min) {
                    if(dx==0 && dy==0) dy=1;
                    float distance=(float)Math.sqrt(dx*dx+dy*dy);
                    root.x=b.x+dx/distance*min; root.y=b.y+dy/distance*min; moved=true;
                }
            }
            float x=clamp(root.x,300+root.w,980-root.w),y=Math.max(165+root.h,root.y);
            moved|=x!=root.x || y!=root.y; root.x=x; root.y=y;
            if(!moved) return true;
        }
        for(Body b:bodies) if(b.active && b!=root && b.kind==ORB && distance2(root.x,root.y,b.x,b.y)<square(root.w+b.w)-.1f) return false;
        return true;
    }
    void combineOrbs(Body root) {
        combo=0;
        for(int pass=0;pass<12;pass++) {
            Body partner=null;
            float tolerance=combo==0?1.5f:12;
            for(Body b:bodies) if(b.active && b!=root && b.kind==ORB && b.number==root.number && distance2(b.x,b.y,root.x,root.y)<square(root.w+b.w+tolerance)) { partner=b; break; }
            if(partner==null) break;
            root.x=(root.x+partner.x)/2; root.y=(root.y+partner.y)/2; partner.active=false;
            root.number=capped((long)root.number*2); root.w=root.h=Math.min(40,28+(31-Integer.numberOfLeadingZeros(root.number))*1.3f);
            score(root.number*(++combo)); value=Math.max(value,root.number);
            event("合成 "+root.number+(combo>1?" · 连锁 ×"+combo:""),root.x,root.y,0xffffc65f,true);
        }
        bestCombo=Math.max(bestCombo,combo);
        boolean fits=settleOrb(root);
        if(value>=target) win("漂亮！合成了 "+value+"，完成目标");
        else if(!fits) lose("球群没有可用落点，试试换弹或反弹");
    }
    private void finishShot() {
        if(state!=0) return;
        if(shots%4==0) { for(Body b:bodies) if(b.active && b.kind==ORB) b.y+=17; message="顶端下降了 · 尽快打开空间"; }
        for(Body b:bodies) if(b.active && b.kind==ORB && b.y+b.h>545) { lose("数字压过了警戒线"); break; }
    }
    private void initTower() {
        target=12+Math.min(8,(level-1)/3); goal=target; time=90; value=2;
        Body base=add(BLOCK,640,590,2,260,42); base.tag=1;
        next=2; px=260; py=200; vx=185+Math.min(90,level*3); message="看准重叠位置按确认 · 完美落点会修复塔宽";
    }
    public Body topBlock() {
        Body top=null;
        for(Body b:bodies) if(b.active && b.kind==BLOCK && (top==null || b.y<top.y)) top=b;
        return top;
    }
    private void dropTower() {
        if(phase!=0) return;
        Body top=topBlock(); if(top==null) return;
        py=200-Math.max(0,420-top.y);
        phase=1; vy=30; sound=2;
    }
    private void updateTower(float dt) {
        Body top=topBlock(); if(top==null) return;
        float camera=Math.max(0,420-top.y);
        if(phase==0) {
            px+=vx*dt;
            if(held[0]) px-=110*dt;
            if(held[2]) px+=110*dt;
            if(px<250) { px=250; vx=Math.abs(vx); }
            if(px>1030) { px=1030; vx=-Math.abs(vx); }
            py=200-camera;
        } else {
            vy+=1500*dt; py+=vy*dt;
            if(py>=top.y-42) landTower(top);
        }
    }
    void landTower(Body top) {
        float width=top.w, left=Math.max(px-width/2,top.x-width/2), right=Math.min(px+width/2,top.x+width/2), overlap=right-left;
        if(overlap<=10) { lose("落空了！看准塔顶再投放"); return; }
        boolean perfect=Math.abs(px-top.x)<=13;
        if(perfect) { px=top.x; overlap=Math.min(280,width+18); combo++; } else { px=(left+right)/2; combo=0; }
        drops++; bestCombo=Math.max(bestCombo,combo); score((perfect?30:10)+combo*5);
        float y=top.y-42;
        if(next==top.number) {
            top.number=capped((long)top.number*2); top.x=px; top.w=overlap; value=Math.max(value,top.number); score(top.number);
            for(int pass=0;pass<12;pass++) {
                Body below=null;
                for(Body b:bodies) if(b.active && b!=top && b.kind==BLOCK && b.y>top.y && b.y-top.y<43 && b.number==top.number) { below=b; break; }
                if(below==null) break;
                below.number=capped((long)below.number*2); below.x=top.x; below.w=top.w; top.active=false; top=below;
                value=Math.max(value,top.number); score(top.number*2); combo++;
            }
            event("合成 "+top.number+(perfect?" · 完美！":""),px,top.y,0xffffcf64,true);
        } else {
            add(BLOCK,px,y,next,overlap,42); value=Math.max(value,next);
            event(perfect?"完美落点！":"稳稳落下",px,y,0xff66d6cc,perfect);
        }
        bestCombo=Math.max(bestCombo,combo);
        if(drops>=target) { win("数字塔建成！完成 "+drops+" 次精准落塔"); return; }
        int[] pattern={2,4,2,2,8,4,4,2,8,8,16,4};
        next=pattern[drops%pattern.length]; phase=0; px=vx>0?250:1030; vy=0;
        py=200-Math.max(0,420-topBlock().y);
    }
    private void initRun() {
        target=35+level*5; goal=420; time=65; px=640; py=565; value=10;
        for(int row=0;row<24;row++) {
            float z=30+row*15; int pattern=row%6, safe=(row/6)%3;
            if(pattern==0) { for(int l=0;l<3;l++) add(FOOD,l,z,2+l*2,1,1); }
            else if(pattern==1) { add(WALL,(safe+1)%3,z,0,1,1); add(FOOD,safe,z,5,1,1); }
            else if(pattern==2) { add(LOW,1,z,0,1,1); add(STAR,1,z+3,1,1,1); add(FOOD,0,z,3,1,1); }
            else if(pattern==3) { add(ADD,0,z,10,1,1); add(MULTIPLY,1,z,2,1,1); add(ADD,2,z,-5,1,1); }
            else if(pattern==4) { add(FOOD,safe,z,8,1,1); add(WALL,(safe+2)%3,z,0,1,1); }
            else { add(HIGH,1,z,0,1,1); add(FOOD,2,z,6,1,1); }
        }
        message="三条跑道 · 收集数字，选择更好的运算门";
    }
    private void initNumberShooter() {
        value=1; next=4; reserve=44; time=60;
        goal=535; target=7777;
        add(FOOD,1,24,1,1,1);
        add(ADD,1,47,1,1,1);
        for(int section=0;section<5;section++){
            int z=68+section*56, good=(section+level)%3, alternate=(good+1)%3;
            int growth=2+section+level/4;
            add(FOOD,good,z,growth,1,1);
            add(FOOD,alternate,z,-growth,1,1);
            add(section%2==0?STAR:ADD,good,z+17,section%2==0?4:1,1,1);
            add(FOOD,alternate,z+17,growth+2,1,1);
            add(LOW,(good+2)%3,z+32,0,1,1);
            add(FOOD,good,z+32,growth*2,1,1);
            if(section==2 || section==4) add(MULTIPLY,alternate,z+48,2,1,1);
        }
        int difficulty=1+(level-1)/8;
        for(int i=0;i<REWARD_WALLS.length;i++){
            int strength=REWARD_WALLS[i]*difficulty;
            add(BLOCK,1,380+i*8,strength,2,Math.min(200,8+i*3));
        }
        add(BLOCK,1,goal,target,2,200);
        message="自动发射数字子弹 · 左右换道";
    }
    private void updateNumberShooter(float dt) {
        float previous=progress;
        lane+=(laneTarget-lane)*Math.min(1,dt*15);
        progress+=(progress<SHOOTER_FINISH?18:12)*dt;
        if(previous<SHOOTER_FINISH && progress>=SHOOTER_FINISH)
            event("冲线！击破奖励数字墙",640,490,0xffffcf64,true);

        spawn-=dt;
        if(spawn<=0){
            Body shot=add(SHOT,lane,progress+2,value,.18f,0);
            if(shot!=null) shot.h=reserve;
            spawn=1f/next;
        }
        for(Body shot:bodies) if(shot.active && shot.kind==SHOT) {
            float end=Math.min(shot.y+112*dt,progress+shot.h);
            Body front=null;
            for(Body b:bodies){
                boolean shootable=b.kind==FOOD && b.number>0 || b.kind==ADD || b.kind==STAR || b.kind==BLOCK;
                if(b.active && shootable && b.y>=shot.y && b.y<=end &&
                    Math.abs(shot.x-b.x)<(b.kind==BLOCK?2:.46f) && (front==null || b.y<front.y)) front=b;
            }
            if(front!=null){
                shot.active=false;
                front.age=0;
                front.tag=1;
                if(front.kind==FOOD) front.number=capped((long)front.number+Math.max(1,shot.number/48));
                else if(front.kind==BLOCK){
                    front.number=Math.max(0,front.number-Math.max(1,shot.number/8));
                    if(front.number==0) clearRewardWall(front);
                } else {
                    front.vx++;
                    if(front.vx>=3){
                        front.number=Math.min(front.kind==ADD?8:24,front.number+1);
                        front.vx=0;
                    }
                }
            } else {
                shot.y=end;
                if(end>=progress+shot.h) shot.active=false;
            }
        }
        for(Body b:bodies) if(b.active && b.kind!=SHOT && b.y<=progress+1){
            if(b.kind==BLOCK){
                if(value<b.number){win("奖励跑道结束 · 本关获得 "+score+" 金币");return;}
                clearRewardWall(b);
                continue;
            }
            b.active=false;
            if(Math.abs(lane-b.x)>=.46f) continue;
            if(b.kind==FOOD){
                value=capped((long)value+b.number);
                if(value==0){lose("数字归零了，试着避开红色数字");return;}
                event((b.number>0?"+":"")+b.number,640,500,b.number>0?0xff73abff:0xffff6969,false);
            } else if(b.kind==ADD){
                next=Math.min(18,next+b.number);
                event("射速 +"+b.number,640,500,0xff91e758,true);
            } else if(b.kind==STAR){
                reserve=Math.min(85,reserve+b.number);
                event("射程 +"+b.number,640,500,0xff91e758,true);
            } else if(b.kind==MULTIPLY){
                value=capped((long)value*2);
                event("数字 ×2",640,500,0xff54dcec,true);
            } else if(b.kind==LOW && hit("撞到尖刺")){
                value=Math.max(1,value*2/3);
                if(state!=0) return;
            }
        }
        if(progress>=goal) win("击破终点金库！本关获得 "+score+" 金币");
    }
    private void clearRewardWall(Body wall) {
        wall.active=false;
        int coins=(int)wall.h;
        stars=capped((long)stars+coins);
        score(coins);
        event("+"+coins+" 金币",640,500,0xffffdb48,true);
    }
    public float jumpHeight() { return jump<=0?0:(float)Math.sin((.84f-jump)/.84f*Math.PI)*110; }
    private void updateRun(float dt) {
        jump=Math.max(0,jump-dt); roll=Math.max(0,roll-dt);
        lane+=(laneTarget-lane)*Math.min(1,dt*15);
        progress+=(11+Math.min(6,level*.16f))*dt;
        for(Body b:bodies) if(b.active && b.y<progress+1.2f) {
            b.active=false;
            if(Math.abs(lane-b.x)>.43f) continue;
            if(b.kind==FOOD) { value=capped((long)value+b.number); score(b.number*2); combo++; event("+"+b.number,640+(lane-1)*220,500,0xff63d6ce,false); }
            else if(b.kind==ADD) { value=Math.max(1,capped((long)value+b.number)); event((b.number>0?"+":"")+b.number,640,450,0xff63d6ce,false); }
            else if(b.kind==MULTIPLY) { value=capped((long)value*2); event("×2  数字翻倍！",640,450,0xffffc65f,true); }
            else if(b.kind==STAR) { if(jumpHeight()>30) { stars++; score(50); event("空中奖励 ★",640,440,0xffffcf64,false); } }
            else if(b.kind==LOW && jumpHeight()>35 || b.kind==HIGH && roll>0) { score(15); combo++; }
            else if(hit(b.kind==LOW?"矮栏要跳过":b.kind==HIGH?"横梁要下滑":"实心路障需要换道")) value=Math.max(1,value*3/4);
            if(state!=0) return;
        }
        bestCombo=Math.max(bestCombo,combo);
        if(progress>=goal) { if(value>=target) { score(value*3); win("冲线！带着 "+value+" 击破终点数字墙"); } else lose("已抵达终点，但数字还差 "+(target-value)+"；多选加法与翻倍门"); }
    }
    private void initArena() {
        boolean duel=mode==DUEL; px=duel?220:450; py=duel?400:680;
        target=duel?3:60+Math.min(260,level*8); goal=duel?3:target; time=duel?80:100;
        for(int i=0;i<(duel?18:36);i++) {
            float x=duel?200+random(880):100+random(2200), y=duel?200+random(350):180+random(1200);
            add(FOOD,x,y,duel?3:1+random(4),duel?12:10,10);
        }
        if(duel) {
            add(RIVAL,1020,420,18+level*2,30,30);
            add(MULTIPLY,640,250,2,42,42); add(ADD,640,520,8,42,42);
            add(ROCK,440,370,0,45,45); add(ROCK,850,410,0,45,45);
            for(Body b:bodies) if(b.active && b.kind==FOOD) placeFood(b);
            message="收集 +3，抢运算环 · 数字更大时碰撞击败对手";
        } else {
            for(int i=0;i<8;i++) add(RIVAL,650+random(1500),250+random(1050),8+i*7,22+i*2,22+i*2);
            for(int i=0;i<5;i++) add(FRIEND,650+random(1500),250+random(1050),5+i*3,22,22);
            message="自由移动 · 吃蓝色同伴与较小红方 · 长大后去东侧出口";
        }
    }
    private void updateArena(float dt) {
        float dx=(held[2]?1:0)-(held[0]?1:0), dy=(held[3]?1:0)-(held[1]?1:0);
        if(dx==0 && dy==0 && tapTime>0) { dx=tapX; dy=tapY; }
        float len=(float)Math.sqrt(dx*dx+dy*dy), speed=(mode==DUEL?215:235-Math.min(45,(float)Math.sqrt(value)*1.7f))*(boost>0?2.25f:1);
        if(len>0) { vx=dx/len*speed; vy=dy/len*speed; } else { vx*=.72f; vy*=.72f; }
        px+=vx*dt; py+=vy*dt;
        float radius=playerRadius();
        confinePlayer();
        for(Body b:bodies) if(b.active) {
            if(b.kind==RIVAL) {
                b.vy=Math.max(0,b.vy-dt);
                float tx=px,ty=py;
                float dist=distance2(px,py,b.x,b.y);
                if(b.number<value) { tx=b.x+(b.x-px); ty=b.y+(b.y-py); }
                else if(dist>square(mode==DUEL?500:330)) {
                    Body food=null; float best=Float.MAX_VALUE;
                    for(Body f:bodies) if(f.active && f.kind==FOOD && f.tag==0) { float d=distance2(f.x,f.y,b.x,b.y); if(d<best) { best=d; food=f; } }
                    if(food!=null) { tx=food.x; ty=food.y; }
                }
                float ddx=tx-b.x,ddy=ty-b.y,d=(float)Math.sqrt(ddx*ddx+ddy*ddy);
                float enemySpeed=mode==DUEL?112+Math.min(30,level):95;
                if(d>1) { b.x+=ddx/d*enemySpeed*dt; b.y+=ddy/d*enemySpeed*dt; }
                b.x=clamp(b.x,mode==DUEL?120:60,mode==DUEL?1160:2340); b.y=clamp(b.y,mode==DUEL?200:180,mode==DUEL?580:1400);
                // ponytail: local circle steering suits these two pillars; use navigation paths for a maze arena.
                if(mode==DUEL) for(Body rock:bodies) if(rock.active && rock.kind==ROCK) {
                    float sx=b.x-rock.x,sy=b.y-rock.y,rd=(float)Math.sqrt(Math.max(1,sx*sx+sy*sy));
                    if(rd<32+rock.w+12) {
                        if(b.vy<=0) { b.vx=((tx-b.x)*sy-(ty-b.y)*sx)>=0?-1:1; b.vy=2; }
                        b.x-=sy/rd*b.vx*enemySpeed*dt*1.5f; b.y+=sx/rd*b.vx*enemySpeed*dt*1.5f;
                    }
                    separate(b,rock,32);
                }
                if(mode==DUEL) for(Body food:bodies) if(food.active && food.kind==FOOD && food.tag==0 && distance2(b.x,b.y,food.x,food.y)<square(b.w+food.w)) {
                    b.number=capped((long)b.number+1); food.tag=1; food.age=0;
                }
            }
            if(b.kind==FOOD && b.tag==1) {
                if(b.age>2.2f) { b.tag=0; b.age=0; placeFood(b); }
                continue;
            }
            if(distance2(px,py,b.x,b.y)>square(radius+b.w)) continue;
            if(b.kind==ROCK) {
                float sx=px-b.x,sy=py-b.y;
                if(sx==0 && sy==0) sx=1;
                float d=(float)Math.sqrt(sx*sx+sy*sy); px=b.x+sx/d*(radius+b.w+1); py=b.y+sy/d*(radius+b.h+1);
            } else if(b.kind==FOOD || b.kind==FRIEND) {
                value=capped((long)value+b.number); score(b.number*3); combo++; event("+"+b.number,px,py,0xff63d6ce,false);
                if(b.kind==FOOD) { b.tag=1; b.age=0; } else b.active=false;
            } else if(b.kind==ADD || b.kind==MULTIPLY) {
                if(b.tag==0) { value=b.kind==ADD?capped((long)value+b.number):capped((long)value*2); b.tag=1; b.age=0; event(b.kind==ADD?"+8 运算环":"×2 运算环",px,py,0xffffcf64,true); }
            } else if(b.kind==RIVAL) {
                if(value>b.number) {
                    value=capped((long)value+b.number); score(b.number*5); enemies++; b.active=false;
                    event("吞噬 +"+b.number,px,py,0xffffc65f,true);
                    if(mode==DUEL && enemies<target) spawnRival();
                } else if(hit("红方更大，先成长再回来")) {
                    value=Math.max(5,value*9/10);
                    float sx=px-b.x,sy=py-b.y;
                    if(sx==0 && sy==0) sx=1;
                    float d=(float)Math.sqrt(sx*sx+sy*sy); px+=sx/d*75; py+=sy/d*75;
                }
            }
            if(state!=0) return;
        }
        confinePlayer();
        for(Body b:bodies) if(b.active && (b.kind==ADD || b.kind==MULTIPLY) && b.tag==1 && b.age>7) { b.tag=0; b.age=0; }
        if(mode==DUEL && enemies>=target) win("竞技场冠军！连续击败三名对手");
        if(mode==COLOR && value>=target && distance2(px,py,EXIT_X,EXIT_Y)<=square(EXIT_RADIUS)) { score(value*2); win("成功撤离！红蓝数字之旅完成"); }
        bestCombo=Math.max(bestCombo,combo);
    }
    private void spawnRival() {
        float x=px<640?1050:230,y=py<390?550:230;
        add(RIVAL,x,y,18+level*2+enemies*24,32+enemies*3,32+enemies*3);
    }
    private void confinePlayer() {
        float radius=playerRadius();
        px=clamp(px,mode==DUEL?105:radius+30,mode==DUEL?1175:2370-radius);
        py=clamp(py,mode==DUEL?185:150+radius,mode==DUEL?595:1430-radius);
    }
    private void placeFood(Body food) {
        for(int attempt=0;attempt<8;attempt++) {
            food.x=mode==DUEL?150+random(980):100+random(2200); food.y=mode==DUEL?200+random(350):180+random(1200);
            boolean blocked=false;
            for(Body rock:bodies) if(rock.active && rock.kind==ROCK && distance2(food.x,food.y,rock.x,rock.y)<square(rock.w+food.w+10)) blocked=true;
            if(!blocked) return;
        }
        food.x=200; food.y=230;
    }
    private void separate(Body a,Body rock,float radius) {
        float dx=a.x-rock.x,dy=a.y-rock.y;
        if(dx==0 && dy==0) dx=1;
        float d=(float)Math.sqrt(dx*dx+dy*dy);
        if(d<radius+rock.w) { a.x=rock.x+dx/d*(radius+rock.w+1); a.y=rock.y+dy/d*(radius+rock.h+1); }
    }
    public float playerRadius() { return Math.min(mode==DUEL?42:64,22+(float)Math.sqrt(value)*2.2f); }
    public boolean sliding() { return mode==RUN?roll>0:mode==PENGUIN && held[3]; }
    private void initPenguin() {
        goal=5400; target=28+Math.min(72,level*2); time=65; px=180; py=ground(px)-33; value=5; coyote=.1f;
        for(int chunk=0;chunk<4;chunk++) {
            float start=chunk*1300;
            for(int j=0;j<9;j++) {
                float x=start+360+j*95, h=j>=3 && j<=5?125:37;
                add(FOOD,x,ground(x)-h,2+j%3,16,16);
            }
            add(LOW,start+1020,ground(start+1020)-32,12+chunk*6,35,60);
            add(STAR,start+680,ground(start+680)-175,1,17,17);
            if(chunk>0) add(HIGH,start+1180,ground(start+1180)-72,0,50,30);
        }
        message="确认跳跃，再按一次二段跳 · 左刹车，右加速";
    }
    public static float ground(float x) { return 530+(float)Math.sin(x/300)*25+(float)Math.sin(x/950)*20; }
    public float gapEnd(float x) {
        if(x<0) return -1;
        int segment=(int)(x/1300); float start=segment*1300+600, end=start+125+Math.min(45,level*2);
        return segment<4 && x>=start && x<=end?end:-1;
    }
    private void updatePenguin(float dt) {
        buffer=Math.max(0,buffer-dt); coyote=Math.max(0,coyote-dt);
        if(phase==0 && coyote<=0) phase=1;
        float speed=held[0]?150:held[2]?300:225+Math.min(25,level);
        progress+=speed*dt; px=180+progress;
        boolean gap=gapEnd(px)>=0;
        vy+=1500*dt; if(held[3] && vy>0) vy+=1200*dt; py+=vy*dt;
        float floor=ground(px)-33;
        if(!gap && py>=floor && vy>=0 && py<floor+100) {
            py=floor; vy=0; coyote=.1f; phase=0;
            if(buffer>0) jumpAction();
        }
        if(py>770) {
            hit("冰缝要提前跳起，空中还能再跳一次");
            if(state!=0) return;
            float end=gapEnd(px);
            if(end>0) { px=end+45; progress=px-180; }
            py=ground(px)-33; vy=0; phase=0; coyote=.1f;
        }
        for(Body b:bodies) if(b.active) {
            // Derive ice bounds from the drawn silhouette, including bodies in older version-2 saves.
            float center=b.kind==LOW?ground(b.x)-ICE_HEIGHT/2:b.kind==HIGH?ground(b.x)-ARCH_TOP+ARCH_HEIGHT/2:b.y;
            float height=b.kind==LOW?ICE_HEIGHT/2:b.kind==HIGH?ARCH_HEIGHT/2:b.h;
            float playerY=py+(sliding()?17:0),playerHeight=sliding()?16:33;
            if(Math.abs(px-b.x)>=34+b.w || Math.abs(playerY-center)>=playerHeight+height) continue;
            if(b.kind==FOOD) { value=capped((long)value+b.number); score(b.number*5); combo++; event("+"+b.number,px,py,0xff6fe0d6,false); b.active=false; }
            else if(b.kind==STAR) { stars++; skin=stars/5%3; score(100); event("★  空中宝藏",px,py,0xffffd271,true); b.active=false; }
            else if(b.kind==LOW) {
                if(value>b.number) { value=capped((long)value+b.number); score(50); event("冲破冰块 +"+b.number,px,py,0xffb1f1fc,true); b.active=false; }
                else if(hit("冰块更大，跳跃绕过")) { b.active=false; vy=-300; }
            } else if(b.kind==HIGH && hit("冰拱下要按住下键滑行")) b.active=false;
            if(state!=0) return;
        }
        bestCombo=Math.max(bestCombo,combo);
        if(progress>=goal) {
            if(value>=target) { score(value*5); win("企鹅抵达营地！带回 "+value+" 条数字小鱼"); }
            else lose("到达营地，但小鱼还差 "+(target-value)+"；跳起收集空中的鱼群");
        }
    }
    public int rating() { return state!=1?0:hp==3 && (mode!=STACK || bestCombo>=3)?3:hp>=2?2:1; }
    static float square(float n) { return n*n; }
    static float distance2(float x,float y,float a,float b) { return square(x-a)+square(y-b); }
    static float clamp(float x,float min,float max) { return Math.max(min,Math.min(max,x)); }
    public String save() {
        try {
            ByteArrayOutputStream bytes=new ByteArrayOutputStream(6000); DataOutputStream out=new DataOutputStream(bytes);
            out.writeInt(2); out.writeLong(seed);
            for(int n:new int[]{mode,level,score,value,hp,state,target,goal,next,reserve,shots,drops,combo,bestCombo,stars,skin,enemies,phase}) out.writeInt(n);
            for(float f:new float[]{elapsed,progress,px,py,vx,vy,aim,lane,laneTarget,jump,roll,cooldown,invincible,time,spawn,buffer,coyote,boost,stepRemainder}) out.writeFloat(f);
            for(Body b:bodies) { out.writeBoolean(b.active); out.writeInt(b.kind); out.writeInt(b.number); out.writeInt(b.tag); for(float f:new float[]{b.x,b.y,b.vx,b.vy,b.w,b.h,b.age}) out.writeFloat(f); }
            out.writeUTF(message); out.flush(); byte[] data=bytes.toByteArray(); CRC32 crc=new CRC32(); crc.update(data); out.writeLong(crc.getValue()); out.close();
            return Base64.getEncoder().encodeToString(bytes.toByteArray());
        } catch(IOException impossible) { throw new IllegalStateException(impossible); }
    }
    public static Game restore(String text) {
        if(text==null || text.length()>14000) return null;
        try {
            byte[] data=Base64.getDecoder().decode(text); if(data.length<100) return null;
            CRC32 crc=new CRC32(); crc.update(data,0,data.length-8);
            DataInputStream tail=new DataInputStream(new ByteArrayInputStream(data,data.length-8,8)); if(crc.getValue()!=tail.readLong()) return null;
            DataInputStream in=new DataInputStream(new ByteArrayInputStream(data,0,data.length-8));
            if(in.readInt()!=2) return null;
            long seed=in.readLong(); Game g=new Game(in.readInt(),in.readInt(),seed); g.seed=seed;
            int expectedTarget=g.target, expectedGoal=g.goal;
            g.score=in.readInt(); g.value=in.readInt(); g.hp=in.readInt(); g.state=in.readInt(); g.target=in.readInt(); g.goal=in.readInt();
            g.next=in.readInt(); g.reserve=in.readInt(); g.shots=in.readInt(); g.drops=in.readInt(); g.combo=in.readInt(); g.bestCombo=in.readInt(); g.stars=in.readInt(); g.skin=in.readInt(); g.enemies=in.readInt(); g.phase=in.readInt();
            g.elapsed=in.readFloat(); g.progress=in.readFloat(); g.px=in.readFloat(); g.py=in.readFloat(); g.vx=in.readFloat(); g.vy=in.readFloat(); g.aim=in.readFloat(); g.lane=in.readFloat(); g.laneTarget=in.readFloat(); g.jump=in.readFloat(); g.roll=in.readFloat(); g.cooldown=in.readFloat(); g.invincible=in.readFloat(); g.time=in.readFloat(); g.spawn=in.readFloat(); g.buffer=in.readFloat(); g.coyote=in.readFloat(); g.boost=in.readFloat(); g.stepRemainder=in.readFloat();
            if(g.score<0 || g.score>LIMIT || g.value<0 || g.value>LIMIT || g.hp<0 || g.hp>3 || g.state<0 || g.state>2 || g.target!=expectedTarget || g.goal!=expectedGoal || g.next<1 || g.next>LIMIT || g.reserve<1 || g.reserve>LIMIT || g.skin<0 || g.skin>2 || g.phase<0 || g.phase>2 || g.stars<0 || g.stars>100000 || g.enemies<0 || g.enemies>20 || g.shots<0 || g.shots>1000 || g.drops<0 || g.drops>50 || g.combo<0 || g.combo>10000 || g.bestCombo<0 || g.bestCombo>10000) return null;
            for(float f:new float[]{g.elapsed,g.progress,g.px,g.py,g.vx,g.vy,g.aim,g.lane,g.laneTarget,g.jump,g.roll,g.cooldown,g.invincible,g.time,g.spawn,g.buffer,g.coyote,g.boost,g.stepRemainder}) if(!Float.isFinite(f) || Math.abs(f)>20000) return null;
            if(g.elapsed<0 || g.elapsed>120 || g.time<0 || g.time>100 || Math.abs(g.aim)>1.06 || g.lane<0 || g.lane>2 || g.laneTarget<0 || g.laneTarget>2 || g.stepRemainder<0 || g.stepRemainder>.11f) return null;
            for(Body b:g.bodies) {
                b.active=in.readBoolean(); b.kind=in.readInt(); b.number=in.readInt(); b.tag=in.readInt(); b.x=in.readFloat(); b.y=in.readFloat(); b.vx=in.readFloat(); b.vy=in.readFloat(); b.w=in.readFloat(); b.h=in.readFloat(); b.age=in.readFloat();
                if(b.kind<0 || b.kind>ROCK || Math.abs((long)b.number)>LIMIT || b.tag<0 || b.tag>1) return null;
                for(float f:new float[]{b.x,b.y,b.vx,b.vy,b.w,b.h,b.age}) if(!Float.isFinite(f) || Math.abs(f)>20000) return null;
                if(b.w<0 || b.w>300 || b.h<0 || b.h>300 || b.age<0 || b.age>120) return null;
            }
            g.message=in.readUTF(); if(in.available()!=0 || g.message.length()>200) return null;
            return g;
        } catch(IOException | IllegalArgumentException ex) { return null; }
    }
}
