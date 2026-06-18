# OntoGraph 工程 — 性能压测技术方案

> 本文档为 OntoGraph(原 Graphiti)知识图谱与系统管理服务提供完整的性能压测技术方案。
> 适用于后端 `ontograph-backend`(Spring Boot 9090)、Neo4j 图数据库、PostgreSQL、Redis 缓存、LM Studio / Spring AI 大模型服务的全链路性能验证。
> 通过 JMeter / Gatling / wrk / Playwright 等工具结合 Spring Boot Actuator、Prometheus、Grafana、Neo4j Metrics、JFR 等监控设施完成端到端压测、瓶颈定位与回归验证。

---

## 目录

- [1.压测目标与指标定义](#1压测目标与指标定义)
- [2.压测场景设计](#2压测场景设计)
- [3.压测工具与技术选型](#3压测工具与技术选型)
- [4.压测环境搭建](#4压测环境搭建)
- [5.压测执行策略](#5压测执行策略)
- [6.监控与指标采集](#6监控与指标采集)
- [7.结果分析与报告](#7结果分析与报告)
- [8.自动化集成](#8自动化集成)
- [9.附录](#9附录)

---

## 1.压测目标与指标定义

### 1.1 核心被测模块

| 模块 ID | 模块名称 | 被测对象 | 关键 Service / Controller | 压测重点 |
|---------|---------|---------|---------------------------|---------|
| **PT-G-01** | 图谱查询 | `/admin-api/graph/episode/page`, `/admin-api/graph/node/page`, `/admin-api/graph/edge/page` | `GraphitiService`, `EpisodeService`, `NodeService`, `EdgeService` | Neo4j Cypher 性能、分页、邻居查询深度 |
| **PT-G-02** | Graph IDE 可视化 | `/admin-api/graph/visualization`, `/admin-api/graph/visualization/expand` | `GraphVisualizationService`, `GraphNeo4jService` | 大图渲染、子图抽取、2-hop 邻居 |
| **PT-G-03** | 混合搜索 | `/admin-api/search/*`(全文 / 向量 / 混合 / BFS / 语义) | `SearchService`, `SearchPipelineService`, `RrfRerankerService` | 向量召回、CrossEncoder 重排、Pipeline 延迟 |
| **PT-G-04** | 数据导入 | `/admin-api/graph/episode/import`, `/admin-api/graph/bulk/import` | `DataImportService`, `BulkImportTaskService`, `GraphDriverService` | 大批量写入、Neo4j 事务批处理、事务一致性 |
| **PT-G-05** | 数据抽取(AI) | `/admin-api/extract/entities`, `/admin-api/extract/relations` | `EntityExtractorService`, `EdgeExtractorService`, `LlmClientService` | LLM 吞吐量、Token 限流、并发请求 |
| **PT-G-06** | AI / Embedding 服务 | `EmbedderService`, `CrossEncoderRerankerService`, `EmbeddingCacheService` | `EmbedderService`, `RerankingUtils` | Embedding QPS、向量缓存命中率、Rerank 延迟 |
| **PT-G-07** | 本体与 Schema | `/admin-api/ontology/*`, `/admin-api/schema/*` | `OntologyClassService`, `SchemaManagementService`, `OntologyReasoner` | 类树遍历、推理机、Schema 校验 |
| **PT-G-08** | 系统管理 | `/admin-api/system/user/*`, `/admin-api/system/role/*`, `/admin-api/system/operation-log/page` | `UserService`, `RoleService`, `OperationLogService` | CRUD、分页查询、JWT 鉴权 |

### 1.2 关键性能指标(KPI)

| 指标分类 | 指标名称 | 缩写 | 单位 | 说明 |
|---------|---------|------|------|------|
| **响应时间** | 平均响应时间 | Avg RT | ms | 所有成功请求 RT 的算术平均 |
| | 百分位响应时间 | P50 / P90 / P95 / P99 | ms | 95% / 99% 请求在该时间内完成 |
| | 最大响应时间 | Max RT | ms | 单次最大 RT(用于发现长尾) |
| | 首字节时间 | TTFB | ms | AI 流式接口首字节延迟 |
| **吞吐量** | 每秒事务数 | TPS | req/s | 单位时间成功完成的事务数 |
| | 每秒查询数 | QPS | req/s | 单位时间接收的请求数(含失败) |
| | 每分钟 Token 数 | TPM | tok/min | LLM 服务吞吐量(限流依据) |
| **稳定性** | 错误率 | Error Rate | % | HTTP 5xx / 业务异常 / 超时占比 |
| | 超时率 | Timeout Rate | % | 超过 SLA 阈值的请求占比 |
| **资源使用率** | CPU 使用率 | CPU% | % | 进程 / 节点 CPU 占用 |
| | 内存使用率 | Mem% | % | JVM Heap / 物理内存占用 |
| | GC 暂停时间 | GC Pause | ms | Young / Full GC 单次与累计暂停 |
| **连接池** | HikariCP 活跃连接 | Hikari Active | count | 关系数据库活跃连接数 |
| | Neo4j Driver 连接池 | Neo4j Pool | count | Bolt 连接获取/释放/等待 |
| | Lettuce / Redis 连接 | Redis Conn | count | Redis 连接活跃 / 空闲 |

### 1.3 性能基线与达标值

> 说明: 基线针对 `dev / staging` 环境、生产环境按 2x 容量评估。🔴 关键 / 🟡 重要 / 🟢 一般。

| 模块 | 接口示例 | P95 RT(ms) | TPS | Error Rate | 资源上限 |
|------|---------|-----------|-----|-----------|---------|
| 图谱查询(分页) | `GET /episode/page` | 🔴 ≤300 | 🟡 ≥200 | 🔴 ≤0.1% | CPU ≤70% |
| 图谱查询(深度) | `GET /node/2hop-neighbors` | 🔴 ≤800 | 🟡 ≥50 | 🔴 ≤0.5% | CPU ≤80% |
| 混合搜索(混合) | `POST /search/hybrid` | 🔴 ≤1500 | 🟡 ≥30 | 🔴 ≤1% | JVM Heap ≤75% |
| 数据导入(批量) | `POST /graph/episode/import` 100条 | 🔴 ≤3000 | 🟡 ≥20 批次/min | 🔴 ≤0.5% | Neo4j Heap ≤80% |
| 数据抽取(AI) | `POST /extract/entities` | 🔴 ≤8000 | 🟢 ≥5 | 🟡 ≤2% | LLM QPS ≤ 厂商限速 |
| Embedding 嵌入 | `POST /embed/batch` 64条 | 🔴 ≤1000 | 🟡 ≥50 | 🔴 ≤0.5% | Redis Hit ≥70% |
| 本体管理 | `GET /ontology/class/tree` | 🟡 ≤500 | 🟢 ≥100 | 🔴 ≤0.1% | CPU ≤60% |
| 系统管理 CRUD | `POST /system/user/create` | 🔴 ≤200 | 🟡 ≥300 | 🔴 ≤0.05% | Hikari Pool <80% |

### 1.4 非功能指标

| 指标 | 目标值 | 测试方法 |
|------|-------|---------|
| **可用性** | 99.9%(7×24h Soak 测试无 P0 故障) | 24h 浸泡 + 故障注入 |
| **可扩展性** | 水平扩容后 TPS 线性增长 ≥0.7x | 多实例压测对比 |
| **数据一致性** | 导入事务 100% 落库或全回滚 | 压测后校验 Neo4j / PG |
| **安全鉴权** | JWT 鉴权耗时 ≤10ms,无绕过 | 鉴权专项压测 |
| **缓存有效性** | Redis 命中率 ≥80% | 监控 `cache_hit_total` |

---

## 2.压测场景设计

### 2.1 单接口性能场景(Single-API)

| 场景编号 | 接口 | 方法 | 并发模型 | 压测参数 |
|---------|------|------|---------|---------|
| PT-S-01 | `/admin-api/graph/episode/page` | GET | 50 并发 / 5min | pageSize=20,groupId=固定 |
| PT-S-02 | `/admin-api/graph/node/page` | GET | 50 并发 / 5min | pageSize=50,depth=2 |
| PT-S-03 | `/admin-api/graph/edge/page` | GET | 30 并发 / 5min | pageSize=100 |
| PT-S-04 | `/admin-api/graph/visualization` | GET | 20 并发 / 5min | 节点数 500 / 1000 / 5000 |
| PT-S-05 | `/admin-api/search/hybrid` | POST | 20 并发 / 5min | query 长度 50 字,topK=10 |
| PT-S-06 | `/admin-api/search/cosine` | POST | 30 并发 / 5min | 向量维度 1536,topK=20 |
| PT-S-07 | `/admin-api/graph/episode/import` | POST | 10 并发 / 5min | 单次 50 条 episode |
| PT-S-08 | `/admin-api/extract/entities` | POST | 5 并发 / 5min | 文本长度 500 字 |
| PT-S-09 | `/admin-api/embed/batch` | POST | 30 并发 / 5min | batch=64 |
| PT-S-10 | `/admin-api/ontology/class/tree` | GET | 20 并发 / 5min | 类数量 100 / 500 |
| PT-S-11 | `/admin-api/system/user/page` | GET | 50 并发 / 5min | pageSize=20 |
| PT-S-12 | `/admin-api/system/auth/login` | POST | 100 并发 / 5min | 模拟登录风控场景 |

### 2.2 业务流程集成场景(E2E Business)

| 场景编号 | 业务链路 | 步骤拆解 | 并发模型 | 验收目标 |
|---------|---------|---------|---------|---------|
| PT-E-01 | **知识入库完整流** | ① 登录 → ② 数据抽取 → ③ 实体去重 → ④ 关系抽取 → ⑤ Episode 入库 → ⑥ Neo4j 同步 → ⑦ 索引刷新 | 5 VU / 10min | 端到端 P95 ≤15s,无中途失败 |
| PT-E-02 | **混合搜索查询流** | ① 登录 → ② 输入 query → ③ Embedding → ④ 向量召回 → ⑤ BM25 召回 → ⑥ RRF 融合 → ⑦ CrossEncoder 重排 → ⑧ 结果返回 | 30 VU / 10min | P95 ≤1500ms,Top10 准确率 ≥95% |
| PT-E-03 | **Graph IDE 探索流** | ① 登录 → ② 加载图谱 → ③ 双击节点 → ④ 加载邻居(2hop) → ⑤ 拖拽布局 → ⑥ 节点编辑 → ⑦ 保存 | 10 VU / 10min | 单步 P95 ≤800ms,无卡顿 |
| PT-E-04 | **本体管理流** | ① 登录 → ② 创建本体类 → ③ 添加属性 → ④ 添加约束 → ⑤ 推理校验 → ⑥ 发布 → ⑦ 数据校验 | 5 VU / 10min | 推理 P95 ≤2000ms |
| PT-E-05 | **法律知识图谱导入** | ① 登录 → ② 上传法律文本 → ③ 实体抽取 → ④ 法律关系抽取 → ⑤ 本体验证 → ⑥ 批量入库 → ⑦ 导出校验 | 3 VU / 15min | 1000 条法规 ≤30min |
| PT-E-06 | **管理员 CRUD 流** | ① 登录 → ② 创建用户 → ③ 分配角色 → ④ 创建角色 → ⑤ 绑定菜单 → ⑥ 操作日志查询 → ⑦ 退出 | 10 VU / 5min | P95 ≤500ms |

### 2.3 并发用户模拟场景(Concurrency)

| 场景编号 | 场景名 | 用户模型 | 持续时间 | 关注点 |
|---------|-------|---------|---------|-------|
| PT-C-01 | **低峰稳态** | 50 VU 恒定 | 30min | 验证无负载泄漏、内存稳定 |
| PT-C-02 | **高峰稳态** | 500 VU 恒定 | 30min | 验证 SLA、连接池充足 |
| PT-C-03 | **突发峰值** | 0→1000 VU(30s 阶跃) | 10min | 验证突发承载、限流降级 |
| PT-C-04 | **脉冲震荡** | 100↔800 VU 周期性切换 | 20min | 验证弹性伸缩、缓存预热 |
| PT-C-05 | **超载压垮** | 0→2000 VU 持续 | 5min | 找出系统极限点(Limit Test) |
| PT-C-06 | **混合并发** | 搜索 40% + 导入 20% + CRUD 30% + AI 10% | 30min | 验证多模块互斥资源争抢 |

### 2.4 数据量增长场景(Volume)

| 场景编号 | 数据规模 | 节点 / 边 | Episode 数 | 关注指标 |
|---------|---------|----------|-----------|---------|
| PT-V-01 | **小型图谱** | 1k 节点 / 5k 边 | 1k | 全接口基线 |
| PT-V-02 | **中型图谱** | 10k 节点 / 50k 边 | 10k | 查询衰减 ≤30% |
| PT-V-03 | **大型图谱** | 100k 节点 / 500k 边 | 100k | 2-hop 邻居 P95 ≤1500ms |
| PT-V-04 | **超大型图谱** | 1M 节点 / 5M 边 | 1M | 分页查询不超时 |
| PT-V-05 | **高基数属性** | 单节点 50+ 属性 | - | 属性索引效果 |
| PT-V-06 | **长链深度** | 10-hop 路径查询 | - | 路径查询性能 |

### 2.5 稳定性 / 浸泡场景(Soak)

| 场景编号 | 场景名 | 条件 | 持续时间 | 关注点 |
|---------|-------|------|---------|-------|
| PT-SK-01 | **24h 浸泡** | 中等并发 100 VU,混合场景 | 24h | 内存泄漏、连接泄漏、GC 恶化 |
| PT-SK-02 | **周末长跑** | 低并发 30 VU | 72h | 磁盘增长、日志膨胀 |
| PT-SK-03 | **缓存预热验证** | 冷启动后逐步加压 | 2h | 缓存预热曲线 |

### 2.6 故障注入场景(Chaos)

| 场景编号 | 故障类型 | 注入方式 | 验证目标 |
|---------|---------|---------|---------|
| PT-CH-01 | Neo4j 重启 | docker restart,30s 内 | 重连机制、事务回滚 |
| PT-CH-02 | Redis 不可用 | 断网 60s | 降级到 DB 查、不阻塞主流程 |
| PT-CH-03 | LLM 服务 5xx | mock 50% 失败率 | 重试机制、熔断降级 |
| PT-CH-04 | 慢 SQL 注入 | 触发全表扫描 | 慢 SQL 监控告警 |
| PT-CH-05 | 网络抖动 | tc netem 100ms±50ms | 超时配置合理性 |

---

## 3.压测工具与技术选型

### 3.1 接口压测工具对比

| 工具 | 协议支持 | 学习成本 | 性能上限 | 推荐场景 | 选型结论 |
|------|---------|---------|---------|---------|---------|
| **Apache JMeter** | HTTP / JDBC / JMS | 中(需 Java) | 4k 并发/JVM | 复杂场景编排、CI 集成 | ✅ **主选** |
| **Gatling** | HTTP / WebSocket | 中(Scala DSL) | 10k+ 并发 | 高并发、CI 友好、HTML 报告 | ✅ **推荐辅助** |
| **wrk / wrk2** | HTTP | 低(命令行) | 100k+ 并发 | 纯 HTTP 极限压测 | ✅ **基线对比** |
| **k6** | HTTP / gRPC | 低(JS) | 30k+ VU | DevOps 友好、阈值断言 | ✅ **回归测试** |
| **Vegeta** | HTTP | 低(Go) | 50k+ 并发 | 简单恒定速率压测 | 🟡 备用 |
| **Locust** | HTTP | 低(Python) | 5k 并发 | 分布式、UI 调试 | 🟡 备用 |
| **ab(Apache Bench)** | HTTP | 极低 | 5k 并发 | 单机快速验证 | 🟢 快速验证 |
| **hey** | HTTP/2 | 极低 | 10k 并发 | HTTP/2 验证 | 🟢 快速验证 |

**选型组合**: 主工具 JMeter(企业级 GUI / CI 集成 / 插件丰富);基线测试 wrk2 + Vegeta(快速定位 TPS 上限);回归测试 k6(GitHub Actions 原生支持);报告美化 JMeter → HTML Dashboard / k6 → InfluxDB + Grafana。

### 3.2 前端性能工具

| 工具 | 用途 | 关键能力 | 集成方式 |
|------|------|---------|---------|
| **Playwright** | E2E 浏览器自动化 + 性能采集 | Trace Viewer、Performance API | `playwright.config.ts` |
| **Lighthouse** | Web Vitals 打分 | FCP / LCP / CLS / TTI | CI(treo/lighthouse-ci) |
| **Chrome DevTools Protocol** | 深度性能剖析 | Network、Performance、Screenshot | `chrome-remote-interface` |
| **WebPageTest** | 多地域、多浏览器测试 | 真实网络环境 | 云端 / 自部署 |
| **Vue DevTools** | Vue 应用运行时分析 | 组件渲染、Pinia 状态 | 开发期 |
| **vite-bundle-visualizer** | 包体分析 | 打包体积可视化 | 构建期 |

### 3.3 监控与可观测工具

| 层级 | 工具 | 用途 | 部署方式 |
|------|------|------|---------|
| **应用指标** | Spring Boot Actuator | `/actuator/metrics`、`/actuator/health` | 内置(已集成) |
| **指标采集** | Micrometer + Prometheus | 指标导出与抓取 | 启用 `prometheus.enabled=true` |
| **可视化** | Grafana | 仪表板、告警 | Docker 部署 |
| **链路追踪** | Micrometer Tracing + Zipkin | 分布式调用链 | 可选集成 |
| **日志聚合** | Loki + Promtail / ELK | 集中日志查询 | Docker 部署 |
| **JVM 分析** | JDK Mission Control + JFR | JVM 内部事件录制 | CLI 工具 |
| **火焰图** | async-profiler | CPU / 锁 / 分配采样 | CLI 工具 |
| **Neo4j 监控** | Neo4j Browser / Metrics | Cypher 与运行时 | 内置 `:sysinfo` |
| **PostgreSQL** | pg_stat_statements / pgAdmin | 慢 SQL、锁等待 | 扩展安装 |
| **Redis** | Redis Exporter + INFO | 命中率、内存、连接 | 启用监控 |
| **节点监控** | Node Exporter + Prometheus | CPU / Mem / Disk / Net | Docker |
| **APM(可选)** | Apache SkyWalking / Elastic APM | 全链路追踪 | 部署 Agent |

### 3.4 AI 服务专项工具

| 工具 | 用途 |
|------|------|
| **LM Studio 内置监控** | 查看 LLM token/s、显存占用 |
| **Spring AI Actuator** | `spring.ai.chat.client` 调用次数、延迟 |
| **Custom Metrics** | 自定义 `ai.llm.tokens.total`、`ai.llm.latency` |
| **Token Counter** | tiktoken / jtokkit 计算成本 |
| **LLM Benchmark** | vLLM bench / llama.cpp bench 评估基线 |

---

## 4.压测环境搭建

### 4.1 硬件配置要求

> 原则: 压测环境独立于生产环境,避免资源争抢影响结果。

| 角色 | 推荐配置 | 数量 | 备注 |
|------|---------|------|------|
| **应用服务器** | 8C16G / 100G SSD | 2 台 | 一台被测,一台备用 |
| **压测机(Load Generator)** | 8C16G / 千兆网卡 | 2 台 | JMeter 分布式 |
| **PostgreSQL** | 8C32G / 500G NVMe | 1 台 | 独立部署,无其他负载 |
| **Neo4j** | 16C64G / 1T NVMe | 1 台 | JVM Heap ≥32G,pagecache ≥16G |
| **Redis** | 4C8G | 1 台 | 关闭持久化或 AOF everysec |
| **LM Studio / LLM 服务** | GPU A100(40G)/ A10 / 4090 | 1 台 | 与生产同型号 |
| **监控节点** | 4C8G | 1 台 | Prometheus + Grafana |

**网络**: 千兆内网,压测机与被测服务延迟 <1ms;带宽监控保证无丢包。

### 4.2 软件与网络配置

#### 4.2.1 软件版本对齐

| 软件 | 版本 | 来源 |
|------|------|------|
| JDK | 21 LTS(对齐 ontograph-backend) | Temurin / Corretto |
| Spring Boot | 3.x(对齐 pom.xml) | Maven |
| Neo4j | 5.x | Docker |
| PostgreSQL | 16 | Docker |
| Redis | 7.x | Docker |
| Prometheus | 2.50+ | Docker |
| Grafana | 10.x | Docker |
| JMeter | 5.6.3 | Apache 官方 |
| k6 | 0.49+ | 自带 |

#### 4.2.2 JVM 启动参数(被测服务)

```bash
java -jar ontograph-backend.jar \
  -Xms8g -Xmx8g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/var/log/ontograph/heapdump.hprof \
  -XX:+UnlockDiagnosticVMOptions \
  -XX:+DebugNonSafepoints \
  -XX:StartFlightRecording=duration=1h,filename=/var/log/ontograph/jfr/recording.jfr,settings=profile \
  -Dmanagement.endpoint.prometheus.enabled=true \
  -Dspring.profiles.active=perf
```

#### 4.2.3 application-perf.yml 配置

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,info,prometheus,threaddump,heapdump
  endpoint:
    health:
      show-details: always
    prometheus:
      access: read_only
  metrics:
    export:
      prometheus:
        enabled: true
    distribution:
      percentiles-histogram:
        http.server.requests: true
      percentiles:
        http.server.requests: 0.5,0.9,0.95,0.99
      sla:
        http.server.requests: 100ms,300ms,1s,3s

spring:
  datasource:
    hikari:
      maximum-pool-size: 50
      minimum-idle: 10
      connection-timeout: 3000
      validation-timeout: 2000
      idle-timeout: 600000
      max-lifetime: 1800000
      register-mbeans: true

ontograph:
  neo4j:
    max-connection-pool-size: 200
    connection-acquisition-timeout: 30s
    max-connection-lifetime: 1h
    connection-timeout: 5s
```

#### 4.2.4 Neo4j 配置(neo4j.conf)

```properties
server.memory.heap.initial_size=16g
server.memory.heap.max_size=32g
server.memory.pagecache.size=16g
server.bolt.thread_pool_min_size=20
server.bolt.thread_pool_max_size=200
server.bolt.connection_keep_alive=30m
metrics.enabled=true
metrics.prometheus.enabled=true
metrics.prometheus.endpoint=0.0.0.0:2004
metrics.jmx.enabled=true
db.logs.query.enabled=true
db.logs.query.threshold=500ms
db.logs.query.parameter_logging_enabled=true
db.logs.query.plan_description_enabled=true
db.index.default.schema_provider=native-btree-1.0
```

### 4.3 测试数据准备与清理

#### 4.3.1 数据生成策略

| 数据类型 | 生成方式 | 工具 | 数据量 |
|---------|---------|------|-------|
| **测试用户** | CSV → SQL 导入 | Faker + psql | 1k / 10k / 100k |
| **图谱节点** | Java 脚本批量构造 | `GraphDriverService.bulkInsert` | 1k ~ 1M |
| **图谱边** | 关联生成器 | `BulkImportTaskService` | 5x 节点数 |
| **Episode 文本** | Faker 模板 + LLM 生成 | Python + Ollama | 1k / 10k |
| **向量数据** | 复用已有 embedding | `EmbedderService` 批量 | 全部 |
| **法律语料** | 复用 `docs/training/legal-kg-quickstart.md` | 直接导入 | 按需 |

**生成脚本示例**:

```java
// scripts/perf/PerfDataGenerator.java
@Component
public class PerfDataGenerator {
    @Resource private GraphDriverService graphDriver;

    public void generateGraph(int nodeCount, int avgEdges) {
        List<Node> nodes = new ArrayList<>();
        for (int i = 0; i < nodeCount; i++) {
            nodes.add(Node.builder()
                .label("PERF_TEST_NODE")
                .name("node_" + i)
                .groupId("perf-group")
                .properties(Map.of("idx", i))
                .build());
        }
        graphDriver.bulkInsertNodes(nodes, 1000);

        Random rnd = new Random(42);
        List<Edge> edges = new ArrayList<>();
        for (int i = 0; i < nodeCount * avgEdges; i++) {
            edges.add(Edge.builder()
                .source("node_" + rnd.nextInt(nodeCount))
                .target("node_" + rnd.nextInt(nodeCount))
                .type("PERF_REL")
                .build());
        }
        graphDriver.bulkInsertEdges(edges, 2000);
    }
}
```

#### 4.3.2 数据隔离与清理

```bash
#!/bin/bash
# scripts/perf/cleanup.sh
set -e
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
echo "[$TIMESTAMP] Cleaning perf test data..."

psql -h localhost -U postgres -d graphiti <<EOF
DELETE FROM episode WHERE group_id LIKE 'perf-%';
DELETE FROM operation_log WHERE biz_id LIKE 'perf-%';
EOF

cypher-shell -u neo4j -p password123 <<EOF
MATCH (n) WHERE n.groupId STARTS WITH 'perf-' DETACH DELETE n;
EOF

redis-cli --scan --pattern 'perf:*' | xargs -L 100 redis-cli DEL

echo "[$(date +%Y%m%d_%H%M%S)] Cleanup done."
```

### 4.4 CI/CD 集成方案

```yaml
# .github/workflows/perf-test.yml
name: Performance Test

on:
  schedule:
    - cron: '0 2 * * 1'
  workflow_dispatch:

jobs:
  perf-test:
    runs-on: [self-hosted, perf-runner]
    timeout-minutes: 120
    steps:
      - uses: actions/checkout@v4

      - name: 启动 perf 环境
        run: docker compose -f docker-compose.perf.yml up -d

      - name: 健康检查
        run: |
          ./scripts/perf/wait-ready.sh localhost:9090
          ./scripts/perf/wait-ready.sh localhost:7687

      - name: 生成测试数据
        run: java -jar scripts/perf/data-gen.jar --scale=medium

      - name: JMeter 基线压测
        run: |
          jmeter -n -t plans/baseline.jmx \
            -l results/baseline.jtl \
            -e -o reports/baseline \
            -Jusers=200 -Jramp=60 -Jduration=600

      - name: k6 回归压测
        run: |
          k6 run --out json=results/k6.json \
            --vus 100 --duration 10m \
            tests/perf/regression.js

      - name: 阈值校验
        run: ./scripts/perf/check-thresholds.sh results/baseline.jtl

      - name: 上传报告
        uses: actions/upload-artifact@v4
        with:
          name: perf-report-${{ github.run_id }}
          path: reports/

      - name: 销毁环境
        if: always()
        run: docker compose -f docker-compose.perf.yml down -v
```

---

## 5.压测执行策略

### 5.1 逐步加压策略(Ramp-Up)

#### 5.1.1 加压模型表

| 阶段 | 持续时间 | 并发用户 | 增长方式 | 目标 |
|------|---------|---------|---------|------|
| **1. 暖机(Warm-up)** | 60s | 0 → 50 | 阶梯 +10/10s | JIT 编译、连接池预热 |
| **2. 爬坡(Ramp-up)** | 240s | 50 → 目标值 | 阶梯 +50/30s | 渐进触发 GC、缓存填充 |
| **3. 峰值(Peak)** | 600s | 目标值恒定 | 0 | 采集稳态指标 |
| **4. 降压(Ramp-down)** | 60s | 目标值 → 0 | -50/10s | 平滑退出、查看资源回收 |

#### 5.1.2 k6 阶梯配置

```javascript
// tests/perf/ramp-up.js
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '1m', target: 50 },
    { duration: '4m', target: 500 },
    { duration: '10m', target: 500 },
    { duration: '1m', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<500', 'p(99)<1500'],
    http_req_failed: ['rate<0.001'],
    http_reqs: ['count>10000'],
  },
};

export default function () {
  const res = http.get(`${__ENV.BASE_URL}/admin-api/graph/episode/page?pageNo=1&pageSize=20`, {
    headers: { Authorization: `Bearer ${__ENV.TOKEN}` },
  });
  check(res, {
    'status 200': (r) => r.status === 200,
    'rt<500': (r) => r.timings.duration < 500,
  });
  sleep(1);
}
```

#### 5.1.3 JMeter 阶梯配置示例

```xml
<com.blazemeter.jmeter.threads.concurrency.ConcurrencyThreadGroup>
  <stringProp name="Target Level">500</stringProp>
  <stringProp name="Ramp Up Time">240</stringProp>
  <stringProp name="Hold Target Rate Time">600</stringProp>
  <stringProp name="Steps">8</stringProp>
</com.blazemeter.jmeter.threads.concurrency.ConcurrencyThreadGroup>
```

### 5.2 压测周期与频率

| 阶段 | 频率 | 时长 | 触发条件 | 负责人 |
|------|------|------|---------|-------|
| **PR 冒烟** | 每次 PR | 5min | 任意 PR 触发 | CI 自动 |
| **每日全量** | 每日 02:00 | 60min | 工作日 | CI 自动 |
| **每周回归** | 每周一 | 4h | 重大版本 | QA |
| **大版本基线** | 每 Release | 8h | Release 前 | QA + Dev |
| **生产巡检** | 每月 | 2h | 月度 | SRE |
| **故障复盘** | 不定期 | 按需 | 性能事件后 | 应急小组 |

### 5.3 异常处理与终止条件

#### 5.3.1 自动终止条件(k6)

```javascript
thresholds: {
  http_req_failed: [{
    threshold: 'rate<0.05',
    abortOnFail: true,
    delay: '30s',
  }],
  http_req_duration: [{
    threshold: 'p(95)<5000',
    abortOnFail: true,
    delay: '60s',
  }],
}
```

#### 5.3.2 手动终止条件

| 现象 | 阈值 | 操作 |
|------|------|------|
| CPU 持续 ≥90% | 3 分钟 | 立即停止 |
| Full GC 频率 ≥1/min | 5 分钟 | 立即停止 |
| Heap 使用率 ≥95% | 立即 | 立即停止 |
| 错误率 ≥10% | 1 分钟 | 立即停止 |
| Neo4j OOM | 立即 | 立即停止 + 重启 |
| 网络丢包 ≥1% | 5 分钟 | 暂停分析 |
| 数据库死锁 ≥3 次 | 立即 | 停止 + 排查 |

#### 5.3.3 应急响应流程

```
异常触发 → 自动停止 → 截图日志 → 通知负责人(钉钉/企微)
   ↓
   ├─ P0(系统崩溃) → 立即拉群 → 启动应急流程
   ├─ P1(性能劣化) → 4h 内复盘 → 提交优化任务
   └─ P2(轻微抖动) → 次日晨会 → 记录跟踪
```

### 5.4 压测执行清单

**压测前(Pre-Check)**

- [ ] 环境已部署,所有服务健康检查通过
- [ ] 测试数据已注入并校验完整性
- [ ] 监控面板已配置(Grafana Dashboard 已打开)
- [ ] Prometheus 抓取任务运行正常
- [ ] JMeter 分布式压测机已连接
- [ ] JWT Token 已生成且未过期
- [ ] 备份已完成,可秒级回滚
- [ ] 通知渠道已就绪(钉钉/Webhook)

**压测中(In-Flight)**

- [ ] 实时监控 CPU / Mem / GC 曲线
- [ ] 实时观察错误率与响应时间分布
- [ ] 记录异常现象与时间点
- [ ] 每 10 分钟采样一次关键指标

**压测后(Post-Check)**

- [ ] 导出 JMeter / k6 原始报告
- [ ] 截图 Grafana 关键面板
- [ ] 备份慢日志与异常日志
- [ ] 数据清理或保留决策
- [ ] 24h 内提交压测报告

---

## 6.监控与指标采集

### 6.1 JVM 指标

| 指标 | 名称 | 告警阈值 | 采集频率 |
|------|------|---------|---------|
| Heap 使用量 | `jvm_memory_used_bytes{area="heap"}` | > 80% | 15s |
| Heap 容量 | `jvm_memory_max_bytes{area="heap"}` | - | 15s |
| GC 暂停次数 | `jvm_gc_pause_seconds_count` | 突增 50% | 15s |
| GC 暂停时长 | `jvm_gc_pause_seconds_sum` | P95 > 200ms | 15s |
| 线程数(Live) | `jvm_threads_live_threads` | > 500 | 15s |
| 线程死锁 | `jvm_threads_deadlocked` | > 0 🔴 | 15s |
| CPU 使用率 | `process_cpu_usage` | > 80% 持续 5min | 15s |

**JFR 录制与火焰图**

```bash
# 启动时录制
-XX:StartFlightRecording=duration=1h,filename=/var/log/jfr/recording.jfr,settings=profile

# 实时分析
jmc /var/log/jfr/recording.jfr

# CPU 火焰图(30s 采样)
./profiler.sh -d 30 -f /var/log/flamegraph/cpu.html <pid>
```

### 6.2 数据库(PostgreSQL)指标

| 指标 | 来源 | 告警阈值 |
|------|------|---------|
| 活跃连接数 | `pg_stat_activity` count | > 80% pool |
| 慢查询数 | `pg_stat_statements` mean_time > 1000ms | > 10/min |
| 死锁次数 | `pg_stat_database` deadlocks | > 0 🔴 |
| 缓存命中率 | `pg_stat_database` blks_hit / (hit+read) | < 95% |
| 表膨胀 | `pg_stat_user_tables` n_dead_tup / n_live_tup | > 20% |
| 锁等待 | `pg_locks` count granted=false | > 5 持续 1min |
| 复制延迟 | `pg_stat_replication` lag | > 30s |

**慢 SQL 开启**

```sql
-- postgresql.conf
shared_preload_libraries = 'pg_stat_statements'
pg_stat_statements.max = 10000
pg_stat_statements.track = top

-- 启用
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;

-- Top 10 慢 SQL
SELECT query, calls, mean_exec_time
FROM pg_stat_statements
ORDER BY mean_exec_time DESC LIMIT 10;
```

### 6.3 Neo4j 图数据库指标

| 指标 | 名称 | 告警阈值 |
|------|------|---------|
| Bolt 连接数 | `neo4j_bolt_connections_open` | > 80% pool |
| 事务回滚数 | `neo4j_transaction_rolledback` | 突增 50% |
| 节点数 | `neo4j_count_nodes` | 趋势告警 |
| 关系数 | `neo4j_count_relationships` | 趋势告警 |
| Page Cache 命中率 | `neo4j_page_cache_hit_ratio` | < 95% |
| Heap 使用量 | `neo4j_jvm_heap_used` | > 80% |
| GC 时间 | `neo4j_jvm_gc_time_total` | P95 > 200ms |

**Cypher 查询示例**

```cypher
-- 查询当前慢查询
CALL dbms.listQueries() YIELD queryId, query, elapsedTimeMillis, status
WHERE elapsedTimeMillis > 1000
RETURN queryId, query, elapsedTimeMillis, status
ORDER BY elapsedTimeMillis DESC;

-- 强制终止慢查询
CALL dbms.killQuery(queryId);
```

### 6.4 Redis 缓存指标

| 指标 | 来源 | 告警阈值 |
|------|------|---------|
| 内存使用 | `INFO memory` used_memory | > 80% maxmemory |
| 命中率 | `keyspace_hits / (hits+misses)` | < 80% |
| 连接数 | `INFO clients` connected_clients | > 8000 |
| OPS | `INFO stats` instantaneous_ops_per_sec | 监控基线 |
| 阻塞客户端 | `INFO clients` blocked_clients | > 10 |
| 主从延迟 | `INFO replication` lag | > 5s |

### 6.5 应用层指标

| 指标 | Micrometer 名称 | 告警阈值 |
|------|-----------------|---------|
| HTTP 请求耗时 | `http_server_requests_seconds_sum` | P95 > SLA |
| HTTP 错误数 | `http_server_requests_seconds_count{status="5xx"}` | > 1% |
| HikariCP 连接 | `hikaricp_connections_active` | > 80% pool |
| HikariCP 等待 | `hikaricp_connections_pending` | > 10 持续 1min |
| Tomcat 线程 | `tomcat_threads_busy` | > 80% max |
| 日志错误 | `logback_events_total{level="error"}` | > 10/min |

**Spring Boot Actuator 自定义指标**

```java
@Component
public class PerfMetrics {
    private final Counter llmCallCounter;
    private final Timer llmLatencyTimer;

    public PerfMetrics(MeterRegistry registry) {
        this.llmCallCounter = Counter.builder("ai.llm.calls")
            .tag("provider", "lmstudio")
            .register(registry);
        this.llmLatencyTimer = Timer.builder("ai.llm.latency")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(registry);
    }

    public <T> T recordLlmCall(Supplier<T> call) {
        Timer.Sample sample = Timer.start(Metrics.globalRegistry);
        try {
            llmCallCounter.increment();
            return call.get();
        } finally {
            sample.stop(llmLatencyTimer);
        }
    }
}
```

### 6.6 AI / LLM 服务指标

| 指标 | 来源 | 告警阈值 |
|------|------|---------|
| LLM 调用次数 | Micrometer `ai.llm.calls` | 趋势告警 |
| LLM 延迟 | Micrometer `ai.llm.latency` | P95 > 5s |
| Token 消耗 | Micrometer `ai.llm.tokens.total` | 成本告警 |
| Embedding QPS | Micrometer `ai.embedding.qps` | 限流基线 |
| Embedding 缓存命中率 | Micrometer `ai.embedding.cache.hit.ratio` | < 70% |
| Rerank 延迟 | Micrometer `ai.rerank.latency` | P95 > 1s |
| LLM 错误率 | Micrometer `ai.llm.errors` | > 5% |
| GPU 显存 | nvidia-smi | > 90% |

---

## 7.结果分析与报告

### 7.1 性能瓶颈识别方法

#### 7.1.1 USE 方法(Brendan Gregg)

```
For every resource, check:
  • Utilization  (使用率)
  • Saturation   (饱和度)
  • Errors       (错误)
```

| 资源 | 检查项 | 工具 |
|------|-------|------|
| **CPU** | 使用率、负载、上下文切换 | top, vmstat, async-profiler |
| **内存** | 使用率、Swap、泄漏 | jstat, MAT, JFR |
| **磁盘** | IOPS、带宽、队列深度 | iostat, iotop |
| **网络** | 带宽、重传、连接数 | netstat, ss, iftop |
| **JVM** | Heap、GC、线程 | JFR, jstack, jmap |
| **数据库** | 连接池、慢查询、锁 | pg_stat_statements |
| **Neo4j** | Bolt 连接、PageCache | Neo4j Metrics |

#### 7.1.2 RED 方法(Tom Wilkie)

针对每个服务:**Rate**(请求速率) / **Errors**(错误率) / **Duration**(响应时间)。

#### 7.1.3 常见瓶颈模式速查

| 现象 | 可能根因 | 排查路径 |
|------|---------|---------|
| P99 突然飙升 | 长尾 GC / 锁竞争 / 慢 SQL | JFR → async-profiler → DB 日志 |
| TPS 上不去,CPU 50% | 线程阻塞 / 锁等待 | jstack → DB 锁 / 分布式锁 |
| Heap 持续增长 | 内存泄漏 | jmap -histo → MAT 分析 |
| Full GC 频繁 | 大对象 / 缓存过大 | GC 日志 → JFR allocation |
| Neo4j 超时 | Cypher 全表扫描 / 无索引 | EXPLAIN → 添加索引 |
| LLM 排队严重 | 并发过高触发限流 | LM Studio 日志 → 降低并发 |
| 错误率突增 | 依赖服务故障 / 限流触发 | 应用日志 → 链路追踪 |
| 连接池耗尽 | 连接泄漏 / 慢查询占用 | HikariCP 指标 → 慢查询 |

### 7.2 报告模板与关键指标

#### 7.2.1 压测报告模板

```markdown
# OntoGraph 性能压测报告

**报告日期**: YYYY-MM-DD
**测试人员**: [姓名]
**测试环境**: staging / 性能环境
**被测版本**: vX.Y.Z (commit: xxxx)
**报告状态**: ✅ 通过 / ⚠️ 风险 / ❌ 不通过

## 测试摘要

| 项目 | 值 |
|------|---|
| 总场景数 | 30 |
| 通过 | 25 |
| 风险 | 3 |
| 失败 | 2 |
| 通过率 | 83.3% |
| 总执行时长 | 6h 30min |
| 整体错误率 | 0.12% |

## 关键模块压测结果

### 图谱查询(PT-S-01~04)

| 接口 | P50(ms) | P95(ms) | P99(ms) | TPS | Error | 结论 |
|------|---------|---------|---------|-----|-------|------|
| /episode/page | 45 | 120 | 280 | 285 | 0.02% | ✅ |
| /node/page | 65 | 180 | 350 | 215 | 0.05% | ✅ |
| /edge/page | 55 | 150 | 290 | 240 | 0.01% | ✅ |
| /visualization 5k | 230 | 780 | 1200 | 52 | 0.3% | ⚠️ |
| /visualization 50k | 850 | 2200 | 3500 | 12 | 1.2% | ❌ |

## 资源使用曲线

[嵌入 Grafana 截图:CPU / Mem / GC / Neo4j Pool]

## 性能瓶颈分析

### 瓶颈 1: 大图谱可视化超时
- **现象**: 50k 节点 P95 = 2200ms,SLA 800ms
- **根因**: Cypher 查询未走索引,`MATCH (n)-[r]->(m)` 全扫描
- **证据**: EXPLAIN 显示 `NodeByLabelScan` 64k 行
- **建议**:
  1. 添加 `CREATE INDEX ON :Node(groupId)`
  2. 启用 Neo4j Subquery 限制返回数量
  3. 前端 Force-Graph 启用节点聚合(>2000 节点时)

### 瓶颈 2: LLM 服务排队严重
- **现象**: Embedding 接口 P95 = 3500ms
- **根因**: LM Studio 默认并发 4,超过后串行排队
- **建议**:
  1. 提高 LM Studio `n_parallel` 至 16
  2. 启用本地 Embedding 缓存(命中率已达 68%)
  3. 引入异步批处理队列

## 优化建议

[详见 7.3]

## 附录

- 原始数据:JMeter JTL / k6 JSON
- Grafana 仪表板:[链接]
- JFR 录制:[链接]
- 慢查询日志:[链接]
```

#### 7.2.2 关键指标展示(P95 趋势对比)

```markdown
## P95 响应时间对比(基线 vs 本次)

| 接口 | 基线 | 本次 | 变化 | 趋势 |
|------|------|------|------|------|
| /episode/page | 95ms | 120ms | +26% | 📈 退化 |
| /search/hybrid | 1100ms | 1500ms | +36% | 📈 退化 |
| /ontology/class/tree | 320ms | 305ms | -5% | 📉 优化 |

## 错误率分布

| HTTP 状态码 | 占比 |
|-----------|------|
| 200 | 99.45% |
| 400 | 0.12% |
| 401 | 0.05% |
| 500 | 0.38% |
```

### 7.3 性能优化建议输出

#### 7.3.1 优化建议模板

```markdown
### OPT-XX: [优化标题]

**优先级**: 🔴 高 / 🟡 中 / 🟢 低
**预期收益**: P95 降低 X%,TPS 提升 Y
**实施成本**: 人日
**风险等级**: 低 / 中 / 高

**问题描述**:
[具体性能问题,含指标数据]

**根因分析**:
[通过火焰图 / 慢日志 / JFR 定位的根因]

**优化方案**:

#### 方案 A: [方案名](推荐)

\`\`\`java
// 代码示例
\`\`\`

\`\`\`sql
-- SQL 示例
\`\`\`

\`\`\`yaml
# 配置示例
\`\`\`

**验证方法**:
- [ ] 单元测试
- [ ] 基准测试(PT-S-XX)
- [ ] 回归压测
- [ ] 生产灰度

**参考文档**:
- [Spring Boot Performance Tuning](...)
- [Neo4j Performance Guide](...)
```

#### 7.3.2 常见优化方向清单

| 类别 | 优化方向 | 预期收益 |
|------|---------|---------|
| **JVM** | G1GC 调优、Heap 调整 | GC 暂停 -50% |
| **连接池** | HikariCP / Bolt Pool 容量 | 错误率 -80% |
| **缓存** | Redis 命中率优化 | RT -30% |
| **DB** | 索引添加 / SQL 重写 | RT -50% |
| **Neo4j** | 索引 / 约束 / Profile | RT -60% |
| **异步化** | CompletableFuture / MQ | 吞吐 +100% |
| **批量** | 批处理代替循环单条 | RT -70% |
| **LLM** | 缓存 / 异步 / 降级 | 成本 -60% |
| **前端** | 懒加载 / 虚拟滚动 / Web Worker | FCP -40% |

---

## 8.自动化集成

### 8.1 自动化压测流水线

#### 8.1.1 流水线架构

```
┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│  Git Push    │ →  │  CI Build    │ →  │  Deploy Perf │ →  │  Smoke Test  │
└──────────────┘    └──────────────┘    └──────────────┘    └──────────────┘
                                                                     ↓
┌──────────────┐    ┌──────────────┐    ┌──────────────┐    ┌──────────────┐
│  Archive     │ ←  │  Threshold   │ ←  │  JMeter/k6   │ ←  │  Data Inject │
│  Reports     │    │  Check       │    │  Execute     │    │              │
└──────────────┘    └──────────────┘    └──────────────┘    └──────────────┘
                            ↓
                  ┌──────────────────────┐
                  │  Slack/钉钉 通知     │
                  │  ❌ 失败阻断合并     │
                  │  ✅ 通过归档        │
                  └──────────────────────┘
```

#### 8.1.2 流水线脚本

```yaml
# .github/workflows/perf-ci.yml
name: Performance CI

on:
  pull_request:
    paths:
      - 'ontograph-backend/**'
      - 'scripts/perf/**'

jobs:
  perf-smoke:
    runs-on: [self-hosted, perf-runner]
    timeout-minutes: 30
    steps:
      - uses: actions/checkout@v4

      - name: 构建镜像
        run: docker build -t ontograph-perf:${{ github.sha }} ontograph-backend/

      - name: 启动环境
        run: |
          docker compose -f docker-compose.perf.yml up -d
          ./scripts/perf/wait-ready.sh

      - name: 注入小型数据集
        run: java -jar scripts/perf/data-gen.jar --scale=small

      - name: JMeter 冒烟测试
        run: |
          jmeter -n -t plans/smoke.jmx \
            -l results/smoke.jtl \
            -Jusers=50 -Jramp=30 -Jduration=300

      - name: k6 阈值校验
        run: k6 run --quiet tests/perf/smoke.js

      - name: 阈值校验脚本
        run: |
          #!/bin/bash
          FAILED=0
          P95=$(awk -F',' '{print $2}' results/smoke.jtl | sort -n | awk 'NR==int(NR*0.95)')
          ERROR_COUNT=$(awk -F',' '$4=="false"' results/smoke.jtl | wc -l)
          TOTAL=$(wc -l < results/smoke.jtl)
          ERR_PCT=$(echo "scale=4; $ERROR_COUNT / $TOTAL * 100" | bc)

          echo "P95: ${P95}ms"
          echo "Error Rate: ${ERR_PCT}%"

          if (( $(echo "$P95 > 1000" | bc -l) )); then
            echo "❌ P95 超过阈值 1000ms"
            FAILED=1
          fi
          if (( $(echo "$ERR_PCT > 0.5" | bc -l) )); then
            echo "❌ 错误率超过阈值 0.5%"
            FAILED=1
          fi

          exit $FAILED

      - name: 清理环境
        if: always()
        run: docker compose -f docker-compose.perf.yml down -v
```

### 8.2 定期性能回归测试

#### 8.2.1 回归测试策略

| 周期 | 范围 | 时长 | 环境 | 工具 |
|------|------|------|------|------|
| **PR 触发** | 核心 5 接口冒烟 | 30min | 性能环境 | k6 |
| **每日 02:00** | 全量核心 30 场景 | 4h | 性能环境 | JMeter + k6 |
| **每周一** | 完整基线 + 回归对比 | 8h | 性能环境 | JMeter |
| **Release 前** | 大版本全量压测 | 24h | 生产预发 | JMeter + Grafana |

#### 8.2.2 回归对比脚本

```bash
#!/bin/bash
# scripts/perf/regression-compare.sh
set -e

BASELINE_DIR="results/baseline-$(date +%Y%m%d)"
CURRENT_DIR="results/current-$(date +%Y%m%d)"

mkdir -p $BASELINE_DIR $CURRENT_DIR

echo "=== Running baseline ==="
jmeter -n -t plans/full.jmx -l $BASELINE_DIR/full.jtl \
  -Jusers=200 -Jramp=60 -Jduration=1800

echo "=== Running current ==="
jmeter -n -t plans/full.jmx -l $CURRENT_DIR/full.jtl \
  -Jusers=200 -Jramp=60 -Jduration=1800

echo "=== Generating regression report ==="
python scripts/perf/compare-jtl.py \
  --baseline $BASELINE_DIR/full.jtl \
  --current $CURRENT_DIR/full.jtl \
  --threshold 0.1 \
  --output reports/regression-$(date +%Y%m%d).html

REGRESSION=$(python scripts/perf/check-regression.py reports/regression-*.html)
if [ "$REGRESSION" == "FAIL" ]; then
  echo "❌ 性能回归超过阈值"
  exit 1
fi
echo "✅ 性能回归在可控范围"
```

```python
# scripts/perf/compare-jtl.py
import csv
import sys
from argparse import ArgumentParser

def load_jtl(path):
    samples = []
    with open(path) as f:
        reader = csv.DictReader(f)
        for row in reader:
            samples.append({
                'label': row['label'],
                'rt': int(row['elapsed']),
                'success': row['success'] == 'true',
            })
    return samples

def percentile(samples, p):
    sorted_s = sorted(samples)
    idx = int(len(sorted_s) * p / 100)
    return sorted_s[idx] if idx < len(sorted_s) else sorted_s[-1]

def compare(baseline, current, threshold=0.1):
    results = {}
    for label in set(s['label'] for s in baseline):
        b_rt = [s['rt'] for s in baseline if s['label'] == label]
        c_rt = [s['rt'] for s in current if s['label'] == label]
        b_p95 = percentile(b_rt, 95)
        c_p95 = percentile(c_rt, 95)
        delta = (c_p95 - b_p95) / b_p95 if b_p95 else 0
        results[label] = {
            'baseline_p95': b_p95,
            'current_p95': c_p95,
            'delta_pct': delta * 100,
            'regression': delta > threshold,
        }
    return results

if __name__ == '__main__':
    parser = ArgumentParser()
    parser.add_argument('--baseline', required=True)
    parser.add_argument('--current', required=True)
    parser.add_argument('--threshold', type=float, default=0.1)
    parser.add_argument('--output', required=True)
    args = parser.parse_args()

    baseline = load_jtl(args.baseline)
    current = load_jtl(args.current)
    results = compare(baseline, current, args.threshold)

    with open(args.output, 'w') as f:
        f.write('<html><body><h1>Regression Report</h1><table border="1">')
        f.write('<tr><th>Label</th><th>Baseline P95</th><th>Current P95</th><th>Delta</th><th>Status</th></tr>')
        for label, r in results.items():
            status = '❌ FAIL' if r['regression'] else '✅ PASS'
            f.write(f'<tr><td>{label}</td><td>{r["baseline_p95"]}ms</td>'
                    f'<td>{r["current_p95"]}ms</td><td>{r["delta_pct"]:.1f}%</td>'
                    f'<td>{status}</td></tr>')
        f.write('</table></body></html>')
```

### 8.3 性能告警与阈值

#### 8.3.1 Prometheus Alertmanager 告警规则

```yaml
# prometheus/alerts/ontograph-perf.yml
groups:
  - name: ontograph.perf.rules
    interval: 30s
    rules:
      # P95 响应时间告警
      - alert: HighP95Latency
        expr: |
          histogram_quantile(0.95,
            sum(rate(http_server_requests_seconds_bucket{application="ontograph-java"}[5m])) by (uri, le)
          ) > 1.0
        for: 2m
        labels:
          severity: warning
          team: backend
        annotations:
          summary: "P95 响应时间超过 1s"
          description: "{{ $labels.uri }} P95 = {{ $value }}s"

      # 错误率告警
      - alert: HighErrorRate
        expr: |
          sum(rate(http_server_requests_seconds_count{status=~"5xx",application="ontograph-java"}[5m]))
          / sum(rate(http_server_requests_seconds_count{application="ontograph-java"}[5m])) > 0.01
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "HTTP 5xx 错误率超过 1%"
          description: "当前错误率 {{ $value | humanizePercentage }}"

      # JVM Heap 使用率
      - alert: HighHeapUsage
        expr: |
          sum(jvm_memory_used_bytes{area="heap",application="ontograph-java"})
          / sum(jvm_memory_max_bytes{area="heap",application="ontograph-java"}) > 0.85
        for: 3m
        labels:
          severity: warning
        annotations:
          summary: "JVM Heap 使用率 > 85%"

      # Full GC 频率
      - alert: FrequentFullGC
        expr: |
          increase(jvm_gc_pause_seconds_count{gc="G1 Old Generation",application="ontograph-java"}[5m]) > 3
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "5 分钟内 Full GC 超过 3 次"

      # HikariCP 连接池等待
      - alert: HikariPoolExhausted
        expr: hikaricp_connections_pending{application="ontograph-java"} > 10
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "HikariCP 等待连接数 > 10"

      # Neo4j Bolt 连接池
      - alert: Neo4jPoolHigh
        expr: neo4j_bolt_connections_open / neo4j_bolt_connections_max > 0.8
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "Neo4j Bolt 连接池使用率 > 80%"

      # LLM 错误率
      - alert: HighLLMErrorRate
        expr: rate(ai_llm_errors_total[5m]) / rate(ai_llm_calls_total[5m]) > 0.05
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "LLM 错误率 > 5%"
```

#### 8.3.2 Alertmanager 路由配置

```yaml
# alertmanager.yml
route:
  group_by: ['alertname', 'cluster']
  group_wait: 30s
  group_interval: 5m
  repeat_interval: 4h
  receiver: 'default-receiver'
  routes:
    - match:
        severity: critical
      receiver: 'pager-duty'
      repeat_interval: 5m
    - match:
        team: backend
      receiver: 'backend-team'

receivers:
  - name: 'default-receiver'
    webhook_configs:
      - url: 'https://oapi.dingtalk.com/robot/send?access_token=xxx'
        send_resolved: true
  - name: 'pager-duty'
    pagerduty_configs:
      - service_key: 'xxx'
  - name: 'backend-team'
    slack_configs:
      - channel: '#backend-alerts'
        send_resolved: true
```

---

## 9.附录

### 9.1 模块清单(对齐 graphiti-scene.md)

本压测方案覆盖 `docs/scene/graphiti-scene.md` 中定义的 26 个测试模块中的核心性能相关模块,具体对应关系:

| 压测模块 | 对应业务模块 | 优先级 |
|---------|-------------|--------|
| PT-G-01 图谱查询 | G-01 / G-04 | 🔴 |
| PT-G-02 Graph IDE 可视化 | G-02 / G-04 | 🔴 |
| PT-G-03 混合搜索 | G-14 / G-15 | 🔴 |
| PT-G-04 数据导入 | G-11 | 🔴 |
| PT-G-05 数据抽取 | G-12 | 🟡 |
| PT-G-06 AI/Embedding | G-17 | 🟡 |
| PT-G-07 本体与 Schema | G-03 / G-06 / G-08 / G-09 | 🟡 |
| PT-G-08 系统管理 | SYS-01~05 | 🟡 |

### 9.2 文档版本

| 版本 | 日期 | 作者 | 变更 |
|------|------|------|------|
| v1.0 | 2026-06-18 | AI Agent | 初版发布,覆盖 8 大模块 30+ 压测场景 |