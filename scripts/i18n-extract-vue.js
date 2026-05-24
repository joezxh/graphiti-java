const fs = require('fs');

const filePath = process.argv[2] || 'graphiti-web/src/views/graph/ide.vue';
const namespace = process.argv[3] || 'graph.ide';

let content = fs.readFileSync(filePath, 'utf8');

const translations = {};
let keyIndex = 0;

function getKey(text) {
  // Create a key from text
  const base = text.replace(/[^\u4e00-\u9fa5a-zA-Z0-9]/g, '').substring(0, 20).toLowerCase();
  if (!base) return `text${keyIndex++}`;
  return base;
}

function addTranslation(key, text) {
  const fullKey = `${namespace}.${key}`;
  translations[fullKey] = text;
  return fullKey;
}

// Pattern 1: title="中文"
content = content.replace(/title="([^"]*[\u4e00-\u9fa5]+[^"]*)"/g, (match, text) => {
  const key = getKey(text);
  return `:title="t('${addTranslation(key, text)}')"`;
});

// Pattern 2: placeholder="中文"
content = content.replace(/placeholder="([^"]*[\u4e00-\u9fa5]+[^"]*)"/g, (match, text) => {
  const key = getKey(text);
  return `:placeholder="t('${addTranslation(key, text)}')"`;
});

// Pattern 3: >中文< (text nodes in tags)
// Be careful to only match direct text content, not attributes
content = content.replace(/>([^<]*[\u4e00-\u9fa5]+[^<]*)</g, (match, text) => {
  const trimmed = text.trim();
  if (!trimmed) return match;
  // Skip if it contains interpolation or is just whitespace
  if (trimmed.includes('{{') || trimmed.includes('}}')) return match;
  const key = getKey(trimmed);
  const replacement = `{{ t('${addTranslation(key, trimmed)}') }}`;
  return match.replace(trimmed, replacement);
});

// Pattern 4: label="中文"
content = content.replace(/label="([^"]*[\u4e00-\u9fa5]+[^"]*)"/g, (match, text) => {
  const key = getKey(text);
  return `:label="t('${addTranslation(key, text)}')"`;
});

// Pattern 5: ok-text="中文" cancel-text="中文"
content = content.replace(/ok-text="([^"]*[\u4e00-\u9fa5]+[^"]*)"/g, (match, text) => {
  const key = getKey(text);
  return `:ok-text="t('${addTranslation(key, text)}')"`;
});
content = content.replace(/cancel-text="([^"]*[\u4e00-\u9fa5]+[^"]*)"/g, (match, text) => {
  const key = getKey(text);
  return `:cancel-text="t('${addTranslation(key, text)}')"`;
});

// Pattern 6: tip="中文"
content = content.replace(/tip="([^"]*[\u4e00-\u9fa5]+[^"]*)"/g, (match, text) => {
  const key = getKey(text);
  return `:tip="t('${addTranslation(key, text)}')"`;
});

// Pattern 7: message.success/error/info("中文")
content = content.replace(/message\.(success|error|info|warn)\([`'"]([^`'"]*[\u4e00-\u9fa5]+[^`'"]*)[`'"]/g, (match, type, text) => {
  const key = getKey(text);
  return `message.${type}(t('${addTranslation(key, text)}')`;
});

// Pattern 8: message.success(`中文 ${var}`)
content = content.replace(/message\.(success|error|info|warn)\(`([^`]*[\u4e00-\u9fa5]+[^`]*)`/g, (match, type, text) => {
  // For template literals with Chinese, we need to be careful
  // Just replace the whole message call
  const key = getKey(text);
  return `message.${type}(t('${addTranslation(key, text)}')`;
});

// Output translations
console.log('\n// Add these to zh-CN.ts under the namespace:\n');
console.log(`  ${namespace.split('.')[0]}: {`);
if (namespace.includes('.')) {
  console.log(`    ${namespace.split('.').slice(1).join('.')}: {`);
}
Object.entries(translations).forEach(([k, v]) => {
  const shortKey = k.split('.').pop();
  console.log(`    ${shortKey}: '${v}',`);
});
if (namespace.includes('.')) {
  console.log(`    },`);
}
console.log(`  },`);

// Write modified file
const outPath = filePath.replace('.vue', '.i18n.vue');
fs.writeFileSync(outPath, content);
console.log(`\nModified file written to: ${outPath}`);
console.log(`Total translations: ${Object.keys(translations).length}`);
