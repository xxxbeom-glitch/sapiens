const fs = require('fs');
const path = require('path');
const dir = __dirname;
const o = JSON.parse(fs.readFileSync(path.join(dir, 'mcp-one-shot.json'), 'utf8'));
const out = path.join(dir, 'mcp-one-shot-utf8.json');
fs.writeFileSync(out, JSON.stringify(o), 'utf8');
const s = fs.readFileSync(out, 'utf8');
console.log('has briefing', s.includes('브리핑'), 'len', s.length);
