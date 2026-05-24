/**
 * 数据导入导出弹窗
 * 支持 CSV/JSON 格式、字段映射、预览、导入进度
 */
<template>
  <a-modal
    v-model:open="visible"
    :title="mode === 'import' ? '导入实例数据' : '导出实例数据'"
    width="700px"
    :footer="null"
    @cancel="handleClose"
  >
    <div class="import-export-modal">
      <!-- 模式切换 -->
      <div class="mode-tabs">
        <a-radio-group v-model:value="mode" button-style="solid">
          <a-radio-button value="import">导入</a-radio-button>
          <a-radio-button value="export">导出</a-radio-button>
        </a-radio-group>
      </div>

      <!-- 导入模式 -->
      <div v-if="mode === 'import'" class="import-section">
        <!-- 步骤1: 选择类 -->
        <div class="step-section">
          <div class="step-label"><span class="step-num">1</span> 选择目标类</div>
          <a-select
            v-model:value="importConfig.classType"
            placeholder="选择要导入到的类"
            style="width: 100%"
            show-search
          >
            <a-select-option v-for="cls in store.classes" :key="cls.id" :value="cls.localName">
              {{ cls.localName }}
            </a-select-option>
          </a-select>
        </div>

        <!-- 步骤2: 选择文件 -->
        <div class="step-section">
          <div class="step-label"><span class="step-num">2</span> 选择文件</div>
          <a-upload-dragger
            v-model:file-list="fileList"
            name="file"
            :multiple="false"
            :before-upload="handleBeforeUpload"
            accept=".csv,.json"
            @remove="handleRemoveFile"
          >
            <p class="ant-upload-drag-icon">
              <InboxOutlined />
            </p>
            <p class="ant-upload-text">点击或拖拽文件到此区域</p>
            <p class="ant-upload-hint">支持 CSV 和 JSON 格式</p>
          </a-upload-dragger>
        </div>

        <!-- 步骤3: 字段映射 -->
        <div v-if="parsedData.length > 0" class="step-section">
          <div class="step-label">
            <span class="step-num">3</span> 字段映射
            <a-button type="link" size="small" @click="handleAutoMap">自动匹配</a-button>
          </div>
          <div class="field-mapping">
            <div class="mapping-header">
              <span>源字段</span>
              <span style="color: #8b949e">→</span>
              <span>目标属性</span>
            </div>
            <div v-for="(col, idx) in sourceColumns" :key="idx" class="mapping-row">
              <span class="source-col">{{ col }}</span>
              <a-select
                v-model:value="fieldMapping[col]"
                placeholder="跳过此字段"
                style="width: 200px"
                allow-clear
              >
                <a-select-option v-for="prop in targetProperties" :key="prop.localName" :value="prop.localName">
                  {{ prop.localName }} ({{ prop.rangeDataType || 'string' }})
                </a-select-option>
              </a-select>
            </div>
          </div>
        </div>

        <!-- 步骤4: 预览 -->
        <div v-if="parsedData.length > 0" class="step-section">
          <div class="step-label"><span class="step-num">4</span> 数据预览 (前5行)</div>
          <div class="data-preview">
            <table class="preview-table">
              <thead>
                <tr>
                  <th v-for="col in sourceColumns" :key="col">{{ col }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(row, idx) in parsedData.slice(0, 5)" :key="idx">
                  <td v-for="col in sourceColumns" :key="col">{{ row[col] ?? '-' }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- 步骤5: 导入配置 -->
        <div v-if="parsedData.length > 0" class="step-section">
          <div class="step-label"><span class="step-num">5</span> 导入配置</div>
          <a-form layout="inline">
            <a-form-item label="冲突处理">
              <a-select v-model:value="importConfig.conflictStrategy" style="width: 180px">
                <a-select-option value="skip">跳过重复</a-select-option>
                <a-select-option value="update">更新已有</a-select-option>
                <a-select-option value="error">报错终止</a-select-option>
              </a-select>
            </a-form-item>
          </a-form>
        </div>

        <!-- 导入进度 -->
        <div v-if="importing" class="import-progress">
          <a-progress :percent="importProgress" status="active" />
          <div class="progress-detail">
            已处理: {{ importedCount }} / {{ parsedData.length }}
            <span v-if="importErrors.length > 0" style="color: #f85149"> | 失败: {{ importErrors.length }}</span>
          </div>
        </div>

        <!-- 导入结果 -->
        <div v-if="importDone" class="import-result">
          <a-result
            :status="importErrors.length === 0 ? 'success' : 'warning'"
            :title="importErrors.length === 0 ? '导入完成' : '导入完成(部分失败)'"
            :sub-title="`成功: ${importedCount - importErrors.length}, 失败: ${importErrors.length}`"
          />
          <div v-if="importErrors.length > 0" class="error-list">
            <div class="error-header">失败记录:</div>
            <div v-for="(err, idx) in importErrors.slice(0, 10)" :key="idx" class="error-item">
              {{ err }}
            </div>
            <div v-if="importErrors.length > 10" class="error-more">
              还有 {{ importErrors.length - 10 }} 条失败记录...
            </div>
          </div>
        </div>

        <div class="modal-footer">
          <a-space>
            <a-button @click="handleClose">{{ t('common.cancel') }}</a-button>
            <a-button
              v-if="!importing && !importDone"
              type="primary"
              :disabled="parsedData.length === 0 || !importConfig.classType"
              :loading="importing"
              @click="handleImport"
            >
              开始导入 ({{ parsedData.length }} 条)
            </a-button>
            <a-button v-if="importDone" type="primary" @click="handleClose">完成</a-button>
          </a-space>
        </div>
      </div>

      <!-- 导出模式 -->
      <div v-if="mode === 'export'" class="export-section">
        <div class="step-section">
          <div class="step-label"><span class="step-num">1</span> 选择类</div>
          <a-select
            v-model:value="exportConfig.classType"
            placeholder="选择要导出的类(不选则导出全部)"
            style="width: 100%"
            allow-clear
            show-search
          >
            <a-select-option v-for="cls in store.classes" :key="cls.id" :value="cls.localName">
              {{ cls.localName }}
            </a-select-option>
          </a-select>
        </div>

        <div class="step-section">
          <div class="step-label"><span class="step-num">2</span> 格式与范围</div>
          <a-form layout="vertical">
            <a-form-item label="导出格式">
              <a-radio-group v-model:value="exportConfig.format">
                <a-radio value="json">JSON</a-radio>
                <a-radio value="csv">CSV</a-radio>
              </a-radio-group>
            </a-form-item>
            <a-form-item label="导出范围">
              <a-radio-group v-model:value="exportConfig.scope">
                <a-radio value="all">全部实例</a-radio>
                <a-radio value="page">当前页</a-radio>
                <a-radio value="selected">已选中 ({{ exportSelectedCount }} 条)</a-radio>
              </a-radio-group>
            </a-form-item>
            <a-form-item label="字段选择">
              <a-checkbox-group v-model:value="exportConfig.selectedFields">
                <a-checkbox v-for="field in exportableFields" :key="field" :value="field">{{ field }}</a-checkbox>
              </a-checkbox-group>
            </a-form-item>
          </a-form>
        </div>

        <div class="export-preview">
          <div class="preview-label">导出预览:</div>
          <div class="preview-code">{{ exportPreview }}</div>
        </div>

        <div class="modal-footer">
          <a-space>
            <a-button @click="handleClose">{{ t('common.cancel') }}</a-button>
            <a-button type="primary" :loading="exporting" @click="handleExport">
              导出文件
            </a-button>
          </a-space>
        </div>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
const { t } = useI18n()
import { ref, reactive, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import { InboxOutlined } from '@ant-design/icons-vue'
import { useOntologyStore } from '@/store/modules/ontology'
import { graphApi } from '@/api/graph'
import { useI18n } from 'vue-i18n'

const props = defineProps<{
  graphId: string
  classType?: string
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'imported', count: number): void
}>()

const store = useOntologyStore()
const visible = ref(false)

// 模式
const mode = ref<'import' | 'export'>('import')

// 导入相关
const fileList = ref<any[]>([])
const parsedData = ref<any[]>([])
const sourceColumns = ref<string[]>([])
const fieldMapping = reactive<Record<string, string>>({})
const importing = ref(false)
const importProgress = ref(0)
const importedCount = ref(0)
const importErrors = ref<string[]>([])
const importDone = ref(false)

const importConfig = reactive({
  classType: '',
  conflictStrategy: 'skip'
})

// 导出相关
const exporting = ref(false)
const exportSelectedCount = ref(0)
const exportConfig = reactive({
  classType: '',
  format: 'json' as 'json' | 'csv',
  scope: 'all' as 'all' | 'page' | 'selected',
  selectedFields: [] as string[]
})

// 初始化
watch(visible, (val) => {
  if (val && props.classType) {
    importConfig.classType = props.classType
  }
})

watch(() => props.classType, (val) => {
  if (val) importConfig.classType = val
})

function open() {
  visible.value = true
  importDone.value = false
  parsedData.value = []
  importProgress.value = 0
  importErrors.value = []
  importConfig.classType = props.classType ?? ''
  fileList.value = []
}

function handleClose() {
  visible.value = false
  emit('close')
}

const targetProperties = computed(() => {
  const cls = store.classes.find(c => c.localName === importConfig.classType)
  if (!cls) return store.properties
  return store.properties.filter(p => p.domainClassId === cls.id)
})

// CSV 解析
function parseCSV(text: string): any[] {
  const lines = text.trim().split('\n')
  if (lines.length < 2) return []
  const headers = lines[0].split(',').map(h => h.trim().replace(/^"|"$/g, ''))
  const rows: any[] = []
  for (let i = 1; i < lines.length; i++) {
    const values = lines[i].split(',').map(v => v.trim().replace(/^"|"$/g, ''))
    const row: any = {}
    headers.forEach((h, idx) => { row[h] = values[idx] ?? '' })
    rows.push(row)
  }
  return rows
}

function parseJSON(text: string): any[] {
  try {
    const data = JSON.parse(text)
    if (Array.isArray(data)) return data
    if (data.records) return data.records
    if (data.data) return data.data
    return [data]
  } catch {
    return []
  }
}

async function handleBeforeUpload(file: any) {
  const text = await file.text()
  if (file.name.endsWith('.csv')) {
    parsedData.value = parseCSV(text)
  } else {
    parsedData.value = parseJSON(text)
  }

  if (parsedData.value.length > 0) {
    sourceColumns.value = Object.keys(parsedData.value[0])
    handleAutoMap()
    message.success(`已解析 ${parsedData.value.length} 条数据`)
  } else {
    message.error(t('TODO_文件格式错误或为空'))
  }
  return false // 阻止默认上传
}

function handleRemoveFile() {
  parsedData.value = []
  sourceColumns.value = []
  Object.keys(fieldMapping).forEach(k => delete fieldMapping[k])
}

function handleAutoMap() {
  sourceColumns.value.forEach(col => {
    if (!fieldMapping[col]) {
      const matched = targetProperties.value.find(p =>
        p.localName.toLowerCase() === col.toLowerCase() ||
        p.localName.toLowerCase().includes(col.toLowerCase()) ||
        col.toLowerCase().includes(p.localName.toLowerCase())
      )
      if (matched) fieldMapping[col] = matched.localName
    }
  })
}

async function handleImport() {
  if (!importConfig.classType) {
    message.warning(t('TODO_请先选择目标类'))
    return
  }

  importing.value = true
  importProgress.value = 0
  importedCount.value = 0
  importErrors.value = []
  importDone.value = false

  try {
    for (let i = 0; i < parsedData.value.length; i++) {
      const row = parsedData.value[i]
      const properties: Record<string, any> = {}

      sourceColumns.value.forEach(col => {
        const mapped = fieldMapping[col]
        if (mapped) {
          properties[mapped] = row[col]
        }
      })

      try {
        await graphApi.createNode(props.graphId, {
          name: row.name || row.Nome || `Import_${i}`,
          type: importConfig.classType,
          properties
        })
        importedCount.value++
      } catch (e: any) {
        if (importConfig.conflictStrategy === 'error') {
          importErrors.value.push(`行${i + 1}: ${e.message}`)
        } else if (importConfig.conflictStrategy === 'skip') {
          // 跳过
        } else {
          try {
            await graphApi.createNode(props.graphId, {
              name: row.name || `Import_${i}_${Date.now()}`,
              type: importConfig.classType,
              properties
            })
            importedCount.value++
          } catch {
            importErrors.value.push(`行${i + 1}: ${e.message}`)
          }
        }
      }

      importProgress.value = Math.round(((i + 1) / parsedData.value.length) * 100)
    }

    importDone.value = true
    message.success(`导入完成: 成功 ${importedCount.value - importErrors.value.length}, 失败 ${importErrors.value.length}`)
    emit('imported', importedCount.value - importErrors.value.length)
  } finally {
    importing.value = false
  }
}

// 导出
const exportableFields = computed(() => {
  const fields = ['name', 'type', 'uuid', 'createdAt', 'updatedAt']
  targetProperties.value.forEach(p => fields.push(p.localName))
  return fields
})

const exportPreview = computed(() => {
  if (exportConfig.format === 'json') {
    return JSON.stringify({ name: '示例', type: exportConfig.classType || 'Person', properties: {} }, null, 2)
  }
  return 'name,type,uuid\n示例,Person,xxx-uuid'
})

async function handleExport() {
  exporting.value = true
  try {
      const data = await graphApi.getClassInstances(props.graphId, exportConfig.classType || '', {
        page: 1,
        pageSize: 10000
      })
    const records = (data.data ?? []).filter(() =>
      exportConfig.scope === 'selected' ? exportSelectedCount.value > 0 : true
    )

    const fields = exportConfig.selectedFields.length > 0
      ? exportConfig.selectedFields
      : exportableFields.value

    let content = ''
    let filename = ''
    const graphName = 'ontology-export'

    if (exportConfig.format === 'json') {
      content = JSON.stringify(records, null, 2)
      filename = `${graphName}-${Date.now()}.json`
    } else {
      const header = fields.join(',')
      const rows = records.map((r: any) =>
        fields.map(f => {
          const val = f === 'uuid' ? r.uuid : f === 'name' ? r.name : f === 'type' ? r.type : r.properties?.[f]
          return `"${String(val ?? '').replace(/"/g, '""')}"`
        }).join(',')
      )
      content = [header, ...rows].join('\n')
      filename = `${graphName}-${Date.now()}.csv`
    }

    const blob = new Blob([content], { type: exportConfig.format === 'json' ? 'application/json' : 'text/csv' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    a.click()
    URL.revokeObjectURL(url)

    message.success(`导出成功: ${records.length} 条记录`)
    handleClose()
  } catch (e: any) {
    message.error(e.message || '导出失败')
  } finally {
    exporting.value = false
  }
}

defineExpose({ open })
</script>

<style scoped lang="less">
.import-export-modal {
  min-height: 400px;

  .mode-tabs {
    margin-bottom: 16px;
  }

  .step-section {
    margin-bottom: 20px;

    .step-label {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 8px;
      font-size: 14px;
      font-weight: 500;
      color: #e6edf3;

      .step-num {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        width: 20px;
        height: 20px;
        border-radius: 50%;
        background: #58a6ff;
        color: #fff;
        font-size: 12px;
        font-weight: 700;
      }
    }
  }

  .field-mapping {
    background: #161b22;
    border-radius: 8px;
    padding: 12px;
    border: 1px solid #30363d;

    .mapping-header {
      display: grid;
      grid-template-columns: 1fr 30px 220px;
      gap: 8px;
      padding: 8px;
      font-size: 12px;
      color: #8b949e;
      font-weight: 600;
    }

    .mapping-row {
      display: grid;
      grid-template-columns: 1fr 30px 220px;
      gap: 8px;
      align-items: center;
      padding: 6px 8px;
      border-bottom: 1px solid #21262d;

      &:last-child { border-bottom: none; }

      .source-col {
        font-size: 13px;
        color: #e6edf3;
        font-family: monospace;
      }
    }
  }

  .data-preview {
    overflow-x: auto;
    border: 1px solid #30363d;
    border-radius: 8px;

    .preview-table {
      width: 100%;
      border-collapse: collapse;
      font-size: 12px;

      th, td {
        padding: 8px 12px;
        border-bottom: 1px solid #21262d;
        text-align: left;
        max-width: 200px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      th {
        background: #161b22;
        color: #8b949e;
        font-weight: 600;
        position: sticky;
        top: 0;
      }

      tr:hover td { background: #161b22; }
    }
  }

  .import-progress {
    margin: 16px 0;

    .progress-detail {
      text-align: center;
      font-size: 13px;
      color: #8b949e;
      margin-top: 8px;
    }
  }

  .import-result {
    .error-list {
      background: #161b22;
      border-radius: 8px;
      padding: 12px;
      max-height: 200px;
      overflow-y: auto;

      .error-header {
        font-weight: 600;
        margin-bottom: 8px;
        color: #f85149;
      }

      .error-item {
        font-size: 12px;
        color: #f85149;
        padding: 4px 0;
        border-bottom: 1px solid #21262d;
        font-family: monospace;
      }

      .error-more {
        font-size: 12px;
        color: #8b949e;
        padding: 8px 0;
        text-align: center;
      }
    }
  }

  .export-section {
    .export-preview {
      background: #161b22;
      border: 1px solid #30363d;
      border-radius: 8px;
      padding: 12px;
      margin-top: 16px;

      .preview-label {
        font-size: 13px;
        color: #8b949e;
        margin-bottom: 8px;
      }

      .preview-code {
        font-family: monospace;
        font-size: 12px;
        color: #3fb950;
        white-space: pre-wrap;
        max-height: 150px;
        overflow-y: auto;
      }
    }
  }

  .modal-footer {
    margin-top: 16px;
    padding-top: 16px;
    border-top: 1px solid #30363d;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
