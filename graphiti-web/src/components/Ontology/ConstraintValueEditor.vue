/**
 * 约束值类型化编辑器 — 根据 constraintType 动态渲染不同的输入组件
 * 输入输出均为 JSON 字符串（与后端 OntConstraintDO.value 格式一致）
 */
<template>
  <div class="constraint-value-editor">
    <!-- CARDINALITY / RANGE: min/max 数字输入 -->
    <template v-if="type === 'CARDINALITY' || type === 'RANGE'">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item :label="type === 'CARDINALITY' ? '最小基数' : '最小值'">
            <a-input-number v-model:value="numValue.min" :min="0" style="width: 100%" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item :label="type === 'CARDINALITY' ? '最大基数' : '最大值'">
            <a-input-number v-model:value="numValue.max" :min="0" style="width: 100%" placeholder="无限制留空" />
          </a-form-item>
        </a-col>
      </a-row>
    </template>

    <!-- PATTERN: 正则输入 + 预设 -->
    <template v-else-if="type === 'PATTERN'">
      <a-form-item label="正则表达式">
        <a-input v-model:value="patternValue.pattern" placeholder="^[A-Za-z]+$">
          <template #addonAfter>
            <a-select v-model:value="patternPreset" style="width: 140px" @change="applyPatternPreset">
              <a-select-option value="">自定义</a-select-option>
              <a-select-option value="phone">手机号</a-select-option>
              <a-select-option value="email">邮箱</a-select-option>
              <a-select-option value="url">URL</a-select-option>
              <a-select-option value="idCard">身份证号</a-select-option>
              <a-select-option value="chinese">纯中文</a-select-option>
              <a-select-option value="number">纯数字</a-select-option>
            </a-select>
          </template>
        </a-input>
      </a-form-item>
    </template>

    <!-- ENUM: 枚举值列表 -->
    <template v-else-if="type === 'ENUM'">
      <a-form-item label="枚举值（回车确认）">
        <a-select
          v-model:value="enumValue.values"
          mode="tags"
          placeholder="输入枚举值后按回车"
          style="width: 100%"
        />
      </a-form-item>
    </template>

    <!-- NOT_NULL: 无额外配置 -->
    <template v-else-if="type === 'NOT_NULL'">
      <a-alert type="info" message="此约束无需额外配置，仅要求值不能为空" />
    </template>

    <!-- CUSTOM / 默认: 原始文本输入 -->
    <template v-else>
      <a-form-item label="约束值（JSON 或自定义文本）">
        <a-textarea v-model:value="rawValue" :rows="3" placeholder='{ "key": "value" }' />
      </a-form-item>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch } from 'vue'

const props = defineProps<{
  type: string
  modelValue: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

// ==================== 内部状态 ====================

const numValue = reactive({ min: undefined as number | undefined, max: undefined as number | undefined })
const patternValue = reactive({ pattern: '' })
const patternPreset = ref('')
const enumValue = reactive({ values: [] as string[] })
const rawValue = ref('')

const patternPresets: Record<string, string> = {
  phone: '^1[3-9]\\d{9}$',
  email: '^[\\w.-]+@[\\w.-]+\\.\\w+$',
  url: '^https?://.+',
  idCard: '^\\d{6}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[\\dXx]$',
  chinese: '^[\\u4e00-\\u9fa5]+$',
  number: '^\\d+$'
}

// ==================== 解析外部值 ====================

function parseValue() {
  try {
    const parsed = JSON.parse(props.modelValue)
    if (props.type === 'CARDINALITY' || props.type === 'RANGE') {
      numValue.min = parsed.min
      numValue.max = parsed.max
    } else if (props.type === 'PATTERN') {
      patternValue.pattern = parsed.pattern || ''
      // 尝试匹配预设
      patternPreset.value = ''
      for (const [key, val] of Object.entries(patternPresets)) {
        if (val === patternValue.pattern) {
          patternPreset.value = key
          break
        }
      }
    } else if (props.type === 'ENUM') {
      enumValue.values = parsed.values || []
    } else {
      rawValue.value = props.modelValue
    }
  } catch {
    // 解析失败，使用原始值
    if (props.type === 'PATTERN') {
      patternValue.pattern = props.modelValue
    } else if (props.type === 'NOT_NULL') {
      // 无需处理
    } else {
      rawValue.value = props.modelValue
    }
  }
}

// ==================== 序列化输出 ====================

const serializedValue = computed(() => {
  switch (props.type) {
    case 'CARDINALITY':
    case 'RANGE': {
      const obj: Record<string, number> = {}
      if (numValue.min !== undefined && numValue.min !== null) obj.min = numValue.min
      if (numValue.max !== undefined && numValue.max !== null) obj.max = numValue.max
      return JSON.stringify(obj)
    }
    case 'PATTERN':
      return JSON.stringify({ pattern: patternValue.pattern })
    case 'ENUM':
      return JSON.stringify({ values: enumValue.values })
    case 'NOT_NULL':
      return JSON.stringify({ notNull: true })
    default:
      return rawValue.value
  }
})

// ==================== 事件处理 ====================

function applyPatternPreset(key: string) {
  if (key && patternPresets[key]) {
    patternValue.pattern = patternPresets[key]
  }
}

// 监听序列化结果变化，向外 emit
watch(serializedValue, (val) => {
  emit('update:modelValue', val)
}, { immediate: false })

// 监听类型变化或外部值变化，重新解析
watch(() => props.type, () => { parseValue() }, { immediate: true })
watch(() => props.modelValue, () => { parseValue() }, { immediate: false })
</script>

<style scoped lang="less">
.constraint-value-editor {
  :deep(.ant-input-number) {
    background: #21262d;
    border-color: #30363d;
    color: #e6edf3;
  }
  :deep(.ant-alert-info) {
    background: rgba(88, 166, 255, 0.1);
    border-color: rgba(88, 166, 255, 0.3);
    color: #58a6ff;
  }
}
</style>
