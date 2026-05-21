<template>
  <div class="property-list-panel">
    <div class="panel-toolbar">
      <a-space>
        <a-button type="primary" @click="handleNew">
          <template #icon><PlusOutlined /></template>
          新建属性
        </a-button>
        <a-button :loading="refreshing" @click="handleRefresh">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </a-space>
      <a-input-search v-model:value="keyword" placeholder="搜索属性..." style="width: 200px" />
    </div>

    <a-table
      :columns="columns"
      :data-source="filteredProperties"
      :loading="store.loading"
      :pagination="{ pageSize: 20 }"
      row-key="id"
      @row-click="handleRowClick"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'propertyType'">
          <a-tag :color="typeColor(record.propertyType)">{{ record.propertyType }}</a-tag>
        </template>
        <template v-if="column.key === 'isRequired'">
          <a-tag :color="record.isRequired ? 'red' : 'default'">{{ record.isRequired ? '是' : '否' }}</a-tag>
        </template>
        <template v-if="column.key === 'domain'">
          {{ getClassName(record.domainClassId) }}
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a-button type="link" size="small" @click.stop="handleEdit(record)">编辑</a-button>
            <a-popconfirm title="确定删除？" ok-text="确定" @confirm="handleDelete(record)">
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

const props = defineProps<{ graphId: string }>()
const emit = defineEmits<{ (e: 'open-property', propertyId: number, name: string): void }>()
const store = useOntologyStore()
const keyword = ref('')
const refreshing = ref(false)

const columns = [
  { title: '名称', dataIndex: 'localName', key: 'localName' },
  { title: '类型', key: 'propertyType', width: 120 },
  { title: '定义域', key: 'domain', width: 120 },
  { title: '数据类型', dataIndex: 'rangeDataType', key: 'rangeDataType' },
  { title: '必填', key: 'isRequired', width: 70 },
  { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
  { title: '操作', key: 'action', width: 140 }
]

const filteredProperties = computed(() => {
  if (!keyword.value) return store.properties
  const kw = keyword.value.toLowerCase()
  return store.properties.filter(p => p.localName.toLowerCase().includes(kw) || p.description?.toLowerCase().includes(kw))
})

function getClassName(classId?: number) {
  if (!classId) return '-'
  return store.classes.find(c => c.id === classId)?.localName ?? '-'
}

function typeColor(type: string) {
  return { DATATYPE: 'blue', OBJECT: 'green', ANNOTATION: 'purple', TRANSITIVE: 'orange', SYMMETRIC: 'cyan', FUNCTIONAL: 'magenta' }[type] ?? 'default'
}

function handleRowClick(record: any) { handleEdit(record) }
function handleEdit(record: any) { emit('open-property', record.id, record.localName) }
function handleNew() { emit('open-property', 0 as any, '新建属性') }

async function handleDelete(record: any) {
  try {
    await ontologyApi.deleteProperty(props.graphId, record.id)
    message.success('删除成功')
    await store.loadFullOntology(props.graphId)
  } catch (e: any) { message.error(e.message || '删除失败') }
}

async function handleRefresh() {
  refreshing.value = true
  await store.loadFullOntology(props.graphId)
  refreshing.value = false
  message.success('已刷新')
}
</script>

<style scoped lang="less">
.property-list-panel {
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
}
</style>
