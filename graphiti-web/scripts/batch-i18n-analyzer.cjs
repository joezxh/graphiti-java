/**
 * 批量Vue文件国际化脚本
 * 自动识别Vue文件中的硬编码中文,生成替换建议
 */

const fs = require('fs')
const path = require('path')

// 配置
const BASE_DIR = path.join(__dirname, '..')
const FILES_TO_PROCESS = [
  // Ontology组件
  'src/components/Ontology/PropertyEditor.vue',
  'src/components/Ontology/ConstraintEditor.vue',
  'src/components/Ontology/ClassListPanel.vue',
  'src/components/Ontology/PropertyListPanel.vue',
  'src/components/Ontology/ConstraintListPanel.vue',
  'src/components/Ontology/CommunityExplorer.vue',
  'src/components/Ontology/EpisodeTypeExplorer.vue',
  'src/components/Ontology/EpisodeTypeEditModal.vue',
  'src/components/Ontology/EpisodeTypeDetailPanel.vue',
  'src/components/Ontology/DomainRuleEditModal.vue',
  'src/components/Ontology/DomainRuleListPanel.vue',
  'src/components/Ontology/DomainRuleTestModal.vue',
  'src/components/Ontology/InstanceForm.vue',
  'src/components/Ontology/InstanceDataTable.vue',
  'src/components/Ontology/OntologyWorkbench.vue',
  'src/components/Ontology/OntologyTabBar.vue',
  'src/components/Ontology/OntologyObjectExplorer.vue',
  'src/components/Ontology/PropertyValueCell.vue',
  'src/components/Ontology/DefinitionEditor.vue',
  'src/components/Ontology/ConsistencyCheckPanel.vue',
  'src/components/Ontology/BatchValidationPanel.vue',
  'src/components/Ontology/VersionHistoryPanel.vue',
  'src/components/Ontology/VersionDiffViewer.vue',
  'src/components/Ontology/DataImportExportModal.vue',
  'src/components/Ontology/OntologyVisualizer.vue',
  // Graph组件
  'src/components/Graph/AddEdgeModal.vue',
  'src/components/Graph/CascadeEditModal.vue',
  'src/components/Graph/ForceGraph.vue',
  // 其他页面
  'src/views/legal-kg/index.vue',
  'src/views/dashboard/index.vue',
  'src/views/search/index.vue',
  'src/views/monitor/index.vue',
  'src/views/notification/index.vue',
  'src/views/profile/index.vue',
  'src/views/prompt/index.vue',
  'src/views/custom-instructions/index.vue',
  'src/views/login/index.vue',
  'src/views/404/index.vue',
  // System页面
  'src/views/system/user/index.vue',
  'src/views/system/role/index.vue',
  'src/views/system/menu/index.vue',
  'src/views/system/config/index.vue',
  'src/views/system/log/index.vue',
]

// 中文正则
const CHINESE_REGEX = /[\u4e00-\u9fff]+/
const CHINESE_LINE_REGEX = /^(?!\s*\/\/)(?!\s*\/\*)(?!\s*\*).*[\u4e00-\u9fff]/

// 常见中文模式映射
const COMMON_PATTERNS = {
  '保存': 'common.save',
  '取消': 'common.cancel',
  '删除': 'common.delete',
  '编辑': 'common.edit',
  '创建': 'common.create',
  '确认': 'common.confirm',
  '确定': 'common.confirm',
  '搜索': 'common.search',
  '重置': 'common.reset',
  '提交': 'common.submit',
  '关闭': 'common.close',
  '是': 'common.yes',
  '否': 'common.no',
  '成功': 'common.success',
  '失败': 'common.failed',
}

/**
 * 分析单个文件
 */
function analyzeFile(filePath) {
  const fullPath = path.join(BASE_DIR, filePath)
  
  if (!fs.existsSync(fullPath)) {
    console.log(`⚠️  文件不存在: ${filePath}`)
    return null
  }
  
  const content = fs.readFileSync(fullPath, 'utf8')
  const lines = content.split('\n')
  
  const chineseLines = []
  lines.forEach((line, index) => {
    // 跳过注释行和import行
    if (line.trim().startsWith('//') || 
        line.trim().startsWith('/*') ||
        line.trim().startsWith('*') ||
        line.trim().startsWith('import ') ||
        line.trim().startsWith('from ')) {
      return
    }
    
    // 检查是否包含中文
    if (CHINESE_REGEX.test(line)) {
      chineseLines.push({
        lineNum: index + 1,
        content: line.trim(),
        chinese: line.match(/[\u4e00-\u9fff]+/g) || []
      })
    }
  })
  
  return {
    filePath,
    totalLines: lines.length,
    chineseLineCount: chineseLines.length,
    chineseLines
  }
}

/**
 * 生成命名空间建议
 */
function suggestNamespace(filePath) {
  if (filePath.includes('Ontology/')) {
    const fileName = path.basename(filePath, '.vue')
    return `ontology.${fileName.charAt(0).toLowerCase() + fileName.slice(1)}`
  } else if (filePath.includes('Graph/')) {
    return 'graphComponent'
  } else if (filePath.includes('views/legal-kg')) {
    return 'legalKg'
  } else if (filePath.includes('views/dashboard')) {
    return 'dashboard'
  } else if (filePath.includes('views/search')) {
    return 'search'
  } else if (filePath.includes('views/system')) {
    return 'system'
  } else {
    return 'common'
  }
}

/**
 * 主函数
 */
function main() {
  console.log('🔍 开始分析Vue文件中的硬编码中文...\n')
  
  const results = []
  let totalChineseLines = 0
  
  FILES_TO_PROCESS.forEach(filePath => {
    const result = analyzeFile(filePath)
    if (result && result.chineseLineCount > 0) {
      results.push(result)
      totalChineseLines += result.chineseLineCount
    }
  })
  
  console.log(`📊 分析完成:`)
  console.log(`   - 检查文件数: ${FILES_TO_PROCESS.length}`)
  console.log(`   - 含中文文件数: ${results.length}`)
  console.log(`   - 含中文总行数: ${totalChineseLines}\n`)
  
  // 生成报告
  const report = results.map(r => {
    const namespace = suggestNamespace(r.filePath)
    return {
      file: r.filePath,
      lines: r.chineseLineCount,
      namespace,
      sample: r.chineseLines.slice(0, 5).map(l => `   Line ${l.lineNum}: ${l.content.substring(0, 80)}...`)
    }
  })
  
  // 输出详细报告
  console.log('📋 详细报告:\n')
  report.forEach((r, idx) => {
    console.log(`${idx + 1}. ${r.file}`)
    console.log(`   命名空间建议: ${r.namespace}`)
    console.log(`   中文行数: ${r.lines}`)
    console.log(`   示例:`)
    r.sample.forEach(s => console.log(s))
    console.log('')
  })
  
  // 生成处理脚本建议
  console.log('💡 建议处理顺序:')
  const sorted = [...report].sort((a, b) => b.lines - a.lines)
  sorted.slice(0, 10).forEach((r, idx) => {
    console.log(`   ${idx + 1}. ${r.file} (${r.lines}行中文)`)
  })
  
  // 保存报告到文件
  const reportPath = path.join(__dirname, 'i18n-batch-report.json')
  fs.writeFileSync(reportPath, JSON.stringify(report, null, 2), 'utf8')
  console.log(`\n📄 详细报告已保存到: ${reportPath}`)
}

main()
