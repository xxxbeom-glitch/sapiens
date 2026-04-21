const fs = require('fs');
const path = require('path');

const dir = __dirname;
const b64 = fs.readFileSync(path.join(dir, 'figma-patch-b64.txt'), 'utf8').trim();
const chunkSize = 2000;
const chunks = [];
for (let i = 0; i < b64.length; i += chunkSize) {
  chunks.push(b64.slice(i, i + chunkSize));
}

const fileKey = 'EaE3QXT7UV66RFZRXppOvi';
const keys = chunks.map((_, i) => '_p' + i);

const calls = [];
for (let i = 0; i < chunks.length; i++) {
  const code =
    '(async()=>{figma.root.setPluginData(' +
    JSON.stringify(keys[i]) +
    ',' +
    JSON.stringify(chunks[i]) +
    ');return {part:' +
    i +
    ',len:' +
    chunks[i].length +
    '};})();';
  calls.push({
    fileKey,
    description: 'patch b64 part ' + i,
    code,
  });
}

const joinKeys = JSON.stringify(keys);
const joinCode =
  '(async()=>{\n' +
  'const keys=' +
  joinKeys +
  ';\n' +
  "let b64='';\n" +
  'for (const k of keys) b64 += figma.root.getPluginData(k) || "";\n' +
  'for (const k of keys) figma.root.setPluginData(k, "");\n' +
  'const bin=atob(b64);\n' +
  'const u8=new Uint8Array(bin.length);\n' +
  'for(let i=0;i<bin.length;i++)u8[i]=bin.charCodeAt(i);\n' +
  'const t=new TextDecoder("utf-8").decode(u8);\n' +
  'return await (0,eval)(t);\n' +
  '})();';
calls.push({ fileKey, description: 'patch b64 join+eval', code: joinCode });

fs.writeFileSync(path.join(dir, 'figma-chunk-mcp-calls.json'), JSON.stringify(calls, null, 2), 'utf8');
calls.forEach((c, i) => {
  const n = JSON.stringify(c).length;
  console.log('call', i, 'jsonLen', n);
});
