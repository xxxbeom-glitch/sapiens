const fs = require('fs');
const b = fs.readFileSync('scripts/figma-briefing-screenshot-text.js').toString('base64');
const n = 9500;
const c1 = b.slice(0, n);
const c2 = b.slice(n);
fs.writeFileSync('scripts/b64-chunk1.txt', c1);
fs.writeFileSync('scripts/b64-chunk2.txt', c2);
console.log(c1.length, c2.length, b.length);
