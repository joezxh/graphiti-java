import type { Rule } from 'ant-design-vue/es/form'
import { isNumericType } from '@/composables/usePropertyType'
import type { OntPropertyVO } from '@/api/ontology'

/**
 * 根据属性定义生成 Ant Design Vue 表单校验规则
 * @param prop 属性定义 VO
 * @returns 校验规则数组
 */
export function getPropertyRules(prop: OntPropertyVO): Rule[] {
  const rules: Rule[] = []

  // 必填校验
  if (prop.isRequired) {
    rules.push({ required: true, message: `请填写 ${prop.localName}`, trigger: 'change' })
  }

  // 正则校验
  if (prop.pattern) {
    rules.push({
      pattern: new RegExp(prop.pattern),
      message: `格式不符合要求: ${prop.pattern}`,
      trigger: 'blur'
    })
  }

  // 数值范围校验（仅对数字类型）
  if (prop.minValue != null && prop.maxValue != null && isNumericType(prop.rangeDataType)) {
    rules.push({
      type: 'number',
      min: Number(prop.minValue),
      max: Number(prop.maxValue),
      message: `值应在 ${prop.minValue} - ${prop.maxValue} 之间`,
      trigger: 'blur'
    })
  }

  // 枚举值校验
  if (prop.allowedValues && prop.allowedValues.length > 0) {
    rules.push({
      type: 'enum',
      enum: prop.allowedValues,
      message: `值必须是以下之一: ${prop.allowedValues.join(', ')}`,
      trigger: 'change'
    })
  }

  return rules
}
