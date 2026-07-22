# Spring Boot 4.1 与 Spring AI 2.0 技术基线升级设计

**状态**：已确认，待计划分解  
**范围**：仅 `ontograph-backend` 的 Maven 依赖、Java 配置和测试；前端依赖不变。

## 1. 目标

将后端从 Spring Boot 3.5.5 / Spring AI 1.1.2 升级至 Spring Boot 4.1.0 / Spring AI 2.0.0，并把所有直接声明的第三方 Maven 依赖升级到与该基线兼容的稳定版本。保持现有 REST API、认证、Neo4j 图谱操作及 OpenAI、Qwen、Anthropic、Ollama 的业务能力不变。

Java 21 保持不变。版本由 Spring Boot BOM 管理的 Spring 生态依赖不得再单独钉版本；第三方依赖只保留经官方兼容矩阵或实际构建验证所需的显式版本。

## 2. 升级策略

采用一次“主干可构建”的兼容性升级，而不是只改两个版本号：

1. 先建立升级前构建、单测与关键 AI 配置的基线。
2. 用 Boot 4.1 BOM 与 Spring AI 2.0 BOM 统一依赖解析，再更新第三方直接依赖。
3. 按编译错误逐层迁移：Spring Boot → Spring Security / Web → Spring AI → 数据库和可观测性。
4. 最后做运行时冒烟：应用上下文、JWT 鉴权、OpenAPI、Neo4j、PostgreSQL、Redis，以及每个启用的 AI 提供方。

不引入 Spring AI 2.0 的 MCP、Agent 或工具调用新功能；这些功能留给法律案件工作台后续任务。

## 3. 依赖治理

| 类别 | 当前状态 | 设计决策 |
| --- | --- | --- |
| Spring Boot | 3.5.5 | 升至 4.1.0，由 BOM 管理 Spring Framework 7、Security 7、Jackson 3 等传递依赖。 |
| Spring AI | 1.1.2 | 升至 2.0.0，由 BOM 管理所有 Spring AI 模块。 |
| Web/OpenAPI | springdoc 2.8.5 | 选择官方支持 Spring Framework 7 / Boot 4 的稳定 springdoc 版本，并回归 Swagger UI。 |
| 数据访问 | MyBatis-Plus、动态数据源、Druid、Redisson、PostgreSQL、Neo4j Driver | 逐项更新至声明支持 Boot 4/Jakarta EE 11 的稳定版本；无法兼容者优先替换为 Boot 原生或被维护的替代方案。 |
| 安全与工具 | jjwt、Lombok、MapStruct、Hutool、POI、Caffeine | 升至稳定兼容版本；删除与 BOM 重复、冲突或已淘汰的显式声明。 |

最终必须使用 Maven Enforcer 或等价依赖分析保证无重复版本、无 Snapshots、无已知的 BOM 覆盖冲突。

## 4. Spring AI 2.0 迁移边界

Spring AI 2.0 基于 Boot 4，并重构 options 与配置属性。当前项目中 `spring.ai.*` 配置、`ChatClient`、`OpenAiChatModel` 和 `OpenAiEmbeddingModel` 的使用将逐项对照 2.0 官方升级说明迁移。

`OntoGraphApplication` 当前显式排除了 Anthropic、Azure OpenAI 和 Mistral 自动配置。这些类名/模块在 2.0 中可能已移动或被移除；升级后应改为最小化且可验证的自动配置策略，优先通过条件化依赖和配置开关避免加载无用 provider，而不依赖过期排除类。

Qwen 继续通过 OpenAI-compatible 配置接入；每个 provider 独立创建上下文测试，确保一个 provider 的缺失凭据不影响其他 provider 或基础应用启动。

## 5. 兼容性风险与处理

| 风险 | 处理方式 |
| --- | --- |
| Boot 4 引入 Spring Framework 7 / Jakarta EE 11 | 使用编译失败清单驱动迁移，并验证 Servlet、安全过滤器、验证注解和异常处理。 |
| Jackson 3 传递影响 | 以 API 契约测试覆盖请求/响应和 JSONB 类型处理；保留或升级兼容的 Jackson 模块。 |
| Spring AI 配置键与模型实现变化 | 为 OpenAI/Qwen、Anthropic、Ollama 分别建立最小上下文和一次真实或可替换的健康检查。 |
| 第三方 starter 不兼容 | 不强行锁旧版；选兼容稳定版或用标准 Spring Boot starter 替换。 |
| 安全与 OpenAPI 行为变化 | 用鉴权、未鉴权、Swagger UI 三类集成测试作为发布门槛。 |

## 6. 验收与回滚

验收必须满足：

- `mvn test`、打包和依赖分析通过；无故意跳过的现有测试。
- 应用能使用开发配置启动并暴露健康检查和 OpenAPI。
- JWT 登录/鉴权、Neo4j 读写、PostgreSQL、Redis、数据导入与混合检索关键路径通过回归测试。
- OpenAI/Qwen、Anthropic、Ollama 的配置绑定和模型 Bean 创建均可独立验证。
- 前端不改代码但能成功构建，并能调用升级后的 API。

升级在专门分支上以小提交完成；若某个第三方依赖阻断主路径，回滚该依赖或替换实现，禁止降级 Spring Boot 或 Spring AI 目标版本。
