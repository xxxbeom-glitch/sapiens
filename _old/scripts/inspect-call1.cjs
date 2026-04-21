const fs = require('fs');
const path = require('path');
const d = fs.readFileSync(path.join(__dirname, 'figma-mcp-call1.js'), 'utf8');
const marker = '");return';
const i = d.indexOf(marker);
console.log('idx', i, 'total', d.length);
console.log('around', JSON.stringify(d.slice(i - 30, i + marker.length + 5)));
