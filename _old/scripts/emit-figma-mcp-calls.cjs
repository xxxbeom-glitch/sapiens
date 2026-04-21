const fs = require('fs');
const path = require('path');
const dir = __dirname;
const a = fs.readFileSync(path.join(dir, 'figma-b64a.txt'), 'utf8').trim();
const b = fs.readFileSync(path.join(dir, 'figma-b64b.txt'), 'utf8').trim();

const c1 =
  '(async()=>{figma.root.setPluginData("_a",' +
  JSON.stringify(a) +
  ');return {ok:1,len:' +
  a.length +
  '};})();';

const c2 =
  '(async()=>{figma.root.setPluginData("_b",' +
  JSON.stringify(b) +
  ');return {ok:2,len:' +
  b.length +
  '};})();';

const c3 = `(async()=>{
const a=(figma.root.getPluginData("_a")||"").trim();
const b=(figma.root.getPluginData("_b")||"").trim();
const bin=atob(a+b);
const u8=new Uint8Array(bin.length);
for(let i=0;i<bin.length;i++)u8[i]=bin.charCodeAt(i);
const t=new TextDecoder("utf-8").decode(u8);
const r=await (0,eval)(t);
figma.root.setPluginData("_a","");
figma.root.setPluginData("_b","");
return r;
})();`;

fs.writeFileSync(path.join(dir, 'figma-mcp-call1.js'), c1);
fs.writeFileSync(path.join(dir, 'figma-mcp-call2.js'), c2);
fs.writeFileSync(path.join(dir, 'figma-mcp-call3.js'), c3);
console.log('len', c1.length, c2.length, c3.length);
