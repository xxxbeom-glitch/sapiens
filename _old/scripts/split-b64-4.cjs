const fs = require('fs');
const b = fs.readFileSync('scripts/figma-briefing-screenshot-text.js').toString('base64');
const n = Math.ceil(b.length / 4);
const parts = [0, 1, 2, 3].map((i) => b.slice(i * n, (i + 1) * n));
parts.forEach((p, i) => {
  const code = `(async()=>{ figma.root.setPluginData('j${i + 1}', ${JSON.stringify(p)}); return {ok:${i + 1},len:${p.length}}; })();`;
  fs.writeFileSync(`scripts/figma-chunk${i + 1}.js`, code);
});
const join = `(async()=>{ const b=[1,2,3,4].map(i=>figma.root.getPluginData('j'+i)).join(''); const t=decodeURIComponent(escape(atob(b))); await (0,eval)(t); for(let i=1;i<=4;i++) figma.root.setPluginData('j'+i,''); return {ok:5}; })();`;
fs.writeFileSync('scripts/figma-chunk-join.js', join);
console.log(parts.map((p) => p.length), join.length);
