# addDataBatch 批量导入性能优化设计

| 字段 | 内容 |
|------|------|
| 版本 | v1.0 |
| 日期 | 2026-05-25 |
| 状态 | 待用户评审 |
| 适用范围 | graphiti-java `DataImportServiceImpl` / `GraphNeo4jService` |

---

## 1. 背景与目标

### 1.1 当前问题

`DataImportServiceImpl.addDataBatch()` 目前采用循环调用 `addData()` 的低效实现：

```java
// 当前实现 (DataImportServiceImpl.java:112-126)
for (var item : reqVO.getItems()) {
    addData(singleReq);  // 每条数据 = N次Neo4j事务
}
```

- 100 条数据 → 约 300 个独立 Neo4j 事务（每个 entity + relation 单独事务）
- LLM 调用串行
- 向量生成逐条调用 Embedding API
- EntityDedupService 未集成
- Redis 向量缓存未实现

### 1.2 优化目标

| 目标 | 指标 |
|------|------|
| 批量接口性能提升 | 100 条数据总耗时从 ~180s 降至 ~20s |
| Neo4j 事务数 | 从 300 个降至 2-3 个 UNWIND 事务 |
| LLM 调用效率 | 50 条/批拼接，20 并发控制 |
| 错误隔离 | 子批次失败不影响其他批次 |
| 向量缓存 | Redis 缓存，命中则跳过 API 调用 |
| 去重集成 | EntityDedupService 三级去重 |

---

## 2. 架构设计

### 2.1 整体数据流

```mermaid
graph TB
    subgraph API["API 层"]
        A1[POST /batch<br/>DataImportController]
        A2[返回 taskId<br/>202 Accepted]
    end

    subgraph Async["异步任务"]
        A3[BulkImportTaskService<br/>CompletableFuture]
    end

    subgraph Extract["Phase 1: LLM 批量提取"]
        E1[contentChunks<br/>50条/批拼接]
        E2[Semaphore 控制<br/>20 并发 LLM]
        E3[collect 所有实体+关系]
    end

    subgraph Dedup["Phase 2: 三级去重"]
        D1[Tier1: 精确匹配]
        D2[Tier2: MinHash+LSH]
        D3[Tier3: LLM 判定]
        D4[Union-Find 合并]
    end

    subgraph Embed["Phase 3: 向量生成"]
        B1[查 Redis 缓存]
        B2[未命中则批量<br/>embed(List)]
        B3[回填缓存]
    end

    subgraph Write["Phase 4: UNWIND 批量写入"]
        W1[neo4jChunks<br/>200条/批]
        W2[UNWIND 单事务]
        W3[失败子批次<br/>重试队列]
    end

    subgraph Result["结果"]
        R1[ImportTaskDO 持久化<br/>PostgreSQL/MySQL]
        R2[轮询 /task/{id} API]
    end

    A1 --> A2
    A2 --> A3
    A3 --> E1
    E1 --> E2
    E2 --> E3
    E3 --> D1
    D1 --> D2
    D2 --> D3
    D3 --> D4
    D4 --> B1
    B1 --> B2
    B2 --> B3
    B3 --> W1
    W1 --> W2
    W2 --> W3
    W3 --> R1
```

### 2.2 分层职责

| 层级 | 组件 | 职责 |
|------|------|------|
| API | `DataImportController` | 接收请求，返回 taskId |
| 编排 | `BulkImportTaskService` | 任务拆分、进度跟踪、结果聚合 |
| 提取 | `LlmClientService` | 带并发控制的 LLM 批量提取 |
| 去重 | `EntityDedupService` | 三级实体去重 |
| 缓存 | `EmbeddingCacheService` | Redis 向量缓存读写 |
| 写入 | `GraphNeo4jService` | UNWIND 批量 Neo4j 写入 |
| 持久化 | `ImportTaskRepository` | 任务状态存 PostgreSQL |

---

## 3. 接口设计

### 3.1 修改现有接口

#### POST /api/v1/graph/data/batch

**请求体**（新增字段）：

```json
{
  "graphId": "graph-001",
  "items": [...],
  "referenceTime": "...",
  "updateCommunities": false,
  "contentChunkSize": 50,
  "neo4jChunkSize": 200
}
```

**响应**：

```json
{
  "code": 200,
  "data": {
    "taskId": "550e8400-e29b-41d4-a716-446655440000",
    "status": "PROCESSING",
    "message": "批量导入任务已提交"
  }
}
```

### 3.2 新增接口

| 方法 | 路径 | 描述 |
|------|------|------|
| GET | `/api/v1/graph/data/task/{taskId}` | 查询任务状态 |
| GET | `/api/v1/graph/data/task/{taskId}/chunks` | 查询各子批次处理结果 |
| DELETE | `/api/v1/graph/data/task/{taskId}` | 取消运行中任务 |

---

## 4. 核心组件设计

### 4.1 BulkImportTaskService

```java
@Service
@RequiredArgsConstructor
public class BulkImportTaskService {

    @Value("${graphiti.batch.content-chunk-size:50}")
    private int contentChunkSize = 50;

    @Value("${graphiti.batch.neo4j-chunk-size:200}")
    private int neo4jChunkSize = 200;

    private final Semaphore llmSemaphore = new Semaphore(20);

    private final ExecutorService taskExecutor =
        Executors.newFixedThreadPool(10);

    public String executeAsync(AddDataBatchReqVO reqVO) {
        String taskId = UUID.randomUUID().toString();
        importTaskRepository.save(ImportTaskDO.builder()
            .taskId(taskId).graphId(reqVO.getGraphId())
            .totalItems(reqVO.getItems().size())
            .status("PROCESSING").build());

        taskExecutor.submit(() -> executeInternal(taskId, reqVO));
        return taskId;
    }

    private void executeInternal(String taskId, AddDataBatchReqVO reqVO) {
        try {
            List<String> contents = reqVO.getItems().stream()
                .map(BatchDataItemVO::getContent).toList();

            // Phase 1: LLM 批量提取
            List<ExtractedEntityVO> allEntities = new ArrayList<>();
            List<ExtractedRelationVO> allRelations = new ArrayList<>();

            List<List<String>> contentChunks = Lists.partition(contents, contentChunkSize);
            List<CompletableFuture<ChunkLLMResult>> futures = contentChunks.stream()
                .map(chunk -> CompletableFuture.supplyAsync(() ->
                    extractEntitiesAndRelations(chunk), taskExecutor), llmSemaphore)
                .toList();

            for (CompletableFuture<ChunkLLMResult> f : futures) {
                ChunkLLMResult r = f.join();
                allEntities.addAll(r.entities);
                allRelations.addAll(r.relations);
            }

            // Phase 2: 三级去重
            DedupResultVO dedup = entityDedupService.deduplicate(
                reqVO.getGraphId(),
                entitiesToMaps(allEntities),
                graphNeo4jService.getValidNodes(reqVO.getGraphId())
            );
            Map<String, String> uuidMapping = buildUuidMapping(dedup);

            // Phase 3: 批量向量生成
            List<float[]> entityEmbeddings =
                embeddingCacheService.getOrComputeBatch(getEntityEmbedTexts(allEntities));
            List<float[]> relEmbeddings =
                embeddingCacheService.getOrComputeBatch(getRelationEmbedTexts(allRelations));

            // Phase 4: UNWIND 子批次写入
            List<ChunkResult> chunkResults = processInChunks(reqVO, allEntities,
                allRelations, entityEmbeddings, relEmbeddings, uuidMapping);

            // 聚合结果
            BulkImportResult result = aggregateResults(chunkResults);
            importTaskRepository.update(taskId, result);

        } catch (Exception e) {
            importTaskRepository.updateFailed(taskId, e.getMessage());
        }
    }
}
```

### 4.2 EmbeddingCacheService

```java
@Service
@RequiredArgsConstructor
public class EmbeddingCacheService {

    private final EmbedderService embedderService;
    private final RedissonClient redissonClient;

    private static final String CACHE_PREFIX = "emb:";
    private static final long CACHE_TTL_SECONDS = 86400;

    public List<float[]> getOrComputeBatch(List<String> texts) {
        List<float[]> results = new ArrayList<>(Collections.nCopies(texts.size(), null));
        List<String> uncached = new ArrayList<>();
        List<Integer> uncachedIndices = new ArrayList<>();

        // 批量查 Redis
        for (int i = 0; i < texts.size(); i++) {
            String cacheKey = CACHE_PREFIX + md5(texts.get(i));
            try {
                String cached = redissonClient.getBucket(cacheKey).get();
                if (cached != null) {
                    results.set(i, deserialize(cached));
                } else {
                    uncached.add(texts.get(i));
                    uncachedIndices.add(i);
                }
            } catch (Exception e) {
                uncached.add(texts.get(i));
                uncachedIndices.add(i);
            }
        }

        // 未命中批量计算
        if (!uncached.isEmpty()) {
            List<float[]> computed = embedderService.embed(uncached);
            for (int i = 0; i < uncached.size(); i++) {
                int idx = uncachedIndices.get(i);
                float[] emb = computed.get(i);
                results.set(idx, emb);
                // 回填缓存
                String cacheKey = CACHE_PREFIX + md5(texts.get(idx));
                try {
                    redissonClient.getBucket(cacheKey).set(
                        serialize(emb), CACHE_TTL_SECONDS, TimeUnit.SECONDS);
                } catch (Exception e) { /* ignore */ }
            }
        }
        return results;
    }
}
```

### 4.3 GraphNeo4jService 批量写入

```java
// 新增接口方法
void batchAddNodesAndEdges(String graphId,
    List<EpisodeBatchDTO> episodes,
    List<EntityBatchDTO> entities,
    List<RelationBatchDTO> relations);
```

```java
@Override
public void batchAddNodesAndEdges(String graphId,
        List<EpisodeBatchDTO> episodes,
        List<EntityBatchDTO> entities,
        List<RelationBatchDTO> relations) {

    String cypher =
        "UNWIND $episodes AS ep " +
        "CREATE (e:Episode {graph_id: $graphId, uuid: ep.uuid, " +
        "name: ep.name, source: ep.source, source_description: ep.sourceDescription, " +
        "content: ep.content, created_at: timestamp(), valid_at: timestamp()}) " +
        "WITH count(e) as epCount " +

        "UNWIND $entities AS n " +
        "CREATE (entity:Entity {graph_id: $graphId, uuid: n.uuid, " +
        "name: n.name, type: n.type, summary: n.summary, " +
        "embedding: n.embedding, valid_at: timestamp(), invalid_at: null}) " +
        "SET entity += n.properties " +
        "SET entity." + getTypeNameField(n.getType()) + " = n.name " +
        "WITH count(entity) as entCount " +

        "UNWIND $relations AS r " +
        "MATCH (a:Entity {graph_id: $graphId, uuid: r.sourceUuid}) " +
        "MATCH (b:Entity {graph_id: $graphId, uuid: r.targetUuid}) " +
        "CREATE (a)-[rel:RELATES_TO {graph_id: $graphId, uuid: r.edgeUuid, " +
        "type: r.type, fact: r.fact, embedding: r.embedding, " +
        "valid_at: timestamp(), invalid_at: null}]->(b) " +
        "SET rel += r.properties " +
        "WITH count(rel) as relCount " +

        "RETURN epCount, entCount, relCount";

    Map<String, Object> params = Map.of(
        "graphId", graphId,
        "episodes", episodes.stream().map(EpisodeBatchDTO::toMap).toList(),
        "entities", entities.stream().map(EntityBatchDTO::toMap).toList(),
        "relations", relations.stream().map(RelationBatchDTO::toMap).toList()
    );

    try (Session session = neo4jDriver.session()) {
        session.executeWrite(tx -> tx.run(cypher, params).consume());
    }
}
```

### 4.4 ImportTaskRepository

```sql
CREATE TABLE IF NOT EXISTS graph_import_task (
    task_id           VARCHAR(64) PRIMARY KEY,
    graph_id          VARCHAR(64) NOT NULL,
    total_items       INT NOT NULL DEFAULT 0,
    processed_items   INT NOT NULL DEFAULT 0,
    failed_items      INT NOT NULL DEFAULT 0,
    entities_created  INT NOT NULL DEFAULT 0,
    relations_created INT NOT NULL DEFAULT 0,
    status            VARCHAR(32) NOT NULL DEFAULT 'PROCESSING',
    error_details     TEXT,
    duration_ms       BIGINT,
    create_time       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_graph_id (graph_id),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
);
```

---

## 5. 错误处理设计

### 5.1 重试策略

| 层级 | 错误类型 | 策略 |
|------|---------|------|
| LLM | API 超时/限流 | 指数退避重试 (1s→2s→4s)，最多 3 次 |
| Neo4j | 单批次写入失败 | 重试 2 次，仍失败跳过并记录 |
| Redis | 缓存读写失败 | 降级为直接计算，不阻塞主流程 |
| 关系 | 节点 UUID 缺失 | 跳过该关系 + warn 日志 |
| 全局 | 任务失败 | 持久化错误详情，返回 FAILED 状态 |

### 5.2 部分成功处理

```java
public record ChunkResult(
    int chunkIndex,
    int itemCount,
    boolean success,
    int entitiesCreated,
    int relationsCreated,
    String errorMessage,
    List<Integer> failedItemIndices
) {}

public BulkImportResult aggregateResults(List<ChunkResult> chunkResults) {
    int processed = 0, failed = 0, entities = 0, relations = 0;
    List<String> errors = new ArrayList<>();

    for (ChunkResult cr : chunkResults) {
        if (cr.success()) {
            processed += cr.itemCount();
            entities += cr.entitiesCreated();
            relations += cr.relationsCreated();
        } else {
            failed += cr.itemCount();
            if (cr.errorMessage() != null) {
                errors.add(String.format("chunk[%d]: %s", cr.chunkIndex(), cr.errorMessage()));
            }
        }
    }
    return new BulkImportResult(processed, failed, entities, relations, errors);
}
```

---

## 6. 配置项

| 配置键 | 默认值 | 描述 |
|--------|--------|------|
| `graphiti.batch.content-chunk-size` | 50 | LLM 内容拼接批次大小 |
| `graphiti.batch.neo4j-chunk-size` | 200 | Neo4j 事务批次大小 |
| `graphiti.batch.llm-concurrency` | 20 | LLM 最大并发数 |
| `graphiti.batch.retry.max-attempts` | 3 | 最大重试次数 |
| `graphiti.batch.cache.enabled` | true | 是否启用 Redis 缓存 |
| `graphiti.batch.cache.ttl-seconds` | 86400 | 缓存 TTL (秒) |
| `graphiti.batch.task-executor-size` | 10 | 任务执行器线程池大小 |

---

## 7. 文件变更清单

| 操作 | 文件路径 |
|------|---------|
| 修改 | `service/GraphNeo4jService.java` — 新增 `batchAddNodesAndEdges` |
| 修改 | `service/impl/GraphNeo4jServiceImpl.java` — UNWIND 批量写入实现 |
| 修改 | `service/impl/ai/OpenAiLlmClientServiceImpl.java` — 并发 `chatBatch` |
| 修改 | `service/impl/DataImportServiceImpl.java` — 重写 `addDataBatch` |
| 新增 | `service/BulkImportTaskService.java` — 任务编排服务 |
| 新增 | `service/EmbeddingCacheService.java` — Redis 向量缓存 |
| 新增 | `dto/batch/EntityBatchDTO.java` — 批量实体 DTO |
| 新增 | `dto/batch/RelationBatchDTO.java` — 批量关系 DTO |
| 新增 | `dto/batch/EpisodeBatchDTO.java` — 批量 Episode DTO |
| 新增 | `dto/batch/BulkImportResult.java` — 导入结果 DTO |
| 新增 | `dto/batch/ChunkResult.java` — 子批次结果 DTO |
| 新增 | `dto/batch/ChunkLLMResult.java` — LLM 提取结果 DTO |
| 新增 | `entity/ImportTaskDO.java` — 任务持久化实体 |
| 新增 | `repository/ImportTaskRepository.java` — 任务 Repository |
| 新增 | `controller/admin/ImportTaskController.java` — 任务状态 API |
| 修改 | `controller/admin/DataImportController.java` — 异步返回 taskId |
| 新增 | `resources/db/migration/V*__add_graph_import_task.sql` — 任务表建表脚本 |
| 修改 | `vo/imports/AddDataBatchReqVO.java` — 新增 `contentChunkSize`, `neo4jChunkSize` 字段 |

---

## 8. 性能预期

### 8.1 100 条数据导入基准测试

| 阶段 | 优化前耗时 | 优化后耗时 | 提升 |
|------|-----------|-----------|------|
| LLM 提取 (200次串行) | 100s | 2.5s (4次并发×50条) | 40x |
| 向量生成 (300次HTTP) | 30s | 5s (2-5次批量) | 6x |
| Neo4j 写入 (300事务) | 120s | 3s (2个UNWIND) | 40x |
| 去重处理 | 0s (未启用) | 1s | 新增 |
| **总计** | **~180s** | **~12s** | **15x** |

### 8.2 扩展性预测

| 数据量 | 预估耗时 | 瓶颈 |
|--------|---------|------|
| 50 条 | ~6s | LLM 并发控制 |
| 100 条 | ~12s | LLM 并发控制 |
| 500 条 | ~55s | LLM API 限流 |
| 1000 条 | ~110s | LLM API 限流 + Neo4j 锁竞争 |

---

## 9. 风险与缓解

| 风险 | 概率 | 影响 | 缓解措施 |
|------|------|------|---------|
| LLM API 限流 | 高 | 中 | 信号量 20 并发 + 指数退避重试 |
| UNWIND 大事务锁超时 | 中 | 高 | 子批次 200 条 + 事务超时 30s |
| Redis 不可用 | 低 | 低 | 降级为直接计算 |
| 重复内容导致向量缓存污染 | 低 | 低 | MD5 规范化文本 |

---

## 10. 实现顺序

| 阶段 | 任务 | 工作量 | 依赖 |
|------|------|--------|------|
| **Phase 1** | 新增 DTO (EntityBatchDTO, RelationBatchDTO, EpisodeBatchDTO, BulkImportResult, ChunkResult, ChunkLLMResult) | 0.5 人天 | 无 |
| **Phase 1** | `GraphNeo4jService.batchAddNodesAndEdges()` UNWIND 实现 | 1 人天 | Phase 1 DTO |
| **Phase 1** | `DataImportServiceImpl` 重写 `addDataBatch` (基础版，无并发) | 1 人天 | Phase 1 UNWIND |
| **Phase 2** | `OpenAiLlmClientServiceImpl` 并发 `chatBatch` + `LlmClientService` 接口扩展 | 0.5 人天 | 无 |
| **Phase 2** | `BulkImportTaskService` 任务编排 + 子批次拆分 | 2 人天 | Phase 1 基础版 |
| **Phase 3** | `EmbeddingCacheService` Redis 缓存 | 1 人天 | Redis 连接就绪 |
| **Phase 3** | `ImportTaskRepository` + `ImportTaskController` | 1 人天 | 无 |
| **Phase 3** | 集成 `EntityDedupService` 去重 | 1 人天 | `EntityDedupService` 已实现 |
| **Phase 4** | 错误处理 + 重试策略 + 单元测试 | 2 人天 | Phase 1-3 |
| **Phase 4** | 集成测试 + 性能基准测试 | 2 人天 | Neo4j 连接 |

**预计总工时**: ~12 人天 (4 周单人)

---

*本文档由 Claude Code 生成，基于 addData_pipeline_analysis_v2.md 技术分析，适用于 graphiti-java 批量导入优化实现。*
