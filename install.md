# OntoGraph 部署流水线设计文档

本文档定义了 ontograph-java 项目的容器化部署方案，涵盖开发、测试、生产全场景。

---

## 1. 整体架构

```
宿主机
├── ontograph-java/              # 应用代码 (git clone)
│   ├── docker/                 # Docker 相关文件
│   │   └── Dockerfile          # 多阶段构建
│   ├── docker-compose.yml      # 开发/测试环境
│   ├── docker-compose.prod.yml  # 生产环境
│   ├── config/                 # 生产配置
│   │   ├── application-prod.yml
│   │   └── logback-prod.xml
│   ├── scripts/               # 运维脚本
│   │   ├── build.sh
│   │   ├── backup.sh
│   │   └── restore.sh
│   ├── .env                    # 环境变量 (gitignore)
│   └── .env.example            # 环境变量模板
├── data/                       # 数据持久化目录
│   ├── postgres/               # PostgreSQL 数据
│   └── redis/                  # Redis 数据
└── (外部) Neo4j               # 独立部署
    (外部) LLM Provider         # 云端 / 本地 Ollama
```

---

## 2. 决策说明

### 2.1 服务规模

- **开发/测试** 与 **生产** 共用一套 `docker-compose`，通过 `profiles` 区分
- 生产模式通过 `.env` 中 `SPRING_PROFILES_ACTIVE=prod` 激活

### 2.2 LLM Provider

- **支持云端切换**：OpenAI / Azure OpenAI / Claude / Gemini 等
- **支持本地切换**：Ollama / LM Studio / vLLM / Xinference 等
- 通过 `spring.ai.*` 配置 + `graphiti.ai.*` 配置共同决定
- 环境变量 `LLM_MODE=cloud|local` 配合 `.env.cloud-llm` / `.env.local-llm` 快速切换

### 2.3 数据存储

| 服务 | 开发/测试 | 生产 |
|------|---------|------|
| ontograph-java | Docker 容器 | Docker 容器，CPU/内存限制 |
| PostgreSQL | Docker 容器，端口 5432 | Docker 容器，持久化到 `data/postgres/` |
| Redis | Docker 容器，端口 6379 | Docker 容器，AOF 持久化 |
| Neo4j | 外部 localhost | 外部配置（bolt://neo4j:7687 或云端） |

---

## 3. 构建流程

### 3.1 前端嵌入后端

```
ontograph-web/          (pnpm build)
    dist/
      index.html
      assets/
        └── static/   ← frontend-maven-plugin 拷贝到
                          ontograph-server/src/main/resources/static/

ontograph-server/       (mvn package)
    target/
      ontograph-server-1.0.0-SNAPSHOT.jar   ← 包含前端资源
```

### 3.2 镜像构建流程

```
1. 前端构建 (Node.js)
   pnpm install && pnpm build
   → dist/

2. 静态资源拷贝 (frontend-maven-plugin)
   dist/* → src/main/resources/static/

3. 后端编译 (Maven + JDK 21)
   mvn clean package -DskipTests

4. Docker 镜像打包 (multi-stage)
   Build stage: Maven 3.9 + JDK 21
   Runtime stage: JRE 21 slim + JAR
```

---

## 4. 环境配置机制

### 4.1 Profile 激活

```bash
# 开发环境 (默认)
SPRING_PROFILES_ACTIVE=dev

# 生产环境
SPRING_PROFILES_ACTIVE=prod
```

### 4.2 LLM Provider 切换

```bash
# 切换到云端 LLM
cp .env.cloud-llm .env
source .env

# 切换到本地 LLM
cp .env.local-llm .env
source .env
```

预置配置模板支持：
- OpenAI (GPT-4o / GPT-4.1)
- Azure OpenAI
- Ollama (本地)
- 通义千问 (Qwen)
- DeepSeek
- Groq
- Gemini

---

## 5. 服务编排

### 5.1 开发/测试环境 (默认 profiles: default)

- ontograph-java (8080)
- postgres (5432)
- redis (6379)
- Neo4j 需手动在 localhost 启动

### 5.2 生产环境 (profiles: prod)

- ontograph-java with resource limits + health checks
- postgres with persistent volumes
- redis with AOF persistence

---

## 6. 数据备份

```bash
# 备份 PostgreSQL + Redis
./scripts/backup.sh

# 恢复
./scripts/restore.sh /path/to/backup.tar.gz
```

备份内容：
- `data/postgres/` 目录
- `data/redis/` 目录
- 备份时间戳文件名

---

## 7. 快速开始

### 开发环境

```bash
# 1. 复制环境变量模板
cp .env.example .env
# 编辑 .env，填入实际值

# 2. 启动服务 (开发/测试)
docker-compose up -d

# 3. 查看日志
docker-compose logs -f ontograph-java

# 4. 访问
# 前端: http://localhost:8080
# API:  http://localhost:8080/api/v1
# Swagger: http://localhost:8080/swagger-ui.html
```

### 生产环境

```bash
# 1. 构建镜像
./scripts/build.sh

# 2. 启动生产服务
docker-compose --profile prod up -d

# 3. 监控
docker-compose -f docker-compose.yml -f docker-compose.prod.yml ps

# 4. 备份
./scripts/backup.sh
```

---

## 8. 前端 API 路径

- **开发时**：Vite proxy 转发 `/api` → `http://localhost:8080`
- **生产时**：SPA 所有路由通过 Spring Boot fallback 兜住（`/**` → `index.html`）

Vite 配置 (`vite.config.ts`)：
```typescript
server: {
  port: 3000,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
      changeOrigin: true
    }
  }
}
```

---

## 9. 版本信息

- Java: 21
- Spring Boot: 3.5.5
- Spring AI: 1.1.2
- Node.js: 20.x
- pnpm: 9.x
- PostgreSQL: 16
- Redis: 7
