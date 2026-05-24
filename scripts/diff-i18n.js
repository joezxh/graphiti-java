const fs = require('fs');

function extractAllKeys(content) {
  // Remove export default, comments, trailing commas before } or ]
  let cleaned = content
    .replace(/export\s+default\s+/, '')
    .replace(/\/\/.*$/gm, '')
    .replace(/\/\*[\s\S]*?\*\//g, '');
  // Remove trailing commas before } or ]
  cleaned = cleaned.replace(/,(\s*[}\]])/g, '$1');
  try {
    const obj = eval('(' + cleaned + ')');
    function walk(o, path, result) {
      for (const k of Object.keys(o)) {
        const p = path ? path + '.' + k : k;
        result.push(p);
        if (o[k] && typeof o[k] === 'object' && !Array.isArray(o[k])) {
          walk(o[k], p, result);
        }
      }
    }
    const r = [];
    walk(obj, '', r);
    return r;
  } catch (e) {
    console.error('Parse error:', e.message);
    return [];
  }
}

const files = ['zh-CN', 'en-US', 'zh-TW', 'ja-JP'];
const all = {};
for (const f of files) {
  const c = fs.readFileSync('graphiti-web/src/i18n/locales/' + f + '.ts', 'utf8');
  all[f] = new Set(extractAllKeys(c));
  console.log(f + ' keys count:', all[f].size);
}

const cn = all['zh-CN'];
const en = all['en-US'];

for (const f of ['zh-TW', 'ja-JP']) {
  console.log('\n=== Missing in ' + f + ' (vs zh-CN) ===');
  const missing = [...cn].filter(k => !all[f].has(k)).sort();
  missing.forEach(k => console.log(k));
}

console.log('\n=== Missing in zh-CN (vs en-US) ===');
const missingCn = [...en].filter(k => !cn.has(k)).sort();
missingCn.forEach(k => console.log(k));

console.log('\n=== Missing in en-US (vs zh-CN) ===');
const missingEn = [...cn].filter(k => !en.has(k)).sort();
missingEn.forEach(k => console.log(k));
