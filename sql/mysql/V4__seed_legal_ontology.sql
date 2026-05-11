-- ============================================================
-- 法律知识图谱本体定义数据
-- MySQL 8.0+
-- ============================================================

USE graphiti;

-- ----------------------------------------------------------
-- 插入法律知识图谱本体定义
-- ----------------------------------------------------------

-- 图谱元数据
INSERT INTO `graphiti_graph_metadata` (`graph_id`, `name`, `description`, `node_count`, `edge_count`)
VALUES ('legal-knowledge-graph', '法律知识图谱', '基于典型案例、商事调解条例的法律领域知识图谱，包含案件、当事人、法官、法院、法律条文、证据、裁判文书等实体类型', 0, 0)
ON DUPLICATE KEY UPDATE name = VALUES(name), description = VALUES(description);

-- 完整本体定义（entities + edges JSON）
INSERT INTO `graphiti_ontology` (`graph_id`, `entities`, `edges`, `is_default`, `create_time`, `update_time`, `deleted`)
VALUES (
  'legal-knowledge-graph',

  -- ============================================================
  -- 实体类型定义 (entities)
  -- ============================================================
  '[
    {
      "name": "Case",
      "displayName": "案件",
      "description": "案件基类，包含所有案件的公共属性",
      "extends": "Entity",
      "properties": {
        "caseNumber": {"type": "string", "required": true, "description": "案件编号", "example": "(2024)沪01民初1234号"},
        "caseName": {"type": "string", "required": true, "description": "案件名称"},
        "caseType": {"type": "string", "required": true, "description": "案件类型", "enum": ["民事", "刑事", "行政", "商事", "执行", "赔偿"]},
        "caseStatus": {"type": "string", "description": "案件状态", "enum": ["立案", "审理中", "调解中", "判决", "上诉中", "结案", "撤销"]},
        "filingDate": {"type": "date", "description": "立案日期"},
        "closedDate": {"type": "date", "description": "结案日期"},
        "amountInDispute": {"type": "decimal", "description": "争议金额(元)"},
        "summary": {"type": "text", "description": "案件摘要"}
      }
    },
    {
      "name": "CommercialCase",
      "displayName": "商事案件",
      "description": "商事纠纷案件",
      "extends": "Case",
      "properties": {
        "disputeType": {"type": "string", "description": "纠纷类型", "enum": ["合同纠纷", "股权纠纷", "知识产权纠纷", "金融纠纷", "投资纠纷"]},
        "mediationAttempted": {"type": "boolean", "description": "是否经过调解"}
      }
    },
    {
      "name": "CivilCase",
      "displayName": "民事案件",
      "description": "民事纠纷案件",
      "extends": "Case",
      "properties": {
        "subjectMatter": {"type": "string", "description": "诉讼标的"}
      }
    },
    {
      "name": "Party",
      "displayName": "当事人",
      "description": "案件中的当事人（原告、被告、第三人等）",
      "properties": {
        "name": {"type": "string", "required": true, "description": "姓名或名称"},
        "partyType": {"type": "string", "required": true, "description": "当事人类型", "enum": ["自然人", "法人", "非法人组织"]},
        "idNumber": {"type": "string", "description": "身份证号/统一社会信用代码"},
        "role": {"type": "string", "required": true, "description": "诉讼角色", "enum": ["原告", "被告", "第三人", "上诉人", "被上诉人", "申请人", "被申请人", "公诉人", "辩护人"]},
        "address": {"type": "string", "description": "住所地"},
        "contact": {"type": "string", "description": "联系方式"},
        "isEnterprise": {"type": "boolean", "description": "是否企业"}
      }
    },
    {
      "name": "Court",
      "displayName": "法院",
      "description": "审判机关",
      "properties": {
        "name": {"type": "string", "required": true, "description": "法院名称"},
        "level": {"type": "string", "description": "法院级别", "enum": ["最高人民法院", "高级人民法院", "中级人民法院", "基层人民法院", "专门法院"]},
        "location": {"type": "string", "description": "所在地"},
        "jurisdiction": {"type": "string", "description": "管辖范围"},
        "parentCourt": {"type": "string", "description": "上级法院名称"}
      }
    },
    {
      "name": "Judge",
      "displayName": "法官",
      "description": "案件审判人员",
      "properties": {
        "name": {"type": "string", "required": true, "description": "法官姓名"},
        "title": {"type": "string", "description": "职务", "enum": ["审判长", "审判员", "人民陪审员", "书记员", "副院长", "院长"]},
        "courtName": {"type": "string", "description": "所属法院"},
        "specialty": {"type": "string", "description": "专业领域"}
      }
    },
    {
      "name": "LegalProvision",
      "displayName": "法律条文",
      "description": "法律、行政法规、司法解释等条文",
      "extends": "Entity",
      "properties": {
        "provisionId": {"type": "string", "required": true, "description": "条文编号"},
        "articleNumber": {"type": "string", "required": true, "description": "条款序号"},
        "content": {"type": "text", "required": true, "description": "条文内容"},
        "lawName": {"type": "string", "required": true, "description": "所属法律名称"},
        "lawType": {"type": "string", "description": "法律类型", "enum": ["法律", "行政法规", "司法解释", "部门规章", "地方性法规"]},
        "chapter": {"type": "string", "description": "所属章节"},
        "effectiveDate": {"type": "date", "description": "生效日期"},
        "abolishedDate": {"type": "date", "description": "废止日期"},
        "keywords": {"type": "string", "description": "关键词标签（逗号分隔）"}
      }
    },
    {
      "name": "Lawyer",
      "displayName": "律师",
      "description": "执业律师",
      "properties": {
        "name": {"type": "string", "required": true, "description": "律师姓名"},
        "licenseNumber": {"type": "string", "required": true, "description": "律师执业证号"},
        "firmName": {"type": "string", "description": "所属律师事务所"},
        "specialty": {"type": "string", "description": "专业领域"},
        "contact": {"type": "string", "description": "联系方式"}
      }
    },
    {
      "name": "Evidence",
      "displayName": "证据",
      "description": "案件证据材料",
      "properties": {
        "evidenceNumber": {"type": "string", "required": true, "description": "证据编号"},
        "evidenceType": {"type": "string", "description": "证据类型", "enum": ["书证", "物证", "视听资料", "电子数据", "证人证言", "当事人陈述", "鉴定意见", "勘验笔录"]},
        "content": {"type": "text", "required": true, "description": "证据内容摘要"},
        "submittedBy": {"type": "string", "description": "提交方"},
        "submissionDate": {"type": "date", "description": "提交日期"},
        "purpose": {"type": "string", "description": "证明目的"}
      }
    },
    {
      "name": "JudgmentDocument",
      "displayName": "裁判文书",
      "description": "法院制作的裁判文书",
      "properties": {
        "documentNumber": {"type": "string", "required": true, "description": "文书编号"},
        "documentType": {"type": "string", "description": "文书类型", "enum": ["判决书", "裁定书", "调解书", "决定书", "裁决书"]},
        "issueDate": {"type": "date", "required": true, "description": "作出日期"},
        "mainContent": {"type": "text", "description": "主要内容摘要"},
        "judgmentResult": {"type": "string", "description": "判决结果"},
        "legalBasis": {"type": "text", "description": "法律依据"}
      }
    },
    {
      "name": "LegalOrganization",
      "displayName": "法律组织",
      "description": "调解组织、仲裁机构、公证机构等",
      "properties": {
        "name": {"type": "string", "required": true, "description": "组织名称"},
        "orgType": {"type": "string", "description": "组织类型", "enum": ["商事调解组织", "人民调解组织", "仲裁机构", "公证机构", "法律援助中心"]},
        "location": {"type": "string", "description": "所在地"},
        "licenseNumber": {"type": "string", "description": "执业证书编号"},
        "establishedDate": {"type": "date", "description": "设立日期"},
        "contact": {"type": "string", "description": "联系方式"}
      }
    },
    {
      "name": "Mediator",
      "displayName": "调解员",
      "description": "商事调解员",
      "properties": {
        "name": {"type": "string", "required": true, "description": "调解员姓名"},
        "qualification": {"type": "string", "description": "资质类型", "enum": ["法律职业资格", "律师", "仲裁员", "公证员", "原法官/检察官", "专业职称"]},
        "licenseNumber": {"type": "string", "description": "资质证书编号"},
        "organizationName": {"type": "string", "description": "所属组织"},
        "specialty": {"type": "string", "description": "专业领域"},
        "yearsExperience": {"type": "integer", "description": "从业年限"}
      }
    },
    {
      "name": "MediationAgreement",
      "displayName": "调解协议",
      "description": "商事调解达成的协议",
      "properties": {
        "agreementNumber": {"type": "string", "required": true, "description": "协议编号"},
        "mainFacts": {"type": "text", "description": "主要事实"},
        "disputeItems": {"type": "text", "description": "争议事项"},
        "agreementContent": {"type": "text", "required": true, "description": "协议主要内容"},
        "performanceMethod": {"type": "string", "description": "履行方式"},
        "performanceDeadline": {"type": "date", "description": "履行期限"},
        "signDate": {"type": "date", "description": "签订日期"},
        "judiciallyConfirmed": {"type": "boolean", "description": "是否经司法确认"}
      }
    }
  ]',

  -- ============================================================
  -- 关系类型定义 (edges)
  -- ============================================================
  '[
    {
      "name": "CASE_PARTY",
      "displayName": "案件-当事人关系",
      "sourceType": "Case",
      "targetType": "Party",
      "description": "案件与当事人之间的参与关系",
      "properties": {
        "role": {"type": "string", "required": true, "description": "当事人在案件中的角色", "enum": ["原告", "被告", "第三人", "上诉人", "被上诉人", "申请人", "被申请人"]},
        "representationType": {"type": "string", "description": "代理类型", "enum": ["本人", "委托代理", "法定代理"]}
      }
    },
    {
      "name": "CASE_JUDGE",
      "displayName": "案件-法官关系",
      "sourceType": "Case",
      "targetType": "Judge",
      "description": "案件审判人员关系",
      "properties": {
        "role": {"type": "string", "required": true, "description": "法官在案件中的角色", "enum": ["审判长", "审判员", "人民陪审员", "书记员"]}
      }
    },
    {
      "name": "CASE_COURT",
      "displayName": "案件-法院关系",
      "sourceType": "Case",
      "targetType": "Court",
      "description": "案件与法院的管辖关系",
      "properties": {
        "courtRole": {"type": "string", "required": true, "description": "法院在案件中的角色", "enum": ["立案法院", "一审法院", "二审法院", "再审法院", "执行法院"]}
      }
    },
    {
      "name": "CASE_LEGAL_PROVISION",
      "displayName": "案件-法条关系",
      "sourceType": "Case",
      "targetType": "LegalProvision",
      "description": "案件适用的法律条文",
      "properties": {
        "usageType": {"type": "string", "required": true, "description": "法条使用方式", "enum": ["适用", "参照", "援引", "参考", "分析"]},
        "articleText": {"type": "text", "description": "引用条文的具体文字"},
        "reasoning": {"type": "text", "description": "适用理由"}
      }
    },
    {
      "name": "CASE_EVIDENCE",
      "displayName": "案件-证据关系",
      "sourceType": "Case",
      "targetType": "Evidence",
      "description": "案件与证据的关联",
      "properties": {
        "evidenceRole": {"type": "string", "description": "证据角色", "enum": ["原告证据", "被告证据", "法院调取", "鉴定意见"]},
        "admissibility": {"type": "string", "description": "采信情况", "enum": ["采纳", "不予采纳", "部分采纳"]}
      }
    },
    {
      "name": "CASE_JUDGMENT",
      "displayName": "案件-裁判文书关系",
      "sourceType": "Case",
      "targetType": "JudgmentDocument",
      "description": "案件与其裁判文书的关联",
      "properties": {
        "documentRole": {"type": "string", "description": "文书角色", "enum": ["一审判决", "二审判决", "再审判决", "裁定", "调解书"]}
      }
    },
    {
      "name": "PARTY_LAWYER",
      "displayName": "当事人-律师关系",
      "sourceType": "Party",
      "targetType": "Lawyer",
      "description": "当事人与代理律师的关系",
      "properties": {
        "representationType": {"type": "string", "description": "代理类型", "enum": ["一般代理", "特别授权", "法律援助"]},
        "startDate": {"type": "date", "description": "委托起始日期"},
        "endDate": {"type": "date", "description": "委托终止日期"}
      }
    },
    {
      "name": "LEGAL_PROVISION_RELATED",
      "displayName": "法条-法条关系",
      "sourceType": "LegalProvision",
      "targetType": "LegalProvision",
      "description": "法律条文之间的关联关系",
      "properties": {
        "relationType": {"type": "string", "required": true, "description": "关系类型", "enum": ["引用", "修订", "替代", "废止", "配套", "补充", "参照"]},
        "effectiveDate": {"type": "date", "description": "关系生效日期"}
      }
    },
    {
      "name": "CASE_RELATED",
      "displayName": "关联案件关系",
      "sourceType": "Case",
      "targetType": "Case",
      "description": "案件之间的关联关系",
      "properties": {
        "relationType": {"type": "string", "required": true, "description": "关联类型", "enum": ["同一事实", "共同被告", "第三人参加", "先决关系", "上诉关系", "执行关联"]}
      }
    },
    {
      "name": "ORG_MEDIATOR",
      "displayName": "组织-调解员关系",
      "sourceType": "LegalOrganization",
      "targetType": "Mediator",
      "description": "法律组织与调解员的聘用关系",
      "properties": {
        "employmentType": {"type": "string", "description": "聘用类型", "enum": ["专职", "兼职", "特聘"]},
        "hireDate": {"type": "date", "description": "聘用日期"}
      }
    },
    {
      "name": "CASE_MEDIATION_ORG",
      "displayName": "案件-调解组织关系",
      "sourceType": "Case",
      "targetType": "LegalOrganization",
      "description": "案件与调解组织的参与关系",
      "properties": {
        "mediationStage": {"type": "string", "description": "调解阶段", "enum": ["诉前调解", "诉中调解", "执行调解"]},
        "mediationResult": {"type": "string", "description": "调解结果", "enum": ["调解成功", "调解终止", "转入诉讼"]}
      }
    },
    {
      "name": "CASE_MEDIATION_AGREEMENT",
      "displayName": "案件-调解协议关系",
      "sourceType": "Case",
      "targetType": "MediationAgreement",
      "description": "案件与调解协议的关联",
      "properties": {
        "agreementRole": {"type": "string", "description": "协议角色", "enum": ["调解达成", "司法确认"]}
      }
    }
  ]',

  true,
  NOW(),
  NOW(),
  false
)
ON DUPLICATE KEY UPDATE
  entities = VALUES(entities),
  edges = VALUES(edges),
  is_default = VALUES(is_default),
  update_time = NOW();

-- ----------------------------------------------------------
-- 完成
-- ----------------------------------------------------------
