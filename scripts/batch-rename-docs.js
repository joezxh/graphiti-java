const fs = require('fs');
const path = require('path');

// 替换规则
const replacements = [
  [/graphiti-server/g, 'ontograph-server'],
  [/graphiti-module-core/g, 'ontograph-module-core'],
  [/graphiti-module-system/g, 'ontograph-module-system'],
  [/graphiti-framework/g, 'ontograph-framework'],
  [/graphiti-web/g, 'ontograph-web'],
  [/graphiti-java/g, 'ontograph-java'],
  [/Graphiti-Java/g, 'OntoGraph'],
  [/Graphiti Java/g, 'OntoGraph'],
  [/Graphiti框架/g, 'OntoGraph框架'],
  [/Graphiti项目/g, 'OntoGraph项目'],
  [/Graphiti系统/g, 'OntoGraph系统'],
];

function walkDir(dir, fileCallback) {
  const files = fs.readdirSync(dir);
  for (const file of files) {
    const filePath = path.join(dir, file);
    const stat = fs.statSync(filePath);
    
    if (stat.isDirectory()) {
      // 跳过这些目录
      if (['node_modules', '.git', 'target', 'dist'].includes(file)) {
        continue;
      }
      walkDir(filePath, fileCallback);
    } else if (file.endsWith('.md')) {
      fileCallback(filePath);
    }
  }
}

let updatedCount = 0;
let skippedCount = 0;

walkDir(path.join(__dirname, '..'), (filePath) => {
  try {
    const content = fs.readFileSync(filePath, 'utf8');
    let newContent = content;
    
    // 应用所有替换
    for (const [pattern, replacement] of replacements) {
      newContent = newContent.replace(pattern, replacement);
    }
    
    // 如果内容有变化,写回文件
    if (newContent !== content) {
      fs.writeFileSync(filePath, newContent, 'utf8');
      console.log(`✓ Updated: ${path.basename(filePath)}`);
      updatedCount++;
    } else {
      skippedCount++;
    }
  } catch (error) {
    console.error(`✗ Error processing ${filePath}: ${error.message}`);
  }
});

console.log('\n========================================');
console.log('批量更新完成!');
console.log('========================================');
console.log(`更新文件数: ${updatedCount}`);
console.log(`跳过文件数: ${skippedCount}`);
console.log('========================================\n');
