# 四模块扁平化合并为 ontograph-backend 设计文档

**日期:** 2026-06-16  
**状态:** 已批准  
**类型:** 架构重构

---

## 背景与动机

### 当前问题

项目采用多模块 Maven 结构,包含四个紧密耦合的后端模块:

- `ontograph-framework` - 基础框架(安全、JWT、MyBatis-Plus、Redis)
- `ontograph-module-system` - 系统模块(用户、认证、权限)
- `ontograph-module-core` - 核心业务模块(图谱、本体、检索、导入)
- `ontograph-server` - 启动模块(Web配置、前端静态资源)

**模块间依赖链:**
```
ontograph-server
  └── ontograph-module-system
        └── ontograph-module-core
              └── ontograph-framework
```

**痛点:**
1. 模块间依赖管理复杂,每次新增依赖需考虑依赖传递
2. 构建速度慢,需依次编译四个模块
3. 模块边界模糊(如 module-system 依赖 framework,module-core 又依赖 module-system)
4. 团队规模较小,不需要如此细粒度的模块拆分

### 目标

- ✅ 简化项目结构,减少模块间依赖管理复杂度
- ✅ 提升构建效率,消除模块间依赖解析
- ✅ 保留现有功能完整性,不影响任何业务逻辑
- ✅ 最小化代码改动,降低重构风险

---

## 方案设计

### 方案选择:扁平化合并(方案A)

**核心原则:**
- 保留所有现有包名,零 import 修改
- 按功能分包,结构清晰
- 单一 `pom.xml`,统一依赖管理
- 最小改动,最大收益

### 目录结构设计

```
ontograph-backend/                          # 新建统一后端模块
├── src/
│   ├── main/
│   │   ├── java/com/ontograph/
│   │   │   ├── OntoGraphApplication.java   # 原 server/OntoGraphApplication
│   │   │   ├── config/                     # 原 server/config (SPA转发、Swagger)
│   │   │   │   ├── SpaForwardController.java
│   │   │   │   └── SwaggerConfig.java
│   │   │   ├── framework/                  # 原 framework (基础框架)
│   │   │   │   ├── common/
│   │   │   │   │   ├── constants/ResultCode.java
│   │   │   │   │   ├── exception/
│   │   │   │   │   │   ├── BusinessException.java
│   │   │   │   │   │   └── GlobalExceptionHandler.java
│   │   │   │   │   └── response/CommonResult.java
│   │   │   │   └── security/
│   │   │   │       ├── config/SecurityConfig.java
│   │   │   │       ├── jwt/
│   │   │   │       │   ├── JwtAuthenticationFilter.java
│   │   │   │       │   └── JwtTokenProvider.java
│   │   │   │       └── util/UserContext.java
│   │   │   ├── system/                     # 原 module-system (系统管理)
│   │   │   │   ├── controller/             # 8个Controller
│   │   │   │   ├── service/                # 10个Service + impl
│   │   │   │   ├── dal/
│   │   │   │   │   ├── dataobject/         # 8个DO
│   │   │   │   │   └── mysql/              # 8个Mapper
│   │   │   │   └── dto/                    # LoginRequest/Response
│   │   │   └── module/graphiti/            # 原 module-core (核心业务,保持原结构)
│   │   │       ├── config/                 # 4个配置类
│   │   │       ├── controller/admin/       # 19个Controller
│   │   │       ├── service/                # 44个Service + impl
│   │   │       ├── dal/
│   │   │       │   ├── dataobject/         # metadata/ont/子目录
│   │   │       │   ├── mysql/              # MySQL Mapper
│   │   │       │   └── repository/         # Neo4j Repository
│   │   │       ├── dto/                    # 批处理DTO等
│   │   │       ├── vo/                     # 视图对象(13个子目录)
│   │   │       ├── model/search/           # 搜索模型
│   │   │       ├── handler/                # MyBatis自动填充
│   │   │       ├── typehandler/            # PgJsonbTypeHandler
│   │   │       ├── util/                   # 工具类(7个)
│   │   │       └── exception/              # 自定义异常
│   │   └── resources/
│   │       ├── application.yml             # 原 server 配置
│   │       ├── application-dev.yml
│   │       ├── application-dev-local.yml.example
│   │       ├── static/                     # 原 server 的前端静态资源
│   │       │   └── index.html + assets/
│   │       ├── prompts/                    # 原 core 的Prompt模板
│   │       │   ├── business_info/
│   │       │   ├── extract_entities.txt
│   │       │   └── ... (7个文件)
│   │       └── db/migration/               # 原 core 的数据库迁移
│   │           └── V20260525__add_graph_import_task.sql
│   └── test/
│       └── java/com/ontograph/
│           ├── module/graphiti/            # 原 core 的测试(12个测试类)
│           └── PasswordTest.java           # 原 server 的测试
└── pom.xml                                 # 合并后的单一POM
```

### POM 依赖合并策略

**关键原则:**
1. 去除所有内部模块依赖(如 `ontograph-framework`、`ontograph-module-system` 等)
2. 保留所有第三方依赖,按来源分组便于审查
3. 继承父 POM 的 `dependencyManagement`,版本号统一管理
4. 包含 Spring Boot Repackage 插件,生成可执行 JAR

**依赖来源映射:**

| 来源模块 | 核心依赖 | 数量 |
|---------|---------|------|
| ontograph-framework | Spring Security, JWT, MyBatis-Plus, Redis, Druid | 12个 |
| ontograph-module-core | Neo4j, Spring AI(6个提供商), Jena, RDF4J, Caffeine | 15个 |
| ontograph-module-system | 无额外依赖(已包含在framework) | 0个 |
| ontograph-server | SpringDoc, Spring Boot Web | 2个 |

### 根 POM 调整

**模块声明变更:**
```xml
<!-- 原配置 -->
<modules>
    <module>ontograph-framework</module>
    <module>ontograph-module-system</module>
    <module>ontograph-module-core</module>
    <module>ontograph-server</module>
    <module>ontograph-api-tester</module>
</modules>

<!-- 新配置 -->
<modules>
    <module>ontograph-backend</module>        <!-- 新增 -->
    <module>ontograph-api-tester</module>     <!-- 保留 -->
</modules>
```

**依赖管理清理:**
移除 `dependencyManagement` 中的内部模块依赖声明:
- `ontograph-framework`
- `ontograph-module-system`
- `ontograph-module-core`

### 资源文件合并规则

| 来源模块 | 文件/目录 | 合并动作 |
|---------|----------|---------|
| ontograph-server | application.yml | ✅ 直接复制(主配置) |
| ontograph-server | application-dev.yml | ✅ 直接复制 |
| ontograph-server | application-dev-local.yml.example | ✅ 直接复制 |
| ontograph-server | static/ (前端资源) | ✅ 直接复制 |
| ontograph-module-core | prompts/ (Prompt模板) | ✅ 复制到根目录 |
| ontograph-module-core | db/migration/ | ✅ 复制到根目录 |

**配置文件无需修改**,因为:
- Spring Boot 的 `@ComponentScan` 默认扫描主类所在包及其子包
- MyBatis Mapper 扫描通过 `@MapperScan` 注解配置
- 静态资源 `static/` 在类路径根目录即可被 Spring MVC 自动服务

---

## 潜在风险与处理

### 风险矩阵

| 风险点 | 影响 | 概率 | 处理策略 |
|--------|------|------|---------|
| MyBatis Mapper 扫描路径 | Mapper 找不到 | 中 | 检查 `@MapperScan` 注解,确保覆盖 `com.ontograph.system.dal.mysql` 和 `com.ontograph.module.graphiti.dal.mysql` |
| Spring Security 配置 | 安全规则失效 | 低 | 确认 `SecurityConfig` 的 `@Configuration` 注解未被移除 |
| Neo4j 配置 | 图数据库连接失败 | 低 | 确认 `GraphNeo4jConfig` 在类路径中 |
| Prompt 模板加载 | 提示词找不到 | 中 | 确认 `PromptTemplateLoader` 使用 `classpath:prompts/` 前缀 |
| 测试类包路径 | 测试无法运行 | 低 | 保持测试类包路径与源码一致 |
| Docker 构建 | Dockerfile 引用旧模块 | 中 | 更新 `docker/Dockerfile` 中的模块引用 |

### 回滚策略

如果合并后出现严重问题:

1. 使用 Git 回退到合并前的 commit
2. 旧模块代码保留在 Git 历史中,可随时恢复
3. 建议在独立分支 `feature/backend-module-merge` 上实施

---

## 实施步骤

### Phase 1: 创建新模块结构 (预计 30 分钟)

1. 创建 `ontograph-backend` 目录
2. 创建标准 Maven 目录结构 (`src/main/java`, `src/main/resources`, `src/test/java`)
3. 创建合并后的 `pom.xml`

### Phase 2: 移动源码 (预计 30 分钟)

4. 移动 `ontograph-server/src/main/java/com/ontograph/` → `ontograph-backend/src/main/java/com/ontograph/`
5. 移动 `ontograph-framework/src/main/java/com/ontograph/` → `ontograph-backend/src/main/java/com/ontograph/`
6. 移动 `ontograph-module-system/src/main/java/com/ontograph/` → `ontograph-backend/src/main/java/com/ontograph/`
7. 移动 `ontograph-module-core/src/main/java/com/ontograph/` → `ontograph-backend/src/main/java/com/ontograph/`
8. 移动测试类到对应目录

### Phase 3: 移动资源文件 (预计 15 分钟)

9. 移动 `ontograph-server/src/main/resources/` → `ontograph-backend/src/main/resources/`
10. 移动 `ontograph-module-core/src/main/resources/prompts/` → `ontograph-backend/src/main/resources/prompts/`
11. 移动 `ontograph-module-core/src/main/resources/db/migration/` → `ontograph-backend/src/main/resources/db/migration/`

### Phase 4: 更新配置 (预计 15 分钟)

12. 更新根 `pom.xml` 模块声明
13. 移除根 `pom.xml` 中的内部模块依赖管理
14. 检查并更新 Docker 配置(如需要)

### Phase 5: 验证 (预计 30 分钟)

15. 执行 `mvn clean compile` 验证编译
16. 执行 `mvn test` 运行测试
17. 执行 `mvn package -DskipTests` 打包
18. 可选:启动服务验证关键端点

### Phase 6: 清理 (预计 10 分钟)

19. 删除旧模块目录
20. 提交变更到 Git

**总预计时间:** 2-2.5 小时

---

## 验证检查清单

### 编译验证
- [ ] `mvn clean compile` 无错误
- [ ] 所有 Lombok 注解处理正常
- [ ] MapStruct 映射生成正常

### 功能验证
- [ ] Spring Security 配置生效(访问受保护端点需认证)
- [ ] MyBatis Mapper 正常工作(执行数据库查询)
- [ ] Neo4j 连接正常(执行图谱查询)
- [ ] Prompt 模板加载正常(检查日志无错误)
- [ ] 静态资源可访问(访问 `http://localhost:8080/`)

### API 验证
- [ ] Swagger UI 可访问 (`http://localhost:8080/swagger-ui.html`)
- [ ] 健康检查端点正常 (`http://localhost:8080/actuator/health`)
- [ ] 核心业务 API 正常(如 `/api/ontology/*`)

### 测试验证
- [ ] 所有单元测试通过
- [ ] 测试覆盖率未降低

---

## 后续优化建议

合并完成后,可考虑以下优化:

1. **包结构调整** - 将 `module/graphiti` 提升为顶级包 `core`,统一命名风格
2. **依赖清理** - 审查是否存在未使用的依赖
3. **构建优化** - 配置 Maven 并行构建,提升编译速度
4. **CI/CD 调整** - 更新构建脚本,适配单一模块结构

---

## 附录

### 原模块依赖关系图

```
ontograph-server
  ├── ontograph-module-system
  │     └── ontograph-framework
  └── ontograph-module-core
        └── ontograph-module-system (循环依赖!)
              └── ontograph-framework
```

### 合并后依赖关系

```
ontograph-backend
  └── 所有第三方依赖(直接声明)
```

### 关键配置示例

**@MapperScan 配置检查点:**
```java
@MapperScan({
    "com.ontograph.system.dal.mysql",
    "com.ontograph.module.graphiti.dal.mysql"
})
```

**ComponentScan 自动覆盖范围:**
```java
@SpringBootApplication
// 默认扫描 com.ontograph.** 所有子包
public class OntoGraphApplication { }
```

---

**文档版本:** 1.0  
**最后更新:** 2026-06-16  
**维护者:** 架构团队
