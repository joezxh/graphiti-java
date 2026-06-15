# Graphiti IDE - 图谱可视化与 Schema 编辑系统

**版本**: v1.0
**创建时间**: 2026-05-16
**状态**: 设计中

---

## 目录

1. [设计愿景](#1-设计愿景)
2. [数据模型](#2-数据模型)
3. [整体架构](#3-整体架构)
4. [功能模块](#4-功能模块)
   - 4.1 实时图谱可视化
   - 4.2 可视化 Schema 编辑
   - 4.3 属性级联编辑
5. [API 接口设计](#5-api-接口设计)
6. [数据库设计](#6-数据库设计)
7. [交互设计](#7-交互设计)
8. [技术实现要点](#8-技术实现要点)

---

## 1. 设计愿景

构建一个专业级知识图谱 IDE，提供：

- **实时图谱可视化**: 支持大规模图数据的高性能渲染，支持节点/边的 CRUD 操作
- **可视化 Schema 编辑**: 通过图形化界面编辑本体定义，支持多继承、属性约束
- **属性级联编辑**: 批量修改节点属性，支持条件筛选和影响范围预览

---

## 2. 数据模型

### 2.1 核心实体关系

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Graphiti 数据模型                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                             │
│  ┌──────────────────┐         1:N        ┌──────────────────┐              │
│  │  GraphMetadata   │◄───────────────────►│  OntDefinition   │              │
│  │  (图谱元数据)    │                    │  (本体定义)      │              │
│  ├──────────────────┤                    ├──────────────────┤              │
│  │ graph_id         │                    │ id               │              │
│  │ name             │                    │ graph_id (FK)    │              │
│  │ description      │                    │ namespace        │              │
│  │ status           │                    │ name             │              │
│  └──────────────────┘                    │ version          │              │
│         │                               │ status           │              │
│         │                               └────────┬─────────┘              │
│         │                                        │                        │
│         │ 1:N                                    │ 1:N                    │
│         ▼                                        ▼                        │
│  ┌──────────────────┐                    ┌──────────────────┐              │
│  │  Entity (Neo4j)  │◄───────────────────┤   OntClass       │              │
│  │  (实体节点)       │   type 语义匹配     │   (类定义)        │              │
│  ├──────────────────┤                    ├──────────────────┤              │
│  │ uuid             │                    │ id               │              │
│  │ name             │                    │ definition_id    │              │
│  │ type (Class)    │                    │ class_uri        │              │
│  │ summary         │                    │ local_name       │              │
│  │ embedding       │                    │ parent_class_id  │ (多继承)     │
│  │ properties {}   │                    │ description      │              │
│  └────────┬─────────┘                    └────────┬─────────┘              │
│           │                                        │                        │
│           │ 1:N                                     │ 1:N                    │
│           ▼                                        ▼                        │
│  ┌──────────────────┐                    ┌──────────────────┐              │
│  │ Relationship     │                    │   OntProperty   │              │
│  │ (Neo4j)          │                    │   (属性定义)     │              │
│  ├──────────────────┤                    ├──────────────────┤              │
│  │ uuid             │                    │ id               │              │
│  │ type             │                    │ definition_id    │              │
│  │ source_uuid      │                    │ property_uri     │              │
│  │ target_uuid      │                    │ local_name       │              │
│  │ fact             │                    │ property_type   │              │
│  │ properties {}   │                    │ domain_class_id │              │
│  └──────────────────┘                    │ range_class_id  │              │
│                                          │ range_data_type │              │
│                                          │ is_required     │              │
│                                          │ allowed_values  │ (枚举)        │
│                                          │ pattern         │ (正则)        │
│                                          │ min_value       │ (数值范围)    │
│                                          │ max_value       │ (数值范围)    │
│                                          │ default_value   │              │
│                                          └──────────────────┘              │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 2.2 模型要点说明

| 关系 | 说明 |
|------|------|
| Graph ↔ Ontology | 1:N，一个 Graph 可有多个 Ontology 版本 |
| Node ↔ Class | 弱关联，通过 Node.type 字符串与 Class.localName 语义匹配 |
| Edge Type | 使用 Neo4j Relationship Type + 业务 type 属性 |
| Property | 双层：Schema 定义 (MySQL) + 实例属性 (Neo4j Node/Edge) |
| Class 继承 | **支持多继承**，通过 parent_class_id 实现 |

---

## 3. 整体架构

### 3.1 技术栈

| 层级 | 技术选型 |
|------|----------|
| 前端框架 | Vue 3 + TypeScript |
| 状态管理 | Pinia |
| 图谱渲染 | G6 / D3.js (力导向图) |
| UI 组件 | Element Plus |
| 样式 | SCSS + CSS Variables |
| 后端 | Java Spring Boot |
| 图数据库 | Neo4j |
| 关系数据库 | MySQL / PostgreSQL |

### 3.2 模块结构

```
ontograph-web/
├── src/
│   ├── views/
│   │   └── graph/
│   │       ├── index.vue           # 主入口
│   │       ├── GraphCanvas.vue     # 图谱可视化画布
│   │       ├── SchemaEditor.vue    # Schema 编辑器
│   │       ├── PropertyPanel.vue   # 属性面板
│   │       └── CascadeEdit.vue      # 级联编辑弹窗
│   ├── components/
│   │   ├── Graph/
│   │   │   ├── NodeRenderer.vue    # 节点渲染器
│   │   │   ├── EdgeRenderer.vue    # 边渲染器
│   │   │   ├── Toolbar.vue         # 工具栏
│   │   │   └── MiniMap.vue         # 小地图
│   │   └── Schema/
│   │       ├── ClassTree.vue       # 类树形结构
│   │       ├── ClassCard.vue       # 类卡片
│   │       ├── PropertyForm.vue    # 属性表单
│   │       └── InheritanceGraph.vue # 继承关系图
│   ├── api/
│   │   └── graph.ts                # 图谱 API
│   ├── stores/
│   │   └── graph.ts                # 图谱状态管理
│   └── types/
│       └── graph.ts                # 类型定义
```

---

## 4. 功能模块

### 4.1 实时图谱可视化

#### 4.1.1 功能概述

提供高性能的图谱可视化渲染，支持节点和边的增删改查操作。

#### 4.1.2 布局算法

| 布局类型 | 说明 | 适用场景 |
|----------|------|----------|
| Force (力导向) | 节点间斥力 + 连线引力 | 展示关系结构 |
| Grid (网格) | 规则网格排列 | 结构化展示 |
| Dagre (层次) | 有向无环图布局 | 树状/层级关系 |
| Concentric (同心圆) | 按重要性分层 | 突出中心节点 |

#### 4.1.3 节点渲染

**节点样式规则**：

```typescript
interface NodeStyle {
  // 根据 Class 类型着色
  colors: {
    Person: '#3B82F6',      // 蓝色
    Company: '#10B981',      // 绿色
    Product: '#F59E0B',      // 黄色
    Location: '#EF4444',     // 红色
    Event: '#8B5CF6',        // 紫色
    default: '#6B7280'       // 灰色
  }
}
```

**节点显示信息**：

- 主标签：节点名称
- 副标签：节点类型 (Class)
- 徽章：属性数量

#### 4.1.4 边渲染

**边样式规则**：

- 线条粗细：根据权重/重要性
- 箭头：表示关系方向
- 标签：边类型名称
- 颜色：根据边类型分组

#### 4.1.5 大规模图处理策略

**策略 C：虚拟滚动 + 聚合**

| 技术 | 实现 |
|------|------|
| **聚合模式** | 按 Class 类型聚合，显示类型节点和数量 |
| **虚拟滚动** | 只渲染可视区域内的节点 |
| **LOD (Level of Detail)** | 缩放级别决定渲染细节 |
| **分页加载** | 按页加载，每次 100-500 节点 |

#### 4.1.6 节点操作

| 操作 | 快捷键 | 说明 |
|------|--------|------|
| 查看详情 | 单击 | 打开详情面板 |
| 编辑属性 | 双击 / Enter | 打开编辑弹窗 |
| 删除节点 | Delete | 确认后删除 |
| 添加关联 | 拖拽 | 从节点拖出创建边 |
| 展开邻居 | 右键 → 展开 | 显示直接关联节点 |

#### 4.1.7 用户流程

```
1. 用户进入图谱视图
         ↓
2. 选择布局类型 (Force/Grid/Dagre/Concentric)
         ↓
3. 图谱加载 → 显示节点和边
         ↓
4. 如节点过多 → 自动进入聚合模式
         ↓
5. 用户可以：
   - 单击节点 → 查看详情
   - 双击节点 → 编辑属性
   - 拖拽节点 → 调整位置
   - 右键节点 → 更多操作
         ↓
6. 节点变更 → 实时同步到 Neo4j
```

---

### 4.2 可视化 Schema 编辑

#### 4.2.1 功能概述

通过图形化界面编辑本体定义，支持类继承、属性约束配置。

#### 4.2.2 编辑模式

**模式 C：图形 + 表单结合**

| 视图 | 用途 |
|------|------|
| 左侧：类树形结构 | 快速导航、创建、删除类 |
| 中间：继承关系图 | 可视化类继承关系 |
| 右侧：属性配置表单 | 编辑类的属性定义 |

#### 4.2.3 类定义

```typescript
interface ClassDefinition {
  id?: number;
  definitionId: number;           // 所属 Ontology
  classUri: string;                 // 完整 URI
  localName: string;                // 本地名称
  parentClassIds: number[];        // 父类 ID 列表 (多继承)
  description?: string;             // 类描述
}
```

#### 4.2.4 属性约束

| 约束类型 | 字段 | 说明 |
|----------|------|------|
| 数据类型 | `rangeDataType` | string/integer/float/boolean/date/json |
| 必填 | `isRequired` | true/false |
| 枚举值 | `allowedValues` | JSON 数组 |
| 数值范围 | `minValue`, `maxValue` | 仅数值类型 |
| 正则 | `pattern` | 字符串格式校验 |
| 默认值 | `defaultValue` | 未提供时的默认值 |
| 多值 | `isMultiple` | 是否允许多个值 |
| 基数 | `minCardinality`, `maxCardinality` | 值数量限制 |

#### 4.2.5 继承关系图

**多继承可视化**：

```
          ┌─────────┐
          │ Thing   │
          └────┬────┘
               │
       ┌───────┴───────┐
       │               │
  ┌────┴────┐     ┌────┴────┐
  │ Agent  │     │ Entity  │
  └────┬────┘     └────┬────┘
       │               │
       └───────┬───────┘
               │
          ┌────┴────┐
          │Person   │  ← 多继承 (Agent + Entity)
          └─────────┘
```

#### 4.2.6 用户流程

```
1. 用户打开 Schema 编辑器
         ↓
2. 左侧显示类树 → 选择一个类
         ↓
3. 中间继承图高亮该类及其父类
         ↓
4. 右侧显示属性配置表单
         ↓
5. 用户可以：
   - 添加/删除属性
   - 配置属性约束 (数据类型、必填、枚举等)
   - 编辑继承关系 (选择父类)
         ↓
6. 保存 → 本体验证 → 同步到 MySQL
         ↓
7. 如有现有数据 → 提示 Schema 变更影响
```

#### 4.2.7 Schema 变更影响

**变更类型与影响**：

| 变更类型 | 影响 | 处理方式 |
|----------|------|----------|
| 新增属性 | 无 | 自动生效 |
| 删除属性 | 丢失数据 | 警告 + 确认 |
| 修改约束 | 现有数据可能不满足 | 统计不满足数量 |
| 添加必填 | 现有数据缺失 | 统计缺失数量 |

---

### 4.3 属性级联编辑

#### 4.3.1 功能概述

批量修改节点属性，支持条件筛选和影响范围预览。

#### 4.3.2 筛选条件

```typescript
interface CascadeFilter {
  // 基础条件
  classType?: string;              // 节点类型
  propertyConditions?: PropertyCondition[];

  // 条件组合
  logic: 'AND' | 'OR';
}

interface PropertyCondition {
  propertyName: string;
  operator: 'eq' | 'ne' | 'gt' | 'lt' | 'gte' | 'lte' | 'contains' | 'not_contains' | 'in' | 'not_in' | 'is_null' | 'is_not_null';
  value: any;
}
```

#### 4.3.3 筛选操作符

| 操作符 | 说明 | 示例 |
|--------|------|------|
| eq | 等于 | status eq 'active' |
| ne | 不等于 | status ne 'deleted' |
| gt | 大于 | age gt 18 |
| lt | 小于 | price lt 100 |
| gte | 大于等于 | score gte 60 |
| lte | 小于等于 | score lte 100 |
| contains | 包含 | name contains 'John' |
| not_contains | 不包含 | desc not_contains 'old' |
| in | 在列表中 | type in ['Person', 'Company'] |
| not_in | 不在列表中 | status not_in ['deleted'] |
| is_null | 为空 | phone is_null |
| is_not_null | 不为空 | email is_not_null |

#### 4.3.4 影响范围预览

**预览显示内容**：

```
┌─────────────────────────────────────────────┐
│  属性级联编辑                               │
├─────────────────────────────────────────────┤
│                                             │
│  筛选条件：                                 │
│  ┌─────────────────────────────────────┐   │
│  │ Class: Person                       │   │
│  │ Status = 'active'                   │   │
│  │ Age > 30                            │   │
│  └─────────────────────────────────────┘   │
│                                             │
│  ┌─────────────────────────────────────┐   │
│  │ 影响范围预览                          │   │
│  ├─────────────────────────────────────┤   │
│  │ 总计匹配：128 个节点                  │   │
│  │                                     │   │
│  │ 分布：                               │   │
│  │ • Company A: 45 个                   │   │
│  │ • Company B: 38 个                   │   │
│  │ • Company C: 45 个                   │   │
│  └─────────────────────────────────────┘   │
│                                             │
│  修改属性：                                 │
│  ┌─────────────────────────────────────┐   │
│  │ 属性名: status                       │   │
│  │ 新值:   'inactive'                   │   │
│  └─────────────────────────────────────┘   │
│                                             │
│           [取消]    [确认修改]              │
└─────────────────────────────────────────────┘
```

#### 4.3.5 用户流程

```
1. 用户选择级联编辑入口
         ↓
2. 打开级联编辑弹窗
         ↓
3. 配置筛选条件：
   - 选择 Class 类型
   - 添加属性条件 (可 AND/OR 组合)
         ↓
4. 点击「预览影响范围」
         ↓
5. 显示匹配的节点数量和分布
         ↓
6. 配置要修改的属性和值
         ↓
7. 点击「确认修改」
         ↓
8. 执行批量更新 → Neo4j
         ↓
9. 显示修改结果统计
```

---

## 5. API 接口设计

### 5.1 图谱操作

#### 5.1.1 获取图谱元数据

```
GET /api/graph/{graphId}/metadata
```

**响应**：

```json
{
  "code": 0,
  "data": {
    "graphId": "graph_001",
    "name": "电商知识图谱",
    "description": "包含商品、用户、订单等实体",
    "status": "ACTIVE",
    "nodeCount": 15234,
    "edgeCount": 45321,
    "classCount": 8,
    "episodeCount": 156,
    "communityCount": 23
  }
}
```

#### 5.1.2 获取图谱可视化数据

```
GET /api/graph/{graphId}/visualization
```

**查询参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| layout | string | 布局类型：force/grid/dagre/concentric |
| page | int | 页码 (从 1 开始) |
| pageSize | int | 每页数量 (默认 100) |
| classType | string | 按类型筛选 |
| keyword | string | 搜索关键词 |

**响应**：

```json
{
  "code": 0,
  "data": {
    "nodes": [
      {
        "uuid": "node_001",
        "name": "张三",
        "type": "Person",
        "properties": {
          "age": 35,
          "city": "北京"
        }
      }
    ],
    "edges": [
      {
        "uuid": "edge_001",
        "source": "node_001",
        "target": "node_002",
        "type": "WORKS_FOR",
        "fact": "张三在阿里巴巴工作"
      }
    ],
    "pagination": {
      "page": 1,
      "pageSize": 100,
      "total": 15234,
      "totalPages": 153
    },
    "aggregations": {
      "byClass": [
        {"type": "Person", "count": 4521},
        {"type": "Company", "count": 2341}
      ]
    }
  }
}
```

#### 5.1.3 创建节点

```
POST /api/graph/{graphId}/nodes
```

**请求**：

```json
{
  "name": "李四",
  "type": "Person",
  "properties": {
    "age": 28,
    "city": "上海"
  }
}
```

#### 5.1.4 更新节点

```
PUT /api/graph/{graphId}/nodes/{uuid}
```

**请求**：

```json
{
  "name": "李四（已改名）",
  "properties": {
    "age": 29,
    "city": "深圳"
  }
}
```

#### 5.1.5 删除节点

```
DELETE /api/graph/{graphId}/nodes/{uuid}
```

#### 5.1.6 创建边

```
POST /api/graph/{graphId}/edges
```

**请求**：

```json
{
  "sourceUuid": "node_001",
  "targetUuid": "node_002",
  "type": "WORKS_FOR",
  "fact": "张三在阿里巴巴工作"
}
```

#### 5.1.7 获取节点详情

```
GET /api/graph/{graphId}/nodes/{uuid}
```

**响应**：

```json
{
  "code": 0,
  "data": {
    "uuid": "node_001",
    "name": "张三",
    "type": "Person",
    "summary": "某互联网公司工程师",
    "properties": {
      "age": 35,
      "city": "北京",
      "skills": ["Java", "Python"]
    },
    "createdAt": "2024-01-15T10:30:00Z",
    "updatedAt": "2024-03-20T14:22:00Z",
    "neighbors": {
      "count": 12,
      "sample": [
        {"uuid": "node_002", "name": "阿里巴巴", "type": "Company", "relation": "WORKS_FOR"}
      ]
    }
  }
}
```

#### 5.1.8 展开邻居节点

```
POST /api/graph/{graphId}/nodes/{uuid}/expand
```

**请求**：

```json
{
  "depth": 1,
  "edgeTypes": ["WORKS_FOR", "KNOWS"],
  "maxNodes": 50
}
```

### 5.2 Schema 操作

#### 5.2.1 获取本体定义列表

```
GET /api/graph/{graphId}/ontology/definitions
```

#### 5.2.2 获取类列表

```
GET /api/graph/{graphId}/ontology/classes
```

**响应**：

```json
{
  "code": 0,
  "data": [
    {
      "id": 1,
      "localName": "Person",
      "classUri": "http://example.org/ontology/Person",
      "description": "人物实体",
      "parentClassIds": [2, 3],
      "propertyCount": 5
    }
  ]
}
```

#### 5.2.3 创建类

```
POST /api/graph/{graphId}/ontology/classes
```

**请求**：

```json
{
  "localName": "Employee",
  "description": "员工",
  "parentClassIds": [1]
}
```

#### 5.2.4 更新类

```
PUT /api/graph/{graphId}/ontology/classes/{id}
```

#### 5.2.5 删除类

```
DELETE /api/graph/{graphId}/ontology/classes/{id}
```

#### 5.2.6 获取类属性列表

```
GET /api/graph/{graphId}/ontology/classes/{classId}/properties
```

**响应**：

```json
{
  "code": 0,
  "data": [
    {
      "id": 1,
      "localName": "name",
      "propertyType": "DATATYPE",
      "rangeDataType": "string",
      "isRequired": true,
      "defaultValue": null,
      "allowedValues": null,
      "pattern": null,
      "minValue": null,
      "maxValue": null
    },
    {
      "id": 2,
      "localName": "age",
      "propertyType": "DATATYPE",
      "rangeDataType": "integer",
      "isRequired": false,
      "defaultValue": "0",
      "allowedValues": null,
      "pattern": null,
      "minValue": 0,
      "maxValue": 150
    }
  ]
}
```

#### 5.2.7 创建/更新属性

```
POST /api/graph/{graphId}/ontology/classes/{classId}/properties
PUT /api/graph/{graphId}/ontology/classes/{classId}/properties/{propertyId}
```

**请求**：

```json
{
  "localName": "status",
  "propertyType": "DATATYPE",
  "rangeDataType": "string",
  "isRequired": true,
  "allowedValues": ["active", "inactive", "deleted"],
  "defaultValue": "active"
}
```

#### 5.2.8 Schema 变更影响检查

```
POST /api/graph/{graphId}/ontology/validate-change
```

**请求**：

```json
{
  "type": "UPDATE_CLASS",
  "classId": 1,
  "changes": {
    "isRequired": {"old": false, "new": true}
  }
}
```

**响应**：

```json
{
  "code": 0,
  "data": {
    "compatible": false,
    "affectedNodes": 45,
    "violations": [
      {
        "nodeUuid": "node_001",
        "propertyName": "status",
        "reason": "节点缺少必填属性 'status'"
      }
    ]
  }
}
```

### 5.3 级联编辑操作

#### 5.3.1 预览影响范围

```
POST /api/graph/{graphId}/cascade/preview
```

**请求**：

```json
{
  "classType": "Person",
  "conditions": [
    {"propertyName": "status", "operator": "eq", "value": "active"},
    {"propertyName": "age", "operator": "gt", "value": 30}
  ],
  "logic": "AND"
}
```

**响应**：

```json
{
  "code": 0,
  "data": {
    "totalMatch": 128,
    "distribution": [
      {"groupBy": "city", "value": "北京", "count": 45},
      {"groupBy": "city", "value": "上海", "count": 38},
      {"groupBy": "city", "value": "深圳", "count": 45}
    ]
  }
}
```

#### 5.3.2 执行级联修改

```
POST /api/graph/{graphId}/cascade/execute
```

**请求**：

```json
{
  "classType": "Person",
  "conditions": [
    {"propertyName": "status", "operator": "eq", "value": "active"},
    {"propertyName": "age", "operator": "gt", "value": 30}
  ],
  "logic": "AND",
  "updates": {
    "status": "inactive",
    "updatedAt": "2024-05-16T00:00:00Z"
  }
}
```

**响应**：

```json
{
  "code": 0,
  "data": {
    "success": true,
    "affectedCount": 128,
    "failedCount": 0,
    "errors": []
  }
}
```

---

## 6. 数据库设计

### 6.1 MySQL 表结构

#### 6.1.1 ont_definition (本体定义表)

```sql
CREATE TABLE ont_definition (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    graph_id VARCHAR(64) NOT NULL COMMENT '关联的图谱ID',
    namespace VARCHAR(255) DEFAULT 'http://example.org/ontology' COMMENT '命名空间',
    name VARCHAR(128) NOT NULL COMMENT '本体名称',
    version VARCHAR(32) NOT NULL DEFAULT '1.0.0' COMMENT '版本号',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/DEPRECATED/DRAFT',
    created_by VARCHAR(64) COMMENT '创建人',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_graph_id (graph_id),
    INDEX idx_status (status)
) COMMENT '本体定义表';
```

#### 6.1.2 ont_class (类定义表)

```sql
CREATE TABLE ont_class (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    definition_id BIGINT NOT NULL COMMENT '所属本体ID',
    class_uri VARCHAR(512) NOT NULL COMMENT '类完整URI',
    local_name VARCHAR(128) NOT NULL COMMENT '本地名称',
    description TEXT COMMENT '类描述',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (definition_id) REFERENCES ont_definition(id) ON DELETE CASCADE,
    INDEX idx_definition_id (definition_id),
    INDEX idx_local_name (local_name)
) COMMENT '类定义表';
```

#### 6.1.3 ont_class_inheritance (类继承关系表)

```sql
CREATE TABLE ont_class_inheritance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    class_id BIGINT NOT NULL COMMENT '子类ID',
    parent_class_id BIGINT NOT NULL COMMENT '父类ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (class_id) REFERENCES ont_class(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_class_id) REFERENCES ont_class(id) ON DELETE CASCADE,
    UNIQUE KEY uk_class_parent (class_id, parent_class_id)
) COMMENT '类继承关系表（支持多继承）';
```

#### 6.1.4 ont_property (属性定义表)

```sql
CREATE TABLE ont_property (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    definition_id BIGINT NOT NULL COMMENT '所属本体ID',
    property_uri VARCHAR(512) NOT NULL COMMENT '属性完整URI',
    local_name VARCHAR(128) NOT NULL COMMENT '本地名称',
    property_type ENUM('DATATYPE', 'OBJECT', 'ANNOTATION', 'TRANSITIVE', 'SYMMETRIC', 'FUNCTIONAL') DEFAULT 'DATATYPE' COMMENT '属性类型',
    domain_class_id BIGINT COMMENT '定义域类ID',
    range_class_id BIGINT COMMENT '值域类ID（对象属性时使用）',
    range_data_type VARCHAR(32) COMMENT '值域数据类型：string/integer/float/boolean/date/json',
    is_required TINYINT(1) DEFAULT 0 COMMENT '是否必填',
    is_multiple TINYINT(1) DEFAULT 0 COMMENT '是否多值',
    default_value TEXT COMMENT '默认值',
    allowed_values JSON COMMENT '枚举值列表',
    pattern VARCHAR(512) COMMENT '正则表达式',
    min_value DECIMAL(20,6) COMMENT '最小值',
    max_value DECIMAL(20,6) COMMENT '最大值',
    min_cardinality INT COMMENT '最小基数',
    max_cardinality INT COMMENT '最大基数',
    description TEXT COMMENT '属性描述',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (definition_id) REFERENCES ont_definition(id) ON DELETE CASCADE,
    FOREIGN KEY (domain_class_id) REFERENCES ont_class(id) ON DELETE CASCADE,
    FOREIGN KEY (range_class_id) REFERENCES ont_class(id) ON DELETE SET NULL,
    INDEX idx_definition_id (definition_id),
    INDEX idx_domain_class_id (domain_class_id),
    INDEX idx_local_name (local_name)
) COMMENT '属性定义表';
```

### 6.2 Neo4j 图结构

#### 6.2.1 节点标签：Entity

```cypher
// 节点属性
{
  group_id: String,       // 图谱ID
  uuid: String,           // 唯一标识
  name: String,          // 节点名称
  type: String,          // 节点类型（对应 Class.localName）
  summary: String,        // 摘要描述
  embedding: Float[],     // 向量嵌入
  valid_at: Long,         // 生效时间戳
  invalid_at: Long,       // 失效时间戳 (null 表示有效)
  // ... 动态属性存储在 properties 中
}
```

#### 6.2.2 关系类型

```cypher
// 关系属性
{
  uuid: String,           // 关系唯一标识
  type: String,           // 关系类型
  fact: String,           // 关系事实描述
  embedding: Float[],     // 向量嵌入
  valid_at: Long,         // 生效时间戳
  invalid_at: Long,       // 失效时间戳
  // ... 动态属性
}
```

---

## 7. 交互设计

### 7.1 主界面布局

```
┌─────────────────────────────────────────────────────────────────────────┐
│  [Logo] Graphiti    图谱管理 / 电商知识图谱          [同步] [设置] [用户] │
├────────┬────────────────────────────────────────────────────┬────────────┤
│        │                                                    │            │
│ 资源   │                                                    │  详情      │
│ 管理   │              图谱可视化画布                          │  面板      │
│        │                                                    │            │
│ ├─图谱 │                                                    │ ┌────────┐ │
│ │  ├─本体│                                                    │ │ 基本   │ │
│ │  │ ├类 │                                                    │ │ 信息   │ │
│ │  │ │属性│                                                    │ ├────────┤ │
│ │  │ └关系│                                                    │ │ 属性   │ │
│ │  │     │                                                    │ ├────────┤ │
│ │  ├─实例│                                                    │ │ 关联   │ │
│ │  ├─边   │                                                    │ │ 关系   │ │
│ │  ├─事件 │                                                    │ ├────────┤ │
│ │  └─社区 │                                                    │ │ 关联   │ │
│ │        │                                                    │ │ 事件   │ │
│ └─其他   │                                                    │ └────────┘ │
├────────┴────────────────────────────────────────────────────┴────────────┤
│  节点: 15,234  边: 45,321  类: 8  事件: 156  社区: 23    [GraphQL ▷]   ● │
└─────────────────────────────────────────────────────────────────────────┘
```

### 7.2 Schema 编辑器布局

```
┌─────────────────────────────────────────────────────────────────────────┐
│  Schema 编辑器                                    [保存] [发布] [撤销]  │
├────────────┬────────────────────────────────┬───────────────────────────┤
│            │                                │                           │
│ 类树       │      继承关系图                 │    属性配置                │
│            │                                │                           │
│ ├─Thing    │         ┌───────┐              │ ┌─────────────────────┐ │
│ │ ├─Agent   │         │ Thing │              │ │ 类: Person           │ │
│ │ │ └Person │         └───┬───┘              │ ├─────────────────────┤ │
│ │ └─Entity  │       ┌─────┴─────┐            │ │ 基本信息             │ │
│ │   └─Person│       │           │            │ │ • 本地名称: Person   │ │
│ ├─Company  │    ┌───┴───┐   ┌───┴───┐        │ │ • 描述: 人物实体     │ │
│ ├─Product  │    │ Agent │   │Entity │        │ │ • 父类: Agent,Entity│ │
│ └─Location │    └───┬───┘   └───┬───┘        │ ├─────────────────────┤ │
│            │        │           │             │ │ 继承关系             │ │
│ [+] 添加类 │        └─────┬─────┘             │ │ ┌───────────────┐   │ │
│            │              │                   │ │ │  ○ Agent      │   │ │
│            │         ┌────┴────┐              │ │ │  ○ Entity     │   │ │
│            │         │ Person  │              │ │ │ ● 当前类       │   │ │
│            │         └─────────┘              │ │ └───────────────┘   │ │
│            │                                │ ├─────────────────────┤ │
│            │                                │ │ 属性列表             │ │
│            │                                │ │ [+ 添加属性]         │ │
│            │                                │ │ ┌─────────────────┐ │ │
│            │                                │ │ │ name (string)   │ │ │
│            │                                │ │ │ age (integer)   │ │ │
│            │                                │ │ │ city (string)   │ │ │
│            │                                │ │ └─────────────────┘ │ │
└────────────┴────────────────────────────────┴───────────────────────────┘
```

### 7.3 级联编辑弹窗

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        属性级联编辑                               [✕]    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  筛选条件                                            [添加条件]         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │ Class: [Person ▼]  [+ 添加条件 ▼]                                │   │
│  │                                                                   │   │
│  │ 条件 1: property=[status ▼]  operator=[等于▼]  value=[active ]   │   │
│  │ 条件 2: property=[age ▼]      operator=[大于▼]  value=[30    ]   │   │
│  │                          [AND ▼]                                 │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  [预览影响范围]                                                         │
│                                                                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │ 匹配节点数：128                                                   │   │
│  │                                                                   │   │
│  │ 分布：                                                            │   │
│  │ • 北京: 45                                                        │   │
│  │ • 上海: 38                                                        │   │
│  │ • 深圳: 45                                                        │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│  修改内容                                                               │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │ 属性名: [status ▼]                                               │   │
│  │ 新值:   [inactive]                                               │   │
│  │                                                                   │   │
│  │ [+ 添加更多修改]                                                  │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                         │
│                            [取消]              [确认修改 (128 个节点)]   │
└─────────────────────────────────────────────────────────────────────────┘
```

### 7.4 交互规范

| 场景 | 行为 |
|------|------|
| 单击节点 | 选中节点，打开详情面板 |
| 双击节点 | 进入属性编辑模式 |
| 右键节点 | 显示上下文菜单（查看、编辑、删除、展开） |
| 拖拽节点 | 调整节点位置（取消拖拽则弹回） |
| 拖出连接线 | 创建新边 |
| 滚轮 | 缩放图谱 |
| 空格+拖拽 | 平移画布 |
| Delete | 删除选中的节点/边 |

---

## 8. 技术实现要点

### 8.1 图谱渲染性能优化

1. **虚拟化渲染**：只渲染可视区域内的节点
2. **LOD 策略**：缩放级别决定渲染细节（节点标签、图标、属性）
3. **聚合模式**：节点过多时按类型聚合
4. **Web Worker**：布局计算在 Worker 线程执行
5. **Canvas 渲染**：大量节点使用 Canvas 替代 SVG

### 8.2 实时同步

1. **WebSocket**：节点变更实时推送到前端
2. **乐观更新**：先更新 UI，后端失败则回滚
3. **冲突检测**：多人编辑时检测冲突并提示

### 8.3 本体验证

1. **前端预览**：编辑时实时校验
2. **后端验证**：保存时完整校验
3. **变更影响**：修改 Schema 前计算影响范围

### 8.4 级联编辑事务

1. **批量操作**：使用 Neo4j 的 `UNWIND` 进行批量更新
2. **事务保障**：整个级联操作在一个事务中
3. **进度反馈**：大批量操作显示进度条

---

## 附录

### A. 类型定义

```typescript
// types/graph.ts

export interface GraphMetadata {
  graphId: string;
  name: string;
  description?: string;
  status: 'ACTIVE' | 'INACTIVE' | 'DRAFT';
  nodeCount: number;
  edgeCount: number;
  classCount: number;
  episodeCount: number;
  communityCount: number;
}

export interface GraphNode {
  uuid: string;
  name: string;
  type: string;
  summary?: string;
  properties: Record<string, any>;
  createdAt?: string;
  updatedAt?: string;
}

export interface GraphEdge {
  uuid: string;
  source: string;
  target: string;
  type: string;
  fact?: string;
  properties: Record<string, any>;
}

export interface OntClass {
  id: number;
  definitionId: number;
  classUri: string;
  localName: string;
  description?: string;
  parentClassIds: number[];
}

export interface OntProperty {
  id: number;
  definitionId: number;
  localName: string;
  propertyType: 'DATATYPE' | 'OBJECT' | 'ANNOTATION';
  rangeDataType?: string;
  domainClassId?: number;
  rangeClassId?: number;
  isRequired: boolean;
  isMultiple: boolean;
  defaultValue?: any;
  allowedValues?: any[];
  pattern?: string;
  minValue?: number;
  maxValue?: number;
}

export interface CascadeFilter {
  classType: string;
  conditions: PropertyCondition[];
  logic: 'AND' | 'OR';
}

export interface PropertyCondition {
  propertyName: string;
  operator: 'eq' | 'ne' | 'gt' | 'lt' | 'gte' | 'lte' | 'contains' | 'not_contains' | 'in' | 'not_in' | 'is_null' | 'is_not_null';
  value: any;
}
```

### B. 状态码定义

| 状态码 | 说明 |
|--------|------|
| 0 | 成功 |
| 1001 | 图谱不存在 |
| 1002 | 节点不存在 |
| 1003 | 类不存在 |
| 1004 | 属性不存在 |
| 2001 | Schema 验证失败 |
| 2002 | Schema 变更影响现有数据 |
| 3001 | 权限不足 |
| 5000 | 系统内部错误 |
