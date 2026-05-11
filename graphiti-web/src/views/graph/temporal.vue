<template>
  <div class="temporal-page">
    <div class="page-header">
      <h1 class="page-title">时序历史</h1>
      <p class="page-desc">查看图谱在不同时间点的状态与事实</p>
    </div>

    <a-card class="filter-card">
      <a-row :gutter="16" align="middle">
        <a-col :span="6">
          <a-select
            v-model:value="selectedGraphId"
            placeholder="选择图谱"
            style="width: 100%"
            allow-clear
            @change="onGraphChange"
          >
            <a-select-option v-for="g in graphOptions" :key="g.graphId" :value="g.graphId">
              {{ g.name }}
            </a-select-option>
          </a-select>
        </a-col>
        <a-col :span="4">
          <a-select v-model:value="viewMode" style="width: 100%">
            <a-select-option value="current">当前事实</a-select-option>
            <a-select-option value="time">指定时间</a-select-option>
            <a-select-option value="history">实体历史</a-select-option>
          </a-select>
        </a-col>
        <a-col v-if="viewMode === 'time'" :span="8">
          <a-date-picker
            v-model:value="selectedTime"
            show-time
            format="YYYY-MM-DD HH:mm:ss"
            placeholder="选择时间点"
            style="width: 100%"
            @change="loadFactsAtTime"
          />
        </a-col>
        <a-col v-if="viewMode === 'history'" :span="6">
          <a-input-search
            v-model:value="historyEntityName"
            placeholder="输入实体名称"
            @search="loadEntityHistory"
          />
        </a-col>
        <a-col :span="4">
          <a-button @click="refresh" :loading="loading">
            <ReloadOutlined /> 刷新
          </a-button>
        </a-col>
      </a-row>
    </a-card>

    <!-- 当前事实视图 -->
    <div v-if="viewMode === 'current'">
      <a-row :gutter="16" class="result-area">
        <a-col :span="12">
          <a-card title="当前有效的边" class="result-card">
            <a-table
              :columns="factColumns"
              :data-source="currentFacts"
              :loading="loading"
              :pagination="{ pageSize: 10 }"
              size="small"
              row-key="uuid"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'source'">
                  <span class="node-name">{{ record.edge?.sourceNodeUuid }}</span>
                </template>
                <template v-if="column.key === 'fact'">
                  <span class="fact-text">{{ record.edge?.fact || record.edge?.name }}</span>
                </template>
                <template v-if="column.key === 'target'">
                  <span class="node-name">{{ record.edge?.targetNodeUuid }}</span>
                </template>
                <template v-if="column.key === 'validAt'">
                  {{ formatDate(record.validAt) }}
                </template>
              </template>
            </a-table>
          </a-card>
        </a-col>
        <a-col :span="12">
          <a-card title="图谱时间轴" class="timeline-card">
            <a-empty v-if="!timelineEvents.length" description="暂无时间事件" />
            <a-timeline v-else>
              <a-timeline-item
                v-for="event in timelineEvents"
                :key="event.time"
                :color="event.type === 'valid' ? 'green' : 'red'"
              >
                <div class="timeline-event">
                  <div class="event-time">{{ formatDate(event.time) }}</div>
                  <div class="event-desc">{{ event.desc }}</div>
                </div>
              </a-timeline-item>
            </a-timeline>
          </a-card>
        </a-col>
      </a-row>
    </div>

    <!-- 指定时间视图 -->
    <div v-if="viewMode === 'time'">
      <a-row :gutter="16" class="result-area">
        <a-col :span="16">
          <a-card title="该时间点的事实" class="result-card">
            <a-table
              :columns="factColumns"
              :data-source="factsAtTime"
              :loading="loading"
              :pagination="{ pageSize: 10 }"
              size="small"
              row-key="uuid"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'source'">
                  <span>{{ record.edge?.sourceNodeUuid }}</span>
                </template>
                <template v-if="column.key === 'fact'">
                  <span>{{ record.edge?.fact || record.edge?.name }}</span>
                </template>
                <template v-if="column.key === 'target'">
                  <span>{{ record.edge?.targetNodeUuid }}</span>
                </template>
                <template v-if="column.key === 'validAt'">
                  {{ formatDate(record.validAt) }} ~ {{ formatDate(record.invalidAt) }}
                </template>
              </template>
            </a-table>
          </a-card>
        </a-col>
        <a-col :span="8">
          <a-card title="时间点" class="time-card">
            <div class="time-display">
              <div class="time-label">查询时间</div>
              <div class="time-value">{{ selectedTime ? formatDate(selectedTime.valueOf()) : '-' }}</div>
            </div>
          </a-card>
        </a-col>
      </a-row>
    </div>

    <!-- 实体历史视图 -->
    <div v-if="viewMode === 'history'">
      <a-card title="实体版本历史" class="result-card">
        <a-table
          :columns="historyColumns"
          :data-source="entityHistory"
          :loading="loading"
          :pagination="{ pageSize: 10 }"
          size="small"
          row-key="uuid"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'name'">
              <span class="node-name">{{ record.name }}</span>
            </template>
            <template v-if="column.key === 'type'">
              <a-tag color="blue">{{ record.type || '-' }}</a-tag>
            </template>
            <template v-if="column.key === 'validPeriod'">
              {{ formatDate(record.validAt) }} ~ {{ formatDate(record.invalidAt) }}
            </template>
          </template>
        </a-table>
      </a-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { ReloadOutlined } from '@ant-design/icons-vue'
import { graphApi } from '@/api/graph'
import { temporalApi, type TemporalFact, type EntityHistoryItem } from '@/api/temporal'

const graphOptions = ref<any[]>([])
const selectedGraphId = ref<string | undefined>(undefined)
const viewMode = ref<'current' | 'time' | 'history'>('current')
const selectedTime = ref<any>(null)
const historyEntityName = ref('')
const loading = ref(false)

const currentFacts = ref<TemporalFact[]>([])
const factsAtTime = ref<TemporalFact[]>([])
const entityHistory = ref<EntityHistoryItem[]>([])

const timelineEvents = reactive<Array<{ time: string; desc: string; type: 'valid' | 'invalid' }>>([])

const factColumns = [
  { title: '源节点', key: 'source', width: 150, ellipsis: true },
  { title: '关系/事实', key: 'fact', ellipsis: true },
  { title: '目标节点', key: 'target', width: 150, ellipsis: true },
  { title: '有效期', key: 'validAt', width: 200 }
]

const historyColumns = [
  { title: '名称', key: 'name' },
  { title: '类型', key: 'type', width: 120 },
  { title: '有效期', key: 'validPeriod', width: 220 },
  { title: '创建时间', key: 'createdAt', width: 180 }
]

const loadGraphs = async () => {
  try {
    graphOptions.value = await graphApi.getList()
  } catch (err) {
    console.error('加载图谱列表失败', err)
  }
}

const loadCurrentFacts = async () => {
  if (!selectedGraphId.value) return
  loading.value = true
  try {
    currentFacts.value = await temporalApi.getCurrentFacts(selectedGraphId.value)
    buildTimeline(currentFacts.value)
  } catch (err: any) {
    message.error(err.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const loadFactsAtTime = async () => {
  if (!selectedGraphId.value || !selectedTime.value) return
  loading.value = true
  try {
    const ts = selectedTime.value.valueOf()
    factsAtTime.value = await temporalApi.getFactsAt(selectedGraphId.value, ts)
  } catch (err: any) {
    message.error(err.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const loadEntityHistory = async () => {
  if (!selectedGraphId.value || !historyEntityName.value.trim()) return
  loading.value = true
  try {
    entityHistory.value = await temporalApi.getEntityHistory(
      selectedGraphId.value,
      historyEntityName.value.trim()
    )
  } catch (err: any) {
    message.error(err.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const buildTimeline = (facts: TemporalFact[]) => {
  timelineEvents.length = 0
  const events = new Set<string>()
  for (const f of facts) {
    if (f.validAt) events.add(`valid|${f.validAt}|${f.edge?.fact || '事实生效'}`)
    if (f.invalidAt) events.add(`invalid|${f.invalidAt}|${f.edge?.fact || '事实失效'}`)
  }
  for (const e of Array.from(events).sort()) {
    const [type, time, desc] = e.split('|')
    timelineEvents.push({ time, desc, type: type as 'valid' | 'invalid' })
  }
}

const onGraphChange = () => {
  refresh()
}

const refresh = () => {
  if (viewMode.value === 'current') loadCurrentFacts()
  else if (viewMode.value === 'time' && selectedTime.value) loadFactsAtTime()
}

const formatDate = (date: string | undefined): string => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

onMounted(() => {
  loadGraphs()
})
</script>

<style scoped lang="less">
@import '@/assets/styles/dark.less';

.temporal-page {
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

.filter-card {
  background: @bg-container;
  border: 1px solid @border-color;
  margin-bottom: 16px;
}

.result-area {
  margin-bottom: 16px;
}

.result-card, .timeline-card, .time-card {
  background: @bg-container;
  border: 1px solid @border-color;
}

.node-name {
  font-family: monospace;
  font-size: 12px;
  color: @primary-color;
}

.fact-text {
  color: @text-secondary;
  font-size: 13px;
}

.timeline-event {
  .event-time {
    font-size: 12px;
    color: @text-tertiary;
    font-family: monospace;
  }
  .event-desc {
    color: @text-secondary;
    font-size: 13px;
  }
}

.time-display {
  text-align: center;
  padding: 16px;

  .time-label {
    color: @text-tertiary;
    font-size: 12px;
    margin-bottom: 8px;
  }

  .time-value {
    font-size: 18px;
    font-weight: 600;
    color: @primary-color;
    font-family: monospace;
  }
}
</style>
