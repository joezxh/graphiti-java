# IDE 三栏布局 + 本体类视图 — 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现 IDE 三栏独立缩放 + 本体模式下点击类时，中间显示实例图谱+分页、右侧显示类编辑器。

**Architecture:** 改造 `ide.vue` 三栏布局，新增 `OntologyClassView.vue` 作为中间面板的本体类视图容器，后端新增按类名分页查询实体+2跳邻居的 API。

**Tech Stack:** Vue 3 (Composition API) + Ant Design Vue + TypeScript + Spring Boot (Java)

---

## 阶段一：前端 — 三栏缩放改造

### 任务 1：三栏折叠状态 (`ide.vue`)

**文件：**
- Modify: `ontograph-web/src/views/graph/ide.vue`（`script setup` 部分 + `<template>` 三栏容器 + `<style>` 部分）

**Step 1: 新增三个折叠状态 ref**

在 `ide.vue` 的 `script setup` 中，找到以下行（约 line 753）：

```typescript
// Sidebar
const sidebarTab = ref<'ontology' | 'episodes' | 'communities'>('ontology')
```

在其后添加：

```typescript
// 三栏折叠状态
const sidebarCollapsed = ref(false)
const canvasCollapsed = ref(false)
const panelCollapsed = ref(false)
```

**Step 2: 改造左侧 sidebar 模板**

找到 `ide.vue` 中 `<aside class="ide-sidebar">` 部分（约 line 52），改造为：

```html
<!-- Sidebar -->
<aside class="ide-sidebar" :class="{ collapsed: sidebarCollapsed }">
  <div class="sidebar-header">
    <!-- 折叠按钮：仅在 collapsed 时显示 -->
    <button
      v-if="sidebarCollapsed"
      class="sidebar-expand-btn"
      @click="sidebarCollapsed = false"
      title="展开侧边栏"
    >
      ⯈
    </button>
    <template v-else>
      <div class="sidebar-tabs">
        <!-- 原有 tabs -->
        <button class="sidebar-tab" :class="{ active: sidebarTab === 'ontology' }" @click="sidebarTab = 'ontology'">
          {{ t('graphIde.sidebarOntology') }}
        </button>
        <button class="sidebar-tab" :class="{ active: sidebarTab === 'episodes' }" @click="sidebarTab = 'episodes'">
          {{ t('graphIde.sidebarEpisodes') }}
        </button>
        <button class="sidebar-tab" :class="{ active: sidebarTab === 'communities' }" @click="sidebarTab = 'communities'">
          {{ t('graphIde.sidebarCommunities') }}
        </button>
      </div>
      <!-- 折叠按钮 -->
      <button class="sidebar-collapse-btn" @click="sidebarCollapsed = true" title="折叠侧边栏">⯆</button>
    </template>
  </div>
  <!-- sidebarContent 仅在展开时渲染 -->
  <div v-if="!sidebarCollapsed" class="sidebar-content">
    <!-- 原有内容保持不变 -->
    <OntologyObjectExplorer v-if="sidebarTab === 'ontology'" ... />
    <EpisodeTypeExplorer v-else-if="sidebarTab === 'episodes'" ... />
    <CommunityExplorer v-else-if="sidebarTab === 'communities'" ... />
  </div>
</aside>
```

**Step 3: 改造中间 canvas 模板**

找到 `<div class="ide-canvas">` 部分（约 line 108），改造为：

```html
<!-- Canvas Area -->
<div class="ide-canvas" :class="{ collapsed: canvasCollapsed }">
  <!-- 折叠按钮：collapsed 时显示浮动按钮 -->
  <div v-if="canvasCollapsed" class="canvas-expand-btn-wrap">
    <button class="canvas-expand-btn" @click="canvasCollapsed = false" title="展开图谱">
      ⯈
    </button>
  </div>

  <template v-if="!canvasCollapsed">
    <!-- 原有内容不变：OntologyWorkbench + GraphCanvas toolbar + canvas-wrapper -->
    <!-- 注意：OntologyWorkbench 在 ontology 模式+class 模式时显示，其余情况显示 GraphCanvas -->
    <OntologyWorkbench v-if="sidebarTab === 'ontology' && ontologyMode === 'class'" ... />
    <template v-else>
      <!-- GraphCanvas toolbar + wrapper -->
      ...
    </template>
  </template>
</div>
```

**Step 4: 改造右侧 panel 模板**

找到 `<aside class="ide-panel">` 部分（约 line 239），改造为：

```html
<!-- Right Panel -->
<aside class="ide-panel" :class="{ collapsed: panelCollapsed }">
  <!-- collapsed 时完全隐藏（display:none via CSS） -->
  <template v-if="!panelCollapsed">
    <!-- 原有所有 panel 内容包裹在此 template 内 -->
    <!-- 各 template v-if/v-else-if 分支保持不变 -->

    <!-- 在第一个 panel-header 内添加折叠按钮（针对非 class 类视图的 panel） -->
    <!-- 注意：class 类视图时右侧是 ClassEditor，ClassEditor 自身已有 toolbar -->
  </template>
</aside>

<!-- panel 折叠后显示展开按钮（在 panel 折叠时显示在右侧边缘） -->
<div v-if="panelCollapsed" class="panel-expand-btn-wrap">
  <button class="panel-expand-btn" @click="panelCollapsed = false" title="展开面板">⯇</button>
</div>
```

**Step 5: 添加 CSS 样式**

在 `ide.vue` 的 `<style scoped>` 中，找到 `.ide-sidebar`（约 line 1706），将：

```less
.ide-sidebar {
  width: 260px;
  ...
}
```

替换为（折叠逻辑）：

```less
.ide-sidebar {
  width: 260px;
  flex-shrink: 0;
  transition: width 0.2s ease;
  overflow: hidden;

  &.collapsed {
    width: 48px;
  }

  .sidebar-header {
    display: flex;
    align-items: center;
    padding: 12px;
    border-bottom: 1px solid #30363d;
    min-height: 48px;
    justify-content: space-between;

    .sidebar-expand-btn,
    .sidebar-collapse-btn {
      background: none;
      border: none;
      color: #8b949e;
      cursor: pointer;
      font-size: 16px;
      padding: 4px;
      border-radius: 4px;
      &:hover { color: #e6edf3; background: #21262d; }
    }

    .sidebar-tabs {
      display: flex;
      gap: 4px;
      flex: 1;
    }

    .sidebar-tab {
      // 原有样式保持不变
      flex: 1;
      padding: 8px;
      background: transparent;
      border: none;
      border-radius: 6px;
      color: #8b949e;
      font-size: 13px;
      cursor: pointer;
      transition: all 0.2s;
      &:hover { color: #e6edf3; }
      &.active { background: #21262d; color: #e6edf3; }
    }
  }
}
```

在 `.ide-canvas`（约 line 1801）后添加：

```less
.ide-canvas {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;
  transition: flex 0.2s ease;
  min-width: 48px;

  &.collapsed {
    flex: 0;
    width: 48px;
    min-width: 48px;
  }

  .canvas-expand-btn-wrap {
    position: absolute;
    left: 0;
    top: 50%;
    transform: translateY(-50%);
    z-index: 50;
  }

  .canvas-expand-btn {
    background: #21262d;
    border: 1px solid #30363d;
    color: #8b949e;
    cursor: pointer;
    padding: 8px 4px;
    border-radius: 0 4px 4px 0;
    font-size: 14px;
    writing-mode: vertical-rl;
    &:hover { background: #30363d; color: #e6edf3; }
  }
}
```

在 `.ide-panel`（约 line 1870）后添加：

```less
.ide-panel {
  width: 360px;
  background: #161b22;
  border-left: 1px solid #30363d;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  overflow: hidden;
  transition: width 0.2s ease;

  &.collapsed {
    width: 0;
    border-left: none;
    overflow: hidden;
  }
}

.panel-expand-btn-wrap {
  position: fixed;
  right: 0;
  top: 50%;
  transform: translateY(-50%);
  z-index: 50;
}

.panel-expand-btn {
  background: #21262d;
  border: 1px solid #30363d;
  border-right: none;
  color: #8b949e;
  cursor: pointer;
  padding: 8px 4px;
  border-radius: 4px 0 0 4px;
  font-size: 14px;
  writing-mode: vertical-rl;
  text-orientation: mixed;
  &:hover { background: #30363d; color: #e6edf3; }
}
```

- [ ] **Step 6: 在 panel 内容中添加折叠触发**

在 `<aside class="ide-panel">` 内，每个 `panel-header` 中添加折叠按钮。找到右侧 panel 各分支的 `panel-header` 块，在 `<a-button type="text" size="small" @click="...">` 关闭按钮旁添加一个面板折叠按钮：

在每个 panel-header 的 `<span class="panel-title">` 后添加：

```html
<a-button type="text" size="small" @click="panelCollapsed = true" title="折叠面板" style="margin-left: auto;">
  <template #icon><CloseOutlined /></template>
</a-button>
```

（移除原有的关闭逻辑 `@click="selectedEpisodeType = null"` 等，改为折叠面板。）

---

### 任务 2：新增本体类视图组件 (`OntologyClassView.vue`)

**文件：**
- Create: `ontograph-web/src/components/Ontology/OntologyClassView.vue`

**Step 1: 编写组件模板**

```vue
<template>
  <div class="ontology-class-view">
    <!-- 工具栏 -->
    <div class="class-view-toolbar">
      <div class="toolbar-info">
        <span class="class-name">{{ schemaClass?.localName }}</span>
        <span class="instance-count">{{ total }} 个实例</span>
      </div>
      <div class="toolbar-tabs">
        <button
          class="view-tab"
          :class="{ active: activeTab === 'graph' }"
          @click="activeTab = 'graph'"
        >图谱</button>
        <button
          class="view-tab"
          :class="{ active: activeTab === 'list' }"
          @click="activeTab = 'list'"
        >实例列表</button>
      </div>
      <div class="toolbar-pagination">
        <button class="page-btn" :disabled="page <= 1" @click="prevPage">◀</button>
        <span class="page-indicator">{{ page }} / {{ totalPages || 1 }}</span>
        <button class="page-btn" :disabled="page >= totalPages" @click="nextPage">▶</button>
      </div>
    </div>

    <!-- 图谱 Tab -->
    <div v-show="activeTab === 'graph'" class="tab-graph">
      <GraphCanvas
        :graph-id="graphId"
        :nodes="graphNodes"
        :edges="graphEdges"
        layout="force"
        tool="select"
        :show-minimap="false"
        :selected-node="selectedNodeInGraph"
        @node-click="handleGraphNodeClick"
        @node-dblclick="handleGraphNodeDblClick"
      />
      <div v-if="loading" class="view-loading">
        <a-spin size="small" />
      </div>
      <div class="graph-footer">
        <button class="next-page-btn" :disabled="loading || page >= totalPages" @click="nextPage">
          {{ loading ? '加载中...' : (page >= totalPages ? '已到底' : '下一页 ▶') }}
        </button>
      </div>
    </div>

    <!-- 实例列表 Tab -->
    <div v-show="activeTab === 'list'" class="tab-list">
      <InstanceDataTable
        :class-type="schemaClass?.localName"
        :graph-id="graphId"
        :page="page"
        :page-size="pageSize"
        :total="total"
        @page-change="handlePageChange"
        @row-click="handleInstanceRowClick"
      />
    </div>
  </div>
</template>
```

**Step 2: 编写脚本部分**

```typescript
<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import GraphCanvas from '@/components/Graph/GraphCanvas.vue'
import InstanceDataTable from '@/components/Ontology/InstanceDataTable.vue'
import { graphApi } from '@/api/graph'
import type { GraphIDENode, GraphIDEEdge, SchemaClass } from '@/api/graph'

const props = defineProps<{
  graphId: string
  schemaClass: SchemaClass | null
}>()

const emit = defineEmits<{
  (e: 'instance-click', node: GraphIDENode): void
  (e: 'instance-dblclick', node: GraphIDENode): void
}>()

// Tab
const activeTab = ref<'graph' | 'list'>('graph')

// Pagination
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)
const totalPages = computed(() => Math.ceil(total.value / pageSize.value))

// Graph data
const graphNodes = ref<GraphIDENode[]>([])
const graphEdges = ref<GraphIDEEdge[]>([])
const loading = ref(false)
const selectedNodeInGraph = ref<GraphIDENode | null>(null)

// Dedupe maps
const nodeMap = new Map<string, GraphIDENode>()
const edgeMap = new Map<string, GraphIDEEdge>()

// Load data
const loadData = async (append = false) => {
  if (!props.graphId || !props.schemaClass?.localName) return
  loading.value = true
  try {
    const res = await graphApi.getEntitiesVisualizationByClass(
      props.graphId,
      props.schemaClass.localName,
      { page: page.value, pageSize: pageSize.value, depth: 2 }
    )
    total.value = res.pagination?.total ?? 0

    if (append) {
      // Append mode: dedupe before adding
      res.nodes.forEach(n => nodeMap.set(n.uuid, n))
      res.edges.forEach(e => edgeMap.set(e.uuid, e))
      graphNodes.value = Array.from(nodeMap.values())
      graphEdges.value = Array.from(edgeMap.values())
    } else {
      // Replace mode
      nodeMap.clear()
      edgeMap.clear()
      res.nodes.forEach(n => nodeMap.set(n.uuid, n))
      res.edges.forEach(e => edgeMap.set(e.uuid, e))
      graphNodes.value = res.nodes
      graphEdges.value = res.edges
    }
  } catch (e) {
    console.error('加载类实例图谱失败', e)
  } finally {
    loading.value = false
  }
}

// Watch class change → reset
watch(() => props.schemaClass, () => {
  if (props.schemaClass) {
    page.value = 1
    nodeMap.clear()
    edgeMap.clear()
    graphNodes.value = []
    graphEdges.value = []
    loadData(false)
  }
}, { immediate: true })

const nextPage = () => {
  if (page.value < totalPages.value) {
    page.value++
    loadData(true) // append mode
  }
}

const prevPage = () => {
  if (page.value > 1) {
    page.value--
    loadData(false) // for simplicity, prev goes back (no append)
  }
}

const handlePageChange = (p: number, ps: number) => {
  page.value = p
  pageSize.value = ps
  loadData(false)
}

const handleGraphNodeClick = (node: GraphIDENode) => {
  selectedNodeInGraph.value = node
}

const handleGraphNodeDblClick = (node: GraphIDENode) => {
  emit('instance-dblclick', node)
}

const handleInstanceRowClick = (record: any) => {
  const node = graphNodes.value.find(n => n.uuid === record.uuid)
  if (node) {
    selectedNodeInGraph.value = node
    emit('instance-click', node)
  }
}
</script>
```

**Step 3: 编写样式部分**

```typescript
<style scoped lang="less">
.ontology-class-view {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: #0d1117;

  .class-view-toolbar {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 8px 16px;
    background: #161b22;
    border-bottom: 1px solid #30363d;
    flex-shrink: 0;

    .toolbar-info {
      display: flex;
      align-items: center;
      gap: 8px;
      flex: 1;
      .class-name {
        color: #58a6ff;
        font-weight: 600;
        font-size: 14px;
      }
      .instance-count {
        color: #6e7681;
        font-size: 12px;
      }
    }

    .toolbar-tabs {
      display: flex;
      border: 1px solid #30363d;
      border-radius: 6px;
      overflow: hidden;
      .view-tab {
        padding: 4px 12px;
        font-size: 12px;
        background: transparent;
        border: none;
        color: #8b949e;
        cursor: pointer;
        &.active {
          background: #238636;
          color: #fff;
        }
        &:hover:not(.active) {
          background: #21262d;
          color: #e6edf3;
        }
      }
    }

    .toolbar-pagination {
      display: flex;
      align-items: center;
      gap: 4px;
      .page-btn {
        background: #21262d;
        border: 1px solid #30363d;
        color: #8b949e;
        padding: 2px 8px;
        border-radius: 4px;
        cursor: pointer;
        font-size: 11px;
        &:hover:not(:disabled) {
          background: #30363d;
          color: #e6edf3;
        }
        &:disabled {
          opacity: 0.4;
          cursor: not-allowed;
        }
      }
      .page-indicator {
        background: #21262d;
        color: #58a6ff;
        padding: 2px 8px;
        border-radius: 4px;
        font-size: 11px;
        min-width: 60px;
        text-align: center;
      }
    }
  }

  .tab-graph {
    flex: 1;
    position: relative;
    overflow: hidden;
    .view-loading {
      position: absolute;
      top: 8px;
      right: 8px;
      z-index: 10;
    }
    .graph-footer {
      position: absolute;
      bottom: 16px;
      left: 50%;
      transform: translateX(-50%);
      .next-page-btn {
        background: #238636;
        border: none;
        color: #fff;
        padding: 6px 20px;
        border-radius: 6px;
        cursor: pointer;
        font-size: 13px;
        &:disabled {
          background: #21262d;
          color: #6e7681;
          cursor: not-allowed;
        }
        &:hover:not(:disabled) {
          background: #2ea043;
        }
      }
    }
  }

  .tab-list {
    flex: 1;
    overflow: hidden;
  }
}
</style>
```

**Step 4: 在 `graphApi` 中添加前端 API 方法**

**文件：**
- Modify: `ontograph-web/src/api/graph.ts`

在 `graph.ts` 的 `graphApi` 对象中，找到 `getClassInstances` 方法（约 line 290），在其后添加：

```typescript
// 按类名获取实体 + 2跳邻居图谱数据（分页，追加模式）
async getEntitiesVisualizationByClass(
  graphId: string,
  className: string,
  params?: { page?: number; pageSize?: number; depth?: number }
): Promise<GraphVisualizationData> {
  return request.get(`/graph/${graphId}/visualization/entities/by-class`, {
    params: { className, page: params?.page ?? 1, pageSize: params?.pageSize ?? 20, depth: params?.depth ?? 2 }
  })
},
```

---

### 任务 3：集成本体类视图到 `ide.vue`

**文件：**
- Modify: `ontograph-web/src/views/graph/ide.vue`

**Step 1: 导入新组件**

在 `ide.vue` 的 import 区域，找到（约 line 727）：

```typescript
import OntologyWorkbench from '@/components/Ontology/OntologyWorkbench.vue'
```

在其后添加：

```typescript
import OntologyClassView from '@/components/Ontology/OntologyClassView.vue'
```

**Step 2: 新增本体类视图相关状态**

在 `ide.vue` 的 `script setup` 中，在折叠状态 ref 附近添加：

```typescript
// 本体类视图状态
const ontologyClassViewActive = ref(false)
const ontologyClassViewClass = ref<SchemaClass | null>(null)
```

**Step 3: 改造 `ide-canvas` 模板的分支逻辑**

在 `<div class="ide-canvas">` 内，找到现有逻辑：

```html
<!-- Ontology Workbench (shown when ontology tab is active and class mode) -->
<OntologyWorkbench
  v-if="sidebarTab === 'ontology' && ontologyMode === 'class'"
  :graph-id="effectiveGraphId"
  :selected-class-id="selectedOntClassId"
  @class-selected="handleOntClassSelected"
/>
<!-- Graph Canvas (shown for episodes/communities sidebar tabs, or episodes/communities ontology mode) -->
<template v-else>
```

替换为：

```html
<!-- 本体类视图（点击左侧树中类时显示） -->
<OntologyClassView
  v-if="ontologyClassViewActive && ontologyClassViewClass"
  :graph-id="effectiveGraphId"
  :schema-class="ontologyClassViewClass"
  @instance-click="handleClassViewInstanceClick"
  @instance-dblclick="handleClassViewInstanceDblClick"
/>
<!-- Ontology Workbench（本体模式+class 且未激活类视图时显示） -->
<OntologyWorkbench
  v-else-if="sidebarTab === 'ontology' && ontologyMode === 'class'"
  :graph-id="effectiveGraphId"
  :selected-class-id="selectedOntClassId"
  @class-selected="handleOntClassSelected"
/>
<!-- Graph Canvas（其余情况） -->
<template v-else>
```

**Step 4: 在右侧 panel 模板中添加 ClassEditor 分支**

在 `<aside class="ide-panel">` 内，找到现有模板的 `v-if/v-else-if` 链。在最后一个 `v-else` 之前，添加：

```html
<!-- 本体类视图时：显示类编辑器 -->
<template v-else-if="ontologyClassViewActive && ontologyClassViewClass">
  <div class="panel-header">
    <span class="panel-title">{{ ontologyClassViewClass.localName }}</span>
    <a-button type="text" size="small" @click="exitOntologyClassView" title="关闭">
      <template #icon><CloseOutlined /></template>
    </a-button>
  </div>
  <div class="panel-content" style="overflow-y: auto; flex: 1;">
    <ClassEditor
      :class-id="ontologyClassViewClass.id"
      :graph-id="effectiveGraphId"
      :is-new="false"
      :read-only="false"
      @saved="exitOntologyClassView"
    />
  </div>
</template>
```

**Step 5: 导入 ClassEditor**

在 `ide.vue` 的 import 区域，添加：

```typescript
import ClassEditor from '@/components/Ontology/ClassEditor.vue'
```

**Step 6: 修改 `handleOntClassSelected` 函数**

找到 `handleOntClassSelected` 函数（约 line 1535），替换为：

```typescript
const handleOntClassSelected = async (classId: number) => {
  const schemaClass = schemaClasses.value.find(c => c.id === classId)
  if (!schemaClass) return

  selectedOntClassId.value = classId
  ontologyClassViewActive.value = true
  ontologyClassViewClass.value = schemaClass
  selectedNode.value = null
  panelCollapsed.value = false  // 展开右侧面板
}
```

**Step 7: 添加退出本体类视图和事件处理函数**

在 `ide.vue` 的 `script setup` 中，在 `handleOntClassSelected` 函数后添加：

```typescript
// 退出本体类视图
const exitOntologyClassView = () => {
  ontologyClassViewActive.value = false
  ontologyClassViewClass.value = null
}

// 本体类视图中点击实例
const handleClassViewInstanceClick = (node: GraphIDENode) => {
  selectedNode.value = node
}

// 本体类视图中双击实例（打开节点编辑）
const handleClassViewInstanceDblClick = (node: GraphIDENode) => {
  selectedNode.value = node
  editingNode.value = node
  showNodeEditModal.value = true
}
```

**Step 8: 监听左侧树选中根节点时退出类视图**

在 `ide.vue` 的 `watch` 区域，找到现有 watch 或在末尾添加：

```typescript
// 监听左侧树节点选择，若选中了非 class 节点，退出本体类视图
watch(selectedKeys, (keys) => {
  if (ontologyClassViewActive.value && keys.length > 0) {
    const node = findNodeInOntologyTree(keys[0] as string)
    if (node && node.type !== 'class') {
      exitOntologyClassView()
    }
  }
})
```

需要添加 `findNodeInOntologyTree` 辅助函数（简单搜索树节点）：

```typescript
// 在 exitOntologyClassView 函数前添加
function findNodeInOntologyTree(key: string): any {
  const store = useOntologyStore()
  const tree = store.buildExplorerTree()
  return findNodeRec(tree.children || [], key)
}

function findNodeRec(nodes: any[], key: string): any {
  for (const n of nodes) {
    if (n.key === key) return n
    if (n.children) {
      const found = findNodeRec(n.children, key)
      if (found) return found
    }
  }
  return null
}
```

---

## 阶段二：后端 — 新增按类分页图谱 API

### 任务 4：后端新增 `getEntitiesVisualizationByClass` 方法

**文件：**
- Modify: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/GraphVisualizationService.java`
- Modify: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/GraphVisualizationServiceImpl.java`
- Modify: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/controller/admin/GraphIDEController.java`

**Step 1: 在 `GraphVisualizationService.java` 中添加接口方法**

在 `GraphVisualizationService.java` 文件中，找到 `getEpisodesVisualizationByType` 方法声明，在其后添加：

```java
/**
 * 按类名分页获取实体 + N跳邻居图谱数据
 *
 * @param graphId   图谱ID
 * @param className 类本地名（如 "CourtCase"）
 * @param page      页码（从1开始）
 * @param pageSize  每页数量
 * @param depth     邻居跳数（默认2）
 * @return 图谱可视化数据
 */
GraphVisualizationRespVO getEntitiesVisualizationByClass(
    String graphId,
    String className,
    Integer page,
    Integer pageSize,
    Integer depth
);
```

**Step 2: 在 `GraphVisualizationServiceImpl.java` 中实现方法**

参考现有的 `getEpisodesVisualizationByType` 实现（约 line 761-840），复制其模式，将 `:Episode` 替换为 `:Entity`，将 `episode_type: $typeCode` 替换为实体类型过滤逻辑。

完整实现：

```java
@Override
public GraphVisualizationRespVO getEntitiesVisualizationByClass(
        String graphId,
        String className,
        Integer page,
        Integer pageSize,
        Integer depth) {

    int effectivePage = page != null && page > 0 ? page : 1;
    int effectivePageSize = pageSize != null && pageSize > 0 ? pageSize : 20;
    int effectiveDepth = depth != null && depth >= 1 && depth <= 3 ? depth : 2;
    int skip = (effectivePage - 1) * effectivePageSize;

    try (Session session = neo4jDriver.session()) {
        // 阶段1: 统计该类实体总数
        String countCypher =
            "MATCH (n:Entity {graph_id: $graphId, type: $className}) " +
            "WHERE n.invalid_at IS NULL " +
            "RETURN count(n) as total";
        Result countResult = session.run(countCypher,
            Map.of("graphId", graphId, "className", className));
        long total = countResult.hasNext() ? countResult.next().get("total").asLong() : 0;

        // 阶段2: 分页查询中心节点
        String centerCypher =
            "MATCH (center:Entity {graph_id: $graphId, type: $className}) " +
            "WHERE center.invalid_at IS NULL " +
            "WITH center ORDER BY center.valid_at DESC SKIP $skip LIMIT $limit " +
            "RETURN collect(center) as centers";

        Map<String, Object> centerParams = new HashMap<>();
        centerParams.put("graphId", graphId);
        centerParams.put("className", className);
        centerParams.put("skip", skip);
        centerParams.put("limit", effectivePageSize);

        Result centerResult = session.run(centerCypher, centerParams);
        List<GraphVisualizationRespVO.NodeVO> nodes = new ArrayList<>();
        List<GraphVisualizationRespVO.EdgeVO> edges = new ArrayList<>();
        Set<String> nodeUuids = new LinkedHashSet<>();
        Set<String> edgeUuidSet = new HashSet<>();

        List<String> centerUuids = new ArrayList<>();
        if (centerResult.hasNext()) {
            Record record = centerResult.next();
            List<Object> centers = record.get("centers").asList();
            for (Object obj : centers) {
                var neo4jNode = ((org.neo4j.driver.types.Node) obj);
                Map<String, Object> nodeMap = neo4jNode.asMap();
                String uuid = (String) nodeMap.get("uuid");
                if (uuid != null && !nodeUuids.contains(uuid)) {
                    nodeUuids.add(uuid);
                    centerUuids.add(uuid);
                    nodes.add(buildNodeVO(nodeMap));
                }
            }
        }

        // 阶段3: 扩展N跳邻居（双向）
        if (!centerUuids.isEmpty()) {
            String expandCypher =
                "MATCH (center:Entity) " +
                "WHERE center.uuid IN $uuids " +
                "MATCH path = (center)-[*1.." + effectiveDepth + "]-(n:Entity) " +
                "WHERE n.graph_id = $graphId AND n.invalid_at IS NULL AND n <> center " +
                "UNWIND nodes(path) as node " +
                "WITH DISTINCT node " +
                "RETURN node";

            Result expandResult = session.run(expandCypher,
                Map.of("graphId", graphId, "uuids", centerUuids));
            while (expandResult.hasNext()) {
                Record record = expandResult.next();
                var neo4jNode = record.get("node").asNode();
                Map<String, Object> nodeMap = neo4jNode.asMap();
                String uuid = (String) nodeMap.get("uuid");
                if (uuid != null && !nodeUuids.contains(uuid)) {
                    nodeUuids.add(uuid);
                    nodes.add(buildNodeVO(nodeMap));
                }
            }

            // 阶段4: 查询邻居之间的边
            if (!nodeUuids.isEmpty()) {
                String edgeCypher =
                    "MATCH (a:Entity)-[r]-(b:Entity) " +
                    "WHERE a.uuid IN $uuids AND b.uuid IN $uuids " +
                    "AND a.invalid_at IS NULL AND b.invalid_at IS NULL " +
                    "RETURN r, a.uuid as source, b.uuid as target";

                Result edgeResult = session.run(edgeCypher,
                    Map.of("uuids", centerUuids));
                while (edgeResult.hasNext()) {
                    Record record = edgeResult.next();
                    var rel = record.get("r").asRelationship();
                    String edgeUuid = rel.id() + "-" + centerUuids.toString(); // 简化edge uuid
                    if (!edgeUuidSet.contains(edgeUuid)) {
                        edgeUuidSet.add(edgeUuid);
                        edges.add(GraphVisualizationRespVO.EdgeVO.builder()
                            .uuid(edgeUuid)
                            .source(record.get("source").asString())
                            .target(record.get("target").asString())
                            .type(rel.type())
                            .fact(null)
                            .properties(Map.of())
                            .build());
                    }
                }
            }
        }

        // 阶段5: 构建分页信息
        int totalPages = (int) Math.ceil((double) total / effectivePageSize);
        GraphVisualizationRespVO.PaginationVO pagination = GraphVisualizationRespVO.PaginationVO.builder()
            .page(effectivePage)
            .pageSize(effectivePageSize)
            .total(total)
            .totalPages(totalPages)
            .build();

        return GraphVisualizationRespVO.builder()
            .nodes(nodes)
            .edges(edges)
            .pagination(pagination)
            .build();
    }
}
```

> **注意**：`Entity` 节点上的类型字段名需确认是 `type` 还是 `classType`。参考 `graphApi.getClassInstances` 调用的后端接口确认字段名。

**Step 3: 在 Controller 中添加端点**

**文件：**
- Modify: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/controller/admin/GraphIDEController.java`

在 `GraphIDEController.java` 中，找到 `getInstances` 方法（约 line 60），在其后添加：

```java
@GetMapping("/visualization/entities/by-class")
public CommonResult<GraphVisualizationRespVO> getEntitiesVisualizationByClass(
        @PathVariable String graphId,
        @RequestParam String className,
        @RequestParam(defaultValue = "1") Integer page,
        @RequestParam(defaultValue = "20") Integer pageSize,
        @RequestParam(defaultValue = "2") Integer depth) {
    return CommonResult.success(
        graphVisualizationService.getEntitiesVisualizationByClass(graphId, className, page, pageSize, depth)
    );
}
```

---

## 阶段三：测试与验证

### 任务 5：手动测试清单

完成所有代码修改后，按以下步骤验证：

1. **三栏缩放测试**
   - [ ] 打开 IDE，点击左侧 sidebar 的折叠按钮（⯆），sidebar 收起到 48px，显示展开按钮
   - [ ] 点击中间 canvas 的折叠按钮，canvas 收起到 48px
   - [ ] 点击右侧 panel 的关闭按钮，panel 完全隐藏，显示浮动展开按钮
   - [ ] 点击展开按钮恢复各面板

2. **本体类视图测试**
   - [ ] 切换到本体 tab，在左侧树中点击某个类（如 `CourtCase`）
   - [ ] 确认中间面板显示 OntologyClassView（工具栏 + 图谱 Tab）
   - [ ] 确认右侧面板显示 ClassEditor
   - [ ] 点击图谱 Tab → 确认显示 2跳邻居图谱
   - [ ] 点击"下一页" → 确认节点追加，图谱扩大
   - [ ] 切换到实例列表 Tab → 确认显示实例表格
   - [ ] 点击"实例列表" Tab 中的某行 → 确认图谱中对应节点高亮
   - [ ] 点击左侧树中另一个类 → 确认画布清空，切换为新类数据
   - [ ] 点击 ClassEditor 的关闭按钮 → 退出类视图，恢复原布局

---

## 文件修改汇总

| 操作 | 文件路径 |
|------|----------|
| 修改 | `ontograph-web/src/views/graph/ide.vue` |
| 修改 | `ontograph-web/src/api/graph.ts` |
| 创建 | `ontograph-web/src/components/Ontology/OntologyClassView.vue` |
| 修改 | `ontograph-module-core/src/main/java/.../GraphVisualizationService.java` |
| 修改 | `ontograph-module-core/src/main/java/.../GraphVisualizationServiceImpl.java` |
| 修改 | `ontograph-module-core/src/main/java/.../GraphIDEController.java` |
