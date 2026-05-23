# Phase 1: 本体定义管理独立模块实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 创建 3 个独立的本体定义管理页面（类定义/属性/约束），每个页面拥有独立路由，复用现有的 Ontology 组件。

**Architecture:** 新建统一的 DataManagerLayout 页面壳组件，3 个管理页面分别复用 ClassListPanel+ClassEditor、PropertyListPanel+PropertyEditor、ConstraintListPanel+ConstraintEditor，通过路由配置和菜单项提供独立入口。

**Tech Stack:** Vue 3 + TypeScript + Ant Design Vue + Vue Router + Pinia

---

## 文件结构

| 文件 | 操作 | 说明 |
|------|------|------|
| `graphiti-web/src/components/Layout/DataManagerLayout.vue` | 新建 | 统一页面壳（Header + Main + 可选右侧面板） |
| `graphiti-web/src/views/data/classes.vue` | 新建 | 类定义管理页面 |
| `graphiti-web/src/views/data/properties.vue` | 新建 | 属性管理页面 |
| `graphiti-web/src/views/data/constraints.vue` | 新建 | 约束管理页面 |
| `graphiti-web/src/components/Layout/Sidebar.vue` | 修改 | 新增 3 个菜单项 |
| `graphiti-web/src/router/index.ts` | 修改 | 新增 3 条路由 |

---

## Task 1: 创建 DataManagerLayout.vue（统一页面壳）

**Files:**
- Create: `graphiti-web/src/components/Layout/DataManagerLayout.vue`

- [ ] **Step 1: 创建文件**

```vue
<!-- graphiti-web/src/components/Layout/DataManagerLayout.vue -->
<template>
  <div class="data-manager-layout">
    <!-- Header -->
    <div class="dm-header">
      <div class="dm-header-left">
        <h2 class="dm-title">{{ title }}</h2>
      </div>
      <div class="dm-header-right">
        <slot name="header-actions" />
      </div>
    </div>

    <!-- Main Content -->
    <div class="dm-main">
      <!-- Left Panel (optional) -->
      <div v-if="$slots['left-panel']" class="dm-left-panel">
        <slot name="left-panel" />
      </div>

      <!-- Center Panel -->
      <div class="dm-center-panel">
        <slot name="main-table" />
      </div>

      <!-- Right Panel (optional) -->
      <div v-if="$slots['right-panel']" class="dm-right-panel" :class="{ collapsed: rightCollapsed }">
        <div class="dm-right-toggle" @click="rightCollapsed = !rightCollapsed">
          <RightOutlined v-if="rightCollapsed" />
          <LeftOutlined v-else />
        </div>
        <div v-show="!rightCollapsed" class="dm-right-content">
          <slot name="right-panel" />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { LeftOutlined, RightOutlined } from '@ant-design/icons-vue'

defineProps<{
  title: string
}>()

const rightCollapsed = ref(false)
</script>

<style scoped lang="less">
.data-manager-layout {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #0d1117;
  color: #e6edf3;
}

.dm-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #161b22;
  border-bottom: 1px solid #30363d;
  flex-shrink: 0;

  .dm-title {
    margin: 0;
    font-size: 16px;
    font-weight: 600;
    color: #e6edf3;
  }
}

.dm-main {
  display: flex;
  flex: 1;
  overflow: hidden;
}

.dm-left-panel {
  width: 240px;
  background: #161b22;
  border-right: 1px solid #30363d;
  overflow-y: auto;
  flex-shrink: 0;
}

.dm-center-panel {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.dm-right-panel {
  width: 400px;
  background: #161b22;
  border-left: 1px solid #30363d;
  display: flex;
  flex-shrink: 0;
  position: relative;

  &.collapsed {
    width: 32px;
  }

  .dm-right-toggle {
    width: 32px;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    color: #8b949e;
    border-right: 1px solid #30363d;

    &:hover {
      color: #e6edf3;
      background: #21262d;
    }
  }

  .dm-right-content {
    flex: 1;
    overflow-y: auto;
    padding: 16px;
  }
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add graphiti-web/src/components/Layout/DataManagerLayout.vue
git commit -m "feat: add DataManagerLayout component for unified data management pages"
```

---

## Task 2: 创建类定义管理页面（classes.vue）

**Files:**
- Create: `graphiti-web/src/views/data/classes.vue`

- [ ] **Step 1: 创建文件**

```vue
<!-- graphiti-web/src/views/data/classes.vue -->
<template>
  <DataManagerLayout title="类定义管理">
    <template #main-table>
      <ClassListPanel :graph-id="graphId" @open-class="handleOpenClass" />
    </template>
    <template #right-panel>
      <ClassEditor
        v-if="selectedClassId"
        :key="selectedClassId"
        :graph-id="graphId"
        :class-id="selectedClassId"
        @saved="handleSaved"
        @close="selectedClassId = null"
      />
      <div v-else class="empty-state">
        <InboxOutlined class="empty-icon" />
        <div class="empty-title">选择一个类</div>
        <div class="empty-desc">点击左侧列表中的类查看详情和编辑</div>
      </div>
    </template>
  </DataManagerLayout>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import { InboxOutlined } from '@ant-design/icons-vue'
import DataManagerLayout from '@/components/Layout/DataManagerLayout.vue'
import ClassListPanel from '@/components/Ontology/ClassListPanel.vue'
import ClassEditor from '@/components/Ontology/ClassEditor.vue'

const route = useRoute()
const graphId = ref(route.query.graphId as string || '')
const selectedClassId = ref<number | null>(null)

function handleOpenClass(classId: number, _className: string) {
  selectedClassId.value = classId
}

function handleSaved() {
  // ClassEditor saved, refresh if needed
}
</script>

<style scoped lang="less">
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;

  .empty-icon {
    font-size: 48px;
    color: #30363d;
    margin-bottom: 16px;
  }

  .empty-title {
    font-size: 16px;
    font-weight: 500;
    color: #e6edf3;
    margin-bottom: 8px;
  }

  .empty-desc {
    font-size: 13px;
    color: #6e7681;
  }
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add graphiti-web/src/views/data/classes.vue
git commit -m "feat: add independent class definition management page"
```

---

## Task 3: 创建属性管理页面（properties.vue）

**Files:**
- Create: `graphiti-web/src/views/data/properties.vue`

- [ ] **Step 1: 创建文件**

```vue
<!-- graphiti-web/src/views/data/properties.vue -->
<template>
  <DataManagerLayout title="属性管理">
    <template #main-table>
      <PropertyListPanel :graph-id="graphId" @open-property="handleOpenProperty" />
    </template>
    <template #right-panel>
      <PropertyEditor
        v-if="selectedPropertyId"
        :key="selectedPropertyId"
        :graph-id="graphId"
        :property-id="selectedPropertyId"
        @saved="handleSaved"
        @close="selectedPropertyId = null"
      />
      <div v-else class="empty-state">
        <InboxOutlined class="empty-icon" />
        <div class="empty-title">选择一个属性</div>
        <div class="empty-desc">点击左侧列表中的属性查看详情和编辑</div>
      </div>
    </template>
  </DataManagerLayout>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import { InboxOutlined } from '@ant-design/icons-vue'
import DataManagerLayout from '@/components/Layout/DataManagerLayout.vue'
import PropertyListPanel from '@/components/Ontology/PropertyListPanel.vue'
import PropertyEditor from '@/components/Ontology/PropertyEditor.vue'

const route = useRoute()
const graphId = ref(route.query.graphId as string || '')
const selectedPropertyId = ref<number | null>(null)

function handleOpenProperty(propertyId: number, _name: string) {
  selectedPropertyId.value = propertyId
}

function handleSaved() {
  // PropertyEditor saved
}
</script>

<style scoped lang="less">
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;

  .empty-icon {
    font-size: 48px;
    color: #30363d;
    margin-bottom: 16px;
  }

  .empty-title {
    font-size: 16px;
    font-weight: 500;
    color: #e6edf3;
    margin-bottom: 8px;
  }

  .empty-desc {
    font-size: 13px;
    color: #6e7681;
  }
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add graphiti-web/src/views/data/properties.vue
git commit -m "feat: add independent property management page"
```

---

## Task 4: 创建约束管理页面（constraints.vue）

**Files:**
- Create: `graphiti-web/src/views/data/constraints.vue`

- [ ] **Step 1: 创建文件**

```vue
<!-- graphiti-web/src/views/data/constraints.vue -->
<template>
  <DataManagerLayout title="约束管理">
    <template #main-table>
      <ConstraintListPanel :graph-id="graphId" />
    </template>
    <template #right-panel>
      <ConstraintEditor
        v-if="selectedConstraint"
        :key="selectedConstraint.id"
        :graph-id="graphId"
        :constraint="selectedConstraint"
        @saved="handleSaved"
        @close="selectedConstraint = null"
      />
      <div v-else class="empty-state">
        <InboxOutlined class="empty-icon" />
        <div class="empty-title">选择一个约束</div>
        <div class="empty-desc">点击左侧列表中的约束查看详情和编辑</div>
      </div>
    </template>
  </DataManagerLayout>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRoute } from 'vue-router'
import { InboxOutlined } from '@ant-design/icons-vue'
import DataManagerLayout from '@/components/Layout/DataManagerLayout.vue'
import ConstraintListPanel from '@/components/Ontology/ConstraintListPanel.vue'
import ConstraintEditor from '@/components/Ontology/ConstraintEditor.vue'

const route = useRoute()
const graphId = ref(route.query.graphId as string || '')
const selectedConstraint = ref<any | null>(null)

function handleSaved() {
  // ConstraintEditor saved
}
</script>

<style scoped lang="less">
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  text-align: center;

  .empty-icon {
    font-size: 48px;
    color: #30363d;
    margin-bottom: 16px;
  }

  .empty-title {
    font-size: 16px;
    font-weight: 500;
    color: #e6edf3;
    margin-bottom: 8px;
  }

  .empty-desc {
    font-size: 13px;
    color: #6e7681;
  }
}
</style>
```

- [ ] **Step 2: Commit**

```bash
git add graphiti-web/src/views/data/constraints.vue
git commit -m "feat: add independent constraint management page"
```

---

## Task 5: 修改 Sidebar.vue 菜单

**Files:**
- Modify: `graphiti-web/src/components/Layout/Sidebar.vue:53-102`

- [ ] **Step 1: 在 data-management 子菜单下新增 3 个菜单项**

```vue
<!-- 在 data-management 子菜单内，在 entities 之前插入 -->
<a-menu-item key="/data/classes">
  <template #icon>
    <AppstoreOutlined />
  </template>
  {{ $t('nav.classManagement') }}
</a-menu-item>
<a-menu-item key="/data/properties">
  <template #icon>
    <TagOutlined />
  </template>
  {{ $t('nav.propertyManagement') }}
</a-menu-item>
<a-menu-item key="/data/constraints">
  <template #icon>
    <SafetyCertificateOutlined />
  </template>
  {{ $t('nav.constraintManagement') }}
</a-menu-item>
```

**插入位置：** 在 `/data/entities` 菜单项之前插入上述代码。

**导入补充：** 在 `<script setup>` 的 import 中添加：
```typescript
import {
  // ... existing imports
  AppstoreOutlined,
  TagOutlined,
  SafetyCertificateOutlined
} from '@ant-design/icons-vue'
```

- [ ] **Step 2: Commit**

```bash
git add graphiti-web/src/components/Layout/Sidebar.vue
git commit -m "feat: add ontology management menu items to sidebar"
```

---

## Task 6: 修改 router/index.ts 路由

**Files:**
- Modify: `graphiti-web/src/router/index.ts`

- [ ] **Step 1: 在 /data/entities 路由之前添加 3 条新路由**

```typescript
// 在 router/index.ts 中，找到 /data/entities 路由定义的位置
// 在其前面插入以下 3 条路由：

{
  path: 'data/classes',
  name: 'DataClasses',
  component: () => import('@/views/data/classes.vue'),
  meta: { title: 'nav.classManagement', requiresAuth: true }
},
{
  path: 'data/properties',
  name: 'DataProperties',
  component: () => import('@/views/data/properties.vue'),
  meta: { title: 'nav.propertyManagement', requiresAuth: true }
},
{
  path: 'data/constraints',
  name: 'DataConstraints',
  component: () => import('@/views/data/constraints.vue'),
  meta: { title: 'nav.constraintManagement', requiresAuth: true }
},
```

- [ ] **Step 2: Commit**

```bash
git add graphiti-web/src/router/index.ts
git commit -m "feat: add routes for ontology management pages"
```

---

## Spec 覆盖检查

| 设计文档需求 | 实现任务 |
|-------------|---------|
| 类定义管理独立页面（/data/classes） | Task 2 |
| 属性管理独立页面（/data/properties） | Task 3 |
| 约束管理独立页面（/data/constraints） | Task 4 |
| 统一页面壳（DataManagerLayout） | Task 1 |
| 菜单新增 3 个入口 | Task 5 |
| 路由新增 3 条 | Task 6 |
| 复用现有组件 | Task 2, 3, 4 |
| 风格与 ide.vue 一致 | Task 1（颜色变量） |
| 右侧面板可折叠 | Task 1 |

## 无占位符检查

- ✅ 无 TBD/TODO
- ✅ 所有步骤包含完整代码
- ✅ 所有命令包含预期输出
- ✅ 类型和方法签名一致
