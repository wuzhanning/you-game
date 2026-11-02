package com.wuzhanning.yougame.nativegames;

import java.util.Base64;

public final class GameCheck {
    private static int checks;
    private static void check(boolean ok,String label) { checks++; if(!ok) throw new AssertionError(label); }
    private static Game empty(int mode) { Game g=new Game(mode,1,12345); for(Game.Body b:g.bodies) b.active=false; return g; }
    private static void advance(Game g,int ms) { for(int i=0;i<ms;i+=16) g.update(Math.min(16,ms-i)); }
    private static Game.Body body(Game g,int kind) { for(Game.Body b:g.bodies) if(b.active && b.kind==kind) return b; return null; }
    private static void blaster() {
        Game g=empty(Game.MERGE);
        Game.Body root=g.add(Game.ORB,640,400,2,30,30);
        g.add(Game.ORB,640,345,2,30,30); g.add(Game.ORB,640,320,4,30,30);
        g.combineOrbs(root);
        check(root.number==8 && g.combo==2 && g.score==20,"contact chain 2+2→4+4→8, with combo score");
        g=empty(Game.MERGE); root=g.add(Game.ORB,640,300,16,30,30); g.add(Game.ORB,640,350,16,30,30);
        g.combineOrbs(root); check(g.state==1 && g.value==32,"blaster target victory");
        g=empty(Game.MERGE); g.aim=-1.05f; g.confirm(); g.confirm();
        int shots=0; for(Game.Body b:g.bodies) if(b.active && b.kind==Game.SHOT) shots++;
        check(shots==1,"only one live projectile");
        advance(g,850); check(body(g,Game.SHOT)!=null && body(g,Game.SHOT).vx>0,"left wall reflects projectile");
        g=empty(Game.MERGE); int next=g.next,reserve=g.reserve; g.input(1);
        check(g.next==reserve && g.reserve==next,"swap ammo");
        g=empty(Game.MERGE); for(int i=0;i<200;i++) g.input(0);
        check(g.aim>=-1.05f,"aim clamp");
    }
    private static void tower() {
        Game g=new Game(Game.STACK,1,5); Game.Body top=g.topBlock(); g.px=640; g.landTower(top);
        check(top.number==4 && g.value==4 && top.w==278 && g.drops==1,"perfect landing doubles and repairs width");
        g.next=2; g.px=640; g.landTower(g.topBlock());
        check(g.topBlock().number==2,"different number adds height");
        g.next=2; g.px=640; g.landTower(g.topBlock());
        check(g.topBlock().number==8 && g.value==8,"tower merges cascade into the layer below");
        g=new Game(Game.STACK,1,1); top=g.topBlock(); g.px=top.x+80; g.next=4; g.landTower(top);
        check(Math.abs(g.topBlock().w-180)<.01,"off-center cut uses overlap, not full width");
        g=new Game(Game.STACK,1,1); g.px=50; g.landTower(g.topBlock()); check(g.state==2,"missed tower loses");
        g=new Game(Game.STACK,1,1);
        for(int i=0;i<g.target;i++) { g.px=g.topBlock().x; g.landTower(g.topBlock()); }
        check(g.state==1 && g.rating()==3,"perfect tower can complete and earn three stars");
    }
    private static void runner() {
        Game g=empty(Game.RUN); g.add(Game.FOOD,1,.1f,6,1,1); advance(g,34);
        check(g.value==16 && g.score==12,"runner collects the current lane");
        g=empty(Game.RUN); g.add(Game.FOOD,0,.1f,6,1,1); advance(g,34); check(g.value==10,"other lane is not collected");
        g=empty(Game.RUN); g.add(Game.MULTIPLY,1,.1f,2,1,1); advance(g,34); check(g.value==20,"multiply gate");
        g=empty(Game.RUN); g.add(Game.ADD,1,.1f,-5,1,1); advance(g,34); check(g.value==5,"negative gate");
        g=empty(Game.RUN); g.add(Game.LOW,1,3,0,1,1); g.confirm(); advance(g,260);
        check(g.hp==3,"jump clears low barrier when height is sufficient");
        g=empty(Game.RUN); g.add(Game.HIGH,1,.1f,0,1,1); g.input(3); advance(g,34); check(g.hp==3,"roll clears overhead beam");
        g=empty(Game.RUN); g.add(Game.WALL,1,.1f,0,1,1); g.confirm(); advance(g,34); check(g.hp==2,"jump cannot bypass solid obstacle");
        g=empty(Game.RUN); g.progress=419.8f; g.value=g.target; advance(g,34); check(g.state==1,"finish wall requires enough number");
        g=empty(Game.RUN); g.progress=419.8f; advance(g,34); check(g.state==2,"finish without enough growth loses");
        // Authored lane pattern is completable at all difficulty bands, including jumps and rolls.
        for(int level:new int[]{1,10,30,60}) {
            g=new Game(Game.RUN,level,1);
            for(int t=0;t<60000 && g.state==0;t+=16) {
                Game.Body nearest=null;
                for(Game.Body b:g.bodies) if(b.active && b.y>g.progress && b.kind!=Game.STAR && (nearest==null || b.y<nearest.y)) nearest=b;
                if(nearest!=null) {
                    float z=nearest.y-g.progress;
                    int desired=1;
                    for(Game.Body b:g.bodies) if(b.active && Math.abs(b.y-nearest.y)<.1 && b.kind==Game.FOOD) desired=(int)b.x;
                    if(nearest.kind==Game.ADD || nearest.kind==Game.MULTIPLY) desired=1;
                    g.laneTarget=desired;
                    if(z<5 && z>2) {
                        for(Game.Body b:g.bodies) if(b.active && Math.abs(b.x-desired)<.1 && Math.abs(b.y-nearest.y)<.1) {
                            if(b.kind==Game.LOW) g.confirm();
                            if(b.kind==Game.HIGH) g.input(3);
                        }
                    }
                }
                g.update(16);
            }
            check(g.state==1,"authored run has a viable route at level "+level);
        }
    }
    private static void arena() {
        Game g=empty(Game.COLOR); g.add(Game.FOOD,g.px,g.py,3,12,12); advance(g,34);
        check(g.value==13,"free-space pickup");
        advance(g,100); check(g.value==13,"consumed food cannot be collected before respawn");
        g=empty(Game.COLOR); g.add(Game.FRIEND,g.px,g.py,15,25,25); advance(g,34); check(g.value==25,"same-color ally merges even if larger");
        g=empty(Game.COLOR); g.add(Game.RIVAL,g.px,g.py,8,25,25); advance(g,34); check(g.value==18 && g.enemies==1,"weaker rival is absorbed");
        g=empty(Game.COLOR); Game.Body rival=g.add(Game.RIVAL,g.px,g.py,80,35,35); advance(g,34);
        check(g.hp==2 && g.value==9,"stronger rival damages but does not wipe the run");
        rival.x=g.px; rival.y=g.py; advance(g,34); check(g.hp==2,"collision invulnerability prevents multi-hit in same contact");
        g=empty(Game.DUEL); g.add(Game.MULTIPLY,g.px,g.py,2,42,42); advance(g,34); check(g.value==20,"arithmetic ring doubles");
        advance(g,1000); check(g.value==20,"ring cooldown prevents every-frame multiplication");
        g=empty(Game.DUEL); g.enemies=2; g.value=100; g.add(Game.RIVAL,g.px,g.py,20,30,30); advance(g,34); check(g.state==1,"three eliminations win arena");
        g=empty(Game.COLOR); g.value=g.target; g.px=2280; g.py=780; advance(g,34); check(g.state==1,"grown player can leave through exit");
        g=empty(Game.COLOR); g.px=2280; g.py=780; advance(g,34); check(g.state==0,"exit rejects insufficient growth");
        g=empty(Game.COLOR); float x=g.px; g.press(2,false); advance(g,300); g.releaseAll(); advance(g,500); check(g.px>x+40 && g.px<x+100,"held movement and pause-release work");
        g=empty(Game.DUEL); g.confirm(); check(g.boost>0 && g.cooldown>2,"dash has a cooldown");
        g=empty(Game.DUEL); g.px=600; g.py=410;
        rival=g.add(Game.RIVAL,1000,410,100,30,30); g.add(Game.ROCK,850,410,0,45,45);
        advance(g,3000); check(rival.x<850,"rival steers around pillar instead of pushing into it forever");
        for(int seed=1;seed<=5;seed++) {
            g=new Game(Game.DUEL,1,seed);
            for(Game.Body food:g.bodies) if(food.active && food.kind==Game.FOOD)
                for(Game.Body rock:g.bodies) if(rock.active && rock.kind==Game.ROCK)
                    check(Game.distance2(food.x,food.y,rock.x,rock.y)>=Game.square(rock.w+food.w+10),"food cannot spawn inside a pillar");
        }
    }
    private static void penguin() {
        Game g=empty(Game.PENGUIN); g.confirm(); float initial=g.vy; advance(g,200); g.confirm();
        check(g.phase==2 && g.vy<-500 && initial<-600,"buffered double jump gives a second impulse");
        g.confirm(); check(g.phase==2,"no infinite jumps");
        check(g.gapEnd(650)>650 && g.gapEnd(800)<0,"continuous track has a finite gap");
        g=empty(Game.PENGUIN); g.progress=450; g.px=630; g.py=780; advance(g,34);
        check(g.hp==2 && g.px>727 && g.py<650,"falling in gap respawns safely beyond lip");
        g=empty(Game.PENGUIN); g.add(Game.FOOD,g.px+5,g.py,3,16,16); advance(g,34); check(g.value==8,"penguin collects fish by physical contact");
        g=empty(Game.PENGUIN); g.add(Game.STAR,g.px+5,g.py,1,17,17); advance(g,34); check(g.stars==1 && g.score==100,"airborne star reward");
        g=empty(Game.PENGUIN); g.progress=5399; g.px=5579; g.py=Game.ground(g.px)-33; g.value=g.target; advance(g,34); check(g.state==1,"camp finish");
        for(int level:new int[]{1,10,30,60}) {
            g=new Game(Game.PENGUIN,level,123);
            for(int t=0;t<65000 && g.state==0;t+=16) {
                float start=((int)(g.px/1300))*1300+600, end=start+125+Math.min(45,level*2);
                if(g.px>start-110 && g.px<start-70 && g.phase==0) g.confirm();
                if(g.phase==1 && g.vy>-100 && g.px>start-80 && g.px<end) g.confirm();
                boolean duck=false;
                for(Game.Body b:g.bodies) if(b.active && b.kind==Game.HIGH && b.x-g.px<110 && b.x-g.px>-80) duck=true;
                if(duck) g.press(3,true); else g.release(3);
                g.update(16);
            }
            check(g.state==1,"penguin track has a viable double-jump route at level "+level);
        }
    }
    private static void savesAndBounds() {
        check(Game.restore(null)==null && Game.restore("bad")==null,"invalid save fails closed");
        for(int mode=0;mode<6;mode++) for(int level:new int[]{1,8,20,40,60}) {
            Game g=new Game(mode,level,1234);
            for(int tick=0;tick<1400 && g.state==0;tick++) {
                if(tick%30==0) g.input((tick/30)%4);
                if(tick%75==0) g.confirm();
                g.update(33);
                check(Float.isFinite(g.px) && Float.isFinite(g.py),"finite continuous positions");
                check(g.hp>=0 && g.hp<=3 && g.value>=0 && g.value<=Game.LIMIT,"bounded health and number");
                if(tick%200==0) {
                    String encoded=g.save(); Game copy=Game.restore(encoded);
                    check(copy!=null && copy.save().equals(encoded),"exact snapshot roundtrip mode "+mode+" level "+level);
                    g=copy;
                }
            }
            byte[] data=Base64.getDecoder().decode(g.save()); data[30]^=4;
            check(Game.restore(Base64.getEncoder().encodeToString(data))==null,"checksum rejects damaged snapshot");
            check(Game.restore(g.save().substring(0,100))==null,"truncated snapshot rejected");
        }
        Game g=empty(Game.MERGE); for(int i=0;i<Game.CAPACITY;i++) check(g.add(Game.ORB,400,200,2,30,30)!=null,"pool slot exists");
        check(g.add(Game.ORB,400,200,2,30,30)==null,"pool cannot grow without bounds");
    }
    private static void logicRegressions() {
        Game g=empty(Game.RUN); g.value=100; g.invincible=1;
        g.add(Game.WALL,1,0,0,1,1); g.add(Game.WALL,1,.1f,0,1,1); advance(g,17);
        check(g.value==100 && g.hp==3,"invulnerability protects number as well as hearts");
        g.invincible=0; g.add(Game.WALL,1,g.progress,0,1,1); advance(g,17);
        check(g.value==75 && g.hp==2,"one real hit reduces number exactly once");

        g=empty(Game.RUN); g.hp=1; g.add(Game.WALL,1,0,0,1,1); g.add(Game.FOOD,1,0,100,1,1); advance(g,17);
        check(g.state==2 && g.score==0 && g.message.contains("体力"),"death stops later pickups and preserves the loss reason");
        String ended=g.save(); g.confirm(); g.input(0); advance(g,1000);
        check(g.save().equals(ended),"a finished round cannot change from input or updates");

        g=new Game(Game.STACK,1,1); g.px=640; g.landTower(g.topBlock());
        check(g.py<300,"new moving block is positioned above the tower immediately");
        g.py=548; // Old version-2 saves may contain the previous block's landing height.
        g=Game.restore(g.save()); check(g!=null,"old tower snapshot remains readable");
        g.confirm(); advance(g,17);
        check(g.state==0 && g.drops==1 && g.py<300,"rapid drop or restored drop cannot land instantly from stale height");
        g=new Game(Game.STACK,1,1);
        g.px=640; g.landTower(g.topBlock()); g.next=2; g.px=640; g.landTower(g.topBlock());
        g.next=2; g.px=640; g.landTower(g.topBlock());
        check(g.bestCombo>=g.combo && g.combo>=4,"best combo includes the final cascade");

        g=empty(Game.MERGE); Game.Body root=g.add(Game.ORB,640,300,2,30,30);
        g.add(Game.ORB,700,300,2,30,30); Game.Body obstacle=g.add(Game.ORB,670,335,8,30,30);
        g.combineOrbs(root);
        check(Game.distance2(root.x,root.y,obstacle.x,obstacle.y)>=Game.square(root.w+obstacle.w)-.1f,"enlarged merged ball does not overlap a different number");
        g=empty(Game.MERGE); Game.Body back=g.add(Game.ORB,610,445,2,20,20);
        g.add(Game.ORB,670,447,8,20,20);
        Game.Body shot=g.add(Game.SHOT,640,491,2,28,28); shot.vy=-690; advance(g,17);
        check(back.active && shot.number==2 && g.score==0,"projectile hits the front ball instead of merging through it by array order");
        g=empty(Game.MERGE); g.shots=3; g.next=16; g.add(Game.ORB,640,400,16,31,31); g.confirm(); advance(g,400);
        check(g.state==1 && g.message.contains("漂亮"),"fourth-shot pressure cannot overwrite the victory message");

        g=empty(Game.DUEL); g.px=200; g.py=300;
        Game.Body rival=g.add(Game.RIVAL,1000,300,100,30,30),eaten=g.add(Game.FOOD,990,300,3,12,12);
        eaten.tag=1; g.add(Game.FOOD,1100,300,3,12,12); advance(g,17);
        check(rival.x>1000,"AI targets available food, not a consumed respawning slot");
        g=empty(Game.DUEL); g.px=1020; g.py=420; g.value=100;
        g.add(Game.RIVAL,1020,420,20,30,30); advance(g,200);
        rival=body(g,Game.RIVAL);
        check(g.enemies==1 && rival!=null && Game.distance2(g.px,g.py,rival.x,rival.y)>250*250,"replacement rival cannot spawn on player and cause instant chain kills");
        g=empty(Game.DUEL); g.px=440; g.py=370; obstacle=g.add(Game.ROCK,440,370,0,45,45); advance(g,17);
        check(Game.distance2(g.px,g.py,obstacle.x,obstacle.y)>=Game.square(g.playerRadius()+obstacle.w),"exact-center pillar overlap resolves without a zero direction");
        g=empty(Game.COLOR); g.px=60; g.py=400; g.add(Game.RIVAL,75,400,100,30,30); advance(g,17);
        check(g.px>=30+g.playerRadius()-.01f,"knockback cannot leave player outside world bounds");
        g=empty(Game.COLOR); g.hp=1;
        g.add(Game.RIVAL,g.px,g.py,100,30,30); g.add(Game.FRIEND,g.px+75,g.py,200,22,22); advance(g,17);
        check(g.state==2 && g.score==0 && g.message.contains("体力"),"arena death cannot collect a later ally or replace its result");
        g=empty(Game.COLOR); g.value=g.target; g.px=2221; g.py=915; advance(g,17);
        check(g.state==0,"being near the exit rectangle does not count as entering the circular portal");
        g.px=Game.EXIT_X-Game.EXIT_RADIUS+1; g.py=Game.EXIT_Y; advance(g,17);
        check(g.state==1,"entering the visible portal with enough growth wins");

        g=empty(Game.PENGUIN); g.progress=450; g.px=630; g.py=600; g.phase=0; g.coyote=0; advance(g,17);
        check(g.phase==1,"walking off the cliff consumes the ground jump after grace time");
        g.confirm(); check(g.phase==2 && g.vy==-590,"only the air jump remains after walking off");
        g.confirm(); check(g.phase==2 && g.vy==-590,"there is no third jump after walking off");
        g=empty(Game.PENGUIN); g.py=Game.ground(g.px)-77; g.vy=0; g.phase=2; g.press(3,false);
        g.add(Game.HIGH,g.px,Game.ground(g.px)-72,0,50,30); advance(g,17);
        check(g.hp==2,"midair crouch does not grant immunity to the ice beam");
        g=empty(Game.PENGUIN); g.press(3,false); g.add(Game.HIGH,g.px,Game.ground(g.px)-72,0,50,30); advance(g,17);
        check(g.hp==3 && g.sliding(),"ground crouch really fits beneath the drawn beam");
        g.releaseAll(); check(!g.sliding(),"pause releases the same crouch state used by the renderer and collisions");
        g=empty(Game.PENGUIN); g.py=Game.ground(g.px)-110; g.phase=2;
        g.add(Game.LOW,g.px,Game.ground(g.px)-32,100,35,60); advance(g,17);
        check(g.hp==3,"jumping above the drawn ice block clears its actual height, including old save geometry");
        g=empty(Game.PENGUIN); g.hp=1;
        g.add(Game.LOW,g.px,Game.ground(g.px)-32,100,35,60); g.add(Game.FOOD,g.px+5,g.py,99,16,16); advance(g,17);
        check(g.state==2 && g.score==0 && g.message.contains("体力"),"penguin death stops pickups in the same physics step");
    }
    public static void main(String[] args) { blaster(); tower(); runner(); arena(); penguin(); savesAndBounds(); logicRegressions(); System.out.println("Arcade checks passed: "+checks); }
}
