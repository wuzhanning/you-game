'use strict';
const $=id=>document.getElementById(id);
const canvas=$('game'),ctx=canvas.getContext('2d');
let balls=[],score=0,best=0,level=0,nextLevel=0,aim=240,paused=false,ended=false,cooldown=0,overTicks=0,sound=false,audioContext,particles=[],discovered=new Set([0]);
try{best=Math.max(0,Number(localStorage.getItem('planet-best'))||0);}catch{}
$('best').textContent=best;
function circle(c,x,y,r,color){c.fillStyle=color;c.beginPath();c.arc(x,y,r,0,Math.PI*2);c.fill();}
function planet(c,l,x,y,r){
  const p=PLANETS[l];c.save();c.translate(x,y);
  if(l===6){c.save();c.rotate(-.35);c.strokeStyle='#c8a5d3';c.lineWidth=r*.22;c.beginPath();c.ellipse(0,0,r*1.37,r*.40,0,0,Math.PI*2);c.stroke();c.restore();}
  c.shadowColor=p.shade+'35';c.shadowBlur=9;c.shadowOffsetY=4;
  const g=c.createRadialGradient(-r*.38,-r*.42,r*.1,0,0,r);g.addColorStop(0,p.color);g.addColorStop(.72,p.color);g.addColorStop(1,p.shade);circle(c,0,0,r,g);c.shadowBlur=0;c.shadowOffsetY=0;
  c.save();c.beginPath();c.arc(0,0,r-.8,0,Math.PI*2);c.clip();
  if(l===4){c.fillStyle='#8fb79c';c.beginPath();c.moveTo(-r*.8,-r*.5);c.lineTo(-r*.23,-r*.87);c.lineTo(r*.05,-r*.5);c.lineTo(-r*.28,-r*.16);c.lineTo(-r*.53,-r*.08);c.lineTo(-r*.64,r*.32);c.lineTo(-r*.91,r*.1);c.fill();c.beginPath();c.ellipse(r*.65,r*.48,r*.36,r*.54,-.5,0,Math.PI*2);c.fill();}
  else if(l===7||l===3||l===5){c.strokeStyle=p.shade+'70';c.lineWidth=r*.16;for(let i=-2;i<3;i++){c.beginPath();c.ellipse(0,i*r*.43,r*1.2,r*.15,.12,0,Math.PI);c.stroke();}}
  else if(l<3){circle(c,-r*.43,-r*.3,r*.18,p.shade+'55');circle(c,r*.48,r*.2,r*.12,p.shade+'55');circle(c,-r*.18,r*.55,r*.12,p.shade+'35');}
  circle(c,-r*.35,-r*.59,r*.16,'#ffffff3d');c.restore();
  const eyeY=r*.06,eyeX=r*.25;
  circle(c,-eyeX,eyeY,Math.max(1.5,r*.055),'#555067');circle(c,eyeX,eyeY,Math.max(1.5,r*.055),'#555067');
  c.strokeStyle='#66596b';c.lineWidth=Math.max(1.1,r*.027);c.lineCap='round';c.beginPath();c.arc(0,r*.13,r*.115,.1,Math.PI-.1);c.stroke();
  c.globalAlpha=.5;circle(c,-r*.40,r*.23,r*.10,'#efa6ab');circle(c,r*.40,r*.23,r*.10,'#efa6ab');c.globalAlpha=1;
  if(l===6){c.save();c.rotate(-.35);c.strokeStyle='#e2cbed';c.lineWidth=r*.13;c.beginPath();c.ellipse(0,0,r*1.37,r*.40,0,0,Math.PI);c.stroke();c.restore();}c.restore();
}
function drawSmall(el,l,r=20){const c=el.getContext('2d');c.clearRect(0,0,el.width,el.height);planet(c,l,el.width/2,el.height/2,r);}
for(let l=0;l<PLANETS.length;l++){const item=document.createElement('div');item.className='planet-item';item.innerHTML='<canvas width="112" height="112"></canvas><span></span>';$('planet-list').append(item);item.querySelector('span').textContent=PLANETS[l].name;drawSmall(item.querySelector('canvas'),l,l===6?32:37);}
const goal=document.createElement('canvas');goal.width=180;goal.height=180;goal.style.width='90px';goal.style.height='90px';$('goal-art').append(goal);drawSmall(goal,4,63);
const demo=$('demo').getContext('2d');planet(demo,1,32,36,19);planet(demo,1,89,36,19);planet(demo,2,180,36,26);demo.font='18px system-ui';demo.fillStyle='#b0a4b9';demo.fillText('+',57,41);demo.fillText('→',125,41);
function update(){
  $('score').innerHTML=score+'<span>颗</span>';$('best').textContent=best;
  $('found').textContent=discovered.size+' / 9 已发现';
  const count=[0,1,2,3,4].filter(l=>discovered.has(l)).length;
  $('progress').style.width=count/5*100+'%';$('progress-text').textContent=discovered.has(4)?'地球已发现，继续探索吧！':'已发现 '+count+' / 5 种星球';
  [...$('planet-list').children].forEach((el,i)=>{el.className='planet-item '+(discovered.has(i)?'active':'locked');});drawSmall($('next'),nextLevel,15);
}
function beep(l){if(!sound)return;try{audioContext??=new(window.AudioContext||window.webkitAudioContext)();audioContext.resume();const o=audioContext.createOscillator(),g=audioContext.createGain();o.type='sine';o.frequency.value=330*Math.pow(1.14,l);g.gain.setValueAtTime(.07,audioContext.currentTime);g.gain.exponentialRampToValueAtTime(.001,audioContext.currentTime+.25);o.connect(g).connect(audioContext.destination);o.start();o.stop(audioContext.currentTime+.26);}catch{sound=false;$('sound').setAttribute('aria-pressed','false');}}
function merged(b){score+=2**b.level*5;discovered.add(b.level);if(score>best){best=score;try{localStorage.setItem('planet-best',String(best));}catch{}}
  $('status').textContent='✦ 好棒！合成了'+PLANETS[b.level].name+'，收获 '+2**b.level*5+' 颗星光';
  for(let i=0;i<12;i++){const a=i*Math.PI/6;particles.push({x:b.x,y:b.y,vx:Math.cos(a)*3,vy:Math.sin(a)*3,life:36,color:PLANETS[b.level].shade});}beep(b.level);update();
}
function drop(x=aim){if(paused||ended||cooldown>0||document.querySelector('dialog[open]'))return false;const r=PLANETS[level].r;aim=Math.max(r+9,Math.min(WIDTH-r-9,x));balls.push(makePlanet(level,aim,45));discovered.add(level);level=nextLevel;nextLevel=Math.floor(Math.random()*3);cooldown=42;$('hint').style.opacity=0;beep(0);update();return true;}
function setOverlay(title,copy,button){$('overlay-title').textContent=title;$('overlay-copy').textContent=copy;$('resume').textContent=button;$('overlay').hidden=false;}
function togglePause(){if(ended)return;paused=!paused;$('pause').textContent=paused?'▷ 继续':'Ⅱ 暂停';if(paused)setOverlay('休息一下','小星球会在这里等你','继续探索');else $('overlay').hidden=true;}
function reset(){balls=[];particles=[];score=0;level=0;nextLevel=0;aim=240;paused=false;ended=false;cooldown=0;overTicks=0;discovered=new Set([0]);$('overlay').hidden=true;$('pause').textContent='Ⅱ 暂停';$('hint').style.opacity=1;$('status').textContent='✧ 两颗相同的星球，会变成一颗新星球';update();}
function draw(){
 ctx.clearRect(0,0,WIDTH,HEIGHT);ctx.fillStyle='#f6f2fc';ctx.fillRect(0,0,WIDTH,HEIGHT);
 for(let i=0;i<37;i++){const x=(i*137+23)%WIDTH,y=(i*193+167)%HEIGHT;ctx.fillStyle=i%3===0?'#d9cfeb':'#e7dff0';if(i%4===0){ctx.fillRect(x-3,y,7,1);ctx.fillRect(x,y-3,1,7);}else circle(ctx,x,y,1.5,ctx.fillStyle);}
 ctx.strokeStyle=overTicks>0?'#e6a0a7':'#dbd0e7';ctx.lineWidth=1;ctx.setLineDash([5,7]);ctx.beginPath();ctx.moveTo(15,LIMIT);ctx.lineTo(WIDTH-15,LIMIT);ctx.stroke();ctx.setLineDash([]);ctx.font='11px system-ui';ctx.fillStyle='#b6a7c6';ctx.fillText('星 光 线',17,LIMIT-12);
 if(!ended){const r=PLANETS[level].r,x=Math.max(r+9,Math.min(WIDTH-r-9,aim));ctx.save();ctx.strokeStyle='#c6b5da';ctx.setLineDash([3,7]);ctx.beginPath();ctx.moveTo(x,65);ctx.lineTo(x,HEIGHT-12);ctx.stroke();ctx.restore();ctx.globalAlpha=cooldown>0?.35:1;planet(ctx,level,x,43,r);ctx.globalAlpha=1;ctx.fillStyle='#b6a0cc';ctx.beginPath();ctx.moveTo(x-4,10);ctx.lineTo(x+4,10);ctx.lineTo(x,15);ctx.fill();}
 for(const b of balls)planet(ctx,b.level,b.x,b.y,PLANETS[b.level].r);
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
  const message=pad?'手柄已连接 · 左摇杆 / 方向键移动 · A / × 投放 · Start 暂停':pads.length?'此手柄按键布局未识别，请使用标准手柄模式':'手柄未就绪 · 连接后按一下手柄按键';
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
    }else if(pressed(9)){togglePause();}
    else if(pressed(0)&&(paused||ended)){ended?reset():togglePause();}
    else if(!paused&&!ended){
      const axis=Number.isFinite(pad.axes[0])?pad.axes[0]:0;
      const stick=Math.abs(axis)>STICK_DEADZONE?Math.sign(axis)*(Math.min(1,Math.abs(axis))-STICK_DEADZONE)/(1-STICK_DEADZONE):0;
      const direction=buttons[14]||buttons[15]?Number(!!buttons[15])-Number(!!buttons[14]):stick;
      const r=PLANETS[level].r;
      aim=Math.max(r+9,Math.min(WIDTH-r-9,aim+direction*300*elapsed/1000));
      if(pressed(0))drop();
    }
  }
  // Always sample held buttons, including while dialogs or another window have focus.
  padButtons=buttons;
}
window.addEventListener('blur',()=>{if(!paused&&!ended)togglePause();});

let previous=0,accumulator=0;
function frame(now){const elapsed=previous?Math.min(now-previous,50):0;previous=now;pollGamepad(elapsed);if(!paused&&!ended&&!document.hidden&&!document.querySelector('dialog[open]')){accumulator+=elapsed;while(accumulator>=1000/60){accumulator-=1000/60;if(cooldown>0)cooldown--;advance(balls,merged);for(const p of particles){p.x+=p.vx;p.y+=p.vy;p.vy+=.03;p.life--;}particles=particles.filter(p=>p.life>0);const overflow=balls.some(b=>b.age>160&&b.y-PLANETS[b.level].r<LIMIT);overTicks=overflow?overTicks+1:0;if(overTicks>150){ended=true;setOverlay('这次旅程，真棒！','你收集了 '+score+' 颗星光。准备好探索新的宇宙了吗？','再玩一次');accumulator=0;break;}}}else accumulator=0;draw();requestAnimationFrame(frame);}
function pointerX(e){const rect=canvas.getBoundingClientRect();return(e.clientX-rect.left)*WIDTH/rect.width;}
canvas.addEventListener('pointermove',e=>{aim=pointerX(e);});
canvas.addEventListener('pointerdown',e=>{e.preventDefault();canvas.focus({preventScroll:true});aim=pointerX(e);canvas.setPointerCapture(e.pointerId);});
canvas.addEventListener('pointerup',e=>{drop(pointerX(e));});
canvas.addEventListener('keydown',e=>{if(['ArrowLeft','ArrowRight',' ','Enter'].includes(e.key)){e.preventDefault();if(e.key==='ArrowLeft')aim=Math.max(35,aim-18);else if(e.key==='ArrowRight')aim=Math.min(WIDTH-35,aim+18);else drop();}});
$('pause').onclick=togglePause;$('resume').onclick=()=>ended?reset():togglePause();
$('restart').onclick=()=>$('restart-dialog').showModal();$('cancel-reset').onclick=()=>$('restart-dialog').close();$('confirm-reset').onclick=()=>{$('restart-dialog').close();reset();};
$('help').onclick=()=>$('help-dialog').showModal();document.querySelectorAll('#help-dialog .close').forEach(b=>b.onclick=()=>$('help-dialog').close());
$('sound').onclick=()=>{sound=!sound;$('sound').setAttribute('aria-pressed',String(sound));$('sound').setAttribute('aria-label',sound?'关闭音效':'开启音效');$('sound').style.background=sound?'#e6ddf1':'transparent';beep(2);};
document.addEventListener('visibilitychange',()=>{if(document.hidden&&!paused&&!ended)togglePause();});
if(document.modelContext?.registerTool){try{Promise.resolve(document.modelContext.registerTool({name:'drop_planet',description:'在当前游戏横坐标 0 到 480 的位置投放一颗星球。',inputSchema:{type:'object',properties:{x:{type:'number',minimum:0,maximum:480}},required:['x'],additionalProperties:false},annotations:{readOnlyHint:false},execute(input){if(!input||typeof input.x!=='number'||!Number.isFinite(input.x)||input.x<0||input.x>480)throw new Error('x 必须在 0 到 480 之间');const dropped=drop(input.x);return {dropped,score,planetCount:balls.length};}})).catch(()=>{});}catch{}}
update();requestAnimationFrame(frame);
