<template>
  <div class="log-management">
    <a-card class="page-header" :bordered="false">
      <div class="header-content">
        <div class="header-left">
          <h2 class="page-title">{{ $t("system.log.title") }}</h2>
          <p class="page-description">{{ $t("system.log.titleDesc") }}</p>
        </div>
        <div class="header-actions">
          <a-space>
            <a-button @click="handleExport">
              <template #icon><DownloadOutlined /></template>
              {{ $t("system.log.export") }}
            </a-button>
            <a-popconfirm
              :title="$t('system.log.confirmClearAll')"
              :ok-text="$t('common.confirm')"
              :cancel-text="$t('common.cancel')"
              @confirm="handleClearAll"
            >
              <a-button danger>
                <template #icon><DeleteOutlined /></template>
                {{ $t("system.log.clearLog") }}
              </a-button>
            </a-popconfirm>
          </a-space>
        </div>
      </div>
    </a-card>

    <a-card class="content-card" :bordered="false">
      <div class="table-operations">
        <a-form layout="inline" :model="queryParams" class="search-form">
          <a-form-item :label="$t('system.log.username')">
            <a-input
              v-model:value="queryParams.username"
              :placeholder="$t('system.user.enterUsername')"
              allow-clear
              style="width: 150px"
            />
          </a-form-item>
          <a-form-item :label="$t('system.log.operation')">
            <a-input
              v-model:value="queryParams.operation"
              :placeholder="$t('system.log.enterOperation')"
              allow-clear
              style="width: 150px"
            />
          </a-form-item>
          <a-form-item :label="$t('common.status')">
            <a-select
              v-model:value="queryParams.status"
              :placeholder="$t('form.pleaseSelect')"
              allow-clear
              style="width: 120px"
            >
              <a-select-option :value="1">{{ $t("common.success") || "Success" }}</a-select-option>
              <a-select-option :value="0">Failed</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item :label="$t('data.time')">
            <a-range-picker
              v-model:value="timeRange"
              show-time
              format="YYYY-MM-DD HH:mm:ss"
              style="width: 380px"
            />
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
        :data-source="logList"
        :loading="loading"
        :pagination="pagination"
        @change="handleTableChange"
        row-key="id"
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'status'">
            <a-badge :status="record.status === 1 ? 'success' : 'error'" />
            <span :style="{ color: record.status === 1 ? '#52c41a' : '#ff4d4f' }">
              {{ record.status === 1 ? ($t("common.success") || "Success") : "Failed" }}
            </span>
          </template>

          <template v-if="column.dataIndex === 'duration'">
            <span :style="{ color: record.duration > 1000 ? '#ff4d4f' : record.duration > 500 ? '#faad14' : '#52c41a' }">
              {{ record.duration }} ms
            </span>
          </template>

          <template v-if="column.dataIndex === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleDetail(record)">
                <template #icon><EyeOutlined /></template>
                {{ $t("common.detail") }}
              </a-button>
              <a-popconfirm
                :title="$t('common.confirm')"
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
      v-model:open="detailVisible"
      :title="$t('system.log.logDetail')"
      width="700px"
      :footer="null"
    >
      <a-descriptions :column="2" bordered v-if="currentLog">
        <a-descriptions-item label="common.id">{{ currentLog.id }}</a-descriptions-item>
        <a-descriptions-item :label="$t('common.status')">
          <a-badge :status="currentLog.status === 1 ? 'success' : 'error'" />
          {{ currentLog.status === 1 ? ($t("common.success") || "Success") : "Failed" }}
        </a-descriptions-item>
        <a-descriptions-item :label="$t('system.log.username')">{{ currentLog.username }}</a-descriptions-item>
        <a-descriptions-item :label="$t('system.log.nickname')">{{ currentLog.nickname }}</a-descriptions-item>
        <a-descriptions-item :label="$t('system.log.operation')">{{ currentLog.operation }}</a-descriptions-item>
        <a-descriptions-item label="system.log.duration">{{ currentLog.duration }} ms</a-descriptions-item>
        <a-descriptions-item :label="$t('system.log.method')" :span="2">{{ currentLog.method }}</a-descriptions-item>
        <a-descriptions-item :label="$t('system.log.logParams')" :span="2">
          <pre class="log-params">{{ formatJson(currentLog.params) }}</pre>
        </a-descriptions-item>
        <a-descriptions-item :label="$t('system.log.ip')">{{ currentLog.ip }}</a-descriptions-item>
        <a-descriptions-item :label="$t('system.log.location')">{{ currentLog.location }}</a-descriptions-item>
        <a-descriptions-item :label="$t('system.log.errorMsg')" :span="2" v-if="currentLog.status === 0">
          <span style="color: #ff4d4f">{{ currentLog.errorMsg }}</span>
        </a-descriptions-item>
        <a-descriptions-item :label="$t('common.createdAt')" :span="2">{{ currentLog.createTime }}</a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue"
import { message } from "ant-design-vue"
import {
  DownloadOutlined,
  DeleteOutlined,
  EyeOutlined
} from "@ant-design/icons-vue"
import { logApi, type OperationLog, type LogQuery } from "@/api/log"

const queryParams = reactive<LogQuery>({
  username: undefined,
  operation: undefined,
  status: undefined,
  startTime: undefined,
  endTime: undefined,
  pageNum: 1,
  pageSize: 10
})

const timeRange = ref<[any, any]>()

const columns = [
  { title: $t("common.id"), dataIndex: "id", width: 60 },
  { title: $t("system.log.username"), dataIndex: "username", width: 100 },
  { title: $t("system.log.operation"), dataIndex: "operation", width: 120 },
  { title: $t("system.log.method"), dataIndex: "method", width: 200, ellipsis: true },
  { title: $t("system.log.ip"), dataIndex: "ip", width: 120 },
  { title: $t("common.status"), dataIndex: "status", width: 100 },
  { title: $t("system.log.duration"), dataIndex: "duration", width: 100 },
  { title: $t("common.createdAt"), dataIndex: "createTime", width: 170 },
  { title: $t("common.action"), dataIndex: "action", width: 150, fixed: "right" }
]

const logList = ref<OperationLog[]>([])
const loading = ref(false)
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true
})

const detailVisible = ref(false)
const currentLog = ref<OperationLog>()

const fetchLogs = async () => {
  loading.value = true
  try {
    const res = await logApi.getLogs(queryParams)
    logList.value = res.list
    pagination.current = res.pageNum
    pagination.pageSize = res.pageSize
    pagination.total = res.total
  } catch (error) {
    message.error("system.log.loadFailed")
  } finally {
    loading.value = false
  }
}

const handleQuery = () => {
  if (timeRange.value) {
    queryParams.startTime = timeRange.value[0].format("YYYY-MM-DD HH:mm:ss")
    queryParams.endTime = timeRange.value[1].format("YYYY-MM-DD HH:mm:ss")
  } else {
    queryParams.startTime = undefined
    queryParams.endTime = undefined
  }

  queryParams.pageNum = 1
  fetchLogs()
}

const handleReset = () => {
  queryParams.username = undefined
  queryParams.operation = undefined
  queryParams.status = undefined
  queryParams.startTime = undefined
  queryParams.endTime = undefined
  timeRange.value = undefined
  queryParams.pageNum = 1
  fetchLogs()
}

const handleTableChange = (pag: any) => {
  queryParams.pageNum = pag.current
  queryParams.pageSize = pag.pageSize
  fetchLogs()
}

const handleDetail = (record: OperationLog) => {
  currentLog.value = record
  detailVisible.value = true
}

const handleDelete = async (id: number) => {
  try {
    await logApi.deleteLog(id)
    message.success("system.log.deleteSuccess")
    fetchLogs()
  } catch (error) {
    message.error("system.log.deleteFailed")
  }
}

const handleClearAll = async () => {
  try {
    await logApi.clearLogs()
    message.success("system.log.clearSuccess")
    fetchLogs()
  } catch (error) {
    message.error("system.log.clearFailed")
  }
}

const handleExport = async () => {
  try {
    const res = await logApi.exportLogs(queryParams)
    message.success("system.log.exportSuccess")
    const url = window.URL.createObjectURL(res)
    const link = document.createElement("a")
    link.href = url
    link.download = "operation-log.xlsx"
    link.click()
    window.URL.revokeObjectURL(url)
  } catch (error) {
    message.error("system.log.exportFailed")
  }
}

const formatJson = (jsonStr: string) => {
  try {
    const obj = JSON.parse(jsonStr)
    return JSON.stringify(obj, null, 2)
  } catch {
    return jsonStr
  }
}

onMounted(() => {
  fetchLogs()
})
</script>

<style scoped lang="less">
.log-management {
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

    .log-params {
      background: #1a1d2e;
      padding: 8px;
      border-radius: 4px;
      max-height: 200px;
      overflow-y: auto;
      font-size: 12px;
      color: #a4aab8;
    }
  }
}
</style>
