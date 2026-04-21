const fs = require('fs');
const c = fs.readFileSync('scripts/figma-briefing-screenshot-text.js');
const b = c.toString('base64');
const wrap =
  '(async()=>{ const b=' +
  JSON.stringify(b) +
  '; const t=decodeURIComponent(escape(atob(b))); await (0,eval)(t); })();';
fs.writeFileSync('scripts/figma-briefing-b64-wrap.js', wrap);
console.log('wrapLen', wrap.length);
