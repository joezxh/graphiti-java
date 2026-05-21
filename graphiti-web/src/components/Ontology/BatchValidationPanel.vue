/**
 * 批量验证面板
 */
<template>
  <div class="batch-validation-panel">
    <div class="panel-toolbar">
      <a-space>
        <a-button type="primary" :loading="validating" @click="handleValidate">
          <template #icon><CheckOutlined /></template>
          开始验证
        </a-button>
        <a-button @click="handleRefresh">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </a-space>
    </div>

    <div class="validation-config">
      <div class="section-header"><span class="section-title">验证配置</span></div>
      <a-form layout="inline">
        <a-form-item label="验证范围">
          <a-select v-model:value="config.scope" style="width: 160px">
            <a-select-option value="all">全部实例</a-select-option>
            <a-select-option value="class">指定类</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item v-if="config.scope === 'class'" label="选择类">
          <a-select v-model:value="config.classType" placeholder="选择类" style="width: 160px" allow-clear>
            <a-select-option v-for="cls in store.classes" :key="cls.id" :value="cls.localName">{{ cls.localName }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="验证规则">
          <a-select v-model:value="config.ruleSet" style="width: 160px">
            <a-select-option value="all">全部规则</a-select-option>
            <a-select-option value="required">仅必填验证</a-select-option>
            <a-select-option value="type">仅类型验证</a-select-option>
            <a-select-option value="range">仅范围验证</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </div>

    <div v-if="validationResult" class="validation-stats">
      <div class="stat-card">
        <div class="stat-value">{{ validationResult.total }}</div><div class="stat-label">总验证数</div>
      </div>
      <div class="stat-card">
        <div class="stat-value green">{{ validationResult.passed }}</div><div class="stat-label">通过</div>
      </div>
      <div class="stat-card">
        <div class="stat-value red">{{ validationResult.failed }}</div><div class="stat-label">失败</div>
      </div>
      <div class="stat-card">
        <div class="stat-value orange">{{ validationResult.warnings }}</div><div class="stat-label">警告</div>
      </div>
      <div class="stat-card">
        <div class="stat-rate" :class="passRateColor">{{ passRate }}%</div><div class="stat-label">通过率</div>
      </div>
    </div>

    <div v-if="validationResult && validationResult.errors.length > 0" class="error-list">
      <div class="section-header">
        <span class="section-title">错误详情</span>
        <a-input-search v-model:value="errorKeyword" placeholder="搜索错误..." style="width: 200px" />
      </div>
      <a-table :columns="errorColumns" :data-source="filteredErrors" :pagination="{ pageSize: 15 }" row-key="id" size="small">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'severity'">
            <a-tag :color="severityColor(record.severity)">{{ record.severity }}</a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-button type="link" size="small" @click="handleLocateError(record)">定位</a-button>
          </template>
        </template>
      </a-table>
    </div>

    <div v-else-if="validationResult && validationResult.errors.length === 0" class="validation-success">
      <a-result status="success" title="全部通过" sub-title="所有实例均满足验证规则" />
    </div>

    <div v-else class="empty-state">
      <a-empty description="点击「开始验证」执行批量验证" />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive } from 'vue'
import { message } from 'ant-design-vue'
import { CheckOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { useOntologyStore } from '@/store/modules/ontology'
import { ontologyApi } from '@/api/ontology'
import { graphApi } from '@/api/graph'

const props = defineProps<{ graphId: string }>()
const store = useOntologyStore()
const validating = ref(false)
const errorKeyword = ref('')

const config = reactive({ scope: 'all', classType: '', ruleSet: 'all' })

interface ValidationError {
  id: string; nodeUuid: string; nodeName: string; field: string; type: string; message: string; severity: string
}

interface ValidationResult { total: number; passed: number; failed: number; warnings: number; errors: ValidationError[] }

const validationResult = ref<ValidationResult | null>(null)

const passRate = computed(() => {
  if (!validationResult.value || validationResult.value.total === 0) return 0
  return Math.round((validationResult.value.passed / validationResult.value.total) * 100)
})

const passRateColor = computed(() => { const r = passRate.value; return r >= 80 ? 'green' : r >= 50 ? 'orange' : 'red' })

const filteredErrors = computed(() => {
  if (!validationResult.value) return []
  if (!errorKeyword.value) return validationResult.value.errors
  const kw = errorKeyword.value.toLowerCase()
  return validationResult.value.errors.filter(e => e.nodeName.toLowerCase().includes(kw) || e.message.toLowerCase().includes(kw))
})

const errorColumns = [
  { title: '节点名称', dataIndex: 'nodeName', key: 'nodeName', ellipsis: true },
  { title: '字段', dataIndex: 'field', key: 'field', width: 120 },
  { title: '错误类型', dataIndex: 'type', key: 'type', width: 120 },
  { title: '错误描述', dataIndex: 'message', key: 'message', ellipsis: true },
  { title: '严重程度', key: 'severity', width: 80 },
  { title: '操作', key: 'action', width: 80 }
]

function severityColor(s?: string) { return { ERROR: 'red', WARNING: 'orange', INFO: 'blue' }[s ?? ''] ?? 'default' }

async function handleValidate() {
  validating.value = true
  try {
    // 1. 收集待验证的类
    const classTypes = config.scope === 'class' && config.classType
      ? [config.classType]
      : store.classes.map(c => c.localName)

    // 2. 加载实例数据并构建验证请求
    const nodes: { nodeType: string; properties: Record<string, any> }[] = []
    let loadCount = 0
    const maxInstances = 100 // 每类最多验证100条

    for (const classType of classTypes) {
      try {
        const result = await graphApi.getClassInstances(props.graphId, classType, {
          page: 1,
          pageSize: maxInstances
        })
        for (const inst of result.data || []) {
          nodes.push({
            nodeType: classType,
            properties: inst.properties || {}
          })
          loadCount++
        }
      } catch (e) {
        console.warn(`加载类 ${classType} 实例失败`, e)
      }
    }

    if (nodes.length === 0) {
      message.warning('未找到可验证的实例数据')
      validating.value = false
      return
    }

    // 3. 调用批量验证 API
    const resp = await ontologyApi.validateBatch(props.graphId, { nodes, edges: [] })

    // 4. 解析结果
    const allErrors: ValidationError[] = []
    let passed = 0
    let failed = 0
    let warnings = 0

    for (let i = 0; i < nodes.length; i++) {
      const nodeResult = resp.nodeResults?.[i]
      if (!nodeResult) continue

      if (nodeResult.passed) {
        passed++
      } else {
        failed++
      }

      // 收集错误
      if (nodeResult.errors) {
        for (const err of nodeResult.errors) {
          allErrors.push({
            id: `err-${i}-${err.code}`,
            nodeUuid: nodes[i].nodeType,
            nodeName: `${nodes[i].nodeType} #${i + 1}`,
            field: err.field || '-',
            type: err.code || 'ERROR',
            message: err.message || '验证失败',
            severity: err.level >= 2 ? 'ERROR' : 'WARNING'
          })
        }
      }

      // 收集警告
      if (nodeResult.warnings) {
        for (const warn of nodeResult.warnings) {
          warnings++
          allErrors.push({
            id: `warn-${i}`,
            nodeUuid: nodes[i].nodeType,
            nodeName: `${nodes[i].nodeType} #${i + 1}`,
            field: '-',
            type: 'WARNING',
            message: warn.message || warn.suggestion || '警告',
            severity: 'WARNING'
          })
        }
      }
    }

    validationResult.value = {
      total: nodes.length,
      passed,
      failed,
      warnings,
      errors: allErrors
    }

    message.success(`验证完成：共 ${nodes.length} 个实例`)
  } catch (e: any) {
    message.error(e.message || '验证失败')
  } finally {
    validating.value = false
  }
}

function handleRefresh() { validationResult.value = null; errorKeyword.value = '' }

function handleLocateError(error: ValidationError) {
  store.openTab({ id: `instance-table-${error.nodeUuid}`, type: 'instance-table', title: '实例数据', classType: config.classType || undefined })
}
</script>

<style scoped lang="less">
.batch-validation-panel {
  display: flex; flex-direction: column; height: 100%; overflow-y: auto; padding: 16px; gap: 16px;
  .panel-toolbar { flex-shrink: 0; }
  .validation-config, .error-list { background: #161b22; border-radius: 8px; padding: 16px; border: 1px solid #30363d;
    .section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; .section-title { font-size: 14px; font-weight: 600; color: #e6edf3; } }
  }
  .validation-stats { display: flex; gap: 16px;
    .stat-card { flex: 1; background: #161b22; border: 1px solid #30363d; border-radius: 8px; padding: 16px; text-align: center;
      .stat-value { font-size: 28px; font-weight: 700; color: #e6edf3; &.green { color: #3fb950; } &.red { color: #f85149; } &.orange { color: #d29922; } }
      .stat-rate { font-size: 28px; font-weight: 700; &.green { color: #3fb950; } &.orange { color: #d29922; } &.red { color: #f85149; } }
      .stat-label { font-size: 12px; color: #8b949e; margin-top: 4px; }
    }
  }
  .validation-success, .empty-state { background: #161b22; border-radius: 8px; padding: 32px; border: 1px solid #30363d; text-align: center; }
}
</style>
