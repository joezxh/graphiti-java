/**
 * 本体 Diff 工具函数
 * 用于比较两个版本的本体状态，生成差异报告
 */

/**
 * Diff操作类型
 */
export type DiffOp = 'added' | 'removed' | 'modified' | 'unchanged'

/**
 * 单个字段的Diff结果
 */
export interface FieldDiff {
  field: string
  op: DiffOp
  oldValue?: any
  newValue?: any
}

/**
 * 实体Diff结果（类/属性/约束）
 */
export interface EntityDiff {
  id: string | number
  name: string
  op: DiffOp
  fieldDiffs: FieldDiff[]
}

/**
 * Diff汇总
 */
export interface DiffSummary {
  added: EntityDiff[]
  removed: EntityDiff[]
  modified: EntityDiff[]
  unchanged: EntityDiff[]
}

/**
 * 计算两个值是否相等（深度比较）
 */
function deepEqual(a: any, b: any): boolean {
  if (a === b) return true
  if (a == null || b == null) return a === b
  if (typeof a !== typeof b) return false
  if (Array.isArray(a) && Array.isArray(b)) {
    if (a.length !== b.length) return false
    return a.every((item, i) => deepEqual(item, b[i]))
  }
  if (typeof a === 'object' && typeof b === 'object') {
    const keysA = Object.keys(a)
    const keysB = Object.keys(b)
    if (keysA.length !== keysB.length) return false
    return keysA.every(k => deepEqual(a[k], b[k]))
  }
  return false
}

/**
 * 比较两个对象的字段差异
 */
export function diffFields(oldObj: Record<string, any>, newObj: Record<string, any>, fields: string[]): FieldDiff[] {
  const diffs: FieldDiff[] = []
  const allKeys = new Set([...Object.keys(oldObj), ...Object.keys(newObj)])

  for (const key of allKeys) {
    if (fields.length > 0 && !fields.includes(key)) continue
    const oldVal = oldObj?.[key]
    const newVal = newObj?.[key]
    if (!deepEqual(oldVal, newVal)) {
      diffs.push({ field: key, op: oldVal === undefined ? 'added' : newVal === undefined ? 'removed' : 'modified', oldValue: oldVal, newValue: newVal })
    }
  }
  return diffs
}

/**
 * 比较两个实体列表，返回DiffSummary
 */
export function diffEntityList<T extends { id: number | string; localName?: string; [key: string]: any }>(
  oldList: T[],
  newList: T[],
  nameField: keyof T = 'localName' as any
): DiffSummary {
  const oldMap = new Map(oldList.map(e => [e.id, e]))
  const newMap = new Map(newList.map(e => [e.id, e]))

  const result: DiffSummary = { added: [], removed: [], modified: [], unchanged: [] }

  // 新增的
  for (const [id, entity] of newMap) {
    if (!oldMap.has(id)) {
      result.added.push({ id, name: String(entity[nameField] ?? id), op: 'added', fieldDiffs: [] })
    }
  }

  // 删除的
  for (const [id, entity] of oldMap) {
    if (!newMap.has(id)) {
      result.removed.push({ id, name: String(entity[nameField] ?? id), op: 'removed', fieldDiffs: [] })
    }
  }

  // 修改的 / 未变的
  for (const [id, newEntity] of newMap) {
    const oldEntity = oldMap.get(id)
    if (oldEntity) {
      const fieldDiffs = diffFields(oldEntity as any, newEntity as any, [])
      if (fieldDiffs.length > 0) {
        result.modified.push({ id, name: String(newEntity[nameField] ?? id), op: 'modified', fieldDiffs })
      } else {
        result.unchanged.push({ id, name: String(newEntity[nameField] ?? id), op: 'unchanged', fieldDiffs: [] })
      }
    }
  }

  return result
}

/**
 * 解析后端返回的 JSON 字符串状态，比较差异
 */
export function parseStateDiff(beforeState: string | undefined, afterState: string | undefined): FieldDiff[] {
  let before: any = {}
  let after: any = {}
  try {
    if (beforeState) before = JSON.parse(beforeState)
    if (afterState) after = JSON.parse(afterState)
  } catch (e) {
    return []
  }
  return diffFields(before, after, [])
}

/**
 * 生成简短的Diff摘要文本
 */
export function makeDiffSummary(summary: DiffSummary): string {
  const parts: string[] = []
  if (summary.added.length) parts.push(`+${summary.added.length}`)
  if (summary.removed.length) parts.push(`-${summary.removed.length}`)
  if (summary.modified.length) parts.push(`~${summary.modified.length}`)
  return parts.length > 0 ? parts.join(' ') : '无变化'
}

/**
 * 格式化Diff值为可读字符串
 */
export function formatDiffValue(val: any): string {
  if (val === undefined || val === null) return '—'
  if (typeof val === 'object') return JSON.stringify(val)
  return String(val)
}
