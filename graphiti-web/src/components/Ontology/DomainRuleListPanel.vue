<template>
  <div class="domain-rule-list-panel">
    <div class="panel-toolbar">
      <a-space>
        <a-button type="primary" @click="showEditModal = true">
          <template #icon><PlusOutlined /></template>
          新建规则
        </a-button>
        <a-button :loading="refreshing" @click="loadRules">
          <template #icon><ReloadOutlined /></template>
          刷新
        </a-button>
      </a-space>
    </div>

    <a-input-search
      v-model:value="searchText"
      placeholder="搜索规则名称/代码..."
      style="margin-bottom: 16px"
      @search="handleSearch"
    />

    <a-table
      :columns="columns"
      :data-source="filteredRules"
      :loading="loading"
      :pagination="{ pageSize: 20 }"
      row-key="id"
    >
      <template #bodyCell="{ column, record }">
        <!-- 适用类列 -->
        <template v-if="column.key === 'applicableClassIds'">
          <span v-if="!record.applicableClassIds || record.applicableClassIds.length === 0">
            🌐 全部类
          </span>
          <a-tag v-else color="blue">
            {{ record.applicableClassIds.length }} 个类
          </a-tag>
        </template>

        <!-- 严重级别列 -->
        <template v-if="column.key === 'severity'">
          <a-tag :color="severityColor(record.severity)">
            {{ record.severity }}
          </a-tag>
        </template>

        <!-- 启用状态列 -->
        <template v-if="column.key === 'enabled'">
          <a-switch
            v-model:checked="record.enabled"
            @change="handleToggle(record)"
          />
        </template>

        <!-- 最近测试列 -->
        <template v-if="column.key === 'lastTest'">
          <div v-if="record.lastTestResult" class="test-result-cell">
            <a-badge
              :status="record.lastTestResult.passed ? 'success' : 'error'"
              :text="record.lastTestResult.passed ? '通过' : '失败'"
            />
            <a-button size="small" type="link" @click="showTestModal(record)">
              详情
            </a-button>
          </div>
          <a-button v-else size="small" type="link" @click="showTestModal(record)">
            🧪 测试
          </a-button>
        </template>

        <!-- 操作列 -->
        <template v-if="column.key === 'action'">
          <a-space>
            <a-button size="small" @click="handleEdit(record)">编辑</a-button>
            <a-button size="small" @click="showTestModal(record)">测试</a-button>
            <a-popconfirm
              title="确定删除此规则吗? 此操作不可恢复。"
              ok-text="确定"
              cancel-text="取消"
              @confirm="handleDelete(record)"
            >
              <a-button size="small" danger>删除</a-button>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>

    <!-- 编辑模态框 -->
    <DomainRuleEditModal
      v-model:open="showEditModal"
      :graph-id="graphId"
      :rule-data="editingRule"
      :class-list="classList"
      @saved="handleRuleSaved"
    />

    <!-- 测试模态框 -->
    <DomainRuleTestModal
      v-model:open="showTestModalVisible"
      :graph-id="graphId"
      :rule-data="testingRule"
      @tested="handleTested"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons-vue'
import { ontologyApi } from '@/api/ontology'
import type { DomainRuleVO, OntClassVO } from '@/api/ontology'
import DomainRuleEditModal from './DomainRuleEditModal.vue'
import DomainRuleTestModal from './DomainRuleTestModal.vue'

const props = defineProps<{ graphId: string }>()

const loading = ref(false)
const refreshing = ref(false)
const rules = ref<DomainRuleVO[]>([])
const classList = ref<OntClassVO[]>([])
const searchText = ref('')

const showEditModal = ref(false)
const showTestModalVisible = ref(false)
const editingRule = ref<DomainRuleVO | null>(null)
const testingRule = ref<DomainRuleVO | null>(null)

const columns = [
  { title: '规则名称', dataIndex: 'ruleName', width: '15%' },
  { title: '规则代码', dataIndex: 'ruleCode', width: '12%' },
  { title: 'SpEL 表达式', dataIndex: 'spelExpression', width: '25%', ellipsis: true },
  { title: '适用类', key: 'applicableClassIds', width: '15%' },
  { title: '严重级别', key: 'severity', width: '10%' },
  { title: '状态', key: 'enabled', width: '8%' },
  { title: '最近测试', key: 'lastTest', width: '15%' },
  { title: '操作', key: 'action', width: '18%' }
]

const filteredRules = computed(() => {
  if (!searchText.value) return rules.value
  const text = searchText.value.toLowerCase()
  return rules.value.filter(
    r => r.ruleName?.toLowerCase().includes(text) ||
         r.ruleCode?.toLowerCase().includes(text)
  )
})

function severityColor(severity?: string) {
  return { ERROR: 'red', WARNING: 'orange', INFO: 'blue' }[severity ?? ''] ?? 'default'
}

async function loadRules() {
  loading.value = true
  try {
    const [rulesData, classesData] = await Promise.all([
      ontologyApi.listDomainRules(props.graphId),
      ontologyApi.listClasses(props.graphId)
    ])
    rules.value = rulesData
    classList.value = classesData
  } catch (e: any) {
    message.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function handleToggle(record: DomainRuleVO) {
  try {
    await ontologyApi.toggleDomainRule(props.graphId, record.id!, record.enabled!)
    message.success(`规则已${record.enabled ? '启用' : '禁用'}`)
  } catch (e: any) {
    message.error(e.message || '操作失败')
    record.enabled = !record.enabled // 回滚
  }
}

function handleEdit(record: DomainRuleVO) {
  editingRule.value = { ...record }
  showEditModal.value = true
}

function showTestModal(record: DomainRuleVO) {
  testingRule.value = record
  showTestModalVisible.value = true
}

async function handleDelete(record: DomainRuleVO) {
  try {
    await ontologyApi.deleteDomainRule(props.graphId, record.id!)
    message.success('删除成功')
    await loadRules()
  } catch (e: any) {
    message.error(e.message || '删除失败')
  }
}

function handleRuleSaved() {
  showEditModal.value = false
  editingRule.value = null
  loadRules()
}

function handleTested(result: any) {
  if (testingRule.value) {
    testingRule.value.lastTestResult = {
      ...result,
      testedAt: new Date().toISOString()
    }
  }
}

function handleSearch() {
  // 搜索由 computed 自动处理
}

onMounted(() => {
  loadRules()
})
</script>

<style scoped lang="less">
.domain-rule-list-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  padding: 16px;

  .panel-toolbar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
  }

  .test-result-cell {
    display: flex;
    align-items: center;
    gap: 8px;
  }
}
</style>
