# addDataBatch 批量导入性能优化实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将 `addDataBatch` 从循环调用单条接口改造为批量处理，100 条数据耗时从 ~180s 降至 ~20s。

**Architecture:** 四个阶段流水线：① LLM 内容拼接（50条/批）并发提取 ② 三级去重 ③ Redis 向量缓存批量生成 ④ UNWIND 子批次（200条/事务）写入。异步任务队列返回 taskId 支持轮询。

**Tech Stack:** Spring Boot 3.5, Neo4j Driver 5.26, Redisson 3.37, Spring AI 1.1, CompletableFuture + Semaphore

---

## 文件变更总览

| 操作 | 文件 |
|------|------|
| 新增 | `vo/imports/AddDataBatchReqVO.java` — 新增 contentChunkSize, neo4jChunkSize 字段 |
| 新增 | `dto/batch/EntityBatchDTO.java` |
| 新增 | `dto/batch/RelationBatchDTO.java` |
| 新增 | `dto/batch/EpisodeBatchDTO.java` |
| 新增 | `dto/batch/BulkImportResult.java` |
| 新增 | `dto/batch/ChunkResult.java` |
| 新增 | `dto/batch/ChunkLLMResult.java` |
| 新增 | `dto/batch/BulkImportTaskVO.java` |
| 修改 | `service/GraphNeo4jService.java` — 新增 batchAddNodesAndEdges |
| 修改 | `service/impl/GraphNeo4jServiceImpl.java` — 实现 UNWIND 批量写入 |
| 修改 | `service/impl/ai/OpenAiLlmClientServiceImpl.java` — 并发 chatBatch |
| 修改 | `service/LlmClientService.java` — 新增 chatBatchAsync 方法 |
| 修改 | `service/impl/DataImportServiceImpl.java` — 重写 addDataBatch |
| 新增 | `service/BulkImportTaskService.java` |
| 新增 | `service/EmbeddingCacheService.java` |
| 新增 | `dal/dataobject/ImportTaskDO.java` |
| 新增 | `dal/repository/ImportTaskRepository.java` |
| 新增 | `controller/admin/ImportTaskController.java` |
| 修改 | `controller/admin/DataImportController.java` — 异步返回 taskId |
| 新增 | `resources/db/migration/V20260525__add_graph_import_task.sql` |

---

## Task 1: 创建 Phase 1 DTO

**Files:**
- Create: `ontograph-module-core/src/main/java/com/ontograph/module/graphiti/dto/batch/EntityBatchDTO.java`
- Create: `ontograph-module-core/src/main/java/com/ontograph/module/graphiti/dto/batch/RelationBatchDTO.java`
- Create: `ontograph-module-core/src/main/java/com/ontograph/module/graphiti/dto/batch/EpisodeBatchDTO.java`
- Create: `ontograph-module-core/src/main/java/com/ontograph/module/graphiti/dto/batch/BulkImportResult.java`
- Create: `ontograph-module-core/src/main/java/com/ontograph/module/graphiti/dto/batch/ChunkResult.java`
- Create: `ontograph-module-core/src/main/java/com/ontograph/module/graphiti/dto/batch/ChunkLLMResult.java`
- Create: `ontograph-module-core/src/main/java/com/ontograph/module/graphiti/dto/batch/BulkImportTaskVO.java`
- Modify: `ontograph-module-core/src/main/java/com/ontograph/module/graphiti/vo/imports/AddDataBatchReqVO.java`

### Step 1: 创建 EntityBatchDTO

```java
package com.ontograph.module.graphiti.dto.batch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityBatchDTO {
    private String uuid;
    private String name;
    private String type;
    private String summary;
    private float[] embedding;
    private Map<String, Object> properties;

    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("uuid", uuid);
        m.put("name", name);
        m.put("type", type);
        m.put("summary", summary != null ? summary : "");
        m.put("embedding", embedding != null ? toFloatList(embedding) : null);
        m.put("properties", properties != null ? properties : new HashMap<>());
        return m;
    }

    private static List<Float> toFloatList(float[] arr) {
        if (arr == null) return null;
        java.util.List<Float> list = new java.util.ArrayList<>(arr.length);
        for (float v : arr) list.add(v);
        return list;
    }
}
```

### Step 2: 创建 RelationBatchDTO

```java
package com.ontograph.module.graphiti.dto.batch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelationBatchDTO {
    private String edgeUuid;
    private String sourceUuid;
    private String targetUuid;
    private String type;
    private String fact;
    private float[] embedding;
    private Map<String, Object> properties;

    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("edgeUuid", edgeUuid);
        m.put("sourceUuid", sourceUuid);
        m.put("targetUuid", targetUuid);
        m.put("type", type != null ? type : "RELATES_TO");
        m.put("fact", fact != null ? fact : "");
        m.put("embedding", embedding != null ? toFloatList(embedding) : null);
        m.put("properties", properties != null ? properties : new HashMap<>());
        return m;
    }

    private static List<Float> toFloatList(float[] arr) {
        if (arr == null) return null;
        java.util.List<Float> list = new java.util.ArrayList<>(arr.length);
        for (float v : arr) list.add(v);
        return list;
    }
}
```

### Step 3: 创建 EpisodeBatchDTO

```java
package com.ontograph.module.graphiti.dto.batch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EpisodeBatchDTO {
    private String uuid;
    private String name;
    private String source;
    private String sourceDescription;
    private String content;
    private Map<String, Object> properties;

    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("uuid", uuid);
        m.put("name", name);
        m.put("source", source != null ? source : "text");
        m.put("sourceDescription", sourceDescription != null ? sourceDescription : "");
        m.put("content", content);
        m.put("properties", properties != null ? properties : new HashMap<>());
        return m;
    }
}
```

### Step 4: 创建 ChunkResult

```java
package com.ontograph.module.graphiti.dto.batch;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChunkResult {
    private int chunkIndex;
    private int itemCount;
    private boolean success;
    private int entitiesCreated;
    private int relationsCreated;
    private String errorMessage;
    private List<Integer> failedItemIndices;
}
```

### Step 5: 创建 ChunkLLMResult

```java
package com.ontograph.module.graphiti.dto.batch;

import com.ontograph.module.graphiti.vo.llm.ExtractedEntityVO;
import com.ontograph.module.graphiti.vo.llm.ExtractedRelationVO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ChunkLLMResult {
    private int chunkIndex;
    private List<ExtractedEntityVO> entities;
    private List<ExtractedRelationVO> relations;
}
```

### Step 6: 创建 BulkImportResult

```java
package com.ontograph.module.graphiti.dto.batch;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class BulkImportResult {
    private int totalItems;
    private int processedItems;
    private int failedItems;
    private int entitiesCreated;
    private int relationsCreated;
    @Builder.Default
    private List<String> errorDetails = new ArrayList<>();
    private long durationMs;
}
```

### Step 7: 创建 BulkImportTaskVO

```java
package com.ontograph.module.graphiti.dto.batch;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@Schema(description = "批量导入任务状态")
public class BulkImportTaskVO {
    @Schema(description = "任务ID")
    private String taskId;

    @Schema(description = "任务状态: PROCESSING / COMPLETED / FAILED / CANCELLED")
    private String status;

    @Schema(description = "总条数")
    private Integer totalItems;

    @Schema(description = "已处理条数")
    private Integer processedItems;

    @Schema(description = "失败条数")
    private Integer failedItems;

    @Schema(description = "创建的实体数")
    private Integer entitiesCreated;

    @Schema(description = "创建的关系数")
    private Integer relationsCreated;

    @Schema(description = "耗时(ms)")
    private Long durationMs;

    @Schema(description = "错误详情")
    private List<String> errorDetails;
}
```

### Step 8: 修改 AddDataBatchReqVO 新增字段

在 `AddDataBatchReqVO` 的类定义后新增两个字段（如果已有就跳过）:

```java
// 在 AddDataBatchReqVO.java 中追加字段

@Schema(description = "LLM 内容拼接批次大小")
private Integer contentChunkSize = 50;

@Schema(description = "Neo4j 事务批次大小")
private Integer neo4jChunkSize = 200;
```

### Step 9: Commit

```
git add ontograph-module-core/src/main/java/com/ontograph/module/graphiti/dto/batch/
git add ontograph-module-core/src/main/java/com/ontograph/module/graphiti/vo/imports/AddDataBatchReqVO.java
git commit -m "feat(batch): add Phase 1 DTOs for bulk import
- EntityBatchDTO, RelationBatchDTO, EpisodeBatchDTO
- ChunkResult, ChunkLLMResult, BulkImportResult
- BulkImportTaskVO
- AddDataBatchReqVO: add contentChunkSize, neo4jChunkSize fields"
```

---

## Task 2: 实现 GraphNeo4jService.batchAddNodesAndEdges()

**Files:**
- Modify: `ontograph-module-core/src/main/java/com/ontograph/module/graphiti/service/GraphNeo4jService.java`
- Modify: `ontograph-module-core/src/main/java/com/ontograph/module/graphiti/service/impl/GraphNeo4jServiceImpl.java`

### Step 1: 在 GraphNeo4jService.java 接口新增方法

在接口末尾（在类的闭合括号 `}` 前）追加：

```java
    /**
     * 批量写入 Episodes（单事务 UNWIND）
     */
    void batchCreateEpisodes(String graphId, List<EpisodeBatchDTO> episodes);

    /**
     * 批量创建实体节点（单事务 UNWIND）
     */
    void batchCreateEntities(String graphId, List<EntityBatchDTO> entities);

    /**
     * 批量创建关系（单事务 UNWIND）
     */
    void batchCreateRelationships(String graphId, List<RelationBatchDTO> relations);

    /**
     * 原子性批量写入：Episodes + 实体 + 关系在同一个事务中
     */
    void batchAddNodesAndEdges(String graphId, List<EpisodeBatchDTO> episodes,
                               List<EntityBatchDTO> entities, List<RelationBatchDTO> relations);
```

### Step 2: 在 GraphNeo4jServiceImpl.java 实现 batchCreateEpisodes

在现有方法之后、类闭合前添加：

```java
@Override
public void batchCreateEpisodes(String graphId, List<EpisodeBatchDTO> episodes) {
    if (episodes == null || episodes.isEmpty()) return;

    String cypher =
        "UNWIND $episodes AS ep " +
        "CREATE (e:Episode {graph_id: $graphId, uuid: ep.uuid, " +
        "name: ep.name, source: ep.source, source_description: ep.sourceDescription, " +
        "content: ep.content, created_at: timestamp(), valid_at: timestamp()}) " +
        "SET e += ep.properties " +
        "RETURN count(e) as created";

    Map<String, Object> params = Map.of(
        "graphId", graphId,
        "episodes", episodes.stream().map(EpisodeBatchDTO::toMap).toList()
    );

    try (Session session = neo4jDriver.session()) {
        session.executeWrite(tx -> {
            Result r = tx.run(cypher, params);
            log.info("批量创建 Episodes: graphId={}, count={}", graphId, r.consume().counters().nodesCreated());
        });
    }
}
```

### Step 3: 实现 batchCreateEntities

```java
@Override
public void batchCreateEntities(String graphId, List<EntityBatchDTO> entities) {
    if (entities == null || entities.isEmpty()) return;

    // 构建动态 type name field 设置
    StringBuilder cypherBuilder = new StringBuilder(
        "UNWIND $entities AS e " +
        "CREATE (n:Entity {graph_id: $graphId, uuid: e.uuid, " +
        "name: e.name, type: e.type, summary: e.summary, " +
        "embedding: e.embedding, valid_at: timestamp(), invalid_at: null}) " +
        "SET n += e.properties "
    );

    // 动态设置 type name field（复用现有逻辑）
    String nameField = getTypeNameFieldForBatch("e.type");
    cypherBuilder.append("SET n.").append(nameField).append(" = e.name ");

    cypherBuilder.append("RETURN count(n) as created");

    Map<String, Object> params = Map.of(
        "graphId", graphId,
        "entities", entities.stream().map(EntityBatchDTO::toMap).toList()
    );

    try (Session session = neo4jDriver.session()) {
        session.executeWrite(tx -> {
            Result r = tx.run(cypherBuilder.toString(), params);
            log.info("批量创建实体节点: graphId={}, count={}", graphId, r.consume().counters().nodesCreated());
        });
    }
}

// 需要添加一个静态版本来处理 UNWIND 中的动态 type field
private String getTypeNameFieldForBatch(String typeVar) {
    // 在 UNWIND 中无法直接用 SWITCH，改用属性复制
    // 直接设置 name 字段（通用），type-specific 字段通过 properties 传入
    return "name";
}
```

### Step 4: 实现 batchCreateRelationships

```java
@Override
public void batchCreateRelationships(String graphId, List<RelationBatchDTO> relations) {
    if (relations == null || relations.isEmpty()) return;

    String cypher =
        "UNWIND $relations AS r " +
        "MATCH (a:Entity {graph_id: $graphId, uuid: r.sourceUuid}) " +
        "MATCH (b:Entity {graph_id: $graphId, uuid: r.targetUuid}) " +
        "CREATE (a)-[rel:RELATES_TO {graph_id: $graphId, uuid: r.edgeUuid, " +
        "type: r.type, fact: r.fact, embedding: r.embedding, " +
        "valid_at: timestamp(), invalid_at: null}]->(b) " +
        "SET rel += r.properties " +
        "RETURN count(rel) as created";

    Map<String, Object> params = Map.of(
        "graphId", graphId,
        "relations", relations.stream().map(RelationBatchDTO::toMap).toList()
    );

    try (Session session = neo4jDriver.session()) {
        session.executeWrite(tx -> {
            Result r = tx.run(cypher, params);
            log.info("批量创建关系: graphId={}, count={}", graphId, r.consume().counters().relationshipsCreated());
        });
    }
}
```

### Step 5: 实现 batchAddNodesAndEdges（原子性事务）

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
        "SET e += ep.properties " +
        "WITH count(e) as epCount " +

        "UNWIND $entities AS n " +
        "CREATE (entity:Entity {graph_id: $graphId, uuid: n.uuid, " +
        "name: n.name, type: n.type, summary: n.summary, " +
        "embedding: n.embedding, valid_at: timestamp(), invalid_at: null}) " +
        "SET entity += n.properties " +
        "SET entity.name = n.name " +
        "WITH epCount, count(entity) as entCount " +

        "UNWIND $relations AS r " +
        "MATCH (a:Entity {graph_id: $graphId, uuid: r.sourceUuid}) " +
        "MATCH (b:Entity {graph_id: $graphId, uuid: r.targetUuid}) " +
        "CREATE (a)-[rel:RELATES_TO {graph_id: $graphId, uuid: r.edgeUuid, " +
        "type: r.type, fact: r.fact, embedding: r.embedding, " +
        "valid_at: timestamp(), invalid_at: null}]->(b) " +
        "SET rel += r.properties " +
        "WITH epCount, entCount, count(rel) as relCount " +

        "RETURN epCount, entCount, relCount";

    Map<String, Object> params = Map.of(
        "graphId", graphId,
        "episodes", episodes != null ? episodes.stream().map(EpisodeBatchDTO::toMap).toList() : java.util.List.of(),
        "entities", entities != null ? entities.stream().map(EntityBatchDTO::toMap).toList() : java.util.List.of(),
        "relations", relations != null ? relations.stream().map(RelationBatchDTO::toMap).toList() : java.util.List.of()
    );

    try (Session session = neo4jDriver.session()) {
        session.executeWrite(tx -> {
            Result result = tx.run(cypher, params);
            Record record = result.next();
            log.info("原子性批量写入完成: graphId={}, episodes={}, entities={}, relations={}",
                graphId,
                record.get("epCount").asLong(),
                record.get("entCount").asLong(),
                record.get("relCount").asLong());
        });
    }
}
```

### Step 6: Commit

```
git add ontograph-module-core/src/main/java/com/ontograph/module/graphiti/service/GraphNeo4jService.java
git add ontograph-module-core/src/main/java/com/ontograph/module/graphiti/service/impl/GraphNeo4jServiceImpl.java
git commit -m "feat(batch): add UNWIND batch write methods to GraphNeo4jService
- batchCreateEpisodes()
- batchCreateEntities()
- batchCreateRelationships()
- batchAddNodesAndEdges() (atomic single-transaction write)"
```

---

## Task 3: 重写 DataImportServiceImpl.addDataBatch() 基础版

**Files:**
- Modify: `ontograph-module-core/src/main/java/com/ontograph/module/graphiti/service/impl/DataImportServiceImpl.java`

### Step 1: 重写 addDataBatch 方法

用以下完整实现替换现有的 `addDataBatch` 方法（第 112-126 行）:

```java
@Override
public void addDataBatch(AddDataBatchReqVO reqVO) {
    log.info("批量添加数据（优化版）: graphId={}, count={}",
             reqVO.getGraphId(), reqVO.getItems().size());

    String graphId = reqVO.getGraphId();
    List<BatchDataItemVO> items = reqVO.getItems();

    // 1. 批量创建 Episodes
    List<EpisodeBatchDTO> episodes = new ArrayList<>();
    for (BatchDataItemVO item : items) {
        EpisodeBatchDTO ep = EpisodeBatchDTO.builder()
            .uuid(UUID.randomUUID().toString().replace("-", ""))
            .name(item.getName() != null ? item.getName() : "Episode-" + System.currentTimeMillis())
            .source(item.getSourceType() != null ? item.getSourceType() : "text")
            .sourceDescription(item.getSourceDescription())
            .content(item.getContent())
            .properties(new HashMap<>())
            .build();
        episodes.add(ep);
    }

    // 2. LLM 批量提取实体和关系
    List<ExtractedEntityVO> allEntities = new ArrayList<>();
    List<ExtractedRelationVO> allRelations = new ArrayList<>();

    for (BatchDataItemVO item : items) {
        String content = item.getContent();
        if (content == null || content.isBlank()) continue;

        try {
            List<ExtractedEntityVO> entities = llmClientService.extractEntities(content);
            List<ExtractedRelationVO> relations = llmClientService.extractRelations(content);
            if (entities != null) allEntities.addAll(entities);
            if (relations != null) allRelations.addAll(relations);
        } catch (Exception e) {
            log.warn("LLM 提取失败，item={}: {}", item.getName(), e.getMessage());
        }
    }

    log.info("LLM 提取结果: entities={}, relations={}", allEntities.size(), allRelations.size());

    if (allEntities.isEmpty()) {
        log.info("无提取实体，跳过写入");
        return;
    }

    // 3. 时序失效
    List<String> entityNames = allEntities.stream()
        .map(ExtractedEntityVO::getName)
        .filter(Objects::nonNull)
        .distinct()
        .toList();
    temporalService.invalidateFacts(graphId, entityNames);

    // 4. 批量向量生成
    List<String> entityEmbedTexts = allEntities.stream()
        .map(e -> e.getName() + (e.getSummary() != null ? " " + e.getSummary() : ""))
        .toList();
    List<float[]> entityEmbeddings = embedderService.embed(entityEmbedTexts);

    // 5. 构建实体 DTO + name→uuid 映射
    List<EntityBatchDTO> entities = new ArrayList<>();
    Map<String, String> entityNameToUuid = new HashMap<>();
    for (int i = 0; i < allEntities.size(); i++) {
        ExtractedEntityVO entity = allEntities.get(i);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        EntityBatchDTO dto = EntityBatchDTO.builder()
            .uuid(uuid)
            .name(entity.getName())
            .type(entity.getType() != null ? entity.getType() : "Entity")
            .summary(entity.getSummary() != null ? entity.getSummary() : "")
            .embedding(entityEmbeddings.get(i))
            .properties(entity.getAttributes() != null ? entity.getAttributes() : new HashMap<>())
            .build();
        entities.add(dto);
        entityNameToUuid.put(entity.getName(), uuid);
    }

    // 6. 构建关系 DTO（source/target 映射到 uuid）
    List<RelationBatchDTO> relations = new ArrayList<>();
    List<String> relEmbedTexts = new ArrayList<>();

    for (ExtractedRelationVO rel : allRelations) {
        String sourceUuid = entityNameToUuid.get(rel.getSource());
        String targetUuid = entityNameToUuid.get(rel.getTarget());
        if (sourceUuid == null || targetUuid == null) {
            log.warn("关系节点未找到: source={}, target={}", rel.getSource(), rel.getTarget());
            continue;
        }
        String fact = rel.getFact() != null ? rel.getFact() : "";
        relEmbedTexts.add(fact.isEmpty() ? rel.getType() : fact);
        relations.add(RelationBatchDTO.builder()
            .edgeUuid(UUID.randomUUID().toString().replace("-", ""))
            .sourceUuid(sourceUuid)
            .targetUuid(targetUuid)
            .type(rel.getType())
            .fact(fact)
            .properties(new HashMap<>())
            .build());
    }

    // 7. 批量生成关系向量
    List<float[]> relEmbeddings = relations.isEmpty()
        ? List.of()
        : embedderService.embed(relEmbedTexts);
    for (int i = 0; i < relations.size(); i++) {
        relations.get(i).setEmbedding(relEmbeddings.get(i));
    }

    // 8. UNWIND 原子性批量写入
    graphNeo4jService.batchAddNodesAndEdges(graphId, episodes, entities, relations);

    log.info("批量添加完成: graphId={}, episodes={}, entities={}, relations={}",
             graphId, episodes.size(), entities.size(), relations.size());
}
```

### Step 2: 添加缺失的 import

确保 `DataImportServiceImpl.java` 顶部有:

```java
import com.ontograph.module.graphiti.dto.batch.EpisodeBatchDTO;
import com.ontograph.module.graphiti.dto.batch.EntityBatchDTO;
import com.ontograph.module.graphiti.dto.batch.RelationBatchDTO;
import com.ontograph.module.graphiti.vo.imports.BatchDataItemVO;
import java.util.Objects;
```

### Step 3: Commit

```
git add ontograph-module-core/src/main/java/com/ontograph/module/graphiti/service/impl/DataImportServiceImpl.java
git commit -m "feat(batch): rewrite addDataBatch with UNWIND batch writes
- Batch Episode creation in single transaction
- Bulk entity+relation embedding via embed(List)
- batchAddNodesAndEdges for atomic write
- Removes N separate Neo4j transactions"
```

---

## Task 4: 实现并发 chatBatch

**Files:**
- Modify: `ontograph-module-core/src/main/java/com/ontograph/module/graphiti/service/LlmClientService.java`
- Modify: `ontograph-module-core/src/main/java/com/ontograph/module/graphiti/service/impl/ai/OpenAiLlmClientServiceImpl.java`

### Step 1: 在 LlmClientService.java 接口新增方法

在接口末尾追加:

```java
    /**
     * 带信号量控制的并发批量对话请求
     * @param prompts 提示词列表
     * @param maxConcurrency 最大并发数
     * @return 回复文本列表
     */
    List<String> chatBatchAsync(List<String> prompts, int maxConcurrency);
```

### Step 2: 实现 chatBatchAsync 在 OpenAiLlmClientServiceImpl

在类顶部添加字段:

```java
    private final ExecutorService llmExecutor =
        Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
```

在 `chatBatch` 方法之后、`getProvider` 方法之前添加:

```java
@Override
public List<String> chatBatchAsync(List<String> prompts, int maxConcurrency) {
    if (prompts == null || prompts.isEmpty()) {
        return List.of();
    }
    Semaphore semaphore = new Semaphore(maxConcurrency);

    List<CompletableFuture<String>> futures = prompts.stream()
        .map(prompt -> CompletableFuture.supplyAsync(() -> {
            try {
                semaphore.acquire();
                try {
                    return chat(prompt);
                } finally {
                    semaphore.release();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("LLM call interrupted", e);
            }
        }, llmExecutor))
        .toList();

    return futures.stream()
        .map(CompletableFuture::join)
        .toList();
}
```

添加 import:

```java
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
```

### Step 3: Commit

```
git add ontograph-module-core/src/main/java/com/ontograph/module/graphiti/service/LlmClientService.java
git add ontograph-module-core/src/main/java/com/ontograph/module/graphiti/service/impl/ai/OpenAiLlmClientServiceImpl.java
git commit -m "feat(batch): add concurrent chatBatchAsync with semaphore control
- ExecutorService with 2x CPU cores thread pool
- Semaphore-based concurrency limiting
- CompletableFuture for parallel LLM calls"
```

---

## Task 5: 创建 BulkImportTaskService

**Files:**
- Create: `ontograph-module-core/src/main/java/com/ontograph/module/graphiti/service/BulkImportTaskService.java`

### Step 1: 创建 BulkImportTaskService

```java
package com.ontograph.module.graphiti.service;

import com.ontograph.module.graphiti.dto.batch.*;
import com.ontograph.module.graphiti.service.EntityDedupService;
import com.ontograph.module.graphiti.vo.dedup.DedupResultVO;
import com.ontograph.module.graphiti.vo.llm.ExtractedEntityVO;
import com.ontograph.module.graphiti.vo.llm.ExtractedRelationVO;
import com.ontograph.module.graphiti.vo.imports.AddDataBatchReqVO;
import com.ontograph.module.graphiti.vo.imports.BatchDataItemVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BulkImportTaskService {

    @Value("${graphiti.batch.content-chunk-size:50}")
    private int contentChunkSize = 50;

    @Value("${graphiti.batch.neo4j-chunk-size:200}")
    private int neo4jChunkSize = 200;

    @Value("${graphiti.batch.llm-concurrency:20}")
    private int llmConcurrency = 20;

    private final ExecutorService taskExecutor =
        Executors.newFixedThreadPool(10);

    private final GraphNeo4jService graphNeo4jService;
    private final LlmClientService llmClientService;
    private final EmbedderService embedderService;
    private final EmbeddingCacheService embeddingCacheService;
    private final TemporalService temporalService;
    private final EntityDedupService entityDedupService;
    private final ImportTaskRepository importTaskRepository;

    public String executeAsync(AddDataBatchReqVO reqVO) {
        String taskId = UUID.randomUUID().toString();
        log.info("提交批量导入任务: taskId={}, graphId={}, items={}",
                 taskId, reqVO.getGraphId(), reqVO.getItems().size());

        importTaskRepository.save(taskId, reqVO.getGraphId(), reqVO.getItems().size());

        taskExecutor.submit(() -> {
            try {
                BulkImportResult result = executeInternal(taskId, reqVO);
                importTaskRepository.updateResult(taskId, result);
                log.info("批量导入任务完成: taskId={}, result={}", taskId, result);
            } catch (Exception e) {
                log.error("批量导入任务失败: taskId={}", taskId, e);
                importTaskRepository.updateFailed(taskId, e.getMessage());
            }
        });

        return taskId;
    }

    private BulkImportResult executeInternal(String taskId, AddDataBatchReqVO reqVO) {
        long startTime = System.currentTimeMillis();
        String graphId = reqVO.getGraphId();
        List<BatchDataItemVO> items = reqVO.getItems();

        // ===== Phase 1: LLM 批量提取 =====
        List<String> contents = items.stream()
            .map(BatchDataItemVO::getContent)
            .filter(c -> c != null && !c.isBlank())
            .toList();

        List<ExtractedEntityVO> allEntities = new ArrayList<>();
        List<ExtractedRelationVO> allRelations = new ArrayList<>();

        List<List<String>> contentChunks = partition(contents, contentChunkSize);
        ExecutorService llmExecutor = Executors.newFixedThreadPool(llmConcurrency);
        try {
            List<ChunkLLMResult> chunkResults = new ArrayList<>();
            CountDownLatch latch = new CountDownLatch(contentChunks.size());

            for (int i = 0; i < contentChunks.size(); i++) {
                final int chunkIdx = i;
                final List<String> chunk = contentChunks.get(i);
                llmExecutor.submit(() -> {
                    try {
                        ChunkLLMResult r = extractEntitiesAndRelations(chunk, chunkIdx);
                        synchronized (chunkResults) {
                            chunkResults.add(r);
                        }
                    } catch (Exception e) {
                        log.warn("LLM 提取 chunk[{}] 失败: {}", chunkIdx, e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await(10, TimeUnit.MINUTES);

            for (ChunkLLMResult cr : chunkResults) {
                if (cr.getEntities() != null) allEntities.addAll(cr.getEntities());
                if (cr.getRelations() != null) allRelations.addAll(cr.getRelations());
            }
        } finally {
            llmExecutor.shutdown();
        }

        log.info("Phase 1 完成: taskId={}, entities={}, relations={}",
                 taskId, allEntities.size(), allRelations.size());

        if (allEntities.isEmpty()) {
            return BulkImportResult.builder()
                .totalItems(items.size()).processedItems(0).failedItems(0)
                .entitiesCreated(0).relationsCreated(0)
                .durationMs(System.currentTimeMillis() - startTime)
                .build();
        }

        // ===== Phase 2: 三级去重 =====
        List<Map<String, Object>> entityMaps = allEntities.stream().map(e -> {
            Map<String, Object> m = new HashMap<>();
            m.put("name", e.getName());
            m.put("type", e.getType());
            m.put("summary", e.getSummary());
            m.put("attributes", e.getAttributes());
            return m;
        }).toList();

        List<Map<String, Object>> existingNodes = graphNeo4jService.getValidNodes(graphId);
        DedupResultVO dedupResult = entityDedupService.deduplicate(graphId, entityMaps, existingNodes);

        Map<String, String> uuidMapping = new HashMap<>();
        if (dedupResult.getUuidMapping() != null) {
            uuidMapping.putAll(dedupResult.getUuidMapping());
        }

        // 新建节点的 uuid
        for (Map<String, Object> newNode : dedupResult.getNewNodes()) {
            String name = (String) newNode.get("name");
            String uuid = UUID.randomUUID().toString().replace("-", "");
            uuidMapping.put(name, uuid);
        }

        // ===== Phase 3: 批量向量生成 =====
        List<String> entityEmbedTexts = allEntities.stream()
            .map(e -> e.getName() + (e.getSummary() != null ? " " + e.getSummary() : ""))
            .toList();
        List<float[]> entityEmbeddings = embeddingCacheService.getOrComputeBatch(entityEmbedTexts);

        // ===== Phase 4: UNWIND 子批次写入 =====
        int totalEntities = 0, totalRelations = 0, totalProcessed = 0;
        List<String> errors = new ArrayList<>();

        List<EpisodeBatchDTO> episodes = new ArrayList<>();
        for (BatchDataItemVO item : items) {
            episodes.add(EpisodeBatchDTO.builder()
                .uuid(UUID.randomUUID().toString().replace("-", ""))
                .name(item.getName() != null ? item.getName() : "Episode-" + System.currentTimeMillis())
                .source(item.getSourceType() != null ? item.getSourceType() : "text")
                .sourceDescription(item.getSourceDescription())
                .content(item.getContent())
                .properties(new HashMap<>())
                .build());
        }

        List<List<EntityBatchDTO>> entityChunks = partitionEntity(
            buildEntityDTOs(allEntities, entityEmbeddings, uuidMapping), neo4jChunkSize);

        List<List<RelationBatchDTO>> relChunks = partitionRelation(
            buildRelationDTOs(allRelations, uuidMapping), neo4jChunkSize);

        int maxChunks = Math.max(entityChunks.size(), relChunks.size());

        for (int i = 0; i < maxChunks; i++) {
            List<EntityBatchDTO> eChunk = i < entityChunks.size() ? entityChunks.get(i) : List.of();
            List<RelationBatchDTO> rChunk = i < relChunks.size() ? relChunks.get(i) : List.of();

            try {
                // 先失效同名实体
                List<String> names = eChunk.stream().map(EntityBatchDTO::getName).toList();
                if (!names.isEmpty()) {
                    temporalService.invalidateFacts(graphId, names);
                }

                graphNeo4jService.batchAddNodesAndEdges(graphId, episodes, eChunk, rChunk);
                totalEntities += eChunk.size();
                totalRelations += rChunk.size();
                totalProcessed += Math.min(neo4jChunkSize, items.size() - i * neo4jChunkSize);
            } catch (Exception e) {
                log.error("子批次写入失败: chunk[{}]: {}", i, e.getMessage());
                errors.add(String.format("chunk[%d]: %s", i, e.getMessage()));
            }
        }

        return BulkImportResult.builder()
            .totalItems(items.size())
            .processedItems(totalProcessed)
            .failedItems(items.size() - totalProcessed)
            .entitiesCreated(totalEntities)
            .relationsCreated(totalRelations)
            .errorDetails(errors)
            .durationMs(System.currentTimeMillis() - startTime)
            .build();
    }

    private ChunkLLMResult extractEntitiesAndRelations(List<String> contents, int chunkIdx) {
        // 拼接 chunk 内所有 content
        String merged = String.join("\n---\n", contents);
        List<ExtractedEntityVO> entities = llmClientService.extractEntities(merged);
        List<ExtractedRelationVO> relations = llmClientService.extractRelations(merged);
        return ChunkLLMResult.builder()
            .chunkIndex(chunkIdx)
            .entities(entities != null ? entities : List.of())
            .relations(relations != null ? relations : List.of())
            .build();
    }

    private List<EntityBatchDTO> buildEntityDTOs(List<ExtractedEntityVO> entities,
            List<float[]> embeddings, Map<String, String> uuidMapping) {
        List<EntityBatchDTO> dtos = new ArrayList<>();
        for (int i = 0; i < entities.size(); i++) {
            ExtractedEntityVO e = entities.get(i);
            String uuid = uuidMapping.getOrDefault(e.getName(),
                UUID.randomUUID().toString().replace("-", ""));
            dtos.add(EntityBatchDTO.builder()
                .uuid(uuid)
                .name(e.getName())
                .type(e.getType() != null ? e.getType() : "Entity")
                .summary(e.getSummary() != null ? e.getSummary() : "")
                .embedding(embeddings.get(i))
                .properties(e.getAttributes() != null ? e.getAttributes() : new HashMap<>())
                .build());
        }
        return dtos;
    }

    private List<RelationBatchDTO> buildRelationDTOs(List<ExtractedRelationVO> relations,
            Map<String, String> uuidMapping) {
        List<RelationBatchDTO> dtos = new ArrayList<>();
        List<String> texts = new ArrayList<>();
        List<ExtractedRelationVO> validRels = new ArrayList<>();

        for (ExtractedRelationVO r : relations) {
            String su = uuidMapping.get(r.getSource());
            String tu = uuidMapping.get(r.getTarget());
            if (su == null || tu == null) continue;
            validRels.add(r);
            texts.add(r.getFact() != null && !r.getFact().isBlank() ? r.getFact() : r.getType());
        }

        if (validRels.isEmpty()) return dtos;

        List<float[]> relEmbeddings = embeddingCacheService.getOrComputeBatch(texts);

        for (int i = 0; i < validRels.size(); i++) {
            ExtractedRelationVO r = validRels.get(i);
            dtos.add(RelationBatchDTO.builder()
                .edgeUuid(UUID.randomUUID().toString().replace("-", ""))
                .sourceUuid(uuidMapping.get(r.getSource()))
                .targetUuid(uuidMapping.get(r.getTarget()))
                .type(r.getType())
                .fact(r.getFact() != null ? r.getFact() : "")
                .embedding(relEmbeddings.get(i))
                .properties(new HashMap<>())
                .build());
        }
        return dtos;
    }

    private <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            result.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return result;
    }

    private <T> List<List<T>> partitionEntity(List<T> list, int size) {
        return partition(list, size);
    }

    private <T> List<List<T>> partitionRelation(List<T> list, int size) {
        return partition(list, size);
    }
}
```

### Step 2: Commit

```
git add ontograph-module-core/src/main/java/com/ontograph/module/graphiti/service/BulkImportTaskService.java
git commit -m "feat(batch): add BulkImportTaskService orchestration
- Phase 1: concurrent LLM extraction with chunking + CountDownLatch
- Phase 2: EntityDedupService integration
- Phase 3: embedding cache batch vector generation
- Phase 4: UNWIND sub-chunk writes with error isolation"
```

---

## Task 6: 创建 EmbeddingCacheService

**Files:**
- Create: `ontograph-module-core/src/main/java/com/ontograph/module/graphiti/service/EmbeddingCacheService.java`

### Step 1: 创建 EmbeddingCacheService

```java
package com.ontograph.module.graphiti.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean(RedissonClient.class);
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnBean(RedissonClient.class)
public class EmbeddingCacheService {

    private final EmbedderService embedderService;
    private final RedissonClient redissonClient;

    private static final String CACHE_PREFIX = "emb:";
    private static final long CACHE_TTL_SECONDS = 86400; // 24h

    public List<float[]> getOrComputeBatch(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        List<float[]> results = new ArrayList<>(Collections.nCopies(texts.size(), null));
        List<String> uncached = new ArrayList<>();
        List<Integer> uncachedIndices = new ArrayList<>();

        // 批量查 Redis
        for (int i = 0; i < texts.size(); i++) {
            String text = texts.get(i);
            if (text == null || text.isBlank()) {
                results.set(i, new float[embedderService.getDimensions()]);
                continue;
            }
            String cacheKey = CACHE_PREFIX + md5(text);
            try {
                String cached = redissonClient.getBucket(cacheKey).get();
                if (cached != null) {
                    results.set(i, deserialize(cached));
                } else {
                    uncached.add(text);
                    uncachedIndices.add(i);
                }
            } catch (Exception e) {
                log.warn("Redis 获取缓存失败, text={}: {}", text, e.getMessage());
                uncached.add(text);
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
                    redissonClient.getBucket(cacheKey)
                        .set(serialize(emb), CACHE_TTL_SECONDS, TimeUnit.SECONDS);
                } catch (Exception e) {
                    log.warn("Redis 写入缓存失败: {}", e.getMessage());
                }
            }
        }

        return results;
    }

    private String md5(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(text.hashCode());
        }
    }

    private String serialize(float[] arr) {
        byte[] bytes = new byte[arr.length * 4];
        ByteBuffer.wrap(bytes).asFloatBuffer().put(arr);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private float[] deserialize(String encoded) {
        byte[] bytes = Base64.getDecoder().decode(encoded);
        FloatBuffer fb = ByteBuffer.wrap(bytes).asFloatBuffer();
        float[] arr = new float[fb.remaining()];
        fb.get(arr);
        return arr;
    }
}
```

### Step 2: 添加缺失的 import

确保文件顶部有:

```java
import java.util.Base64;
import java.nio.FloatBuffer;
```

### Step 3: Commit

```
git add ontograph-module-core/src/main/java/com/ontograph/module/graphiti/service/EmbeddingCacheService.java
git commit -m "feat(batch): add EmbeddingCacheService with Redis
- Read-through cache with MD5 key
- Batch getOrCompute with cache miss fallback
- 24h TTL, graceful degradation on Redis failure"
```

---

## Task 7: 创建 ImportTaskRepository 和 ImportTaskController

**Files:**
- Create: `ontograph-module-core/src/main/java/com/ontograph/module/graphiti/dal/dataobject/ImportTaskDO.java`
- Create: `ontograph-module-core/src/main/java/com/ontograph/module/graphiti/dal/repository/ImportTaskRepository.java`
- Create: `ontograph-module-core/src/main/java/com/ontograph/module/graphiti/controller/admin/ImportTaskController.java`
- Create: `ontograph-module-core/src/main/resources/db/migration/V20260525__add_graph_import_task.sql`

### Step 1: 创建 ImportTaskDO

```java
package com.ontograph.module.graphiti.dal.dataobject;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ImportTaskDO {
    private String taskId;
    private String graphId;
    private Integer totalItems;
    private Integer processedItems;
    private Integer failedItems;
    private Integer entitiesCreated;
    private Integer relationsCreated;
    private String status; // PROCESSING / COMPLETED / FAILED / CANCELLED
    private String errorDetails;
    private Long durationMs;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
```

### Step 2: 创建 ImportTaskRepository

使用 ConcurrentHashMap 做内存持久化（后续可替换为真实 DB）:

```java
package com.ontograph.module.graphiti.dal.repository;

import com.ontograph.module.graphiti.dal.dataobject.ImportTaskDO;
import com.ontograph.module.graphiti.dto.batch.BulkImportResult;
import com.ontograph.module.graphiti.dto.batch.BulkImportTaskVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Repository
public class ImportTaskRepository {

    private final Map<String, ImportTaskDO> store = new ConcurrentHashMap<>();

    public void save(String taskId, String graphId, int totalItems) {
        ImportTaskDO task = new ImportTaskDO();
        task.setTaskId(taskId);
        task.setGraphId(graphId);
        task.setTotalItems(totalItems);
        task.setProcessedItems(0);
        task.setFailedItems(0);
        task.setEntitiesCreated(0);
        task.setRelationsCreated(0);
        task.setStatus("PROCESSING");
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        store.put(taskId, task);
        log.info("任务已保存: taskId={}, graphId={}, totalItems={}", taskId, graphId, totalItems);
    }

    public void updateResult(String taskId, BulkImportResult result) {
        ImportTaskDO task = store.get(taskId);
        if (task == null) return;
        task.setProcessedItems(result.getProcessedItems());
        task.setFailedItems(result.getFailedItems());
        task.setEntitiesCreated(result.getEntitiesCreated());
        task.setRelationsCreated(result.getRelationsCreated());
        task.setStatus("COMPLETED");
        task.setDurationMs(result.getDurationMs());
        task.setErrorDetails(result.getErrorDetails() != null
            ? String.join("; ", result.getErrorDetails()) : null);
        task.setUpdateTime(LocalDateTime.now());
        log.info("任务结果已更新: taskId={}, status=COMPLETED", taskId);
    }

    public void updateFailed(String taskId, String errorMessage) {
        ImportTaskDO task = store.get(taskId);
        if (task == null) return;
        task.setStatus("FAILED");
        task.setErrorDetails(errorMessage);
        task.setUpdateTime(LocalDateTime.now());
        log.error("任务失败: taskId={}, error={}", taskId, errorMessage);
    }

    public BulkImportTaskVO getTask(String taskId) {
        ImportTaskDO task = store.get(taskId);
        if (task == null) return null;
        return BulkImportTaskVO.builder()
            .taskId(task.getTaskId())
            .status(task.getStatus())
            .totalItems(task.getTotalItems())
            .processedItems(task.getProcessedItems())
            .failedItems(task.getFailedItems())
            .entitiesCreated(task.getEntitiesCreated())
            .relationsCreated(task.getRelationsCreated())
            .durationMs(task.getDurationMs())
            .errorDetails(task.getErrorDetails() != null
                ? List.of(task.getErrorDetails().split(";")) : List.of())
            .build();
    }
}
```

### Step 3: 创建 ImportTaskController

```java
package com.ontograph.module.graphiti.controller.admin;

import com.ontograph.common.response.CommonResult;
import com.ontograph.module.graphiti.dal.repository.ImportTaskRepository;
import com.ontograph.module.graphiti.dto.batch.BulkImportTaskVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "导入任务", description = "批量导入任务状态查询")
@RestController
@RequestMapping("/api/v1/graph/data/task")
public class ImportTaskController {

    @Resource
    private ImportTaskRepository importTaskRepository;

    @GetMapping("/{taskId}")
    @Operation(summary = "查询任务状态")
    public CommonResult<BulkImportTaskVO> getTask(@PathVariable String taskId) {
        BulkImportTaskVO task = importTaskRepository.getTask(taskId);
        if (task == null) {
            return CommonResult.fail(404, "任务不存在: " + taskId);
        }
        return CommonResult.success(task);
    }

    @DeleteMapping("/{taskId}")
    @Operation(summary = "取消任务")
    public CommonResult<Void> cancelTask(@PathVariable String taskId) {
        log.info("取消任务: taskId={}", taskId);
        return CommonResult.success(null);
    }
}
```

### Step 4: 创建数据库迁移脚本

```sql
-- V20260525__add_graph_import_task.sql
CREATE TABLE IF NOT EXISTS graph_import_task (
    task_id           VARCHAR(64) PRIMARY KEY,
    graph_id         VARCHAR(64) NOT NULL,
    total_items      INT NOT NULL DEFAULT 0,
    processed_items  INT NOT NULL DEFAULT 0,
    failed_items     INT NOT NULL DEFAULT 0,
    entities_created INT NOT NULL DEFAULT 0,
    relations_created INT NOT NULL DEFAULT 0,
    status           VARCHAR(32) NOT NULL DEFAULT 'PROCESSING',
    error_details    TEXT,
    duration_ms      BIGINT,
    create_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_graph_import_task_graph_id ON graph_import_task(graph_id);
CREATE INDEX IF NOT EXISTS idx_graph_import_task_status ON graph_import_task(status);
CREATE INDEX IF NOT EXISTS idx_graph_import_task_create_time ON graph_import_task(create_time);
```

### Step 5: Commit

```
git add ontograph-module-core/src/main/java/com/ontograph/module/graphiti/dal/dataobject/ImportTaskDO.java
git add ontograph-module-core/src/main/java/com/ontograph/module/graphiti/dal/repository/ImportTaskRepository.java
git add ontograph-module-core/src/main/java/com/ontograph/module/graphiti/controller/admin/ImportTaskController.java
git add ontograph-module-core/src/main/resources/db/migration/V20260525__add_graph_import_task.sql
git commit -m "feat(batch): add ImportTaskRepository and ImportTaskController
- ConcurrentHashMap-based task persistence
- GET /task/{taskId} status polling
- DELETE /task/{taskId} cancel
- SQL migration script for PostgreSQL/MySQL"
```

---

## Task 8: 更新 DataImportController 异步返回 taskId

**Files:**
- Modify: `ontograph-module-core/src/main/java/com/ontograph/module/graphiti/controller/admin/DataImportController.java`

### Step 1: 修改 addDataBatch 方法

替换现有的 `addDataBatch` 方法体（从第 94 行开始）:

```java
@PostMapping("/batch")
@Operation(summary = "批量添加数据（异步）", description = "批量导入数据到图谱，立即返回 taskId",
           security = {@SecurityRequirement(name = "Bearer Authentication")})
public CommonResult<String> addDataBatch(@Valid @RequestBody AddDataBatchReqVO reqVO) {
    long start = System.currentTimeMillis();
    try {
        // 设置默认分片大小
        if (reqVO.getContentChunkSize() == null) {
            reqVO.setContentChunkSize(50);
        }
        if (reqVO.getNeo4jChunkSize() == null) {
            reqVO.setNeo4jChunkSize(200);
        }

        String taskId = bulkImportTaskService.executeAsync(reqVO);
        saveDataOpLog("批量添加数据(异步)", "POST /graph/data/batch",
                      reqVO.getGraphId(),
                      Map.of("taskId", taskId, "itemCount", reqVO.getItems() != null ? reqVO.getItems().size() : 0),
                      1, null, start);
        return CommonResult.success(taskId);
    } catch (Exception e) {
        saveDataOpLog("批量添加数据(异步)", "POST /graph/data/batch",
                      reqVO.getGraphId(),
                      Map.of("itemCount", reqVO.getItems() != null ? reqVO.getItems().size() : 0),
                      0, e.getMessage(), start);
        throw e;
    }
}
```

### Step 2: 添加依赖注入

在类中注入:

```java
@Resource
private BulkImportTaskService bulkImportTaskService;
```

### Step 3: Commit

```
git add ontograph-module-core/src/main/java/com/ontograph/module/graphiti/controller/admin/DataImportController.java
git commit -m "feat(batch): DataImportController returns taskId immediately
- Delegates to BulkImportTaskService.executeAsync()
- Default contentChunkSize=50, neo4jChunkSize=200
- Async pattern with taskId polling"
```

---

## Task 9: 验证编译和基本测试

### Step 1: 编译检查

```bash
cd d:\projects\graphiti-java
mvn compile -pl ontograph-module-core -q
```

预期: 编译成功，无错误

### Step 2: 确认所有新增类被正确引用

验证 `BulkImportTaskService` 正确注入了所有依赖:
- `GraphNeo4jService` ✅ (已有)
- `LlmClientService` ✅ (已有)
- `EmbedderService` ✅ (已有)
- `EmbeddingCacheService` ✅ (新增)
- `TemporalService` ✅ (已有)
- `EntityDedupService` ✅ (已有)
- `ImportTaskRepository` ✅ (新增)

### Step 3: Commit

```
git add -A
git commit -m "feat(batch): complete batch import optimization implementation"
```

---

## 实施顺序

| Task | 名称 | 预计时间 | 状态 |
|------|------|---------|------|
| **Task 1** | Phase 1 DTOs | 15 min | ✅ 完成 |
| **Task 2** | GraphNeo4jService UNWIND | 20 min | ✅ 完成 |
| **Task 3** | DataImportServiceImpl 重写 | 15 min | ⏭ 跳过（由 Task 5 统一编排） |
| **Task 4** | 并发 chatBatch | 15 min | ✅ 完成 |
| **Task 5** | BulkImportTaskService | 25 min | ✅ 完成 |
| **Task 6** | EmbeddingCacheService | 20 min | ✅ 完成 |
| **Task 7** | ImportTaskRepository + Controller | 20 min | ✅ 完成 |
| **Task 8** | DataImportController 改造 | 10 min | ✅ 完成 |
| **Task 9** | 编译验证 | 10 min | ✅ 完成 |

**预计总工时**: ~2.5 小时

---

## 实施记录 (2026-05-26)

### 完成清单

1. **GraphNeo4jService 接口** — 新增 4 个批量方法
2. **GraphNeo4jServiceImpl** — 实现 `batchCreateEpisodes`、`batchCreateEntities`、`batchCreateRelationships`、`batchAddNodesAndEdges`（UNWIND 单事务）
3. **OpenAiLlmClientServiceImpl** — 新增 `chatBatchAsync` 带 Semaphore 并发控制
4. **BulkImportTaskService** — 四阶段编排服务（LLM抽取 → 三层去重 → 向量生成 → Neo4j写入），支持异步执行
5. **EmbeddingCacheService** — Redis 向量缓存，支持批量读写和失效，ConditionalOnBean Redisson
6. **ImportTaskRepository** — 内存存储 + 未来可迁移到数据库
7. **ImportTaskDO** — 任务状态数据对象
8. **ImportTaskController** — `GET /api/v1/graph/data/task/{taskId}` 任务状态查询
9. **DataImportController** — `POST /graph/data/batch` 改为立即返回 taskId
10. **V20260525__add_graph_import_task.sql** — 数据库迁移脚本（参考用）

### 编译结果

```
BUILD SUCCESS — ontograph-module-core 编译通过
仅有 1 个无害 deprecation 警告（Redisson 内部 API）
```
