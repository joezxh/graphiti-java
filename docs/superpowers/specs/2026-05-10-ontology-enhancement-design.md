# graphiti-java 本体论功能优化设计方案

**日期：** 2026-05-10
**版本：** v1.0
**架构方案：** 应用层本体引擎（Application-layer Ontology Engine）
**元数据存储：** PostgreSQL
**推理引擎：** Apache Jena (OWL 2 RL) + HermiT Reasoner

---

## 一、现状分析

### 1.1 现有本体论功能

| 组件 | 现状 | 问题 |
|------|------|------|
| `OntologyService` | 有接口，5个方法 | 推理能力为零 |
| `OntologyServiceImpl` | validateNode/validateEdge 已实现 | **从未被调用** |
| `OntologyController` | GET + POST 两个端点 | 无版本管理、无导入导出 |
| `OntologyDO` | entities/edges 存为 JSON 字符串 | 无结构化模型，无法做层次推理 |
| MySQL `graphiti_ontology` 表 | 单表，graph_id 唯一索引 | 无类层次、无约束定义 |
| 前端 `ontology.ts` | 使用 mock 数据 | 未接入真实 API |
| Neo4j | 仅存 Entity/Episode 节点 | 无本体节点，推理无图模型支撑 |

### 1.2 本体论理论模型对照

依据 IEEE TKDE 论文 *Ontology Embedding: A Survey* 和 W3C OWL 2 标准，完整本体论应包含以下核心要素：

| 理论要素 | 当前实现 | 缺失程度 |
|----------|----------|----------|
| Classes / Concepts | 仅存 name，无层次 | 🔴 严重缺失 |
| Individuals / Instances | 无直接对应 | 🔴 缺失 |
| Object Properties（对象属性） | edges 存为 JSON，无 domain/range | 🔴 缺失 |
| Data Properties（数据属性） | properties 存为 JSON，无类型层次 | 🟡 部分实现 |
| Class Hierarchy（类层次） | 无 | 🔴 严重缺失 |
| Property Hierarchy（属性层次） | 无 | 🔴 缺失 |
| Domain / Range 约束 | 无 | 🔴 缺失 |
| Cardinality（基数约束） | 无 | 🔴 缺失 |
| Disjointness（不相交约束） | 无 | 🔴 缺失 |
| Equivalent Classes | 无 | 🔴 缺失 |
| SWRL 规则 | 无 | 🟡 可选扩展 |
| 本体版本管理 | 无 | 🔴 缺失 |
| 本体导入/导出 | 无 | 🔴 缺失 |

---

## 二、总体架构

### 2.1 架构图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         Application Layer                                │
│                                                                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌────────────┐  │
│  │  Reasoning   │  │  Validation  │  │   Import/    │  │  Version   │  │
│  │   Engine     │  │   Engine     │  │   Export     │  │  Manager   │  │
│  │ Jena OWL2RL │  │ OWL+SHACL    │  │ Schema.org   │  │            │  │
│  │ +HermiT     │  │ +Custom      │  │ OWL/TTL/RDF │  │            │  │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  └─────┬──────┘  │
│         │                 │                 │                 │         │
│         └─────────────────┼─────────────────┼─────────────────┘         │
│                           ▼                                            │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │              OntologyService (增强版)                               │  │
│  │                                                                   │  │
│  │  - getClassHierarchy()    - validateEntity()                      │  │
│  │  - getPropertyDef()       - validateRelation()                    │  │
│  │  - reasonTypes()          - inferTypes()                          │  │
│  │  - exportOntology()       - importOntology()                      │  │
│  │  - getVersionHistory()    - rollbackOntology()                    │  │
│  └──────────────────────────────┬───────────────────────────────────┘  │
│                                 │                                       │
│          ┌──────────────────────┴──────────────────────┐              │
│          ▼                                              ▼              │
│  ┌───────────────────┐                    ┌───────────────────────────┐ │
│  │   PostgreSQL      │                    │        Neo4j              │ │
│  │  (本体元数据存储)   │                    │   (OWL 图模型 + 推理查询)  │ │
│  │                   │                    │                           │ │
│  │  ont_class        │                    │  (:OntologyClass)         │ │
│  │  ont_property     │                    │  (:OntologyProperty)      │ │
│  │  ont_constraint   │                    │  (:rdf:type rdfs:Class)   │ │
│  │  ont_version      │                    │  (:rdfs:subClassOf)       │ │
│  │  ont_mapping      │                    │  (:rdfs:domain)           │ │
│  │                   │                    │  (:rdfs:range)            │ │
│  │                   │  Sync via         │  (:owl:equivalentClass)   │ │
│  │                   │  OntologySyncService│  (:owl:ObjectProperty)    │ │
│  └───────────────────┘                    └───────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│                     Integration Layer (Consumer)                         │
│                                                                          │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────────────────┐ │
│  │ NodeServiceImpl │  │ EdgeServiceImpl │  │ DataImportServiceImpl    │ │
│  │                 │  │                 │  │                          │ │
│  │ validateNode() ◄┘  │ validateEdge() ◄┘  │ validateImportBatch() ◄┘  │ │
│  │ inferTypes()       │ reasonEdgeType()    │ Episode → Entity Type     │ │
│  │ recommendSchema()  │ recommendEdge()     │  Inference               │ │
│  └─────────────────┘  └─────────────────┘  └──────────────────────────┘ │
│                                                                          │
│  ┌─────────────────┐  ┌─────────────────┐  ┌──────────────────────────┐ │
│  │EpisodeServiceImpl│  │SearchServiceImpl│  │ GraphitiServiceImpl      │ │
│  │ inferEntityType()│  │ reasonSearch()  │  │ validateOnRebuild()      │ │
│  │ enrichEpisode()   │  │ typeExpanding   │  │ getSchemaCompleteness() │ │
│  └─────────────────┘  └─────────────────┘  └──────────────────────────┘ │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.2 核心设计原则

1. **本体优先（Ontology-First）**：图谱操作必须经过本体校验层
2. **双存储互补**：PostgreSQL 负责元数据高性能读写，Neo4j 负责图推理查询
3. **渐进增强**：兼容现有 JSON 格式，逐步迁移到结构化模型
4. **推理分层**：基础校验在应用层完成，高级 OWL 推理按需调用 Jena
5. **版本化管理**：所有本体变更原子化记录，支持回滚

---

## 三、数据模型扩展设计

### 3.1 PostgreSQL 元数据表设计

#### 3.1.1 本体定义主表 `ont_definition`

```sql
CREATE TABLE ont_definition (
    id              BIGSERIAL PRIMARY KEY,
    graph_id        VARCHAR(64) NOT NULL,
    namespace       VARCHAR(255) DEFAULT 'default',
    name            VARCHAR(128) NOT NULL,
    version         VARCHAR(32) NOT NULL DEFAULT '1.0.0',
    status          VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
                    -- DRAFT | ACTIVE | DEPRECATED | ARCHIVED
    description     TEXT,
    parent_version_id BIGINT REFERENCES ont_definition(id),
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      VARCHAR(64),
    UNIQUE (graph_id, namespace, name, version)
);

CREATE INDEX idx_ont_def_graph_id ON ont_definition(graph_id);
CREATE INDEX idx_ont_def_status ON ont_definition(status);
```

#### 3.1.2 类（Class）定义表 `ont_class`

```sql
CREATE TABLE ont_class (
    id              BIGSERIAL PRIMARY KEY,
    definition_id   BIGINT NOT NULL REFERENCES ont_definition(id) ON DELETE CASCADE,
    class_uri       VARCHAR(512) NOT NULL,
    local_name      VARCHAR(128) NOT NULL,
    parent_class_id BIGINT REFERENCES ont_class(id),
    equivalent_to   TEXT[],           -- 等价类 URI 列表
    disjoint_with   BIGINT[],         -- 不相交类 ID 列表
    description     TEXT,
    example         TEXT,
    domain_hint     VARCHAR(64),      -- 业务域标记: FINANCIAL/MEDICAL/ECOMMERCE/KNOWLEDGE
    metadata        JSONB,            -- 扩展元数据: { "icon": "...", "color": "..." }
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (definition_id, class_uri)
);

CREATE INDEX idx_ont_class_def ON ont_class(definition_id);
CREATE INDEX idx_ont_class_parent ON ont_class(parent_class_id);
CREATE INDEX idx_ont_class_domain ON ont_class(domain_hint);
```

#### 3.1.3 属性（Property）定义表 `ont_property`

```sql
CREATE TABLE ont_property (
    id              BIGSERIAL PRIMARY KEY,
    definition_id   BIGINT NOT NULL REFERENCES ont_definition(id) ON DELETE CASCADE,
    property_uri    VARCHAR(512) NOT NULL,
    local_name      VARCHAR(128) NOT NULL,
    property_type   VARCHAR(16) NOT NULL,
                    -- OBJECT: 指向另一个实体
                    -- DATATYPE: 指向字面量值
                    -- ANNOTATION: 注释属性（不可推理）
                    -- TRANSITIVE / SYMMETRIC / FUNCTIONAL / INVERSE_FUNCTIONAL

    domain_class_id BIGINT REFERENCES ont_class(id),  -- 允许出现的类（可为 NULL 表示任意类）
    range_class_id   BIGINT REFERENCES ont_class(id), -- 对象属性指向的类（仅 OBJECT 属性）
    range_data_type VARCHAR(32),    -- 数据类型: string/int/float/boolean/date/json/...
                                     -- 约束来自 XSD + JSON Schema 类型系统

    min_cardinality  INTEGER,
    max_cardinality  INTEGER,
    default_value    TEXT,
    allowed_values   TEXT[],        -- 枚举值列表

    parent_property_id BIGINT REFERENCES ont_property(id),  -- 属性层次
    equivalent_to    TEXT[],        -- 等价属性 URI 列表
    inverse_of_id    BIGINT REFERENCES ont_property(id),    -- 逆属性

    is_required     BOOLEAN NOT NULL DEFAULT FALSE,
    is_multiple      BOOLEAN NOT NULL DEFAULT FALSE,
    pattern          VARCHAR(256),  -- 正则校验 (DATATYPE 属性)
    min_value        NUMERIC,
    max_value        NUMERIC,

    description      TEXT,
    example          TEXT,
    metadata         JSONB,
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (definition_id, property_uri)
);

CREATE INDEX idx_ont_prop_def ON ont_property(definition_id);
CREATE INDEX idx_ont_prop_type ON ont_property(property_type);
CREATE INDEX idx_ont_prop_domain ON ont_property(domain_class_id);
CREATE INDEX idx_ont_prop_range ON ont_property(range_class_id);
CREATE INDEX idx_ont_prop_parent ON ont_property(parent_property_id);
```

#### 3.1.4 约束（Constraint）定义表 `ont_constraint`

```sql
CREATE TABLE ont_constraint (
    id              BIGSERIAL PRIMARY KEY,
    definition_id   BIGINT NOT NULL REFERENCES ont_definition(id) ON DELETE CASCADE,
    class_id        BIGINT REFERENCES ont_class(id),
    property_id     BIGINT REFERENCES ont_property(id),
    constraint_type VARCHAR(32) NOT NULL,
                    -- CARDINALITY / MIN_COUNT / MAX_COUNT / UNIQUE
                    -- PATTERN / RANGE / ENUM / NOT_NULL / CUSTOM_SPARQL

    value           JSONB NOT NULL,  -- { "min": 1, "max": 5 } 等
    error_message   VARCHAR(512),
    severity         VARCHAR(16) NOT NULL DEFAULT 'ERROR',
                    -- ERROR: 阻止创建
                    -- WARNING: 警告但不阻止
                    -- INFO: 提示性约束

    description     TEXT,
    created_at       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (class_id, property_id, constraint_type)
);

CREATE INDEX idx_ont_constraint_def ON ont_constraint(definition_id);
CREATE INDEX idx_ont_constraint_class ON ont_constraint(class_id);
```

#### 3.1.5 本体版本历史表 `ont_version_history`

```sql
CREATE TABLE ont_version_history (
    id              BIGSERIAL PRIMARY KEY,
    definition_id   BIGINT NOT NULL REFERENCES ont_definition(id),
    version         VARCHAR(32) NOT NULL,
    change_type     VARCHAR(16) NOT NULL,
                    -- CREATED / CLASS_ADDED / CLASS_MODIFIED / CLASS_DELETED
                    -- PROPERTY_ADDED / PROPERTY_MODIFIED / PROPERTY_DELETED
                    -- CONSTRAINT_ADDED / CONSTRAINT_MODIFIED / CONSTRAINT_DELETED
                    -- STATUS_CHANGED / IMPORTED

    entity_type     VARCHAR(16) NOT NULL,  -- CLASS / PROPERTY / CONSTRAINT / DEFINITION
    entity_id       BIGINT,
    before_state    JSONB,   -- 修改前的完整 JSON 快照
    after_state     JSONB,   -- 修改后的完整 JSON 快照
    diff_summary    TEXT,
    changed_by      VARCHAR(64),
    changed_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ont_version_def ON ont_version_history(definition_id);
CREATE INDEX idx_ont_version_time ON ont_version_history(changed_at DESC);
```

#### 3.1.6 本体映射（Integration）表 `ont_mapping`

```sql
CREATE TABLE ont_mapping (
    id              BIGSERIAL PRIMARY KEY,
    definition_id   BIGINT NOT NULL REFERENCES ont_definition(id) ON DELETE CASCADE,
    source_ontology VARCHAR(512),  -- 外部本体 URI (e.g., https://schema.org/)
    source_type     VARCHAR(16),   -- SCHEMA_ORG / OBO Foundry / CUSTOM
    mapped_class_uri VARCHAR(512),
    mapping_type    VARCHAR(16),   -- EQUIVALENT / SUPERCLASS / SUBPROPERTY / ...
    confidence      DECIMAL(3,2),  -- 0.00 - 1.00
    metadata        JSONB,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_ont_mapping_def ON ont_mapping(definition_id);
CREATE INDEX idx_ont_mapping_source ON ont_mapping(source_ontology);
```

### 3.2 领域预置类体系

针对金融风控、医疗健康、电商、知识库四大领域，预置 Schema.org 基础类子集：

```
Thing
├── Person ─────────────────────────────────── [医疗: Patient, Doctor; 金融: AccountHolder]
│   ├── Organization ───────────────────────── [金融: Company, Bank, Insurance; 电商: Merchant]
│   │   └── Product ────────────────────────── [电商: Product, Service]
│   │       └── Offer ───────────────────────── [电商: Offer, Price]
│   └── Place
│       └── Organization
├── Event
├── Action
└── Intangible
    ├── FinancialProduct ───────────────────── [金融: Loan, Insurance, Fund, CryptoCurrency]
    ├── MedicalEntity ───────────────────────── [医疗: Drug, Procedure, Condition]
    └── CreativeWork ────────────────────────── [知识库: Article, Book, Dataset]
```

---

## 四、验证机制完善设计

### 4.1 验证分层架构

```
验证请求
    │
    ▼
Layer 1: 类型存在性校验 ──────────────────────────────────────────
│  检查 nodeType/edgeType 是否在本体中定义                         │
│  → 失败: BusinessException(ONT001, "未定义的类型: {type}")       │
    │
    ▼
Layer 2: 属性必填校验 ────────────────────────────────────────────
│  检查 required=true 的属性是否存在且非空                         │
│  → 失败: BusinessException(ONT002, "缺少必需属性: {property}")   │
    │
    ▼
Layer 3: 数据类型校验 ────────────────────────────────────────────
│  string/int/float/boolean/date/json 等基础类型检查               │
│  → 失败: BusinessException(ONT003, "属性 {prop} 类型错误")      │
    │
    ▼
Layer 4: 约束规则校验 ───────────────────────────────────────────
│  Cardinality / Pattern / Enum / Range 等                        │
│  → 失败: BusinessException(ONT004, "违反约束: {detail}")         │
    │
    ▼
Layer 5: OWL 约束校验（按需）─────────────────────────────────────
│  Disjointness / Domain-Range / Equivalent                       │
│  → 失败: BusinessException(ONT005, "OWL 约束冲突: {detail}")   │
    │
    ▼
Layer 6: 推理扩展（按需）────────────────────────────────────────
│  subsumption 推理: 推断隐含类型                                  │
│  → 返回 enriched entity (自动注入 inferred types)               │
    │
    ▼
验证通过 ─→ 写入 Neo4j + 记录本体操作日志
```

### 4.2 验证引擎接口设计

```java
public interface OntologyValidationEngine {
    /**
     * 完整验证流程（6层）
     */
    ValidationResult validateNode(String graphId, String nodeType,
                                   Map<String, Object> properties);

    ValidationResult validateEdge(String graphId, String edgeType,
                                   String sourceType, String targetType,
                                   Map<String, Object> properties);

    /**
     * 批量验证（DataImportService 使用）
     */
    BatchValidationResult validateBatch(String graphId,
                                         List<NodeValidationRequest> nodes,
                                         List<EdgeValidationRequest> edges);

    /**
     * OWL 一致性检查
     */
    ConsistencyResult checkConsistency(String graphId);

    /**
     * 类型推断（Episode → Entity 类型自动推断）
     */
    List<String> inferTypes(String graphId, Episode episode);
}
```

### 4.3 验证结果模型

```java
public record ValidationResult(
    boolean passed,
    int level,               // 1-6，对应6层验证
    List<ValidationError> errors,
    List<ValidationWarning> warnings,
    List<InferredType> inferredTypes,  // Layer 6 推理结果
    Map<String, Object> enrichedProperties  // 注入的默认值/inferred属性
) {}

public record ValidationError(
    int layer,
    String code,             // ONT001 - ONT005
    String message,
    String property,
    Object attemptedValue
) {}

public record ValidationWarning(
    int layer,
    String message,
    String suggestion
) {}

public record InferredType(
    String type,
    String reason,           // "subClassOf(Person, Organization)"
    double confidence
) {}
```

### 4.4 集成点设计

#### 4.4.1 NodeServiceImpl 集成

```java
// 在 createNode() 方法中插入验证：
@Override
public Node createNode(String graphId, String nodeType,
                        Map<String, Object> properties) {
    // 1. 本体验证
    ValidationResult vr = validationEngine.validateNode(graphId, nodeType, properties);
    if (!vr.passed()) {
        throw new OntologyViolationException(vr.errors());
    }

    // 2. 记录本体操作（用于图谱血缘追踪）
    ontologyAuditService.logNodeCreation(graphId, nodeType, properties, vr);

    // 3. 写入 Neo4j（使用 enrichedProperties）
    Map<String, Object> finalProps = vr.enrichedProperties() != null
        ? vr.enrichedProperties() : properties;
    Node node = graphNeo4jService.createNode(graphId, nodeType, finalProps);

    // 4. 自动标签注入：inferredTypes 作为额外标签
    if (!vr.inferredTypes().isEmpty()) {
        graphNeo4jService.addLabels(node.getUuid(),
            vr.inferredTypes().stream().map(InferredType::type).toList());
    }

    return node;
}
```

#### 4.4.2 EdgeServiceImpl 集成

```java
// 在 createEdge() 中，额外校验 source/target 节点类型与边定义的 domain/range 一致性
@Override
public Edge createEdge(String graphId, String edgeType,
                        String sourceUuid, String targetUuid,
                        Map<String, Object> properties) {
    // 1. 边类型定义获取
    PropertyDef edgeDef = ontologyService.getPropertyDef(graphId, edgeType);

    // 2. 校验 source/target 节点类型是否匹配 domain/range
    ValidationResult vr = validationEngine.validateEdge(graphId, edgeType,
        sourceNodeType, targetNodeType, properties);
    if (!vr.passed()) {
        throw new OntologyViolationException(vr.errors());
    }

    // 3. 如果边定义了 inverse_of，自动创建逆向边
    if (edgeDef.inverseOf() != null) {
        graphNeo4jService.createEdge(graphId, edgeDef.inverseOf(),
            targetUuid, sourceUuid, Map.of());  // 逆向边空属性
    }

    // 4. 写入边
    return graphNeo4jService.createEdge(graphId, edgeType, sourceUuid, targetUuid, properties);
}
```

#### 4.4.3 DataImportServiceImpl 集成

```java
// 在 addFactTriple() 和批量导入流程中插入验证
@Override
public ImportReport importData(String graphId, ImportRequest request) {
    BatchValidationResult bvr = validationEngine.validateBatch(graphId,
        request.nodes(), request.edges());

    // 收集验证失败的项，但不整体失败
    List<NodeValidationRequest> validNodes = bvr.validNodes();
    List<EdgeValidationRequest> validEdges = bvr.validEdges();

    // 对失败项的处理策略：取决于 request.onViolation()
    // STRICT: 拒绝整批导入
    // LAX: 仅导入有效项
    // CORRECT: 自动修正后导入

    if (request.onViolation() == ViolationStrategy.STRICT && !bvr.allPassed()) {
        throw new OntologyImportException(bvr.summary());
    }

    // Episode 自动类型推断
    for (Episode ep : request.episodes()) {
        List<String> inferredTypes = validationEngine.inferTypes(graphId, ep);
        ep.setInferredEntityTypes(inferredTypes);
    }

    // 批量写入
    return writeToGraph(graphId, validNodes, validEdges, request.episodes());
}
```

---

## 五、推理引擎设计

### 5.1 推理引擎架构

```java
/**
 * 基于 Apache Jena 的 OWL 2 RL 推理引擎
 * 推理策略：Eager（预计算）+ Lazy（按需）混合
 */
public interface OntologyReasoner {
    /**
     * 预热推理机：加载本体到 TDB 图
     * 在 GraphitiService.initGraph() 时调用
     */
    void warmUp(String graphId, OntologyDefinition ontology);

    /**
     * 释放推理机资源
     */
    void shutdown(String graphId);

    /**
     * 类层次推理：获取一个类的所有祖先类
     * "CEO" → [Person, Thing]
     */
    List<String> getAncestorClasses(String graphId, String classUri);

    /**
     * 类层次推理：获取一个类的所有后代类
     * "Person" → [Person, Employee, CEO, Doctor, ...]
     */
    List<String> getDescendantClasses(String graphId, String classUri);

    /**
     * 类型推断：给定一个实体的属性集合，推断其可能的类型
     * Episode.content → 推断最可能的 Entity Type
     */
    List<InferredType> inferTypes(String graphId, Map<String, Object> properties);

    /**
     * 属性 domain 推导：给定一个属性，列出所有可出现的类
     */
    List<String> getPropertyDomains(String graphId, String propertyUri);

    /**
     * 属性 range 推导：给定一个属性，列出其值的所有可能类型
     */
    List<String> getPropertyRanges(String graphId, String propertyUri);

    /**
     * 一致性检查：检查本体是否存在逻辑矛盾
     */
    ConsistencyResult checkConsistency(String graphId);

    /**
     * 可满足性检查：检查某个类是否可以有实例
     */
    boolean isSatisfiable(String graphId, String classUri);

    /**
     * SPARQL-DL 查询（可选，高级推理查询）
     */
    Model querySPARQLDL(String graphId, String sparqlDlQuery);
}
```

### 5.2 Episode → Entity 类型自动推断

```java
public class EpisodeTypeInferenceEngine {
    /**
     * 基于 LLM + 本体约束的 Episode 类型推断
     *
     * 输入: Episode.content (一段文本或结构化数据)
     * 输出: 推断出的 Entity Types (带置信度)
     *
     * 推断策略:
     * 1. LLM 提取关键词和实体
     * 2. 与 ont_class 表中的 class_uri / local_name / description 匹配
     * 3. 基于类层次结构扩展候选类型
     * 4. 基于 domain_hint (FINANCIAL/MEDICAL/ECOMMERCE/KNOWLEDGE) 过滤
     * 5. 按置信度排序返回
     */
    public List<InferredType> inferEntityTypes(Episode episode, String domainHint);
}
```

### 5.3 与 Jena 的集成

```
PostgreSQL (元数据)
       │ OntologySyncService.syncToNeo4j()
       ▼
Neo4j (:OntologyClass, :OntologyProperty, :rdf:type rdfs:Class ...)
       │
       │ Jena TDBLoader.load()
       ▼
Apache Jena TDB (In-Memory OWL Model)
       │
       │ OWL_reasoner_factory.createReifiedOntology()
       ▼
Jena OWL 2 RL Model (包含 HermiT / Pellet 推理机)
       │
       ├── getAncestorClasses() → InfGraph.getPredecessors(rdfs:subClassOf)
       ├── getDescendantClasses() → InfGraph.getSuccessors(rdfs:subClassOf)
       ├── inferTypes() → InfGraph.listStatements(s, rdf:type, ?type)
       └── checkConsistency() → reasoner.validate(model)
```

**依赖引入：**

```xml
<!-- Apache Jena Core -->
<dependency>
    <groupId>org.apache.jena</groupId>
    <artifactId>apache-jena-libs</artifactId>
    <version>4.9.0</version>
    <type>pom</type>
</dependency>

<!-- OWL Reasoner -->
<dependency>
    <groupId>org.apache.jena</groupId>
    <artifactId>jena-owlapi</artifactId>
    <version>4.9.0</version>
</dependency>

<!-- HermiT Reasoner -->
<dependency>
    <groupId>org.semanticweb.hermit</groupId>
    <artifactId>hermit-reasoner</artifactId>
    <version>1.4.5</version>
</dependency>
```

---

## 六、API 增强设计

### 6.1 新增 Controller 端点

#### 6.1.1 OntologyController 增强

| 方法 | 端点 | 描述 |
|------|------|------|
| GET | `/api/v1/ontology/{graphId}` | 获取本体定义 |
| POST | `/api/v1/ontology/{graphId}` | 创建/设置本体 |
| PUT | `/api/v1/ontology/{graphId}` | 更新本体（创建新版本） |
| DELETE | `/api/v1/ontology/{graphId}` | 删除本体（软删除） |
| GET | `/api/v1/ontology/{graphId}/classes` | 获取类层次树 |
| GET | `/api/v1/ontology/{graphId}/classes/{classUri}` | 获取单个类详情 |
| POST | `/api/v1/ontology/{graphId}/classes` | 新增类定义 |
| PUT | `/api/v1/ontology/{graphId}/classes/{classUri}` | 更新类定义 |
| DELETE | `/api/v1/ontology/{graphId}/classes/{classUri}` | 删除类定义 |
| GET | `/api/v1/ontology/{graphId}/properties` | 获取属性列表 |
| GET | `/api/v1/ontology/{graphId}/properties/{propertyUri}` | 获取单个属性详情 |
| POST | `/api/v1/ontology/{graphId}/properties` | 新增属性定义 |
| PUT | `/api/v1/ontology/{graphId}/properties/{propertyUri}` | 更新属性定义 |
| DELETE | `/api/v1/ontology/{graphId}/properties/{propertyUri}` | 删除属性定义 |
| POST | `/api/v1/ontology/{graphId}/validate/node` | 验证节点 |
| POST | `/api/v1/ontology/{graphId}/validate/edge` | 验证边 |
| POST | `/api/v1/ontology/{graphId}/validate/batch` | 批量验证 |
| GET | `/api/v1/ontology/{graphId}/versions` | 获取版本历史 |
| POST | `/api/v1/ontology/{graphId}/versions/{version}/rollback` | 回滚到指定版本 |
| GET | `/api/v1/ontology/{graphId}/consistency` | OWL 一致性检查 |
| POST | `/api/v1/ontology/{graphId}/reason/infer-types` | 推理类型 |
| GET | `/api/v1/ontology/{graphId}/graph/reasoners` | 获取推理机状态 |

#### 6.1.2 ImportExportController（新增）

| 方法 | 端点 | 描述 |
|------|------|------|
| POST | `/api/v1/ontology/{graphId}/import` | 导入本体（RDF/TTL/OWL/JSON-LD） |
| GET | `/api/v1/ontology/{graphId}/export` | 导出本体 |
| POST | `/api/v1/ontology/{graphId}/import/schema-org` | 从 Schema.org 导入 |
| POST | `/api/v1/ontology/{graphId}/import/mapping` | 导入本体映射规则 |
| GET | `/api/v1/ontology/{graphId}/mappings` | 获取本体映射列表 |
| POST | `/api/v1/ontology/{graphId}/align` | 自动本体对齐（基于 LLM） |

**导出格式支持：** JSON-LD（默认）、RDF/TTL、OWL/XML、NTriples、Turtle

**Schema.org 导入参数：**

```json
{
  "domains": ["FinancialProduct", "MedicalEntity", "Product", "Article"],
  "language": "zh-CN",
  "includeInferred": false,
  "hierarchyDepth": 3
}
```

### 6.2 VO 数据模型

```java
// Class 定义请求/响应
public record ClassDefVO(
    String classUri,
    String localName,
    String parentClassUri,
    List<String> equivalentTo,
    List<String> disjointWith,
    String description,
    String example,
    String domainHint,       // FINANCIAL / MEDICAL / ECOMMERCE / KNOWLEDGE
    List<PropertyRefVO> inheritedProperties,
    Map<String, Object> metadata
) {}

// Property 定义请求/响应
public record PropertyDefVO(
    String propertyUri,
    String localName,
    PropertyType propertyType,  // OBJECT / DATATYPE / ANNOTATION
    String domainClassUri,
    String rangeClassUri,
    String rangeDataType,
    Integer minCardinality,
    Integer maxCardinality,
    boolean isRequired,
    boolean isMultiple,
    String inverseOfUri,
    String pattern,
    String defaultValue,
    List<String> allowedValues,
    String description,
    Map<String, Object> metadata
) {}

// 版本历史记录
public record VersionHistoryVO(
    Long id,
    String version,
    String changeType,
    String entityType,
    Long entityId,
    String entityName,
    String diffSummary,
    String beforeJson,
    String afterJson,
    String changedBy,
    LocalDateTime changedAt
) {}
```

---

## 七、与图谱其他功能的集成优化

### 7.1 Episode → Entity 类型自动推断

```
用户导入 Episode
       │
       ▼
EpisodeTypeInferenceEngine.inferEntityTypes()
       │
       ├── 步骤1: LLM 提取 Episode 中的实体关键词
       │      Prompt: "从以下内容中提取关键实体类型: {episode.content}"
       │
       ├── 步骤2: 在 ont_class 表中模糊匹配 class_uri / local_name / description
       │      SQL: SELECT * FROM ont_class
       │           WHERE definition_id = ? AND (
       │             local_name % keyword OR
       │             description ILIKE '%' || keyword || '%'
       │           )
       │
       ├── 步骤3: 基于类层次扩展候选类型
       │      reasoner.getAncestorClasses(classUri)  // 上行
       │      reasoner.getDescendantClasses(classUri)  // 下行
       │
       ├── 步骤4: 按 domain_hint 过滤（FINANCIAL/MEDICAL/ECOMMERCE/KNOWLEDGE）
       │
       └── 步骤5: 计算置信度，返回 Top-N 类型
                [{type: "FinancialProduct", confidence: 0.85},
                 {type: "BankAccount", confidence: 0.72}]
       │
       ▼
Episode.enrich(inferredTypes) → 自动注入 Entity Types
       │
       ▼
NodeServiceImpl.createNode() 使用 enriched types 创建节点
```

### 7.2 图谱重建时的本体校验

```java
// GraphitiServiceImpl.rebuildGraph() 中集成
public void rebuildGraph(String graphId, RebuildRequest request) {
    // 1. 检查本体是否存在
    OntologyDefinition ont = ontologyService.getActiveOntology(graphId)
        .orElseThrow(() -> new OntologyNotFoundException(graphId));

    // 2. OWL 一致性检查
    ConsistencyResult cr = reasoner.checkConsistency(graphId);
    if (!cr.isConsistent()) {
        log.warn("图谱 {} 的本体存在逻辑矛盾: {}", graphId, cr.inconsistencies());
        // 策略: WARN（警告但不阻止）或 STRICT（阻止重建）
        if (request.consistencyStrategy() == ConsistencyStrategy.STRICT) {
            throw new OntologyInconsistencyException(cr.summary());
        }
    }

    // 3. 图谱完整性报告
    SchemaCompletenessReport report = analyzeSchemaCompleteness(graphId);
    report.orphanedNodes();      // 无类型定义的节点
    report.orphanedEdges();       // 无类型定义的边
    report.unusedClasses();       // 定义了但从未使用的类

    // 4. 根据策略处理
    switch (request.orphanStrategy()) {
        case DELETE -> deleteOrphanedEntities(graphId, report);
        case PROMOTE_TO_GENERIC -> promoteToGenericType(graphId, report);
        case KEEP -> { /* 仅记录，不处理 */ }
    }

    // 5. 执行重建
    doRebuild(graphId, request);
}
```

### 7.3 搜索服务推理增强

```java
// SearchServiceImpl 中的推理增强
public SearchResult search(String graphId, SearchQuery query) {
    // 1. 基础向量检索
    List<EntityNode> candidates = vectorSearch(graphId, query);

    // 2. 类型扩展推理（可选）
    if (query.expandTypes()) {
        Set<String> expandedTypes = new HashSet<>();
        for (String requestedType : query.targetTypes()) {
            expandedTypes.add(requestedType);
            // 包含该类型及其所有后代类
            expandedTypes.addAll(reasoner.getDescendantClasses(graphId, requestedType));
        }
        candidates = candidates.stream()
            .filter(e -> expandedTypes.contains(e.getType()))
            .toList();
    }

    // 3. 属性路径推理（可选）
    if (query.usePropertyPaths()) {
        candidates = expandViaPropertyPaths(graphId, candidates, query.propertyPath());
    }

    return buildSearchResult(candidates, query);
}
```

---

## 八、分阶段实现路径

### 阶段 1：Schema Enforcement 基础（4 周）

**目标：** 让本体真正作用于图谱操作

| 任务 | 描述 | 依赖 |
|------|------|------|
| 1.1 | PostgreSQL 表结构迁移（6 张表） | 无 |
| 1.2 | 数据迁移脚本：JSON → 结构化表 | 1.1 |
| 1.3 | 重构 OntologyService + ValidationEngine | 1.2 |
| 1.4 | NodeServiceImpl 集成验证 | 1.3 |
| 1.5 | EdgeServiceImpl 集成验证 | 1.3 |
| 1.6 | DataImportServiceImpl 集成 + 批量验证 | 1.4, 1.5 |
| 1.7 | 前端 ontology.ts 接入真实 API | 1.4, 1.5 |
| 1.8 | E2E 测试 + 验证报告 | 1.6, 1.7 |

**交付物：** 本体驱动的图谱操作，前端可创建/编辑本体类型

### 阶段 2：本体建模能力（4 周）

**目标：** 完整的本体建模工具链

| 任务 | 描述 | 依赖 |
|------|------|------|
| 2.1 | OntologyClassService（类 CRUD + 层次管理） | 1.1 |
| 2.2 | OntologyPropertyService（属性 CRUD + 层次管理） | 1.1 |
| 2.3 | OntologyConstraintService（约束 CRUD） | 2.1, 2.2 |
| 2.4 | 本体版本管理（ont_version_history） | 2.1, 2.2, 2.3 |
| 2.5 | 领域预置类导入（Schema.org 子集） | 2.1 |
| 2.6 | 前端本体编辑器 UI 完善 | 2.1, 2.2, 2.3 |
| 2.7 | 导入/导出 API（RDF/TTL/JSON-LD） | 2.1 |
| 2.8 | 一致性检查 API | 2.1, 2.2, 2.3 |

**交付物：** 完整的本体建模工作台，支持版本管理

### 阶段 3：推理引擎集成（4 周）

**目标：** 让图谱具有智能推理能力

| 任务 | 描述 | 依赖 |
|------|------|------|
| 3.1 | 引入 Jena 依赖，搭建推理环境 | 无 |
| 3.2 | OntologySyncService（MySQL → Neo4j 同步） | 2.1 |
| 3.3 | OntologyReasoner 实现类 | 3.1, 3.2 |
| 3.4 | Episode → Entity 类型自动推断 | 3.3 |
| 3.5 | 推理增强的搜索服务 | 3.3 |
| 3.6 | 图谱重建时的本体一致性校验 | 3.3 |
| 3.7 | SWRL 规则引擎（可选，高级） | 3.3 |
| 3.8 | 推理性能基准测试 + 优化 | 3.3, 3.7 |

**交付物：** 支持类层次推理、类型推断、一致性检查的知识图谱

### 阶段 4：本体集成与生态（3 周）

**目标：** 外部本体生态接入，完整的数据集成能力

| 任务 | 描述 | 依赖 |
|------|------|------|
| 4.1 | Schema.org 导入器（四大领域） | 2.5 |
| 4.2 | 本体映射管理（ont_mapping 表） | 阶段 2 |
| 4.3 | LLM 辅助本体对齐（ontology alignment） | 4.2 |
| 4.4 | 异构数据源本体映射管道 | 4.1, 4.2, 4.3 |
| 4.5 | 本体嵌入（Ontology Embedding） | 阶段 3 |
| 4.6 | 性能与稳定性全链路测试 | 4.1-4.5 |
| 4.7 | 文档与 API Reference | 全部 |

**交付物：** 完整的企业级本体管理系统

---

## 九、关键技术决策

| 决策项 | 选择 | 理由 |
|--------|------|------|
| 元数据存储 | PostgreSQL | 现有项目已集成，支持 JSONB/数组/全文检索 |
| OWL 推理引擎 | Jena OWL 2 RL + HermiT | 团队有经验，完整 OWL 2 支持 |
| 图推理存储 | Neo4j | 现有项目已集成，天然适合 RDF 图模型 |
| 推理时机 | Eager（预热）+ Lazy（按需）混合 | 避免冷启动延迟，也不在无查询时浪费资源 |
| 本体同步 | 应用层双写 + 异步同步 | 保证 PostgreSQL 为主，Neo4j 最终一致 |
| 类型推断 | LLM + 本体约束混合 | LLM 负责语义理解，本体负责结构约束 |
| 数据迁移 | JSON → 结构化分阶段迁移 | 兼容现有 JSON 格式，渐进迁移 |

---

## 十、已知风险与缓解措施

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| Jena 推理在大图谱上性能差 | 高 | Lazy 加载 + TDB 缓存 + 按 graphId 隔离推理实例 |
| PostgreSQL → Neo4j 同步延迟导致推理不一致 | 中 | 同步写 + 事件驱动增量更新 |
| 本体验证影响导入性能 | 中 | 异步验证 + 批量验证缓存 |
| OWL 约束配置复杂，用户门槛高 | 中 | 提供"简单模式"（仅 name+properties）和"专家模式"（完整 OWL） |
| Schema.org 导入类过多影响推理效率 | 低 | 按 domain_hint 过滤，仅导入相关子类 |

---

*本设计方案遵循 Ontology-First 原则，通过分阶段实现路径，确保每阶段都有可验证的产出价值。*
