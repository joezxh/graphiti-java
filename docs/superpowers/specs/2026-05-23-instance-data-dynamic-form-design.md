# 实例数据动态表单与表格设计文档

## 概述

基于本体定义的类属性，动态生成实例数据表格列和实例编辑表单，支持完整的数据类型处理和表单校验。

## 目标

1. 在数据表格中根据类属性动态生成列（仅显示当前过滤类的属性）
2. 在表单中根据类属性定义动态生成表单项
3. 实现基于类属性约束的表单校验规则
4. 确保日期、数字、布尔值等不同数据类型的正确处理
5. 实现枚举值、范围限制等特殊约束的验证
6. 保持现有功能的兼容性

## 架构设计

### 方法选择

**方法 2：提取共享逻辑（推荐）**

将数据类型判断、属性编辑器渲染、校验规则生成等逻辑提取到共享模块，避免 InstanceDataTable 和 InstanceForm 中的逻辑重复。

### 新建共享模块

#### 1. `usePropertyType.ts`（composable）

集中管理所有属性数据类型判断逻辑。

```typescript
// 数据类型判断
export function isBoolType(dt?: string): boolean
export function isNumericType(dt?: string): boolean
export function isDateType(dt?: string): boolean
export function isLongTextType(dt?: string): boolean

// 格式化显示值（用于表格单元格显示）
export function formatPropertyValue(value: any, propType: string, rangeDataType?: string): string
```

#### 2. `getPropertyRules.ts`（工具函数）

根据属性定义生成 Ant Design Vue 表单校验规则。

```typescript
import type { Rule } from 'ant-design-vue/es/form'

export function getPropertyRules(prop: OntPropertyVO): Rule[]
```

规则包括：
- 必填校验（`isRequired`）
- 正则校验（`pattern`）
- 数值范围校验（`minValue` / `maxValue`）
- 枚举值校验（`allowedValues`）

#### 3. `PropertyValueCell.vue`（组件）

表格单元格的显示/编辑组件，根据属性类型动态渲染。

**Props：**
- `value: any` — 当前值
- `propDef: OntPropertyVO` — 属性定义
- `editing: boolean` — 是否处于编辑模式

**事件：**
- `@update(value: any)` — 值更新
- `@start-edit` — 开始编辑

**渲染逻辑：**
- `DATATYPE` + `boolean` → `a-switch`
- `DATATYPE` + `integer`/`float` → `a-input-number`
- `DATATYPE` + `date`/`datetime` → `a-date-picker`
- `DATATYPE` + `allowedValues` → `a-select`
- `DATATYPE` + `text` → `a-textarea`
- 其他 → `a-input`

## InstanceDataTable.vue 改进

### 动态列生成

```typescript
const dynamicColumns = computed(() => {
  const cols: any[] = [
    { title: 'UUID', key: 'uuid', dataIndex: 'uuid', width: 220, fixed: 'left' },
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
          __propDef: prop  // 供 bodyCell 使用
        })
      })
    }
  }
  
  cols.push({ title: '操作', key: 'action', width: 120, fixed: 'right' })
  return cols
})
```

### 内联编辑

属性列使用 `PropertyValueCell` 组件渲染，支持双击进入编辑模式：

```vue
<template #bodyCell="{ column, record }">
  <template v-if="column.__propDef">
    <PropertyValueCell
      :value="getNestedValue(record, column.dataIndex)"
      :prop-def="column.__propDef"
      :editing="editingKey === record.uuid && editingColumn === column.key"
      @update="handleCellUpdate(record, column, $event)"
      @start-edit="startCellEdit(record, column)"
    />
  </template>
</template>
```

### 查看详情 Drawer

按属性定义格式化显示属性值：

```vue
<a-descriptions :column="1" bordered size="small">
  <a-descriptions-item label="UUID">{{ selectedRecord.uuid }}</a-descriptions-item>
  <a-descriptions-item label="名称">{{ selectedRecord.name }}</a-descriptions-item>
  <a-descriptions-item label="类型">{{ selectedRecord.type }}</a-descriptions-item>
  
  <a-descriptions-item 
    v-for="prop in selectedClassProperties" 
    :key="prop.id" 
    :label="prop.localName"
  >
    {{ formatPropertyValue(selectedRecord.properties?.[prop.localName], prop) }}
  </a-descriptions-item>
</a-descriptions>
```

## InstanceForm.vue 改进

### 复用共享逻辑

删除原有的 `isBoolType`、`isNumericType`、`isDateType` 函数，改为从 `usePropertyType.ts` 导入。

### 表单校验规则

使用 `getPropertyRules.ts` 生成校验规则：

```typescript
import { getPropertyRules } from '@/utils/getPropertyRules'

function getPropertyRules(prop: any) {
  return getPropertyRulesFromShared(prop)
}
```

### 数据类型处理优化

**加载实例时（字符串 → 对应类型）：**

```typescript
function parsePropertyValue(value: any, prop: OntPropertyVO): any {
  if (value == null) return value
  if (isDateType(prop.rangeDataType) && typeof value === 'string') {
    return dayjs(value)
  }
  if (isNumericType(prop.rangeDataType) && typeof value === 'string') {
    return Number(value)
  }
  if (isBoolType(prop.rangeDataType) && typeof value === 'string') {
    return value === 'true' || value === true
  }
  return value
}
```

**保存实例时（对应类型 → 可序列化格式）：**

```typescript
function serializePropertyValue(value: any, prop: OntPropertyVO): any {
  if (value == null) return value
  if (isDateType(prop.rangeDataType) && dayjs.isDayjs(value)) {
    return value.format('YYYY-MM-DD')
  }
  return value
}
```

## 数据流

```
后端 API
  ↓ getClassInstances
InstanceDataTable
  ↓ props.classType
动态列生成（基于 classType 过滤属性）
  ↓
表格渲染（每行按属性定义显示/编辑）
  ↓ 双击编辑
PropertyValueCell（按属性类型渲染编辑器）
  ↓ @update
graphApi.updateNode（保存到后端）
  ↓
后端校验（OntologyValidationService）
```

## 兼容性保证

| 现有功能 | 处理方式 |
|---------|---------|
| 批量删除 | 保持不变 |
| 导入导出 | 保持不变 |
| 右键菜单 | 保持不变 |
| 搜索过滤 | 保持不变 |
| InstanceForm 的扩展属性 | 保持不变 |
| InstanceForm 的节点选择器 | 保持不变 |

## 文件变更清单

### 新建文件

1. `graphiti-web/src/composables/usePropertyType.ts`
2. `graphiti-web/src/utils/getPropertyRules.ts`
3. `graphiti-web/src/components/Ontology/PropertyValueCell.vue`

### 修改文件

1. `graphiti-web/src/components/Ontology/InstanceDataTable.vue`
   - 动态列生成逻辑
   - bodyCell 插槽（属性列使用 PropertyValueCell）
   - 查看详情 Drawer 格式化显示

2. `graphiti-web/src/components/Ontology/InstanceForm.vue`
   - 导入共享的类型判断函数
   - 复用共享的校验规则生成函数
   - 数据类型解析和序列化逻辑

## 依赖关系

```
InstanceDataTable.vue
  ├── usePropertyType.ts
  ├── PropertyValueCell.vue
  └── getPropertyRules.ts（间接，通过 PropertyValueCell）

InstanceForm.vue
  ├── usePropertyType.ts
  └── getPropertyRules.ts

PropertyValueCell.vue
  └── usePropertyType.ts
```

## 测试要点

1. 表格列生成：classType 为空时只显示基础列；有 classType 时显示该类属性列
2. 内联编辑：各数据类型的编辑器正确渲染，值正确保存
3. 表单校验：必填、正则、范围、枚举校验正确触发
4. 日期处理：加载时字符串 → dayjs，保存时 dayjs → 字符串
5. 查看详情：属性值按类型正确格式化显示
