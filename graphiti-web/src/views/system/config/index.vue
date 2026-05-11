<template>
  <div class="config-management">
    <a-card class="page-header" :bordered="false">
      <div class="header-content">
        <div class="header-left">
          <h2 class="page-title">系统配置</h2>
          <p class="page-description">管理系统配置参数</p>
        </div>
        <div class="header-actions">
          <a-button type="primary" @click="handleCreate">
            <template #icon><PlusOutlined /></template>
            新建配置
          </a-button>
        </div>
      </div>
    </a-card>

    <a-card class="content-card" :bordered="false">
      <!-- 搜索表单 -->
      <div class="table-operations">
        <a-form layout="inline" :model="queryParams" class="search-form">
          <a-form-item label="配置键">
            <a-input
              v-model:value="queryParams.configKey"
              placeholder="请输入配置键"
              allow-clear
              style="width: 160px"
            />
          </a-form-item>
          <a-form-item label="配置名称">
            <a-input
              v-model:value="queryParams.configName"
              placeholder="请输入配置名称"
              allow-clear
              style="width: 160px"
            />
          </a-form-item>
          <a-form-item label="分组">
            <a-select
              v-model:value="queryParams.groupName"
              placeholder="请选择分组"
              allow-clear
              style="width: 150px"
            >
              <a-select-option v-for="group in groups" :key="group" :value="group">
                {{ group }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="状态">
            <a-select
              v-model:value="queryParams.status"
              placeholder="请选择状态"
              allow-clear
              style="width: 120px"
            >
              <a-select-option :value="1">启用</a-select-option>
              <a-select-option :value="0">禁用</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item>
            <a-space>
              <a-button type="primary" @click="handleQuery">查询</a-button>
              <a-button @click="handleReset">重置</a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </div>

      <!-- 数据表格 -->
      <a-table
        :columns="columns"
        :data-source="configList"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'configType'">
            <a-tag :color="getConfigTypeColor(record.configType)">
              {{ getConfigTypeText(record.configType) }}
            </a-tag>
          </template>
          
          <template v-if="column.dataIndex === 'status'">
            <a-badge :status="record.status === 1 ? 'success' : 'error'" />
            <span :style="{ color: record.status === 1 ? '#52c41a' : '#ff4d4f' }">
              {{ record.status === 1 ? '启用' : '禁用' }}
            </span>
          </template>
          
          <template v-if="column.dataIndex === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleEdit(record)">
                <template #icon><EditOutlined /></template>
                编辑
              </a-button>
              <a-popconfirm
                title="确定要删除此配置吗？"
                ok-text="确定"
                cancel-text="取消"
                @confirm="handleDelete(record.id)"
              >
                <a-button type="link" size="small" danger>
                  <template #icon><DeleteOutlined /></template>
                  删除
                </a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 配置表单对话框 -->
    <a-modal
      v-model:visible="modalVisible"
      :title="modalTitle"
      :confirm-loading="submitLoading"
      width="600px"
      @ok="handleSubmit"
      @cancel="handleCancel"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="配置键" name="configKey">
          <a-input v-model:value="formData.configKey" placeholder="请输入配置键" :disabled="isEdit" />
        </a-form-item>
        
        <a-form-item label="配置值" name="configValue">
          <a-textarea v-if="formData.configType === 4" v-model:value="formData.configValue" placeholder="请输入配置值（JSON格式）" :rows="5" />
          <a-input-number v-else-if="formData.configType === 2" v-model:value="formData.configValue" placeholder="请输入配置值" style="width: 100%" />
          <a-switch v-else-if="formData.configType === 3" v-model:checked="configValueBoolean" checked-children="启用" un-checked-children="禁用" />
          <a-input v-else v-model:value="formData.configValue" placeholder="请输入配置值" />
        </a-form-item>
        
        <a-form-item label="配置名称" name="configName">
          <a-input v-model:value="formData.configName" placeholder="请输入配置名称" />
        </a-form-item>
        
        <a-form-item label="配置描述" name="configDescription">
          <a-textarea v-model:value="formData.configDescription" placeholder="请输入配置描述" :rows="3" />
        </a-form-item>
        
        <a-form-item label="配置类型" name="configType">
          <a-select v-model:value="formData.configType" placeholder="请选择配置类型">
            <a-select-option :value="1">文本</a-select-option>
            <a-select-option :value="2">数字</a-select-option>
            <a-select-option :value="3">布尔</a-select-option>
            <a-select-option :value="4">JSON</a-select-option>
          </a-select>
        </a-form-item>
        
        <a-form-item label="分组" name="groupName">
          <a-input v-model:value="formData.groupName" placeholder="请输入分组名称" />
        </a-form-item>
        
        <a-form-item label="排序" name="sort">
          <a-input-number v-model:value="formData.sortNum" :min="0" style="width: 100%" />
        </a-form-item>
        
        <a-form-item label="状态" name="status">
          <a-radio-group v-model:value="formData.status">
            <a-radio :value="1">启用</a-radio>
            <a-radio :value="0">禁用</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined
} from '@ant-design/icons-vue'
import { systemApi, type SystemConfig, type SystemConfigQuery, type SystemConfigForm } from '@/api/system'

// 查询参数
const queryParams = reactive<SystemConfigQuery>({
  configKey: undefined,
  configName: undefined,
  groupName: undefined,
  status: undefined,
  pageNum: 1,
  pageSize: 10
})

// 表格列定义
const columns = [
  {
    title: 'ID',
    dataIndex: 'id',
    width: 60
  },
  {
    title: '配置键',
    dataIndex: 'configKey',
    width: 150
  },
  {
    title: '配置值',
    dataIndex: 'configValue',
    width: 150,
    ellipsis: true
  },
  {
    title: '配置名称',
    dataIndex: 'configName',
    width: 120
  },
  {
    title: '配置描述',
    dataIndex: 'configDescription',
    width: 200,
    ellipsis: true
  },
  {
    title: '配置类型',
    dataIndex: 'configType',
    width: 100
  },
  {
    title: '分组',
    dataIndex: 'groupName',
    width: 120
  },
  {
    title: '状态',
    dataIndex: 'status',
    width: 100
  },
  {
    title: '操作',
    dataIndex: 'action',
    width: 150,
    fixed: 'right'
  }
]

// 数据列表
const configList = ref<SystemConfig[]>([])
const loading = ref(false)
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true
})

// 分组列表
const groups = ref<string[]>([])

// 模态框状态
const modalVisible = ref(false)
const modalTitle = ref('新建配置')
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref()

// 表单数据
const formData = reactive<SystemConfigForm>({
  configKey: '',
  configValue: '',
  configName: '',
  configDescription: '',
  configType: 1,
  groupName: '',
  sortNum: 0,
  status: 1
})

// 布尔值配置的处理
const configValueBoolean = computed({
  get: () => formData.configValue === 'true',
  set: (val: boolean) => {
    formData.configValue = val ? 'true' : 'false'
  }
})

// 表单校验规则
const formRules = {
  configKey: [
    { required: true, message: '请输入配置键', trigger: 'blur' }
  ],
  configValue: [
    { required: true, message: '请输入配置值', trigger: 'blur' }
  ],
  configName: [
    { required: true, message: '请输入配置名称', trigger: 'blur' }
  ],
  configType: [
    { required: true, message: '请选择配置类型', trigger: 'change' }
  ],
  groupName: [
    { required: true, message: '请输入分组名称', trigger: 'blur' }
  ]
}

// 获取配置列表
const fetchConfigs = async () => {
  loading.value = true
  try {
    const res = await systemApi.getConfigs(queryParams)
    configList.value = res.list
    pagination.current = res.pageNum
    pagination.pageSize = res.pageSize
    pagination.total = res.total
  } catch (error) {
    message.error('获取配置列表失败')
  } finally {
    loading.value = false
  }
}

// 获取分组列表
const fetchGroups = async () => {
  try {
    const res = await systemApi.getGroups()
    groups.value = res
  } catch (error) {
    console.error('获取分组列表失败', error)
  }
}

// 获取配置类型颜色
const getConfigTypeColor = (type: number) => {
  switch (type) {
    case 1: return 'blue'
    case 2: return 'green'
    case 3: return 'orange'
    case 4: return 'purple'
    default: return 'default'
  }
}

// 获取配置类型文本
const getConfigTypeText = (type: number) => {
  switch (type) {
    case 1: return '文本'
    case 2: return '数字'
    case 3: return '布尔'
    case 4: return 'JSON'
    default: return '未知'
  }
}

// 查询
const handleQuery = () => {
  queryParams.pageNum = 1
  fetchConfigs()
}

// 重置
const handleReset = () => {
  queryParams.configKey = undefined
  queryParams.configName = undefined
  queryParams.groupName = undefined
  queryParams.status = undefined
  queryParams.pageNum = 1
  fetchConfigs()
}

// 表格变化
const handleTableChange = (pag: any) => {
  queryParams.pageNum = pag.current
  queryParams.pageSize = pag.pageSize
  fetchConfigs()
}

// 新建配置
const handleCreate = () => {
  isEdit.value = false
  modalTitle.value = '新建配置'
  resetForm()
  modalVisible.value = true
}

// 编辑配置
const handleEdit = async (record: SystemConfig) => {
  isEdit.value = true
  modalTitle.value = '编辑配置'
  resetForm()
  
  try {
    const config = await systemApi.getConfig(record.id)
    formData.configKey = config.configKey
    formData.configValue = config.configValue
    formData.configName = config.configName
    formData.configDescription = config.configDescription
    formData.configType = config.configType
    formData.groupName = config.groupName
    formData.sortNum = config.sortNum
    formData.status = config.status
    
    modalVisible.value = true
  } catch (error) {
    message.error('获取配置详情失败')
  }
}

// 删除配置
const handleDelete = async (id: number) => {
  try {
    await systemApi.deleteConfig(id)
    message.success('删除成功')
    fetchConfigs()
  } catch (error) {
    message.error('删除失败')
  }
}

// 提交表单
const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    
    submitLoading.value = true
    
    if (isEdit.value) {
      await systemApi.updateConfig(formData.id!, {
        configKey: formData.configKey,
        configValue: formData.configValue,
        configName: formData.configName,
        configDescription: formData.configDescription,
        configType: formData.configType,
        groupName: formData.groupName,
        sortNum: formData.sortNum,
        status: formData.status
      })
      message.success('更新成功')
    } else {
      await systemApi.createConfig(formData)
      message.success('创建成功')
    }
    
    modalVisible.value = false
    fetchConfigs()
  } catch (error) {
    console.error('提交失败', error)
  } finally {
    submitLoading.value = false
  }
}

// 取消表单
const handleCancel = () => {
  modalVisible.value = false
  resetForm()
}

// 重置表单
const resetForm = () => {
  formData.id = undefined
  formData.configKey = ''
  formData.configValue = ''
  formData.configName = ''
  formData.configDescription = ''
  formData.configType = 1
  formData.groupName = ''
  formData.sortNum = 0
  formData.status = 1
  
  if (formRef.value) {
    formRef.value.resetFields()
  }
}

onMounted(() => {
  fetchConfigs()
  fetchGroups()
})
</script>

<style scoped lang="less">
.config-management {
  .page-header {
    margin-bottom: 16px;
    
    .header-content {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    
    .page-title {
      font-size: 20px;
      font-weight: 600;
      color: #f7f8f8;
      margin: 0 0 4px 0;
    }
    
    .page-description {
      font-size: 14px;
      color: #8a8f98;
      margin: 0;
    }
  }
  
  .content-card {
    .table-operations {
      margin-bottom: 16px;
      
      .search-form {
        .ant-form-item {
          margin-bottom: 16px;
        }
      }
    }
  }
}
</style>
