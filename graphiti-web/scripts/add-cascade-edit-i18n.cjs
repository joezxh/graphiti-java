/**
 * 添加cascadeEdit翻译key到zh-TW.ts和ja-JP.ts
 */

const fs = require('fs')
const path = require('path')

const BASE_DIR = path.join(__dirname, '../src/i18n/locales')

// 繁体中文翻译
const zhTWCascadeEdit = `    cascadeEdit: {
      title: '屬性階級編輯',
      nodeType: '節點類型',
      selectNodeType: '選擇要編輯的節點類型',
      properties: '屬性',
      filterSection: '篩選條件',
      selectProperty: '選擇屬性',
      value: '值',
      addCondition: '添加條件',
      logicHint: '多個條件的組合方式',
      previewImpact: '預覽影響範圍',
      previewTitle: '影響範圍預覽',
      nodesMatched: '個節點匹配',
      nodes: '個節點',
      distribution: '分佈',
      updateSection: '修改內容',
      newValue: '新值',
      addMoreUpdates: '添加更多修改',
      confirmUpdate: '確認修改',
      op: {
        eq: '等於',
        ne: '不等於',
        gt: '大於',
        lt: '小於',
        gte: '大於等於',
        lte: '小於等於',
        contains: '包含',
        notContains: '不包含',
        in: '在列表中',
        notIn: '不在列表',
        isNull: '為空',
        isNotNull: '不為空',
      },
      selectNodeClassFirst: '請先選擇節點類型',
      addFilterCondition: '請至少添加一個篩選條件',
      previewFailed: '預覽失敗',
      noMatchedNodes: '沒有匹配的節點',
      setUpdateProperty: '請設置要修改的屬性',
      executeSuccess: '成功修改 {count} 個節點',
      executePartialFailed: '部分失敗: {count} 個',
      executeFailed: '執行失敗',
    },
`

// 日语翻译
const jaJPCascadeEdit = `    cascadeEdit: {
      title: 'プロパティカスケード編集',
      nodeType: 'ノードタイプ',
      selectNodeType: '編集するノードタイプを選択',
      properties: 'プロパティ',
      filterSection: 'フィルター条件',
      selectProperty: 'プロパティを選択',
      value: '値',
      addCondition: '条件を追加',
      logicHint: '複数の条件の組み合わせ方法',
      previewImpact: '影響範囲をプレビュー',
      previewTitle: '影響範囲プレビュー',
      nodesMatched: '件のノードが一致',
      nodes: '件のノード',
      distribution: '分布',
      updateSection: '更新内容',
      newValue: '新しい値',
      addMoreUpdates: '更新を追加',
      confirmUpdate: '更新を確認',
      op: {
        eq: '等しい',
        ne: '等しくない',
        gt: 'より大きい',
        lt: 'より小さい',
        gte: '以上',
        lte: '以下',
        contains: '含む',
        notContains: '含まない',
        in: 'リスト内',
        notIn: 'リスト外',
        isNull: 'null',
        isNotNull: 'nullでない',
      },
      selectNodeClassFirst: '最初にノードタイプを選択してください',
      addFilterCondition: '少なくとも1つのフィルター条件を追加してください',
      previewFailed: 'プレビューに失敗しました',
      noMatchedNodes: '一致するノードがありません',
      setUpdateProperty: '更新するプロパティを設定してください',
      executeSuccess: '{count}件のノードを正常に更新しました',
      executePartialFailed: '部分的に失敗: {count}件',
      executeFailed: '実行に失敗しました',
    },
`

// 处理zh-TW.ts
const zhTWPath = path.join(BASE_DIR, 'zh-TW.ts')
let zhTWContent = fs.readFileSync(zhTWPath, 'utf8')

const zhTWSearchNode = "    searchNodePlaceholder: '搜尋節點...',\n"
const zhTWInsertPos = zhTWContent.indexOf(zhTWSearchNode) + zhTWSearchNode.length

// 删除旧的cascadeEdit行
zhTWContent = zhTWContent.replace(/    cascadeEdit: '階級編輯',\n/, '')

zhTWContent = zhTWContent.slice(0, zhTWInsertPos) + zhTWCascadeEdit + zhTWContent.slice(zhTWInsertPos)

fs.writeFileSync(zhTWPath, zhTWContent, 'utf8')
console.log('✅ zh-TW.ts cascadeEdit keys已添加')

// 处理ja-JP.ts
const jaJPPath = path.join(BASE_DIR, 'ja-JP.ts')
let jaJPContent = fs.readFileSync(jaJPPath, 'utf8')

const jaJPSearchNode = "    searchNodePlaceholder: 'ノードを検索...',\n"
const jaJPInsertPos = jaJPContent.indexOf(jaJPSearchNode) + jaJPSearchNode.length

// 删除旧的cascadeEdit行
jaJPContent = jaJPContent.replace(/    cascadeEdit: 'カスケード編集',\n/, '')

jaJPContent = jaJPContent.slice(0, jaJPInsertPos) + jaJPCascadeEdit + jaJPContent.slice(jaJPInsertPos)

fs.writeFileSync(jaJPPath, jaJPContent, 'utf8')
console.log('✅ ja-JP.ts cascadeEdit keys已添加')

console.log('\n🎉 所有翻译文件cascadeEdit命名空间添加完成')
