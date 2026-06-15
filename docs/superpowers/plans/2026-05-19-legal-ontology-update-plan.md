# 法律知识图谱本体与范例数据更新实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 更新 SQL 初始化脚本中的法律本体定义、继承关系和范例数据，并同步更新前端法律知识图谱页面，使前后端本体定义保持一致。

**Architecture:** 本次更新分为两个独立的子系统：(1) SQL 层：补全 PostgreSQL/MySQL 本体继承体系 + 扩充 Neo4j 案例数据；(2) 前端层：修正 legal-kg-data.ts 中的本体定义 + 修正 index.vue 中的字段匹配和统计逻辑。两个子系统可独立开发和测试。

**Tech Stack:** PostgreSQL, MySQL, Neo4j (Cypher), TypeScript/Vue 3

---

## 第一部分：SQL 脚本更新

### Task 1: 读取并确认现有 SQL 文件

**Files:**
- Read: `sql/postgresql/init-data.sql`
- Read: `sql/mysql/init-data.sql`
- Read: `sql/neo4j/init.cypher`
- Read: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/dal/dataobject/ont/OntClassDO.java`
- Read: `ontograph-module-core/src/main/java/com/graphiti/module/graphiti/dal/dataobject/ont/OntPropertyDO.java`

- [ ] **Step 1: Read sql/postgresql/init-data.sql**

确认当前 ont_class 插入语句中所有类的 `parent_class_id` 是否为 NULL，确认 ont_property 插入语句数量。

- [ ] **Step 2: Read sql/mysql/init-data.sql**

确认与 PostgreSQL 版本的结构一致性。

- [ ] **Step 3: Read sql/neo4j/init.cypher**

确认现有 Neo4j 节点数量和关系数量，检查是否已有 `definition_id` 属性。

---

### Task 2: 更新 PostgreSQL init-data.sql — 继承关系

**Files:**
- Modify: `sql/postgresql/init-data.sql:140-177`

- [ ] **Step 1: 在 ont_class 插入后追加继承关系 UPDATE + ont_class_inheritance 插入**

在现有的 ont_class 批量插入语句之后、ont_property 插入之前，追加以下代码：

```sql
    -- ----------------------------------------------------------
    -- 本体类继承关系填充
    -- ----------------------------------------------------------

    -- 填充 ont_class.parent_class_id（单继承）
    UPDATE ont_class SET parent_class_id = (
        SELECT id FROM ont_class c2
        WHERE c2.definition_id = ont_class.definition_id
          AND c2.local_name = 'Case'
    )
    WHERE definition_id = v_def_id
      AND local_name IN ('CivilCase', 'CriminalCase', 'AdministrativeCase', 'CommercialCase', 'ExecutionCase');

    UPDATE ont_class SET parent_class_id = (
        SELECT id FROM ont_class c2
        WHERE c2.definition_id = ont_class.definition_id
          AND c2.local_name = 'Party'
    )
    WHERE definition_id = v_def_id
      AND local_name = 'LegalPerson';

    -- 填充 ont_class_inheritance（多继承表）
    INSERT INTO ont_class_inheritance (class_id, parent_class_id, definition_id, distance, created_at)
    SELECT
        (SELECT id FROM ont_class WHERE definition_id = v_def_id AND local_name = sub_class).id,
        (SELECT id FROM ont_class WHERE definition_id = v_def_id AND local_name = parent_class).id,
        v_def_id,
        1,
        CURRENT_TIMESTAMP
    FROM (VALUES
        ('CivilCase', 'Case'),
        ('CriminalCase', 'Case'),
        ('AdministrativeCase', 'Case'),
        ('CommercialCase', 'Case'),
        ('ExecutionCase', 'Case'),
        ('LegalPerson', 'Party')
    ) AS inheritance(sub_class, parent_class)
    ON CONFLICT ON CONSTRAINT uk_class_parent DO NOTHING;
```

- [ ] **Step 2: 验证继承关系填充**

追加验证查询：

```sql
    -- 验证 ont_class.parent_class_id
    SELECT local_name, (SELECT local_name FROM ont_class c2 WHERE c2.id = ont_class.parent_class_id) AS parent
    FROM ont_class WHERE definition_id = v_def_id AND parent_class_id IS NOT NULL;

    -- 验证 ont_class_inheritance 记录数
    SELECT COUNT(*) AS inheritance_count FROM ont_class_inheritance WHERE definition_id = v_def_id;
```

---

### Task 3: 更新 PostgreSQL init-data.sql — 补充新属性

**Files:**
- Modify: `sql/postgresql/init-data.sql`（在 ont_property 插入末尾追加）

- [ ] **Step 1: 在现有 ont_property 插入后，追加所有新增属性**

在 `INSERT INTO ont_constraint ... RAISE NOTICE 'Inserted ontology constraints';` 之前，追加：

```sql
    -- ----------------------------------------------------------
    -- 新增本体属性（Court 扩展）
    -- ----------------------------------------------------------
    SET @court_class_id = (SELECT id FROM ont_class WHERE definition_id = @v_def_id AND local_name = 'Court');
    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at) VALUES
    (@v_def_id, 'http://legal-ai.cc/ontology/property/jurisdiction', 'jurisdiction', 'DATATYPE', @court_class_id, 'string', 0, 1, 0, 0, '管辖范围', '上海市辖区内的重大案件', '{"displayName": "管辖范围", "formType": "text"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (@v_def_id, 'http://legal-ai.cc/ontology/property/parentCourt', 'parentCourt', 'DATATYPE', @court_class_id, 'string', 0, 1, 0, 0, '上级法院名称', '上海市高级人民法院', '{"displayName": "上级法院", "formType": "text"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

    -- ----------------------------------------------------------
    -- 新增本体属性（Judge 扩展）
    -- ----------------------------------------------------------
    SET @judge_class_id = (SELECT id FROM ont_class WHERE definition_id = @v_def_id AND local_name = 'Judge');
    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at) VALUES
    (@v_def_id, 'http://legal-ai.cc/ontology/property/specialty', 'specialty', 'DATATYPE', @judge_class_id, 'string', 0, 1, 0, 0, '专业领域', '民商事审判', '{"displayName": "专业领域", "formType": "text"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

    -- ----------------------------------------------------------
    -- 新增本体属性（LegalProvision 扩展）
    -- ----------------------------------------------------------
    SET @lp_class_id = (SELECT id FROM ont_class WHERE definition_id = @v_def_id AND local_name = 'LegalProvision');
    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at) VALUES
    (@v_def_id, 'http://legal-ai.cc/ontology/property/keywords', 'keywords', 'DATATYPE', @lp_class_id, 'string', 0, 1, 0, 0, '关键词标签', '公司解散,公司僵局,判断标准', '{"displayName": "关键词", "formType": "text"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

    -- ----------------------------------------------------------
    -- 新增本体属性（JudgmentDocument 扩展）
    -- ----------------------------------------------------------
    SET @jd_class_id = (SELECT id FROM ont_class WHERE definition_id = @v_def_id AND local_name = 'JudgmentDocument');
    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at) VALUES
    (@v_def_id, 'http://legal-ai.cc/ontology/property/mainContent', 'mainContent', 'DATATYPE', @jd_class_id, 'text', 0, 1, 0, 0, '主要内容摘要', '经审理查明...', '{"displayName": "正文摘要", "formType": "textarea"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (@v_def_id, 'http://legal-ai.cc/ontology/property/courtName', 'courtName', 'DATATYPE', @jd_class_id, 'string', 0, 1, 0, 0, '作出法院名称', '上海市第一中级人民法院', '{"displayName": "作出法院", "formType": "text"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

    -- ----------------------------------------------------------
    -- 新增本体属性（CaseReasoning）
    -- ----------------------------------------------------------
    -- 注意：CaseReasoning 类已在 Task 1 的类插入中出现，此处补充其属性
    SET @cr_class_id = (SELECT id FROM ont_class WHERE definition_id = @v_def_id AND local_name = 'CaseReasoning');
    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at) VALUES
    (@v_def_id, 'http://legal-ai.cc/ontology/property/reasoning', 'reasoning', 'DATATYPE', @cr_class_id, 'text', 1, 1, 1, 0, '裁判要旨内容', '公司解散纠纷是股东在穷尽公司自治或其他途径...', '{"displayName": "裁判要旨", "formType": "textarea"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (@v_def_id, 'http://legal-ai.cc/ontology/property/guidanceLevel', 'guidanceLevel', 'DATATYPE', @cr_class_id, 'string', 0, 1, 0, 0, '指导级别', '参考', '{"displayName": "指导级别", "formType": "select", "allowedValues": ["典型", "参考", "备查"]}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (@v_def_id, 'http://legal-ai.cc/ontology/property/applicableScenario', 'applicableScenario', 'DATATYPE', @cr_class_id, 'text', 0, 1, 0, 0, '适用场景', '股东诉请解散公司时，公司运营良好且股东矛盾可通过其他途径解决的', '{"displayName": "适用场景", "formType": "textarea"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

    -- ----------------------------------------------------------
    -- 新增本体属性（CaseFact）
    -- ----------------------------------------------------------
    SET @cf_class_id = (SELECT id FROM ont_class WHERE definition_id = @v_def_id AND local_name = 'CaseFact');
    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at) VALUES
    (@v_def_id, 'http://legal-ai.cc/ontology/property/factDescription', 'factDescription', 'DATATYPE', @cf_class_id, 'text', 1, 1, 1, 0, '事实描述', '2020年3月30日，原告受让被告五位股东持有的股权...', '{"displayName": "事实描述", "formType": "textarea"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (@v_def_id, 'http://legal-ai.cc/ontology/property/factCategory', 'factCategory', 'DATATYPE', @cf_class_id, 'string', 0, 1, 0, 0, '事实类别', '股权转让', '{"displayName": "事实类别", "formType": "text"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (@v_def_id, 'http://legal-ai.cc/ontology/property/factImportance', 'factImportance', 'DATATYPE', @cf_class_id, 'string', 0, 1, 0, 0, '重要程度', 'high', '{"displayName": "重要程度", "formType": "select", "allowedValues": ["high", "medium", "low"]}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

    -- ----------------------------------------------------------
    -- 新增本体属性（CommercialMediationOrganization）
    -- ----------------------------------------------------------
    SET @cmo_class_id = (SELECT id FROM ont_class WHERE definition_id = @v_def_id AND local_name = 'CommercialMediationOrganization');
    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at) VALUES
    (@v_def_id, 'http://legal-ai.cc/ontology/property/orgType', 'orgType', 'DATATYPE', @cmo_class_id, 'string', 0, 1, 0, 0, '组织类型', '商事调解组织', '{"displayName": "组织类型", "formType": "text"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (@v_def_id, 'http://legal-ai.cc/ontology/property/licenseNumber', 'licenseNumber', 'DATATYPE', @cmo_class_id, 'string', 0, 1, 0, 0, '证照编号', '沪商调证字2024001号', '{"displayName": "证照编号", "formType": "text"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (@v_def_id, 'http://legal-ai.cc/ontology/property/establishedDate', 'establishedDate', 'DATATYPE', @cmo_class_id, 'date', 0, 1, 0, 0, '成立日期', '2024-01-01', '{"displayName": "成立日期", "formType": "date"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (@v_def_id, 'http://legal-ai.cc/ontology/property/assetAmount', 'assetAmount', 'DATATYPE', @cmo_class_id, 'decimal', 0, 1, 0, 0, '资产金额', '500000', '{"displayName": "资产金额", "formType": "number"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (@v_def_id, 'http://legal-ai.cc/ontology/property/mediatorCount', 'mediatorCount', 'DATATYPE', @cmo_class_id, 'integer', 0, 1, 0, 0, '调解员数量', '15', '{"displayName": "调解员数量", "formType": "number"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

    -- ----------------------------------------------------------
    -- 新增本体属性（Mediator）
    -- ----------------------------------------------------------
    SET @med_class_id = (SELECT id FROM ont_class WHERE definition_id = @v_def_id AND local_name = 'Mediator');
    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at) VALUES
    (@v_def_id, 'http://legal-ai.cc/ontology/property/qualification', 'qualification', 'DATATYPE', @med_class_id, 'string', 0, 1, 0, 0, '资质类型', '法律职业资格+5年调解经验', '{"displayName": "资质", "formType": "text"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (@v_def_id, 'http://legal-ai.cc/ontology/property/organizationName', 'organizationName', 'DATATYPE', @med_class_id, 'string', 0, 1, 0, 0, '所属组织', '上海国际商事调解中心', '{"displayName": "所属组织", "formType": "text"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (@v_def_id, 'http://legal-ai.cc/ontology/property/yearsExperience', 'yearsExperience', 'DATATYPE', @med_class_id, 'integer', 0, 1, 0, 0, '从业年限', '5', '{"displayName": "从业年限", "formType": "number"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
```

- [ ] **Step 2: 更新 ont_version_history 中的版本统计**

在版本历史插入中，将 propertyCount 从 35 改为实际数量（新增属性后约 50+）：

```sql
    -- 找到并更新 ont_version_history 插入语句
    -- 将 '"propertyCount": 35' 改为当前实际数量
```

- [ ] **Step 3: Commit**

```bash
git add sql/postgresql/init-data.sql
git commit -m "feat(sql): add ontology inheritance and new properties to PostgreSQL init-data"
```

---

### Task 4: 同步更新 MySQL init-data.sql

**Files:**
- Modify: `sql/mysql/init-data.sql`

- [ ] **Step 1: 在 ont_class 插入后追加继承关系**

在 MySQL 版本的 ont_class 批量插入后、ont_property 插入前追加：

```sql
-- ----------------------------------------------------------
-- 本体类继承关系填充
-- ----------------------------------------------------------

-- 填充 ont_class.parent_class_id（单继承）
UPDATE ont_class SET parent_class_id = (
    SELECT c2.id FROM ont_class c2
    WHERE c2.definition_id = ont_class.definition_id
      AND c2.local_name = 'Case'
    LIMIT 1
)
WHERE definition_id = @v_def_id
  AND local_name IN ('CivilCase', 'CriminalCase', 'AdministrativeCase', 'CommercialCase', 'ExecutionCase');

UPDATE ont_class SET parent_class_id = (
    SELECT c2.id FROM ont_class c2
    WHERE c2.definition_id = ont_class.definition_id
      AND c2.local_name = 'Party'
    LIMIT 1
)
WHERE definition_id = @v_def_id
  AND local_name = 'LegalPerson';

-- 填充 ont_class_inheritance（多继承表）
INSERT INTO ont_class_inheritance (class_id, parent_class_id, definition_id, distance, created_at)
SELECT
    (SELECT id FROM ont_class WHERE definition_id = @v_def_id AND local_name = sub_class LIMIT 1),
    (SELECT id FROM ont_class WHERE definition_id = @v_def_id AND local_name = parent_class LIMIT 1),
    @v_def_id,
    1,
    CURRENT_TIMESTAMP
FROM (
    SELECT 'CivilCase' AS sub_class, 'Case' AS parent_class
    UNION ALL SELECT 'CriminalCase', 'Case'
    UNION ALL SELECT 'AdministrativeCase', 'Case'
    UNION ALL SELECT 'CommercialCase', 'Case'
    UNION ALL SELECT 'ExecutionCase', 'Case'
    UNION ALL SELECT 'LegalPerson', 'Party'
) AS inheritance_data
ON DUPLICATE KEY UPDATE created_at = CURRENT_TIMESTAMP;
```

- [ ] **Step 2: 追加所有新增属性（MySQL 语法）**

在 ont_constraint 插入之前追加相同的属性 INSERT 语句，使用 MySQL 的 `SET @var = (SELECT ...)` 模式获取 class_id。

- [ ] **Step 3: Commit**

```bash
git add sql/mysql/init-data.sql
git commit -m "feat(sql): sync ontology inheritance and new properties to MySQL init-data"
```

---

### Task 5: 更新 Neo4j init.cypher — 增加 definition_id

**Files:**
- Modify: `sql/neo4j/init.cypher`

- [ ] **Step 1: 在所有节点 MERGE 语句中追加 `definition_id: 1`**

检查现有 `init.cypher` 中各节点是否已有 `definition_id` 属性。如无，在每个节点块的 `graph_id` 行之后追加 `definition_id: 1,`：

```cypher
// 在每个 MERGE 块的 graph_id 行追加 definition_id
// 例如：
MERGE (c1:Court {
  uuid: 'court-supreme-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,   -- 新增
  courtName: '中华人民共和国最高人民法院',
```

- [ ] **Step 2: Commit**

```bash
git add sql/neo4j/init.cypher
git commit -m "feat(neo4j): add definition_id to all nodes in init.cypher"
```

---

### Task 6: 提取真实案例数据并更新 Neo4j init.cypher

**Files:**
- Modify: `sql/neo4j/init.cypher`（追加新案例节点和关系）

- [ ] **Step 1: 确定要提取的案例**

从以下目录各取 3 个代表性案例（优先选择与现有案例不同类型的）：

| 目录 | 案例类型 | 目标数量 |
|------|---------|---------|
| 1-13-未分类 | 公司解散、劳动争议 | +3 |
| 13-33-分类 | 知识产权、合同纠纷 | +3 |
| 116-155-分类 | 专利侵权、商标侵权 | +3 |

- [ ] **Step 2: 读取案例 JSON 并提取节点数据**

对每个案例 JSON，提取：
- `title` → Case.caseName
- `api_data.cpws_al_ajzh` → Case.caseNumber
- `api_data.cpws_al_case_sort_name` → Case.caseType
- `api_data.cpws_al_slfy_name` → Court.courtName
- `api_data.cpws_al_infos` 中的日期 → Case.filingDate / closedDate
- `api_data.cpws_al_cpyz` → CaseReasoning.reasoning

- [ ] **Step 3: 追加案例节点到 V7 脚本**

在 Neo4j 脚本的第四部分末尾追加：

```cypher
// ==================== 新增真实案例节点 ====================
// 案例: [从 JSON 提取的 caseName]

MERGE (ca_new:CivilCase:Case {
  uuid: 'case-new-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  caseNumber: '（XXXX）XX民初XXXX号',
  caseName: '[提取的案件名称]',
  caseType: '[民事/商事/刑事/行政]',
  caseStatus: '结案',
  caseSummary: '[案件摘要，不超过200字]',
  metadata: '{"source": "人民法院案例库", "crawlingDate": "2025-11-XX"}',
  created_at: datetime()
});
```

- [ ] **Step 4: 为新案例创建关联关系**

追加 CASE_PARTY、CASE_COURT、CASE_JUDGE、CASE_LEGAL_PROVISION 关系。

- [ ] **Step 5: Commit**

```bash
git add sql/neo4j/init.cypher
git commit -m "feat(neo4j): add real cases from court case library to init.cypher"
```

---

## 第二部分：前端页面对齐

### Task 7: 更新 legal-kg-data.ts — 修正本体定义

**Files:**
- Modify: `ontograph-web/src/api/legal-kg-data.ts`

- [ ] **Step 1: 修正 `LEGAL_ENTITIES` 中的类名**

将 `LegalOrganization` 改为 `CommercialMediationOrganization`：

```typescript
// 修正前
{ name: 'LegalOrganization', displayName: '法律组织', ... }

// 修正后
{ name: 'CommercialMediationOrganization', displayName: '商事调解组织', ... }
```

- [ ] **Step 2: 补全 Case 子类**

在 `LEGAL_ENTITIES` 中追加：

```typescript
{
  name: 'CivilCase',
  displayName: '民事案件',
  description: '民事案件，包括婚姻家庭、继承、合同等纠纷',
  extends: 'Case',
  properties: {
    disputeType: { type: 'string', description: '纠纷类型' },
    caseStatus: { type: 'string', description: '案件状态' },
    filingDate: { type: 'date', description: '立案日期' }
  }
},
{
  name: 'CriminalCase',
  displayName: '刑事案件',
  description: '刑事案件，包括公诉和自诉',
  extends: 'Case',
  properties: {}
},
{
  name: 'AdministrativeCase',
  displayName: '行政案件',
  description: '行政案件',
  extends: 'Case',
  properties: {}
},
{
  name: 'CommercialCase',
  displayName: '商事案件',
  description: '商事案件',
  extends: 'Case',
  properties: {
    disputeType: { type: 'string', description: '纠纷类型' },
    mediationAttempted: { type: 'boolean', description: '是否经过调解' }
  }
},
{
  name: 'ExecutionCase',
  displayName: '执行案件',
  description: '执行案件',
  extends: 'Case',
  properties: {}
},
```

- [ ] **Step 3: 补全 `CaseReasoning` 和 `CaseFact` 类**

```typescript
{
  name: 'CaseReasoning',
  displayName: '裁判要旨',
  description: '案例的裁判要旨或指导意义',
  properties: {
    reasoning: { type: 'text', required: true, description: '裁判要旨内容' },
    guidanceLevel: { type: 'string', description: '指导级别', enum: ['典型', '参考', '备查'] },
    applicableScenario: { type: 'text', description: '适用场景' },
    keywords: { type: 'string', description: '关键词' }
  }
},
{
  name: 'CaseFact',
  displayName: '案件事实',
  description: '案件事实描述',
  properties: {
    factDescription: { type: 'text', required: true, description: '事实描述' },
    factCategory: { type: 'string', description: '事实类别' },
    factImportance: { type: 'string', description: '重要程度', enum: ['high', 'medium', 'low'] }
  }
},
```

- [ ] **Step 4: 修正 `LEGAL_EDGES` 中的错误关系名**

将 `PARTY_LAWYER` 的 sourceType 从 `Party` 改为 `LegalOrganization` → 实际上是 `CommercialMediationOrganization`，修正为：

```typescript
// 修正：ORG_MEDIATOR 应从 CommercialMediationOrganization 出发
{
  name: 'ORG_MEDIATOR',
  sourceType: 'CommercialMediationOrganization',
  targetType: 'Mediator',
  ...
}
```

- [ ] **Step 5: 补全缺失的关系**

追加以下关系到 `LEGAL_EDGES`：

```typescript
{
  name: 'HAS_CASE_REASONING',
  displayName: '案件-裁判要旨',
  sourceType: 'Case',
  targetType: 'CaseReasoning',
  description: '案件关联的裁判要旨',
  properties: {
    reasoningRole: { type: 'string', description: '要旨角色' },
    reasoningSummary: { type: 'string', description: '要旨摘要' }
  }
},
{
  name: 'HAS_CASE_FACT',
  displayName: '案件-案件事实',
  sourceType: 'Case',
  targetType: 'CaseFact',
  description: '案件关联的事实',
  properties: {
    factRole: { type: 'string', description: '事实角色' },
    factNarrative: { type: 'string', description: '事实描述' }
  }
},
{
  name: 'COURT_HIERARCHY',
  displayName: '法院层级关系',
  sourceType: 'Court',
  targetType: 'Court',
  description: '法院之间的上下级关系',
  properties: {
    relationType: { type: 'string', description: '关系类型' }
  }
},
{
  name: 'AGREEMENT_JUDICIALLY_CONFIRMED',
  displayName: '调解协议-司法确认',
  sourceType: 'MediationAgreement',
  targetType: 'Court',
  description: '调解协议经法院司法确认',
  properties: {
    confirmDate: { type: 'date', description: '确认日期' },
    confirmResult: { type: 'string', description: '确认结果' }
  }
},
```

- [ ] **Step 6: 修正 `LEGAL_NODES` 中的节点类型**

将所有 `type: 'LegalOrganization'` 改为 `type: 'CommercialMediationOrganization'`。

- [ ] **Step 7: Commit**

```bash
git add ontograph-web/src/api/legal-kg-data.ts
git commit -m "feat(frontend): align legal-kg-data with backend ontology definitions"
```

---

### Task 8: 更新 legal-kg/index.vue — 修正字段匹配和统计逻辑

**Files:**
- Modify: `ontograph-web/src/views/legal-kg/index.vue`

- [ ] **Step 1: 修正 `loadGraphStats` 中的 Case 节点统计（含子类）**

将：

```typescript
stats.caseCount = nodes.filter(n => n.type === 'Case').length
```

替换为：

```typescript
const caseSubtypes = ['Case', 'CivilCase', 'CriminalCase', 'AdministrativeCase', 'CommercialCase', 'ExecutionCase'];
stats.caseCount = nodes.filter(n => caseSubtypes.includes(n.type)).length;
```

- [ ] **Step 2: 修正 `loadGraphStats` 中的法院法官统计**

将：

```typescript
stats.courtJudgeCount = nodes.filter(n => n.type === 'Court' || n.type === 'Judge').length
```

修正为独立统计（法院和法官分开）：

```typescript
stats.courtCount = nodes.filter(n => n.type === 'Court').length;
stats.judgeCount = nodes.filter(n => n.type === 'Judge').length;
```

- [ ] **Step 3: 修正 `getEdgeColor` 补全缺失颜色**

在 `getEdgeColor` 函数中添加：

```typescript
'COURT_HIERARCHY': 'purple',
'HAS_CASE_REASONING': 'gold',
'HAS_CASE_FACT': 'cyan',
'AGREEMENT_JUDICIALLY_CONFIRMED': 'pink',
```

- [ ] **Step 4: 修正 `autoSuggestMappings` 中的字段匹配**

将字段匹配键名与后端 ont_property.local_name 对齐：

- `Case.caseNumber`（而非 `Case.ajh`）
- `Case.caseType`
- `Case.filingDate`
- `Case.closedDate`
- `Case.amountInDispute`
- `Case.caseSummary`（或 `Case.summary`）
- `Party.partyName`（而非 `Party.name`）
- `Court.courtName`（而非 `Court.name`）
- `Court.courtLevel`（而非 `Court.level`）
- `Judge.judgeName`（而非 `Judge.name`）
- `LegalProvision.provisionContent`（而非 `LegalProvision.content`）

- [ ] **Step 5: Commit**

```bash
git add ontograph-web/src/views/legal-kg/index.vue
git commit -m "feat(frontend): align legal-kg index.vue field matching and stats with backend ontology"
```

---

## 第三部分：验证

### Task 9: 验证 SQL 脚本可执行性

- [ ] **Step 1: 验证 PostgreSQL 脚本语法**

确认 DO 块中的 INSERT 语句语法正确，所有 SELECT ... LIMIT 1 能正确返回 class_id。

- [ ] **Step 2: 验证 MySQL 脚本语法**

确认 MySQL 版本的继承关系 UPDATE 和 INSERT ON DUPLICATE KEY UPDATE 语法正确。

- [ ] **Step 3: 验证 Neo4j 脚本语法**

确认所有 MERGE 语句的 `definition_id` 语法正确，所有 MATCH 查询的节点属性与 MERGE 一致。

---

### Task 10: 整体验证

- [ ] **Step 1: 确认所有文件变更**

```bash
git status
```

- [ ] **Step 2: 确认无冲突**

```bash
git diff --stat
```

- [ ] **Step 3: 提交最终变更（如有剩余）**

```bash
git add -A
git commit -m "chore: finalize legal ontology update and frontend alignment"
```

---

## 文件变更汇总

| 文件 | 修改类型 | 关键变更 |
|------|---------|---------|
| `sql/postgresql/init-data.sql` | 修改 | 继承关系填充 + 新增 40+ 属性 |
| `sql/mysql/init-data.sql` | 修改 | 同步 PostgreSQL 变更 |
| `sql/neo4j/init.cypher` | 修改 | definition_id + 新案例节点 |
| `ontograph-web/src/api/legal-kg-data.ts` | 修改 | 类名/关系名修正 + 补全子类/新类 |
| `ontograph-web/src/views/legal-kg/index.vue` | 修改 | 统计逻辑 + 字段匹配 + 颜色映射 |
