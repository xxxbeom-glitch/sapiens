const fs = require('fs');
const path = require('path');
const src = path.join(__dirname, 'mcp-b64-eval-payload.json');
const dst = path.join(__dirname, '..', 'mcp-invoke-b64.json');
fs.copyFileSync(src, dst);
console.log('copied to', dst, 'bytes', fs.statSync(dst).size);
