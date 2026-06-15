# OntoGraph 品牌重构实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将项目从 ontograph-java 品牌全面迁移至 OntoGraph 品牌，包括文档、配置、代码包名、前端界面和视觉资源。

**Architecture:** 采用分层分批渐进式重构策略，按文档层→后端配置→后端代码→前端配置→前端代码→视觉资源→全链路验证的顺序执行，每阶段独立验证。

**Tech Stack:** Java 21, Spring Boot 3.5.5, Maven, Vue 3, TypeScript, Vite, Ant Design Vue

---

## 文件结构映射

### 将修改的文件清单

**文档层：**
- `README.md` - 项目主文档（英文）
- `README_CN.md` - 项目主文档（中文）
- `docs/product/logo.md` - Logo 方案文档

**后端配置层：**
- `pom.xml` - 根 Maven 配置
- `ontograph-framework/pom.xml` - 框架模块配置
- `ontograph-module-core/pom.xml` - 核心模块配置
- `ontograph-module-system/pom.xml` - 系统模块配置
- `ontograph-server/pom.xml` - 服务器模块配置
- `ontograph-server/src/main/resources/application.yml` - 应用配置
- `.env.example` - 环境变量模板
- `docker-compose.yml` - Docker 编排配置

**后端代码层（包名重构）：**
- 所有 `src/main/java/com/graphiti/**/*.java` - package 声明
- 所有 `src/test/java/com/graphiti/**/*.java` - package 声明
- 目录结构：`com/graphiti/` → `com/ontograph/`

**前端配置层：**
- `ontograph-web/package.json` - 前端项目配置

**前端代码层：**
- `ontograph-web/src/i18n/locales/zh-CN.ts` - 中文国际化
- `ontograph-web/src/i18n/locales/en-US.ts` - 英文国际化
- `ontograph-web/src/i18n/locales/zh-TW.ts` - 繁体中文国际化
- `ontograph-web/src/i18n/locales/ja-JP.ts` - 日文国际化
- `ontograph-web/src/i18n/index.ts` - i18n 初始化
- `ontograph-web/src/views/graph/ide.vue` - 主界面 Logo
- `ontograph-web/src/views/login/index.vue` - 登录页版权
- `ontograph-web/src/router/index.ts` - 路由标题
- `ontograph-web/src/utils/auth.ts` - 认证存储 key
- `ontograph-web/src/components/Ontology/ClassEditor.vue` - URI 示例
- `ontograph-web/src/components/Ontology/PropertyEditor.vue` - URI 示例
- `ontograph-web/src/types/graphiti.ts` - 类型文件（需重命名）
- `ontograph-web/src/api/search.ts` - API 路径

---

## 阶段 1：文档层重构

### Task 1: 更新 README.md（英文文档）

**Files:**
- Modify: `README.md`

- [ ] **Step 1: 替换标题和描述**

将第 1-6 行替换为：

```markdown
# OntoGraph

<p align="center">
  <strong>Where ontology becomes living structure.</strong><br>
  <em>A production-ready knowledge graph system for ontology modeling and semantic relationships, powered by Java, Neo4j, and LLMs.</em>
</p>
```

- [ ] **Step 2: 替换项目概述**

将第 28 行替换为：

```markdown
OntoGraph is a production-ready knowledge graph backend system that brings the power of temporal knowledge graphs to the Java ecosystem. It automatically extracts entities and relationships from unstructured text using Large Language Models (LLM), stores them in Neo4j with vector embeddings, and provides advanced hybrid search capabilities combining full-text, semantic, and graph traversal.
```

- [ ] **Step 3: 替换架构图中的模块名**

将第 80-115 行的架构图中的 `graphiti-*` 替换为 `ontograph-*`：

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              ontograph-web                                   │
│                    Vue 3 + Vite + Ant Design Vue                             │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                              ontograph-server                                │
│                        Spring Boot 3.5.5 (Entry)                             │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
            ┌─────────────────────────┼─────────────────────────┐
            ▼                         ▼                         ▼
┌─────────────────────┐   ┌─────────────────────┐   ┌─────────────────────┐
│ ontograph-module-core│  │ontograph-module-sys │  │ ontograph-framework  │
│  Knowledge Graph    │   │  User/Role/Menu/Auth│   │  Common/Security/   │
│  - Graph CRUD       │   │  - JWT Auth         │   │  - MyBatis Starter  │
│  - Search/Import    │   │  - RBAC             │   │  - Redis Starter    │
│  - Ontology/Community│  │  - Menu Management  │   │                     │
└─────────────────────┘   └─────────────────────┘   └─────────────────────┘
```

- [ ] **Step 4: 替换模块结构说明**

将第 119-173 行的所有 `graphiti-*` 路径替换为 `ontograph-*`。

- [ ] **Step 5: 替换配置示例中的前缀**

将第 289-292 行的配置示例：

```yaml
ontograph:
  ai:
    llm-provider: openai        # openai | anthropic | qwen | ollama | mistral
    embedding-provider: openai
```

- [ ] **Step 6: 替换 Docker 相关命令和路径**

将所有 `ontograph-java` 替换为 `ontograph-java`，例如：
- 第 326 行：`docker-compose up -d`
- 第 341 行：`docker-compose logs -f ontograph-java`
- 第 521 行：`docker build -t ontograph-java:latest -f docker/Dockerfile .`

- [ ] **Step 7: 更新致谢部分**

将第 803-805 行替换为：

```markdown
## Acknowledgements

OntoGraph (原 ontograph-java) 灵感来源于 Zep AI 的原始 [Graphiti](https://github.com/getzep/graphiti) Python 库。
```

- [ ] **Step 8: 提交更改**

```bash
git add README.md
git commit -m "docs: update README.md with OntoGraph branding"
```

---

### Task 2: 更新 README_CN.md（中文文档）

**Files:**
- Modify: `README_CN.md`

- [ ] **Step 1: 替换标题和描述**

将第 1-6 行替换为：

```markdown
# OntoGraph

<p align="center">
  <strong>本体成为可运行的结构</strong><br>
  <em>生产级知识图谱系统，支持本体建模、语义关系管理、LLM 自动实体关系提取、混合检索和时序事实管理。</em>
</p>
```

- [ ] **Step 2: 替换项目概述**

将第 28 行替换为：

```markdown
OntoGraph 是一个生产级的知识图谱后端系统，将时序知识图谱能力引入 Java 生态。系统利用大型语言模型（LLM）从非结构化文本中自动提取实体和关系，将其以向量嵌入的形式存储在 Neo4j 中，并提供结合全文检索、语义搜索和图遍历的混合检索能力。
```

- [ ] **Step 3-6: 执行与 Task 1 相同的架构图、模块结构、配置示例、Docker 命令替换**

（参考 Task 1 的 Step 3-6，所有 `graphiti-*` → `ontograph-*`）

- [ ] **Step 7: 更新致谢部分**

将第 803-805 行替换为：

```markdown
## 致谢

OntoGraph (原 ontograph-java) 灵感来源于 Zep AI 的原始 [Graphiti](https://github.com/getzep/graphiti) Python 库。
```

- [ ] **Step 8: 提交更改**

```bash
git add README_CN.md
git commit -m "docs: update README_CN.md with OntoGraph branding"
```

---

### Task 3: 整理 docs/product/logo.md

**Files:**
- Modify: `docs/product/logo.md`

- [ ] **Step 1: 将文档转换为 OntoGraph 品牌说明**

将文件内容替换为：

```markdown
# OntoGraph 品牌标识规范

## 产品信息

- **产品名：** OntoGraph
- **Slogan：** Where ontology becomes living structure.
- **中文感受：** 本体成为可运行的结构。
- **定位：** 知识图谱、本体建模、语义关系系统

## Logo 资源

- **Logo 文件：** `docs/product/images/ontoGraph.png`
- **使用场景：** 前端界面、文档、部署配置

## 品牌使用规范

1. 产品名称统一使用 "OntoGraph"（注意大小写）
2. Slogan 仅在正式文档和首页使用
3. Logo 不得修改颜色或比例
4. 保留对原 Graphiti 项目的致谢
```

- [ ] **Step 2: 提交更改**

```bash
git add docs/product/logo.md
git commit -m "docs: update logo.md with OntoGraph brand guidelines"
```

---

## 阶段 2：后端配置层重构

### Task 4: 更新根 pom.xml

**Files:**
- Modify: `pom.xml`

- [ ] **Step 1: 替换 groupId、artifactId、name、description**

将第 8-13 行替换为：

```xml
<groupId>com.ontograph</groupId>
<artifactId>ontograph-java</artifactId>
<version>1.0.0-SNAPSHOT</version>
<packaging>pom</packaging>
<name>OntoGraph</name>
<description>OntoGraph知识图谱后端服务</description>
```

- [ ] **Step 2: 替换所有 dependency 中的 groupId**

将第 164-180 行的所有 `<groupId>com.graphiti</groupId>` 替换为 `<groupId>com.ontograph</groupId>`。

- [ ] **Step 3: 提交更改**

```bash
git add pom.xml
git commit -m "build: update root pom.xml with com.ontograph groupId"
```

---

### Task 5: 更新子模块 pom.xml

**Files:**
- Modify: `ontograph-framework/pom.xml`
- Modify: `ontograph-module-core/pom.xml`
- Modify: `ontograph-module-system/pom.xml`
- Modify: `ontograph-server/pom.xml`

- [ ] **Step 1: 更新 ontograph-framework/pom.xml**

将文件中的：
- `<groupId>com.graphiti</groupId>` → `<groupId>com.ontograph</groupId>`
- `<artifactId>ontograph-framework</artifactId>` → `<artifactId>ontograph-framework</artifactId>`

- [ ] **Step 2: 更新 ontograph-module-core/pom.xml**

将文件中的：
- `<groupId>com.graphiti</groupId>` → `<groupId>com.ontograph</groupId>`
- `<artifactId>ontograph-module-core</artifactId>` → `<artifactId>ontograph-module-core</artifactId>`
- `<artifactId>ontograph-framework</artifactId>` → `<artifactId>ontograph-framework</artifactId>`（依赖引用）

- [ ] **Step 3: 更新 ontograph-module-system/pom.xml**

将文件中的：
- `<groupId>com.graphiti</groupId>` → `<groupId>com.ontograph</groupId>`
- `<artifactId>ontograph-module-system</artifactId>` → `<artifactId>ontograph-module-system</artifactId>`
- `<artifactId>ontograph-framework</artifactId>` → `<artifactId>ontograph-framework</artifactId>`（依赖引用）

- [ ] **Step 4: 更新 ontograph-server/pom.xml**

将文件中的：
- `<groupId>com.graphiti</groupId>` → `<groupId>com.ontograph</groupId>`
- `<artifactId>ontograph-server</artifactId>` → `<artifactId>ontograph-server</artifactId>`
- 所有依赖模块的 artifactId 从 `graphiti-*` → `ontograph-*`

- [ ] **Step 5: 提交更改**

```bash
git add ontograph-framework/pom.xml ontograph-module-core/pom.xml ontograph-module-system/pom.xml ontograph-server/pom.xml
git commit -m "build: update all module pom.xml files with com.ontograph groupId"
```

---

### Task 6: 更新 application.yml

**Files:**
- Modify: `ontograph-server/src/main/resources/application.yml`

- [ ] **Step 1: 替换应用名称和配置前缀**

将第 1-34 行替换为：

```yaml
# OntoGraph 知识图谱后端服务配置文件
# 应用配置
spring:
  application:
    name: ontograph-java
  profiles:
    active: dev



# MyBatis-Plus 配置
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    db-config:
      id-type: auto

# Spring AI 配置（根据 Provider 选择配置）
# OpenAI: spring.ai.openai.api-key + spring.ai.openai.base-url
# Qwen:   spring.ai.openai.api-key + spring.ai.openai.base-url (指向 dashscope)
# Ollama: spring.ai.ollama.base-url

# JWT 配置
ontograph:
  security:
    jwt:
      secret: mySecretKeyForJWTTokenGenerationWhichShouldBeAtLeast512BitsLong
      expiration: 86400  # 24 hours
  ai:
    llm-provider: openai
    embedding-provider: openai

# 日志配置
logging:
  level:
    com.ontograph: debug
    org.springframework.security: debug
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

- [ ] **Step 2: 提交更改**

```bash
git add ontograph-server/src/main/resources/application.yml
git commit -m "config: update application.yml with ontograph prefix"
```

---

### Task 7: 更新 .env.example

**Files:**
- Modify: `.env.example`

- [ ] **Step 1: 替换环境变量前缀**

将第 1-90 行中的所有：
- `GRAPHTI_AI_LLM_PROVIDER` → `ONTOGRAPH_AI_LLM_PROVIDER`
- `GRAPHTI_AI_EMBEDDING_PROVIDER` → `ONTOGRAPH_AI_EMBEDDING_PROVIDER`
- `GRAPHTI_AI_RERANK_PROVIDER` → `ONTOGRAPH_AI_RERANK_PROVIDER`
- `GRAPHTI_AI_OLLAMA_CHAT_MODEL` → `ONTOGRAPH_AI_OLLAMA_CHAT_MODEL`
- `GRAPHTI_AI_OLLAMA_EMBEDDING_MODEL` → `ONTOGRAPH_AI_OLLAMA_EMBEDDING_MODEL`
- `GRAPHTI_AI_QWEN_API_KEY` → `ONTOGRAPH_AI_QWEN_API_KEY`
- `GRAPHTI_AI_QWEN_BASE_URL` → `ONTOGRAPH_AI_QWEN_BASE_URL`

将第 2 行的注释：
```bash
# OntoGraph 环境变量配置模板
```

- [ ] **Step 2: 提交更改**

```bash
git add .env.example
git commit -m "config: update .env.example with ONTOGRAPH_AI_ prefix"
```

---

### Task 8: 更新 docker-compose.yml

**Files:**
- Modify: `docker-compose.yml`

- [ ] **Step 1: 替换服务名**

将所有 `ontograph-java` 替换为 `ontograph-java`，包括：
- 服务定义名称
- 环境变量引用
- 日志命令示例

- [ ] **Step 2: 提交更改**

```bash
git add docker-compose.yml
git commit -m "config: update docker-compose.yml service name to ontograph-java"
```

---

### Task 9: 验证后端配置

**Files:**
- 无

- [ ] **Step 1: 清理并编译**

```bash
mvn clean compile
```

**Expected:** BUILD SUCCESS，无编译错误

- [ ] **Step 2: 检查依赖关系**

```bash
mvn dependency:tree | head -50
```

**Expected:** 所有模块 groupId 为 `com.ontograph`

- [ ] **Step 3: 提交验证结果**

```bash
git add .
git commit -m "chore: verify backend configuration compiles successfully"
```

---

## 阶段 3：后端代码层重构（Java 包名）

### Task 10: 重命名 Java 包目录

**Files:**
- Rename directory: `ontograph-framework/src/main/java/com/graphiti` → `ontograph-framework/src/main/java/com/ontograph`
- Rename directory: `ontograph-module-core/src/main/java/com/graphiti` → `ontograph-module-core/src/main/java/com/ontograph`
- Rename directory: `ontograph-module-system/src/main/java/com/graphiti` → `ontograph-module-system/src/main/java/com/ontograph`
- Rename directory: `ontograph-server/src/main/java/com/graphiti` → `ontograph-server/src/main/java/com/ontograph`
- Rename directory: 所有测试目录中的 `com/graphiti` → `com/ontograph`

- [ ] **Step 1: 重命名 ontograph-framework 包目录**

```bash
# Linux/Mac
mkdir -p ontograph-framework/src/main/java/com/ontograph
mv ontograph-framework/src/main/java/com/graphiti/* ontograph-framework/src/main/java/com/ontograph/
rm -rf ontograph-framework/src/main/java/com/graphiti

# Windows PowerShell
New-Item -ItemType Directory -Force -Path "ontograph-framework/src/main/java/com/ontograph"
Move-Item -Path "ontograph-framework/src/main/java/com/graphiti/*" -Destination "ontograph-framework/src/main/java/com/ontograph/"
Remove-Item -Recurse -Force "ontograph-framework/src/main/java/com/graphiti"
```

- [ ] **Step 2: 重命名 ontograph-module-core 包目录**

```bash
# Windows PowerShell
New-Item -ItemType Directory -Force -Path "ontograph-module-core/src/main/java/com/ontograph"
Move-Item -Path "ontograph-module-core/src/main/java/com/graphiti/*" -Destination "ontograph-module-core/src/main/java/com/ontograph/"
Remove-Item -Recurse -Force "ontograph-module-core/src/main/java/com/graphiti"

# 测试目录
if (Test-Path "ontograph-module-core/src/test/java/com/graphiti") {
    New-Item -ItemType Directory -Force -Path "ontograph-module-core/src/test/java/com/ontograph"
    Move-Item -Path "ontograph-module-core/src/test/java/com/graphiti/*" -Destination "ontograph-module-core/src/test/java/com/ontograph/"
    Remove-Item -Recurse -Force "ontograph-module-core/src/test/java/com/graphiti"
}
```

- [ ] **Step 3: 重命名 ontograph-module-system 包目录**

```bash
# Windows PowerShell
New-Item -ItemType Directory -Force -Path "ontograph-module-system/src/main/java/com/ontograph"
Move-Item -Path "ontograph-module-system/src/main/java/com/graphiti/*" -Destination "ontograph-module-system/src/main/java/com/ontograph/"
Remove-Item -Recurse -Force "ontograph-module-system/src/main/java/com/graphiti"
```

- [ ] **Step 4: 重命名 ontograph-server 包目录**

```bash
# Windows PowerShell
New-Item -ItemType Directory -Force -Path "ontograph-server/src/main/java/com/ontograph"
Move-Item -Path "ontograph-server/src/main/java/com/graphiti/*" -Destination "ontograph-server/src/main/java/com/ontograph/"
Remove-Item -Recurse -Force "ontograph-server/src/main/java/com/graphiti"

# 测试目录
if (Test-Path "ontograph-server/src/test/java/com/graphiti") {
    New-Item -ItemType Directory -Force -Path "ontograph-server/src/test/java/com/ontograph"
    Move-Item -Path "ontograph-server/src/test/java/com/graphiti/*" -Destination "ontograph-server/src/test/java/com/ontograph/"
    Remove-Item -Recurse -Force "ontograph-server/src/test/java/com/graphiti"
}
```

- [ ] **Step 5: 提交目录重命名**

```bash
git add -A
git commit -m "refactor: rename Java package directories from com.graphiti to com.ontograph"
```

---

### Task 11: 批量替换 package 声明

**Files:**
- 所有 `**/*.java` 文件

- [ ] **Step 1: 使用 grep 查找所有需要修改的文件**

```bash
grep -r "package com.graphiti" --include="*.java" .
```

**Expected:** 输出所有需要修改的 Java 文件路径

- [ ] **Step 2: 批量替换 package 声明（使用 PowerShell）**

```powershell
# 查找所有 Java 文件并替换 package 声明
Get-ChildItem -Recurse -Filter "*.java" | ForEach-Object {
    $content = Get-Content $_.FullName -Raw -Encoding UTF8
    if ($content -match "package com\.graphiti") {
        $content = $content -replace "package com\.graphiti", "package com.ontograph"
        Set-Content -Path $_.FullName -Value $content -Encoding UTF8 -NoNewline
        Write-Host "Updated package in: $($_.FullName)"
    }
}
```

- [ ] **Step 3: 验证替换结果**

```bash
grep -r "package com.graphiti" --include="*.java" .
```

**Expected:** 无输出（所有 package 声明已更新）

```bash
grep -r "package com.ontograph" --include="*.java" . | wc -l
```

**Expected:** 输出文件数量（应与 Step 1 的数量一致）

- [ ] **Step 4: 提交 package 声明更新**

```bash
git add -A
git commit -m "refactor: update all Java package declarations to com.ontograph"
```

---

### Task 12: 批量替换 import 语句

**Files:**
- 所有 `**/*.java` 文件

- [ ] **Step 1: 批量替换 import 语句**

```powershell
# 查找所有 Java 文件并替换 import 语句
Get-ChildItem -Recurse -Filter "*.java" | ForEach-Object {
    $content = Get-Content $_.FullName -Raw -Encoding UTF8
    if ($content -match "import com\.graphiti") {
        $content = $content -replace "import com\.graphiti", "import com.ontograph"
        Set-Content -Path $_.FullName -Value $content -Encoding UTF8 -NoNewline
        Write-Host "Updated import in: $($_.FullName)"
    }
}
```

- [ ] **Step 2: 验证替换结果**

```bash
grep -r "import com.graphiti" --include="*.java" .
```

**Expected:** 无输出

- [ ] **Step 3: 提交 import 语句更新**

```bash
git add -A
git commit -m "refactor: update all Java import statements to com.ontograph"
```

---

### Task 13: 更新代码注释中的品牌名

**Files:**
- 所有 `**/*.java` 文件

- [ ] **Step 1: 批量替换注释中的 Graphiti**

```powershell
# 替换注释中的 Graphiti → OntoGraph（保留大小写变体）
Get-ChildItem -Recurse -Filter "*.java" | ForEach-Object {
    $content = Get-Content $_.FullName -Raw -Encoding UTF8
    $original = $content
    # 替换注释中的 Graphiti（不区分大小写）
    $content = $content -replace "(?i)(//.*?|/\*.*?\*/)\bGraphiti\b", { param($match) 
        $match.Value -replace "Graphiti", "OntoGraph" 
    }
    if ($content -ne $original) {
        Set-Content -Path $_.FullName -Value $content -Encoding UTF8 -NoNewline
        Write-Host "Updated comments in: $($_.FullName)"
    }
}
```

- [ ] **Step 2: 手动检查关键文件**

重点检查以下文件的注释：
- `ontograph-module-core/src/main/java/com/ontograph/module/core/service/*.java`
- `ontograph-server/src/main/java/com/ontograph/server/GraphitiApplication.java` → 可能需重命名为 `OntoGraphApplication.java`

- [ ] **Step 3: 提交注释更新**

```bash
git add -A
git commit -m "docs: update Java code comments with OntoGraph branding"
```

---

### Task 14: 重命名主应用类

**Files:**
- Rename: `ontograph-server/src/main/java/com/ontograph/server/GraphitiApplication.java` → `OntoGraphApplication.java`

- [ ] **Step 1: 重命名文件并更新类名**

```powershell
# 重命名文件
Rename-Item -Path "ontograph-server/src/main/java/com/ontograph/server/GraphitiApplication.java" -NewName "OntoGraphApplication.java"

# 更新文件内容
$file = "ontograph-server/src/main/java/com/ontograph/server/OntoGraphApplication.java"
$content = Get-Content $file -Raw -Encoding UTF8
$content = $content -replace "class GraphitiApplication", "class OntoGraphApplication"
Set-Content -Path $file -Value $content -Encoding UTF8 -NoNewline
```

- [ ] **Step 2: 提交应用类重命名**

```bash
git add -A
git commit -m "refactor: rename main application class to OntoGraphApplication"
```

---

### Task 15: 验证后端代码编译

**Files:**
- 无

- [ ] **Step 1: 清理并编译**

```bash
mvn clean compile
```

**Expected:** BUILD SUCCESS

- [ ] **Step 2: 运行测试**

```bash
mvn test
```

**Expected:** All tests pass

- [ ] **Step 3: 提交验证结果**

```bash
git add .
git commit -m "chore: verify backend code compiles and tests pass after package rename"
```

---

## 阶段 4：前端配置层重构

### Task 16: 更新 package.json

**Files:**
- Modify: `ontograph-web/package.json`

- [ ] **Step 1: 替换项目名称**

将第 2 行替换为：

```json
"name": "ontograph-web",
```

- [ ] **Step 2: 提交更改**

```bash
cd ontograph-web
git add package.json
git commit -m "build: update package.json name to ontograph-web"
cd ..
```

---

## 阶段 5：前端代码层重构

### Task 17: 更新国际化文件

**Files:**
- Modify: `ontograph-web/src/i18n/locales/zh-CN.ts`
- Modify: `ontograph-web/src/i18n/locales/en-US.ts`
- Modify: `ontograph-web/src/i18n/locales/zh-TW.ts`
- Modify: `ontograph-web/src/i18n/locales/ja-JP.ts`

- [ ] **Step 1: 更新 zh-CN.ts**

将文件中的：
- 第 4 行：`name: 'Graphiti Console'` → `name: 'OntoGraph Console'`
- 第 148 行：`title: 'ontograph-java'` → `title: 'OntoGraph'`

- [ ] **Step 2: 更新 en-US.ts**

将文件中的：
- 第 4 行：`name: 'Graphiti Console'` → `name: 'OntoGraph Console'`
- 第 148 行：`title: 'ontograph-java'` → `title: 'OntoGraph'`

- [ ] **Step 3: 更新 zh-TW.ts**

将文件中的：
- 第 4 行：`name: 'Graphiti Console'` → `name: 'OntoGraph Console'`
- 第 148 行：`title: 'ontograph-java'` → `title: 'OntoGraph'`

- [ ] **Step 4: 更新 ja-JP.ts**

将文件中的：
- 第 4 行：`name: 'Graphiti Console'` → `name: 'OntoGraph Console'`
- 第 149 行：`title: 'ontograph-java'` → `title: 'OntoGraph'`

- [ ] **Step 5: 提交国际化文件更新**

```bash
git add ontograph-web/src/i18n/locales/*.ts
git commit -m "i18n: update all locale files with OntoGraph branding"
```

---

### Task 18: 更新 i18n 初始化和认证工具

**Files:**
- Modify: `ontograph-web/src/i18n/index.ts`
- Modify: `ontograph-web/src/utils/auth.ts`

- [ ] **Step 1: 更新 i18n/index.ts**

将第 17 行替换为：

```typescript
const LOCALE_STORAGE_KEY = 'ontograph-locale'
```

- [ ] **Step 2: 更新 utils/auth.ts**

将第 1-2 行替换为：

```typescript
const TOKEN_KEY = 'ontograph_token'
const USER_KEY = 'ontograph_user'
```

- [ ] **Step 3: 提交工具函数更新**

```bash
git add ontograph-web/src/i18n/index.ts ontograph-web/src/utils/auth.ts
git commit -m "refactor: update storage keys with ontograph prefix"
```

---

### Task 19: 更新视图组件中的品牌文本

**Files:**
- Modify: `ontograph-web/src/views/graph/ide.vue`
- Modify: `ontograph-web/src/views/login/index.vue`
- Modify: `ontograph-web/src/router/index.ts`

- [ ] **Step 1: 更新 ide.vue Logo**

将第 19 行替换为：

```vue
<span class="logo-text">OntoGraph</span>
```

- [ ] **Step 2: 更新登录页版权**

将第 80 行替换为：

```vue
© 2026 OntoGraph · All rights reserved
```

- [ ] **Step 3: 更新路由标题**

将第 253 行替换为：

```typescript
document.title = `${translated} - OntoGraph Console`
```

- [ ] **Step 4: 提交视图组件更新**

```bash
git add ontograph-web/src/views/graph/ide.vue ontograph-web/src/views/login/index.vue ontograph-web/src/router/index.ts
git commit -m "ui: update view components with OntoGraph branding"
```

---

### Task 20: 更新本体编辑器 URI 示例

**Files:**
- Modify: `ontograph-web/src/components/Ontology/ClassEditor.vue`
- Modify: `ontograph-web/src/components/Ontology/PropertyEditor.vue`

- [ ] **Step 1: 更新 ClassEditor.vue**

将第 40 行和第 417 行中的 `graphiti.io` 替换为 `ontograph.io`：

```vue
<!-- Line 40 -->
<a-input v-model:value="form.classUri" placeholder="http://ontograph.io/ontology/Person" />

<!-- Line 417 -->
return cls?.classUri || `http://ontograph.io/${cls?.localName || id}`
```

- [ ] **Step 2: 更新 PropertyEditor.vue**

将第 37 行替换为：

```vue
<a-input v-model:value="form.propertyUri" placeholder="http://ontograph.io/ontology/name" />
```

- [ ] **Step 3: 提交本体编辑器更新**

```bash
git add ontograph-web/src/components/Ontology/ClassEditor.vue ontograph-web/src/components/Ontology/PropertyEditor.vue
git commit -m "ui: update ontology editor URI examples with ontograph.io"
```

---

### Task 21: 重命名类型文件并更新 API 路径

**Files:**
- Rename: `ontograph-web/src/types/graphiti.ts` → `ontograph.ts`
- Modify: `ontograph-web/src/api/search.ts`

- [ ] **Step 1: 重命名类型文件**

```powershell
Rename-Item -Path "ontograph-web/src/types/graphiti.ts" -NewName "ontograph.ts"

# 更新文件注释
$file = "ontograph-web/src/types/ontograph.ts"
$content = Get-Content $file -Raw -Encoding UTF8
$content = $content -replace "ontograph-java", "OntoGraph"
Set-Content -Path $file -Value $content -Encoding UTF8 -NoNewline
```

- [ ] **Step 2: 更新 search.ts 中的 API 路径**

将第 128、131、149、152、159、162 行中的 `/admin/graphiti/` 替换为 `/admin/ontograph/`：

```typescript
// Line 128
* 后端: GET /admin/ontograph/search-history/list

// Line 131
const resp = await request.get<any>('/admin/ontograph/search-history/list', {

// Line 149
* 后端: POST /admin/ontograph/search-history/save

// Line 152
await request.post('/admin/ontograph/search-history/save', null, {

// Line 159
* 后端: DELETE /admin/ontograph/search-history/clear

// Line 162
await request.delete('/admin/ontograph/search-history/clear')
```

- [ ] **Step 3: 提交类型文件和 API 路径更新**

```bash
git add ontograph-web/src/types/ontograph.ts ontograph-web/src/api/search.ts
git commit -m "refactor: rename types file and update API paths to /admin/ontograph/"
```

---

## 阶段 6：视觉资源更新

### Task 22: 在 ide.vue 中引入新 Logo

**Files:**
- Modify: `ontograph-web/src/views/graph/ide.vue`

- [ ] **Step 1: 添加 Logo 图片引用**

在 ide.vue 的合适位置（第 19 行附近）添加 Logo 图片：

```vue
<img src="/docs/product/images/ontoGraph.png" alt="OntoGraph Logo" class="logo-image" />
<span class="logo-text">OntoGraph</span>
```

- [ ] **Step 2: 添加样式（如需要）**

在 `<style>` 部分添加：

```css
.logo-image {
  height: 32px;
  margin-right: 8px;
}
```

- [ ] **Step 3: 提交 Logo 更新**

```bash
git add ontograph-web/src/views/graph/ide.vue
git commit -m "ui: add OntoGraph logo to IDE header"
```

---

## 阶段 7：全链路验证

### Task 23: 前端构建验证

**Files:**
- 无

- [ ] **Step 1: 安装依赖**

```bash
cd ontograph-web
pnpm install
```

**Expected:** 安装成功，无错误

- [ ] **Step 2: 构建项目**

```bash
pnpm build
```

**Expected:** BUILD SUCCESS，无 TypeScript 错误

- [ ] **Step 3: 提交构建验证**

```bash
git add .
git commit -m "chore: verify frontend build succeeds"
cd ..
```

---

### Task 24: 端到端品牌一致性检查

**Files:**
- 无

- [ ] **Step 1: 检查遗留的 Graphiti 引用**

```bash
# 查找除致谢外的所有 Graphiti 引用
grep -r "Graphiti" --include="*.md" --include="*.java" --include="*.ts" --include="*.vue" --include="*.yml" --include="*.json" . | grep -v "Acknowledgements" | grep -v "致谢" | grep -v "原 ontograph-java"
```

**Expected:** 仅有致谢部分包含 Graphiti

- [ ] **Step 2: 验证品牌一致性**

检查以下关键点：
- ✅ README.md 标题为 "OntoGraph"
- ✅ README_CN.md 标题为 "OntoGraph"
- ✅ pom.xml 中 groupId 为 "com.ontograph"
- ✅ application.yml 中配置前缀为 "ontograph"
- ✅ .env.example 中环境变量前缀为 "ONTOGRAPH_AI_"
- ✅ package.json 中 name 为 "ontograph-web"
- ✅ i18n 文件中的应用名为 "OntoGraph Console"

- [ ] **Step 3: 提交最终检查报告**

```bash
git add .
git commit -m "chore: complete brand consistency check - all Graphiti references updated to OntoGraph"
```

---

## 自审检查清单

### 1. 规范覆盖检查

- ✅ 文档层：README.md、README_CN.md、logo.md
- ✅ 后端配置：所有 pom.xml、application.yml、.env.example、docker-compose.yml
- ✅ 后端代码：包目录、package 声明、import 语句、注释、主应用类
- ✅ 前端配置：package.json
- ✅ 前端代码：i18n、视图组件、工具函数、本体编辑器、类型文件、API 路径
- ✅ 视觉资源：Logo 引入
- ✅ 全链路验证：编译、构建、一致性检查

### 2. 占位符扫描

- ✅ 无 "TBD"、"TODO"、"implement later"
- ✅ 所有步骤包含实际代码和命令
- ✅ 无 "add appropriate error handling" 等模糊描述

### 3. 类型一致性

- ✅ 所有包名统一为 `com.ontograph`
- ✅ 所有 artifactId 统一为 `ontograph-*`
- ✅ 所有环境变量前缀统一为 `ONTOGRAPH_AI_`
- ✅ 所有前端存储 key 统一为 `ontograph_*`

---

## 执行建议

**推荐执行方式：** Subagent-Driven（子代理驱动）

每个 Task 独立执行，完成后进行审查，确保：
1. 编译/构建通过
2. 无遗留错误
3. 品牌一致性正确

**总任务数：** 24 个 Task  
**预估时间：** 2.5 小时  
**风险等级：** 中（主要风险在 Task 10-14 的包名重构）

---

**计划状态：** ✅ 已完成  
**下一步：** 选择执行方式并开始实施
