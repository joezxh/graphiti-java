# 使用 Graphiti 构建 AI 会话上下文记忆系统

> 本文档面向使用 **ontograph-java** 项目的 Java 开发者，介绍如何将 Graphiti 时序上下文图（Temporal Context Graph）用作 AI 会话的持久化记忆系统。
>
> 文档同时参考了 [Python 原版 Graphiti](https://github.com/getzep/graphiti) 的设计理念和 `ontograph-java` 项目的现有 Java API，确保概念准确且代码可直接落地。

---

## 目录

1. [核心概念](#1-核心概念)
2. [Java 项目中的核心 API 概览](#2-java-项目中的核心-api-概览)
3. [实现流程](#3-实现流程)
4. [代码示例](#4-代码示例)
5. [完整应用范例：智能客服聊天机器人](#5-完整应用范例智能客服聊天机器人)
6. [最佳实践与注意事项](#6-最佳实践与注意事项)

---

## 1. 核心概念

### 1.1 什么是时序上下文图（Temporal Context Graph）

在传统的 AI 对话系统中，"记忆"通常通过以下两种方式实现：

- **滑动窗口**：仅保留最近 N 轮对话，超过窗口的历史完全丢失。
- **向量检索（RAG）**：将对话历史切分为文本块，存入向量数据库，需要时通过语义相似度检索相关内容。

这两种方式都有明显缺陷：滑动窗口丢失了远期但重要的信息；RAG 虽然能检索语义相关内容，但丢失了**时间顺序**和**实体间的结构化关系**——例如，你无法轻易回答"用户在三分钟前提到的那个品牌叫什么"这类需要结合时间和实体关联的问题。

**Graphiti 的核心创新**在于：它在向量检索之上叠加了一层**时序图谱结构**。每个输入的信息单元被称为一个 **Episode**（记忆片段），Episode 之间通过 `NEXT` 边维护严格的时间序列；同时，系统会自动从 Episode 中提取**实体**（Entity）和**关系**（Fact），构建语义网络。

这形成了独特的**双层结构**：

```
┌─────────────────────────────────────────────────────────────┐
│                    时序层（Episodic Timeline）                 │
│   Episode_1 ──NEXT──> Episode_2 ──NEXT──> Episode_3 ...     │
│     ↑                    ↑                    ↑              │
│   MENTIONS            MENTIONS             MENTIONS          │
│     ↓                    ↓                    ↓              │
├─────────────────────────────────────────────────────────────┤
│                   语义层（Entity-Relation Graph）              │
│                                                            │
│     [用户A] ──INTERESTED_IN──> [产品X]                      │
│        │                            ↑                      │
│        └── PREFERS ────────────────> [颜色: 黑色]            │
│                                                            │
│     [产品X] ──HAS_FEATURE ────────> [透气材质]              │
│                                                            │
└─────────────────────────────────────────────────────────────┘
```

**时序层**保证你可以按时间回溯对话历史；**语义层**让你可以通过实体和关系进行关联检索。两者结合，使得 Graphiti 能够同时回答"用户之前说过什么"（时序）和"用户喜欢什么类型的产品"（语义）这两类问题。

### 1.2 记忆的基本单元：Episode

**Episode**（`EpisodicNode`）是 Graphiti 中最基本的信息单元，代表一次独立的信息输入。在 AI 会话场景中，通常每一轮对话（用户消息或 AI 回复）都会被存储为一个 Episode。

Graphiti 支持多种 Episode 来源类型（`source` 字段）：

| 来源类型 | 说明 | 在 AI 记忆场景中的用途 |
|---------|------|---------------------|
| `message` | 对话消息，推荐格式 `"actor: content"`（如 `"user: 你好，我想买双鞋"`） | **首选**。保留发言者身份，便于后续实体归属分析 |
| `text` | 纯文本片段（如文章段落、播客转录） | 导入背景知识文档 |
| `json` | 结构化数据 | 导入产品目录、用户资料等结构化信息 |
| `fact_triple` | 预提取的事实三元组 | 直接注入已知事实，跳过自动提取 |

**在 AI 会话记忆场景中，强烈建议使用 `message` 类型**，原因有二：

1. **发言者身份保留**：`"user: 我的脚比较宽"` 与 `"assistant: 推荐您选择宽楦版本"` 可以清晰区分信息来源。
2. **实体归属**：后续自动提取的实体（如"宽楦版本"）可以追溯到具体是哪一轮对话提到的，便于调试和审计。

### 1.3 实体与关系在记忆中的作用

当 Episode 被添加到 Graphiti 后，系统会自动触发以下流水线操作：

1. **实体提取（Entity Extraction）**：从 Episode 内容中识别人名、地名、组织、产品、概念等，创建 `EntityNode`。
2. **关系提取（Edge Extraction）**：识别实体之间的关系，创建 `EntityEdge`（也称为 **Fact**）。
3. **去重（Deduplication）**：如果提取的实体已存在于图谱中，会合并到现有节点并更新 `summary` 和 `attributes`。
4. **Embedding 生成**：为实体和关系生成向量 Embedding，用于语义检索。

这些操作会在后台**异步执行**，添加 Episode 的调用本身不会等待提取完成。

#### 核心数据模型

| 元素 | 类型 | 关键属性 | 作用 |
|-----|------|---------|------|
| `EpisodicNode` | 节点 | `uuid`, `name`, `content`, `source`, `created_at` | 存储原始信息单元（对话消息） |
| `EntityNode` | 节点 | `uuid`, `name`, `labels`, `summary`, `attributes` | 从 Episode 中提取的实体 |
| `EntityEdge` (Fact) | 边 | `uuid`, `fact`, `valid_at`, `invalid_at` | 实体间的关系，用自然语言描述 |
| `EpisodicEdge` | 边 | - | `MENTIONS` 关系，连接 Episode 与提及的 Entity |
| `NextEpisodeEdge` | 边 | - | `NEXT` 关系，维护 Episode 的时间序列 |

**时态事实（Temporal Facts）** 是 Graphiti 的一个独特能力。关系的 `valid_at` 和 `invalid_at` 字段支持表达会随时间变化的事实。例如：

- `"Kamala Harris 曾任 California Attorney General"` —— `valid_at: 2011-01-03`, `invalid_at: 2017-01-03`
- `"用户当前的预算偏好是 500 元以下"` —— `valid_at: 2026-05-13`，若用户后续改口，旧事实会被标记 `invalid_at`

### 1.4 记忆检索机制

Graphiti 提供三种互补的检索方式：

#### 混合检索（Hybrid Search）

结合 **BM25 全文检索** 和 **向量语义检索**，通过 RRF（Reciprocal Rank Fusion）融合两种排序结果。这使得检索既能匹配精确关键词（如产品型号），又能理解语义相似性（如"透气"和"通风"）。

在 `ontograph-java` 中，`SearchService.doSearch()` 实现了这一逻辑，支持通过 `SearchConfigVO` 调整 BM25 与向量检索的权重：

```java
SearchConfigVO config = new SearchConfigVO();
config.setMode("hybrid");
config.setBm25Weight(0.3);
config.setVectorWeight(0.7);
```

#### 中心节点重排序（Center Node Reranking）

在基础搜索结果之上，以某个特定实体节点为中心，按图距离（BFS 遍历深度）对结果进行重排序。距离中心节点越近的事实，排名越靠前。

**典型应用场景**：在智能客服中，以**当前用户**的 `EntityNode` 为中心节点进行搜索，确保优先返回与该用户相关的记忆（偏好、历史订单），而非其他用户的记录或通用产品信息。

#### 时间窗口检索

通过 `NextEpisodeEdge` 构成的时序链，可以回溯最近的 N 个 Episode。这在需要严格按时间顺序回顾对话时非常有用，例如："用户在上一句话中提到的尺码是多少？"

---

## 2. Java 项目中的核心 API 概览

### 2.1 Python 原版 API 与 Java 版对照

如果你熟悉 Python 原版的 Graphiti API，以下对照表可以帮助你快速迁移到 Java 版本：

| Python 原版 API | Java 版对应方法 | 所在 Service |
|---------------|--------------|-----------|
| `Graphiti(neo4j_uri, user, password)` | Spring Boot 自动配置 + `GraphNeo4jService` | 配置层 / `GraphNeo4jService` |
| `await graphiti.add_episode(...)` | `episodeService.createEpisode(graphId, episodeData)` | `EpisodeService` |
| `await graphiti.search(query)` | `searchService.searchGraph(graphId, reqVO)` | `SearchService` |
| `await graphiti._search(query, config)` | `searchService.search(reqVO)` | `SearchService` |
| `await graphiti.close()` | Spring 容器销毁时自动释放 | 生命周期管理 |
| **对话记忆专用** | `searchService.getMemory(reqVO)` | `SearchService` |

**特别说明**：Java 版的 `SearchService.getMemory()` 是专门为 AI 会话记忆场景封装的方法，在 Python 原版中没有直接等价物（需要手动拼接 `search()` 结果和 Prompt）。

### 2.2 关键 VO 和数据结构

#### GetMemoryReqVO / GetMemoryRespVO

`getMemory()` 是 AI 记忆场景的核心方法，其请求和响应结构如下：

```java
// 请求：传入对话历史，返回相关记忆
@Data
public class GetMemoryReqVO implements Serializable {
    private List<MessageQueryVO> messages;  // 对话历史消息列表
    private List<String> groupIds;          // 限定的图谱 ID 列表
    private Integer maxFacts = 10;          // 最大返回事实数
}

// 响应：包含事实、实体和拼接好的上下文字符串
@Data
public class GetMemoryRespVO implements Serializable {
    private List<FactResultVO> facts;       // 相关事实（边）列表
    private List<NodeResultVO> entities;    // 相关实体（节点）列表
    private String context;                 // 拼接好的上下文字符串
}
```

`getMemory()` 的内部实现逻辑（位于 `SearchServiceImpl`）：

1. 从 `messages` 中提取**最后一条非 system 消息**的内容作为查询文本。
2. 对该查询文本执行**混合检索**（hybrid search）。
3. 将检索到的事实拼接为如下格式的字符串：
   ```
   相关知识：
   - 事实描述 1
   - 事实描述 2
   - ...
   ```
4. 返回 `GetMemoryRespVO`，其中 `context` 字段可以直接注入到 AI 的 System Prompt 中。

#### SearchQueryReqVO / SearchResultsRespVO

用于通用搜索场景：

```java
@Data
public class SearchQueryReqVO implements Serializable {
    private String query;              // 搜索查询文本（必填）
    private List<String> groupIds;     // 限定的图谱 ID
    private Integer maxFacts = 10;     // 最大返回事实数
    private Boolean enableRerank = true; // 是否启用重排序
    private SearchConfigVO config;     // 搜索配置（模式、权重等）
}

@Data
public class SearchResultsRespVO implements Serializable {
    private List<FactResultVO> facts;   // 事实列表
    private int totalCount;
    private List<NodeResultVO> nodes;   // 实体节点列表
    private int nodeCount;
}
```

#### FactResultVO

单条事实（边）的完整信息：

```java
@Data
public class FactResultVO implements Serializable {
    private String uuid;                // 边 UUID
    private String name;
    private String fact;                // 事实自然语言描述
    private String sourceNodeUuid;      // 源实体 UUID
    private String targetNodeUuid;      // 目标实体 UUID
    private String groupId;             // 所属图谱 ID
    private String createdAt;
    private String validAt;             // 事实生效时间
    private String invalidAt;           // 事实失效时间（支持时态更新）
    private Double score;               // 搜索得分
    private Double relevance;           // 相关性得分（0-1）
    private List<String> episodes;      // 关联的 Episode UUID 列表
}
```

#### MessageQueryVO

用于在 `getMemory()` 中传递对话历史：

```java
@Data
public class MessageQueryVO implements Serializable {
    private String role;      // 角色：system / user / assistant
    private String content;   // 消息内容
}
```

---

## 3. 实现流程

### 3.1 初始化与连接

ontograph-java 项目基于 Spring Boot 构建，推荐使用自动配置方式接入 Neo4j。

#### 方式一：Spring Boot 自动配置（推荐）

在 `application.yml` 中配置 Neo4j 连接信息：

```yaml
spring:
  neo4j:
    uri: bolt://localhost:7687
    authentication:
      username: neo4j
      password: your-password
```

确保项目中已引入依赖（在 `ontograph-module-core/pom.xml` 中已包含）：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-neo4j</artifactId>
</dependency>
```

Service 层通过构造函数注入自动获取依赖：

```java
@Service
@RequiredArgsConstructor
public class ChatMemoryService {
    private final EpisodeService episodeService;
    private final SearchService searchService;
    private final GraphNeo4jService graphNeo4jService;
}
```

#### 方式二：手动配置（非 Spring 环境）

```java
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;

Driver driver = GraphDatabase.driver(
    "bolt://localhost:7687",
    AuthTokens.basic("neo4j", "your-password")
);
// 使用 driver 执行 Cypher 查询
```

### 3.2 添加记忆片段（Episodes）

#### Service 层方式（推荐）

```java
Map<String, Object> episodeData = new HashMap<>();
episodeData.put("name", "用户询问产品");
episodeData.put("source", "message");              // 来源类型：message
episodeData.put("sourceDescription", "客服对话");
episodeData.put("content", "user: 你好，我想买一双跑步鞋，平时主要在公园跑");
episodeData.put("properties", new HashMap<>());

EpisodeInfoRespVO episode = episodeService.createEpisode(graphId, episodeData);
```

`createEpisode()` 的底层实现（`EpisodeServiceImpl`）会调用 `GraphNeo4jService.createEpisode()` 在 Neo4j 中创建节点：

```cypher
CREATE (e:Episode {
  group_id: $group_id,
  uuid: $uuid,
  name: $name,
  source: $source,
  source_description: $source_description,
  content: $content,
  created_at: timestamp(),
  valid_at: timestamp()
})
```

**关键注意**：`createEpisode()` 只是将原始内容存入图谱，后续的实体提取、关系提取、去重、Embedding 生成等操作是**异步触发**的（通常通过后台任务或消息队列）。这意味着刚添加的 Episode 可能不会立即出现在搜索结果中，需要等待流水线处理完成。

#### 底层 API 方式（直接操作 Neo4j）

如果你需要更细粒度的控制，可以直接使用 `GraphNeo4jService`：

```java
String uuid = UUID.randomUUID().toString().replace("-", "");
Map<String, Object> properties = new HashMap<>();
properties.put("sender", "user");

Map<String, Object> episode = graphNeo4jService.createEpisode(
    graphId,
    uuid,
    "用户询问跑步鞋",           // name
    "message",                   // source
    "客服对话",                  // sourceDescription
    "user: 你好，我想买一双跑步鞋", // content
    properties
);
```

### 3.3 检索相关记忆

#### 对话记忆重建：getMemory()

这是 AI 记忆场景中最常用的方法。传入当前对话的历史消息，自动返回相关的记忆上下文：

```java
List<MessageQueryVO> messages = new ArrayList<>();

MessageQueryVO msg1 = new MessageQueryVO();
msg1.setRole("user");
msg1.setContent("你好，我想买一双跑步鞋");
messages.add(msg1);

MessageQueryVO msg2 = new MessageQueryVO();
msg2.setRole("assistant");
msg2.setContent("您好！我们有多种跑步鞋可供选择。您主要在什么场地跑步？");
messages.add(msg2);

MessageQueryVO msg3 = new MessageQueryVO();
msg3.setRole("user");
msg3.setContent("平时在公园跑，预算 500 左右");
messages.add(msg3);

GetMemoryReqVO reqVO = new GetMemoryReqVO();
reqVO.setMessages(messages);
reqVO.setGroupIds(List.of(graphId));
reqVO.setMaxFacts(10);

GetMemoryRespVO memory = searchService.getMemory(reqVO);
System.out.println(memory.getContext());
// 输出示例：
// 相关知识：
// - 用户平时在公园跑步
// - 用户的预算偏好是 500 元左右
```

**实现原理**：`getMemory()` 会提取 `messages` 中最后一条非 `system` 消息的内容（本例中为 `"平时在公园跑，预算 500 左右"`），以此作为查询执行混合检索，并将结果拼接为可直接使用的上下文字符串。

#### 通用搜索：searchGraph()

当你需要基于特定关键词主动检索时，使用 `searchGraph()`：

```java
SearchQueryReqVO reqVO = new SearchQueryReqVO();
reqVO.setQuery("公园跑步鞋推荐");
reqVO.setGroupIds(List.of(graphId));
reqVO.setMaxFacts(10);
reqVO.setEnableRerank(true);

SearchConfigVO config = new SearchConfigVO();
config.setMode("hybrid");
config.setBm25Weight(0.3);
config.setVectorWeight(0.7);
reqVO.setConfig(config);

SearchResultsRespVO results = searchService.searchGraph(graphId, reqVO);

for (FactResultVO fact : results.getFacts()) {
    System.out.println("- " + fact.getFact() + " (得分: " + fact.getScore() + ")");
}
```

#### 最近 Episode 回溯：getRecentEpisodes()

当需要按时间顺序回溯最近对话时：

```java
List<Map<String, Object>> recentEpisodes = searchService.getRecentEpisodes(graphId, 5);
for (Map<String, Object> ep : recentEpisodes) {
    System.out.println(ep.get("created_at") + ": " + ep.get("content"));
}
```

### 3.4 时间序列与上下文更新

#### Episode 链式时序

每添加一个新的 Episode，系统会自动将其与上一个 Episode 建立 `NEXT` 关系，形成时间链：

```
(Episode_1)-[:NEXT]->(Episode_2)-[:NEXT]->(Episode_3)
```

这使得你可以精确地按时间顺序遍历整个对话历史。

#### 实体关系的增量更新

同一实体在不同 Episode 中被提及时，Graphiti 会自动合并：

1. **首次提及**：创建新的 `EntityNode`，生成 `summary` 和 `attributes`。
2. **再次提及**：更新现有 `EntityNode` 的 `summary`（追加新信息），合并 `attributes`。
3. **关系更新**：如果新信息与旧关系冲突（如用户改口说预算变为 800），旧事实会被标记 `invalid_at`，新事实被创建并标记 `valid_at`。

这种**增量更新**机制确保图谱始终反映最新状态，同时保留历史版本的可追溯性。

---

## 4. 代码示例

### 4.1 初始化 Graphiti 记忆系统

以下是一个完整的 Spring Boot 配置示例：

```java
import com.graphiti.module.graphiti.service.EpisodeService;
import com.graphiti.module.graphiti.service.SearchService;
import com.graphiti.module.graphiti.service.GraphNeo4jService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class GraphitiMemoryConfig {

    /**
     * 所有 Service 已通过 Spring Boot 自动注入可用：
     * - EpisodeService: 管理 Episode（添加、查询、删除）
     * - SearchService: 执行搜索和记忆检索
     * - GraphNeo4jService: 底层 Neo4j 操作
     *
     * 只需在 application.yml 中配置 Neo4j 连接即可。
     */
}
```

对应的 `application.yml`：

```yaml
spring:
  neo4j:
    uri: bolt://localhost:7687
    authentication:
      username: neo4j
      password: your-password
```

### 4.2 将用户消息添加为 Episode

封装一个工具方法，将对话消息格式化为 Episode：

```java
import com.graphiti.module.graphiti.service.EpisodeService;
import com.graphiti.module.graphiti.vo.episode.EpisodeInfoRespVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class EpisodeRecorder {

    private final EpisodeService episodeService;

    /**
     * 将单条对话消息记录为 Episode
     *
     * @param graphId   图谱 ID（会话隔离标识）
     * @param role      角色：user / assistant / system
     * @param content   消息内容
     * @return 创建的 Episode 信息
     */
    public EpisodeInfoRespVO recordMessage(String graphId, String role, String content) {
        String timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        Map<String, Object> episodeData = new HashMap<>();
        episodeData.put("name", role + "_" + timestamp);
        episodeData.put("source", "message");
        episodeData.put("sourceDescription", "AI对话记忆");
        // message 类型的推荐格式: "actor: content"
        episodeData.put("content", role + ": " + content);
        episodeData.put("properties", Map.of("role", role));

        return episodeService.createEpisode(graphId, episodeData);
    }
}
```

使用示例：

```java
@Service
@RequiredArgsConstructor
public class ChatService {
    private final EpisodeRecorder episodeRecorder;

    public void onUserMessage(String graphId, String userMessage) {
        // 记录用户消息
        episodeRecorder.recordMessage(graphId, "user", userMessage);
    }

    public void onAssistantMessage(String graphId, String assistantMessage) {
        // 记录 AI 回复
        episodeRecorder.recordMessage(graphId, "assistant", assistantMessage);
    }
}
```

### 4.3 搜索相关记忆片段

#### 使用 getMemory() 重建上下文

```java
import com.graphiti.module.graphiti.service.SearchService;
import com.graphiti.module.graphiti.vo.search.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MemoryRetriever {

    private final SearchService searchService;

    /**
     * 基于对话历史检索相关记忆
     *
     * @param graphId      图谱 ID
     * @param chatHistory  当前对话历史（包含 user 和 assistant 消息）
     * @param maxFacts     最大返回事实数
     * @return 可直接注入 Prompt 的上下文字符串
     */
    public String retrieveContext(String graphId, List<ChatMessage> chatHistory, int maxFacts) {
        List<MessageQueryVO> messages = new ArrayList<>();
        for (ChatMessage msg : chatHistory) {
            MessageQueryVO vo = new MessageQueryVO();
            vo.setRole(msg.getRole());
            vo.setContent(msg.getContent());
            messages.add(vo);
        }

        GetMemoryReqVO reqVO = new GetMemoryReqVO();
        reqVO.setMessages(messages);
        reqVO.setGroupIds(List.of(graphId));
        reqVO.setMaxFacts(maxFacts);

        GetMemoryRespVO resp = searchService.getMemory(reqVO);
        return resp.getContext(); // 格式: "相关知识：\n- 事实1\n- 事实2..."
    }

    /**
     * 获取结构化的记忆事实列表（用于自定义 Prompt 拼接）
     */
    public List<FactResultVO> retrieveFacts(String graphId, String query, int maxFacts) {
        SearchQueryReqVO reqVO = new SearchQueryReqVO();
        reqVO.setQuery(query);
        reqVO.setGroupIds(List.of(graphId));
        reqVO.setMaxFacts(maxFacts);

        SearchResultsRespVO resp = searchService.searchGraph(graphId, reqVO);
        return resp.getFacts();
    }

    /**
     * 获取最近的 N 轮对话
     */
    public List<Map<String, Object>> getRecentMessages(String graphId, int lastN) {
        return searchService.getRecentEpisodes(graphId, lastN);
    }
}

// 简单的消息封装类
@Data
@AllArgsConstructor
public class ChatMessage {
    private String role;      // system / user / assistant
    private String content;
}
```

### 4.4 将检索结果整合到 AI 提示中

#### 方式一：直接使用 getMemory() 的 context（推荐）

```java
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiPromptBuilder {

    private final MemoryRetriever memoryRetriever;
    private final ChatClient chatClient;

    public String generateResponse(String graphId, List<ChatMessage> history, String currentUserMessage) {
        // 1. 从 Graphiti 检索相关记忆
        String memoryContext = memoryRetriever.retrieveContext(graphId, history, 10);

        // 2. 构建 System Prompt
        String systemContent = String.format("""
            你是一位专业的智能客服助手。请根据以下信息回答用户问题。
            
            %s
            
            请保持回答简洁、专业。如果用户的问题涉及之前对话中提到的内容，请确保连贯性。
            """, memoryContext.isEmpty() ? "暂无历史记忆。" : memoryContext);

        // 3. 构建完整 Prompt
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemContent));

        // 添加历史对话（可选：可限制最近 N 轮）
        for (ChatMessage msg : history) {
            if ("user".equals(msg.getRole())) {
                messages.add(new UserMessage(msg.getContent()));
            } else if ("assistant".equals(msg.getRole())) {
                messages.add(new AssistantMessage(msg.getContent()));
            }
        }

        // 添加当前用户消息
        messages.add(new UserMessage(currentUserMessage));

        // 4. 调用 LLM
        Prompt prompt = new Prompt(messages);
        return chatClient.prompt(prompt).call().content();
    }
}
```

#### 方式二：手动遍历 FactResultVO 构建自定义 Prompt

当你需要更精细地控制记忆在 Prompt 中的呈现方式时：

```java
public String buildCustomPrompt(String graphId, String query, List<ChatMessage> history) {
    List<FactResultVO> facts = memoryRetriever.retrieveFacts(graphId, query, 10);

    StringBuilder factBuilder = new StringBuilder("以下是与当前问题相关的事实：\n");
    for (FactResultVO fact : facts) {
        factBuilder.append(String.format("- %s", fact.getFact()));

        // 标注事实的时间有效性
        if (fact.getValidAt() != null) {
            factBuilder.append(String.format(" [生效于: %s]", fact.getValidAt()));
        }
        if (fact.getInvalidAt() != null) {
            factBuilder.append(String.format(" [已于 %s 失效]", fact.getInvalidAt()));
        }
        factBuilder.append("\n");
    }

    return factBuilder.toString();
}
```

### 4.5 对话状态管理与维护

维护一个会话状态对象，贯穿整个对话生命周期：

```java
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * 对话会话状态
 * 每个用户会话对应一个 ConversationState 实例
 */
@Data
public class ConversationState {

    /** 会话唯一标识 */
    private String sessionId;

    /** 对应的图谱 ID（groupId），用于隔离不同用户的记忆 */
    private String graphId;

    /** 用户实体节点的 UUID（用于中心节点重排序） */
    private String userNodeUuid;

    /** 用户名 */
    private String userName;

    /** 当前对话历史 */
    private List<ChatMessage> messages = new ArrayList<>();

    /** 会话创建时间 */
    private long createdAt = System.currentTimeMillis();

    public void addMessage(String role, String content) {
        this.messages.add(new ChatMessage(role, content));
    }
}
```

会话管理器：

```java
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ConversationManager {

    // 内存中的会话缓存（生产环境建议使用 Redis）
    private final Map<String, ConversationState> sessions = new ConcurrentHashMap<>();

    public ConversationState getOrCreateSession(String sessionId, String userName) {
        return sessions.computeIfAbsent(sessionId, id -> {
            ConversationState state = new ConversationState();
            state.setSessionId(id);
            state.setGraphId("session_" + id);  // 每个会话独立图谱
            state.setUserName(userName);
            return state;
        });
    }

    public ConversationState getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    public void removeSession(String sessionId) {
        sessions.remove(sessionId);
    }
}
```

---

## 5. 完整应用范例：智能客服聊天机器人

### 5.1 需求场景描述

构建一个电商智能客服机器人，具备以下能力：

1. **产品咨询**：回答用户关于产品的问题（产品信息已预加载到图谱）。
2. **偏好记忆**：记住用户的偏好（尺码、颜色、预算、使用场景）。
3. **上下文连贯**：在多轮对话中保持上下文一致性，能引用之前提到的信息。
4. **个性化推荐**：基于用户偏好和历史行为给出个性化推荐。

### 5.2 项目依赖配置

在 `pom.xml` 中添加以下依赖：

```xml
<!-- Graphiti 核心模块 -->
<dependency>
    <groupId>com.graphiti</groupId>
    <artifactId>ontograph-module-core</artifactId>
    <version>${project.version}</version>
</dependency>

<!-- Spring AI（以 OpenAI 为例，可替换为 Qwen、Ollama 等） -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
</dependency>
```

`application.yml` 配置：

```yaml
spring:
  neo4j:
    uri: bolt://localhost:7687
    authentication:
      username: neo4j
      password: your-password

  ai:
    openai:
      api-key: ${OPENAI_API_KEY}
      chat:
        options:
          model: gpt-4o-mini
          temperature: 0.7
```

### 5.3 记忆服务封装类 ChatMemoryService

```java
package com.example.chatbot.memory;

import com.graphiti.module.graphiti.service.EpisodeService;
import com.graphiti.module.graphiti.service.SearchService;
import com.graphiti.module.graphiti.service.GraphNeo4jService;
import com.graphiti.module.graphiti.vo.episode.EpisodeInfoRespVO;
import com.graphiti.module.graphiti.vo.search.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMemoryService {

    private final EpisodeService episodeService;
    private final SearchService searchService;
    private final GraphNeo4jService graphNeo4jService;

    /**
     * 初始化用户记忆空间
     * 创建用户实体节点和首个 Episode，返回用户节点 UUID
     */
    public String initializeUserMemory(String graphId, String userName) {
        // 1. 记录用户进入对话
        Map<String, Object> episodeData = new HashMap<>();
        episodeData.put("name", "用户初始化");
        episodeData.put("source", "message");
        episodeData.put("sourceDescription", "客服系统");
        episodeData.put("content", "user: " + userName + " 进入了对话");
        episodeService.createEpisode(graphId, episodeData);

        // 2. 搜索用户实体节点 UUID（等待实体提取完成后才能获取）
        // 实际项目中可通过异步回调或延迟查询获取
        // 这里简化处理，返回 graphId 作为标识
        return graphId;
    }

    /**
     * 记录对话消息到 Graphiti
     */
    public void addMessage(String graphId, String role, String content) {
        try {
            String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            Map<String, Object> episodeData = new HashMap<>();
            episodeData.put("name", role + "_" + timestamp);
            episodeData.put("source", "message");
            episodeData.put("sourceDescription", "客服对话");
            episodeData.put("content", role + ": " + content);

            EpisodeInfoRespVO episode = episodeService.createEpisode(graphId, episodeData);
            log.debug("已记录 Episode: uuid={}, content={}", episode.getUuid(), content);
        } catch (Exception e) {
            log.error("记录消息到 Graphiti 失败", e);
            // 记录失败不应阻塞主流程
        }
    }

    /**
     * 基于对话历史检索相关记忆上下文
     */
    public String retrieveContext(String graphId, List<ChatMessage> messages, int maxFacts) {
        try {
            List<MessageQueryVO> messageVOs = new ArrayList<>();
            for (ChatMessage msg : messages) {
                MessageQueryVO vo = new MessageQueryVO();
                vo.setRole(msg.getRole());
                vo.setContent(msg.getContent());
                messageVOs.add(vo);
            }

            GetMemoryReqVO reqVO = new GetMemoryReqVO();
            reqVO.setMessages(messageVOs);
            reqVO.setGroupIds(List.of(graphId));
            reqVO.setMaxFacts(maxFacts);

            GetMemoryRespVO resp = searchService.getMemory(reqVO);
            return resp.getContext();
        } catch (Exception e) {
            log.error("检索记忆失败", e);
            return "";
        }
    }

    /**
     * 检索产品相关事实（用于工具调用）
     */
    public List<String> searchProductFacts(String graphId, String query) {
        SearchQueryReqVO reqVO = new SearchQueryReqVO();
        reqVO.setQuery(query);
        reqVO.setGroupIds(List.of(graphId));
        reqVO.setMaxFacts(10);

        SearchResultsRespVO resp = searchService.searchGraph(graphId, reqVO);
        List<String> facts = new ArrayList<>();
        for (FactResultVO fact : resp.getFacts()) {
            facts.add(fact.getFact());
        }
        return facts;
    }

    /**
     * 构建带记忆上下文的 System Prompt
     */
    public String buildSystemPrompt(String memoryContext, String baseInstruction) {
        StringBuilder prompt = new StringBuilder();
        prompt.append(baseInstruction).append("\n\n");

        if (memoryContext != null && !memoryContext.isEmpty()) {
            prompt.append(memoryContext).append("\n\n");
        }

        prompt.append("请记住：");
        prompt.append("1. 保持回答简洁、专业、友好\n");
        prompt.append("2. 如果用户的问题涉及上述记忆中的内容，请自然引用\n");
        prompt.append("3. 主动询问缺失的关键信息（如尺码、预算等）\n");

        return prompt.toString();
    }
}
```

### 5.4 与 Spring AI ChatClient 集成

```java
package com.example.chatbot.service;

import com.example.chatbot.memory.ChatMemoryService;
import com.example.chatbot.memory.ConversationState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerServiceBot {

    private final ChatClient chatClient;
    private final ChatMemoryService chatMemoryService;

    private static final String BASE_INSTRUCTION = """
        你是一位专业的运动鞋电商客服助手，服务于品牌"轻跑"。
        你的目标是帮助用户找到最适合他们的运动鞋。
        为了给出好的推荐，你需要了解：
        - 用户的鞋码
        - 主要使用场景（跑步/日常/篮球等）
        - 场地类型（公路/跑道/山地/公园等）
        - 预算范围
        - 特殊需求（宽脚/高足弓/扁平足等）
        """;

    /**
     * 处理用户消息并生成回复
     */
    public String chat(ConversationState state, String userMessage) {
        String graphId = state.getGraphId();

        // 1. 检索相关记忆
        String memoryContext = chatMemoryService.retrieveContext(
            graphId, state.getMessages(), 10);

        // 2. 构建 System Prompt
        String systemPrompt = chatMemoryService.buildSystemPrompt(
            memoryContext, BASE_INSTRUCTION);

        // 3. 构建消息列表
        List<org.springframework.ai.chat.messages.Message> aiMessages = new ArrayList<>();
        aiMessages.add(new SystemMessage(systemPrompt));

        // 添加最近的历史消息（限制窗口大小）
        List<ChatMessage> recentMessages = getRecentWindow(state.getMessages(), 6);
        for (ChatMessage msg : recentMessages) {
            if ("user".equals(msg.getRole())) {
                aiMessages.add(new UserMessage(msg.getContent()));
            } else if ("assistant".equals(msg.getRole())) {
                aiMessages.add(new org.springframework.ai.chat.messages.AssistantMessage(msg.getContent()));
            }
        }

        // 添加当前消息
        aiMessages.add(new UserMessage(userMessage));

        // 4. 调用 LLM
        Prompt prompt = new Prompt(aiMessages);
        String response = chatClient.prompt(prompt).call().content();

        // 5. 异步保存对话到 Graphiti（不阻塞响应）
        saveConversationAsync(graphId, userMessage, response);

        return response;
    }

    /**
     * 获取最近 N 轮对话（user + assistant 算一轮）
     */
    private List<ChatMessage> getRecentWindow(List<ChatMessage> allMessages, int maxRounds) {
        int start = Math.max(0, allMessages.size() - maxRounds * 2);
        return allMessages.subList(start, allMessages.size());
    }

    /**
     * 异步保存对话记录到 Graphiti
     */
    @Async
    protected void saveConversationAsync(String graphId, String userMessage, String assistantResponse) {
        try {
            // 将完整的对话轮次作为一个 Episode 记录
            String combined = String.format("user: %s\nassistant: %s", userMessage, assistantResponse);
            chatMemoryService.addMessage(graphId, "system", combined);
        } catch (Exception e) {
            log.error("异步保存对话到 Graphiti 失败", e);
        }
    }
}
```

### 5.5 完整对话流程演示

以下是一个完整的对话脚本，展示 Graphiti 记忆系统如何工作：

**预加载产品数据**（只需执行一次）：

```java
// 在系统启动时，将产品目录加载到图谱中
List<Map<String, Object>> products = loadProducts();
for (Map<String, Object> product : products) {
    Map<String, Object> episodeData = new HashMap<>();
    episodeData.put("name", product.get("name"));
    episodeData.put("source", "json");
    episodeData.put("sourceDescription", "产品目录");
    episodeData.put("content", JsonUtils.toJson(product));
    episodeService.createEpisode("products_graph", episodeData);
}
```

**对话演示**：

```
=== 第 1 轮 ===
用户: 你好，我想买一双跑步鞋
AI: 您好！欢迎来到轻跑。为了帮您找到最合适的跑步鞋，我想了解一下：
     您主要在什么场地跑步？（公路、公园跑道、跑步机还是山地？）
     另外您的鞋码是多少呢？

[Graphiti 状态]: 新增 Episode("user: 你好，我想买一双跑步鞋")
                自动提取实体: [用户, 跑步鞋] —— 关系: [用户]-[INTERESTED_IN]->[跑步鞋]

=== 第 2 轮 ===
用户: 平时在公园跑，鞋码 42，预算 500 左右
AI: 好的！公园跑步的话，我推荐您看看"轻跑 CloudStride"系列。
     这个系列专为公园和城市道路设计，中底采用 CloudFoam 缓震材料，
     42 码标准版目前售价 459 元，在您的预算范围内。
     请问您脚型偏宽还是标准？

[Graphiti 状态]: 新增 Episode("user: 平时在公园跑，鞋码 42，预算 500 左右")
                自动提取实体: [公园, 42码, 500元预算]
                关系更新: [用户]-[PREFERS]->[公园], [用户]-[HAS_SIZE]->[42码],
                         [用户]-[HAS_BUDGET]->[500元预算]

=== 第 3 轮 ===
用户: 我脚比较宽，之前买的鞋总是挤脚
AI: 了解了！宽脚的话我推荐选择"宽楦版（Wide Fit）"。
     CloudStride 系列有提供 42 码宽楦版本，宽度比标准版增加 4mm，
     售价也是 459 元。另外提醒您，宽楦版建议选与平时相同的码数，
     不需要加大。您看这款合适吗？

[Graphiti 状态]: 新增 Episode("user: 我脚比较宽，之前买的鞋总是挤脚")
                自动提取实体: [宽脚]
                关系更新: [用户]-[HAS_FEATURE]->[宽脚]
                事实更新: "用户的脚型偏宽" (valid_at: 当前时间)

=== 第 5 轮（几小时后，新会话）===
用户: 之前那双鞋还有别的颜色吗？
AI: 您是指 CloudStride 系列的 42 码宽楦版吗？
     这款目前有曜石黑、冰川白和森林绿三种颜色可选。
     考虑到您主要在公园跑步，森林绿可能比较耐脏，您喜欢哪个？

[Graphiti 检索]: getMemory() 提取查询: "之前那双鞋还有别的颜色吗？"
                 混合检索命中: "用户偏好的鞋码是 42", "用户的脚型偏宽",
                                "用户对 CloudStride 系列感兴趣"
                 上下文注入: System Prompt 中包含上述事实
                 AI 因此能推断"那双鞋"指的是 CloudStride 宽楦版
```

### 5.6 记忆可视化

对话产生的图谱可以在 Neo4j Browser 中可视化查看。

**查看某个会话的所有 Episode**：

```cypher
MATCH (e:Episode {group_id: 'session_abc123'})
RETURN e
ORDER BY e.created_at
```

**查看从 Episode 中提取的实体和关系**：

```cypher
MATCH (e:Episode {group_id: 'session_abc123'})-[m:MENTIONS]->(n:Entity)
RETURN e.name, n.name, n.summary
```

**查看实体间的关系网络**：

```cypher
MATCH (a:Entity {group_id: 'session_abc123'})-[r:RELATES_TO]->(b:Entity)
RETURN a.name, r.fact, b.name, r.valid_at, r.invalid_at
```

典型的对话记忆图谱结构如下：

```
┌──────────────────────────────────────────────────────┐
│                                                      │
│   [Episode: user_你好想...] ──NEXT──> [Episode: ...] │
│         │                              │             │
│         │ MENTIONS                     │ MENTIONS    │
│         ↓                              ↓             │
│    [Entity: 用户]               [Entity: 跑步鞋]      │
│         │                              ↑             │
│         │ PREFERS                      │             │
│         ↓                              │             │
│    [Entity: 公园] ────INTERESTED_IN────┘             │
│         │                                            │
│         │ HAS_SIZE                                   │
│         ↓                                            │
│    [Entity: 42码]                                    │
│         │                                            │
│         │ HAS_BUDGET                                 │
│         ↓                                            │
│    [Entity: 500元]                                   │
│                                                      │
└──────────────────────────────────────────────────────┘
```

---

## 6. 最佳实践与注意事项

### 6.1 记忆管理策略

#### 会话隔离（Session Isolation）

**不同用户、不同会话必须使用不同的 `groupId`**，这是防止记忆混淆的最重要原则。

推荐策略：

| 隔离级别 | groupId 示例 | 适用场景 |
|---------|------------|---------|
| 用户级别 | `user_10086` | 长期个人助手，跨会话保持记忆 |
| 会话级别 | `session_uuid` | 短期客服对话，会话结束后归档 |
| 主题级别 | `user_10086_shoes` | 按主题隔离记忆（如鞋类 vs 服装） |

```java
// 用户级别隔离
String userGraphId = "user_" + userId;

// 会话级别隔离
String sessionGraphId = "session_" + UUID.randomUUID().toString();
```

#### 记忆衰减与归档

长期运行的系统会积累大量 Episode，建议实施以下策略：

1. **定期归档**：将超过 30 天的旧 Episode 迁移到冷存储（如 Elasticsearch），图谱中仅保留最近活跃记忆。
2. **摘要压缩**：当 Episode 数量超过阈值时，使用 LLM 生成高层摘要，用摘要 Episode 替代原始 Episode 链。
3. **去重维护**：定期运行 `EntityDedupService`，合并同一实体的多个表达（如"跑步鞋"和"跑鞋"）。

#### 记忆合并与去重

```java
// 定期执行实体去重（建议在低峰期运行）
@Autowired
private EntityDedupService entityDedupService;

// 对指定图谱执行去重
entityDedupService.dedupeEntities(graphId);
```

### 6.2 性能优化建议

#### 索引策略

确保 Neo4j 中已建立以下关键索引：

```cypher
// Episode 节点索引
CREATE INDEX episode_group_id IF NOT EXISTS
FOR (e:Episode) ON (e.group_id);

// Entity 节点索引
CREATE INDEX entity_group_id_name IF NOT EXISTS
FOR (n:Entity) ON (n.group_id, n.name);

// 全文索引（用于 BM25 检索）
CALL db.index.fulltext.createNodeIndex("episodeContent", ["Episode"], ["content"]);
CALL db.index.fulltext.createNodeIndex("entityName", ["Entity"], ["name", "summary"]);
```

#### 批量插入

批量导入历史对话时，使用批量模式：

```java
// 批量添加 Episode，减少数据库往返
List<Map<String, Object>> episodes = new ArrayList<>();
for (Message message : historyMessages) {
    Map<String, Object> ep = new HashMap<>();
    ep.put("name", message.getRole() + "_" + message.getTimestamp());
    ep.put("source", "message");
    ep.put("content", message.getRole() + ": " + message.getContent());
    episodes.add(ep);
}

// 分批处理，每批 50 条
for (int i = 0; i < episodes.size(); i += 50) {
    List<Map<String, Object>> batch = episodes.subList(i, Math.min(i + 50, episodes.size()));
    // 批量创建（若底层支持批量 API）
    batch.forEach(ep -> episodeService.createEpisode(graphId, ep));
}
```

#### 异步处理

添加 Episode 后的实体提取流水线是耗时的（涉及 LLM 调用和 Embedding 生成）。**永远不要**在响应用户的主线程中等待提取完成：

```java
// 好的做法：异步保存
@Async("taskExecutor")
public void saveMessageAsync(String graphId, String role, String content) {
    chatMemoryService.addMessage(graphId, role, content);
}

// 避免：同步等待
public String chat(String graphId, String message) {
    chatMemoryService.addMessage(graphId, "user", message);  // 不要等这里！
    // ... 应该直接返回 AI 响应
}
```

#### Embedding 缓存

对于重复出现的文本（如常见产品描述），可以在应用层缓存 Embedding：

```java
@Component
public class EmbeddingCache {
    private final Map<String, float[]> cache = new ConcurrentHashMap<>();

    public float[] getOrCompute(String text, Supplier<float[]> compute) {
        return cache.computeIfAbsent(text, k -> compute.get());
    }
}
```

### 6.3 数据隐私与安全

#### 敏感信息过滤

在将消息存入 Graphiti 之前，过滤敏感信息：

```java
@Component
public class SensitiveInfoFilter {

    // 身份证号、手机号、银行卡号等正则
    private static final List<Pattern> PATTERNS = List.of(
        Pattern.compile("\\b\\d{17}[\\dXx]\\b"),           // 身份证号
        Pattern.compile("\\b1[3-9]\\d{9}\\b"),               // 手机号
        Pattern.compile("\\b\\d{16,19}\\b")                  // 银行卡号
    );

    public String filter(String content) {
        String filtered = content;
        for (Pattern pattern : PATTERNS) {
            filtered = pattern.matcher(filtered).replaceAll("[已隐藏]");
        }
        return filtered;
    }
}

// 使用
String safeContent = sensitiveInfoFilter.filter(userMessage);
chatMemoryService.addMessage(graphId, "user", safeContent);
```

#### 访问控制

利用 `groupId` 实现多租户隔离：

```java
// 确保用户只能访问自己的图谱
public String getUserGraphId(Long userId) {
    return "user_" + userId;
}

// 在 Controller 层验证权限
@GetMapping("/memory/{graphId}")
public ResponseEntity<?> getMemory(@PathVariable String graphId,
                                    @AuthenticationPrincipal User user) {
    if (!graphId.equals(getUserGraphId(user.getId()))) {
        return ResponseEntity.status(403).body("无权访问该记忆空间");
    }
    // ...
}
```

#### 数据保留策略

```java
/**
 * 定期清理过期记忆（建议每月执行一次）
 */
@Scheduled(cron = "0 0 2 1 * ?")  // 每月 1 日凌晨 2 点
public void cleanupOldMemories() {
    // 清理超过 1 年未更新的会话图谱
    List<String> oldGraphIds = findInactiveGraphs(365);
    for (String graphId : oldGraphIds) {
        graphitiService.clearGraph(graphId);  // 清空图谱数据
        // 或 graphitiService.deleteGraph(graphId);  // 彻底删除
    }
}
```

#### 用户数据权利（GDPR/CCPA 合规）

提供用户查看和删除自己记忆数据的接口：

```java
/**
 * 导出用户的全部记忆数据
 */
public Map<String, Object> exportUserMemory(String graphId) {
    return graphitiService.exportGraph(graphId);
}

/**
 * 删除用户的全部记忆数据
 */
public void deleteUserMemory(String graphId) {
    graphitiService.deleteGraph(graphId);
}
```

---

## 附录：Python 原版与 Java 版核心差异速查

| 特性 | Python 原版 | Java 版 (ontograph-java) |
|-----|-----------|------------------------|
| 初始化 | `Graphiti(uri, user, password)` | Spring Boot 自动配置 |
| 添加 Episode | `await graphiti.add_episode(...)` | `episodeService.createEpisode(graphId, data)` |
| 搜索 | `await graphiti.search(query)` | `searchService.searchGraph(graphId, reqVO)` |
| 对话记忆 | 手动拼接 search 结果到 Prompt | `searchService.getMemory(reqVO)` 封装方法 |
| 中心节点排序 | `center_node_uuid` 参数 | `SearchConfigVO` 中配置（如支持） |
| 异步模型 | `async/await` | Spring `@Async` + 线程池 |
| Episode 类型 | `EpisodeType` 枚举 | `source` 字符串字段 |
| 返回类型 | Pydantic Model | VO 对象（Serializable） |

---

*本文档基于 ontograph-java 项目当前实现编写。随着项目迭代，部分 API 可能会有调整，请以实际源码为准。*
