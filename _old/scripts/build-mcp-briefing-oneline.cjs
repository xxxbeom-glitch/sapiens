const fs = require('fs');
const c = fs.readFileSync(__dirname + '/figma-patch-briefing-rows.js', 'utf8');
const one = c.replace(/\s+/g, ' ').trim();
const o = {
  fileKey: 'EaE3QXT7UV66RFZRXppOvi',
  description: 'briefing copy one-line',
  code: one,
};
fs.writeFileSync(__dirname + '/mcp-briefing-oneline.json', JSON.stringify(o), 'utf8');
console.log('jsonLen', JSON.stringify(o).length);
