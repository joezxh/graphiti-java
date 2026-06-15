# ontograph-java 全量迁移设计文档

> **目标**: 将 Python 版 graphiti 全部核心能力迁移至 Java 版，实现功能完全对齐。
> **版本**: 2.0
> **日期**: 2026-05-11

---

## 一、现状分析

### Java 版已完成（阶段一 P0 骨架）
- 基础图谱 CRUD、节点/边管理、Episode 管理
- 本体管理（Ontology）、用户权限系统
- Spring Security + JWT、Redis 缓存
- Vue 3 前端控制台
- 空壳服务：EmbedderService、LlmClientService、TemporalService、RerankerService、CommunityService

### Java 版待完善（14 个 TODO）
1. EmbedderServiceImpl - 占位实现，未接入 Spring AI EmbeddingClient
2. LlmClientServiceImpl - 占位实现，未接入 ChatClient
3. EmbedderConfig - 空配置
4. SearchServiceImpl.doSearch - 仅全文搜索，缺向量/BFS/RRF/MMR
5. DataImportServiceImpl - 4 个 TODO（LLM 提取、批量处理、对话提取、节点检查）
6. NodeServiceImpl - 节点更新逻辑未完成
7. EdgeServiceImpl - 边更新逻辑未完成
8. GraphitiServiceImpl - Neo4j 数据清理未完成
9. GraphNeo4jService - 提及边查询未完成

### Python 版核心模块（迁移源）
```
graphiti_core/
├── graphiti.py           (1741 行) - 核心编排入口
├── nodes.py              (1111 行) - 6 种节点模型
├── edges.py              (1047 行) - 6 种边模型 + 嵌入生成
├── search/               (7 文件) - 混合检索引擎
│   ├── search.py         - 主搜索逻辑
│   ├── search_config.py  - SearchConfig/SearchResults
│   ├── search_config_recipes.py - 6 种预置配方
│   ├── search_filters.py - 搜索过滤器
│   └── search_utils.py   - BM25/向量/BFS/RRF/MMR 实现
├── llm_client/           (15 文件) - 多 LLM 提供商抽象
├── embedder/             (6 文件) - 多 Embedder 提供商抽象
├── cross_encoder/        (5 文件) - 重排序客户端
├── prompts/              (13 文件) - Prompt 模板库
├── utils/                (7 文件) - 批量处理/去重/维护工具
│   ├── bulk_utils.py     - 批量节点边操作
│   └── maintenance/      - 社区/边/数据维护
└── driver/               (15 文件) - 多数据库驱动抽象
```

---

## 二、总体架构设计

### 2.1 迁移后 Java 架构

```
ontograph-module-core/
├── controller/admin/
│   ├── GraphitiController.java      # 扩展：社区/克隆/导出/历史
│   ├── SearchController.java        # 扩展：混合/语义/BFS 检索
│   ├── EpisodeController.java       # 扩展：LLM 提取触发
│   ├── DataImportController.java    # 已有
│   ├── NodeController.java          # 已有
│   ├── EdgeController.java          # 已有
│   └── MaintenanceController.java   # 新增：数据质量/维护
├── service/
│   ├── GraphitiCoreService.java     # 新增：统一编排（对应 graphiti.py）
│   ├── LlmClientService.java        # 已有 - 完善实现
│   ├── EmbedderService.java         # 已有 - 完善实现
│   ├── SearchService.java           # 已有 - 完善混合检索
│   ├── TemporalService.java         # 已有 - 完善时序
│   ├── CommunityService.java        # 已有 - 完善社区
│   ├── RerankerService.java         # 已有 - 完善重排序
│   ├── DataQualityService.java      # 已有 - 完善去重
│   ├── SagaService.java             # 已有 - 完善 Saga
│   ├── GraphDriverService.java      # 已有 - 完善驱动抽象
│   └── GraphNeo4jService.java       # 已有 - 扩展向量/时序
├── service/impl/
│   └── ... 全部完善
├── dal/neo4j/                       # 新增：Neo4j 数据访问层
│   ├── NodeRepository.java
│   ├── EdgeRepository.java
│   ├── EpisodeRepository.java
│   ├── CommunityRepository.java
│   └── VectorIndexRepository.java
├── vo/
│   ├── search/                      # 扩展 SearchConfig/SearchResults
│   ├── llm/                         # 已有
│   ├── temporal/                    # 已有
│   ├── node/                        # 扩展 6 种节点类型
│   ├── edge/                        # 扩展 6 种边类型
│   └── embedding/                   # 已有
└── resources/prompts/               # 已有 - 完善 13 个模板
```

### 2.2 与 Python 版模块映射

| Python 模块 | Java 对应 | 状态 |
|------------|----------|------|
| `graphiti.py` | `GraphitiCoreServiceImpl` | 新增编排层 |
| `nodes.py` (6 节点类型) | `vo/node/*` + `dal/neo4j/NodeRepository` | 扩展 |
| `edges.py` (6 边类型) | `vo/edge/*` + `dal/neo4j/EdgeRepository` | 扩展 |
| `search/*.py` | `service/impl/SearchServiceImpl` + `RerankerServiceImpl` | 完善 |
| `llm_client/*.py` | `service/impl/LlmClientServiceImpl` | 完善 |
| `embedder/*.py` | `service/impl/EmbedderServiceImpl` | 完善 |
| `cross_encoder/*.py` | `service/impl/RerankerServiceImpl` | 完善 |
| `prompts/*.py` | `resources/prompts/*.txt` | 完善 |
| `utils/bulk_utils.py` | `service/impl/DataQualityServiceImpl` | 完善 |
| `utils/maintenance/*.py` | `service/impl/CommunityServiceImpl` | 完善 |
| `driver/*.py` | `service/impl/Neo4jDriverAdapter` | 完善 |

---

## 三、子系统详细设计

### 3.1 时序事实管理系统 (Temporal Management)

**Python 对应**: `nodes.py` (valid_at/invalid_at) + `edges.py` (fact/facts)

**核心设计**:
- 所有 EntityNode/EpisodeNode/CommunityNode/SagaNode 添加 `valid_at` (Long) 和 `invalid_at` (Long) 属性
- 所有 EntityEdge/EpisodicEdge/CommunityEdge/HasEpisodeEdge/NextEpisodeEdge 添加 `valid_at`/`invalid_at` + `fact` (String) + `facts` (List<Map>)
- 新增 Episode 时自动触发 `invalidateFacts`：匹配同名实体，将旧节点/边的 `invalid_at` 设为当前时间
- 查询时默认过滤 `invalid_at IS NULL OR invalid_at > now`

**接口扩展**:
```java
public interface TemporalService {
    void invalidateFacts(String graphId, List<String> entityNames, LocalDateTime invalidAt);
    List<TemporalNodeVO> getValidNodes(String graphId);
    List<TemporalNodeVO> getValidNodesAt(String graphId, LocalDateTime referenceTime);
    List<TemporalEdgeVO> getValidEdgesAt(String graphId, LocalDateTime referenceTime);
    List<TemporalNodeVO> getFactVersions(String graphId, String entityName);
}
```

### 3.2 LLM 提取系统 (LLM Extraction)

**Python 对应**: `llm_client/` (15 文件) + `prompts/` (13 文件)

**核心设计**:
- 多 Provider 支持：OpenAI / 阿里云通义千问 / Ollama
- Spring AI 集成：`ChatClient` + `EmbeddingClient`
- 配置文件驱动切换：
```yaml
graphiti:
  llm:
    provider: openai  # openai | qwen | ollama
    openai:
      api-key: ${OPENAI_API_KEY}
      model: gpt-4o
    qwen:
      api-key: ${DASHSCOPE_API_KEY}
      base-url: https://dashscope.aliyuncs.com/api/v1
    ollama:
      base-url: http://localhost:11434
      model: llama3
```
- Prompt 模板库（resources/prompts/）：
  - `extract_entities.txt` - 实体提取（含 few-shot 示例）
  - `extract_relations.txt` - 关系提取
  - `summarize_node.txt` - 节点摘要生成
  - `summarize_community.txt` - 社区摘要生成
  - `summarize_saga.txt` - Saga 摘要生成
  - `system_prompt.txt` - 系统提示词
- 结构化输出：使用 Jackson 解析 JSON，带重试机制

### 3.3 混合检索系统 (Hybrid Search)

**Python 对应**: `search/search.py` + `search/search_utils.py` (2064 行)

**核心设计**:
- **BM25 全文搜索**: Neo4j `db.index.fulltext.queryNodes/Relationships`
- **向量相似度搜索**: Neo4j Vector Index + `db.index.vector.queryNodes`
- **BFS 图遍历**: Cypher `apoc.path.subgraphNodes` 或自定义 BFS
- **RRF 融合**: `score = Σ(1 / (k + rank))`，k=60
- **MMR 重排序**: 平衡相关性 λ 与多样性 (1-λ)
- **SearchConfig 配置对象**:
```java
public class SearchConfig {
    private EdgeSearchConfig edgeConfig;
    private NodeSearchConfig nodeConfig;
    private EpisodeSearchConfig episodeConfig;
    private CommunitySearchConfig communityConfig;
    private SearchFilters filters;
}
```
- **预置检索配方**（对应 Python 的 `search_config_recipes.py`）：
  - `EDGE_HYBRID_SEARCH_RRF` - 边混合检索（BM25 + 向量 + RRF）
  - `EDGE_HYBRID_SEARCH_NODE_DISTANCE` - 边混合 + 节点距离
  - `COMBINED_HYBRID_SEARCH_CROSS_ENCODER` - 组合混合 + Cross Encoder
  - `NODE_SEMANTIC_SEARCH` - 节点语义搜索
  - `EPISODE_SEARCH` - Episode 搜索
  - `COMMUNITY_SEARCH` - 社区搜索

### 3.4 嵌入向量系统 (Embeddings)

**Python 对应**: `embedder/` (6 文件) + `nodes.py` (create_entity_node_embeddings) + `edges.py` (create_entity_edge_embeddings)

**核心设计**:
- 多 Provider 支持：OpenAI / 阿里云 / Ollama / 本地模型
- 维度自动检测（OpenAI text-embedding-3-small = 1536）
- **节点嵌入**: 名称嵌入 + 摘要嵌入
- **边嵌入**: 事实描述嵌入
- **Neo4j Vector Index**:
```cypher
CREATE VECTOR INDEX node_embedding_index IF NOT EXISTS
FOR (n:Entity) ON (n.embedding)
OPTIONS {indexConfig: {`vector.dimensions`: 1536, `vector.similarity_function`: 'cosine'}}
```
- 节点/边创建时自动调用 `EmbedderService.embed()` 生成向量

### 3.5 社区发现系统 (Community Detection)

**Python 对应**: `utils/maintenance/community_operations.py`

**核心设计**:
- 使用 Neo4j GDS (Graph Data Science) 的 Louvain 算法或标签传播
- 若无 GDS，使用 Cypher 实现的简化聚类
- 社区节点 `CommunityNode`：uuid、name、summary、member_count
- 社区边 `CommunityEdge`：连接社区与成员节点
- LLM 生成社区摘要：`LlmClientService.generateCommunitySummary()`

### 3.6 数据质量保障 (Data Quality)

**Python 对应**: `utils/bulk_utils.py` (dedupe_nodes_bulk, dedupe_edges_bulk)

**核心设计**:
- **节点去重**: 基于名称相似度（Jaccard + Levenshtein），阈值 0.85
- **边去重**: 基于 (source_uuid, target_uuid, type, fact) 组合
- **实体解析**: 合并重复节点，更新所有关联边
- **矛盾检测**: 检测同一实体对的不同事实描述

### 3.7 Saga 管理 (Saga Management)

**Python 对应**: `nodes.py` (SagaNode) + `edges.py` (HasEpisodeEdge, NextEpisodeEdge)

**核心设计**:
- `SagaNode`: 长篇叙事节点，包含 title、summary、episode_count
- `HasEpisodeEdge`: Saga → Episode 的归属关系
- `NextEpisodeEdge`: Episode → Episode 的时序连接
- `Episode` 添加时自动维护 `previous_episode_uuids`

### 3.8 多数据库驱动抽象 (GraphDriver)

**Python 对应**: `driver/` (15 文件)

**核心设计**:
- `GraphDriverService` 接口：抽象所有图数据库操作
- `Neo4jDriverAdapter` 实现：基于 Neo4j Java Driver
- 预留 `FalkorDbDriverAdapter`、`KuzuDriverAdapter` 接口

---

## 四、数据模型扩展

### 4.1 节点类型（6 种）

| 节点类型 | 标签 | 核心属性 | 说明 |
|---------|------|---------|------|
| EntityNode | `:Entity` | name, type, summary, valid_at, invalid_at, embedding | 实体节点 |
| EpisodicNode | `:EpisodicNode` | content, source, source_description, valid_at, processed | Episode 节点 |
| CommunityNode | `:CommunityNode` | name, summary, member_count | 社区节点 |
| SagaNode | `:SagaNode` | title, summary, episode_count | 长篇叙事节点 |

### 4.2 边类型（6 种）

| 边类型 | 类型 | 核心属性 | 说明 |
|--------|------|---------|------|
| EntityEdge | `:RELATES_TO` | type, fact, valid_at, invalid_at, embedding | 实体关系 |
| EpisodicEdge | `:EPISODIC` |  | Episode 提及节点 |
| CommunityEdge | `:COMMUNITY` |  | 社区成员关系 |
| HasEpisodeEdge | `:HAS_EPISODE` |  | Saga 拥有 Episode |
| NextEpisodeEdge | `:NEXT_EPISODE` |  | Episode 时序连接 |
| MentionsEdge | `:MENTIONS` |  | Episode 提及实体 |

---

## 五、API 设计

### 5.1 新增 REST 接口

```
# 混合检索
POST   /api/v1/graph/{graphId}/search/hybrid
POST   /api/v1/graph/{graphId}/search/semantic
POST   /api/v1/graph/{graphId}/search/bfs

# 社区
POST   /api/v1/graph/{graphId}/communities/build
GET    /api/v1/graph/{graphId}/communities
DELETE /api/v1/graph/{graphId}/communities

# Saga
POST   /api/v1/graph/{graphId}/sagas
GET    /api/v1/graph/{graphId}/sagas

# 数据质量
POST   /api/v1/graph/{graphId}/maintenance/dedupe
POST   /api/v1/graph/{graphId}/maintenance/resolve

# 历史查询
GET    /api/v1/graph/{graphId}/history?time=2024-01-01T00:00:00
GET    /api/v1/graph/{graphId}/facts/{entityName}/versions

# 克隆/导出
POST   /api/v1/graph/{graphId}/clone
GET    /api/v1/graph/{graphId}/export
```

### 5.2 扩展接口

```
POST   /api/v1/graph/{graphId}/data        # 扩展：支持 LLM 自动提取
POST   /api/v1/graph/{graphId}/data/batch  # 扩展：批量 + LLM 提取
POST   /api/v1/graph/{graphId}/messages    # 扩展：对话历史提取
```

---

## 六、配置设计

### 6.1 application.yml

```yaml
spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY:}
      chat:
        options:
          model: gpt-4o
          temperature: 0.2
      embedding:
        options:
          model: text-embedding-3-small

graphiti:
  llm:
    provider: openai  # openai | qwen | ollama
  embedding:
    provider: openai
    dimensions: 1536
  search:
    default-limit: 10
    rrf-k: 60
    mmr-lambda: 0.5
  temporal:
    auto-invalidate: true
  community:
    min-members: 2
    max-communities: 50
  data-quality:
    dedupe-threshold: 0.85
```

---

## 七、测试策略

### 7.1 单元测试
- `EmbedderServiceImplTest` - 测试嵌入生成和相似度计算
- `LlmClientServiceImplTest` - Mock ChatClient 测试提取逻辑
- `TemporalServiceImplTest` - 测试时序失效和查询
- `RerankerServiceImplTest` - 测试 RRF/MMR 算法
- `SearchServiceImplTest` - Mock Neo4j 测试混合检索

### 7.2 集成测试
- `SearchIntegrationTest` - Testcontainers + Neo4j 测试完整检索流程
- `TemporalIntegrationTest` - 测试 Episode 添加 → 自动失效 → 历史查询
- `DataImportIntegrationTest` - 测试 LLM 提取 → 节点创建 → 向量生成

### 7.3 契约测试
- 前端 API 契约一致性验证

---

## 八、实施顺序

```
Phase 1: 基础能力（1-2 周）
├── Task 1: Spring AI 多 Provider 集成
│   ├── 完善 EmbedderServiceImpl（接入 EmbeddingClient）
│   ├── 完善 LlmClientServiceImpl（接入 ChatClient）
│   └── 配置 OpenAI/通义千问/Ollama 切换
├── Task 2: Prompt 工程完善
│   ├── 优化 extract_entities.txt（few-shot 示例）
│   ├── 优化 extract_relations.txt
│   └── 添加系统提示词
└── Task 3: Neo4j 向量索引
    ├── 创建 Vector Index Repository
    └── 节点/边创建时自动嵌入

Phase 2: 核心功能（2-3 周）
├── Task 4: SearchService 混合检索完善
│   ├── 实现 BM25 全文搜索
│   ├── 实现向量相似度搜索
│   ├── 实现 BFS 图遍历搜索
│   └── 集成 RRF/MMR 重排序
├── Task 5: 时序管理完善
│   ├── GraphNeo4jService 扩展时序字段
│   ├── DataImportService 集成自动失效
│   └── 历史查询接口完善
├── Task 6: 数据导入完善
│   ├── addData 集成 LLM 实体提取
│   ├── addMessages 集成对话历史提取
│   └── addFactTriple 完善节点检查
└── Task 7: 社区发现完善
    ├── 集成图聚类算法
    └── LLM 社区摘要生成

Phase 3: 高级功能（2-3 周）
├── Task 8: 数据质量保障
│   ├── 节点去重（相似度计算）
│   ├── 边去重
│   └── 实体解析
├── Task 9: Saga 管理
│   ├── SagaNode 模型
│   ├── HasEpisodeEdge / NextEpisodeEdge
│   └── Episode 时序链
├── Task 10: 多数据库驱动
│   └── 完善 GraphDriverService + Neo4jDriverAdapter
└── Task 11: 测试覆盖
    ├── 单元测试（Mockito）
    └── 集成测试（Testcontainers）

Phase 4: 收尾（1 周）
├── Task 12: 所有 TODO 清理
├── Task 13: 前端 API 同步
└── Task 14: 文档更新 + 合并分支
```

---

## 九、风险与应对

| 风险 | 影响 | 应对策略 |
|------|------|---------|
| Spring AI 版本兼容性 | 高 | 锁定 1.1.2 版本，逐步升级 |
| Neo4j Vector Index 性能 | 中 | 监控查询耗时，必要时分片 |
| LLM 提取准确性 | 中 | Prompt 工程 + 重试机制 + 兜底逻辑 |
| 多 Provider 切换复杂度 | 低 | 条件装配 `@ConditionalOnProperty` |
| 数据模型变更兼容性 | 中 | 向后兼容，新字段允许 null |

---

**文档版本**: 2.0  
**创建时间**: 2026-05-11  
**关联文档**: ontograph-java-vs-python-comparison.md, 2026-05-10-ontograph-java-full-alignment-design.md
