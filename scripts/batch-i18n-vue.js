const fs = require('fs');
const path = require('path');

const SRC_DIR = 'graphiti-web/src';
const LOCALES_DIR = path.join(SRC_DIR, 'i18n/locales');
const LOCALE_FILES = ['zh-CN.ts', 'en-US.ts', 'zh-TW.ts', 'ja-JP.ts'];

// Namespace mappings for different file paths
function getNamespace(filePath) {
  const rel = filePath.replace(/^graphiti-web\/src\//, '');
  if (rel.startsWith('views/graph/')) return 'graph';
  if (rel.startsWith('views/data/')) return 'data';
  if (rel.startsWith('views/legal-kg/')) return 'legalKg';
  if (rel.startsWith('views/system/')) return 'system';
  if (rel.startsWith('views/dashboard/')) return 'dashboard';
  if (rel.startsWith('views/search/')) return 'search';
  if (rel.startsWith('views/monitor/')) return 'monitor';
  if (rel.startsWith('views/profile/')) return 'profile';
  if (rel.startsWith('views/prompt/')) return 'prompt';
  if (rel.startsWith('components/Graph/')) return 'graph';
  if (rel.startsWith('components/Ontology/')) return 'ontology';
  return 'common';
}

function getSubNamespace(filePath) {
  const base = path.basename(filePath, '.vue');
  // Convert camelCase or kebab-case to camelCase
  return base.replace(/-([a-z])/g, (_, c) => c.toUpperCase());
}

function sanitizeKey(text) {
  // Remove non-alphanumeric chars, keep Chinese for dedup but map to pinyin-like short keys
  return text
    .replace(/[^\u4e00-\u9fa5a-zA-Z0-9]/g, '')
    .substring(0, 20)
    .toLowerCase();
}

// Simple mapping for common labels to avoid super long keys
const COMMON_KEY_MAP = {
  '名称': 'name',
  '名称（localName）': 'localName',
  '类型': 'type',
  '描述': 'description',
  '创建时间': 'createdAt',
  '更新时间': 'updatedAt',
  '操作': 'action',
  '保存': 'save',
  '取消': 'cancel',
  '确认': 'confirm',
  '删除': 'delete',
  '编辑': 'edit',
  '新建': 'create',
  '添加': 'add',
  '搜索': 'search',
  '加载中...': 'loading',
  '暂无数据': 'noData',
  '成功': 'success',
  '失败': 'failed',
  '请输入': 'pleaseEnter',
  '请选择': 'pleaseSelect',
};

function getKey(text) {
  if (COMMON_KEY_MAP[text]) return COMMON_KEY_MAP[text];
  const s = sanitizeKey(text);
  if (!s) return 'text';
  return s;
}

function extractChineseFromVue(content) {
  const results = [];
  
  // Remove HTML comments
  let cleaned = content.replace(/<!--[\s\S]*?-->/g, '');
  // Remove JS line comments (but preserve strings)
  // We process line by line for safety
  const lines = cleaned.split('\n');
  
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    // Skip pure comment lines
    if (line.trim().startsWith('//')) continue;
    
    // Find Chinese in the line
    const regex = /[\u4e00-\u9fa5][\u4e00-\u9fa5\s\.\.\.\:\（\）\(\)\,\.\-]*/g;
    let m;
    while ((m = regex.exec(line)) !== null) {
      const text = m[0].trim();
      if (text.length >= 2) {
        results.push({ text, line: i + 1, column: m.index });
      }
    }
  }
  
  return results;
}

// Process a single Vue file
function processVueFile(filePath) {
  const content = fs.readFileSync(filePath, 'utf8');
  const matches = extractChineseFromVue(content);
  if (matches.length === 0) return null;

  const ns = getNamespace(filePath);
  const subNs = getSubNamespace(filePath);
  const translations = {};
  let modified = content;
  
  // Process from end to start to preserve indices
  const sorted = [...matches].sort((a, b) => {
    if (a.line !== b.line) return b.line - a.line;
    return b.column - a.column;
  });
  
  for (const match of sorted) {
    const { text, line, column } = match;
    const key = getKey(text);
    const fullKey = `${ns}.${subNs}.${key}`;
    translations[fullKey] = text;
    
    // Find the exact occurrence in the line and replace
    const lines = modified.split('\n');
    const lineIndex = line - 1;
    const originalLine = lines[lineIndex];
    
    // Try to determine context and create appropriate replacement
    let replacement;
    const linePrefix = originalLine.substring(0, column);
    const lineSuffix = originalLine.substring(column + text.length);
    
    if (linePrefix.includes('placeholder=') && linePrefix.trim().endsWith('placeholder="')) {
      // placeholder="中文" -> :placeholder="t('key')"
      replacement = linePrefix.replace(/placeholder="$/, ':placeholder="t(\'') + fullKey + lineSuffix.replace(/^"/, '\')"');
    } else if (linePrefix.includes('title=') && linePrefix.trim().endsWith('title="')) {
      replacement = linePrefix.replace(/title="$/, ':title="t(\'') + fullKey + lineSuffix.replace(/^"/, '\')"');
    } else if (linePrefix.includes('label=') && linePrefix.trim().endsWith('label="')) {
      replacement = linePrefix.replace(/label="$/, ':label="t(\'') + fullKey + lineSuffix.replace(/^"/, '\')"');
    } else if (linePrefix.includes('ok-text=') && linePrefix.trim().endsWith('ok-text="')) {
      replacement = linePrefix.replace(/ok-text="$/, ':ok-text="t(\'') + fullKey + lineSuffix.replace(/^"/, '\')"');
    } else if (linePrefix.includes('cancel-text=') && linePrefix.trim().endsWith('cancel-text="')) {
      replacement = linePrefix.replace(/cancel-text="$/, ':cancel-text="t(\'') + fullKey + lineSuffix.replace(/^"/, '\')"');
    } else if (linePrefix.includes('tip=') && linePrefix.trim().endsWith('tip="')) {
      replacement = linePrefix.replace(/tip="$/, ':tip="t(\'') + fullKey + lineSuffix.replace(/^"/, '\')"');
    } else if (/message\.(success|error|info|warn)\s*\(\s*[`'"]\s*$/.test(linePrefix)) {
      // message.xxx("中文") -> message.xxx(t('key'))
      replacement = linePrefix + `t('${fullKey}')` + lineSuffix.replace(/^[`'"]/, '');
    } else if (linePrefix.trim().endsWith('>') && lineSuffix.trim().startsWith('<')) {
      // >中文< -> >{{ t('key') }}<
      replacement = linePrefix + `{{ t('${fullKey}') }}` + lineSuffix;
    } else if (linePrefix.includes(':title="') || linePrefix.includes(':placeholder="')) {
      // Already dynamic binding, just replace the inner text
      replacement = linePrefix + `t('${fullKey}')` + lineSuffix;
    } else {
      // Default: wrap with {{ t('key') }}
      replacement = linePrefix + `{{ t('${fullKey}') }}` + lineSuffix;
    }
    
    lines[lineIndex] = replacement;
    modified = lines.join('\n');
  }
  
  return { translations, modified, count: matches.length };
}

// Main
const vueFiles = [];
function collectVueFiles(dir) {
  const items = fs.readdirSync(dir);
  for (const item of items) {
    const fp = path.join(dir, item);
    const stat = fs.statSync(fp);
    if (stat.isDirectory() && !item.includes('node_modules')) {
      collectVueFiles(fp);
    } else if (stat.isFile() && item.endsWith('.vue')) {
      vueFiles.push(fp);
    }
  }
}
collectVueFiles(SRC_DIR);

const allTranslations = {};
let totalReplacements = 0;

for (const fp of vueFiles) {
  const result = processVueFile(fp);
  if (result) {
    // Write modified file (backup original first)
    fs.writeFileSync(fp + '.bak', fs.readFileSync(fp, 'utf8'));
    fs.writeFileSync(fp, result.modified);
    Object.assign(allTranslations, result.translations);
    totalReplacements += result.count;
    console.log(`Processed: ${fp.replace(SRC_DIR + '/', '')} (${result.count} replacements)`);
  }
}

console.log(`\nTotal replacements: ${totalReplacements}`);
console.log(`Total unique translations: ${Object.keys(allTranslations).length}`);
