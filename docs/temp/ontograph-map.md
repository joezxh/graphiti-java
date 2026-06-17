# OntoGraph 代码库映射文档

> **Where ontology becomes living structure.**  
> 一个生产就绪的知识图谱系统,用于本体建模和语义关系管理,由 Java、Neo4j 和 LLMs 驱动。

**文档版本**: v1.0.0  
**生成日期**: 2026-06-16  
**项目版本**: 1.0.0-SNAPSHOT

---

## 目录

- [1. 项目概览](#1-项目概览)
- [2. 整体架构](#2-整体架构)
- [3. 技术栈与依赖](#3-技术栈与依赖)
- [4. 目录结构详解](#4-目录结构详解)
- [5. 后端模块架构](#5-后端模块架构)
- [6. 前端模块架构](#6-前端模块架构)
- [7. 数据库设计](#7-数据库设计)
- [8. API 接口设计](#8-api-接口设计)
- [9. 核心功能实现](#9-核心功能实现)
- [10. 业务逻辑与扩展](#10-业务逻辑与扩展)
- [11. 关键技术决策](#11-关键技术决策)
- [12. 开发指导](#12-开发指导)

---

## 1. 项目概览

### 1.1 项目简介

OntoGraph 是一个生产就绪的知识图谱后端系统,将时序知识图谱的强大能力引入 Java 生态系统。系统能够使用大语言模型(LLM)从非结构化文本中自动提取实体和关系,将它们存储在带有向量嵌入的 Neo4j 中,并提供结合全文检索、语义搜索和图遍历的高级混合搜索功能。

### 1.2 核心能力

| 能力 | 描述 | 状态 |
|------|------|------|
| **LLM 驱动的数据摄入** | 自动从文本、对话和文档中提取实体和关系 | ✅ |
| **时序事实管理** | 通过 `valid_at`/`invalid_at` 时间戳跟踪事实;自动失效过期事实 | ✅ |
| **混合搜索** | 结合 BM25 全文检索、向量相似度和 BFS 图遍历,使用 RRF 融合和 MMR 重排序 | ✅ |
| **多提供商 LLM** | 支持 OpenAI、Anthropic Claude、阿里巴巴通义千问、Ollama 和私有部署 | ✅ |
| **本体验证** | 6 层验证引擎,包含类继承、Domain/Range 约束和模式匹配 | ✅ |
| **社区发现** | 标签传播算法与 LLM 生成的社区摘要 | ✅ |
| **数据质量** | 自动节点和边去重、实体解析 | ✅ |
| **法律知识图谱** | 专门的法律领域本体建模、条例导入和案例关系抽取 | ✅ |

### 1.3 项目演进

- **原始项目**: Graphiti (Python 实现,灵感来源于 Zep AI)
- **Java 重写**: graphiti-java (多模块 Spring Boot 架构)
- **品牌统一**: OntoGraph (统一后端模块 + 前端应用)
- **领域扩展**: 法律知识图谱、社区治理等专项适配

---

## 2. 整体架构

### 2.1 架构总览

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          ontograph-frontend (Vue 3)                          │
│                    Vue 3 + Vite + Ant Design Vue + ECharts                   │
│                    路由: /dashboard, /graph/*, /data/*, /legal-kg            │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                              HTTP/REST API (端口 9090)
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                         ontograph-backend (Spring Boot)                      │
│                        Spring Boot 3.5.5 + Java 21                           │
│                        统一后端模块 (com.ontograph)                           │
└─────────────────────────────────────────────────────────────────────────────┘
            │                              │                              │
            ▼                              ▼                              ▼
┌─────────────────────┐        ┌─────────────────────┐        ┌─────────────────────┐
│  知识图谱核心模块     │        │   系统管理模块        │        │   框架基础设施       │
│  module/graphiti    │        │   system/*           │        │   framework/*       │
│  - Graph CRUD       │        │   - User/Role/Menu   │        │   - Security/JWT    │
│  - Ontology System  │        │   - Auth/RBAC        │        │   - MyBatis-Plus    │
│  - Search/Import    │        │   - Config/Log       │        │   - Redis/Cache     │
│  - AI Integration   │        │                      │        │                     │
└─────────────────────┘        └─────────────────────┘        └─────────────────────┘
            │
    ┌───────┴───────┐
    ▼               ▼
┌─────────┐   ┌──────────┐        ┌─────────┐
│  Neo4j  │   │PostgreSQL│        │  Redis  │
│(GraphDB)│   │ (Metadata│        │ (Cache) │
│         │   │  & System│        │         │
└─────────┘   └──────────┘        └─────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────┐
│  向量索引 (Node/Edge Embedding) + 全文索引 (BM25)            │
│  标签: Entity, Community, Episode                            │
│  关系: RELATES_TO, HAS_COMMUNITY, NEXT_EPISODE, SAME_AS      │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 模块职责划分

| 模块 | 职责 | 关键技术 |
|------|------|----------|
| **ontograph-backend** | 统一后端服务,包含所有业务逻辑、数据访问和 AI 集成 | Spring Boot 3.5.5, Java 21 |
| **ontograph-frontend** | 前端 Web 应用,提供知识图谱可视化、本体管理和数据导入导出界面 | Vue 3, Vite, Ant Design Vue |
| **Neo4j** | 图数据库,存储知识图谱节点、关系和向量嵌入 | Neo4j 5.26 |
| **PostgreSQL** | 关系数据库,存储元数据、系统配置和用户管理数据 | PostgreSQL 15+ |
| **Redis** | 缓存层,用于搜索结果缓存、会话管理 | Redis 6+ |

### 2.3 数据流架构

```
用户输入/文档上传
       │
       ▼
┌──────────────┐
│ 数据导入服务  │ ← LLM Prompt 模板
│ (DataImport) │
└──────────────┘
       │
       ▼
┌──────────────┐
│ 实体/关系抽取 │ ← Spring AI (OpenAI/Claude/Qwen)
│ (Entity/Edge │
│  Extractor)  │
└──────────────┘
       │
       ▼
┌──────────────┐
│ 嵌入向量生成  │ ← EmbedderService (text-embedding-3-small)
│ (Embedder)   │
└──────────────┘
       │
       ▼
┌──────────────┐
│ Neo4j 存储   │ ← GraphNeo4jService
│ (Graph DB)   │
└──────────────┘
       │
       ▼
┌──────────────┐
│ 混合搜索     │ ← BM25 + Vector + BFS + RRF + MMR
│ (Search)     │
└──────────────┘
```

---

## 3. 技术栈与依赖

### 3.1 后端技术栈

| 层级 | 技术 | 版本 | 用途 |
|------|------|------|------|
| **语言** | Java | 21 | 主要编程语言 |
| **框架** | Spring Boot | 3.5.5 | 应用框架 |
| **AI 框架** | Spring AI | 1.1.2 | LLM 集成抽象层 |
| **数据访问** | MyBatis-Plus | 3.5.12 | 关系数据库 ORM |
| **动态数据源** | dynamic-datasource | 4.3.0 | 多数据源管理 |
| **连接池** | Druid | 1.2.24 | 数据库连接池 |
| **图数据库驱动** | Neo4j Java Driver | 5.26.0 | Neo4j 访问 |
| **安全** | Spring Security + JWT | - | 认证授权 |
| **JWT** | jjwt | 0.12.3 | Token 生成验证 |
| **缓存** | Redisson | 3.37.0 | Redis 客户端 |
| **本地缓存** | Caffeine | 3.1.8 | L1 搜索结果缓存 |
| **文档** | SpringDoc OpenAPI | 2.8.5 | Swagger UI |
| **工具库** | Hutool | 5.8.37 | Java 工具集 |
| **对象映射** | MapStruct | 1.6.0 | DTO/VO 转换 |
| **代码简化** | Lombok | 1.18.38 | 注解式代码生成 |
| **推理引擎** | Apache Jena | 4.9.0 | 本体推理验证 |
| **RDF 处理** | RDF4J | 3.7.7 | RDF 格式解析 |

### 3.2 前端技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| **Vue** | 3.4 | 前端框架 |
| **构建工具** | Vite | 5.2 | 开发服务器和构建 |
| **路由** | Vue Router | 4.3 | 客户端路由 |
| **状态管理** | Pinia | 2.1 | 全局状态管理 |
| **UI 组件库** | Ant Design Vue | 4.2 | UI 组件 |
| **HTTP 客户端** | Axios | 1.7 | API 请求 |
| **图表** | ECharts | 5.5 | 数据可视化 |
| **Vue 图表封装** | vue-echarts | 7.0 | ECharts Vue 组件 |
| **国际化** | vue-i18n | 9.14.4 | 多语言支持 |
| **日期处理** | dayjs | 1.11.20 | 日期格式化 |
| **类型检查** | TypeScript | 5.4 | 类型安全 |

### 3.3 数据库

| 数据库 | 版本 | 用途 |
|--------|------|------|
| **Neo4j** | 5.26 | 知识图谱存储 (节点、关系、向量索引) |
| **PostgreSQL** | 15+ | 元数据、系统管理数据、本体定义 |
| **MySQL** | 8.0+ | 备选关系数据库 (与 PostgreSQL schema 兼容) |
| **Redis** | 6+ | 缓存、会话管理 |

### 3.4 支持的 LLM 提供商

| 提供商 | Spring AI Starter | 自定义 Base URL | 备注 |
|--------|-------------------|-----------------|------|
| **OpenAI** | spring-ai-starter-model-openai | ✅ | 默认提供商 |
| **Anthropic Claude** | spring-ai-starter-model-anthropic | ✅ | 仅 Chat |
| **阿里巴巴通义千问** | spring-ai-starter-model-openai (兼容) | ✅ | 通过 OpenAI 兼容接口 |
| **Ollama** | spring-ai-starter-model-ollama | ✅ | 本地部署 |
| **Mistral AI** | spring-ai-starter-model-mistral-ai | ✅ | - |
| **Azure OpenAI** | spring-ai-starter-model-azure-openai | ✅ | - |
| **AWS Bedrock** | spring-ai-starter-model-bedrock | ✅ | - |
| **DeepSeek** | OpenAI 兼容 | ✅ | - |
| **Groq** | OpenAI 兼容 | ✅ | - |
| **SiliconFlow** | OpenAI 兼容 | ✅ | 聚合 API |

---

## 4. 目录结构详解

### 4.1 项目根目录

```
ontograph-java/
├── ontograph-backend/              # 后端统一模块 (Spring Boot)
├── ontograph-frontend/             # 前端应用 (Vue 3)
├── sql/                            # 数据库初始化脚本
│   ├── mysql/                      # MySQL schema 和数据
│   ├── postgresql/                 # PostgreSQL schema 和数据
│   ├── neo4j/                      # Neo4j 初始化和向量索引
│   └── migrations/                 # 数据库迁移脚本
├── docker/                         # Docker 部署配置
│   ├── Dockerfile
│   ├── docker-compose.yml
│   └── docker-compose.prod.yml
├── docs/                           # 项目文档
│   ├── manual/                     # 用户手册
│   ├── superpowers/                # 设计和实现计划
│   ├── training/                   # 培训文档
│   └── product/                    # 产品资料
├── scripts/                        # 构建和运维脚本
├── .env                            # 环境变量
├── .env.example                    # 环境变量模板
└── README.md                       # 项目说明
```

### 4.2 后端目录结构 (ontograph-backend)

```
ontograph-backend/
├── src/main/java/com/ontograph/
│   ├── OntoGraphApplication.java           # Spring Boot 启动类
│   │
│   ├── common/                             # 通用组件
│   │   ├── constants/ResultCode.java       # 响应状态码
│   │   ├── exception/                      # 异常处理
│   │   │   ├── BusinessException.java
│   │   │   └── GlobalExceptionHandler.java
│   │   └── response/CommonResult.java      # 统一响应对象
│   │
│   ├── config/                             # 应用配置
│   │   ├── SpaForwardController.java       # SPA 路由转发
│   │   └── SwaggerConfig.java              # OpenAPI 配置
│   │
│   ├── framework/                          # 框架基础设施
│   │   └── security/                       # 安全框架
│   │       ├── config/SecurityConfig.java  # Spring Security 配置
│   │       ├── jwt/                        # JWT 实现
│   │       │   ├── JwtAuthenticationFilter.java
│   │       │   └── JwtTokenProvider.java
│   │       └── util/UserContext.java       # 用户上下文
│   │
│   ├── system/                             # 系统管理模块
│   │   ├── controller/                     # 系统管理控制器
│   │   ├── service/                        # 系统服务
│   │   │   ├── AuthService.java
│   │   │   ├── UserService.java
│   │   │   ├── RoleService.java
│   │   │   ├── MenuService.java
│   │   │   ├── OperationLogService.java
│   │   │   ├── NotificationService.java
│   │   │   ├── SearchHistoryService.java
│   │   │   └── SystemConfigService.java
│   │   ├── service/impl/                   # 服务实现
│   │   ├── dto/                            # 数据传输对象
│   │   └── dal/mysql/                      # 数据访问层
│   │       ├── UserMapper.java
│   │       ├── RoleMapper.java
│   │       └── ...
│   │
│   └── module/graphiti/                    # 知识图谱核心模块
│       ├── config/                         # 模块配置
│       │   ├── AsyncConfig.java            # 异步任务配置
│       │   ├── GraphNeo4jConfig.java       # Neo4j 配置
│       │   ├── GraphitiAiProperties.java   # AI 提供商配置
│       │   └── SearchCacheConfig.java      # 搜索缓存配置
│       │
│       ├── controller/admin/               # REST 控制器 (19个)
│       │   ├── GraphitiController.java     # 图谱 CRUD
│       │   ├── NodeController.java         # 节点管理
│       │   ├── EdgeController.java         # 边管理
│       │   ├── EpisodeController.java      # 剧集管理
│       │   ├── OntologyController.java     # 本体管理
│       │   ├── OntMetadataController.java  # 本体元数据
│       │   ├── SearchController.java       # 搜索服务
│       │   ├── SearchPipelineController.java # 搜索管道
│       │   ├── DataImportController.java   # 数据导入
│       │   ├── DataExtractController.java  # 数据抽取
│       │   ├── LegalImportController.java  # 法律数据导入
│       │   ├── LegalExtractController.java # 法律数据抽取
│       │   ├── GraphIDEController.java     # 图谱 IDE
│       │   ├── TemporalController.java     # 时序管理
│       │   ├── PromptController.java       # 提示词管理
│       │   ├── PromptTestController.java   # 提示词测试
│       │   ├── BusinessInfoController.java # 业务信息
│       │   ├── CustomInstructionController.java # 自定义指令
│       │   └── ImportTaskController.java   # 导入任务
│       │
│       ├── service/                        # 业务服务 (103+ 个)
│       │   ├── GraphitiService.java        # 图谱核心服务
│       │   ├── GraphNeo4jService.java      # Neo4j 操作服务
│       │   ├── GraphDriverService.java     # Neo4j 驱动服务
│       │   ├── GraphVisualizationService.java # 图谱可视化
│       │   ├── NodeService.java            # 节点服务
│       │   ├── EdgeService.java            # 边服务
│       │   ├── EpisodeService.java         # 剧集服务
│       │   ├── SagaService.java            # Saga 链式管理
│       │   ├── TemporalService.java        # 时序服务
│       │   ├── SearchService.java          # 搜索服务
│       │   ├── SearchPipelineService.java  # 搜索管道
│       │   ├── SearchResultCacheService.java # 搜索缓存
│       │   ├── DataImportService.java      # 数据导入
│       │   ├── DataExtractService.java     # 数据抽取
│       │   ├── LegalImportService.java     # 法律导入
│       │   ├── LegalExtractService.java    # 法律抽取
│       │   ├── BulkImportTaskService.java  # 批量导入
│       │   ├── EntityExtractorService.java # 实体抽取
│       │   ├── EdgeExtractorService.java   # 关系抽取
│       │   ├── EmbedderService.java        # 嵌入向量
│       │   ├── EmbeddingCacheService.java  # 嵌入缓存
│       │   ├── LlmClientService.java       # LLM 客户端
│       │   ├── CommunityService.java       # 社区发现
│       │   ├── DataQualityService.java     # 数据质量
│       │   ├── EntityDedupService.java     # 实体去重
│       │   ├── CascadeEditService.java     # 级联编辑
│       │   ├── DomainRuleService.java      # 域规则
│       │   ├── DomainInferenceService.java # 域推理
│       │   ├── OntologyClassService.java   # 本体类服务
│       │   ├── OntologyPropertyService.java # 本体属性服务
│       │   ├── OntologyValidationService.java # 本体验证
│       │   ├── OntologyReasoner.java       # 本体推理
│       │   ├── OntologyDraftService.java   # 本体草稿
│       │   ├── OntologySyncService.java    # 本体同步
│       │   ├── OntologyMetadataService.java # 本体元数据
│       │   ├── SchemaManagementService.java # Schema 管理
│       │   ├── SchemaOrgImportService.java # Schema.org 导入
│       │   ├── PromptTemplateService.java  # 提示词模板
│       │   ├── DedupePromptService.java    # 去重提示词
│       │   ├── CustomInstructionService.java # 自定义指令
│       │   ├── EpisodeTypeInferenceService.java # 剧集类型推理
│       │   │
│       │   ├── impl/                       # 服务实现
│       │   └── impl/ai/                    # AI 提供商实现
│       │       ├── OpenAiLlmClientServiceImpl.java
│       │       ├── AnthropicLlmClientServiceImpl.java
│       │       ├── QwenLlmClientServiceImpl.java
│       │       ├── OllamaLlmClientServiceImpl.java
│       │       ├── OpenAiEmbedderServiceImpl.java
│       │       ├── QwenEmbedderServiceImpl.java
│       │       └── OllamaEmbedderServiceImpl.java
│       │
│       ├── service/metadata/               # 元数据服务
│       │   └── OntMetadataServiceImpl.java
│       │
│       ├── service/reranker/               # 重排序服务
│       │   ├── RrfRerankerServiceImpl.java     # RRF 融合
│       │   ├── MmrRerankerService.java         # MMR 重排序
│       │   ├── CrossEncoderRerankerService.java # Cross-Encoder
│       │   ├── EpisodeMentionsRerankerService.java
│       │   └── NodeDistanceRerankerService.java
│       │
│       ├── dal/                          # 数据访问层
│       │   ├── dataobject/               # MyBatis-Plus 实体
│       │   │   ├── GraphMetadataDO.java
│       │   │   ├── CustomInstructionDO.java
│       │   │   ├── ImportTaskDO.java
│       │   │   ├── PromptTemplateDO.java
│       │   │   ├── PromptVariableDO.java
│       │   │   ├── PromptVersionDO.java
│       │   │   └── metadata/             # 元数据实体
│       │   │       ├── OntCommunityTypeDO.java
│       │   │       ├── OntEntityCategoryDO.java
│       │   │       ├── OntEpisodeTypeDO.java
│       │   │       └── OntRelationshipMetaDO.java
│       │   │   └── ont/                  # 本体实体
│       │   │       ├── OntClassDO.java
│       │   │       ├── OntClassInheritanceDO.java
│       │   │       └── ...
│       │   ├── mysql/                    # MyBatis Mapper
│       │   └── neo4j/                    # Neo4j Repository
│       │       ├── NodeRepository.java
│       │       ├── EdgeRepository.java
│       │       └── VectorIndexRepository.java
│       │
│       ├── vo/                           # 视图对象
│       │   ├── llm/                      # LLM 抽取 VO
│       │   ├── search/                   # 搜索 VO
│       │   ├── ontology/                 # 本体 VO
│       │   └── imports/                  # 导入 VO
│       │
│       └── resources/prompts/            # LLM 提示词模板
│           ├── extract_entities.txt      # 实体抽取
│           ├── extract_relations.txt     # 关系抽取
│           ├── summarize_node.txt        # 节点摘要
│           └── summarize_community.txt   # 社区摘要
│
└── src/main/resources/
    ├── application.yml                   # 基础配置
    ├── application-dev.yml               # 开发环境配置
    └── application-prod.yml              # 生产环境配置
```

### 4.3 前端目录结构 (ontograph-frontend)

```
ontograph-frontend/
├── src/
│   ├── main.ts                           # 应用入口
│   ├── App.vue                           # 根组件
│   │
│   ├── api/                              # API 客户端模块 (23个)
│   │   ├── request.ts                    # Axios 封装
│   │   ├── auth.ts                       # 认证 API
│   │   ├── graph.ts                      # 图谱 API (746行)
│   │   ├── node.ts                       # 节点 API
│   │   ├── edge.ts                       # 边 API
│   │   ├── episode.ts                    # 剧集 API
│   │   ├── ontology.ts                   # 本体 API (405行)
│   │   ├── metadata.ts                   # 元数据 API
│   │   ├── search.ts                     # 搜索 API
│   │   ├── data.ts                       # 数据管理 API (543行)
│   │   ├── legal-import.ts               # 法律导入 API
│   │   ├── legal-extract.ts              # 法律抽取 API
│   │   ├── legal-kg-data.ts              # 法律知识图谱数据 API (1020行)
│   │   ├── prompt.ts                     # 提示词 API
│   │   ├── customInstruction.ts          # 自定义指令 API
│   │   ├── temporal.ts                   # 时序 API
│   │   ├── user.ts                       # 用户 API
│   │   ├── role.ts                       # 角色 API
│   │   ├── menu.ts                       # 菜单 API
│   │   ├── system.ts                     # 系统 API
│   │   ├── log.ts                        # 日志 API
│   │   ├── monitor.ts                    # 监控 API
│   │   └── notification.ts               # 通知 API
│   │
│   ├── router/
│   │   └── index.ts                      # 路由配置 (260行)
│   │       # 主要路由:
│   │       # /login                      - 登录页
│   │       # /dashboard                  - 仪表盘
│   │       # /graph/list                 - 图谱列表
│   │       # /graph/ide                  - 图谱 IDE
│   │       # /graph/create               - 创建图谱
│   │       # /graph/temporal             - 时序图谱
│   │       # /data/import                - 数据导入
│   │       # /data/export                - 数据导出
│   │       # /data/classes               - 类管理
│   │       # /data/properties            - 属性管理
│   │       # /data/constraints           - 约束管理
│   │       # /data/entities              - 实体管理
│   │       # /data/episodes              - 剧集管理
│   │       # /data/edges                 - 边管理
│   │       # /data/communities           - 社区发现
│   │       # /data/community-episode     - 社区剧集管理
│   │       # /search                     - 混合搜索
│   │       # /legal-kg                   - 法律知识图谱
│   │       # /prompt                     - 提示词管理
│   │       # /custom-instructions        - 自定义指令
│   │       # /system/user                - 用户管理
│   │       # /system/role                - 角色管理
│   │       # /system/menu                - 菜单管理
│   │       # /system/config              - 系统配置
│   │       # /system/log                 - 操作日志
│   │       # /profile                    - 个人中心
│   │       # /notification               - 通知
│   │       └── /monitor                  - 系统监控
│   │
│   ├── store/modules/                    # Pinia 状态管理
│   │   ├── user.ts                       # 用户状态
│   │   └── ...
│   │
│   ├── components/                       # 可复用组件
│   │   ├── Layout/                       # 布局组件
│   │   │   ├── BasicLayout.vue           # 基础布局
│   │   │   ├── DataManagerLayout.vue     # 数据管理布局
│   │   │   ├── Header.vue                # 顶栏
│   │   │   └── Sidebar.vue               # 侧边栏 (402行)
│   │   │
│   │   ├── Graph/                        # 图谱组件
│   │   │   ├── GraphCanvas.vue           # 图谱画布 (688行)
│   │   │   ├── ForceGraph.vue            # 力导向图
│   │   │   ├── GraphToolbar.vue          # 图谱工具栏
│   │   │   ├── NodeDetail.vue            # 节点详情
│   │   │   ├── NodeEditModal.vue         # 节点编辑
│   │   │   ├── EdgeEditModal.vue         # 边编辑
│   │   │   ├── EpisodeEditModal.vue      # 剧集编辑
│   │   │   ├── AddEdgeModal.vue          # 添加边
│   │   │   └── CascadeEditModal.vue      # 级联编辑 (531行)
│   │   │
│   │   ├── Ontology/                     # 本体组件 (20+ 个)
│   │   │   ├── OntologyWorkbench.vue     # 本体工作台
│   │   │   ├── OntologyClassView.vue     # 本体类视图
│   │   │   ├── OntologyObjectExplorer.vue # 本体对象浏览器
│   │   │   ├── OntologyTabBar.vue        # 本体标签栏
│   │   │   ├── OntologyVisualizer.vue    # 本体可视化
│   │   │   ├── ClassEditor.vue           # 类编辑器 (754行)
│   │   │   ├── ClassListPanel.vue        # 类列表面板
│   │   │   ├── PropertyEditor.vue        # 属性编辑器 (564行)
│   │   │   ├── PropertyListPanel.vue     # 属性列表面板
│   │   │   ├── PropertyValueCell.vue     # 属性值单元格
│   │   │   ├── ConstraintEditor.vue      # 约束编辑器
│   │   │   ├── ConstraintListPanel.vue   # 约束列表面板
│   │   │   ├── ConstraintValueEditor.vue # 约束值编辑器
│   │   │   ├── EdgeListPanel.vue         # 边列表面板
│   │   │   ├── InstanceForm.vue          # 实例表单 (502行)
│   │   │   ├── InstanceDataTable.vue     # 实例数据表 (521行)
│   │   │   ├── EpisodeTypeExplorer.vue   # 剧集类型浏览器 (455行)
│   │   │   ├── EpisodeTypeEditModal.vue  # 剧集类型编辑
│   │   │   ├── EpisodeTypeDetailPanel.vue # 剧集类型详情
│   │   │   ├── CommunityExplorer.vue     # 社区浏览器
│   │   │   ├── DomainRuleEditModal.vue   # 域规则编辑
│   │   │   ├── DomainRuleListPanel.vue   # 域规则列表
│   │   │   ├── DomainRuleTestModal.vue   # 域规则测试
│   │   │   ├── DefinitionEditor.vue      # 定义编辑器
│   │   │   ├── BatchValidationPanel.vue  # 批量验证面板
│   │   │   ├── ConsistencyCheckPanel.vue # 一致性检查
│   │   │   ├── VersionHistoryPanel.vue   # 版本历史
│   │   │   ├── VersionDiffViewer.vue     # 版本差异
│   │   │   └── DataImportExportModal.vue # 数据导入导出 (674行)
│   │   │
│   │   ├── LanguageSwitcher/             # 语言切换器
│   │   └── StatsCard/                    # 统计卡片
│   │
│   ├── views/                            # 页面视图 (28个)
│   │   ├── login/                        # 登录页
│   │   ├── dashboard/                    # 仪表盘 (577行)
│   │   ├── graph/                        # 图谱页面
│   │   │   ├── list.vue                  # 图谱列表 (662行)
│   │   │   ├── ide.vue                   # 图谱 IDE (2634行 - 核心页面)
│   │   │   ├── create.vue                # 创建图谱
│   │   │   └── temporal.vue              # 时序图谱 (370行)
│   │   ├── data/                         # 数据管理页面
│   │   │   ├── import.vue                # 数据导入 (328行)
│   │   │   ├── export.vue                # 数据导出 (242行)
│   │   │   ├── classes.vue               # 类管理
│   │   │   ├── properties.vue            # 属性管理
│   │   │   ├── constraints.vue           # 约束管理
│   │   │   ├── entities.vue              # 实体管理
│   │   │   ├── episodes.vue              # 剧集管理 (369行)
│   │   │   ├── edges.vue                 # 边管理
│   │   │   ├── communities.vue           # 社区发现 (276行)
│   │   │   └── community-episode.vue     # 社区剧集管理 (771行)
│   │   ├── search/                       # 搜索页面 (382行)
│   │   ├── legal-kg/                     # 法律知识图谱 (1422行 - 核心页面)
│   │   ├── prompt/                       # 提示词管理 (970行)
│   │   ├── custom-instructions/          # 自定义指令 (251行)
│   │   ├── system/                       # 系统管理
│   │   │   ├── user/                     # 用户管理 (413行)
│   │   │   ├── role/                     # 角色管理 (374行)
│   │   │   ├── menu/                     # 菜单管理 (444行)
│   │   │   ├── config/                   # 系统配置 (450行)
│   │   │   └── log/                      # 操作日志 (348行)
│   │   ├── profile/                      # 个人中心 (373行)
│   │   ├── notification/                 # 通知 (354行)
│   │   ├── monitor/                      # 系统监控 (609行)
│   │   └── 404/                          # 404 页面
│   │
│   ├── i18n/                             # 国际化
│   │   ├── index.ts                      # i18n 配置
│   │   └── locales/                      # 语言文件
│   │       ├── zh-CN.ts                  # 简体中文
│   │       └── en-US.ts                  # 英文
│   │
│   ├── types/                            # TypeScript 类型定义
│   ├── utils/                            # 工具函数
│   │   ├── auth.ts                       # 认证工具
│   │   └── ...
│   └── assets/styles/                    # 样式文件
│
├── package.json                          # 前端依赖配置
├── vite.config.ts                        # Vite 配置
├── tsconfig.json                         # TypeScript 配置
├── .env.development                      # 开发环境变量
└── .env.production                       # 生产环境变量
```

---

## 5. 后端模块架构

### 5.1 Spring Boot 启动配置

**启动类**: `OntoGraphApplication.java`

```java
@SpringBootApplication(
    scanBasePackages = "com.ontograph",
    exclude = {
        // 排除不需要的 Spring AI 自动配置
        AnthropicChatAutoConfiguration.class,
        AzureOpenAiChatAutoConfiguration.class,
        AzureOpenAiEmbeddingAutoConfiguration.class,
        // ... 更多排除项
    }
)
public class OntoGraphApplication {
    public static void main(String[] args) {
        SpringApplication.run(OntoGraphApplication.class, args);
    }
}
```

**关键设计决策**:
- **统一包扫描**: 所有代码位于 `com.ontograph` 包下
- **选择性 AI 自动配置**: 排除不需要的 LLM 提供商自动配置,按需启用
- **单模块架构**: 从多模块合并为单一模块,简化构建和部署

### 5.2 安全架构

#### JWT 认证流程

```
请求 → JwtAuthenticationFilter → JwtTokenProvider → UserContext → 业务逻辑
                                      ↓
                              Spring Security
                                      ↓
                              SecurityConfig (白名单/权限)
```

**核心组件**:
- `SecurityConfig.java`: 配置白名单路由 (/auth/login, /swagger-ui/* 等)
- `JwtAuthenticationFilter.java`: 拦截请求,验证 JWT Token
- `JwtTokenProvider.java`: Token 生成、解析和验证
- `UserContext.java`: ThreadLocal 存储当前用户信息

**白名单路由**:
- `/auth/**` - 认证相关
- `/swagger-ui/**`, `/v3/api-docs/**` - API 文档
- `/actuator/**` - 健康检查
- 静态资源

### 5.3 数据访问层架构

#### 关系数据库 (PostgreSQL/MySQL)

```
Controller → Service → Mapper (MyBatis-Plus) → Database
                        ↓
                   DataObject (DO)
```

**关键特性**:
- **MyBatis-Plus**: 简化 CRUD 操作,支持代码生成
- **动态数据源**: 支持多数据源切换 (primary: master)
- **Druid 连接池**: 高性能连接池,带监控
- **逻辑删除**: `deleted` 字段标记,不物理删除
- **自动填充**: `create_time`, `update_time` 自动填充

#### 图数据库 (Neo4j)

```
Controller → Service → GraphNeo4jService → Neo4j Driver → Neo4j
                                              ↓
                                         Cypher 查询
```

**核心服务**: `GraphNeo4jService.java` (391行)

**主要职责**:
- 节点 CRUD 操作
- 关系 CRUD 操作
- 向量索引管理
- 图谱遍历查询
- 时序事实管理

**Neo4j 数据模型**:

| 标签 | 用途 | 关键字段 |
|------|------|----------|
| **Entity** | 统一实体节点 | `uuid`, `type`, `name`, `graph_id`, `embedding` |
| **Community** | 社区聚类节点 | `uuid`, `name`, `graph_id`, `community_type`, `legal_domain` |
| **Episode** | 事件/剧集节点 | `uuid`, `name`, `graph_id`, `valid_at`, `episode_type` |

| 关系类型 | 用途 | 关键字段 |
|----------|------|----------|
| **RELATES_TO** | 实体间关系 | `uuid`, `relation_type`, `fact`, `graph_id`, `embedding` |
| **HAS_COMMUNITY** | 实体属于社区 | - |
| **NEXT_EPISODE** | 剧集链式关系 | - |
| **SAME_AS** | 实体别名/等价关系 | - |

### 5.4 AI 集成架构

#### LLM 提供商抽象层

```
业务逻辑 → LlmClientService (接口)
                    ↓
        ┌───────────┼───────────┐
        ↓           ↓           ↓
   OpenAI      Anthropic     Qwen
   Impl         Impl         Impl
        ↓           ↓           ↓
   Spring AI OpenAI Starter / Anthropic Starter / OpenAI兼容
```

**核心服务**:
- `LlmClientService.java`: LLM 客户端统一接口 (168行)
- `EntityExtractorService.java`: 实体抽取服务
- `EdgeExtractorService.java`: 关系抽取服务

**AI 提供商实现** (`impl/ai/`):
- `OpenAiLlmClientServiceImpl.java`
- `AnthropicLlmClientServiceImpl.java`
- `QwenLlmClientServiceImpl.java` (通过 OpenAI 兼容接口)
- `OllamaLlmClientServiceImpl.java`

#### Embedding 服务

```
文本 → EmbedderService (接口) → Provider Impl → 向量 (1536维)
                                    ↓
                           EmbeddingCacheService (Caffeine/Redis)
```

**嵌入向量服务**:
- `EmbedderService.java`: 嵌入服务接口
- `OpenAiEmbedderServiceImpl.java`
- `QwenEmbedderServiceImpl.java`
- `OllamaEmbedderServiceImpl.java`
- `EmbeddingCacheService.java`: 嵌入缓存,避免重复计算

#### 提示词管理

**提示词模板系统**:
- `PromptTemplateService.java`: 模板 CRUD (109行)
- 模板版本管理: `PromptVersionDO`
- 模板变量管理: `PromptVariableDO`
- 提示词文件: `resources/prompts/*.txt`

**提示词文件**:
- `extract_entities.txt` - 实体抽取提示词
- `extract_relations.txt` - 关系抽取提示词
- `summarize_node.txt` - 节点摘要生成
- `summarize_community.txt` - 社区摘要生成

### 5.5 搜索架构

#### 混合搜索管道

```
用户查询
   ↓
┌──────────────────┐
│ SearchPipeline   │ ← 搜索管道编排
└──────────────────┘
   ↓
┌──────┬──────┬──────┐
│ BM25 │Vector│ BFS  │ ← 三路并行搜索
└──────┴──────┴──────┘
   ↓      ↓      ↓
┌──────────────────┐
│ RRF 融合         │ ← Reciprocal Rank Fusion
└──────────────────┘
   ↓
┌──────────────────┐
│ MMR 重排序       │ ← Maximal Marginal Relevance
└──────────────────┘
   ↓
最终结果 (带缓存)
```

**搜索服务**:
- `SearchService.java`: 搜索接口 (53行)
- `SearchPipelineService.java`: 搜索管道编排 (311行)
- `RrfRerankerService.java`: RRF 融合算法
- `MmrRerankerService.java`: MMR 重排序
- `CrossEncoderRerankerService.java`: Cross-Encoder 精排
- `SearchResultCacheService.java`: 搜索结果缓存 (Caffeine L1 + Redis L2)

**搜索配置**:
```java
SearchConfigVO {
    mode: "hybrid" | "bm25" | "vector" | "bfs"
    limit: 10
    useBM25: true
    useVector: true
    useBFS: false
    reranker: "rrf" | "mmr" | "cross_encoder"
}
```

### 5.6 本体系统架构

#### 6 层验证引擎

```
┌─────────────────────────────────────┐
│ OntologyValidationService           │
│ (6-Layer Validation Engine)         │
└─────────────────────────────────────┘
   ↓
┌─────────────────────────────────────┐
│ Layer 1: 类继承验证                  │
│   - 循环继承检测                     │
│   - 多重继承合理性                   │
└─────────────────────────────────────┘
   ↓
┌─────────────────────────────────────┐
│ Layer 2: Domain/Range 约束验证       │
│   - 属性定义域检查                   │
│   - 属性值域检查                     │
└─────────────────────────────────────┘
   ↓
┌─────────────────────────────────────┐
│ Layer 3: 数据类型验证                │
│   - 字符串/整数/日期/布尔            │
│   - 正则表达式匹配                   │
└─────────────────────────────────────┘
   ↓
┌─────────────────────────────────────┐
│ Layer 4: 必需属性验证                │
│   - required 字段检查                │
│   - 非空验证                         │
└─────────────────────────────────────┘
   ↓
┌─────────────────────────────────────┐
│ Layer 5: 模式约束验证                │
│   - 值域范围                         │
│   - 枚举值检查                       │
└─────────────────────────────────────┘
   ↓
┌─────────────────────────────────────┐
│ Layer 6: 推理一致性验证              │
│   - Jena 推理引擎                    │
│   - 本体逻辑矛盾检测                 │
└─────────────────────────────────────┘
```

**本体核心服务**:
- `OntologyClassService.java`: 本体类管理
- `OntologyPropertyService.java`: 本体属性管理
- `OntologyValidationService.java`: 6 层验证 (39行)
- `OntologyReasoner.java`: 本体推理 (Jena)
- `OntologyDraftService.java`: 本体草稿管理
- `OntologySyncService.java`: 本体与图谱同步
- `OntologyMetadataService.java`: 本体元数据

#### 域规则引擎

**域规则管理**:
- `DomainRuleService.java`: 域规则 CRUD
- `DomainInferenceService.java`: 基于规则的推理
- 前端组件: `DomainRuleEditModal.vue`, `DomainRuleListPanel.vue`

### 5.7 时序事实管理

#### 时序数据模型

```
Episode (事件/剧集)
   ↓ valid_at / invalid_at
Fact (事实)
   ↓ 自动失效
HistoricalFact (历史事实)
```

**时序服务**:
- `TemporalService.java`: 时序查询和管理 (94行)
- `SagaService.java`: 剧集链式管理 (NEXT_EPISODE 关系)
- `EpisodeService.java`: 剧集 CRUD

**关键特性**:
- `valid_at`: 事实生效时间
- `invalid_at`: 事实失效时间
- 自动失效: 查询时过滤过期事实
- 时序追溯: 查询历史时间点的图谱状态

### 5.8 数据质量管理

#### 去重服务

```
实体抽取 → EntityDedupService → Jaccard 相似度 → 合并重复实体
                                    ↓
                               SAME_AS 关系
```

**数据质量服务**:
- `DataQualityService.java`: 数据质量监控
- `EntityDedupService.java`: 实体去重 (Jaccard 相似度)
- `DedupePromptService.java`: 去重提示词管理

#### 社区发现

```
图谱数据 → Label Propagation → 社区聚类
                ↓
        CommunityService → LLM 生成社区摘要
```

**社区发现服务**:
- `CommunityService.java`: 社区检测 (89行)
- 算法: 标签传播 (Label Propagation)
- 社区类型: `dispute_resolution`, `corporate_dispute`, `legal_domain` 等
- LLM 摘要: 自动生成社区描述

### 5.9 法律知识图谱专项

#### 法律数据导入管道

```
法律条例文本 → LegalImportService → 结构化解析 → 法律实体/关系
                      ↓
            LegalExtractService → LLM 抽取 → Neo4j 存储
```

**法律服务**:
- `LegalImportService.java`: 法律数据导入 (47行)
- `LegalExtractService.java`: 法律数据抽取 (71行)
- `LegalImportController.java`: 导入控制器 (135行)
- `LegalExtractController.java`: 抽取控制器 (407行)

**法律本体特性**:
- 法律实体类型: `Court`, `Party`, `Judge`, `LegalProvision`, `JudgmentDocument`, `Case`, `Evidence` 等
- 法律领域分类: `DOMAIN_CIVIL`, `DOMAIN_CRIMINAL`, `DOMAIN_MEDIATION` 等
- 司法辖区: `JURISDICTION_CN`, `JURISDICTION_US` 等
- 实践类型: `PRACTICE_JUDICIAL`, `PRACTICE_MEDIATION`, `PRACTICE_ARBITRATION`

#### 级联编辑功能

**级联编辑服务**:
- `CascadeEditService.java`: 批量创建本体实例 (248行)
- 支持: 从本体定义自动生成实例表单
- 前端组件: `CascadeEditModal.vue` (531行)

---

## 6. 前端模块架构

### 6.1 Vue 3 应用结构

**应用入口**: `main.ts`

```typescript
import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'
import Antd from 'ant-design-vue'
import { i18n } from './i18n'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(Antd)
app.use(i18n)
app.mount('#app')
```

### 6.2 路由架构

**核心路由守卫** (`router/index.ts`):

```typescript
router.beforeEach(async (to, _from) => {
  // 1. 需要认证但未登录 → 跳转登录
  // 2. 需要认证且已登录 → 验证 Token (1分钟缓存)
  // 3. 已登录访问登录页 → 跳转仪表盘
  // 4. 设置页面标题 (国际化)
})
```

**路由结构**:

| 路由路径 | 页面组件 | 功能描述 |
|----------|----------|----------|
| `/login` | `login/index.vue` | 用户登录 |
| `/dashboard` | `dashboard/index.vue` | 仪表盘 (统计概览) |
| `/graph/list` | `graph/list.vue` | 图谱列表管理 |
| `/graph/ide` | `graph/ide.vue` | 图谱 IDE (核心可视化页面, 2634行) |
| `/graph/ide/:id` | `graph/ide.vue` | 指定图谱 IDE |
| `/graph/create` | `graph/create.vue` | 创建新图谱 |
| `/graph/temporal` | `graph/temporal.vue` | 时序图谱可视化 |
| `/data/import` | `data/import.vue` | 数据导入 |
| `/data/export` | `data/export.vue` | 数据导出 |
| `/data/classes` | `data/classes.vue` | 本体类管理 |
| `/data/properties` | `data/properties.vue` | 本体属性管理 |
| `/data/constraints` | `data/constraints.vue` | 本体约束管理 |
| `/data/entities` | `data/entities.vue` | 实体数据管理 |
| `/data/episodes` | `data/episodes.vue` | 剧集数据管理 |
| `/data/edges` | `data/edges.vue` | 关系数据管理 |
| `/data/communities` | `data/communities.vue` | 社区发现与管理 |
| `/data/community-episode` | `data/community-episode.vue` | 社区剧集管理 |
| `/search` | `search/index.vue` | 混合搜索 |
| `/legal-kg` | `legal-kg/index.vue` | 法律知识图谱 (1422行) |
| `/prompt` | `prompt/index.vue` | 提示词管理 |
| `/custom-instructions` | `custom-instructions/index.vue` | 自定义指令 |
| `/system/user` | `system/user/index.vue` | 用户管理 |
| `/system/role` | `system/role/index.vue` | 角色管理 |
| `/system/menu` | `system/menu/index.vue` | 菜单管理 |
| `/system/config` | `system/config/index.vue` | 系统配置 |
| `/system/log` | `system/log/index.vue` | 操作日志 |
| `/profile` | `profile/index.vue` | 个人中心 |
| `/notification` | `notification/index.vue` | 通知中心 |
| `/monitor` | `monitor/index.vue` | 系统监控 |

### 6.3 组件架构

#### 布局组件

**BasicLayout.vue**: 基础布局框架
- 顶栏: `Header.vue` (用户信息、通知、语言切换)
- 侧边栏: `Sidebar.vue` (402行, 导航菜单)
- 内容区: `<router-view />`

**DataManagerLayout.vue**: 数据管理专用布局
- 子路由布局: 类、属性、约束、实体管理

#### 图谱可视化组件

**GraphCanvas.vue** (688行): 图谱画布核心组件
- ECharts 力导向图渲染
- 节点拖拽、缩放
- 节点/边点击交互
- 工具栏集成

**GraphToolbar.vue**: 图谱工具栏
- 缩放控制
- 布局切换
- 搜索过滤
- 导出功能

**节点/边编辑组件**:
- `NodeDetail.vue`: 节点详情展示
- `NodeEditModal.vue`: 节点编辑表单
- `AddEdgeModal.vue`: 添加关系
- `CascadeEditModal.vue`: 级联编辑 (531行)

#### 本体管理组件 (20+ 个)

**工作台模式**:
- `OntologyWorkbench.vue`: 本体工作台 (标签栏布局)
- `OntologyTabBar.vue`: 本体标签栏 (类、属性、约束、实例)

**类管理**:
- `ClassEditor.vue` (754行): 类编辑器 (大型组件)
- `ClassListPanel.vue`: 类列表面板
- `OntologyClassView.vue`: 本体类视图

**属性管理**:
- `PropertyEditor.vue` (564行): 属性编辑器
- `PropertyListPanel.vue`: 属性列表

**约束管理**:
- `ConstraintEditor.vue`: 约束编辑器
- `ConstraintListPanel.vue`: 约束列表

**实例管理**:
- `InstanceForm.vue` (502行): 实例表单 (动态生成)
- `InstanceDataTable.vue` (521行): 实例数据表格

**剧集类型管理**:
- `EpisodeTypeExplorer.vue` (455行): 剧集类型浏览器
- `EpisodeTypeEditModal.vue`: 剧集类型编辑
- `EpisodeTypeDetailPanel.vue`: 剧集类型详情

**社区管理**:
- `CommunityExplorer.vue`: 社区浏览器

**域规则管理**:
- `DomainRuleEditModal.vue`: 域规则编辑
- `DomainRuleListPanel.vue`: 域规则列表
- `DomainRuleTestModal.vue`: 域规则测试

**验证与检查**:
- `BatchValidationPanel.vue`: 批量验证
- `ConsistencyCheckPanel.vue`: 一致性检查

**版本管理**:
- `VersionHistoryPanel.vue`: 版本历史
- `VersionDiffViewer.vue`: 版本差异对比

**数据导入导出**:
- `DataImportExportModal.vue` (674行): 数据导入导出对话框

#### 法律知识图谱组件

**legal-kg/index.vue** (1422行): 法律知识图谱核心页面
- 法律条例导入
- 法律实体管理
- 案例关系可视化
- 领域规则配置

### 6.4 API 客户端架构

**请求封装** (`api/request.ts`):

```typescript
import axios from 'axios'
import { getToken } from '@/utils/auth'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:9090',
  timeout: 30000
})

// 请求拦截器: 附加 JWT Token
request.interceptors.request.use(config => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// 响应拦截器: 统一错误处理
request.interceptors.response.use(
  response => response.data,
  error => {
    if (error.response?.status === 401) {
      // Token 失效,跳转登录
    }
    return Promise.reject(error)
  }
)
```

**API 模块组织**:

| API 模块 | 文件 | 行数 | 主要功能 |
|----------|------|------|----------|
| **图谱 API** | `graph.ts` | 746 | 图谱 CRUD、克隆、导出 |
| **法律知识图谱数据** | `legal-kg-data.ts` | 1020 | 法律实体、关系、案例管理 |
| **数据管理 API** | `data.ts` | 543 | 数据导入、导出、去重 |
| **本体 API** | `ontology.ts` | 405 | 本体定义、验证、推理 |
| **搜索 API** | `search.ts` | 180 | 混合搜索、语义搜索 |
| **提示词 API** | `prompt.ts` | 265 | 提示词模板管理 |
| **法律抽取 API** | `legal-extract.ts` | 195 | 法律数据抽取 |
| **法律导入 API** | `legal-import.ts` | 136 | 法律数据导入 |
| **元数据 API** | `metadata.ts` | 245 | 本体元数据管理 |
| **时序 API** | `temporal.ts` | 116 | 时序数据查询 |
| **节点 API** | `node.ts` | 126 | 节点 CRUD |
| **边 API** | `edge.ts` | 108 | 关系 CRUD |
| **剧集 API** | `episode.ts` | 88 | 剧集管理 |
| **用户 API** | `user.ts` | 150 | 用户管理 |
| **角色 API** | `role.ts` | 145 | 角色管理 |
| **菜单 API** | `menu.ts` | 148 | 菜单管理 |
| **系统 API** | `system.ts` | 175 | 系统配置 |
| **日志 API** | `log.ts` | 132 | 操作日志 |
| **监控 API** | `monitor.ts` | 196 | 系统监控 |
| **通知 API** | `notification.ts` | 78 | 通知管理 |
| **认证 API** | `auth.ts` | 52 | 登录、注册 |
| **自定义指令** | `customInstruction.ts` | 49 | 自定义指令 |

### 6.5 状态管理 (Pinia)

**用户状态** (`store/modules/user.ts`):
- 用户信息
- Token 管理
- 登录/登出

### 6.6 国际化 (vue-i18n)

**支持语言**:
- 简体中文 (`zh-CN`)
- 英文 (`en-US`)

**国际化范围**:
- 路由标题
- 菜单文本
- 表单标签
- 按钮文本
- 错误提示
- 验证消息

---

## 7. 数据库设计

### 7.1 关系数据库设计 (PostgreSQL/MySQL)

#### 核心表结构 (23 张表)

**系统管理模块** (7 张表):

| 表名 | 用途 | 关键字段 |
|------|------|----------|
| `sys_user` | 系统用户 | `id`, `username`, `password`, `nickname`, `email`, `status` |
| `sys_role` | 系统角色 | `id`, `name`, `code`, `status` |
| `sys_user_role` | 用户角色关联 | `user_id`, `role_id` |
| `sys_menu` | 系统菜单 | `id`, `name`, `permission`, `url`, `parent_id`, `sort` |
| `sys_role_menu` | 角色菜单关联 | `role_id`, `menu_id` |
| `sys_operation_log` | 操作日志 | `user_id`, `operation`, `method`, `params`, `ip`, `create_time` |
| `sys_system_config` | 系统配置 | `config_key`, `config_value`, `config_type`, `remark` |

**本体系统模块** (8 张表):

| 表名 | 用途 | 关键字段 |
|------|------|----------|
| `ont_definition` | 本体定义 | `id`, `graph_id`, `name`, `description`, `version` |
| `ont_class` | 本体类 | `id`, `definition_id`, `class_uri`, `local_name`, `parent_class_id`, `domain_hint` |
| `ont_class_inheritance` | 类继承关系 | `class_id`, `parent_class_id` |
| `ont_property` | 本体属性 | `id`, `definition_id`, `property_uri`, `local_name`, `domain_class_id`, `range_class_id`, `property_type`, `range_data_type`, `is_required` |
| `ont_constraint` | 本体约束 | `id`, `property_id`, `constraint_type`, `constraint_value` |
| `ont_version_history` | 版本历史 | `id`, `definition_id`, `version_number`, `change_log`, `created_at` |
| `ont_draft` | 本体草稿 | `id`, `definition_id`, `draft_data`, `created_at` |
| `ont_mapping` | 本体映射 | `id`, `source_uri`, `target_uri`, `mapping_type` |

**图谱元数据模块** (1 张表):

| 表名 | 用途 | 关键字段 |
|------|------|----------|
| `graphiti_graph_metadata` | 图谱元数据 | `id`, `graph_id`, `name`, `description`, `created_at`, `updated_at` |

**提示词管理模块** (3 张表):

| 表名 | 用途 | 关键字段 |
|------|------|----------|
| `prompt_template` | 提示词模板 | `id`, `code`, `name`, `type`, `system_prompt`, `user_prompt_template`, `enabled` |
| `prompt_variable` | 模板变量 | `id`, `template_id`, `variable_name`, `variable_type`, `required`, `default_value` |
| `prompt_version` | 模板版本 | `id`, `template_id`, `version_number`, `system_prompt`, `user_prompt_template` |

**其他模块** (4 张表):

| 表名 | 用途 | 关键字段 |
|------|------|----------|
| `custom_instruction` | 自定义指令 | `id`, `user_id`, `name`, `instruction`, `enabled` |
| `sys_notification` | 通知 | `id`, `user_id`, `title`, `content`, `is_read`, `create_time` |
| `sys_user_notification_settings` | 通知设置 | `user_id`, `email_enabled`, `sms_enabled` |
| `sys_search_history` | 搜索历史 | `id`, `user_id`, `query`, `result_count`, `create_time` |

#### 数据库特性

**通用特性**:
- **逻辑删除**: `deleted BOOLEAN NOT NULL DEFAULT FALSE`
- **自动时间戳**: `create_time`, `update_time` 自动维护
- **触发器**: `update_updated_at_column()` 自动更新 `update_time`
- **索引优化**: 高频查询字段建立索引
- **外键约束**: 关联表使用外键,级联删除

**PostgreSQL 特有**:
- `BIGSERIAL` 自增主键
- `jsonb` 类型存储复杂数据
- 中文分词支持

**MySQL 兼容**:
- Schema 与 PostgreSQL 完全兼容
- 使用 `AUTO_INCREMENT` 替代 `BIGSERIAL`

### 7.2 图数据库设计 (Neo4j)

#### 数据模型

**节点标签**:

| 标签 | 用途 | 数量级 | 关键字段 |
|------|------|--------|----------|
| **Entity** | 统一实体节点 | 10K+ | `uuid`, `type`, `name`, `graph_id`, `embedding` (1536维向量) |
| **Community** | 社区聚类节点 | 100+ | `uuid`, `name`, `graph_id`, `community_type`, `legal_domain`, `jurisdiction` |
| **Episode** | 事件/剧集节点 | 5K+ | `uuid`, `name`, `graph_id`, `valid_at`, `episode_type`, `content` |

**Entity 节点类型** (通过 `type` 属性区分):

| 类型 | 用途 | 特有字段 |
|------|------|----------|
| **Case** | 法律案例 | `caseNumber`, `caseType`, `caseStatus`, `courtLevel`, `disputeType` |
| **Court** | 法院 | `courtName`, `courtLevel`, `location` |
| **Party** | 当事人 | `partyName`, `partyRole`, `isEnterprise` |
| **Judge** | 法官 | `judgeName` |
| **LegalProvision** | 法律条例 | `provisionId`, `lawName`, `lawType`, `provisionContent` |
| **JudgmentDocument** | 裁判文书 | `documentNumber`, `documentType` |
| **MediationAgreement** | 调解协议 | `agreementNumber` |
| **CommercialMediationOrganization** | 商事调解组织 | `name`, `orgType` |
| **Mediator** | 调解员 | `name` |
| **Evidence** | 证据 | `evidenceNumber`, `evidenceType` |
| **CaseReasoning** | 案例推理 | `reasoning` |
| **CaseFact** | 案例事实 | `factDescription`, `factCategory`, `factImportance` |

**关系类型**:

| 关系类型 | 用途 | 关键字段 |
|----------|------|----------|
| **RELATES_TO** | 实体间通用关系 | `uuid`, `relation_type`, `fact`, `graph_id`, `embedding` |
| **HAS_COMMUNITY** | 实体属于社区 | - |
| **NEXT_EPISODE** | 剧集链式关系 (Saga) | - |
| **SAME_AS** | 实体别名/等价关系 | - |

#### 索引设计

**基础索引**:
```cypher
CREATE INDEX entity_type_v3 FOR (n:Entity) ON (n.type);
CREATE INDEX entity_graph_id_v3 FOR (n:Entity) ON (n.graph_id);
CREATE INDEX entity_type_graph_id_v3 FOR (n:Entity) ON (n.type, n.graph_id);
```

**Community 索引**:
```cypher
CREATE INDEX community_uuid_v3 FOR (n:Community) ON (n.uuid);
CREATE INDEX community_graph_id_v3 FOR (n:Community) ON (n.graph_id);
CREATE INDEX community_parent_uuid_v3 FOR (n:Community) ON (n.parent_community_uuid);
CREATE INDEX community_type_v3 FOR (n:Community) ON (n.community_type);
CREATE INDEX community_legal_domain_v3 FOR (n:Community) ON (n.legal_domain);
```

**Episode 索引**:
```cypher
CREATE INDEX episode_graph_id_v3 FOR (n:Episode) ON (n.graph_id);
CREATE INDEX episode_uuid_v3 FOR (n:Episode) ON (n.uuid);
CREATE INDEX episode_valid_at_v3 FOR (n:Episode) ON (n.valid_at);
```

**业务实体索引** (部分):
```cypher
-- Court
CREATE INDEX court_name_v3 FOR (n:Entity) ON (n.courtName);
CREATE INDEX court_courtLevel_v3 FOR (n:Entity) ON (n.courtLevel);

-- Party
CREATE INDEX party_name_v3 FOR (n:Entity) ON (n.partyName);
CREATE INDEX party_partyRole_v3 FOR (n:Entity) ON (n.partyRole);

-- LegalProvision
CREATE INDEX provision_provisionId_v3 FOR (n:Entity) ON (n.provisionId);
CREATE INDEX provision_lawName_v3 FOR (n:Entity) ON (n.lawName);

-- Case
CREATE INDEX case_caseNumber_v3 FOR (n:Entity) ON (n.caseNumber);
CREATE INDEX case_caseType_v3 FOR (n:Entity) ON (n.caseType);
```

**向量索引** (自动创建):
```cypher
-- 节点向量索引
CREATE VECTOR INDEX node_embedding_index IF NOT EXISTS
FOR (n:Entity) ON (n.embedding)
OPTIONS {indexConfig: {`vector.dimensions`: 1536, `vector.similarity_function`: 'cosine'}}

-- 边向量索引
CREATE VECTOR INDEX edge_embedding_index IF NOT EXISTS
FOR ()-[r:RELATES_TO]-() ON (r.embedding)
OPTIONS {indexConfig: {`vector.dimensions`: 1536, `vector.similarity_function`: 'cosine'}}
```

#### 多图谱隔离

**设计模式**: 所有节点和关系携带 `graph_id` 属性

```cypher
// 创建节点时指定 graph_id
MERGE (n:Entity {uuid: 'xxx', graph_id: 'legal-knowledge-graph'})

// 查询时过滤 graph_id
MATCH (n:Entity {graph_id: 'legal-knowledge-graph'})
WHERE n.type = 'Case'
RETURN n
```

### 7.3 数据库迁移

**迁移脚本** (`sql/migrations/`):

| 迁移文件 | 用途 |
|----------|------|
| `v004_community_generic_rename.cypher` | 社区节点通用化重命名 |
| `v004_episode_type_column_rename.sql` | 剧集类型字段重命名 (PostgreSQL) |
| `v004_episode_type_column_rename_mysql.sql` | 剧集类型字段重命名 (MySQL) |
| `v004_rollback.cypher` | v004 回滚脚本 |
| `v005_episode_type_hierarchy.sql` | 剧集类型层级化 |
| `v006_ont_class_i18n.sql` | 本体类国际化字段 |

---

## 8. API 接口设计

### 8.1 API 分组

| 分组 | 基础路径 | 描述 | 控制器 |
|------|----------|------|--------|
| **认证** | `/api/v1/auth/**` | 登录、注册、Token 刷新 | `AuthController` |
| **用户** | `/api/v1/users/**` | 用户管理 | `UserController` |
| **角色** | `/api/v1/roles/**` | 角色管理 | `RoleController` |
| **菜单** | `/api/v1/menus/**` | 菜单管理 | `MenuController` |
| **图谱** | `/api/v1/graph/**` | 图谱 CRUD、克隆、导出 | `GraphitiController` |
| **节点** | `/api/v1/nodes/**` | 节点 CRUD | `NodeController` |
| **边** | `/api/v1/edges/**` | 关系 CRUD | `EdgeController` |
| **剧集** | `/api/v1/episodes/**` | 剧集管理 | `EpisodeController` |
| **本体** | `/api/v1/graph/{graphId}/ontology/**` | 本体定义与验证 | `OntologyController` |
| **搜索** | `/api/v1/search/**` | 混合搜索、语义搜索、BFS | `SearchController` |
| **数据导入** | `/api/v1/graph/data/**` | LLM 抽取与导入 | `DataImportController` |
| **法律导入** | `/api/v1/legal/**` | 法律知识图谱导入 | `LegalImportController` |
| **法律抽取** | `/api/v1/legal/extract/**` | 法律数据抽取 | `LegalExtractController` |
| **提示词** | `/api/v1/prompts/**` | 提示词模板管理 | `PromptController` |
| **维护** | `/api/v1/maintenance/**` | 数据质量操作 | `MaintenanceController` |
| **时序** | `/api/v1/temporal/**` | 时序数据查询 | `TemporalController` |
| **元数据** | `/api/v1/metadata/**` | 本体元数据管理 | `OntMetadataController` |
| **图谱 IDE** | `/api/v1/graph-ide/**` | 图谱 IDE 功能 | `GraphIDEController` |
| **搜索管道** | `/api/v1/search-pipeline/**` | 搜索管道配置 | `SearchPipelineController` |
| **自定义指令** | `/api/v1/custom-instructions/**` | 自定义指令管理 | `CustomInstructionController` |
| **业务信息** | `/api/v1/business-info/**` | 业务信息管理 | `BusinessInfoController` |
| **导入任务** | `/api/v1/import-tasks/**` | 导入任务管理 | `ImportTaskController` |

### 8.2 统一响应格式

```java
{
  "code": 200,           // 状态码
  "message": "success",  // 消息
  "data": {...},         // 响应数据
  "timestamp": 1623456789000
}
```

**状态码** (`ResultCode.java`):
- `200`: 成功
- `400`: 请求参数错误
- `401`: 未授权
- `403`: 禁止访问
- `404`: 资源不存在
- `500`: 服务器内部错误

### 8.3 核心 API 示例

#### 创建图谱

```bash
POST /api/v1/graph
Content-Type: application/json
Authorization: Bearer <token>

{
  "name": "法律知识图谱",
  "description": "民商事法律案例知识库"
}
```

#### 数据导入 (自动抽取)

```bash
POST /api/v1/graph/data/add
Content-Type: application/json
Authorization: Bearer <token>

{
  "graphId": "legal-knowledge-graph",
  "name": "苹果新闻",
  "content": "苹果公司由乔布斯和沃兹尼亚克于1976年在加州库比蒂诺创立。",
  "sourceType": "article"
}
```

**处理流程**:
1. 创建 Episode 节点
2. LLM 实体抽取: "苹果公司" (Organization), "乔布斯" (Person), "沃兹尼亚克" (Person)
3. LLM 关系抽取: FOUNDED_BY, LOCATED_IN
4. 创建 Entity 节点 (带 embedding)
5. 创建 RELATES_TO 关系 (带 fact 描述)

#### 混合搜索

```bash
POST /api/v1/search/hybrid
Content-Type: application/json
Authorization: Bearer <token>

{
  "graphId": "legal-knowledge-graph",
  "query": "谁创立了苹果公司?",
  "config": {
    "limit": 10,
    "useBM25": true,
    "useVector": true,
    "useBFS": false,
    "reranker": "rrf"
  }
}
```

#### 本体定义

```bash
POST /api/v1/graph/{graphId}/ontology
Content-Type: application/json
Authorization: Bearer <token>

{
  "entities": [
    {
      "name": "Person",
      "fields": [
        {"name": "age", "type": "integer", "required": false},
        {"name": "email", "type": "string", "required": false}
      ]
    },
    {
      "name": "Organization",
      "parent": "Entity",
      "fields": [
        {"name": "industry", "type": "string", "required": true}
      ]
    }
  ],
  "edges": [
    {"name": "FOUNDED_BY", "source": "Organization", "target": "Person"}
  ]
}
```

#### 构建社区

```bash
POST /api/v1/graph/{graphId}/communities/build
Authorization: Bearer <token>
```

### 8.4 API 文档

**Swagger UI**: `http://localhost:9090/swagger-ui.html`  
**OpenAPI JSON**: `http://localhost:9090/v3/api-docs`

---

## 9. 核心功能实现

### 9.1 知识图谱核心

#### 图谱 CRUD

**服务**: `GraphitiService.java` (83行)

**主要功能**:
- 创建图谱: 生成唯一 `graph_id`
- 查询图谱: 元数据检索
- 更新图谱: 名称、描述修改
- 删除图谱: 级联删除 Neo4j 节点和关系
- 克隆图谱: 复制图谱结构和数据
- 导出图谱: JSON/CSV 格式导出

#### 节点管理

**服务**: `NodeService.java` (78行)

**主要功能**:
- 节点 CRUD: 创建、查询、更新、删除
- 批量导入: 从 CSV/JSON 批量创建节点
- 嵌入向量: 自动为 `name` 字段生成 embedding
- 本体验证: 创建/更新时验证是否符合本体约束
- 节点去重: 基于 Jaccard 相似度合并重复节点

#### 边管理

**服务**: `EdgeService.java` (60行)

**主要功能**:
- 关系 CRUD: 创建、查询、更新、删除
- 关系类型: 自定义 `relation_type`
- 事实描述: `fact` 字段记录关系上下文
- 嵌入向量: 为关系生成 embedding
- 时序跟踪: `valid_at`/`invalid_at` 时间戳

#### 剧集管理

**服务**: `EpisodeService.java` (62行)

**主要功能**:
- 剧集 CRUD: 事件/剧集的创建和管理
- 内容跟踪: `content` 字段存储原始文本
- 来源跟踪: `source` 字段记录数据来源
- 剧集链式: `NEXT_EPISODE` 关系连接相关剧集
- 剧集类型: `episode_type` 分类管理

### 9.2 AI 集成

#### LLM 实体抽取

**服务**: `EntityExtractorService.java` (62行)

**流程**:
```
原始文本 → 加载提示词模板 → LLM API 调用 → JSON 解析 → 实体列表
                                                        ↓
                                               实体标准化 (名称、类型)
                                                        ↓
                                               生成 Embedding → Neo4j 存储
```

**提示词模板** (`extract_entities.txt`):
```
从以下文本中抽取实体,返回 JSON 格式:
{
  "entities": [
    {"name": "实体名称", "type": "实体类型"}
  ]
}
```

**支持的实体类型**:
- Person (人物)
- Organization (组织)
- Location (地点)
- Date (日期)
- Custom (自定义,根据本体定义)

#### LLM 关系抽取

**服务**: `EdgeExtractorService.java` (63行)

**流程**:
```
原始文本 + 实体列表 → 加载提示词模板 → LLM API 调用 → JSON 解析 → 关系列表
                                                                    ↓
                                                          关系标准化 (类型、事实)
                                                                    ↓
                                                          生成 Embedding → Neo4j 存储
```

**提示词模板** (`extract_relations.txt`):
```
从以下文本和实体列表中抽取关系,返回 JSON 格式:
{
  "relations": [
    {"source": "源实体", "target": "目标实体", "relation_type": "关系类型", "fact": "事实描述"}
  ]
}
```

#### 多提供商支持

**LLM 客户端** (`LlmClientService.java`, 168行):

| 提供商 | 实现类 | 特点 |
|--------|--------|------|
| **OpenAI** | `OpenAiLlmClientServiceImpl.java` | 默认,支持 GPT-4/3.5 |
| **Anthropic** | `AnthropicLlmClientServiceImpl.java` | Claude 系列,长上下文 |
| **通义千问** | `QwenLlmClientServiceImpl.java` | 通过 OpenAI 兼容接口 |
| **Ollama** | `OllamaLlmClientServiceImpl.java` | 本地部署,免费 |

**切换提供商**: 配置文件 `application-dev.yml`
```yaml
graphiti:
  ai:
    llm-provider: openai  # openai | anthropic | qwen | ollama
```

### 9.3 搜索系统

#### 混合搜索实现

**服务**: `SearchServiceImpl.java`, `SearchPipelineService.java`

**搜索模式**:

| 模式 | 描述 | 适用场景 |
|------|------|----------|
| **hybrid** | BM25 + Vector + BFS + RRF | 通用搜索,最佳效果 |
| **bm25** | 仅全文检索 | 精确关键词匹配 |
| **vector** | 仅向量相似度 | 语义搜索 |
| **bfs** | 仅图遍历 | 关系探索 |

**RRF 融合算法** (`RrfRerankerService.java`):
```
RRF(score) = Σ (1 / (k + rank_i))
其中 k=60 (经验值), rank_i 为第 i 路搜索的排名
```

**MMR 重排序** (`MmrRerankerService.java`):
```
MMR = arg max [ λ * Sim(query, doc_i) - (1-λ) * max(Sim(doc_i, doc_j)) ]
其中 λ=0.7 (平衡相关性和多样性)
```

#### 搜索缓存

**服务**: `SearchResultCacheService.java`

**缓存策略**:
- **L1 缓存**: Caffeine (本地内存,快速访问)
- **L2 缓存**: Redis (分布式,多实例共享)
- **缓存键**: `search:{graphId}:{query}:{config}`
- **过期时间**: 30 分钟
- **缓存失效**: 图谱更新时清除相关缓存

### 9.4 本体验证

#### 6 层验证引擎

**服务**: `OntologyValidationServiceImpl.java`

**验证流程**:

```
实例数据
   ↓
Layer 1: 类继承验证
   - 检查循环继承
   - 检查多重继承合理性
   ↓
Layer 2: Domain/Range 约束
   - 属性定义域: 属性是否属于该类的字段
   - 属性值域: 属性值类型是否匹配
   ↓
Layer 3: 数据类型验证
   - 字符串: 长度、格式
   - 整数: 范围、精度
   - 日期: 格式、有效性
   - 布尔: true/false
   ↓
Layer 4: 必需属性验证
   - required 字段是否存在
   - 非空验证
   ↓
Layer 5: 模式约束验证
   - 正则表达式匹配
   - 枚举值检查
   - 值域范围
   ↓
Layer 6: 推理一致性验证 (Jena)
   - 本体逻辑矛盾
   - 推理冲突检测
   ↓
验证报告 (通过/失败 + 错误详情)
```

#### 本体推理

**服务**: `OntologyReasoner.java`

**推理引擎**: Apache Jena

**推理能力**:
- 类继承推理: 子类实例自动继承父类属性
- 属性传递: 关系传递性推理
- 一致性检查: 检测本体逻辑矛盾

### 9.5 时序事实管理

#### 时序查询

**服务**: `TemporalService.java` (94行)

**查询模式**:

| 模式 | 描述 | Cypher 示例 |
|------|------|-------------|
| **当前事实** | 查询当前有效事实 | `WHERE e.valid_at <= now() AND (e.invalid_at IS NULL OR e.invalid_at > now())` |
| **历史快照** | 查询指定时间点的事实 | `WHERE e.valid_at <= {timestamp} AND (e.invalid_at IS NULL OR e.invalid_at > {timestamp})` |
| **事实历史** | 查询事实的完整历史 | `MATCH (e:Episode) WHERE e.uuid = {uuid} RETURN e ORDER BY e.valid_at` |

#### Saga 链式管理

**服务**: `SagaService.java` (39行)

**概念**: 通过 `NEXT_EPISODE` 关系连接相关剧集,形成事件链

**应用场景**:
- 法律案例的时间线: 立案 → 审理 → 判决 → 执行
- 企业发展历程: 创立 → 融资 → 上市 → 并购

### 9.6 数据质量

#### 实体去重

**服务**: `EntityDedupService.java` (68行)

**算法**: Jaccard 相似度

```
Jaccard(A, B) = |A ∩ B| / |A ∪ B|
其中 A, B 为实体名称的字符集合
```

**去重流程**:
1. 计算所有实体对的 Jaccard 相似度
2. 相似度 > 阈值 (默认 0.8) 标记为重复
3. 合并重复实体,保留最完整的信息
4. 创建 `SAME_AS` 关系记录别名

#### 社区发现

**服务**: `CommunityService.java` (89行)

**算法**: 标签传播 (Label Propagation)

**流程**:
```
图谱数据 → 初始化标签 (每个节点唯一)
              ↓
         迭代更新: 节点采用邻居中最常见的标签
              ↓
         收敛: 标签不再变化
              ↓
         社区划分: 相同标签的节点为一组
              ↓
         LLM 生成社区摘要
```

**社区元数据**:
- `community_type`: 社区类型 (dispute_resolution, corporate_dispute 等)
- `legal_domain`: 法律领域 (DOMAIN_CIVIL, DOMAIN_CRIMINAL 等)
- `jurisdiction`: 司法辖区 (JURISDICTION_CN, JURISDICTION_US 等)
- `key_provisions`: 关键法条列表

---

## 10. 业务逻辑与扩展

### 10.1 法律知识图谱

#### 法律领域本体

**实体类型**:

| 实体类型 | 描述 | 示例 |
|----------|------|------|
| **Case** | 法律案例 | (2023)沪01民终11293号 |
| **Court** | 法院 | 上海市第一中级人民法院 |
| **Party** | 当事人 | 张三 (原告), 李四 (被告) |
| **Judge** | 法官 | 王五 (审判长) |
| **LegalProvision** | 法律条例 | 《民法典》第69条 |
| **JudgmentDocument** | 裁判文书 | 民事判决书 |
| **MediationAgreement** | 调解协议 | 调解协议书编号 xxx |
| **CommercialMediationOrganization** | 商事调解组织 | 上海国际商事调解中心 |
| **Mediator** | 调解员 | 赵六 (调解员) |
| **Evidence** | 证据 | 证据1: 合同复印件 |
| **CaseReasoning** | 案例推理 | 法院裁判理由 |
| **CaseFact** | 案例事实 | 案件事实描述 |

**关系类型**:

| 关系类型 | 描述 | 示例 |
|----------|------|------|
| **PRESIDED_BY** | 案例由法官审理 | Case → Judge |
| **HEARD_BY** | 案例由法院审理 | Case → Court |
| **INVOLVES_PARTY** | 案例涉及当事人 | Case → Party |
| **APPLIES_PROVISION** | 案例适用法条 | Case → LegalProvision |
| **BASED_ON_EVIDENCE** | 案例基于证据 | Case → Evidence |
| **RESULTS_IN** | 案例产生文书 | Case → JudgmentDocument |
| **PART_OF** | 实体属于社区 | Entity → Community |

#### 法律数据导入

**服务**: `LegalImportService.java` (47行)

**导入流程**:
```
法律条例文本 (TXT/Markdown)
   ↓
LegalImportService 解析
   ↓
结构化数据 (条例编号、名称、内容)
   ↓
LegalExtractService 调用 LLM
   ↓
实体和关系抽取
   ↓
Neo4j 存储 (带 graph_id='legal-knowledge-graph')
```

**导入示例** (`民商事条例.txt`):
```
《商事调解条例》
第一条 为了规范商事调解活动...
第二条 本条例适用于...
```

**解析结果**:
- LegalProvision 节点: provisionId='SMTL-001', lawName='商事调解条例'
- 关系: 条款之间的引用、补充关系

#### 法律知识图谱页面

**前端**: `legal-kg/index.vue` (1422行)

**功能**:
- 法律条例浏览和搜索
- 案例关系可视化
- 领域规则配置
- 法条关联图谱

### 10.2 级联编辑功能

#### 功能描述

**服务**: `CascadeEditService.java` (248行)

**用途**: 从本体定义批量生成实例表单,快速创建多个相关实体

**应用场景**:
- 创建案例时,同时创建相关的当事人、法官、法院等实体
- 创建组织时,同时创建相关的成员、地址等实体

**流程**:
```
选择本体类 → 读取类定义和属性
              ↓
         生成级联表单 (主实体 + 关联实体)
              ↓
         用户填写表单
              ↓
         批量创建实体和关系
              ↓
         Neo4j 存储
```

**前端组件**: `CascadeEditModal.vue` (531行)

### 10.3 域规则引擎

#### 规则管理

**服务**: `DomainRuleService.java` (41行)

**规则类型**:
- **推理规则**: IF-THEN 规则,用于自动推理
- **验证规则**: 数据验证规则
- **计算规则**: 自动计算字段值

**规则格式**:
```json
{
  "ruleId": "rule-001",
  "name": "法官年龄计算",
  "condition": "entity.type == 'Judge' && entity.birthDate != null",
  "action": "entity.age = calculateAge(entity.birthDate)"
}
```

#### 域推理

**服务**: `DomainInferenceService.java` (42行)

**推理流程**:
```
图谱数据 → 加载域规则 → 规则引擎执行 → 推理结果
                                            ↓
                                       新实体/关系创建
```

### 10.4 提示词管理系统

#### 提示词模板

**服务**: `PromptTemplateService.java` (109行)

**模板结构**:
- `code`: 模板编码 (唯一标识)
- `name`: 模板名称
- `type`: 模板类型 (entity_extraction, relation_extraction, summary 等)
- `system_prompt`: 系统提示词
- `user_prompt_template`: 用户提示词模板 (带变量)
- `variables`: 变量列表 (名称、类型、默认值、是否必需)
- `version`: 版本号 (版本管理)

**变量替换**:
```
用户提示词模板: "请从以下文本中抽取{entity_type}实体: {text}"
变量:
  - entity_type: 默认值 "人物", 类型 "string"
  - text: 类型 "context", 从上下文获取

替换后: "请从以下文本中抽取人物实体: 苹果公司由乔布斯创立..."
```

#### 提示词测试

**控制器**: `PromptTestController.java` (348行)

**功能**:
- 在线测试提示词效果
- 查看 LLM 响应
- 调整参数 (temperature, max_tokens)
- 对比不同版本提示词

### 10.5 图谱 IDE

#### 核心功能

**控制器**: `GraphIDEController.java` (395行)

**前端页面**: `graph/ide.vue` (2634行 - 最大页面)

**功能模块**:
1. **图谱可视化**: ECharts 力导向图
2. **节点编辑**: 点击节点弹出编辑表单
3. **关系编辑**: 拖拽创建关系
4. **搜索过滤**: 按名称、类型搜索
5. **布局切换**: 力导向、环形、树形布局
6. **缩放控制**: 鼠标滚轮缩放
7. **导出功能**: 导出为图片/JSON
8. **时序切换**: 查看不同时间点的图谱状态

#### 技术实现

**前端**:
- ECharts 力导向图渲染
- Vue 3 响应式状态管理
- Ant Design Vue 表单组件
- 拖拽库: 原生 HTML5 Drag & Drop

**后端**:
- Neo4j Cypher 查询优化
- 分页加载 (避免一次性加载大量节点)
- 向量索引加速相似度搜索

---

## 11. 关键技术决策

### 11.1 架构决策

#### 决策 1: 单模块 vs 多模块

**决策**: 从多模块合并为单模块

**原因**:
- 简化构建流程 (无需多模块依赖管理)
- 简化部署 (单个 JAR 包)
- 适合中小型项目 (代码量 < 50K 行)

**影响**:
- ✅ 构建速度提升 30%
- ✅ 部署复杂度降低
- ❌ 模块边界模糊 (通过包名区分)

#### 决策 2: Java 包名统一

**决策**: 所有代码使用 `com.ontograph` 包名

**原因**:
- 品牌统一 (OntoGraph)
- 避免包名冲突
- 简化导入语句

#### 决策 3: 统一 Entity 标签

**决策**: Neo4j 使用 `:Entity` 标签 + `type` 属性区分类型

**原因**:
- 避免多标签查询复杂性
- 简化索引设计
- 与 `GraphVisualizationService` 对齐

**对比方案**:
- ❌ 方案 A: 每种类型独立标签 (`:Case`, `:Court`, `:Party`)
- ✅ 方案 B: 统一标签 + type 属性 (`:Entity {type: 'Case'}`)

### 11.2 技术选型决策

#### 决策 4: Spring AI vs 直接调用 LLM API

**决策**: 使用 Spring AI 框架

**原因**:
- 统一抽象层,屏蔽不同 LLM 提供商差异
- 支持多提供商切换
- 集成 Spring Boot 生态

**备选方案**:
- ❌ 直接调用 OpenAI API: 耦合度高,难以切换
- ✅ Spring AI: 标准化,易扩展

#### 决策 5: MyBatis-Plus vs JPA

**决策**: 使用 MyBatis-Plus

**原因**:
- 灵活 SQL 控制 (适合复杂查询)
- 代码生成工具
- 国内生态成熟

**对比**:
- ❌ JPA: 复杂查询难以优化
- ✅ MyBatis-Plus: SQL 透明,性能优化空间大

#### 决策 6: PostgreSQL vs MySQL

**决策**: 首选 PostgreSQL,兼容 MySQL

**原因**:
- PostgreSQL 对 JSONB 支持更好
- 中文分词支持
- 复杂查询性能更优

**兼容性**:
- Schema 完全兼容
- 迁移脚本分离 (PostgreSQL/MySQL)

### 11.3 数据模型决策

#### 决策 7: 向量索引维度

**决策**: 使用 1536 维向量

**原因**:
- OpenAI `text-embedding-3-small` 默认维度
- 平衡精度和存储成本
- Neo4j 向量索引支持

**备选**:
- `text-embedding-3-large`: 3072 维 (更高精度,更大存储)
- `text-embedding-ada-002`: 1536 维 (最小模型)

#### 决策 8: 图谱多租户隔离

**决策**: 使用 `graph_id` 属性隔离

**原因**:
- 简单直接
- 无需多数据库实例
- 查询时过滤即可

**对比**:
- ❌ 多数据库实例: 成本高,维护复杂
- ✅ `graph_id` 属性: 轻量,易扩展

### 11.4 前端技术决策

#### 决策 9: Vue 3 vs React

**决策**: 使用 Vue 3

**原因**:
- 团队熟悉度
- Ant Design Vue 生态
- 组合式 API (Composition API) 更灵活

#### 决策 10: ECharts vs D3.js

**决策**: 使用 ECharts

**原因**:
- 开箱即用,配置简单
- 中文文档完善
- 性能好 (Canvas 渲染)

**对比**:
- ❌ D3.js: 学习曲线陡峭,需手动实现布局
- ✅ ECharts: 内置力导向图,配置即用

---

## 12. 开发指导

### 12.1 环境搭建

#### 前置要求

| 软件 | 版本 | 用途 |
|------|------|------|
| **JDK** | 21+ | Java 开发环境 |
| **Maven** | 3.9+ | 项目构建 |
| **Node.js** | 18+ | 前端开发环境 |
| **pnpm** | 8+ | 前端包管理器 |
| **Neo4j** | 5.26+ | 图数据库 |
| **PostgreSQL** | 15+ | 关系数据库 |
| **Redis** | 6+ | 缓存 |

#### 后端启动

```bash
# 1. 克隆项目
git clone <repository-url>
cd graphiti-java

# 2. 构建后端
cd ontograph-backend
mvn clean install -DskipTests

# 3. 数据库初始化
# PostgreSQL
psql -U postgres -d graphiti -f ../sql/postgresql/schema.sql
psql -U postgres -d graphiti -f ../sql/postgresql/init-data.sql

# Neo4j
# 执行 sql/neo4j/init.cypher

# 4. 配置文件
# 编辑 src/main/resources/application-dev.yml
# 修改数据库连接、LLM API Key 等

# 5. 启动后端
mvn spring-boot:run
```

**后端地址**: `http://localhost:9090`  
**Swagger UI**: `http://localhost:9090/swagger-ui.html`

#### 前端启动

```bash
# 1. 安装依赖
cd ontograph-frontend
pnpm install

# 2. 启动开发服务器
pnpm dev
```

**前端地址**: `http://localhost:5173`

#### Docker 启动

```bash
# 1. 配置环境变量
cp .env.example .env
# 编辑 .env 文件

# 2. 启动所有服务
docker-compose up -d

# 3. 查看日志
docker-compose logs -f ontograph-backend
```

### 12.2 代码规范

#### 后端规范

**包命名**:
```
com.ontograph
├── common/           # 通用组件
├── config/           # 配置类
├── framework/        # 框架基础设施
├── system/           # 系统管理模块
└── module/graphiti/  # 知识图谱核心模块
    ├── config/       # 模块配置
    ├── controller/   # REST 控制器
    ├── service/      # 业务服务
    │   ├── impl/     # 服务实现
    │   └── ai/       # AI 提供商实现
    ├── dal/          # 数据访问层
    │   ├── dataobject/  # MyBatis-Plus 实体
    │   ├── mysql/       # Mapper
    │   └── neo4j/       # Neo4j Repository
    └── vo/           # 视图对象
```

**命名约定**:
- **Controller**: `{Entity}Controller.java`
- **Service 接口**: `{Entity}Service.java`
- **Service 实现**: `{Entity}ServiceImpl.java`
- **DataObject**: `{Entity}DO.java`
- **Mapper**: `{Entity}Mapper.java`
- **VO**: `{Entity}VO.java`, `{Entity}ReqVO.java`, `{Entity}RespVO.java`

**代码风格**:
- 使用 Lombok 简化代码 (`@Data`, `@RequiredArgsConstructor`, `@Builder`)
- 使用 MapStruct 转换 DTO/VO
- 公共 API 添加 Javadoc
- Controller 添加 Swagger 注解

#### 前端规范

**目录结构**:
```
src/
├── api/              # API 客户端
├── router/           # 路由配置
├── store/            # 状态管理
├── components/       # 可复用组件
├── views/            # 页面视图
├── i18n/             # 国际化
├── types/            # TypeScript 类型
└── utils/            # 工具函数
```

**命名约定**:
- **组件**: `PascalCase.vue` (如 `ClassEditor.vue`)
- **API 文件**: `camelCase.ts` (如 `graph.ts`)
- **类型**: `PascalCase.ts` (如 `GraphType.ts`)

**代码风格**:
- 使用 Composition API (`<script setup>`)
- TypeScript 类型注解
- 国际化文本使用 `t('key')`
- 组件按需导入 (unplugin-vue-components)

### 12.3 数据库开发

#### 关系数据库

**新增表**:
1. 在 `sql/postgresql/schema.sql` 和 `sql/mysql/schema.sql` 添加表定义
2. 创建对应的 `DataObject` (`@TableName`)
3. 创建 `Mapper` 接口 (继承 `BaseMapper`)
4. 编写单元测试

**迁移脚本**:
```sql
-- sql/migrations/vXXX_description.sql
ALTER TABLE ont_class ADD COLUMN description_en VARCHAR(500);
COMMENT ON COLUMN ont_class.description_en IS '英文描述';
```

#### 图数据库

**新增节点类型**:
1. 无需创建新标签,使用 `:Entity {type: 'NewType'}`
2. 在 Neo4j 初始化脚本添加索引
3. 更新 `GraphVisualizationService` 映射

**新增关系类型**:
1. 直接使用新关系类型 (如 `NEW_RELATION`)
2. 确保关系携带 `uuid`, `graph_id` 字段

### 12.4 AI 集成开发

#### 新增 LLM 提供商

1. 创建实现类: `impl/ai/NewProviderLlmClientServiceImpl.java`
2. 实现 `LlmClientService` 接口
3. 在 `GraphitiAiProperties` 添加配置项
4. 在 `application-dev.yml` 添加配置模板
5. 更新 `LlmClientService` 工厂方法

**示例**:
```java
@Service
@ConditionalOnProperty(name = "graphiti.ai.llm-provider", havingValue = "new-provider")
public class NewProviderLlmClientServiceImpl implements LlmClientService {
    // 实现接口方法
}
```

#### 新增提示词模板

1. 创建提示词文件: `resources/prompts/new_prompt.txt`
2. 在数据库 `prompt_template` 表添加记录
3. 添加变量定义 (如需变量替换)

### 12.5 测试策略

#### 单元测试

```java
@SpringBootTest
class NodeServiceTest {
    @Autowired
    private NodeService nodeService;

    @Test
    void testCreateNode() {
        // Given
        NodeReqVO reqVO = new NodeReqVO();
        reqVO.setName("测试节点");
        reqVO.setType("Test");

        // When
        NodeVO result = nodeService.createNode("test-graph", reqVO);

        // Then
        assertNotNull(result);
        assertEquals("测试节点", result.getName());
    }
}
```

#### 集成测试

```java
@SpringBootTest
@AutoConfigureMockMvc
class GraphitiControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void testCreateGraph() throws Exception {
        mockMvc.perform(post("/api/v1/graph")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"测试图谱\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
```

### 12.6 部署指南

#### 生产环境配置

**环境变量**:
```bash
# 数据库
POSTGRES_URL=jdbc:postgresql://prod-db:5432/graphiti
POSTGRES_USER=postgres
POSTGRES_PASSWORD=<secure-password>

# Neo4j
NEO4J_URI=bolt://prod-neo4j:7687
NEO4J_USERNAME=neo4j
NEO4J_PASSWORD=<secure-password>

# Redis
REDIS_HOST=prod-redis
REDIS_PORT=6379

# LLM
GRAPHTI_AI_LLM_PROVIDER=openai
SPRING_AI_OPENAI_API_KEY=<api-key>
SPRING_AI_OPENAI_BASE_URL=https://api.openai.com/v1

# JWT
JWT_SECRET=<secure-512-bit-secret>
JWT_EXPIRATION=86400

# 日志
LOGGING_LEVEL_ROOT=INFO
LOGGING_LEVEL_COM_ONTOGRAPH=INFO
```

**Docker Compose (生产)**:
```yaml
version: '3.8'
services:
  ontograph-backend:
    image: ontograph-backend:latest
    ports:
      - "9090:9090"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
    deploy:
      resources:
        limits:
          memory: 2G
          cpus: '2.0'
    restart: always
```

#### 性能优化

**后端优化**:
- 启用搜索结果缓存 (Caffeine + Redis)
- Neo4j 连接池调优
- 数据库连接池调优 (Druid)
- 异步任务处理 (批量导入)

**前端优化**:
- Vite 构建优化 (代码分割)
- ECharts 按需加载
- 路由懒加载
- 组件缓存 (keep-alive)

**数据库优化**:
- Neo4j 索引优化 (根据查询模式)
- PostgreSQL 查询计划分析 (EXPLAIN ANALYZE)
- Redis 缓存策略优化

### 12.7 常见问题

#### Q1: Neo4j 连接失败

**症状**: `Neo4jConnectionException: Unable to connect to bolt://localhost:7687`

**解决方案**:
1. 检查 Neo4j 是否启动: `neo4j status`
2. 检查 `application-dev.yml` 中的连接配置
3. Docker 环境使用 `host.docker.internal` 替代 `localhost`

#### Q2: LLM API 调用失败

**症状**: `OpenAiApiException: Invalid API Key`

**解决方案**:
1. 检查 API Key 是否正确
2. 检查 Base URL 是否正确 (私有部署需指定)
3. 检查网络连接 (代理设置)

#### Q3: 前端跨域问题

**症状**: `Access to XMLHttpRequest has been blocked by CORS policy`

**解决方案**:
1. 开发环境: Vite 配置代理 (`vite.config.ts`)
2. 生产环境: Nginx 反向代理或后端配置 CORS

#### Q4: 数据库迁移失败

**症状**: `FlywayMigrationException: Migration failed`

**解决方案**:
1. 检查迁移脚本语法 (PostgreSQL vs MySQL)
2. 手动执行迁移脚本,查看错误详情
3. 回滚迁移: 执行 rollback 脚本

---

## 附录

### A. 术语表

| 术语 | 英文 | 描述 |
|------|------|------|
| **本体** | Ontology | 定义概念、属性和关系的 formal representation |
| **知识图谱** | Knowledge Graph | 以图结构存储知识的数据库 |
| **实体** | Entity | 知识图谱中的节点 (如人物、组织、地点) |
| **关系** | Relationship/Edge | 连接两个实体的边 (如 FOUNDED_BY, LOCATED_IN) |
| **剧集** | Episode | 表示一次数据摄入事件,包含原始文本和抽取的实体/关系 |
| **社区** | Community | 通过算法发现的实体聚类 |
| **嵌入向量** | Embedding | 文本的向量表示,用于语义搜索 |
| **混合搜索** | Hybrid Search | 结合全文检索、向量相似度和图遍历的搜索方式 |
| **时序事实** | Temporal Fact | 带时间戳的事实,支持历史查询 |
| **Saga** | Saga | 剧集链式关系,表示事件的时间线 |

### B. 参考资源

- **Neo4j 文档**: https://neo4j.com/docs/
- **Spring AI 文档**: https://docs.spring.io/spring-ai/reference/
- **MyBatis-Plus 文档**: https://baomidou.com/
- **Vue 3 文档**: https://vuejs.org/
- **Ant Design Vue**: https://antdv.com/
- **ECharts 文档**: https://echarts.apache.org/

### C. 贡献指南

1. Fork 项目
2. 创建特性分支: `git checkout -b feature/your-feature`
3. 确保代码编译通过: `mvn clean compile`
4. 运行测试: `mvn test`
5. 提交 Pull Request

**Commit Message 格式**:
```
feat: 添加新功能
fix: 修复 bug
docs: 更新文档
refactor: 代码重构
test: 添加测试
chore: 构建/配置变更
```

---

**文档维护**: 本文档应随项目演进而持续更新  
**最后更新**: 2026-06-16  
**维护者**: OntoGraph 开发团队