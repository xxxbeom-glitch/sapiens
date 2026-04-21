const fs = require('fs');
const path = require('path');
const dir = __dirname;

function extractB64(js, marker) {
  const p = js.indexOf(marker) + marker.length;
  const sub = js.slice(p);
  const end = sub.indexOf(');return');
  return JSON.parse(sub.slice(0, end));
}

const c1 = fs.readFileSync(path.join(dir, 'figma-mcp-call1.js'), 'utf8');
const c2 = fs.readFileSync(path.join(dir, 'figma-mcp-call2.js'), 'utf8');
const a = extractB64(c1, 'setPluginData("_a",');
const b = extractB64(c2, 'setPluginData("_b",');
const orig = fs.readFileSync(path.join(dir, 'figma-patch-briefing-rows.js'), 'utf8');
const dec = Buffer.from(a + b, 'base64').toString('utf8');
console.log('a', a.length, 'b', b.length, 'match', dec === orig);
