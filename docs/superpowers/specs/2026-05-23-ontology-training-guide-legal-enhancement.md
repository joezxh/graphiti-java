# 本体培训文档法律知识图谱主线优化设计

**文档版本**: v1.0  
**创建日期**: 2026-05-23  
**状态**: 实施中

---

## 一、优化背景

### 1.1 现状问题

当前 `ontology-training-guide.md` (3072行) 培训文档存在以下问题:

1. **示例领域不统一**: 混用通用商业示例(张三、阿里巴巴)和法律示例,缺乏主线
2. **与关系文档不一致**: `knowledge-graph-relationships.md` 已提供完整的法律领域示例,但培训文档未采用
3. **实用性不足**: 通用示例缺乏生产级参考价值,无法直接指导法律知识图谱开发

### 1.2 优化目标

以"徐某骥与上海某物业管理有限公司公司解散纠纷案"为贯穿全文的主线案例,将法律领域示例深度整合到培训文档中,实现:

- ✅ 6个核心关系(本体类与实体、本体属性与实体、实体与关系边、剧集与实体/关系、社区与实体、本体约束与实体)100%法律示例覆盖
- ✅ 与 `knowledge-graph-relationships.md` 完全对齐(术语、数据、代码一致)
- ✅ 培训文档可直接作为法律知识图谱开发参考手册

---

## 二、设计策略

### 2.1 方案选型

**选定方案**: 方案 A - 法律知识图谱主线

**选型理由**:
1. 培训文档目标读者(知识图谱工程师、系统架构师)需要深度而非广度
2. 法律领域示例来自真实项目数据,具有生产级参考价值
3. 单一主线案例能形成完整认知链路
4. 与关系文档的法律示例完全一致,形成文档体系

### 2.2 核心原则

1. **替换而非追加**: 用法律示例替换通用示例,而非简单添加(控制文档膨胀)
2. **保留对比说明**: 关键概念保留少量通用示例作为对比,帮助理解
3. **数据完全一致**: 实体 UUID、关系 UUID、社区 UUID 与关系文档保持一致
4. **格式统一**: 代码块语言标识、表格格式、Mermaid 图风格与关系文档一致

---

## 三、章节改造详细设计

### 3.1 第三章 本体核心要素详解 (重点改造)

#### 3.1.1 Class (类) - 第297-433行

**改造内容**:

| 改造项 | 原内容 | 新内容 |
|-------|--------|--------|
| 类层次示例 | `Agent → Person → Employee → CEO` | `LegalEntity → Party → NaturalPerson/LegalPerson` |
| 类定义示例 | `Person`, `Company`, `Organization` | `Party`, `Court`, `Case`, `Judge`, `LegalProvision` |
| 创建类API示例 | `Person` 类创建 | `Party` 类创建 (含完整属性说明) |
| 领域提示 | "法律主体" | "KNOWLEDGE" (与关系文档一致) |

**新增法律类定义示例**:

```sql
-- Party (当事人)
INSERT INTO ont_class (id, definition_id, class_uri, local_name, description, example, domain_hint) VALUES
(10, 1, 'http://legal-ai.cc/ontology/Party', 'Party', 
 '案件中的当事人,包括自然人、法人和非法人组织。',
 '{"partyName": "徐某骥", "partyType": "自然人", "partyRole": "原告"}',
 'KNOWLEDGE');

-- Court (法院)
(20, 1, 'http://legal-ai.cc/ontology/Court', 'Court',
 '审判机关,包括最高人民法院、高级人民法院、中级人民法院、基层人民法院。',
 '{"courtName": "上海市第一中级人民法院", "courtLevel": "中级人民法院"}',
 'KNOWLEDGE');

-- Case (案件)
(30, 1, 'http://legal-ai.cc/ontology/Case', 'Case',
 '法律诉讼案件,包括民事、刑事、行政案件。',
 '{"caseName": "公司解散纠纷案", "caseNumber": "（2022）沪0105民初21387号"}',
 'KNOWLEDGE');
```

**新增法律类继承关系**:

```sql
-- 法律本体类继承关系
INSERT INTO ont_class (id, definition_id, class_uri, local_name, parent_class_id, description) VALUES
(5, 1, 'http://legal-ai.cc/ontology/LegalEntity', 'LegalEntity', NULL,
 '法律领域实体的顶层抽象类'),
(10, 1, 'http://legal-ai.cc/ontology/Party', 'Party', 5,
 '案件当事人(继承自 LegalEntity)'),
(11, 1, 'http://legal-ai.cc/ontology/NaturalPerson', 'NaturalPerson', 10,
 '自然人当事人(继承自 Party)'),
(12, 1, 'http://legal-ai.cc/ontology/LegalPerson', 'LegalPerson', 10,
 '法人当事人(继承自 Party)');
```

---

#### 3.1.2 Property (属性) - 第435-539行

**改造内容**:

| 改造项 | 原内容 | 新内容 |
|-------|--------|--------|
| 数据属性示例 | `Person.age` (integer) | `Party.partyName` (string, 必填) |
| 对象属性示例 | `WORKS_AT (Person→Company)` | `CASE_PARTY (Party→Case)` |
| 属性类型枚举 | 通用6种类型 | 保持不变 + 法律关系元数据说明 |
| 创建属性API示例 | `age` 属性创建 | `partyName`, `caseNumber` 属性创建 |

**新增法律属性定义示例**:

```sql
-- Party 类属性
INSERT INTO ont_property (id, definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, is_required, description) VALUES
(101, 1, 'http://legal-ai.cc/ontology/hasPartyName', 'partyName', 'DATATYPE', 10, 'string', TRUE,
 '当事人姓名或名称'),
(102, 1, 'http://legal-ai.cc/ontology/hasPartyType', 'partyType', 'DATATYPE', 10, 'string', TRUE,
 '当事人类型:自然人/法人/非法人组织'),
(103, 1, 'http://legal-ai.cc/ontology/hasPartyRole', 'partyRole', 'DATATYPE', 10, 'string', TRUE,
 '当事人在案件中的角色:原告/被告/第三人');

-- Case 类属性
(201, 1, 'http://legal-ai.cc/ontology/hasCaseNumber', 'caseNumber', 'DATATYPE', 30, 'string', TRUE,
 '案件编号,格式:(年份)法院简称+案件类型+编号'),
(202, 1, 'http://legal-ai.cc/ontology/hasCaseType', 'caseType', 'DATATYPE', 30, 'string', TRUE,
 '案件类型:民事案件/刑事案件/行政案件');
```

**新增法律关系元数据说明**:

```sql
-- 法律关系类型定义 (ont_relationship_meta 表)
INSERT INTO ont_relationship_meta (id, definition_id, relationship_type, relationship_name, 
    source_entity_types, target_entity_types, is_directional, description) VALUES
(1, 1, 'CASE_PARTY', '案件当事人', 
 '["Party"]', '["Case"]', TRUE,
 '当事人参与案件的关系,包括原告、被告、第三人'),
(2, 1, 'CASE_COURT', '案件法院',
 '["Case"]', '["Court"]', TRUE,
 '案件由某法院审理的关系,包括一审、二审法院');
```

---

#### 3.1.3 Constraint (约束) - 第541-605行

**改造内容**:

| 改造项 | 原内容 | 新内容 |
|-------|--------|--------|
| 正则约束示例 | 邮箱格式 `^[\w-\.]+@...` | 案件编号格式 `^（\d{4}）[\u4e00-\u9fa5]{2,6}民[初终]{1}\d{3,8}号$` |
| 范围约束示例 | 年龄 18-150 | 争议金额 0-100亿 |
| 枚举约束示例 | `MALE,FEMALE,OTHER` | 当事人类型、案件类型、法院级别 |
| 创建约束API示例 | 邮箱、年龄约束 | 案件编号格式、当事人类型枚举约束 |

**新增法律约束定义示例**:

```sql
-- 案件编号格式约束 (PATTERN)
INSERT INTO ont_constraint (id, definition_id, class_id, property_id, constraint_type, value, error_message, severity) VALUES
(1, 1, 30, 201, 'PATTERN',
 '{"pattern": "^（\\d{4}）[\\u4e00-\\u9fa5]{2,6}\\u6c11[\\u521d\\u7ec8]{1}\\d{3,8}号$"}',
 '案件编号格式错误,应为:(年份)法院简称+案件类型+编号',
 'ERROR');

-- 当事人类型枚举约束 (ENUM)
(2, 1, 10, 102, 'ENUM',
 '{"allowed_values": ["自然人", "法人", "非法人组织"]}',
 '当事人类型必须是:自然人、法人或非法人组织',
 'ERROR');

-- 争议金额范围约束 (RANGE)
(6, 1, 30, NULL, 'RANGE',
 '{"property": "amountInDispute", "min": 0, "max": 10000000000}',
 '争议金额必须在 0 到 100亿元之间',
 'WARNING');
```

**新增约束验证流程示例**:

```java
// Java 后端法律约束验证流程示例 (伪代码)
public ValidationResult validateParty(Map<String, Object> properties) {
    // 1. 根据 entity.type="Party" 找到 ont_class.id=10
    OntClassDO partyClass = classMapper.findByLocalName("Party");
    
    // 2. 查询该类的所有约束
    List<OntConstraintDO> constraints = constraintMapper.findByClassId(partyClass.getId());
    
    // 3. 验证 partyType 枚举约束
    if (!List.of("自然人", "法人", "非法人组织").contains(properties.get("partyType"))) {
        errors.add(new ValidationError("ONT004", "当事人类型必须是:自然人、法人或非法人组织"));
    }
    
    // 4. 验证 idNumber 格式约束
    String idNumber = (String) properties.get("idNumber");
    if (idNumber != null && !idNumber.matches("^(\\d{15}|\\d{18}|\\d{17}X)$")) {
        errors.add(new ValidationError("ONT004", "身份证号码格式错误,应为15位或18位"));
    }
    
    return errors.isEmpty() ? ValidationResult.pass() : ValidationResult.fail(errors);
}
```

---

#### 3.1.4 Relationship/Edge (关系边) - 第607-738行

**改造内容**:

| 改造项 | 原内容 | 新内容 |
|-------|--------|--------|
| 关系边示例 | `WORKS_AT (张三→阿里巴巴)` | `CASE_PARTY (徐某骥→公司解散纠纷案)` |
| 对象属性→边转换 | 通用转换流程 | 法律关系边转换流程 |
| 边的验证机制 | 通用 domain/range 验证 | 法律实体类型验证 (Party→Case) |
| 创建边API示例 | `WORKS_AT` 边创建 | `CASE_PARTY`, `CASE_COURT` 边创建 |

**新增法律关系边示例**:

```cypher
// 案件当事人关系 (CASE_PARTY)
(party:Entity {uuid: "party-001", name: "徐某骥", type: "Party"})
-[:RELATES_TO {
    uuid: "rel-party-case-001",
    type: "CASE_PARTY",
    fact: "徐某骥作为原告提起公司解散纠纷诉讼",
    role: "原告",
    valid_at: 1668470400000,         // 2022-11-15 立案日期
    invalid_at: null
}]->
(case:Entity {uuid: "case-001", name: "公司解散纠纷案", type: "Case"})

// 上诉关系时序示例 (APPEALED_CASE)
(case1:Entity {uuid: "case-001", name: "公司解散纠纷案一审"})
-[:RELATES_TO {
    uuid: "rel-appeal-001",
    type: "APPEALED_CASE",
    fact: "徐某骥不服一审判决,提起上诉",
    appealDate: date('2023-01-20'),
    valid_at: 1674172800000,         // 2023-01-20
    invalid_at: 1698105600000        // 2023-10-24 二审判决后失效
}]->
(case2:Entity {uuid: "case-002", name: "公司解散纠纷案二审", caseNumber: "（2023）沪01民终11293号"})
```

**新增法律领域关系类型完整列表**:

| 关系类型 | 源实体类型 | 目标实体类型 | 说明 | 示例 |
|---------|----------|------------|------|------|
| CASE_PARTY | Party | Case | 当事人参与案件 | 原告、被告、第三人 |
| CASE_COURT | Case | Court | 案件由某法院审理 | 一审法院、二审法院 |
| CASE_JUDGE | Case | Judge | 法官审理案件 | 审判长、审判员 |
| CASE_LEGAL_BASIS | Case | LegalProvision | 案件适用某法条 | 《公司法》第182条 |
| CASE_EVIDENCE | Case | Evidence | 案件证据 | 书证、物证、证人证言 |
| APPEALED_CASE | Case | Case | 上诉关系 | 一审→二审 |
| MENTIONS | Episode | Entity/Edge | Episode 提及实体/关系 | 文书→当事人 |
| HAS_MEMBER | Community | Entity | 社区包含成员 | 案件社区→当事人 |

---

#### 3.1.5 Instance/Individual (实例) - 第741-814行

**改造内容**:

| 改造项 | 原内容 | 新内容 |
|-------|--------|--------|
| Entity节点结构 | `张三, type: Person, age: 44` | `徐某骥, type: Party, partyName: "徐某骥"` |
| 与本体类映射 | 通用映射流程 | 法律实体映射流程 |
| 实例验证流程 | 通用验证 | 法律约束验证 (正误对比) |

**新增法律实体节点结构示例**:

```json
{
  "uuid": "party-001",
  "graph_id": "legal-knowledge-graph",
  "name": "徐某骥",
  "type": "Party",
  "partyName": "徐某骥",
  "partyType": "自然人",
  "partyRole": "原告",
  "idNumber": "310105199001011234",
  "summary": "案件原告方,自然人当事人",
  "valid_at": 1668470400000,
  "invalid_at": null
}
```

**新增法律实体创建正误对比**:

```cypher
// ✅ 正确的法律实体创建 (符合所有约束)
CREATE (party:Entity {
    uuid: "party-valid-001",
    graph_id: "legal-knowledge-graph",
    type: "Party",
    name: "徐某骥",
    partyName: "徐某骥",              // ✅ 长度 2-100,非空
    partyType: "自然人",              // ✅ 在枚举值中
    partyRole: "原告",                // ✅ 在枚举值中
    idNumber: "310105199001011234"    // ✅ 符合18位格式
})

// ❌ 错误的法律实体创建 (违反多个约束)
CREATE (party:Entity {
    uuid: "party-invalid-001",
    type: "Party",
    name: "徐",                       // ❌ 违反:姓名长度至少2个字符
    partyName: "徐",
    partyType: "个人",                // ❌ 违反:不在枚举值中
    partyRole: "起诉人",              // ❌ 违反:不在枚举值中
    idNumber: "123456"                // ❌ 违反:身份证号码格式错误
})
```

---

#### 3.1.6 层次结构 (Hierarchy) - 第816-910行

**改造内容**:

| 改造项 | 原内容 | 新内容 |
|-------|--------|--------|
| 类层次树构建 | `Agent → Person → Employee` | `LegalEntity → Party → NaturalPerson/LegalPerson` |
| API响应示例 | 通用类层次 JSON | 法律类层次 JSON |
| 属性层次示例 | `hasContactInfo → hasEmail` | 法律属性层次 (如有) |

**新增法律类层次查询示例**:

```sql
-- 查询法律本体类继承层级
SELECT 
    c1.local_name AS child_class,
    c2.local_name AS parent_class,
    c3.local_name AS grandparent_class
FROM ont_class c1
LEFT JOIN ont_class c2 ON c1.parent_class_id = c2.id
LEFT JOIN ont_class c3 ON c2.parent_class_id = c3.id
WHERE c1.definition_id = 1
ORDER BY c1.local_name;

-- 结果示例:
-- child_class       | parent_class | grandparent_class
-- ------------------|--------------|------------------
-- Case              | NULL         | NULL
-- Court             | LegalEntity  | NULL
-- NaturalPerson     | Party        | LegalEntity
-- Party             | LegalEntity  | NULL
```

---

### 3.2 第四章 知识图谱核心概念

#### 3.2.1 Episode (剧集) - 第991-1076行

**改造内容**:

| 改造项 | 原内容 | 新内容 |
|-------|--------|--------|
| Episode定义 | 通用 Episode 概念 | 法律 Episode 概念 |
| EpisodeType | 通用分类 | 法律 Episode 来源类型 |
| Episode数据结构 | "2024年公司法修订新闻" | "一审判决书"、"庭审笔录" |
| Episode与Entity关系 | 通用抽取流程 | 法律实体抽取流程 |

**新增法律 Episode 示例**:

```cypher
// 法律文书 Episode - 一审判决书
(:Episode {
    graph_id: "legal-knowledge-graph",
    uuid: "ep-judgment-001",
    name: "一审判决书",
    source: "judgment_document",
    source_description: "上海市长宁区人民法院民事判决书",
    content: "原告徐某骥诉被告上海某物业管理有限公司公司解散纠纷一案...本院查明:公司经营管理发生严重困难...",
    documentNumber: "（2022）沪0105民初21387号",
    documentType: "民事判决书",
    issueDate: date('2023-06-15'),
    valid_at: 1686787200000,
    processed: true
})

// Episode 提及法律实体
(ep:Episode {uuid: "ep-judgment-001", name: "一审判决书"})
-[:MENTIONS {uuid: "mention-001", graph_id: "legal-knowledge-graph"}]->
(party:Entity {uuid: "party-001", name: "徐某骥", type: "Party"})
```

**新增法律溯源查询示例**:

```cypher
// 查询"徐某骥"这个实体是从哪些法律文书中提取的
MATCH (ep:Episode)-[:MENTIONS]->(e:Entity {name: "徐某骥"})
RETURN ep.name as document_name, 
       ep.source as document_type,
       ep.documentNumber as document_number,
       ep.issueDate as issue_date
ORDER BY ep.issueDate DESC;

-- 结果示例:
-- document_name        | document_type      | document_number              | issue_date
-- ---------------------|-------------------|------------------------------|------------
-- "一审判决书"         | judgment_document | （2022）沪0105民初21387号    | 2023-06-15
-- "庭审笔录-20230301"  | trial_transcript  | NULL                         | 2023-03-01
```

---

#### 3.2.2 Entity (实体节点) - 第1078-1134行

**改造内容**:

| 改造项 | 原内容 | 新内容 |
|-------|--------|--------|
| Entity定义 | 通用实体类型 | 法律实体类型 (Party, Case, Court) |
| Entity属性结构 | 通用属性 (age, gender) | 法律属性 (partyName, caseNumber) |
| Entity与OntClass映射 | 通用映射 | 法律实体映射 |

**新增法律实体类型示例**:

```
Party (当事人): 徐某骥、上海某物业管理有限公司
Case (案件): 公司解散纠纷案、上诉案
Court (法院): 上海市长宁区人民法院、上海市第一中级人民法院
Judge (法官): 张某法官
LegalProvision (法律条文): 公司法第182条
Evidence (证据): 财务报表、审计报告
JudgmentDocument (裁判文书): 一审判决书、二审判决书
```

---

#### 3.2.3 Edge/Relationship (关系边) - 第1136-1164行

**改造内容**:

| 改造项 | 原内容 | 新内容 |
|-------|--------|--------|
| Edge定义 | 通用关系边 | 法律关系边 |
| 关系类型 | `RELATES_TO`, `MENTIONS`, `IN_COMMUNITY` | 法律8种关系类型 |
| 核心属性 | 通用属性 | 法律属性 (role, courtRole, appealDate) |

---

#### 3.2.4 Community (社区) - 第1166-1214行

**改造内容**:

| 改造项 | 原内容 | 新内容 |
|-------|--------|--------|
| Community定义 | 通用社区概念 | 法律社区概念 |
| 社区元数据 | 通用分类 | 法律社区分类 |
| 实际应用场景 | "劳动合同法相关实体" | "公司解散纠纷相关实体" |

**新增法律社区示例**:

```cypher
// 公司解散纠纷社区
(:Community {
    graph_id: "legal-knowledge-graph",
    uuid: "comm-dissolution-001",
    name: "公司解散纠纷相关实体",
    summary: "该社区包含上海某物业管理有限公司解散纠纷案的所有相关实体,包括股东、董事、法院、法官、律师等。",
    member_count: 23,
    algorithm: "louvain",
    detected_at: datetime('2024-01-15T10:30:00')
})

// 社区成员关系
(comm:Community {uuid: "comm-dissolution-001"})
-[:HAS_MEMBER {uuid: "member-001", graph_id: "legal-knowledge-graph"}]->
(party1:Entity {uuid: "party-001", name: "徐某骥", type: "Party"})
```

**新增法律社区查询示例**:

```cypher
// 查询某个社区的所有成员(按类型分组)
MATCH (c:Community {uuid: "comm-dissolution-001"})-[:HAS_MEMBER]->(e:Entity)
RETURN e.type as entity_type, count(*) as count, collect(e.name) as members
ORDER BY count DESC;

-- 结果示例:
-- entity_type          | count | members
-- ---------------------|-------|----------------------------------
-- Party                | 8     | ["徐某骥", "上海某物业管理有限公司", ...]
-- Case                 | 3     | ["公司解散纠纷案", "上诉案", ...]
-- Court                | 2     | ["上海市长宁区人民法院", ...]
-- LegalProvision       | 5     | ["公司法第182条", "公司法解释二第1条", ...]
```

---

#### 3.2.5 Temporal Graph (时序图谱) - 第1216-1255行

**改造内容**:

| 改造项 | 原内容 | 新内容 |
|-------|--------|--------|
| 时序示例 | "张三的职业变迁" | "上诉关系时序" (一审→二审) |
| 查询示例 | "2018年张三在哪里工作" | "2023年6月案件的有效关系" |

**新增法律时序示例**:

```cypher
// 上诉关系时序 (一审→二审)
(case1:Entity {uuid: "case-001", name: "公司解散纠纷案一审"})
-[:RELATES_TO {
    uuid: "rel-appeal-001",
    type: "APPEALED_CASE",
    valid_at: 1674172800000,         // 2023-01-20 提起上诉
    invalid_at: 1698105600000        // 2023-10-24 二审判决后失效
}]->
(case2:Entity {uuid: "case-002", name: "公司解散纠纷案二审"})

// 时序查询 - 查找案件在特定时间点的有效关系
MATCH (case:Entity {caseNumber: "（2022）沪0105民初21387号"})
      <-[r:RELATES_TO]-
      (entity:Entity)
WHERE r.valid_at <= timestamp('2023-06-15T00:00:00')
  AND (r.invalid_at IS NULL OR r.invalid_at > timestamp('2023-06-15T00:00:00'))
RETURN entity.name, type(r), r.fact, r.valid_at, r.invalid_at;
```

---

#### 3.2.7 概念间的关系图 - 第1281-1308行

**改造内容**:
- 更新 Mermaid 关系图中的节点类型为法律类型
- 更新关系类型为法律关系类型

---

### 3.3 第五章 社区检测功能详解

#### 3.3.1 应用场景 - 第1459-1466行

**新增法律社区检测案例**:

| 场景 | 应用 |
|------|------|
| **法律知识图谱** | 发现"公司解散纠纷相关实体簇"、"商事审判网络" |
| **法条引用分析** | 发现"公司法核心条款引用网络" |
| **法官协作网络** | 发现"经常共同审理案件的法官群体" |

---

### 3.4 第十章 上下文工程应用 (完全重写)

#### 3.4.1 场景描述 - 第2357-2367行

**改造内容**:

| 改造项 | 原内容 | 新内容 |
|-------|--------|--------|
| 背景 | 律师事务所构建"公司法"知识图谱 | 律师事务所构建"公司解散纠纷"知识图谱 |
| 需求 | 通用法律查询 | 徐某骥案件全流程追溯 |

---

#### 3.4.2 步骤1-7: 创建本体到实体关系

**改造内容**: 所有 curl 命令示例中的类、属性、实体、关系改为法律领域

**示例**:

```bash
# 步骤2: 定义法律类
curl -X POST 'http://localhost:8080/api/v1/ontology/legal-kg/classes' \
  -H 'Content-Type: application/json' \
  -d '{
    "localName": "Party",
    "classUri": "http://legal-ai.cc/ontology#Party",
    "description": "案件中的当事人,包括自然人、法人和非法人组织。",
    "example": "{\"partyName\": \"徐某骥\", \"partyType\": \"自然人\"}",
    "domainHint": "KNOWLEDGE"
  }'

# 步骤6: 创建法律实体节点
curl -X POST 'http://localhost:8080/api/v1/graph/legal-kg/nodes' \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "徐某骥",
    "type": "Party",
    "summary": "案件原告方,自然人当事人",
    "properties": {
      "partyName": "徐某骥",
      "partyType": "自然人",
      "partyRole": "原告"
    },
    "validAt": "2022-11-15T00:00:00Z"
  }'
```

---

#### 3.4.3 步骤8: 上下文提取与组装 - 第2766-2857行

**新增法律上下文组装示例**:

```python
# 法律上下文组装
context = {
    "case_info": {
        "name": "徐某骥与上海某物业管理有限公司公司解散纠纷案",
        "caseNumber": "（2022）沪0105民初21387号",
        "caseType": "民事案件",
        "court": "上海市长宁区人民法院",
        "filingDate": "2022-11-15"
    },
    "related_entities": [
        {"name": "徐某骥", "type": "Party", "role": "原告", "partyType": "自然人"},
        {"name": "上海某物业管理有限公司", "type": "Party", "role": "被告", "partyType": "法人"},
        {"name": "上海市长宁区人民法院", "type": "Court", "role": "一审法院"},
        {"name": "张某法官", "type": "Judge", "role": "审判长"},
        {"name": "公司法第182条", "type": "LegalProvision", "relevance": "高"}
    ],
    "timeline": [
        {"date": "2022-11-15", "event": "案件立案"},
        {"date": "2023-03-01", "event": "开庭审理"},
        {"date": "2023-06-15", "event": "一审判决:解散公司"},
        {"date": "2023-01-20", "event": "被告提起上诉"},
        {"date": "2023-10-24", "event": "二审判决:驳回上诉,维持原判"}
    ],
    "applicable_laws": [
        {
            "name": "《中华人民共和国公司法》第182条",
            "content": "公司经营管理发生严重困难,继续存续会使股东利益受到重大损失,通过其他途径不能解决的,持有公司全部股东表决权百分之十以上的股东,可以请求人民法院解散公司。"
        },
        {
            "name": "《公司法解释二》第1条",
            "content": "单独或者合计持有公司全部股东表决权百分之十以上的股东,以下列事由之一提起解散公司诉讼..."
        }
    ],
    "evidence": [
        {"number": "证据1", "type": "书证", "name": "财务报表", "purpose": "证明公司经营管理发生严重困难"},
        {"number": "证据2", "type": "书证", "name": "审计报告", "purpose": "证明公司连续三年亏损"}
    ]
}
```

**新增法律LLM提示词**:

```python
# 构建法律LLM提示词
prompt = f"""
你是一位资深民商事律师,请根据以下上下文信息,为公司解散纠纷案件提供法律意见:

## 案件信息
{context['case_info']}

## 相关实体
{context['related_entities']}

## 时间线
{context['timeline']}

## 适用法律
{context['applicable_laws']}

## 证据清单
{context['evidence']}

请分析:
1. 原告徐某骥的解散请求是否符合《公司法》第182条的法定条件?
2. 公司是否存在"经营管理发生严重困难"的情形?
3. 法院判决解散公司的法律依据是否充分?
4. 被告上诉成功的可能性分析
"""
```

---

## 四、实施步骤

### 4.1 第一阶段:第三章改造 (预计 400-500 行)

1. ✅ 3.1 Class - 替换法律类定义和继承关系
2. ✅ 3.2 Property - 替换法律属性定义
3. ✅ 3.3 Constraint - 替换法律约束定义和验证流程
4. ✅ 3.4 Edge - 替换法律关系边定义
5. ✅ 3.5 Instance - 替换法律实体实例
6. ✅ 3.6 Hierarchy - 替换法律类层次
7. ✅ 3.7 数据模型 - 更新 ER 图和表结构示例

### 4.2 第二阶段:第四章改造 (预计 300-400 行)

1. ✅ 4.1 Episode - 替换法律 Episode 示例和溯源查询
2. ✅ 4.2 Entity - 替换法律实体节点结构
3. ✅ 4.3 Edge - 替换法律关系边类型
4. ✅ 4.4 Community - 替换法律社区示例和查询
5. ✅ 4.5 Temporal Graph - 替换法律时序示例
6. ✅ 4.7 关系图 - 更新 Mermaid 图

### 4.3 第三阶段:第五章和第十章改造 (预计 200-300 行)

1. ✅ 5.5 应用场景 - 添加法律社区检测案例
2. ✅ 10.3 实战案例 - 完全重写为徐某骥案件
3. ✅ 10.3.9 上下文提取 - 添加法律上下文组装示例

### 4.4 第四阶段:其他章节微调 (预计 50-100 行)

1. ✅ 第二章 - 更新形式化本体四要素示例
2. ✅ 第九章 - 更新 API 示例为法律领域
3. ✅ 全局术语检查和格式统一

---

## 五、质量保障

### 5.1 一致性检查清单

- [ ] 所有法律实体 UUID 与 `knowledge-graph-relationships.md` 一致
- [ ] 所有法律关系 UUID 与关系文档一致
- [ ] 所有 SQL 代码块与关系文档一致
- [ ] 所有 Cypher 代码块与关系文档一致
- [ ] 法律术语使用统一 (如"当事人"而非"参与方")
- [ ] 案号格式统一 (使用中文括号"（）"而非英文括号"()")

### 5.2 格式统一检查

- [ ] SQL 代码块使用 ` ```sql ` 标识
- [ ] Cypher 代码块使用 ` ```cypher ` 标识
- [ ] Java 代码块使用 ` ```java ` 标识
- [ ] JSON 代码块使用 ` ```json ` 标识
- [ ] 表格格式与关系文档一致
- [ ] Mermaid 图样式与关系文档一致

### 5.3 内容完整性检查

- [ ] 6个核心关系都有对应的法律示例
- [ ] 每个改造章节都有"法律领域数据示例"板块
- [ ] 查询示例都包含结果输出示例
- [ ] 正误对比示例清晰标注 (✅/❌)

---

## 六、风险评估与应对

### 6.1 风险

| 风险 | 影响 | 概率 | 应对措施 |
|------|------|------|---------|
| 文档过长 | 读者阅读负担增加 | 中 | 严格控制新增行数在 800-1000 行以内 |
| 法律示例理解成本高 | 非法律领域开发者难以理解 | 低 | 添加"法律领域背景说明"小节 |
| 与关系文档不一致 | 文档体系混乱 | 低 | 实施过程中逐项对照检查 |

### 6.2 应对策略

1. **文档长度控制**: 采用"替换而非追加"策略,删除通用示例后再添加法律示例
2. **背景知识补充**: 在第三章开头添加"法律领域背景说明" (200字以内)
3. **一致性保障**: 实施过程中打开两个文档对照编辑,使用搜索工具快速定位

---

## 七、预期成果

### 7.1 定量指标

- 📄 新增/修改内容: 800-1000 行
- 📊 法律示例覆盖: 6个核心关系 100% 覆盖
- 🎯 与关系文档一致性: 100%
- 📚 章节改造数量: 4个主要章节 (3/4/5/10章)

### 7.2 定性指标

- ✅ 培训文档可直接作为法律知识图谱开发参考手册
- ✅ 读者能通过单一主线案例建立完整的知识图谱认知
- ✅ 所有代码示例可直接复制运行 (假设数据库已初始化)
- ✅ 形成"理论→实践→应用"的完整学习链路

---

**文档状态**: ✅ 设计完成,等待实施
