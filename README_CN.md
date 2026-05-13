# Graphiti-Java

<p align="center">
  <strong>时序知识图谱后端系统</strong><br>
  <em>基于 Java 实现的 Graphiti 知识图谱系统，支持 LLM 自动实体关系提取、混合检索和时序事实管理。</em>
</p>

<p align="center">
  <a href="README.md">🇺🇸 English</a> •
  <a href="README_CN.md">🇨🇳 中文文档</a> •
  <a href=".qoder/repowiki/zh/content/快速开始.md">📖 使用手册</a>
</p>

<p align="center">
  <a href="#功能特性">功能特性</a> •
  <a href="#系统架构">系统架构</a> •
  <a href="#技术栈">技术栈</a> •
  <a href="#快速开始">快速开始</a> •
  <a href="#api文档">API文档</a> •
  <a href="#配置说明">配置说明</a> •
  <a href="#文档索引">文档索引</a>
</p>

---

## 项目概述

Graphiti-Java 是一个生产级的知识图谱后端系统，将时序知识图谱能力引入 Java 生态。系统利用大型语言模型（LLM）从非结构化文本中自动提取实体和关系，将其以向量嵌入的形式存储在 Neo4j 中，并提供结合全文检索、语义搜索和图遍历的混合检索能力。

### 核心能力

- **LLM 驱动的数据导入**：自动从文本、对话和文档中提取实体和关系
- **时序事实管理**：通过 `valid_at`/`invalid_at` 时间戳追踪事实演变；自动失效过时事实
- **混合检索**：结合 BM25 全文检索、向量相似度和 BFS 图遍历，通过 RRF 融合和 MMR 重排序
- **多厂商 LLM**：支持 OpenAI、Anthropic Claude、阿里云通义千问、Ollama 及通过自定义 base URL 接入的私有化部署
- **本体验证**：6 层验证引擎，支持类继承、Domain/Range 约束和模式匹配
- **社区发现**：标签传播算法 + LLM 生成社区摘要
- **数据质量**：节点/边自动去重、实体解析

---

## 功能特性

### 知识图谱核心

| 功能 | 说明 | 状态 |
|------|------|------|
| 图谱生命周期 | 创建、查询、更新、删除、克隆、导出 | ✅ |
| 节点管理 | 支持嵌入向量和本体验证的 CRUD | ✅ |
| 边管理 | 支持自定义关系类型和事实追踪的 CRUD | ✅ |
| 事件管理 | 带内容和来源追踪的时序事件 | ✅ |
| 本体系统 | 类层次结构、属性约束、6 层验证 | ✅ |

### AI 与检索

| 功能 | 说明 | 状态 |
|------|------|------|
| LLM 实体提取 | 通过 Spring AI 从文本中提取实体/关系 | ✅ |
| LLM 关系提取 | 带事实陈述的关系提取 | ✅ |
| 嵌入生成 | 通过 OpenAI/Ollama 嵌入模型生成文本向量 | ✅ |
| 向量索引 | Neo4j 5.x 向量索引实现语义搜索 | ✅ |
| 混合检索 | BM25 + 向量 + BFS，RRF 融合 | ✅ |
| MMR 重排序 | 最大边际相关性实现结果多样性 | ✅ |
| 社区发现 | 标签传播 + LLM 摘要生成 | ✅ |

### 数据质量与时序

| 功能 | 说明 | 状态 |
|------|------|------|
| 时序管理 | `valid_at`/`invalid_at` 自动失效 | ✅ |
| Saga 管理 | 通过 `NEXT_EPISODE` 关系链管理事件 | ✅ |
| 节点去重 | 基于 Jaccard 相似度的合并 | ✅ |
| 边去重 | 重复关系检测与删除 | ✅ |
| 实体解析 | 通过 `SAME_AS` 关系处理名称变体 | ✅ |

---

## 系统架构

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              graphiti-web                                    │
│                    Vue 3 + Vite + Ant Design Vue                             │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              graphiti-server                                 │
│                        Spring Boot 3.5.5 (入口模块)                           │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
            ┌─────────────────────────┼─────────────────────────┐
            ▼                         ▼                         ▼
┌─────────────────────┐   ┌─────────────────────┐   ┌─────────────────────┐
│ graphiti-module-core│   │ graphiti-module-sys │   │ graphiti-framework  │
│    知识图谱核心      │   │   用户/角色/菜单/认证│   │   公共/安全/        │
│  - 图谱 CRUD        │   │  - JWT 认证         │   │   MyBatis Starter   │
│  - 检索/导入        │   │  - RBAC 权限        │   │   Redis Starter     │
│  - 本体/社区        │   │  - 菜单管理         │   │                     │
└─────────────────────┘   └─────────────────────┘   └─────────────────────┘
            │
    ┌───────┴───────┐
    ▼               ▼
┌─────────┐   ┌──────────┐
│  Neo4j  │   │PostgreSQL│
│(图数据库)│   │ (元数据  │
│         │   │  与系统) │
└─────────┘   └──────────┘
    │
    ▼
┌─────────┐
│  Redis  │
│ (缓存)  │
└─────────┘
```

### 模块结构

```
graphiti-java/
├── graphiti-server/              # Spring Boot 启动入口
│   └── src/main/resources/
│       ├── application.yml       # 基础配置
│       └── application-dev.yml   # 开发环境配置
│
├── graphiti-module-core/         # 核心业务模块
│   ├── controller/admin/         # REST 控制器
│   ├── service/                  # 业务服务层
│   │   ├── impl/ai/              # LLM 厂商实现
│   │   ├── impl/                 # 服务实现
│   │   ├── GraphNeo4jService.java # Neo4j 图操作
│   │   ├── SearchService.java    # 混合检索
│   │   ├── DataImportService.java # LLM 提取与导入
│   │   ├── OntologyValidationService.java # 6 层验证
│   │   └── CommunityService.java # 社区发现
│   ├── dal/
│   │   ├── dataobject/           # MyBatis-Plus 实体
│   │   │   └── ont/              # 本体实体（类/属性/约束）
│   │   └── mysql/                # Mapper 接口
│   ├── dal/neo4j/                # Neo4j 仓储层
│   │   ├── NodeRepository.java
│   │   ├── EdgeRepository.java
│   │   └── VectorIndexRepository.java
│   ├── vo/                       # 视图对象
│   │   ├── llm/                  # LLM 提取 VO
│   │   ├── search/               # 检索请求/响应 VO
│   │   ├── ontology/             # 本体 VO
│   │   └── imports/              # 数据导入 VO
│   └── resources/prompts/        # LLM Prompt 模板
│       ├── extract_entities.txt
│       ├── extract_relations.txt
│       ├── summarize_node.txt
│       └── summarize_community.txt
│
├── graphiti-module-system/       # 系统管理模块
│   └── 用户/角色/菜单/认证控制器与服务
│
├── graphiti-framework/           # 框架基础设施
│   ├── graphiti-common/          # 公共工具与异常
│   ├── graphiti-spring-boot-starter-security/  # JWT 安全
│   ├── graphiti-spring-boot-starter-mybatis/   # MyBatis 配置
│   └── graphiti-spring-boot-starter-redis/     # Redis 配置
│
├── graphiti-web/                 # 前端 (Vue 3)
│   ├── src/api/                  # API 客户端模块
│   ├── src/views/                # 页面组件
│   └── src/components/           # 可复用组件
│
└── sql/                          # 数据库初始化脚本
    ├── mysql/
    ├── postgresql/
    └── neo4j/
```

---

## 技术栈

### 后端

| 层级 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 21 |
| 框架 | Spring Boot | 3.5.5 |
| AI 框架 | Spring AI | 1.1.2 |
| 数据访问 | MyBatis-Plus | 3.5.12 |
| 图数据库 | Neo4j Java Driver | 5.26.0 |
| 安全 | Spring Security + JWT | - |
| 缓存 | Redisson | 3.37.0 |
| 接口文档 | SpringDoc OpenAPI | 2.8.5 |
| 工具库 | Hutool | 5.8.37 |

### 前端

| 技术 | 版本 |
|------|------|
| Vue | 3.4 |
| Vite | 5.2 |
| Vue Router | 4.3 |
| Pinia | 2.1 |
| Ant Design Vue | 4.2 |
| Axios | 1.7 |
| ECharts | 5.5 |

### 数据库

| 数据库 | 用途 | 版本 |
|--------|------|------|
| Neo4j | 知识图谱存储 | 5.26 |
| PostgreSQL | 元数据与系统数据 | 15+ |
| MySQL | 元数据存储（可选） | 8.0+ |
| Redis | 会话与缓存 | 6+ |

### 支持的 LLM 厂商

| 厂商 | Spring AI Starter | 自定义 Base URL |
|------|-------------------|-----------------|
| OpenAI | `spring-ai-starter-model-openai` | ✅ |
| Anthropic Claude | `spring-ai-starter-model-anthropic` | ✅ |
| 阿里云通义千问 | `spring-ai-starter-model-openai`（兼容） | ✅ |
| Ollama | `spring-ai-starter-model-ollama` | ✅ |
| Mistral AI | `spring-ai-starter-model-mistral-ai` | ✅ |
| Azure OpenAI | `spring-ai-starter-model-azure-openai` | ✅ |
| AWS Bedrock | `spring-ai-starter-model-bedrock` | ✅ |

---

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.9+
- Neo4j 5.26+
- PostgreSQL 15+（或 MySQL 8.0+）
- Redis 6+
- Node.js 18+（前端）

### 1. 克隆与构建

```bash
git clone <repository-url>
cd graphiti-java
mvn clean install -DskipTests
```

### 2. 数据库初始化

**Neo4j**（创建向量索引）：
```bash
# 执行 Neo4j 初始化脚本
neo4j-shell -f sql/neo4j/init.cypher
```

**PostgreSQL**：
```bash
psql -U postgres -d graphiti -f sql/postgresql/schema.sql
psql -U postgres -d graphiti -f sql/postgresql/init-data.sql
```

**MySQL**（可选）：
```bash
mysql -u root -p graphiti < sql/mysql/schema.sql
mysql -u root -p graphiti < sql/mysql/init-data.sql
```

### 3. 配置应用

编辑 `graphiti-server/src/main/resources/application-dev.yml`：

```yaml
spring:
  ai:
    openai:
      api-key: your-api-key
      base-url: http://your-llm-deployment:8000/v1  # 私有化部署地址
  datasource:
    dynamic:
      datasource:
        master:
          url: jdbc:postgresql://localhost:5432/graphiti
          username: postgres
          password: your-password
  data:
    redis:
      host: localhost
      port: 6379

graphiti:
  ai:
    llm-provider: openai        # openai | anthropic | qwen | ollama | mistral
    embedding-provider: openai

neo4j:
  uri: bolt://localhost:7687
  username: neo4j
  password: your-neo4j-password
```

### 4. 启动后端

```bash
cd graphiti-server
mvn spring-boot:run
```

后端启动于 `http://localhost:8080`。
Swagger UI: `http://localhost:8080/swagger-ui.html`

### 5. 启动前端

```bash
cd graphiti-web
pnpm install
pnpm dev
```

前端启动于 `http://localhost:5173`。

---

## 使用示例

### 创建图谱

```bash
curl -X POST http://localhost:8080/api/v1/graph \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"name": "科技公司", "description": "科技行业知识图谱"}'
```

### 添加数据（自动提取）

```bash
curl -X POST http://localhost:8080/api/v1/graph/data/add \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "graphId": "your-graph-id",
    "name": "苹果新闻",
    "content": "Apple Inc. 由 Steve Jobs 和 Steve Wozniak 于 1976 年在加利福尼亚州 Cupertino 创立。",
    "sourceType": "article"
  }'
```

系统将自动：
1. 创建 Episode 节点
2. 提取实体："Apple Inc."（组织）、"Steve Jobs"（人物）、"Steve Wozniak"（人物）、"Cupertino"（地点）
3. 提取关系：FOUNDED_BY、LOCATED_IN
4. 创建带嵌入向量的实体节点
5. 创建带事实描述的 RELATES_TO 边

### 混合检索

```bash
curl -X POST http://localhost:8080/api/v1/search/hybrid \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "graphId": "your-graph-id",
    "query": "谁创立了 Apple？",
    "config": {
      "limit": 10,
      "useBM25": true,
      "useVector": true,
      "useBFS": false,
      "reranker": "rrf"
    }
  }'
```

### 定义本体

```bash
curl -X POST http://localhost:8080/api/v1/graph/your-graph-id/ontology \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
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
          {"name": "industry", "type": "string", "required": true},
          {"name": "foundedYear", "type": "integer", "required": false}
        ]
      }
    ],
    "edges": [
      {"name": "FOUNDED_BY", "source": "Organization", "target": "Person"},
      {"name": "WORKS_AT", "source": "Person", "target": "Organization"}
    ]
  }'
```

### 构建社区

```bash
curl -X POST http://localhost:8080/api/v1/graph/your-graph-id/communities/build \
  -H "Authorization: Bearer <token>"
```

---

## API 文档

### Swagger / OpenAPI

应用启动后，访问交互式 API 文档：

- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`

### API 分组

| 分组 | 基础路径 | 说明 |
|------|---------|------|
| 认证 | `/api/v1/auth/**` | 登录、注册、刷新令牌 |
| 用户 | `/api/v1/users/**` | 用户管理 |
| 角色 | `/api/v1/roles/**` | 角色管理 |
| 菜单 | `/api/v1/menus/**` | 菜单管理 |
| 图谱 | `/api/v1/graph/**` | 图谱 CRUD、克隆、导出 |
| 节点 | `/api/v1/nodes/**` | 节点 CRUD |
| 边 | `/api/v1/edges/**` | 边/关系 CRUD |
| 事件 | `/api/v1/episodes/**` | 事件管理 |
| 本体 | `/api/v1/graph/{graphId}/ontology/**` | 本体定义与验证 |
| 检索 | `/api/v1/search/**` | 混合、语义、BFS 检索 |
| 数据导入 | `/api/v1/graph/data/**` | LLM 提取与导入 |
| 数据维护 | `/api/v1/maintenance/**` | 数据质量操作 |

---

## 配置说明

### LLM 厂商选择

在 `application-dev.yml` 中设置：

```yaml
graphiti:
  ai:
    llm-provider: openai      # 在此切换厂商
    embedding-provider: openai
```

可选厂商：`openai`、`anthropic`、`qwen`、`ollama`、`mistral`

### 私有化部署示例

**OpenAI 兼容（vLLM / LM Studio）：**
```yaml
spring:
  ai:
    openai:
      api-key: any-key
      base-url: http://localhost:8000/v1
```

**Anthropic 兼容（LiteLLM 代理）：**
```yaml
spring:
  ai:
    anthropic:
      api-key: any-key
      base-url: http://localhost:8080/v1
```

**Ollama：**
```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434
```

### 向量索引配置

向量索引在启动时通过 `VectorIndexRepository` 自动创建：

```java
// 节点向量索引
CREATE VECTOR INDEX node_embedding_index IF NOT EXISTS
FOR (n:Entity) ON (n.embedding)
OPTIONS {indexConfig: {`vector.dimensions`: 1536, `vector.similarity_function`: 'cosine'}}

// 边向量索引
CREATE VECTOR INDEX edge_embedding_index IF NOT EXISTS
FOR ()-[r:RELATES_TO]-() ON (r.embedding)
OPTIONS {indexConfig: {`vector.dimensions`: 1536, `vector.similarity_function`: 'cosine'}}
```

---

## 文档索引

### 快速入门

- **[使用手册（快速开始）](.qoder/repowiki/zh/content/快速开始.md)** - 完整的安装和配置指南

### 设计与架构

- [项目概述 (docs/01-项目概述.md)](docs/01-项目概述.md)
- [Java vs Python 对比 (docs/graphiti-java-vs-python-comparison.md)](docs/graphiti-java-vs-python-comparison.md)
- [实现总结 (docs/implementation-summary.md)](docs/implementation-summary.md)
- [设计文档 (DESIGN.md)](DESIGN.md)

### AI 与记忆系统

- [AI 会话记忆系统 (docs/ai-chat-memory.md)](docs/ai-chat-memory.md)

### 本体与数据管道

- [本体系统 (docs/ontology.md)](docs/ontology.md)
- [数据管道 (docs/pipeline.md)](docs/pipeline.md)
- [法律本体迁移指南 (docs/legal-ontology-migration-guide.md)](docs/legal-ontology-migration-guide.md)

### 数据库

- [数据库迁移指南 (docs/database-migration-guide.md)](docs/database-migration-guide.md)

### 规划与设计规格

- [后端实现规划 (docs/superpowers/plans/2026-05-08-graphiti-backend-implementation.md)](docs/superpowers/plans/2026-05-08-graphiti-backend-implementation.md)
- [控制台实现规划 (docs/superpowers/plans/2026-05-08-graphiti-console-implementation.md)](docs/superpowers/plans/2026-05-08-graphiti-console-implementation.md)
- [MySQL 到 PostgreSQL 迁移 (docs/superpowers/plans/2026-05-08-mysql-to-postgresql-migration.md)](docs/superpowers/plans/2026-05-08-mysql-to-postgresql-migration.md)
- [完整对齐规划 (docs/superpowers/plans/2026-05-10-graphiti-java-full-alignment-plan.md)](docs/superpowers/plans/2026-05-10-graphiti-java-full-alignment-plan.md)
- [完整迁移规划 (docs/superpowers/plans/2026-05-11-graphiti-java-full-migration-plan.md)](docs/superpowers/plans/2026-05-11-graphiti-java-full-migration-plan.md)
- [本体阶段1 (docs/superpowers/plans/2026-05-10-ontology-phase1-schema-enforcement.md)](docs/superpowers/plans/2026-05-10-ontology-phase1-schema-enforcement.md)
- [本体阶段2-4 (docs/superpowers/plans/2026-05-10-ontology-phase2-4-remaining.md)](docs/superpowers/plans/2026-05-10-ontology-phase2-4-remaining.md)
- [法律本体设计 (docs/superpowers/plans/2026-05-11-legal-ontology-design.md)](docs/superpowers/plans/2026-05-11-legal-ontology-design.md)
- [Neo4j 关系类型一致性 (docs/superpowers/plans/2026-05-11-neo4j-relation-type-consistency.md)](docs/superpowers/plans/2026-05-11-neo4j-relation-type-consistency.md)
- [控制台设计规格 (docs/superpowers/specs/2026-05-08-graphiti-console-design.md)](docs/superpowers/specs/2026-05-08-graphiti-console-design.md)
- [完整对齐设计规格 (docs/superpowers/specs/2026-05-10-graphiti-java-full-alignment-design.md)](docs/superpowers/specs/2026-05-10-graphiti-java-full-alignment-design.md)
- [本体增强设计规格 (docs/superpowers/specs/2026-05-10-ontology-enhancement-design.md)](docs/superpowers/specs/2026-05-10-ontology-enhancement-design.md)
- [后端 API 实现设计规格 (docs/superpowers/specs/2026-05-11-backend-api-impl-design.md)](docs/superpowers/specs/2026-05-11-backend-api-impl-design.md)
- [完整迁移设计规格 (docs/superpowers/specs/2026-05-11-graphiti-java-full-migration-design.md)](docs/superpowers/specs/2026-05-11-graphiti-java-full-migration-design.md)
- [AI 会话记忆设计规格 (docs/superpowers/specs/2026-05-13-ai-chat-memory-design.md)](docs/superpowers/specs/2026-05-13-ai-chat-memory-design.md)

---

## 参与贡献

欢迎为 Graphiti-Java 贡献力量！请遵循以下指南：

### 开发环境搭建

1. Fork 仓库
2. 创建功能分支：`git checkout -b feature/your-feature`
3. 确保代码编译：`mvn clean compile`
4. 运行测试：`mvn test`
5. 提交 Pull Request

### 代码规范

- 遵循 Java 21 编码规范
- 使用 Lombok 减少样板代码
- 为公共 API 添加 Javadoc
- 为控制器添加 Swagger 注解

### 提交信息格式

```
feat: 新功能
fix: 修复 bug
docs: 更新文档
refactor: 代码重构
test: 添加测试
chore: 构建/配置变更
```

### 架构原则

- **服务层**：业务逻辑放在 `*Service` 接口中，实现放在 `impl/`
- **Neo4j 操作**：集中在 `GraphNeo4jService` 中管理
- **LLM 集成**：厂商特定实现放在 `impl/ai/`，通过 `LlmClientService` 统一
- **本体验证**：6 层验证逻辑在 `OntologyValidationServiceImpl` 中实现

---

## 许可证

[MIT 许可证](LICENSE)

## 致谢

Graphiti-Java 灵感来源于 Zep AI 的原始 [Graphiti](https://github.com/getzep/graphiti) Python 库。

---

<p align="center">
  <sub>为知识图谱社区打造 ❤️</sub>
</p>
