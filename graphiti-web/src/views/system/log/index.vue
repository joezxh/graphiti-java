<template>
  <div class="log-management">
    <a-card class="page-header" :bordered="false">
      <div class="header-content">
        <div class="header-left">
          <h2 class="page-title">操作日志</h2>
          <p class="page-description">查看系统操作记录</p>
        </div>
        <div class="header-actions">
          <a-space>
            <a-button @click="handleExport">
              <template #icon><DownloadOutlined /></template>
              导出
            </a-button>
            <a-popconfirm
              title="确定要清空所有日志吗？"
              ok-text="确定"
              cancel-text="取消"
              @confirm="handleClearAll"
            >
              <a-button danger>
                <template #icon><DeleteOutlined /></template>
                清空日志
              </a-button>
            </a-popconfirm>
          </a-space>
        </div>
      </div>
    </a-card>

    <a-card class="content-card" :bordered="false">
      <!-- 搜索表单 -->
      <div class="table-operations">
        <a-form layout="inline" :model="queryParams" class="search-form">
          <a-form-item label="用户名">
            <a-input
              v-model:value="queryParams.username"
              placeholder="请输入用户名"
              allow-clear
              style="width: 150px"
            />
          </a-form-item>
          <a-form-item label="操作">
            <a-input
              v-model:value="queryParams.operation"
              placeholder="请输入操作"
              allow-clear
              style="width: 150px"
            />
          </a-form-item>
          <a-form-item label="状态">
            <a-select
              v-model:value="queryParams.status"
              placeholder="请选择状态"
              allow-clear
              style="width: 120px"
            >
              <a-select-option :value="1">成功</a-select-option>
              <a-select-option :value="0">失败</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item label="时间">
            <a-range-picker
              v-model:value="timeRange"
              show-time
              format="YYYY-MM-DD HH:mm:ss"
              style="width: 380px"
            />
          </a-form-item>
          <a-form-item>
            <a-space>
              <a-button type="primary" @click="handleQuery">查询</a-button>
              <a-button @click="handleReset">重置</a-button>
            </a-space>
          </a-form-item>
        </a-form>
      </div>

      <!-- 数据表格 -->
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
              {{ record.status === 1 ? '成功' : '失败' }}
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
                详情
              </a-button>
              <a-popconfirm
                title="确定要删除此日志吗？"
                ok-text="确定"
                cancel-text="取消"
                @confirm="handleDelete(record.id)"
              >
                <a-button type="link" size="small" danger>
                  <template #icon><DeleteOutlined /></template>
                  删除
                </a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 日志详情对话框 -->
    <a-modal
      v-model:visible="detailVisible"
      title="日志详情"
      width="700px"
      :footer="null"
    >
      <a-descriptions :column="2" bordered v-if="currentLog">
        <a-descriptions-item label="ID">{{ currentLog.id }}</a-descriptions-item>
        <a-descriptions-item label="状态">
          <a-badge :status="currentLog.status === 1 ? 'success' : 'error'" />
          {{ currentLog.status === 1 ? '成功' : '失败' }}
        </a-descriptions-item>
        <a-descriptions-item label="用户名">{{ currentLog.username }}</a-descriptions-item>
        <a-descriptions-item label="用户昵称">{{ currentLog.nickname }}</a-descriptions-item>
        <a-descriptions-item label="操作">{{ currentLog.operation }}</a-descriptions-item>
        <a-descriptions-item label="耗时">{{ currentLog.duration }} ms</a-descriptions-item>
        <a-descriptions-item label="请求方法" :span="2">{{ currentLog.method }}</a-descriptions-item>
        <a-descriptions-item label="请求参数" :span="2">
          <pre class="log-params">{{ formatJson(currentLog.params) }}</pre>
        </a-descriptions-item>
        <a-descriptions-item label="IP地址">{{ currentLog.ip }}</a-descriptions-item>
        <a-descriptions-item label="地理位置">{{ currentLog.location }}</a-descriptions-item>
        <a-descriptions-item label="错误信息" :span="2" v-if="currentLog.status === 0">
          <span style="color: #ff4d4f">{{ currentLog.errorMsg }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="创建时间" :span="2">{{ currentLog.createdAt }}</a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import {
  DownloadOutlined,
  DeleteOutlined,
  EyeOutlined
} from '@ant-design/icons-vue'
import { logApi, type OperationLog, type LogQuery } from '@/api/log'
// import type { Dayjs } from 'dayjs'

// 查询参数
const queryParams = reactive<LogQuery>({
  username: undefined,
  operation: undefined,
  status: undefined,
  startTime: undefined,
  endTime: undefined,
  pageNum: 1,
  pageSize: 10
})

// 时间范围
const timeRange = ref<[any, any]>()

// 表格列定义
const columns = [
  {
    title: 'ID',
    dataIndex: 'id',
    width: 60
  },
  {
    title: '用户名',
    dataIndex: 'username',
    width: 100
  },
  {
    title: '操作',
    dataIndex: 'operation',
    width: 120
  },
  {
    title: '请求方法',
    dataIndex: 'method',
    width: 200,
    ellipsis: true
  },
  {
    title: 'IP地址',
    dataIndex: 'ip',
    width: 120
  },
  {
    title: '状态',
    dataIndex: 'status',
    width: 100
  },
  {
    title: '耗时',
    dataIndex: 'duration',
    width: 100
  },
  {
    title: '创建时间',
    dataIndex: 'createdAt',
    width: 170
  },
  {
    title: '操作',
    dataIndex: 'action',
    width: 150,
    fixed: 'right'
  }
]

// 数据列表
const logList = ref<OperationLog[]>([])
const loading = ref(false)
const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true
})

// 详情对话框
const detailVisible = ref(false)
const currentLog = ref<OperationLog>()

// 获取日志列表
const fetchLogs = async () => {
  loading.value = true
  try {
    const res = await logApi.getLogs(queryParams)
    logList.value = res.list
    pagination.current = res.pageNum
    pagination.pageSize = res.pageSize
    pagination.total = res.total
  } catch (error) {
    message.error('获取日志列表失败')
  } finally {
    loading.value = false
  }
}

// 查询
const handleQuery = () => {
  // 处理时间范围
  if (timeRange.value) {
    queryParams.startTime = timeRange.value[0].format('YYYY-MM-DD HH:mm:ss')
    queryParams.endTime = timeRange.value[1].format('YYYY-MM-DD HH:mm:ss')
  } else {
    queryParams.startTime = undefined
    queryParams.endTime = undefined
  }
  
  queryParams.pageNum = 1
  fetchLogs()
}

// 重置
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

// 表格变化
const handleTableChange = (pag: any) => {
  queryParams.pageNum = pag.current
  queryParams.pageSize = pag.pageSize
  fetchLogs()
}

// 查看详情
const handleDetail = (record: OperationLog) => {
  currentLog.value = record
  detailVisible.value = true
}

// 删除日志
const handleDelete = async (id: number) => {
  try {
    await logApi.deleteLog(id)
    message.success('删除成功')
    fetchLogs()
  } catch (error) {
    message.error('删除失败')
  }
}

// 清空所有日志
const handleClearAll = async () => {
  try {
    await logApi.clearLogs()
    message.success('已清空所有日志')
    fetchLogs()
  } catch (error) {
    message.error('清空失败')
  }
}

// 导出日志
const handleExport = async () => {
  try {
    const res = await logApi.exportLogs(queryParams)
    message.success('导出成功，正在下载...')
    // 实际应该下载文件
    console.log('下载链接:', res.url)
  } catch (error) {
    message.error('导出失败')
  }
}

// 格式化 JSON
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
