# EpisodeType 2跳查询可视化设计文档

> **日期**: 2026-05-24
> **关联需求**: EpisodeTypeExplorer.vue 树节点点击后，查询该类型节点的2跳邻居并在画布可视化展示
> **设计状态**: 已评审通过

---

## 1. 概述

### 1.1 目标

当用户在 EpisodeTypeExplorer 侧边栏点击剧集类型树节点时：
1. 查询所有具有该 `episode_type` 的节点（支持分页，每页20个）
2. 查询这些节点的2跳邻居子图（双向关系，支持1/2/3跳可选）
3. 将数据追加到主窗体画布进行可视化展示
4. 右侧面板实例列表与画布节点保持分页同步

### 1.2 架构原则

- **单次请求**：后端统一 API 完成分页+扩展，减少网络往返
- **追加模式**：翻页时新数据追加到画布，保留上下文
- **同步分页**：画布节点与右侧面板实例表格共享分页状态
- **跳数可选**：用户可在右侧面板切换1/2/3跳，切换时清空重载

---

## 2. 数据流架构

```
EpisodeTypeExplorer.vue          ide.vue                    后端 API
       │                            │                           │
       │  @select-type              │                           │
       │ {typeId,typeCode}          │                           │
       ├───────────────────────────>│                           │
       │                            │  GET /visualization/      │
       │                            │  episodes/by-type         │
       │                            │  ?typeCode=X&page=1&      │
       │                            │  pageSize=20&depth=2      │
       │                            ├──────────────────────────>│
       │                            │                           │ Neo4j
       │                            │  {nodes,edges,pagination} │
       │                            │<──────────────────────────┤
       │                            │                           │
       │                            │  nodes = dedupe(旧+新)    │
       │                            │  edges = dedupe(旧+新)    │
       │                            │                           │
       │                            │  <GraphCanvas>            │
       │                            │  :nodes :edges            │
       │                            │                           │
       │                            │  <EpisodeTypeDetailPanel> │
       │                            │  :pagination :depth       │
       │                            │  @pagination-change       │
       │                            │  @depth-change            │
       └────────────────────────────┴───────────────────────────┘
```

---

## 3. 后端设计

### 3.1 API 端点

```java
@GetMapping("/{graphId}/visualization/episodes/by-type")
@Operation(
    summary = "根据剧集类型获取分页可视化数据（含N跳邻居）",
    description = "获取指定 episode_type 的分页节点数据，以及这些节点的N跳双向关系子图"
)
public CommonResult<GraphVisualizationRespVO> getEpisodesVisualizationByType(
    @PathVariable @Parameter(description = "图谱ID") String graphId,
    @RequestParam @Parameter(description = "类型编码", required = true) String typeCode,
    @RequestParam(defaultValue = "1") @Parameter(description = "页码") Integer page,
    @RequestParam(defaultValue = "20") @Parameter(description = "每页数量") Integer pageSize,
    @RequestParam(defaultValue = "2") @Parameter(description = "扩展跳数") Integer depth
)
```

### 3.2 Cypher 查询

#### 3.2.1 分页匹配节点

```cypher
MATCH (n:Episode {graph_id: $graphId, episode_type: $typeCode})
WHERE n.invalid_at IS NULL
RETURN n
ORDER BY n.valid_at DESC
SKIP $skip LIMIT $limit
```

#### 3.2.2 2跳扩展（depth=2）

```cypher
MATCH (center:Episode {graph_id: $graphId, episode_type: $typeCode})
WHERE center.invalid_at IS NULL
WITH center
ORDER BY center.valid_at DESC
SKIP $skip LIMIT $limit

// 第1跳（双向）
OPTIONAL MATCH (center)-[r1]-(n1)
WHERE n1.graph_id = $graphId AND n1.invalid_at IS NULL

// 第2跳（双向，避免回环中心节点）
OPTIONAL MATCH (n1)-[r2]-(n2)
WHERE n2.graph_id = $graphId AND n2.invalid_at IS NULL
  AND n2 <> center

RETURN center, r1, n1, r2, n2
```

#### 3.2.3 动态跳数生成

根据 `depth` 参数动态生成 Cypher（1/2/3跳）：

```java
StringBuilder cypher = new StringBuilder();
cypher.append("MATCH (center:Episode {graph_id: $graphId, episode_type: $typeCode}) ");
cypher.append("WHERE center.invalid_at IS NULL ");
cypher.append("WITH center ORDER BY center.valid_at DESC SKIP $skip LIMIT $limit ");

// 动态添加 OPTIONAL MATCH 链
for (int i = 1; i <= depth; i++) {
    String prev = (i == 1) ? "center" : "n" + (i - 1);
    String curr = "n" + i;
    String rel = "r" + i;
    cypher.append("OPTIONAL MATCH (").append(prev).append(")-[").append(rel).append("]-(").append(curr).append(") ");
    cypher.append("WHERE ").append(curr).append(".graph_id = $graphId AND ").append(curr).append(".invalid_at IS NULL ");
    // 避免回环到中心节点
    if (i == depth) {
        cypher.append("AND ").append(curr).append(" <> center ");
    }
}

cypher.append("RETURN center");
for (int i = 1; i <= depth; i++) {
    cypher.append(", r").append(i).append(", n").append(i);
}
```

### 3.3 返回格式

复用现有的 `GraphVisualizationRespVO`，增加分页元数据：

```json
{
  "code": 200,
  "data": {
    "nodes": [
      { "uuid": "...", "name": "...", "type": "Episode", "summary": "..." }
    ],
    "edges": [
      { "uuid": "...", "source": "...", "target": "...", "type": "MENTIONS", "fact": "..." }
    ],
    "pagination": {
      "page": 1,
      "pageSize": 20,
      "total": 150,
      "totalPages": 8,
      "hasNextPage": true
    }
  }
}
```

---

## 4. 前端设计

### 4.1 状态管理（ide.vue）

```typescript
// 分页状态（与右侧面板共享）
const episodePagination = ref({
  page: 1,
  pageSize: 20,
  total: 0,
  totalPages: 0,
  hasNextPage: false
})

// 跳数（1/2/3）
const episodeDepth = ref(2)

// V5.0: Episode 类型管理（已有）
const selectedEpisodeType = ref<OntEpisodeTypeVO | null>(null)
```

### 4.2 事件处理

#### 4.2.1 首次加载（类型选择）

```typescript
const handleEpisodeTypeSelect = async (payload: { 
  typeId: number; 
  typeCode: string; 
  typeName: string 
}) => {
  // 重置
  episodePage.value = 1
  episodeDepth.value = 2
  nodes.value = []
  edges.value = []
  ontologyMode.value = 'episodes'
  showPanel.value = true
  loading.value = true
  
  try {
    await Promise.all([
      loadEpisodeTypeVisualization(payload.typeCode),
      loadEpisodeTypeDetail(payload.typeId)
    ])
  } catch (e) {
    message.error('加载类型数据失败')
  } finally {
    loading.value = false
  }
}
```

#### 4.2.2 加载可视化数据

```typescript
const loadEpisodeTypeVisualization = async (typeCode: string) => {
  const res = await graphApi.getEpisodesVisualizationByType(
    effectiveGraphId.value,
    typeCode,
    episodePagination.value.page,
    episodePagination.value.pageSize,
    episodeDepth.value
  )
  
  // 追加模式（按 uuid 去重）
  nodes.value = dedupeNodes([...nodes.value, ...(res.nodes || [])])
  edges.value = dedupeEdges([...edges.value, ...(res.edges || [])])
  
  // 更新分页
  episodePagination.value = {
    ...episodePagination.value,
    total: res.pagination?.total || 0,
    totalPages: res.pagination?.totalPages || 0,
    hasNextPage: res.pagination?.hasNextPage || false
  }
}
```

#### 4.2.3 分页变更（同步画布+表格）

```typescript
const handleEpisodePaginationChange = async (newPage: number) => {
  episodePagination.value.page = newPage
  const typeCode = selectedEpisodeType.value?.typeCode
  if (!typeCode) return
  
  loading.value = true
  try {
    await loadEpisodeTypeVisualization(typeCode)
    // EpisodeTypeDetailPanel 通过 props 自动感知分页变化，自动刷新表格
  } finally {
    loading.value = false
  }
}
```

#### 4.2.4 跳数变更（清空重载）

```typescript
const handleEpisodeDepthChange = async (newDepth: number) => {
  episodeDepth.value = newDepth
  episodePagination.value.page = 1
  nodes.value = []
  edges.value = []
  
  const typeCode = selectedEpisodeType.value?.typeCode
  if (typeCode) {
    loading.value = true
    try {
      await loadEpisodeTypeVisualization(typeCode)
    } finally {
      loading.value = false
    }
  }
}
```

### 4.3 EpisodeTypeDetailPanel.vue 改造

#### 4.3.1 Props 扩展

```typescript
const props = defineProps<{
  graphId: string
  typeId: number
  typeData?: OntEpisodeTypeVO
  pagination: {
    page: number
    pageSize: number
    total: number
    totalPages: number
    hasNextPage: boolean
  }
  depth: number
}>()
```

#### 4.3.2 Emits 扩展

```typescript
const emit = defineEmits<{
  (e: 'edit-type', typeId: number): void
  (e: 'delete-type', typeId: number): void
  (e: 'navigate-to-instance', uuid: string): void
  (e: 'pagination-change', page: number): void
  (e: 'depth-change', depth: number): void
}>()
```

#### 4.3.3 分页控件（实例 Tab 下方）

```vue
<div v-else-if="activeTab === 'instances'" class="tab-content">
  <a-table
    :data-source="instanceList"
    :columns="instanceColumns"
    :pagination="false"
    size="small"
    :loading="instancesLoading"
  >
    <template #bodyCell="{ column, record }">
      <template v-if="column.key === 'name'">
        <a @click="handleNavigateToInstance(record.uuid)">{{ record.name }}</a>
      </template>
      <template v-if="column.key === 'source'">
        <a-tag size="small">{{ record.source }}</a-tag>
      </template>
    </template>
  </a-table>
  
  <!-- 同步分页控件 -->
  <div class="instance-pagination-bar">
    <a-pagination
      v-model:current="currentPage"
      :total="props.pagination.total"
      :pageSize="props.pagination.pageSize"
      size="small"
      show-less-items
      @change="handlePageChange"
    />
    <a-select 
      v-model:value="currentDepth" 
      size="small" 
      style="width: 70px; margin-left: 8px"
      @change="handleDepthChange"
    >
      <a-select-option :value="1">1跳</a-select-option>
      <a-select-option :value="2">2跳</a-select-option>
      <a-select-option :value="3">3跳</a-select-option>
    </a-select>
  </div>
</div>
```

#### 4.3.4 计算属性（双向绑定风格）

```typescript
const currentPage = computed({
  get: () => props.pagination.page,
  set: (val) => emit('pagination-change', val)
})

const currentDepth = computed({
  get: () => props.depth,
  set: (val) => emit('depth-change', val)
})
```

#### 4.3.5 实例表格加载（响应分页变化）

```typescript
const loadInstances = async () => {
  instancesLoading.value = true
  try {
    const res = await episodeTypeApi.getInstances(
      props.graphId,
      props.typeId,
      props.pagination.page,
      props.pagination.pageSize
    )
    instanceList.value = res.episodes || []
  } finally {
    instancesLoading.value = false
  }
}

// 监听分页变化自动刷新
watch(() => props.pagination.page, loadInstances, { immediate: true })
```

### 4.4 graph.ts API 签名更新

```typescript
// 修改现有方法签名
async getEpisodesVisualizationByType(
  graphId: string,
  typeCode: string,
  page?: number,        // 新增
  pageSize?: number,    // 新增
  depth?: number        // 新增
): Promise<GraphVisualizationData> {
  return request.get(`/graph/${graphId}/visualization/episodes/by-type`, {
    params: { 
      typeCode, 
      page: page || 1, 
      pageSize: pageSize || 20, 
      depth: depth || 2 
    }
  })
}
```

---

## 5. 错误处理

| 场景 | 处理策略 |
|------|----------|
| 类型无实例 | API 返回空数据，`nodes: []`，表格显示"暂无数据"，画布保留或清空 |
| Neo4j 超时 | 后端返回 500 + 错误消息，前端 `message.error('查询超时，请缩小查询范围')` |
| 翻页网络中断 | 保留当前数据，提示失败，用户可重试 |
| 跳数切换 | 清空画布 + 重置 page=1，避免不同跳数数据混合 |
| 切换类型 | 清空画布 + 重置分页，加载新类型 |
| 3跳数据过大 | 后端设置 LIMIT 保护（如最大 1000 节点），超出时截断并返回警告 |

---

## 6. 文件变更清单

### 后端

| 文件 | 变更类型 | 说明 |
|------|----------|------|
| `GraphIDEController.java` | 新增方法 | `GET /{graphId}/visualization/episodes/by-type` |
| `GraphVisualizationService.java` | 新增方法 | `getEpisodesVisualizationByType(...)` |
| `GraphVisualizationServiceImpl.java` | 新增实现 | 分页 Cypher + 动态跳数扩展 |

### 前端

| 文件 | 变更类型 | 说明 |
|------|----------|------|
| `graph.ts` | 修改 | 更新 `getEpisodesVisualizationByType` 签名 |
| `ide.vue` | 修改 | 新增分页/深度状态，改造事件处理函数 |
| `EpisodeTypeDetailPanel.vue` | 修改 | 新增分页控件、跳数选择器、props/emits |

---

## 7. 性能考量

1. **Cypher 优化**：使用 `SKIP/LIMIT` 分页中心节点，避免全表扫描；`OPTIONAL MATCH` 确保即使无邻居也返回中心节点
2. **数据量控制**：`pageSize=20`，每页最多 20 个中心节点 + 其邻居，后端设置总体节点上限（如 1000）
3. **追加去重**：前端使用 `Map<uuid, Node>` 去重，时间复杂度 O(n)
4. **深度限制**：最大支持 3 跳，避免指数级数据膨胀

---

*设计文档完成，待用户评审通过后进入实施阶段。*
