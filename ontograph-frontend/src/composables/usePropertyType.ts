import dayjs from 'dayjs'
import type { Dayjs } from 'dayjs'

/** 布尔类型判断 */
export function isBoolType(dt?: string): boolean {
  return dt === 'boolean' || dt === 'Boolean'
}

/** 数字类型判断 */
export function isNumericType(dt?: string): boolean {
  return ['integer', 'long', 'float', 'double', 'decimal', 'Int', 'Long', 'Float', 'Double'].includes(dt ?? '')
}

/** 日期类型判断 */
export function isDateType(dt?: string): boolean {
  return ['date', 'datetime', 'dateTime', 'Date', 'DateTime'].includes(dt ?? '')
}

/** 长文本类型判断 */
export function isLongTextType(dt?: string): boolean {
  return dt === 'text' || dt === 'Text'
}

/** 格式化属性值用于显示 */
export function formatPropertyValue(value: any, propType?: string, rangeDataType?: string): string {
  if (value === null || value === undefined) return '-'
  if (isBoolType(rangeDataType)) {
    return value === true || value === 'true' ? '是' : '否'
  }
  if (isDateType(rangeDataType) && (dayjs.isDayjs(value) || typeof value === 'string')) {
    const d = dayjs.isDayjs(value) ? value : dayjs(value)
    return d.isValid() ? d.format('YYYY-MM-DD') : String(value)
  }
  if (typeof value === 'object') {
    return JSON.stringify(value)
  }
  return String(value)
}

/** 解析后端值到前端类型 */
export function parsePropertyValue(value: any, rangeDataType?: string): any {
  if (value === null || value === undefined) return value
  if (isDateType(rangeDataType) && typeof value === 'string') {
    const d = dayjs(value)
    return d.isValid() ? d : value
  }
  if (isNumericType(rangeDataType) && typeof value === 'string') {
    const n = Number(value)
    return isNaN(n) ? value : n
  }
  if (isBoolType(rangeDataType) && typeof value === 'string') {
    return value === 'true' || value === '1'
  }
  return value
}

/** 序列化前端值到后端格式 */
export function serializePropertyValue(value: any, rangeDataType?: string): any {
  if (value === null || value === undefined) return value
  if (isDateType(rangeDataType) && dayjs.isDayjs(value)) {
    return value.format('YYYY-MM-DD')
  }
  return value
}
