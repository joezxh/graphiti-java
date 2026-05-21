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
              <a-popconfirm
                :title="$t('graph.confirmDelete')"
                :ok-text="$t('common.confirm')"
                :cancel-text="$t('common.cancel')"
                @confirm="deleteGraph(record.graphId)"
              >
                <a class="danger-link">{{ $t('common.delete') }}</a>
              </a-popconfirm>
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'
import { useI18n } from 'vue-i18n'
import { graphApi, type Graph } from '@/api/graph'

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
  router.push(`/graph/detail/${record.graphId}`)
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

// 删除图谱
const deleteGraph = async (id: string) => {
  try {
    await graphApi.delete(id)
      message.success(t('graph.deleteSuccess'))
    loadGraphs()
  } catch (error) {
    message.error(t('graph.deleteFailed'))
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
</style>
