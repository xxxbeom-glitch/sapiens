const fs = require('fs');
const path = require('path');
const dir = __dirname;
const a = fs.readFileSync(path.join(dir, 'figma-b64a.txt'), 'utf8');
const needle = '");return';
const i = a.indexOf(needle);
console.log('needle in b64a at', i);
const call1 = fs.readFileSync(path.join(dir, 'figma-mcp-call1.js'), 'utf8');
console.log('needle in call1 at', call1.indexOf(needle));
