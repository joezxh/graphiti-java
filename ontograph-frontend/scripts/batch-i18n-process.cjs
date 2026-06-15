/**
 * 批量处理剩余Vue文件的国际化
 * 使用统一的替换策略快速处理所有文件
 */

const fs = require('fs')
const path = require('path')

const BASE_DIR = path.join(__dirname, '..')

// 需要处理的文件列表
const FILES_TO_PROCESS = [
  {
    file: 'src/components/Ontology/DataImportExportModal.vue',
    namespace: 'dataImportExport',
    prefix: 'ontology'
  },
  {
    file: 'src/components/Ontology/DomainRuleEditModal.vue',
    namespace: 'domainRuleEdit',
    prefix: 'ontology'
  },
  {
    file: 'src/components/Ontology/InstanceForm.vue',
    namespace: 'instanceForm',
    prefix: 'ontology'
  },
  {
    file: 'src/components/Ontology/InstanceDataTable.vue',
    namespace: 'instanceDataTable',
    prefix: 'ontology'
  },
  {
    file: 'src/components/Ontology/ConstraintEditor.vue',
    namespace: 'constraintEditor',
    prefix: 'ontology'
  },
  {
    file: 'src/components/Ontology/ClassListPanel.vue',
    namespace: 'classListPanel',
    prefix: 'ontology'
  },
  {
    file: 'src/components/Ontology/PropertyListPanel.vue',
    namespace: 'propertyListPanel',
    prefix: 'ontology'
  },
  {
    file: 'src/components/Ontology/ConstraintListPanel.vue',
    namespace: 'constraintListPanel',
    prefix: 'ontology'
  },
  {
    file: 'src/components/Graph/AddEdgeModal.vue',
    namespace: 'addEdgeModal',
    prefix: 'graph'
  },
  {
    file: 'src/components/Graph/CascadeEditModal.vue',
    namespace: 'cascadeEditModal',
    prefix: 'graph'
  },
]

/**
 * 添加useI18n导入
 */
function addI18nImport(content) {
  // 检查是否已经有useI18n导入
  if (content.includes("import { useI18n } from 'vue-i18n'")) {
    return content
  }
  
  // 在第一个import语句后添加
  const importRegex = /import\s+.*?from\s+['"].*?['"]/g
  const matches = content.match(importRegex)
  
  if (matches && matches.length > 0) {
    const lastImport = matches[matches.length - 1]
    const lastImportIndex = content.lastIndexOf(lastImport)
    const insertPos = lastImportIndex + lastImport.length
    
    return content.slice(0, insertPos) + 
           "\nimport { useI18n } from 'vue-i18n'" +
           content.slice(insertPos)
  }
  
  return content
}

/**
 * 添加const { t } = useI18n()
 */
function addTFunction(content) {
  if (content.includes('const { t } = useI18n()')) {
    return content
  }
  
  // 在<script setup>块中添加
  const scriptSetupRegex = /<script setup lang="ts">/
  const match = content.match(scriptSetupRegex)
  
  if (match) {
    const insertPos = match.index + match[0].length
    return content.slice(0, insertPos) + '\nconst { t } = useI18n()' + content.slice(insertPos)
  }
  
  return content
}

/**
 * 替换常见的中文模式
 */
function replaceCommonChinese(content) {
  const replacements = [
    // 按钮文本
    [/>(保存)</g, `>{{ t('common.save') }}<`],
    [/>(取消)</g, `>{{ t('common.cancel') }}<`],
    [/>(删除)</g, `>{{ t('common.delete') }}<`],
    [/>(编辑)</g, `>{{ t('common.edit') }}<`],
    [/>(创建)</g, `>{{ t('common.create') }}<`],
    [/>(确认)</g, `>{{ t('common.confirm') }}<`],
    [/>(确定)</g, `>{{ t('common.confirm') }}<`],
    [/>(搜索)</g, `>{{ t('common.search') }}<`],
    [/>(重置)</g, `>{{ t('common.reset') }}<`],
    [/>(关闭)</g, `>{{ t('common.close') }}<`],
    [/>(刷新)</g, `>{{ t('common.refresh') }}<`],
    
    // ok-text 和 cancel-text
    [/ok-text="确定"/g, `:ok-text="t('common.confirm')"`],
    [/ok-text="确认"/g, `:ok-text="t('common.confirm')"`],
    [/cancel-text="取消"/g, `:cancel-text="t('common.cancel')"`],
    
    // message.success/error
    [/message\.success\('([^']+)'\)/g, `message.success(t('TODO_$1'))`],
    [/message\.error\('([^']+)'\)/g, `message.error(t('TODO_$1'))`],
    [/message\.warning\('([^']+)'\)/g, `message.warning(t('TODO_$1'))`],
  ]
  
  let result = content
  replacements.forEach(([pattern, replacement]) => {
    result = result.replace(pattern, replacement)
  })
  
  return result
}

/**
 * 处理单个文件
 */
function processFile(config) {
  const filePath = path.join(BASE_DIR, config.file)
  
  if (!fs.existsSync(filePath)) {
    console.log(`⚠️  文件不存在: ${config.file}`)
    return false
  }
  
  let content = fs.readFileSync(filePath, 'utf8')
  const originalContent = content
  
  // 步骤1: 添加useI18n导入
  content = addI18nImport(content)
  
  // 步骤2: 添加const { t } = useI18n()
  content = addTFunction(content)
  
  // 步骤3: 替换常见中文模式
  content = replaceCommonChinese(content)
  
  // 只在实际修改后写回
  if (content !== originalContent) {
    fs.writeFileSync(filePath, content, 'utf8')
    console.log(`✅ ${config.file}`)
    return true
  } else {
    console.log(`⏭️  ${config.file} (无需修改)`)
    return false
  }
}

/**
 * 主函数
 */
function main() {
  console.log('🚀 开始批量国际化处理...\n')
  
  let processedCount = 0
  
  FILES_TO_PROCESS.forEach(config => {
    if (processFile(config)) {
      processedCount++
    }
  })
  
  console.log(`\n📊 处理完成:`)
  console.log(`   - 总文件数: ${FILES_TO_PROCESS.length}`)
  console.log(`   - 已修改: ${processedCount}`)
  console.log(`\n⚠️  注意:`)
  console.log(`   1. message中的key使用了TODO_前缀,需要手动调整`)
  console.log(`   2. tab标签、表单label等需要根据具体业务逻辑调整`)
  console.log(`   3. 建议逐个文件审查并完善翻译key`)
}

main()
