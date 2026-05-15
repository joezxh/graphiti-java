<template>
  <div class="monitor-page">
    <a-row :gutter="16" class="stats-row">
      <a-col :span="6">
        <a-card class="stats-card" :bordered="false">
          <div class="stats-content">
            <div class="stats-header">
              <span class="stats-title">{{ $t('monitor.cpuUsage') }}</span>
              <DashboardOutlined class="stats-icon" />
            </div>
            <div class="stats-value" :style="{ color: getCpuColor(systemStatus?.cpuUsage) }">
              {{ systemStatus?.cpuUsage.toFixed(1) || '--' }}%
            </div>
            <a-progress
              :percent="systemStatus?.cpuUsage"
              :stroke-color="getCpuColor(systemStatus?.cpuUsage)"
              :format="() => ''"
              size="small"
            />
          </div>
        </a-card>
      </a-col>
      
      <a-col :span="6">
        <a-card class="stats-card" :bordered="false">
          <div class="stats-content">
            <div class="stats-header">
              <span class="stats-title">{{ $t('monitor.memoryUsage') }}</span>
              <ClockCircleOutlined class="stats-icon" />
            </div>
            <div class="stats-value" :style="{ color: getMemoryColor(systemStatus?.memoryUsage) }">
              {{ systemStatus?.memoryUsage.toFixed(1) || '--' }}%
            </div>
            <a-progress
              :percent="systemStatus?.memoryUsage"
              :stroke-color="getMemoryColor(systemStatus?.memoryUsage)"
              :format="() => ''"
              size="small"
            />
          </div>
        </a-card>
      </a-col>
      
      <a-col :span="6">
        <a-card class="stats-card" :bordered="false">
          <div class="stats-content">
            <div class="stats-header">
              <span class="stats-title">{{ $t('monitor.diskUsage') }}</span>
              <DatabaseOutlined class="stats-icon" />
            </div>
            <div class="stats-value" :style="{ color: getDiskColor(systemStatus?.diskUsage) }">
              {{ systemStatus?.diskUsage.toFixed(1) || '--' }}%
            </div>
            <a-progress
              :percent="systemStatus?.diskUsage"
              :stroke-color="getDiskColor(systemStatus?.diskUsage)"
              :format="() => ''"
              size="small"
            />
          </div>
        </a-card>
      </a-col>
      
      <a-col :span="6">
        <a-card class="stats-card" :bordered="false">
          <div class="stats-content">
            <div class="stats-header">
              <span class="stats-title">{{ $t('monitor.uptime') }}</span>
              <CheckCircleOutlined class="stats-icon" />
            </div>
            <div class="stats-value" style="font-size: 20px">
              {{ formatUptime(systemStatus?.uptime) || '--' }}
            </div>
            <div class="stats-footer">
              {{ $t('monitor.currentTime') }}: {{ systemStatus?.currentTime || '--' }}
            </div>
          </div>
        </a-card>
      </a-col>
    </a-row>
    
    <a-row :gutter="16" class="charts-row">
      <a-col :span="18">
        <a-card class="chart-card" :bordered="false">
          <template #title>{{ $t('monitor.performanceTrend') }}</template>
          <template #extra>
            <a-radio-group v-model:value="timeRange" button-style="solid" size="small">
              <a-radio-button value="1h">{{ $t('monitor.oneHour') }}</a-radio-button>
              <a-radio-button value="6h">{{ $t('monitor.sixHours') }}</a-radio-button>
              <a-radio-button value="24h">{{ $t('monitor.twentyFourHours') }}</a-radio-button>
              <a-radio-button value="7d">{{ $t('monitor.sevenDays') }}</a-radio-button>
            </a-radio-group>
          </template>
          
          <div ref="performanceChart" class="chart-container"></div>
        </a-card>
      </a-col>
      
      <a-col :span="6">
        <a-card class="status-card" :bordered="false">
          <template #title>{{ $t('monitor.serviceStatus') }}</template>
          
          <div class="service-status">
            <div class="service-item">
              <div class="service-name">
                <span class="service-dot" :class="systemStatus?.neo4jStatus"></span>
                {{ $t('monitor.neo4jDatabase') }}
              </div>
              <a-tag :color="systemStatus?.neo4jStatus === 'healthy' ? 'success' : 'error'">
                {{ systemStatus?.neo4jStatus === 'healthy' ? $t('monitor.healthy') : $t('monitor.unhealthy') }}
              </a-tag>
            </div>
            
            <div class="service-item">
              <div class="service-name">
                <span class="service-dot" :class="systemStatus?.mysqlStatus"></span>
                {{ $t('monitor.mysqlDatabase') }}
              </div>
              <a-tag :color="systemStatus?.mysqlStatus === 'healthy' ? 'success' : 'error'">
                {{ systemStatus?.mysqlStatus === 'healthy' ? $t('monitor.healthy') : $t('monitor.unhealthy') }}
              </a-tag>
            </div>
            
            <div class="service-item">
              <div class="service-name">
                <span class="service-dot" :class="systemStatus?.redisStatus"></span>
                {{ $t('monitor.redisCache') }}
              </div>
              <a-tag :color="systemStatus?.redisStatus === 'healthy' ? 'success' : 'error'">
                {{ systemStatus?.redisStatus === 'healthy' ? $t('monitor.healthy') : $t('monitor.unhealthy') }}
              </a-tag>
            </div>
          </div>
          
          <a-divider />
          
          <div class="database-info">
            <div class="info-title">{{ $t('monitor.dbInfo') }}</div>
            
            <div class="info-item">
              <span class="info-label">{{ $t('monitor.neo4jVersion') }}</span>
              <span class="info-value">5.0.0</span>
            </div>
            
            <div class="info-item">
              <span class="info-label">{{ $t('monitor.nodeCount') }}</span>
              <span class="info-value">1,250</span>
            </div>
            
            <div class="info-item">
              <span class="info-label">{{ $t('monitor.relationCount') }}</span>
              <span class="info-value">3,680</span>
            </div>
          </div>
        </a-card>
      </a-col>
    </a-row>
    
    <a-card class="api-log-card" :bordered="false">
      <template #title>{{ $t('monitor.apiLogs') }}</template>
      
      <a-table
        :data-source="apiLogs"
        :columns="columns"
        :pagination="false"
        :loading="loading"
        size="small"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'method'">
            <a-tag :color="getMethodColor(record.method)">
              {{ record.method }}
            </a-tag>
          </template>
          
          <template v-if="column.dataIndex === 'statusCode'">
            <span :style="{ color: record.statusCode === 200 ? '#52c41a' : '#ff4d4f' }">
              {{ record.statusCode }}
            </span>
          </template>
          
          <template v-if="column.dataIndex === 'responseTime'">
            <span :style="{ color: record.responseTime > 500 ? '#ff4d4f' : record.responseTime > 200 ? '#faad14' : '#52c41a' }">
              {{ record.responseTime }} ms
            </span>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, onUnmounted, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  DashboardOutlined,
  ClockCircleOutlined,
  DatabaseOutlined,
  CheckCircleOutlined
} from '@ant-design/icons-vue'
import { monitorApi, type SystemStatus, type PerformanceMetrics, type ApiLog } from '@/api/monitor'
import * as echarts from 'echarts'

const { t } = useI18n()

// System status
const systemStatus = ref<SystemStatus>()

// 性能指标
const performanceMetrics = ref<PerformanceMetrics[]>([])

// API日志
const apiLogs = ref<ApiLog[]>([])
const loading = ref(false)

// 时间范围
const timeRange = ref<string>('24h')

// 性能图表
const performanceChart = ref<HTMLElement>()
let chartInstance: echarts.ECharts | null = null

// Table column definitions
const columns = [
  {
    title: t('monitor.method'),
    dataIndex: 'method',
    width: 80
  },
  {
    title: t('monitor.path'),
    dataIndex: 'path',
    width: 200,
    ellipsis: true
  },
  {
    title: t('monitor.statusCode'),
    dataIndex: 'statusCode',
    width: 80
  },
  {
    title: t('monitor.responseTime'),
    dataIndex: 'responseTime',
    width: 100
  },
  {
    title: t('monitor.ipAddress'),
    dataIndex: 'ip',
    width: 120
  },
  {
    title: t('monitor.timestamp'),
    dataIndex: 'timestamp',
    width: 170
  }
]

// Fetch system status
const fetchSystemStatus = async () => {
  try {
    const res = await monitorApi.getSystemStatus()
    systemStatus.value = res
  } catch (error) {
    console.error(t('monitor.fetchStatusFailed'), error)
  }
}

// Fetch performance metrics
const fetchPerformanceMetrics = async () => {
  try {
    const res = await monitorApi.getPerformanceMetrics(timeRange.value)
    performanceMetrics.value = res
    updatePerformanceChart()
  } catch (error) {
    console.error(t('monitor.fetchMetricsFailed'), error)
  }
}

// Fetch API logs
const fetchApiLogs = async () => {
  loading.value = true
  try {
    const res = await monitorApi.getApiLogs()
    apiLogs.value = res.list
  } catch (error) {
    console.error(t('monitor.fetchLogsFailed'), error)
  } finally {
    loading.value = false
  }
}

// 更新性能图表
const updatePerformanceChart = () => {
  if (!chartInstance || !performanceMetrics.value.length) return
  
  const timestamps = performanceMetrics.value.map(m => {
    const date = new Date(m.timestamp)
    return timeRange.value === '7d' 
      ? date.toLocaleDateString() 
      : date.toLocaleTimeString()
  })
  
  const cpuData = performanceMetrics.value.map(m => m.cpuUsage)
  const memoryData = performanceMetrics.value.map(m => m.memoryUsage)
  const diskData = performanceMetrics.value.map(m => m.diskUsage)
  
  const option = {
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(13, 17, 28, 0.9)',
      borderColor: 'rgba(94, 106, 210, 0.3)',
      textStyle: {
        color: '#a4aab8'
      }
    },
    legend: {
      data: [t('monitor.cpuUsage'), t('monitor.memoryUsage'), t('monitor.diskUsage')],
      textStyle: {
        color: '#a4aab8'
      },
      top: 10
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '60px',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: timestamps,
      axisLine: {
        lineStyle: {
          color: 'rgba(94, 106, 210, 0.2)'
        }
      },
      axisLabel: {
        color: '#8a8f98'
      }
    },
    yAxis: {
      type: 'value',
      axisLine: {
        lineStyle: {
          color: 'rgba(94, 106, 210, 0.2)'
        }
      },
      axisLabel: {
        color: '#8a8f98'
      },
      splitLine: {
        lineStyle: {
          color: 'rgba(94, 106, 210, 0.1)'
        }
      }
    },
    series: [
      {
        name: computed(() => t('monitor.cpuUsage')).value,
        type: 'line',
        smooth: true,
        data: cpuData,
        lineStyle: {
          color: '#5e6ad2'
        },
        itemStyle: {
          color: '#5e6ad2'
        }
      },
      {
        name: computed(() => t('monitor.memoryUsage')).value,
        type: 'line',
        smooth: true,
        data: memoryData,
        lineStyle: {
          color: '#36cfc9'
        },
        itemStyle: {
          color: '#36cfc9'
        }
      },
      {
        name: computed(() => t('monitor.diskUsage')).value,
        type: 'line',
        smooth: true,
        data: diskData,
        lineStyle: {
          color: '#ffc53d'
        },
        itemStyle: {
          color: '#ffc53d'
        }
      }
    ]
  }
  
  chartInstance.setOption(option)
}

// 初始化性能图表
const initPerformanceChart = () => {
  if (!performanceChart.value) return
  
  chartInstance = echarts.init(performanceChart.value, null, { renderer: 'canvas' })
  updatePerformanceChart()
  
  window.addEventListener('resize', handleResize)
}

// 处理窗口大小变化
const handleResize = () => {
  if (chartInstance) {
    chartInstance.resize()
  }
}

// 获取CPU颜色
const getCpuColor = (value?: number) => {
  if (!value) return '#8a8f98'
  if (value > 80) return '#ff4d4f'
  if (value > 60) return '#faad14'
  return '#52c41a'
}

// 获取内存颜色
const getMemoryColor = (value?: number) => {
  if (!value) return '#8a8f98'
  if (value > 80) return '#ff4d4f'
  if (value > 60) return '#faad14'
  return '#52c41a'
}

// 获取磁盘颜色
const getDiskColor = (value?: number) => {
  if (!value) return '#8a8f98'
  if (value > 80) return '#ff4d4f'
  if (value > 60) return '#faad14'
  return '#52c41a'
}

// 获取请求方法颜色
const getMethodColor = (method: string) => {
  switch (method) {
    case 'GET': return 'green'
    case 'POST': return 'blue'
    case 'PUT': return 'orange'
    case 'DELETE': return 'red'
    default: return 'default'
  }
}

// Format uptime
const formatUptime = (seconds?: number) => {
  if (!seconds) return '--'

  const days = Math.floor(seconds / 86400)
  const hours = Math.floor((seconds % 86400) / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)

  if (days > 0) {
    return t('monitor.formatDaysHours', { days, hours })
  } else if (hours > 0) {
    return t('monitor.formatHoursMinutes', { hours, minutes })
  } else {
    return t('monitor.formatMinutes', { minutes })
  }
}

// 监听时间范围变化
watch(timeRange, () => {
  fetchPerformanceMetrics()
})

onMounted(() => {
  fetchSystemStatus()
  fetchPerformanceMetrics()
  fetchApiLogs()
  initPerformanceChart()
  
  // 每60秒刷新一次系统状态
  const interval = setInterval(fetchSystemStatus, 60000)
  
  // 组件卸载时清除定时器
  onUnmounted(() => {
    clearInterval(interval)
    window.removeEventListener('resize', handleResize)
    if (chartInstance) {
      chartInstance.dispose()
      chartInstance = null
    }
  })
})
</script>

<style scoped lang="less">
.monitor-page {
  .stats-row {
    margin-bottom: 16px;
  }
  
  .stats-card {
    .stats-content {
      .stats-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 12px;
        
        .stats-title {
          font-size: 14px;
          color: #8a8f98;
        }
        
        .stats-icon {
          font-size: 18px;
          color: #5e6ad2;
        }
      }
      
      .stats-value {
        font-size: 24px;
        font-weight: 600;
        margin-bottom: 12px;
      }
      
      .stats-footer {
        font-size: 12px;
        color: #8a8f98;
        margin-top: 8px;
      }
    }
  }
  
  .charts-row {
    margin-bottom: 16px;
  }
  
  .chart-card {
    .chart-container {
      height: 300px;
      width: 100%;
    }
  }
  
  .status-card {
    .service-status {
      .service-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 16px;
        
        .service-name {
          display: flex;
          align-items: center;
          gap: 8px;
          color: #f7f8f8;
          
          .service-dot {
            display: inline-block;
            width: 8px;
            height: 8px;
            border-radius: 50%;
            
            &.healthy {
              background-color: #52c41a;
            }
            
            &.unhealthy {
              background-color: #ff4d4f;
            }
          }
        }
      }
    }
    
    .database-info {
      .info-title {
        font-size: 14px;
        font-weight: 600;
        color: #f7f8f8;
        margin-bottom: 12px;
      }
      
      .info-item {
        display: flex;
        justify-content: space-between;
        margin-bottom: 8px;
        
        .info-label {
          color: #8a8f98;
        }
        
        .info-value {
          color: #f7f8f8;
          font-weight: 500;
        }
      }
    }
  }
  
  .api-log-card {
    // 样式已在全局定义
  }
}
</style>
