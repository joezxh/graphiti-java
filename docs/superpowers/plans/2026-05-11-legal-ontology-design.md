# 法律知识图谱本体设计

## 1. 概述

本文档定义法律领域知识图谱的本体论（Ontology），涵盖典型案例分析、商事调解、民事诉讼等场景。
基于 ontograph-java 框架的 Entity-Edge 双层模型设计。

## 2. 系统架构

```
┌─────────────────────────────────────────────┐
│                 MySQL                       │
│  (元数据: ontology, graph_metadata, nodes)   │
│                                             │
│  graphiti_ontology.entities = JSON 实体定义   │
│  graphiti_ontology.edges     = JSON 关系定义  │
└─────────────────┬───────────────────────────┘
                  │ REST API
                  ▼
┌─────────────────────────────────────────────┐
│                 Neo4j                        │
│  (图数据: nodes + relationships)            │
│                                             │
│  Node {uuid, name, type, properties, embed}  │
│  Edge {uuid, type, source, target, props}   │
└─────────────────────────────────────────────┘
```

## 3. 实体类型定义 (Entity Types)

### 3.1 基类

| 实体类型 | 说明 | 父类型 |
|---------|------|--------|
| Entity | 基础实体（框架内置） | - |
| Case | 案件基类 | Entity |
| LegalProvision | 法律条文基类 | Entity |

### 3.2 案件实体 (Case)

```json
{
  "name": "Case",
  "displayName": "案件",
  "description": "案件基类，包含所有案件的公共属性",
  "extends": "Entity",
  "properties": {
    "caseNumber": {
      "type": "string",
      "required": true,
      "description": "案件编号",
      "example": "(2024)沪01民初1234号"
    },
    "caseName": {
      "type": "string",
      "required": true,
      "description": "案件名称",
      "example": "上海某某贸易公司诉某某物流公司货物运输合同纠纷"
    },
    "caseType": {
      "type": "string",
      "required": true,
      "description": "案件类型",
      "enum": ["民事", "刑事", "行政", "商事", "执行", "赔偿"]
    },
    "caseStatus": {
      "type": "string",
      "description": "案件状态",
      "enum": ["立案", "审理中", "调解中", "判决", "上诉中", "结案", "撤销"]
    },
    "filingDate": {
      "type": "date",
      "description": "立案日期"
    },
    "closedDate": {
      "type": "date",
      "description": "结案日期"
    },
    "amountInDispute": {
      "type": "decimal",
      "description": "争议金额(元)"
    },
    "summary": {
      "type": "text",
      "description": "案件摘要"
    }
  }
}
```

### 3.3 子类案件

| 类型 | 说明 | 特有属性 |
|------|------|---------|
| CommercialCase | 商事案件 | disputeType(纠纷类型), mediationAttempted(是否经过调解) |
| CivilCase | 民事案件 | subjectMatter(诉讼标的) |
| CriminalCase | 刑事案件 | crimeType(罪名), publicProsecutor(公诉机关) |
| AdministrativeCase | 行政案件 | administrativePenalty(行政处罚) |

### 3.4 当事人实体 (Party)

```json
{
  "name": "Party",
  "displayName": "当事人",
  "description": "案件中的当事人（原告、被告、第三人等）",
  "properties": {
    "name": {
      "type": "string",
      "required": true,
      "description": "姓名或名称"
    },
    "partyType": {
      "type": "string",
      "required": true,
      "enum": ["自然人", "法人", "非法人组织"],
      "description": "当事人类型"
    },
    "idNumber": {
      "type": "string",
      "description": "身份证号/统一社会信用代码"
    },
    "role": {
      "type": "string",
      "required": true,
      "enum": ["原告", "被告", "第三人", "上诉人", "被上诉人", "申请人", "被申请人", "公诉人", "辩护人"],
      "description": "诉讼角色"
    },
    "address": {
      "type": "string",
      "description": "住所地"
    },
    "contact": {
      "type": "string",
      "description": "联系方式"
    },
    "isEnterprise": {
      "type": "boolean",
      "description": "是否企业"
    }
  }
}
```

### 3.5 法院实体 (Court)

```json
{
  "name": "Court",
  "displayName": "法院",
  "description": "审判机关",
  "properties": {
    "name": {
      "type": "string",
      "required": true,
      "description": "法院名称"
    },
    "level": {
      "type": "string",
      "enum": ["最高人民法院", "高级人民法院", "中级人民法院", "基层人民法院", "专门法院"],
      "description": "法院级别"
    },
    "location": {
      "type": "string",
      "description": "所在地"
    },
    "jurisdiction": {
      "type": "string",
      "description": "管辖范围"
    },
    "parentCourt": {
      "type": "string",
      "description": "上级法院名称"
    }
  }
}
```

### 3.6 法官实体 (Judge)

```json
{
  "name": "Judge",
  "displayName": "法官",
  "description": "案件审判人员",
  "properties": {
    "name": {
      "type": "string",
      "required": true,
      "description": "法官姓名"
    },
    "title": {
      "type": "string",
      "enum": ["审判长", "审判员", "人民陪审员", "书记员", "副院长", "院长"],
      "description": "职务"
    },
    "courtName": {
      "type": "string",
      "description": "所属法院"
    },
    "specialty": {
      "type": "string",
      "description": "专业领域"
    }
  }
}
```

### 3.7 法律条文实体 (LegalProvision)

```json
{
  "name": "LegalProvision",
  "displayName": "法律条文",
  "description": "法律、行政法规、司法解释等条文",
  "extends": "Entity",
  "properties": {
    "provisionId": {
      "type": "string",
      "required": true,
      "description": "条文编号",
      "example": "商事调解条例第八条"
    },
    "articleNumber": {
      "type": "string",
      "required": true,
      "description": "条款序号",
      "example": "第八条"
    },
    "content": {
      "type": "text",
      "required": true,
      "description": "条文内容"
    },
    "lawName": {
      "type": "string",
      "required": true,
      "description": "所属法律名称",
      "example": "商事调解条例"
    },
    "lawType": {
      "type": "string",
      "enum": ["法律", "行政法规", "司法解释", "部门规章", "地方性法规"],
      "description": "法律类型"
    },
    "chapter": {
      "type": "string",
      "description": "所属章节"
    },
    "effectiveDate": {
      "type": "date",
      "description": "生效日期"
    },
    "abolishedDate": {
      "type": "date",
      "description": "废止日期"
    },
    "keywords": {
      "type": "string",
      "description": "关键词标签（逗号分隔）"
    }
  }
}
```

### 3.8 律师实体 (Lawyer)

```json
{
  "name": "Lawyer",
  "displayName": "律师",
  "description": "执业律师",
  "properties": {
    "name": {
      "type": "string",
      "required": true,
      "description": "律师姓名"
    },
    "licenseNumber": {
      "type": "string",
      "required": true,
      "description": "律师执业证号"
    },
    "firmName": {
      "type": "string",
      "description": "所属律师事务所"
    },
    "specialty": {
      "type": "string",
      "description": "专业领域"
    },
    "contact": {
      "type": "string",
      "description": "联系方式"
    }
  }
}
```

### 3.9 证据实体 (Evidence)

```json
{
  "name": "Evidence",
  "displayName": "证据",
  "description": "案件证据材料",
  "properties": {
    "evidenceNumber": {
      "type": "string",
      "required": true,
      "description": "证据编号"
    },
    "evidenceType": {
      "type": "string",
      "enum": ["书证", "物证", "视听资料", "电子数据", "证人证言", "当事人陈述", "鉴定意见", "勘验笔录"],
      "description": "证据类型"
    },
    "content": {
      "type": "text",
      "required": true,
      "description": "证据内容摘要"
    },
    "submittedBy": {
      "type": "string",
      "description": "提交方"
    },
    "submissionDate": {
      "type": "date",
      "description": "提交日期"
    },
    "purpose": {
      "type": "string",
      "description": "证明目的"
    }
  }
}
```

### 3.10 裁判文书实体 (JudgmentDocument)

```json
{
  "name": "JudgmentDocument",
  "displayName": "裁判文书",
  "description": "法院制作的裁判文书",
  "properties": {
    "documentNumber": {
      "type": "string",
      "required": true,
      "description": "文书编号"
    },
    "documentType": {
      "type": "string",
      "enum": ["判决书", "裁定书", "调解书", "决定书", "裁决书"],
      "description": "文书类型"
    },
    "issueDate": {
      "type": "date",
      "required": true,
      "description": "作出日期"
    },
    "mainContent": {
      "type": "text",
      "description": "主要内容摘要"
    },
    "judgmentResult": {
      "type": "string",
      "description": "判决结果"
    },
    "legalBasis": {
      "type": "text",
      "description": "法律依据"
    }
  }
}
```

### 3.11 法律组织实体 (LegalOrganization)

```json
{
  "name": "LegalOrganization",
  "displayName": "法律组织",
  "description": "调解组织、仲裁机构、公证机构等",
  "properties": {
    "name": {
      "type": "string",
      "required": true,
      "description": "组织名称"
    },
    "orgType": {
      "type": "string",
      "enum": ["商事调解组织", "人民调解组织", "仲裁机构", "公证机构", "法律援助中心"],
      "description": "组织类型"
    },
    "location": {
      "type": "string",
      "description": "所在地"
    },
    "licenseNumber": {
      "type": "string",
      "description": "执业证书编号"
    },
    "establishedDate": {
      "type": "date",
      "description": "设立日期"
    },
    "contact": {
      "type": "string",
      "description": "联系方式"
    }
  }
}
```

### 3.12 调解员实体 (Mediator)

```json
{
  "name": "Mediator",
  "displayName": "调解员",
  "description": "商事调解员",
  "properties": {
    "name": {
      "type": "string",
      "required": true,
      "description": "调解员姓名"
    },
    "qualification": {
      "type": "string",
      "enum": ["法律职业资格", "律师", "仲裁员", "公证员", "原法官/检察官", "专业职称"],
      "description": "资质类型"
    },
    "licenseNumber": {
      "type": "string",
      "description": "资质证书编号"
    },
    "organizationName": {
      "type": "string",
      "description": "所属组织"
    },
    "specialty": {
      "type": "string",
      "description": "专业领域"
    },
    "yearsExperience": {
      "type": "integer",
      "description": "从业年限"
    }
  }
}
```

### 3.13 调解协议实体 (MediationAgreement)

```json
{
  "name": "MediationAgreement",
  "displayName": "调解协议",
  "description": "商事调解达成的协议",
  "properties": {
    "agreementNumber": {
      "type": "string",
      "required": true,
      "description": "协议编号"
    },
    "mainFacts": {
      "type": "text",
      "description": "主要事实"
    },
    "disputeItems": {
      "type": "text",
      "description": "争议事项"
    },
    "agreementContent": {
      "type": "text",
      "required": true,
      "description": "协议主要内容"
    },
    "performanceMethod": {
      "type": "string",
      "description": "履行方式"
    },
    "performanceDeadline": {
      "type": "date",
      "description": "履行期限"
    },
    "signDate": {
      "type": "date",
      "description": "签订日期"
    },
    "judiciallyConfirmed": {
      "type": "boolean",
      "description": "是否经司法确认"
    }
  }
}
```

## 4. 关系类型定义 (Edge Types)

### 4.1 案件-当事人关系

```json
{
  "name": "CASE_PARTY",
  "displayName": "案件-当事人关系",
  "sourceType": "Case",
  "targetType": "Party",
  "description": "案件与当事人之间的参与关系",
  "properties": {
    "role": {
      "type": "string",
      "required": true,
      "enum": ["原告", "被告", "第三人", "上诉人", "被上诉人", "申请人", "被申请人"],
      "description": "当事人在案件中的角色"
    },
    "representationType": {
      "type": "string",
      "enum": ["本人", "委托代理", "法定代理"],
      "description": "代理类型"
    }
  }
}
```

### 4.2 案件-法官关系

```json
{
  "name": "CASE_JUDGE",
  "displayName": "案件-法官关系",
  "sourceType": "Case",
  "targetType": "Judge",
  "description": "案件审判人员关系",
  "properties": {
    "role": {
      "type": "string",
      "required": true,
      "enum": ["审判长", "审判员", "人民陪审员", "书记员"],
      "description": "法官在案件中的角色"
    }
  }
}
```

### 4.3 案件-法院关系

```json
{
  "name": "CASE_COURT",
  "displayName": "案件-法院关系",
  "sourceType": "Case",
  "targetType": "Court",
  "description": "案件与法院的管辖关系",
  "properties": {
    "courtRole": {
      "type": "string",
      "required": true,
      "enum": ["立案法院", "一审法院", "二审法院", "再审法院", "执行法院"],
      "description": "法院在案件中的角色"
    }
  }
}
```

### 4.4 案件-法律条文关系

```json
{
  "name": "CASE_LEGAL_PROVISION",
  "displayName": "案件-法条关系",
  "sourceType": "Case",
  "targetType": "LegalProvision",
  "description": "案件适用的法律条文",
  "properties": {
    "usageType": {
      "type": "string",
      "required": true,
      "enum": ["适用", "参照", "援引", "参考", "分析"],
      "description": "法条使用方式"
    },
    "articleText": {
      "type": "text",
      "description": "引用条文的具体文字"
    },
    "reasoning": {
      "type": "text",
      "description": "适用理由"
    }
  }
}
```

### 4.5 案件-证据关系

```json
{
  "name": "CASE_EVIDENCE",
  "displayName": "案件-证据关系",
  "sourceType": "Case",
  "targetType": "Evidence",
  "description": "案件与证据的关联",
  "properties": {
    "evidenceRole": {
      "type": "string",
      "enum": ["原告证据", "被告证据", "法院调取", "鉴定意见"],
      "description": "证据角色"
    },
    "admissibility": {
      "type": "string",
      "enum": ["采纳", "不予采纳", "部分采纳"],
      "description": "采信情况"
    }
  }
}
```

### 4.6 案件-裁判文书关系

```json
{
  "name": "CASE_JUDGMENT",
  "displayName": "案件-裁判文书关系",
  "sourceType": "Case",
  "targetType": "JudgmentDocument",
  "description": "案件与其裁判文书的关联",
  "properties": {
    "documentRole": {
      "type": "string",
      "enum": ["一审判决", "二审判决", "再审判决", "裁定", "调解书"],
      "description": "文书角色"
    }
  }
}
```

### 4.7 当事人-律师关系

```json
{
  "name": "PARTY_LAWYER",
  "displayName": "当事人-律师关系",
  "sourceType": "Party",
  "targetType": "Lawyer",
  "description": "当事人与代理律师的关系",
  "properties": {
    "representationType": {
      "type": "string",
      "enum": ["一般代理", "特别授权", "法律援助"],
      "description": "代理类型"
    },
    "startDate": {
      "type": "date",
      "description": "委托起始日期"
    },
    "endDate": {
      "type": "date",
      "description": "委托终止日期"
    }
  }
}
```

### 4.8 法律条文-法律条文关系

```json
{
  "name": "LEGAL_PROVISION_RELATED",
  "displayName": "法条-法条关系",
  "sourceType": "LegalProvision",
  "targetType": "LegalProvision",
  "description": "法律条文之间的关联关系",
  "properties": {
    "relationType": {
      "type": "string",
      "required": true,
      "enum": ["引用", "修订", "替代", "废止", "配套", "补充", "参照"],
      "description": "关系类型"
    },
    "effectiveDate": {
      "type": "date",
      "description": "关系生效日期"
    }
  }
}
```

### 4.9 案件-案件关系（关联案件）

```json
{
  "name": "CASE_RELATED",
  "displayName": "关联案件关系",
  "sourceType": "Case",
  "targetType": "Case",
  "description": "案件之间的关联关系",
  "properties": {
    "relationType": {
      "type": "string",
      "required": true,
      "enum": ["同一事实", "共同被告", "第三人参加", "先决关系", "上诉关系", "执行关联"],
      "description": "关联类型"
    }
  }
}
```

### 4.10 法律组织-调解员关系

```json
{
  "name": "ORG_MEDIATOR",
  "displayName": "组织-调解员关系",
  "sourceType": "LegalOrganization",
  "targetType": "Mediator",
  "description": "法律组织与调解员的聘用关系",
  "properties": {
    "employmentType": {
      "type": "string",
      "enum": ["专职", "兼职", "特聘"],
      "description": "聘用类型"
    },
    "hireDate": {
      "type": "date",
      "description": "聘用日期"
    }
  }
}
```

### 4.11 案件-调解组织关系

```json
{
  "name": "CASE_MEDIATION_ORG",
  "displayName": "案件-调解组织关系",
  "sourceType": "Case",
  "targetType": "LegalOrganization",
  "description": "案件与调解组织的参与关系",
  "properties": {
    "mediationStage": {
      "type": "string",
      "enum": ["诉前调解", "诉中调解", "执行调解"],
      "description": "调解阶段"
    },
    "mediationResult": {
      "type": "string",
      "enum": ["调解成功", "调解终止", "转入诉讼"],
      "description": "调解结果"
    }
  }
}
```

### 4.12 案件-调解协议关系

```json
{
  "name": "CASE_MEDIATION_AGREEMENT",
  "displayName": "案件-调解协议关系",
  "sourceType": "Case",
  "targetType": "MediationAgreement",
  "description": "案件与调解协议的关联",
  "properties": {
    "agreementRole": {
      "type": "string",
      "enum": ["调解达成", "司法确认"],
      "description": "协议角色"
    }
  }
}
```

## 5. 继承关系图

```
Entity (框架基类)
├── Case (案件)
│   ├── CivilCase (民事案件)
│   │   └── CommercialCase (商事案件)
│   ├── CriminalCase (刑事案件)
│   └── AdministrativeCase (行政案件)
├── LegalProvision (法律条文)
│   ├── Law (法律)
│   ├── Regulation (行政法规)
│   └── JudicialInterpretation (司法解释)
├── Party (当事人)
├── Judge (法官)
├── Court (法院)
├── Lawyer (律师)
├── Evidence (证据)
├── JudgmentDocument (裁判文书)
├── LegalOrganization (法律组织)
├── Mediator (调解员)
└── MediationAgreement (调解协议)
```

## 6. 约束条件

### 6.1 唯一性约束

| 实体 | 唯一键 |
|------|--------|
| Case | caseNumber |
| Court | name |
| Judge | name + courtName |
| Lawyer | licenseNumber |
| LegalProvision | lawName + articleNumber |

### 6.2 必填字段

| 实体 | 必填字段 |
|------|---------|
| Case | caseNumber, caseName, caseType |
| Party | name, role |
| Judge | name |
| Court | name |
| LegalProvision | provisionId, articleNumber, content, lawName |
| Lawyer | name, licenseNumber |

### 6.3 关系方向性

所有案件相关关系均为 **有向关系**，方向从案件指向关联实体。

### 6.4 多重性

| 关系 | 源→目标 | 目标→源 |
|------|--------|--------|
| CASE_PARTY | 1:N | N:1 |
| CASE_JUDGE | 1:N | N:1 |
| CASE_COURT | 1:N | N:1 |
| CASE_LEGAL_PROVISION | 1:N | N:1 |
| CASE_EVIDENCE | 1:N | N:1 |
| CASE_JUDGMENT | 1:N | N:1 |
| LEGAL_PROVISION_RELATED | N:N | N:N |

## 7. Neo4j 索引设计

```cypher
-- 案件编号索引
CREATE INDEX case_number_index IF NOT EXISTS
FOR (n:Case) ON (n.caseNumber);

-- 案件类型索引
CREATE INDEX case_type_index IF NOT EXISTS
FOR (n:Case) ON (n.caseType);

-- 当事人名称索引
CREATE INDEX party_name_index IF NOT EXISTS
FOR (n:Party) ON (n.name);

-- 法院名称索引
CREATE INDEX court_name_index IF NOT EXISTS
FOR (n:Court) ON (n.name);

-- 法律名称+条款复合索引
CREATE INDEX provision_law_article_index IF NOT EXISTS
FOR (n:LegalProvision) ON (n.lawName, n.articleNumber);

-- 向量索引（用于语义检索）
CREATE VECTOR INDEX case_embedding_index IF NOT EXISTS
FOR (n:Case) ON (n.embedding)
OPTIONS {indexConfig: {
  `vector.dimensions`: 1536,
  `vector.similarity_function`: 'cosine'
}};
```
