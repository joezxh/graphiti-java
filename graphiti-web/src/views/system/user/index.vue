<template>
  <div class="user-management">
    <a-card class="page-header" :bordered="false">
      <div class="header-content">
        <div class="header-left">
          <h2 class="page-title">用户管理</h2>
          <p class="page-description">管理系统用户，分配角色权限</p>
        </div>
        <div class="header-actions">
          <a-button type="primary" @click="handleCreate">
            <template #icon><PlusOutlined /></template>
            新建用户
          </a-button>
        </div>
      </div>
    </a-card>

    <a-card class="content-card" :bordered="false">
      <!-- 搜索表单 -->
      <div class="table-operations">
        <a-form layout="inline" :model="queryParams" class="search-form">
          <a-form-item label="用户名">
            <a-input
              v-model:value="queryParams.username"
              placeholder="请输入用户名"
              allow-clear
              style="width: 160px"
            />
          </a-form-item>
          <a-form-item label="昵称">
            <a-input
              v-model:value="queryParams.nickname"
              placeholder="请输入昵称"
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
        :data-source="userList"
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
              <a-button type="link" size="small" @click="handleResetPassword(record)">
                <template #icon><KeyOutlined /></template>
                重置密码
              </a-button>
              <a-popconfirm
                title="确定要删除此用户吗？"
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

    <!-- 用户表单对话框 -->
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
        <a-form-item label="用户名" name="username">
          <a-input v-model:value="formData.username" placeholder="请输入用户名" :disabled="isEdit" />
        </a-form-item>
        
        <a-form-item label="昵称" name="nickname">
          <a-input v-model:value="formData.nickname" placeholder="请输入昵称" />
        </a-form-item>
        
        <a-form-item v-if="!isEdit" label="密码" name="password">
          <a-input-password v-model:value="formData.password" placeholder="请输入密码" />
        </a-form-item>
        
        <a-form-item label="邮箱" name="email">
          <a-input v-model:value="formData.email" placeholder="请输入邮箱" />
        </a-form-item>
        
        <a-form-item label="手机号" name="phone">
          <a-input v-model:value="formData.phone" placeholder="请输入手机号" />
        </a-form-item>
        
        <a-form-item label="状态" name="status">
          <a-radio-group v-model:value="formData.status">
            <a-radio :value="1">启用</a-radio>
            <a-radio :value="0">禁用</a-radio>
          </a-radio-group>
        </a-form-item>
        
        <a-form-item label="角色" name="roleIds">
          <a-select
            v-model:value="formData.roleIds"
            mode="multiple"
            placeholder="请选择角色"
          >
            <a-select-option v-for="role in roleOptions" :key="role.id" :value="role.id">
              {{ role.name }}
            </a-select-option>
          </a-select>
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
  DeleteOutlined,
  KeyOutlined
} from '@ant-design/icons-vue'
import { userApi, type User, type UserQuery, type UserForm } from '@/api/user'
import { roleApi, type Role } from '@/api/role'

// 查询参数
const queryParams = reactive<UserQuery>({
  username: undefined,
  nickname: undefined,
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
    title: '用户名',
    dataIndex: 'username',
    width: 120
  },
  {
    title: '昵称',
    dataIndex: 'nickname',
    width: 120
  },
  {
    title: '邮箱',
    dataIndex: 'email',
    width: 180
  },
  {
    title: '手机号',
    dataIndex: 'phone',
    width: 130
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
    width: 220,
    fixed: 'right'
  }
]

// 数据列表
const userList = ref<User[]>([])
const loading = ref(false)
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true
})

// 角色选项
const roleOptions = ref<Pick<Role, 'id' | 'name' | 'code'>[]>([])

// 模态框状态
const modalVisible = ref(false)
const modalTitle = ref('新建用户')
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref()

// 表单数据
const formData = reactive<UserForm>({
  username: '',
  nickname: '',
  password: '',
  email: '',
  phone: '',
  status: 1,
  roleIds: []
})

// 表单校验规则
const formRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能小于6位', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入有效的邮箱地址', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' }
  ],
  roleIds: [
    { required: true, message: '请选择角色', trigger: 'change' }
  ]
}

// 获取用户列表
const fetchUsers = async () => {
  loading.value = true
  try {
    const res = await userApi.getUsers(queryParams)
    userList.value = res.list
    pagination.current = res.pageNum
    pagination.pageSize = res.pageSize
    pagination.total = res.total
  } catch (error) {
    message.error('获取用户列表失败')
  } finally {
    loading.value = false
  }
}

// 获取角色选项
const fetchRoles = async () => {
  try {
    const res = await roleApi.getAllRoles()
    roleOptions.value = res
  } catch (error) {
    console.error('获取角色列表失败', error)
  }
}

// 查询
const handleQuery = () => {
  queryParams.pageNum = 1
  fetchUsers()
}

// 重置
const handleReset = () => {
  queryParams.username = undefined
  queryParams.nickname = undefined
  queryParams.status = undefined
  queryParams.pageNum = 1
  fetchUsers()
}

// 表格变化
const handleTableChange = (pag: any) => {
  queryParams.pageNum = pag.current
  queryParams.pageSize = pag.pageSize
  fetchUsers()
}

// 新建用户
const handleCreate = () => {
  isEdit.value = false
  modalTitle.value = '新建用户'
  resetForm()
  modalVisible.value = true
}

// 编辑用户
const handleEdit = async (record: User) => {
  isEdit.value = true
  modalTitle.value = '编辑用户'
  resetForm()
  
  try {
    const user = await userApi.getUser(record.id)
    formData.username = user.username
    formData.nickname = user.nickname
    formData.email = user.email
    formData.phone = user.phone
    formData.status = user.status
    formData.roleIds = user.roleIds
    
    modalVisible.value = true
  } catch (error) {
    message.error('获取用户详情失败')
  }
}

// 重置密码
const handleResetPassword = async (record: User) => {
  try {
    await userApi.resetPassword(record.id)
    message.success('密码已重置为默认密码')
  } catch (error) {
    message.error('重置密码失败')
  }
}

// 删除用户
const handleDelete = async (id: number) => {
  try {
    await userApi.deleteUser(id)
    message.success('删除成功')
    fetchUsers()
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
      await userApi.updateUser(formData.id!, {
        nickname: formData.nickname,
        email: formData.email,
        phone: formData.phone,
        status: formData.status,
        roleIds: formData.roleIds
      })
      message.success('更新成功')
    } else {
      await userApi.createUser(formData)
      message.success('创建成功')
    }
    
    modalVisible.value = false
    fetchUsers()
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
  formData.username = ''
  formData.nickname = ''
  formData.password = ''
  formData.email = ''
  formData.phone = ''
  formData.status = 1
  formData.roleIds = []
  
  if (formRef.value) {
    formRef.value.resetFields()
  }
}

onMounted(() => {
  fetchUsers()
  fetchRoles()
})
</script>

<style scoped lang="less">
.user-management {
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
