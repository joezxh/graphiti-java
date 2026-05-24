const fs = require('fs');
const path = require('path');

// 扫描目录下所有 .vue 文件中的中文
function scanVueFiles(dir, results = []) {
  const items = fs.readdirSync(dir, { withFileTypes: true });
  for (const item of items) {
    const fullPath = path.join(dir, item.name);
    if (item.isDirectory() && item.name !== 'node_modules') {
      scanVueFiles(fullPath, results);
    } else if (item.isFile() && item.name.endsWith('.vue')) {
      const content = fs.readFileSync(fullPath, 'utf-8');
      // 匹配中文字符（排除注释中的中文）
      const lines = content.split('\n');
      const chineseLines = [];
      for (let i = 0; i < lines.length; i++) {
        const line = lines[i];
        // 跳过 HTML 注释 <!-- --> 和 JS 注释 // /* */
        const trimmed = line.trim();
        if (trimmed.startsWith('//') || trimmed.startsWith('*') || trimmed.startsWith('/*')) continue;
        if (trimmed.startsWith('<!--') || trimmed.endsWith('-->')) continue;
        // 匹配中文字符
        if (/[\u4e00-\u9fff]/.test(line)) {
          chineseLines.push({ line: i + 1, text: line.trim() });
        }
      }
      if (chineseLines.length > 0) {
        results.push({ file: fullPath, count: chineseLines.length, lines: chineseLines.slice(0, 5) });
      }
    }
  }
  return results;
}

const srcDir = path.join(__dirname, '..', 'src');
const results = scanVueFiles(srcDir);

// 按中文行数排序
results.sort((a, b) => b.count - a.count);

console.log(`Found ${results.length} Vue files with Chinese text:`);
console.log('='.repeat(80));
for (const r of results) {
  const relPath = path.relative(srcDir, r.file);
  console.log(`\n${relPath} (${r.count} lines)`);
  for (const l of r.lines) {
    console.log(`  L${l.line}: ${l.text.substring(0, 100)}`);
  }
  if (r.count > r.lines.length) {
    console.log(`  ... and ${r.count - r.lines.length} more`);
  }
}
