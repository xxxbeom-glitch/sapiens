const fs = require('fs');
const path = require('path');
const dir = __dirname;
const fileKey = 'EaE3QXT7UV66RFZRXppOvi';

for (const [name, desc] of [
  ['figma-mcp-call1.js', 'Figma clientStorage _a base64 part 1'],
  ['figma-mcp-call2.js', 'Figma clientStorage _b base64 part 2'],
  ['figma-mcp-call3.js', 'Figma decode+eval patch script'],
]) {
  const code = fs.readFileSync(path.join(dir, name), 'utf8');
  const payload = { fileKey, code, description: desc };
  fs.writeFileSync(path.join(dir, name.replace('.js', '-payload.json')), JSON.stringify(payload), 'utf8');
}
