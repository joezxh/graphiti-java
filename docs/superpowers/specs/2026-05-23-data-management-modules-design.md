# 数据管理独立模块设计文档

## 概述

基于现有 ide.vue 和 Ontology 组件体系，设计 7 个独立的数据管理模块（类定义、属性、约束、实体、关系边、社区、剧集），每个模块拥有独立的路由入口和统一的页面布局，支持完整的 CRUD 操作和模块间关联导航。

## 目标

1. 每个模块拥有独立的 URL 入口，可直接访问
2. 最大限度复用现有前端组件与后端接口
3. 保持操作界面极简，流程简洁
4. 界面风格与 ide.vue 和 Ontology 组件保持一致
5. 模块间支持关联导航（跳转链接、侧滑面板、行内嵌套）

## 架构设计

### 方法选择

**方法 1：独立页面 + 组件复用**

每个模块独立路由页面，统一页面壳（DataManagerLayout），中间复用现有组件。

### 菜单与路由架构

**菜单结构（Sidebar.vue `data-management` 子菜单）：**

```vue
<a-sub-menu key="data-management">
  <template #title>数据管理</template>
  <!-- Phase 1: 本体定义管理 -->
  <a-menu-item key="/data/classes">类定义管理</a-menu-item>
  <a-menu-item key="/data/properties">属性管理</a-menu-item>
  <a-menu-item key="/data/constraints">约束管理</a-menu-item>
  <!-- Phase 2: 核心数据管理 -->
  <a-menu-item key="/data/entities">实体管理</a-menu-item>
  <a-menu-item key="/data/edges">关系边管理</a-menu-item>
  <!-- Phase 3: 业务数据管理 -->
  <a-menu-item key="/data/communities">社区管理</a-menu-item>
  <a-menu-item key="/data/episodes">剧集管理</a-menu-item>
  <!-- 数据导入导出 -->
  <a-menu-item key="/data/import">数据导入</a-menu-item>
  <a-menu-item key="/data/export">数据导出</a-menu-item>
</a-sub-menu>
```

**路由调整：**

| 菜单项 | 新路由 | 原路由 | 操作 |
|--------|--------|--------|------|
| 类定义管理 | `/data/classes` | 无 | 新增 |
| 属性管理 | `/data/properties` | 无 | 新增 |
| 约束管理 | `/data/constraints` | 无 | 新增 |
| 实体管理 | `/data/entities` | `/data/entities` | 覆盖 |
| 关系边管理 | `/data/edges` | `/edges` | 路由迁移 |
| 社区管理 | `/data/communities` | `/data/community-episode` | 覆盖 |
| 剧集管理 | `/data/episodes` | `/episodes` | 路由迁移 |

### 统一页面布局

**DataManagerLayout.vue**（新建）：

```
┌─────────────────────────────────────────────────────────────┐
│  Header: 模块标题 + 面包屑 + 图谱选择器 + 搜索/刷新/新建      │
├──────────────┬──────────────────────────────┬───────────────┤
│              │                              │               │
│  左侧筛选栏   │      中间数据表格             │  右侧详情/编辑 │
│  (可选)      │      (ListPanel/Table)       │  (Editor)     │
│              │                              │               │
└──────────────┴──────────────────────────────┴───────────────┘
```

## 模块详细设计

### Phase 1：本体定义管理（3 个模块）

#### 1. 类定义管理（/data/classes）

**复用组件：**
- `ClassListPanel.vue` - 类列表
- `ClassEditor.vue` - 类编辑器

**页面结构：**
```vue
<DataManagerLayout title="类定义管理">
  <template #main-table>
    <ClassListPanel :graph-id="graphId" @edit="handleEdit" />
  </template>
  <template #right-panel>
    <ClassEditor
      v-if="selectedClass"
      :graph-id="graphId"
      :class-id="selectedClass.id"
      @saved="handleSaved"
    />
  </template>
</DataManagerLayout>
```

#### 2. 属性管理（/data/properties）

**复用组件：**
- `PropertyListPanel.vue` - 属性列表
- `PropertyEditor.vue` - 属性编辑器

#### 3. 约束管理（/data/constraints）

**复用组件：**
- `ConstraintListPanel.vue` - 约束列表
- `ConstraintEditor.vue` - 约束编辑器

### Phase 2：核心数据管理（2 个模块）

#### 4. 实体管理（/data/entities）

**复用组件：**
- `InstanceDataTable.vue` - 实例数据表格
- `InstanceForm.vue` - 实例表单

**页面结构：**
```vue
<DataManagerLayout title="实体管理">
  <template #left-panel>
    <ClassFilter :graph-id="graphId" @select="handleClassSelect" />
  </template>
  <template #main-table>
    <InstanceDataTable
      :graph-id="graphId"
      :class-type="selectedClassType"
      @edit-instance="handleEditInstance"
    />
  </template>
  <template #right-panel>
    <InstanceForm
      v-if="selectedInstance"
      :graph-id="graphId"
      :instance-data="selectedInstance"
      @saved="handleSaved"
    />
  </template>
</DataManagerLayout>
```

#### 5. 关系边管理（/data/edges）

**新建组件：**
- `EdgeListPanel.vue` - 边列表
- `EdgeEditor.vue` - 边编辑器

**表格列定义：**
```typescript
const columns = [
  { title: 'UUID', dataIndex: 'uuid', key: 'uuid', width: 200 },
  { title: '源节点', dataIndex: 'sourceName', key: 'source', width: 150 },
  { title: '目标节点', dataIndex: 'targetName', key: 'target', width: 150 },
  { title: '关系类型', dataIndex: 'type', key: 'type', width: 120 },
  { title: '事实描述', dataIndex: 'fact', key: 'fact', ellipsis: true },
  { title: '操作', key: 'action', width: 120 }
]
```

**API 复用：**
- `graphApi.createEdge` - 创建边
- `graphApi.getNodeDetail` - 获取节点详情（用于显示节点名称）

### Phase 3：业务数据管理（2 个模块）

#### 6. 社区管理（/data/communities）

**新建组件：**
- `CommunityListPanel.vue` - 社区列表
- `CommunityEditor.vue` - 社区编辑器

**表格列定义：**
```typescript
const columns = [
  { title: 'UUID', dataIndex: 'uuid', key: 'uuid', width: 200 },
  { title: '名称', dataIndex: 'name', key: 'name', width: 150 },
  { title: '类型', dataIndex: 'communityType', key: 'type', width: 120 },
  { title: '法律领域', dataIndex: 'legalDomain', key: 'domain', width: 120 },
  { title: '成员数', dataIndex: 'memberCount', key: 'memberCount', width: 100 },
  { title: '操作', key: 'action', width: 120 }
]
```

**关联导航：**
- 行内嵌套：展开显示成员实体列表
- 跳转链接：点击"成员数"跳转到实体管理并筛选

#### 7. 剧集管理（/data/episodes）

**新建组件：**
- `EpisodeListPanel.vue` - 剧集列表
- `EpisodeEditor.vue` - 剧集编辑器

**表格列定义：**
```typescript
const columns = [
  { title: 'UUID', dataIndex: 'uuid', key: 'uuid', width: 200 },
  { title: '名称', dataIndex: 'name', key: 'name', width: 150 },
  { title: '类型', dataIndex: 'episodeType', key: 'type', width: 120 },
  { title: '来源', dataIndex: 'source', key: 'source', width: 120 },
  { title: '创建时间', dataIndex: 'createdAt', key: 'createdAt', width: 150 },
  { title: '操作', key: 'action', width: 120 }
]
```

**关联导航：**
- 侧滑详情：点击行右侧滑出详情面板显示提及的实体列表
- 跳转链接：提及的实体可点击跳转到实体管理

## 模块间关联导航设计

### 1. 表格行内嵌套（一对多关系）

```vue
<a-table :expandable="{ expandedRowRender: renderMembers }">
  <template #expandedRowRender="{ record }">
    <a-table :data-source="record.members" size="small" />
  </template>
</a-table>
```

**适用场景：** 社区 → 成员实体列表

### 2. 跳转链接（跨模块导航）

```vue
<a-button type="link" @click="navigateToEntities(record.memberUuids)">
  {{ record.memberCount }} 个成员
</a-button>

function navigateToEntities(uuids: string[]) {
  router.push({
    path: '/data/entities',
    query: { filterUuids: uuids.join(',') }
  })
}
```

**适用场景：** 社区 → 实体，剧集 → 实体

### 3. 侧滑详情面板

```vue
<a-drawer v-model:open="detailVisible" width="400">
  <EpisodeDetail :episode="selectedEpisode" />
</a-drawer>
```

**适用场景：** 剧集 → 提及实体详情

## 文件结构

```
graphiti-web/src/
├── views/data/                    # 独立管理页面
│   ├── classes.vue                # 类定义管理（Phase 1）
│   ├── properties.vue             # 属性管理（Phase 1）
│   ├── constraints.vue            # 约束管理（Phase 1）
│   ├── entities.vue               # 实体管理（Phase 2）
│   ├── edges.vue                  # 关系边管理（Phase 2）
│   ├── communities.vue            # 社区管理（Phase 3）
│   ├── episodes.vue               # 剧集管理（Phase 3）
│   ├── import.vue                 # 数据导入（已有）
│   └── export.vue                 # 数据导出（已有）
├── components/Layout/
│   └── DataManagerLayout.vue      # 统一页面壳（新建）
├── components/Ontology/           # Ontology 组件（已有/新建）
│   ├── ClassListPanel.vue         # 复用
│   ├── ClassEditor.vue            # 复用
│   ├── PropertyListPanel.vue      # 复用
│   ├── PropertyEditor.vue         # 复用
│   ├── ConstraintListPanel.vue    # 复用
│   ├── InstanceDataTable.vue      # 复用
│   ├── InstanceForm.vue           # 复用
│   ├── EdgeListPanel.vue          # 新建（Phase 2）
│   ├── EdgeEditor.vue             # 新建（Phase 2）
│   ├── CommunityListPanel.vue     # 新建（Phase 3）
│   ├── CommunityEditor.vue        # 新建（Phase 3）
│   ├── EpisodeListPanel.vue       # 新建（Phase 3）
│   └── EpisodeEditor.vue          # 新建（Phase 3）
├── components/Layout/Sidebar.vue  # 菜单调整
└── router/index.ts                # 路由配置
```

## 实施计划

### Phase 1：本体定义管理（预计 2 天）

| 任务 | 文件 | 说明 |
|------|------|------|
| 新建页面壳 | `DataManagerLayout.vue` | 统一布局组件 |
| 新建页面 | `views/data/classes.vue` | 复用 ClassListPanel + ClassEditor |
| 新建页面 | `views/data/properties.vue` | 复用 PropertyListPanel + PropertyEditor |
| 新建页面 | `views/data/constraints.vue` | 复用 ConstraintListPanel + ConstraintEditor |
| 调整菜单 | `Sidebar.vue` | 新增 3 个菜单项 |
| 配置路由 | `router/index.ts` | 新增 3 条路由 |

### Phase 2：核心数据管理（预计 3 天）

| 任务 | 文件 | 说明 |
|------|------|------|
| 覆盖页面 | `views/data/entities.vue` | 复用 InstanceDataTable + InstanceForm |
| 新建组件 | `EdgeListPanel.vue` | 边列表表格 |
| 新建组件 | `EdgeEditor.vue` | 边编辑表单 |
| 新建页面 | `views/data/edges.vue` | 组装 EdgeListPanel + EdgeEditor |
| 调整菜单 | `Sidebar.vue` | 覆盖/迁移 2 个菜单项 |
| 配置路由 | `router/index.ts` | 覆盖/迁移 2 条路由 |

### Phase 3：业务数据管理（预计 4 天）

| 任务 | 文件 | 说明 |
|------|------|------|
| 新建组件 | `CommunityListPanel.vue` | 社区列表表格 |
| 新建组件 | `CommunityEditor.vue` | 社区编辑表单 |
| 新建页面 | `views/data/communities.vue` | 组装 CommunityListPanel + CommunityEditor |
| 新建组件 | `EpisodeListPanel.vue` | 剧集列表表格 |
| 新建组件 | `EpisodeEditor.vue` | 剧集编辑表单 |
| 新建页面 | `views/data/episodes.vue` | 组装 EpisodeListPanel + EpisodeEditor |
| 调整菜单 | `Sidebar.vue` | 覆盖/迁移 2 个菜单项 |
| 配置路由 | `router/index.ts` | 覆盖/迁移 2 条路由 |
| 关联导航 | 多个文件 | 模块间跳转链接、侧滑面板 |

## 兼容性保证

| 功能 | 处理方式 |
|------|---------|
| 现有 ide.vue | 保持不变，独立模块为新增入口 |
| 现有 OntologyWorkbench | 保持不变，独立模块复用其子组件 |
| 现有路由 /edges | 重定向到 /data/edges |
| 现有路由 /episodes | 重定向到 /data/episodes |
| 现有路由 /data/community-episode | 重定向到 /data/communities |
| 数据导入导出 | 保持不变，菜单位置不变 |

## 技术要点

1. **GraphID 传递**：所有模块通过 URL query 或 props 传递 graphId，确保数据隔离
2. **状态管理**：复用现有的 `useOntologyStore`，不新建 store
3. **样式一致性**：使用 ide.vue 的颜色变量（#0d1117, #161b22, #30363d 等）
4. **响应式布局**：表格区域支持横向滚动，右侧面板可折叠
5. **权限控制**：复用现有的路由守卫，按菜单权限控制访问
