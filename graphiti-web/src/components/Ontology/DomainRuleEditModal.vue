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
