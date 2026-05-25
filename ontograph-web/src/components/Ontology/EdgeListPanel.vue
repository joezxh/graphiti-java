<template>
  <div class="edge-list-panel">
    <div class="panel-toolbar">
      <a-space>
        <a-button type="primary" @click="showModal = true">
          <template #icon><PlusOutlined /></template>
          新建关系边
        </a-button>
        <a-button :loading="refreshing" @click="handleRefresh">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </a-space>
    </div>

    <a-table
      :columns="columns"
      :data-source="edgeList"
      :loading="loading"
      :pagination="{ pageSize: 20 }"
      row-key="uuid"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'source'">
          <a-tag color="blue">{{ truncate(record.sourceNodeUuid, 8) }}</a-tag>
        </template>
        <template v-if="column.key === 'target'">
          <a-tag color="purple">{{ truncate(record.targetNodeUuid, 8) }}</a-tag>
        </template>
        <template v-if="column.key === 'fact'">
          <span class="fact-text">{{ record.fact || record.name || '-' }}</span>
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="handleEdit(record)">编辑</a-button>
            <a-popconfirm title="确定删除？" ok-text="确定" @confirm="handleDelete(record)">
              <a-button type="link" size="small" danger>删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 新建/编辑模态框 -->
    <a-modal v-model:open="showModal" :title="editingId ? '编辑关系边' : '新建关系边'" @ok="handleSave">
      <a-form :model="form" layout="vertical">
        <a-form-item label="源节点 UUID" required>
          <a-input v-model:value="form.sourceNodeUuid" placeholder="输入源节点 UUID" />
        </a-form-item>
        <a-form-item label="目标节点 UUID" required>
          <a-input v-model:value="form.targetNodeUuid" placeholder="输入目标节点 UUID" />
        </a-form-item>
        <a-form-item label="关系类型">
          <a-input v-model:value="form.type" placeholder="输入关系类型" />
        </a-form-item>
        <a-form-item label="事实描述">
          <a-textarea v-model:value="form.fact" :rows="3" placeholder="输入事实描述" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { edgeApi, type EdgeListItem } from '@/api/edge'

const props = defineProps<{ graphId: string }>()

const loading = ref(false)
const refreshing = ref(false)
const showModal = ref(false)
const editingId = ref<string | null>(null)
const edgeList = ref<EdgeListItem[]>([])

const form = reactive({
  sourceNodeUuid: '',
  targetNodeUuid: '',
  type: '',
  fact: ''
})

const columns = [
  { title: 'UUID', dataIndex: 'uuid', key: 'uuid', width: 200, ellipsis: true },
  { title: '源节点', key: 'source', width: 120 },
  { title: '目标节点', key: 'target', width: 120 },
  { title: '名称', dataIndex: 'name', key: 'name', width: 120 },
  { title: '事实描述', key: 'fact', ellipsis: true },
  { title: '操作', key: 'action', width: 140 }
]

async function loadEdges() {
  if (!props.graphId) return
  loading.value = true
  try {
    const data = await edgeApi.list(props.graphId)
    edgeList.value = data
  } catch (e: any) {
    message.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function handleRefresh() {
  refreshing.value = true
  await loadEdges()
  refreshing.value = false
  message.success('已刷新')
}

function handleEdit(record: EdgeListItem) {
  editingId.value = record.uuid
  form.sourceNodeUuid = record.sourceNodeUuid
  form.targetNodeUuid = record.targetNodeUuid
  form.type = record.type || ''
  form.fact = record.fact || ''
  showModal.value = true
}

async function handleSave() {
  try {
    if (editingId.value) {
      await edgeApi.update(props.graphId, editingId.value, {
        fact: form.fact,
        name: form.type
      })
      message.success('关系边已更新')
    } else {
      await edgeApi.create(props.graphId, {
        sourceNodeUuid: form.sourceNodeUuid,
        targetNodeUuid: form.targetNodeUuid,
        type: form.type,
        fact: form.fact
      })
      message.success('关系边已创建')
    }
    showModal.value = false
    editingId.value = null
    resetForm()
    loadEdges()
  } catch (e: any) {
    message.error(e.message || '保存失败')
  }
}

async function handleDelete(record: EdgeListItem) {
  try {
    await edgeApi.delete(props.graphId, record.uuid)
    message.success('删除成功')
    loadEdges()
  } catch (e: any) {
    message.error(e.message || '删除失败')
  }
}

function resetForm() {
  form.sourceNodeUuid = ''
  form.targetNodeUuid = ''
  form.type = ''
  form.fact = ''
}

function truncate(str: string, len: number) {
  if (!str) return '-'
  return str.length > len ? str.slice(0, len) + '...' : str
}

// Load on mount
loadEdges()
</script>

<style scoped lang="less">
.edge-list-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  padding: 16px;

  .panel-toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
  }

  .fact-text {
    color: #8b949e;
    font-size: 13px;
  }
}
</style>
