// The native TV shell forwards remote/gamepad input here; no network or JS bridge.
(function(){
  if(!new URLSearchParams(location.search).has('tv'))return;
  window.nativeTV=true;window.tvAxis=0;document.body.classList.add('tv');
  $('gamepad-status').textContent='← → 移动 · 确认投放 · ↑ 菜单 · B 炸弹 · X 摇一摇 · 返回暂停';
  window.tvInput=function(action){
    unlockAudio();
    const dialog=document.querySelector('dialog[open]');
    if(dialog){
      if(action==='back'||action==='menu'){dialog.close();return true;}
      const buttons=[...dialog.querySelectorAll('button')].filter(b=>!b.disabled);
      if(['left','right','up','down'].includes(action)){
        const index=buttons.indexOf(document.activeElement),step=['left','up'].includes(action)?-1:1;
        buttons[(index+step+buttons.length)%buttons.length]?.focus();
      }else if(action==='ok'){
        const button=buttons.includes(document.activeElement)?document.activeElement:buttons[0];button?.click();
      }
      return true;
    }
    if(action==='back'){if(paused||ended)return false;togglePause();return true;}
    if(action==='menu'||action==='up'||action==='down'){$('tv-menu').showModal();return true;}
    if(action==='pause'){togglePause();return true;}
    if(action==='ok'){if(ended)reset();else if(paused)togglePause();else if(bombMode)removePlanet(bombTarget);else requestDrop();return true;}
    if(paused||ended)return true;
    if(action==='left'||action==='right'){
      if(bombMode)selectBombTarget(action==='left'?-1:1);else aim=Math.max(30,Math.min(WIDTH-30,aim+(action==='left'?-18:18)));
    }else if(action==='bomb')toggleBomb();else if(action==='shake')useShake();
    return true;
  };
  $('tv-controls').onclick=()=>$('tv-menu').showModal();
  const actions={continue:()=>{if(paused&&!ended)togglePause();},bomb:toggleBomb,shake:useShake,mode:()=>$('mode-dialog').showModal(),help:()=>$('help-dialog').showModal(),restart:()=>$('restart-dialog').showModal()};
  document.querySelectorAll('[data-tv]').forEach(b=>b.onclick=()=>{$('tv-menu').close();actions[b.dataset.tv]();});
})();
