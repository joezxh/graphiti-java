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
