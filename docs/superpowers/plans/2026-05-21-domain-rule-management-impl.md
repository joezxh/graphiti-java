# 域规则管理界面实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在 OntologyWorkbench 中集成域规则管理功能,提供完整的 CRUD 操作、SpEL 表达式测试和 LLM 辅助编辑能力。

**Architecture:** 采用列表面板模式 (参照 ConstraintListPanel),创建 3 个新组件并集成到 OntologyWorkbench 标签页系统,使用 ontologyApi 已实现的后端端点。

**Tech Stack:** Vue 3 + TypeScript + Ant Design Vue + Pinia Store

---

## 文件结构

### 新增文件
- `graphiti-web/src/components/Ontology/DomainRuleListPanel.vue` - 列表主面板
- `graphiti-web/src/components/Ontology/DomainRuleEditModal.vue` - 编辑模态框
- `graphiti-web/src/components/Ontology/DomainRuleTestModal.vue` - 测试模态框

### 修改文件
- `graphiti-web/src/store/modules/ontology.ts` - 添加 'domain-rule-list' 类型
- `graphiti-web/src/components/Ontology/OntologyWorkbench.vue` - 集成新组件

---

### Task 1: 扩展 OntologyStore 类型定义

**Files:**
- Modify: `graphiti-web/src/store/modules/ontology.ts:17-30`

- [ ] **Step 1: 添加 domain-rule-list 到 OntologyTabType**

```typescript
export type OntologyTabType =
  | 'class-list'
  | 'class-editor'
  | 'property-list'
  | 'property-editor'
  | 'constraint-list'
  | 'domain-rule-list'  // ← 新增: 域规则列表
  | 'definition-editor'
  | 'instance-table'
  | 'instance-editor'
  | 'version-history'
  | 'version-diff'
  | 'consistency-check'
  | 'batch-validation'
  | 'ontology-graph'
```

- [ ] **Step 2: 验证类型定义**

检查 TypeScript 编译:
```bash
cd graphiti-web
npx tsc --noEmit
```
Expected: No errors related to ontology.ts

- [ ] **Step 3: Commit**

```bash
git add graphiti-web/src/store/modules/ontology.ts
git commit -m "feat: add domain-rule-list tab type to ontology store"
```

---

### Task 2: 创建 DomainRuleListPanel 主面板

**Files:**
- Create: `graphiti-web/src/components/Ontology/DomainRuleListPanel.vue`
- Test: 手动测试 (浏览器访问 Graph IDE)

- [ ] **Step 1: 创建组件骨架**

```vue
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
import { ref, reactive, computed, onMounted } from 'vue'
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
```

- [ ] **Step 2: 添加 LastTestResult 类型到 DomainRuleVO**

在 `graphiti-web/src/api/ontology.ts` 的 DomainRuleVO 接口中添加:

```typescript
export interface DomainRuleVO {
  id?: number
  definitionId?: number
  ruleName: string
  ruleCode: string
  spelExpression: string
  applicableClassIds?: number[]
  severity?: string
  errorMessage?: string
  description?: string
  enabled?: boolean
  createdAt?: string
  updatedAt?: string
  lastTestResult?: {        // ← 新增: 前端存储的测试结果
    passed: boolean
    testData: string
    result: any
    error: string | null
    testedAt: string
  }
}
```

- [ ] **Step 3: 验证组件创建**

```bash
cd graphiti-web
npx tsc --noEmit
```
Expected: No errors

- [ ] **Step 4: Commit**

```bash
git add graphiti-web/src/components/Ontology/DomainRuleListPanel.vue
git add graphiti-web/src/api/ontology.ts
git commit -m "feat: create DomainRuleListPanel component with CRUD operations"
```

---

### Task 3: 创建 DomainRuleEditModal 编辑模态框

**Files:**
- Create: `graphiti-web/src/components/Ontology/DomainRuleEditModal.vue`

- [ ] **Step 1: 创建编辑模态框组件**

```vue
<template>
  <a-modal
    :open="open"
    :title="isEdit ? '编辑域规则' : '新建域规则'"
    :width="800"
    @ok="handleSave"
    @cancel="handleCancel"
  >
    <a-form
      :model="form"
      :rules="rules"
      ref="formRef"
      layout="vertical"
    >
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="规则名称" name="ruleName">
            <a-input v-model:value="form.ruleName" placeholder="例: 金额校验规则" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="规则代码" name="ruleCode">
            <a-input
              v-model:value="form.ruleCode"
              placeholder="例: AMOUNT_CHECK"
              :disabled="isEdit"
            />
          </a-form-item>
        </a-col>
      </a-row>

      <a-form-item label="SpEL 表达式" name="spelExpression">
        <div class="spel-editor">
          <a-textarea
            v-model:value="form.spelExpression"
            :rows="5"
            placeholder="例: #amount > 0 && #amount <= 1000000"
          />
          <div class="spel-toolbar">
            <a-button size="small" @click="showSpelHelp = true">
              📖 语法帮助
            </a-button>
            <a-button size="small" @click="showTemplatePicker = true">
              📝 模板库
            </a-button>
          </div>
        </div>
      </a-form-item>

      <a-form-item label="适用类" name="applicableClassIds">
        <a-select
          v-model:value="form.applicableClassIds"
          mode="multiple"
          placeholder="选择适用的本体类 (留空表示全部类)"
          allow-clear
        >
          <a-select-option :value="null" style="font-weight: bold; color: #5e6ad2">
            🌐 全部类 (应用于所有节点)
          </a-select-option>
          <a-select-divider />
          <a-select-option
            v-for="cls in classList"
            :key="cls.id"
            :value="cls.id"
          >
            {{ cls.localName }} ({{ cls.classUri }})
          </a-select-option>
        </a-select>
        <div class="form-help">
          💡 不选择任何类时,规则将应用于图谱中的所有节点
        </div>
      </a-form-item>

      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="严重级别" name="severity">
            <a-select v-model:value="form.severity">
              <a-select-option value="ERROR">❌ 错误</a-select-option>
              <a-select-option value="WARNING">⚠️ 警告</a-select-option>
              <a-select-option value="INFO">ℹ️ 提示</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="错误消息" name="errorMessage">
            <a-input v-model:value="form.errorMessage" placeholder="验证失败时显示的消息" />
          </a-form-item>
        </a-col>
      </a-row>

      <a-form-item label="描述" name="description">
        <a-textarea v-model:value="form.description" :rows="3" />
      </a-form-item>
    </a-form>

    <!-- SpEL 语法帮助弹窗 -->
    <a-modal v-model:open="showSpelHelp" title="SpEL 语法帮助" width="700">
      <div class="spel-help-content">
        <h4>常用语法:</h4>
        <ul>
          <li><strong>变量引用:</strong> <code>#propertyName</code></li>
          <li><strong>比较运算:</strong> <code>&gt;</code>, <code>&lt;</code>, <code>&gt;=</code>, <code>&lt;=</code>, <code>==</code>, <code>!=</code></li>
          <li><strong>逻辑运算:</strong> <code>&amp;&amp;</code>, <code>||</code>, <code>!</code></li>
          <li><strong>字符串:</strong> <code>#name.contains('test')</code>, <code>#name.matches('regex')</code></li>
          <li><strong>数值:</strong> <code>#amount &gt; 0</code>, <code>#age between 18 and 65</code></li>
          <li><strong>集合:</strong> <code>#tags.size() &gt; 0</code>, <code>#tags.contains('active')</code></li>
          <li><strong>空值检查:</strong> <code>#field != null</code>, <code>#field?.length &gt; 0</code></li>
        </ul>
      </div>
    </a-modal>

    <!-- 模板库弹窗 -->
    <a-modal v-model:open="showTemplatePicker" title="规则模板库" width="800">
      <a-list :data-source="templates" bordered>
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta>
              <template #title>{{ item.name }}</template>
              <template #description>
                <code>{{ item.expression }}</code>
              </template>
            </a-list-item-meta>
            <template #actions>
              <a-button type="link" @click="useTemplate(item)">使用</a-button>
            </template>
          </a-list-item>
        </template>
      </a-list>
    </a-modal>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'
import { message } from 'ant-design-vue'
import { ontologyApi } from '@/api/ontology'
import type { DomainRuleVO, OntClassVO } from '@/api/ontology'

const props = defineProps<{
  open: boolean
  graphId: string
  ruleData: DomainRuleVO | null
  classList: OntClassVO[]
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  saved: []
}>()

const formRef = ref()
const showSpelHelp = ref(false)
const showTemplatePicker = ref(false)

const form = reactive({
  ruleName: '',
  ruleCode: '',
  spelExpression: '',
  applicableClassIds: [] as (number | null)[],
  severity: 'ERROR',
  errorMessage: '',
  description: ''
})

const isEdit = computed(() => !!props.ruleData?.id)

const rules = {
  ruleName: [
    { required: true, message: '请输入规则名称', trigger: 'blur' },
    { max: 100, message: '规则名称最多 100 字符', trigger: 'blur' }
  ],
  ruleCode: [
    { required: true, message: '请输入规则代码', trigger: 'blur' },
    { pattern: /^[A-Z_]+$/, message: '仅允许大写字母和下划线', trigger: 'blur' }
  ],
  spelExpression: [
    { required: true, message: '请输入 SpEL 表达式', trigger: 'blur' },
    {
      validator: (_rule: any, value: string) => {
        if (value && !value.includes('#')) {
          return Promise.reject(new Error('SpEL 表达式应包含至少一个变量引用 (#)'))
        }
        return Promise.resolve()
      },
      trigger: 'blur'
    }
  ],
  severity: [
    { required: true, message: '请选择严重级别', trigger: 'change' }
  ]
}

const templates = [
  { name: '金额校验', expression: '#amount > 0 && #amount <= 1000000' },
  { name: '日期范围', expression: '#date >= T(java.time.LocalDate).now()' },
  { name: '必填字段', expression: '#field != null && !#field.trim().isEmpty()' },
  { name: '格式验证', expression: "#email matches '^[\\\\w-\\\\.]+@([\\\\w-]+\\\\.)+[\\\\w-]{2,4}$'" },
  { name: '年龄限制', expression: '#age >= 18 && #age <= 120' },
  { name: '枚举校验', expression: "#status in {'ACTIVE', 'PENDING', 'COMPLETED'}" }
]

watch(() => props.open, (val) => {
  if (val && props.ruleData) {
    Object.assign(form, {
      ruleName: props.ruleData.ruleName || '',
      ruleCode: props.ruleData.ruleCode || '',
      spelExpression: props.ruleData.spelExpression || '',
      applicableClassIds: props.ruleData.applicableClassIds || [],
      severity: props.ruleData.severity || 'ERROR',
      errorMessage: props.ruleData.errorMessage || '',
      description: props.ruleData.description || ''
    })
  } else if (!val) {
    resetForm()
  }
})

function resetForm() {
  form.ruleName = ''
  form.ruleCode = ''
  form.spelExpression = ''
  form.applicableClassIds = []
  form.severity = 'ERROR'
  form.errorMessage = ''
  form.description = ''
  formRef.value?.resetFields()
}

function useTemplate(template: { expression: string }) {
  form.spelExpression = template.expression
  showTemplatePicker.value = false
  message.success('已应用模板')
}

async function handleSave() {
  try {
    await formRef.value?.validateFields()

    const data = {
      ...form,
      definitionId: props.ruleData?.definitionId,
      applicableClassIds: form.applicableClassIds.filter(id => id !== null) as number[]
    }

    if (isEdit.value) {
      await ontologyApi.updateDomainRule(props.graphId, props.ruleData!.id!, data)
      message.success('规则已更新')
    } else {
      await ontologyApi.createDomainRule(props.graphId, data)
      message.success('规则已创建')
    }

    emit('saved')
  } catch (e: any) {
    if (e.errorFields) {
      message.error('请检查表单填写')
    } else {
      message.error(e.message || '保存失败')
    }
  }
}

function handleCancel() {
  emit('update:open', false)
}
</script>

<style scoped lang="less">
.spel-editor {
  .spel-toolbar {
    margin-top: 8px;
    display: flex;
    gap: 8px;
  }
}

.form-help {
  margin-top: 4px;
  font-size: 12px;
  color: #8c8c8c;
}

.spel-help-content {
  code {
    background: #f5f5f5;
    padding: 2px 6px;
    border-radius: 4px;
    font-family: monospace;
  }

  ul {
    list-style: none;
    padding-left: 0;

    li {
      padding: 4px 0;
    }
  }
}
</style>
```

- [ ] **Step 2: 验证组件**

```bash
cd graphiti-web
npx tsc --noEmit
```

- [ ] **Step 3: Commit**

```bash
git add graphiti-web/src/components/Ontology/DomainRuleEditModal.vue
git commit -m "feat: create DomainRuleEditModal with SpEL editor and templates"
```

---

### Task 4: 创建 DomainRuleTestModal 测试模态框

**Files:**
- Create: `graphiti-web/src/components/Ontology/DomainRuleTestModal.vue`

- [ ] **Step 1: 创建测试模态框组件**

```vue
<template>
  <a-modal
    :open="open"
    title="🧪 测试 SpEL 表达式"
    :width="700"
    @cancel="handleCancel"
  >
    <div v-if="ruleData" class="test-modal-content">
      <a-descriptions title="当前规则" bordered size="small" :column="1">
        <a-descriptions-item label="规则名称">
          {{ ruleData.ruleName }}
        </a-descriptions-item>
        <a-descriptions-item label="SpEL 表达式">
          <code class="spel-code">{{ ruleData.spelExpression }}</code>
        </a-descriptions-item>
      </a-descriptions>

      <a-divider />

      <a-form layout="vertical">
        <a-form-item label="测试数据 (JSON 格式)">
          <a-textarea
            v-model:value="testDataJson"
            :rows="6"
            placeholder='{"amount": 500}'
          />
          <div class="form-help">
            💡 输入 JSON 对象,属性名将映射为 SpEL 变量
          </div>
        </a-form-item>

        <a-button
          type="primary"
          :loading="testing"
          :disabled="!testDataJson"
          @click="handleTest"
        >
          ▶ 执行测试
        </a-button>
      </a-form>

      <a-divider />

      <div v-if="testResult" class="test-result">
        <h4>测试结果:</h4>
        <a-alert
          v-if="testResult.error"
          type="error"
          :message="`验证失败: ${testResult.error}`"
          show-icon
        />
        <a-alert
          v-else-if="testResult.passed"
          type="success"
          message="✅ 验证通过"
          description="SpEL 表达式返回 true"
          show-icon
        />
        <a-alert
          v-else
          type="warning"
          message="❌ 验证失败"
          description="SpEL 表达式返回 false"
          show-icon
        />

        <a-descriptions
          v-if="testResult.result !== undefined && !testResult.error"
          title="返回值"
          bordered
          size="small"
          :column="1"
          style="margin-top: 16px"
        >
          <a-descriptions-item label="类型">
            {{ typeof testResult.result }}
          </a-descriptions-item>
          <a-descriptions-item label="值">
            <code>{{ JSON.stringify(testResult.result) }}</code>
          </a-descriptions-item>
        </a-descriptions>
      </div>
    </div>

    <template #footer>
      <a-button @click="handleCancel">关闭</a-button>
    </template>
  </a-modal>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { ontologyApi } from '@/api/ontology'
import type { DomainRuleVO } from '@/api/ontology'

const props = defineProps<{
  open: boolean
  graphId: string
  ruleData: DomainRuleVO | null
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  tested: [result: any]
}>()

const testDataJson = ref('')
const testing = ref(false)
const testResult = ref<{
  passed: boolean
  result: any
  error: string | null
} | null>(null)

watch(() => props.open, (val) => {
  if (!val) {
    testDataJson.value = ''
    testResult.value = null
  }
})

async function handleTest() {
  if (!props.ruleData?.spelExpression) return

  let testData: Record<string, any>
  try {
    testData = JSON.parse(testDataJson.value)
  } catch (e) {
    message.error('测试数据不是有效的 JSON 格式')
    return
  }

  testing.value = true
  try {
    const result = await ontologyApi.testDomainRule(
      props.graphId,
      props.ruleData.spelExpression,
      testData
    )

    testResult.value = {
      passed: result.passed,
      result: result.result,
      error: result.error
    }

    emit('tested', {
      passed: result.passed,
      testData: testDataJson.value,
      result: result.result,
      error: result.error
    })

    if (result.passed) {
      message.success('验证通过')
    } else if (result.error) {
      message.error(`验证失败: ${result.error}`)
    } else {
      message.warning('验证失败: 表达式返回 false')
    }
  } catch (e: any) {
    message.error(e.message || '测试失败')
  } finally {
    testing.value = false
  }
}

function handleCancel() {
  emit('update:open', false)
}
</script>

<style scoped lang="less">
.test-modal-content {
  .spel-code {
    background: #f5f5f5;
    padding: 4px 8px;
    border-radius: 4px;
    font-family: monospace;
    display: block;
    margin-top: 4px;
  }

  .form-help {
    margin-top: 4px;
    font-size: 12px;
    color: #8c8c8c;
  }

  .test-result {
    h4 {
      margin-bottom: 12px;
    }
  }
}
</style>
```

- [ ] **Step 2: 验证组件**

```bash
cd graphiti-web
npx tsc --noEmit
```

- [ ] **Step 3: Commit**

```bash
git add graphiti-web/src/components/Ontology/DomainRuleTestModal.vue
git commit -m "feat: create DomainRuleTestModal for SpEL expression testing"
```

---

### Task 5: 集成到 OntologyWorkbench

**Files:**
- Modify: `graphiti-web/src/components/Ontology/OntologyWorkbench.vue`

- [ ] **Step 1: 添加异步组件导入**

在 `<script setup lang="ts">` 部分添加:

```typescript
const DomainRuleListPanel = defineAsyncComponent(
  () => import('./DomainRuleListPanel.vue')
)
```

- [ ] **Step 2: 添加新建菜单项**

在"新建"菜单的"定义管理"组中添加:

```vue
<a-menu-item key="domain-rule-list">
  <span>⚙️</span> 域规则列表
</a-menu-item>
```

完整菜单结构示例:
```vue
<a-menu-item-group title="定义管理">
  <a-menu-item key="definition-editor">
    <span>📋</span> 本体定义
  </a-menu-item>
  <a-menu-item key="class-editor">
    <span>◉</span> 新建类
  </a-menu-item>
  <a-menu-item key="property-editor">
    <span>◆</span> 新建属性
  </a-menu-item>
  <a-menu-item key="constraint-list">
    <span>◇</span> 新建约束
  </a-menu-item>
  <a-menu-item key="domain-rule-list">  <!-- ← 新增 -->
    <span>⚙️</span> 域规则列表
  </a-menu-item>
</a-menu-item-group>
```

- [ ] **Step 3: 添加菜单映射**

在 `handleAddMenuClick` 函数的 `menuMap` 中添加:

```typescript
const menuMap: Record<string, { type: OntologyTabType; title: string }> = {
  // ... 现有映射
  'domain-rule-list': { type: 'domain-rule-list', title: '域规则列表' }
}
```

- [ ] **Step 4: 添加标签页渲染**

在标签页渲染区域添加:

```vue
<DomainRuleListPanel
  v-else-if="store.activeTab.type === 'domain-rule-list'"
  :graph-id="graphId"
/>
```

完整渲染逻辑示例:
```vue
<ClassEditor
  v-if="store.activeTab.type === 'class-editor'"
  :graph-id="graphId"
  :class-id="store.activeTab.classId"
  @saved="handleSaved"
/>
<!-- ... 其他组件 ... -->
<DomainRuleListPanel
  v-else-if="store.activeTab.type === 'domain-rule-list'"
  :graph-id="graphId"
/>
<div v-else class="panel-placeholder">
  <div class="empty-desc">暂未实现: {{ store.activeTab.type }}</div>
</div>
```

- [ ] **Step 5: 验证集成**

```bash
cd graphiti-web
npm run build
```
Expected: BUILD SUCCESS (可能有一些已有的 TypeScript 警告)

- [ ] **Step 6: Commit**

```bash
git add graphiti-web/src/components/Ontology/OntologyWorkbench.vue
git commit -m "feat: integrate DomainRuleListPanel into OntologyWorkbench"
```

---

### Task 6: 端到端测试与优化

**Files:**
- 无文件修改,纯测试

- [ ] **Step 1: 启动开发服务器**

```bash
cd graphiti-web
npm run dev
```

- [ ] **Step 2: 功能测试清单**

在浏览器中访问 Graph IDE 页面,测试以下功能:

1. **打开域规则列表**
   - 点击"新建" → "域规则列表"
   - 验证新标签页打开
   - 验证列表加载 (可能为空)

2. **创建域规则**
   - 点击"新建规则"
   - 填写表单所有字段
   - 测试 SpEL 语法帮助弹窗
   - 测试模板库弹窗
   - 提交并验证创建成功

3. **编辑域规则**
   - 点击"编辑"按钮
   - 修改字段
   - 提交并验证更新成功

4. **启用/禁用**
   - 切换开关
   - 验证状态更新

5. **测试 SpEL 表达式**
   - 点击"测试"按钮
   - 输入 JSON 测试数据
   - 执行测试并查看结果
   - 验证列表显示最近测试结果

6. **删除域规则**
   - 点击"删除"按钮
   - 确认删除
   - 验证删除成功

7. **搜索功能**
   - 输入规则名称
   - 验证过滤结果

8. **适用类"全部类"选项**
   - 创建规则时不选择类
   - 验证列表显示"🌐 全部类"

- [ ] **Step 3: 性能检查**

- 列表加载时间 < 2s
- 表单提交响应时间 < 1s
- 模态框打开/关闭流畅

- [ ] **Step 4: Commit (如有优化)**

```bash
git add .
git commit -m "fix: optimize domain rule management UX and performance"
```

---

## 自审检查

### 1. 规范覆盖

| 需求 | 实现任务 | 状态 |
|------|---------|------|
| 域规则列表展示 | Task 2 | ✅ |
| 新建域规则表单 | Task 3 | ✅ |
| 编辑域规则功能 | Task 3 | ✅ |
| 删除域规则 (带确认) | Task 2 | ✅ |
| 启用/禁用开关 | Task 2 | ✅ |
| 规则测试功能 | Task 4 | ✅ |
| 适用类"全部类"选项 | Task 3 | ✅ |
| SpEL 语法帮助 | Task 3 | ✅ |
| SpEL 模板库 | Task 3 | ✅ |
| LLM AI 生成 | 暂不实现 | ⚠️ 降级为手动输入 |
| 列表显示最近测试 | Task 2 | ✅ |
| 集成到 OntologyWorkbench | Task 5 | ✅ |

**LLM API 处理**: 后端未实现 `/generate-spel` 端点,前端仅提供语法帮助和模板库。如需 LLM 功能,后续可添加。

### 2. 占位符扫描

✅ 无 "TBD"/"TODO"/"implement later"  
✅ 所有代码步骤包含完整实现  
✅ 类型定义一致 (DomainRuleVO, OntClassVO)  
✅ API 调用使用已实现端点

### 3. 类型一致性

- `DomainRuleVO` 类型在所有组件中一致
- `OntClassVO` 用于适用类选择
- API 签名与 `ontology.ts` 定义匹配
- 事件发射 (`emit`) 签名一致

### 4. 范围检查

✅ 聚焦单一功能 (域规则管理)  
✅ 3 个组件职责清晰  
✅ 适合单个实现计划

---

**计划完成!** 总计 6 个任务,每个任务包含具体代码、验证步骤和提交指令。

**执行建议:** 使用 Subagent-Driven 模式,每个 Task 分配一个子代理并行开发,最后集成测试。
