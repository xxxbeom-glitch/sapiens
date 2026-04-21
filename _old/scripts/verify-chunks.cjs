const fs = require('fs');
function extractPayload(js, key) {
  const prefix = `setPluginData('${key}', `;
  const a = js.indexOf(prefix);
  if (a < 0) throw new Error('prefix ' + key);
  let i = a + prefix.length;
  if (js[i] !== '"') throw new Error('expected quote');
  i++;
  let out = '';
  while (i < js.length) {
    const c = js[i];
    if (c === '\\') {
      i++;
      out += js[i];
      i++;
      continue;
    }
    if (c === '"') break;
    out += c;
    i++;
  }
  return out;
}
let full = '';
for (let k = 1; k <= 4; k++) {
  const js = fs.readFileSync(`scripts/figma-chunk${k}.js`, 'utf8');
  full += extractPayload(js, 'j' + k);
}
const dec = Buffer.from(full, 'base64').toString('utf8');
console.log('b64len', full.length, 'declen', dec.length);
console.log('head', dec.slice(0, 60));
console.log('tail', dec.slice(-60));
