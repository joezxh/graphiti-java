# OntoGraph 品牌重构设计文档

**日期：** 2026-05-25  
**版本：** v1.0  
**状态：** 待执行

---

## 1. 重构目标

将项目从 **ontograph-java** 品牌全面迁移至 **OntoGraph** 品牌，包括：
- 产品名称：OntoGraph
- Slogan：Where ontology becomes living structure.
- 中文感受：本体成为可运行的结构。
- 定位：知识图谱、本体建模、语义关系系统
- Logo：`docs/product/images/ontoGraph.png`

---

## 2. 技术决策

### 2.1 标识更新策略

| 技术标识 | 原文本 | 新文本 | 理由 |
|---------|--------|--------|------|
| **Java 包名** | `com.graphiti` | `com.ontograph` | 品牌一致性，需全局重构 |
| **Maven groupId** | `com.graphiti` | `com.ontograph` | 与包名同步 |
| **环境变量前缀** | `GRAPHTI_AI_` | `ONTOGRAPH_AI_` | 提升品牌统一性 |
| **前端存储 key** | `graphiti_*` | `ontograph_*` | 前端品牌一致性 |
| **历史致谢** | 保留 | 保留 | 体现开源传承 |

### 2.2 重构原则

1. **文本优先**：先改可见文本（文档、UI），再改技术标识
2. **分层验证**：每完成一层即验证编译/运行
3. **包名谨慎**：Java 包名重构需同步更新所有 import 和配置
4. **向后兼容**：保留对原 Graphiti Python 项目的致谢

---

## 3. 重构范围

### 3.1 文档层

| 文件 | 改动内容 |
|------|---------|
| `README.md` | 标题、描述、架构图模块名、快速开始路径 |
| `README_CN.md` | 标题、描述、架构图模块名、快速开始路径 |
| `docs/product/logo.md` | 保留 OntoGraph 方案，移除其他候选 |
| `DESIGN.md` | 如引用品牌名则更新 |

### 3.2 后端配置层

| 文件 | 改动内容 |
|------|---------|
| `pom.xml` (根) | groupId、artifactId、name、description |
| `graphiti-*/pom.xml` | groupId、artifactId |
| `application.yml` | spring.application.name、graphiti 配置前缀 → ontograph |
| `.env.example` | GRAPHTI_AI_ → ONTOGRAPH_AI_、注释中的品牌名 |
| `docker-compose.yml` | 服务名 ontograph-java → ontograph-java |
| `docker/Dockerfile` | 应用名引用 |

### 3.3 后端代码层

| 范围 | 改动内容 |
|------|---------|
| **Java 包名** | `com/graphiti/**` → `com/ontograph/**` (目录重命名) |
| **package 声明** | 所有 `.java` 文件的 `package com.graphiti` → `package com.ontograph` |
| **import 语句** | 所有 `import com.graphiti` → `import com.ontograph` |
| **代码注释** | 注释中的 "Graphiti" → "OntoGraph" |
| **日志配置** | `logging.level.com.graphiti` → `logging.level.com.ontograph` |

### 3.4 前端配置层

| 文件 | 改动内容 |
|------|---------|
| `ontograph-web/package.json` | name: `ontograph-web` → `ontograph-web` |
| `ontograph-web/vite.config.ts` | 如有品牌引用则更新 |
| `ontograph-web/.env.*` | 环境变量前缀 |

### 3.5 前端代码层

| 文件/范围 | 改动内容 |
|----------|---------|
| `src/i18n/locales/*.ts` (4个文件) | `Graphiti Console` → `OntoGraph Console`, `ontograph-java` → `OntoGraph` |
| `src/views/graph/ide.vue` | Logo 文本 "Graphiti" → "OntoGraph" |
| `src/views/login/index.vue` | 版权 "ontograph-java" → "OntoGraph" |
| `src/router/index.ts` | 页面标题 "Graphiti Console" → "OntoGraph Console" |
| `src/utils/auth.ts` | TOKEN_KEY/USER_KEY: `graphiti_*` → `ontograph_*` |
| `src/i18n/index.ts` | LOCALE_STORAGE_KEY: `graphiti-locale` → `ontograph-locale` |
| `src/components/Ontology/*.vue` | URI 示例中的 `graphiti.io` → `ontograph.io` |
| `src/types/graphiti.ts` | 文件名 → `ontograph.ts`，注释更新 |
| `src/api/*.ts` | API 路径中的 `/admin/graphiti/` → `/admin/ontograph/` (需后端同步) |

### 3.6 视觉资源

| 操作 | 说明 |
|------|------|
| Logo 图片 | 已存在：`docs/product/images/ontoGraph.png` |
| 前端引用 | 在 ide.vue 中引入新 Logo |
| favicon | 如有则更新 |

---

## 4. 执行顺序

### 阶段 1：文档层重构（低风险）
1. 更新 README.md / README_CN.md
2. 整理 docs/product/logo.md
3. 验证文档渲染

### 阶段 2：后端配置重构（中风险）
1. 更新根 pom.xml
2. 更新子模块 pom.xml
3. 更新 application.yml
4. 更新 .env.example
5. 更新 docker-compose.yml
6. **验证：`mvn clean compile`**

### 阶段 3：后端代码重构（高风险）
1. 重命名 Java 包目录：`com/graphiti` → `com/ontograph`
2. 批量替换 package 声明
3. 批量替换 import 语句
4. 更新代码注释
5. **验证：`mvn clean compile` + 运行测试**

### 阶段 4：前端配置重构（中风险）
1. 更新 package.json
2. 更新 .env 文件
3. **验证：`pnpm install`**

### 阶段 5：前端代码重构（中风险）
1. 更新 i18n 文件
2. 更新组件和视图
3. 更新路由和工具函数
4. 更新 API 路径（需与后端同步）
5. **验证：`pnpm build`**

### 阶段 6：视觉资源更新（低风险）
1. 在 ide.vue 中引入新 Logo
2. 更新登录页
3. **验证：前端运行检查**

### 阶段 7：全链路验证
1. 后端启动测试
2. 前端启动测试
3. API 联调测试
4. 登录流程测试

---

## 5. 风险控制

### 5.1 包名重构风险

**风险点：**
- import 遗漏导致编译失败
- MyBatis mapper 扫描路径错误
- Spring 组件扫描失败

**缓解措施：**
- 使用 IDE 重构功能（非文本替换）
- 每完成一批即编译验证
- 保留 git 提交点以便回滚

### 5.2 API 路径风险

**风险点：**
- 前后端 API 路径不同步
- 硬编码路径遗漏

**缓解措施：**
- 先改后端 Controller 路径
- 再改前端 API 调用
- 运行联调测试

### 5.3 环境变量风险

**风险点：**
- 运行时环境变量未更新
- Docker 容器启动失败

**缓解措施：**
- 同步更新 .env.example 和 .env
- 更新 docker-compose.yml 中的环境变量引用

---

## 6. 验证检查清单

### 编译验证
- [ ] `mvn clean compile` 无错误
- [ ] `mvn test` 全部通过
- [ ] `pnpm build` 无错误

### 运行验证
- [ ] 后端启动成功（`mvn spring-boot:run`）
- [ ] 前端启动成功（`pnpm dev`）
- [ ] Swagger UI 可访问
- [ ] 登录流程正常

### 品牌一致性验证
- [ ] README 标题为 "OntoGraph"
- [ ] 前端应用标题为 "OntoGraph Console"
- [ ] Logo 显示正确
- [ ] 版权信息正确
- [ ] 环境变量前缀为 ONTOGRAPH_
- [ ] Java 包名为 com.ontograph

### 代码质量验证
- [ ] 无遗留的 "Graphiti" 文本（除致谢外）
- [ ] 所有 import 路径正确
- [ ] 配置文件无遗漏

---

## 7. 回滚策略

### 紧急回滚
如果重构过程中发现严重问题：
```bash
# 回滚到重构前的提交
git reset --hard <pre-refactor-commit>
```

### 部分回滚
如果只是某一层出现问题：
- 后端问题：回退 pom.xml 和包名
- 前端问题：回退 package.json 和代码
- 配置问题：回退 .env 和 application.yml

---

## 8. 时间估算

| 阶段 | 预估时间 | 风险等级 |
|------|---------|---------|
| 阶段 1：文档层 | 15 分钟 | 低 |
| 阶段 2：后端配置 | 20 分钟 | 中 |
| 阶段 3：后端代码 | 40 分钟 | 高 |
| 阶段 4：前端配置 | 10 分钟 | 中 |
| 阶段 5：前端代码 | 30 分钟 | 中 |
| 阶段 6：视觉资源 | 10 分钟 | 低 |
| 阶段 7：全链路验证 | 20 分钟 | 低 |
| **总计** | **约 2.5 小时** | |

---

## 9. 特殊说明

### 9.1 保留的致谢内容

在 README.md 和 README_CN.md 的 Acknowledgements 部分保留：

```markdown
## Acknowledgements

OntoGraph (原 ontograph-java) 灵感来源于 Zep AI 的原始 [Graphiti](https://github.com/getzep/graphiti) Python 库。
```

### 9.2 URI 示例更新

前端本体编辑器中的 URI 示例：
- 旧：`http://graphiti.io/ontology/Person`
- 新：`http://ontograph.io/ontology/Person`

### 9.3 数据库影响

**本次重构不涉及：**
- 数据库表名
- 数据库 schema
- Neo4j 节点标签
- 已存储的数据

这些属于数据迁移范畴，不在品牌重构范围内。

---

## 10. 后续工作

品牌重构完成后，建议后续执行：
1. 更新 Git 远程仓库名（如需要）
2. 更新 CI/CD 配置中的项目引用
3. 更新文档站点（如有）
4. 发布版本说明（CHANGELOG）

---

**文档状态：** ✅ 已完成  
**下一步：** 进入实施计划编写阶段
