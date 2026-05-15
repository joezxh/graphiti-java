<template>
  <div class="data-export-page">
    <div class="page-header">
      <h1 class="page-title">{{ $t("data.export") }}</h1>
      <p class="page-desc">{{ $t("data.exportDesc") }}</p>
    </div>

    <a-row :gutter="24">
      <a-col :span="12">
        <a-card :title="$t('data.importConfig')" class="export-card">
          <a-form :model="form" layout="vertical">
            <a-form-item label="Source Graph" required>
              <a-select v-model:value="form.graphId" :placeholder="$t('data.selectTargetGraph')">
                <a-select-option v-for="g in graphOptions" :key="g.id" :value="g.id">{{ g.name }}</a-select-option>
              </a-select>
            </a-form-item>

            <a-form-item label="Export Format" required>
              <a-radio-group v-model:value="form.format">
                <a-radio-button value="json">JSON</a-radio-button>
                <a-radio-button value="csv">CSV</a-radio-button>
                <a-radio-button value="triple">Triple (N-Triples)</a-radio-button>
              </a-radio-group>
            </a-form-item>

            <a-form-item label="Export Content">
              <a-checkbox-group v-model:value="form.content">
                <a-checkbox value="nodes">{{ $t("data.nodes") || "Node Data" }}</a-checkbox>
                <a-checkbox value="edges">{{ $t("data.edges") || "Edge Data" }}</a-checkbox>
                <a-checkbox value="metadata">Metadata</a-checkbox>
              </a-checkbox-group>
            </a-form-item>

            <a-form-item>
              <a-button type="primary" :loading="exporting" @click="executeExport" :disabled="!canExport">
                <ExportOutlined /> {{ $t("data.executeImport") || "Execute Export" }}
              </a-button>
            </a-form-item>
          </a-form>
        </a-card>
      </a-col>

      <a-col :span="12">
        <a-card :title="$t('data.importHistory')" class="history-card">
          <a-list :data-source="exportHistory" size="small">
            <template #renderItem="{ item }">
              <a-list-item>
                <div class="export-item">
                  <div class="export-info">
                    <div class="export-title">{{ formatLabel(item.format) }} {{ $t("data.export") }}</div>
                    <div class="export-meta">
                      <a-tag :color="statusColor(item.status)">{{ statusText(item.status) }}</a-tag>
                      <span class="export-time">{{ formatTime(item.createdAt) }}</span>
                    </div>
                    <div v-if="item.fileName" class="export-file">
                      <FileOutlined /> {{ item.fileName }} ({{ formatSize(item.fileSize) }})
                    </div>
                  </div>
                  <a-button v-if="item.status === 'completed'" type="link" size="small" @click="downloadFile(item.id)">
                    <DownloadOutlined /> Download
                  </a-button>
                </div>
              </a-list-item>
            </template>
            <template #empty>
              <a-empty :description="$t('data.noHistory')" />
            </template>
          </a-list>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from "vue"
import { message } from "ant-design-vue"
import { ExportOutlined, FileOutlined, DownloadOutlined } from "@ant-design/icons-vue"
import { dataApi, type ExportTask } from "@/api/data"
import { graphApi, type Graph } from "@/api/graph"

const form = reactive({
  graphId: "",
  format: "json" as "json" | "csv" | "triple",
  content: ["nodes", "edges"] as string[]
})

const graphOptions = ref<Graph[]>([])
const exporting = ref(false)
const exportHistory = ref<ExportTask[]>([])

const canExport = computed(() => form.graphId && form.content.length > 0)

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
    exportHistory.value = await dataApi.getExportHistory(form.graphId || undefined)
  } catch (err) {
    console.error("data.loadHistoryFailed", err)
  }
}

const executeExport = async () => {
  if (!canExport.value) return
  exporting.value = true
  try {
    const task = await dataApi.exportData(form.graphId, form.format)
    if (task.status === "completed") {
      message.success("data.exportSuccess: " + task.fileName)
    } else {
      message.info("data.exportProcessing")
    }
  } catch (err: any) {
    message.error(err.message || "data.exportFailed")
  } finally {
    exporting.value = false
    await loadHistory()
  }
}

const downloadFile = async (taskId: string) => {
  try {
    const blob = await dataApi.downloadExport(taskId)
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement("a")
    a.href = url
    a.download = `export-${taskId}.json`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
    message.success("common.success")
  } catch (err: any) {
    message.error(err.message || "common.error")
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

const formatSize = (size?: number) => {
  if (!size) return ""
  if (size < 1024) return `${size} B`
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`
  return `${(size / (1024 * 1024)).toFixed(2)} MB`
}

onMounted(() => {
  loadGraphs()
  loadHistory()
})
</script>

<style scoped lang="less">
@import "@/assets/styles/dark.less";

.data-export-page {
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

.export-card, .history-card {
  background: @bg-container;
  border: 1px solid @border-color;
}

.export-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.export-info {
  flex: 1;
}

.export-title {
  font-weight: 500;
  color: @text-primary;
  margin-bottom: 4px;
}

.export-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.export-time {
  color: @text-tertiary;
  font-size: 12px;
}

.export-file {
  color: @text-secondary;
  font-size: 12px;
}
</style>
