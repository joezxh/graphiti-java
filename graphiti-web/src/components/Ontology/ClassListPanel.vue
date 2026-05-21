<template>
  <div class="class-list-panel">
    <div class="panel-toolbar">
      <a-space>
        <a-button type="primary" @click="handleNew">
          <template #icon><PlusOutlined /></template>
          新建类
        </a-button>
        <a-button :loading="refreshing" @click="handleRefresh">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </a-space>
      <a-input-search
        v-model:value="keyword"
        placeholder="搜索类..."
        style="width: 200px"
        @search="handleSearch"
      />
    </div>

    <a-table
      :columns="columns"
      :data-source="filteredClasses"
      :loading="store.loading"
      :pagination="{ pageSize: 20 }"
      row-key="id"
      @row-click="handleRowClick"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'domainHint'">
          <a-tag>{{ record.domainHint || '-' }}</a-tag>
        </template>
        <template v-if="column.key === 'propertyCount'">
          <span class="prop-count">{{ getPropertyCount(record.id) }}</span>
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a-button type="link" size="small" @click.stop="handleEdit(record)">编辑</a-button>
            <a-popconfirm title="确定删除？" ok-text="确定" cancel-text="取消" @confirm="handleDelete(record)">
              <a-button type="link" size="small" danger>删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { useOntologyStore } from '@/store/modules/ontology'
import { ontologyApi } from '@/api/ontology'
import type { OntClassVO } from '@/api/ontology'

const props = defineProps<{ graphId: string }>()
const emit = defineEmits<{ (e: 'open-class', classId: number, className: string): void }>()
const store = useOntologyStore()
const keyword = ref('')
const refreshing = ref(false)

const columns = [
  { title: '名称', dataIndex: 'localName', key: 'localName' },
  { title: 'URI', dataIndex: 'classUri', key: 'classUri', ellipsis: true },
  { title: '领域', key: 'domainHint' },
  { title: '属性数', key: 'propertyCount', width: 80 },
  { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
  { title: '操作', key: 'action', width: 140 }
]

const filteredClasses = computed(() => {
  if (!keyword.value) return store.classes
  const kw = keyword.value.toLowerCase()
  return store.classes.filter(c =>
    c.localName.toLowerCase().includes(kw) ||
    c.classUri?.toLowerCase().includes(kw) ||
    c.description?.toLowerCase().includes(kw)
  )
})

function getPropertyCount(classId: number) {
  return store.properties.filter(p => p.domainClassId === classId).length
}

function handleRowClick(record: OntClassVO) {
  handleEdit(record)
}

function handleEdit(cls: OntClassVO) {
  emit('open-class', cls.id, cls.localName)
}

function handleNew() {
  emit('open-class', 0 as any, '新建类')
}

async function handleDelete(cls: OntClassVO) {
  try {
    await ontologyApi.deleteClass(props.graphId, cls.id)
    message.success('删除成功')
    await store.loadFullOntology(props.graphId)
  } catch (e: any) {
    message.error(e.message || '删除失败')
  }
}

async function handleRefresh() {
  refreshing.value = true
  await store.loadFullOntology(props.graphId)
  refreshing.value = false
  message.success('已刷新')
}

function handleSearch() { /* filtered by computed */ }
</script>

<style scoped lang="less">
.class-list-panel {
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

  .prop-count {
    font-family: monospace;
    font-size: 12px;
    color: #8b949e;
  }
}
</style>
