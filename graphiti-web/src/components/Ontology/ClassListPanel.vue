<template>
  <div class="class-list-panel">
    <div class="panel-toolbar">
      <a-space>
        <a-button type="primary" @click="handleNew">
          <template #icon><PlusOutlined /></template>
          {{ t('ontology.addClass') }}
        </a-button>
        <a-button :loading="refreshing" @click="handleRefresh">
          <template #icon><ReloadOutlined /></template>
          {{ t('common.refresh') }}
        </a-button>
      </a-space>
      <a-input-search
        v-model:value="keyword"
        :placeholder="t('ontology.searchClass')"
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
            <a-button type="link" size="small" @click.stop="handleEdit(record)">{{ t('common.edit') }}</a-button>
            <a-popconfirm :title="t('common.confirmDelete')" :ok-text="t('common.confirm')" :cancel-text="t('common.cancel')" @confirm="handleDelete(record)">
              <a-button type="link" size="small" danger>{{ t('common.delete') }}</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { useOntologyStore } from '@/store/modules/ontology'
import { ontologyApi } from '@/api/ontology'
import type { OntClassVO } from '@/api/ontology'

const { t } = useI18n()

const props = defineProps<{ graphId: string }>()
const emit = defineEmits<{ (e: 'open-class', classId: number, className: string): void }>()
const store = useOntologyStore()
const keyword = ref('')
const refreshing = ref(false)

const columns = computed(() => [
  { title: t('common.name'), dataIndex: 'localName', key: 'localName' },
  { title: 'URI', dataIndex: 'classUri', key: 'classUri', ellipsis: true },
  { title: t('ontology.domainClassification'), key: 'domainHint' },
  { title: t('graphIde.labelPropertyCount'), key: 'propertyCount', width: 80 },
  { title: t('common.description'), dataIndex: 'description', key: 'description', ellipsis: true },
  { title: t('common.action'), key: 'action', width: 140 }
])

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
  emit('open-class', 0 as any, t('ontology.newClass'))
}

async function handleDelete(cls: OntClassVO) {
  try {
    await ontologyApi.deleteClass(props.graphId, cls.id)
    message.success(t('common.deleteSuccess'))
    await store.loadFullOntology(props.graphId)
  } catch (e: any) {
    message.error(e.message || t('common.deleteFailed'))
  }
}

async function handleRefresh() {
  refreshing.value = true
  await store.loadFullOntology(props.graphId)
  refreshing.value = false
  message.success(t('common.refreshSuccess'))
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
