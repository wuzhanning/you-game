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
function baseMode(mode){return mode.replace('-glyph','');}
function isGlyphMode(mode){return mode.endsWith('-glyph');}
const glyphShapes=new Map();
function bodySpec(level,mode='planet'){
  mode=baseMode(mode);
  if(mode==='planet')return PLANETS[level];
  return {...PLANETS[level%PLANETS.length],name:mode==='letter'?String.fromCharCode(65+level):String(level),r:Math.min(104,19+level*7)};
}
function bodyRadius(level,mode='planet'){return mode!=='planet'?Math.min(104,19+level*7):PLANETS[level].r;}
// Upright glyphs use a low-resolution compound-circle outline, including holes.
// ponytail: 32px masks keep CPU bounded; use polygon rigid bodies if rotation is added.
function setGlyphShape(level,mode,pixels){
  const r=bodyRadius(level,mode),step=r/16,points=[],cells=new Map();
  for(let y=0;y<32;y++)for(let x=0;x<32;x++)if(pixels[(y*32+x)*4+3]>100){
    const point={x:(x-15.5)*step,y:(y-15.5)*step};points.push(point);cells.set(y*32+x,point);
  }
  const radius=step*.72;
  const bounds=points.length?{left:Math.min(...points.map(p=>p.x))-radius,right:Math.max(...points.map(p=>p.x))+radius,top:Math.min(...points.map(p=>p.y))-radius,bottom:Math.max(...points.map(p=>p.y))+radius}:{left:-r,right:r,top:-r,bottom:r};
  glyphShapes.set(mode+':'+level,{points,cells,step,radius,...bounds});
}
function bodyBounds(level,mode){const r=bodyRadius(level,mode);return glyphShapes.get(mode+':'+level)||{left:-r,right:r,top:-r,bottom:r};}
function hitBody(body,x,y,mode){
  const shape=glyphShapes.get(mode+':'+body.level);
  return shape?shape.points.some(p=>Math.hypot(body.x+p.x-x,body.y+p.y-y)<=shape.radius):Math.hypot(body.x-x,body.y-y)<=bodyRadius(body.level,mode);
}
function contact(a,b,mode){
  const sa=glyphShapes.get(mode+':'+a.level),sb=glyphShapes.get(mode+':'+b.level);
  if(!sa||!sb){const dx=b.x-a.x,dy=b.y-a.y,sum=bodyRadius(a.level,mode)+bodyRadius(b.level,mode);if(dx*dx+dy*dy>=sum*sum)return null;const d=Math.hypot(dx,dy);return {nx:d?dx/d:1,ny:d?dy/d:0,overlap:sum-d};}
  if(a.x+sa.right<=b.x+sb.left||b.x+sb.right<=a.x+sa.left||a.y+sa.bottom<=b.y+sb.top||b.y+sb.bottom<=a.y+sa.top)return null;
  let result=null;const sum=sa.radius+sb.radius,reach=Math.ceil(sum/sb.step);
  for(const pa of sa.points){
    const col=Math.round((a.x+pa.x-b.x)/sb.step+15.5),row=Math.round((a.y+pa.y-b.y)/sb.step+15.5);
    for(let y=Math.max(0,row-reach);y<=Math.min(31,row+reach);y++)for(let x=Math.max(0,col-reach);x<=Math.min(31,col+reach);x++){
      const pb=sb.cells.get(y*32+x);if(!pb)continue;
      const dx=b.x+pb.x-a.x-pa.x,dy=b.y+pb.y-a.y-pa.y,d2=dx*dx+dy*dy;
      if(d2>=sum*sum)continue;const d=Math.sqrt(d2),overlap=sum-d;
      if(!result||overlap>result.overlap)result={nx:d?dx/d:1,ny:d?dy/d:0,overlap};
    }
  }
  return result;
}
function shakeBodies(balls,ticks){
  const force=Math.sin((48-ticks)*Math.PI/8)*.38;
  for(const b of balls){b.vx=Math.max(-4,Math.min(4,b.vx+force));if(ticks===48){b.vy=-2.5-(b.x%17)/17;b.age=0;}}
}
function advance(balls,onMerge,mode='planet'){
  for(const b of balls){b.age++;b.vy+=.14;b.vx*=.993;b.x+=b.vx;b.y+=b.vy;}
  // ponytail: O(n²) pairs suit this small board; use a spatial grid if the board grows.
  for(let pass=0;pass<6;pass++){
    for(let i=0;i<balls.length;i++){
      const a=balls[i];
      for(let j=i+1;j<balls.length;j++){
        const b=balls[j],ra=bodyRadius(a.level,mode),rb=bodyRadius(b.level,mode);
        const collision=contact(a,b,mode);if(!collision)continue;
        if(a.level===b.level&&a.level<(baseMode(mode)==='number'?Number.MAX_SAFE_INTEGER:baseMode(mode)==='letter'?25:PLANETS.length-1)){
          const level=a.level+1,merged=makePlanet(level,(a.x+b.x)/2,(a.y+b.y)/2);
          merged.vx=(a.vx+b.vx)/2;merged.vy=Math.min(0,(a.vy+b.vy)/2);merged.age=24;
          balls.splice(j,1);balls.splice(i,1,merged);onMerge(merged);return;
        }
        const {nx,ny,overlap}=collision,ma=ra*ra,mb=rb*rb,wa=mb/(ma+mb),wb=ma/(ma+mb);
        a.x-=nx*overlap*wa;a.y-=ny*overlap*wa;b.x+=nx*overlap*wb;b.y+=ny*overlap*wb;
        const velocity=(b.vx-a.vx)*nx+(b.vy-a.vy)*ny;
        if(velocity<0){const impulse=-velocity*1.12;a.vx-=impulse*nx*wa;a.vy-=impulse*ny*wa;b.vx+=impulse*nx*wb;b.vy+=impulse*ny*wb;}
      }
    }
    for(const b of balls){const bounds=bodyBounds(b.level,mode);
      if(b.x+bounds.left<7){b.x=7-bounds.left;b.vx=Math.abs(b.vx)*.3;}
      if(b.x+bounds.right>WIDTH-7){b.x=WIDTH-7-bounds.right;b.vx=-Math.abs(b.vx)*.3;}
      if(b.y+bounds.bottom>HEIGHT-9){b.y=HEIGHT-9-bounds.bottom;b.vy=-Math.abs(b.vy)*.12;b.vx*=.97;}
    }
  }
}
if(typeof module!=='undefined')module.exports={baseMode,isGlyphMode,glyphShapes,setGlyphShape,bodyBounds,hitBody,contact,bodySpec,bodyRadius,shakeBodies,PLANETS,makePlanet,advance,WIDTH,HEIGHT,LIMIT};
