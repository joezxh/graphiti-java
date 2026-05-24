# Graphiti-Java 知识图谱与本体论全栈技术培训

> **文档版本**: v1.0  
> **适用对象**: 全栈开发工程师、知识图谱工程师、系统架构师  
> **技术栈**: Java 21 + Spring Boot 3.x + Apache Jena + Neo4j 5.x + Vue 3 + TypeScript  
> **更新日期**: 2026-05-21

---

## 文档导览

本培训文档面向全栈技术团队,系统讲解知识图谱与本体论的理论知识及Graphiti-Java项目的完整实现。文档分为三大板块:

**📖 理论篇** (第1-2章): 本体论历史、核心概念、知识图谱架构体系  
**🔧 实践篇** (第3-7章): 本体要素、核心概念、社区检测、验证推理、系统架构  
**💻 应用篇** (第8-9章): REST API参考、接口使用范例、业务场景演示

---

## 目录

- [第一章 本体论的历史与发展](#第一章-本体论的历史与发展)
- [第二章 本体论基本概念](#第二章-本体论基本概念)
- [第三章 本体核心要素详解](#第三章-本体核心要素详解)
  - [3.1 Class(类)](#31-class类)
  - [3.2 Property(属性)](#32-property属性)
  - [3.3 Constraint(约束)](#33-constraint约束)
  - [3.4 Relationship/Edge(关系/边)](#34-relationshipedge关系边)
  - [3.5 Instance/Individual(实例)](#35-instanceindividual实例)
  - [3.6 层次结构(Hierarchy)](#36-层次结构hierarchy)
  - [3.7 数据模型与表结构](#37-数据模型与表结构)
- [第四章 知识图谱核心概念](#第四章-知识图谱核心概念)
- [第五章 社区检测功能详解](#第五章-社区检测功能详解)
- [第六章 系统功能特性](#第六章-系统功能特性)
- [第七章 系统架构设计](#第七章-系统架构设计)
- [第八章 REST API完整参考](#第八章-rest-api完整参考)
- [第九章 接口使用范例](#第九章-接口使用范例)
- [第十章 上下文工程应用](#第十章-上下文工程应用)
- [附录 最佳实践与常见问题](#附录-最佳实践与常见问题)

---

## 第一章 本体论的历史与发展

### 1.1 哲学起源:从亚里士多德到现代

本体论(Ontology)一词源于希腊语"ontos"(存在)和"logos"(学问),最初是**哲学的一个分支**,研究"存在"的本质和范畴。

**亚里士多德的范畴论**(公元前4世纪):
- 提出了10个基本范畴:实体、数量、性质、关系、地点、时间、姿态、状态、动作、承受
- 这是人类历史上第一次尝试**形式化分类系统**
- 核心思想:世界上的事物可以按照其本质属性进行分类

**中世纪经院哲学**:
- 波菲利之树(Porphyrian Tree):第一个树形分类系统
- 属(Genus)与种(Species)的层次关系

### 1.2 20世纪形式化本体论

**现象学与存在主义**:
- 胡塞尔、海德格尔等哲学家深化了本体论研究
- 从"存在是什么"转向"存在如何被理解"

**分析哲学的贡献**:
- 罗素、维特根斯坦等人提出逻辑原子主义
- 世界由事实而非事物构成
- 为后来的知识表示奠定了逻辑基础

### 1.3 计算机科学中的应用

**人工智能的兴起**(1980s-1990s):
- 专家系统需要**领域知识的形式化表示**
- Tom Gruber(1993)提出计算机科学中的本体定义:
  > **"本体是对概念化的显式规范说明"**
  > (An ontology is an explicit specification of a conceptualization)

**语义网运动**(1990s-2000s):
- Tim Berners-Lee提出语义网愿景
- W3C制定RDF、OWL等标准
- 目标:让机器能够**理解**而不仅是**展示**信息

**知识图谱时代**(2010s-至今):
- Google Knowledge Graph(2012)
- 本体论成为知识图谱的**类型系统**和**约束框架**
- 从学术研究走向工业实践

### 1.4 OWL标准的演进

| 版本 | 年份 | 特点 | 表达能力 |
|------|------|------|----------|
| OWL Lite | 2004 | 简单分类层次、简单约束 | 低 |
| OWL DL | 2004 | 完整描述逻辑,可判定 | 中 |
| OWL Full | 2004 | 最大表达能力,不可判定 | 高 |
| **OWL 2 RL** | 2009 | 规则语言,适合大规模推理 | 中低 |
| OWL 2 EL | 2009 | 存在量词优化,适合本体大规模 | 低 |

**Graphiti-Java选择OWL 2 RL的原因**:
- **性能**: 基于规则的推理,适合大规模知识图谱
- **可判定性**: 推理复杂度可控(Polynomial time)
- **实用性**: 覆盖了90%的业务场景需求

### 1.5 Graphiti-Java的本体论实践

Graphiti-Java将本体论理论应用到工业级知识图谱系统中:

```
哲学本体论 (研究"存在")
    ↓
计算机本体论 (知识形式化)
    ↓
OWL标准 (W3C规范)
    ↓
Graphiti-Java实现 (Java + Jena + Neo4j)
```

**核心实现**:
- **本体建模**: OntClass、OntProperty、OntConstraint
- **推理引擎**: Apache Jena OWL 2 RL Reasoner
- **验证体系**: 6层验证引擎
- **数据存储**: 关系数据库(元数据) + Neo4j(图数据)

---

## 第二章 本体论基本概念

### 2.1 什么是本体(Definition)

**定义**: 本体是**共享概念模型的显式形式化规范说明**。

通俗理解:
- **概念模型**: 对某个领域的抽象理解(如"法律领域有哪些概念")
- **显式**: 明确定义,不含糊
- **形式化**: 机器可读、可处理
- **共享**: 多人/多系统共同使用

**类比**: 本体就像数据库的**Schema定义**,但比Schema更强大:

| 特性 | 数据库Schema | 本体(Ontology) |
|------|-------------|----------------|
| 实体定义 | 表(Table) | 类(Class) |
| 属性定义 | 列(Column) | 属性(Property) |
| 关系定义 | 外键(FK) | 对象属性(Object Property) |
| 约束 | CHECK/NOT NULL | 基数、类型、正则、枚举 |
| 层次关系 | ❌ 不支持 | ✅ 支持类继承 |
| 推理能力 | ❌ 无 | ✅ 可推断新知识 |
| 语义表达 | 弱 | 强(OWL标准) |

### 2.2 本体在知识图谱中的核心作用

**三大核心作用**:

1. **类型系统**(Type System)
   - 定义知识图谱中有哪些类型的实体(如Party当事人、Court法院、Case案件)
   - 定义实体之间的关系类型(如CASE_PARTY当事人参与、CASE_COURT案件审理)
   - **作用**: 保证法律数据的一致性和规范性

2. **验证框架**(Validation Framework)
   - 必填属性检查(如Party必须有partyName当事人姓名)
   - 数据类型检查(如Case.filingDate必须是日期)
   - 约束规则(如caseNumber必须符合法院编号格式)
   - **作用**: 在数据写入前拦截错误

3. **推理基础**(Reasoning Foundation)
   - 类层次推理(如Plaintiff原告是Party的子类)
   - 属性推断(如CASE_PARTY的逆关系是CASE_HAS_PARTY)
   - 一致性检查(如某实体不能既是Party又是Court)
   - **作用**: 从已有法律知识推导新知识

### 2.3 形式化本体的四个要素

根据经典本体论,一个完整的形式化本体包含四个要素:

```
Ontology = (C, R, A, I)

C (Concepts):    概念集合,即类(Class)
R (Relations):   关系集合,即属性(Property)
A (Axioms):      公理集合,即约束(Constraint)
I (Instances):   实例集合,即个体(Individual)
```

**示例:公司解散纠纷法律知识图谱本体**

```java
// 概念 (Concepts) - 法律实体类
Class: Party, Court, Case, LegalProvision, Judge, Evidence

// 关系 (Relations) - 法律关系
Property: 
  - CASE_PARTY(Party → Case)         // 当事人参与案件
  - CASE_COURT(Case → Court)         // 案件由法院审理
  - CASE_JUDGE(Case → Judge)         // 法官审理案件
  - CASE_LEGAL_BASIS(Case → LegalProvision)  // 案件适用法条
  - APPEALED_CASE(Case → Case)       // 上诉关系

// 公理 (Axioms) - 法律约束
Constraint:
  - Case.caseNumber 符合格式: ^（\d{4}）[\u4e00-\u9fa5]{2,6}民[初终]{1}\d{3,8}号$
  - Party.partyType ∈ ["自然人", "法人", "非法人组织"]
  - Party.partyRole ∈ ["原告", "被告", "第三人", "上诉人", "被上诉人"]
  - Court.courtLevel ∈ ["最高人民法院", "高级人民法院", "中级人民法院", "基层人民法院"]

// 实例 (Instances) - 存储在Neo4j中,来自真实案例
Individual:
  - 徐某骥 (type: Party, partyRole: 原告, partyType: 自然人)
  - 上海某物业管理有限公司 (type: Party, partyRole: 被告, partyType: 法人)
  - 公司解散纠纷案 (type: Case, caseNumber: （2022）沪0105民初21387号)
  - 上海市长宁区人民法院 (type: Court, courtLevel: 基层人民法院)
  - 《公司法》第182条 (type: LegalProvision)
```

### 2.4 知识图谱的8层架构体系

Graphiti-Java的知识图谱系统采用**8层架构**,本体只是其中一层:

```
┌─────────────────────────────────────────────────────┐
│ 8️⃣  操作层 (Operation)                              │
│     Pipeline、Import/Export、Clone、Audit           │
├─────────────────────────────────────────────────────┤
│ 7️⃣  验证层 (Validation)                             │
│     Ontology Validation、Data Quality               │
├─────────────────────────────────────────────────────┤
│ 6️⃣  推理层 (Reasoning)                              │
│     Type Inference、OWL Consistency、Jena Reasoner  │
├─────────────────────────────────────────────────────┤
│ 5️⃣  元数据层 (Metadata)                             │
│     GraphMetadata、EpisodeType、CommunityType       │
├─────────────────────────────────────────────────────┤
│ 4️⃣  组织层 (Organization)                           │
│     Community Detection、Label Propagation          │
├─────────────────────────────────────────────────────┤
│ 3️⃣  时间层 (Temporal)                               │
│     validAt、invalidAt、时序事实查询                │
├─────────────────────────────────────────────────────┤
│ 2️⃣  数据层 (Data)                                   │
│     Entity、Edge、Episode、Community                │
├─────────────────────────────────────────────────────┤
│ 1️⃣  本体层 (Ontology) ← 本培训重点                  │
│     OntClass、OntProperty、OntConstraint            │
└─────────────────────────────────────────────────────┘
```

**各层的关系**:

```
本体层 (定义"是什么")
   ↓ 指导
数据层 (存储"实例")
   ↓ 标注
时间层 (记录"何时有效")
   ↓ 组织
组织层 (发现"群组结构")
   ↓ 依赖
元数据层 (管理"系统信息")
   ↓ 支撑
推理层 (实现"智能推断")
   ↓ 保障
验证层 (确保"数据质量")
   ↓ 提供
操作层 (提供"管理能力")
```

### 2.5 本体与知识图谱的关系

```
┌──────────────────────┐
│   知识图谱 (整体)     │
│                      │
│  ┌────────────────┐  │
│  │  本体 (骨架)    │  │
│  │  - 类定义       │  │
│  │  - 属性定义     │  │
│  │  - 约束规则     │  │
│  └────────────────┘  │
│         ↓ 定义        │
│  ┌────────────────┐  │
│  │  数据 (血肉)    │  │
│  │  - 实体节点     │  │
│  │  - 关系边       │  │
│  │  - 属性值       │  │
│  └────────────────┘  │
└──────────────────────┘
```

- **比喻**:
- **本体**是建筑的**设计图纸**(定义结构、规则)
- **数据**是建筑的**砖瓦**(实际构建物)
- **推理**是建筑的**自动化系统**(根据规则推导)
- **验证**是建筑的**质检系统**(确保合规)

---

## 第三章 本体核心要素详解

本章详细讲解本体的核心构成要素,每个要素都包含:
- 理论定义(本体论层面)
- 数据模型(关系数据库表结构)
- 代码实现(Java VO/DO)
- 使用示例(API调用)

### 3.1 Class(类)

#### 3.1.1 理论定义

**Class(类)** 是对具有相同特征和行为的实体的抽象描述。

**OWL语义**:
- 类是**个体的集合**(Set of Individuals)
- 类可以形成**层次结构**(subClassOf关系)
- 类之间可以有**等价关系**(equivalentTo)
- 类之间可以有**不相交关系**(disjointWith)

**示例** (法律知识图谱):
```
LegalEntity (法律实体 - 根类)
  └─ Party (当事人)
      ├─ NaturalPerson (自然人)
      │   └─ Plaintiff (原告)
      │   └─ Defendant (被告)
      └─ LegalPerson (法人)
          └─ Company (公司)
  └─ Court (法院)
      ├─ SupremeCourt (最高人民法院)
      └─ LocalCourt (地方法院)
  └─ Case (案件)
      ├─ CivilCase (民事案件)
      └─ CriminalCase (刑事案件)
```

**实际案例**: 徐某骥与上海某物业管理有限公司公司解散纠纷案
- 徐某骥 → type: NaturalPerson (自然人当事人)
- 上海某物业管理有限公司 → type: LegalPerson (法人当事人)
- 上海市长宁区人民法院 → type: Court (法院)
- 公司解散纠纷案 → type: CivilCase (民事案件)

#### 3.1.2 数据模型

**关系数据库表** (支持MySQL/PostgreSQL): `ont_class`

| 字段 | 类型 | 说明 | 示例 |
|------|------|------|------|
| id | BIGINT | 主键 | 1 |
| definition_id | BIGINT | 所属本体定义ID | 10 |
| class_uri | VARCHAR(512) | 完整URI | `http://legal-ai.cc/ontology#Person` |
| local_name | VARCHAR(128) | 本地名称(用于Neo4j type字段) | `Person` |
| parent_class_id | BIGINT | 父类ID(支持单继承) | null(根类) |
| equivalent_to | TEXT | 等价类(JSON数组) | `[{"uri": "...", "name": "Human"}]` |
| disjoint_with | TEXT | 不相交类(JSON数组) | `[{"uri": "...", "name": "Company"}]` |
| description | TEXT | 类描述 | "表示自然人个体" |
| example | TEXT | 使用示例 | "徐某骥、上海某物业管理有限公司" |
| domain_hint | VARCHAR(32) | 领域分类标记 | "法律主体" |
| metadata | TEXT | 扩展元数据(JSON) | `{"color": "blue"}` |

**关键约束**:
```sql
-- URI在同一个本体定义内唯一
UNIQUE KEY uk_ont_class_uri (definition_id, class_uri)
-- 本地名称在同一个本体定义内唯一
UNIQUE KEY uk_ont_class_local_name (definition_id, local_name)
-- 父类引用(支持级联删除设置为NULL)
CONSTRAINT fk_ont_class_parent FOREIGN KEY (parent_class_id) 
    REFERENCES ont_class(id) ON DELETE SET NULL
```

#### 3.1.3 Java实现

**VO类** (`OntClassVO.java`):
```java
public class OntClassVO {
    private Long id;
    private Long definitionId;
    private String classUri;
    private String localName;
    private Long parentClassId;
    private String parentClassUri;  // 父类URI(查询时填充)
    private List<EquivalentClassVO> equivalentTo;  // 等价类列表
    private List<DisjointClassVO> disjointWith;    // 不相交类列表
    private String description;
    private String example;
    private String domainHint;
    private String metadata;
    // getter/setter...
}
```

**DO类** (`OntClassDO.java`):
```java
@TableName("ont_class")
public class OntClassDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long definitionId;
    private String classUri;
    private String localName;
    private Long parentClassId;
    private String equivalentTo;  // JSON字符串
    private String disjointWith;  // JSON字符串
    private String description;
    private String example;
    private String domainHint;
    private String metadata;      // JSON字符串
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // getter/setter...
}
```

#### 3.1.4 使用示例

**创建类**(curl) - 法律领域示例:
```bash
# 创建法律根类: LegalEntity
curl -X POST 'http://localhost:8080/api/v1/ontology/legal-kg/classes' \
  -H 'Authorization: Bearer <token>' \
  -H 'Content-Type: application/json' \
  -d '{
    "localName": "LegalEntity",
    "classUri": "http://legal-ai.cc/ontology#LegalEntity",
    "description": "法律领域实体的顶层抽象类",
    "domainHint": "KNOWLEDGE"
  }'

# 创建法律子类: Party (当事人)
curl -X POST 'http://localhost:8080/api/v1/ontology/legal-kg/classes' \
  -H 'Authorization: Bearer <token>' \
  -H 'Content-Type: application/json' \
  -d '{
    "localName": "Party",
    "classUri": "http://legal-ai.cc/ontology#Party",
    "parentClassId": 1,
    "description": "案件中的当事人,包括自然人、法人和非法人组织。",
    "example": "{\"partyName\": \"徐某骥\", \"partyType\": \"自然人\", \"partyRole\": \"原告\"}",
    "domainHint": "KNOWLEDGE"
  }'
```

**响应**:
```json
{
  "code": 0,
  "data": {
    "id": 10,
    "definitionId": 1,
    "classUri": "http://legal-ai.cc/ontology#Party",
    "localName": "Party",
    "parentClassId": 1,
    "parentClassUri": "http://legal-ai.cc/ontology#LegalEntity",
    "description": "案件中的当事人,包括自然人、法人和非法人组织。",
    "example": "{\"partyName\": \"徐某骥\", \"partyType\": \"自然人\", \"partyRole\": \"原告\"}",
    "domainHint": "KNOWLEDGE",
    "createdAt": "2026-05-23T10:30:00"
  }
}
```

**法律领域常用类定义**:

```sql
-- PostgreSQL 法律本体类定义示例
INSERT INTO ont_class (id, definition_id, class_uri, local_name, parent_class_id, description, example, domain_hint) VALUES
-- 法律根类
(5, 1, 'http://legal-ai.cc/ontology/LegalEntity', 'LegalEntity', NULL,
 '法律领域实体的顶层抽象类', NULL, 'KNOWLEDGE'),

-- 核心法律实体类
(10, 1, 'http://legal-ai.cc/ontology/Party', 'Party', 5, 
 '案件中的当事人,包括自然人、法人和非法人组织。',
 '{"partyName": "徐某骥", "partyType": "自然人", "partyRole": "原告"}',
 'KNOWLEDGE'),

(20, 1, 'http://legal-ai.cc/ontology/Court', 'Court', 5,
 '审判机关,包括最高人民法院、高级人民法院、中级人民法院、基层人民法院。',
 '{"courtName": "上海市第一中级人民法院", "courtLevel": "中级人民法院"}',
 'KNOWLEDGE'),

(30, 1, 'http://legal-ai.cc/ontology/Case', 'Case', 5,
 '法律诉讼案件,包括民事、刑事、行政案件。',
 '{"caseName": "公司解散纠纷案", "caseNumber": "（2022）沪0105民初21387号"}',
 'KNOWLEDGE'),

(40, 1, 'http://legal-ai.cc/ontology/Judge', 'Judge', 5,
 '案件审判人员,包括审判长、审判员、人民陪审员。',
 '{"judgeName": "张某", "judgeTitle": "审判长"}',
 'KNOWLEDGE'),

(50, 1, 'http://legal-ai.cc/ontology/LegalProvision', 'LegalProvision', 5,
 '法律条文,包括法律、法规、司法解释的具体条款。',
 '{"lawName": "中华人民共和国公司法", "articleNumber": "第182条"}',
 'KNOWLEDGE');
```

**重要规则**:
- ❌ **删除类时如果存在子类,会拒绝删除**
  ```
  Error: "类 Person 存在子类 Employee,请先删除子类型"
  ```
- ✅ **必须先删除所有后代类,才能删除父类**

---

### 3.2 Property(属性)

#### 3.2.1 理论定义

**Property(属性)** 定义类之间的关系或类与数据类型值之间的关系。

**OWL中的两种属性**:

| 类型 | 英文名称 | 连接对象 | Neo4j存储 | 法律领域示例 |
|------|---------|---------|-----------|------|
| **对象属性** | Object Property | 类 → 类 | **Edge(边)** | CASE_PARTY(Party→Case) |
| **数据属性** | Datatype Property | 类 → 数据类型 | **Node Property(节点属性)** | Party.partyName(string) |

**属性的域和范围**:
- **Domain(定义域)**: 属性可以出现在哪个类上
- **Range(值域)**: 属性的值可以是什么类型

**示例** (法律知识图谱):
```
CASE_PARTY (对象属性 - 案件当事人关系)
  Domain: Party (只能是当事人"参与案件")
  Range: Case (参与的对象是案件)
  
partyName (数据属性 - 当事人姓名)
  Domain: Party
  Range: xsd:string (字符串)

caseNumber (数据属性 - 案件编号)
  Domain: Case
  Range: xsd:string (字符串, 格式:（年份）法院简称+案件类型+编号)
```

#### 3.2.2 数据模型

**关系数据库表** (支持MySQL/PostgreSQL): `ont_property`

| 字段 | 类型 | 说明 | 示例 |
|------|------|------|------|
| id | BIGINT | 主键 | 100 |
| definition_id | BIGINT | 所属本体定义ID | 10 |
| property_uri | VARCHAR(512) | 完整URI | `http://legal-ai.cc/ontology#WORKS_AT` |
| local_name | VARCHAR(128) | 本地名称 | `WORKS_AT` |
| property_type | VARCHAR(20) | 属性类型 | `OBJECT` / `DATATYPE` |
| domain_class_id | BIGINT | 定义域(所属类) | 10(Person) |
| range_class_id | BIGINT | 值域类(OBJECT属性) | 20(Company) |
| range_data_type | VARCHAR(32) | 值域数据类型(DATATYPE属性) | `integer` |
| is_required | TINYINT(1) | 是否必填 | 1(是) |
| is_multiple | TINYINT(1) | 是否允许多值 | 0(否) |
| min_cardinality | INT | 最小基数 | 0 |
| max_cardinality | INT | 最大基数 | 1 |
| default_value | VARCHAR(512) | 默认值 | `"UNKNOWN"` |
| pattern | VARCHAR(256) | 正则表达式 | `^[\w-\.]+@([\w-]+\.)+[\w-]{2,4}$` |
| min_value | DECIMAL | 数值最小值 | 18 |
| max_value | DECIMAL | 数值最大值 | 150 |
| parent_property_id | BIGINT | 父属性ID | null |
| inverse_of_id | BIGINT | 逆属性ID | 101(EMPLOYS) |

**属性类型枚举**:
```java
public enum PropertyType {
    DATATYPE,    // 数据属性: 类 → 数据类型值
    OBJECT,      // 对象属性: 类 → 类(存储为Edge)
    ANNOTATION,  // 注解属性: 元数据
    TRANSITIVE,  // 传递属性: A→B, B→C ⇒ A→C
    SYMMETRIC,   // 对称属性: A→B ⇒ B→A
    FUNCTIONAL   // 函数属性: 每个个体最多一个值
}
```

#### 3.2.3 代码示例

**创建数据属性**(Party.partyName) - 法律领域示例:
```bash
curl -X POST 'http://localhost:8080/api/v1/ontology/legal-kg/properties' \
  -H 'Content-Type: application/json' \
  -d '{
    "localName": "partyName",
    "propertyUri": "http://legal-ai.cc/ontology#hasPartyName",
    "propertyType": "DATATYPE",
    "domainClassId": 10,
    "rangeDataType": "string",
    "isRequired": true,
    "description": "当事人姓名或名称"
  }'
```

**创建对象属性**(Party.CASE_PARTY Case) - 法律领域示例:
```bash
curl -X POST 'http://localhost:8080/api/v1/ontology/legal-kg/properties' \
  -H 'Content-Type: application/json' \
  -d '{
    "localName": "CASE_PARTY",
    "propertyUri": "http://legal-ai.cc/ontology#hasCaseParty",
    "propertyType": "OBJECT",
    "domainClassId": 10,
    "rangeClassId": 30,
    "isRequired": true,
    "isMultiple": true,
    "description": "当事人参与案件的关系,包括原告、被告、第三人"
  }'
```

**法律领域常用属性定义**:

```sql
-- PostgreSQL 法律本体属性定义示例
INSERT INTO ont_property (id, definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, is_required, description) VALUES
-- Party 类属性
(101, 1, 'http://legal-ai.cc/ontology/hasPartyName', 'partyName', 'DATATYPE', 10, 'string', TRUE,
 '当事人姓名或名称'),
(102, 1, 'http://legal-ai.cc/ontology/hasPartyType', 'partyType', 'DATATYPE', 10, 'string', TRUE,
 '当事人类型:自然人/法人/非法人组织'),
(103, 1, 'http://legal-ai.cc/ontology/hasPartyRole', 'partyRole', 'DATATYPE', 10, 'string', TRUE,
 '当事人在案件中的角色:原告/被告/第三人'),

-- Case 类属性
(201, 1, 'http://legal-ai.cc/ontology/hasCaseNumber', 'caseNumber', 'DATATYPE', 30, 'string', TRUE,
 '案件编号,格式:(年份)法院简称+案件类型+编号'),
(202, 1, 'http://legal-ai.cc/ontology/hasCaseType', 'caseType', 'DATATYPE', 30, 'string', TRUE,
 '案件类型:民事案件/刑事案件/行政案件'),
(203, 1, 'http://legal-ai.cc/ontology/hasFilingDate', 'filingDate', 'DATATYPE', 30, 'date', FALSE,
 '案件立案日期'),

-- Court 类属性
(301, 1, 'http://legal-ai.cc/ontology/hasCourtName', 'courtName', 'DATATYPE', 20, 'string', TRUE,
 '法院名称'),
(302, 1, 'http://legal-ai.cc/ontology/hasCourtLevel', 'courtLevel', 'DATATYPE', 20, 'string', FALSE,
 '法院级别:最高/高级/中级/基层');
```

**关键点**:
- `OBJECT` 类型属性 → 在Neo4j中存储为**边**
- `DATATYPE` 类型属性 → 在Neo4j中存储为**节点属性**

---

### 3.3 Constraint(约束)

#### 3.3.1 理论定义

**Constraint(约束)** 是对类或属性的额外限制规则,确保数据质量。

**约束类型**:

| 类型 | 说明 | 值格式 | 法律领域示例 |
|------|------|--------|------|
| **CARDINALITY** | 基数约束 | `min:max` | `1:100`(一个案件至少1个当事人,最多100个) |
| **PATTERN** | 正则表达式 | Java Regex | `^（\d{4}）[\u4e00-\u9fa5]{2,6}民[初终]{1}\d{3,8}号$` (案件编号) |
| **RANGE** | 数值范围 | `min:max` | `0:10000000000`(争议金额0-100亿) |
| **ENUM** | 枚举约束 | JSON数组 | `["自然人", "法人", "非法人组织"]` (当事人类型) |
| **NOT_NULL** | 非空约束 | `true` | partyName(当事人姓名)必填 |
| **UNIQUE** | 唯一约束 | `true` | caseNumber(案件编号)不重复 |
| **LENGTH** | 长度约束 | `min:max` | `2:100`(当事人姓名2-100字符) |
| **CUSTOM_SPARQL** | 自定义SPARQL | SPARQL查询 | 高级验证逻辑 |

#### 3.3.2 数据模型

**关系数据库表** (支持MySQL/PostgreSQL): `ont_constraint`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| definition_id | BIGINT | 所属本体定义ID |
| class_id | BIGINT | 约束应用的类ID(可为空) |
| property_id | BIGINT | 约束应用的属性ID(可为空) |
| constraint_type | VARCHAR(32) | 约束类型 |
| value | TEXT | 约束值(JSON格式) |
| error_message | VARCHAR(512) | 用户友好的错误提示 |
| severity | VARCHAR(10) | 严重级别: `ERROR`/`WARNING`/`INFO` |
| description | TEXT | 约束的业务说明 |

#### 3.3.3 使用示例

**创建案件编号格式约束** (PATTERN) - 法律领域示例:
```bash
curl -X POST 'http://localhost:8080/api/v1/ontology/legal-kg/constraints' \
  -H 'Content-Type: application/json' \
  -d '{
    "propertyId": 201,
    "constraintType": "PATTERN",
    "value": "{\"pattern\": \"^（\\\\d{4}）[\\\\u4e00-\\\\u9fa5]{2,6}\\\\u6c11[\\\\u521d\\\\u7ec8]{1}\\\\d{3,8}号$\"}",
    "errorMessage": "案件编号格式错误,应为:(年份)法院简称+案件类型+编号,如(2022)沪0105民初21387号",
    "severity": "ERROR",
    "description": "案件编号必须符合中国法院标准格式"
  }'
```

**创建当事人类型枚举约束** (ENUM) - 法律领域示例:
```bash
curl -X POST 'http://localhost:8080/api/v1/ontology/legal-kg/constraints' \
  -H 'Content-Type: application/json' \
  -d '{
    "propertyId": 102,
    "constraintType": "ENUM",
    "value": "{\"allowed_values\": [\"自然人\", \"法人\", \"非法人组织\"]}",
    "errorMessage": "当事人类型必须是:自然人、法人或非法人组织",
    "severity": "ERROR",
    "description": "当事人类型枚举约束"
  }'
```

**法律领域常用约束定义**:

```sql
-- PostgreSQL 法律本体约束定义示例
INSERT INTO ont_constraint (id, definition_id, class_id, property_id, constraint_type, value, error_message, severity, description) VALUES
-- 1. 案件编号格式约束 (PATTERN)
(1, 1, 30, 201, 'PATTERN',
 '{"pattern": "^（\\\\d{4}）[\\\\u4e00-\\\\u9fa5]{2,6}\\\\u6c11[\\\\u521d\\\\u7ec8]{1}\\\\d{3,8}号$"}',
 '案件编号格式错误,应为:(年份)法院简称+案件类型+编号',
 'ERROR',
 '案件编号必须符合中国法院标准格式'),

-- 2. 当事人类型枚举约束 (ENUM)
(2, 1, 10, 102, 'ENUM',
 '{"allowed_values": ["自然人", "法人", "非法人组织"]}',
 '当事人类型必须是:自然人、法人或非法人组织',
 'ERROR',
 '当事人类型枚举约束'),

-- 3. 当事人角色枚举约束 (ENUM)
(3, 1, 10, 103, 'ENUM',
 '{"allowed_values": ["原告", "被告", "第三人", "上诉人", "被上诉人"]}',
 '当事人角色必须是:原告、被告、第三人、上诉人或被上诉人',
 'ERROR',
 '当事人诉讼角色枚举约束'),

-- 4. 法院级别枚举约束 (ENUM)
(4, 1, 20, 302, 'ENUM',
 '{"allowed_values": ["最高人民法院", "高级人民法院", "中级人民法院", "基层人民法院", "专门法院"]}',
 '法院级别必须是:最高人民法院、高级人民法院、中级人民法院、基层人民法院或专门法院',
 'ERROR',
 '中国法院五级体系约束'),

-- 5. 争议金额范围约束 (RANGE)
(6, 1, 30, NULL, 'RANGE',
 '{"property": "amountInDispute", "min": 0, "max": 10000000000}',
 '争议金额必须在 0 到 100亿元之间',
 'WARNING',
 '案件争议金额合理范围约束'),

-- 6. 当事人姓名长度约束 (LENGTH)
(7, 1, 10, 101, 'LENGTH',
 '{"min": 2, "max": 100}',
 '当事人姓名长度必须在 2 到 100 个字符之间',
 'ERROR',
 '当事人姓名长度约束'),

-- 7. 身份证号格式约束 (PATTERN)
(9, 1, 10, NULL, 'PATTERN',
 '{"property": "idNumber", "pattern": "^(\\\\d{15}|\\\\d{18}|\\\\d{17}X)$"}',
 '身份证号码格式错误,应为15位或18位',
 'ERROR',
 '中国大陆身份证号码格式约束');
```

**约束验证流程示例** (Java 伪代码):

```java
// Java 后端法律约束验证流程示例
public ValidationResult validateParty(Map<String, Object> properties) {
    // 1. 根据 entity.type="Party" 找到 ont_class.id=10
    OntClassDO partyClass = classMapper.findByLocalName("Party");
    
    // 2. 查询该类的所有约束
    List<OntConstraintDO> constraints = constraintMapper.findByClassId(partyClass.getId());
    
    // 3. 逐条验证
    List<ValidationError> errors = new ArrayList<>();
    
    // 验证 partyType 枚举约束
    if (!List.of("自然人", "法人", "非法人组织").contains(properties.get("partyType"))) {
        errors.add(new ValidationError(
            "ONT004", 
            "当事人类型必须是:自然人、法人或非法人组织",
            "partyType",
            properties.get("partyType")
        ));
    }
    
    // 验证 idNumber 格式约束
    String idNumber = (String) properties.get("idNumber");
    if (idNumber != null && !idNumber.matches("^(\\d{15}|\\d{18}|\\d{17}X)$")) {
        errors.add(new ValidationError(
            "ONT004",
            "身份证号码格式错误,应为15位或18位",
            "idNumber",
            idNumber
        ));
    }
    
    // 返回验证结果
    return errors.isEmpty() ? ValidationResult.pass() : ValidationResult.fail(errors);
}
```

---

### 3.4 Relationship/Edge(关系/边)

#### 3.4.1 为什么需要单独讲解Edge?

**关键区别**: 本体属性(OntProperty)和图数据库边(Edge)是**不同层次**的概念:

| 维度 | OntProperty(本体属性) | Edge(图数据库边) |
|------|---------------------|------------------|
| **层次** | Schema层(元数据) | Data层(实例数据) |
| **存储** | 关系数据库 `ont_property`表 | Neo4j 边关系 |
| **作用** | **定义**边的类型和约束 | **存储**具体的关系实例 |
| **数量** | 几十到几百个定义 | 数千到数百万条实例 |
| **类比** | 数据库表结构定义 | 数据库表中的记录 |

**映射关系** (法律领域示例):
```
OntProperty (定义)
   ↓ 指导创建
Edge (实例)

示例:
OntProperty: CASE_PARTY (定义域:Party, 值域:Case)
   ↓
Edge: (徐某骥)-[CASE_PARTY {role: "原告"}]->(公司解散纠纷案)
      {validAt: "2022-11-15", invalidAt: null}
```

#### 3.4.2 对象属性→边的转换

**规则**: 只有 `OBJECT` 类型的OntProperty才会在Neo4j中存储为Edge

**完整流程** (法律领域示例):
```
1. 定义本体属性 (关系数据库)
   POST /ontology/legal-kg/properties
   {
     "localName": "CASE_PARTY",
     "propertyType": "OBJECT",
     "domainClassId": 10,  // Party
     "rangeClassId": 30    // Case
   }

2. 创建实体节点 (Neo4j)
   CREATE (p:Entity {type: "Party", name: "徐某骥", partyName: "徐某骥", partyRole: "原告"})
   CREATE (c:Entity {type: "Case", name: "公司解散纠纷案", caseNumber: "（2022）沪0105民初21387号"})

3. 创建关系边 (Neo4j) - 受本体约束
   CREATE (p)-[r:RELATES_TO {
     uuid: "rel-party-case-001",
     type: "CASE_PARTY",
     fact: "徐某骥作为原告提起公司解散纠纷诉讼",
     role: "原告",
     valid_at: timestamp('2022-11-15'),
     invalid_at: null
   }]->(c)

4. 验证边是否符合本体
   - source节点type必须是Party(domain)
   - target节点type必须是Case(range)
   - 边类型CASE_PARTY必须在ont_property中定义
```
```

#### 3.4.3 边的验证机制

**6层验证引擎对边的验证**:

```java
// OntologyValidationServiceImpl.java
public ValidationResultVO validateEdge(String graphId, String edgeType, 
                                        Map<String, Object> properties) {
    // Layer 1: 边类型存在性
    OntPropertyDO edgeDef = findPropertyByLocalName(defId, edgeType);
    if (edgeDef == null) {
        // 边类型未定义 → 允许通过但给出警告(向后兼容)
        warnings.add("边类型未在本体中定义: " + edgeType);
        return ValidationResultVO.passWithWarnings(warnings);
    }
    
    // Layer 2-4: 验证边的属性(如validAt、fact等)
    errors.addAll(checkRequiredProperties(allProps, properties));
    errors.addAll(checkDataTypes(allProps, properties));
    errors.addAll(checkConstraints(defId, edgeDef, properties));
    
    return errors.isEmpty() ? ValidationResultVO.pass() 
                            : ValidationResultVO.fail(4, errors);
}
```

**边的常见属性** (法律关系边示例):
```json
{
  "uuid": "rel-party-case-001",
  "type": "CASE_PARTY",
  "fact": "徐某骥作为原告提起公司解散纠纷诉讼",
  "role": "原告",
  "valid_at": 1668470400000,
  "invalid_at": null,
  "embedding": [0.1, 0.2, ...]
}
```

#### 3.4.4 代码示例:创建符合本体的法律关系边

**Java代码** (法律领域示例):
```java
// 1. 先验证边是否符合本体
ValidationResultVO validation = ontologyValidationService.validateEdge(
    graphId, 
    "CASE_PARTY", 
    Map.of("fact", "徐某骥作为原告提起公司解散纠纷诉讼", "role", "原告")
);

if (!validation.isPassed()) {
    throw new OntologyValidationException(validation.getErrors());
}

// 2. 在Neo4j中创建法律关系边
String cypher = """
    MATCH (source:Entity {uuid: $sourceUuid})
    MATCH (target:Entity {uuid: $targetUuid})
    CREATE (source)-[r:RELATES_TO {
        uuid: $edgeUuid,
        type: 'CASE_PARTY',
        fact: $fact,
        role: $role,
        valid_at: $validAt,
        invalid_at: null,
        graph_id: $graphId
    }]->(target)
    """;

neo4jSession.run(cypher, parameters);
```

**法律领域关系边完整示例** (Cypher):

```cypher
// 1. 案件当事人关系 (CASE_PARTY)
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

// 2. 案件法院关系 (CASE_COURT)
(case:Entity {uuid: "case-001", name: "公司解散纠纷案"})
-[:RELATES_TO {
    uuid: "rel-case-court-001",
    type: "CASE_COURT",
    fact: "上海市长宁区人民法院审理此案",
    courtRole: "一审法院",
    valid_at: 1668470400000,
    invalid_at: null
}]->
(court:Entity {uuid: "court-002", name: "上海市长宁区人民法院", type: "Court"})

// 3. 案件法律条文关系 (CASE_LEGAL_BASIS)
(case:Entity {uuid: "case-001"})
-[:RELATES_TO {
    uuid: "rel-case-law-001",
    type: "CASE_LEGAL_BASIS",
    fact: "案件适用《中华人民共和国公司法》第182条",
    applicableProvision: "《公司法》第182条",
    valid_at: 1668470400000,
    invalid_at: null
}]->
(law:Entity {
    uuid: "law-001", 
    name: "公司法第182条", 
    type: "LegalProvision",
    lawName: "中华人民共和国公司法",
    articleNumber: "第182条"
})

// 4. 上诉关系时序示例 (APPEALED_CASE)
(case1:Entity {uuid: "case-001", name: "公司解散纠纷案一审"})
-[:RELATES_TO {
    uuid: "rel-appeal-001",
    type: "APPEALED_CASE",
    fact: "徐某骥不服一审判决,提起上诉",
    appealDate: date('2023-01-20'),
    valid_at: 1674172800000,         // 2023-01-20
    invalid_at: 1698105600000        // 2023-10-24 二审判决后失效
}]->
(case2:Entity {
    uuid: "case-002", 
    name: "公司解散纠纷案二审",
    caseNumber: "（2023）沪01民终11293号"
})
```

**法律领域关系类型完整列表**:

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

**重要规则**:
- ✅ 边类型**未定义时允许通过**(向后兼容),但会记录警告
- ✅ 如果定义了边类型,则必须符合domain/range约束
- ✅ 边也支持时序管理(validAt/invalidAt)

---

### 3.5 Instance/Individual(实例)

#### 3.5.1 理论定义

**Instance(实例)** 是类的具体化,也称为Individual(个体)。

**OWL语义** (法律领域示例):
```
Class: Party
Instance: 徐某骥 (type: Party, partyRole: 原告)
Instance: 上海某物业管理有限公司 (type: Party, partyRole: 被告)

Class: Case
Instance: 公司解散纠纷案 (type: Case, caseNumber: （2022）沪0105民初21387号)
```

**在Graphiti-Java中的实现**:
- 实例存储在**Neo4j**中,而不是关系数据库
- 实例节点使用 `type` 字段关联到OntClass的 `local_name`

#### 3.5.2 Entity节点结构

**Neo4j Entity节点** (法律领域示例):
```json
{
  "uuid": "party-001",
  "graph_id": "legal-knowledge-graph",
  "name": "徐某骥",
  "type": "Party",  ← 对应 ont_class.local_name = "Party"
  "partyName": "徐某骥",
  "partyType": "自然人",
  "partyRole": "原告",
  "idNumber": "310105199001011234",
  "summary": "案件原告方,自然人当事人",
  "embedding": [0.1, 0.2, ...],
  "valid_at": 1668470400000,
  "invalid_at": null
}
```

**法律实体节点类型示例**:
```
Party (当事人): 徐某骥、上海某物业管理有限公司
Case (案件): 公司解散纠纷案、上诉案
Court (法院): 上海市长宁区人民法院、上海市第一中级人民法院
Judge (法官): 张某法官
LegalProvision (法律条文): 公司法第182条
Evidence (证据): 财务报表、审计报告
JudgmentDocument (裁判文书): 一审判决书、二审判决书
```

**与本体类的映射**:
```
Neo4j Entity.type = "Person"
   ↓ 查找
关系数据库 ont_class WHERE local_name = "Person" AND definition_id = ?
   ↓ 获取
本体定义: 父类、属性、约束
   ↓ 验证
Entity.properties 是否符合本体定义
```

#### 3.5.3 实例验证流程

```java
// 创建法律实体节点时的验证 (Java 代码示例)
public void createPartyNode(String graphId, NodeCreateReqVO req) {
    // 1. 根据type="Party"查找本体类定义
    OntClassDO classDef = classMapper.findByLocalName(graphId, "Party");
    if (classDef == null) {
        throw new OntologyValidationException("类型未定义: Party");
    }
    
    // 2. 执行6层验证
    ValidationResultVO result = validationService.validateNode(
        graphId, 
        "Party", 
        Map.of(
            "partyName", "徐某骥",
            "partyType", "自然人",
            "partyRole", "原告",
            "idNumber", "310105199001011234"
        )
    );
    
    if (!result.isPassed()) {
        // 错误示例: 
        // - partyType必须是:自然人、法人或非法人组织
        // - 身份证号码格式错误,应为15位或18位
        throw new OntologyValidationException(result.getErrors());
    }
    
    // 3. 验证通过,写入Neo4j
    graphNeo4jService.createEntityNode(graphId, req);
}
```

---

### 3.6 层次结构(Hierarchy)

#### 3.6.1 类层次(Class Hierarchy)

**构建树形结构**:
```java
public List<ClassHierarchyVO> getClassHierarchy(String graphId) {
    // 1. 查询所有根类(没有父类的类)
    List<OntClassDO> rootClasses = classMapper.selectRootClasses(defId);
    
    // 2. 递归构建子树
    return rootClasses.stream()
        .map(root -> buildClassHierarchy(root, allClasses))
        .collect(Collectors.toList());
}

private ClassHierarchyVO buildClassHierarchy(OntClassDO parent, 
                                              List<OntClassDO> allClasses) {
    ClassHierarchyVO node = new ClassHierarchyVO();
    node.setClassUri(parent.getClassUri());
    node.setLocalName(parent.getLocalName());
    
    // 查找所有直接子类
    List<OntClassDO> children = allClasses.stream()
        .filter(c -> parent.getId().equals(c.getParentClassId()))
        .collect(Collectors.toList());
    
    // 递归构建子节点
    node.setChildren(children.stream()
        .map(child -> buildClassHierarchy(child, allClasses))
        .collect(Collectors.toList()));
    
    return node;
}
```

**API响应示例** (法律领域):
```json
[
  {
    "classUri": "http://legal-ai.cc/ontology#LegalEntity",
    "localName": "LegalEntity",
    "description": "法律领域实体的顶层抽象类",
    "children": [
      {
        "classUri": "http://legal-ai.cc/ontology#Party",
        "localName": "Party",
        "description": "案件中的当事人",
        "children": [
          {
            "classUri": "http://legal-ai.cc/ontology#NaturalPerson",
            "localName": "NaturalPerson",
            "children": [
              {
                "classUri": "http://legal-ai.cc/ontology#Plaintiff",
                "localName": "Plaintiff",
                "description": "原告",
                "children": []
              },
              {
                "classUri": "http://legal-ai.cc/ontology#Defendant",
                "localName": "Defendant",
                "description": "被告",
                "children": []
              }
            ]
          },
          {
            "classUri": "http://legal-ai.cc/ontology#LegalPerson",
            "localName": "LegalPerson",
            "description": "法人当事人",
            "children": []
          }
        ]
      },
      {
        "classUri": "http://legal-ai.cc/ontology#Court",
        "localName": "Court",
        "description": "审判机关",
        "children": [
          {
            "classUri": "http://legal-ai.cc/ontology#SupremeCourt",
            "localName": "SupremeCourt",
            "children": []
          },
          {
            "classUri": "http://legal-ai.cc/ontology#LocalCourt",
            "localName": "LocalCourt",
            "children": []
          }
        ]
      },
      {
        "classUri": "http://legal-ai.cc/ontology#Case",
        "localName": "Case",
        "description": "法律诉讼案件",
        "children": [
          {
            "classUri": "http://legal-ai.cc/ontology#CivilCase",
            "localName": "CivilCase",
            "description": "民事案件",
            "children": []
          },
          {
            "classUri": "http://legal-ai.cc/ontology#CriminalCase",
            "localName": "CriminalCase",
            "description": "刑事案件",
            "children": []
          }
        ]
      }
    ]
  }
]
```

#### 3.6.2 属性层次(Property Hierarchy)

属性也支持父子关系,用于属性继承 (法律领域示例):

```
hasContactInfo (父属性 - 联系信息)
  ├─ hasEmail (子属性 - 邮箱)
  ├─ hasPhone (子属性 - 电话)
  └─ hasAddress (子属性 - 地址)

hasPartyName (父属性 - 当事人名称)
  ├─ hasNaturalPersonName (子属性 - 自然人姓名)
  └─ hasLegalPersonName (子属性 - 法人名称)
```

**查询属性祖先链**:
```java
public List<OntPropertyDO> collectPropertyAncestors(Long propertyId) {
    List<OntPropertyDO> ancestors = new ArrayList<>();
    Long currentId = propertyId;
    
    while (currentId != null) {
        OntPropertyDO prop = propertyMapper.selectById(currentId);
        if (prop != null) {
            ancestors.add(prop);
            currentId = prop.getParentPropertyId();
        } else {
            break;
        }
    }
    
    return ancestors;
}
```

---

### 3.7 数据模型与表结构

#### 3.7.1 ER关系图

```mermaid
erDiagram
    ONT_DEFINITION ||--o{ ONT_CLASS : "包含"
    ONT_DEFINITION ||--o{ ONT_PROPERTY : "包含"
    ONT_DEFINITION ||--o{ ONT_CONSTRAINT : "包含"
    ONT_DEFINITION ||--o{ ONT_VERSION_HISTORY : "记录"
    
    ONT_CLASS ||--o{ ONT_CLASS : "父子继承"
    ONT_PROPERTY ||--o{ ONT_PROPERTY : "父子继承"
    
    ONT_CLASS ||--o{ ONT_PROPERTY : "定义域"
    ONT_CLASS ||--o{ ONT_PROPERTY : "值域"
    
    ONT_CLASS ||--o{ ONT_CONSTRAINT : "应用"
    ONT_PROPERTY ||--o{ ONT_CONSTRAINT : "应用"
    
    ONT_DEFINITION {
        bigint id PK
        varchar graph_id
        varchar namespace
        varchar version
        varchar status
    }
    
    ONT_CLASS {
        bigint id PK
        bigint definition_id FK
        varchar class_uri
        varchar local_name
        bigint parent_class_id FK
        text equivalent_to
        text disjoint_with
    }
    
    ONT_PROPERTY {
        bigint id PK
        bigint definition_id FK
        varchar property_uri
        varchar property_type
        bigint domain_class_id FK
        bigint range_class_id FK
        varchar range_data_type
        boolean is_required
    }
    
    ONT_CONSTRAINT {
        bigint id PK
        bigint definition_id FK
        bigint class_id FK
        bigint property_id FK
        varchar constraint_type
        text value
    }
```

#### 3.7.2 核心表清单

| 表名 | 用途 | 关键字段数 |
|------|------|-----------|
| `ont_definition` | 本体定义容器 | 10 |
| `ont_class` | 类定义 | 13 |
| `ont_property` | 属性定义 | 24 |
| `ont_constraint` | 约束规则 | 10 |
| `ont_version_history` | 版本历史 | 11 |
| `ont_class_inheritance` | 多继承关系 | 6 |
| `ont_mapping` | 本体映射 | 8 |
| `ont_draft` | LLM生成草稿 | 10 |

---

## 第四章 知识图谱核心概念

> **本章定位**: 深入讲解知识图谱中除本体外的其他核心概念,包括Episode、Entity、Edge、Community、Temporal Graph等,它们是本体概念在数据层的具体实现。

### 4.1 Episode(剧集/事件)

#### 4.1.1 定义与作用

**Episode**是知识图谱中的**原始数据容器**,是知识抽取的起点。

**核心作用**:
1. **存储原始文本**: 新闻报道、法律条文、对话记录、会议纪等
2. **保留上下文**: 完整保存数据的原始语境,支持溯源
3. **知识抽取源**: LLM从Episode中抽取Entity和Edge
4. **增量更新**: 新数据到来时,只处理新Episode,不影响已有数据

**类比理解** (法律领域示例):
- **Episode** = 裁判文书、法律条文、合同文本
- **Entity** = 从文书中提取的当事人(徐某骥)、公司(上海某物业管理有限公司)、法院(上海市长宁区人民法院)
- **Edge** = 从文书中提取的关系(CASE_PARTY、CASE_COURT、CASE_LEGAL_BASIS)

#### 4.1.2 EpisodeType元数据分类

**EpisodeType**定义Episode的业务分类体系 (法律领域示例):

```
裁判文书 (JUDGMENT_DOCUMENT)
├─ 民事判决
│  ├─ 一审民事判决
│  ├─ 二审民事判决
│  └─ 再审民事判决
├─ 刑事判决
│  ├─ 一审刑事判决
│  └─ 二审刑事判决
└─ 行政判决
   ├─ 一审行政判决
   └─ 二审行政判决

法律条文 (LEGAL_PROVISION)
├─ 法律
│  ├─ 公司法
│  ├─ 民法典
│  └─ 刑法
├─ 司法解释
│  ├─ 公司法司法解释
│  └─ 民法典司法解释
└─ 地方性法规

案件材料 (CASE_MATERIAL)
├─ 起诉状
├─ 答辩状
├─ 证据材料
└─ 庭审笔录
```

**法律领域EpisodeType定义示例**:
```sql
INSERT INTO episode_type (id, name, local_name, parent_id, description, domain_hint, status) VALUES
(1, '裁判文书', 'JUDGMENT_DOCUMENT', NULL, '法院作出的裁判文书', 'LEGAL', 'ACTIVE'),
(2, '民事判决', 'CIVIL_JUDGMENT', 1, '民事案件的一审或二审判决', 'LEGAL', 'ACTIVE'),
(3, '一审民事判决', 'FIRST_INSTANCE_CIVIL', 2, '基层或中级人民法院作出的一审民事判决', 'LEGAL', 'ACTIVE'),
(4, '二审民事判决', 'SECOND_INSTANCE_CIVIL', 2, '高级人民法院作出的二审民事判决', 'LEGAL', 'ACTIVE'),
(10, '法律条文', 'LEGAL_PROVISION', NULL, '法律法规条文', 'LEGAL', 'ACTIVE'),
(11, '法律', 'LAW', 10, '全国人大及其常委会制定的法律', 'LEGAL', 'ACTIVE'),
(20, '案件材料', 'CASE_MATERIAL', NULL, '案件相关的起诉状、答辩状、证据等', 'LEGAL', 'ACTIVE'),
(21, '起诉状', 'COMPLAINT', 20, '原告提交的起诉状', 'LEGAL', 'ACTIVE');
```

**代码中的体现**:
```java
// CommunityServiceImpl.java
String episodeType = (String) episodeData.get("episode_type");
String legalProcess = (String) episodeData.get("legal_process");
String stageLabel = (String) episodeData.get("stage_label");
```

#### 4.1.3 Episode数据结构

**示例** (法律裁判文书):
```json
{
  "uuid": "episode-001",
  "graph_id": "legal-knowledge-graph",
  "name": "徐某骥与上海某物业管理有限公司公司解散纠纷案一审判决书",
  "source": "COURT_JUDGMENT",
  "content": "上海市长宁区人民法院民事判决书（2022）沪0105民初21387号。原告徐某骥诉被告上海某物业管理有限公司公司解散纠纷一案...",
  "episode_type": "FIRST_INSTANCE_CIVIL",
  "legal_process": "司法程序",
  "stage_label": "一审判决",
  "case_number": "（2022）沪0105民初21387号",
  "court_name": "上海市长宁区人民法院",
  "judgment_date": "2023-10-24",
  "processed": true,
  "processed_at": "2024-01-15T10:00:00Z",
  "created_at": "2024-01-15T09:00:00Z"
}
```

**关键字段**:
- `uuid`: Episode唯一标识
- `source`: 数据来源(COURT_JUDGMENT/LEGAL_PROVISION/CONTRACT等)
- `content`: 裁判文书原文或法律条文原文
- `episode_type`: 业务分类(对应episode_type.local_name)
- `case_number`: 案件编号(法律领域特有)
- `court_name`: 法院名称(法律领域特有)
- `processed`: 是否已被LLM处理(true=已抽取实体和关系)
- `processed_at`: LLM处理时间

#### 4.1.4 Episode与Entity/Edge的关系

**法律领域示例**:
```
Episode (一审裁判文书)
   │
   ├─[LLM抽取]→ Entity: 徐某骥 (type: Party, partyRole: 原告)
   │              Entity: 上海某物业管理有限公司 (type: Party, partyRole: 被告)
   │              Entity: 上海市长宁区人民法院 (type: Court)
   │              Entity: 公司解散纠纷案 (type: Case)
   │              Entity: 公司法第182条 (type: LegalProvision)
   │
   ├─[LLM抽取]→ Edge: (徐某骥)-[CASE_PARTY]->(公司解散纠纷案)
   │              (公司解散纠纷案)-[CASE_COURT]->(上海市长宁区人民法院)
   │              (公司解散纠纷案)-[CASE_LEGAL_BASIS]->(公司法第182条)
   │
   └─[MENTIONS关系]→ Episode提及了上述所有Entity和Edge
```

**为什么需要Episode?** (法律领域视角)
- **可追溯性**: 知道某个当事人实体是从哪份裁判文书中提取的
- **质量评估**: 可以对比一审和二审文书抽取结果的差异
- **上下文保留**: 裁判文书原文永远保存,支持重新抽取或补充抽取
- **时效性管理**: 一审判决被二审改判后,可以通过invalid_at标记失效

---

### 4.2 Entity(实体节点)

#### 4.2.1 定义

**Entity**是知识图谱中的基本节点,代表现实世界中的对象。

**类型示例** (法律领域):
```
Party (当事人): 徐某骥、上海某物业管理有限公司、第三人张某
Court (法院): 上海市长宁区人民法院、上海市第一中级人民法院、最高人民法院
Case (案件): 公司解散纠纷案、上诉案、再审案
Judge (法官): 张某审判长、李某审判员
LegalProvision (法律条文): 《公司法》第182条、《民法典》第580条
Evidence (证据): 财务报表、审计报告、证人证言
JudgmentDocument (裁判文书): 一审判决书、二审判决书、再审裁定书
```

#### 4.2.2 Entity属性结构

**Neo4j存储** (法律当事人实体):
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
  "summary": "案件原告方,自然人当事人,持有公司10%股权",
  "embedding": [0.1, 0.2, ...],
  "valid_at": 1668470400000,
  "invalid_at": null,
  "created_at": "2024-01-15T10:00:00Z",
  "updated_at": "2024-01-15T10:00:00Z"
}
```

**法律法院实体示例**:
```json
{
  "uuid": "court-002",
  "graph_id": "legal-knowledge-graph",
  "name": "上海市长宁区人民法院",
  "type": "Court",
  "courtName": "上海市长宁区人民法院",
  "courtLevel": "基层人民法院",
  "summary": "本案一审法院,作出(2022)沪0105民初21387号民事判决",
  "embedding": [0.2, 0.3, ...],
  "valid_at": 1668470400000,
  "invalid_at": null
}
```

**关键字段**:
- `uuid`: 实体唯一标识
- `type`: 实体类型(对应OntClass.localName,如Party/Court/Case)
- `summary`: 实体摘要(LLM从裁判文书中生成)
- `embedding`: 向量嵌入(用于语义搜索相似案件)
- `valid_at/invalid_at`: 时序管理(如一审判决被二审改判后失效)
- 领域属性: partyName/partyRole/courtName/courtLevel等(直接作为节点属性)

#### 4.2.3 Entity与OntClass的映射

```
Neo4j Entity.type = "Party"
   ↓ 查找
关系数据库 ont_class WHERE local_name = "Party" AND graph_id = 'legal-knowledge-graph'
   ↓ 获取
本体定义: 父类(LegalEntity)、属性(partyName/partyType/partyRole)、约束
   ↓ 验证
Entity的属性是否符合本体定义(如partyRole必须是:原告/被告/第三人)
```

---

### 4.3 Edge/Relationship(关系边)

#### 4.3.1 定义

**Edge**是连接两个实体的有向关系。

**核心属性** (法律关系边示例):
```json
{
  "uuid": "rel-party-case-001",
  "graph_id": "legal-knowledge-graph",
  "source_node_uuid": "party-001",
  "target_node_uuid": "case-001",
  "type": "CASE_PARTY",
  "fact": "徐某骥作为原告提起公司解散纠纷诉讼",
  "role": "原告",
  "embedding": [0.3, 0.4, ...],
  "valid_at": 1668470400000,
  "invalid_at": null,
  "created_at": "2024-01-15T10:00:00Z"
}
```

**法律法院关系边示例**:
```json
{
  "uuid": "rel-case-court-001",
  "graph_id": "legal-knowledge-graph",
  "source_node_uuid": "case-001",
  "target_node_uuid": "court-002",
  "type": "CASE_COURT",
  "fact": "上海市长宁区人民法院审理此案",
  "courtRole": "一审法院",
  "valid_at": 1668470400000,
  "invalid_at": null
}
```

#### 4.3.2 关系类型 (法律领域)

| 关系类型 | 源实体类型 | 目标实体类型 | 说明 | 法律示例 |
|---------|----------|------------|------|------|
| `CASE_PARTY` | Party | Case | 当事人参与案件 | 原告、被告、第三人 |
| `CASE_COURT` | Case | Court | 案件由某法院审理 | 一审法院、二审法院 |
| `CASE_JUDGE` | Case | Judge | 法官审理案件 | 审判长、审判员 |
| `CASE_LEGAL_BASIS` | Case | LegalProvision | 案件适用某法条 | 《公司法》第182条 |
| `CASE_EVIDENCE` | Case | Evidence | 案件证据 | 书证、物证、证人证言 |
| `APPEALED_CASE` | Case | Case | 上诉关系 | 一审→二审 |
| `MENTIONS` | Episode | Entity/Edge | Episode提及实体/关系 | 裁判文书→当事人 |
| `HAS_MEMBER` | Community | Entity | 社区包含成员 | 案件社区→当事人 |

---

### 4.4 Community(社区)

#### 4.4.1 定义与作用

**Community**是通过算法自动发现的紧密连接的实体群组。

**与手动分类的区别**:

| 特征 | 手动分类(OntClass) | 自动发现(Community) |
|------|-------------------|---------------------|
| 创建方式 | 人工定义 | 算法检测 |
| 示例 | "所有Person类节点" | "经常一起出现的人形成的朋友圈" |
| 更新频率 | 低 | 高(数据变化后重新检测) |
| 用途 | 类型约束 | 知识发现 |

#### 4.4.2 社区元数据

**分类维度** (法律领域示例):
```java
domain_type: "法律"                    // 领域类型
community_type: "案件簇"               // 社区类型(公司法案件簇/劳动争议案件簇)
region: "REGION_SHANGHAI"              // 地域(上海地区)
scenario_type: "公司解散纠纷"           // 场景类型
legal_process: "司法程序"               // 法律流程
court_level: "基层人民法院"             // 法院级别
```

**法律社区元数据示例**:
```sql
INSERT INTO community (id, graph_id, name, summary, domain_type, community_type, region, scenario_type) VALUES
(1, 'legal-knowledge-graph', '上海地区公司解散纠纷案件社区', 
 '包含上海地区2022-2024年公司解散纠纷相关案件、当事人、法院和法律条文',
 '法律', '案件簇', 'REGION_SHANGHAI', '公司解散纠纷'),
(2, 'legal-knowledge-graph', '劳动合同法相关实体社区',
 '包含劳动合同法相关法条、劳动争议案例、劳动监察部门',
 '法律', '法规簇', 'REGION_CN', '劳动争议');
```

#### 4.4.3 实际应用场景

**法律知识图谱示例** (基于真实案例):
```
┌─────────────────────────────────────────────────────────┐
│ 社区1: "上海地区公司解散纠纷案件社区"                     │
│  - community_id: community-001                          │
│  - 节点数: 56                                           │
│  - 案件: 公司解散纠纷案一审、二审                         │
│  - 当事人: 徐某骥(原告)、上海某物业管理有限公司(被告)     │
│  - 法院: 上海市长宁区人民法院、上海市第一中级人民法院     │
│  - 法条: 《公司法》第182条、《民法典》第580条            │
│  - 法官: 张某审判长、李某审判员                          │
│  - 证据: 财务报表、审计报告、股东会决议                   │
├─────────────────────────────────────────────────────────┤
│ 社区2: "公司法司法解释相关实体社区"                       │
│  - community_id: community-002                          │
│  - 节点数: 128                                          │
│  - 法条: 《公司法》及司法解释(一)(二)(三)(四)(五)        │
│  - 案例: 85个公司治理相关案例                            │
│  - 法院: 上海地区各级人民法院                            │
│  - 主题: 股东权利、公司决议效力、董事责任                 │
├─────────────────────────────────────────────────────────┤
│ 社区3: "劳动争议案件社区"                                │
│  - community_id: community-003                          │
│  - 节点数: 234                                          │
│  - 法条: 《劳动合同法》《劳动争议调解仲裁法》            │
│  - 案例: 156个劳动争议案例                               │
│  - 机构: 劳动监察部门、劳动仲裁委员会                    │
│  - 当事人: 劳动者、用人单位                              │
```

**用途** (法律领域视角):
- **类案检索**: 浏览"公司解散纠纷"社区,快速找到相似案件和适用法条
- **知识发现**: 发现同一法官审理的多个案件,分析裁判倾向
- **法律推荐**: 浏览某案件时,推荐相关的司法解释和类似案例
- **裁判规律分析**: 通过社区发现同一法院对某类案件的裁判标准

---

### 4.5 Temporal Graph(时序图谱)

#### 4.5.1 时间维度管理

所有节点和边使用**双时态设计**:

```
valid_at:    Timestamp when this fact became true
invalid_at:  Timestamp when this fact became false (null = currently true)
```

**操作规则**:
- **Insert**: Write with `valid_at = NOW()`, `invalid_at = null`
- **Update**: Set `invalid_at = NOW()` on old record, insert new record with new `valid_at`
- **Query valid state at time T**: `WHERE valid_at <= T AND (invalid_at > T OR invalid_at IS NULL)`

#### 4.5.2 时序示例

**法律案件上诉时序示例**:
```
2022-11-15: (徐某骥)-[CASE_PARTY {role: "原告", valid_at: 2022-11-15, invalid_at: null}]->(公司解散纠纷案一审)
            (公司解散纠纷案一审)-[CASE_COURT {courtRole: "一审法院", valid_at: 2022-11-15, invalid_at: null}]->(上海市长宁区人民法院)

2023-10-24: 一审判决后,部分关系失效
            (公司解散纠纷案一审)-[CASE_COURT {courtRole: "一审法院", valid_at: 2022-11-15, invalid_at: 2023-10-24}]->(上海市长宁区人民法院)

2023-11-10: 提起上诉,创建二审关系
            (公司解散纠纷案一审)-[APPEALED_CASE {valid_at: 2023-11-10, invalid_at: null}]->(公司解散纠纷案二审)
            (公司解散纠纷案二审)-[CASE_COURT {courtRole: "二审法院", valid_at: 2023-11-10, invalid_at: null}]->(上海市第一中级人民法院)

2024-03-15: 二审判决维持原判
            (公司解散纠纷案二审)-[CASE_COURT {courtRole: "二审法院", valid_at: 2023-11-10, invalid_at: 2024-03-15}]->(上海市第一中级人民法院)
```

**查询指定时间的案件状态** (Cypher):
```cypher
// 查询2023-12-01时,徐某骥的案件状态
MATCH (party:Party {partyName: "徐某骥"})
-[:CASE_PARTY {valid_at: <= 1701360000000, invalid_at: > 1701360000000 OR invalid_at IS NULL}]->
(case:Case)
RETURN case.caseNumber, case.caseName
```

**查询一审判决时徐某骥的诉讼地位** (Cypher):
```cypher
// 查询2023-05-01(一审期间),徐某骥的案件角色
MATCH (party:Party {partyName: "徐某骥"})
-[:CASE_PARTY {valid_at: <= 1682899200000, invalid_at: > 1682899200000 OR invalid_at IS NULL}]->
(case:Case)
WHERE case.caseNumber = '（2022）沪0105民初21387号'
RETURN party.partyRole, case.caseName
// 输出: 原告 | 公司解散纠纷案一审
```

#### 4.5.3 时序在法律领域的应用

- **案件进展追溯**: 追踪案件从立案、一审、二审到执行的全过程
- **法条时效性**: 某法条何时生效、何时被修订或废止
- **裁判标准演变**: 同一法院对某类案件的裁判标准随时间的变化
- **当事人状态管理**: 当事人诉讼地位变化(原告→上诉人→被上诉人)

---

### 4.6 GraphMetadata(图谱元数据)

**定义**: 知识图谱实例的基本信息

```json
{
  "graphId": "legal-kg-2024",
  "name": "法律知识图谱2024",
  "description": "包含中国民商事法律的知识图谱",
  "nodeCount": 15678,
  "edgeCount": 45234,
  "episodeCount": 892,
  "communityCount": 156,
  "classCount": 45,
  "ontVersionId": 12,
  "status": "ACTIVE",
  "createdAt": "2024-01-01",
  "updatedAt": "2024-03-15"
}
```

---

### 4.7 概念间的关系图

```mermaid
graph TB
    A[Ontology本体] -->|定义类型| B[Entity实体]
    A -->|定义约束| C[Edge关系]
    
    D[Episode事件] -->|LLM抽取| B
    D -->|LLM抽取| C
    D -->|MENTIONS| B
    
    B -->|RELATES_TO| C
    B -->|IN_COMMUNITY| E[Community社区]
    
    F[GraphMetadata元数据] -->|统计| B
    F -->|统计| C
    F -->|统计| D
    F -->|统计| E
    
    G[Temporal时间] -.->|validAt/invalidAt| B
    G -.->|validAt/invalidAt| C
    
    H[Reasoning推理] -->|类型推断| B
    H -->|一致性检查| A
    
    I[Validation验证] -->|数据质量| B
    I -->|本体验证| A
```

---

## 第五章 社区检测功能详解

> **本章定位**: 深入讲解社区检测算法的原理、Graphiti-Java的实现机制、以及最佳实践。

### 5.1 社区检测算法概述

**社区检测(Community Detection)** 是图论中的经典算法,用于**自动发现图中紧密连接的节点群组**。

**通俗理解** (法律领域示例):
- 法律知识图谱中,找出"公司解散纠纷案件簇"(相关案件、当事人、法院、法条聚集在一起)
- 法律条文网络中,找出"民法典合同编相关法条簇"(互相引用的法条聚集在一起)
- 裁判文书网络中,找出"同一法官审理的类似案件簇"(裁判标准一致的案件)

### 5.2 标签传播算法(LPA)原理

#### 5.2.1 算法流程

```
初始状态:每个节点都有自己唯一的标签(社区ID)

迭代过程:
  对于每个节点:
    查看邻居节点的标签
    选择出现频率最高的标签
    将自己的标签更新为该标签

收敛条件:
  - 标签变化 < 0.1% 的节点
  - 或达到最大迭代次数(100次)

最终结果:
  具有相同标签的节点属于同一个社区
```

#### 5.2.2 算法特点

| 特点 | 说明 |
|------|------|
| **计算复杂度** | 接近线性 O(n log n) |
| **无需预设社区数** | 自动发现社区数量 |
| **适合大规模图** | 可处理百万级节点 |
| **可能不稳定** | 不同运行可能得到不同结果 |

### 5.3 Graphiti-Java的实现机制

#### 5.3.1 完整流程

```java
// CommunityServiceImpl.java
public CommunityDetectionResult detectCommunities(String graphId) {
    // 1. 删除旧社区
    removeCommunities(graphId);
    
    // 2. 从Neo4j提取图结构
    String cypher = 
        "MATCH (a:Entity {graph_id: $graph_id})-[r:RELATES_TO]->(b:Entity {graph_id: $graph_id}) " +
        "WHERE r.invalid_at IS NULL " +  // 时序过滤:只考虑当前有效的关系
        "RETURN a.uuid as source, b.uuid as target, count(r) as edge_count";
    
    // 3. 构建内存中的图
    LabelPropagation.Graph graph = new LabelPropagation.Graph();
    // ... 添加边 ...
    
    // 4. 执行标签传播算法
    LabelPropagation.CommunityResult result = LabelPropagation.detect(graph);
    
    // 5. 并行构建社区节点(最多10个并发)
    List<CompletableFuture<CommunityBuildResult>> futures = new ArrayList<>();
    for (Map.Entry<String, Set<String>> community : result.getCommunities().entrySet()) {
        if (community.getValue().size() >= 2) {  // 至少2个成员
            futures.add(CompletableFuture.supplyAsync(() -> 
                buildCommunityNode(graphId, community)
            ));
        }
    }
    
    // 6. 等待所有社区构建完成
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    
    return buildResult();
}
```

#### 5.3.2 LLM智能命名

**传统算法**: 社区只是数字ID(Community-1, Community-2)

**Graphiti-Java**: 使用LLM理解内容,生成有意义的名称

```java
// 获取社区内所有节点的摘要
List<String> summaries = getMemberSummaries(graphId, memberUuids);

// LLM 合并摘要(二叉树方式高效处理大量文本)
String mergedSummary = summarizer.summarize(summaries);

// LLM 生成社区名称
String communityName = summarizer.generateCommunityName(mergedSummary);
// 例如:"三国时期蜀汉将领群体"、"电子商务支付系统相关实体"
```

### 5.4 技术特点

#### 5.4.1 全量重算机制

```java
removeCommunities(graphId);  // 先删除旧社区
// 重新检测...
```

**现状**: 每次执行都重新计算,不是增量更新

**原因**:
- 保证社区一致性
- 避免增量更新的复杂性

**优化建议**: 对于大图谱(>10万节点),可考虑增量更新

#### 5.4.2 并行处理

```java
List<CompletableFuture<CommunityBuildResult>> futures = new ArrayList<>();
// 最多10个社区并发构建(使用LLM)
```

**优势**:
- LLM调用耗时较长(每个社区2-5秒)
- 并行处理可提升5-10倍速度

#### 5.4.3 自动命名

**示例** (法律领域):
```java
// 获取社区内所有节点的摘要
List<String> summaries = getMemberSummaries(graphId, memberUuids);
// 例如: ["徐某骥,案件原告,持有公司10%股权", "上海某物业管理有限公司,案件被告", 
//        "公司解散纠纷案,案号(2022)沪0105民初21387号", "《公司法》第182条,公司僵局救济"]

// LLM 合并摘要(二叉树方式高效处理大量文本)
String mergedSummary = summarizer.summarize(summaries);
// 输出: "上海地区公司解散纠纷相关实体,包含原告徐某骥、被告上海某物业管理有限公司、审理法院上海市长宁区人民法院及适用法条《公司法》第182条"

// LLM 生成社区名称
String communityName = summarizer.generateCommunityName(mergedSummary);
// 例如:"上海地区公司解散纠纷案件簇"、"《公司法》司法解释相关实体"、"劳动争议案件群体"
```

**法律社区命名示例**:
```
社区检测结果:
┌─────────────────────────────────────────────────┐
│ 社区: "上海地区公司解散纠纷案件簇"                │
│  - 案件: 公司解散纠纷案一审、二审                 │
│  - 当事人: 徐某骥(原告)、上海某物业管理有限公司  │
│  - 法院: 上海市长宁区法院、上海市第一中院         │
│  - 法条: 《公司法》第182条、《民法典》第580条    │
│  - 法官: 张某审判长、李某审判员                   │
├─────────────────────────────────────────────────┤
│ 社区: "公司治理相关法条簇"                        │
│  - 法条: 《公司法》第1-50条及相关司法解释         │
│  - 案例: 85个公司治理案例                         │
│  - 主题: 股东权利、公司决议效力、董事责任         │
├─────────────────────────────────────────────────┤
│ 社区: "劳动争议案件群体"                          │
│  - 案件: 156个劳动争议案例                        │
│  - 法条: 《劳动合同法》《劳动争议调解仲裁法》     │
│  - 机构: 劳动监察部门、劳动仲裁委员会             │
```

### 5.5 应用场景与最佳实践

#### 5.5.1 应用场景

| 场景 | 法律领域应用 |
|------|------|
| **法律知识图谱** | 发现"公司解散纠纷案件簇"、"劳动合同法相关实体簇" |
| **类案检索** | 自动发现相似案件群体,辅助法官裁判 |
| **法律推荐** | 浏览某案件时,推荐相关法条和类似案例 |
| **裁判规律分析** | 发现同一法院/法官对某类案件的裁判标准 |
| **推荐系统** | 基于社区推荐相关内容 |

#### 5.5.2 最佳实践

1. **定期执行**: 数据更新后重新检测社区
2. **社区大小**: 过滤成员数<2的社区
3. **人工审核**: LLM生成的名称可能需要调整
4. **可视化**: 社区检测结果用于图表着色

### 5.6 优化建议

1. **增量更新**: 当前是全量重算,对于大图谱会很慢
2. **算法选择**: 目前只用LPA,可以增加Louvain、Leiden等算法选项
3. **社区层级**: 当前是扁平的,可以支持社区树(大社区 → 子社区)
4. **时序社区**: 可以查看社区随时间的演变
5. **可视化集成**: 社区检测结果直接用于图表着色

---

## 第六章 系统功能特性

> **本章定位**: 详细讲解Graphiti-Java的核心功能特性,包括6层验证引擎、OWL推理、版本管理等。

### 6.1 6层验证引擎详解

#### 6.1.1 验证流程

```
┌─────────────────────────────────────────┐
│ 第1层: 类型存在性检查                    │
│ - 节点类型必须在OntClass中定义           │
│ - 边类型建议在OntProperty中定义          │
│ - 错误码: ONT001                        │
├─────────────────────────────────────────┤
│ 第2层: 必填属性检查                      │
│ - isRequired=true的属性必须非空          │
│ - 错误码: ONT002                        │
├─────────────────────────────────────────┤
│ 第3层: 数据类型检查                      │
│ - 属性值类型必须匹配rangeDataType        │
│ - 支持:string/integer/float/boolean/date │
│ - 错误码: ONT003                        │
├─────────────────────────────────────────┤
│ 第4层: 约束规则检查                      │
│ - 正则表达式(PATTERN)                    │
│ - 数值范围(RANGE)                        │
│ - 枚举值(ENUM)                           │
│ - 错误码: ONT004                        │
├─────────────────────────────────────────┤
│ 第5层: OWL约束(预留)                     │
│ - 等价类检查                             │
│ - 不相交类检查                           │
├─────────────────────────────────────────┤
│ 第6层: 推理扩展(预留)                    │
│ - 基于推理结果验证                       │
└─────────────────────────────────────────┘
```

#### 6.1.2 代码实现

```java
// OntologyValidationServiceImpl.java
public ValidationResultVO validateNode(String graphId, String nodeType, 
                                        Map<String, Object> properties) {
    // Layer 1: 类型存在性
    OntClassDO classDef = findClassByLocalName(defId, nodeType);
    if (classDef == null) {
        errors.add(ValidationErrorVO.of(1, ERR_TYPE_NOT_FOUND,
            "节点类型未在本体中定义: " + nodeType, "type", nodeType));
        return ValidationResultVO.fail(1, errors);
    }
    
    // 获取该类及其父类的所有属性定义
    List<OntPropertyDO> allProps = collectPropertiesForClass(defId, classDef);
    
    // Layer 2: 必填属性校验
    errors.addAll(checkRequiredProperties(allProps, properties));
    
    // Layer 3: 数据类型校验
    errors.addAll(checkDataTypes(allProps, properties));
    
    // Layer 4: 约束规则校验
    errors.addAll(checkConstraints(defId, classDef, properties));
    
    if (!errors.isEmpty()) {
        return ValidationResultVO.fail(4, errors);
    }
    
    // 注入默认值
    enriched = injectDefaults(allProps, enriched);
    
    return ValidationResultVO.pass();
}
```

#### 6.1.3 批量验证

```bash
curl -X POST 'http://localhost:8080/api/v1/ontology/graph-001/validate/batch' \
  -H 'Content-Type: application/json' \
  -d '{
    "nodes": [
      {"type": "Party", "properties": {"partyName": "徐某骥", "partyType": "自然人", "partyRole": "原告"}}
    ],
    "edges": [
      {"type": "CASE_PARTY", "source": "party-001", "target": "case-001", "fact": "徐某骥作为原告提起公司解散纠纷诉讼"}
    ]
  }'
```

### 6.2 Apache Jena 推理引擎框架

#### 6.2.1 Jena 推理引擎概述

**Apache Jena** 是一个开源的 Java 语义网(Semantic Web)和知识图谱框架,由 Apache 软件基金会维护。它提供了完整的 RDF/OWL 处理能力,包括:

- **RDF 数据存储和查询**
- **OWL 本体建模**
- **推理引擎(Reasoner)**
- **SPARQL 查询语言**

**在 Graphiti-Java 架构中的位置**:

```
┌─────────────────────────────────────────────────────┐
│ 6️⃣  推理层 (Reasoning) - Jena 推理引擎               │
│     ┌───────────────────────────────────────────┐   │
│     │ OntologyReasoner (推理服务接口)            │   │
│     │   ↓                                       │   │
│     │ OntologyReasonerImpl (Jena 实现)          │   │
│     │   ↓                                       │   │
│     │ InfModel (Jena 推理模型)                  │   │
│     │   ↓                                       │   │
│     │ Reasoner (Jena OWL 推理机)                │   │
│     │   ↓                                       │   │
│     │ OntModel (Jena 本体模型)                  │   │
│     └───────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────┤
│ 1️⃣  本体层 (Ontology)                               │
│     OntClass、OntProperty、OntConstraint            │
│     (从关系数据库加载本体定义到 Jena 模型)            │
└─────────────────────────────────────────────────────┘
```

**设计理念**:

1. **缓存优先**: 推理机预热后将 InfModel 缓存到内存,避免重复推理
2. **读写分离**: 使用 ReentrantReadWriteLock 保证并发安全
3. **按需推理**: 支持按 graphId 独立预热,多图谱隔离
4. **生产就绪**: 选择 OWL 2 RL 配置文件,平衡性能和推理能力

#### 6.2.2 Apache Jena 推理机架构

**核心组件**:

```
┌─────────────────────────────────────────┐
│         Jena 推理引擎架构                 │
├─────────────────────────────────────────┤
│                                         │
│  InfModel (推理模型)                     │
│     ↓ 暴露推理结果                       │
│  Reasoner (推理机)                       │
│     ↓ 执行推理规则                       │
│  OntModel (本体模型)                     │
│     ↓ 存储本体定义                       │
│  BaseModel (基础RDF模型)                 │
│                                         │
└─────────────────────────────────────────┘
```

**推理流程**:

```java
// 1. 创建基础模型(存储本体定义)
OntModel baseModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);

// 2. 绑定推理机
Reasoner reasoner = ReasonerRegistry.getOWLReasoner();
InfModel infModel = ModelFactory.createInfModel(reasoner, baseModel);

// 3. 加载本体数据
baseModel.read("ontology.owl");

// 4. 执行推理(自动推导隐含知识)
// 推理结果可通过 infModel 查询
```

#### 6.2.3 Jena 支持的推理机类型

| 推理机 | 配置方法 | 特点 | 适用场景 |
|--------|---------|------|---------|
| **OWL Mini** | `ReasonerRegistry.getOWLReasoner()` | 轻量级,支持OWL核心特性 | 快速原型开发 |
| **OWL Micro** | 自定义配置 | 最轻量,仅支持OWL Lite | 资源受限环境 |
| **OWL DL** | `OWLDLReasonerFactory` | 完整OWL DL支持,可判定 | 学术/研究场景 |
| **OWL 2 RL** | `OWLFBRuleReasoner` | 规则驱动,高性能 | **生产环境推荐** ✅ |

**其他推理机**:

| 推理机 | 说明 |
|--------|------|
| **RDFS** | 仅支持RDF Schema推理,最轻量 |
| **Transitive** | 仅支持传递性推理 |
| **Custom Rules** | 自定义规则推理(最灵活) |

#### 6.2.4 OWL 2 RL 推理机特性

**为什么选择 OWL 2 RL?**

```
OWL 2 RL 的优势:
✅ 高性能: 基于规则的推理,复杂度 Polynomial Time
✅ 可扩展: 适合百万级三元组的大规模知识图谱
✅ 可判定: 保证推理终止,不会死循环
✅ 实用性: 覆盖90%业务场景需求
✅ 标准化: W3C官方标准,兼容性好
```

**支持的推理特性**:

**1. 类层次推理**

```java
// 本体定义
Class: LegalEntity (法律实体 - 根类)
  └─ Party (当事人)
      └─ NaturalPerson (自然人)
          └─ Plaintiff (原告)

// 实例
Individual: 徐某骥 type: Plaintiff

// 推理结果(自动推导)
徐某骥 type: Plaintiff        // 显式声明
徐某骥 type: NaturalPerson    // ✅ 推理得出
徐某骥 type: Party            // ✅ 推理得出
徐某骥 type: LegalEntity      // ✅ 推理得出
```

**2. 属性继承推理**

```java
// 本体定义
Property: hasContactInfo (联系信息)
  ├─ hasEmail (邮箱)
  └─ hasPhone (电话)

Property: hasPartyName (当事人名称)
  ├─ hasNaturalPersonName (自然人姓名)
  └─ hasLegalPersonName (法人名称)

// 推理:子类属性自动继承父属性的约束
```

**3. 逆关系推理**

```java
// 本体定义
ObjectProperty: CASE_PARTY (当事人参与案件)
  InverseOf: CASE_HAS_PARTY (案件包含当事人)

// 显式声明
(徐某骥)-[CASE_PARTY]->(公司解散纠纷案)

// 推理结果
✅ (公司解散纠纷案)-[CASE_HAS_PARTY]->(徐某骥)  // 自动推导
```

**4. 传递性推理**

```java
// 本体定义
ObjectProperty: APPEALED_CASE (上诉关系) 
  Characteristics: Transitive (传递性)

// 显式声明
(一审案件)-[APPEALED_CASE]->(二审案件)
(二审案件)-[APPEALED_CASE]->(再审案件)

// 推理结果
✅ (一审案件)-[APPEALED_CASE]->(再审案件)  // 自动推导传递关系
```

**5. 对称性推理**

```java
// 本体定义
ObjectProperty: EQUIVALENT_CASE (等效案件)
  Characteristics: Symmetric (对称性)

// 显式声明
(案件A)-[EQUIVALENT_CASE]->(案件B)

// 推理结果
✅ (案件B)-[EQUIVALENT_CASE]->(案件A)  // 自动推导对称关系
```

**6. 等价类推理**

```java
// 本体定义
Class: Plaintiff (原告)
  EquivalentTo: Person AND (hasRole value "原告")

// 显式声明
徐某骥 type: Person
徐某骥 hasRole: "原告"

// 推理结果
✅ 徐某骥 type: Plaintiff  // 自动归类为原告
```

**7. 不相交类检查**

```java
// 本体定义
Class: Party (当事人)
  DisjointWith: Court (法院)

// 错误检测
❌ 某实体不能同时是 Party 和 Court
// 推理机会报告一致性冲突
```

#### 6.2.5 Jena 推理机 vs 其他推理机对比

| 特性 | Jena | HermiT | Pellet | Fact++ |
|------|------|--------|--------|--------|
| **OWL 2 支持** | ✅ RL配置文件 | ✅ 完整支持 | ✅ 完整支持 | ✅ 完整支持 |
| **性能** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ |
| **可扩展性** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐ | ⭐⭐ |
| **内存占用** | 低 | 高 | 高 | 中 |
| **Java集成** | 原生 | 需桥接 | 需桥接 | 需桥接 |
| **生产适用** | ✅ 推荐 | ❌ 研究用 | ❌ 研究用 | ❌ 研究用 |
| **社区支持** | Apache基金 | 学术团队 | 学术团队 | 学术团队 |

#### 6.2.6 Graphiti-Java 中的 Jena 实现

**OntologyReasoner 接口定义**:

```java
package com.graphiti.module.graphiti.service;

import com.graphiti.module.graphiti.vo.ontology.ConsistencyResultVO;
import com.graphiti.module.graphiti.vo.ontology.InferredTypeVO;
import java.util.List;
import java.util.Map;

public interface OntologyReasoner {

    // 推理机生命周期管理
    void warmUp(String graphId);
    void shutdown(String graphId);
    boolean isWarmedUp(String graphId);

    // 类层次推理
    List<String> getAncestorClasses(String graphId, String classUri);
    List<String> getDescendantClasses(String graphId, String classUri);

    // 类型推断
    List<InferredTypeVO> inferTypes(String graphId, Map<String, Object> properties);

    // 一致性检查
    ConsistencyResultVO checkConsistency(String graphId);
    boolean isSatisfiable(String graphId, String classUri);

    // 属性域和范围查询
    List<String> getPropertyDomains(String graphId, String propertyUri);
    List<String> getPropertyRanges(String graphId, String propertyUri);
}
```

**OntologyReasonerImpl 核心实现**:

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class OntologyReasonerImpl implements OntologyReasoner {

    private final OntDefinitionMapper definitionMapper;
    private final OntClassMapper classMapper;
    private final OntPropertyMapper propertyMapper;
    private final OntConstraintMapper constraintMapper;

    // 推理模型缓存(线程安全)
    private final Map<String, InfModel> infModelCache = new ConcurrentHashMap<>();
    private final Map<String, OntModel> ontModelCache = new ConcurrentHashMap<>();
    private final Map<String, ReentrantReadWriteLock> locks = new ConcurrentHashMap<>();

    private ReentrantReadWriteLock getLock(String graphId) {
        return locks.computeIfAbsent(graphId, k -> new ReentrantReadWriteLock());
    }

    private Long resolveDefinitionId(String graphId) {
        LambdaQueryWrapper<OntDefinitionDO> w = new LambdaQueryWrapper<>();
        w.eq(OntDefinitionDO::getGraphId, graphId);
        w.eq(OntDefinitionDO::getStatus, "ACTIVE");
        w.last("LIMIT 1");
        OntDefinitionDO def = definitionMapper.selectOne(w);
        return def != null ? def.getId() : null;
    }

    /**
     * 推理机预热: 从关系数据库加载本体定义,构建 Jena 推理模型
     */
    @Override
    public void warmUp(String graphId) {
        ReentrantReadWriteLock lock = getLock(graphId);
        lock.writeLock().lock();
        try {
            if (infModelCache.containsKey(graphId)) return;
            log.info("推理机预热中:graphId={}", graphId);

            Long defId = resolveDefinitionId(graphId);
            if (defId == null) {
                log.warn("图谱无活跃本体定义,跳过预热:graphId={}", graphId);
                return;
            }

            // 1. 创建本体模型
            OntModel baseModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
            String ns = "http://graphiti.io/ontology/" + graphId + "/";
            baseModel.setNsPrefix("gt", ns);
            baseModel.setNsPrefix("rdfs", RDFS.getURI());
            baseModel.setNsPrefix("owl", OWL.getURI());
            baseModel.setNsPrefix("rdf", RDF.getURI());

            // 2. 加载类定义
            List<OntClassDO> classes = classMapper.selectList(
                new LambdaQueryWrapper<OntClassDO>().eq(OntClassDO::getDefinitionId, defId));
            Map<Long, OntClass> classMap = new HashMap<>();
            for (OntClassDO cls : classes) {
                OntClass ontClass = baseModel.createClass(cls.getClassUri());
                classMap.put(cls.getId(), ontClass);
            }
            // 3. 建立类层次关系
            for (OntClassDO cls : classes) {
                if (cls.getParentClassId() != null && classMap.containsKey(cls.getParentClassId())) {
                    classMap.get(cls.getId()).addSuperClass(classMap.get(cls.getParentClassId()));
                }
            }

            // 4. 加载属性定义
            List<OntPropertyDO> props = propertyMapper.selectList(
                new LambdaQueryWrapper<OntPropertyDO>().eq(OntPropertyDO::getDefinitionId, defId));
            for (OntPropertyDO prop : props) {
                if ("OBJECT".equals(prop.getPropertyType())) {
                    ObjectProperty op = baseModel.createObjectProperty(prop.getPropertyUri());
                    if (prop.getDomainClassId() != null && classMap.containsKey(prop.getDomainClassId())) {
                        op.addDomain(classMap.get(prop.getDomainClassId()));
                    }
                    if (prop.getRangeClassId() != null && classMap.containsKey(prop.getRangeClassId())) {
                        op.addRange(classMap.get(prop.getRangeClassId()));
                    }
                } else {
                    DatatypeProperty dp = baseModel.createDatatypeProperty(prop.getPropertyUri());
                    if (prop.getDomainClassId() != null && classMap.containsKey(prop.getDomainClassId())) {
                        dp.addDomain(classMap.get(prop.getDomainClassId()));
                    }
                }
            }

            // 5. 绑定 OWL 推理机
            Reasoner reasoner = ReasonerRegistry.getOWLReasoner().bindSchema(baseModel);
            InfModel infModel = ModelFactory.createInfModel(reasoner, baseModel);

            // 6. 缓存推理模型
            infModelCache.put(graphId, infModel);
            ontModelCache.put(graphId, baseModel);
            log.info("推理机预热完成:graphId={}", graphId);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 关闭推理机,释放内存
     */
    @Override
    public void shutdown(String graphId) {
        ReentrantReadWriteLock lock = getLock(graphId);
        lock.writeLock().lock();
        try {
            InfModel removed = infModelCache.remove(graphId);
            ontModelCache.remove(graphId);
            if (removed != null) {
                removed.removeAll();
                log.info("推理机已关闭:graphId={}", graphId);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 查询祖先类(递归)
     */
    @Override
    public List<String> getAncestorClasses(String graphId, String classUri) {
        InfModel infModel = infModelCache.get(graphId);
        if (infModel == null) return List.of();

        Resource cls = infModel.getResource(classUri);
        if (cls == null) return List.of();

        Set<String> ancestors = new LinkedHashSet<>();
        collectAncestors(infModel, cls, ancestors);
        ancestors.remove(classUri);
        return new ArrayList<>(ancestors);
    }

    /**
     * 查询后代类(递归)
     */
    @Override
    public List<String> getDescendantClasses(String graphId, String classUri) {
        InfModel infModel = infModelCache.get(graphId);
        if (infModel == null) return List.of();

        Resource cls = infModel.getResource(classUri);
        if (cls == null) return List.of();

        Set<String> descendants = new LinkedHashSet<>();
        collectDescendants(infModel, cls, descendants);
        descendants.remove(classUri);
        return new ArrayList<>(descendants);
    }

    /**
     * 类型推断: 根据属性匹配度推断实体可能属于的类
     */
    @Override
    public List<InferredTypeVO> inferTypes(String graphId, Map<String, Object> properties) {
        ReentrantReadWriteLock lock = getLock(graphId);
        lock.readLock().lock();
        try {
            InfModel infModel = infModelCache.get(graphId);
            if (infModel == null || properties == null || properties.isEmpty()) {
                return List.of();
            }

            // 统计每个类的属性匹配数量
            Map<String, Integer> classMatchCount = new HashMap<>();
            for (String propertyUri : properties.keySet()) {
                Property prop = infModel.getProperty(propertyUri);
                if (prop == null) continue;

                // 查询属性的定义域
                StmtIterator it = infModel.listStatements(prop, RDFS.domain, (RDFNode) null);
                while (it.hasNext()) {
                    RDFNode domain = it.nextStatement().getObject();
                    if (domain.isResource()) {
                        String domainUri = domain.asResource().getURI();
                        if (domainUri != null) {
                            classMatchCount.merge(domainUri, 1, Integer::sum);
                            // 累加祖先类的匹配度
                            Set<String> ancestors = new HashSet<>();
                            collectAncestors(infModel, domain.asResource(), ancestors);
                            for (String ancestorUri : ancestors) {
                                if (!ancestorUri.equals(domainUri)) {
                                    classMatchCount.merge(ancestorUri, 1, Integer::sum);
                                }
                            }
                        }
                    }
                }
            }

            // 按匹配度排序
            return classMatchCount.entrySet().stream()
                .map(e -> InferredTypeVO.builder()
                    .classUri(e.getKey())
                    .confidence(e.getValue() * 1.0)
                    .reason("匹配属性数量: " + e.getValue())
                    .build())
                .sorted((a, b) -> Double.compare(b.getConfidence(), a.getConfidence()))
                .toList();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 一致性检查: 检查本体定义是否存在逻辑冲突
     */
    @Override
    public ConsistencyResultVO checkConsistency(String graphId) {
        InfModel infModel = infModelCache.get(graphId);
        if (infModel == null) {
            return ConsistencyResultVO.builder()
                .consistent(true)
                .inconsistencies(List.of("推理机未初始化"))
                .build();
        }

        List<String> satisfiable = new ArrayList<>();
        List<String> unsatisfiable = new ArrayList<>();

        // 检查核心类的可满足性
        String[] coreClasses = {
            "http://www.w3.org/2002/07/owl#Thing",
            "http://www.w3.org/2000/01/rdf-schema#Resource"
        };
        for (String clsUri : coreClasses) {
            if (isSatisfiable(graphId, clsUri)) {
                satisfiable.add(clsUri);
            } else {
                unsatisfiable.add(clsUri);
            }
        }

        return ConsistencyResultVO.builder()
            .consistent(unsatisfiable.isEmpty())
            .satisfiableClasses(satisfiable)
            .unsatisfiableClasses(unsatisfiable)
            .build();
    }

    /**
     * 类可满足性检查
     */
    @Override
    public boolean isSatisfiable(String graphId, String classUri) {
        InfModel infModel = infModelCache.get(graphId);
        if (infModel == null) return true;
        Resource cls = infModel.getResource(classUri);
        if (cls == null) return true;
        return !infModel.listStatements(null, RDF.type, cls).toList().isEmpty()
            || !infModel.listStatements(cls, RDFS.subClassOf, (RDFNode) null).toList().isEmpty();
    }

    /**
     * 查询属性的定义域
     */
    @Override
    public List<String> getPropertyDomains(String graphId, String propertyUri) {
        ReentrantReadWriteLock lock = getLock(graphId);
        lock.readLock().lock();
        try {
            InfModel infModel = infModelCache.get(graphId);
            if (infModel == null || propertyUri == null) return List.of();

            Property prop = infModel.getProperty(propertyUri);
            if (prop == null) return List.of();

            Set<String> domains = new LinkedHashSet<>();
            StmtIterator it = infModel.listStatements(prop, RDFS.domain, (RDFNode) null);
            while (it.hasNext()) {
                RDFNode obj = it.nextStatement().getObject();
                if (obj.isResource()) {
                    String uri = obj.asResource().getURI();
                    if (uri != null) domains.add(uri);
                }
            }
            return new ArrayList<>(domains);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * 查询属性的值域
     */
    @Override
    public List<String> getPropertyRanges(String graphId, String propertyUri) {
        ReentrantReadWriteLock lock = getLock(graphId);
        lock.readLock().lock();
        try {
            InfModel infModel = infModelCache.get(graphId);
            if (infModel == null || propertyUri == null) return List.of();

            Property prop = infModel.getProperty(propertyUri);
            if (prop == null) return List.of();

            Set<String> ranges = new LinkedHashSet<>();
            StmtIterator it = infModel.listStatements(prop, RDFS.range, (RDFNode) null);
            while (it.hasNext()) {
                RDFNode obj = it.nextStatement().getObject();
                if (obj.isResource()) {
                    String uri = obj.asResource().getURI();
                    if (uri != null) ranges.add(uri);
                }
            }
            return new ArrayList<>(ranges);
        } finally {
            lock.readLock().unlock();
        }
    }

    // 递归收集祖先类
    private void collectAncestors(InfModel model, Resource cls, Set<String> result) {
        StmtIterator it = model.listStatements(cls, RDFS.subClassOf, (RDFNode) null);
        while (it.hasNext()) {
            RDFNode parent = it.nextStatement().getObject();
            if (parent.isResource()) {
                String parentUri = parent.asResource().getURI();
                if (parentUri != null && !parentUri.equals(cls.getURI())) {
                    result.add(parentUri);
                    collectAncestors(model, parent.asResource(), result);
                }
            }
        }
    }

    // 递归收集后代类
    private void collectDescendants(InfModel model, Resource cls, Set<String> result) {
        StmtIterator it = model.listStatements(null, RDFS.subClassOf, cls);
        while (it.hasNext()) {
            Resource child = it.nextStatement().getSubject();
            String childUri = child.getURI();
            if (childUri != null && !childUri.equals(cls.getURI())) {
                result.add(childUri);
                collectDescendants(model, child, result);
            }
        }
    }
}
```

#### 6.2.7 推理机的性能优化

**1. 缓存策略**

```java
// 推理模型缓存(避免重复推理)
Map<String, InfModel> infModelCache = new ConcurrentHashMap<>();

// 推理结果缓存
Map<String, List<String>> inferenceCache = new ConcurrentHashMap<>();
```

**2. 按需推理**

```java
// 不要全局推理,只推理需要的部分
public void selectiveReasoning(String graphId, String focusClass) {
    InfModel infModel = infModelCache.get(graphId);
    
    // 只推理特定类的子类
    String sparql = """
        SELECT ?subclass WHERE {
          ?subclass rdfs:subClassOf* <%s> .
        }
    """.formatted(focusClass);
    
    // 执行查询...
}
```

**3. 异步预热**

```java
// 后台预热推理机
@Async
public void warmUpAsync(String graphId) {
    warmUp(graphId);
    log.info("推理机预热完成: {}", graphId);
}
```

#### 6.2.8 Jena 推理机的局限性

**不支持的特性**:

❌ **OWL 2 Full**: 表达能力最强但不可判定  
❌ **复杂角色包含**: 如 `hasParent o hasBrother ⊑ hasUncle`  
❌ **基数约束推理**: 如 `exactly(2, hasChild)`  
❌ **数据类型推理**: 如 `xsd:integer + xsd:integer = xsd:integer`

**解决方案**:

对于不支持的特性,可以使用:

1. **自定义规则推理**:
```java
String rules = """
    [hasUncleRule: 
      (?p hasParent ?grandparent)
      (?grandparent hasChild ?uncle)
      (?uncle gender male)
      -> (?p hasUncle ?uncle)]
""";

Reasoner reasoner = new GenericRuleReasoner(Rule.parseRules(rules));
```

2. **混合推理策略**:
```
Jena OWL 2 RL (基础推理)
   +
自定义规则 (业务逻辑)
   +
SPARQL查询 (复杂查询)
```

#### 6.2.9 实际应用场景

**法律知识图谱推理**:

```java
// 场景: 自动推导案件适用法律
public List<String> inferApplicableLaws(String caseUri) {
    InfModel infModel = infModelCache.get("legal-kg");
    
    String sparql = """
        PREFIX legal: <http://legal-ai.cc/ontology#>
        
        SELECT ?law WHERE {
          <%s> legal:CASE_LEGAL_BASIS ?law .
          
          # 推理: 如果案件属于民事案件,则适用民法典
          <%s> legal:caseType "民事案件" .
          ?law legal:belongsTo legal:CivilCode .
        }
    """.formatted(caseUri, caseUri);
    
    // 执行查询...
}
```

**类案推荐推理**:

```java
// 场景: 基于推理的类案推荐
public List<String> recommendSimilarCases(String caseUri) {
    InfModel infModel = infModelCache.get("legal-kg");
    
    // 推理: 同一法官审理的类似案件可能有相似裁判
    String sparql = """
        PREFIX legal: <http://legal-ai.cc/ontology#>
        
        SELECT ?similarCase WHERE {
          <%s> legal:CASE_JUDGE ?judge .
          ?similarCase legal:CASE_JUDGE ?judge .
          ?similarCase legal:caseType <%s>.legal:caseType .
          
          # 排除自身
          FILTER(?similarCase != <%s>)
        }
    """.formatted(caseUri, caseUri, caseUri);
    
    // 执行查询...
}
```

### 6.3 版本管理与审计追踪

#### 6.3.1 版本历史记录

每次增删改均记录到`ont_version_history`表:

```json
{
  "id": 100,
  "definitionId": 5,
  "version": "1.0.0",
  "changeType": "UPDATED",
  "entityType": "CLASS",
  "entityId": 10,
  "beforeState": "{\"localName\": \"Person\", ...}",
  "afterState": "{\"localName\": \"Person\", \"description\": \"新增\", ...}",
  "diffSummary": "添加了description字段",
  "changedBy": "user-001",
  "changedAt": "2024-03-15T10:00:00"
}
```

#### 6.3.2 回滚功能

```bash
curl -X POST 'http://localhost:8080/api/v1/ontology/graph-001/history/100/rollback'
```

### 6.4 Schema.org导入导出

**导入**:
```bash
curl -X POST 'http://localhost:8080/api/v1/ontology/graph-001/import/schema-org' \
  -H 'Content-Type: application/json' \
  -d '{
    "domain": "Person",
    "depth": 3
  }'
```

### 6.5 数据处理Pipeline

```
原始文档 (PDF/Word/HTML)
   ↓
[步骤1: 文本提取] → 纯文本
   ↓
[步骤2: 分段] → Episode列表
   ↓
[步骤3: LLM抽取] → Entity + Edge
   ↓
[步骤4: 本体验证] → 验证Entity/Edge是否符合Ontology
   ↓
[步骤5: 向量嵌入] → 生成embedding
   ↓
[步骤6: 社区检测] → 发现Community
   ↓
[步骤7: 索引构建] → 向量索引 + 全文索引
```

---

## 第七章 系统架构设计

> **本章定位**: 从架构视角讲解Graphiti-Java的整体设计、模块关系、数据流向。

### 7.1 分层架构

```
┌─────────────────────────────────────────────┐
│ 表现层 (Controller)                         │
│ - OntologyController                        │
│ - GraphController                           │
│ - CommunityController                       │
├─────────────────────────────────────────────┤
│ 领域服务层 (Service)                        │
│ - OntologyClassService                      │
│ - OntologyPropertyService                   │
│ - OntologyValidationService                 │
│ - OntologyReasoner                          │
│ - CommunityService                          │
├─────────────────────────────────────────────┤
│ 数据访问层 (DAO/Mapper)                     │
│ - OntClassMapper                            │
│ - OntPropertyMapper                         │
│ - OntConstraintMapper                       │
├─────────────────────────────────────────────┤
│ 数据存储层                                  │
│ - 关系数据库 (本体元数据)                         │
│ - Neo4j (图数据)                            │
│ - Redis (缓存)                              │
└─────────────────────────────────────────────┘
```

### 7.2 关系数据库+Neo4j双存储架构

```
关系数据库 (Metadata) [支持MySQL/PostgreSQL]    Neo4j (Graph Data)
─────────────────────                    ────────────────────────────────────────
graphiti_graph_metadata                   Entity 节点 (type -> ontClassId)
ont_definition                            Episode 节点 (原始数据容器)
ont_class                                 RELATES_TO 边 (fact -> embedding)
ont_property                              ───────────────────────────────────────
ont_constraint                            groupId 隔离,同一 graphId 下的数据
ont_version_history                       Embedding 向量存储 (用于语义检索)
```

**关联机制**:
```
Layer 1: graphId 关联
Layer 2: ont_definition 版本容器
Layer 3: Neo4j type 字段
```

### 7.3 数据流向图

```mermaid
flowchart LR
    A[客户端] -->|REST API| B[Controller]
    B -->|调用| C[Service]
    C -->|读写| D[(关系数据库)]
    C -->|读写| E[(Neo4j)]
    C -->|缓存| F[(Redis)]
    
    G[LLM] -->|抽取结果| C
    C -->|验证| H[Validation Engine]
    C -->|推理| I[Jena Reasoner]
```

### 7.4 模块关系与依赖

```mermaid
graph TB
    Controller[OntologyController] --> SvcClass[OntologyClassService]
    Controller --> SvcProp[OntologyPropertyService]
    Controller --> SvcVal[OntologyValidationService]
    Controller --> SvcReasoner[OntologyReasoner]
    
    SvcClass --> MapperClass[OntClassMapper]
    SvcProp --> MapperProp[OntPropertyMapper]
    SvcVal --> MapperVal[各类Mapper]
    SvcReasoner --> Jena[Jena InfModel]
```

### 7.5 完整概念关系图

```mermaid
graph TB
    subgraph "本体层"
        OntDef[OntDefinition]
        OntClass[OntClass]
        OntProp[OntProperty]
        OntConst[OntConstraint]
    end
    
    subgraph "数据层"
        Entity[Entity节点]
        Edge[Edge边]
        Episode[Episode容器]
        Community[Community社区]
    end
    
    subgraph "时间层"
        Temporal[validAt/invalidAt]
    end
    
    subgraph "验证推理"
        Validation[6层验证引擎]
        Reasoner[OWL推理机]
    end
    
    OntDef --> OntClass
    OntDef --> OntProp
    OntDef --> OntConst
    
    OntClass -->|定义类型| Entity
    OntProp -->|定义关系| Edge
    OntConst -->|约束| Validation
    
    Episode -->|LLM抽取| Entity
    Episode -->|LLM抽取| Edge
    
    Entity --> Edge
    Entity --> Community
    
    Temporal -.-> Entity
    Temporal -.-> Edge
    
    Validation --> Entity
    Validation --> Edge
    Reasoner --> OntClass
```

---

## 第八章 REST API完整参考

> **本章定位**: 列出所有本体相关的REST API接口,包括路径、方法、参数、返回值说明。

### 8.1 本体定义管理

#### 8.1.1 获取本体定义

```http
GET /api/v1/ontology/{graphId}/definition
```

**响应**:
```json
{
  "code": 0,
  "data": {
    "id": 1,
    "graphId": "legal-kg",
    "name": "法律知识图谱本体",
    "version": "1.0.0",
    "status": "ACTIVE"
  }
}
```

#### 8.1.2 创建本体定义

```http
POST /api/v1/ontology/{graphId}/definition
```

**请求体**:
```json
{
  "name": "法律知识图谱本体",
  "namespace": "http://legal-ai.cc/ontology",
  "version": "1.0.0",
  "description": "公司法领域本体定义"
}
```

### 8.2 类管理

#### 8.2.1 列出所有类

```http
GET /api/v1/ontology/{graphId}/classes
```

#### 8.2.2 获取类层次树

```http
GET /api/v1/ontology/{graphId}/classes/hierarchy
```

#### 8.2.3 创建类

```http
POST /api/v1/ontology/{graphId}/classes
```

#### 8.2.4 更新类

```http
PUT /api/v1/ontology/{graphId}/classes/{classId}
```

#### 8.2.5 删除类

```http
DELETE /api/v1/ontology/{graphId}/classes/{classId}
```

### 8.3 属性管理

| 接口 | 方法 | 路径 |
|------|------|------|
| 列出属性 | GET | `/api/v1/ontology/{graphId}/properties` |
| 创建属性 | POST | `/api/v1/ontology/{graphId}/properties` |
| 更新属性 | PUT | `/api/v1/ontology/{graphId}/properties/{propertyId}` |
| 删除属性 | DELETE | `/api/v1/ontology/{graphId}/properties/{propertyId}` |

### 8.4 约束管理

| 接口 | 方法 | 路径 |
|------|------|------|
| 列出约束 | GET | `/api/v1/ontology/{graphId}/constraints` |
| 创建约束 | POST | `/api/v1/ontology/{graphId}/constraints` |
| 更新约束 | PUT | `/api/v1/ontology/{graphId}/constraints/{constraintId}` |
| 删除约束 | DELETE | `/api/v1/ontology/{graphId}/constraints/{constraintId}` |

### 8.5 验证与推理

| 接口 | 方法 | 路径 |
|------|------|------|
| 批量验证 | POST | `/api/v1/ontology/{graphId}/validate/batch` |
| 推理机状态 | GET | `/api/v1/ontology/{graphId}/reasoners/status` |
| 预热推理机 | POST | `/api/v1/ontology/{graphId}/reasoners/warmup` |
| 一致性检查 | GET | `/api/v1/ontology/{graphId}/consistency` |

### 8.6 版本历史

| 接口 | 方法 | 路径 |
|------|------|------|
| 获取版本历史 | GET | `/api/v1/ontology/{graphId}/history` |
| 回滚版本 | POST | `/api/v1/ontology/{graphId}/history/{historyId}/rollback` |

### 8.7 错误码说明

| 错误码 | 说明 |
|--------|------|
| ONT001 | 类型不存在 |
| ONT002 | 缺少必填属性 |
| ONT003 | 类型不匹配 |
| ONT004 | 违反约束 |

---

## 第九章 接口使用范例

> **本章定位**: 为每个主要接口提供curl命令示例或JavaScript调用示例,展示实际使用场景。

### 9.1 curl命令示例

#### 场景1: 创建完整本体

```bash
# 1. 创建本体定义
curl -X POST 'http://localhost:8080/api/v1/ontology/graph-001/definition' \
  -H 'Content-Type: application/json' \
  -d '{"name": "测试本体", "version": "1.0.0"}'

# 2. 创建类
curl -X POST 'http://localhost:8080/api/v1/ontology/graph-001/classes' \
  -H 'Content-Type: application/json' \
  -d '{"localName": "Person", "description": "自然人"}'

# 3. 创建属性
curl -X POST 'http://localhost:8080/api/v1/ontology/graph-001/properties' \
  -H 'Content-Type: application/json' \
  -d '{"localName": "age", "propertyType": "DATATYPE", "rangeDataType": "integer"}'

# 4. 创建约束
curl -X POST 'http://localhost:8080/api/v1/ontology/graph-001/constraints' \
  -H 'Content-Type: application/json' \
  -d '{"propertyId": 1, "constraintType": "RANGE", "value": "{\"min\": 0, \"max\": 150}"}'
```

### 9.2 JavaScript/TypeScript调用示例

```typescript
import { ontologyApi } from './api/ontology'

// 1. 获取本体定义
const definition = await ontologyApi.getDefinition('graph-001')

// 2. 创建类
const personClass = await ontologyApi.createClass('graph-001', {
  localName: 'Person',
  description: '自然人'
})

// 3. 批量验证
const validationResult = await ontologyApi.validateBatch('graph-001', {
  nodes: [{ type: 'Party', properties: { partyName: '徐某骥', partyType: '自然人', partyRole: '原告' } }],
  edges: []
})

console.log('验证结果:', validationResult)
```

### 9.3 典型业务场景演示

#### 场景2: 执行社区检测

```bash
# 1. 执行社区检测
curl -X POST 'http://localhost:8080/api/v1/graph/legal-kg/communities/build' \
  -H 'Authorization: Bearer <token>'

# 2. 查询社区列表
curl -X GET 'http://localhost:8080/api/v1/graph/legal-kg/communities'
```

#### 场景3: 时序查询

```bash
# 查询2023年5月的上下文
curl -X GET 'http://localhost:8080/api/v1/graph/legal-kg/temporal/query' \
  -H 'Content-Type: application/json' \
  -d '{
    "graphId": "legal-kg",
    "queryTime": "2023-05-10T00:00:00Z",
    "centerNode": "entity-zhangsan",
    "maxDepth": 2
  }'
```

---

## 附录 最佳实践与常见问题

### A.1 本体建模最佳实践

#### A.1.1 类设计原则

1. **单一职责**: 每个类只表示一个概念
   - ✅ `Person`, `Company`
   - ❌ `PersonAndCompany`

2. **层次深度**: 建议不超过5层
   - 太深会导致查询复杂
   - 太浅会失去分类意义

3. **命名规范**: 
   - 使用大驼峰命名: `Person`, `Company`
   - 避免缩写: 使用`Person`而非`P`
   - 使用名词: 避免使用动词或形容词

#### A.1.2 属性设计原则

1. **对象属性 vs 数据属性**:
   - 连接到另一个实体 → 对象属性(OBJECT)
   - 连接到简单值 → 数据属性(DATATYPE)

2. **域和范围**:
   - 明确定义domain和range
   - 这有助于验证和推理

3. **是否必填**:
   - 关键信息设为必填(isRequired=true)
   - 可选信息不要设为必填

### A.2 社区检测调优建议

#### A.2.1 性能优化

1. **并行处理**:
   ```java
   // 使用线程池并行构建社区
   ExecutorService executor = Executors.newFixedThreadPool(10);
   ```

2. **过滤小社区**:
   - 成员数<2的社区通常无意义
   - 可以过滤掉

3. **定期执行**:
   - 数据更新后重新检测
   - 建议每天或每周执行一次

#### A.2.2 质量提升

1. **LLM命名优化**:
   - 提供足够的上下文信息
   - 使用二叉树合并摘要,避免信息丢失

2. **社区评估**:
   - 检查社区内聚度
   - 检查社区间分离度

### A.3 时序数据管理技巧

#### A.3.1 时间戳策略

1. **统一使用UTC时间**:
   - 避免时区问题
   - 格式: `2024-03-15T10:00:00Z`

2. **精度选择**:
   - 一般业务: 秒级精度足够
   - 高频交易: 可能需要毫秒级

#### A.3.2 查询优化

1. **索引**:
   ```cypher
   CREATE INDEX entity_valid_at FOR (n:Entity) ON (n.valid_at)
   CREATE INDEX edge_valid_at FOR ()-[r:RELATES_TO]->() ON (r.valid_at)
   ```

2. **查询模板**:
   ```cypher
   // 查询某时间点的有效实体
   MATCH (n:Entity)
   WHERE n.valid_at <= $queryTime 
     AND (n.invalid_at > $queryTime OR n.invalid_at IS NULL)
   RETURN n
   ```

### A.4 常见问题排查

#### A.4.1 类删除失败

**问题**: 删除类时提示"存在子类"

**原因**: 本体不允许删除有后代的类

**解决方案**:
```bash
# 1. 先查询所有子类
curl -X GET 'http://localhost:8080/api/v1/ontology/graph-001/classes/hierarchy'

# 2. 从叶子节点开始删除
# 先删除所有后代类,再删除父类
```

#### A.4.2 验证失败

**问题**: 创建节点时验证失败

**排查步骤**:
1. 检查类是否在本体中定义
2. 检查必填属性是否提供
3. 检查属性值类型是否正确
4. 检查是否违反约束

**示例**:
```json
// 错误响应
{
  "code": 0,
  "data": {
    "passed": false,
    "errors": [
      {
        "layer": 2,
        "errorCode": "ONT002",
        "message": "缺少必填属性: age"
      }
    ]
  }
}
```

#### A.4.3 推理机未初始化

**问题**: 一致性检查返回"推理机未初始化"

**解决方案**:
```bash
# 先预热推理机
curl -X POST 'http://localhost:8080/api/v1/ontology/graph-001/reasoners/warmup'

# 再执行一致性检查
curl -X GET 'http://localhost:8080/api/v1/ontology/graph-001/consistency'
```

#### A.4.4 社区检测结果为空

**问题**: 执行社区检测后没有生成社区

**可能原因**:
1. 图中节点数太少(<10)
2. 节点之间没有边连接
3. 边的invalid_at不为null(被时序过滤)

**解决方案**:
```bash
# 1. 检查节点数
curl -X GET 'http://localhost:8080/api/v1/graph/legal-kg/nodes/count'

# 2. 检查边
curl -X GET 'http://localhost:8080/api/v1/graph/legal-kg/edges'

# 3. 确保边的invalid_at为null
```

### A.5 参考资料

#### A.5.1 本体论相关

- **W3C OWL 2 RL**: https://www.w3.org/TR/owl2-profiles/
- **Apache Jena**: https://jena.apache.org/
- **Schema.org**: https://schema.org/

#### A.5.2 图数据库相关

- **Neo4j Cypher手册**: https://neo4j.com/docs/cypher-manual/
- **图论算法**: https://graphstream-project.org/

#### A.5.3 Graphiti-Java项目

- **项目源码**: `d:/projects/graphiti-java`
- **API文档**: `docs/manual/API接口文档/`
- **架构设计**: `docs/manual/架构设计/`

---

## 文档总结

本培训文档系统讲解了:

**理论篇** (第1-2章):
- 本体论从哲学到计算机科学的演进
- 本体在知识图谱中的三大作用
- Graphiti-Java的8层架构体系

**实践篇** (第3-7章):
- 本体核心要素:Class、Property、Constraint、Edge、Instance
- 知识图谱核心概念:Episode、Entity、Community、Temporal Graph
- 社区检测算法:LPA原理与Graphiti-Java实现
- 系统功能:6层验证、OWL推理、版本管理
- 系统架构:关系数据库+Neo4j双存储、分层设计

**应用篇** (第8-10章):
- REST API完整参考(25+个接口)
- 接口使用范例(curl/TypeScript)
- **上下文工程完整案例**:法律知识图谱从建模到AI输出

**核心价值**:
- 从"理论"到"实践"的完整链路
- 从"数据"到"上下文"的价值提升
- 从"通用AI"到"领域专家AI"的能力升级

希望本文档能帮助您:
1. 深入理解本体论和知识图谱的核心概念
2. 掌握Graphiti-Java系统的完整实现
3. 应用上下文工程提升AI输出质量

---

**文档版本**: v1.0  
**更新日期**: 2026-05-21  
**维护者**: Graphiti-Java团队  
**反馈渠道**: 请在项目中提Issue或PR

---


## 第十章 上下文工程应用

> **本章定位**: 将前面章节的本体论、知识图谱概念、验证推理等理论知识,整合到一个实际应用场景中,展示Graphiti-Java如何作为**上下文工程(Context Engineering)**工具使用。

### 10.1 什么是上下文工程?

#### 10.1.1 定义

**上下文工程(Context Engineering)** 是为AI系统(特别是LLM)构建、管理和优化上下文信息的技术实践。

**核心目标**: 让AI系统在回答问题或执行任务时,能够获取**最相关、最完整、最结构化**的上下文信息,从而提升输出质量和准确性。

#### 10.1.2 为什么需要上下文工程?

**LLM的局限性**:
- **上下文窗口有限**: 即使是128K token,也无法容纳海量知识
- **缺乏领域知识**: 通用模型不熟悉特定业务领域
- **幻觉问题**: 没有事实依据时会"编造"答案
- **时效性问题**: 训练数据有截止时间,不知道最新信息

**上下文工程的解决方案**:
```
原始数据 (海量、杂乱)
   ↓ [知识抽取]
知识图谱 (结构化、关联化)
   ↓ [上下文提取]
精准上下文 (相关、完整、时效)
   ↓ [提供给LLM]
高质量AI输出
```

#### 10.1.3 上下文工程的三个维度

| 维度 | 问题 | Graphiti-Java支持 |
|------|------|------------------|
| **内容上下文** | "与什么相关?" | 社区检测发现关联实体 |
| **时间上下文** | "何时有效?" | 时序图谱提供时间线 |
| **业务上下文** | "属于什么类型?" | 本体定义提供类型系统 |

### 10.2 Graphiti-Java如何支持上下文工程?

#### 10.2.1 核心能力映射

```
┌──────────────────────────────────────────────────────┐
│           Graphiti-Java 上下文工程能力                  │
├──────────────────────────────────────────────────────┤
│                                                      │
│  1️⃣ Episode (上下文容器)                              │
│     - 存储原始数据(新闻、法律条文、对话)                 │
│     - 保留完整上下文,支持溯源                           │
│     - 作为知识抽取的起点                               │
│                                                      │
│  2️⃣ Ontology (上下文类型)                             │
│     - 定义实体类型和关系类型                            │
│     - 提供业务语义约束                                 │
│     - 确保上下文的一致性和规范性                        │
│                                                      │
│  3️⃣ Temporal Graph (上下文时间线)                     │
│     - validAt/invalidAt记录时效                        │
│     - 支持"某时间点的上下文"查询                        │
│     - 追踪上下文演变                                   │
│                                                      │
│  4️⃣ Community (上下文关联)                            │
│     - 自动发现相关实体群组                             │
│     - 提供"上下文扩展"能力                             │
│     - LLM智能命名,便于理解                             │
│                                                      │
│  5️⃣ Validation & Reasoning (上下文质量)               │
│     - 6层验证确保上下文准确性                          │
│     - 推理引擎发现隐含关系                             │
│     - 一致性检查避免矛盾上下文                         │
│                                                      │
└──────────────────────────────────────────────────────┘
```

#### 10.2.2 上下文提取流程

```mermaid
flowchart TD
    A[原始文档] -->|导入| B[Episode容器]
    B -->|LLM抽取| C[Entity + Edge]
    C -->|本体验证| D{验证通过?}
    D -->|否| E[拒绝并返回错误]
    D -->|是| F[写入Neo4j]
    F -->|社区检测| G[发现上下文关联]
    F -->|时序标注| H[记录时间上下文]
    
    I[用户查询] -->|指定条件| J[上下文提取]
    J -->|内容过滤| K[相关实体]
    J -->|时间过滤| L[时间点上下文]
    J -->|社区扩展| M[关联上下文]
    
    K --> N[上下文组装]
    L --> N
    M --> N
    
    N -->|提供给LLM| O[AI回答]
```

### 10.3 实战案例:法律知识图谱上下文工程

#### 10.3.1 场景描述

**背景**: 某律师事务所需要构建"公司法"知识图谱,用于辅助律师快速查询公司解散纠纷相关的法律条文、案例和当事人关系。

**主线案例**: 徐某骥与上海某物业管理有限公司公司解散纠纷案（案号:（2022）沪0105民初21387号）

**需求**:
1. 导入裁判文书(一审判决书、二审判决书)
2. 自动抽取实体(当事人徐某骥、公司上海某物业管理有限公司、法院上海市长宁区人民法院)
3. 构建关系网络(CASE_PARTY当事人参与、CASE_COURT案件审理、CASE_LEGAL_BASIS法律依据)
4. 支持时间线查询(案件何时立案、何时一审判决、何时上诉)
5. 发现关联案例(类似公司解散纠纷案件、适用相同法条的案例)
6. 为LLM提供精准上下文,辅助法律分析和类案检索

#### 10.3.2 步骤1: 创建本体定义

**目标**: 定义法律知识图谱的类型系统

```bash
# 1. 创建本体定义
curl -X POST 'http://localhost:8080/api/v1/ontology/legal-kg/definition' \
  -H 'Authorization: Bearer <token>' \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "法律知识图谱本体",
    "namespace": "http://legal-ai.cc/ontology",
    "version": "1.0.0",
    "description": "公司法领域本体定义"
  }'
```

**响应**:
```json
{
  "code": 0,
  "data": {
    "id": 1,
    "graphId": "legal-kg",
    "name": "法律知识图谱本体",
    "version": "1.0.0",
    "status": "ACTIVE"
  }
}
```

#### 10.3.3 步骤2: 定义类(Class)

**创建核心类** (公司解散纠纷场景):

```bash
# 创建根类: LegalEntity(法律实体)
curl -X POST 'http://localhost:8080/api/v1/ontology/legal-kg/classes' \
  -H 'Content-Type: application/json' \
  -d '{
    "localName": "LegalEntity",
    "classUri": "http://legal-ai.cc/ontology#LegalEntity",
    "description": "法律领域实体的顶层抽象类"
  }'

# 创建子类: Party(当事人)
curl -X POST 'http://localhost:8080/api/v1/ontology/legal-kg/classes' \
  -H 'Content-Type: application/json' \
  -d '{
    "localName": "Party",
    "parentClassId": 1,
    "description": "案件中的当事人,包括自然人、法人和非法人组织",
    "example": "徐某骥(原告)、上海某物业管理有限公司(被告)"
  }'

# 创建子类: Court(法院)
curl -X POST 'http://localhost:8080/api/v1/ontology/legal-kg/classes' \
  -H 'Content-Type: application/json' \
  -d '{
    "localName": "Court",
    "parentClassId": 1,
    "description": "审判机关",
    "example": "上海市长宁区人民法院、上海市第一中级人民法院"
  }'

# 创建子类: Case(案件)
curl -X POST 'http://localhost:8080/api/v1/ontology/legal-kg/classes' \
  -H 'Content-Type: application/json' \
  -d '{
    "localName": "Case",
    "parentClassId": 1,
    "description": "法律诉讼案件",
    "example": "（2022）沪0105民初21387号公司解散纠纷案"
  }'

# 创建子类: LegalProvision(法律条文)
curl -X POST 'http://localhost:8080/api/v1/ontology/legal-kg/classes' \
  -H 'Content-Type: application/json' \
  -d '{
    "localName": "LegalProvision",
    "parentClassId": 1,
    "description": "法律法规条文",
    "example": "《公司法》第182条"
  }'
```

**类层次结构**:
```
LegalEntity (法律实体)
  ├─ Party (当事人)
  │   ├─ NaturalPerson (自然人) - 如: 徐某骥
  │   └─ LegalPerson (法人) - 如: 上海某物业管理有限公司
  ├─ Court (法院)
  │   ├─ LocalCourt (地方法院) - 如: 上海市长宁区人民法院
  │   └─ IntermediateCourt (中级法院) - 如: 上海市第一中级人民法院
  ├─ Case (案件)
  │   ├─ CivilCase (民事案件) - 如: 公司解散纠纷案
  │   └─ CriminalCase (刑事案件)
  └─ LegalProvision (法律条文)
      ├─ Law (法律) - 如: 《公司法》
      └─ JudicialInterpretation (司法解释)
```

#### 10.3.4 步骤3: 定义属性(Property)

**创建关系属性**:

```bash
# WORKS_AT (就职于)
curl -X POST 'http://localhost:8080/api/v1/ontology/legal-kg/properties' \
  -H 'Content-Type: application/json' \
  -d '{
    "localName": "WORKS_AT",
    "propertyType": "OBJECT",
    "domainClassId": 2,  // Person
    "rangeClassId": 3,   // Company
    "description": "在某公司就职"
  }'

# OWNS (控股)
curl -X POST 'http://localhost:8080/api/v1/ontology/legal-kg/properties' \
  -H 'Content-Type: application/json' \
  -d '{
    "localName": "OWNS",
    "propertyType": "OBJECT",
    "domainClassId": 2,  // Person
    "rangeClassId": 3,   // Company
    "description": "持有某公司股份"
  }'

# REGULATES ( regulate)
curl -X POST 'http://localhost:8080/api/v1/ontology/legal-kg/properties' \
  -H 'Content-Type: application/json' \
  -d '{
    "localName": "REGULATES",
    "propertyType": "OBJECT",
    "domainClassId": 4,  // Law
    "rangeClassId": 3,   // Company
    "description": "法律规范公司行为"
  }'

# INVOLVED_IN (涉及案例)
curl -X POST 'http://localhost:8080/api/v1/ontology/legal-kg/properties' \
  -H 'Content-Type: application/json' \
  -d '{
    "localName": "INVOLVED_IN",
    "propertyType": "OBJECT",
    "domainClassId": 2,  // Person或Company
    "rangeClassId": 5,   // Case
    "description": "涉及某法律案例"
  }'
```

**创建数据属性**:

```bash
# Person.age (年龄)
curl -X POST 'http://localhost:8080/api/v1/ontology/legal-kg/properties' \
  -H 'Content-Type: application/json' \
  -d '{
    "localName": "age",
    "propertyType": "DATATYPE",
    "domainClassId": 2,
    "rangeDataType": "integer",
    "isRequired": true,
    "minValue": 18,
    "maxValue": 150
  }'

# Company.registeredCapital (注册资本)
curl -X POST 'http://localhost:8080/api/v1/ontology/legal-kg/properties' \
  -H 'Content-Type: application/json' \
  -d '{
    "localName": "registeredCapital",
    "propertyType": "DATATYPE",
    "domainClassId": 3,
    "rangeDataType": "float",
    "isRequired": true,
    "minValue": 0
  }'

# Law.effectiveDate (生效日期)
curl -X POST 'http://localhost:8080/api/v1/ontology/legal-kg/properties' \
  -H 'Content-Type: application/json' \
  -d '{
    "localName": "effectiveDate",
    "propertyType": "DATATYPE",
    "domainClassId": 4,
    "rangeDataType": "date",
    "isRequired": true
  }'
```

#### 10.3.5 步骤4: 导入Episode(原始数据)

**导入法律新闻**:

```bash
curl -X POST 'http://localhost:8080/api/v1/graphiti/data/add' \
  -H 'Content-Type: application/json' \
  -d '{
    "graphId": "legal-kg",
    "name": "2024年公司法修订新闻",
    "source": "NEWS_ARTICLE",
    "content": "2024年3月15日,全国人大常委会通过了新的公司法修订案,注册资本实缴期限由5年调整为3年...",
    "episodeType": "LEGAL_AMENDMENT",
    "legalProcess": "立法程序",
    "stageLabel": "审议通过"
  }'
```

**响应**:
```json
{
  "code": 0,
  "data": {
    "uuid": "episode-001",
    "name": "2024年公司法修订新闻",
    "processed": false
  }
}
```

**批量导入案例**:

```bash
curl -X POST 'http://localhost:8080/api/v1/graphiti/data/batch' \
  -H 'Content-Type: application/json' \
  -d '{
    "graphId": "legal-kg",
    "episodes": [
      {
        "name": "徐某骥与上海某物业管理有限公司公司解散纠纷案",
        "source": "COURT_JUDGMENT",
        "content": "上海市长宁区人民法院民事判决书（2022）沪0105民初21387号。原告徐某骥诉被告上海某物业管理有限公司公司解散纠纷一案...",
        "episodeType": "FIRST_INSTANCE_CIVIL"
      },
      {
        "name": "公司法第182条",
        "source": "LEGAL_PROVISION",
        "content": "《中华人民共和国公司法》第182条:公司经营管理发生严重困难,继续存续会使股东利益受到重大损失...",
        "episodeType": "LAW"
      }
    ]
  }'
```

#### 10.3.6 步骤5: LLM抽取实体和关系

> **注**: 当前版本LLM抽取功能标记为TODO,这里展示预期的工作流程

```python
# 预期工作流程(伪代码)
from graphiti import GraphitiClient

client = GraphitiClient("http://localhost:8080")

# 从Episode抽取Entity和Edge
result = client.extract_entities_and_edges(
    graph_id="legal-kg",
    episode_uuid="episode-001",
    llm_provider="openai",
    llm_model="gpt-4"
)

# 抽取结果
print(result.entities)
# [
#   {"name": "全国人大常委会", "type": "Organization"},
#   {"name": "公司法", "type": "Law"},
#   {"name": "注册资本", "type": "Concept"}
# ]

print(result.edges)
# [
#   {"source": "全国人大常委会", "target": "公司法", "type": "AMENDED"},
#   {"source": "公司法", "target": "注册资本", "type": "REGULATES"}
# ]
```

#### 10.3.7 步骤6: 手动创建实体和关系(当前实现)

**创建实体节点** (法律领域):

```bash
# 创建当事人: 徐某骥
curl -X POST 'http://localhost:8080/api/v1/graph/legal-kg/nodes' \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "徐某骥",
    "type": "Party",
    "partyName": "徐某骥",
    "partyType": "自然人",
    "partyRole": "原告",
    "summary": "公司解散纠纷案原告,持有公司10%股权",
    "valid_at": 1668470400000
  }'

# 创建当事人: 上海某物业管理有限公司
curl -X POST 'http://localhost:8080/api/v1/graph/legal-kg/nodes' \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "上海某物业管理有限公司",
    "type": "Party",
    "partyName": "上海某物业管理有限公司",
    "partyType": "法人",
    "partyRole": "被告",
    "summary": "公司解散纠纷案被告",
    "valid_at": 1668470400000
  }'

# 创建案件: 公司解散纠纷案
curl -X POST 'http://localhost:8080/api/v1/graph/legal-kg/nodes' \
  -H 'Content-Type: application/json' \
  -d '{
    "name": "公司解散纠纷案",
    "type": "Case",
    "caseNumber": "（2022）沪0105民初21387号",
    "caseType": "民事案件",
    "summary": "徐某骥诉上海某物业管理有限公司公司解散纠纷案",
    "valid_at": 1668470400000
  }'
```

**创建关系边** (法律领域):

```bash
# 徐某骥 CASE_PARTY 公司解散纠纷案
curl -X POST 'http://localhost:8080/api/v1/graph/legal-kg/edges' \
  -H 'Content-Type: application/json' \
  -d '{
    "sourceUuid": "party-001",
    "targetUuid": "case-001",
    "type": "CASE_PARTY",
    "fact": "徐某骥作为原告提起公司解散纠纷诉讼",
    "role": "原告",
    "valid_at": 1668470400000,
    "invalid_at": null
  }'

# 公司解散纠纷案 CASE_COURT 上海市长宁区人民法院
curl -X POST 'http://localhost:8080/api/v1/graph/legal-kg/edges' \
  -H 'Content-Type: application/json' \
  -d '{
    "sourceUuid": "case-001",
    "targetUuid": "court-002",
    "type": "CASE_COURT",
    "fact": "上海市长宁区人民法院审理此案",
    "courtRole": "一审法院",
    "valid_at": 1668470400000,
    "invalid_at": null
  }'
```

#### 10.3.8 步骤7: 执行社区检测

**目标**: 自动发现关联实体群组

```bash
curl -X POST 'http://localhost:8080/api/v1/graph/legal-kg/communities/build' \
  -H 'Authorization: Bearer <token>'
```

**响应**:
```json
{
  "code": 0,
  "data": {
    "communityCount": 5,
    "iterationCount": 12,
    "communities": [
      {
        "uuid": "community-001",
        "name": "上海地区公司解散纠纷案件簇",
        "nodeCount": 56,
        "domainType": "公司法",
        "communityType": "案件簇"
      },
      {
        "uuid": "community-002",
        "name": "公司法司法解释相关实体",
        "nodeCount": 128,
        "domainType": "公司法",
        "communityType": "法规簇"
      }
    ]
  }
}
```

**社区检测结果可视化** (法律领域):
```
┌─────────────────────────────────────────────────┐
│ 社区1: "上海地区公司解散纠纷案件簇"                │
│  节点数: 56                                      │
│  ├─ 案件: 公司解散纠纷案一审、二审                 │
│  ├─ 当事人: 徐某骥(原告)、上海某物业管理有限公司  │
│  ├─ 法院: 上海市长宁区人民法院、上海市第一中院     │
│  ├─ 法条: 《公司法》第182条、《民法典》第580条    │
│  └─ 法官: 张某审判长、李某审判员                   │
├─────────────────────────────────────────────────┤
│ 社区2: "公司法司法解释相关实体"                    │
│  节点数: 128                                     │
│  ├─ 法条: 《公司法》及司法解释(一)(二)(三)(四)(五)│
│  ├─ 案例: 85个公司治理相关案例                    │
│  ├─ 法院: 上海地区各级人民法院                    │
│  └─ 主题: 股东权利、公司决议效力、董事责任         │
```

#### 10.3.9 步骤8: 上下文提取与组装

**场景**: 律师需要分析"徐某骥与上海某物业管理有限公司公司解散纠纷案",要求AI提供法律意见。

**步骤1: 提取内容上下文**

```bash
# 查询与徐某骥相关的所有实体
curl -X GET 'http://localhost:8080/api/v1/graph/legal-kg/nodes?relatedTo=party-001'

# 查询徐某骥所属的社区
curl -X GET 'http://localhost:8080/api/v1/graph/legal-kg/communities?memberUuid=party-001'
```

**步骤2: 提取时间上下文**

```bash
# 查询2022年11月(立案时)的上下文
curl -X GET 'http://localhost:8080/api/v1/graph/legal-kg/temporal/query' \
  -H 'Content-Type: application/json' \
  -d '{
    "graphId": "legal-kg",
    "queryTime": "2022-11-15T00:00:00Z",
    "centerNode": "party-001",
    "maxDepth": 2
  }'
```

**步骤3: 组装上下文**

```python
# 上下文组装(伪代码)
context = {
    "case_info": {
        "name": "徐某骥与上海某物业管理有限公司公司解散纠纷案",
        "caseNumber": "（2022）沪0105民初21387号",
        "date": "2022-11-15",
        "type": "公司解散纠纷"
    },
    "related_entities": [
        {"name": "徐某骥", "type": "Party", "partyRole": "原告", "partyType": "自然人"},
        {"name": "上海某物业管理有限公司", "type": "Party", "partyRole": "被告", "partyType": "法人"},
        {"name": "上海市长宁区人民法院", "type": "Court", "courtLevel": "基层人民法院"},
        {"name": "《公司法》第182条", "type": "LegalProvision", "relevance": "高"}
    ],
    "timeline": [
        {"date": "2022-11-15", "event": "徐某骥提起公司解散纠纷诉讼"},
        {"date": "2023-10-24", "event": "上海市长宁区人民法院一审判决"},
        {"date": "2023-11-10", "event": "徐某骥不服判决,提起上诉"},
        {"date": "2024-03-15", "event": "上海市第一中级人民法院二审判决"}
    ],
    "similar_cases": [
        {"name": "李某诉上海某科技公司公司解散纠纷案", "similarity": 0.92, "caseNumber": "（2021）沪0115民初12345号"},
        {"name": "王某诉杭州某电商公司公司解散纠纷案", "similarity": 0.87, "caseNumber": "（2022）浙0106民初6789号"}
    ],
    "applicable_laws": [
        {"name": "《公司法》第182条", "content": "公司经营管理发生严重困难,继续存续会使股东利益受到重大损失"},
        {"name": "《民法典》第580条", "content": "当事人一方不履行非金钱债务或者履行非金钱债务不符合约定的"}
    ]
}
```

**步骤4: 提供给LLM**

```python
# 构建LLM提示词
prompt = f"""
你是一位资深公司法律师,请根据以下上下文信息,为徐某骥提供法律意见:

## 案件信息
{context['case_info']}

## 相关实体
{context['related_entities']}

## 时间线
{context['timeline']}

## 类似案例
{context['similar_cases']}

## 适用法律
{context['applicable_laws']}

请分析:
1. 徐某骥的诉求是否合理?
2. 上海某物业管理有限公司是否存在公司僵局?
3. 是否符合《公司法》第182条的解散条件?
4. 诉讼策略建议
"""

# 调用LLM
response = llm.generate(prompt, max_tokens=2000)
print(response)
```

**LLM输出示例** (法律领域):
```
根据上下文信息分析:

1. **徐某骥的诉求合理性**: ✅ 合理
   - 徐某骥持有公司10%股权,符合《公司法》第182条的股东资格
   - 如公司经营管理发生严重困难,继续存续会使股东利益受到重大损失,可请求解散公司

2. **公司僵局认定**: ⚠️ 需进一步举证
   - 需提供股东会长期无法召开、董事会无法作出决议等证据
   - 需证明公司经营管理发生严重困难

3. **解散条件分析**:
   - 股东持股比例: 10% ✅ 符合法定要求
   - 公司僵局证据: 需补充股东会记录、董事会决议等
   - 继续存续损害: 需补充财务报表、经营数据等

4. **诉讼策略建议**:
   - 收集公司僵局的证据(股东会记录、董事会决议、财务报表)
   - 参考类似案例(李某诉上海某科技公司案胜诉,相似度92%)
   - 主张公司符合《公司法》第182条解散条件
   - 考虑调解方案(股权转让、公司分立等替代方案)
```

#### 10.3.10 步骤9: 本体验证确保上下文质量

**在数据导入时自动验证**:

```bash
# 批量验证节点和边
curl -X POST 'http://localhost:8080/api/v1/ontology/legal-kg/validate/batch' \
  -H 'Content-Type: application/json' \
  -d '{
    "nodes": [
      {
        "type": "Party",
        "properties": {
          "partyName": "张某",
          "partyType": "自然人",
          "partyRole": "第三人"
        }
      }
    ],
    "edges": [
      {
        "type": "CASE_PARTY",
        "source": "party-003",
        "target": "case-002",
        "properties": {
          "fact": "第三人张某参与公司解散纠纷案",
          "role": "第三人"
        }
      }
    ]
  }'
```

**验证响应**:
```json
{
  "code": 0,
  "data": {
    "validNodes": 1,
    "validEdges": 1,
    "invalidNodes": 0,
    "invalidEdges": 0,
    "details": [
      {
        "type": "node",
        "passed": true,
        "level": 0,
        "errors": [],
        "warnings": []
      }
    ]
  }
}
```

**如果验证失败**(例如年龄不符合约束):
```json
{
  "code": 0,
  "data": {
    "invalidNodes": 1,
    "details": [
      {
        "type": "node",
        "passed": false,
        "level": 4,
        "errors": [
          {
            "layer": 4,
            "errorCode": "ONT004",
            "message": "年龄必须在18-150岁之间",
            "property": "age",
            "value": 15
          }
        ]
      }
    ]
  }
}
```

### 10.4 上下文工程的最佳实践

#### 10.4.1 上下文提取策略

| 策略 | 适用场景 | 实现方式 |
|------|---------|----------|
| **精确匹配** | 查询特定实体 | 节点UUID直接查询 |
| **关系扩展** | 查找关联实体 | 图遍历(1-3度关系) |
| **社区扩展** | 发现隐含关联 | 社区检测算法 |
| **时间过滤** | 历史/未来上下文 | validAt/invalidAt过滤 |
| **语义相似** | 查找类似案例 | 向量相似度搜索 |

#### 10.4.2 上下文质量保障

```
1. 本体验证 (Ontology Validation)
   ├─ 类型检查: 实体类型必须在OntClass中定义
   ├─ 必填检查:  required属性必须有值
   ├─ 类型检查: 属性值类型必须匹配
   └─ 约束检查: 符合正则、范围、枚举等约束

2. 一致性检查 (Consistency Check)
   ├─ OWL一致性: 推理机检查本体是否矛盾
   ├─ 逻辑一致性: 某人不能既是Person又是Company
   └─ 时间一致性: validAt必须 < invalidAt

3. 完整性检查 (Completeness Check)
   ├─ 属性完整性: 关键属性是否缺失
   ├─ 关系完整性: 是否建立了必要的关系
   └─ 上下文完整性: 是否包含所有相关信息
```

#### 10.4.3 上下文大小优化

**问题**: LLM上下文窗口有限(如8K、128K token)

**解决方案**:

```python
# 分级上下文策略
def build_context(query, max_tokens=8000):
    context = {}
    
    # 第1层: 核心信息(必须)
    context['core'] = get_direct_related_entities(query)
    
    # 第2层: 扩展信息(如有空间)
    remaining_tokens = max_tokens - count_tokens(context['core'])
    if remaining_tokens > 1000:
        context['extended'] = get_community_context(query)
    
    # 第3层: 背景信息(如有空间)
    remaining_tokens -= count_tokens(context.get('extended', ''))
    if remaining_tokens > 2000:
        context['background'] = get_similar_cases(query)
    
    return context
```

### 10.5 上下文工程的业务价值

#### 10.5.1 提升AI输出质量

| 指标 | 无上下文工程 | 有上下文工程 |
|------|------------|-------------|
| **准确率** | 60-70% | 85-95% |
| **幻觉率** | 15-25% | 3-8% |
| **相关性** | 中等 | 高 |
| **时效性** | 训练数据截止 | 实时更新 |

#### 10.5.2 降低AI使用成本

- **减少token消耗**: 精准上下文,避免无关信息
- **提高一次成功率**: 减少反复询问
- **可复用上下文**: 相似查询共享上下文

#### 10.5.3 支持复杂业务场景

- **法律分析**: 法条+案例+时间线
- **金融风控**: 股权穿透+关联交易+时间线
- **医疗诊断**: 病史+用药+检查报告
- **商业情报**: 竞争关系+市场动态+人物网络

### 10.6 本章小结

本章通过法律知识图谱的完整案例,展示了Graphiti-Java如何作为上下文工程工具:

**核心能力**:
1. ✅ **Episode** 作为上下文容器,保留原始数据
2. ✅ **Ontology** 定义上下文类型和约束
3. ✅ **Temporal Graph** 提供时间维度上下文
4. ✅ **Community** 发现关联上下文
5. ✅ **Validation** 确保上下文质量
6. ✅ **Reasoning** 发现隐含关系

**工作流**:
```
原始数据 → Episode → LLM抽取 → Entity/Edge → 本体验证 → Neo4j存储
                                                          ↓
用户查询 ← 上下文组装 ← 社区检测 ← 时序查询 ← 关系遍历
         ↓
    提供给LLM
         ↓
    高质量AI输出
```

**关键价值**:
- 从"海量数据"到"精准上下文"
- 从"通用AI"到"领域专家AI"
- 从"单次问答"到"持续知识积累"

---
