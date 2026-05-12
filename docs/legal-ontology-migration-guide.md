# 法律知识图谱本体导入指南

> 基于典型类案（裁判文书）与法律条文设计的法律领域本体 V2
> 适用于 graphiti-java 应用

## 一、数据概述

### 1.1 数据来源

| 数据源 | 内容 | 数量 |
|--------|------|------|
| 人民法院案例库 | 典型民商事案例 JSON 文件 | 500+ 案例 |
| 商事调解条例（国务院令第827号） | 2026年5月1日起施行的行政法规 | 33条 |
| 中华人民共和国民法典 | 民商事基本法律 | 相关条文 |
| 最高人民法院司法解释 | 公司法解释（二）等 | 相关条文 |
| 民事诉讼法 | 2023年修正版 | 相关条文 |

### 1.2 本体设计概览

```
法律知识图谱本体 V2
├── 案件域 (Case Domain)
│   ├── Case（案件基类）
│   ├── CommercialCase（商事案件）
│   ├── CivilCase（民事案件）
│   ├── CriminalCase（刑事案件）
│   ├── AdministrativeCase（行政案件）
│   └── ExecutionCase（执行案件）
│
├── 当事人与代理人 (Parties)
│   ├── Party（当事人）
│   ├── LegalPerson（法人当事人）
│   └── Lawyer（律师）
│
├── 司法机构 (Judiciary)
│   ├── Court（法院）
│   └── Judge（法官）
│
├── 法律条文 (Legal Provisions)
│   ├── LegalProvision（法律条文）
│   └── LegalDocument（法规文件）
│
├── 裁判文书 (Judgment Documents)
│   ├── JudgmentDocument（裁判文书）
│   └── CaseReasoning（裁判要旨）
│
├── 证据与事实 (Evidence & Facts)
│   ├── Evidence（证据）
│   └── CaseFact（案件事实）
│
└── 商事调解 (Commercial Mediation)
    ├── CommercialMediationOrganization（商事调解组织）
    ├── Mediator（调解员）
    └── MediationAgreement（调解协议）
```

### 1.3 关系类型概览

| 关系名称 | 源实体 | 目标实体 | 说明 |
|---------|--------|---------|------|
| CASE_PARTY | Case | Party | 案件-当事人参与关系 |
| CASE_JUDGE | Case | Judge | 案件-法官审理关系 |
| CASE_COURT | Case | Court | 案件-法院管辖关系 |
| CASE_LEGAL_PROVISION | Case | LegalProvision | 案件-适用法条关系 |
| CASE_JUDGMENT | Case | JudgmentDocument | 案件-裁判文书关系 |
| CASE_EVIDENCE | Case | Evidence | 案件-证据关系 |
| HAS_CASE_FACT | Case | CaseFact | 案件-事实描述关系 |
| HAS_CASE_REASONING | Case | CaseReasoning | 案件-裁判要旨关系 |
| LEGAL_PROVISION_RELATED | LegalProvision | LegalProvision | 法条-关联法条关系 |
| CASE_MEDIATION_ORG | Case | CommercialMediationOrganization | 案件-调解组织关系 |
| CASE_MEDIATION_AGREEMENT | Case | MediationAgreement | 案件-调解协议关系 |
| ORG_MEDIATOR | CommercialMediationOrganization | Mediator | 调解组织-调解员关系 |
| AGREEMENT_JUDICIALLY_CONFIRMED | MediationAgreement | Court | 调解协议-司法确认关系 |
| COURT_HIERARCHY | Court | Court | 法院-上级法院层级关系 |

---

## 二、导入步骤

### 2.1 前提条件

- [ ] PostgreSQL 14+ 已安装并运行
- [ ] Neo4j 5.x 已安装并运行（带 APOC 插件）
- [ ] graphiti-java 应用已编译并可连接数据库
- [ ] 已执行 V5__create_ontology_tables.sql 创建本体表结构

### 2.2 执行顺序

```sql
执行顺序    SQL 文件                          说明
--------    ------------------------------    ----------------------
Step 1      V5__create_ontology_tables.sql    创建本体系统表结构（已执行则跳过）
Step 2      V6__seed_legal_ontology_v2.sql   导入本体定义（类、属性、约束）
Step 3      V7__seed_legal_neo4j_data.sql    导入 Neo4j 示例数据（节点和关系）
```

### 2.3 Step 1: 确认表结构（已执行则跳过）

```bash
# 连接到 PostgreSQL
psql -U postgres -d graphiti -h localhost

# 确认本体表存在
\d ont_definition
\d ont_class
\d ont_property
\d ont_constraint
\d ont_version_history
\d ont_mapping
```

### 2.4 Step 2: 导入本体定义数据

```bash
# 方式一：使用 psql 命令行
psql -U postgres -d graphiti -h localhost -f sql/postgresql/V6__seed_legal_ontology_v2.sql

# 方式二：在数据库客户端（如 DBeaver）中打开并执行
```

**预期结果：**
- `ont_definition` 表中插入 1 条本体定义记录
- `ont_class` 表中插入 20 个实体类定义
- `ont_property` 表中插入约 35 个属性定义
- `ont_constraint` 表中插入 6 个约束规则
- `ont_version_history` 表中插入 1 条版本历史记录

**验证查询：**
```sql
-- 查看本体定义
SELECT id, graph_id, name, version, status, description
FROM ont_definition
WHERE graph_id = 'legal-knowledge-graph';

-- 查看所有类
SELECT local_name, class_uri, description, domain_hint
FROM ont_class
WHERE definition_id = (SELECT id FROM ont_definition WHERE graph_id = 'legal-knowledge-graph' ORDER BY id DESC LIMIT 1)
ORDER BY id;

-- 查看类统计
SELECT
  (SELECT COUNT(*) FROM ont_class WHERE definition_id = (SELECT id FROM ont_definition WHERE graph_id = 'legal-knowledge-graph' ORDER BY id DESC LIMIT 1)) AS class_count,
  (SELECT COUNT(*) FROM ont_property WHERE definition_id = (SELECT id FROM ont_definition WHERE graph_id = 'legal-knowledge-graph' ORDER BY id DESC LIMIT 1)) AS property_count,
  (SELECT COUNT(*) FROM ont_constraint WHERE definition_id = (SELECT id FROM ont_definition WHERE graph_id = 'legal-knowledge-graph' ORDER BY id DESC LIMIT 1)) AS constraint_count;
```

### 2.5 Step 3: 导入 Neo4j 图谱数据

#### 方式一：使用 Neo4j Browser

1. 打开 Neo4j Browser（http://localhost:7474）
2. 登录后，选择数据库：`legal-knowledge-graph`（或默认数据库）
3. 将 `V7__seed_legal_neo4j_data.sql` 中的 Cypher 语句分段复制执行

#### 方式二：使用 cypher-shell

```bash
# 连接到 Neo4j
cypher-shell -u neo4j -p your_password -d neo4j < sql/postgresql/V7__seed_legal_neo4j_data.sql

# 或使用 --format=plain 输出
cypher-shell -u neo4j -p your_password -d neo4j --format=plain -f sql/postgresql/V7__seed_legal_neo4j_data.sql
```

**注意：** V7 文件中包含 PostgreSQL 特定的 `:BEGIN` 块注释和 `//` Cypher 注释，需清理后执行 Cypher 部分。

#### 方式三：使用 APOC

如果使用 APOC，可以分批导入：

```cypher
// 1. 创建约束
CREATE CONSTRAINT case_number_unique_v2 IF NOT EXISTS
FOR (n:Case) REQUIRE n.caseNumber IS UNIQUE;

// 2. 批量导入节点（可使用 apoc.periodic.iterate）
CALL apoc.periodic.iterate(
  'RETURN [...] AS data',
  'CREATE (n:Case) SET n += data',
  {batchSize: 100}
);
```

**预期结果：**
- 约 46 个节点（法院、法律条文、案件、当事人、裁判文书等）
- 约 40 条关系（案件-法条、案件-当事人、法院层级等）

**验证查询：**
```cypher
// 查看所有节点类型及数量
MATCH (n) WITH labels(n)[0] AS label, count(*) AS cnt
RETURN label, cnt ORDER BY cnt DESC;

// 查看关键案件
MATCH (ca:Case)
RETURN ca.caseNumber, ca.caseName, ca.caseStatus, ca.courtLevel;

// 查看法条引用关系
MATCH (ca:Case)-[r:CASE_LEGAL_PROVISION]->(lp:LegalProvision)
RETURN ca.caseName, lp.lawName, lp.articleNumber, r.usageType
ORDER BY ca.caseNumber;
```

---

## 三、数据说明

### 3.1 真实案例数据

已导入以下真实典型案例（来源：人民法院案例库 2025年入库）：

| 案例编号 | 案件名称 | 案由 | 典型意义 |
|---------|---------|------|---------|
| 2025-08-2-283-001 | 徐某骥诉上海某物业管理有限公司等公司解散纠纷案 | 公司解散 | 公司僵局判断标准：运营良好则不解散 |
| 2025-01-2-078-001 | 谭某诉吴某、雒某债权人撤销权纠纷案 | 债权人撤销权 | 部分被告被监禁时的管辖法院确定 |

### 3.2 法律条文数据

| 类别 | 条文数量 | 主要内容 |
|------|---------|---------|
| 商事调解条例 | 6条（第1/2/8/12/14/22条） | 适用范围、设立条件、调解员资质、调解协议 |
| 民法典 | 2条（第69/70条） | 法人解散与清算 |
| 公司法司法解释（二） | 1条（第1条） | 公司解散诉讼的具体情形 |
| 民事诉讼法 | 3条（第22/23/24条） | 管辖法院确定规则 |

### 3.3 商事调解数据

根据《商事调解条例》（2026年5月1日施行）设计了：
- 商事调解组织：设立条件、资质要求
- 调解员：资质条件（法律职业资格+3年经验等）
- 调解协议：内容要素、司法确认

---

## 四、批量导入真实案例

### 4.1 案例数据格式

人民法院案例库中的案例 JSON 文件格式如下：

```json
{
  "title": "案件名称",
  "content": "完整裁判文书内容",
  "case_type": "民事案例",
  "api_data": {
    "cpws_al_ajzh": "案号",
    "cpws_al_sort_name": "案由",
    "cpws_al_slfy_name": "审理法院",
    "cpws_al_zs_date": "作出日期",
    "cpws_al_cpyz": "裁判要旨",
    "cpws_al_infos": "案件基本信息"
  }
}
```

### 4.2 批量导入脚本思路

```python
import json
import os
from pathlib import Path

# 遍历所有案例文件
case_dir = Path("D:/work/docs/legal-case/人民法院案例库")
for json_file in case_dir.rglob("*.json"):
    with open(json_file, encoding="utf-8") as f:
        case_data = json.load(f)

    # 解析案件信息
    title = case_data.get("title", "")
    content = case_data.get("content", "")
    api_data = case_data.get("api_data", {})

    case_number = api_data.get("cpws_al_ajzh", "")
    case_type = api_data.get("cpws_al_sort_name", "")
    court_name = api_data.get("cpws_al_slfy_name", "")
    judgment_date = api_data.get("cpws_al_zs_date", "")
    reasoning = api_data.get("cpws_al_cpyz", "")

    # 生成 Cypher 语句
    cypher = f"""
    MERGE (ca:Case:CommercialCase {{
      caseNumber: '{case_number}',
      caseName: '{title}',
      caseType: '民事',
      caseStatus: '结案',
      caseSummary: '{content[:500]}...',
      courtLevel: '{court_name}'
    }})
    """

    print(cypher)
```

### 4.3 完整批量导入

建议使用 Python + Neo4j Driver 实现：

```python
from neo4j import GraphDatabase

class LegalCaseImporter:
    def __init__(self, uri, user, password):
        self.driver = GraphDatabase.driver(uri, auth=(user, password))

    def import_case(self, case_data):
        with self.driver.session() as session:
            session.run("""
                MERGE (ca:Case:CommercialCase {
                    caseNumber: $caseNumber
                })
                SET ca.caseName = $caseName,
                    ca.caseType = $caseType,
                    ca.caseSummary = $caseSummary,
                    ca.courtLevel = $courtLevel
            """, case_data)

    def import_party(self, party_data):
        with self.driver.session() as session:
            session.run("""
                MERGE (p:Party {
                    partyName: $partyName
                })
                SET p.partyType = $partyType,
                    p.partyRole = $partyRole
            """, party_data)

    def import_relationship(self, source_id, target_id, rel_type, props):
        with self.driver.session() as session:
            session.run(f"""
                MATCH (a {{uuid: $sourceId}})
                MATCH (b {{uuid: $targetId}})
                CALL apoc.create.relationship(a, '{rel_type}', $props, b) YIELD rel
                RETURN rel
            """, sourceId=source_id, targetId=target_id, props=props)
```

---

## 五、API 接口使用

### 5.1 本体定义 API

```bash
# 创建本体定义
POST /api/admin/ontology/definition
{
  "graphId": "legal-knowledge-graph",
  "name": "法律知识图谱本体 V2",
  "namespace": "http://legal-ai.cc/ontology",
  "version": "2.0.0",
  "description": "基于典型案例与商事调解条例的法律本体"
}

# 获取本体完整信息
GET /api/admin/ontology/full?graphId=legal-knowledge-graph

# 列出所有类
GET /api/admin/ontology/classes?graphId=legal-knowledge-graph

# 列出所有类层级
GET /api/admin/ontology/hierarchy?graphId=legal-knowledge-graph

# 列出所有属性
GET /api/admin/ontology/properties?graphId=legal-knowledge-graph
```

### 5.2 实体数据导入 API

```bash
# 批量添加实体
POST /api/admin/import/entities
{
  "graphId": "legal-knowledge-graph",
  "nodes": [
    {
      "name": "Party",
      "properties": {
        "partyName": "徐某骥",
        "partyType": "自然人",
        "partyRole": "原告"
      }
    }
  ]
}

# 添加事实三元组
POST /api/admin/import/fact-triple
{
  "graphId": "legal-knowledge-graph",
  "sourceUuid": "party-xu-jiji-001",
  "targetUuid": "case-xj-company-dissolution-001",
  "relationType": "CASE_PARTY",
  "fact": "原告持股39.54%诉请解散公司",
  "properties": {
    "role": "上诉人"
  }
}
```

---

## 六、FAQ

### Q1: 如何扩展新的案件类型？
A: 在 V6 SQL 文件中，找到 `ont_class` 插入语句，添加新的案件子类（如 `CriminalCase` 已预定义）。

### Q2: 如何添加新的法律条文？
A: 直接在 V7 SQL 中添加 `LegalProvision` 节点即可，注意设置 `provisionId` 唯一标识。

### Q3: 如何处理案例中的多名当事人？
A: 使用 `CASE_PARTY` 关系，为每个当事人创建独立的 `Party` 节点，通过不同 `role` 属性区分。

### Q4: Neo4j 数据导入失败怎么办？
A: 检查：
1. Neo4j 是否运行中
2. APOC 插件是否已安装
3. 约束名称是否已存在（IF NOT EXISTS 会忽略）
4. 查看 Neo4j 日志排查具体错误

### Q5: 如何将本体定义与 Neo4j 数据关联？
A: 通过 `graph_id = 'legal-knowledge-graph'` 进行关联，应用层会根据此 ID 查询对应的本体定义和数据。

---

## 七、文件清单

| 文件 | 路径 | 说明 |
|------|------|------|
| V5__create_ontology_tables.sql | sql/postgresql/ | 本体系统表结构 |
| V6__seed_legal_ontology_v2.sql | sql/postgresql/ | 本体定义数据（类/属性/约束） |
| V7__seed_legal_neo4j_data.sql | sql/postgresql/ | Neo4j 图谱数据（节点/关系） |
| 本文档 | docs/ | 导入指南 |

---

*最后更新：2026-05-12*
