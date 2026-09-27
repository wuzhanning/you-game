const assert = require('node:assert/strict');
const {PLANETS,makePlanet,advance,WIDTH,HEIGHT}=require('./dist/physics.js');
let balls=[makePlanet(0,100,400),makePlanet(0,130,400)],merges=[];
advance(balls,b=>merges.push(b.level));assert.equal(balls.length,1);assert.deepEqual(merges,[1]);
balls=[makePlanet(0,100,400),makePlanet(1,132,400)];advance(balls,()=>assert.fail('different planets cannot merge'));assert.equal(balls.length,2);
balls=[makePlanet(8,160,490),makePlanet(8,310,490)];advance(balls,()=>assert.fail('sun is final planet'));assert.equal(balls.length,2);
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
const state={navigator:{getGamepads:()=>pads},document:{hidden:false,hasFocus:()=>focused,querySelector:()=>null},$:()=>message,PLANETS,WIDTH,level:0,aim:240,paused:false,ended:false};
state.drop=()=>drops++;
state.togglePause=()=>{if(!state.ended)state.paused=!state.paused;};
state.reset=()=>{restarts++;state.paused=false;state.ended=false;};
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
const savedState={score:60,best:90,balls:[makePlanet(2,160,400)],level:1,nextLevel:0,aim:160,cooldown:12,overTicks:0,ended:false,paused:false,discovered:new Set([0,1,2]),PLANETS,WIDTH,
  localStorage:{setItem:(k,v)=>storage.set(k,v),getItem:k=>storage.get(k)||null},
  $:id=>ui[id]??=( {textContent:'',style:{}} ),setOverlay:()=>{}};
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
