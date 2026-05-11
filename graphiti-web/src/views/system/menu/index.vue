<template>
  <div class="menu-management">
    <a-card class="page-header" :bordered="false">
      <div class="header-content">
        <div class="header-left">
          <h2 class="page-title">菜单管理</h2>
          <p class="page-description">管理系统菜单，配置菜单结构</p>
        </div>
        <div class="header-actions">
          <a-button type="primary" @click="handleCreate(null)">
            <template #icon><PlusOutlined /></template>
            新建菜单
          </a-button>
        </div>
      </div>
    </a-card>

    <a-card class="content-card" :bordered="false">
      <!-- 搜索表单 -->
      <div class="table-operations">
        <a-form layout="inline" :model="queryParams" class="search-form">
          <a-form-item label="菜单名称">
            <a-input
              v-model:value="queryParams.name"
              placeholder="请输入菜单名称"
              allow-clear
              style="width: 160px"
            />
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
        :data-source="menuList"
        :loading="loading"
        :pagination="false"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'type'">
            <a-tag :color="getTypeColor(record.type)">
              {{ getTypeText(record.type) }}
            </a-tag>
          </template>
          
          <template v-if="column.dataIndex === 'status'">
            <a-badge :status="record.status === 1 ? 'success' : 'error'" />
            <span :style="{ color: record.status === 1 ? '#52c41a' : '#ff4d4f' }">
              {{ record.status === 1 ? '启用' : '禁用' }}
            </span>
          </template>
          
          <template v-if="column.dataIndex === 'icon'">
            <component :is="record.icon" v-if="record.icon" />
            <span v-else>-</span>
          </template>
          
          <template v-if="column.dataIndex === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleCreate(record.id)">
                <template #icon><PlusOutlined /></template>
                添加子菜单
              </a-button>
              <a-button type="link" size="small" @click="handleEdit(record)">
                <template #icon><EditOutlined /></template>
                编辑
              </a-button>
              <a-popconfirm
                title="确定要删除此菜单吗？"
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

    <!-- 菜单表单对话框 -->
    <a-modal
      v-model:open="modalVisible"
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
        <a-form-item label="上级菜单" name="parentId">
          <a-tree-select
            v-model:value="formData.parentId"
            :tree-data="menuTreeData"
            :field-names="{ label: 'name', value: 'id', children: 'children' }"
            placeholder="请选择上级菜单"
            allow-clear
            tree-default-expand-all
          />
        </a-form-item>
        
        <a-form-item label="菜单名称" name="name">
          <a-input v-model:value="formData.name" placeholder="请输入菜单名称" />
        </a-form-item>
        
        <a-form-item label="菜单编码" name="code">
          <a-input v-model:value="formData.code" placeholder="请输入菜单编码" />
        </a-form-item>
        
        <a-form-item label="菜单类型" name="type">
          <a-radio-group v-model:value="formData.type">
            <a-radio :value="1">目录</a-radio>
            <a-radio :value="2">菜单</a-radio>
            <a-radio :value="3">按钮</a-radio>
          </a-radio-group>
        </a-form-item>
        
        <a-form-item label="菜单图标" name="icon" v-if="formData.type !== 3">
          <a-input v-model:value="formData.icon" placeholder="请输入图标名称" />
        </a-form-item>
        
        <a-form-item label="菜单路径" name="path" v-if="formData.type === 2">
          <a-input v-model:value="formData.path" placeholder="请输入菜单路径" />
        </a-form-item>
        
        <a-form-item label="组件路径" name="component" v-if="formData.type === 2">
          <a-input v-model:value="formData.component" placeholder="请输入组件路径" />
        </a-form-item>
        
        <a-form-item label="权限标识" name="permission">
          <a-input v-model:value="formData.permission" placeholder="请输入权限标识" />
        </a-form-item>
        
        <a-form-item label="排序" name="sort">
          <a-input-number v-model:value="formData.sort" :min="0" style="width: 100%" />
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
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined
} from '@ant-design/icons-vue'
import { menuApi, type MenuItem, type MenuQuery, type MenuForm } from '@/api/menu'

// 查询参数
const queryParams = reactive<MenuQuery>({
  name: undefined,
  status: undefined
})

// 表格列定义
const columns = [
  {
    title: 'ID',
    dataIndex: 'id',
    width: 60
  },
  {
    title: '菜单名称',
    dataIndex: 'name',
    width: 150
  },
  {
    title: '菜单编码',
    dataIndex: 'code',
    width: 150
  },
  {
    title: '类型',
    dataIndex: 'type',
    width: 100
  },
  {
    title: '图标',
    dataIndex: 'icon',
    width: 80
  },
  {
    title: '路径',
    dataIndex: 'path',
    width: 150
  },
  {
    title: '权限标识',
    dataIndex: 'permission',
    width: 150
  },
  {
    title: '排序',
    dataIndex: 'sort',
    width: 80
  },
  {
    title: '状态',
    dataIndex: 'status',
    width: 100
  },
  {
    title: '操作',
    dataIndex: 'action',
    width: 250,
    fixed: 'right'
  }
]

// 数据列表
const menuList = ref<MenuItem[]>([])
const loading = ref(false)

// 菜单树数据（用于选择上级菜单）
const menuTreeData = ref<MenuItem[]>([])

// 模态框状态
const modalVisible = ref(false)
const modalTitle = ref('新建菜单')
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref()

// 表单数据
const formData = reactive<MenuForm>({
  parentId: 0,
  name: '',
  code: '',
  type: 2,
  icon: '',
  path: '',
  component: '',
  permission: '',
  sort: 0,
  status: 1
})

// 表单校验规则
const formRules = {
  parentId: [
    { required: true, message: '请选择上级菜单', trigger: 'change' }
  ],
  name: [
    { required: true, message: '请输入菜单名称', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入菜单编码', trigger: 'blur' }
  ],
  type: [
    { required: true, message: '请选择菜单类型', trigger: 'change' }
  ],
  permission: [
    { required: true, message: '请输入权限标识', trigger: 'blur' }
  ]
}

// 获取菜单列表
const fetchMenus = async () => {
  loading.value = true
  try {
    const res = await menuApi.getMenus(queryParams)
    menuList.value = res
  } catch (error) {
    message.error('获取菜单列表失败')
  } finally {
    loading.value = false
  }
}

// 获取菜单树（用于选择上级菜单）
/*
const _fetchMenuTree = async () => {
  try {
    const res = await menuApi.getMenus()
    // 添加一个根节点选项
    menuTreeData.value = [
      {
        id: 0,
        parentId: -1,
        name: '根目录',
        code: '',
        type: 0,
        icon: '',
        path: '',
        component: '',
        permission: '',
        sort: 0,
        status: 1,
        children: res
      }
    ]
  } catch (error) {
    console.error('获取菜单树失败', error)
  }
}
*/

// 获取菜单选项（用于下拉选择）
const fetchMenuOptions = async () => {
  try {
    const res = await menuApi.getAllMenus()
    menuTreeData.value = [
      {
        id: 0,
        parentId: -1,
        name: '根目录',
        code: '',
        type: 0,
        icon: '',
        path: '',
        component: '',
        permission: '',
        sort: 0,
        status: 1,
        children: res as any
      }
    ]
  } catch (error) {
    console.error('获取菜单选项失败', error)
  }
}

// 获取菜单类型颜色
const getTypeColor = (type: number) => {
  switch (type) {
    case 1: return 'blue'
    case 2: return 'green'
    case 3: return 'orange'
    default: return 'default'
  }
}

// 获取菜单类型文本
const getTypeText = (type: number) => {
  switch (type) {
    case 1: return '目录'
    case 2: return '菜单'
    case 3: return '按钮'
    default: return '未知'
  }
}

// 查询
const handleQuery = () => {
  fetchMenus()
}

// 重置
const handleReset = () => {
  queryParams.name = undefined
  queryParams.status = undefined
  fetchMenus()
}

// 新建菜单
const handleCreate = (parentId: number | null) => {
  isEdit.value = false
  modalTitle.value = parentId ? '新建子菜单' : '新建菜单'
  resetForm()
  formData.parentId = parentId === null ? 0 : parentId
  modalVisible.value = true
}

// 编辑菜单
const handleEdit = async (record: MenuItem) => {
  isEdit.value = true
  modalTitle.value = '编辑菜单'
  resetForm()
  
  try {
    const menu = await menuApi.getMenu(record.id)
    formData.id = menu.id
    formData.parentId = menu.parentId
    formData.name = menu.name
    formData.code = menu.code
    formData.type = menu.type
    formData.icon = menu.icon
    formData.path = menu.path
    formData.component = menu.component
    formData.permission = menu.permission
    formData.sort = menu.sort
    formData.status = menu.status
    
    modalVisible.value = true
  } catch (error) {
    message.error('获取菜单详情失败')
  }
}

// 删除菜单
const handleDelete = async (id: number) => {
  try {
    await menuApi.deleteMenu(id)
    message.success('删除成功')
    fetchMenus()
    fetchMenuOptions()
  } catch (error: any) {
    message.error(error.message || '删除失败')
  }
}

// 提交表单
const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    
    submitLoading.value = true
    
    if (isEdit.value) {
      await menuApi.updateMenu(formData.id!, {
        parentId: formData.parentId,
        name: formData.name,
        code: formData.code,
        type: formData.type,
        icon: formData.icon,
        path: formData.path,
        component: formData.component,
        permission: formData.permission,
        sort: formData.sort,
        status: formData.status
      })
      message.success('更新成功')
    } else {
      await menuApi.createMenu(formData)
      message.success('创建成功')
    }
    
    modalVisible.value = false
    fetchMenus()
    fetchMenuOptions()
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
  formData.parentId = 0
  formData.name = ''
  formData.code = ''
  formData.type = 2
  formData.icon = ''
  formData.path = ''
  formData.component = ''
  formData.permission = ''
  formData.sort = 0
  formData.status = 1
  
  if (formRef.value) {
    formRef.value.resetFields()
  }
}

onMounted(() => {
  fetchMenus()
  fetchMenuOptions()
})
</script>

<style scoped lang="less">
.menu-management {
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
