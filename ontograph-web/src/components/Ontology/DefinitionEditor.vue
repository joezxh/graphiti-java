/**
 * 本体定义编辑器 — 管理本体元数据（命名空间、版本、状态等）
 */
<template>
  <div class="definition-editor">
    <div class="editor-toolbar">
      <a-space>
        <a-button type="primary" :loading="saving" @click="handleSave">
          <template #icon><SaveOutlined /></template>
          保存
        </a-button>
      </a-space>
      <div class="toolbar-right">
        <a-tag v-if="store.definition?.id" color="blue">ID: {{ store.definition.id }}</a-tag>
        <a-tag :color="statusColor">{{ form.status }}</a-tag>
      </div>
    </div>

    <div class="tab-content">
      <!-- 统计卡片 -->
      <a-row :gutter="16" class="stats-row">
        <a-col :span="6">
          <div class="stat-card">
            <div class="stat-value blue">{{ store.definition?.classCount ?? 0 }}</div>
            <div class="stat-label">类定义</div>
          </div>
        </a-col>
        <a-col :span="6">
          <div class="stat-card">
            <div class="stat-value purple">{{ store.definition?.propertyCount ?? 0 }}</div>
            <div class="stat-label">属性定义</div>
          </div>
        </a-col>
        <a-col :span="6">
          <div class="stat-card">
            <div class="stat-value orange">{{ store.definition?.constraintCount ?? 0 }}</div>
            <div class="stat-label">约束规则</div>
          </div>
        </a-col>
        <a-col :span="6">
          <div class="stat-card">
            <div class="stat-value green">{{ store.definition?.version ?? '-' }}</div>
            <div class="stat-label">当前版本</div>
          </div>
        </a-col>
      </a-row>

      <a-divider />

      <a-form :model="form" layout="vertical" class="basic-form">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="本体名称" required>
              <a-input v-model:value="form.name" placeholder="如 电商领域本体" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="版本号">
              <a-input v-model:value="form.version" placeholder="1.0.0" />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="命名空间">
              <a-input v-model:value="form.namespace" placeholder="http://ontograph.io/ontology" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="状态">
              <a-select v-model:value="form.status">
                <a-select-option value="ACTIVE">活跃</a-select-option>
                <a-select-option value="DEPRECATED">已弃用</a-select-option>
                <a-select-option value="ARCHIVED">已归档</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item label="描述">
          <a-textarea v-model:value="form.description" :rows="4" placeholder="本体的用途、覆盖领域、版本说明..." />
        </a-form-item>

        <a-form-item label="创建者" v-if="store.definition?.createdBy">
          <span class="readonly-field">{{ store.definition.createdBy }}</span>
        </a-form-item>

        <a-form-item label="创建时间" v-if="store.definition?.createdAt">
          <span class="readonly-field">{{ formatDate(store.definition.createdAt) }}</span>
        </a-form-item>
      </a-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import { SaveOutlined } from '@ant-design/icons-vue'
import { useOntologyStore } from '@/store/modules/ontology'
import { ontologyApi } from '@/api/ontology'

const props = defineProps<{ graphId: string }>()
const emit = defineEmits<{ (e: 'saved'): void }>()

const store = useOntologyStore()
const saving = ref(false)

const form = reactive({
  name: '',
  namespace: '',
  version: '1.0.0',
  status: 'ACTIVE',
  description: ''
})

const statusColor = computed(() => {
  switch (form.status) {
    case 'ACTIVE': return 'green'
    case 'DEPRECATED': return 'orange'
    case 'ARCHIVED': return 'red'
    default: return 'default'
  }
})

function loadFromStore() {
  const def = store.definition
  if (!def) return
  form.name = def.name || ''
  form.namespace = def.namespace || ''
  form.version = def.version || '1.0.0'
  form.status = def.status || 'ACTIVE'
  form.description = def.description || ''
}

function formatDate(date?: string): string {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

async function handleSave() {
  if (!form.name.trim()) {
    message.error('请填写本体名称')
    return
  }
  saving.value = true
  try {
    // 如果已有定义则更新，否则创建
    if (store.definition?.id) {
      // 更新：通过创建新版本的方式
      await ontologyApi.createDefinition(props.graphId, {
        name: form.name,
        namespace: form.namespace || undefined,
        version: form.version || undefined,
        description: form.description || undefined
      })
      message.success('本体定义已更新')
    } else {
      await ontologyApi.createDefinition(props.graphId, {
        name: form.name,
        namespace: form.namespace || undefined,
        version: form.version || undefined,
        description: form.description || undefined
      })
      message.success('本体定义已创建')
    }
    await store.loadFullOntology(props.graphId)
    emit('saved')
  } catch (e: any) {
    message.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

watch(() => store.definition, () => loadFromStore(), { immediate: true })
</script>

<style scoped lang="less">
.definition-editor {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;

  .editor-toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 16px;
    background: #161b22;
    border-bottom: 1px solid #30363d;
    flex-shrink: 0;

    .toolbar-right { display: flex; gap: 8px; align-items: center; }
  }

  .tab-content {
    flex: 1;
    overflow-y: auto;
    padding: 20px;
  }

  .stats-row { margin-bottom: 24px; }

  .stat-card {
    background: #0d1117;
    border: 1px solid #21262d;
    border-radius: 6px;
    padding: 16px;
    text-align: center;

    .stat-value {
      font-size: 28px;
      font-weight: 700;
      color: #e6edf3;
      &.green { color: #3fb950; }
      &.purple { color: #a371f7; }
      &.orange { color: #d29922; }
      &.blue { color: #58a6ff; }
      &.red { color: #f85149; }
    }
    .stat-label {
      font-size: 12px;
      color: #8b949e;
      margin-top: 4px;
    }
  }

  .basic-form { max-width: 800px; }

  .readonly-field {
    color: #8b949e;
    font-size: 13px;
  }
}
</style>
