const fs = require('fs');
const path = require('path');
const dir = __dirname;

const c = fs.readFileSync(path.join(dir, 'figma-mcp-call1.js'), 'utf8');
const start = 'setAsync("_a","';
const i = c.indexOf(start) + start.length;
const j = c.indexOf('");return');
const inner = c.slice(i, j);

const c2 = fs.readFileSync(path.join(dir, 'figma-mcp-call2.js'), 'utf8');
const s2 = 'setAsync("_b","';
const i2 = c2.indexOf(s2) + s2.length;
const j2 = c2.indexOf('");return');
const inner2 = c2.slice(i2, j2);

const orig =
  fs.readFileSync(path.join(dir, 'figma-b64a.txt'), 'utf8').trim() +
  fs.readFileSync(path.join(dir, 'figma-b64b.txt'), 'utf8').trim();

console.log('inner', inner.length, 'inner2', inner2.length, 'orig', orig.length, 'match', inner + inner2 === orig);
