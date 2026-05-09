<template>
  <div class="role-management">
    <a-card class="page-header" :bordered="false">
      <div class="header-content">
        <div class="header-left">
          <h2 class="page-title">角色管理</h2>
          <p class="page-description">管理系统角色，配置菜单权限</p>
        </div>
        <div class="header-actions">
          <a-button type="primary" @click="handleCreate">
            <template #icon><PlusOutlined /></template>
            新建角色
          </a-button>
        </div>
      </div>
    </a-card>

    <a-card class="content-card" :bordered="false">
      <!-- 搜索表单 -->
      <div class="table-operations">
        <a-form layout="inline" :model="queryParams" class="search-form">
          <a-form-item label="角色名称">
            <a-input
              v-model:value="queryParams.name"
              placeholder="请输入角色名称"
              allow-clear
              style="width: 160px"
            />
          </a-form-item>
          <a-form-item label="角色编码">
            <a-input
              v-model:value="queryParams.code"
              placeholder="请输入角色编码"
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
        :data-source="roleList"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
        row-key="id"
      >
        <template #bodyCell="{ column, record }">
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
                title="确定要删除此角色吗？"
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

    <!-- 角色表单对话框 -->
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
        <a-form-item label="角色名称" name="name">
          <a-input v-model:value="formData.name" placeholder="请输入角色名称" />
        </a-form-item>
        
        <a-form-item label="角色编码" name="code">
          <a-input v-model:value="formData.code" placeholder="请输入角色编码" />
        </a-form-item>
        
        <a-form-item label="描述" name="description">
          <a-textarea v-model:value="formData.description" placeholder="请输入描述" :rows="3" />
        </a-form-item>
        
        <a-form-item label="状态" name="status">
          <a-radio-group v-model:value="formData.status">
            <a-radio :value="1">启用</a-radio>
            <a-radio :value="0">禁用</a-radio>
          </a-radio-group>
        </a-form-item>
        
        <a-form-item label="菜单权限" name="menuIds">
          <a-tree
            v-model:checkedKeys="formData.menuIds"
            :tree-data="menuTreeData"
            :field-names="{ title: 'name', key: 'id', children: 'children' }"
            checkable
            default-expand-all
          />
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
import { roleApi, type Role, type RoleQuery, type RoleForm } from '@/api/role'
import { menuApi, type MenuItem } from '@/api/menu'

// 查询参数
const queryParams = reactive<RoleQuery>({
  name: undefined,
  code: undefined,
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
    title: '角色名称',
    dataIndex: 'name',
    width: 120
  },
  {
    title: '角色编码',
    dataIndex: 'code',
    width: 120
  },
  {
    title: '描述',
    dataIndex: 'description',
    width: 200
  },
  {
    title: '状态',
    dataIndex: 'status',
    width: 100
  },
  {
    title: '创建时间',
    dataIndex: 'createdAt',
    width: 170
  },
  {
    title: '操作',
    dataIndex: 'action',
    width: 150,
    fixed: 'right'
  }
]

// 数据列表
const roleList = ref<Role[]>([])
const loading = ref(false)
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true
})

// 菜单树数据
const menuTreeData = ref<MenuItem[]>([])

// 模态框状态
const modalVisible = ref(false)
const modalTitle = ref('新建角色')
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref()

// 表单数据
const formData = reactive<RoleForm>({
  name: '',
  code: '',
  description: '',
  status: 1,
  menuIds: []
})

// 表单校验规则
const formRules = {
  name: [
    { required: true, message: '请输入角色名称', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入角色编码', trigger: 'blur' }
  ],
  menuIds: [
    { required: true, message: '请选择菜单权限', trigger: 'change' }
  ]
}

// 获取角色列表
const fetchRoles = async () => {
  loading.value = true
  try {
    const res = await roleApi.getRoles(queryParams)
    roleList.value = res.list
    pagination.current = res.pageNum
    pagination.pageSize = res.pageSize
    pagination.total = res.total
  } catch (error) {
    message.error('获取角色列表失败')
  } finally {
    loading.value = false
  }
}

// 获取菜单树
const fetchMenuTree = async () => {
  try {
    const res = await menuApi.getMenus()
    menuTreeData.value = res
  } catch (error) {
    console.error('获取菜单树失败', error)
  }
}

// 查询
const handleQuery = () => {
  queryParams.pageNum = 1
  fetchRoles()
}

// 重置
const handleReset = () => {
  queryParams.name = undefined
  queryParams.code = undefined
  queryParams.status = undefined
  queryParams.pageNum = 1
  fetchRoles()
}

// 表格变化
const handleTableChange = (pag: any) => {
  queryParams.pageNum = pag.current
  queryParams.pageSize = pag.pageSize
  fetchRoles()
}

// 新建角色
const handleCreate = () => {
  isEdit.value = false
  modalTitle.value = '新建角色'
  resetForm()
  modalVisible.value = true
}

// 编辑角色
const handleEdit = async (record: Role) => {
  isEdit.value = true
  modalTitle.value = '编辑角色'
  resetForm()
  
  try {
    const role = await roleApi.getRole(record.id)
    formData.name = role.name
    formData.code = role.code
    formData.description = role.description
    formData.status = role.status
    formData.menuIds = role.menuIds
    
    modalVisible.value = true
  } catch (error) {
    message.error('获取角色详情失败')
  }
}

// 删除角色
const handleDelete = async (id: number) => {
  try {
    await roleApi.deleteRole(id)
    message.success('删除成功')
    fetchRoles()
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
      await roleApi.updateRole(formData.id!, {
        name: formData.name,
        code: formData.code,
        description: formData.description,
        status: formData.status,
        menuIds: formData.menuIds
      })
      message.success('更新成功')
    } else {
      await roleApi.createRole(formData)
      message.success('创建成功')
    }
    
    modalVisible.value = false
    fetchRoles()
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
  formData.name = ''
  formData.code = ''
  formData.description = ''
  formData.status = 1
  formData.menuIds = []
  
  if (formRef.value) {
    formRef.value.resetFields()
  }
}

onMounted(() => {
  fetchRoles()
  fetchMenuTree()
})
</script>

<style scoped lang="less">
.role-management {
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
