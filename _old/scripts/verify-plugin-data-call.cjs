const fs = require('fs');
const path = require('path');
const dir = __dirname;

const raw = fs.readFileSync(path.join(dir, 'figma-mcp-call1.js'), 'utf8');
const marker = 'setPluginData("_a",';
const p = raw.indexOf(marker) + marker.length;
const sub = raw.slice(p);
const end = sub.indexOf(');return');
const json = sub.slice(0, end);
const s = JSON.parse(json);
const a = fs.readFileSync(path.join(dir, 'figma-b64a.txt'), 'utf8');
console.log('parsed len', s.length, 'file len', a.length, 'match', s === a);
