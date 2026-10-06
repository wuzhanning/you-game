const assert = require('node:assert/strict');
const {bodySpec,bodyRadius,shakeBodies,PLANETS,makePlanet,advance,WIDTH,HEIGHT}=require('./dist/physics.js');
let balls=[makePlanet(0,100,400),makePlanet(0,130,400)],merges=[];
advance(balls,b=>merges.push(b.level));assert.equal(balls.length,1);assert.deepEqual(merges,[1]);
balls=[makePlanet(0,100,400),makePlanet(1,132,400)];advance(balls,()=>assert.fail('different planets cannot merge'));assert.equal(balls.length,2);
balls=[makePlanet(10,160,450),makePlanet(10,310,450)];advance(balls,()=>assert.fail('black hole is final planet'));assert.equal(balls.length,2);
balls=[makePlanet(0,-90,HEIGHT+20),makePlanet(2,WIDTH+60,250)];
for(let i=0;i<1200;i++)advance(balls,()=>{});
for(const b of balls){const r=PLANETS[b.level].r;assert(Number.isFinite(b.x)&&Number.isFinite(b.y));assert(b.x>=r&&b.x<=WIDTH-r);assert(b.y<=HEIGHT-r);}
balls=Array.from({length:30},(_,i)=>makePlanet(i%4,40+(i*71)%400,150-Math.floor(i/6)*60));
for(let i=0;i<1800;i++)advance(balls,b=>assert(b.level<PLANETS.length));
for(const b of balls)assert(Number.isFinite(b.x)&&Number.isFinite(b.y));
console.log('PASS: equal merge, unequal collision, final planet, boundaries, dense-board stability');

// Exercise the actual gamepad polling code with synthetic controller snapshots.
const vm=require('node:vm'),fs=require('node:fs');
const source=fs.readFileSync(__dirname+'/dist/game.js','utf8');
const pad={index:0,id:'test controller',connected:true,mapping:'standard',axes:[0],buttons:Array.from({length:16},()=>({pressed:false}))};
let pads=[pad],focused=true,drops=0,restarts=0,message={textContent:''};
const state={navigator:{getGamepads:()=>pads},document:{hidden:false,hasFocus:()=>focused,querySelector:()=>null},$:()=>message,PLANETS,WIDTH,level:0,aim:240,paused:false,ended:false,bombMode:false};
state.drop=()=>drops++;
state.togglePause=()=>{if(!state.ended)state.paused=!state.paused;};
state.reset=()=>{restarts++;state.paused=false;state.ended=false;};
state.mode='planet';state.shakes=3;state.mergeCount=0;state.shakeTicks=0;state.kind=l=>bodySpec(l,state.mode);
vm.createContext(state);
vm.runInContext(source.slice(source.indexOf('const STICK_DEADZONE='),source.indexOf("window.addEventListener('blur'")),state);
const poll=(ms=16)=>state.pollGamepad(ms);
const button=(i,on)=>{pad.buttons[i].pressed=on;poll();};
pad.buttons[0].pressed=true;poll();assert.equal(drops,0,'initial held button ignored');
button(0,false);button(0,true);poll();assert.equal(drops,1,'held button cannot repeat');
button(0,false);button(0,true);assert.equal(drops,2,'release rearms button');button(0,false);
pad.axes[0]=.15;poll();assert.equal(state.aim,240,'deadzone prevents drift');
pad.axes[0]=1;poll(100);assert.equal(state.aim,270);pad.axes[0]=0;
button(14,true);assert(state.aim<270);button(14,false);
button(9,true);assert(state.paused);poll();assert(state.paused,'pause cannot repeat');button(9,false);
button(0,true);assert(!state.paused);assert.equal(drops,2,'resume does not also drop');button(0,false);
focused=false;button(0,true);focused=true;poll();assert.equal(drops,2,'no deferred press after refocus');button(0,false);
pads=[];poll();assert(state.paused,'disconnect pauses');pads=[pad];pad.buttons[0].pressed=true;poll();assert(state.paused,'reconnection does not resume');button(0,false);button(0,true);assert(!state.paused);button(0,false);
state.ended=true;button(0,true);assert.equal(restarts,1);assert.equal(drops,2);button(0,false);
pad.mapping='';poll();assert.match(message.textContent,/未识别/);button(0,true);assert.equal(drops,2);
state.navigator.getGamepads=()=>{throw new Error('blocked API');};poll();
console.log('PASS: gamepad movement, deadzone, button edges, pause, focus, disconnect, reconnect, replay, unsupported API');

const storage=new Map(),ui={};
const savedState={bombs:2,bombMode:false,score:60,best:90,balls:[makePlanet(2,160,400)],level:1,nextLevel:0,aim:160,cooldown:12,overTicks:0,ended:false,paused:false,discovered:new Set([0,1,2]),PLANETS,WIDTH,
  localStorage:{setItem:(k,v)=>storage.set(k,v),getItem:k=>storage.get(k)||null},
  $:id=>ui[id]??=( {textContent:'',style:{}} ),setOverlay:()=>{}};
savedState.mode='planet';savedState.shakes=3;savedState.mergeCount=0;savedState.shakeTicks=0;savedState.kind=l=>bodySpec(l,savedState.mode);
vm.createContext(savedState);
vm.runInContext(source.slice(source.indexOf('function saveGame('),source.indexOf('function merged(')),savedState);
savedState.saveGame();savedState.score=0;savedState.balls=[];savedState.restoreGame();
assert.equal(savedState.score,60);assert.equal(savedState.best,90);assert.equal(savedState.balls[0].level,2);assert(savedState.paused,'resume is opt-in');
savedState.ended=true;savedState.saveGame();savedState.ended=false;savedState.restoreGame();assert(savedState.ended,'completed round survives reload');
const validSave=storage.get('planet-save');storage.set('planet-save','{bad');savedState.restoreGame();assert.match(ui['save-status'].textContent,/无法读取/);
const badSave=JSON.parse(validSave);badSave.balls[0].level=99;storage.set('planet-save',JSON.stringify(badSave));savedState.restoreGame();assert.equal(savedState.balls[0].level,2,'invalid save does not overwrite state');
savedState.localStorage.setItem=()=>{throw new Error('quota');};savedState.saveGame();assert.match(ui['save-status'].textContent,/无法保存/);
let notes=0,resumes=0;
const audio={sound:true,audioContext:{state:'running',currentTime:0,destination:{},resume:()=>{resumes++;return Promise.resolve();},createOscillator:()=>({frequency:{setValueAtTime(){}},connect(){},start(){notes++;},stop(){},disconnect(){}}),createGain:()=>({gain:{setValueAtTime(){},exponentialRampToValueAtTime(){}},connect(){},disconnect(){}})},$:()=>({textContent:''}),window:{}};
vm.createContext(audio);vm.runInContext(source.slice(source.indexOf('function unlockAudio('),source.indexOf('function saveGame(')),audio);
audio.beep('drop');assert.equal(notes,2);audio.beep('merge',2);assert.equal(notes,5);audio.beep('end');assert.equal(notes,8);audio.sound=false;audio.beep('drop');assert.equal(notes,8);audio.sound=true;audio.audioContext.state='suspended';audio.beep('drop');assert.equal(resumes,1);assert.equal(notes,8,'wait for user gesture to unlock audio');
console.log('PASS: saved round restoration, ended round, corrupt storage, quota error, sound patterns and mute');

(async()=>{
  const fullUI={fullscreen:{},status:{}},full={document:{fullscreenElement:null,documentElement:{requestFullscreen:async()=>{full.document.fullscreenElement={};}},exitFullscreen:async()=>{full.document.fullscreenElement=null;}},$:id=>fullUI[id]};
  vm.createContext(full);vm.runInContext(source.slice(source.indexOf("$('fullscreen').onclick="),source.indexOf("document.addEventListener('fullscreenchange'")),full);
  await fullUI.fullscreen.onclick();assert(full.document.fullscreenElement);
  await fullUI.fullscreen.onclick();assert.equal(full.document.fullscreenElement,null);
  full.document.documentElement.requestFullscreen=undefined;await fullUI.fullscreen.onclick();assert.match(fullUI.status.textContent,/不支持/);
  full.document.documentElement.requestFullscreen=async()=>{throw new Error('denied');};await fullUI.fullscreen.onclick();assert.match(fullUI.status.textContent,/未允许/);
  console.log('PASS: fullscreen entry, exit, unsupported and rejected requests');
})().catch(error=>{console.error(error);process.exitCode=1;});

for(const [from,to] of [[8,9],[9,10]]){
  const pair=[makePlanet(from,170,400),makePlanet(from,300,400)];let result;
  advance(pair,b=>result=b.level);assert.equal(result,to);assert.equal(pair.length,1);
}
const bombUI={};let bombSaves=0;
const bombState={bombs:0,bombMode:false,bombTarget:0,balls:[makePlanet(0,100,350),makePlanet(4,240,450)],score:0,best:0,discovered:new Set([0]),particles:[],paused:false,ended:false,overTicks:70,level:0,nextLevel:0,aim:240,cooldown:0,PLANETS,WIDTH,
  document:{querySelector:()=>null},$:id=>bombUI[id]??={textContent:'',style:{},setAttribute(){}},beep(){},update(){},saveGame(){bombSaves++;}};
bombState.mode='planet';bombState.shakes=3;bombState.mergeCount=0;bombState.shakeTicks=0;bombState.kind=l=>bodySpec(l,bombState.mode);
vm.createContext(bombState);
vm.runInContext(source.slice(source.indexOf('function merged('),source.indexOf('// Render each planet once;')),bombState);
bombState.toggleBomb();assert(!bombState.bombMode,'no free bombs');
bombState.merged(makePlanet(7,100,300));bombState.merged(makePlanet(7,150,300));assert.equal(bombState.bombs,2,'every Jupiter earns one bomb');
bombState.merged(makePlanet(8,150,300));assert.equal(bombState.bombs,2,'other merges earn no bomb');
bombState.toggleBomb();assert(bombState.bombMode);assert.equal(bombState.drop(),false,'no drops while targeting');
assert.equal(bombState.removePlanet(-1),false);assert.equal(bombState.bombs,2,'empty hit does not consume');
bombState.selectBombTarget(1);assert.equal(bombState.bombTarget,1);
const previousScore=bombState.score;
assert(bombState.removePlanet(1));assert.equal(bombState.balls.length,1);assert.equal(bombState.balls[0].level,0);assert.equal(bombState.bombs,1);assert.equal(bombState.score,previousScore);assert.equal(bombState.overTicks,0);assert(!bombState.bombMode);
assert.equal(bombState.removePlanet(0),false,'confirmation cannot consume twice');
bombState.toggleBomb();bombState.toggleBomb();assert.equal(bombState.bombs,1,'cancel is free');
bombState.paused=true;bombState.toggleBomb();assert(!bombState.bombMode);bombState.paused=false;
bombState.reset();assert.equal(bombState.bombs,0);assert(!bombState.bombMode);assert(bombSaves>0);
// The save schema is additive: old version-1 saves default to zero bombs.
savedState.localStorage.setItem=(k,v)=>storage.set(k,v);
const legacy=JSON.parse(validSave);delete legacy.bombs;storage.set('planet-save',JSON.stringify(legacy));savedState.restoreGame();assert.equal(savedState.bombs,0);
savedState.bombs=3;savedState.saveGame();savedState.bombs=0;savedState.restoreGame();assert.equal(savedState.bombs,3);
const invalidBombs=JSON.parse(storage.get('planet-save'));invalidBombs.bombs=-1;storage.set('planet-save',JSON.stringify(invalidBombs));savedState.restoreGame();assert.equal(savedState.bombs,3);
// Feed the real bomb actions through the same controller polling loop.
state.navigator.getGamepads=()=>[pad];pad.mapping='standard';pad.buttons.forEach(b=>b.pressed=false);state.paused=false;state.ended=false;state.bombMode=false;let bombActions=0;
state.toggleBomb=()=>{state.bombMode=!state.bombMode;};state.selectBombTarget=()=>{};state.bombTarget=0;state.removePlanet=()=>{bombActions++;state.bombMode=false;};
poll();button(1,true);assert(state.bombMode);button(1,false);button(0,true);poll();assert.equal(bombActions,1);button(0,false);
console.log('PASS: advanced merges, Jupiter rewards, exact-target removal, cancellation, gamepad confirmation, bomb persistence and old saves');

let renders=0,physicsSteps=0,rafCalls=0,polls=0;
const scheduler={document:{hidden:false,querySelector:()=>null},paused:true,ended:false,bombMode:false,bombTarget:0,aim:240,balls:[],particles:[],cooldown:0,overTicks:0,PLANETS,LIMIT:94,
  requestAnimationFrame:()=>++rafCalls,cancelAnimationFrame(){},setTimeout:()=>1,clearTimeout(){},pollGamepad:()=>polls++,draw:()=>renders++,advance:()=>physicsSteps++,merged(){},updateBomb(){},beep(){},saveGame(){},setOverlay(){}};
scheduler.mode='planet';scheduler.shakes=3;scheduler.mergeCount=0;scheduler.shakeTicks=0;scheduler.kind=l=>bodySpec(l,scheduler.mode);
vm.createContext(scheduler);vm.runInContext(source.slice(source.indexOf('let previous=0,accumulator=0'),source.indexOf('function pointerX(')),scheduler);
scheduler.frame(1000);scheduler.frame(1100);scheduler.frame(1200);assert.equal(renders,1,'paused board draws once');assert.equal(physicsSteps,0);
scheduler.document.hidden=true;const beforePolls=polls,beforeRAF=rafCalls;scheduler.frame(1300);assert.equal(polls,beforePolls);assert.equal(rafCalls,beforeRAF,'hidden page schedules no frame');
scheduler.document.hidden=false;scheduler.paused=false;scheduler.frame(2000);const first=renders;
scheduler.frame(2008);scheduler.frame(2016);scheduler.frame(2024);assert.equal(renders,first,'high refresh display does not oversample');scheduler.frame(2034);assert.equal(renders,first+1);assert.equal(physicsSteps,2,'60Hz physics retained');
scheduler.paused=true;scheduler.frame(2200);const pausedRenders=renders;scheduler.frame(2300);assert.equal(renders,pausedRenders);scheduler.bombTarget=1;scheduler.frame(2400);assert.equal(renders,pausedRenders+1,'changed selection repaints');
console.log('PASS: paused redraw suppression, hidden suspension, 30fps rendering and 60Hz physics');

for(const n of [0,1,2,10,11,30,100]){
  const pair=[makePlanet(n,190,350),makePlanet(n,210,350)];
  advance(pair,()=>{},'number');assert.equal(pair.length,1);assert.equal(pair[0].level,n+1);
  assert(bodyRadius(n,'number')<=104);
}
const unequal=[makePlanet(0,190,350),makePlanet(1,200,350)];advance(unequal,()=>assert.fail('unlike numbers merged'),'number');assert.equal(unequal.length,2);
const shaken=[makePlanet(0,25,500),makePlanet(1,430,500)];shaken.forEach(b=>b.age=999);
for(let t=48;t>0;t--){shakeBodies(shaken,t);advance(shaken,()=>{},'number');}
assert(shaken.some(b=>Math.abs(b.vx)>.01||Math.abs(b.vy)>.01));assert(shaken.every(b=>Number.isFinite(b.x)&&Number.isFinite(b.y)&&b.x>=bodyRadius(b.level,'number')&&b.x<=WIDTH-bodyRadius(b.level,'number')));
let shakesUsed=0;state.useShake=()=>{shakesUsed++;};button(2,true);poll();button(2,false);assert.equal(shakesUsed,1,'holding X consumes only once');
let animations=0;
bombState.window={matchMedia:()=>({matches:false})};bombState.canvas={parentElement:{animate:()=>animations++}};bombState.makePlanet=makePlanet;
bombState.reset();assert.equal(bombState.shakes,3);assert.equal(bombState.useShake(),false,'empty board does not consume');
bombState.balls=[makePlanet(0,120,400)];assert(bombState.useShake());assert.equal(bombState.shakes,2);assert.equal(bombState.shakeTicks,48);assert.equal(animations,1);assert.equal(bombState.useShake(),false,'no overlapping shakes');
bombState.shakeTicks=0;bombState.bombMode=true;assert.equal(bombState.useShake(),false);bombState.bombMode=false;
bombState.paused=true;assert.equal(bombState.useShake(),false);bombState.paused=false;
for(let i=0;i<5;i++)bombState.merged(makePlanet(1,120,400));assert.equal(bombState.shakes,3);assert.equal(bombState.mergeCount,0);
const modeStorage=new Map();bombState.localStorage={getItem:k=>modeStorage.get(k)||null,setItem:(k,v)=>modeStorage.set(k,v)};bombState.planetSprites=new Map();bombState.lastDrawKey='';bombState.buildCollection=()=>{};bombState.wakeFrames=()=>{};
vm.runInContext(source.slice(source.indexOf('function saveGame('),source.indexOf('function merged(')),bombState);
bombState.score=100;bombState.best=150;bombState.bombs=2;bombState.shakes=4;
assert(bombState.switchMode('number'));assert.equal(bombState.score,0);assert.equal(bombState.best,0);assert.equal(bombState.shakes,3);assert.equal(bombState.balls.length,0);
assert(bombState.drop());assert.equal(bombState.balls[0].level,0);assert.equal(bombState.level,0);assert.equal(bombState.nextLevel,0);
bombState.score=200;bombState.best=220;bombState.shakes=1;bombState.balls=[makePlanet(12,200,400)];
assert(bombState.switchMode('planet'));assert.equal(bombState.score,100);assert.equal(bombState.best,150);assert.equal(bombState.bombs,2);assert.equal(bombState.shakes,4);
assert(bombState.switchMode('number'));assert.equal(bombState.score,200);assert.equal(bombState.balls[0].level,12);assert.equal(bombState.best,220);assert.equal(bombState.shakes,1);
bombState.localStorage.setItem=()=>{throw new Error('quota');};assert.equal(bombState.switchMode('planet'),false);assert.equal(bombState.mode,'number','cannot lose unsaved round on switch');
assert.equal(bombState.switchMode('invalid'),false);assert.equal(bombState.mode,'number');
console.log('PASS: numeric chain beyond 10, shake impulses and bounds, item earning, gamepad X, separate mode saves and failed-save protection');

// Execute both scripts against a minimal DOM to catch startup/integration errors.
const html=fs.readFileSync(__dirname+'/dist/index.html','utf8');
const fakeCanvas=new Proxy({createRadialGradient:()=>({addColorStop(){}})},{get:(target,key)=>target[key]??(()=>{})});
function element(tag='div'){
  const el={tagName:tag.toUpperCase(),width:112,height:112,children:[],style:{},dataset:{},textContent:'',hidden:false,
    getContext:()=>fakeCanvas,setAttribute(){},addEventListener(){},focus(){},append(child){this.children.push(child);},replaceChildren(){this.children=[];},
    querySelector(selector){return this.parts[selector]??=(element(selector));},parts:{},close(){this.open=false;},showModal(){this.open=true;}};
  return el;
}
const nodes=Object.fromEntries([...html.matchAll(/id="([^"]+)"/g)].map(m=>[m[1],element()]));
nodes.game.parentElement=element();
const browserStorage=new Map([['planet-sound','off']]);
const app={console,document:{getElementById:id=>nodes[id]||null,createElement:element,querySelector:()=>null,querySelectorAll:()=>[],addEventListener(){},hidden:false,hasFocus:()=>true},window:{addEventListener(){}},navigator:{},localStorage:{getItem:k=>browserStorage.get(k)||null,setItem:(k,v)=>browserStorage.set(k,v)},requestAnimationFrame:()=>1,cancelAnimationFrame(){},setInterval(){},setTimeout:()=>1,clearTimeout(){}};
vm.createContext(app);vm.runInContext(fs.readFileSync(__dirname+'/dist/physics.js','utf8'),app);vm.runInContext(source,app);
assert.match(nodes['mode-picker'].textContent,/星球/);assert.equal(nodes['planet-list'].children.length,11);
vm.runInContext("switchMode('number'); balls=[makePlanet(0,100,400),makePlanet(0,120,400)];advance(balls,merged,mode);draw();",app);
assert.match(nodes['mode-picker'].textContent,/数字/);assert.equal(vm.runInContext('balls[0].level',app),1);assert.match(nodes.status.textContent,/合成了1/);
vm.runInContext("useShake();shakeBodies(balls,shakeTicks);switchMode('planet');switchMode('number');draw();",app);
assert.equal(vm.runInContext('balls[0].level',app),1);assert.equal(vm.runInContext('shakes',app),2);assert.equal(vm.runInContext('shakeTicks',app),48);
console.log('PASS: complete app startup, numeric rendering, real merge callbacks and mode round-trip');
