const fs = require('fs');
const path = require('path');
const calls = JSON.parse(fs.readFileSync(path.join(__dirname, 'figma-chunk-mcp-calls.json'), 'utf8'));
const orig = fs.readFileSync(path.join(__dirname, 'figma-patch-b64.txt'), 'utf8').trim();
let acc = '';
for (let i = 0; i < 4; i++) {
  const code = calls[i].code;
  const marker = 'setPluginData("_p' + i + '",';
  const p = code.indexOf(marker) + marker.length;
  const sub = code.slice(p);
  const chunk = JSON.parse(sub.split(');return')[0]);
  acc += chunk;
}
console.log('orig', orig.length, 'acc', acc.length, 'match', acc === orig);
