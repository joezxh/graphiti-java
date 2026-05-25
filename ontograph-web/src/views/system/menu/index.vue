<template>
  <div class="menu-management">
    <a-card class="page-header" :bordered="false">
      <div class="header-content">
        <div class="header-left">
          <h2 class="page-title">{{ $t("system.menu.title") }}</h2>
          <p class="page-description">{{ $t("system.menu.titleDesc") }}</p>
        </div>
        <div class="header-actions">
          <a-button type="primary" @click="handleCreate(null)">
            <template #icon><PlusOutlined /></template>
            {{ $t("system.menu.createMenu") }}
          </a-button>
        </div>
      </div>
    </a-card>

    <a-card class="content-card" :bordered="false">
      <div class="table-operations">
        <a-form layout="inline" :model="queryParams" class="search-form">
          <a-form-item :label="$t('system.menu.menuName')">
            <a-input
              v-model:value="queryParams.name"
              :placeholder="$t('system.menu.enterMenuName')"
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
              {{ record.status === 1 ? $t("common.enabled") : $t("common.disabled") }}
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
                {{ $t("system.menu.addSubmenu") }}
              </a-button>
              <a-button type="link" size="small" @click="handleEdit(record)">
                <template #icon><EditOutlined /></template>
                {{ $t("common.edit") }}
              </a-button>
              <a-popconfirm
                :title="$t('system.menu.confirmDelete')"
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
        <a-form-item :label="$t('system.menu.parentMenu')" name="parentId">
          <a-tree-select
            v-model:value="formData.parentId"
            :tree-data="menuTreeData"
            :field-names="{ label: 'name', value: 'id', children: 'children' }"
            :placeholder="$t('system.menu.selectParentMenu')"
            allow-clear
            tree-default-expand-all
          />
        </a-form-item>

        <a-form-item :label="$t('system.menu.menuName')" name="name">
          <a-input v-model:value="formData.name" :placeholder="$t('system.menu.enterMenuName')" />
        </a-form-item>

        <a-form-item :label="$t('system.menu.menuCode')" name="code">
          <a-input v-model:value="formData.code" :placeholder="$t('system.menu.enterMenuCode')" />
        </a-form-item>

        <a-form-item :label="$t('system.menu.menuType')" name="type">
          <a-radio-group v-model:value="formData.type">
            <a-radio :value="1">{{ $t("common.directory") }}</a-radio>
            <a-radio :value="2">{{ $t("common.menu") }}</a-radio>
            <a-radio :value="3">{{ $t("common.button") }}</a-radio>
          </a-radio-group>
        </a-form-item>

        <a-form-item :label="$t('system.menu.menuIcon')" name="icon" v-if="formData.type !== 3">
          <a-input v-model:value="formData.icon" :placeholder="$t('system.menu.menuIcon')" />
        </a-form-item>

        <a-form-item :label="$t('system.menu.menuPath')" name="path" v-if="formData.type === 2">
          <a-input v-model:value="formData.path" :placeholder="$t('system.menu.menuPath')" />
        </a-form-item>

        <a-form-item :label="$t('system.menu.componentPath')" name="component" v-if="formData.type === 2">
          <a-input v-model:value="formData.component" :placeholder="$t('system.menu.componentPath')" />
        </a-form-item>

        <a-form-item :label="$t('system.menu.permission')" name="permission">
          <a-input v-model:value="formData.permission" :placeholder="$t('system.menu.enterPermission')" />
        </a-form-item>

        <a-form-item :label="$t('system.menu.sort')" name="sort">
          <a-input-number v-model:value="formData.sort" :min="0" style="width: 100%" />
        </a-form-item>

        <a-form-item :label="$t('common.status')" name="status">
          <a-radio-group v-model:value="formData.status">
            <a-radio :value="1">{{ $t("common.enabled") }}</a-radio>
            <a-radio :value="0">{{ $t("common.disabled") }}</a-radio>
          </a-radio-group>
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
  DeleteOutlined
} from "@ant-design/icons-vue"
import { menuApi, type MenuItem, type MenuQuery, type MenuForm } from "@/api/menu"

const queryParams = reactive<MenuQuery>({
  name: undefined,
  status: undefined
})

const columns = [
  { title: "common.id", dataIndex: "id", width: 60 },
  { title: "system.menu.menuName", dataIndex: "name", width: 150 },
  { title: "system.menu.menuCode", dataIndex: "code", width: 150 },
  { title: "common.type", dataIndex: "type", width: 100 },
  { title: "common.directory", dataIndex: "icon", width: 80 },
  { title: "Path", dataIndex: "path", width: 150 },
  { title: "system.menu.permission", dataIndex: "permission", width: 150 },
  { title: "system.menu.sort", dataIndex: "sort", width: 80 },
  { title: "common.status", dataIndex: "status", width: 100 },
  { title: "common.action", dataIndex: "action", width: 250, fixed: "right" }
]

const menuList = ref<MenuItem[]>([])
const loading = ref(false)

const menuTreeData = ref<MenuItem[]>([])

const modalVisible = ref(false)
const modalTitle = ref("")
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref()

const formData = reactive<MenuForm>({
  parentId: 0,
  name: "",
  code: "",
  type: 2,
  icon: "",
  path: "",
  component: "",
  permission: "",
  sort: 0,
  status: 1
})

const formRules = {
  parentId: [{ required: true, message: "system.menu.selectParentMenuRequired", trigger: "change" }],
  name: [{ required: true, message: "system.menu.enterMenuName", trigger: "blur" }],
  code: [{ required: true, message: "system.menu.enterMenuCode", trigger: "blur" }],
  type: [{ required: true, message: "system.menu.selectMenuType", trigger: "change" }],
  permission: [{ required: true, message: "system.menu.enterPermission", trigger: "blur" }]
}

const fetchMenus = async () => {
  loading.value = true
  try {
    const res = await menuApi.getMenus(queryParams)
    menuList.value = res
  } catch (error) {
    message.error("system.menu.loadFailed")
  } finally {
    loading.value = false
  }
}

const fetchMenuOptions = async () => {
  try {
    const res = await menuApi.getAllMenus()
    menuTreeData.value = [
      {
        id: 0,
        parentId: -1,
        name: "common.rootDirectory",
        code: "",
        type: 0,
        icon: "",
        path: "",
        component: "",
        permission: "",
        sort: 0,
        status: 1,
        children: res as any
      }
    ]
  } catch (error) {
    console.error("system.menu.loadFailed", error)
  }
}

const getTypeColor = (type: number) => {
  switch (type) {
    case 1: return "blue"
    case 2: return "green"
    case 3: return "orange"
    default: return "default"
  }
}

const getTypeText = (type: number) => {
  switch (type) {
    case 1: return "common.directory"
    case 2: return "common.menu"
    case 3: return "common.button"
    default: return "common.unknown"
  }
}

const handleQuery = () => {
  fetchMenus()
}

const handleReset = () => {
  queryParams.name = undefined
  queryParams.status = undefined
  fetchMenus()
}

const handleCreate = (parentId: number | null) => {
  isEdit.value = false
  modalTitle.value = parentId ? "system.menu.newSubMenu" : "system.menu.newMenu"
  resetForm()
  formData.parentId = parentId === null ? 0 : parentId
  modalVisible.value = true
}

const handleEdit = async (record: MenuItem) => {
  isEdit.value = true
  modalTitle.value = "system.menu.editMenu"
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
    message.error("system.menu.getDetailFailed")
  }
}

const handleDelete = async (id: number) => {
  try {
    await menuApi.deleteMenu(id)
    message.success("system.menu.deleteSuccess")
    fetchMenus()
    fetchMenuOptions()
  } catch (error: any) {
    message.error(error.message || "system.menu.deleteSuccess")
  }
}

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
      message.success("system.menu.updateSuccess")
    } else {
      await menuApi.createMenu(formData)
      message.success("system.menu.createSuccess")
    }

    modalVisible.value = false
    fetchMenus()
    fetchMenuOptions()
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
  formData.parentId = 0
  formData.name = ""
  formData.code = ""
  formData.type = 2
  formData.icon = ""
  formData.path = ""
  formData.component = ""
  formData.permission = ""
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
