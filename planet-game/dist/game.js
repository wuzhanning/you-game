'use strict';
const $=id=>document.getElementById(id);
const canvas=$('game'),ctx=canvas.getContext('2d');
let mode='planet',shakes=3,mergeCount=0,shakeTicks=0;
try{if(localStorage.getItem('game-mode')==='number')mode='number';}catch{}
function kind(l){return bodySpec(l,mode);}
let bombs=0,bombMode=false,bombTarget=0;
let balls=[],score=0,best=0,level=0,nextLevel=0,aim=240,paused=false,ended=false,cooldown=0,overTicks=0,sound=true,audioContext,particles=[],discovered=new Set([0]);
try{const n=Number(localStorage.getItem(mode+'-best'));best=Number.isSafeInteger(n)&&n>=0?n:0;sound=localStorage.getItem('planet-sound')!=='off';}catch{}
$('best').textContent=best;
function circle(c,x,y,r,color){c.fillStyle=color;c.beginPath();c.arc(x,y,r,0,Math.PI*2);c.fill();}
function planet(c,l,x,y,r){
  const p=kind(l);c.save();c.translate(x,y);
  if(mode==='number'){
    circle(c,0,0,r,p.color);c.strokeStyle=p.shade;c.lineWidth=2;c.beginPath();c.arc(0,0,r-1,0,Math.PI*2);c.stroke();
    c.fillStyle='#302941';c.textAlign='center';c.textBaseline='middle';c.font='bold '+Math.min(r*.95,r*1.5/String(l).length)+'px system-ui';c.fillText(String(l),0,r*.04);c.restore();return;
  }
  if(l===10){
    c.save();c.rotate(-.35);c.strokeStyle='#bea2ef';c.lineWidth=r*.14;c.beginPath();c.ellipse(0,0,r*.92,r*.57,0,0,Math.PI*2);c.stroke();
    c.strokeStyle='#efbc86';c.lineWidth=r*.07;c.beginPath();c.ellipse(0,0,r*.96,r*.32,0,0,Math.PI*2);c.stroke();c.restore();
    circle(c,0,0,r*.56,'#241b38');circle(c,-r*.16,-r*.04,r*.04,'#e5d5ff');circle(c,r*.16,-r*.04,r*.04,'#e5d5ff');
    c.strokeStyle='#c3abed';c.lineWidth=r*.025;c.beginPath();c.arc(0,r*.05,r*.1,0,Math.PI);c.stroke();c.restore();return;
  }
  if(l===8||l===9){
    c.fillStyle=l===8?'#efae39':'#dc6272';
    for(let i=0;i<12;i++){const a=i*Math.PI/6;c.beginPath();c.moveTo(Math.cos(a-.16)*r*.76,Math.sin(a-.16)*r*.76);c.lineTo(Math.cos(a)*r,Math.sin(a)*r);c.lineTo(Math.cos(a+.16)*r*.76,Math.sin(a+.16)*r*.76);c.fill();}
    r*=.81;
  }
  if(l===6){c.save();c.rotate(-.35);c.strokeStyle='#c8a5d3';c.lineWidth=r*.22;c.beginPath();c.ellipse(0,0,r*1.37,r*.40,0,0,Math.PI*2);c.stroke();c.restore();}
  c.shadowColor=p.shade+'35';c.shadowBlur=9;c.shadowOffsetY=4;
  const g=c.createRadialGradient(-r*.38,-r*.42,r*.1,0,0,r);g.addColorStop(0,p.color);g.addColorStop(.72,p.color);g.addColorStop(1,p.shade);circle(c,0,0,r,g);c.shadowBlur=0;c.shadowOffsetY=0;
  c.save();c.beginPath();c.arc(0,0,r-.8,0,Math.PI*2);c.clip();
  if(l===4){c.fillStyle='#8fb79c';c.beginPath();c.moveTo(-r*.8,-r*.5);c.lineTo(-r*.23,-r*.87);c.lineTo(r*.05,-r*.5);c.lineTo(-r*.28,-r*.16);c.lineTo(-r*.53,-r*.08);c.lineTo(-r*.64,r*.32);c.lineTo(-r*.91,r*.1);c.fill();c.beginPath();c.ellipse(r*.65,r*.48,r*.36,r*.54,-.5,0,Math.PI*2);c.fill();}
  else if(l===7||l===3||l===5){c.strokeStyle=p.shade+'70';c.lineWidth=r*.16;for(let i=-2;i<3;i++){c.beginPath();c.ellipse(0,i*r*.43,r*1.2,r*.15,.12,0,Math.PI);c.stroke();}}
  else if(l===2){c.strokeStyle='#d7b582';c.lineWidth=r*.13;for(let i=-1;i<=1;i++){c.beginPath();c.ellipse(0,i*r*.47,r*1.1,r*.2,-.25,0,Math.PI);c.stroke();}}
  else if(l<2){circle(c,-r*.43,-r*.3,r*.18,p.shade+'55');circle(c,r*.48,r*.2,r*.12,p.shade+'55');circle(c,-r*.18,r*.55,r*.12,p.shade+'35');}
  circle(c,-r*.35,-r*.59,r*.16,'#ffffff3d');c.restore();
  const eyeY=r*.06,eyeX=r*.25;
  circle(c,-eyeX,eyeY,Math.max(1.5,r*.055),'#555067');circle(c,eyeX,eyeY,Math.max(1.5,r*.055),'#555067');
  c.strokeStyle='#66596b';c.lineWidth=Math.max(1.1,r*.027);c.lineCap='round';c.beginPath();c.arc(0,r*.13,r*.115,.1,Math.PI-.1);c.stroke();
  c.globalAlpha=.5;circle(c,-r*.40,r*.23,r*.10,'#efa6ab');circle(c,r*.40,r*.23,r*.10,'#efa6ab');c.globalAlpha=1;
  if(l===6){c.save();c.rotate(-.35);c.strokeStyle='#e2cbed';c.lineWidth=r*.13;c.beginPath();c.ellipse(0,0,r*1.37,r*.40,0,0,Math.PI);c.stroke();c.restore();}c.restore();
}
function drawSmall(el,l,r=20){const c=el.getContext('2d');c.clearRect(0,0,el.width,el.height);planet(c,l,el.width/2,el.height/2,r);}
function buildCollection(){
$('planet-list').replaceChildren();
for(let l=0;l<PLANETS.length;l++){const item=document.createElement('div');item.className='planet-item';item.innerHTML='<canvas width="112" height="112"></canvas><span></span>';$('planet-list').append(item);item.querySelector('span').textContent=kind(l).name;drawSmall(item.querySelector('canvas'),l,l===6?32:37);}
}
const goal=document.createElement('canvas');goal.width=180;goal.height=180;goal.style.width='90px';goal.style.height='90px';$('goal-art').append(goal);drawSmall(goal,4,63);
const demo=$('demo').getContext('2d');planet(demo,1,32,36,19);planet(demo,1,89,36,19);planet(demo,2,180,36,26);demo.font='18px system-ui';demo.fillStyle='#b0a4b9';demo.fillText('+',57,41);demo.fillText('→',125,41);
function update(){
  updateBomb();
  $('score').innerHTML=score+'<span>颗</span>';$('best').textContent=best;
  $('found').textContent=mode==='number'?'最大数字 '+Math.max(...discovered):discovered.size+' / '+PLANETS.length+' 已发现';
  $('mode-picker').textContent=mode==='number'?'数字模式 ▾':'星球模式 ▾';
  canvas.setAttribute('aria-label',mode==='number'?'数字合成游戏。方向键移动，空格投放，S 摇一摇，B 炸弹，M 切换模式。':'星球合成游戏。方向键移动，空格投放，S 摇一摇，B 炸弹，M 切换模式。');
  $('collection-title').textContent=mode==='number'?'数字成长图鉴（可继续升级）':'星球成长图鉴';
  const count=[0,1,2,3,4].filter(l=>discovered.has(l)).length;
  $('progress').style.width=count/5*100+'%';$('progress-text').textContent=discovered.has(4)?'地球已发现，继续探索吧！':'已发现 '+count+' / 5 种星球';
  [...$('planet-list').children].forEach((el,i)=>{el.className='planet-item '+(discovered.has(i)?'active':'locked');});drawSmall($('next'),nextLevel,15);
}
function soundButton(){
  $('sound').setAttribute('aria-pressed',String(sound));
  $('sound').setAttribute('aria-label',sound?'关闭音效':'开启音效');
  $('sound').style.background=sound?'#e6ddf1':'transparent';
}
function unlockAudio(){
  if(!sound)return;
  try{audioContext??=new(window.AudioContext||window.webkitAudioContext)();
    if(audioContext.state==='suspended')audioContext.resume().catch(()=>{});
  }catch{$('status').textContent='此浏览器暂不支持音效，仍可继续游戏';}
}
function beep(kind,l=0){
  if(!sound)return;unlockAudio();if(audioContext?.state!=='running')return;
  const notes=kind==='shake'?[294,392,294]:kind==='bomb'?[220,165,110]:kind==='merge'?[440,554,659]:kind==='end'?[523,440,349]:kind==='start'?[392,523]:[300,220];
  try{notes.forEach((frequency,i)=>{
    const time=audioContext.currentTime+i*.09,o=audioContext.createOscillator(),g=audioContext.createGain();
    o.type='sine';o.frequency.setValueAtTime(frequency*(kind==='merge'?1+Math.min(l,30)*.06:1),time);
    g.gain.setValueAtTime(.001,time);g.gain.exponentialRampToValueAtTime(.055,time+.012);g.gain.exponentialRampToValueAtTime(.001,time+.18);
    o.connect(g);g.connect(audioContext.destination);o.start(time);o.stop(time+.2);o.onended=()=>{o.disconnect();g.disconnect();};
  });}catch{}
}
function saveGame(){
  try{localStorage.setItem(mode+'-save',JSON.stringify({version:1,shakes,mergeCount,shakeTicks,bombs,score,best,balls,level,nextLevel,aim,cooldown,overTicks,ended,discovered:[...discovered]}));
    localStorage.setItem(mode+'-best',String(best));return true;
  }catch{$('save-status').textContent='浏览器存储不可用，本次进度无法保存';return false;}
}
function restoreGame(){
  try{
    const saved=JSON.parse(localStorage.getItem(mode+'-save'));
    if(!saved)return;
    const validLevel=n=>Number.isSafeInteger(n)&&n>=0&&(mode==='number'?true:n<PLANETS.length);
    const count=n=>Number.isSafeInteger(n)&&n>=0;
    if((saved.shakes!==undefined&&!count(saved.shakes))||(saved.mergeCount!==undefined&&(!count(saved.mergeCount)||saved.mergeCount>4))||(saved.shakeTicks!==undefined&&(!count(saved.shakeTicks)||saved.shakeTicks>48))||(saved.bombs!==undefined&&!count(saved.bombs))||saved.version!==1||!count(saved.score)||!count(saved.best)||!validLevel(saved.level)||!validLevel(saved.nextLevel)||!Number.isFinite(saved.aim)||saved.aim<0||saved.aim>WIDTH||!count(saved.cooldown)||saved.cooldown>42||!count(saved.overTicks)||saved.overTicks>151||typeof saved.ended!=='boolean'||!Array.isArray(saved.discovered)||!saved.discovered.every(validLevel)||!Array.isArray(saved.balls)||saved.balls.length>500||!saved.balls.every(b=>b&&validLevel(b.level)&&['x','y','vx','vy'].every(k=>Number.isFinite(b[k])&&Math.abs(b[k])<10000)&&count(b.age)))throw new Error('Invalid save');
    shakes=saved.shakes??3;mergeCount=saved.mergeCount??0;shakeTicks=saved.shakeTicks??0;
    bombs=saved.bombs??0;bombMode=false;
    ({score,balls,level,nextLevel,aim,cooldown,overTicks,ended}=saved);best=Math.max(best,saved.best,score);discovered=new Set(saved.discovered);paused=!ended;
    $('hint').style.opacity=balls.length?0:1;$('pause').textContent=paused?'▷ 继续':'Ⅱ 暂停';
    setOverlay(ended?'上次旅程已完成':'欢迎回来，小宇航员！','已恢复 '+score+' 颗星光和当前进度。',ended?'再玩一次':'继续探索');
  }catch{$('save-status').textContent='存档无法读取，已开启新旅程';}
}
function merged(b){const earned=Math.min(Number.MAX_SAFE_INTEGER,2**Math.min(b.level,50)*5);score=Math.min(Number.MAX_SAFE_INTEGER,score+earned);discovered.add(b.level);if(score>best)best=score;
  $('status').textContent='✦ 好棒！合成了'+kind(b.level).name+'，收获 '+earned+' 颗星光';
  mergeCount++;if(mergeCount===5){mergeCount=0;shakes++;$('status').textContent+=' · 摇一摇 +1';}
  if(b.level===7){bombs++;$('status').textContent+=' · 获得 1 颗炸弹！';}
  for(let i=0;i<12;i++){const a=i*Math.PI/6;particles.push({x:b.x,y:b.y,vx:Math.cos(a)*3,vy:Math.sin(a)*3,life:36,color:kind(b.level).shade});}beep('merge',b.level);update();saveGame();
}
function drop(x=aim){if(bombMode||paused||ended||cooldown>0||document.querySelector('dialog[open]'))return false;const r=kind(level).r;aim=Math.max(r+9,Math.min(WIDTH-r-9,x));balls.push(makePlanet(level,aim,45));discovered.add(level);level=nextLevel;nextLevel=mode==='number'?0:Math.floor(Math.random()*3);cooldown=42;$('hint').style.opacity=0;beep('drop');update();saveGame();return true;}
function setOverlay(title,copy,button){$('overlay-title').textContent=title;$('overlay-copy').textContent=copy;$('resume').textContent=button;$('overlay').hidden=false;}
function togglePause(){if(ended)return;bombMode=false;paused=!paused;updateBomb();$('pause').textContent=paused?'▷ 继续':'Ⅱ 暂停';if(paused)setOverlay('休息一下','小星球会在这里等你','继续探索');else $('overlay').hidden=true;saveGame();}
function reset(persist=true){shakes=3;mergeCount=0;shakeTicks=0;bombs=0;bombMode=false;bombTarget=0;balls=[];particles=[];score=0;level=0;nextLevel=0;aim=240;paused=false;ended=false;cooldown=0;overTicks=0;discovered=new Set([0]);$('overlay').hidden=true;$('pause').textContent='Ⅱ 暂停';$('hint').style.opacity=1;$('status').textContent=mode==='number'?'✧ 两个相同数字合成下一个数字':'✧ 两颗相同的星球，会变成一颗新星球';update();if(persist){saveGame();beep('start');}}
function switchMode(next){
  if(!['planet','number'].includes(next))return false;
  if(next===mode)return true;
  if(!saveGame())return false;
  mode=next;best=0;
  try{const n=Number(localStorage.getItem(mode+'-best'));if(Number.isSafeInteger(n)&&n>=0)best=n;localStorage.setItem('game-mode',mode);}catch{}
  planetSprites.clear();lastDrawKey='';reset(false);restoreGame();buildCollection();update();return true;
}
function useShake(){
  if(shakes<1||shakeTicks>0||bombMode||paused||ended||balls.length===0||document.querySelector('dialog[open]'))return false;
  shakes--;shakeTicks=48;overTicks=0;
  if(!window.matchMedia?.('(prefers-reduced-motion: reduce)').matches)canvas.parentElement.animate?.([{transform:'translateX(0)'},{transform:'translateX(-5px)'},{transform:'translateX(5px)'},{transform:'translateX(-3px)'},{transform:'translateX(0)'}],{duration:600});
  $('status').textContent='↔ 摇一摇，让相同的小伙伴靠近！';beep('shake');updateBomb();saveGame();return true;
}
function updateBomb(){
  $('shake').textContent='↔ 摇一摇 '+shakes;
  $('shake').setAttribute('aria-label','摇一摇，剩余 '+shakes+' 次，每 5 次合成奖励 1 次');
  $('shake').disabled=shakes<1||shakeTicks>0||bombMode||paused||ended||balls.length===0;
  $('bomb').textContent=bombMode?'取消消除':'💣 '+bombs;
  $('bomb').setAttribute('aria-pressed',String(bombMode));
  $('bomb').setAttribute('aria-label',bombMode?'取消消除':'使用炸弹，剩余 '+bombs+' 颗');
  $('bomb').disabled=!bombMode&&(bombs===0||balls.length===0||paused||ended);
}
function toggleBomb(){
  if(paused||ended||document.querySelector('dialog[open]'))return;
  if(!bombMode&&(bombs===0||balls.length===0))return;
  bombMode=!bombMode;bombTarget=0;updateBomb();
  $('status').textContent=bombMode?'点选星球消除；← → 选目标，A / 空格确认，B 取消':'已取消消除，继续投放吧';
}
function selectBombTarget(direction){
  if(!bombMode||!balls.length)return;
  bombTarget=(bombTarget+direction+balls.length)%balls.length;
  $('status').textContent='已选中 '+kind(balls[bombTarget].level).name+'（'+(bombTarget+1)+' / '+balls.length+'），按 A / 空格消除';
}
function removePlanet(index){
  if(!bombMode||bombs<1||paused||ended||document.querySelector('dialog[open]')||!Number.isInteger(index)||index<0||index>=balls.length)return false;
  const [b]=balls.splice(index,1);bombs--;bombMode=false;overTicks=0;
  for(let i=0;i<16;i++){const a=i*Math.PI/8;particles.push({x:b.x,y:b.y,vx:Math.cos(a)*3,vy:Math.sin(a)*3,life:36,color:kind(b.level).shade});}
  $('status').textContent='✦ 已消除'+kind(b.level).name+'，腾出新空间！';beep('bomb');update();saveGame();return true;
}
// Render each planet once; frames only copy these small cached bitmaps.
const planetSprites=new Map();
const background=document.createElement('canvas');background.width=WIDTH;background.height=HEIGHT;
const bg=background.getContext('2d');bg.fillStyle='#f6f2fc';bg.fillRect(0,0,WIDTH,HEIGHT);
for(let i=0;i<37;i++){const x=(i*137+23)%WIDTH,y=(i*193+167)%HEIGHT;bg.fillStyle=i%3===0?'#d9cfeb':'#e7dff0';if(i%4===0){bg.fillRect(x-3,y,7,1);bg.fillRect(x,y-3,1,7);}else circle(bg,x,y,1.5,bg.fillStyle);}
function drawPlanet(l,x,y){
  let image=planetSprites.get(l);
  if(!image){
    image=document.createElement('canvas');image.width=image.height=Math.ceil((kind(l).r+36)*2);
    planet(image.getContext('2d'),l,image.width/2,image.height/2,kind(l).r);
    // Bound the cache even when numeric levels grow beyond the planet chart.
    if(planetSprites.size>=32)planetSprites.delete(planetSprites.keys().next().value);
    planetSprites.set(l,image);
  }
  ctx.drawImage(image,x-image.width/2,y-image.height/2);
}
function draw(){
 ctx.drawImage(background,0,0);
 ctx.strokeStyle=overTicks>0?'#e6a0a7':'#dbd0e7';ctx.lineWidth=1;ctx.setLineDash([5,7]);ctx.beginPath();ctx.moveTo(15,LIMIT);ctx.lineTo(WIDTH-15,LIMIT);ctx.stroke();ctx.setLineDash([]);ctx.font='11px system-ui';ctx.fillStyle='#b6a7c6';ctx.fillText('星 光 线',17,LIMIT-12);
 if(!ended&&!bombMode){const r=kind(level).r,x=Math.max(r+9,Math.min(WIDTH-r-9,aim));ctx.save();ctx.strokeStyle='#c6b5da';ctx.setLineDash([3,7]);ctx.beginPath();ctx.moveTo(x,65);ctx.lineTo(x,HEIGHT-12);ctx.stroke();ctx.restore();ctx.globalAlpha=cooldown>0?.35:1;drawPlanet(level,x,43);ctx.globalAlpha=1;ctx.fillStyle='#b6a0cc';ctx.beginPath();ctx.moveTo(x-4,10);ctx.lineTo(x+4,10);ctx.lineTo(x,15);ctx.fill();}
 for(const b of balls)drawPlanet(b.level,b.x,b.y);
 if(bombMode&&balls[bombTarget]){const b=balls[bombTarget];ctx.strokeStyle='#8460b3';ctx.lineWidth=3;ctx.setLineDash([6,4]);ctx.beginPath();ctx.arc(b.x,b.y,kind(b.level).r+5,0,Math.PI*2);ctx.stroke();ctx.setLineDash([]);}
 for(const p of particles){ctx.globalAlpha=p.life/36;circle(ctx,p.x,p.y,2.5,p.color);}ctx.globalAlpha=1;
}
// Standard mapping: left stick / D-pad, south face button, Start.
const STICK_DEADZONE=.2;
let padKey=null,padButtons=[],padMessage='';
function pollGamepad(elapsed){
  let pads=[];
  try{pads=Array.from(navigator.getGamepads?.()||[]).filter(p=>p?.connected);}catch{}
  const pad=pads.find(p=>p.mapping==='standard');
  const key=pad?pad.index+':'+pad.id:null;
  const message=pad?'手柄已连接 · 左摇杆 / 方向键移动 · A / × 投放 · B 炸弹 · X 摇一摇 · Select 模式 · Start 暂停':pads.length?'此手柄按键布局未识别，请使用标准手柄模式':'手柄未就绪 · 连接后按一下手柄按键';
  if(message!==padMessage){$('gamepad-status').textContent=message;padMessage=message;}
  const changed=key!==padKey;
  if(changed&&padKey!==null&&!paused&&!ended)togglePause();
  const buttons=pad?pad.buttons.map(b=>b.pressed):[];
  const pressed=i=>!changed&&buttons[i]&&!padButtons[i];
  padKey=key;
  if(pad&&!document.hidden&&document.hasFocus()){
    const dialog=document.querySelector('dialog[open]');
    if(dialog){
      if(pressed(1)||pressed(9))dialog.close();
      else if(pressed(0)){
        const focused=document.activeElement;
        if(focused?.tagName==='BUTTON'&&dialog.contains(focused))focused.click();
        else dialog.querySelector('button')?.focus();
      }
      const choices=[...dialog.querySelectorAll('button')];
      if(pressed(14)||pressed(15)){
        const index=choices.indexOf(document.activeElement);
        choices[(index+(pressed(14)?-1:1)+choices.length)%choices.length]?.focus();
      }
    }else if(pressed(8)){$('mode-dialog').showModal();}
    else if(pressed(9)){togglePause();}
    else if(pressed(0)&&(paused||ended)){ended?reset():togglePause();}
    else if(!paused&&!ended&&pressed(2)){useShake();}
    else if(!paused&&!ended&&pressed(1)){toggleBomb();}
    else if(!paused&&!ended&&bombMode){
      if(pressed(14)||pressed(12))selectBombTarget(-1);
      if(pressed(15)||pressed(13))selectBombTarget(1);
      if(pressed(0))removePlanet(bombTarget);
    }
    else if(!paused&&!ended){
      const axis=Number.isFinite(pad.axes[0])?pad.axes[0]:0;
      const stick=Math.abs(axis)>STICK_DEADZONE?Math.sign(axis)*(Math.min(1,Math.abs(axis))-STICK_DEADZONE)/(1-STICK_DEADZONE):0;
      const direction=buttons[14]||buttons[15]?Number(!!buttons[15])-Number(!!buttons[14]):stick;
      const r=kind(level).r;
      aim=Math.max(r+9,Math.min(WIDTH-r-9,aim+direction*300*elapsed/1000));
      if(pressed(0))drop();
    }
  }
  // Always sample held buttons, including while dialogs or another window have focus.
  padButtons=buttons;
}
window.addEventListener('blur',()=>{if(!paused&&!ended)togglePause();});

let previous=0,accumulator=0,frameId=0,idleTimer=0,lastDrawKey='';
function stopFrames(){cancelAnimationFrame(frameId);clearTimeout(idleTimer);frameId=0;idleTimer=0;previous=0;accumulator=0;}
function wakeFrames(){stopFrames();if(!document.hidden)frameId=requestAnimationFrame(frame);}

function frame(now){
 if(document.hidden){stopFrames();return;}
 // Cap painting at 30 fps even on 120/144 Hz screens; physics stays at 60 Hz.
 if(previous&&now-previous<1000/30-1){frameId=requestAnimationFrame(frame);return;}
 const elapsed=previous?Math.min(now-previous,50):0;previous=now;pollGamepad(elapsed);
 const running=!bombMode&&!paused&&!ended&&!document.querySelector('dialog[open]');
 if(running){accumulator+=elapsed;while(accumulator>=1000/60){accumulator-=1000/60;if(cooldown>0)cooldown--;if(shakeTicks>0){shakeBodies(balls,shakeTicks);shakeTicks--;if(shakeTicks===0)updateBomb();}advance(balls,merged,mode);for(const p of particles){p.x+=p.vx;p.y+=p.vy;p.vy+=.03;p.life--;}particles=particles.filter(p=>p.life>0);const overflow=balls.some(b=>b.age>160&&b.y-kind(b.level).r<LIMIT);overTicks=overflow?overTicks+1:0;if(overTicks>150){ended=true;updateBomb();beep('end');saveGame();setOverlay('这次旅程，真棒！','你收集了 '+score+' 颗星光。准备好探索新的宇宙了吗？','再玩一次');accumulator=0;break;}}}else accumulator=0;
 const drawKey=[mode,paused,ended,bombMode,bombTarget,aim,balls.length].join(':');
 if(running||drawKey!==lastDrawKey){draw();lastDrawKey=drawKey;}
 if(running)frameId=requestAnimationFrame(frame);
 else idleTimer=setTimeout(()=>{frameId=requestAnimationFrame(frame);},100);
}
function pointerX(e){const rect=canvas.getBoundingClientRect();return Math.max(0,Math.min(WIDTH,(e.clientX-rect.left)*WIDTH/rect.width));}
canvas.addEventListener('pointermove',e=>{aim=pointerX(e);});
canvas.addEventListener('pointerdown',e=>{e.preventDefault();canvas.focus({preventScroll:true});aim=pointerX(e);canvas.setPointerCapture(e.pointerId);});
canvas.addEventListener('pointerup',e=>{
  if(!bombMode){drop(pointerX(e));return;}
  const rect=canvas.getBoundingClientRect(),x=(e.clientX-rect.left)*WIDTH/rect.width,y=(e.clientY-rect.top)*HEIGHT/rect.height;
  const index=balls.findLastIndex(b=>Math.hypot(b.x-x,b.y-y)<=kind(b.level).r);
  removePlanet(index);
});
canvas.addEventListener('keydown',e=>{
  if(['s','S'].includes(e.key)){e.preventDefault();if(!e.repeat)useShake();return;}
  if(['m','M'].includes(e.key)){e.preventDefault();if(!e.repeat)$('mode-dialog').showModal();return;}
  if(['b','B','Escape'].includes(e.key)){e.preventDefault();if(!e.repeat&&(e.key!=='Escape'||bombMode))toggleBomb();return;}
  if(!['ArrowLeft','ArrowRight','ArrowUp','ArrowDown',' ','Enter'].includes(e.key))return;
  e.preventDefault();
  if(bombMode){if(e.key.startsWith('Arrow'))selectBombTarget(['ArrowLeft','ArrowUp'].includes(e.key)?-1:1);else if(!e.repeat)removePlanet(bombTarget);return;}
  if(e.key==='ArrowLeft')aim=Math.max(35,aim-18);else if(e.key==='ArrowRight')aim=Math.min(WIDTH-35,aim+18);else if(!e.repeat&&(e.key===' '||e.key==='Enter'))drop();
});
$('shake').onclick=()=>{useShake();canvas.focus({preventScroll:true});};
$('mode-picker').onclick=()=>$('mode-dialog').showModal();
document.querySelectorAll('[data-mode]').forEach(button=>button.onclick=()=>{if(switchMode(button.dataset.mode))$('mode-dialog').close();});
$('close-mode').onclick=()=>$('mode-dialog').close();
$('bomb').onclick=()=>{toggleBomb();canvas.focus({preventScroll:true});};
$('pause').onclick=togglePause;$('resume').onclick=()=>ended?reset():togglePause();
$('restart').onclick=()=>$('restart-dialog').showModal();$('cancel-reset').onclick=()=>$('restart-dialog').close();$('confirm-reset').onclick=()=>{$('restart-dialog').close();reset();};
$('help').onclick=()=>$('help-dialog').showModal();document.querySelectorAll('#help-dialog .close').forEach(b=>b.onclick=()=>$('help-dialog').close());
$('sound').onclick=()=>{sound=!sound;soundButton();try{localStorage.setItem('planet-sound',sound?'on':'off');}catch{}if(sound)beep('start');else audioContext?.suspend().catch(()=>{});};
document.addEventListener('pointerdown',unlockAudio,{capture:true});
document.addEventListener('keydown',unlockAudio,{capture:true});
$('fullscreen').onclick=async()=>{
  try{if(document.fullscreenElement)await document.exitFullscreen();
    else if(document.documentElement.requestFullscreen)await document.documentElement.requestFullscreen();
    else $('status').textContent='已铺满页面；此浏览器不支持隐藏地址栏';
  }catch{$('status').textContent='已铺满页面，浏览器暂未允许系统全屏';}
};
document.addEventListener('fullscreenchange',()=>{$('fullscreen').textContent=document.fullscreenElement?'退出全屏':'全屏';});
window.addEventListener('pagehide',()=>{saveGame();stopFrames();});
window.addEventListener('pageshow',wakeFrames);
setInterval(()=>{if(!paused&&!ended&&!document.hidden)saveGame();},1000);
document.addEventListener('visibilitychange',()=>{if(document.hidden){if(!paused&&!ended)togglePause();saveGame();stopFrames();audioContext?.suspend().catch(()=>{});}else wakeFrames();});
if(document.modelContext?.registerTool){try{Promise.resolve(document.modelContext.registerTool({name:'drop_planet',description:'在当前游戏横坐标 0 到 480 的位置投放一颗星球。',inputSchema:{type:'object',properties:{x:{type:'number',minimum:0,maximum:480}},required:['x'],additionalProperties:false},annotations:{readOnlyHint:false},execute(input){if(!input||typeof input.x!=='number'||!Number.isFinite(input.x)||input.x<0||input.x>480)throw new Error('x 必须在 0 到 480 之间');const dropped=drop(input.x);return {dropped,score,planetCount:balls.length};}})).catch(()=>{});}catch{}}
restoreGame();buildCollection();soundButton();update();wakeFrames();
