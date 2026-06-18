<template>
  <div class="role-management">
    <a-card class="page-header" :bordered="false">
      <div class="header-content">
        <div class="header-left">
          <h2 class="page-title">{{ $t("system.role.title") }}</h2>
          <p class="page-description">{{ $t("system.role.titleDesc") }}</p>
        </div>
        <div class="header-actions">
          <a-button type="primary" @click="handleCreate">
            <template #icon><PlusOutlined /></template>
            {{ $t("system.role.createRole") }}
          </a-button>
        </div>
      </div>
    </a-card>

    <a-card class="content-card" :bordered="false">
      <div class="table-operations">
        <a-form layout="inline" :model="queryParams" class="search-form">
          <a-form-item :label="$t('system.role.roleName')">
            <a-input
              v-model:value="queryParams.name"
              :placeholder="$t('system.role.enterRoleName')"
              allow-clear
              style="width: 160px"
            />
          </a-form-item>
          <a-form-item :label="$t('system.role.roleCode')">
            <a-input
              v-model:value="queryParams.code"
              :placeholder="$t('system.role.enterRoleCode')"
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
              {{ record.status === 1 ? $t("common.enabled") : $t("common.disabled") }}
            </span>
          </template>

          <template v-if="column.dataIndex === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleEdit(record)">
                <template #icon><EditOutlined /></template>
                {{ $t("common.edit") }}
              </a-button>
              <a-popconfirm
                :title="$t('system.role.confirmDelete')"
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
      :ok-text="$t('common.confirm')"
      :cancel-text="$t('common.cancel')"
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
        <a-form-item :label="$t('system.role.roleName')" name="name">
          <a-input v-model:value="formData.name" :placeholder="$t('system.role.enterRoleName')" />
        </a-form-item>

        <a-form-item :label="$t('system.role.roleCode')" name="code">
          <a-input v-model:value="formData.code" :placeholder="$t('system.role.enterRoleCode')" />
        </a-form-item>

        <a-form-item :label="$t('common.description')" name="description">
          <a-textarea v-model:value="formData.description" :placeholder="$t('common.noDescription')" :rows="3" />
        </a-form-item>

        <a-form-item :label="$t('common.status')" name="status">
          <a-radio-group v-model:value="formData.status">
            <a-radio :value="1">{{ $t("common.enabled") }}</a-radio>
            <a-radio :value="0">{{ $t("common.disabled") }}</a-radio>
          </a-radio-group>
        </a-form-item>

        <a-form-item :label="$t('system.menu.permission')" name="menuIds">
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
import { ref, reactive, onMounted, computed } from "vue"
import { message } from "ant-design-vue"
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined
} from "@ant-design/icons-vue"
import { roleApi, type Role, type RoleQuery, type RoleForm } from "@/api/role"
import { menuApi, type MenuItem } from "@/api/menu"
import { useI18n } from "vue-i18n"

const { t } = useI18n()

const queryParams = reactive<RoleQuery>({
  name: undefined,
  code: undefined,
  status: undefined,
  pageNum: 1,
  pageSize: 10
})

const columns = computed(() => [
  { title: t("common.id"), dataIndex: "id", width: 60 },
  { title: t("system.role.roleName"), dataIndex: "name", width: 120 },
  { title: t("system.role.roleCode"), dataIndex: "code", width: 120 },
  { title: t("common.description"), dataIndex: "description", width: 200 },
  { title: t("common.status"), dataIndex: "status", width: 100 },
  { title: t("common.createdAt"), dataIndex: "createdAt", width: 170 },
  { title: t("common.action"), dataIndex: "action", width: 150, fixed: "right" }
])

const roleList = ref<Role[]>([])
const loading = ref(false)
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true
})

const menuTreeData = ref<MenuItem[]>([])

const modalVisible = ref(false)
const modalTitle = ref("")
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref()

const formData = reactive<RoleForm>({
  name: "",
  code: "",
  description: "",
  status: 1,
  menuIds: []
})

const formRules = {
  name: [{ required: true, message: t("system.role.enterRoleName"), trigger: "blur" }],
  code: [{ required: true, message: t("system.role.enterRoleCode"), trigger: "blur" }],
  menuIds: [{ required: false, message: t("system.role.selectMenuPermission"), trigger: "change" }]
}

const fetchRoles = async () => {
  loading.value = true
  try {
    const res = await roleApi.getRoles(queryParams)
    roleList.value = res.list
    pagination.current = res.pageNum
    pagination.pageSize = res.pageSize
    pagination.total = res.total
  } catch (error) {
    message.error(t("system.role.loadFailed"))
  } finally {
    loading.value = false
  }
}

const fetchMenuTree = async () => {
  try {
    const res = await menuApi.getMenus()
    menuTreeData.value = res
  } catch (error) {
    console.error("system.role.loadMenuFailed", error)
  }
}

const handleQuery = () => {
  queryParams.pageNum = 1
  fetchRoles()
}

const handleReset = () => {
  queryParams.name = undefined
  queryParams.code = undefined
  queryParams.status = undefined
  queryParams.pageNum = 1
  fetchRoles()
}

const handleTableChange = (pag: any) => {
  queryParams.pageNum = pag.current
  queryParams.pageSize = pag.pageSize
  fetchRoles()
}

const handleCreate = () => {
  isEdit.value = false
  modalTitle.value = t("system.role.newRole")
  resetForm()
  modalVisible.value = true
}

const handleEdit = async (record: Role) => {
  isEdit.value = true
  modalTitle.value = t("system.role.editRole")
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
    message.error(t("system.role.getDetailFailed"))
  }
}

const handleDelete = async (id: number) => {
  try {
    await roleApi.deleteRole(id)
    message.success(t("system.role.deleteSuccess"))
    fetchRoles()
  } catch (error) {
    message.error(t("system.role.deleteFailed"))
  }
}

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
      message.success(t("system.role.updateSuccess"))
    } else {
      await roleApi.createRole(formData)
      message.success(t("system.role.createSuccess"))
    }

    modalVisible.value = false
    fetchRoles()
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
  formData.name = ""
  formData.code = ""
  formData.description = ""
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
