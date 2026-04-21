const fs = require('fs');
const path = require('path');
const dir = __dirname;
const b64 = fs.readFileSync(path.join(dir, 'figma-patch-b64.txt'), 'utf8').trim();
const code =
  '(async()=>{\n' +
  'const b64=' +
  JSON.stringify(b64) +
  ';\n' +
  'const bin=atob(b64);\n' +
  'const u8=new Uint8Array(bin.length);\n' +
  'for(let i=0;i<bin.length;i++)u8[i]=bin.charCodeAt(i);\n' +
  'const t=new TextDecoder("utf-8").decode(u8);\n' +
  'return await (0,eval)(t);\n' +
  '})();';
const payload = {
  fileKey: 'EaE3QXT7UV66RFZRXppOvi',
  description: '브리핑 카드 행 수·텍스트 base64 eval',
  code,
};
fs.writeFileSync(path.join(dir, 'mcp-b64-eval-payload.json'), JSON.stringify(payload), 'utf8');
console.log('total json len', JSON.stringify(payload).length, 'code len', code.length);
