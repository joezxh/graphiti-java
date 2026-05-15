<template>
  <div class="user-management">
    <a-card class="page-header" :bordered="false">
      <div class="header-content">
        <div class="header-left">
          <h2 class="page-title">{{ $t("system.user.title") }}</h2>
          <p class="page-description">{{ $t("system.user.titleDesc") }}</p>
        </div>
        <div class="header-actions">
          <a-button type="primary" @click="handleCreate">
            <template #icon><PlusOutlined /></template>
            {{ $t("system.user.createUser") }}
          </a-button>
        </div>
      </div>
    </a-card>

    <a-card class="content-card" :bordered="false">
      <div class="table-operations">
        <a-form layout="inline" :model="queryParams" class="search-form">
          <a-form-item :label="$t('system.user.username')">
            <a-input
              v-model:value="queryParams.username"
              :placeholder="$t('system.user.enterUsername')"
              allow-clear
              style="width: 160px"
            />
          </a-form-item>
          <a-form-item :label="$t('system.user.nickname')">
            <a-input
              v-model:value="queryParams.nickname"
              :placeholder="$t('system.user.enterNickname')"
              allow-clear
              style="width: 160px"
            />
          </a-form-item>
          <a-form-item :label="$t('common.status')">
            <a-select
              v-model:value="queryParams.status"
              :placeholder="$t('form.pleaseSelect')"
              allow-clear
              style="width: 120px"
            >
              <a-select-option :value="1">{{ $t("common.enabled") }}</a-select-option>
              <a-select-option :value="0">{{ $t("common.disabled") }}</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item>
            <a-space>
              <a-button type="primary" @click="handleQuery">{{ $t("common.query") }}</a-button>
              <a-button @click="handleReset">{{ $t("common.reset") }}</a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </div>

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
              {{ record.status === 1 ? $t("common.enabled") : $t("common.disabled") }}
            </span>
          </template>

          <template v-if="column.dataIndex === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleEdit(record)">
                <template #icon><EditOutlined /></template>
                {{ $t("common.edit") }}
              </a-button>
              <a-button type="link" size="small" @click="handleResetPassword(record)">
                <template #icon><KeyOutlined /></template>
                {{ $t("system.user.resetPassword") }}
              </a-button>
              <a-popconfirm
                :title="$t('system.user.confirmDelete')"
                :ok-text="$t('common.confirm')"
                :cancel-text="$t('common.cancel')"
                @confirm="handleDelete(record.id)"
              >
                <a-button type="link" size="small" danger>
                  <template #icon><DeleteOutlined /></template>
                  {{ $t("common.delete") }}
                </a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

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
        <a-form-item :label="$t('system.user.username')" name="username">
          <a-input v-model:value="formData.username" :placeholder="$t('system.user.enterUsername')" :disabled="isEdit" />
        </a-form-item>

        <a-form-item :label="$t('system.user.nickname')" name="nickname">
          <a-input v-model:value="formData.nickname" :placeholder="$t('system.user.enterNickname')" />
        </a-form-item>

        <a-form-item v-if="!isEdit" :label="$t('system.user.password')" name="password">
          <a-input-password v-model:value="formData.password" :placeholder="$t('system.user.enterPassword')" />
        </a-form-item>

        <a-form-item :label="$t('system.user.email')" name="email">
          <a-input v-model:value="formData.email" :placeholder="$t('system.user.enterEmail')" />
        </a-form-item>

        <a-form-item :label="$t('system.user.phone')" name="phone">
          <a-input v-model:value="formData.phone" :placeholder="$t('system.user.enterPhone')" />
        </a-form-item>

        <a-form-item :label="$t('common.status')" name="status">
          <a-radio-group v-model:value="formData.status">
            <a-radio :value="1">{{ $t("common.enabled") }}</a-radio>
            <a-radio :value="0">{{ $t("common.disabled") }}</a-radio>
          </a-radio-group>
        </a-form-item>

        <a-form-item :label="$t('system.user.role')" name="roleIds">
          <a-select
            v-model:value="formData.roleIds"
            mode="multiple"
            :placeholder="$t('system.user.selectRole')"
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
import { ref, reactive, onMounted } from "vue"
import { message } from "ant-design-vue"
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  KeyOutlined
} from "@ant-design/icons-vue"
import { userApi, type User, type UserQuery, type UserForm } from "@/api/user"
import { roleApi, type Role } from "@/api/role"

const queryParams = reactive<UserQuery>({
  username: undefined,
  nickname: undefined,
  status: undefined,
  pageNum: 1,
  pageSize: 10
})

const columns = [
  { title: "common.id", dataIndex: "id", width: 60 },
  { title: "system.user.username", dataIndex: "username", width: 120 },
  { title: "system.user.nickname", dataIndex: "nickname", width: 120 },
  { title: "system.user.email", dataIndex: "email", width: 180 },
  { title: "system.user.phone", dataIndex: "phone", width: 130 },
  { title: "common.status", dataIndex: "status", width: 100 },
  { title: "common.createdAt", dataIndex: "createdAt", width: 170 },
  { title: "common.action", dataIndex: "action", width: 220, fixed: "right" }
]

const userList = ref<User[]>([])
const loading = ref(false)
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true
})

const roleOptions = ref<Pick<Role, "id" | "name" | "code">[]>([])

const modalVisible = ref(false)
const modalTitle = ref("")
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref()

const formData = reactive<UserForm>({
  username: "",
  nickname: "",
  password: "",
  email: "",
  phone: "",
  status: 1,
  roleIds: []
})

const formRules = {
  username: [{ required: true, message: "system.user.enterUsername", trigger: "blur" }],
  nickname: [{ required: true, message: "system.user.enterNickname", trigger: "blur" }],
  password: [
    { required: true, message: "system.user.enterPassword", trigger: "blur" },
    { min: 6, message: "system.user.passwordMin", trigger: "blur" }
  ],
  email: [
    { required: true, message: "system.user.enterEmail", trigger: "blur" },
    { type: "email", message: "system.user.invalidEmail", trigger: "blur" }
  ],
  phone: [{ required: true, message: "system.user.enterPhone", trigger: "blur" }],
  roleIds: [{ required: true, message: "system.user.selectRole", trigger: "change" }]
}

const fetchUsers = async () => {
  loading.value = true
  try {
    const res = await userApi.getUsers(queryParams)
    userList.value = res.list
    pagination.current = res.pageNum
    pagination.pageSize = res.pageSize
    pagination.total = res.total
  } catch (error) {
    message.error("system.user.loadFailed")
  } finally {
    loading.value = false
  }
}

const fetchRoles = async () => {
  try {
    const res = await roleApi.getAllRoles()
    roleOptions.value = res
  } catch (error) {
    console.error("system.role.loadFailed", error)
  }
}

const handleQuery = () => {
  queryParams.pageNum = 1
  fetchUsers()
}

const handleReset = () => {
  queryParams.username = undefined
  queryParams.nickname = undefined
  queryParams.status = undefined
  queryParams.pageNum = 1
  fetchUsers()
}

const handleTableChange = (pag: any) => {
  queryParams.pageNum = pag.current
  queryParams.pageSize = pag.pageSize
  fetchUsers()
}

const handleCreate = () => {
  isEdit.value = false
  modalTitle.value = "system.user.newUser"
  resetForm()
  modalVisible.value = true
}

const handleEdit = async (record: User) => {
  isEdit.value = true
  modalTitle.value = "system.user.editUser"
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
    message.error("system.user.getDetailFailed")
  }
}

const handleResetPassword = async (record: User) => {
  try {
    await userApi.resetPassword(record.id)
    message.success("system.user.passwordResetSuccess")
  } catch (error) {
    message.error("system.user.resetFailed")
  }
}

const handleDelete = async (id: number) => {
  try {
    await userApi.deleteUser(id)
    message.success("system.user.deleteSuccess")
    fetchUsers()
  } catch (error) {
    message.error("system.user.deleteSuccess")
  }
}

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
      message.success("system.user.updateSuccess")
    } else {
      await userApi.createUser(formData)
      message.success("system.user.createSuccess")
    }

    modalVisible.value = false
    fetchUsers()
  } catch (error) {
    console.error("common.submitFailed", error)
  } finally {
    submitLoading.value = false
  }
}

const handleCancel = () => {
  modalVisible.value = false
  resetForm()
}

const resetForm = () => {
  formData.id = undefined
  formData.username = ""
  formData.nickname = ""
  formData.password = ""
  formData.email = ""
  formData.phone = ""
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
