# IDE 三栏布局 + 本体模式增强 — 设计文档

**日期**：2026-05-24
**状态**：草稿

---

## 1. 背景与目标

Graphiti IDE (`ide.vue`) 现有布局为三固定面板：左侧树 + 中间图谱 + 右侧详情面板。本体模式下，当用户点击左侧树中的类时，当前行为不够直观——本体编辑内容混杂在 `OntologyWorkbench` 标签页中，无法与类实例数据形成联动。

本次改造目标是：

1. **三栏独立缩放**：左/中/右三个面板均可独立展开/收起
2. **本体模式新交互**：点击类 → 中间面板显示类实例图谱 + 分页；右侧显示类编辑器
3. **剧集/社区模式保持不变**

---

## 2. 设计决策汇总

| 问题 | 决策 |
|------|------|
| 缩放交互方式 | **方案 B：侧边折叠按钮** — 每个面板头部有折叠按钮，折叠时完全隐藏 |
| 中间面板内容顺序 | **Tab 切换** —「图谱」Tab（默认）+「实例列表」Tab，分页状态共享 |
| 分页数据追加方式 | **追加模式** — 下一页数据追加到画布，节点不重复 |
| 切换类时状态 | **替换模式** — 切换类时清空中间画布和右侧面板，完全加载新类数据 |
| 剧集/社区模式 | **暂不改造** — 保持现有行为不变 |

---

## 3. 布局改造

### 3.1 整体结构

```
┌─────────────────────────────────────────────────────────────────────┐
│  Header (56px, 固定)                                                │
├──────────────┬────────────────────────────────┬────────────────────┤
│  左侧面板     │  中间面板                        │  右侧面板           │
│  ide-sidebar  │  ide-canvas                     │  ide-panel         │
│  [折叠按钮]   │  [折叠按钮] [展开按钮]            │  [折叠按钮]         │
│              │                                │                    │
│  本体/剧集/  │  图谱画布 / 本体类视图            │  详情编辑面板       │
│  社区树       │                                │                    │
│  260px 展开  │  flex: 1                       │  360px 展开        │
│  48px 收缩  │  48px 收缩（工具栏缩为图标）     │  0px 折叠（全隐藏） │
│  (仅图标)   │                                │                    │
└──────────────┴────────────────────────────────┴────────────────────┘
```

### 3.2 折叠行为

每个面板在头部有独立的折叠/展开按钮：

- **左侧面板**：
  - 展开：显示完整树形结构（260px）
  - 折叠：只显示 Tab 切换按钮区域（48px），树内容隐藏
  - 折叠按钮位置：sidebar-header 右上角（⯆/⯈ 图标）

- **中间面板**：
  - 展开：完整宽度 + 工具栏完整显示
  - 折叠：收缩为 48px 宽，显示一个浮动展开按钮（垂直居中靠左）
  - 折叠按钮位置：在左侧分隔线上（可拖拽分隔线本身）

- **右侧面板**：
  - 展开：360px 宽度
  - 折叠：完全隐藏（0px），显示一个浮动展开按钮（垂直居中靠右）
  - 折叠按钮位置：在右侧分隔线上

### 3.3 实现方式

使用 CSS `display: none` 或 `width: 0` 配合 `overflow: hidden` 实现折叠，Vue 通过 `v-show` 或条件渲染控制显示。折叠状态存入组件 `ref`，不持久化到 URL。

---

## 4. 本体模式新交互

### 4.1 触发条件

当 `sidebarTab === 'ontology'` 且用户在左侧树中点击了某个类节点（`node.type === 'class'`）时，进入本体类视图。

### 4.2 中间面板：本体类视图

#### 工具栏（顶部，固定）

```
[当前类：CourtCase]  [2,847 个实例]  [图谱|实例列表]  [◀]  [1 / 29]  [▶]
```

- 类名 + 实例数：只读信息
- Tab 切换器：图谱（默认激活）/ 实例列表
- 分页控制：上一页 / 当前页 / 下一页（右侧固定显示）

#### 图谱 Tab（默认）

- 显示 `GraphCanvas` 组件
- 图谱数据来源：调用后端 API，按类实例 + 2跳邻居分页获取
- 分页：下一页数据**追加**到画布（nodes/edges 拼接，已存在节点去重）
- "下一页" 按钮在图谱下方居中，点击加载下一页数据

#### 实例列表 Tab

- 显示 `InstanceDataTable` 组件（复用已有组件）
- 数据与图谱 Tab 共享分页状态（同一个 page 变量）
- 点击某行实例：高亮对应图谱节点（双向联动）
- 分页控制与图谱 Tab 同步（切换 Tab 保留当前页）

### 4.3 右侧面板：类编辑器

当进入本体类视图时，右侧面板替换为 `ClassEditor` 组件（复用已有 `Ontology/ClassEditor.vue`），显示内容：

- 基本信息：类名、描述、URI、父类
- 属性列表（可折叠）
- 约束列表（可折叠）
- 实例统计（只读数字，无实例管理功能）

**不包含实例管理功能**（实例管理集中在中间面板）。

### 4.4 退出本体类视图

当用户在左侧树中点击根节点（本体根）或点击空白区域时，退出本体类视图：
- 中间面板恢复为标准图谱画布（`GraphCanvas`）
- 右侧面板恢复为标准详情面板（节点/关系详情）
- `OntologyWorkbench` 不再显示

### 4.5 切换类

在本体类视图下，点击左侧树中的另一个类：
- 清空中间画布（替换，非追加）
- 右侧面板替换为新类的编辑器
- 重新加载第一页数据

---

## 5. API 需求

### 5.1 新增 API（后端）

**按类获取实例 + 2跳邻居图谱数据（分页）**

```
GET /graph/{graphId}/visualization/entities/by-class
```

| 参数 | 类型 | 说明 |
|------|------|------|
| `className` | string | 类本地名（如 `CourtCase`） |
| `page` | int | 页码，从 1 开始 |
| `pageSize` | int | 每页实例数，默认 20 |
| `depth` | int | 邻居跳数，默认 2 |

**返回**：同现有 `GraphVisualizationData`：
- `nodes`：实例节点 + 2跳邻居节点
- `edges`：节点之间的边
- `pagination`：page, pageSize, total, totalPages

### 5.2 复用现有 API

如后端已有类似能力（参考 `GraphVisualizationServiceImpl` 中的 2跳邻居查询），可直接复用，无需新增。

---

## 6. 组件改造清单

| 组件 | 改造内容 |
|------|----------|
| `ide.vue` | 新增三个 `ref<boolean>` 控制折叠状态；新增 `OntologyClassView` 组件引用；改造 `ide-canvas` 模板分支逻辑 |
| `OntologyClassView.vue`（新增） | 本体类视图容器：顶部工具栏 + Tab 切换 + GraphCanvas + InstanceDataTable |
| `OntologyObjectExplorer.vue` | 树节点点击时触发 `class-selected` 事件（含 classId） |
| `ClassEditor.vue` | 已有组件，确认可嵌入右侧面板，无需大改 |
| `InstanceDataTable.vue` | 已有组件，确认支持外部传入 classId 和分页参数 |
| `GraphCanvas.vue` | 确认支持追加模式加载（append nodes/edges 而非全量替换） |

---

## 7. 状态管理

在 `ide.vue` 中新增以下 `ref`：

```typescript
// 折叠状态
const sidebarCollapsed = ref(false)
const canvasCollapsed = ref(false)
const panelCollapsed = ref(false)

// 本体类视图状态
const ontologyClassViewActive = ref(false)
const selectedClassForView = ref<SchemaClass | null>(null)

// 中间面板 Tab
const classViewTab = ref<'graph' | 'list'>('graph')

// 分页
const classViewPage = ref(1)
const classViewPageSize = ref(20)
const classViewTotal = ref(0)
```

---

## 8. 忽略范围

- 剧集模式、导航社区模式的改造（保持现有行为）
- `OntologyWorkbench` 标签页系统的改造
- 分页状态的 URL 持久化
- 移动端适配
