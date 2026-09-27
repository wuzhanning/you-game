/* A small fixed-step circle simulation. */
const PLANETS = [
  {name:'水星',r:19,color:'#c5becd',shade:'#a59aae'},
  {name:'火星',r:25,color:'#edac98',shade:'#d98274'},
  {name:'金星',r:32,color:'#eed293',shade:'#dcb970'},
  {name:'海王星',r:39,color:'#a8b8df',shade:'#829acd'},
  {name:'地球',r:47,color:'#96c6cf',shade:'#72aeb9'},
  {name:'天王星',r:55,color:'#acd7d0',shade:'#83b7ad'},
  {name:'土星',r:64,color:'#d2b8df',shade:'#b298c7'},
  {name:'木星',r:73,color:'#e4b392',shade:'#c99375'},
  {name:'太阳',r:84,color:'#ffd66b',shade:'#f5a938'},
  {name:'红巨星',r:94,color:'#f58b78',shade:'#c64f64'},
  {name:'黑洞',r:104,color:'#42345e',shade:'#21182f'}
];
const WIDTH=480, HEIGHT=580, LIMIT=94;
function makePlanet(level,x,y){return {level,x,y,vx:0,vy:0,age:0};}
function advance(balls,onMerge){
  for(const b of balls){b.age++;b.vy+=.14;b.vx*=.993;b.x+=b.vx;b.y+=b.vy;}
  // ponytail: O(n²) pairs suit this small board; use a spatial grid if the board grows.
  for(let pass=0;pass<6;pass++){
    for(let i=0;i<balls.length;i++){
      const a=balls[i];
      for(let j=i+1;j<balls.length;j++){
        const b=balls[j],ra=PLANETS[a.level].r,rb=PLANETS[b.level].r;
        let dx=b.x-a.x,dy=b.y-a.y;
        if(dx*dx+dy*dy>=(ra+rb)*(ra+rb))continue;
        let d=Math.sqrt(dx*dx+dy*dy);
        if(a.level===b.level&&a.level<PLANETS.length-1){
          const level=a.level+1,merged=makePlanet(level,(a.x+b.x)/2,(a.y+b.y)/2);
          merged.vx=(a.vx+b.vx)/2;merged.vy=Math.min(0,(a.vy+b.vy)/2);merged.age=24;
          balls.splice(j,1);balls.splice(i,1,merged);onMerge(merged);return;
        }
        if(d<.001){dx=.001;dy=0;d=.001;}
        const nx=dx/d,ny=dy/d,overlap=ra+rb-d,ma=ra*ra,mb=rb*rb,wa=mb/(ma+mb),wb=ma/(ma+mb);
        a.x-=nx*overlap*wa;a.y-=ny*overlap*wa;b.x+=nx*overlap*wb;b.y+=ny*overlap*wb;
        const velocity=(b.vx-a.vx)*nx+(b.vy-a.vy)*ny;
        if(velocity<0){const impulse=-velocity*1.12;a.vx-=impulse*nx*wa;a.vy-=impulse*ny*wa;b.vx+=impulse*nx*wb;b.vy+=impulse*ny*wb;}
      }
    }
    for(const b of balls){const r=PLANETS[b.level].r;
      if(b.x<r+7){b.x=r+7;b.vx=Math.abs(b.vx)*.3;}
      if(b.x>WIDTH-r-7){b.x=WIDTH-r-7;b.vx=-Math.abs(b.vx)*.3;}
      if(b.y>HEIGHT-r-9){b.y=HEIGHT-r-9;b.vy=-Math.abs(b.vy)*.12;b.vx*=.97;}
    }
  }
}
if(typeof module!=='undefined')module.exports={PLANETS,makePlanet,advance,WIDTH,HEIGHT,LIMIT};
