<template>
  <div class="graph-list-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">图谱管理</h2>
        <span class="page-description">管理和查看所有知识图谱</span>
      </div>
      <a-button type="primary" @click="showCreateModal">
        <template #icon><PlusOutlined /></template>
        新建图谱
      </a-button>
    </div>
    
    <!-- 搜索筛选 -->
    <div class="filter-section">
      <a-input-search
        v-model:value="searchText"
        placeholder="搜索图谱名称..."
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
        row-key="id"
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
              <a @click="viewGraphDetail(record)">查看</a>
              <a-divider type="vertical" />
              <a @click="editGraph(record)">编辑</a>
              <a-divider type="vertical" />
              <a-popconfirm
                title="确定要删除此图谱吗？"
                ok-text="确定"
                cancel-text="取消"
                @confirm="deleteGraph(record.graphId)"
              >
                <a class="danger-link">删除</a>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </div>
    
    <!-- 创建/编辑模态框 -->
    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      @ok="handleModalOk"
      @cancel="handleModalCancel"
    >
      <a-form
        ref="formRef"
        :model="formState"
        :rules="formRules"
        layout="vertical"
      >
        <a-form-item label="图谱名称" name="name">
          <a-input v-model:value="formState.name" placeholder="请输入图谱名称" />
        </a-form-item>
        
        <a-form-item label="图谱描述" name="description">
          <a-textarea
            v-model:value="formState.description"
            placeholder="请输入图谱描述"
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
import { graphApi, type Graph } from '@/api/graph'

const router = useRouter()

// 状态
const loading = ref(false)
const searchText = ref('')
const graphs = ref<Graph[]>([])
const modalVisible = ref(false)
const modalTitle = ref('新建图谱')
const isEdit = ref(false)
const editingId = ref<string | null>(null)
const formRef = ref()

// 表单状态
const formState = reactive({
  name: '',
  description: ''
})

const formRules = {
  name: [
    { required: true, message: '请输入图谱名称' },
    { min: 2, max: 50, message: '图谱名称长度为 2-50 个字符' }
  ]
}

// 表格列定义
const columns = [
  {
    title: '图谱名称',
    key: 'name',
    dataIndex: 'name',
    width: '25%'
  },
  {
    title: '描述',
    key: 'description',
    dataIndex: 'description',
    width: '35%',
    ellipsis: true
  },
  {
    title: '节点数',
    key: 'nodeCount',
    width: '10%',
    align: 'center'
  },
  {
    title: '边数',
    key: 'edgeCount',
    width: '10%',
    align: 'center'
  },
  {
    title: '创建时间',
    key: 'createdAt',
    width: '15%'
  },
  {
    title: '操作',
    key: 'action',
    width: '15%'
  }
]

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
    message.error('加载图谱列表失败')
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
  modalTitle.value = '新建图谱'
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
  modalTitle.value = '编辑图谱'
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
    message.success('删除成功')
    loadGraphs()
  } catch (error) {
    message.error('删除失败')
  }
}

// 模态框确认
const handleModalOk = async () => {
  try {
    await formRef.value.validate()
    
    if (isEdit.value && editingId.value) {
      await graphApi.update(editingId.value, formState)
      message.success('更新成功')
    } else {
      await graphApi.create(formState)
      message.success('创建成功')
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
