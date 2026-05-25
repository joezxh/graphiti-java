<template>
  <div class="graph-list-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">{{ $t('graph.management') }}</h2>
        <span class="page-description">{{ $t('graph.managementDesc') }}</span>
      </div>
      <a-button type="primary" @click="showCreateModal">
        <template #icon><PlusOutlined /></template>
        {{ $t('graph.createGraph') }}
      </a-button>
    </div>

    <!-- 搜索筛选 -->
    <div class="filter-section">
      <a-input-search
        v-model:value="searchText"
        :placeholder="$t('graph.searchPlaceholder')"
        style="width: 300px;"
        @search="handleSearch"
      />
    </div>

    <!-- 图谱表格 -->
    <div class="table-section">
      <a-table
        :columns="columns"
        :data-source="filteredGraphs"
        :loading="loading"
        row-key="graphId"
        :pagination="{ pageSize: 10, showSizeChanger: true }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <a @click="viewGraphDetail(record)">{{ record.name }}</a>
          </template>

          <template v-if="column.key === 'nodeCount'">
            <a-tag color="blue">{{ record.nodeCount || 0 }}</a-tag>
          </template>

          <template v-if="column.key === 'edgeCount'">
            <a-tag color="green">{{ record.edgeCount || 0 }}</a-tag>
          </template>

          <template v-if="column.key === 'createdAt'">
            {{ formatDate(record.createdAt) }}
          </template>

          <template v-if="column.key === 'action'">
            <a-space>
              <a @click="viewGraphDetail(record)">{{ $t('common.view') }}</a>
              <a-divider type="vertical" />
              <a @click="editGraph(record)">{{ $t('common.edit') }}</a>
              <a-divider type="vertical" />
              <a class="danger-link" @click="handleDeleteClick(record.graphId)">{{ $t('common.delete') }}</a>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>

    <!-- 创建/编辑模态框 -->
    <a-modal
      v-model:open="modalVisible"
      :title="$t(modalTitle)"
      @ok="handleModalOk"
      @cancel="handleModalCancel"
    >
      <a-form
        ref="formRef"
        :model="formState"
        :rules="formRules"
        layout="vertical"
      >
        <a-form-item :label="$t('graph.graphName')" name="name">
          <a-input v-model:value="formState.name" :placeholder="$t('graph.enterGraphName')" />
        </a-form-item>

        <a-form-item :label="$t('graph.graphDesc')" name="description">
          <a-textarea
            v-model:value="formState.description"
            :placeholder="$t('graph.enterGraphName')"
            :rows="4"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 删除确认模态框 -->
    <a-modal
      v-model:open="deleteModalVisible"
      :title="t('graph.deleteConfirmTitle')"
      :confirm-loading="deleteLoading"
      width="720px"
      @ok="handleDeleteConfirm"
      @cancel="deleteModalVisible = false"
      :ok-text="t('common.confirm')"
      :cancel-text="t('common.cancel')"
    >
      <div class="delete-confirm-content" v-if="deletePreview">
        <a-alert
          :message="t('graph.deleteWarning')"
          type="warning"
          show-icon
          style="margin-bottom: 12px;"
        />
        <p class="delete-graph-name">
          <strong>{{ deletePreview.name }}</strong>
        </p>
        <p class="delete-graph-desc" v-if="deletePreview.description">
          {{ deletePreview.description }}
        </p>

        <!-- Neo4j 图谱数据 -->
        <div class="data-section compact">
          <div class="section-title">
            <span>{{ t('graph.neo4jDataSection') }}</span>
            <span class="section-badge" :class="getNeo4jDataCount > 0 ? 'badge-warning' : 'badge-success'">
              {{ t('common.total') }}: {{ getNeo4jDataCount }}
            </span>
          </div>
          <a-descriptions :column="3" bordered size="small">
            <a-descriptions-item :label="t('graph.entityNodes')">
              <span :class="deletePreview.entityNodeCount > 0 ? 'data-count-warning' : ''">
                {{ deletePreview.entityNodeCount }}
              </span>
            </a-descriptions-item>
            <a-descriptions-item :label="t('graph.episodes')">
              <span :class="deletePreview.episodeCount > 0 ? 'data-count-warning' : ''">
                {{ deletePreview.episodeCount }}
              </span>
            </a-descriptions-item>
            <a-descriptions-item :label="t('graph.relationships')">
              <span :class="deletePreview.relationshipCount > 0 ? 'data-count-warning' : ''">
                {{ deletePreview.relationshipCount }}
              </span>
            </a-descriptions-item>
            <a-descriptions-item :label="t('graph.communities')">
              <span :class="deletePreview.communityNodeCount > 0 ? 'data-count-warning' : ''">
                {{ deletePreview.communityNodeCount }}
              </span>
            </a-descriptions-item>
            <a-descriptions-item :label="t('graph.totalNodes')">
              {{ deletePreview.nodeCount }}
            </a-descriptions-item>
            <a-descriptions-item :label="t('graph.totalEdges')">
              {{ deletePreview.edgeCount }}
            </a-descriptions-item>
          </a-descriptions>
        </div>

        <!-- 本体元数据 -->
        <div class="data-section compact">
          <div class="section-title">
            <span>{{ t('graph.ontologySection') }}</span>
            <span class="section-badge" :class="getOntologyDataCount > 0 ? 'badge-warning' : 'badge-success'">
              {{ t('common.total') }}: {{ getOntologyDataCount }}
            </span>
          </div>
          <a-descriptions :column="3" bordered size="small">
            <a-descriptions-item :label="t('graph.ontDefinitions')">
              <span :class="(deletePreview.ontDefinitionCount || 0) > 0 ? 'data-count-warning' : ''">
                {{ deletePreview.ontDefinitionCount || 0 }}
              </span>
            </a-descriptions-item>
            <a-descriptions-item :label="t('graph.ontClasses')">
              <span :class="(deletePreview.ontClassCount || 0) > 0 ? 'data-count-warning' : ''">
                {{ deletePreview.ontClassCount || 0 }}
              </span>
            </a-descriptions-item>
            <a-descriptions-item :label="t('graph.ontProperties')">
              <span :class="(deletePreview.ontPropertyCount || 0) > 0 ? 'data-count-warning' : ''">
                {{ deletePreview.ontPropertyCount || 0 }}
              </span>
            </a-descriptions-item>
            <a-descriptions-item :label="t('graph.ontConstraints')">
              <span :class="(deletePreview.ontConstraintCount || 0) > 0 ? 'data-count-warning' : ''">
                {{ deletePreview.ontConstraintCount || 0 }}
              </span>
            </a-descriptions-item>
            <a-descriptions-item :label="t('graph.ontMappings')">
              {{ deletePreview.ontMappingCount || 0 }}
            </a-descriptions-item>
            <a-descriptions-item :label="t('graph.ontClassInheritance')">
              {{ deletePreview.ontClassInheritanceCount || 0 }}
            </a-descriptions-item>
            <a-descriptions-item :label="t('graph.ontEntityCategories')">
              {{ deletePreview.ontEntityCategoryCount || 0 }}
            </a-descriptions-item>
            <a-descriptions-item :label="t('graph.ontEpisodeTypes')">
              {{ deletePreview.ontEpisodeTypeCount || 0 }}
            </a-descriptions-item>
            <a-descriptions-item :label="t('graph.ontRelationshipMeta')">
              {{ deletePreview.ontRelationshipMetaCount || 0 }}
            </a-descriptions-item>
            <a-descriptions-item :label="t('graph.ontCommunityTypes')">
              {{ deletePreview.ontCommunityTypeCount || 0 }}
            </a-descriptions-item>
            <a-descriptions-item :label="t('graph.ontVersionHistory')">
              {{ deletePreview.ontVersionHistoryCount || 0 }}
            </a-descriptions-item>
            <a-descriptions-item :label="t('graph.ontDrafts')">
              {{ deletePreview.ontDraftCount || 0 }}
            </a-descriptions-item>
          </a-descriptions>
        </div>

        <!-- 汇总警告 -->
        <div class="delete-summary" v-if="deletePreview.totalDataCount > 0">
          <a-alert
            :message="t('graph.deleteTotalWarning', { count: deletePreview.totalDataCount })"
            type="error"
            show-icon
            style="margin-top: 12px;"
          />
        </div>
        <div class="delete-summary" v-else>
          <a-alert
            :message="t('graph.deleteEmptyHint')"
            type="info"
            show-icon
            style="margin-top: 12px;"
          />
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { useI18n } from 'vue-i18n'
import { graphApi, type Graph, type GraphDeletePreview } from '@/api/graph'

const { t } = useI18n()

const router = useRouter()

// 状态
const loading = ref(false)
const searchText = ref('')
const graphs = ref<Graph[]>([])
const modalVisible = ref(false)
const modalTitle = ref('graph.newGraph')
const isEdit = ref(false)
const editingId = ref<string | null>(null)
const formRef = ref()

// 删除确认相关状态
const deleteModalVisible = ref(false)
const deletePreview = ref<GraphDeletePreview | null>(null)
const deleteLoading = ref(false)
const deletingId = ref<string | null>(null)

// 表单状态
const formState = reactive({
  name: '',
  description: ''
})

const formRules = computed(() => {
  return {
    name: [
      { required: true, message: t('graph.enterGraphName') },
      { min: 2, max: 50, message: t('graph.graphNameLength') }
    ]
  }
})

// 表格列定义
const columns = computed(() => [
  {
    title: t('graph.graphName'),
    key: 'name',
    dataIndex: 'name',
    width: '25%'
  },
  {
    title: t('common.description'),
    key: 'description',
    dataIndex: 'description',
    width: '35%',
    ellipsis: true
  },
  {
    title: t('graph.nodeCount'),
    key: 'nodeCount',
    width: '10%',
    align: 'center'
  },
  {
    title: t('graph.edgeCount'),
    key: 'edgeCount',
    width: '10%',
    align: 'center'
  },
  {
    title: t('graph.createTime'),
    key: 'createdAt',
    width: '15%'
  },
  {
    title: t('common.action'),
    key: 'action',
    width: '15%'
  }
])

// 过滤后的图谱列表
const filteredGraphs = computed(() => {
  if (!searchText.value) return graphs.value
  const keyword = searchText.value.toLowerCase()
  return graphs.value.filter(g =>
    g.name.toLowerCase().includes(keyword) ||
    (g.description && g.description.toLowerCase().includes(keyword))
  )
})

// 计算 Neo4j 图谱数据总数
const getNeo4jDataCount = computed(() => {
  if (!deletePreview.value) return 0
  const p = deletePreview.value
  return (p.entityNodeCount || 0) + (p.episodeCount || 0) + (p.relationshipCount || 0) + (p.communityNodeCount || 0)
})

// 计算本体元数据总数
const getOntologyDataCount = computed(() => {
  if (!deletePreview.value) return 0
  const p = deletePreview.value
  return (p.ontDefinitionCount || 0) + (p.ontClassCount || 0) + (p.ontPropertyCount || 0) +
    (p.ontConstraintCount || 0) + (p.ontMappingCount || 0) + (p.ontClassInheritanceCount || 0) +
    (p.ontVersionHistoryCount || 0) + (p.ontEntityCategoryCount || 0) + (p.ontEpisodeTypeCount || 0) +
    (p.ontRelationshipMetaCount || 0) + (p.ontCommunityTypeCount || 0) + (p.ontDraftCount || 0)
})

// 加载图谱列表
const loadGraphs = async () => {
  loading.value = true
  try {
    const res = await graphApi.getList()
    graphs.value = res || []
  } catch (error) {
    message.error(t('graph.loadFailed'))
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  // 前端搜索，无需重新加载
}

// 显示创建模态框
const showCreateModal = () => {
  modalTitle.value = 'graph.newGraph'
  isEdit.value = false
  editingId.value = null
  formState.name = ''
  formState.description = ''
  modalVisible.value = true
}

// 查看图谱详情
const viewGraphDetail = (record: Graph) => {
  router.push(`/graph/ide/${record.graphId}`)
}

// 编辑图谱
const editGraph = (record: Graph) => {
  modalTitle.value = 'graph.editGraph'
  isEdit.value = true
  editingId.value = record.graphId
  formState.name = record.name
  formState.description = record.description || ''
  modalVisible.value = true
}

// 删除图谱 - 第一步：获取预览并展示确认框
const handleDeleteClick = async (id: string) => {
  deletingId.value = id
  try {
    deletePreview.value = await graphApi.getDeletePreview(id)
    deleteModalVisible.value = true
  } catch (error) {
    message.error(t('graph.deletePreviewFailed'))
  }
}

// 删除图谱 - 第二步：确认后执行删除
const handleDeleteConfirm = async () => {
  if (!deletingId.value) return
  deleteLoading.value = true
  try {
    await graphApi.delete(deletingId.value)
    message.success(t('graph.deleteSuccess'))
    deleteModalVisible.value = false
    deletePreview.value = null
    deletingId.value = null
    loadGraphs()
  } catch (error) {
    message.error(t('graph.deleteFailed'))
  } finally {
    deleteLoading.value = false
  }
}

// 模态框确认
const handleModalOk = async () => {
  try {
    await formRef.value.validate()

    if (isEdit.value && editingId.value) {
      await graphApi.update(editingId.value, formState)
      message.success(t('graph.updateSuccess'))
    } else {
      await graphApi.create(formState)
      message.success(t('graph.createSuccess'))
    }

    modalVisible.value = false
    loadGraphs()
  } catch (error) {
    // 表单验证失败
  }
}

// 模态框取消
const handleModalCancel = () => {
  modalVisible.value = false
  formRef.value?.resetFields()
}

// 格式化日期
const formatDate = (date: string): string => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

onMounted(() => {
  loadGraphs()
})
</script>

<style scoped lang="less">
.graph-list-container {
  padding: 20px;
  background: #010102;
  min-height: calc(100vh - 56px);
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
  
  .header-left {
    .page-title {
      font-size: 20px;
      font-weight: 600;
      color: #f7f8f8;
      margin-bottom: 4px;
    }
    
    .page-description {
      font-size: 14px;
      color: #8a8f98;
    }
  }
}

.filter-section {
  margin-bottom: 16px;
  
  :deep(.ant-input-search) {
    .ant-input {
      background: rgba(15, 16, 17, 0.8);
      border-color: #23252a;
      color: #f7f8f8;
      
      &:hover, &:focus {
        border-color: #5e6ad2;
      }
    }
    
    .ant-input-search-button {
      background: rgba(94, 106, 210, 0.2);
      border-color: #23252a;
      color: #f7f8f8;
      
      &:hover {
        background: rgba(94, 106, 210, 0.3);
      }
    }
  }
}

.table-section {
  background: rgba(15, 16, 17, 0.8);
  border: 1px solid #23252a;
  border-radius: 8px;
  padding: 16px;
  
  :deep(.ant-table) {
    a {
      color: #5e6ad2;
      
      &:hover {
        color: #7b7ff0;
      }
    }
    
    .danger-link {
      color: #ff6b6b;
      
      &:hover {
        color: #ff8a8a;
      }
    }
  }
  
  :deep(.ant-pagination) {
    .ant-pagination-item {
      background: rgba(15, 16, 17, 0.8);
      border-color: #23252a;
      
      a {
        color: #f7f8f8;
      }
      
      &:hover, &.ant-pagination-item-active {
        border-color: #5e6ad2;
        
        a {
          color: #5e6ad2;
        }
      }
    }
    
      .ant-pagination-prev, .ant-pagination-next {
      .ant-pagination-item-link {
        background: rgba(15, 16, 17, 0.8);
        border-color: #23252a;
        color: #f7f8f8;
      }
    }
  }
}

.delete-confirm-content {
  .delete-graph-name {
    font-size: 16px;
    margin-bottom: 4px;
    color: #000000;
  }

  .delete-graph-desc {
    font-size: 13px;
    color: #000000;
    margin-bottom: 12px;
  }

  .delete-summary {
    margin-top: 8px;
  }

  .delete-hint {
    margin-top: 12px;
    font-size: 13px;
    color: #8a8f98;
  }

  .data-count-warning {
    color: #ff6b6b;
    font-weight: 600;
  }

  .data-section.compact {
    margin-bottom: 10px;

    .section-title {
      display: flex;
      align-items: center;
      justify-content: space-between;
      font-size: 13px;
      font-weight: 600;
      color: #000000;
      margin-bottom: 6px;
      padding-bottom: 4px;
      border-bottom: 1px solid #23252a;
    }

    .section-badge {
      font-size: 12px;
      font-weight: 500;
      padding: 2px 8px;
      border-radius: 10px;
    }

    .badge-warning {
      background: rgba(255, 107, 107, 0.15);
      color: #ff6b6b;
    }

    .badge-success {
      background: rgba(63, 185, 80, 0.15);
      color: #3fb950;
    }
  }

  :deep(.ant-descriptions) {
    background: rgba(15, 16, 17, 0.6);
    border-color: #23252a;
    color: #000000;

    .ant-descriptions-item-label {
      background: rgba(15, 16, 17, 0.4);
      border-color: #23252a;
      color: #000000;
    }

    .ant-descriptions-item-content {
      background: rgba(15, 16, 17, 0.4);
      border-color: #23252a;
      color: #000000;
    }
  }

  :deep(.ant-alert-warning) {
    background: rgba(255, 183, 77, 0.1);
    border-color: rgba(255, 183, 77, 0.3);

    .ant-alert-message {
      color: #ffb74d;
    }
  }

  :deep(.ant-alert-error) {
    background: rgba(255, 107, 107, 0.1);
    border-color: rgba(255, 107, 107, 0.3);

    .ant-alert-message {
      color: #ff8a8a;
    }
  }

  :deep(.ant-alert-info) {
    background: rgba(94, 106, 210, 0.1);
    border-color: rgba(94, 106, 210, 0.3);

    .ant-alert-message {
      color: #7b7ff0;
    }
  }
}
</style>
