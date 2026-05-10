# Graphiti-Java vs Graphiti (Python原版) 对比分析

**分析日期**: 2026-05-10  
**Java版本**: graphiti-java (v1.0.0-SNAPSHOT)  
**Python原版**: graphiti (getzep/graphiti, 最新主分支)

---

## 一、项目概述对比

| 维度 | Graphiti (Python原版) | Graphiti-Java |
|------|---------------------|---------------|
| **项目定位** | 时序上下文图谱引擎，面向AI Agent记忆 | 知识图谱后端服务系统 |
| **语言/框架** | Python 3.10+ / FastAPI | Java 21 / Spring Boot 3.5.5 |
| **核心特性** | 时序事实管理、本体学习、增量构建、混合检索 | 图谱CRUD、本体管理、数据导入、基础检索 |
| **数据库支持** | Neo4j、FalkorDB、Kuzu、Amazon Neptune | Neo4j (图数据库) + MySQL/PostgreSQL (元数据) |
| **LLM集成** | OpenAI、Gemini、Anthropic、Groq、Azure OpenAI | Spring AI (预留，未实现) |
| **部署方式** | Docker、pip/uv安装、MCP Server | Spring Boot Jar包 |
| **开源协议** | Apache 2.0 | 未指定 |

---

## 二、架构设计对比

### 2.1 项目结构

#### Python原版 (graphiti)
```
graphiti/
├── graphiti_core/              # 核心库
│   ├── graphiti.py            # 主入口 (1741行)
│   ├── nodes.py               # 节点模型 (1111行)
│   ├── edges.py               # 边模型 (1047行)
│   ├── search/                # 检索模块 (7个文件)
│   ├── driver/                # 数据库驱动 (15个文件)
│   ├── llm_client/           # LLM客户端 (15个文件)
│   ├── embedder/             # 嵌入向量客户端 (6个文件)
│   ├── cross_encoder/        # 重排序客户端 (5个文件)
│   ├── models/               # 数据模型
│   ├── prompts/              # LLM提示词 (13个文件)
│   ├── utils/                # 工具类
│   ├── namespaces/           # 命名空间管理
│   ├── migrations/           # 数据库迁移
│   └── telemetry/            # OpenTelemetry追踪
├── server/                    # REST API服务
│   └── graph_service/        # FastAPI应用
│       └── routers/          # 路由 (8个)
├── mcp_server/               # MCP协议服务器
├── examples/                  # 示例代码 (9个)
└── tests/                     # 测试套件
```

#### Java版本 (graphiti-java)
```
graphiti-java/
├── graphiti-framework/        # 框架层
│   ├── graphiti-common/      # 公共模块
│   ├── graphiti-spring-boot-starter-security/
│   ├── graphiti-spring-boot-starter-mybatis/
│   └── graphiti-spring-boot-starter-redis/
├── graphiti-module-system/    # 系统模块 (用户/角色/权限)
├── graphiti-module-core/      # 核心业务模块
│   └── src/main/java/.../graphiti/
│       ├── controller/admin/  # 控制器 (7个)
│       ├── service/          # 服务接口 (8个)
│       ├── service/impl/     # 服务实现 (7个)
│       ├── vo/               # 视图对象 (7个子包)
│       ├── dal/              # 数据访问层
│       └── config/           # 配置类
├── graphiti-server/          # 启动模块
└── graphiti-web/             # 前端控制台 (Vue 3)
```

### 2.2 架构差异

| 架构特性 | Python原版 | Java版本 | 状态 |
|---------|-----------|---------|------|
| **多数据库驱动抽象** | ✅ GraphDriver接口，支持4种数据库 | ❌ 仅支持Neo4j | 🔴 缺失 |
| **多模块Maven架构** | ❌ 单模块 + server | ✅ 完整的多模块设计 | 🟢 Java优势 |
| **前端控制台** | ❌ 无官方前端 | ✅ Vue 3 + Ant Design | 🟢 Java优势 |
| **用户权限系统** | ❌ 无 | ✅ Spring Security + JWT | 🟢 Java优势 |
| **LLM客户端抽象** | ✅ LLMClient接口，多提供商 | ❌ 未实现 | 🔴 缺失 |
| **Embedder抽象** | ✅ EmbedderClient接口 | ❌ 未实现 | 🔴 缺失 |
| **Cross Encoder重排序** | ✅ 多提供商支持 | ❌ 未实现 | 🔴 缺失 |
| **Telemetry追踪** | ✅ OpenTelemetry集成 | ❌ 未实现 | 🔴 缺失 |

---

## 三、核心功能模块对比

### 3.1 图谱管理 (Graph Management)

| 功能 | Python原版 | Java版本 | 详细对比 |
|------|-----------|---------|---------|
| 创建图谱 | ✅ `create_graph()` | ✅ `POST /graph/create` | ✅ 已实现 |
| 图谱列表 | ✅ `list_graphs()` | ✅ `GET /graph/list` | ✅ 已实现 |
| 图谱详情 | ✅ `get_graph()` | ✅ `GET /graph/{id}` | ✅ 已实现 |
| 更新图谱 | ✅ `update_graph()` | ✅ `PUT /graph/{id}` | ✅ 已实现 |
| 删除图谱 | ✅ `delete_graph()` | ✅ `DELETE /graph/{id}` | ✅ 已实现 |
| **克隆图谱** | ✅ `clone_graph()` | ❌ | 🔴 缺失 |
| **图谱分区 (group_id)** | ✅ 多分区支持 | ❌ 单图谱 | 🔴 缺失 |
| **图谱统计** | ✅ 完整统计 | ✅ `GET /graph/stats` | ✅ 已实现 |

### 3.2 数据导入 (Data Ingestion)

| 功能 | Python原版 | Java版本 | 详细对比 |
|------|-----------|---------|---------|
| **添加Episode (LLM提取)** | ✅ `add_episode()` | ⚠️ 仅创建Episode，无LLM提取 | 🟡 部分实现 |
| 批量添加Episode | ✅ `add_episode_bulk()` | ⚠️ `addDataBatch()` (无LLM) | 🟡 部分实现 |
| 添加JSON数据 | ✅ `source=EpisodeType.json` | ✅ `POST /graph/{id}/data` | ✅ 已实现 |
| 添加消息 | ✅ `source=EpisodeType.message` | ✅ `addMessages()` | ✅ 已实现 |
| 添加文本 | ✅ `source=EpisodeType.text` | ✅ `sourceType="text"` | ✅ 已实现 |
| 添加事实三元组 | ✅ `add_fact_triple()` | ✅ `addFactTriple()` | ✅ 已实现 |
| **直接保存实体节点** | ✅ `save_entity_node()` | ✅ `addEntityNode()` | ✅ 已实现 |
| **直接保存实体边** | ✅ `save_entity_edge()` | ❌ | 🔴 缺失 |
| **LLM实体提取** | ✅ 完整实现 (prompt + structured output) | ❌ TODO | 🔴 缺失 |
| **LLM关系提取** | ✅ 完整实现 | ❌ TODO | 🔴 缺失 |
| **自定义实体类型** | ✅ `entity_types` 参数 (Pydantic) | ⚠️ 本体定义，但未用于提取 | 🟡 部分实现 |
| **自定义边类型** | ✅ `edge_types` 参数 | ⚠️ 本体定义，但未用于提取 | 🟡 部分实现 |
| **自定义提取指令** | ✅ `custom_extraction_instructions` | ❌ | 🔴 缺失 |
| **排除实体类型** | ✅ `excluded_entity_types` | ❌ | 🔴 缺失 |

### 3.3 时序管理 (Temporal Management) ⭐ 核心差异

| 功能 | Python原版 | Java版本 | 重要程度 |
|------|-----------|---------|---------|
| **事实有效性窗口 (valid_at/invalid_at)** | ✅ 自动管理 | ❌ 无时间窗口 | 🔴🔴🔴 **核心缺失** |
| **自动事实失效** | ✅ 新事实覆盖旧事实 | ❌ | 🔴🔴🔴 **核心缺失** |
| **历史查询** | ✅ 查询任意时间点的图谱状态 | ❌ | 🔴🔴🔴 **核心缺失** |
| **Episode时序关联** | ✅ `previous_episode_uuids` | ❌ | 🔴🔴 重要缺失 |
| **Saga (长篇叙事)** | ✅ SagaNode、NEXT_EPISODE边 | ❌ | 🔴🔴 重要缺失 |
| **Episode去重** | ✅ 基于内容哈希 | ❌ | 🔴 缺失 |
| **节点摘要演化** | ✅ 随时间更新summary | ❌ | 🔴 缺失 |

**说明**: 时序管理是Graphiti的核心创新点，Java版本完全缺失此功能。

### 3.4 节点管理 (Node Management)

| 功能 | Python原版 | Java版本 | 详细对比 |
|------|-----------|---------|---------|
| 节点列表 | ✅ `get_nodes()` | ✅ `GET /graph/{id}/nodes` | ✅ 已实现 |
| 节点详情 | ✅ `get_node_by_uuid()` | ✅ `GET /nodes/{uuid}` | ✅ 已实现 |
| 创建节点 | ✅ `EntityNode.save()` | ✅ `createEntityNode()` | ✅ 已实现 |
| 删除节点 | ✅ `node.delete()` | ✅ `DELETE /nodes/{uuid}` | ✅ 已实现 |
| **节点嵌入向量** | ✅ `generate_name_embedding()` | ❌ | 🔴 缺失 |
| **节点全文索引** | ✅ 自动创建 | ⚠️ 需手动创建索引 | 🟡 部分实现 |
| **节点去重** | ✅ `dedupe_nodes_bulk()` | ❌ | 🔴 缺失 |
| **社区节点 (CommunityNode)** | ✅ 自动聚类生成 | ❌ | 🔴 缺失 |

### 3.5 边管理 (Edge Management)

| 功能 | Python原版 | Java版本 | 详细对比 |
|------|-----------|---------|---------|
| 边列表 | ✅ `get_edges()` | ✅ `GET /graph/{id}/edges` | ✅ 已实现 |
| 边详情 | ✅ `get_edge_by_uuid()` | ✅ `GET /edges/{uuid}` | ✅ 已实现 |
| 创建边 | ✅ `EntityEdge.save()` | ✅ `createRelationship()` | ✅ 已实现 |
| 删除边 | ✅ `edge.delete()` | ✅ `DELETE /edges/{uuid}` | ✅ 已实现 |
| **边类型多样性** | ✅ 6种边类型 | ❌ 仅1种 (RELATES_TO) | 🔴 缺失 |
| **边嵌入向量** | ✅ `create_entity_edge_embeddings()` | ❌ | 🔴 缺失 |
| **边去重** | ✅ `dedupe_edges_bulk()` | ❌ | 🔴 缺失 |
| **边失效机制** | ✅ 自动标记invalid_at | ❌ | 🔴 缺失 |

**Python原版边类型**:
1. `EntityEdge` - 实体关系边 (核心)
2. `EpisodicEdge` - Episode与节点的关联边
3. `CommunityEdge` - 社区节点间的边
4. `HasEpisodeEdge` - Saga拥有Episode的边
5. `NextEpisodeEdge` - Episode时序连接边

**Java版本边类型**:
1. `RELATES_TO` - 唯一的关系类型 (Neo4j中使用)

### 3.6 Episode (事件) 管理

| 功能 | Python原版 | Java版本 | 详细对比 |
|------|-----------|---------|---------|
| Episode列表 | ✅ `get_episodes()` | ✅ `GET /graph/{id}/episodes` | ✅ 已实现 |
| Episode详情 | ✅ `get_episode_by_uuid()` | ✅ `GET /episodes/{uuid}` | ✅ 已实现 |
| 创建Episode | ✅ `add_episode()` | ⚠️ 简化版 (无LLM) | 🟡 部分实现 |
| 删除Episode | ✅ `delete_episode()` | ✅ `DELETE /episodes/{uuid}` | ✅ 已实现 |
| **Episode提及查询** | ✅ `get_episode_mentions()` | ✅ 已实现 | ✅ 已实现 |
| **Episode时序查询** | ✅ `retrieve_episodes(last_n)` | ❌ | 🔴 缺失 |
| **Episode内容搜索** | ✅ BM25全文搜索 | ⚠️ 基础实现 | 🟡 部分实现 |

### 3.7 本体管理 (Ontology Management)

| 功能 | Python原版 | Java版本 | 详细对比 |
|------|-----------|---------|---------|
| 设置本体 | ✅ `set_ontology()` | ✅ `PUT /ontology` | ✅ 已实现 |
| 获取本体 | ✅ `list_ontology()` | ✅ `GET /ontology` | ✅ 已实现 |
| **Pydantic模型定义** | ✅ 运行时验证 | ❌ JSON格式 | 🟡 简化实现 |
| **实体类型字段定义** | ✅ fields (JSON Schema) | ❌ 仅名称和描述 | 🟡 简化实现 |
| **边类型字段定义** | ✅ fields + source_types + target_types | ❌ 仅名称和描述 | 🟡 简化实现 |
| **本体持久化** | ✅ ontology_store (多种后端) | ✅ MySQL存储 | ✅ 已实现 |
| **自定义指令存储** | ✅ instruction_store | ❌ | 🔴 缺失 |

### 3.8 检索服务 (Search & Retrieval) 🔴 重大差异

| 功能 | Python原版 | Java版本 | 详细对比 |
|------|-----------|---------|---------|
| **混合检索 (Hybrid Search)** | ✅ BM25 + 语义 + 图遍历 | ❌ | 🔴🔴🔴 **核心缺失** |
| **语义搜索 (向量)** | ✅ cosine_similarity | ❌ 无向量支持 | 🔴🔴🔴 **核心缺失** |
| **全文搜索 (BM25)** | ✅ Neo4j全文索引 | ⚠️ 基础LIKE查询 | 🟡 简化实现 |
| **图遍历搜索 (BFS)** | ✅ bfs搜索 | ❌ | 🔴 缺失 |
| **RRF重排序** | ✅ Reciprocal Rank Fusion | ❌ | 🔴 缺失 |
| **MMR重排序** | ✅ Maximal Marginal Relevance | ❌ | 🔴 缺失 |
| **Cross Encoder重排序** | ✅ BGE/OpenAI/Gemini | ❌ | 🔴 缺失 |
| **社区搜索** | ✅ CommunitySearchConfig | ❌ | 🔴 缺失 |
| **Episode搜索** | ✅ EpisodeSearchConfig | ❌ | 🔴 缺失 |
| **预置检索配方** | ✅ 6种SearchConfig | ❌ | 🔴 缺失 |
| **节点中心搜索** | ✅ `center_node_uuid` | ❌ | 🔴 缺失 |
| **BFS起点搜索** | ✅ `bfs_origin_node_uuids` | ❌ | 🔴 缺失 |
| **搜索过滤器** | ✅ SearchFilters | ❌ | 🔴 缺失 |
| **检索结果封装** | ✅ SearchResults (nodes+edges+episodes+communities) | ❌ 仅返回边 | 🔴 缺失 |

**Python原版检索配置示例**:
```python
COMBINED_HYBRID_SEARCH_CROSS_ENCODER = SearchConfig(
    edge_config=EdgeSearchConfig(
        search_methods=[
            EdgeSearchMethod.bm25,
            EdgeSearchMethod.cosine_similarity,
            EdgeSearchMethod.bfs,
        ],
        reranker=EdgeReranker.cross_encoder,
    ),
    node_config=NodeSearchConfig(...),
    episode_config=EpisodeSearchConfig(...),
    community_config=CommunitySearchConfig(...),
)
```

**Java版本当前实现**:
```java
// SearchController.java - 仅支持简单的全文搜索
@PostMapping("/nodes")
public CommonResult<List<NodeSearchRespVO>> searchNodes(...) {
    // 简单的LIKE查询
}
```

### 3.9 社区发现 (Community Detection)

| 功能 | Python原版 | Java版本 | 详细对比 |
|------|-----------|---------|---------|
| **构建社区** | ✅ `build_communities()` | ❌ 前端有按钮，后端未实现 | 🔴 缺失 |
| **社区聚类算法** | ✅ 图聚类算法 | ❌ | 🔴 缺失 |
| **社区摘要生成** | ✅ LLM生成社区摘要 | ❌ | 🔴 缺失 |
| **社区搜索** | ✅ CommunitySearchConfig | ❌ | 🔴 缺失 |
| **社区节点 (CommunityNode)** | ✅ 自动创建 | ❌ | 🔴 缺失 |
| **社区边 (CommunityEdge)** | ✅ 自动创建 | ❌ | 🔴 缺失 |

### 3.10 数据导出 (Data Export)

| 功能 | Python原版 | Java版本 | 详细对比 |
|------|-----------|---------|---------|
| 导出图谱 | ❌ 无直接导出 | ⚠️ `GET /graph/{id}/export` (前端定义) | 🟡 未实现 |
| **图谱克隆** | ✅ `clone_graph()` | ❌ | 🔴 缺失 |

---

## 四、API接口对比

### 4.1 Python原版 Server API (FastAPI)

| 路由 | 方法 | 功能 | Java版本对应 |
|------|------|------|-------------|
| `/graph` | POST | 创建图谱 | ✅ `POST /api/v1/graph/create` |
| `/graph/list` | GET | 图谱列表 | ✅ `GET /api/v1/graph/list` |
| `/graph/{graph_id}` | GET | 图谱详情 | ✅ `GET /api/v1/graph/{id}` |
| `/graph/{graph_id}` | PATCH | 更新图谱 | ✅ `PUT /api/v1/graph/{id}` |
| `/graph/{graph_id}` | DELETE | 删除图谱 | ✅ `DELETE /api/v1/graph/{id}` |
| `/graph/clone` | POST | 克隆图谱 | ❌ 缺失 |
| `/graph/ontology` | PUT | 设置本体 | ✅ `PUT /api/v1/ontology` |
| `/graph/ontology` | GET | 获取本体 | ✅ `GET /api/v1/ontology` |
| `/graph/data` | POST | 添加数据 | ✅ `POST /api/v1/graph/{id}/data` |
| `/graph/data/batch` | POST | 批量添加 | ✅ `POST /api/v1/import/data/batch` |
| `/graph/fact-triple` | POST | 添加三元组 | ✅ `POST /api/v1/import/fact-triple` |
| `/graph/search` | POST | 搜索图谱 | ⚠️ `POST /api/v1/search/*` (简化) |
| `/nodes` | GET | 节点列表 | ✅ `GET /api/v1/graph/{id}/nodes` |
| `/nodes/{uuid}` | GET | 节点详情 | ✅ `GET /api/v1/nodes/{uuid}` |
| `/nodes/{uuid}` | DELETE | 删除节点 | ✅ `DELETE /api/v1/nodes/{uuid}` |
| `/nodes/{uuid}/edges` | GET | 节点关联边 | ✅ `GET /api/v1/nodes/{uuid}/edges` |
| `/edges` | GET | 边列表 | ✅ `GET /api/v1/graph/{id}/edges` |
| `/edges/{uuid}` | GET | 边详情 | ✅ `GET /api/v1/edges/{uuid}` |
| `/edges/{uuid}` | DELETE | 删除边 | ✅ `DELETE /api/v1/edges/{uuid}` |
| `/episodes/{group_id}` | GET | Episode列表 | ✅ `GET /api/v1/graph/{id}/episodes` |
| `/episodes/{uuid}` | GET | Episode详情 | ✅ `GET /api/v1/episodes/{uuid}` |
| `/episodes/{uuid}` | DELETE | 删除Episode | ✅ `DELETE /api/v1/episodes/{uuid}` |
| `/episodes/{uuid}/mentions` | GET | Episode提及 | ✅ `GET /api/v1/episodes/{uuid}/mentions` |
| `/search` | POST | 混合检索 | ❌ 缺失 |
| `/entity-edge/{uuid}` | GET | 实体边详情 | ⚠️ 同 `/edges/{uuid}` |
| `/get-memory` | POST | 获取记忆 | ❌ 缺失 |
| `/instruction` | POST | 自定义指令 | ❌ 缺失 |

### 4.2 Java版本独有API

| 路由 | 方法 | 功能 | Python原版对应 |
|------|------|------|---------------|
| `/api/v1/auth/login` | POST | 用户登录 | ❌ 无用户系统 |
| `/api/v1/auth/logout` | POST | 用户登出 | ❌ 无用户系统 |
| `/api/v1/graph/stats` | GET | 图谱统计 | ❌ 无此接口 |
| `/api/v1/system/users/*` | * | 用户管理 | ❌ 无用户系统 |
| `/api/v1/system/roles/*` | * | 角色管理 | ❌ 无用户系统 |
| `/api/v1/system/menus/*` | * | 菜单管理 | ❌ 无用户系统 |

---

## 五、数据模型对比

### 5.1 节点类型 (Node Types)

| 节点类型 | Python原版 | Java版本 | 说明 |
|---------|-----------|---------|------|
| **EntityNode** | ✅ | ✅ | 实体节点 (核心) |
| **EpisodicNode** | ✅ | ✅ | Episode节点 |
| **CommunityNode** | ✅ | ❌ | 社区节点 |
| **SagaNode** | ✅ | ❌ | 长篇叙事节点 |

**Python原版EntityNode属性**:
```python
class EntityNode(Node):
    summary: str                    # 节点摘要 (会随时间演化)
    details: dict[str, str]         # 详细信息
    created_at: datetime
    valid_at: datetime              # 有效时间
    invalid_at: datetime | None     # 失效时间
    name_embedding: list[float]     # 名称嵌入向量
    summary_embedding: list[float]  # 摘要嵌入向量
    attributes: dict                # 动态属性
```

**Java版本EntityNode属性**:
```java
// GraphNeo4jService.createEntityNode()
- group_id (String)
- uuid (String)
- name (String)
- type (String)
- properties (Map)
// 缺失: summary, valid_at, invalid_at, embeddings
```

### 5.2 边类型 (Edge Types)

| 边类型 | Python原版 | Java版本 | 说明 |
|-------|-----------|---------|------|
| **EntityEdge** | ✅ | ⚠️ (RELATES_TO) | 实体关系边 |
| **EpisodicEdge** | ✅ | ❌ | Episode关联边 |
| **CommunityEdge** | ✅ | ❌ | 社区关系边 |
| **HasEpisodeEdge** | ✅ | ❌ | Saga拥有Episode |
| **NextEpisodeEdge** | ✅ | ❌ | Episode时序连接 |
| **MentionsEdge** | ✅ | ❌ | Episode提及节点 |

**Python原版EntityEdge属性**:
```python
class EntityEdge(Edge):
    name: str                       # 关系名称
    fact: str                       # 事实描述
    valid_at: datetime              # 有效时间
    invalid_at: datetime | None     # 失效时间
    facts: list[Fact]               # 事实列表 (时序)
    embedding: list[float]          # 事实嵌入向量
    attributes: dict                # 动态属性
```

**Java版本关系边属性**:
```java
// GraphNeo4jService.createRelationship()
- group_id (String)
- uuid (String)
- type (String)
- properties (Map)
// 缺失: name, fact, valid_at, invalid_at, embedding
```

### 5.3 Episode模型

**Python原版**:
```python
class EpisodicNode(Node):
    content: str                    # 内容
    source_description: str         # 来源描述
    source: EpisodeType             # 来源类型
    valid_at: datetime              # 有效时间
    created_at: datetime
    processed: bool                 # 是否已处理
```

**Java版本**:
```java
// GraphNeo4jService.createEpisode()
- group_id (String)
- uuid (String)
- name (String)
- source (String)
- source_description (String)
- content (String)
- created_at (Long)
- valid_at (Long)
- processed (Boolean)
// ✅ 基本属性已实现
```

---

## 六、LLM与AI能力对比

### 6.1 LLM集成

| 能力 | Python原版 | Java版本 | 状态 |
|------|-----------|---------|------|
| **多LLM提供商** | ✅ OpenAI/Gemini/Anthropic/Groq | ❌ | 🔴 缺失 |
| **LLM客户端抽象** | ✅ LLMClient接口 | ❌ | 🔴 缺失 |
| **结构化输出** | ✅ 使用Pydantic + OpenAI structured output | ❌ | 🔴 缺失 |
| **实体提取Prompt** | ✅ 13个prompt文件 | ❌ | 🔴 缺失 |
| **关系提取Prompt** | ✅ 完整prompt工程 | ❌ | 🔴 缺失 |
| **社区摘要Prompt** | ✅ 自动生成社区摘要 | ❌ | 🔴 缺失 |
| **节点摘要演化** | ✅ 随时间更新摘要 | ❌ | 🔴 缺失 |

### 6.2 嵌入向量 (Embeddings)

| 能力 | Python原版 | Java版本 | 状态 |
|------|-----------|---------|------|
| **多Embedder提供商** | ✅ OpenAI/Voyage/SentenceTransformers | ❌ | 🔴 缺失 |
| **节点名称嵌入** | ✅ 自动生成 | ❌ | 🔴 缺失 |
| **节点摘要嵌入** | ✅ 自动生成 | ❌ | 🔴 缺失 |
| **边事实嵌入** | ✅ 自动生成 | ❌ | 🔴 缺失 |
| **向量相似度搜索** | ✅ cosine_similarity | ❌ | 🔴 缺失 |

### 6.3 重排序 (Reranking)

| 能力 | Python原版 | Java版本 | 状态 |
|------|-----------|---------|------|
| **RRF重排序** | ✅ Reciprocal Rank Fusion | ❌ | 🔴 缺失 |
| **MMR重排序** | ✅ Maximal Marginal Relevance | ❌ | 🔴 缺失 |
| **Cross Encoder** | ✅ BGE/OpenAI/Gemini | ❌ | 🔴 缺失 |

---

## 七、MCP Server对比

| 特性 | Python原版 | Java版本 | 状态 |
|------|-----------|---------|------|
| **MCP协议支持** | ✅ 完整实现 | ❌ | 🔴 缺失 |
| **HTTP传输** | ✅ `/mcp/` 端点 | ❌ | 🔴 缺失 |
| **stdio传输** | ✅ Claude Desktop等 | ❌ | 🔴 缺失 |
| **MCP工具暴露** | ✅ 12个工具 | ❌ | 🔴 缺失 |
| **Docker部署** | ✅ docker-compose | ❌ | 🔴 缺失 |

---

## 八、高级功能对比

### 8.1 时序特性 (Temporal Features) ⭐⭐⭐

| 特性 | Python原版 | Java版本 | 重要程度 |
|------|-----------|---------|---------|
| **双时间追踪** | ✅ valid_at + invalid_at | ❌ | 🔴🔴🔴 核心 |
| **自动事实失效** | ✅ 新事实自动覆盖旧事实 | ❌ | 🔴🔴🔴 核心 |
| **历史状态查询** | ✅ 查询任意时间点 | ❌ | 🔴🔴🔴 核心 |
| **事实版本链** | ✅ facts列表 | ❌ | 🔴🔴 重要 |
| **节点摘要演化** | ✅ 随时间更新 | ❌ | 🔴🔴 重要 |
| **Episode时序链** | ✅ NEXT_EPISODE边 | ❌ | 🔴🔴 重要 |
| **Saga支持** | ✅ 长篇叙事管理 | ❌ | 🔴 缺失 |

### 8.2 数据质量

| 特性 | Python原版 | Java版本 | 状态 |
|------|-----------|---------|------|
| **节点去重** | ✅ 基于名称相似度 | ❌ | 🔴 缺失 |
| **边去重** | ✅ 基于关系相似度 | ❌ | 🔴 缺失 |
| **实体解析** | ✅ 合并相同实体 | ❌ | 🔴 缺失 |
| **矛盾处理** | ✅ 自动标记矛盾事实 | ❌ | 🔴 缺失 |

### 8.3 性能与扩展性

| 特性 | Python原版 | Java版本 | 状态 |
|------|-----------|---------|------|
| **并行处理** | ✅ semaphore_gather | ❌ | 🔴 缺失 |
| **批量操作** | ✅ bulk_utils | ⚠️ 基础实现 | 🟡 部分 |
| **连接池** | ✅ Neo4j连接池 | ✅ 已实现 | ✅ 已实现 |
| **缓存** | ❌ | ✅ Redis集成 | 🟢 Java优势 |
| **分库支持** | ✅ group_id作为database | ❌ | 🔴 缺失 |

### 8.4 可观测性

| 特性 | Python原版 | Java版本 | 状态 |
|------|-----------|---------|------|
| **OpenTelemetry** | ✅ 完整tracing | ❌ | 🔴 缺失 |
| **Token追踪** | ✅ LLM token使用统计 | ❌ | 🔴 缺失 |
| **性能指标** | ✅ span追踪 | ❌ | 🔴 缺失 |
| **日志** | ✅ structured logging | ✅ Slf4j | ✅ 已实现 |

---

## 九、数据库支持对比

| 数据库类型 | Python原版 | Java版本 | 说明 |
|-----------|-----------|---------|------|
| **Neo4j** | ✅ 5.26+ | ✅ 5.26.0 | ✅ 已实现 |
| **FalkorDB** | ✅ 1.1.2+ | ❌ | 🔴 缺失 |
| **Kuzu** | ✅ 0.11.2+ | ❌ | 🔴 缺失 |
| **Amazon Neptune** | ✅ Database + Analytics | ❌ | 🔴 缺失 |
| **MySQL** | ❌ | ✅ 元数据存储 | 🟢 Java优势 |
| **PostgreSQL** | ❌ | ✅ 元数据存储 (可选) | 🟢 Java优势 |
| **Redis** | ❌ | ✅ 缓存 | 🟢 Java优势 |

---

## 十、graphiti-java 缺失功能清单 (按优先级排序)

### 🔴🔴🔴 P0 - 核心功能缺失 (Graphiti的核心价值)

1. **时序事实管理**
   - 事实有效性窗口 (valid_at / invalid_at)
   - 自动事实失效机制
   - 历史状态查询
   - 事实版本链

2. **LLM实体和关系提取**
   - 实体提取Prompt工程
   - 关系提取Prompt工程
   - 结构化输出解析
   - 自定义实体类型提取

3. **混合检索系统**
   - 向量语义搜索 (cosine_similarity)
   - 图遍历搜索 (BFS)
   - RRF/MMR重排序
   - Cross Encoder重排序
   - 预置检索配方 (SearchConfig)

4. **嵌入向量系统**
   - EmbedderClient接口
   - 节点名称/摘要嵌入
   - 边事实嵌入
   - 向量相似度计算

### 🔴🔴 P1 - 重要功能缺失

5. **社区发现与聚类**
   - 社区构建算法
   - 社区摘要生成
   - 社区搜索

6. **多数据库驱动支持**
   - GraphDriver抽象接口
   - FalkorDB驱动
   - Kuzu驱动
   - Amazon Neptune驱动

7. **Saga (长篇叙事) 管理**
   - SagaNode
   - NEXT_EPISODE边
   - Episode时序链

8. **数据质量保障**
   - 节点去重 (基于相似度)
   - 边去重
   - 实体解析
   - 矛盾检测

9. **多LLM提供商支持**
   - LLMClient接口
   - OpenAI/Gemini/Anthropic/Groq集成
   - 结构化输出

### 🔴 P2 - 功能完善

10. **本体系统增强**
    - Pydantic模型验证
    - 实体类型字段定义
    - 边类型字段定义
    - 自定义指令存储

11. **边类型多样化**
    - EpisodicEdge
    - CommunityEdge
    - HasEpisodeEdge
    - NextEpisodeEdge

12. **节点属性完善**
    - summary字段 (会演化)
    - details字段
    - attributes动态属性

13. **边属性完善**
    - name字段
    - fact字段
    - facts列表 (时序)
    - attributes动态属性

14. **图谱克隆**
    - clone_graph()

15. **数据导出**
    - 导出为JSON/CSV

### 🟡 P3 - 增强功能

16. **可观测性**
    - OpenTelemetry集成
    - Token使用统计
    - 性能span追踪

17. **性能优化**
    - 并行处理 (async/await)
    - 批量操作优化
    - 分库支持 (group_id)

18. **MCP Server**
    - MCP协议实现
    - HTTP/stdio传输
    - Docker部署

19. **Episode高级功能**
    - Episode时序查询
    - Episode去重
    - 自定义提取指令

20. **搜索增强**
    - 搜索过滤器 (SearchFilters)
    - 节点中心搜索
    - BFS起点搜索
    - 社区搜索
    - Episode搜索

---

## 十一、graphiti-java 的优势

1. **企业级架构**
   - ✅ 完整的Maven多模块设计
   - ✅ Spring Boot生态集成
   - ✅ 类型安全 (Java强类型)

2. **用户权限系统**
   - ✅ Spring Security + JWT
   - ✅ 用户/角色/权限管理
   - ✅ 菜单权限控制

3. **前端控制台**
   - ✅ Vue 3 + TypeScript
   - ✅ Ant Design组件库
   - ✅ 图谱可视化 (ECharts)
   - ✅ 暗色主题UI

4. **缓存系统**
   - ✅ Redis集成
   - ✅ 分布式锁 (Redisson)
   - ✅ 会话管理

5. **数据持久化**
   - ✅ MyBatis-Plus (MySQL/PostgreSQL)
   - ✅ 元数据关系型存储
   - ✅ Druid连接池

6. **API文档**
   - ✅ SpringDoc (OpenAPI 3.0)
   - ✅ Swagger UI
   - ✅ 完整的API注解

---

## 十二、实施建议

### 阶段一：核心能力补齐 (P0)

**预估工作量**: 4-6周

1. **实现LLM提取** (2周)
   - 集成Spring AI
   - 实现实体提取Prompt
   - 实现关系提取Prompt
   - 结构化输出解析

2. **实现时序管理** (2周)
   - 添加valid_at/invalid_at字段
   - 实现自动事实失效逻辑
   - 实现历史查询接口

3. **实现混合检索** (2周)
   - 集成向量数据库或Neo4j向量索引
   - 实现BM25全文搜索
   - 实现RRF重排序
   - 实现基础BFS搜索

### 阶段二：重要功能 (P1)

**预估工作量**: 4-5周

4. **社区发现** (1周)
   - 实现图聚类算法
   - LLM生成社区摘要

5. **多数据库驱动** (2周)
   - 设计GraphDriver接口
   - 实现FalkorDB驱动

6. **数据质量** (1周)
   - 节点去重
   - 边去重

7. **Saga管理** (1周)
   - SagaNode模型
   - Episode时序链

### 阶段三：功能完善 (P2)

**预估工作量**: 3-4周

8. **本体增强** (1周)
9. **边类型多样化** (1周)
10. **图谱克隆/导出** (1周)
11. **测试与优化** (1周)

---

## 十三、总结

### 核心差距

**graphiti-java** 目前是一个**知识图谱CRUD系统**，实现了基础的图谱管理、节点边操作和数据导入功能。

**graphiti (Python原版)** 是一个**时序上下文图谱引擎**，核心价值在于：
1. ⏰ **时序事实管理** - 追踪事实的有效期，自动失效旧事实
2. 🤖 **LLM自动提取** - 从非结构化文本自动提取实体和关系
3. 🔍 **混合检索系统** - 向量+全文+图遍历的多维检索
4. 🧠 **嵌入向量支持** - 语义相似度计算

### 定位差异

| 维度 | graphiti-java | graphiti (Python) |
|------|--------------|------------------|
| **当前定位** | 图谱管理后台 | AI Agent记忆引擎 |
| **核心价值** | 图谱可视化+管理 | 时序上下文+自动提取 |
| **适用场景** | 知识图谱展示 | AI Agent、智能客服、推荐系统 |
| **技术难度** | ⭐⭐ (CRUD) | ⭐⭐⭐⭐⭐ (AI+时序) |

### 建议

如果目标是**复刻Graphiti的核心能力**，需要优先实现P0级别的4个核心功能。

如果目标是**构建知识图谱管理平台**，当前实现已满足基本需求，可继续完善UI和用户体验。

---

**文档生成时间**: 2026-05-10  
**分析基于**: graphiti-java (commit: 最新) vs graphiti (getzep/graphiti main分支)
