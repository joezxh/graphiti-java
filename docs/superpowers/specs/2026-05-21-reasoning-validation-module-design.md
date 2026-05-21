# 推理验证模块技术设计方案

**日期**: 2026-05-21
**方案选型**: 方案 A（增量增强架构）
**范围**: 后端推理验证引擎 + 前端 OntologyWorkbench 推理验证控制台面板

---

## 一、概述

### 1.1 背景

当前本体管理系统已具备基础能力：

- **验证引擎**：4 层验证（类型存在性 → 必填属性 → 数据类型 → 约束规则），由 `OntologyValidationServiceImpl` 实现
- **推理引擎**：基于 Apache Jena OWL 2 RL 的 `OntologyReasonerImpl`，但 `warmUp()` 未真正加载本体数据，`inferTypes()` 返回空列表
- **API 端点**：推理机状态、预热、一致性检查、批量验证

本方案将推理验证能力从"框架占位"提升到"生产可用"，补全 6 层验证体系、实现真正的 OWL 2 RL 推理、并配套前端控制台面板。

### 1.2 设计目标

1. **完整性**：实现 6 层验证（L1-L4 增强 + L5 领域规则 + L6 图谱完整性）
2. **可用性**：推理引擎能真正从数据库加载本体并执行 OWL 2 RL 推理
3. **可观测性**：前端提供推理验证控制台，实时查看推理状态、验证报告、一致性结果
4. **性能**：推理缓存、异步检查、并发优化
5. **稳定性**：降级策略、异常隔离、错误聚合

---

## 二、模块架构设计

### 2.1 总体架构

保持现有 **Controller → Service → Mapper/DO** 三层架构不变，在 Service 层内部新增验证子层：

```
OntologyController (新增推理验证端点)
    │
    ├─ OntologyReasoner (推理引擎接口)
    │       └─ OntologyReasonerImpl
    │           ├─ Jena InfModel (OWL 2 RL 推理模型)
    │           └─ OntModel (本体 RDF 模型)
    │
    ├─ OntologyValidationService (6层验证编排器)
    │       └─ OntologyValidationServiceImpl
    │           ├─ NodeValidator (L1-L4)
    │           ├─ EdgeValidator (L1-L4)
    │           ├─ DomainRuleValidator (L5)
    │           └─ GraphIntegrityValidator (L6)
    │
    └─ DomainRuleService (L5 规则管理)
            └─ DomainRuleMapper / OntDomainRuleDO
```

### 2.2 组件职责

| 组件 | 职责 | 现有状态 |
|------|------|---------|
| `OntologyReasoner` | OWL 2 RL 推理：类层次查询、类型推断、一致性检查、可满足性检查 | 接口已有，实现需增强 |
| `OntologyValidationService` | 6 层验证编排：按顺序执行 L1-L6，聚合结果 | 4 层已实现，需扩展 |
| `DomainRuleService` | L5 领域规则的 CRUD、启用/禁用、表达式测试 | 新增 |
| `GraphIntegrityValidator` | L6 图谱完整性：Neo4j 查询孤立节点、必填关系、domain/range 违规 | 新增 |
| `OntologyController` | REST API 入口，聚合推理验证报告 | 需新增端点 |

### 2.3 前端架构

在 `OntologyWorkbench` 中新增 **`reasoning-validation`** 标签页，包含子面板：

- **推理机控制台**：状态显示、预热/关闭按钮、缓存信息
- **一致性检查面板**：执行检查、展示不一致类型列表、解释信息
- **验证报告面板**：6 层验证结果汇总、错误详情、导出报告
- **领域规则配置**：规则列表、SpEL 表达式编辑器、测试执行、启用/禁用
- **图谱完整性检查**：检查项选择、异步执行、结果展示

---

## 三、技术栈选择

| 层级 | 技术选型 | 说明 |
|------|---------|------|
| 推理引擎 | **Apache Jena 4.9.0** | 已引入，`ReasonerRegistry.getOWLReasoner()` 提供 OWL 2 RL 推理 |
| 推理缓存 | **Caffeine**（或 Guava Cache） | 按 graphId 隔离，支持过期和大小限制 |
| 规则表达式 | **Spring Expression Language (SpEL)** | L5 领域规则条件表达式，与 Spring 生态零成本集成 |
| 规则存储 | **PostgreSQL + MyBatis-Plus** | 新增 `ont_domain_rule` 表，复用现有持久层 |
| 图谱查询 | **Neo4j Cypher** | L6 需查询图数据库，复用现有 `Neo4jClient` |
| 异步任务 | **Spring @Async + 任务表** | L6 全图扫描耗时较长，异步执行 |
| 前端组件 | **Vue 3 + Ant Design Vue + ECharts** | 与现有 OntologyWorkbench 保持一致 |

**不选其他技术的理由**：
- Drools/Aviator：规则数量少，SpEL 足够且无需额外依赖
- 独立规则引擎服务：与当前单体架构不符，增加通信复杂度

---

## 四、核心功能实现

### 4.1 六层验证体系

| 层级 | 名称 | 职责 | 错误码 | 失败策略 |
|------|------|------|--------|----------|
| **L1** | 类型存在性 | 验证 `nodeType`/`edgeType` 在本体中已定义 | `ONT001` | **阻断**：后续层不再执行 |
| **L2** | 必填属性校验 | 验证 `isRequired=true` 的属性已提供且非空 | `ONT002` | **累积**：继续执行 L3-L4，最终汇总 |
| **L3** | 数据类型校验 | 验证属性值类型匹配 `rangeDataType` | `ONT003` | **累积** |
| **L4** | 约束规则校验 | 验证 PATTERN/RANGE/ENUM/CARDINALITY/NOT_NULL/CUSTOM | `ONT004` | **累积** |
| **L5** | 领域规则验证 | 验证跨属性业务规则（SpEL 表达式） | `ONT005` | **累积** |
| **L6** | 图谱完整性验证 | 验证孤立节点、必填关系缺失、domain/range 合规 | `ONT006` | **异步**：仅在批量导入/全量检查场景执行 |

#### 4.1.1 L1-L4 增强

**L4 约束扩展**：现有代码已支持 PATTERN、RANGE、ENUM，需新增：

- **CARDINALITY**：检查多值属性数量是否在 `[minCardinality, maxCardinality]` 范围内
- **NOT_NULL**：与 L2 互补，针对非必填但提供后不能为空的场景
- **CUSTOM**：原始 JSON 约束，由前端 `ConstraintValueEditor` 配置

#### 4.1.2 L5 领域规则验证（新增）

**规则模型**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `ruleName` | String | 规则名称（如"企业成立日期不能晚于注销日期"） |
| `ruleCode` | String | 唯一编码 |
| `spelExpression` | String | SpEL 表达式（如 `#establishmentDate <= #dissolutionDate`） |
| `applicableClassIds` | List<Long> | 适用的本体类 ID 列表，空表示全部 |
| `severity` | String | `ERROR` / `WARNING` |
| `errorMessage` | String | 违反时的提示信息 |
| `enabled` | boolean | 是否启用 |

**执行流程**：

1. 查询适用于当前类且 `enabled=true` 的领域规则
2. 使用 `SpelExpressionParser` 解析表达式
3. 将节点属性注入 `StandardEvaluationContext`
4. 求值并收集失败结果

#### 4.1.3 L6 图谱完整性验证（新增）

L6 需要查询 Neo4j 图数据库，与 L1-L5 的"单节点/单边验证"不同，L6 是"图谱级"验证：

| 检查项 | Cypher 查询逻辑 | 说明 |
|--------|----------------|------|
| 孤立节点检测 | `MATCH (n) WHERE NOT (n)--() RETURN n` | 发现没有任何关系的节点 |
| 必填关系缺失 | `MATCH (n:Person) WHERE NOT (n)-[:WORKS_AT]->() RETURN n` | 某类节点必须存在某类关系 |
| Domain/Range 违规 | `MATCH (a)-[r:WORKS_AT]->(b) WHERE NOT a:Person OR NOT b:Company RETURN r` | 关系两端类型是否符合本体定义 |

**执行策略**：L6 不阻塞实时写入，仅在以下场景触发：
- 批量数据导入后的后验检查
- 用户主动点击"图谱完整性检查"按钮
- 定时任务（可选）

### 4.2 推理引擎增强

#### 4.2.1 warmUp() — 本体数据加载

当前 `warmUp()` 仅创建空模型，增强后需从数据库加载：

1. 查询活跃本体定义 `definitionId`
2. 加载类定义：创建 OWL Class，设置 `subClassOf`、`equivalentClass`、`disjointWith`
3. 加载属性定义：创建 ObjectProperty/DatatypeProperty，设置 `domain`、`range`、`inverseOf`
4. 加载约束：创建 OWL Restrictions（如 cardinality restrictions）
5. 绑定 OWL 2 RL Reasoner，生成 InfModel

#### 4.2.2 inferTypes() — 属性驱动的类型推断

当前返回空列表，增强后：

1. 收集输入属性名
2. 查询每个属性的 `rdfs:domain` → 候选类集合
3. 统计每个候选类的匹配属性数作为得分
4. 返回按得分排序的候选类型列表

#### 4.2.3 新增推理接口

| 接口方法 | 用途 | 实现 |
|----------|------|------|
| `getPropertyDomains(graphId, propertyUri)` | 获取属性适用的类 | `InfModel.listStatements(prop, RDFS.domain, null)` |
| `getPropertyRanges(graphId, propertyUri)` | 获取属性值的类型 | `InfModel.listStatements(prop, RDFS.range, null)` |
| `explainInconsistency(graphId)` | 解释不一致性原因 | 解析 Jena `ValidityReport` |

---

## 五、数据模型设计

### 5.1 新增数据库表

**`ont_domain_rule`** — L5 领域规则存储

```sql
CREATE TABLE ont_domain_rule (
    id                  BIGSERIAL PRIMARY KEY,
    definition_id       BIGINT NOT NULL,
    rule_name           VARCHAR(128) NOT NULL,
    rule_code           VARCHAR(64) NOT NULL,
    spel_expression     TEXT NOT NULL,
    applicable_class_ids JSONB,
    severity            VARCHAR(16) DEFAULT 'ERROR',
    error_message       VARCHAR(512),
    description         TEXT,
    enabled             BOOLEAN DEFAULT TRUE,
    created_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(definition_id, rule_code)
);
```

### 5.2 VO 设计

**`DomainRuleVO`**

```java
@Data @Builder
public class DomainRuleVO {
    private Long id;
    private String ruleName;
    private String ruleCode;
    private String spelExpression;
    private List<Long> applicableClassIds;
    private String severity;
    private String errorMessage;
    private String description;
    private boolean enabled;
}
```

**`ValidationResultVO`**（扩展现有）

```java
@Data @Builder
public class ValidationResultVO {
    private boolean passed;
    private int level;            // 失败时的最高层级 1-6，0=通过
    private List<ValidationErrorVO> errors;
    private List<ValidationWarningVO> warnings;
    private Map<String, Object> enrichedProperties;
}
```

**`InferredTypeVO`**

```java
@Data @AllArgsConstructor
public class InferredTypeVO {
    private String classUri;
    private int matchScore;
}
```

**`GraphIntegrityResultVO`**

```java
@Data @Builder
public class GraphIntegrityResultVO {
    private boolean passed;
    private String checkType;    // ISOLATED_NODE / REQUIRED_RELATION / DOMAIN_RANGE
    private int violationCount;
    private List<ViolationVO> violations;
}
```

**`ReasoningReportVO`** — 推理验证综合报告

```java
@Data @Builder
public class ReasoningReportVO {
    private String graphId;
    private boolean reasonerWarmedUp;
    private ConsistencyResultVO consistency;
    private ValidationSummaryVO validationSummary;
    private List<GraphIntegrityResultVO> integrityChecks;
    private List<InferredTypeVO> inferredTypes;
    private long reportTime;
}
```

---

## 六、REST API 接口设计

所有接口挂载在 `/api/v1/ontology/{graphId}` 下。

### 6.1 推理引擎接口

| 方法 | 端点 | 说明 | 状态 |
|------|------|------|------|
| `GET` | `/{graphId}/reasoners/status` | 推理机状态 | 已有 |
| `POST` | `/{graphId}/reasoners/warmup` | 预热推理机 | 已有，增强实现 |
| `GET` | `/{graphId}/consistency` | 一致性检查 | 已有，增强返回 |
| `POST` | `/{graphId}/reasoners/infer-types` | 属性驱动类型推断 | **新增** |
| `GET` | `/{graphId}/classes/{classUri}/ancestors` | 获取祖先类 | **新增** |
| `GET` | `/{graphId}/classes/{classUri}/descendants` | 获取后代类 | **新增** |
| `GET` | `/{graphId}/properties/{propertyUri}/domains` | 属性 domain 推导 | **新增** |
| `GET` | `/{graphId}/properties/{propertyUri}/ranges` | 属性 range 推导 | **新增** |

### 6.2 验证接口

| 方法 | 端点 | 说明 | 状态 |
|------|------|------|------|
| `POST` | `/{graphId}/validate/batch` | 批量验证（L1-L5） | 已有，L5 自动包含 |
| `POST` | `/{graphId}/validate/node` | 单节点验证 | **新增** |
| `POST` | `/{graphId}/validate/edge` | 单边验证 | **新增** |
| `POST` | `/{graphId}/validate/integrity` | L6 图谱完整性检查（异步） | **新增** |
| `GET` | `/validate/tasks/{taskId}` | 查询异步验证任务结果 | **新增** |

### 6.3 领域规则管理接口

| 方法 | 端点 | 说明 |
|------|------|------|
| `GET` | `/{graphId}/domain-rules` | 列出领域规则 |
| `POST` | `/{graphId}/domain-rules` | 创建领域规则 |
| `PUT` | `/{graphId}/domain-rules/{ruleId}` | 更新领域规则 |
| `DELETE` | `/{graphId}/domain-rules/{ruleId}` | 删除领域规则 |
| `POST` | `/{graphId}/domain-rules/{ruleId}/toggle` | 启用/禁用规则 |
| `POST` | `/{graphId}/domain-rules/{ruleId}/test` | 测试 SpEL 表达式 |

### 6.4 综合报告接口

| 方法 | 端点 | 说明 |
|------|------|------|
| `GET` | `/{graphId}/reasoning-report` | 推理验证综合报告（聚合所有信息） |

---

## 七、集成方案

### 7.1 与本体管理系统的集成

**缓存失效策略**：本体数据变更时，推理缓存必须自动失效。

| 操作 | 缓存策略 |
|------|---------|
| 创建/更新/删除类 | 立即失效推理缓存 |
| 创建/更新/删除属性 | 立即失效推理缓存 |
| 创建/更新/删除约束 | 无需失效推理缓存 |
| 版本回滚 | 失效推理缓存 + 下次访问重新 warmUp |

实现方式：在 `OntologyClassServiceImpl` / `OntologyPropertyServiceImpl` 的写操作后调用 `reasoner.shutdown(graphId)`。

### 7.2 与图数据库（Neo4j）的集成

L6 图谱完整性验证通过复用现有 `Neo4jClient` 查询：

```java
@Service
public class GraphIntegrityValidator {
    @Autowired
    private Neo4jClient neo4jClient;
    
    public List<GraphIntegrityResultVO> validate(String graphId, List<String> checkTypes) {
        // 根据 checkTypes 执行对应的 Cypher 查询
    }
}
```

### 7.3 与数据导入流程的集成

在批量数据导入完成后，自动触发 L6 异步检查：

```java
public void afterImport(String graphId, ImportResult result) {
    if (result.getImportedNodes() > 0 || result.getImportedEdges() > 0) {
        asyncIntegrityCheck(graphId);
    }
}
```

---

## 八、性能考虑

### 8.1 推理缓存策略

引入 **Caffeine** 缓存替代原生 `ConcurrentHashMap`：

```java
private final Cache<String, InfModel> infModelCache = Caffeine.newBuilder()
    .maximumSize(50)
    .expireAfterAccess(Duration.ofHours(2))
    .removalListener((key, value, cause) -> {
        if (value instanceof InfModel model) {
            model.removeAll();
        }
    })
    .build();
```

### 8.2 预热机制

- **懒加载**：首次访问推理接口时，若缓存未命中，同步执行 `warmUp()`
- **异步预热**：图谱创建/本体导入完成后，后台线程异步预热

### 8.3 并发处理

按 `graphId` 加读写锁，避免不同图谱间的竞争：

```java
private final Map<String, ReentrantReadWriteLock> locks = new ConcurrentHashMap<>();

public void warmUp(String graphId) {
    getLock(graphId).writeLock().lock();
    try { /* ... */ } finally { /* unlock */ }
}
```

### 8.4 L6 异步化

图谱完整性检查涉及全图扫描，耗时较长（秒级），采用异步任务模式：

1. 提交检查时返回 `taskId`
2. 后台线程执行 Cypher 查询
3. 通过 `GET /validate/tasks/{taskId}` 轮询结果

---

## 九、错误处理策略

### 9.1 不一致性检测与报告

OWL 一致性检查可能发现的不一致类型：

| 不一致类型 | 示例 |
|-----------|------|
| 类不可满足 | `Person` 被定义为等价于 `Company` |
| 循环继承 | `A subClassOf B` 且 `B subClassOf A` |
| 属性 domain/range 冲突 | `name` 的 domain 同时声明为 `Person` 和 `Company` |
| 基数冲突 | `hasChild maxCardinality 0` 但实例存在 `hasChild` |

通过解析 Jena `ValidityReport` 生成结构化报告。

### 9.2 推理缓存异常降级

```java
public List<String> getAncestorClasses(String graphId, String classUri) {
    try {
        InfModel infModel = infModelCache.get(graphId);
        if (infModel == null) {
            return fallbackQueryFromDb(graphId, classUri);  // 降级到数据库
        }
        return queryAncestors(infModel, classUri);
    } catch (Exception e) {
        log.warn("推理查询失败，降级到数据库查询", e);
        return fallbackQueryFromDb(graphId, classUri);
    }
}
```

### 9.3 SpEL 表达式异常处理

表达式解析或执行失败时，不阻断验证流程，标记为 `WARNING` 级别错误：

```java
catch (SpelEvaluationException e) {
    return List.of(ValidationErrorVO.builder()
        .level(5).code("ONT005E")
        .message("领域规则表达式执行失败: " + ruleName)
        .build());
}
```

### 9.4 批量验证错误聚合

批量验证时，单个节点/边的异常不中断整体流程：

```java
private ValidationResultVO safeValidate(Supplier<ValidationResultVO> supplier) {
    try {
        return supplier.get();
    } catch (Exception e) {
        return ValidationResultVO.fail(0, List.of(
            ValidationErrorVO.of(0, "SYS001", "验证过程异常: " + e.getMessage(), null, null)
        ));
    }
}
```

---

## 十、前端控制台设计

### 10.1 集成到 OntologyWorkbench

在 `OntologyWorkbench` 标签页系统中新增 **`reasoning-validation`** 类型：

```typescript
type OntologyTabType = 
  | 'class-editor' 
  | 'property-editor' 
  | 'constraint-list'
  | 'definition-editor'
  | 'reasoning-validation'  // 新增
```

### 10.2 面板组成

#### 推理机控制台
- 状态卡片：`warmedUp` / `cold`，显示推理模型内存占用（估算）
- 操作按钮：预热、关闭、刷新缓存
- 最近操作日志

#### 一致性检查面板
- 执行按钮 + 加载状态
- 结果展示：`consistent` 为绿色通过，`inconsistent` 为红色失败
- 不一致详情列表：类型 + 描述 + 涉及类/属性

#### 验证报告面板
- 6 层验证汇总统计：每层通过/失败数量
- 失败详情表格：层级、错误码、字段、值、消息
- 导出 JSON/Excel 按钮

#### 领域规则配置
- 规则列表：名称、表达式、适用类、严重程度、启用状态
- 新建/编辑抽屉：表单包含表达式输入框 + 实时测试按钮
- 测试面板：输入 JSON 属性 → 显示执行结果

#### 图谱完整性检查
- 检查项多选框：孤立节点、必填关系、Domain/Range
- 异步执行：提交后显示任务状态，轮询结果
- 违规详情：节点名称、类型、违规详情

---

## 十一、实施计划建议

### Phase 1：后端核心（1 周）

1. 完善 `OntologyReasonerImpl.warmUp()` 真正加载本体数据
2. 实现 `inferTypes()`、`getPropertyDomains()`、`getPropertyRanges()`
3. 扩展 L4 约束：CARDINALITY、NOT_NULL、CUSTOM
4. 新建 `DomainRuleService` + `ont_domain_rule` 表 + CRUD API
5. 实现 L5 `DomainRuleValidator`

### Phase 2：后端 L6 + 集成（3-4 天）

1. 新建 `GraphIntegrityValidator`，实现 Neo4j Cypher 查询
2. 实现 L6 异步任务框架
3. 集成缓存失效机制到本体写操作
4. 推理缓存优化（Caffeine + 读写锁）

### Phase 3：前端控制台（3-4 天）

1. `OntologyWorkbench` 注册 `reasoning-validation` 标签页
2. 新建 `ReasoningPanel.vue` 主容器
3. 实现推理机控制台子面板
4. 实现一致性检查 + 验证报告子面板
5. 实现领域规则配置子面板（含 SpEL 测试）
6. 实现图谱完整性检查子面板

### Phase 4：联调与测试（2-3 天）

1. 后端单元测试：Reasoner、Validator、DomainRuleService
2. 前端 TypeScript 编译检查
3. 端到端联调
4. 性能测试：推理预热时间、批量验证吞吐量
