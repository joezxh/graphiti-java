<template>
  <div class="config-management">
    <a-card class="page-header" :bordered="false">
      <div class="header-content">
        <div class="header-left">
          <h2 class="page-title">{{ $t("system.config.title") }}</h2>
          <p class="page-description">{{ $t("system.config.titleDesc") }}</p>
        </div>
        <div class="header-actions">
          <a-button type="primary" @click="handleCreate">
            <template #icon><PlusOutlined /></template>
            {{ $t("system.config.createConfig") }}
          </a-button>
        </div>
      </div>
    </a-card>

    <a-card class="content-card" :bordered="false">
      <div class="table-operations">
        <a-form layout="inline" :model="queryParams" class="search-form">
          <a-form-item :label="$t('system.config.configKey')">
            <a-input
              v-model:value="queryParams.configKey"
              :placeholder="$t('system.config.enterConfigKey')"
              allow-clear
              style="width: 160px"
            />
          </a-form-item>
          <a-form-item :label="$t('system.config.configName')">
            <a-input
              v-model:value="queryParams.configName"
              :placeholder="$t('system.config.configName')"
              allow-clear
              style="width: 160px"
            />
          </a-form-item>
          <a-form-item :label="$t('system.config.groupName')">
            <a-select
              v-model:value="queryParams.groupName"
              :placeholder="$t('form.pleaseSelect')"
              allow-clear
              style="width: 150px"
            >
              <a-select-option v-for="group in groups" :key="group" :value="group">
                {{ group }}
              </a-select-option>
            </a-select>
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
                :title="$t('system.config.confirmDelete')"
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
        <a-form-item :label="$t('system.config.configKey')" name="configKey">
          <a-input v-model:value="formData.configKey" :placeholder="$t('system.config.enterConfigKey')" :disabled="isEdit" />
        </a-form-item>

        <a-form-item :label="$t('system.config.configValue')" name="configValue">
          <a-textarea v-if="formData.configType === 4" v-model:value="formData.configValue" :placeholder="$t('system.config.jsonFormat')" :rows="5" />
          <a-input-number v-else-if="formData.configType === 2" v-model:value="formData.configValue" :placeholder="$t('system.config.enterConfigValue')" style="width: 100%" />
          <a-switch v-else-if="formData.configType === 3" v-model:checked="configValueBoolean" checked-children="common.enabled" un-checked-children="common.disabled" />
          <a-input v-else v-model:value="formData.configValue" :placeholder="$t('system.config.enterConfigValue')" />
        </a-form-item>

        <a-form-item :label="$t('system.config.configName')" name="configName">
          <a-input v-model:value="formData.configName" :placeholder="$t('system.config.configName')" />
        </a-form-item>

        <a-form-item :label="$t('system.config.configDesc')" name="configDescription">
          <a-textarea v-model:value="formData.configDescription" :placeholder="$t('system.config.configDesc')" :rows="3" />
        </a-form-item>

        <a-form-item :label="$t('system.config.configType')" name="configType">
          <a-select v-model:value="formData.configType" :placeholder="$t('system.config.selectConfigType')">
            <a-select-option :value="1">{{ $t("system.config.text") }}</a-select-option>
            <a-select-option :value="2">{{ $t("system.config.number") }}</a-select-option>
            <a-select-option :value="3">{{ $t("system.config.boolean") }}</a-select-option>
            <a-select-option :value="4">{{ $t("system.config.json") }}</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item :label="$t('system.config.groupName')" name="groupName">
          <a-input v-model:value="formData.groupName" :placeholder="$t('system.config.enterGroupName')" />
        </a-form-item>

        <a-form-item :label="$t('system.menu.sort')" name="sort">
          <a-input-number v-model:value="formData.sortNum" :min="0" style="width: 100%" />
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
import { ref, reactive, computed, onMounted } from "vue"
import { message } from "ant-design-vue"
import { useI18n } from "vue-i18n"
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined
} from "@ant-design/icons-vue"
import { systemApi, type SystemConfig, type SystemConfigQuery, type SystemConfigForm } from "@/api/system"

const { t } = useI18n()

const queryParams = reactive<SystemConfigQuery>({
  configKey: undefined,
  configName: undefined,
  groupName: undefined,
  status: undefined,
  pageNum: 1,
  pageSize: 10
})

const columns = computed(() => [
  { title: t('common.id'), dataIndex: "id", width: 60 },
  { title: t('system.config.configKey'), dataIndex: "configKey", width: 150 },
  { title: t('system.config.configValue'), dataIndex: "configValue", width: 150, ellipsis: true },
  { title: t('system.config.configName'), dataIndex: "configName", width: 120 },
  { title: t('system.config.configDesc'), dataIndex: "configDescription", width: 200, ellipsis: true },
  { title: t('system.config.configType'), dataIndex: "configType", width: 100 },
  { title: t('system.config.groupName'), dataIndex: "groupName", width: 120 },
  { title: t('common.status'), dataIndex: "status", width: 100 },
  { title: t('common.action'), dataIndex: "action", width: 150, fixed: "right" }
])

const configList = ref<SystemConfig[]>([])
const loading = ref(false)
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true
})

const groups = ref<string[]>([])

const modalVisible = ref(false)
const modalTitle = ref("")
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref()

const formData = reactive<SystemConfigForm>({
  configKey: "",
  configValue: "",
  configName: "",
  configDescription: "",
  configType: 1,
  groupName: "",
  sortNum: 0,
  status: 1
})

const configValueBoolean = computed({
  get: () => formData.configValue === "true",
  set: (val: boolean) => {
    formData.configValue = val ? "true" : "false"
  }
})

const formRules = computed(() => ({
  configKey: [{ required: true, message: t('system.config.enterConfigKey'), trigger: 'blur' }],
  configValue: [{ required: true, message: t('system.config.enterConfigValue'), trigger: 'blur' }],
  configName: [{ required: true, message: t('system.config.enterConfigName'), trigger: 'blur' }],
  configType: [{ required: true, message: t('system.config.selectConfigType'), trigger: 'change' }],
  groupName: [{ required: true, message: t('system.config.enterGroupName'), trigger: 'blur' }]
}))

const fetchConfigs = async () => {
  loading.value = true
  try {
    const res = await systemApi.getConfigs(queryParams)
    configList.value = res.list
    pagination.current = res.pageNum
    pagination.pageSize = res.pageSize
    pagination.total = res.total
  } catch (error) {
    message.error(t('system.config.loadFailed'))
  } finally {
    loading.value = false
  }
}

const fetchGroups = async () => {
  try {
    const res = await systemApi.getGroups()
    groups.value = res
  } catch (error) {
    console.error("system.config.getGroupsFailed", error)
  }
}

const getConfigTypeColor = (type: number) => {
  switch (type) {
    case 1: return "blue"
    case 2: return "green"
    case 3: return "orange"
    case 4: return "purple"
    default: return "default"
  }
}

const getConfigTypeText = (type: number) => {
  switch (type) {
    case 1: return t('system.config.text')
    case 2: return t('system.config.number')
    case 3: return t('system.config.boolean')
    case 4: return t('system.config.json')
    default: return t('common.unknown')
  }
}

const handleQuery = () => {
  queryParams.pageNum = 1
  fetchConfigs()
}

const handleReset = () => {
  queryParams.configKey = undefined
  queryParams.configName = undefined
  queryParams.groupName = undefined
  queryParams.status = undefined
  queryParams.pageNum = 1
  fetchConfigs()
}

const handleTableChange = (pag: any) => {
  queryParams.pageNum = pag.current
  queryParams.pageSize = pag.pageSize
  fetchConfigs()
}

const handleCreate = () => {
  isEdit.value = false
  modalTitle.value = t('system.config.newConfig')
  resetForm()
  modalVisible.value = true
}

const handleEdit = async (record: SystemConfig) => {
  isEdit.value = true
  modalTitle.value = t('system.config.editConfig')
  resetForm()

  try {
    const config = await systemApi.getConfig(record.id)
    formData.id = config.id
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
    message.error(t('system.config.getDetailFailed'))
  }
}

const handleDelete = async (id: number) => {
  try {
    await systemApi.deleteConfig(id)
    message.success(t('system.config.deleteSuccess'))
    fetchConfigs()
  } catch (error) {
    message.error(t('system.config.deleteFailed'))
  }
}

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
      message.success(t('system.config.updateSuccess'))
    } else {
      await systemApi.createConfig(formData)
      message.success(t('system.config.createSuccess'))
    }

    modalVisible.value = false
    fetchConfigs()
  } catch (error: any) {
    // 表单验证失败
    if (error && error.errorFields) {
      return
    }
    
    // 后端业务错误
    const errorCode = error?.code
    const errorMessage = error?.message
    
    if (errorCode === 2004) {
      // configKey 唯一性校验失败，始终显示错误消息
      message.error(errorMessage || t('system.config.configKeyExists'))
    } else if (errorCode && errorCode !== 200) {
      // 其他业务错误（拦截器可能已弹出消息，这里确保再次显示）
      message.error(errorMessage || t('common.submitFailed'))
    } else {
      // 未知错误
      message.error(t('common.submitFailed'))
    }
    
    console.error('common.submitFailed', error)
  } finally {
    submitLoading.value = false
  }
}

const handleCancel = () => {
  modalVisible.value = false
  resetForm()
}

const resetForm = () => {
  formData.configKey = ""
  formData.configValue = ""
  formData.configName = ""
  formData.configDescription = ""
  formData.configType = 1
  formData.groupName = ""
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
