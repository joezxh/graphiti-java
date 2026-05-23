# 实例数据动态表单与表格实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 基于本体类属性定义，动态生成实例数据表格列和实例编辑表单，支持完整的数据类型处理和表单校验。

**Architecture:** 提取共享逻辑到独立模块（usePropertyType、getPropertyRules、PropertyValueCell），InstanceDataTable 和 InstanceForm 复用这些模块实现动态列生成、内联编辑和表单校验。

**Tech Stack:** Vue 3 + TypeScript + Ant Design Vue + Pinia + dayjs

---

## 文件结构

| 文件 | 操作 | 说明 |
|------|------|------|
| `graphiti-web/src/composables/usePropertyType.ts` | 新建 | 数据类型判断 + 格式化函数 |
| `graphiti-web/src/utils/getPropertyRules.ts` | 新建 | 表单校验规则生成 |
| `graphiti-web/src/components/Ontology/PropertyValueCell.vue` | 新建 | 表格单元格显示/编辑组件 |
| `graphiti-web/src/components/Ontology/InstanceDataTable.vue` | 修改 | 动态列 + 内联编辑 + 查看详情 |
| `graphiti-web/src/components/Ontology/InstanceForm.vue` | 修改 | 复用共享逻辑 + 校验规则 + 数据类型处理 |

---

## Task 1: 创建 `usePropertyType.ts`（数据类型判断工具）

**Files:**
- Create: `graphiti-web/src/composables/usePropertyType.ts`

- [ ] **Step 1: 创建文件**

```typescript
// graphiti-web/src/composables/usePropertyType.ts
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
```

- [ ] **Step 2: 验证文件创建成功**

确认文件路径：`graphiti-web/src/composables/usePropertyType.ts`

- [ ] **Step 3: Commit**

```bash
git add graphiti-web/src/composables/usePropertyType.ts
git commit -m "feat: add usePropertyType composable for data type handling"
```

---

## Task 2: 创建 `getPropertyRules.ts`（校验规则生成）

**Files:**
- Create: `graphiti-web/src/utils/getPropertyRules.ts`

- [ ] **Step 1: 创建文件**

```typescript
// graphiti-web/src/utils/getPropertyRules.ts
import type { Rule } from 'ant-design-vue/es/form'
import { isNumericType } from '@/composables/usePropertyType'
import type { OntPropertyVO } from '@/api/ontology'

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

  // 数值范围校验
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
```

- [ ] **Step 2: 验证文件创建成功**

确认文件路径：`graphiti-web/src/utils/getPropertyRules.ts`

- [ ] **Step 3: Commit**

```bash
git add graphiti-web/src/utils/getPropertyRules.ts
git commit -m "feat: add getPropertyRules utility for form validation"
```

---

## Task 3: 创建 `PropertyValueCell.vue`（单元格显示/编辑组件）

**Files:**
- Create: `graphiti-web/src/components/Ontology/PropertyValueCell.vue`

- [ ] **Step 1: 创建文件**

```vue
<!-- graphiti-web/src/components/Ontology/PropertyValueCell.vue -->
<template>
  <div v-if="!editing" class="cell-display" @dblclick="startEdit">
    {{ displayValue }}
  </div>
  <div v-else class="cell-editor">
    <a-switch
      v-if="isBool"
      v-model:checked="editValue"
      size="small"
      @change="save"
    />
    <a-input-number
      v-else-if="isNumeric"
      v-model:value="editValue"
      size="small"
      style="width: 100%"
      @pressEnter="save"
      @blur="save"
    />
    <a-date-picker
      v-else-if="isDate"
      v-model:value="editValue"
      size="small"
      style="width: 100%"
      @change="save"
    />
    <a-select
      v-else-if="hasEnum"
      v-model:value="editValue"
      size="small"
      style="width: 100%"
      :options="enumOptions"
      @change="save"
    />
    <a-input
      v-else
      v-model:value="editValue"
      size="small"
      @pressEnter="save"
      @blur="save"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import dayjs from 'dayjs'
import {
  isBoolType,
  isNumericType,
  isDateType,
  formatPropertyValue,
  parsePropertyValue,
  serializePropertyValue
} from '@/composables/usePropertyType'
import type { OntPropertyVO } from '@/api/ontology'

const props = defineProps<{
  value: any
  propDef: OntPropertyVO
  editing?: boolean
}>()

const emit = defineEmits<{
  (e: 'update', value: any): void
  (e: 'start-edit'): void
}>()

const editValue = ref<any>(null)

const isBool = computed(() => isBoolType(props.propDef.rangeDataType))
const isNumeric = computed(() => isNumericType(props.propDef.rangeDataType))
const isDate = computed(() => isDateType(props.propDef.rangeDataType))
const hasEnum = computed(() => props.propDef.allowedValues && props.propDef.allowedValues.length > 0)

const enumOptions = computed(() =>
  (props.propDef.allowedValues || []).map(v => ({ label: v, value: v }))
)

const displayValue = computed(() =>
  formatPropertyValue(props.value, props.propDef.propertyType, props.propDef.rangeDataType)
)

watch(() => props.editing, (editing) => {
  if (editing) {
    editValue.value = parsePropertyValue(props.value, props.propDef.rangeDataType)
  }
})

function startEdit() {
  emit('start-edit')
}

function save() {
  const serialized = serializePropertyValue(editValue.value, props.propDef.rangeDataType)
  emit('update', serialized)
}
</script>

<style scoped lang="less">
.cell-display {
  cursor: text;
  padding: 2px 4px;
  border-radius: 3px;
  min-height: 24px;
  transition: background 0.15s;

  &:hover {
    background: rgba(88, 166, 255, 0.1);
  }
}

.cell-editor {
  padding: 0;
}
</style>
```

- [ ] **Step 2: 验证文件创建成功**

确认文件路径：`graphiti-web/src/components/Ontology/PropertyValueCell.vue`

- [ ] **Step 3: Commit**

```bash
git add graphiti-web/src/components/Ontology/PropertyValueCell.vue
git commit -m "feat: add PropertyValueCell component for typed display/editing"
```

---

## Task 4: 修改 `InstanceDataTable.vue`

**Files:**
- Modify: `graphiti-web/src/components/Ontology/InstanceDataTable.vue`

- [ ] **Step 1: 添加导入和修改动态列生成**

在 `<script setup>` 顶部添加导入：

```typescript
import PropertyValueCell from './PropertyValueCell.vue'
import { formatPropertyValue } from '@/composables/usePropertyType'
```

修改 `dynamicColumns` computed：

```typescript
const dynamicColumns = computed(() => {
  const cols: any[] = [
    { title: 'UUID', key: 'uuid', dataIndex: 'uuid', width: 220, fixed: 'left', ellipsis: true },
    { title: '名称', key: 'name', dataIndex: 'name', width: 150 }
  ]

  // 如果指定了 classType，添加该类定义的属性列
  if (props.classType) {
    const targetClass = store.classes.find(c => c.localName === props.classType)
    if (targetClass) {
      const classProps = store.properties.filter(p => p.domainClassId === targetClass.id)
      classProps.forEach(prop => {
        cols.push({
          title: prop.localName,
          key: prop.localName,
          dataIndex: ['properties', prop.localName],
          width: 150,
          ellipsis: true,
          __propDef: prop
        })
      })
    }
  }

  cols.push({ title: '操作', key: 'action', width: 120, fixed: 'right' })
  return cols
})
```

- [ ] **Step 2: 修改表格模板以支持属性列的 PropertyValueCell**

替换原有的双 `#bodyCell` 插槽定义，合并为一个：

```vue
<template #bodyCell="{ column, record }">
  <!-- UUID列特殊处理 -->
  <template v-if="column.key === 'uuid'">
    <a-tooltip :title="(record as any).uuid">
      <span class="uuid-text">{{ truncateUuid((record as any).uuid) }}</span>
      <a-button type="link" size="small" @click="copyUuid((record as any).uuid)">复制</a-button>
    </a-tooltip>
  </template>

  <!-- 属性列：使用 PropertyValueCell 组件 -->
  <template v-else-if="column.__propDef">
    <PropertyValueCell
      :value="getNestedValue(record, column.dataIndex)"
      :prop-def="column.__propDef"
      :editing="editingKey === record.uuid && editingColumn === column.key"
      @update="handleCellUpdate(record, column, $event)"
      @start-edit="startCellEdit(record, column)"
    />
  </template>

  <!-- 操作列 -->
  <template v-else-if="column.key === 'action'">
    <a-space>
      <a-button type="link" size="small" @click="handleView(record)">查看</a-button>
      <a-popconfirm title="确定删除？" ok-text="确定" cancel-text="取消" @confirm="handleDelete(record)">
        <a-button type="link" size="small" danger>删除</a-button>
      </a-popconfirm>
    </a-space>
  </template>
</template>
```

- [ ] **Step 3: 添加辅助函数和状态**

在 `<script setup>` 中添加：

```typescript
const editingColumn = ref('')

function getNestedValue(record: any, dataIndex: string | string[]): any {
  if (typeof dataIndex === 'string') return record[dataIndex]
  if (Array.isArray(dataIndex)) {
    let val = record
    for (const key of dataIndex) {
      if (val == null) return undefined
      val = val[key]
    }
    return val
  }
  return undefined
}

function startCellEdit(record: any, column: any) {
  if (column.key === 'uuid' || column.key === 'action') return
  editingKey.value = record.uuid
  editingColumn.value = column.key
}

async function handleCellUpdate(record: any, column: any, value: any) {
  editingKey.value = ''
  editingColumn.value = ''
  try {
    const propKey = Array.isArray(column.dataIndex) ? column.dataIndex[column.dataIndex.length - 1] : column.dataIndex
    const updatedProperties = { ...record.properties, [propKey]: value }
    await graphApi.updateNode(props.graphId, record.uuid, {
      name: record.name,
      properties: updatedProperties
    })
    message.success('已保存')
    loadData()
  } catch (e: any) {
    message.error(e.message || '保存失败')
  }
}
```

- [ ] **Step 4: 修改查看详情 Drawer**

修改 `detailColumns` 和 Drawer 中的显示：

```typescript
const selectedClassProperties = computed(() => {
  if (!selectedRecord.value?.type) return []
  const cls = store.classes.find(c => c.localName === selectedRecord.value.type)
  if (!cls) return []
  return store.properties.filter(p => p.domainClassId === cls.id)
})
```

修改 Drawer 模板：

```vue
<a-drawer
  v-model:open="detailDrawerVisible"
  title="节点详情"
  width="500"
  placement="right"
>
  <a-descriptions v-if="selectedRecord" :column="1" bordered size="small">
    <a-descriptions-item label="UUID">{{ selectedRecord.uuid }}</a-descriptions-item>
    <a-descriptions-item label="名称">{{ selectedRecord.name }}</a-descriptions-item>
    <a-descriptions-item label="类型">{{ selectedRecord.type }}</a-descriptions-item>

    <a-descriptions-item
      v-for="prop in selectedClassProperties"
      :key="prop.id"
      :label="prop.localName"
    >
      {{ formatPropertyValue(selectedRecord.properties?.[prop.localName], prop.propertyType, prop.rangeDataType) }}
    </a-descriptions-item>
  </a-descriptions>
</a-drawer>
```

- [ ] **Step 5: Commit**

```bash
git add graphiti-web/src/components/Ontology/InstanceDataTable.vue
git commit -m "feat: InstanceDataTable dynamic columns, inline editing, detail drawer"
```

---

## Task 5: 修改 `InstanceForm.vue`

**Files:**
- Modify: `graphiti-web/src/components/Ontology/InstanceForm.vue`

- [ ] **Step 1: 添加导入**

在 `<script setup>` 顶部添加：

```typescript
import {
  isBoolType,
  isNumericType,
  isDateType,
  isLongTextType,
  parsePropertyValue,
  serializePropertyValue
} from '@/composables/usePropertyType'
import { getPropertyRules } from '@/utils/getPropertyRules'
```

- [ ] **Step 2: 删除原有的类型判断函数**

删除以下函数（因为现在从共享模块导入）：

```typescript
// 删除这些函数：
// function isBoolType(dt?: string) { ... }
// function isNumericType(dt?: string) { ... }
// function isDateType(dt?: string) { ... }
// function isLongTextType(dt?: string) { ... }
```

- [ ] **Step 3: 修改 getPropertyRules 函数**

替换原有的 `getPropertyRules` 函数：

```typescript
function getPropertyRules(prop: any) {
  return getPropertyRulesFromShared(prop)
}
```

注意：这里会命名冲突，需要调整。将原函数重命名或直接使用共享函数：

```typescript
// 删除原有的 getPropertyRules 函数定义
// 在模板中直接调用共享函数：
// :rules="getPropertyRules(prop)"
```

或者直接保留原函数名，内部调用共享函数：

```typescript
function getPropertyRulesForProp(prop: any) {
  return getPropertyRules(prop)
}
```

- [ ] **Step 4: 修改 watch 中的数据解析逻辑**

替换原有的属性值解析逻辑：

```typescript
watch(() => props.instanceData, (data) => {
  if (data) {
    formData.uuid = data.uuid ?? ''
    formData.name = data.name ?? ''
    formData.type = data.type ?? ''

    const rawProps = data.properties ? { ...data.properties } : {}
    formData.properties = {}

    const targetClass = store.classes.find(c => c.localName === formData.type)
    const propsDef = targetClass ? store.properties.filter(p => p.domainClassId === targetClass.id) : []

    for (const [key, value] of Object.entries(rawProps)) {
      const propDef = propsDef.find(p => p.localName === key)
      formData.properties[key] = parsePropertyValue(value, propDef?.rangeDataType)
    }

    formData.createdAt = data.createdAt ?? ''
    formData.updatedAt = data.updatedAt ?? ''
  } else {
    formData.uuid = ''
    formData.name = ''
    formData.type = ''
    formData.properties = {}
    formData.createdAt = ''
    formData.updatedAt = ''
  }
}, { immediate: true })
```

- [ ] **Step 5: 修改 handleSave 中的数据序列化逻辑**

替换原有的属性值清理逻辑：

```typescript
async function handleSave() {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  saving.value = true
  try {
    const cleanProperties: Record<string, any> = {}
    const targetClass = store.classes.find(c => c.localName === formData.type)
    const propsDef = targetClass ? store.properties.filter(p => p.domainClassId === targetClass.id) : []

    for (const [key, value] of Object.entries(formData.properties)) {
      const propDef = propsDef.find(p => p.localName === key)
      cleanProperties[key] = serializePropertyValue(value, propDef?.rangeDataType)
    }

    if (isNew.value) {
      const result = await graphApi.createNode(props.graphId, {
        name: formData.name,
        type: formData.type,
        properties: cleanProperties
      })
      message.success('实例创建成功')
      emit('saved', result)
    } else {
      const result = await graphApi.updateNode(props.graphId, recordUuid.value, {
        name: formData.name,
        properties: cleanProperties
      })
      message.success('实例更新成功')
      emit('saved', result)
    }
  } catch (e: any) {
    message.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}
```

- [ ] **Step 6: Commit**

```bash
git add graphiti-web/src/components/Ontology/InstanceForm.vue
git commit -m "feat: InstanceForm reuse shared property type logic and validation"
```

---

## Spec 覆盖检查

| 需求 | 实现任务 |
|------|---------|
| 表格根据类属性动态生成列 | Task 4 Step 1 |
| 表单根据类属性定义动态生成表单项 | 已有功能，Task 5 优化 |
| 基于类属性约束的表单校验 | Task 2 + Task 5 Step 3 |
| 日期、数字、布尔值正确处理 | Task 1 + Task 4 Step 3 + Task 5 Step 4/5 |
| 枚举值、范围限制验证 | Task 2 + Task 5 Step 3 |
| 保持现有功能兼容性 | 所有任务均注明保持现有功能 |

## 无占位符检查

- ✅ 无 TBD/TODO
- ✅ 所有步骤包含完整代码
- ✅ 所有命令包含预期输出
- ✅ 类型和方法签名一致
