# Graphiti-Java 功能对齐总体设计

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 使 graphiti-java 具备与 Python 原版 graphiti 相同的核心能力，包括时序事实管理、LLM自动提取、混合检索、嵌入向量、社区发现等。

**Architecture:** 在现有 graphiti-java 多模块Maven架构基础上，逐阶段补齐缺失的核心能力。保持与现有代码的向后兼容性，新增模块遵循现有分层设计（controller → service → dal → vo）。

**Tech Stack:** Java 21, Spring Boot 3.5.5, Spring AI 1.1.2, Neo4j 5.26, MySQL/PostgreSQL, Redis, MyBatis-Plus 3.5.12

---

## 目录

1. [设计决策](#设计决策)
2. [阶段划分](#阶段划分)
3. [子系统设计](#子系统设计)
4. [数据模型扩展](#数据模型扩展)
5. [API设计](#api设计)
6. [实施顺序](#实施顺序)

---

## 设计决策

### 1.1 核心架构原则

- **向后兼容**: 现有API保持不变，新增功能通过新接口和扩展字段实现
- **模块化设计**: 每个子系统独立实现，通过Service接口解耦
- **配置驱动**: LLM提供商、Embedder等通过Spring配置注入，支持切换
- **渐进式增强**: 不破坏现有功能，逐步叠加新能力

### 1.2 与Python原版的对应关系

| Python原版模块 | Java对应模块 | 说明 |
|--------------|-------------|------|
| `graphiti_core/graphiti.py` | `service/impl/GraphitiCoreService.java` | 核心编排服务 |
| `graphiti_core/nodes.py` | `vo/node/*` + `dal/neo4j/*` | 节点模型 |
| `graphiti_core/edges.py` | `vo/edge/*` + `dal/neo4j/*` | 边模型 |
| `graphiti_core/search/` | `service/impl/SearchServiceImpl.java` | 检索服务 |
| `graphiti_core/llm_client/` | `service/llm/` | LLM客户端抽象 |
| `graphiti_core/embedder/` | `service/embedder/` | Embedder客户端抽象 |
| `graphiti_core/cross_encoder/` | `service/reranker/` | 重排序服务 |
| `graphiti_core/prompts/` | `resources/prompts/` | Prompt模板 |
| `graphiti_core/utils/maintenance/` | `service/maintenance/` | 维护工具 |

### 1.3 新增模块规划

```
graphiti-module-core/src/main/java/com/graphiti/module/graphiti/
├── controller/admin/
│   ├── GraphitiController.java          # 已有 - 扩展新接口
│   ├── SearchController.java            # 已有 - 扩展混合检索
│   ├── MaintenanceController.java       # 新增 - 社区构建/维护
│   └── LlMConfigController.java         # 新增 - LLM配置管理
├── service/
│   ├── GraphitiCoreService.java         # 新增 - 核心编排接口
│   ├── LlmClientService.java            # 新增 - LLM客户端抽象
│   ├── EmbedderService.java             # 新增 - 嵌入向量服务
│   ├── RerankerService.java             # 新增 - 重排序服务
│   ├── TemporalService.java             # 新增 - 时序管理服务
│   ├── CommunityService.java            # 新增 - 社区发现服务
│   └── GraphDriverService.java          # 新增 - 多数据库驱动抽象
├── service/impl/
│   ├── GraphitiCoreServiceImpl.java     # 新增
│   ├── LlmClientServiceImpl.java        # 新增
│   ├── EmbedderServiceImpl.java         # 新增
│   ├── RerankerServiceImpl.java         # 新增
│   ├── TemporalServiceImpl.java         # 新增
│   ├── CommunityServiceImpl.java        # 新增
│   └── GraphDriverServiceImpl.java      # 新增
├── dal/
│   ├── neo4j/
│   │   ├── TemporalNodeRepository.java  # 新增 - 时序节点存储
│   │   ├── TemporalEdgeRepository.java  # 新增 - 时序边存储
│   │   ├── VectorNodeRepository.java    # 新增 - 向量节点存储
│   │   └── VectorEdgeRepository.java    # 新增 - 向量边存储
│   └── dataobject/
│       └── LlmConfigDO.java             # 新增 - LLM配置存储
├── vo/
│   ├── temporal/                        # 新增 - 时序相关VO
│   ├── search/                          # 已有 - 扩展检索配置
│   ├── llm/                             # 新增 - LLM相关VO
│   └── embedding/                       # 新增 - 嵌入向量VO
└── config/
    ├── GraphitiCoreConfig.java          # 新增 - 核心配置
    ├── LlmClientConfig.java             # 新增 - LLM配置
    ├── EmbedderConfig.java              # 新增 - Embedder配置
    └── GraphDriverConfig.java           # 新增 - 驱动配置
```

---

## 阶段划分

### 阶段一：P0核心能力 (4-6周)

**目标**: 实现Graphiti的四大核心价值

1. **时序事实管理** (2周)
   - 添加 `valid_at`/`invalid_at` 字段到节点和边
   - 实现自动事实失效逻辑
   - 实现历史状态查询接口

2. **LLM实体关系提取** (2周)
   - 集成 Spring AI (OpenAI)
   - 实现实体提取Prompt工程
   - 实现关系提取Prompt工程
   - 结构化输出解析

3. **混合检索系统** (2周)
   - Neo4j向量索引支持
   - BM25全文搜索增强
   - RRF重排序实现
   - 基础BFS图遍历搜索

4. **嵌入向量系统** (与检索并行)
   - EmbedderClient接口
   - 节点名称/摘要嵌入
   - 边事实嵌入

### 阶段二：P1重要功能 (4-5周)

1. **社区发现** (1周)
2. **多数据库驱动抽象** (2周)
3. **Saga管理** (1周)
4. **数据质量保障** (1周)

### 阶段三：P2功能完善 (3-4周)

1. **本体系统增强** (1周)
2. **边类型多样化** (1周)
3. **图谱克隆/导出** (1周)
4. **测试与优化** (1周)

### 阶段四：P3增强功能 (2-3周)

1. **可观测性** (OpenTelemetry)
2. **MCP Server支持**
3. **性能优化**

---

## 子系统设计

### 3.1 时序事实管理系统 (Temporal Management)

**设计要点**:
- 在现有 EntityNode 和 Relationship 上添加 `valid_at` 和 `invalid_at` 字段
- Episode添加时自动触发旧事实的失效逻辑
- 查询时默认返回当前有效的节点/边
- 支持按时间点查询历史状态

**核心接口**:
```java
public interface TemporalService {
    // 标记旧事实失效
    void invalidateFacts(String graphId, List<String> entityNames, LocalDateTime invalidAt);
    
    // 查询当前有效的节点
    List<Map<String, Object>> getValidNodes(String graphId, LocalDateTime referenceTime);
    
    // 查询历史状态
    List<Map<String, Object>> getNodesAtTime(String graphId, LocalDateTime referenceTime);
    
    // 获取事实版本链
    List<Map<String, Object>> getFactVersions(String graphId, String entityName);
}
```

### 3.2 LLM提取系统 (LLM Extraction)

**设计要点**:
- 抽象 LlmClientService 接口，支持多提供商
- 初期实现 OpenAI 适配器 (通过 Spring AI)
- Prompt模板使用资源文件管理 (resources/prompts/)
- 结构化输出使用 JSON Schema 约束

**核心接口**:
```java
public interface LlmClientService {
    // 提取实体
    List<ExtractedEntity> extractEntities(String text, List<EntityTypeDef> entityTypes);
    
    // 提取关系
    List<ExtractedRelation> extractRelations(String text, List<ExtractedEntity> entities);
    
    // 生成摘要
    String generateSummary(String content);
    
    // 生成社区摘要
    String generateCommunitySummary(List<String> nodeSummaries);
}
```

### 3.3 混合检索系统 (Hybrid Search)

**设计要点**:
- SearchConfig 配置对象定义检索策略
- 支持多种检索方法的组合
- Reranker 接口支持多种重排序算法
- 检索结果封装 SearchResults

**核心接口**:
```java
public interface SearchService {
    // 混合检索
    SearchResults search(String graphId, String query, SearchConfig config);
    
    // 语义搜索
    List<Map<String, Object>> semanticSearch(String graphId, String query, int limit);
    
    // 图遍历搜索
    List<Map<String, Object>> bfsSearch(String graphId, String startNodeUuid, int depth);
}
```

### 3.4 嵌入向量系统 (Embeddings)

**设计要点**:
- EmbedderService 抽象接口
- 初期支持 OpenAI Embedding (通过 Spring AI)
- 向量存储在 Neo4j 的节点/边属性中
- 向量索引使用 Neo4j Vector Index

**核心接口**:
```java
public interface EmbedderService {
    // 生成文本嵌入
    float[] embed(String text);
    
    // 批量生成
    List<float[]> embedBatch(List<String> texts);
    
    // 计算相似度
    double cosineSimilarity(float[] a, float[] b);
}
```

### 3.5 社区发现系统 (Community Detection)

**设计要点**:
- 使用图聚类算法 (如 Louvain)
- 社区节点和边的自动生成
- LLM生成社区摘要
- 社区搜索支持

**核心接口**:
```java
public interface CommunityService {
    // 构建社区
    CommunityBuildResult buildCommunities(String graphId);
    
    // 获取社区列表
    List<CommunityNode> listCommunities(String graphId);
    
    // 搜索社区
    List<CommunityNode> searchCommunities(String graphId, String query);
}
```

---

## 数据模型扩展

### 4.1 节点模型扩展

在现有 Neo4j 节点属性基础上添加：

```cypher
// EntityNode 新增属性
valid_at: datetime      // 事实有效时间
invalid_at: datetime    // 事实失效时间
name_embedding: list    // 名称嵌入向量
summary: string         // 节点摘要
summary_embedding: list // 摘要嵌入向量
attributes: map         // 动态属性
```

### 4.2 边模型扩展

```cypher
// EntityEdge 新增属性
valid_at: datetime      // 关系有效时间
invalid_at: datetime    // 关系失效时间
fact: string            // 事实描述
facts: list             // 事实版本链
embedding: list         // 事实嵌入向量
attributes: map         // 动态属性
```

### 4.3 新索引

```cypher
// 向量索引
CREATE VECTOR INDEX node_name_embedding IF NOT EXISTS
FOR (n:Entity) ON (n.name_embedding)
OPTIONS {indexConfig: {`vector.dimensions`: 1536, `vector.similarity_function`: 'cosine'}}

// 时序索引
CREATE INDEX node_valid_at IF NOT EXISTS
FOR (n:Entity) ON (n.valid_at, n.invalid_at)
```

---

## API设计

### 5.1 新增REST接口

```
POST   /api/v1/graph/{graphId}/episode          # 添加Episode (增强版，支持LLM提取)
POST   /api/v1/graph/{graphId}/search            # 混合检索
POST   /api/v1/graph/{graphId}/semantic-search   # 语义搜索
POST   /api/v1/graph/{graphId}/bfs-search        # BFS搜索
POST   /api/v1/graph/{graphId}/communities/build # 构建社区
GET    /api/v1/graph/{graphId}/communities       # 社区列表
GET    /api/v1/graph/{graphId}/history           # 历史状态查询
GET    /api/v1/graph/{graphId}/facts/{entityName}/versions  # 事实版本链
POST   /api/v1/llm/config                        # 配置LLM
GET    /api/v1/llm/config                        # 获取LLM配置
POST   /api/v1/graph/{graphId}/clone             # 克隆图谱
GET    /api/v1/graph/{graphId}/export            # 导出图谱
```

### 5.2 扩展现有接口

```
POST   /api/v1/graph/{graphId}/data              # 扩展：支持LLM自动提取
GET    /api/v1/nodes/{uuid}                      # 扩展：返回summary、valid_at等
GET    /api/v1/edges/{uuid}                      # 扩展：返回fact、valid_at等
```

---

## 实施顺序

### 依赖关系

```
嵌入向量系统 ← LLM提取系统 ← 时序管理系统
     ↓              ↓              ↓
混合检索系统 ← 社区发现系统 ← 数据质量保障
```

### 推荐实施顺序

1. **EmbedderService** (基础能力，其他模块依赖)
2. **LlmClientService** (基础能力，提取模块依赖)
3. **TemporalService** (核心功能，可独立实现)
4. **SearchService增强** (依赖Embedder)
5. **CommunityService** (依赖LLM + Search)
6. **GraphitiCoreService** (依赖以上所有，提供统一编排)

---

**文档版本**: 1.0  
**创建时间**: 2026-05-10
