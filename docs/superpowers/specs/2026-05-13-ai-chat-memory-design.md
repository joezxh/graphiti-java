# AI 会话上下文记忆系统设计文档

**日期**: 2026-05-13
**目标文件**: `docs/ai-chat-memory.md`
**文档类型**: 用户指南 + API 实践教程

---

## 1. 设计目标

编写一份详细的中文技术文档，指导开发者如何将 Graphiti 用作 AI 会话的上下文记忆系统。文档需同时满足：
- **概念理解**：让读者理解时序上下文图的工作原理
- **实战落地**：提供可直接复制使用的 Java 代码示例
- **架构对齐**：与 ontograph-java 项目现有 Service 层和 REST API 保持一致

## 2. 目标读者

- 已了解 Spring Boot 和基础图数据库概念的 Java 开发者
- 希望在 AI 应用中引入持久化记忆能力的架构师
- 使用 ontograph-java 项目的后端开发人员

## 3. 参考来源

| 来源 | 路径 | 用途 |
|------|------|------|
| Python Quickstart | `examples/quickstart/quickstart_neo4j.py` | Episode 添加、基础搜索流程 |
| LangGraph Agent | `examples/langgraph-agent/agent.ipynb` | AI 聊天机器人集成模式、记忆检索与 Prompt 拼接 |
| Python Core API | `graphiti_core/graphiti.py`, `nodes.py`, `edges.py` | 核心概念定义（EpisodeType、EntityNode、EntityEdge 等） |
| Java Service API | `graphiti-module-core/service/*.java` | Java 版现有接口对照 |
| Java Controller | `graphiti-module-core/controller/admin/GraphitiController.java` | REST API 端点展示 |
| Java Search Impl | `service/impl/SearchServiceImpl.java` | getMemory() 实现逻辑 |

## 4. 章节详细设计

### 第 1 章：核心概念（约 1500 字）

**1.1 什么是时序上下文图（Temporal Context Graph）**
- 对比传统 RAG：传统 RAG 将对话历史切分为文本块进行向量检索，丢失了时间顺序和实体间的结构化关系
- Graphiti 的核心创新：在向量检索之上叠加**时序图谱层**，每个对话轮次是一个 Episode，Episode 之间通过 `NEXT` 边维护时间序列，同时自动提取实体和关系构成语义网络
- 双层结构：时序层（Episodic Timeline）+ 语义层（Entity-Relation Graph）

**1.2 记忆的基本单元：Episode**
- 定义：EpisodicNode，代表一次信息输入（对话消息、文档片段、结构化数据）
- EpisodeType 枚举说明：
  - `message`：对话消息，格式 `"actor: content"`（如 `"user: 你好"`）
  - `text`：纯文本片段（如播客转录、文章段落）
  - `json`：结构化数据（如用户资料、产品信息）
  - `fact_triple`：预提取的事实三元组
- 在 AI 记忆场景中优先使用 `message` 类型的原因：保留发言者身份，便于后续实体归属和上下文重建

**1.3 实体与关系在记忆中的作用**
- **EntityNode**：从 Episode 中自动提取的实体（人名、地名、组织、概念等）
  - 属性：uuid, name, labels, summary, attributes
- **EntityEdge（Fact）**：实体间的持久化关系
  - 属性：uuid, fact（自然语言描述）, valid_at / invalid_at（支持时态事实）
- **EpisodicEdge**：连接 Episode 与 EntityNode 的 `MENTIONS` 边
  - 作用：追溯"哪个对话提到了哪个实体"
- **NextEpisodeEdge**：连接相邻 Episode 的 `NEXT` 边
  - 作用：维护严格的时间序列，支持"最近 N 轮对话"回溯

**1.4 记忆检索机制**
- **混合检索（Hybrid Search）**：BM25 全文检索 + 向量语义检索 + RRF 融合排序
- **中心节点重排序（Center Node Reranking）**：以特定实体节点为中心，按图距离重排结果
  - 应用场景：以当前用户节点为中心，优先返回与该用户相关的记忆
- **时间窗口检索**：基于 `NextEpisodeEdge` 链回溯最近 N 个 Episode

### 第 2 章：Java 项目中的核心 API 概览（约 800 字）

**2.1 Python 原版 API 与 Java 版对照**

| Python API | Java Service 方法 | 用途 |
|-----------|------------------|------|
| `Graphiti(uri, user, password)` | Spring Boot 自动配置 / `GraphNeo4jService` | 初始化连接 |
| `graphiti.add_episode(...)` | `EpisodeService.createEpisode(graphId, episodeData)` | 添加记忆片段 |
| `graphiti.search(query)` | `SearchService.searchGraph(graphId, reqVO)` | 通用搜索 |
| `graphiti._search(query, config)` | `SearchService.search(reqVO)` | 全局搜索 |
| 对话记忆专用 | `SearchService.getMemory(reqVO)` | **基于对话历史重建上下文** |
| `graphiti.close()` | Spring 容器销毁时自动关闭 | 资源释放 |

**2.2 关键 VO 和数据结构**
- `GetMemoryReqVO`：传入对话历史（List<MessageQueryVO>）+ groupIds + maxFacts
- `GetMemoryRespVO`：返回 facts（相关事实）+ entities（相关实体）+ context（拼接好的上下文字符串）
- `SearchQueryReqVO`：通用搜索请求，含 query、groupIds、maxFacts、enableRerank、config
- `FactResultVO`：单条事实结果，含 uuid、fact、sourceNodeUuid、targetNodeUuid、validAt、invalidAt、score
- `MessageQueryVO`：单条消息，含 role（system/user/assistant）和 content

### 第 3 章：实现流程（约 1200 字）

**3.1 初始化与连接**
- 方式一：Spring Boot 自动配置（ontograph-java 项目默认方式）
  - 依赖 `graphiti-spring-boot-starter-neo4j`
  - `application.yml` 配置 Neo4j URI、用户名、密码
  - Service 层通过 `@RequiredArgsConstructor` 自动注入
- 方式二：手动配置 `GraphNeo4jService` 和 `Driver`
  - 适用于非 Spring 环境或需要多数据库连接的场景

**3.2 添加记忆片段（Episodes）**
- **底层 API 方式**：通过 `GraphNeo4jService` 直接执行 Cypher 查询插入 EpisodicNode 和边
- **Service 层方式**：调用 `EpisodeService.createEpisode(graphId, episodeData)`
  - episodeData 需包含：name、content、sourceType、sourceDescription、referenceTime
  - 对于对话消息，sourceType 应为 `"message"`，content 格式为 `"user: 消息内容"`
- **关键注意**：添加 Episode 后会**异步触发**实体提取、关系提取、去重、Embedding 生成等流水线操作

**3.3 检索相关记忆**
- **通用搜索**：`SearchService.searchGraph(graphId, reqVO)`
  - 适合：根据关键词检索图谱中的事实和实体
  - 支持：重排序、搜索模式配置（hybrid / vector / text）
- **对话记忆重建**：`SearchService.getMemory(reqVO)`
  - 内部逻辑：提取最后一条非 system 消息 → 执行混合检索 → 将结果拼接为 `"相关知识：\n- fact1\n- fact2..."`
  - 这是 Java 版专门为 AI 记忆场景封装的方法
- **最近 Episode 回溯**：`SearchService.getRecentEpisodes(graphId, lastN)`
  - 适合：需要严格时间顺序的最近对话回溯

**3.4 时间序列与上下文更新**
- Episode 链式时序：每个新 Episode 自动与上一个 Episode 建立 `NEXT` 边
- 实体关系的增量更新：同一实体在新 Episode 中出现时，会更新 summary 和 attributes，而非重复创建
- 事实的时态管理：新事实可能使旧事实失效（设置 invalid_at），支持"曾任职"类时态表达

### 第 4 章：代码示例（约 2000 字）

提供 5 个独立的、可直接运行的代码片段：

**4.1 初始化 Graphiti 记忆系统**
- 展示 Spring Boot 配置类 + application.yml 配置

**4.2 将用户消息添加为 Episode**
- 展示 `EpisodeService.createEpisode()` 的调用
- 展示消息格式化工具方法
- 对比底层 Cypher 查询方式

**4.3 搜索相关记忆片段**
- 展示 `SearchService.searchGraph()` 调用
- 展示 `SearchService.getMemory()` 调用
- 展示 `SearchService.getRecentEpisodes()` 调用
- 展示中心节点重排序的使用（Python 版特性，Java 版如有则展示）

**4.4 将检索结果整合到 AI 提示中**
- 展示如何将 `GetMemoryRespVO.context` 拼接到 System Prompt
- 展示如何手动遍历 `FactResultVO` 列表构建自定义 Prompt
- 与 Spring AI `ChatClient` 集成的示例

**4.5 对话状态管理与维护**
- 展示如何维护一个 `ConversationState` 类（包含 graphId、userNodeUuid、recentEpisodes）
- 展示会话开始时的用户节点创建（类似 LangGraph Agent 中的 `user_node_uuid`）

### 第 5 章：完整应用范例（约 2500 字）

**场景**：智能客服聊天机器人（类似 LangGraph Agent 中的 ShoeBot，但改为通用的电商客服）

**5.1 需求场景描述**
- 用户与 AI 客服进行多轮对话
- AI 需要记住用户的偏好（尺码、颜色、预算）和历史咨询记录
- 产品信息已预加载到图谱中

**5.2 项目依赖配置**
- `pom.xml` 中需要的依赖：`graphiti-module-core`、`spring-ai-openai`（或其他 Provider）

**5.3 记忆服务封装类 `ChatMemoryService`**
- 方法：
  - `initializeUserMemory(userId, userName)`：创建用户 Episode，获取 userNodeUuid
  - `addMessage(graphId, role, content)`：将对话消息添加为 Episode
  - `retrieveContext(graphId, messages, maxFacts)`：调用 getMemory 获取上下文
  - `buildSystemPrompt(context, baseInstruction)`：拼接 System Prompt

**5.4 与 Spring AI ChatClient 集成**
- 使用 `ChatClient` 进行对话
- 每次调用前注入 Graphiti 检索到的上下文
- 异步保存对话到 Graphiti（避免阻塞响应）

**5.5 完整对话流程演示**
- 提供一个完整的对话脚本（5-6 轮），展示：
  - 第 1 轮：用户询问产品，AI 回答
  - 第 2 轮：用户提到偏好，AI 记录并推荐
  - 第 3 轮：用户再次询问，AI 基于之前提到的偏好给出个性化回答
  - 展示图谱中如何体现这些关系

**5.6 记忆可视化**
- 展示如何在 Neo4j Browser 中查看对话生成的图谱
- 解释节点和边的含义

### 第 6 章：最佳实践与注意事项（约 1200 字）

**6.1 记忆管理策略**
- **记忆衰减**：长期未访问的事实可以归档或降低权重
- **会话隔离**：不同用户/会话使用不同的 `groupId`，避免记忆混淆
- **记忆合并**：定期运行去重（EntityDedupService），合并同一实体的多个表达
- **记忆摘要**：当 Episode 数量过多时，使用 LLM 生成高层摘要

**6.2 性能优化建议**
- **索引策略**：确保 Neo4j 中 Entity.name、EpisodicNode.content、Edge.fact 已建立全文索引和向量索引
- **批量插入**：批量添加 Episode 时，使用批量提取和去重流水线
- **异步处理**：添加 Episode 后的实体提取是异步的，响应不应等待提取完成
- **Embedding 缓存**：避免重复计算相同文本的 Embedding

**6.3 数据隐私与安全**
- **敏感信息过滤**：在添加 Episode 前，使用正则或 LLM 过滤密码、身份证号等敏感信息
- **访问控制**：利用 `groupId` 实现多租户隔离
- **数据保留策略**：定期清理过旧的 Episode（或归档到冷存储）
- **用户数据权利**：支持用户查看和删除自己的记忆数据（GDPR/CCPA 合规）

## 5. 代码示例规范

- 所有 Java 代码使用 Java 17 语法
- 使用 Lombok（`@Data`、`@RequiredArgsConstructor`）与项目保持一致
- 使用 Spring Boot 3.x 和 Spring AI 1.x API
- 每个代码片段包含：imports、完整类/方法、关键注释
- 引用现有 Java 项目中的真实类名和包名

## 6. 与现有项目的集成点

文档中会引用以下真实存在的 Java 类和接口：
- `com.graphiti.module.graphiti.service.SearchService#getMemory`
- `com.graphiti.module.graphiti.service.EpisodeService#createEpisode`
- `com.graphiti.module.graphiti.service.GraphNeo4jService`
- `com.graphiti.module.graphiti.controller.admin.GraphitiController`
- `com.graphiti.module.graphiti.vo.search.*`
- `com.graphiti.common.response.CommonResult`

## 7. 风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| Java 版 API 与 Python 版存在差异 | 文档中的 Python→Java 映射可能不完全准确 | 所有 Java API 引用基于实际源码，不假设未实现的特性 |
| getMemory 实现可能变化 | 文档中的 getMemory 行为描述可能过时 | 注明基于当前版本实现，核心逻辑自包含 |
| 文档篇幅过长 | 读者可能失去耐心 | 每个章节提供 TL;DR 摘要，代码示例可独立复制运行 |
