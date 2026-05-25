<template>
  <div class="data-import-page">
    <div class="page-header">
      <h1 class="page-title">{{ $t("data.import") }}</h1>
      <p class="page-desc">{{ $t("data.importDesc") }}</p>
    </div>

    <a-row :gutter="24">
      <a-col :span="16">
        <a-card :title="$t('data.importConfig')" class="import-card">
          <a-form :model="form" layout="vertical">
            <a-form-item :label="$t('data.targetGraph')" required>
              <a-select v-model:value="form.graphId" :placeholder="$t('data.selectTargetGraph')">
                <a-select-option v-for="g in graphOptions" :key="g.id" :value="g.id">{{ g.name }}</a-select-option>
              </a-select>
            </a-form-item>

            <a-form-item :label="$t('data.dataFormat')" required>
              <a-radio-group v-model:value="form.format">
                <a-radio-button value="json">JSON</a-radio-button>
                <a-radio-button value="csv">CSV</a-radio-button>
                <a-radio-button value="triple">Triple (N-Triples)</a-radio-button>
              </a-radio-group>
            </a-form-item>

            <a-form-item :label="$t('data.dataContent')" required>
              <a-tabs v-model:activeKey="inputMode">
                <a-tab-pane key="text" :tab="$t('data.textInput')">
                  <a-textarea
                    v-model:value="form.data"
                    :rows="12"
                    :placeholder="placeholderText"
                    class="data-textarea"
                  />
                </a-tab-pane>
                <a-tab-pane key="file" :tab="$t('data.fileUpload')">
                  <a-upload-dragger
                    :show-upload-list="false"
                    :before-upload="handleFileUpload"
                    accept=".json,.csv,.nt,.txt"
                  >
                    <p class="ant-upload-drag-icon">
                      <InboxOutlined />
                    </p>
                    <p class="ant-upload-text">{{ $t("data.clickOrDrag") }}</p>
                    <p class="ant-upload-hint">{{ $t("data.supportedFormats") }}</p>
                  </a-upload-dragger>
                  <div v-if="uploadedFileName" class="file-info">
                    <FileOutlined /> {{ uploadedFileName }}
                    <a-button type="link" size="small" @click="clearFile">{{ $t("data.clear") }}</a-button>
                  </div>
                </a-tab-pane>
              </a-tabs>
            </a-form-item>

            <a-form-item>
              <a-space>
                <a-button type="primary" :loading="previewLoading" @click="previewData" :disabled="!canPreview">
                  <EyeOutlined /> {{ $t("data.previewData") }}
                </a-button>
                <a-button type="primary" :loading="importLoading" @click="executeImport" :disabled="!canImport">
                  <ImportOutlined /> {{ $t("data.executeImport") }}
                </a-button>
              </a-space>
            </a-form-item>
          </a-form>
        </a-card>
      </a-col>

      <a-col :span="8">
        <a-card :title="$t('data.importHistory')" class="history-card">
          <a-timeline>
            <a-timeline-item
              v-for="task in importHistory"
              :key="task.id"
              :color="task.status === 'completed' ? 'green' : task.status === 'failed' ? 'red' : 'blue'"
            >
              <div class="history-item">
                <div class="history-title">{{ formatLabel(task.format) }} {{ $t("data.import") }}</div>
                <div class="history-meta">
                  <a-tag :color="statusColor(task.status)">{{ statusText(task.status) }}</a-tag>
                  <span class="history-time">{{ formatTime(task.createdAt) }}</span>
                </div>
                <div v-if="task.status === 'completed'" class="history-detail">
                  {{ $t("data.rowsImported", { processed: task.processedRows, total: task.totalRows }) }}
                </div>
                <div v-if="task.status === 'failed' && task.errorMessage" class="history-error">
                  {{ task.errorMessage }}
                </div>
              </div>
            </a-timeline-item>
          </a-timeline>
          <a-empty v-if="!importHistory.length" :description="$t('data.noHistory')" />
        </a-card>
      </a-col>
    </a-row>

    <a-modal v-model:open="previewVisible" :title="$t('data.previewData')" width="900px" :footer="null">
      <a-table
        :columns="previewColumns"
        :data-source="previewDataList"
        :pagination="{ pageSize: 5 }"
        size="small"
      />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from "vue"
import { message } from "ant-design-vue"
import { InboxOutlined, FileOutlined, EyeOutlined, ImportOutlined } from "@ant-design/icons-vue"
import { dataApi, type ImportTask, type EntityItem } from "@/api/data"
import { graphApi, type Graph } from "@/api/graph"

const form = reactive({
  graphId: "",
  format: "json" as "json" | "csv" | "triple",
  data: ""
})

const inputMode = ref("text")
const uploadedFileName = ref("")
const graphOptions = ref<Graph[]>([])

const previewLoading = ref(false)
const importLoading = ref(false)
const previewVisible = ref(false)
const previewDataList = ref<EntityItem[]>([])
const importHistory = ref<ImportTask[]>([])

const placeholderText = computed(() => {
  switch (form.format) {
    case "json":
      return '{\n  "nodes": [\n    { "name": "Zhang San", "type": "Person", "properties": { "age": 30 } }\n  ],\n  "edges": []\n}'
    case "csv":
      return "name,type,age,email\nZhang San,Person,30,zhangsan@example.com\nLi Si,Person,28,lisi@example.com"
    case "triple":
      return '<http://example.org/person/1> <http://example.org/name> "Zhang San" .\n<http://example.org/person/1> <http://example.org/age> "30"^^<http://www.w3.org/2001/XMLSchema#integer> .'
    default:
      return ""
  }
})

const canPreview = computed(() => form.graphId && form.data.trim().length > 0)
const canImport = computed(() => form.graphId && form.data.trim().length > 0)

const loadGraphs = async () => {
  try {
    graphOptions.value = await graphApi.getList()
    if (graphOptions.value.length > 0 && !form.graphId) {
      form.graphId = graphOptions.value[0].graphId
    }
  } catch (err) {
    console.error("data.loadFailed", err)
  }
}

const loadHistory = async () => {
  try {
    importHistory.value = await dataApi.getImportHistory(form.graphId || undefined)
  } catch (err) {
    console.error("data.loadHistoryFailed", err)
  }
}

const handleFileUpload = (file: File) => {
  const reader = new FileReader()
  reader.onload = (e) => {
    form.data = e.target?.result as string
    uploadedFileName.value = file.name
    inputMode.value = "text"
    message.success("data.fileRead: " + file.name)
  }
  reader.readAsText(file)
  return false
}

const clearFile = () => {
  uploadedFileName.value = ""
  form.data = ""
}

const previewData = async () => {
  if (!canPreview.value) return
  previewLoading.value = true
  try {
    previewDataList.value = await dataApi.previewImport(form.graphId, form.format, form.data)
    previewVisible.value = true
  } catch (err: any) {
    message.error(err.message || "data.previewFailed")
  } finally {
    previewLoading.value = false
  }
}

const executeImport = async () => {
  if (!canImport.value) return
  importLoading.value = true
  try {
    const task = await dataApi.importData(form.graphId, form.format, form.data)
    if (task.status === "completed") {
      message.success("data.importSuccess: " + task.processedRows)
    } else {
      message.warning("data.importProcessing: " + task.totalRows)
    }
    form.data = ""
    uploadedFileName.value = ""
  } catch (err: any) {
    message.error(err.message || "data.importFailed")
  } finally {
    importLoading.value = false
    await loadHistory()
  }
}

const formatLabel = (fmt: string) => {
  const map: Record<string, string> = { json: "JSON", csv: "CSV", triple: "Triple" }
  return map[fmt] || fmt
}

const statusColor = (status: string) => {
  const map: Record<string, string> = { completed: "success", failed: "error", pending: "default", processing: "processing" }
  return map[status] || "default"
}

const statusText = (status: string) => {
  const map: Record<string, string> = { completed: "data.completed", failed: "data.failed", pending: "data.pending", processing: "data.processing" }
  return map[status] || status
}

const formatTime = (time?: string) => {
  if (!time) return ""
  return new Date(time).toLocaleString()
}

const previewColumns = [
  { title: "data.uuid", dataIndex: "uuid", key: "uuid" },
  { title: "common.name", dataIndex: "name", key: "name" },
  { title: "common.type", dataIndex: "type", key: "type" },
  { title: "data.properties", key: "properties", customRender: ({ text }: any) => JSON.stringify(text).slice(0, 80) }
]

onMounted(() => {
  loadGraphs()
  loadHistory()
})
</script>

<style scoped lang="less">
@import "@/assets/styles/dark.less";

.data-import-page {
  padding: 24px;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: @text-primary;
  margin-bottom: 4px;
}

.page-desc {
  color: @text-secondary;
  font-size: 14px;
}

.import-card, .history-card {
  background: @bg-container;
  border: 1px solid @border-color;
}

.data-textarea {
  font-family: "Courier New", monospace;
  font-size: 13px;
}

.file-info {
  margin-top: 12px;
  padding: 8px 12px;
  background: @bg-elevated;
  border-radius: @border-radius-md;
  color: @text-secondary;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.history-item {
  .history-title {
    font-weight: 500;
    color: @text-primary;
    margin-bottom: 4px;
  }
  .history-meta {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 4px;
  }
  .history-time {
    color: @text-tertiary;
    font-size: 12px;
  }
  .history-detail {
    color: @text-secondary;
    font-size: 13px;
  }
  .history-error {
    color: @error-color;
    font-size: 12px;
  }
}

:deep(.ant-upload-drag) {
  background: @bg-elevated;
  border-color: @border-color;
}

:deep(.ant-timeline-item-tail) {
  border-left-color: @border-color;
}
</style>
