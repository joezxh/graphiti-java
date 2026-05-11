<template>
  <div class="custom-instructions-page">
    <div class="page-header">
      <h1 class="page-title">自定义指令</h1>
      <p class="page-desc">管理 LLM 实体抽取时的自定义指令模板</p>
    </div>

    <a-card class="filter-card">
      <a-row :gutter="16" align="middle">
        <a-col :span="6">
          <a-select
            v-model:value="filterGraphId"
            placeholder="选择图谱（不选则查看全局）"
            style="width: 100%"
            allow-clear
            @change="loadInstructions"
          >
            <a-select-option v-for="g in graphOptions" :key="g.graphId" :value="g.graphId">
              {{ g.name }}
            </a-select-option>
          </a-select>
               </a-col>
        <a-col :span="6">
          <a-button type="primary" @click="showCreateModal">
            <PlusOutlined /> 新建指令
          </a-button>
        </a-col>
        <a-col :span="6">
          <a-button @click="loadInstructions">
            <ReloadOutlined /> 刷新
          </a-button>
        </a-col>
      </a-row>
    </a-card>

    <a-card class="table-card">
      <a-table
        :columns="columns"
        :data-source="instructionList"
        :loading="loading"
        row-key="id"
        :pagination="{ pageSize: 10 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'instruction'">
            <div class="instruction-text">{{ record.instruction }}</div>
          </template>
          <template v-if="column.key === 'scope'">
            <a-tag :color="record.graphId ? 'blue' : 'green'">
              {{ record.graphId ? '图谱专属' : '全局' }}
            </a-tag>
            <span v-if="record.graphId" class="graph-name">
              {{ getGraphName(record.graphId) }}
            </span>
          </template>
          <template v-if="column.key === 'createdAt'">
            {{ formatDate(record.createdAt) }}
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="viewDetail(record)">查看</a-button>
              <a-popconfirm title="确定删除该指令？" @confirm="deleteInstruction(record.id)">
                <a-button type="link" size="small" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 创建/查看模态框 -->
    <a-modal
      v-model:open="modalVisible"
      :title="isViewing ? '指令详情' : '新建自定义指令'"
      @ok="handleSave"
      :confirm-loading="saving"
      width="600px"
    >
      <a-form :model="form" layout="vertical">
        <a-form-item label="作用域">
          <a-select v-model:value="form.graphId" placeholder="选择图谱（留空则为全局）" allow-clear>
            <a-select-option v-for="g in graphOptions" :key="g.graphId" :value="g.graphId">
              {{ g.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="指令内容" required>
          <a-textarea
            v-model:value="form.instruction"
            placeholder="输入自定义抽取指令，最大 5000 字符，如：\n请特别关注以下实体类型：Person、Company。\n关系类型仅限：WORKS_AT、FOUNDED。"
            :rows="8"
            :maxlength="5000"
            show-count
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { graphApi } from '@/api/graph'
import { customInstructionApi, type CustomInstruction, type CreateCustomInstructionReq } from '@/api/customInstruction'

const graphOptions = ref<any[]>([])
const filterGraphId = ref<string | undefined>(undefined)
const instructionList = ref<CustomInstruction[]>([])
const loading = ref(false)

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 200, ellipsis: true },
  { title: '指令内容', key: 'instruction' },
  { title: '作用域', key: 'scope', width: 180 },
  { title: '创建时间', key: 'createdAt', width: 160 },
  { title: '操作', key: 'action', width: 150 }
]

const modalVisible = ref(false)
const saving = ref(false)
const isViewing = ref(false)
const form = reactive<CreateCustomInstructionReq>({
  instruction: '',
  graphId: undefined
})

const loadGraphs = async () => {
  try {
    graphOptions.value = await graphApi.getList()
  } catch (err) {
    console.error('加载图谱列表失败', err)
  }
}

const loadInstructions = async () => {
  loading.value = true
  try {
    instructionList.value = await customInstructionApi.list(filterGraphId.value)
  } catch (err: any) {
    message.error(err.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const showCreateModal = () => {
  isViewing.value = false
  form.instruction = ''
  form.graphId = filterGraphId.value
  modalVisible.value = true
}

const viewDetail = (record: CustomInstruction) => {
  isViewing.value = true
  form.instruction = record.instruction
  form.graphId = record.graphId
  modalVisible.value = true
}

const handleSave = async () => {
  if (!form.instruction.trim()) {
    message.error('请输入指令内容')
    return
  }
  saving.value = true
  try {
    await customInstructionApi.create(form)
    message.success('创建成功')
    modalVisible.value = false
    loadInstructions()
  } catch (err: any) {
    message.error(err.message || '创建失败')
  } finally {
    saving.value = false
  }
}

const deleteInstruction = async (id: string) => {
  try {
    await customInstructionApi.delete(id)
    message.success('删除成功')
    loadInstructions()
  } catch (err: any) {
    message.error(err.message || '删除失败')
  }
}

const getGraphName = (graphId: string): string => {
  const g = graphOptions.value.find(x => x.graphId === graphId)
  return g?.name || graphId
}

const formatDate = (date: string | undefined): string => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

onMounted(() => {
  loadGraphs()
  loadInstructions()
})
</script>

<style scoped lang="less">
@import '@/assets/styles/dark.less';

.custom-instructions-page {
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

.filter-card, .table-card {
  background: @bg-container;
  border: 1px solid @border-color;
  margin-bottom: 16px;
}

.instruction-text {
  color: @text-secondary;
  font-size: 13px;
  max-width: 400px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.graph-name {
  color: @text-tertiary;
  font-size: 12px;
  margin-left: 4px;
}
</style>
