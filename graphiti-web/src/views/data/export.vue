<template>
  <div class="data-export-page">
    <div class="page-header">
      <h1 class="page-title">数据导出</h1>
      <p class="page-desc">将图谱数据导出为 JSON、CSV 或 Triple 格式</p>
    </div>

    <a-row :gutter="24">
      <a-col :span="12">
        <a-card title="导出配置" class="export-card">
          <a-form :model="form" layout="vertical">
            <a-form-item label="源图谱" required>
              <a-select v-model:value="form.graphId" placeholder="选择源图谱">
                <a-select-option v-for="g in graphOptions" :key="g.id" :value="g.id">{{ g.name }}</a-select-option>
              </a-select>
            </a-form-item>

            <a-form-item label="导出格式" required>
              <a-radio-group v-model:value="form.format">
                <a-radio-button value="json">JSON</a-radio-button>
                <a-radio-button value="csv">CSV</a-radio-button>
                <a-radio-button value="triple">Triple (N-Triples)</a-radio-button>
              </a-radio-group>
            </a-form-item>

            <a-form-item label="导出内容">
              <a-checkbox-group v-model:value="form.content">
                <a-checkbox value="nodes">节点数据</a-checkbox>
                <a-checkbox value="edges">边数据</a-checkbox>
                <a-checkbox value="metadata">元数据</a-checkbox>
              </a-checkbox-group>
            </a-form-item>

            <a-form-item>
              <a-button type="primary" :loading="exporting" @click="executeExport" :disabled="!canExport">
                <ExportOutlined /> 执行导出
              </a-button>
            </a-form-item>
          </a-form>
        </a-card>
      </a-col>

      <a-col :span="12">
        <a-card title="导出历史" class="history-card">
          <a-list :data-source="exportHistory" size="small">
            <template #renderItem="{ item }">
              <a-list-item>
                <div class="export-item">
                  <div class="export-info">
                    <div class="export-title">{{ formatLabel(item.format) }} 导出</div>
                    <div class="export-meta">
                      <a-tag :color="statusColor(item.status)">{{ statusText(item.status) }}</a-tag>
                      <span class="export-time">{{ formatTime(item.createdAt) }}</span>
                    </div>
                    <div v-if="item.fileName" class="export-file">
                      <FileOutlined /> {{ item.fileName }} ({{ formatSize(item.fileSize) }})
                    </div>
                  </div>
                  <a-button v-if="item.status === 'completed'" type="link" size="small" @click="downloadFile(item.id)">
                    <DownloadOutlined /> 下载
                  </a-button>
                </div>
              </a-list-item>
            </template>
            <template #empty>
              <a-empty description="暂无导出记录" />
            </template>
          </a-list>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { ExportOutlined, FileOutlined, DownloadOutlined } from '@ant-design/icons-vue'
import { dataApi, type ExportTask } from '@/api/data'
import { graphApi, type Graph } from '@/api/graph'

const form = reactive({
  graphId: '',
  format: 'json' as 'json' | 'csv' | 'triple',
  content: ['nodes', 'edges'] as string[]
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
    console.error('加载图谱列表失败', err)
  }
}

const loadHistory = async () => {
  try {
    exportHistory.value = await dataApi.getExportHistory(form.graphId || undefined)
  } catch (err) {
    console.error('加载导出历史失败', err)
  }
}

const executeExport = async () => {
  if (!canExport.value) return
  exporting.value = true
  try {
    const task = await dataApi.exportData(form.graphId, form.format)
    if (task.status === 'completed') {
      message.success(`导出成功：${task.fileName}`)
    } else {
      message.info('导出任务已提交')
    }
  } catch (err: any) {
    message.error(err.message || '导出失败')
  } finally {
    exporting.value = false
    // 无论成功或失败，都刷新历史记录（操作日志已记录）
    await loadHistory()
  }
}

const downloadFile = async (taskId: string) => {
  try {
    const blob = await dataApi.downloadExport(taskId)
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `export-${taskId}.json`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    window.URL.revokeObjectURL(url)
    message.success('下载已开始')
  } catch (err: any) {
    message.error(err.message || '下载失败')
  }
}

const formatLabel = (fmt: string) => {
  const map: Record<string, string> = { json: 'JSON', csv: 'CSV', triple: 'Triple' }
  return map[fmt] || fmt
}

const statusColor = (status: string) => {
  const map: Record<string, string> = { completed: 'success', failed: 'error', pending: 'default', processing: 'processing' }
  return map[status] || 'default'
}

const statusText = (status: string) => {
  const map: Record<string, string> = { completed: '完成', failed: '失败', pending: '待处理', processing: '处理中' }
  return map[status] || status
}

const formatTime = (time?: string) => {
  if (!time) return ''
  return new Date(time).toLocaleString('zh-CN')
}

const formatSize = (size?: number) => {
  if (!size) return ''
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
@import '@/assets/styles/dark.less';

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
