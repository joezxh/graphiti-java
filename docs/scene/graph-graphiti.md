# graphiti-java 代码库地图

> 本文档由 `/gsd-map-codebase` 自动生成,基于对 `d:\projects\graphiti-java` 代码库的全面分析。
> 生成时间: 2026-06-17
> 生成方式: 4 个并行子代理分别分析 tech(技术栈+集成)、arch(架构+结构)、quality(规范+测试)、concerns(风险+隐患)四个维度。

---

## 目录

- [1. 概览](#1-概览)
- [2. 技术栈](#2-技术栈)
- [3. 外部集成](#3-外部集成)
- [4. 系统架构](#4-系统架构)
- [5. 目录结构](#5-目录结构)
- [6. 编码规范](#6-编码规范)
- [7. 测试策略](#7-测试策略)
- [8. 风险与隐患](#8-风险与隐患)
- [9. 关键发现汇总](#9-关键发现汇总)

---

## 1. 概览

### 1.1 项目定位

**graphiti-java** (OntoGraph) 是一个知识图谱平台,包含:

- **后端** `ontograph-backend/` — Java Spring Boot 服务 (port 9090)
- **前端** `ontograph-frontend/` — Vue 3 SPA (Vite dev server port 3000/5173)
- **数据库** — PostgreSQL(业务元数据) + Neo4j(图数据) + Redis(缓存)
- **AI 能力** — Spring AI + LM Studio (OpenAI 兼容接口), 支持 12+ 云端 LLM 提供商

### 1.2 顶层架构图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           Browser (Vue 3 SPA)                            │
│                     port 5173 (Vite dev server)                        │
└────────────────────────────┬────────────────────────────────────────────┘
                             │ HTTP/REST
                             ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    Spring Boot Backend (port 9090)                      │
│         ontograph-backend / com.ontograph.*                            │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────────────────────┐  │
│  │ Controller   │→│ Service      │→│ Mapper / Neo4j Driver        │  │
│  │ (admin/*)   │ │ (impl/*)    │ │ (MyBatis-Plus + Neo4j Driver) │  │
│  └──────────────┘ └──────────────┘ └──────────────────────────────┘  │
│                                                                         │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │  Security Filter Chain: JwtAuthFilter → SecurityConfig → CORS    │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│                                                                         │
│  ┌─────────────────────┐  ┌─────────────────┐  ┌───────────────────┐  │
│  │ GraphitiModule      │  │ SystemModule    │  │ Framework         │  │
│  │ (graphiti/*)       │  │ (system/*)     │  │ (security/*)      │  │
│  │ - Graph/LLM/Search │  │ - Auth/User/   │  │ - JWT             │  │
│  │ - Neo4j Ops        │  │   Role/Menu    │  └───────────────────┘  │
│  └─────────────────────┘  └─────────────────┘                        │
└────┬──────────────┬──────────────────────────┬───────────────────────┘
     │              │                          │
     ▼              ▼                          ▼
┌──────────┐  ┌──────────┐  ┌──────────────────────────────┐
│PostgreSQL│  │  Neo4j   │  │        Redis               │
│(metadata)│  │(graph DB)│  │ (cache + embedding cache)   │
│ port 5432│  │ port 7687│  │      port 6379              │
└──────────┘  └──────────┘  └──────────────────────────────┘
                                              │
                                              ▼
                                    ┌──────────────────┐
                                    │ LM Studio / OpenAI│
                                    │ compatible LLM    │
                                    │ (REST API)       │
                                    └──────────────────┘
```

### 1.3 模块依赖关系

```
frontend (ontograph-frontend/src)
  views/ → components/ → store/ → api/
  router/ → store/
  api/ → port 9090

backend (ontograph-backend)
  graphiti → common
  system → common
  graphiti → framework
  graphiti → system
  graphiti → common
  framework → common
```

---

## 2. 技术栈

> 详细内容见 `.planning/codebase/STACK.md`

### 2.1 语言与运行时

| 语言 | 版本 | 用途 | 证据 |
|------|------|------|------|
| Java | 21 | 后端运行时 | `pom.xml:17-19` |
| TypeScript | ^5.4 | 前端源码 | `package.json:28` |
| Vue | ^3.4 | 前端框架 | `package.json:19` |
| SQL | PostgreSQL 16 | 关系数据库 | `docker-compose.yml:68` |

### 2.2 后端技术栈

| 类别 | 技术 | 版本 | 证据 |
|------|------|------|------|
| 框架 | Spring Boot | 3.5.5 | `pom.xml:23` |
| ORM | MyBatis-Plus | 3.5.12 | `pom.xml:25,199-201` |
| 图数据库驱动 | Neo4j Java Driver | 5.26.0 | `pom.xml:26,76-80` |
| 安全 | Spring Security + JWT (jjwt) | 0.12.3 | `pom.xml:182-183,83-99` |
| LLM | Spring AI | 1.1.2 | `pom.xml:24,49-56` |
| 缓存 L1 | Caffeine | 3.1.8 | `pom.xml:156-161,275-277` |
| 缓存 L2 | Redis (Redisson) | 3.37.0 | `pom.xml:33,142-147` |
| 构建 | Maven | — | `pom.xml` |
| 文档 | SpringDoc OpenAPI | 2.8.5 | `pom.xml:131-133` |
| RDF 处理 | Apache Jena | 4.9.0 | `pom.xml:269-273` |
| RDF 解析 | Eclipse RDF4J | 3.7.7 | `pom.xml:279-302` |
| 工具库 | Hutool | 5.8.37 | `pom.xml:30,232` |
| 对象映射 | MapStruct | 1.6.0 | `pom.xml:29,109-119` |
| 注解处理 | Lombok | 1.18.38 | `pom.xml:28,103-107,169-171` |

### 2.3 前端技术栈

| 类别 | 技术 | 版本 | 证据 |
|------|------|------|------|
| 框架 | Vue 3 + `<script setup>` | ^3.4 | `package.json:19` |
| 构建 | Vite | 5.2.0 | `package.json:31` |
| UI 库 | Ant Design Vue | 4.2.0 | `package.json:13` |
| 状态管理 | Pinia | ^2.1.0 | `package.json:18` |
| 路由 | Vue Router | ^4.3.0 | `package.json:22` |
| HTTP | Axios | ^1.7.0 | `package.json:14` |
| i18n | vue-i18n | ^9.14.4 | `package.json:21` |
| 可视化 | ECharts + vue-echarts | 5.5.0 / 7.0.0 | `package.json:16,20` |
| 样式 | LESS | 4.2.0 | `package.json:17` |
| 类型检查 | TypeScript + vue-tsc | 5.4 / 2.0 | `package.json:8,28,32` |
| E2E 测试 | Playwright | 1.61.0 | `playwright-test/package.json:13` |

### 2.4 LLM 模型适配器

项目内置了 Spring AI 的 6 种模型适配器:

| 适配器 | Starter | 证据 |
|--------|---------|------|
| OpenAI 兼容 (LM Studio) | `spring-ai-starter-model-openai` | `pom.xml:242-243` |
| Anthropic | `spring-ai-starter-model-anthropic` | `pom.xml:245-247` |
| Ollama | `spring-ai-starter-model-ollama` | `pom.xml:249-251` |
| Mistral AI | `spring-ai-starter-model-mistral-ai` | `pom.xml:253-255` |
| Azure OpenAI | `spring-ai-starter-model-azure-openai` | `pom.xml:257-259` |
| AWS Bedrock | `spring-ai-starter-model-bedrock` | `pom.xml:261-263` |

提供方配置支持: `openai | qwen | ollama | anthropic | mistral | deepseek | groq | fireworks | nebius | hyperbolic | siliconflow | voyage`

**开发环境**指向 LM Studio (`http://127.0.0.1:1234/v1`),使用本地模型:
- 聊天模型: `google/gemma-4-e4b`
- Embedding 模型: `text-embedding-qwen3-embedding-0.6b`
- Rerank 模型: `text-embedding-bge-reranker-v2-m3`

### 2.5 基础设施

| 组件 | 技术 | 证据 |
|------|------|------|
| 容器化 | Docker + docker-compose | `docker/Dockerfile`, `docker-compose.yml` |
| 镜像 | `eclipse-temurin:21-jre-alpine` (runtime) | `docker/Dockerfile:5` |
| PostgreSQL 镜像 | `postgres:16-alpine` | `docker-compose.yml:66-87` |
| Redis 镜像 | `redis:7-alpine` | `docker-compose.yml:92-107` |

**注意:** Neo4j 未包含在 docker-compose 中,需外部运行。

---

## 3. 外部集成

> 详细内容见 `.planning/codebase/INTEGRATIONS.md`

### 3.1 PostgreSQL

| 属性 | 值 |
|------|---|
| 主机 | `localhost:5432` (dev), `postgres:5432` (container) |
| 数据库 | `graphiti` |
| 用户 | `postgres` |
| 密码 | `postgres@2026!` (**硬编码,已提交**) |
| 连接池 | HikariCP (dev: max 10; prod: max 20) |
| ORM | MyBatis-Plus 3.5.12 + Dynamic DataSource 4.3.0 |
| 池提供者 | Druid 1.2.24 |

### 3.2 Neo4j

| 属性 | 值 |
|------|---|
| URI | `bolt://localhost:7687` (dev) |
| 用户 | `neo4j` |
| 密码 | `password123` (**硬编码,已提交**) |
| 驱动 | Neo4j Java Driver 5.26.0 (Bolt 协议) |
| Spring Data Neo4j | **未使用** — 纯原生驱动 |
| Docker | **未在 docker-compose 中定义** |

### 3.3 Redis

| 属性 | 值 |
|------|---|
| 主机 | `localhost:6379` (dev), `redis:6379` (container) |
| 密码 | 无 (未配置认证) |
| 持久化 | AOF + RDB |
| 客户端 | Redisson 3.37.0 |
| 用途 | 分布式缓存(L2)、嵌入向量缓存 |
| 最大内存 | 256MB (dev), 384MB (prod) — `allkeys-lru` 淘汰策略 |

### 3.4 JWT 认证

- 库: jjwt 0.12.3
- 密钥: **硬编码在 `application.yml:31`** (`mySecretKeyForJWTTokenGenerationWhichShouldBeAtLeast512BitsLong`)
- 过期时间: 86400s (24h),可通过 `ONTOGRAPH_SECURITY_JWT_EXPIRATION` 配置
- 密码哈希: BCrypt
- **无 OAuth2 / OIDC**
- **无第三方 IAM**

### 3.5 存储/文件/消息队列

| 类别 | 状态 |
|------|------|
| 文件存储 | 本地文件系统 (`ontograph-web/dist/`), 无 S3/MinIO/OSS |
| 邮件/SMS | **未检测到** |
| 搜索引擎 | **未检测到** (ES/OpenSearch) |
| 消息队列 | **未检测到** (无 RabbitMQ/Kafka) |
| 嵌入缓存 | Redis (L2) + Caffeine (L1) |

### 3.6 第三方 SDK 一览

| SDK | 版本 | 用途 |
|-----|------|------|
| Spring AI OpenAI starter | 1.1.2 | OpenAI 兼容 LLM |
| Spring AI Anthropic starter | 1.1.2 | Claude API |
| Spring AI Ollama starter | 1.1.2 | Ollama 本地 LLM |
| Spring AI Mistral AI starter | 1.1.2 | Mistral API |
| Spring AI Azure OpenAI starter | 1.1.2 | Azure OpenAI |
| Spring AI Bedrock starter | 1.1.2 | AWS Bedrock |
| Apache Jena | 4.9.0 | RDF/OWL 处理 |
| Eclipse RDF4J | 3.7.7 | RDF 解析 (Turtle, RDF/XML, JSON-LD) |
| Druid | 1.2.24 | DB 连接池 |
| Redisson | 3.37.0 | Redis 客户端 |
| Hutool | 5.8.37 | 通用工具 |
| Lombok | 1.18.38 | 样板代码生成 |
| MapStruct | 1.6.0 | 对象映射 |
| SpringDoc OpenAPI | 2.8.5 | Swagger/OpenAPI UI |
| Caffeine | 3.1.8 | 本地缓存 |
| Ant Design Vue | 4.2.0 | UI 组件库 |
| ECharts + vue-echarts | 5.5.0 / 7.0.0 | 数据可视化 |
| Playwright | 1.61.0 | E2E 测试 |

---

## 4. 系统架构

> 详细内容见 `.planning/codebase/ARCHITECTURE.md`

### 4.1 分层架构

```
HTTP Request
    │
    ▼
@RestController (e.g. GraphitiController.java)
    │
    ▼
Service Interface + @Service Impl (e.g. GraphitiService.java)
    ├──► LLM Client (LlmClientService.java) → Spring AI → OpenAI/LM Studio
    ├──► MyBatis-Plus Mapper (dal/mysql/*.java) → PostgreSQL
    ├──► Neo4j Driver (GraphNeo4jConfig.java → Driver bean) → Neo4j Bolt
    └──► Redis Template (EmbeddingCacheService → SearchCacheConfig)
    │
    ▼
CommonResult<?> wrapper (CommonResult.java)
    │
    ▼
@GlobalExceptionHandler (GlobalExceptionHandler.java)
```

**注意:** 无 Manager 层 — services 直接调用 mappers 和 Neo4j。

### 4.2 模块职责

| 模块 | 包路径 | 职责 |
|------|--------|------|
| **graphiti** | `com.ontograph.module.graphiti` | 核心图谱操作、LLM、搜索、本体、IDE、法律图谱、剧集、社区 |
| **system** | `com.ontograph.module.system` | 认证、用户、角色、菜单、通知、搜索历史、系统配置、操作日志 |
| **common** | `com.ontograph.common` | CommonResult、BusinessException、GlobalExceptionHandler、ResultCode |
| **framework** | `com.ontograph.framework` | JWT (JwtTokenProvider, JwtAuthenticationFilter)、SecurityConfig、UserContext |

### 4.3 请求生命周期

1. Axios 发送 `Authorization: Bearer <token>` 头部
2. Spring Security 过滤器链: `JwtAuthenticationFilter` 验证 token,设置 `SecurityContextHolder`
3. Controller 接收请求
4. Service 编排: MyBatis-Plus → PostgreSQL, Neo4j Driver → Neo4j, LlmClientService → LLM
5. `CommonResult.success(data)` 包装响应
6. `GlobalExceptionHandler` 捕获 `BusinessException` → `CommonResult.error(code, message)`

### 4.4 认证流程

```
POST /auth/login → AuthController.login() → AuthServiceImpl
→ BCrypt 密码校验 → JwtTokenProvider.generateToken()
→ 返回 { token, user }
│
├─ 前端: localStorage.setItem(TOKEN_KEY, result.token)
├─ 后续请求: Authorization: Bearer <token>
└─ Token 过期 (24h): 前端调用 /auth/refresh → 后端无此端点 (死代码)
```

### 4.5 LLM 调用流程

```
API Request (e.g. entity extraction, search)
    │
    ▼
GraphitiService / EntityExtractorService / SearchPipelineService
    │
    ▼
LlmClientService (interface) → Spring AI ChatModel / EmbeddingModel
    │
    ▼
配置提供方 (application.yml):
  - ontograph.ai.llm-provider: openai
  - spring.ai.openai.base-url: LM Studio 端点
  - spring.ai.openai.api-key: <LM Studio 密钥>
    │
    ▼
LLM 响应 → 解析 → Neo4j 写回 / Redis 缓存
```

### 4.6 Neo4j 集成

- **配置:** `GraphNeo4jConfig.java` 从 `neo4j.uri/username/password` 创建 `Driver` bean
- **核心服务:** `GraphNeo4jService.java` (11,644 字节,最大的单个服务) — 通过 Cypher 直接操作
- **写路径:** Service → `GraphNeo4jService` → `driver.session()` → Cypher → Neo4j
- **读路径:** 同上,映射为 `GraphIDENode`/`GraphIDEEdge` DTO
- **无 Spring Data Neo4j:** 纯原生驱动

### 4.7 Graph IDE 前端子系统

- **组件:** `GraphCanvas.vue`, `ForceGraph.vue`, `GraphToolbar.vue`, `NodeEditModal.vue`, `AddEdgeModal.vue`, `CascadeEditModal.vue`
- **API:** `GET /graph/{id}/visualization`, `GET /graph/{id}/visualization/by-types`, `GET /graph/{id}/visualization/instances`, `POST /graph/{id}/nodes/{uuid}/expand`
- **同步:** 纯 REST 轮询 — **未使用 WebSocket 或 SSE**

### 4.8 数据一致性策略

- **双写:** 图操作在 service 事务内同时写入 PostgreSQL (元数据) 和 Neo4j (图数据)
- **导入:** `BulkImportTaskService.java` 处理分块,写入 Neo4j,在 PostgreSQL 跟踪进度
- **无 2PC:** 无分布式事务协调器;一致性是最终性的

### 4.9 启动初始化

```
SpringApplication.run(OntographApplication.java)
    │
    ├── @SpringBootApplication(scanBasePackages="com.ontograph")
    │       (排除: Anthropic, Azure OpenAI, Mistral AI 自动配置)
    │
    ├── SecurityConfig bean → CorsConfigurationSource
    │       → SecurityFilterChain (stateless, JWT 模式)
    │
    ├── GraphNeo4jConfig → Driver bean
    │
    ├── SearchCacheConfig → 搜索结果 Redis 缓存
    │
    ├── AsyncConfig → ThreadPoolTaskExecutor (异步任务执行)
    │
    ├── GraphitiAiProperties → @ConfigurationProperties("ontograph.ai")
    │
    ├── SwaggerConfig → SpringDoc OpenAPI /swagger-ui.html
    │
    └── Management → Actuator health/metrics at /actuator/*
```

**未发现 CommandLineRunner 或 ApplicationRunner** — 缓存预热是惰性的。

---

## 5. 目录结构

> 详细内容见 `.planning/codebase/STRUCTURE.md`

### 5.1 顶层布局

```
d:\projects\graphiti-java\
├── .planning/                   # GSD 规划产物
│   └── codebase/               # ← 本文档的来源: 7 份分析文档
├── docker/                     # Docker 构建和编排
├── docs/                       # 文档 (scene/, *.md)
├── ontograph-backend/           # Spring Boot 后端
├── ontograph-frontend/         # Vue 3 SPA 前端
├── playwright-test/             # E2E 测试 (Playwright)
├── scripts/                    # 工具脚本
├── sql/                        # DB schema 和迁移
│   ├── mysql/                  # MySQL schema + 初始数据
│   ├── postgresql/             # PostgreSQL schema + 初始数据
│   └── migrations/             # 跨 DBMS 迁移脚本
├── test-results/               # Playwright 测试结果
├── .env                        # 环境变量 (gitignored)
├── .env.example                # 环境变量模板
└── test-menu-tree.js           # 开发工具脚本
```

### 5.2 后端结构 (ontograph-backend/)

```
ontograph-backend/
├── pom.xml                    # Maven 构建配置
├── src/main/java/com/ontograph/
│   ├── OntographApplication.java        # @SpringBootApplication 主类
│   ├── config/
│   │   └── SwaggerConfig.java          # SpringDoc OpenAPI 配置
│   ├── common/
│   │   ├── constants/ResultCode.java  # 错误码 (SUCCESS=200, 业务码 1001-2099)
│   │   ├── exception/
│   │   │   ├── BusinessException.java
│   │   │   └── GlobalExceptionHandler.java
│   │   └── response/CommonResult.java # 统一响应包装
│   ├── framework/security/
│   │   ├── config/SecurityConfig.java # BCrypt, JWT 过滤器, CORS, 无状态
│   │   ├── jwt/
│   │   │   ├── JwtAuthenticationFilter.java
│   │   │   └── JwtTokenProvider.java
│   │   └── util/UserContext.java      # ThreadLocal 当前用户持有者
│   └── module/
│       ├── graphiti/                  # 核心图谱模块 (314 文件)
│       │   ├── config/               # AsyncConfig, GraphNeo4jConfig, SearchCacheConfig
│       │   ├── controller/admin/     # 19 个 REST 控制器
│       │   ├── dal/dataobject/       # ~15 个 MyBatis-Plus 实体
│       │   ├── dal/mysql/            # MyBatis-Plus Mapper 接口
│       │   ├── dal/repository/       # ImportTaskRepository
│       │   ├── dto/                  # DTO (~15 个)
│       │   ├── dto/batch/            # ChunkResult, BulkImportResult
│       │   ├── exception/             # 自定义异常
│       │   ├── handler/              # ~15 个处理器
│       │   ├── model/search/          # 搜索配置模型
│       │   ├── service/              # 48 个业务服务
│       │   │   ├── metadata/          # 元数据服务
│       │   │   ├── validator/         # 验证逻辑
│       │   │   ├── impl/             # 20+ 服务实现
│       │   │   └── (核心服务:)
│       │   │       ├── GraphNeo4jService.java      # Neo4j Cypher 操作 (最大, 11.6KB)
│       │   │       ├── LlmClientService.java     # LLM 客户端接口
│       │   │       ├── SearchPipelineService.java # 混合搜索编排器
│       │   │       ├── BulkImportTaskService.java # 分块 LLM 抽取 → Neo4j 批量写入
│       │   │       ├── CascadeEditService.java    # 批量属性更新
│       │   │       ├── EmbeddingCacheService.java# Redis 嵌入缓存
│       │   │       ├── EntityExtractorService.java
│       │   │       └── EdgeExtractorService.java
│       │   ├── typehandler/          # MyBatis 类型处理器
│       │   ├── util/                 # 工具类
│       │   └── vo/                   # 视图对象 (90+ 文件)
│       │       ├── graph/             # 图相关 VO
│       │       ├── node/              # 节点 VO
│       │       ├── edge/              # 边 VO
│       │       ├── ontology/          # 本体 VO
│       │       ├── llm/               # LLM VO
│       │       ├── legal/             # 法律图谱 VO
│       │       ├── search/             # 搜索 VO
│       │       └── metadata/          # 元数据 VO
│       └── system/                    # 系统模块 (49 文件)
│           ├── controller/            # 8 个系统控制器
│           │   ├── AuthController.java    # POST /auth/login, /logout, /refresh
│           │   ├── UserController.java
│           │   ├── RoleController.java
│           │   ├── MenuController.java
│           │   ├── NotificationController.java
│           │   ├── OperationLogController.java
│           │   ├── SearchHistoryController.java
│           │   └── SystemConfigController.java
│           ├── dal/dataobject/        # MyBatis-Plus 实体 (UserDO, RoleDO, MenuDO...)
│           ├── dal/mysql/             # MyBatis-Plus Mapper (UserMapper, RoleMapper...)
│           ├── dto/
│           │   ├── LoginRequest.java
│           │   └── LoginResponse.java
│           └── service/
│               ├── AuthService.java
│               ├── UserService.java
│               ├── RoleService.java
│               ├── MenuService.java
│               ├── NotificationService.java
│               ├── OperationLogService.java
│               ├── SystemConfigService.java
│               ├── SearchHistoryService.java
│               └── impl/             # 8 个服务实现
│                   ├── AuthServiceImpl.java
│                   ├── UserServiceImpl.java
│                   └── ...
└── src/main/resources/
    ├── application.yml               # 基础配置 (port 9090, JWT, MyBatis-Plus, Spring AI)
    └── application-dev.yml             # 开发配置覆盖 (数据源, Redis, Neo4j, AI 提供商)
```

### 5.3 前端结构 (ontograph-frontend/src/)

```
src/
├── main.ts                         # 应用入口: createApp, pinia, router, i18n
├── App.vue
├── router/index.ts                 # 路由 (懒加载), beforeEach 守卫
│
├── api/                           # 23 个 API 模块 (Axios 封装)
│   ├── request.ts                 # Axios 实例 (baseURL, 拦截器, token 刷新)
│   ├── auth.ts                    # login, logout, getInfo, getMenus
│   ├── graph.ts                   # 图 CRUD, IDE 可视化, schema, 级联编辑
│   ├── ontology.ts                # getFullOntology, classes, properties...
│   ├── search.ts                  # 混合搜索, 搜索管线
│   ├── prompt.ts                  # Prompt 模板 CRUD
│   ├── temporal.ts                # 历史图谱状态
│   ├── legal-import.ts            # 法律数据导入
│   ├── legal-extract.ts           # 法律实体抽取
│   ├── legal-kg-data.ts           # 法律图谱数据
│   ├── metadata.ts                # 剧集类型, 实体类别, 关系元数据
│   ├── customInstruction.ts       # 自定义指令
│   ├── node.ts                    # 节点 CRUD
│   ├── edge.ts                    # 边 CRUD
│   ├── episode.ts                  # 剧集 CRUD
│   ├── data.ts                    # 数据导入/导出
│   ├── notification.ts            # 通知
│   ├── monitor.ts                 # 系统监控
│   ├── log.ts                    # 操作日志
│   ├── user.ts                   # 用户 CRUD
│   ├── role.ts                   # 角色 CRUD
│   ├── menu.ts                   # 菜单 CRUD
│   └── system.ts                 # 系统配置, 健康检查
│
├── store/modules/
│   ├── user.ts                   # ~138 行: token, login, logout, 动态路由
│   ├── permission.ts             # ~122 行: menuList, permissions, hasMenuPermission
│   └── ontology.ts               # ~391 行: definition, classes, properties,
│                                  #     constraints, tabs (Navicat 风格), explorer tree
│
├── views/                        # 27 个页面组件
│   ├── 404/
│   ├── dashboard/
│   ├── login/
│   ├── graph/
│   │   ├── create.vue
│   │   ├── ide.vue               # Graph IDE (canvas + sidebar + detail panel)
│   │   ├── list.vue
│   │   └── temporal.vue
│   ├── data/
│   │   ├── classes.vue
│   │   ├── properties.vue
│   │   ├── constraints.vue
│   │   ├── entities.vue
│   │   ├── episodes.vue
│   │   ├── edges.vue
│   │   ├── communities.vue
│   │   ├── community-episode.vue
│   │   ├── import.vue
│   │   └── export.vue
│   ├── search/
│   ├── prompt/
│   ├── legal-kg/
│   ├── custom-instructions/
│   ├── notification/
│   ├── monitor/
│   ├── profile/
│   └── system/
│       ├── user/
│       ├── role/
│       ├── menu/
│       ├── config/
│       └── log/
│
├── components/
│   ├── Layout/
│   │   ├── BasicLayout.vue       # 顶层布局 (sidebar + header + content)
│   │   ├── DataManagerLayout.vue
│   │   ├── Header.vue
│   │   └── Sidebar.vue           # 从 permissionStore 动态渲染菜单
│   ├── Graph/
│   │   ├── GraphCanvas.vue        # D3/力导向图画布
│   │   ├── ForceGraph.vue
│   │   ├── GraphToolbar.vue
│   │   ├── NodeEditModal.vue
│   │   ├── NodeDetail.vue
│   │   ├── AddEdgeModal.vue
│   │   ├── EpisodeEditModal.vue
│   │   └── CascadeEditModal.vue
│   ├── Ontology/
│   │   ├── OntologyWorkbench.vue  # 多标签本体编辑器
│   │   ├── OntologyTabBar.vue     # 标签栏 (dirty 标记, 关闭按钮)
│   │   ├── OntologyObjectExplorer.vue  # 树视图: classes, properties, constraints, instances
│   │   ├── OntologyVisualizer.vue
│   │   ├── ClassListPanel.vue
│   │   ├── ClassEditor.vue
│   │   ├── PropertyListPanel.vue
│   │   ├── PropertyEditor.vue
│   │   ├── ConstraintListPanel.vue
│   │   ├── ConstraintEditor.vue
│   │   ├── InstanceForm.vue
│   │   ├── InstanceDataTable.vue
│   │   ├── EdgeListPanel.vue
│   │   ├── VersionHistoryPanel.vue
│   │   ├── VersionDiffViewer.vue
│   │   ├── DefinitionEditor.vue
│   │   ├── ConsistencyCheckPanel.vue
│   │   ├── BatchValidationPanel.vue
│   │   ├── DomainRuleListPanel.vue
│   │   ├── DomainRuleEditModal.vue
│   │   ├── DomainRuleTestModal.vue
│   │   ├── EpisodeTypeExplorer.vue
│   │   ├── CommunityExplorer.vue
│   │   ├── DataImportExportModal.vue
│   │   └── useOntologyKeyboard.ts
│   ├── StatsCard/
│   └── LanguageSwitcher/
│
├── composables/
│   ├── usePropertyType.ts
│   └── useOntologyKeyboard.ts
│
├── directives/
│   └── permission.ts             # v-permission 指令
│
├── utils/
│   ├── auth.ts                  # getToken, setToken, clearToken, getUser (localStorage)
│   ├── permission.ts             # generateRoutesFromMenus (MenuItem → RouteRecordRaw[])
│   ├── graph.ts                  # 图工具函数
│   ├── ontologyDiff.ts           # 版本历史 diff 计算
│   └── getPropertyRules.ts       # 属性验证规则
│
├── i18n/
│   ├── index.ts                 # vue-i18n 实例设置
│   └── locales/
│       ├── en-US.ts
│       ├── zh-CN.ts
│       ├── zh-TW.ts
│       └── ja-JP.ts
│
├── types/
│   ├── vite-env.d.ts
│   ├── graphiti.ts
│   ├── graph-ide.ts
│   └── legal-graph-v3.ts
│
└── assets/
    └── styles/
        ├── dark.less             # 暗色主题
        ├── global.less
        ├── scifi-variables.less
        ├── scifi-glass.less
        └── scifi-animation.less
```

### 5.4 SQL 结构

```
sql/
├── mysql/
│   ├── schema.sql                # MySQL schema
│   ├── init-data.sql             # MySQL 种子数据
│   ├── schema-v3.sql             # V3.0 schema 扩展
│   └── init-data-v3.sql          # V3.0 种子数据
├── postgresql/
│   ├── schema.sql                # PostgreSQL schema
│   ├── init-data.sql             # PostgreSQL 种子数据
│   ├── schema-v3.sql             # V3.0 schema 扩展
│   └── init-data-v3.sql          # V3.0 种子数据
└── migrations/
    ├── v004_episode_type_column_rename.sql
    ├── v004_episode_type_column_rename_mysql.sql
    ├── v005_episode_type_hierarchy.sql
    └── v006_ont_class_i18n.sql
```

**PostgreSQL 表清单:** `sys_user`, `sys_role`, `sys_menu`, `sys_user_role`, `sys_role_menu`, `sys_operation_log`, `sys_system_config`, `sys_notification`, `sys_notification_settings`, `ont_graph`, `ont_definition`, `ont_class`, `ont_property`, `ont_constraint`, `ont_episode`, `ont_community`, `ont_entity_category`, `ont_episode_type`, `ont_relationship_meta`, `ont_community_type`, `ont_version_history`, `ont_custom_instruction`, `ont_search_history`, `ont_import_task`

**无 Mapper XML 文件:** MyBatis-Plus 完全使用注解查询。

---

## 6. 编码规范

> 详细内容见 `.planning/codebase/CONVENTIONS.md`

### 6.1 Java 命名规范

| 元素 | 规范 | 示例 |
|------|------|------|
| 包名 | `com.ontograph.module.{name}` | `com.ontograph.module.graphiti` |
| 类名 | PascalCase | `AuthController`, `GraphitiServiceImpl` |
| 接口 | PascalCase, 无 `I` 前缀 | `AuthService`, `GraphitiService` |
| DO | PascalCase + `DO` 后缀 | `UserDO`, `GraphMetadataDO`, `OntClassDO` |
| DTO | PascalCase + `DTO` 或 `Request` 后缀 | `EntityBatchDTO`, `LoginRequest` |
| VO | PascalCase + `VO`/`ReqVO`/`RespVO` 后缀 | `GraphInfoVO`, `CreateGraphReqVO`, `GraphListRespVO` |
| Mapper | PascalCase + `Mapper` 后缀 | `UserMapper`, `OntDefinitionMapper` |
| 字段 | camelCase | `graphId`, `classUri`, `localName` |
| 常量 | SCREAMING_SNAKE_CASE | `ResultCode.SUCCESS = 200` |

### 6.2 Lombok 使用

三种模式共存:

**模式 A — 简单 POJO (DTO/VO/DO):**
```java
@Data
public class LoginRequest implements Serializable {
    private static final long serialVersionUID = 1L;
```
- 所有简单 POJO 使用 `@Data`
- 每个 DTO/VO/DO 实现 `Serializable` 并声明 `serialVersionUID = 1L`

**模式 B — 复杂 VO (需要 builder):**
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OntClassVO {
```
- 字段多或结构复杂时使用

**模式 C — Controller 和 Service (依赖注入):**
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {
    private final AuthService authService;
```
- `@RequiredArgsConstructor` 生成构造函数注入
- 所有 Controller 和 Service 类使用 `@Slf4j`

### 6.3 Controller 模式

```java
@Tag(name = "认证管理", description = "用户登录、登出、获取用户信息等接口")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @Operation(summary = "用户登录", description = "用户通过用户名和密码登录系统，返回JWT令牌")
    @PostMapping("/login")
    public CommonResult<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {
        return CommonResult.success(authService.login(request));
    }
```

- `@Tag` — 类级别, **中文** 名称
- `@RestController` + `@RequestMapping("/api/v1/{resource}")` — URL 版本化
- `@RequiredArgsConstructor` — Lombok 生成构造函数
- 所有方法返回 `CommonResult<T>` (从不返回裸类型)
- `@Valid` 在 `@RequestBody` 参数上触发 Jakarta 验证
- `@Operation(summary = "...", description = "...")` — **中文** 方法文档

### 6.4 Service 接口 + 实现模式

- 接口在 `service/`, 实现在 `service/impl/`
- `@Service` 在实现类上
- 变更方法使用 `@Transactional(rollbackFor = Exception.class)`
- `@Slf4j` 记录日志
- 通过 `@RequiredArgsConstructor` 构造函数注入

### 6.5 异常处理规范

**统一通过 `BusinessException` + `ResultCode` 接口。**

错误码定义:
- HTTP 码: `200, 400, 401, 403, 404, 500`
- 图谱错误: `GRAPH_NOT_FOUND = 1001`, `NODE_NOT_FOUND = 1003`, `EDGE_NOT_FOUND = 1004`, `INVALID_PARAMETER = 1006`
- 本体错误: `ONTOLOGY_NOT_DEFINED = 1002`, `ONT_DRAFT_NOT_FOUND = 2000`, `ONT_GENERATION_FAILED = 2002`

**GlobalExceptionHandler 捕获:**
- `BusinessException` → `CommonResult.error(code, message)`
- `MethodArgumentNotValidException` → 400 + 串联字段错误
- `MissingServletRequestParameterException` → 400
- `IllegalArgumentException` → 400
- `Exception` → 500

### 6.6 前端 TypeScript/Vue 规范

| 元素 | 规范 | 示例 |
|------|------|------|
| Vue 组件文件 | kebab-case | `graph/ide.vue`, `system/user/index.vue` |
| 模板内组件 | PascalCase | `<NodeEditModal />`, `<OntologyObjectExplorer />` |
| Composables | camelCase, `use*.ts` | `usePropertyType.ts`, `useOntologyKeyboard.ts` |
| Store 模块 | camelCase, `use*Store` | `useUserStore`, `useOntologyStore` |
| API 模块 | kebab-case `.ts` | `graph.ts`, `auth.ts`, `menu.ts` |
| API 命名导出 | camelCase | `graphApi`, `authApi`, `getGraphStats` |

**Composition API 专用** — `<script setup lang="ts">` 贯穿整个代码库,无 Options API。

**Pinia Setup Store 模式:**
```typescript
export const useUserStore = defineStore('user', () => {
  const token = ref<string | null>(getToken())
  const userInfo = ref<LoginResult['user'] | null>(getUser())
  const login = async (username: string, password: string) => { ... }
  const logout = async () => { ... }
  const fetchUserInfo = async () => { ... }
  const fetchUserMenus = async () => { ... }
  return { token, userInfo, login, logout, fetchUserInfo, fetchUserMenus }
})
```

**权限指令:** `v-permission` (非 `v-has-permi`)

**样式:** LESS (Ant Design 的预处理器) — 无 Tailwind/UnoCSS/SCSS
**暗色主题:** 通过 `src/assets/styles/dark.less` 自定义 LESS 变量实现

### 6.7 API 模块模式

两种模式共存:

**模式 A — 对象命名空间导出:**
```typescript
export const authApi = {
  login: (data: LoginForm): Promise<LoginResult> => { ... },
  logout: (): Promise<void> => { ... },
  getInfo: (): Promise<UserInfo> => { ... },
  getMenus: (): Promise<MenuItem[]> => { ... }
}
export default authApi
```

**模式 B — 命名函数导出:**
```typescript
export const graphApi = {
  async getList(): Promise<Graph[]> { ... }
}
export async function getGraphStats(): Promise<GraphStats> { ... }
export async function listGraphs(): Promise<Graph[]> { ... }
export default graphApi
```

**Request 模块 (`src/api/request.ts`):**
- Axios 实例, Bearer token 来自 localStorage
- Token 刷新队列 (401 时基于 axios-adapter 重试)
- 业务码拦截器
- Base URL: `import.meta.env.VITE_API_BASE_URL`

### 6.8 提交规范 (Git)

**Conventional Commits 格式:** `type(scope): message`

类型: `feat`, `fix`, `docs`, `refactor`, `chore`, `build`, `config`, `move`, `perf`

分支策略: 基于主干的开发 (`main` 分支),偶尔有 `feature/` 前缀的特性分支。

### 6.9 代码质量工具

| 类别 | 状态 |
|------|------|
| 后端 ESLint / Checkstyle / Spotless | **未配置** |
| 前端 ESLint / Prettier | **未配置** |
| 前端/后端 Lint 脚本 | **不存在** |
| EditorConfig | **不存在** |
| 代码覆盖率工具 (Jacoco) | **未配置** |

---

## 7. 测试策略

> 详细内容见 `.planning/codebase/TESTING.md`

### 7.1 测试金字塔

```
┌─────────────────────────────────────────────────────────┐
│                    E2E (Playwright)                     │
│              3 spec files, 30+ test scenarios          │
│              Multi-browser: Chromium/Firefox/WebKit     │
├─────────────────────────────────────────────────────────┤
│               Integration Tests                          │
│                  NONE PRESENT                           │
│         (No Testcontainers, no @SpringBootTest)        │
├─────────────────────────────────────────────────────────┤
│          Service Unit Tests (Mockito)                   │
│         11 test classes in src/test/java/               │
│              Pure unit, no DB, no real I/O              │
├─────────────────────────────────────────────────────────┤
│           Mapper / Compile Tests                        │
│         Class.forName() compile check only              │
│              No SQL execution                           │
└─────────────────────────────────────────────────────────┘
```

### 7.2 后端测试

| 类别 | 值 |
|------|---|
| 位置 | `ontograph-backend/src/test/java/com/ontograph/` |
| 框架 | JUnit Jupiter 5 + Mockito (通过 `spring-boot-starter-test`) |
| 测试文件数 | 14 个文件, 11 个有效测试类 |
| 集成测试 | **不存在** |
| 持久化测试 | **不存在** (无 Testcontainers, 无 H2, 无嵌入式 DB) |
| 覆盖率工具 | **未配置** (无 Jacoco) |
| 质量门禁 | **不存在** (无 CI 配置) |

**测试文件清单:**

| 文件 | 描述 |
|------|------|
| `NodeServiceImplTest.java` | 图节点创建与验证绕过 |
| `EdgeServiceImplTest.java` | 图边创建逻辑 |
| `SearchPipelineServiceImplTest.java` | 最复杂 — 13 个嵌套类, 50+ 测试方法 |
| `OntologyClassServiceImplTest.java` | 类 CRUD + @Builder 测试数据 |
| `OntologyReasonerImplTest.java` | 本体推理 |
| `OntologyValidationServiceImplTest.java` | 本体验证 |
| `OntologyPropertyServiceImplTest.java` | 属性 CRUD |
| `RrfRerankerServiceTest.java` | RRF 重排 |
| `MmrRerankerServiceTest.java` | MMR 重排 |
| `SearchConfigModelTest.java` | 搜索配置模型 |
| `SearchResultCacheServiceImplTest.java` | 缓存层 (Caffeine + Redis) |
| `OntDOTest.java` | 模型单元测试 |
| `OntMapperTest.java` | 仅编译检查 (`assertDoesNotThrow(() -> Class.forName(...))`) |
| `PasswordTest.java` | 独立 `main()` 方法 — **不是 JUnit 测试** |

**Mockito 使用模式:**

```java
// 模式 A — @Mock + @InjectMocks (最常用)
@ExtendWith(MockitoExtension.class)
class NodeServiceImplTest {
    @Mock private GraphNeo4jService graphNeo4jService;
    @InjectMocks private NodeServiceImpl nodeService;
}

// 模式 B — openMocks() 手动构造
@BeforeEach
void setUp() {
    MockitoAnnotations.openMocks(this);
    reasoner = new OntologyReasonerImpl(definitionMapper, classMapper, ...);
}

// 模式 C — @MockitoSettings LENIENT
@MockitoSettings(strictness = Strictness.LENIENT)
class SearchPipelineServiceImplTest {
```

### 7.3 前端测试

| 类别 | 状态 |
|------|------|
| Vitest / Jest | **不存在** |
| Vue Test Utils | **不存在** |
| 组件测试 | **不存在** |
| E2E Playwright | ✅ 存在 (`ontograph-frontend/tests/`) |

**E2E 测试布局:**

```
ontograph-frontend/tests/                  # @playwright/test suite
├── README.md
├── user-management.spec.ts               # 核心功能测试 (15 scenarios)
├── user-management-boundary.spec.ts      # 边界条件测试 (15 scenarios)
└── screenshots/                        # 失败时自动截图

playwright-test/                          # 独立脚本
├── package.json                          # playwright ^1.61.0 (no @playwright/test)
├── test-menu-tree.js
└── test-menu-tree-scenario.js
```

**Playwright 配置 (`playwright.config.ts`):**
- 多项目: Chromium / Firefox / WebKit / Mobile Chrome
- CI 感知: `retries: 2`, `workers: 1` (CI 模式)
- 失败时自动截图和 trace
- 自动启动 web server: `pnpm dev`

**API Mocking:** 通过 `page.route()` 而非 MSW

```typescript
// 网络错误模拟
test('网络错误处理', async ({ page }) => {
  await page.route('**/api/v1/admin/system/user', route => {
    route.fulfill({ status: 500, body: 'Internal Server Error' })
  })
})

// 超时模拟
test('网络超时处理', async ({ page }) => {
  await page.route('**/api/**', async route => {
    await new Promise(resolve => setTimeout(resolve, 10000))
    await route.continue()
  })
})
```

### 7.4 质量门禁

| 类别 | 状态 |
|------|------|
| GitHub Actions / CI | **不存在** |
| 覆盖率阈值 | **不存在** |
| pre-commit hooks | **不存在** |
| Lint 检查 | **不存在** |
| maven-surefire-plugin 配置 | 仅默认配置,无特殊设置 |

`playwright.config.ts` 有 CI 感知设置 (retries, workers),表明有计划但未实现的 CI 集成。

---

## 8. 风险与隐患

> 详细内容见 `.planning/codebase/CONCERNS.md`

### 8.1 严重程度说明

| 等级 | 定义 |
|------|------|
| **HIGH** | 可主动利用或导致生产故障;下次部署前必须修复 |
| **MEDIUM** | 数据丢失、安全绕过或 UX 下降的重大风险;1 个 sprint 内修复 |
| **LOW** | 技术债务、最佳实践违规或轻微风险;安排清理 |

### 8.2 问题汇总表

| # | 等级 | 领域 | 隐患 | 证据 |
|---|------|------|------|------|
| C1 | HIGH | 安全 | LM Studio API 密钥硬编码在 YAML 中 (已提交) | `application-dev.yml:17` |
| C2 | HIGH | 安全 | Postgres 密码硬编码在 YAML 中 (已提交) | `application-dev.yml:43,203` |
| C3 | HIGH | 安全 | Neo4j 密码硬编码在 YAML 中 (已提交) | `application-dev.yml:161` |
| C4 | HIGH | 安全 | JWT 密钥硬编码在 YAML 中 (已提交) | `application.yml:31` |
| C5 | HIGH | 可靠性 | LLM 调用无超时 — `ChatClient` 无限挂起 | `OpenAiLlmClientServiceImpl.java:51-54` |
| C6 | HIGH | 数据完整性 | `UserDetailsServiceImpl` 硬编码 `admin/admin123` 在内存中;整个 DB 认证被绕过 | `UserDetailsServiceImpl.java:30-34` |
| C7 | HIGH | 可靠性 | `updateNode` 和 `updateEdge` 总是抛出 `BusinessException(500)` — 无操作存根 | `NodeServiceImpl.java:115-117`, `EdgeServiceImpl.java:105-108` |
| C8 | HIGH | 安全 | 前端 refresh-token 调用无后端端点 — 静默失效 | `request.ts:88`; `AuthController.java` — 无 `/refresh` 路由 |
| C9 | HIGH | 配置 | `application-dev.yml` (含密钥) 未在 `.gitignore` 中 — 有提交风险 | `.gitignore:6` |
| C10 | MEDIUM | 安全 | `allowedOriginPatterns: "*"` 配合 `allowCredentials: true` — 浏览器阻止此组合,CORS 在生产环境实际失效 | `SecurityConfig.java:71,74` |
| C11 | MEDIUM | 安全 | JWT 密钥明文在 YAML 中,无保证的 env-var 覆盖 | `application.yml:31` |
| C12 | MEDIUM | 安全 | `/actuator/**` 是 `permitAll` — 暴露 heap dumps, env vars, thread dumps | `SecurityConfig.java:131` |
| C13 | MEDIUM | 安全 | `/swagger-ui.html` 是 `permitAll` — 完整 API schema 未认证暴露 | `SecurityConfig.java:129` |
| C14 | MEDIUM | 安全 | JWT 存储在 `localStorage` — XSS 可窃取 token | `utils/auth.ts:15-19` |
| C15 | MEDIUM | 可靠性 | `listHistory` 有 `LIMIT 200` + 分页大小不匹配 — 最多返回 200 条记录 | `SearchHistoryServiceImpl.java:39-40` |
| C16 | MEDIUM | 可靠性 | `CompletableFuture.allOf(...).join()`: 一个 scope 失败则全部结果丢失 | `SearchPipelineServiceImpl.java:111` |
| C17 | MEDIUM | 可靠性 | 无界限的 `Executors.newFixedThreadPool` — OOM 风险 | `OpenAiLlmClientServiceImpl.java:44-45` |
| C18 | MEDIUM | 数据完整性 | `CommunityServiceImpl` 的 `@Transactional` 仅作用于 Postgres 端;Neo4j 写在事务外 | `CommunityServiceImpl.java` |
| C19 | MEDIUM | 数据完整性 | 嵌入模型变更时搜索缓存无失效机制 — 提供过期向量 | `SearchCacheConfig.java` |
| C20 | MEDIUM | 可观测性 | 无 trace-id / request-id 传播 — 无法跨异步边界关联日志 | 全局 grep 零匹配 |
| C21 | MEDIUM | 可观测性 | Prometheus 指标显式禁用 (`prometheus.enabled: false`) | `application.yml:57` |
| C22 | MEDIUM | 性能 | `getClassInstances` 每页都执行 COUNT + DATA 两次全图扫描 | `SchemaManagementServiceImpl.java:675-689` |
| C23 | MEDIUM | 性能 | `getDimensions()` 硬编码返回 `1536`,与实际模型不符 — 向量维度错配 | `OpenAiEmbedderServiceImpl.java:99` |
| C24 | LOW | 安全 | 登录时密码以明文记录在 INFO 级日志中 | `AuthServiceImpl.java:40` |
| C25 | LOW | 安全 | 无 token 时 `Authorization: Bearer` 静默忽略 — 以匿名身份继续 | `JwtAuthenticationFilter.java:49-58` |
| C26 | LOW | 可靠性 | Neo4j 驱动无连接池大小配置 — 依赖驱动默认值 | `GraphNeo4jConfig.java` |
| C27 | LOW | 数据完整性 | 无可见的 Neo4j 索引约束定义 — schema 未知 | 仓库中未找到索引 DDL |
| C28 | LOW | 可观测性 | 仅输出到 stdout;无结构化 JSON 日志;无日志聚合标签 | `application.yml:42-43` |
| C29 | LOW | 可维护性 | 后端 5 个 TODO: 2 个功能存根 (C7), 3 个清理笔记 | grep TODO 结果 |
| C30 | LOW | 可维护性 | TypeScript API 层约 15 处使用 `any` | `graph.ts`, `data.ts` 等 |
| C31 | LOW | 可维护性 | 代码中混用中文和英文 | 全局 |
| C32 | LOW | 配置 | `localhost` URL 散落在 `application-dev.yml` 中 — 仅在开发者机器上有效 | `application-dev.yml:18,41,52,68,81,159,201,212` |

### 8.3 关键 HIGH 问题详解

#### C1-C4: 密钥硬编码 (已提交到 Git)

```yaml
# application-dev.yml:17
api-key: sk-lm-sbY78sMA:Nzdd8WQMcbOzuuufmvLy  # LM Studio API 密钥
# application-dev.yml:43
password: postgres@2026!                       # 数据库密码
# application-dev.yml:161
password: password123                            # Neo4j 密码
# application.yml:31
secret: mySecretKeyForJWTTokenGeneration...     # JWT 密钥 (可预测短语)
```

`.gitignore` 仅排除根目录的 `.env`,但 `application-dev.yml` 未被忽略。

#### C6: 数据库认证被完全绕过

```java
// UserDetailsServiceImpl.java:30-34
if (!"admin".equals(username)) {
    throw new UsernameNotFoundException("用户不存在");
}
// 模拟从数据库查询的密码（admin123 的 BCrypt 加密结果）
String password = new BCryptPasswordEncoder().encode("admin123");
```

所有非 admin 用户收到 "user not found"。`sys_user` 表中所有其他用户无法登录。

#### C7: 节点和边更新是存根

```java
// NodeServiceImpl.java:115-117
// TODO: 实现节点更新逻辑
throw new BusinessException(500, "节点更新功能待实现");
```

`updateNode` 和 `updateEdge` 两个 API 端点完全不可用。

#### C8: Token 刷新端点不存在

前端 `request.ts:88` 调用 `POST /auth/refresh`,但 `AuthController` 中无此路由。前端的 token 刷新逻辑 (含请求排队) 是死代码,JWT 24h 过期后强制重新登录。

#### C5: LLM 调用无超时

```java
// OpenAiLlmClientServiceImpl.java:51-54
return chatClient.prompt()
        .user(prompt)
        .call()
        .content();  // 无 .timeout()
```

LLM 挂起时请求线程无限阻塞,在搜索管线中耗尽所有 executor 线程。

### 8.4 优先修复建议

1. **[C6, C9, C4]** 修复 `UserDetailsServiceImpl` 从 DB 读取;将所有密钥移出 YAML 到 `.env`;将 `application-dev.yml` 加入 `.gitignore`
2. **[C8]** 实现 `POST /api/v1/auth/refresh` 后端端点
3. **[C5]** 为所有 `ChatClient` 调用添加超时
4. **[C7]** 实现 `updateNode` / `updateEdge` Cypher 逻辑
5. **[C10]** 修复 CORS 配置使用具体来源
6. **[C14]** 将 JWT 存储从 `localStorage` 迁移到 `httpOnly` cookie
7. **[C12, C13]** 在非 dev profile 中限制 Actuator 和 Swagger
8. **[C19]** 在搜索缓存 key 中加入嵌入模型版本;实现失效机制
9. **[C20, C21]** 添加 trace-id 传播并启用 Prometheus 指标
10. **[C23]** 修复 `getDimensions()` 反映实际模型配置
11. **[C22]** 优化 `getClassInstances` 计数查询 (近似计数或缓存)
12. **[C18]** 为多 DB 社区操作设计 saga/补偿策略
13. **[C30, C31]** 对 API 层进行 TypeScript 严格类型化;中英文规范审计
14. **[C15, C16, C17]** 修复分页、搜索管线故障隔离、无界限线程池

---

## 9. 关键发现汇总

### 9.1 最重要的架构发现

1. **双数据库架构**: PostgreSQL (MyBatis-Plus, 49 张表用于元数据、auth RBAC、本体 schema) + Neo4j (原生 Bolt 驱动, Cypher 查询, 无 Spring Data Neo4j)
2. **LLM 高度可插拔**: Spring AI 1.1.2 + 6 种适配器 + 12+ 云端提供商配置,但开发环境指向本地 LM Studio
3. **前端本体编辑器最复杂**: `ontology.ts` Pinia store (391 行) 实现了 Navicat 风格的多标签编辑、脏标记管理、explorer 树构建
4. **Graph IDE 纯 REST**: 无 WebSocket/SSE,仅轮询/fetch;`GraphNeo4jService` 是最大的单个服务 (11.6KB)
5. **无 Manager 层**: 标准文档提到 `→ Manager → DAO`,但代码中无 `*Manager*.java` 文件

### 9.2 最重要的安全发现

1. **4 个密钥硬编码在 YAML 中,已提交到 Git** — 最严重的安全风险
2. **数据库认证被完全绕过** — 只有 admin 能登录
3. **JWT 存储在 localStorage** — XSS 可窃取
4. **Swagger 和 Actuator 在生产环境未认证** — 信息泄露

### 9.3 最重要的测试发现

1. **无集成测试** — 无 Testcontainers, 无 `@SpringBootTest`, 无 H2
2. **无前端单元测试** — 无 Vitest/Jest, 无 Vue Test Utils
3. **无代码覆盖率工具** — 无 Jacoco
4. **无 CI/CD** — GitHub Actions 等均不存在
5. **仅 E2E Playwright** — 30+ 场景,多浏览器,但缺少中间层测试

### 9.4 最重要的可维护性发现

1. **前后端均无 lint 配置** — 完全无格式化/静态检查
2. **Token 刷新是死代码** — JWT 24h 过期后强制重新登录
3. **LLM 调用无超时** — 高负载下线程耗尽
4. **搜索缓存无模型变更失效机制** — 嵌入模型变更后提供过期结果
5. **硬编码向量维度 1536** — 与实际模型不符导致静默错误

### 9.5 Evidence Index

| 文档 | 位置 | 描述 |
|------|------|------|
| STACK.md | `.planning/codebase/STACK.md` | 完整技术栈清单 |
| INTEGRATIONS.md | `.planning/codebase/INTEGRATIONS.md` | 外部集成详情 |
| ARCHITECTURE.md | `.planning/codebase/ARCHITECTURE.md` | 架构详情 |
| STRUCTURE.md | `.planning/codebase/STRUCTURE.md` | 目录结构详情 |
| CONVENTIONS.md | `.planning/codebase/CONVENTIONS.md` | 编码规范详情 |
| TESTING.md | `.planning/codebase/TESTING.md` | 测试策略详情 |
| CONCERNS.md | `.planning/codebase/CONCERNS.md` | 风险与隐患详情 |
