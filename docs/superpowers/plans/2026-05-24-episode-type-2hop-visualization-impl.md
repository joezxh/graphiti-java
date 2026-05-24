# EpisodeType 2跳查询可视化 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现 EpisodeType 树节点点击后的2跳邻居查询可视化，支持分页（每页20个）、追加模式、跳数可选（1/2/3跳），并与右侧面板实例列表分页同步。

**Architecture:** 后端新增统一API完成分页+动态跳数扩展（Neo4j Cypher），前端 ide.vue 管理分页/深度状态，EpisodeTypeDetailPanel.vue 展示同步分页控件，GraphCanvas 渲染追加后的图谱数据。

**Tech Stack:** Spring Boot 3 + Neo4j + Vue 3 + TypeScript + Ant Design Vue

**Design Doc:** `docs/superpowers/specs/2026-05-24-episode-type-2hop-visualization-design.md`

---

## 文件结构

| 文件 | 责任 | 变更类型 |
|------|------|----------|
| `graphiti-module-core/.../GraphIDEController.java` | REST API 入口，路由到 Service | 修改 |
| `graphiti-module-core/.../GraphVisualizationService.java` | 接口定义 | 修改 |
| `graphiti-module-core/.../GraphVisualizationServiceImpl.java` | Cypher 查询实现（分页+动态跳数） | 修改 |
| `graphiti-web/src/api/graph.ts` | 前端 API 调用，更新签名 | 修改 |
| `graphiti-web/src/views/graph/ide.vue` | 状态管理、事件处理、数据流 orchestration | 修改 |
| `graphiti-web/src/components/Ontology/EpisodeTypeDetailPanel.vue` | 实例表格、分页控件、跳数选择器 | 修改 |

---

## Task 1: 后端 Service 接口扩展

**Files:**
- Modify: `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/service/GraphVisualizationService.java`

- [ ] **Step 1: 在接口中添加新方法声明**

```java
/**
 * V5.0: 根据剧集类型获取分页可视化数据（含N跳邻居）
 */
GraphVisualizationRespVO getEpisodesVisualizationByType(
        String graphId,
        String typeCode,
        Integer page,
        Integer pageSize,
        Integer depth);
```

- [ ] **Step 2: Commit**

```bash
git add graphiti-module-core/src/main/java/com/graphiti/module/graphiti/service/GraphVisualizationService.java
git commit -m "feat(api): add getEpisodesVisualizationByType interface"
```

---

## Task 2: 后端 Service 实现（Cypher 查询）

**Files:**
- Modify: `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/GraphVisualizationServiceImpl.java`

**前置检查：** 先确认文件中现有的 `getEpisodesVisualization` 方法实现，了解节点/边转换模式。

- [ ] **Step 1: 添加实现方法**

在 `GraphVisualizationServiceImpl.java` 的合适位置（如 `getEpisodesVisualization` 方法之后）添加：

```java
@Override
public GraphVisualizationRespVO getEpisodesVisualizationByType(
        String graphId,
        String typeCode,
        Integer page,
        Integer pageSize,
        Integer depth) {

    int effectivePage = page != null && page > 0 ? page : 1;
    int effectivePageSize = pageSize != null && pageSize > 0 ? pageSize : 20;
    int effectiveDepth = depth != null && depth >= 1 && depth <= 3 ? depth : 2;
    int skip = (effectivePage - 1) * effectivePageSize;

    try (Session session = neo4jDriver.session()) {
        // 阶段 1: 统计总数
        String countCypher =
            "MATCH (n:Episode {graph_id: $graphId, episode_type: $typeCode}) " +
            "WHERE n.invalid_at IS NULL " +
            "RETURN count(n) as total";
        Result countResult = session.run(countCypher,
            Map.of("graphId", graphId, "typeCode", typeCode));
        long total = countResult.hasNext() ? countResult.next().get("total").asLong() : 0;

        // 阶段 2: 分页查询中心节点 + N跳扩展
        StringBuilder cypherBuilder = new StringBuilder();
        cypherBuilder.append("MATCH (center:Episode {graph_id: $graphId, episode_type: $typeCode}) ");
        cypherBuilder.append("WHERE center.invalid_at IS NULL ");
        cypherBuilder.append("WITH center ORDER BY center.valid_at DESC SKIP $skip LIMIT $limit ");

        // 动态生成 OPTIONAL MATCH 链
        for (int i = 1; i <= effectiveDepth; i++) {
            String prev = (i == 1) ? "center" : "n" + (i - 1);
            String curr = "n" + i;
            String rel = "r" + i;
            cypherBuilder.append("OPTIONAL MATCH (").append(prev).append(")-[").append(rel).append("]-(").append(curr).append(") ");
            cypherBuilder.append("WHERE ").append(curr).append(".graph_id = $graphId AND ").append(curr).append(".invalid_at IS NULL ");
            if (i == effectiveDepth) {
                cypherBuilder.append("AND ").append(curr).append(" <> center ");
            }
        }

        // RETURN 所有节点和关系
        cypherBuilder.append("RETURN center");
        for (int i = 1; i <= effectiveDepth; i++) {
            cypherBuilder.append(", r").append(i).append(", n").append(i);
        }

        Map<String, Object> params = new HashMap<>();
        params.put("graphId", graphId);
        params.put("typeCode", typeCode);
        params.put("skip", skip);
        params.put("limit", effectivePageSize);

        Result result = session.run(cypherBuilder.toString(), params);

        // 收集节点和边
        List<GraphVisualizationRespVO.NodeVO> nodes = new ArrayList<>();
        List<GraphVisualizationRespVO.EdgeVO> edges = new ArrayList<>();
        Set<String> nodeUuids = new HashSet<>();
        Set<String> edgeUuids = new HashSet<>();

        while (result.hasNext()) {
            Record record = result.next();

            // 提取中心节点
            extractNode(record, "center", nodes, nodeUuids);

            // 提取各跳节点和关系
            for (int i = 1; i <= effectiveDepth; i++) {
                extractNode(record, "n" + i, nodes, nodeUuids);
                extractEdge(record, "r" + i, edges, edgeUuids);
            }
        }

        // 查询匹配节点之间的边（可能通过不同跳数路径遗漏的边）
        if (!nodeUuids.isEmpty()) {
            String edgeCypher =
                "MATCH (a:Episode {graph_id: $graphId})-[r]-(b:Episode {graph_id: $graphId}) " +
                "WHERE a.uuid IN $uuids AND b.uuid IN $uuids " +
                "AND a.invalid_at IS NULL AND b.invalid_at IS NULL " +
                "RETURN a.uuid as source, b.uuid as target, type(r) as type, r.uuid as uuid, r.fact as fact";
            Result edgeResult = session.run(edgeCypher,
                Map.of("graphId", graphId, "uuids", new ArrayList<>(nodeUuids)));
            while (edgeResult.hasNext()) {
                extractEdgeFromResult(edgeResult.next(), edges, edgeUuids);
            }
        }

        int totalPages = (int) Math.ceil((double) total / effectivePageSize);
        boolean hasNextPage = effectivePage < totalPages;

        return GraphVisualizationRespVO.builder()
                .nodes(nodes)
                .edges(edges)
                .pagination(GraphVisualizationRespVO.PaginationVO.builder()
                        .page(effectivePage)
                        .pageSize(effectivePageSize)
                        .total(total)
                        .totalPages(totalPages)
                        .hasNextPage(hasNextPage)
                        .build())
                .build();
    }
}

// 辅助方法：提取节点
private void extractNode(Record record, String key,
                         List<GraphVisualizationRespVO.NodeVO> nodes,
                         Set<String> nodeUuids) {
    if (!record.containsKey(key) || record.get(key).isNull()) return;
    var neo4jNode = record.get(key).asNode();
    Map<String, Object> nodeMap = neo4jNode.asMap();
    String uuid = (String) nodeMap.get("uuid");
    if (uuid == null || nodeUuids.contains(uuid)) return;
    nodeUuids.add(uuid);

    String nodeType = (String) nodeMap.get("type");
    String nodeName = extractNodeName(nodeType, nodeMap);

    nodes.add(GraphVisualizationRespVO.NodeVO.builder()
            .uuid(uuid)
            .name(nodeName)
            .type(nodeType)
            .summary((String) nodeMap.get("summary"))
            .properties(extractProperties(nodeMap))
            .build());
}

// 辅助方法：提取边
private void extractEdge(Record record, String key,
                         List<GraphVisualizationRespVO.EdgeVO> edges,
                         Set<String> edgeUuids) {
    if (!record.containsKey(key) || record.get(key).isNull()) return;
    var rel = record.get(key).asRelationship();
    String uuid = rel.get("uuid").asString();
    if (uuid == null || edgeUuids.contains(uuid)) return;
    edgeUuids.add(uuid);

    edges.add(GraphVisualizationRespVO.EdgeVO.builder()
            .uuid(uuid)
            .source(rel.startNodeElementId())
            .target(rel.endNodeElementId())
            .type(rel.type())
            .fact(rel.containsKey("fact") ? rel.get("fact").asString() : null)
            .build());
}

// 从独立查询结果提取边
private void extractEdgeFromResult(Record record,
                                   List<GraphVisualizationRespVO.EdgeVO> edges,
                                   Set<String> edgeUuids) {
    String uuid = record.get("uuid").asString();
    if (uuid == null || edgeUuids.contains(uuid)) return;
    edgeUuids.add(uuid);

    edges.add(GraphVisualizationRespVO.EdgeVO.builder()
            .uuid(uuid)
            .source(record.get("source").asString())
            .target(record.get("target").asString())
            .type(record.get("type").asString())
            .fact(record.containsKey("fact") && !record.get("fact").isNull()
                    ? record.get("fact").asString() : null)
            .build());
}
```

- [ ] **Step 2: 编译验证**

```bash
cd graphiti-module-core && mvn compile -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add graphiti-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/GraphVisualizationServiceImpl.java
git commit -m "feat(api): implement getEpisodesVisualizationByType with dynamic depth Cypher"
```

---

## Task 3: 后端 Controller 新增端点

**Files:**
- Modify: `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/controller/admin/GraphIDEController.java`

- [ ] **Step 1: 在 Controller 中添加新端点**

在现有 `getEpisodesVisualization` 方法之后添加：

```java
/**
 * V5.0: 根据剧集类型获取分页可视化数据（含N跳邻居）
 */
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
        @RequestParam(defaultValue = "2") @Parameter(description = "扩展跳数") Integer depth) {
    return CommonResult.success(
            graphVisualizationService.getEpisodesVisualizationByType(graphId, typeCode, page, pageSize, depth)
    );
}
```

- [ ] **Step 2: 编译验证**

```bash
cd graphiti-module-core && mvn compile -q
```

Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add graphiti-module-core/src/main/java/com/graphiti/module/graphiti/controller/admin/GraphIDEController.java
git commit -m "feat(api): add GET /visualization/episodes/by-type endpoint"
```

---

## Task 4: 前端 API 签名更新

**Files:**
- Modify: `graphiti-web/src/api/graph.ts`

- [ ] **Step 1: 更新 `getEpisodesVisualizationByType` 方法签名**

找到现有方法（约第 397 行），替换为：

```typescript
// V5.0: 根据剧集类型获取分页可视化数据（含N跳邻居）
async getEpisodesVisualizationByType(
  graphId: string,
  typeCode: string,
  page?: number,
  pageSize?: number,
  depth?: number
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

- [ ] **Step 2: TypeScript 编译检查**

```bash
cd graphiti-web && npx vue-tsc --noEmit
```

Expected: 无新增类型错误

- [ ] **Step 3: Commit**

```bash
git add graphiti-web/src/api/graph.ts
git commit -m "feat(web): update getEpisodesVisualizationByType API signature with pagination and depth"
```

---

## Task 5: EpisodeTypeDetailPanel.vue 分页控件改造

**Files:**
- Modify: `graphiti-web/src/components/Ontology/EpisodeTypeDetailPanel.vue`

- [ ] **Step 1: 扩展 Props**

在现有 props 定义后添加：

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

- [ ] **Step 2: 扩展 Emits**

```typescript
const emit = defineEmits<{
  (e: 'edit-type', typeId: number): void
  (e: 'delete-type', typeId: number): void
  (e: 'navigate-to-instance', uuid: string): void
  (e: 'pagination-change', page: number): void
  (e: 'depth-change', depth: number): void
}>()
```

- [ ] **Step 3: 添加计算属性（双向绑定风格）**

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

- [ ] **Step 4: 修改实例 Tab 模板，添加分页控件**

找到 `<div v-else-if="activeTab === 'instances'" class="tab-content">` 块，替换为：

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

- [ ] **Step 5: 添加分页/深度变更处理函数**

```typescript
function handlePageChange(page: number) {
  emit('pagination-change', page)
}

function handleDepthChange(depth: number) {
  emit('depth-change', depth)
}
```

- [ ] **Step 6: 修改实例加载逻辑，响应分页变化**

找到 `loadInstances` 函数，修改为使用 `props.pagination`：

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
  } catch (e) {
    console.error('加载实例列表失败:', e)
    instanceList.value = []
  } finally {
    instancesLoading.value = false
  }
}

// 监听分页变化自动刷新
watch(() => props.pagination.page, loadInstances, { immediate: true })
```

- [ ] **Step 7: 添加分页栏样式**

在 `<style>` 中添加：

```less
.instance-pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
  margin-top: 8px;
  border-top: 1px solid #21262d;
}
```

- [ ] **Step 8: TypeScript 编译检查**

```bash
cd graphiti-web && npx vue-tsc --noEmit
```

Expected: 无新增类型错误

- [ ] **Step 9: Commit**

```bash
git add graphiti-web/src/components/Ontology/EpisodeTypeDetailPanel.vue
git commit -m "feat(web): add pagination and depth controls to EpisodeTypeDetailPanel"
```

---

## Task 6: ide.vue 状态管理与事件处理改造

**Files:**
- Modify: `graphiti-web/src/views/graph/ide.vue`

- [ ] **Step 1: 新增分页和深度状态**

在 `<script setup>` 的现有 V5.0 状态区域（约第 798 行附近）添加：

```typescript
// V5.0: Episode 类型分页与深度状态
const episodePagination = ref({
  page: 1,
  pageSize: 20,
  total: 0,
  totalPages: 0,
  hasNextPage: false
})
const episodeDepth = ref(2)
```

- [ ] **Step 2: 改造 `handleEpisodeTypeSelect` 方法**

找到现有方法（约第 1295 行），替换为：

```typescript
// V5.0: 选择剧集类型 → 加载类型详情 + 可视化数据
const handleEpisodeTypeSelect = async (payload: {
  typeId: number
  typeCode: string
  typeName: string
}) => {
  // 重置分页和深度
  episodePagination.value = {
    page: 1,
    pageSize: 20,
    total: 0,
    totalPages: 0,
    hasNextPage: false
  }
  episodeDepth.value = 2

  // 清空画布
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
    console.error('加载类型数据失败:', e)
    message.error('加载类型数据失败')
  } finally {
    loading.value = false
  }
}
```

- [ ] **Step 3: 新增 `loadEpisodeTypeVisualization` 方法**

```typescript
const loadEpisodeTypeVisualization = async (typeCode: string) => {
  const res = await graphApi.getEpisodesVisualizationByType(
    effectiveGraphId.value,
    typeCode,
    episodePagination.value.page,
    episodePagination.value.pageSize,
    episodeDepth.value
  )

  // 追加模式：新数据与现有数据合并（按 uuid 去重）
  nodes.value = dedupeNodes([...nodes.value, ...(res.nodes || [])])
  edges.value = dedupeEdges([...edges.value, ...(res.edges || [])])

  // 更新分页状态
  episodePagination.value = {
    page: res.pagination?.page || episodePagination.value.page,
    pageSize: res.pagination?.pageSize || episodePagination.value.pageSize,
    total: res.pagination?.total || 0,
    totalPages: res.pagination?.totalPages || 0,
    hasNextPage: res.pagination?.hasNextPage || false
  }
}
```

- [ ] **Step 4: 新增 `loadEpisodeTypeDetail` 方法**

```typescript
const loadEpisodeTypeDetail = async (typeId: number) => {
  try {
    const detail = await episodeTypeApi.get(effectiveGraphId.value, typeId)
    selectedEpisodeType.value = detail
  } catch (e) {
    console.error('加载类型详情失败:', e)
  }
}
```

- [ ] **Step 5: 新增分页变更处理**

```typescript
const handleEpisodePaginationChange = async (newPage: number) => {
  episodePagination.value.page = newPage
  const typeCode = selectedEpisodeType.value?.typeCode
  if (!typeCode) return

  loading.value = true
  try {
    await loadEpisodeTypeVisualization(typeCode)
  } catch (e) {
    console.error('翻页加载失败:', e)
    message.error('加载失败')
  } finally {
    loading.value = false
  }
}
```

- [ ] **Step 6: 新增深度变更处理**

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
    } catch (e) {
      console.error('深度变更加载失败:', e)
      message.error('加载失败')
    } finally {
      loading.value = false
    }
  }
}
```

- [ ] **Step 7: 更新 EpisodeTypeDetailPanel 绑定**

找到 `<EpisodeTypeDetailPanel>` 组件调用（约第 248 行附近），更新为：

```vue
<EpisodeTypeDetailPanel
  :graph-id="effectiveGraphId"
  :type-id="selectedEpisodeType.id"
  :type-data="selectedEpisodeType"
  :pagination="episodePagination"
  :depth="episodeDepth"
  @edit-type="handleEpisodeTypeEdit"
  @delete-type="handleEpisodeTypeDelete"
  @navigate-to-instance="handleNavigateToInstance"
  @pagination-change="handleEpisodePaginationChange"
  @depth-change="handleEpisodeDepthChange"
/>
```

- [ ] **Step 8: TypeScript 编译检查**

```bash
cd graphiti-web && npx vue-tsc --noEmit
```

Expected: 无新增类型错误

- [ ] **Step 9: Commit**

```bash
git add graphiti-web/src/views/graph/ide.vue
git commit -m "feat(web): add episode type pagination, depth state and sync handlers in ide.vue"
```

---

## Task 7: 集成测试与验证

**Files:**
- N/A（端到端验证）

- [ ] **Step 1: 后端单元测试（可选）**

如果项目有测试框架，添加 Service 层测试：

```java
@Test
void testGetEpisodesVisualizationByType_Depth2() {
    // 准备测试数据...
    GraphVisualizationRespVO result =
        service.getEpisodesVisualizationByType("test-graph", "Trial", 1, 20, 2);
    assertNotNull(result);
    assertNotNull(result.getNodes());
    assertNotNull(result.getEdges());
    assertNotNull(result.getPagination());
}
```

- [ ] **Step 2: 启动后端服务**

```bash
cd graphiti-server && mvn spring-boot:run
```

- [ ] **Step 3: 启动前端开发服务器**

```bash
cd graphiti-web && pnpm dev
```

- [ ] **Step 4: 端到端验证清单**

打开浏览器访问 IDE 页面，按以下步骤验证：

| 步骤 | 操作 | 期望结果 |
|------|------|----------|
| 1 | 点击侧边栏"剧集"Tab | 显示 EpisodeType 树 |
| 2 | 点击某个类型节点 | 画布显示该类型节点及2跳邻居 |
| 3 | 查看右侧面板"实例列表"Tab | 显示20个实例，底部分页控件可见 |
| 4 | 点击分页"下一页" | 画布追加新节点，表格显示新页数据 |
| 5 | 切换跳数选择器为"3跳" | 画布清空，重新加载3跳数据 |
| 6 | 切换跳数选择器为"1跳" | 画布清空，重新加载1跳数据（更少节点） |
| 7 | 点击另一个类型节点 | 画布清空，加载新类型数据 |
| 8 | 检查浏览器 Network 面板 | `/visualization/episodes/by-type` 请求参数包含 typeCode/page/pageSize/depth |

- [ ] **Step 5: Commit（如有测试文件）**

```bash
git add -A
git commit -m "test: add integration tests for episode type 2-hop visualization"
```

---

## 自审检查

### Spec 覆盖率

| Spec 需求 | 对应 Task |
|-----------|-----------|
| 后端统一API（分页+动态跳数） | Task 1, 2, 3 |
| 前端API签名更新 | Task 4 |
| 右侧面板分页控件+跳数选择器 | Task 5 |
| ide.vue 状态管理与事件处理 | Task 6 |
| 追加模式去重 | Task 6 Step 3 |
| 分页同步（画布+表格） | Task 5, 6 |
| 错误处理 | Task 6（try/catch） |
| 集成测试 | Task 7 |

✅ 全部覆盖

### Placeholder 扫描

- [x] 无 "TBD"/"TODO"/"implement later"
- [x] 无模糊描述（如"添加适当错误处理"）
- [x] 每个步骤包含完整代码
- [x] 方法签名跨任务一致

### 类型一致性

- `getEpisodesVisualizationByType` 参数：后端 `(graphId, typeCode, page, pageSize, depth)` ↔ 前端 `(graphId, typeCode, page, pageSize, depth)` ✅
- `pagination` 结构前后端一致 ✅
- `depth` 范围限制：后端 `1-3`，前端选择器 `1/2/3` ✅

---

## 执行交接

**计划完成并保存至 `docs/superpowers/plans/2026-05-24-episode-type-2hop-visualization-impl.md`。**

**两种执行方式可选：**

**1. Subagent-Driven（推荐）** - 每个 Task 派发独立子代理执行，中间评审，快速迭代

**2. Inline Execution** - 在本会话中使用 executing-plans 批量执行，带检查点评审

**您希望采用哪种方式执行？**
