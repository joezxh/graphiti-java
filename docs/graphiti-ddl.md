## Graphiti-Java 本体概念完整整理

根据代码库探索，我为你整理出完整的本体概念层级关系：

---

### 📊 核心概念层级总览

```
Service 层 (业务入口)
│
├── NodeService / NodeServiceImpl → Entity (实体/节点)
├── EdgeService / EdgeServiceImpl → Edge (关系/边)
├── EpisodeService / EpisodeServiceImpl → Episode (事件/片段)
├── CommunityService / CommunityServiceImpl → Community (社区)
├── GraphitiService / GraphitiServiceImpl → GraphMetadata (图谱元数据)
├── OntologyClassService / OntologyClassServiceImpl → Ontology (本体)
│   ├── OntologyDefinition (版本化本体容器)
│   ├── OntClass (类定义)
│   ├── OntProperty (属性定义)
│   ├── OntConstraint (约束定义)
│   ├── OntDraft (草稿)
│   └── OntVersionHistory (版本历史)
└── SearchService / SearchServiceImpl → Search (检索)
```

---

### 🏗️ 第0层: 顶层容器

| 概念 | 描述 | 核心属性 |
|------|------|---------|
| **GraphMetadata** | 知识图谱实例，顶层容器 | graphId, name, description, nodeCount, edgeCount, ontVersionId |

---

### 🗂️ 第1层: 本体容器层

| 概念 | 描述 | 核心属性 |
|------|------|---------|
| **OntologyDefinition** | 版本化本体定义容器 | definitionId, namespace, version, status (DRAFT/ACTIVE/DEPRECATED/ARCHIVED) |
| **OntClass** | 本体类定义，等价于OWL Class | classUri, localName, parentClassId, equivalentTo, disjointWith |
| **OntProperty** | 本体属性定义，等价于OWL Property | propertyUri, propertyType, domainClassId, rangeClassId |
| **OntConstraint** | 本体约束规则 | constraintType, value, severity |

---

### 📦 第2层: 本体元素定义层

#### OntClass 继承关系
```
Thing (顶层类)
  └── Person (人)
        └── SoftwareEngineer (软件工程师)
        └── Customer (客户)
```

#### OntProperty 属性类型
| 类型 | 说明 |
|------|------|
| OBJECT | 对象属性，关联另一个类 |
| DATATYPE | 数据类型属性，关联值 |
| ANNOTATION | 注解属性 |
| TRANSITIVE | 传递属性 |
| SYMMETRIC | 对称属性 |
| FUNCTIONAL | 函数属性 |

#### OntConstraint 约束类型
| 类型 | 说明 |
|------|------|
| CARDINALITY | 数量约束 |
| PATTERN | 正则表达式 |
| RANGE | 数值范围 |
| ENUM | 枚举值 |
| NOT_NULL | 非空约束 |
| CUSTOM_SPARQL | 自定义SPARQL |
| UNIQUE | 唯一性 |
| LENGTH | 长度约束 |

---

### 🔗 第3层: 实例数据层

| 概念 | 存储位置 | 核心属性 |
|------|---------|---------|
| **Entity** | Neo4j | uuid, name, type, properties, summary, embedding |
| **Edge** | Neo4j | uuid, source, target, type, fact, episodes |
| **Episode** | Neo4j | uuid, name, source, content, entityEdges |
| **Community** | Neo4j | uuid, name, nodeCount |

---

### 🗄️ 存储架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        MySQL (元数据)                            │
├─────────────────────────────────────────────────────────────────┤
│  graphiti_graph_metadata   │ 图谱元数据                           │
│  ont_definition           │ 本体定义                              │
│  ont_class                │ 类定义                               │
│  ont_property             │ 属性定义                              │
│  ont_constraint           │ 约束定义                              │
│  ont_version_history      │ 版本历史                              │
│  ont_draft                │ 草稿                                 │
│  ont_mapping              │ 映射                                 │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                        Neo4j (图数据)                            │
├─────────────────────────────────────────────────────────────────┤
│  Entity 节点 (type -> ontClassId)                               │
│  Episode 节点 (原始数据容器)                                     │
│  Community 节点 (社区检测结果)                                    │
│  RELATES_TO 边 (fact -> embedding)                              │
└─────────────────────────────────────────────────────────────────┘
```

---

### 📁 关键文件索引

**核心DO/VO:**
- `vo/ontology/OntDefinitionVO.java` - 本体定义
- `vo/ontology/OntClassVO.java` - 类定义
- `vo/ontology/OntPropertyVO.java` - 属性定义
- `vo/ontology/OntConstraintVO.java` - 约束定义
- `dal/dataobject/ont/OntDefinitionDO.java` - 本体定义DO
- `dal/dataobject/ont/OntClassDO.java` - 类定义DO

**核心服务:**
- `service/OntologyClassService.java` - 本体类服务
- `service/OntologyPropertyService.java` - 本体属性服务
- `service/OntologyValidationService.java` - 本体验证服务
- `service/OntologyReasoner.java` - 本体推理服务



## Neo4j 在 graphiti-java 中的作用与存储结构

### 一、Neo4j 的核心作用

graphiti-java 采用**双存储架构**，Neo4j 负责以下核心职能：

| 职责 | 说明 |
|------|------|
| **图遍历与关联查询** | 利用 Cypher 进行多跳路径查询、BFS 遍历、社区发现等，这是关系数据库无法高效完成的工作 |
| **语义向量检索** | 通过 `db.index.vector` 索引对 `Entity.embedding` 和 `RELATES_TO.embedding` 做余弦相似度搜索 |
| **全文关键词检索** | 通过全文索引对 `name`、`summary`、`fact` 等文本字段做 `CONTAINS` 匹配 |
| **原始事件容器** | Episode 节点存储未经 LLM 抽取的原始文本/消息等非结构化数据 |
| **多跳路径发现** | 利用 Cypher 的路径表达式发现实体间的间接关联关系 |

**Neo4j 不负责的事**（由 MySQL 承担）：
- 本体定义（`ont_class`、`ont_property`、`ont_constraint` 等）
- 图谱元数据（`graphiti_graph_metadata`）
- 版本历史与审计

---

### 二、Neo4j 自身核心概念

#### 节点标签（Labels）

| 标签 | 含义 | 典型属性 |
|------|------|---------|
| `:Entity` | 实体/概念节点 | `group_id`, `uuid`, `name`, `type`, `summary`, `embedding`, `valid_at`, `invalid_at` |
| `:Episode` | 原始事件/片段节点 | `group_id`, `uuid`, `name`, `source`, `content`, `created_at`, `valid_at`, `processed` |
| `:Community` | 社区发现结果节点 | `group_id`, `uuid`, `name`, `nodeCount` |
| `:Case` / `:Court` / `:Party` 等 | 法律领域扩展标签 | 在 `V7__seed_legal_neo4j_data.sql` 中定义，与 `:Entity` 并存 |

#### 关系类型（Relationship Types）

| 类型 | 连接方向 | 含义 | 核心属性 |
|------|---------|------|---------|
| `RELATES_TO` | `Entity → Entity` | 实体间的语义关联（事实三元组） | `uuid`, `type`, `fact`, `embedding`, `valid_at`, `invalid_at`, `group_id` |
| `MENTIONS` | `Episode → Entity` | Episode 提及了哪些实体 | `uuid`, `group_id` |
| `IN_COMMUNITY` | `Entity → Community` | 实体归属哪个社区 | `uuid`, `group_id` |
| `CASE_PARTY` / `CASE_COURT` / `CASE_LEGAL_PROVISION` 等 | 法律领域扩展关系 | 在 `V7__seed_legal_neo4j_data.sql` 中定义 | 各自携带业务属性 |

#### 索引体系

```cypher
// 唯一性约束
CREATE CONSTRAINT entity_uuid IF NOT EXISTS FOR (n:Entity) REQUIRE n.uuid IS UNIQUE;
CREATE CONSTRAINT episode_uuid IF NOT EXISTS FOR (n:Episode) REQUIRE n.uuid IS UNIQUE;

// 属性索引
CREATE INDEX entity_group_id IF NOT EXISTS FOR (n:Entity) ON (n.group_id);
CREATE INDEX entity_name IF NOT EXISTS FOR (n:Entity) ON (n.name);
CREATE INDEX relation_type IF NOT EXISTS FOR ()-[r:RELATES_TO]-() ON (r.type);

// 全文索引
CREATE FULLTEXT INDEX entity_search IF NOT EXISTS FOR (n:Entity) ON EACH [n.name, n.summary];
CREATE FULLTEXT INDEX nodeNameIndex IF NOT EXISTS FOR (n:Entity) ON EACH [n.name, n.summary];
CREATE FULLTEXT INDEX edgeFactIndex IF NOT EXISTS FOR ()-[r:RELATES_TO]-() ON EACH [r.fact];

// 向量索引
CREATE VECTOR INDEX node_embedding_index IF NOT EXISTS
  FOR (n:Entity) ON (n.embedding)
  OPTIONS {indexConfig: {`vector.dimensions`: 1536, `vector.similarity_function`: 'cosine'}};

CREATE VECTOR INDEX edge_embedding_index IF NOT EXISTS
  FOR ()-[r:RELATES_TO]-() ON (r.embedding)
  OPTIONS {indexConfig: {`vector.dimensions`: 1536, `vector.similarity_function`: 'cosine'}};
```

#### Bi-temporal 时间模型

```
valid_at:   记录生效时间戳（创建时 = timestamp()，毫秒）
invalid_at: 记录失效时间戳（null = 当前有效）
```

- **查询当前有效**：`WHERE invalid_at IS NULL`
- **查询时间点 T 的快照**：`WHERE valid_at <= T AND (invalid_at > T OR invalid_at IS NULL)`
- **更新策略**：旧记录设 `invalid_at`，新记录设新 `valid_at`（不覆盖旧记录，实现历史追溯）

---

### 三、本体定义与存储方式对照

#### MySQL 存储本体定义（Schema 层）

```
ont_definition  ──── 一组版本的容器（namespace + version + status）
    │
    ├── ont_class  ───── 类定义（classUri → Neo4j type 字段的来源）
    │       ├── parentClassId  ──── 继承关系（单继承树）
    │       ├── equivalentTo     ──── OWL equivalentTo
    │       └── disjointWith     ──── OWL disjointWith
    │
    ├── ont_property  ──── 属性定义
    │       ├── propertyType     ──── OBJECT / DATATYPE / ANNOTATION
    │       ├── domainClassId    ──── 主语类
    │       ├── rangeClassId     ──── 宾语类（OBJECT 时）
    │       ├── rangeDataType   ──── 数据类型（DATATYPE 时）
    │       ├── isRequired       ──── 是否必填
    │       └── cardinality      ──── 数量约束
    │
    ├── ont_constraint  ──── 约束规则
    │       └── constraintType: CARDINALITY / PATTERN / RANGE / ENUM / NOT_NULL
    │
    └── ont_version_history  ──── 版本变更记录
```

**MySQL 本体表的字段映射到 `OntClassDO` / `OntPropertyDO` / `OntConstraintDO`**。

#### Neo4j 存储实例数据（Data 层）

```
同一 graphId 下的 Neo4j 数据：
  :Entity 节点
    ├── type = "Person"  ←─── 对应 ont_class.local_name
    ├── name = "Alice"
    └── embedding = [0.1, 0.2, ...]  ←─── 由 EmbedderService 生成

  (Entity)-[:RELATES_TO {type: "WORKS_AT", fact: "Alice works at Google", embedding: [...]}]->(Entity)

  :Episode 节点
    └── content = 原始文本  ←─── 未经 LLM 抽取的原始数据
```

---

### 四、存储方式与结构的完整对照表

#### 通用图谱数据（`init.cypher` 体系）

| 概念 | MySQL 存储 | Neo4j 存储 |
|------|-----------|-----------|
| 图谱顶层容器 | `graphiti_graph_metadata`（graphId, name, nodeCount, edgeCount, ontVersionId） | — |
| 实体类型定义 | `ont_class`（classUri, localName, parentClassId） | — |
| 属性定义 | `ont_property`（propertyUri, propertyType, domainClassId, rangeClassId） | — |
| 约束规则 | `ont_constraint`（constraintType, constraintValue） | — |
| **实体节点实例** | — | `:Entity {groupId, uuid, name, type, summary, embedding, validAt, invalidAt}` |
| **关系边实例** | — | `RELATES_TO {uuid, type, fact, embedding, validAt, invalidAt, groupId}` |
| **事件/片段** | — | `:Episode {groupId, uuid, name, source, content, createdAt, validAt, processed}` |
| **社区节点** | — | `:Community {groupId, uuid, name, nodeCount}` |
| **提及关系** | — | `MENTIONS {uuid, groupId}` |
| **社区成员关系** | — | `IN_COMMUNITY {uuid, groupId}` |

#### 法律知识图谱数据（`V7__seed_legal_neo4j_data.sql` 体系）

法律领域扩展了额外的标签和关系类型，不再受 `:Entity` / `:RELATES_TO` 的通用框架限制，直接用具体标签和关系名：

| 节点标签 | 说明 | 关键属性 |
|---------|------|---------|
| `:Court` | 法院节点 | `courtName`, `courtLevel`, `location`, `jurisdiction`, `parentCourt` |
| `:LegalProvision` | 法律条文节点 | `provisionId`, `articleNumber`, `lawName`, `provisionContent`, `effectiveDate` |
| `:Case` | 案件节点 | `caseNumber`, `caseName`, `caseType`, `caseStatus`, `courtLevel`, `disputeType` |
| `:Party` | 当事人节点 | `partyName`, `partyType`, `partyRole`, `isEnterprise` |
| `:JudgmentDocument` | 裁判文书节点 | `documentNumber`, `documentType`, `judgmentResult`, `legalBasis` |
| `:Judge` | 法官节点 | `judgeName`, `judgeTitle`, `courtName`, `specialty` |
| `:Evidence` | 证据节点 | `evidenceNumber`, `evidenceType`, `content`, `admissibility` |
| `:CaseReasoning` | 裁判要旨节点 | `reasoning`, `guidanceLevel`, `applicableScenario` |
| `:CaseFact` | 案件事实节点 | `factDescription`, `factCategory`, `factImportance` |
| `:CommercialMediationOrganization` | 调解组织 | `name`, `orgType`, `location`, `licenseNumber`, `mediatorCount` |
| `:Mediator` | 调解员节点 | `name`, `qualification`, `licenseNumber`, `specialty` |
| `:MediationAgreement` | 调解协议节点 | `agreementNumber`, `disputeItems`, `agreementContent`, `performanceDeadline` |

| 关系类型 | 连接 | 说明 | 携带属性 |
|---------|------|------|---------|
| `CASE_PARTY` | Case → Party | 案件与当事人 | `role`, `representationType`, `fact` |
| `CASE_COURT` | Case → Court | 案件与审理法院 | `courtRole`, `jurisdictionBasis`, `fact` |
| `CASE_JUDGE` | Case → Judge | 案件与法官 | `role`, `caseLevel` |
| `CASE_LEGAL_PROVISION` | Case → LegalProvision | 案件引用法条 | `usageType`, `articleText`, `reasoning`, `importance` |
| `CASE_JUDGMENT` | Case → JudgmentDocument | 案件与裁判文书 | `documentRole`, `fact` |
| `CASE_EVIDENCE` | Case → Evidence | 案件与证据 | `evidenceRole`, `admissibility`, `fact` |
| `HAS_CASE_FACT` | Case → CaseFact | 案件含有的事实 | `factRole`, `factNarrative` |
| `HAS_CASE_REASONING` | Case → CaseReasoning | 案件含有的裁判要旨 | `reasoningRole`, `reasoningSummary` |
| `LEGAL_PROVISION_RELATED` | LegalProvision → LegalProvision | 法条之间的关联 | `relationType`, `description` |
| `COURT_HIERARCHY` | Court → Court | 法院层级 | `relationType` |
| `CASE_MEDIATION_ORG` | Case → CommercialMediationOrganization | 案件与调解组织 | `mediationStage`, `mediationResult` |
| `CASE_MEDIATION_AGREEMENT` | Case → MediationAgreement | 案件与调解协议 | `agreementRole`, `fact` |
| `ORG_MEDIATOR` | CommercialMediationOrganization → Mediator | 调解组织与调解员 | `employmentType`, `qualification` |
| `AGREEMENT_JUDICIALLY_CONFIRMED` | MediationAgreement → Court | 调解协议司法确认 | `confirmResult`, `enforceability` |

---

### 五、本体校验与数据写入流程

```
创建实体节点时（NodeServiceImpl.createNode）：

  L1 类型存在性检查
    → 查询 ont_class WHERE graphId + localName = type
    → 不存在则抛 OntologyValidationException

  L2 必填属性检查
    → 查询 ont_property WHERE domainClassId = classId AND isRequired = true
    → 缺失则抛异常

  L3 数据类型检查
    → 对每个属性验证值类型是否匹配 rangeDataType

  L4 约束规则检查
    → 查询 ont_constraint，评估 CARDINALITY / PATTERN / RANGE / ENUM / NOT_NULL

  → 通过后生成 UUID + timestamp
  → 调用 EmbedderService.embed(name + summary) 生成向量
  → 执行 Cypher CREATE (n:Entity {...})
  → 更新 MySQL graphiti_graph_metadata.node_count
```

---

### 六、两层存储的关联机制

```
graphiti_graph_metadata.graphId（MySQL）
    │
    ├── ont_definition.graphId  ──► version / status（DRAFT / ACTIVE / DEPRECATED）
    │       ├── ont_class.definitionId  ──► class 定义
    │       │       ├── ont_class_inheritance.definitionId  ──► 继承关系（多继承支持）
    │       │       │       └── ont_property.definitionId  ──► 属性定义
    │       │       │               ├── isRequired          ──► 必填约束
    │       │       │               ├── allowedValues       ──► 枚举值约束
    │       │       │               ├── pattern           ──► 正则表达式约束
    │       │       │               ├── minValue / maxValue ──► 数值范围约束
    │       │       │               └── defaultValue      ──► 默认值
    │       │       └── ont_constraint.classId / propertyId
    │       └── ont_draft.definitionId  ──► 草稿管理（LLM 生成）
    │
    └── Neo4j Entity/Episode/RELATES_TO  ──► 共享相同的 graphId 值
            └── type 字段 ──► 映射到 ont_class.local_name
```

`graphId` 是跨存储的唯一关联键。MySQL 中的 `ont_class.local_name`（如 `"Person"`）对应 Neo4j 中 `Entity.type` 的值（如 `"Person"`），从而实现本体约束与图数据的类型一致性校验。

---

### 七、Graph IDE 功能实现

#### 7.1 核心功能模块

| 模块 | 功能 | 后端接口 | 前端组件 |
|------|------|----------|----------|
| **图谱可视化** | 力导向/网格/层次/同心圆布局 | `GET /api/v1/graph/{graphId}/visualization` | `GraphCanvas.vue` |
| **节点 CRUD** | 创建/编辑/删除节点 | `POST/GET/PUT/DELETE /api/v1/graph/{graphId}/nodes` | `NodeEditModal.vue` |
| **边 CRUD** | 创建/删除边 | `POST/DELETE /api/v1/graph/{graphId}/edges` | `AddEdgeModal.vue` |
| **Schema 编辑** | 类定义、继承关系、属性约束 | `POST/PUT/DELETE /api/v1/graph/{graphId}/ontology/classes` | `SchemaEditorModal.vue` |
| **属性约束** | 数据类型/必填/枚举/正则/范围 | 属性定义字段 | 属性配置表单 |
| **级联编辑** | 条件筛选、影响预览、批量修改 | `POST /api/v1/graph/{graphId}/cascade/preview/execute` | `CascadeEditModal.vue` |
| **Schema 变更验证** | 验证变更对现有数据的影响 | `POST /api/v1/graph/{graphId}/ontology/validate-change` | `SchemaEditorModal.vue` |

#### 7.2 Schema 变更验证

**支持的变更类型：**

| 变更类型 | 验证内容 | 影响 |
|----------|----------|------|
| `UPDATE_CLASS` | 类名变更检查 | 影响节点 type 属性引用 |
| `UPDATE_PROPERTY` | 必填属性、正则表达式验证 | 检查现有节点是否满足约束 |
| `DELETE_PROPERTY` | 删除属性影响检查 | 统计将丢失数据的节点数量 |
| `ADD_REQUIRED_PROPERTY` | 新增必填属性检查 | 统计缺失该属性的现有节点 |

#### 7.3 级联编辑操作符

| 操作符 | 说明 | 示例 |
|--------|------|------|
| `eq` | 等于 | `status eq 'active'` |
| `ne` | 不等于 | `status ne 'deleted'` |
| `gt` | 大于 | `age gt 18` |
| `lt` | 小于 | `price lt 100` |
| `gte` | 大于等于 | `score gte 60` |
| `lte` | 小于等于 | `score lte 100` |
| `contains` | 包含 | `name contains 'John'` |
| `not_contains` | 不包含 | `desc not_contains 'old'` |
| `in` | 在列表中 | `type in ['Person', 'Company']` |
| `not_in` | 不在列表中 | `status not_in ['deleted']` |
| `is_null` | 为空 | `phone is_null` |
| `is_not_null` | 不为空 | `email is_not_null` |