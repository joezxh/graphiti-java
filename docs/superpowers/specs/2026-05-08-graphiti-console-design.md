# Graphiti-Java 控制台前端设计文档

> 基于 Ontology 本体论的图谱关系管理系统 · 深色科技风 UI 设计
> 
> 创建日期：2026-05-08
> 状态：待审阅

---

## 一、项目概述

### 1.1 项目背景

Graphiti-Java 是一个基于 Java 的独立本体论知识图谱服务。现有后端已完成，具备完整的 REST API（独立部署的 graphiti-java 服务）。本次设计目标是为其构建一个**科技感十足的控制台前端**，充分体现基于 Ontology 本体论的图谱关系管理功能。

**后端服务信息**：
- 服务地址：<code>http://localhost:8080</code> (可配置)
- API 版本：<code>/api/v1</code>
- 认证方式：JWT Token (Bearer)
- 文档地址：<code>http://localhost:8080/swagger-ui.html</code>

### 1.2 设计目标

1. **深色科技风**：参考 Linear 设计语言，纯黑底色 + 单一亮色点缀
2. **双视图模式**：同时服务开发者用户和业务用户
3. **本体论可视化**：直观展示实体类型、关系类型及其约束
4. **混合图谱可视化**：力导向图 + 层级视图可切换
5. **混合检索界面**：全文 / 向量 / 图遍历三合一

### 1.3 目标用户

| 用户类型 | 需求 | 对应视图 |
|---------|------|---------|
| 开发者 / 工程师 | API 测试、Cypher 查询、数据导入导出 | 开发者视图 |
| 数据分析师 / 业务用户 | 图谱可视化、本体配置、搜索界面 | 业务视图 |

---

## 二、整体架构设计

### 2.1 双视图架构

```
┌─────────────────────────────────────────────┐
│  Header（Logo + 导航 + 视图切换 + 用户）   │
├──────────┬──────────────────────────────────┤
│          │                                  │
│  Sidebar │   主工作区（动态切换）              │
│  (固定)   │                                  │
│          │                                  │
└──────────┴──────────────────────────────────┘
```

**视图切换器**（Header 右侧）：
- 🔹 `业务视图` - 图谱可视化 + 搜索 + 本体预览
- 🔸 `开发者视图` - Cypher 编辑器 + API 测试 + 原始数据

### 2.2 业务视图布局

```
┌─────────────────────────────────────────────┐
│  Stats 卡片（图谱数 / 节点数 / 边数）        │
├─────────────────────────────────────────────┤
│  📊 图谱可视化  │  📋 属性面板              │
│  (力导向图)      │  (节点/边详情)               │
│                 │                           │
│  [切换层级视图]   │  [编辑] [删除] [导出]       │
├─────────────────────────────────────────────┤
│  🔍 混合检索栏（全文 / 向量 / 图遍历）        │
│  - 搜索结果列表                            │
└─────────────────────────────────────────────┘
```

### 2. 3 开发者视图布局

```
┌─────────────────────────────────────────────┐
│  ⚡ Cypher 查询编辑器 + 执行按钮             │
├─────────────────────────────────────────────┤
│  📊 查询结果（表格 / JSON / 图）             │
│                                             │
├─────────────────────────────────────────────┤
│  📜 API 测试面板（直接调用 REST API）         │
│  - 请求构建器                              │
│  - 响应预览                                │
└─────────────────────────────────────────────┘
```

---

## 三、页面详细设计

### 3.1 登录页

**布局**：
- 居中卡片式布局
- 背景：纯黑 (#010102) + 微妙网格线（科技感）
- 表单卡片：surface-1 色 (#0f1011) + 细边框 + 聚焦时 accent 色光晕

**元素**：
- Logo：几何图形 + 渐变色（primary #5e6ad2 → cyan #00d4ff）
- 用户名输入框
- 密码输入框（带显示/隐藏切换）
- "记住我" 复选框
- 登录按钮（accent 色填充）
- 错误提示（红色边框 + 消息）

**交互**：
- 输入框聚焦：border 变色 + 外发光 (box-shadow)
- 登录中：按钮 loading 状态
- 登录失败：Shake 动画 + 错误提示

---

### 3.2 仪表盘（业务视图首页）

**Stats 卡片区**（4 列网格）：
- 卡片样式：surface-1 背景 + hairline 边框 + hover 微浮起
- 显示内容：
  - 图谱总数（primary 色）
  - 实体节点数（cyan 色）
  - 关系边数（green 色）
  - 推理事件数（orange 色）
- 每个卡片附带趋势指标（↑ 12% 本周）

**快捷操作区**：
- 创建新图谱
- 导入数据
- 查看推理日志

---

### 3.3 图谱可视化页面

**核心组件**：`GraphViewer`（混合模式）

#### 3.3.1 力导向图（默认视图）
- **库**：ECharts 5.5 力导向图
- **节点样式**：
  - 实体节点：圆形 + primary 色边框
  - 概念节点：菱形 + cyan 色边框
  - 事件节点：方形 + orange 色边框
  - 大小：根据度中心性 (degree centrality) 动态调整
- **边样式**：
  - 实线：有效关系
  - 虚线：失效关系（expired_at 已设置）
  - 颜色：根据关系类型分类
- **交互**：
  - 拖拽节点
  - 滚轮缩放
  - 点击节点：高亮关联关系 + 右侧属性面板
  - 双击节点：展开 N 度关系（默认 2 度）

#### 3.3.2 层级视图（切换按钮）
- **布局**：Dagre 层级布局（自上而下）
- **适用场景**：查看明确的本体层级（如 Person → worksAt → Company）
- **交互**：可折叠/展开子节点

#### 3.3.3 工具栏
- 放大 / 缩小 / 重置视图
- 切换力导向 / 层级视图
- 按实体类型筛选（多选下拉）
- 按关系类型筛选
- 导出为图片 / JSON

#### 3.3.4 属性面板（右侧）
- 显示选中节点/边的详细信息：
  - 名称、类型、UUID
  - 属性列表（key-value 表格）
  - 关联关系列表（可点击跳转）
  - 来源事件列表
- 操作按钮：编辑 / 删除 / 导出

---

### 3.4 本体论编辑器

**布局**：左右分栏
- 左侧：表单编辑器（结构化）
- 右侧：本体预览可视化（只读）

#### 3.4.1 实体类型管理
- **卡片列表**：显示所有已定义实体类型
  - 每个卡片：图标 + 类型名称 + 已有实例数
  - 支持：新增 / 编辑 / 删除 / 复制
- **编辑弹窗**：
  - 类型名称（必填）
  - 图标选择（从预设图标库）
  - 属性 Schema 定义（键值对列表）
  - 约束规则（如：唯一性、必填）

#### 3.4.2 关系类型管理
- **表格列表**：显示所有已定义关系类型
  - 列：关系名称、源类型 → 目标类型、约束、操作
- **编辑弹窗**：
  - 关系名称（必填，大写，如 WORKS_FOR）
  - 源实体类型（下拉多选）
  - 目标实体类型（下拉多选）
  - 属性 Schema 定义
  - 时效性设置（是否支持 valid_at / invalid_at）

#### 3.4.3 本体预览可视化
- 只读力导向图
- 节点 = 实体类型（矩形）
- 边 = 关系类型（带标签）
- 颜色：实体类型用 primary 色，关系类型用 cyan 色
- 交互：悬停高亮，点击跳转编辑

---

### 3.5 混合检索页面

**搜索栏**：
- 输入框：支持自然语言查询
- 搜索选项：
  - ☑ 全文搜索
  - ☑ 向量搜索
  - ☑ 图遍历扩展
- 高级选项：
  - 最大返回结果数
  - 相似度阈值（向量搜索）
  - 遍历深度（图遍历）

**结果展示**（Tab 切换）：
1. **全部结果**：混合展示实体、关系、事件
2. **实体**：仅显示实体节点
3. **关系**：仅显示关系边
4. **事件**：仅显示事件

**结果卡片**：
- 标题：实体/关系名称
- 类型标签： colored badge
- 相似度/关联度得分
- 摘要信息
- 操作：查看详情 / 在图谱中定位 / 编辑

---

### 3.6 数据管理页面

#### 3.6.1 节点管理
- 表格视图：UUID、名称、类型、所属图谱、创建时间
- 筛选：按名称、类型、图谱
- 操作：新增 / 编辑 / 删除 / 批量导入

#### 3.6.2 边管理
- 表格视图：UUID、源节点、关系类型、目标节点、有效时间
- 筛选：按关系类型、失效状态
- 操作：编辑 / 软删除

#### 3.6.3 事件管理
- 表格视图：UUID、名称、内容摘要、来源、创建时间
- 操作：查看提及的实体 / 删除

#### 3.6.4 数据导入向导
- **步骤 1**：选择导入方式
  - 文本自动提取（LLM）
  - 批量数据导入
  - 消息/对话历史
  - 事实三元组（直接写入）
  - 实体节点（手动创建）
- **步骤 2**：配置导入参数
  - 目标图谱
  - 数据源（文件上传 / 文本输入 / JSON）
  - 是否更新社区
- **步骤 3**：预览 + 确认
- **步骤 4**：导入进度条 + 结果报告

---

### 3.7 推理引擎页面（开发者视图）

**推理规则列表**：
- 资金同源推理（Sybil Inference）
- 产业链利好传导推理（Alpha Propagation）
- 言行背离推理（Manipulation Detection）

**执行面板**：
- 选择目标图谱
- 配置推理参数
- 执行按钮 + 进度条
- 结果展示：
  - 新发现的关系（高亮显示）
  - 推理路径（溯源）
  - 置信度评分

---

### 3.8 社区发现页面

**功能**：
- 触发社区构建（K-means 聚类）
- 查看社区列表：
  - 社区 ID
  - 成员数量
  - 社区摘要（LLM 生成）
- 可视化：
  - 按社区着色的图谱视图
  - 社区间关系网络

---

## 四、视觉设计规范

### 4.1 色彩系统（参考 Linear）

| 角色 | 色值 | 用途 |
|------|------|------|
| Canvas | `#010102` | 页面背景（近纯黑，微蓝调） |
| Surface-1 | `#0f1011` | 卡片、面板背景 |
| Surface-2 | `#141516` | 悬浮卡片、选中状态 |
| Surface-3 | `#18191a` | 下拉菜单、次级面板 |
| Hairline | `#23252a` | 默认边框 |
| Hairline Strong | `#34343a` | 聚焦边框 |
| Ink | `#f7f8f8` | 主文本 |
| Ink Muted | `#d0d6e0` | 次要文本 |
| Ink Subtle | `#8a8f98` | 禁用、占位符 |
| Primary | `#5e6ad2` | Accent 色（薰衣草蓝）|
| Primary Hover | `#828fff` | Hover 状态 |
| Cyan | `#00d4ff` | 辅助 accent（科技感）|
| Green | `#27a644` | 成功、在线状态 |
| Orange | `#ff8c00` | 警告、待处理 |
| Red | `#ff6b6b` | 错误、危险操作 |

**使用原则**：
- Primary 色仅用于：Logo、主 CTA、聚焦环、链接
- 不使用第二 chromatic accent 做背景或填充
- 图表中的节点颜色从 Primary / Cyan / Green 三色衍生的色板中取色

### 4.2 字体系统

| Token | 字体大小 | 字重 | 字间距 | 用途 |
|-------|---------|------|--------|------|
| Display LG | 40px | 600 | -1.0px | 页面标题 |
| Headline | 28px | 600 | -0.6px | Section 标题 |
| Subhead | 20px | 400 | -0.2px | 卡片标题 |
| Body | 14px | 400 | 0 | 正文 |
| Caption | 12px | 400 | 0 | 辅助文本 |
| Button | 14px | 500 | 0 | 按钮文本 |
| Eyebrow | 11px | 500 | +0.4px | 分区标签（大写）|

**字体族**：
- 首选：Inter / SF Pro Display（macOS）
- 降级：system-ui, -apple-system, sans-serif
- Mono：`JetBrains Mono`, `SF Mono`（用于 UUID、Cypher 代码）

### 4.3 间距与圆角

**间距系统**（4px 基准）：
- xs: 4px, sm: 8px, md: 12px, lg: 16px, xl: 24px, xxl: 32px

**圆角**：
- 按钮 / 输入框：8px (`rounded-md`)
- 卡片：12px (`rounded-lg`)
- 图谱面板：16px (`rounded-xl`)
- 状态徽章：9999px (pill)

### 4.4 阴影与深度

- **Level 0**：无阴影（主文本、背景）
- **Level 1**：surface-1 背景 + 1px hairline 边框（默认卡片）
- **Level 2**：surface-2 背景 + 2px primary 聚焦环（选中状态）
- **不使用** drop shadow（深色主题中避免使用）

---

## 五、技术实现方案

### 5.1 技术栈详细选型

| 类别 | 选型 | 版本 | 理由 |
|------|------|------|------|
| 框架 | Vue 3 + TypeScript | 3.4 / 5.x | 官方推荐，Ant Design Vue 3 原生支持 |
| UI 组件 | Ant Design Vue | 4.x | 企业级组件库，深色主题完善 |
| 状态管理 | Pinia | 2.x | Vue 3 官方推荐，TypeScript 友好 |
| 图谱可视化 | ECharts 5 + vue-echarts | 5.5 | 内置力导向图，深色主题，性能好 |
| 备用可视化 | D3.js | 7.x | 更灵活的自定义能力（如需） |
| HTTP 客户端 | Axios | 1.x | 拦截器完善，易于封装 |
| 构建工具 | Vite | 5.x | 快速 HMR，按需加载 |
| CSS 方案 | Less + CSS Variables | - | Ant Design 原生支持主题定制 |
| 代码规范 | ESLint + Prettier | - | 统一代码风格 |

### 5.2 项目结构

```
graphiti-web/
├── public/
│   └── favicon.ico
├── src/
│   ├── api/                         # API 接口层
│   │   ├── graph.ts                # 图谱管理 API
│   │   ├── ontology.ts             # 本体论 API
│   │   ├── node.ts                 # 节点管理 API
│   │   ├── edge.ts                 # 边管理 API
│   │   ├── episode.ts              # 事件管理 API
│   │   ├── search.ts               # 搜索检索 API
│   │   ├── inference.ts            # 推理引擎 API
│   │   └── request.ts             # Axios 封装
│   ├── assets/                      # 静态资源
│   │   ├── styles/
│   │   │   ├── dark.less          # 深色主题变量
│   │   │   └── global.less        # 全局样式
│   │   └── images/
│   ├── components/                  # 公共组件
│   │   ├── GraphViewer/           # 图谱可视化组件
│   │   │   ├── ForceGraph.vue   # 力导向图
│   │   │   ├── TreeGraph.vue     # 层级图
│   │   │   ├── GraphToolbar.vue  # 工具栏
│   │   │   └── NodeDetail.vue    # 节点详情面板
│   │   ├── OntologyEditor/        # 本体编辑器
│   │   │   ├── EntityTypePanel.vue
│   │   │   ├── RelationTypePanel.vue
│   │   │   └── OntologyPreview.vue
│   │   ├── SearchBox/             # 混合搜索组件
│   │   │   ├── SearchInput.vue
│   │   │   └── SearchResults.vue
│   │   ├── StatsCard/             # 统计卡片
│   │   └── Layout/                # 布局组件
│   │       ├── BasicLayout.vue    # 主布局
│   │       ├── Sidebar.vue
│   │       └── Header.vue
│   ├── layouts/                     # 布局模式
│   │   ├── BusinessLayout.vue     # 业务视图
│   │   └── DeveloperLayout.vue   # 开发者视图
│   ├── router/                      # 路由配置
│   │   ├── index.ts
│   │   └── routes.ts
│   ├── store/                       # Pinia 状态管理
│   │   ├── modules/
│   │   │   ├── graph.ts           # 图谱状态
│   │   │   ├── ontology.ts        # 本体状态
│   │   │   ├── search.ts          # 搜索状态
│   │   │   └── user.ts           # 用户状态
│   │   └── index.ts
│   ├── views/                       # 页面组件
│   │   ├── login/
│   │   │   └── index.vue          # 登录页
│   │   ├── dashboard/
│   │   │   └── index.vue          # 仪表盘
│   │   ├── graph/
│   │   │   ├── list.vue           # 图谱列表
│   │   │   ├── detail.vue         # 图谱详情
│   │   │   └── workspace.vue      # 工作区（图谱操作）
│   │   ├── ontology/
│   │   │   └── index.vue          # 本体编辑器
│   │   ├── data/
│   │   │   ├── nodes.vue          # 节点管理
│   │   │   ├── edges.vue          # 边管理
│   │   │   ├── episodes.vue       # 事件管理
│   │   │   └── import.vue         # 数据导入
│   │   ├── search/
│   │   │   └── index.vue          # 混合检索页
│   │   ├── inference/
│   │   │   └── index.vue          # 推理引擎页
│   │   └── community/
│   │       └── index.vue           # 社区发现页
│   ├── utils/                       # 工具函数
│   │   ├── auth.ts                # 认证工具
│   │   ├── format.ts              # 格式化工具
│   │   └── graph.ts               # 图谱工具函数
│   ├── App.vue
│   └── main.ts
├── .env.development
├── .env.production
├── package.json
├── tsconfig.json
├── vite.config.ts
└── vitest.config.ts
```

### 5.3 主题定制（Ant Design Vue 深色主题）

```less
// assets/styles/dark.less
@primary-color: #5e6ad2;
@bg-color: #010102;
@surface-1: #0f1011;
@surface-2: #141516;
@border-color: #23252a;
@text-color: #f7f8f8;
@text-color-secondary: #8a8f98;

// 组件变量覆盖
@component-background: @surface-1;
@border-radius-base: 8px;
```

### 5.4 API 对接方案

基于独立的 **graphiti-java 服务** 的 REST API，前端需要对接以下 API 组：

| API 组 | 端点前缀 | 功能 | 对应前端模块 |
|--------|----------|------|---------------|
| 认证管理 | `/api/v1/auth` | 登录/登出/刷新Token | `api/auth.ts` |
| 用户管理 | `/api/v1/user` | CRUD + 角色分配 | `api/user.ts` |
| 角色管理 | `/api/v1/role` | CRUD + 权限分配 | `api/role.ts` |
| 图谱管理 | `/api/v1/graph` | CRUD + 克隆 + 清空 + 社区构建 | `api/graph.ts` |
| 本体管理 | `/api/v1/ontology` | 设置/获取 | `api/ontology.ts` |
| 数据写入 | `/api/v1/data` | 单条/批量/三元组/节点 | `views/data/import.vue` |
| 节点管理 | `/api/v1/graph/{graphId}/nodes` | 列表/详情/关联边/关联事件 | `api/node.ts` |
| 边管理 | `/api/v1/graph/{graphId}/edges` | 列表/详情/删除 | `api/edge.ts` |
| 事件管理 | `/api/v1/graph/{graphId}/episodes` | 列表/详情/提及 | `api/episode.ts` |
| 搜索检索 | `/api/v1/search` | 全文/向量/图遍历 | `api/search.ts` |
| 记忆获取 | `/api/v1/get-memory` | 获取记忆上下文 | `api/search.ts` |

**Axios 封装** (`api/request.ts`)：
- 请求拦截器：自动附加 JWT token
- 响应拦截器：统一错误处理、token 过期刷新
- 超时设置：10 秒（普通请求），60 秒（数据导入）

---

## 六、组件交互设计

### 6.1 图谱可视化组件 (`GraphViewer`)

**Props**：
- `graphId: string` - 目标图谱 ID
- `layout: 'force' | 'tree'` - 布局模式
- `showLabels: boolean` - 是否显示节点标签
- `filter: { entityTypes?: string[], relationTypes?: string[] }` - 筛选条件

**Events**：
- `@node-click(uuid: string)` - 节点点击
- `@node-double-click(uuid: string)` - 节点双击（展开关联）
- `@edge-click(uuid: string)` - 边点击
- `@background-click()` - 空白处点击（取消选中）

**核心方法**：
- `zoomIn()` / `zoomOut()` / `resetZoom()`
- `highlightNode(uuid: string)` - 高亮节点及其关联
- `expandNode(uuid: string, depth: number)` - 展开 N 度关系
- `exportImage(format: 'png' | 'svg')` - 导出图片

### 6.2 本体编辑器组件 (`OntologyEditor`)

**Props**：
- `graphId: string` - 目标图谱 ID
- `mode: 'edit' | 'preview'` - 模式（编辑 / 预览）

**Events**：
- `@entity-type-add(type: EntityType)` - 新增实体类型
- `@entity-type-update(type: EntityType)` - 更新实体类型
- `@relation-type-add(rel: RelationType)` - 新增关系类型
- `@ontology-change(ontology: Ontology)` - 本体定义变更

### 6.3 混合搜索组件 (`SearchBox`)

**Props**：
- `placeholder: string` - 占位符文本
- `enableVector: boolean` - 是否启用向量搜索
- `enableFulltext: boolean` - 是否启用全文搜索
- `enableGraphTraversal: boolean` - 是否启用图遍历

**Events**：
- `@search(query: SearchQuery)` - 搜索触发
- `@result-click(result: SearchResult)` - 结果项点击
- `@result-locate(result: SearchResult)` - 在图谱中定位

---

## 七、数据流设计

### 7.1 图谱可视化数据流

```
用户操作（拖拽/缩放/点击）
    ↓
GraphViewer 组件
    ↓
ECharts 实例（force 布局计算）
    ↓
更新节点位置 / 高亮状态
    ↓
触发 @node-click 事件
    ↓
Store (Pinia) 更新选中状态
    ↓
属性面板重新渲染
```

### 7.2 搜索数据流

```
用户输入查询文本
    ↓
点击搜索按钮 / 回车
    ↓
api/search.ts → POST /api/v1/search
    ↓
后端执行：
  1. Embedding 生成查询向量
  2. 边检索（向量 + 全文）
  3. 节点检索（向量 + 全文）
  4. RRF 融合多路结果
  5. MMR 重排序
  6. 一跳扩展（BFS）
    ↓
返回 SearchResultsRespVO
    ↓
Store 更新搜索结果
    ↓
SearchResults 组件重新渲染
```

### 7.3 本体编辑数据流

```
用户编辑实体类型 / 关系类型
    ↓
OntologyEditor 组件本地验证
    ↓
提交到 Store
    ↓
api/ontology.ts → PUT /api/v1/ontology
    ↓
后端保存本体定义到 MySQL
    ↓
前端更新 OntologyPreview 可视化
    ↓
显示成功提示
```

---

## 八、性能优化策略

### 8.1 图谱可视化性能

- **大数据集**：节点 > 1000 时，启用采样/聚类展示
- **按需加载**：先加载中心节点，再按需展开关联
- **Web Worker**：力导向布局计算放入 Worker，避免 UI 卡顿
- **虚拟化**：表格/列表使用虚拟滚动（Ant Design Vue 支持）

### 8.2 搜索性能

- **防抖**：输入搜索文本时，500ms 防抖
- **缓存**：搜索结果缓存到 Pinia Store，避免重复请求
- **分页**：结果列表分页加载（Infinite Scroll）

### 8.3 代码分割

- **路由懒加载**：每个页面组件使用 `() => import('./views/...')`
- **组件按需加载**：Ant Design Vue 组件使用 `unplugin-vue-components` 自动按需引入
- **第三方库按需引入**：ECharts 使用按需引入（仅引入力导向图模块）

---

## 九、实施优先级

### Phase 1：基础框架 + 登录（1-2 天）
- [ ] 项目初始化（Vite + Vue 3 + TypeScript）
- [ ] Ant Design Vue 集成 + 深色主题配置
- [ ] 登录页面 + JWT 认证
- [ ] 基础布局组件（Header + Sidebar）

### Phase 2：业务视图核心（3-5 天）
- [ ] 仪表盘页面（Stats 卡片）
- [ ] 图谱列表 + 详情页
- [ ] 图谱可视化组件（力导向图）
- [ ] 节点/边属性面板

### Phase 3：数据管理 + 搜索（3-4 天）
- [ ] 节点管理页面（CRUD + 表格）
- [ ] 边管理页面
- [ ] 事件管理页面
- [ ] 混合检索页面
- [ ] 数据导入向导

### Phase 4：本体论编辑器（2-3 天）
- [ ] 实体类型管理
- [ ] 关系类型管理
- [ ] 本体预览可视化

### Phase 5：开发者视图 + 推理（2-3 天）
- [ ] Cypher 查询编辑器（代码编辑器集成）
- [ ] API 测试面板
- [ ] 推理引擎页面
- [ ] 社区发现页面

### Phase 6：优化 + 测试（2-3 天）
- [ ] 性能优化（大数据集测试）
- [ ] 响应式适配（平板 + 小屏）
- [ ] E2E 测试（Cypress / Playwright）
- [ ] 部署配置（Docker + Nginx）

---

## 十、风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| ECharts 力导向图性能不足（> 5000 节点） | 可视化卡顿 | 1. 使用采样/聚类<br>2. 备用 D3.js 方案<br>3. 后端预计算布局 |
| Ant Design Vue 深色主题不完全匹配设计 | UI 差异 | 1. 自定义 Less 变量<br>2. 必要时写覆盖样式 |
| 本体编辑器表单复杂度高 | 用户学习成本高 | 1. 分步骤向导<br>2. 提供默认值<br>3. 实时预览 |
| 双视图模式增加开发成本 | 工期延长 | 1. 复用核心组件<br>2. 优先实现业务视图 |

---

## 十一、附录

### A. 设计参考

- **Linear App**：深色科技风设计语言
- **Neo4j Browser**：图谱可视化交互参考
- **Grafana**：数据仪表盘布局参考
- **Ant Design Vue 官方文档**：组件使用规范

### B. 相关文档

- `docs/01-项目概述.md` - Graphiti-Java 项目背景
- `docs/02-系统架构设计.md` - 后端架构设计
- `docs/04-数据库设计-Neo4j.md` - Neo4j 图数据模型
- `docs/05-API接口规范与开发计划.md` - API 接口定义

---

**文档状态**：已审阅并确认 ✓
**后端服务**：独立 graphiti-java 服务 (<code>http://localhost:8080/api/v1</code>)
**下一步**：启动 `writing-plans` 技能制定实施计划
