'use strict';
const classicDirections=['left','up','right','down'];
function direction2048(x=0,y=0){
  if(Math.max(Math.abs(x),Math.abs(y))<.5)return null;
  return Math.abs(x)>Math.abs(y)?(x<0?'left':'right'):(y<0?'up':'down');
}
function slide2048(board,direction){
  const next=Array(16).fill(0),tiles=[],merges=[];
  let points=0;
  for(let line=0;line<4;line++){
    const cells=[0,1,2,3].map(i=>{
      const offset=direction==='right'||direction==='down'?3-i:i;
      return direction==='up'||direction==='down'?offset*4+line:line*4+offset;
    });
    const occupied=cells.filter(i=>board[i]);
    for(let i=0,destination=0;i<occupied.length;i++,destination++){
      const from=occupied[i],to=cells[destination];
      let value=board[from];
      tiles.push({from,to});
      if(value===board[occupied[i+1]]){
        value*=2;points+=value;
        tiles.push({from:occupied[++i],to});
        merges.push(to);
      }
      next[to]=value;
    }
  }
  return {next,points,tiles,merges,changed:tiles.some(({from,to})=>from!==to)};
}
function canMove2048(board){
  return board.some((n,i)=>!n||(i%4<3&&n===board[i+1])||(i<12&&n===board[i+4]));
}
function spawn2048(board,random=Math.random){
  const empty=board.map((n,i)=>n?-1:i).filter(i=>i>=0);
  if(empty.length)board[empty[Math.floor(random()*empty.length)]]=random()<.9?2:4;
}
if(typeof module!=='undefined')module.exports={slide2048,canMove2048,spawn2048};

function initClassic(){
  const panel=$('classic'),boardEl=$('classic-board'),menu=$('classic-menu');
  const cells=Array.from({length:16},()=>{
    const el=document.createElement('span');el.className='classic-tile';boardEl.append(el);return el;
  });
  let round,gesture,slideLayer,animations=[];
  const won=()=>!round.continued&&round.board.includes(2048);
  function read(){
    try{
      const saved=JSON.parse(localStorage.getItem('classic-2048-save'));
      const count=n=>Number.isSafeInteger(n)&&n>=0;
      if(saved&&Array.isArray(saved.board)&&saved.board.length===16&&saved.board.some(Boolean)&&
         saved.board.every(n=>n===0||(count(n)&&n>=2&&Number.isInteger(Math.log2(n))))&&
         count(saved.score)&&count(saved.best)&&saved.best>=saved.score&&typeof saved.continued==='boolean')return saved;
    }catch(error){}
    return {board:Array(16).fill(0),score:0,best:0,continued:false};
  }
  function save(){
    try{localStorage.setItem('classic-2048-save',JSON.stringify(round));$('classic-save').hidden=true;}
    catch(error){$('classic-save').hidden=false;}
  }
  function render(){
    cells.forEach((cell,i)=>{
      const n=round.board[i];cell.textContent=n||'';cell.dataset.value=n;
      if(n>2048)cell.setAttribute('data-large','');else cell.removeAttribute('data-large');
    });
    $('classic-score').textContent=round.score;$('classic-best').textContent=round.best;
    $('classic-continue').hidden=!won();
    $('classic-status').textContent=won()?'你合成了 2048！':!canMove2048(round.board)?'没有可移动的方块，本局结束。':'滑动方块，合成 '+(round.continued?'更大的数字':'2048')+'！';
  }
  function clearMotion(){
    animations.forEach(animation=>animation.cancel());
    animations=[];
    if(slideLayer){slideLayer.remove();slideLayer=null;boardEl.classList.remove('classic-sliding');}
  }
  function animateMove(tiles,merges){
    clearMotion();
    if(!boardEl.animate||(window.matchMedia&&window.matchMedia('(prefers-reduced-motion: reduce)').matches))return;
    const layer=document.createElement('div');
    layer.className='classic-motion';layer.setAttribute('aria-hidden','true');
    slideLayer=layer;
    boardEl.append(layer);
    for(const {from,to} of tiles){
      const source=cells[from],target=cells[to],tile=source.cloneNode(true);
      tile.style.left=source.offsetLeft+'px';tile.style.top=source.offsetTop+'px';
      tile.style.width=source.offsetWidth+'px';tile.style.height=source.offsetHeight+'px';
      layer.append(tile);
      if(from===to)continue;
      const x=target.offsetLeft-source.offsetLeft,y=target.offsetTop-source.offsetTop;
      animations.push(tile.animate([{transform:'translate(0,0)'},{transform:'translate('+x+'px,'+y+'px)'}],
        {duration:125,easing:'ease-out',fill:'forwards'}));
    }
    boardEl.classList.add('classic-sliding');
    animations[0].onfinish=()=>{
      if(slideLayer!==layer)return;
      clearMotion();
      animations=merges.map(to=>cells[to].animate([
        {transform:'scale(.88)'},{transform:'scale(1.18)',offset:.45},{transform:'scale(1)'}
      ],{duration:175,easing:'ease-out'}));
    };
  }
  function resetRound(){
    clearMotion();
    round.board.fill(0);round.score=0;round.continued=false;
    spawn2048(round.board);spawn2048(round.board);save();render();
  }
  function move(direction){
    if(won()||document.querySelector('dialog[open]'))return;
    const {next,points,tiles,merges,changed}=slide2048(round.board,direction);
    if(!changed)return;
    spawn2048(next);round.board=next;round.score+=points;round.best=Math.max(round.score,round.best);
    save();animateMove(tiles,merges);render();
    if(points)beep('merge');else if(!canMove2048(next))beep('end');
  }
  function continueRound(){
    round.continued=true;save();render();boardEl.focus({preventScroll:true});
  }
  function select(active){
    if(classicActive===active)return;
    clearMotion();
    if(active){
      saveGame();pendingDrop=null;
      if(!round){round=read();if(!round.board.some(Boolean))resetRound();}
      render();
    }
    classicActive=active;
    panel.hidden=!active;$('drop-layout').hidden=active;
    document.body.classList.toggle('classic-mode',active);
    if(active)$('mode-picker').textContent='经典 2048 ▾';else update();
    try{localStorage.setItem('classic-2048-selected',active?'1':'0');}catch(error){}
    wakeFrames();
  }
  $('choose-classic').onclick=()=>{select(true);$('mode-dialog').close();boardEl.focus({preventScroll:true});};
  $('classic-continue').onclick=continueRound;
  $('classic-restart').onclick=openRestart;
  $('classic-resume').onclick=()=>menu.close();
  $('classic-modes').onclick=()=>{menu.close();$('mode-dialog').showModal();};
  $('classic-menu-restart').onclick=()=>{menu.close();openRestart();};
  boardEl.addEventListener('pointerdown',e=>{
    if(!e.isPrimary||e.button!==0)return;
    gesture=[e.clientX,e.clientY];boardEl.setPointerCapture(e.pointerId);
  });
  boardEl.addEventListener('pointercancel',()=>{gesture=null;});
  boardEl.addEventListener('pointerup',e=>{
    if(!gesture||!e.isPrimary)return;
    const x=e.clientX-gesture[0],y=e.clientY-gesture[1];gesture=null;
    if(Math.max(Math.abs(x),Math.abs(y))>=20)move(Math.abs(x)>Math.abs(y)?(x<0?'left':'right'):(y<0?'up':'down'));
  });
  if(new URLSearchParams(location.search).has('tv')){
    $('classic-controls').textContent='方向键滑动 · 确认继续或重开 · 返回打开菜单';
    $('classic-menu-tip').textContent='方向键选择，确认执行。按返回键回到游乐场。';
  }
  classicUI={
    move,select,reset:resetRound,focus:()=>boardEl.focus({preventScroll:true}),menu:()=>menu.showModal(),
    confirm(){if(won())continueRound();else if(!canMove2048(round.board))$('classic-restart').click();}
  };
  try{if(localStorage.getItem('classic-2048-selected')==='1')select(true);}catch(error){}
}

if(typeof document!=='undefined')initClassic();
