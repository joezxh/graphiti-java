// ============================================================
// Graphiti Neo4j 初始化脚本
// 版本: 2026-05-20
// 说明: 法律知识图谱完整节点和关系示例数据
//       包含: Entity(法律实体) + Community(法律领域聚类) + Episode(法律事件/剧集)
//       建模方案: 统一使用 :Entity Label + type 属性区分节点类型
//                 Community 使用独立 :Community Label
//                 Episode 使用独立 :Episode Label
//                 与 GraphVisualizationService / SchemaManagementService / CommunityServiceImpl 对齐
// 要求: Neo4j 5.x (APOC plugin recommended)
// ============================================================

// ============================================================
// 图谱ID参数配置
// ============================================================
// 所有节点和关系均携带 graph_id 属性，用于多图谱隔离
// 可通过参数注入: :param graphId = 'legal-knowledge-graph'

// ============================================================
// 第一部分: 索引（社区版兼容）
// 注意: Neo4j 社区版不支持约束（Constraints），仅支持索引
// 索引不强制唯一性或非空，但可以提升查询性能
// 如需约束，请使用 Neo4j Enterprise Edition
// ============================================================

// 基础索引
CREATE INDEX entity_type_v3 IF NOT EXISTS FOR (n:Entity) ON (n.type);
CREATE INDEX entity_graph_id_v3 IF NOT EXISTS FOR (n:Entity) ON (n.graph_id);
CREATE INDEX entity_type_graph_id_v3 IF NOT EXISTS FOR (n:Entity) ON (n.type, n.graph_id);

// Community 索引
CREATE INDEX community_uuid_v3 IF NOT EXISTS FOR (n:Community) ON (n.uuid);
CREATE INDEX community_graph_id_v3 IF NOT EXISTS FOR (n:Community) ON (n.graph_id);
CREATE INDEX community_parent_uuid_v3 IF NOT EXISTS FOR (n:Community) ON (n.parent_community_uuid);
CREATE INDEX community_type_v3 IF NOT EXISTS FOR (n:Community) ON (n.community_type);
CREATE INDEX community_legal_domain_v3 IF NOT EXISTS FOR (n:Community) ON (n.legal_domain);
CREATE INDEX community_jurisdiction_v3 IF NOT EXISTS FOR (n:Community) ON (n.jurisdiction);
CREATE INDEX court_name_v3 IF NOT EXISTS FOR (n:Entity) ON (n.courtName);
CREATE INDEX court_courtLevel_v3 IF NOT EXISTS FOR (n:Entity) ON (n.courtLevel);
CREATE INDEX court_location_v3 IF NOT EXISTS FOR (n:Entity) ON (n.location);

// Party 索引
CREATE INDEX party_name_v3 IF NOT EXISTS FOR (n:Entity) ON (n.partyName);
CREATE INDEX party_partyRole_v3 IF NOT EXISTS FOR (n:Entity) ON (n.partyRole);
CREATE INDEX party_isEnterprise_v3 IF NOT EXISTS FOR (n:Entity) ON (n.isEnterprise);

// LegalProvision 索引
CREATE INDEX provision_provisionId_v3 IF NOT EXISTS FOR (n:Entity) ON (n.provisionId);
CREATE INDEX provision_lawName_v3 IF NOT EXISTS FOR (n:Entity) ON (n.lawName);
CREATE INDEX provision_lawType_v3 IF NOT EXISTS FOR (n:Entity) ON (n.lawType);

// Judge 索引
CREATE INDEX judge_name_v3 IF NOT EXISTS FOR (n:Entity) ON (n.judgeName);

// JudgmentDocument 索引
CREATE INDEX judgment_documentNumber_v3 IF NOT EXISTS FOR (n:Entity) ON (n.documentNumber);
CREATE INDEX judgment_documentType_v3 IF NOT EXISTS FOR (n:Entity) ON (n.documentType);

// MediationAgreement 索引
CREATE INDEX agreement_agreementNumber_v3 IF NOT EXISTS FOR (n:Entity) ON (n.agreementNumber);

// CommercialMediationOrganization 索引
CREATE INDEX org_name_v3 IF NOT EXISTS FOR (n:Entity) ON (n.name);
CREATE INDEX org_orgType_v3 IF NOT EXISTS FOR (n:Entity) ON (n.orgType);

// Mediator 索引
CREATE INDEX mediator_name_v3 IF NOT EXISTS FOR (n:Entity) ON (n.name);

// Evidence 索引
CREATE INDEX evidence_evidenceNumber_v3 IF NOT EXISTS FOR (n:Entity) ON (n.evidenceNumber);
CREATE INDEX evidence_evidenceType_v3 IF NOT EXISTS FOR (n:Entity) ON (n.evidenceType);

// CaseReasoning 索引
CREATE INDEX reason_reasoning_v3 IF NOT EXISTS FOR (n:Entity) ON (n.reasoning);

// CaseFact 索引
CREATE INDEX fact_description_v3 IF NOT EXISTS FOR (n:Entity) ON (n.factDescription);
CREATE INDEX fact_category_v3 IF NOT EXISTS FOR (n:Entity) ON (n.factCategory);
CREATE INDEX fact_importance_v3 IF NOT EXISTS FOR (n:Entity) ON (n.factImportance);

// Case 索引
CREATE INDEX case_caseNumber_v3 IF NOT EXISTS FOR (n:Entity) ON (n.caseNumber);
CREATE INDEX case_caseType_v3 IF NOT EXISTS FOR (n:Entity) ON (n.caseType);
CREATE INDEX case_caseStatus_v3 IF NOT EXISTS FOR (n:Entity) ON (n.caseStatus);
CREATE INDEX case_courtLevel_v3 IF NOT EXISTS FOR (n:Entity) ON (n.courtLevel);
CREATE INDEX case_disputeType_v3 IF NOT EXISTS FOR (n:Entity) ON (n.disputeType);

// 文本索引 (中文分词) - 需要 APOC 插件支持
// 如果社区版没有 APOC，请注释掉以下文本索引
// CREATE TEXT INDEX case_name_text_v3 IF NOT EXISTS
// FOR (n:Entity) ON (n.caseName)
// WHERE n.type = 'Case';
//
// CREATE TEXT INDEX provision_content_text_v3 IF NOT EXISTS
// FOR (n:Entity) ON (n.provisionContent)
// WHERE n.type = 'LegalProvision';
//
// CREATE TEXT INDEX party_name_text_v3 IF NOT EXISTS
// FOR (n:Entity) ON (n.partyName)
// WHERE n.type = 'Party';
//
// CREATE TEXT INDEX court_name_text_v3 IF NOT EXISTS
// FOR (n:Entity) ON (n.courtName)
// WHERE n.type = 'Court';


// Community 索引
CREATE INDEX community_graph_id_v3 IF NOT EXISTS FOR (n:Community) ON (n.graph_id);
CREATE INDEX community_uuid_v3 IF NOT EXISTS FOR (n:Community) ON (n.uuid);

// Episode 索引
CREATE INDEX episode_graph_id_v3 IF NOT EXISTS FOR (n:Episode) ON (n.graph_id);
CREATE INDEX episode_uuid_v3 IF NOT EXISTS FOR (n:Episode) ON (n.uuid);
CREATE INDEX episode_valid_at_v3 IF NOT EXISTS FOR (n:Episode) ON (n.valid_at);

// ============================================================
// 第二部分: 社区节点 (Community) — :Community
// 法律领域聚类: 按主题、案由、司法辖区等维度组织法律知识
// 关系: HAS_COMMUNITY (Entity -> Community)
//       由 CommunityServiceImpl 自动创建
// ============================================================

// 社区1: 商事调解 — 商事调解条例体系下的调解活动
MERGE (comm1:Community {
  graph_id: 'legal-knowledge-graph',
  uuid: 'community-commercial-mediation-001',
  name: '商事调解纠纷处理',
  summary: '本社区涵盖商事调解组织设立、调解员资质、调解程序及调解协议效力等全链条法律问题。核心法条为《商事调解条例》第1-30条，涉及上海国际商事调解中心的设立及运营规范。成员包括商事调解组织、商事调解员、调解协议、司法确认裁定等实体，反映了多元化纠纷解决机制（ADR）在商事领域的具体应用。',
  member_count: 0,
  community_type: 'dispute_resolution',
  legal_domain: 'DOMAIN_MEDIATION',
  jurisdiction: 'JURISDICTION_CN',
  practice_type: 'PRACTICE_MEDIATION',
  key_provisions: ['SMTL-001', 'SMTL-002', 'SMTL-008', 'SMTL-012', 'SMTL-014', 'SMTL-022'],
  parent_community_uuid: 'community-legal-ai-001',
  created_at: datetime()
});

// 社区2: 公司解散纠纷 — 公司僵局与股东权益保护
MERGE (comm2:Community {
  graph_id: 'legal-knowledge-graph',
  uuid: 'community-company-dissolution-001',
  name: '公司解散与股东权益保护',
  summary: '本社区围绕公司解散诉讼展开，包含公司僵局认定标准（持续两年无法召开股东会、无法形成有效决议）、股东利益受损判断、司法救济的审慎适用等核心法律问题。涉及《民法典》第69条（法人解散一般规则）及《公司法解释（二）》第1条（公司解散诉讼具体情形）。关联案例（2023）沪01民终11293号展示了公司运营良好时驳回解散诉请的典型裁判思路。',
  member_count: 0,
  community_type: 'corporate_dispute',
  legal_domain: 'DOMAIN_CIVIL',
  jurisdiction: 'JURISDICTION_CN',
  practice_type: 'PRACTICE_JUDICIAL',
  key_provisions: ['MFD-069', 'GSF2-001'],
  parent_community_uuid: 'community-legal-ai-001',
  created_at: datetime()
});

// 社区3: 民事诉讼管辖权 — 债权人撤销权诉讼的管辖确定
MERGE (comm3:Community {
  graph_id: 'legal-knowledge-graph',
  uuid: 'community-jurisdiction-dispute-001',
  name: '债权人撤销权诉讼管辖权争议',
  summary: '本社区聚焦于特殊被告情形下的管辖权确定问题。当共同被告中仅有部分被告被监禁时，是否适用《民事诉讼法》第23条关于原告住所地管辖的特别规定。核心案例（2020）渝民辖188号明确：仅部分被告被监禁不构成第23条的适用条件，应按一般管辖规定（被告住所地）确定管辖法院。涉及民事诉讼法第22、23、24条的体系解释与适用。',
  member_count: 0,
  community_type: 'procedural_law',
  legal_domain: 'DOMAIN_CIVIL',
  jurisdiction: 'JURISDICTION_CN',
  practice_type: 'PRACTICE_JUDICIAL',
  key_provisions: ['MSSSF-022', 'MSSSF-023', 'MSSSF-024'],
  parent_community_uuid: 'community-legal-ai-001',
  created_at: datetime()
});

// 社区4: 知识产权保护 — 专利侵权认定与赔偿
MERGE (comm4:Community {
  graph_id: 'legal-knowledge-graph',
  uuid: 'community-ip-patent-001',
  name: '专利侵权认定与损害赔偿',
  summary: '本社区涵盖专利权保护领域的核心法律问题，包括专利权属确认、侵权行为认定、法定免责事由（现有技术抗辩、授权许可等）及损害赔偿计算。涉及专利法及相关司法解释，反映了技术类知识产权案件的裁判规律，对于理解知识产权民事纠纷的审理思路具有参考价值。',
  member_count: 0,
  community_type: 'intellectual_property',
  legal_domain: 'DOMAIN_IP',
  jurisdiction: 'JURISDICTION_CN',
  practice_type: 'PRACTICE_JUDICIAL',
  key_provisions: [],
  parent_community_uuid: 'community-legal-ai-001',
  created_at: datetime()
});

// 社区5: 劳动争议 — 劳动合同解除的合法性审查
MERGE (comm5:Community {
  graph_id: 'legal-knowledge-graph',
  uuid: 'community-labor-dispute-001',
  name: '劳动合同解除合法性审查',
  summary: '本社区围绕用人单位单方解除劳动合同的合法性判断展开，核心问题包括：客观情况发生重大变化的认定标准、与劳动者协商变更的前置程序、未满足条件时解除行为的违法性评价。案例展示了"组织架构调整"不能直接作为解除劳动合同依据的裁判观点，对于理解劳动争议中用人单位的举证责任具有重要意义。',
  member_count: 0,
  community_type: 'labor_dispute',
  legal_domain: 'DOMAIN_LABOR',
  jurisdiction: 'JURISDICTION_CN',
  practice_type: 'PRACTICE_JUDICIAL',
  key_provisions: [],
  parent_community_uuid: 'community-legal-ai-001',
  created_at: datetime()
});

// 社区6: 民商事法律基础 — 法人制度与法律适用
MERGE (comm6:Community {
  graph_id: 'legal-knowledge-graph',
  uuid: 'community-civil-law-foundation-001',
  name: '民商事法律基础与法人制度',
  summary: '本社区涵盖民商事法律体系的基础性规范，包括法人的成立、解散与清算，民事法律行为的效力要件，合同的成立与履行等核心民法问题。作为各类民商事纠纷的底层法律支撑，本社区为其他专题社区（如公司解散、劳动争议）提供基础法律概念框架，体现民法典作为民事基本法的统摄地位。',
  member_count: 0,
  community_type: 'foundational_civil_law',
  legal_domain: 'DOMAIN_CIVIL',
  jurisdiction: 'JURISDICTION_CN',
  practice_type: 'PRACTICE_JUDICIAL',
  key_provisions: ['MFD-069', 'MFD-070'],
  parent_community_uuid: 'community-legal-ai-001',
  created_at: datetime()
});

// ============================================================
// 顶级社区: 法律AI — 所有法律专题社区的父社区
// 方便后续扩展金融AI、医疗AI等其他行业社区
// ============================================================
MERGE (comm0:Community {
  graph_id: 'legal-knowledge-graph',
  uuid: 'community-legal-ai-001',
  name: '法律AI',
  summary: '法律AI顶级社区，汇聚各类法律专题子社区，包括民商事纠纷、劳动争议、知识产权、公司法、刑事法律、行政法律等领域。本社区为法律知识图谱的核心入口，支持按法律领域检索和扩展，方便未来与金融AI、医疗AI等行业知识图谱互联互通，构建跨行业智能法律服务生态。',
  member_count: 0,
  community_type: 'top_level',
  legal_domain: 'DOMAIN_ROOT',
  jurisdiction: 'JURISDICTION_CN',
  practice_type: 'PRACTICE_JUDICIAL',
  key_provisions: [],
  created_at: datetime()
});

// ============================================================
// 顶级社区成员关联 (PARENT_OF)
// 将各法律专题子社区纳入 法律AI 顶级社区
// ============================================================
MATCH (comm0:Community {uuid: 'community-legal-ai-001', graph_id: 'legal-knowledge-graph'})
MATCH (comm1:Community {uuid: 'community-commercial-mediation-001', graph_id: 'legal-knowledge-graph'})
MERGE (comm0)-[:PARENT_OF]->(comm1);

MATCH (comm0:Community {uuid: 'community-legal-ai-001', graph_id: 'legal-knowledge-graph'})
MATCH (comm2:Community {uuid: 'community-company-dissolution-001', graph_id: 'legal-knowledge-graph'})
MERGE (comm0)-[:PARENT_OF]->(comm2);

MATCH (comm0:Community {uuid: 'community-legal-ai-001', graph_id: 'legal-knowledge-graph'})
MATCH (comm3:Community {uuid: 'community-jurisdiction-dispute-001', graph_id: 'legal-knowledge-graph'})
MERGE (comm0)-[:PARENT_OF]->(comm3);

MATCH (comm0:Community {uuid: 'community-legal-ai-001', graph_id: 'legal-knowledge-graph'})
MATCH (comm4:Community {uuid: 'community-ip-patent-001', graph_id: 'legal-knowledge-graph'})
MERGE (comm0)-[:PARENT_OF]->(comm4);

MATCH (comm0:Community {uuid: 'community-legal-ai-001', graph_id: 'legal-knowledge-graph'})
MATCH (comm5:Community {uuid: 'community-labor-dispute-001', graph_id: 'legal-knowledge-graph'})
MERGE (comm0)-[:PARENT_OF]->(comm5);

MATCH (comm0:Community {uuid: 'community-legal-ai-001', graph_id: 'legal-knowledge-graph'})
MATCH (comm6:Community {uuid: 'community-civil-law-foundation-001', graph_id: 'legal-knowledge-graph'})
MERGE (comm0)-[:PARENT_OF]->(comm6);

// ============================================================
// 社区成员关联关系 (HAS_COMMUNITY)
// 将法律实体归入对应的社区
// ============================================================

// 商事调解社区成员
MATCH (lp1:Entity {type: 'LegalProvision', provisionId: 'SMTL-001', graph_id: 'legal-knowledge-graph'})
MATCH (comm1:Community {uuid: 'community-commercial-mediation-001', graph_id: 'legal-knowledge-graph'})
MERGE (lp1)-[:HAS_COMMUNITY]->(comm1);

MATCH (lp2:Entity {type: 'LegalProvision', provisionId: 'SMTL-002', graph_id: 'legal-knowledge-graph'})
MATCH (comm1:Community {uuid: 'community-commercial-mediation-001', graph_id: 'legal-knowledge-graph'})
MERGE (lp2)-[:HAS_COMMUNITY]->(comm1);

MATCH (lp3:Entity {type: 'LegalProvision', provisionId: 'SMTL-008', graph_id: 'legal-knowledge-graph'})
MATCH (comm1:Community {uuid: 'community-commercial-mediation-001', graph_id: 'legal-knowledge-graph'})
MERGE (lp3)-[:HAS_COMMUNITY]->(comm1);

MATCH (lp4:Entity {type: 'LegalProvision', provisionId: 'SMTL-012', graph_id: 'legal-knowledge-graph'})
MATCH (comm1:Community {uuid: 'community-commercial-mediation-001', graph_id: 'legal-knowledge-graph'})
MERGE (lp4)-[:HAS_COMMUNITY]->(comm1);

MATCH (lp5:Entity {type: 'LegalProvision', provisionId: 'SMTL-014', graph_id: 'legal-knowledge-graph'})
MATCH (comm1:Community {uuid: 'community-commercial-mediation-001', graph_id: 'legal-knowledge-graph'})
MERGE (lp5)-[:HAS_COMMUNITY]->(comm1);

MATCH (lp6:Entity {type: 'LegalProvision', provisionId: 'SMTL-022', graph_id: 'legal-knowledge-graph'})
MATCH (comm1:Community {uuid: 'community-commercial-mediation-001', graph_id: 'legal-knowledge-graph'})
MERGE (lp6)-[:HAS_COMMUNITY]->(comm1);

MATCH (mo:Entity {type: 'CommercialMediationOrganization', graph_id: 'legal-knowledge-graph'})
MATCH (comm1:Community {uuid: 'community-commercial-mediation-001', graph_id: 'legal-knowledge-graph'})
MERGE (mo)-[:HAS_COMMUNITY]->(comm1);

MATCH (m:Entity {type: 'Mediator', graph_id: 'legal-knowledge-graph'})
MATCH (comm1:Community {uuid: 'community-commercial-mediation-001', graph_id: 'legal-knowledge-graph'})
MERGE (m)-[:HAS_COMMUNITY]->(comm1);

MATCH (ma:Entity {type: 'MediationAgreement', graph_id: 'legal-knowledge-graph'})
MATCH (comm1:Community {uuid: 'community-commercial-mediation-001', graph_id: 'legal-knowledge-graph'})
MERGE (ma)-[:HAS_COMMUNITY]->(comm1);

// 公司解散社区成员
MATCH (lp7:Entity {type: 'LegalProvision', provisionId: 'MFD-069', graph_id: 'legal-knowledge-graph'})
MATCH (comm2:Community {uuid: 'community-company-dissolution-001', graph_id: 'legal-knowledge-graph'})
MERGE (lp7)-[:HAS_COMMUNITY]->(comm2);

MATCH (lp9:Entity {type: 'LegalProvision', provisionId: 'GSF2-001', graph_id: 'legal-knowledge-graph'})
MATCH (comm2:Community {uuid: 'community-company-dissolution-001', graph_id: 'legal-knowledge-graph'})
MERGE (lp9)-[:HAS_COMMUNITY]->(comm2);

MATCH (ca1:Entity {type: 'Case', caseNumber: '（2023）沪01民终11293号', graph_id: 'legal-knowledge-graph'})
MATCH (comm2:Community {uuid: 'community-company-dissolution-001', graph_id: 'legal-knowledge-graph'})
MERGE (ca1)-[:HAS_COMMUNITY]->(comm2);

MATCH (jd1:Entity {type: 'JudgmentDocument', documentNumber: '（2022）沪0105民初21387号', graph_id: 'legal-knowledge-graph'})
MATCH (comm2:Community {uuid: 'community-company-dissolution-001', graph_id: 'legal-knowledge-graph'})
MERGE (jd1)-[:HAS_COMMUNITY]->(comm2);

MATCH (jd2:Entity {type: 'JudgmentDocument', documentNumber: '（2023）沪01民终11293号', graph_id: 'legal-knowledge-graph'})
MATCH (comm2:Community {uuid: 'community-company-dissolution-001', graph_id: 'legal-knowledge-graph'})
MERGE (jd2)-[:HAS_COMMUNITY]->(comm2);

MATCH (r1:Entity {type: 'CaseReasoning', graph_id: 'legal-knowledge-graph'})
WHERE r1.reasoning STARTS WITH '公司解散纠纷是股东在穷尽公司自治'
MATCH (comm2:Community {uuid: 'community-company-dissolution-001', graph_id: 'legal-knowledge-graph'})
MERGE (r1)-[:HAS_COMMUNITY]->(comm2);

// 管辖权争议社区成员
MATCH (lp10:Entity {type: 'LegalProvision', provisionId: 'MSSSF-022', graph_id: 'legal-knowledge-graph'})
MATCH (comm3:Community {uuid: 'community-jurisdiction-dispute-001', graph_id: 'legal-knowledge-graph'})
MERGE (lp10)-[:HAS_COMMUNITY]->(comm3);

MATCH (lp11:Entity {type: 'LegalProvision', provisionId: 'MSSSF-023', graph_id: 'legal-knowledge-graph'})
MATCH (comm3:Community {uuid: 'community-jurisdiction-dispute-001', graph_id: 'legal-knowledge-graph'})
MERGE (lp11)-[:HAS_COMMUNITY]->(comm3);

MATCH (lp12:Entity {type: 'LegalProvision', provisionId: 'MSSSF-024', graph_id: 'legal-knowledge-graph'})
MATCH (comm3:Community {uuid: 'community-jurisdiction-dispute-001', graph_id: 'legal-knowledge-graph'})
MERGE (lp12)-[:HAS_COMMUNITY]->(comm3);

MATCH (ca2:Entity {type: 'Case', caseNumber: '（2020）渝民辖188号', graph_id: 'legal-knowledge-graph'})
MATCH (comm3:Community {uuid: 'community-jurisdiction-dispute-001', graph_id: 'legal-knowledge-graph'})
MERGE (ca2)-[:HAS_COMMUNITY]->(comm3);

MATCH (jd3:Entity {type: 'JudgmentDocument', documentNumber: '（2020）渝民辖188号', graph_id: 'legal-knowledge-graph'})
MATCH (comm3:Community {uuid: 'community-jurisdiction-dispute-001', graph_id: 'legal-knowledge-graph'})
MERGE (jd3)-[:HAS_COMMUNITY]->(comm3);

MATCH (r2:Entity {type: 'CaseReasoning', graph_id: 'legal-knowledge-graph'})
WHERE r2.reasoning STARTS WITH '共同被告中仅有部分被告被监禁'
MATCH (comm3:Community {uuid: 'community-jurisdiction-dispute-001', graph_id: 'legal-knowledge-graph'})
MERGE (r2)-[:HAS_COMMUNITY]->(comm3);

// ============================================================
// 第三部分: 事件节点 (Episode) — :Episode
// 法律过程中的具体事件: 案件受理、庭审、调解、判决、执行等
// 关系: MENTIONS (Episode -> Entity)
//       NEXT_EPISODE (Episode -> Episode, 时序链)
// ============================================================

// ---------- 案例一: 公司解散纠纷（2023）沪01民终11293号 ----------

// 事件1: 股权转让纠纷初始接触
MERGE (ep1:Episode {
  graph_id: 'legal-knowledge-graph',
  uuid: 'episode-company-dissolution-001',
  name: '股权转让与股东资格取得',
  source: 'case_file',
  source_description: '（2023）沪01民终11293号 案件档案',
  content: '2020年3月30日，原告徐某骥通过股权转让协议受让被告上海某物业管理有限公司五位股东持有的全部股权，成为持股39.54%的股东。股权转让完成后，原告与第三人孙某瑾就公司经营权产生纠纷。',
  episode_type: '股权变动',
  episode_stage: '起因',
  legal_process: 'litigation',

  is_trial_stage: false,
  start_time: datetime('2020-03-30T00:00:00'),
  end_time: datetime('2020-03-30T23:59:59'),
  legal_significance: '确定原告股东资格及持股比例',
  valid_at: timestamp(),
  created_at: datetime()
});

// 事件2: 股东矛盾激化
MERGE (ep2:Episode {
  graph_id: 'legal-knowledge-graph',
  uuid: 'episode-company-dissolution-002',
  name: '股东矛盾与分红争议',
  source: 'case_file',
  source_description: '（2023）沪01民终11293号 案件档案',
  content: '原告与第三人孙某瑾就公司经营、利润分红、办公室使用等问题产生严重分歧。原告称第三人把持公司、拒绝分红、拒绝提供办公室，多次要求召开股东会未果。双方就公司经营管理产生纠纷，原告开始寻求司法救济途径。',
  episode_type: '股东争议',
  episode_stage: '发展',
  legal_process: 'litigation',

  is_trial_stage: false,
  start_time: datetime('2021-01-01T00:00:00'),
  end_time: datetime('2022-01-01T00:00:00'),
  legal_significance: '公司僵局的形成与股东内部救济的尝试',
  valid_at: timestamp(),
  created_at: datetime()
});

// 事件3: 一审起诉
MERGE (ep3:Episode {
  graph_id: 'legal-knowledge-graph',
  uuid: 'episode-company-dissolution-003',
  name: '一审起诉与立案',
  source: 'case_file',
  source_description: '（2022）沪0105民初21387号',
  content: '原告徐某骥以公司经营管理发生严重困难、继续存续会使股东利益遭受重大损失为由，向上海市长宁区人民法院提起公司解散诉讼。法院受理案件并进行审理。',
  episode_type: '立案',
  episode_stage: '诉讼启动',
  legal_process: 'litigation',

  is_trial_stage: false,
  start_time: datetime('2022-01-01T00:00:00'),
  end_time: datetime('2022-01-01T00:00:00'),
  legal_significance: '启动司法救济程序',
  valid_at: timestamp(),
  created_at: datetime()
});

// 事件4: 一审庭审与举证
MERGE (ep4:Episode {
  graph_id: 'legal-knowledge-graph',
  uuid: 'episode-company-dissolution-004',
  name: '一审庭审与证据交换',
  source: 'case_file',
  source_description: '（2022）沪0105民初21387号',
  content: '一审庭审中，原告提交股权转让协议、微信聊天记录等证据证明股东资格及曾要求召开股东会；被告提交股东会决议、银行转账记录证明公司正常运营并向原告分红。一审法院审理后认定公司运营良好，判决驳回原告全部诉讼请求。',
  episode_type: '庭审',
  episode_stage: '审理',
  legal_process: 'litigation',
  court_level: '一审',
  is_trial_stage: true,
  start_time: datetime('2022-06-01T00:00:00'),
  end_time: datetime('2023-05-04T00:00:00'),
  legal_significance: '一审认定公司未达解散条件',
  valid_at: timestamp(),
  created_at: datetime()
});

// 事件5: 一审判决
MERGE (ep5:Episode {
  graph_id: 'legal-knowledge-graph',
  uuid: 'episode-company-dissolution-005',
  name: '一审判决: 驳回解散诉请',
  source: 'judgment',
  source_description: '（2022）沪0105民初21387号 民事判决书',
  content: '上海市长宁区人民法院作出（2022）沪0105民初21387号民事判决，驳回原告徐某骥的全部诉讼请求。判决理由：（1）原告与第三人曾就公司经营协商达成一致；（2）公司尚在经营并处于盈利状态；（3）原告可通过转让股权等途径解决纠纷。',
  episode_type: 'EP_JUDGMENT_1ST',
  episode_stage: '裁判',
  legal_process: 'litigation',
  court_level: '一审',
  is_trial_stage: true,
  start_time: datetime('2023-05-04T00:00:00'),
  end_time: datetime('2023-05-04T00:00:00'),
  legal_significance: '一审不支持解散公司',
  valid_at: timestamp(),
  created_at: datetime()
});

// 事件6: 二审上诉
MERGE (ep6:Episode {
  graph_id: 'legal-knowledge-graph',
  uuid: 'episode-company-dissolution-006',
  name: '提起上诉',
  source: 'case_file',
  source_description: '（2023）沪01民终11293号',
  content: '原告徐某骥不服一审判决，向上海市第一中级人民法院提起上诉，请求撤销一审判决，改判解散上海某物业管理有限公司。上诉理由维持原有主张，认为公司经营管理存在严重困难。',
  episode_type: 'EP_APPEAL',
  episode_stage: '二审启动',
  legal_process: 'litigation',

  is_trial_stage: false,
  start_time: datetime('2023-06-01T00:00:00'),
  end_time: datetime('2023-06-30T00:00:00'),
  legal_significance: '启动二审程序',
  valid_at: timestamp(),
  created_at: datetime()
});

// 事件7: 二审审理
MERGE (ep7:Episode {
  graph_id: 'legal-knowledge-graph',
  uuid: 'episode-company-dissolution-007',
  name: '二审审理与裁判',
  source: 'case_file',
  source_description: '（2023）沪01民终11293号',
  content: '上海市第一中级人民法院依法组成合议庭，对本案进行审理。经审查，二审法院认为：（1）一审法院认定事实清楚；（2）公司尚在正常经营，原审原告可通过转让股权等其他途径解决；（3）一审适用法律正确。最终驳回上诉，维持原判。',
  episode_type: 'EP_TRIAL_2ND',
  episode_stage: '二审裁判',
  legal_process: 'litigation',
  court_level: '二审',
  is_trial_stage: true,
  start_time: datetime('2023-09-01T00:00:00'),
  end_time: datetime('2023-10-24T00:00:00'),
  legal_significance: '二审维持一审，案件终审',
  valid_at: timestamp(),
  created_at: datetime()
});

// 事件8: 终审判决
MERGE (ep8:Episode {
  graph_id: 'legal-knowledge-graph',
  uuid: 'episode-company-dissolution-008',
  name: '终审判决: 驳回上诉维持原判',
  source: 'judgment',
  source_description: '（2023）沪01民终11293号 民事判决书',
  content: '上海市第一中级人民法院作出（2023）沪01民终11293号民事判决：驳回上诉，维持原判。本案诉讼程序终结，公司不解散。判决于2023年10月24日作出并生效。',
  episode_type: 'EP_JUDGMENT_2ND',
  episode_stage: '终审',
  legal_process: 'litigation',
  court_level: '二审',
  is_trial_stage: true,
  start_time: datetime('2023-10-24T00:00:00'),
  end_time: datetime('2023-10-24T00:00:00'),
  legal_significance: '案件终审，公司解散诉请最终被驳回',
  valid_at: timestamp(),
  created_at: datetime()
});

// ---------- 案例二: 债权人撤销权管辖权争议（2020）渝民辖188号 ----------

// 事件9: 债权人撤销权纠纷起诉
MERGE (ep9:Episode {
  graph_id: 'legal-knowledge-graph',
  uuid: 'episode-jurisdiction-001',
  name: '债权人撤销权纠纷起诉与管辖确定',
  source: 'case_file',
  source_description: '（2020）渝民辖188号',
  content: '债权人谭某以债务人吴某与雒某离婚时分割房屋的行为损害其债权为由，向重庆市南岸区人民法院提起债权人撤销权诉讼，请求撤销该房屋分割行为。案件涉及共同被告中部分被告被监禁的特殊管辖问题。',
  episode_type: '立案',
  episode_stage: '诉讼启动',
  legal_process: 'litigation',

  is_trial_stage: false,
  start_time: datetime('2020-06-08T00:00:00'),
  end_time: datetime('2020-06-08T00:00:00'),
  legal_significance: '债权人启动撤销权诉讼',
  valid_at: timestamp(),
  created_at: datetime()
});

// 事件10: 管辖权异议与移送
MERGE (ep10:Episode {
  graph_id: 'legal-knowledge-graph',
  uuid: 'episode-jurisdiction-002',
  name: '管辖权异议及移送处理',
  source: 'case_file',
  source_description: '（2020）渝0108民初637号',
  content: '重庆市南岸区人民法院审查后认为，被告吴某被羁押于重庆市永川监狱，应适用《民事诉讼法》第23条关于被监禁被告的原告住所地管辖规定，将案件移送至原告谭某住所地的重庆市江北区人民法院审理。',
  episode_type: '管辖权处理',
  episode_stage: '程序推进',
  legal_process: 'litigation',

  is_trial_stage: false,
  start_time: datetime('2020-09-01T00:00:00'),
  end_time: datetime('2020-11-01T00:00:00'),
  legal_significance: '一审法院错误适用特殊管辖规定',
  valid_at: timestamp(),
  created_at: datetime()
});

// 事件11: 高级法院审查与指定管辖
MERGE (ep11:Episode {
  graph_id: 'legal-knowledge-graph',
  uuid: 'episode-jurisdiction-003',
  name: '高院审查与撤销移送裁定',
  source: 'case_file',
  source_description: '（2020）渝民辖188号',
  content: '重庆市高级人民法院经审查认为，本案系共同被告，仅部分被告被监禁，不属于《民事诉讼法》第23条"对被监禁的人提起诉讼"的适用情形，应按一般管辖规定（被告住所地）确定管辖法院。南岸区法院作为主要被告吴某（监禁地所在）法院具有管辖权。裁定撤销一审移送裁定，指定由重庆市南岸区人民法院审理。',
  episode_type: '裁定',
  episode_stage: '管辖确定',
  legal_process: 'litigation',

  is_trial_stage: false,
  start_time: datetime('2020-11-01T00:00:00'),
  end_time: datetime('2020-12-28T00:00:00'),
  legal_significance: '明确部分被告被监禁时的管辖确定规则',
  valid_at: timestamp(),
  created_at: datetime()
});

// ---------- 案例三: 商事调解 ----------

// 事件12: 技术合同纠纷诉前调解
MERGE (ep12:Episode {
  graph_id: 'legal-knowledge-graph',
  uuid: 'episode-mediation-001',
  name: '技术合同纠纷诉前调解',
  source: 'mediation_record',
  source_description: '上海国际商事调解中心调解档案',
  content: '某科技公司（申请人）与某投资公司（被申请人）就技术合同款项支付及项目验收标准产生争议，申请人向上海国际商事调解中心申请调解。经审查，该争议属于《商事调解条例》第2条规定的商事调解适用范围，调解中心受理案件并指定调解员李某主持调解。',
  episode_type: 'EP_MEDIATION_ACCEPT',
  episode_stage: '调解启动',
  legal_process: 'mediation',

  is_trial_stage: false,
  start_time: datetime('2024-04-01T00:00:00'),
  end_time: datetime('2024-04-01T00:00:00'),
  legal_significance: '启动多元化纠纷解决机制',
  valid_at: timestamp(),
  created_at: datetime()
});

// 事件13: 调解过程
MERGE (ep13:Episode {
  graph_id: 'legal-knowledge-graph',
  uuid: 'episode-mediation-002',
  name: '调解协商与达成协议',
  source: 'mediation_record',
  source_description: '上海国际商事调解中心调解档案',
  content: '调解员李某主持调解，在查清事实、分清责任的基础上，依据《商事调解条例》第14条确立的自愿、合法、诚信、保密原则，组织双方进行协商。经多轮沟通，双方就合同款项支付金额、支付方式及期限达成一致，并签署调解协议：被申请人于2024年12月31日前分期支付合同款项共计人民币150万元。',
  episode_type: 'EP_MEDIATION_NEGOTIATION',
  episode_stage: '调解进行',
  legal_process: 'mediation',

  is_trial_stage: false,
  start_time: datetime('2024-04-01T00:00:00'),
  end_time: datetime('2024-06-15T00:00:00'),
  legal_significance: '体现商事调解自愿与诚信原则',
  valid_at: timestamp(),
  created_at: datetime()
});

// 事件14: 调解协议签署
MERGE (ep14:Episode {
  graph_id: 'legal-knowledge-graph',
  uuid: 'episode-mediation-003',
  name: '调解协议签署与司法确认',
  source: 'mediation_agreement',
  source_description: 'MA20240001号商事调解协议',
  content: '2024年6月15日，双方当事人在调解员李某主持下签署MA20240001号商事调解协议。调解员在协议上签名并加盖上海国际商事调解中心印章。同日，协议经上海市浦东新区人民法院确认，出具（2024）沪0115民调确字第1234号民事裁定，赋予调解协议强制执行效力。',
  episode_type: 'EP_MEDIATION_CONFIRM',
  episode_stage: '调解完成',
  legal_process: 'mediation',

  is_trial_stage: false,
  start_time: datetime('2024-06-15T00:00:00'),
  end_time: datetime('2024-06-20T00:00:00'),
  legal_significance: '调解协议具有法律约束力并经司法确认',
  valid_at: timestamp(),
  created_at: datetime()
});

// ============================================================
// 第四部分: 事件-实体关联关系 (MENTIONS)
// Episode 提及/涉及的关键法律实体
// ============================================================

// 公司解散案件事件链 - 关联案件、法条、裁判文书、当事人
MATCH (ep1:Episode {uuid: 'episode-company-dissolution-001', graph_id: 'legal-knowledge-graph'})
MATCH (ca1:Entity {type: 'Case', caseNumber: '（2023）沪01民终11293号', graph_id: 'legal-knowledge-graph'})
MATCH (p1:Entity {type: 'Party', partyName: '徐某骥', graph_id: 'legal-knowledge-graph'})
MATCH (p2:Entity {type: 'Party', partyName: '上海某物业管理有限公司', graph_id: 'legal-knowledge-graph'})
MERGE (ep1)-[:MENTIONS {uuid: 'mentions-ep1-1', graph_id: 'legal-knowledge-graph', entity_role: '案件主体', mention_type: '直接涉及'}]->(ca1)
MERGE (ep1)-[:MENTIONS {uuid: 'mentions-ep1-2', graph_id: 'legal-knowledge-graph', entity_role: '股权受让方', mention_type: '直接涉及'}]->(p1)
MERGE (ep1)-[:MENTIONS {uuid: 'mentions-ep1-3', graph_id: 'legal-knowledge-graph', entity_role: '标的公司', mention_type: '直接涉及'}]->(p2);

MATCH (ep2:Episode {uuid: 'episode-company-dissolution-002', graph_id: 'legal-knowledge-graph'})
MATCH (ca1:Entity {type: 'Case', caseNumber: '（2023）沪01民终11293号', graph_id: 'legal-knowledge-graph'})
MATCH (p1:Entity {type: 'Party', partyName: '徐某骥', graph_id: 'legal-knowledge-graph'})
MATCH (p3:Entity {type: 'Party', partyName: '孙某瑾', graph_id: 'legal-knowledge-graph'})
MATCH (f3:Entity {type: 'CaseFact', uuid: 'fact-c1-shareholder-dispute-002', graph_id: 'legal-knowledge-graph'})
MERGE (ep2)-[:MENTIONS {uuid: 'mentions-ep2-1', graph_id: 'legal-knowledge-graph', entity_role: '案件背景', mention_type: '直接涉及'}]->(ca1)
MERGE (ep2)-[:MENTIONS {uuid: 'mentions-ep2-2', graph_id: 'legal-knowledge-graph', entity_role: '争议股东', mention_type: '直接涉及'}]->(p1)
MERGE (ep2)-[:MENTIONS {uuid: 'mentions-ep2-3', graph_id: 'legal-knowledge-graph', entity_role: '争议相对人', mention_type: '直接涉及'}]->(p3)
MERGE (ep2)-[:MENTIONS {uuid: 'mentions-ep2-4', graph_id: 'legal-knowledge-graph', entity_role: '关键事实', mention_type: '直接涉及'}]->(f3);

MATCH (ep3:Episode {uuid: 'episode-company-dissolution-003', graph_id: 'legal-knowledge-graph'})
MATCH (ca1:Entity {type: 'Case', caseNumber: '（2023）沪01民终11293号', graph_id: 'legal-knowledge-graph'})
MATCH (c1:Entity {type: 'Court', courtName: '上海市长宁区人民法院', graph_id: 'legal-knowledge-graph'})
MATCH (jd1:Entity {type: 'JudgmentDocument', documentNumber: '（2022）沪0105民初21387号', graph_id: 'legal-knowledge-graph'})
MATCH (lp7:Entity {type: 'LegalProvision', provisionId: 'MFD-069', graph_id: 'legal-knowledge-graph'})
MATCH (lp9:Entity {type: 'LegalProvision', provisionId: 'GSF2-001', graph_id: 'legal-knowledge-graph'})
MERGE (ep3)-[:MENTIONS {uuid: 'mentions-ep3-1', graph_id: 'legal-knowledge-graph', entity_role: '诉讼标的', mention_type: '直接涉及'}]->(ca1)
MERGE (ep3)-[:MENTIONS {uuid: 'mentions-ep3-2', graph_id: 'legal-knowledge-graph', entity_role: '受理法院', mention_type: '直接涉及'}]->(c1)
MERGE (ep3)-[:MENTIONS {uuid: 'mentions-ep3-3', graph_id: 'legal-knowledge-graph', entity_role: '程序载体', mention_type: '直接涉及'}]->(jd1)
MERGE (ep3)-[:MENTIONS {uuid: 'mentions-ep3-4', graph_id: 'legal-knowledge-graph', entity_role: '法律依据', mention_type: '法律引用'}]->(lp7)
MERGE (ep3)-[:MENTIONS {uuid: 'mentions-ep3-5', graph_id: 'legal-knowledge-graph', entity_role: '法律依据', mention_type: '法律引用'}]->(lp9);

MATCH (ep4:Episode {uuid: 'episode-company-dissolution-004', graph_id: 'legal-knowledge-graph'})
MATCH (ca1:Entity {type: 'Case', caseNumber: '（2023）沪01民终11293号', graph_id: 'legal-knowledge-graph'})
MATCH (e1:Entity {type: 'Evidence', evidenceNumber: '证据001', graph_id: 'legal-knowledge-graph'})
MATCH (e2:Entity {type: 'Evidence', evidenceNumber: '证据002', graph_id: 'legal-knowledge-graph'})
MATCH (e3:Entity {type: 'Evidence', evidenceNumber: '证据003', graph_id: 'legal-knowledge-graph'})
MATCH (e4:Entity {type: 'Evidence', evidenceNumber: '证据004', graph_id: 'legal-knowledge-graph'})
MATCH (r1:Entity {type: 'CaseReasoning', graph_id: 'legal-knowledge-graph'})
WHERE r1.reasoning STARTS WITH '公司解散纠纷是股东在穷尽公司自治'
MERGE (ep4)-[:MENTIONS {uuid: 'mentions-ep4-1', graph_id: 'legal-knowledge-graph', entity_role: '审理对象', mention_type: '直接涉及'}]->(ca1)
MERGE (ep4)-[:MENTIONS {uuid: 'mentions-ep4-2', graph_id: 'legal-knowledge-graph', entity_role: '原告证据', mention_type: '证据提及'}]->(e1)
MERGE (ep4)-[:MENTIONS {uuid: 'mentions-ep4-3', graph_id: 'legal-knowledge-graph', entity_role: '原告证据', mention_type: '证据提及'}]->(e2)
MERGE (ep4)-[:MENTIONS {uuid: 'mentions-ep4-4', graph_id: 'legal-knowledge-graph', entity_role: '被告证据', mention_type: '证据提及'}]->(e3)
MERGE (ep4)-[:MENTIONS {uuid: 'mentions-ep4-5', graph_id: 'legal-knowledge-graph', entity_role: '被告证据', mention_type: '证据提及'}]->(e4)
MERGE (ep4)-[:MENTIONS {uuid: 'mentions-ep4-6', graph_id: 'legal-knowledge-graph', entity_role: '裁判要旨', mention_type: '要旨引用'}]->(r1);

MATCH (ep5:Episode {uuid: 'episode-company-dissolution-005', graph_id: 'legal-knowledge-graph'})
MATCH (jd1:Entity {type: 'JudgmentDocument', documentNumber: '（2022）沪0105民初21387号', graph_id: 'legal-knowledge-graph'})
MATCH (lp7:Entity {type: 'LegalProvision', provisionId: 'MFD-069', graph_id: 'legal-knowledge-graph'})
MATCH (lp9:Entity {type: 'LegalProvision', provisionId: 'GSF2-001', graph_id: 'legal-knowledge-graph'})
MATCH (f2:Entity {type: 'CaseFact', uuid: 'fact-c1-company-profitable-001', graph_id: 'legal-knowledge-graph'})
MATCH (r1:Entity {type: 'CaseReasoning', graph_id: 'legal-knowledge-graph'})
WHERE r1.reasoning STARTS WITH '公司解散纠纷是股东在穷尽公司自治'
MERGE (ep5)-[:MENTIONS {uuid: 'mentions-ep5-1', graph_id: 'legal-knowledge-graph', entity_role: '裁判文书', mention_type: '直接涉及'}]->(jd1)
MERGE (ep5)-[:MENTIONS {uuid: 'mentions-ep5-2', graph_id: 'legal-knowledge-graph', entity_role: '适用法条', mention_type: '法律引用'}]->(lp7)
MERGE (ep5)-[:MENTIONS {uuid: 'mentions-ep5-3', graph_id: 'legal-knowledge-graph', entity_role: '适用法条', mention_type: '法律引用'}]->(lp9)
MERGE (ep5)-[:MENTIONS {uuid: 'mentions-ep5-4', graph_id: 'legal-knowledge-graph', entity_role: '关键事实', mention_type: '事实引用'}]->(f2)
MERGE (ep5)-[:MENTIONS {uuid: 'mentions-ep5-5', graph_id: 'legal-knowledge-graph', entity_role: '裁判要旨', mention_type: '要旨引用'}]->(r1);

MATCH (ep6:Episode {uuid: 'episode-company-dissolution-006', graph_id: 'legal-knowledge-graph'})
MATCH (ca1:Entity {type: 'Case', caseNumber: '（2023）沪01民终11293号', graph_id: 'legal-knowledge-graph'})
MATCH (p1:Entity {type: 'Party', partyName: '徐某骥', graph_id: 'legal-knowledge-graph'})
MATCH (jd1:Entity {type: 'JudgmentDocument', documentNumber: '（2022）沪0105民初21387号', graph_id: 'legal-knowledge-graph'})
MATCH (c2:Entity {type: 'Court', courtName: '上海市第一中级人民法院', graph_id: 'legal-knowledge-graph'})
MERGE (ep6)-[:MENTIONS {uuid: 'mentions-ep6-1', graph_id: 'legal-knowledge-graph', entity_role: '诉讼标的', mention_type: '直接涉及'}]->(ca1)
MERGE (ep6)-[:MENTIONS {uuid: 'mentions-ep6-2', graph_id: 'legal-knowledge-graph', entity_role: '上诉人', mention_type: '直接涉及'}]->(p1)
MERGE (ep6)-[:MENTIONS {uuid: 'mentions-ep6-3', graph_id: 'legal-knowledge-graph', entity_role: '上诉对象', mention_type: '直接涉及'}]->(jd1)
MERGE (ep6)-[:MENTIONS {uuid: 'mentions-ep6-4', graph_id: 'legal-knowledge-graph', entity_role: '上诉法院', mention_type: '直接涉及'}]->(c2);

MATCH (ep7:Episode {uuid: 'episode-company-dissolution-007', graph_id: 'legal-knowledge-graph'})
MATCH (ca1:Entity {type: 'Case', caseNumber: '（2023）沪01民终11293号', graph_id: 'legal-knowledge-graph'})
MATCH (jd2:Entity {type: 'JudgmentDocument', documentNumber: '（2023）沪01民终11293号', graph_id: 'legal-knowledge-graph'})
MATCH (lp7:Entity {type: 'LegalProvision', provisionId: 'MFD-069', graph_id: 'legal-knowledge-graph'})
MATCH (lp9:Entity {type: 'LegalProvision', provisionId: 'GSF2-001', graph_id: 'legal-knowledge-graph'})
MATCH (c2:Entity {type: 'Court', courtName: '上海市第一中级人民法院', graph_id: 'legal-knowledge-graph'})
MATCH (c1:Entity {type: 'Court', courtName: '上海市长宁区人民法院', graph_id: 'legal-knowledge-graph'})
MERGE (ep7)-[:MENTIONS {uuid: 'mentions-ep7-1', graph_id: 'legal-knowledge-graph', entity_role: '审理对象', mention_type: '直接涉及'}]->(ca1)
MERGE (ep7)-[:MENTIONS {uuid: 'mentions-ep7-2', graph_id: 'legal-knowledge-graph', entity_role: '二审载体', mention_type: '直接涉及'}]->(jd2)
MERGE (ep7)-[:MENTIONS {uuid: 'mentions-ep7-3', graph_id: 'legal-knowledge-graph', entity_role: '二审法条', mention_type: '法律引用'}]->(lp7)
MERGE (ep7)-[:MENTIONS {uuid: 'mentions-ep7-4', graph_id: 'legal-knowledge-graph', entity_role: '二审法条', mention_type: '法律引用'}]->(lp9)
MERGE (ep7)-[:MENTIONS {uuid: 'mentions-ep7-5', graph_id: 'legal-knowledge-graph', entity_role: '二审法院', mention_type: '直接涉及'}]->(c2)
MERGE (ep7)-[:MENTIONS {uuid: 'mentions-ep7-6', graph_id: 'legal-knowledge-graph', entity_role: '一审法院', mention_type: '参照'}]->(c1);

MATCH (ep8:Episode {uuid: 'episode-company-dissolution-008', graph_id: 'legal-knowledge-graph'})
MATCH (ca1:Entity {type: 'Case', caseNumber: '（2023）沪01民终11293号', graph_id: 'legal-knowledge-graph'})
MATCH (jd2:Entity {type: 'JudgmentDocument', documentNumber: '（2023）沪01民终11293号', graph_id: 'legal-knowledge-graph'})
MATCH (c2:Entity {type: 'Court', courtName: '上海市第一中级人民法院', graph_id: 'legal-knowledge-graph'})
MERGE (ep8)-[:MENTIONS {uuid: 'mentions-ep8-1', graph_id: 'legal-knowledge-graph', entity_role: '终审案件', mention_type: '直接涉及'}]->(ca1)
MERGE (ep8)-[:MENTIONS {uuid: 'mentions-ep8-2', graph_id: 'legal-knowledge-graph', entity_role: '终审文书', mention_type: '直接涉及'}]->(jd2)
MERGE (ep8)-[:MENTIONS {uuid: 'mentions-ep8-3', graph_id: 'legal-knowledge-graph', entity_role: '终审法院', mention_type: '直接涉及'}]->(c2);

// 管辖权案件事件链
MATCH (ep9:Episode {uuid: 'episode-jurisdiction-001', graph_id: 'legal-knowledge-graph'})
MATCH (ca2:Entity {type: 'Case', caseNumber: '（2020）渝民辖188号', graph_id: 'legal-knowledge-graph'})
MATCH (p5:Entity {type: 'Party', partyName: '谭某', graph_id: 'legal-knowledge-graph'})
MATCH (p6:Entity {type: 'Party', partyName: '吴某', graph_id: 'legal-knowledge-graph'})
MATCH (p7:Entity {type: 'Party', partyName: '雒某', graph_id: 'legal-knowledge-graph'})
MATCH (lp10:Entity {type: 'LegalProvision', provisionId: 'MSSSF-022', graph_id: 'legal-knowledge-graph'})
MATCH (lp11:Entity {type: 'LegalProvision', provisionId: 'MSSSF-023', graph_id: 'legal-knowledge-graph'})
MERGE (ep9)-[:MENTIONS {uuid: 'mentions-ep9-1', graph_id: 'legal-knowledge-graph', entity_role: '诉讼标的', mention_type: '直接涉及'}]->(ca2)
MERGE (ep9)-[:MENTIONS {uuid: 'mentions-ep9-2', graph_id: 'legal-knowledge-graph', entity_role: '原告', mention_type: '直接涉及'}]->(p5)
MERGE (ep9)-[:MENTIONS {uuid: 'mentions-ep9-3', graph_id: 'legal-knowledge-graph', entity_role: '被告', mention_type: '直接涉及'}]->(p6)
MERGE (ep9)-[:MENTIONS {uuid: 'mentions-ep9-4', graph_id: 'legal-knowledge-graph', entity_role: '被告', mention_type: '直接涉及'}]->(p7)
MERGE (ep9)-[:MENTIONS {uuid: 'mentions-ep9-5', graph_id: 'legal-knowledge-graph', entity_role: '管辖依据', mention_type: '法律引用'}]->(lp10)
MERGE (ep9)-[:MENTIONS {uuid: 'mentions-ep9-6', graph_id: 'legal-knowledge-graph', entity_role: '管辖依据', mention_type: '法律引用'}]->(lp11);

MATCH (ep10:Episode {uuid: 'episode-jurisdiction-002', graph_id: 'legal-knowledge-graph'})
MATCH (ca2:Entity {type: 'Case', caseNumber: '（2020）渝民辖188号', graph_id: 'legal-knowledge-graph'})
MATCH (c5:Entity {type: 'Court', courtName: '重庆市南岸区人民法院', graph_id: 'legal-knowledge-graph'})
MATCH (c7:Entity {type: 'Court', courtName: '重庆市江北区人民法院', graph_id: 'legal-knowledge-graph'})
MATCH (p6:Entity {type: 'Party', partyName: '吴某', graph_id: 'legal-knowledge-graph'})
MATCH (lp11:Entity {type: 'LegalProvision', provisionId: 'MSSSF-023', graph_id: 'legal-knowledge-graph'})
MERGE (ep10)-[:MENTIONS {uuid: 'mentions-ep10-1', graph_id: 'legal-knowledge-graph', entity_role: '程序载体', mention_type: '直接涉及'}]->(ca2)
MERGE (ep10)-[:MENTIONS {uuid: 'mentions-ep10-2', graph_id: 'legal-knowledge-graph', entity_role: '原审法院', mention_type: '直接涉及'}]->(c5)
MERGE (ep10)-[:MENTIONS {uuid: 'mentions-ep10-3', graph_id: 'legal-knowledge-graph', entity_role: '移送法院', mention_type: '直接涉及'}]->(c7)
MERGE (ep10)-[:MENTIONS {uuid: 'mentions-ep10-4', graph_id: 'legal-knowledge-graph', entity_role: '被监禁被告', mention_type: '直接涉及'}]->(p6)
MERGE (ep10)-[:MENTIONS {uuid: 'mentions-ep10-5', graph_id: 'legal-knowledge-graph', entity_role: '适用法条', mention_type: '法律引用'}]->(lp11);

MATCH (ep11:Episode {uuid: 'episode-jurisdiction-003', graph_id: 'legal-knowledge-graph'})
MATCH (ca2:Entity {type: 'Case', caseNumber: '（2020）渝民辖188号', graph_id: 'legal-knowledge-graph'})
MATCH (c5:Entity {type: 'Court', courtName: '重庆市南岸区人民法院', graph_id: 'legal-knowledge-graph'})
MATCH (c6:Entity {type: 'Court', courtName: '重庆市高级人民法院', graph_id: 'legal-knowledge-graph'})
MATCH (jd3:Entity {type: 'JudgmentDocument', documentNumber: '（2020）渝民辖188号', graph_id: 'legal-knowledge-graph'})
MATCH (lp10:Entity {type: 'LegalProvision', provisionId: 'MSSSF-022', graph_id: 'legal-knowledge-graph'})
MATCH (lp11:Entity {type: 'LegalProvision', provisionId: 'MSSSF-023', graph_id: 'legal-knowledge-graph'})
MATCH (r2:Entity {type: 'CaseReasoning', graph_id: 'legal-knowledge-graph'})
WHERE r2.reasoning STARTS WITH '共同被告中仅有部分被告被监禁'
MERGE (ep11)-[:MENTIONS {uuid: 'mentions-ep11-1', graph_id: 'legal-knowledge-graph', entity_role: '裁定载体', mention_type: '直接涉及'}]->(jd3)
MERGE (ep11)-[:MENTIONS {uuid: 'mentions-ep11-2', graph_id: 'legal-knowledge-graph', entity_role: '终局确定法院', mention_type: '直接涉及'}]->(c5)
MERGE (ep11)-[:MENTIONS {uuid: 'mentions-ep11-3', graph_id: 'legal-knowledge-graph', entity_role: '审查法院', mention_type: '直接涉及'}]->(c6)
MERGE (ep11)-[:MENTIONS {uuid: 'mentions-ep11-4', graph_id: 'legal-knowledge-graph', entity_role: '案件载体', mention_type: '直接涉及'}]->(ca2)
MERGE (ep11)-[:MENTIONS {uuid: 'mentions-ep11-5', graph_id: 'legal-knowledge-graph', entity_role: '一般管辖依据', mention_type: '法律引用'}]->(lp10)
MERGE (ep11)-[:MENTIONS {uuid: 'mentions-ep11-6', graph_id: 'legal-knowledge-graph', entity_role: '特别管辖依据', mention_type: '法律引用'}]->(lp11)
MERGE (ep11)-[:MENTIONS {uuid: 'mentions-ep11-7', graph_id: 'legal-knowledge-graph', entity_role: '裁判要旨', mention_type: '要旨引用'}]->(r2);

// 商事调解事件链
MATCH (ep12:Episode {uuid: 'episode-mediation-001', graph_id: 'legal-knowledge-graph'})
MATCH (ca_demo:Entity {type: 'Case', caseNumber: '（2024）沪0115商初1234号', graph_id: 'legal-knowledge-graph'})
MATCH (mo:Entity {type: 'CommercialMediationOrganization', graph_id: 'legal-knowledge-graph'})
MATCH (m:Entity {type: 'Mediator', graph_id: 'legal-knowledge-graph'})
MATCH (lp2:Entity {type: 'LegalProvision', provisionId: 'SMTL-002', graph_id: 'legal-knowledge-graph'})
MATCH (lp5:Entity {type: 'LegalProvision', provisionId: 'SMTL-014', graph_id: 'legal-knowledge-graph'})
MERGE (ep12)-[:MENTIONS {uuid: 'mentions-ep12-1', graph_id: 'legal-knowledge-graph', entity_role: '案件载体', mention_type: '直接涉及'}]->(ca_demo)
MERGE (ep12)-[:MENTIONS {uuid: 'mentions-ep12-2', graph_id: 'legal-knowledge-graph', entity_role: '受理组织', mention_type: '直接涉及'}]->(mo)
MERGE (ep12)-[:MENTIONS {uuid: 'mentions-ep12-3', graph_id: 'legal-knowledge-graph', entity_role: '指定调解员', mention_type: '直接涉及'}]->(m)
MERGE (ep12)-[:MENTIONS {uuid: 'mentions-ep12-4', graph_id: 'legal-knowledge-graph', entity_role: '适用范围', mention_type: '法律依据'}]->(lp2)
MERGE (ep12)-[:MENTIONS {uuid: 'mentions-ep12-5', graph_id: 'legal-knowledge-graph', entity_role: '调解原则', mention_type: '法律依据'}]->(lp5);

MATCH (ep13:Episode {uuid: 'episode-mediation-002', graph_id: 'legal-knowledge-graph'})
MATCH (mo:Entity {type: 'CommercialMediationOrganization', graph_id: 'legal-knowledge-graph'})
MATCH (m:Entity {type: 'Mediator', graph_id: 'legal-knowledge-graph'})
MATCH (lp5:Entity {type: 'LegalProvision', provisionId: 'SMTL-014', graph_id: 'legal-knowledge-graph'})
MATCH (lp6:Entity {type: 'LegalProvision', provisionId: 'SMTL-022', graph_id: 'legal-knowledge-graph'})
MERGE (ep13)-[:MENTIONS {uuid: 'mentions-ep13-1', graph_id: 'legal-knowledge-graph', entity_role: '调解组织', mention_type: '直接涉及'}]->(mo)
MERGE (ep13)-[:MENTIONS {uuid: 'mentions-ep13-2', graph_id: 'legal-knowledge-graph', entity_role: '主持调解', mention_type: '直接涉及'}]->(m)
MERGE (ep13)-[:MENTIONS {uuid: 'mentions-ep13-3', graph_id: 'legal-knowledge-graph', entity_role: '原则依据', mention_type: '法律依据'}]->(lp5)
MERGE (ep13)-[:MENTIONS {uuid: 'mentions-ep13-4', graph_id: 'legal-knowledge-graph', entity_role: '协议要求', mention_type: '法律依据'}]->(lp6);

MATCH (ep14:Episode {uuid: 'episode-mediation-003', graph_id: 'legal-knowledge-graph'})
MATCH (ma:Entity {type: 'MediationAgreement', graph_id: 'legal-knowledge-graph'})
MATCH (mo:Entity {type: 'CommercialMediationOrganization', graph_id: 'legal-knowledge-graph'})
MATCH (m:Entity {type: 'Mediator', graph_id: 'legal-knowledge-graph'})
MATCH (lp6:Entity {type: 'LegalProvision', provisionId: 'SMTL-022', graph_id: 'legal-knowledge-graph'})
MERGE (ep14)-[:MENTIONS {uuid: 'mentions-ep14-1', graph_id: 'legal-knowledge-graph', entity_role: '协议标的', mention_type: '直接涉及'}]->(ma)
MERGE (ep14)-[:MENTIONS {uuid: 'mentions-ep14-2', graph_id: 'legal-knowledge-graph', entity_role: '签章组织', mention_type: '直接涉及'}]->(mo)
MERGE (ep14)-[:MENTIONS {uuid: 'mentions-ep14-3', graph_id: 'legal-knowledge-graph', entity_role: '签署人', mention_type: '直接涉及'}]->(m)
MERGE (ep14)-[:MENTIONS {uuid: 'mentions-ep14-4', graph_id: 'legal-knowledge-graph', entity_role: '协议效力', mention_type: '法律依据'}]->(lp6);

// ============================================================
// 第五部分: 事件时序链 (NEXT_EPISODE)
// 按法律程序时间线连接 Episode 节点
// ============================================================

// 公司解散案件时序链
MATCH (ep1:Episode {uuid: 'episode-company-dissolution-001', graph_id: 'legal-knowledge-graph'})
MATCH (ep2:Episode {uuid: 'episode-company-dissolution-002', graph_id: 'legal-knowledge-graph'})
MERGE (ep1)-[:NEXT_EPISODE {
  uuid: 'next-episode-chain-001',
  graph_id: 'legal-knowledge-graph',
  sequence_order: 1,
  time_gap_days: 0,
  transition_description: '股东矛盾逐步激化'
}]->(ep2);

MATCH (ep2:Episode {uuid: 'episode-company-dissolution-002', graph_id: 'legal-knowledge-graph'})
MATCH (ep3:Episode {uuid: 'episode-company-dissolution-003', graph_id: 'legal-knowledge-graph'})
MERGE (ep2)-[:NEXT_EPISODE {
  uuid: 'next-episode-chain-002',
  graph_id: 'legal-knowledge-graph',
  sequence_order: 2,
  time_gap_days: 0,
  transition_description: '股东诉诸司法救济'
}]->(ep3);

MATCH (ep3:Episode {uuid: 'episode-company-dissolution-003', graph_id: 'legal-knowledge-graph'})
MATCH (ep4:Episode {uuid: 'episode-company-dissolution-004', graph_id: 'legal-knowledge-graph'})
MERGE (ep3)-[:NEXT_EPISODE {
  uuid: 'next-episode-chain-003',
  graph_id: 'legal-knowledge-graph',
  sequence_order: 3,
  time_gap_days: 150,
  transition_description: '一审庭审与证据交换'
}]->(ep4);

MATCH (ep4:Episode {uuid: 'episode-company-dissolution-004', graph_id: 'legal-knowledge-graph'})
MATCH (ep5:Episode {uuid: 'episode-company-dissolution-005', graph_id: 'legal-knowledge-graph'})
MERGE (ep4)-[:NEXT_EPISODE {
  uuid: 'next-episode-chain-004',
  graph_id: 'legal-knowledge-graph',
  sequence_order: 4,
  time_gap_days: 0,
  transition_description: '一审法院作出判决'
}]->(ep5);

MATCH (ep5:Episode {uuid: 'episode-company-dissolution-005', graph_id: 'legal-knowledge-graph'})
MATCH (ep6:Episode {uuid: 'episode-company-dissolution-006', graph_id: 'legal-knowledge-graph'})
MERGE (ep5)-[:NEXT_EPISODE {
  uuid: 'next-episode-chain-005',
  graph_id: 'legal-knowledge-graph',
  sequence_order: 5,
  time_gap_days: 60,
  transition_description: '原告不服一审判决提起上诉'
}]->(ep6);

MATCH (ep6:Episode {uuid: 'episode-company-dissolution-006', graph_id: 'legal-knowledge-graph'})
MATCH (ep7:Episode {uuid: 'episode-company-dissolution-007', graph_id: 'legal-knowledge-graph'})
MERGE (ep6)-[:NEXT_EPISODE {
  uuid: 'next-episode-chain-006',
  graph_id: 'legal-knowledge-graph',
  sequence_order: 6,
  time_gap_days: 90,
  transition_description: '二审法院开庭审理'
}]->(ep7);

MATCH (ep7:Episode {uuid: 'episode-company-dissolution-007', graph_id: 'legal-knowledge-graph'})
MATCH (ep8:Episode {uuid: 'episode-company-dissolution-008', graph_id: 'legal-knowledge-graph'})
MERGE (ep7)-[:NEXT_EPISODE {
  uuid: 'next-episode-chain-007',
  graph_id: 'legal-knowledge-graph',
  sequence_order: 7,
  time_gap_days: 0,
  transition_description: '二审法院作出终审判决'
}]->(ep8);

// 管辖权争议案件时序链
MATCH (ep9:Episode {uuid: 'episode-jurisdiction-001', graph_id: 'legal-knowledge-graph'})
MATCH (ep10:Episode {uuid: 'episode-jurisdiction-002', graph_id: 'legal-knowledge-graph'})
MERGE (ep9)-[:NEXT_EPISODE {
  uuid: 'next-episode-chain-008',
  graph_id: 'legal-knowledge-graph',
  sequence_order: 1,
  time_gap_days: 90,
  transition_description: '一审法院审查并移送案件'
}]->(ep10);

MATCH (ep10:Episode {uuid: 'episode-jurisdiction-002', graph_id: 'legal-knowledge-graph'})
MATCH (ep11:Episode {uuid: 'episode-jurisdiction-003', graph_id: 'legal-knowledge-graph'})
MERGE (ep10)-[:NEXT_EPISODE {
  uuid: 'next-episode-chain-009',
  graph_id: 'legal-knowledge-graph',
  sequence_order: 2,
  time_gap_days: 60,
  transition_description: '上级法院审查并撤销移送裁定'
}]->(ep11);

// 商事调解事件时序链
MATCH (ep12:Episode {uuid: 'episode-mediation-001', graph_id: 'legal-knowledge-graph'})
MATCH (ep13:Episode {uuid: 'episode-mediation-002', graph_id: 'legal-knowledge-graph'})
MERGE (ep12)-[:NEXT_EPISODE {
  uuid: 'next-episode-chain-010',
  graph_id: 'legal-knowledge-graph',
  sequence_order: 1,
  time_gap_days: 0,
  transition_description: '调解受理后启动调解程序'
}]->(ep13);

MATCH (ep13:Episode {uuid: 'episode-mediation-002', graph_id: 'legal-knowledge-graph'})
MATCH (ep14:Episode {uuid: 'episode-mediation-003', graph_id: 'legal-knowledge-graph'})
MERGE (ep13)-[:NEXT_EPISODE {
  uuid: 'next-episode-chain-011',
  graph_id: 'legal-knowledge-graph',
  sequence_order: 2,
  time_gap_days: 0,
  transition_description: '双方达成协议并签署'
}]->(ep14);

// ============================================================
// 第六部分: 法院节点 (Court) — type: 'Court'
// ============================================================

MERGE (c1:Entity {
  uuid: 'court-supreme-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'Court',
  courtName: '中华人民共和国最高人民法院',
  courtLevel: '最高人民法院',
  location: '北京市',
  jurisdiction: '全国范围内的重大案件、最高人民法院直接管辖的案件',
  metadata: '{"icon": "supreme", "color": "#B71C1C"}',
  created_at: datetime()
});

MERGE (c2:Entity {
  uuid: 'court-shanghai-high-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'Court',
  courtName: '上海市高级人民法院',
  courtLevel: '高级人民法院',
  location: '上海市',
  jurisdiction: '上海市辖区内的重大案件',
  parentCourt: '中华人民共和国最高人民法院',
  metadata: '{"icon": "high", "color": "#1565C0"}',
  created_at: datetime()
});

MERGE (c3:Entity {
  uuid: 'court-shanghai-1-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'Court',
  courtName: '上海市第一中级人民法院',
  courtLevel: '中级人民法院',
  location: '上海市',
  jurisdiction: '上海市第一中级人民法院管辖范围内的案件',
  parentCourt: '上海市高级人民法院',
  metadata: '{"icon": "intermediate", "color": "#0D47A1"}',
  created_at: datetime()
});

MERGE (c4:Entity {
  uuid: 'court-shanghai-changning-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'Court',
  courtName: '上海市长宁区人民法院',
  courtLevel: '基层人民法院',
  location: '上海市长宁区',
  jurisdiction: '上海市长宁区管辖范围内的第一审案件',
  parentCourt: '上海市第一中级人民法院',
  metadata: '{"icon": "grassroots", "color": "#1976D2"}',
  created_at: datetime()
});

MERGE (c5:Entity {
  uuid: 'court-chongqing-high-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'Court',
  courtName: '重庆市高级人民法院',
  courtLevel: '高级人民法院',
  location: '重庆市',
  jurisdiction: '重庆市范围内的重大案件',
  parentCourt: '中华人民共和国最高人民法院',
  metadata: '{"icon": "high", "color": "#6A1B9A"}',
  created_at: datetime()
});

MERGE (c6:Entity {
  uuid: 'court-chongqing-nanan-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'Court',
  courtName: '重庆市南岸区人民法院',
  courtLevel: '基层人民法院',
  location: '重庆市南岸区',
  jurisdiction: '重庆市南岸区管辖范围内的第一审案件',
  parentCourt: '重庆市第五中级人民法院',
  metadata: '{"icon": "grassroots", "color": "#7B1FA2"}',
  created_at: datetime()
});

MERGE (c7:Entity {
  uuid: 'court-chongqing-jiangbei-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'Court',
  courtName: '重庆市江北区人民法院',
  courtLevel: '基层人民法院',
  location: '重庆市江北区',
  jurisdiction: '重庆市江北区管辖范围内的第一审案件',
  parentCourt: '重庆市第五中级人民法院',
  metadata: '{"icon": "grassroots", "color": "#8E24AA"}',
  created_at: datetime()
});


// ============================================================
// 第三部分: 法律条文节点 (LegalProvision) — type: 'LegalProvision'
// ============================================================

MERGE (lp1:Entity {
  uuid: 'prov-shangshi-tiao1',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'LegalProvision',
  provisionId: 'SMTL-001',
  articleNumber: '第1条',
  provisionContent: '为了规范商事调解活动，有效解决商事争议，保护当事人合法权益，促进商事调解行业发展，优化营商环境，制定本条例。',
  lawName: '商事调解条例',
  lawType: '行政法规',
  effectiveDate: date('2026-05-01'),
  keywords: '商事调解,立法目的,营商环境',
  metadata: '{"source": "国务院令第827号", "category": "商事调解"}',
  created_at: datetime()
});

MERGE (lp2:Entity {
  uuid: 'prov-shangshi-tiao2',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'LegalProvision',
  provisionId: 'SMTL-002',
  articleNumber: '第2条',
  provisionContent: '本条例所称商事调解活动，是指在商事调解组织主持下，当事人自愿友好协商解决贸易、投资、金融、运输、房地产、工程建设、知识产权等领域商事争议的活动。婚姻家庭、继承、监护、劳动人事、消费者权益争议以及依法应当以其他方式解决的争议，不适用商事调解。',
  lawName: '商事调解条例',
  lawType: '行政法规',
  effectiveDate: date('2026-05-01'),
  keywords: '商事调解定义,适用范围,除外情形',
  metadata: '{"source": "国务院令第827号", "category": "商事调解"}',
  created_at: datetime()
});

MERGE (lp3:Entity {
  uuid: 'prov-shangshi-tiao8',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'LegalProvision',
  provisionId: 'SMTL-008',
  articleNumber: '第8条',
  provisionContent: '设立商事调解组织，应当符合下列条件：（一）发起人为非营利法人；（二）有规范的名称，名称中含有"商事调解"字样；（三）有自己的住所和章程；（四）有30万元以上的资产；（五）有5名以上商事调解员和适当数量的专职工作人员。',
  lawName: '商事调解条例',
  lawType: '行政法规',
  effectiveDate: date('2026-05-01'),
  keywords: '商事调解组织设立条件,非营利法人,注册资本',
  metadata: '{"source": "国务院令第827号", "category": "商事调解"}',
  created_at: datetime()
});

MERGE (lp4:Entity {
  uuid: 'prov-shangshi-tiao12',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'LegalProvision',
  provisionId: 'SMTL-012',
  articleNumber: '第12条',
  provisionContent: '商事调解组织聘任的商事调解员应当公道正派，具备良好的专业素质。商事调解员应当符合下列条件之一：（一）通过国家统一法律职业资格考试取得法律职业资格，从事调解工作满3年；（二）从事律师、仲裁、公证工作满3年或者曾任法官、检察官满3年；（三）具有法律、经济、科学技术等相关专业知识，从事法律、经济贸易等专业工作，并具有中级以上职称或者具有同等专业水平；（四）本条例施行前已从事商事调解工作满3年，并具有大学本科以上学历。',
  lawName: '商事调解条例',
  lawType: '行政法规',
  effectiveDate: date('2026-05-01'),
  keywords: '调解员资质,法律职业资格,专业素质',
  metadata: '{"source": "国务院令第827号", "category": "商事调解"}',
  created_at: datetime()
});

MERGE (lp5:Entity {
  uuid: 'prov-shangshi-tiao14',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'LegalProvision',
  provisionId: 'SMTL-014',
  articleNumber: '第14条',
  provisionContent: '商事调解活动应当遵循自愿、合法、诚信、保密的原则。',
  lawName: '商事调解条例',
  lawType: '行政法规',
  effectiveDate: date('2026-05-01'),
  keywords: '调解原则,自愿,合法,诚信,保密',
  metadata: '{"source": "国务院令第827号", "category": "商事调解"}',
  created_at: datetime()
});

MERGE (lp6:Entity {
  uuid: 'prov-shangshi-tiao22',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'LegalProvision',
  provisionId: 'SMTL-022',
  articleNumber: '第22条',
  provisionContent: '经商事调解达成协议的，除当事人另有约定外，应当制作商事调解协议，载明主要事实、争议事项和当事人达成协议的主要内容、履行方式与期限等。商事调解员应当在商事调解协议上签名并加盖商事调解组织的印章。商事调解协议的内容不得损害国家利益、社会公共利益和他人合法权益，不得违反法律、行政法规的强制性规定，不得违背公序良俗。商事调解协议具有法律约束力，当事人应当履行。',
  lawName: '商事调解条例',
  lawType: '行政法规',
  effectiveDate: date('2026-05-01'),
  keywords: '调解协议,效力,履行,限制',
  metadata: '{"source": "国务院令第827号", "category": "商事调解"}',
  created_at: datetime()
});

MERGE (lp7:Entity {
  uuid: 'prov-minfadian-69',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'LegalProvision',
  provisionId: 'MFD-069',
  articleNumber: '第69条',
  provisionContent: '法人解散的，除合并或者分立的情形外，清算义务人应当及时组成清算组进行清算。法人的董事、理事等执行机构或者决策机构的成员为清算义务人。法律、行政法规另有规定的，依照其规定。清算义务人未及时履行清算义务，造成损害的，应当承担民事责任；主管机关或者利害关系人可以申请人民法院指定有关人员组成清算组进行清算。',
  lawName: '中华人民共和国民法典',
  lawType: '法律',
  effectiveDate: date('2021-01-01'),
  keywords: '法人解散,清算,清算义务人,民事责任',
  metadata: '{"source": "中华人民共和国主席令第45号", "category": "民商事"}',
  created_at: datetime()
});

MERGE (lp8:Entity {
  uuid: 'prov-minfadian-70',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'LegalProvision',
  provisionId: 'MFD-070',
  articleNumber: '第70条',
  provisionContent: '法人解散后的清算程序和清算组职权，依照有关法律规定。没有法律规定或者当事人没有约定的，可以参照适用民法典的有关规定。',
  lawName: '中华人民共和国民法典',
  lawType: '法律',
  effectiveDate: date('2021-01-01'),
  keywords: '法人清算,程序,清算组职权',
  metadata: '{"source": "中华人民共和国主席令第45号", "category": "民商事"}',
  created_at: datetime()
});

MERGE (lp9:Entity {
  uuid: 'prov-gongsifa-shi2-tiao1',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'LegalProvision',
  provisionId: 'GSF2-001',
  articleNumber: '第1条',
  provisionContent: '单独或者合计持有公司全部股东表决权百分之十以上的股东，以下列事由之一提起解散公司诉讼，并符合公司法第一百八十二条规定的，人民法院应当受理：（一）公司持续两年以上无法召开股东会或者股东大会，公司经营管理发生严重困难的；（二）股东表决时无法达到法定或者公司章程规定的比例，持续两年以上不能做出有效的股东会或者股东大会决议，公司经营管理发生严重困难的；（三）公司董事长期冲突，且无法通过股东会或者股东大会解决，公司经营管理发生严重困难的；（四）经营管理发生其他严重困难，公司继续存续会使股东利益受到重大损失的情形。',
  lawName: '最高人民法院关于适用《中华人民共和国公司法》若干问题的规定（二）',
  lawType: '司法解释',
  effectiveDate: date('2021-01-01'),
  keywords: '公司解散诉讼,股东表决权,经营管理困难,公司僵局',
  metadata: '{"source": "法释〔2008〕6号（2020年修正）", "category": "公司法"}',
  created_at: datetime()
});

MERGE (lp10:Entity {
  uuid: 'prov-minshisusongfa-22',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'LegalProvision',
  provisionId: 'MSSSF-022',
  articleNumber: '第22条',
  provisionContent: '对公民提起的民事诉讼，由被告住所地人民法院管辖；被告住所地与经常居住地不一致的，由经常居住地人民法院管辖。',
  lawName: '中华人民共和国民事诉讼法',
  lawType: '法律',
  effectiveDate: date('2021-01-01'),
  keywords: '管辖,被告住所地,经常居住地',
  metadata: '{"source": "2023年修正", "category": "民事诉讼"}',
  created_at: datetime()
});

MERGE (lp11:Entity {
  uuid: 'prov-minshisusongfa-23',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'LegalProvision',
  provisionId: 'MSSSF-023',
  articleNumber: '第23条',
  provisionContent: '下列民事诉讼，由原告住所地人民法院管辖；原告住所地与经常居住地不一致的，由原告经常居住地人民法院管辖：（一）对不在中华人民共和国领域内居住的人提起的有关身份关系的诉讼；（二）对下落不明或者宣告失踪的人提起的有关身份关系的诉讼；（三）对被采取强制性教育措施的人提起的诉讼；（四）对被监禁的人提起的诉讼。',
  lawName: '中华人民共和国民事诉讼法',
  lawType: '法律',
  effectiveDate: date('2021-01-01'),
  keywords: '管辖,原告住所地,被监禁,特殊情形',
  metadata: '{"source": "2023年修正", "category": "民事诉讼"}',
  created_at: datetime()
});

MERGE (lp12:Entity {
  uuid: 'prov-minshisusongfa-24',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'LegalProvision',
  provisionId: 'MSSSF-024',
  articleNumber: '第24条',
  provisionContent: '因合同纠纷提起的诉讼，由被告住所地或者合同履行地人民法院管辖。',
  lawName: '中华人民共和国民事诉讼法',
  lawType: '法律',
  effectiveDate: date('2021-01-01'),
  keywords: '合同纠纷,管辖,被告住所地,合同履行地',
  metadata: '{"source": "2023年修正", "category": "民事诉讼"}',
  created_at: datetime()
});


// ============================================================
// 第四部分: 案件节点 (Case) — type: 'Case'
// 子类型通过 disputeType 属性区分（如 disputeType: '公司解散', '合同纠纷' 等）
// ============================================================

MERGE (ca1:Entity {
  uuid: 'case-xj-company-dissolution-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'Case',
  caseNumber: '（2023）沪01民终11293号',
  caseName: '徐某骥诉上海某物业管理有限公司等公司解散纠纷案',
  caseType: '民事',
  caseStatus: '结案',
  filingDate: date('2022-01-01'),
  closedDate: date('2023-10-24'),
  amountInDispute: 0,
  caseSummary: '原告作为被告公司持股39.54%的股东，因公司经营管理僵局诉请解散公司。法院认定公司运营良好、股东矛盾可通过其他途径解决，判决驳回解散诉请。',
  disputeType: '公司解散',
  mediationAttempted: false,
  courtLevel: '中级人民法院',
  originalCaseNumber: '（2022）沪0105民初21387号',
  globalCaseNum: '2025-08-2-283-001',
  source: '人民法院案例库',
  crawlingDate: '2025-11-07',
  url: 'https://rmfyalk.court.gov.cn/view/content.html?id=iJabnFfgJmNRHEI4TPDmHOU6Syvay+hVbocBjlk58zU=',
  metadata: '{"caseIndex": 10, "pageNum": 10, "apiData": {"caseSortName": "民事", "sortName": "公司解散纠纷", "courtName": "上海市第一中级人民法院"}}',
  created_at: datetime()
});

MERGE (ca2:Entity {
  uuid: 'case-tan-creditor-revocation-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'Case',
  caseNumber: '（2020）渝民辖188号',
  caseName: '谭某诉吴某、雒某债权人撤销权纠纷案',
  caseType: '民事',
  caseStatus: '结案',
  filingDate: date('2020-06-08'),
  closedDate: date('2020-12-28'),
  amountInDispute: 0,
  caseSummary: '债权人谭某诉请撤销债务人吴某与雒某离婚时分割房屋的行为。案件经历管辖权异议移送，最终由重庆市高级人民法院指定管辖回原法院。核心争点：共同被告中仅部分被告被监禁时，是否适用原告住所地管辖。',
  courtLevel: '高级人民法院',
  disputeType: '债权人撤销权',
  mediationAttempted: false,
  globalCaseNum: '2025-01-2-078-001',
  source: '人民法院案例库',
  crawlingDate: '2025-11-07',
  url: 'https://rmfyalk.court.gov.cn/view/content.html?id=1XcOaaovBlmK500dPh8CVj2zfisBUPFvvMAeqeqFCWQ=',
  metadata: '{"caseIndex": 4, "pageNum": 10, "apiData": {"caseSortName": "民事", "sortName": "债权人撤销权纠纷", "courtName": "重庆市高级人民法院"}}',
  created_at: datetime()
});

MERGE (ca_demo:Entity {
  uuid: 'case-demo-mediation-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'Case',
  caseNumber: '（2024）沪0115商初1234号',
  caseName: '某科技公司诉某投资公司技术合同纠纷案',
  caseType: '民事',
  caseStatus: '调解中',
  filingDate: date('2024-03-01'),
  amountInDispute: 1500000,
  caseSummary: '技术合同款项支付争议，经上海国际商事调解中心调解，双方达成和解协议。',
  disputeType: '合同纠纷',
  mediationAttempted: true,
  courtLevel: '基层人民法院',
  metadata: '{"mediationOrg": "上海国际商事调解中心", "mediationDate": "2024-06-15"}',
  created_at: datetime()
});


// ============================================================
// 第五部分: 当事人节点 (Party) — type: 'Party'
// :LegalPerson 子标签 → subType: 'LegalPerson' 属性
// ============================================================

MERGE (p1:Entity {
  uuid: 'party-xu-jiji-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'Party',
  partyName: '徐某骥',
  partyType: '自然人',
  partyRole: '原告/上诉人',
  isEnterprise: false,
  metadata: '{"roleType": "股东", "shareholdingRatio": "39.54%"}',
  created_at: datetime()
});

MERGE (p2:Entity {
  uuid: 'party-shanghai-property-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'Party',
  subType: 'LegalPerson',
  partyName: '上海某物业管理有限公司',
  partyType: '法人',
  partyRole: '被告',
  unifiedSocialCreditCode: '91310000MA1FXXXXX',
  address: '上海市',
  isEnterprise: true,
  metadata: '{"companyType": "有限责任公司", "businessScope": "物业管理"}',
  created_at: datetime()
});

MERGE (p3:Entity {
  uuid: 'party-sun-jin-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'Party',
  partyName: '孙某瑾',
  partyType: '自然人',
  partyRole: '第三人',
  isEnterprise: false,
  metadata: '{"roleType": "公司董事长兼总经理", "shareholdingRatio": "38.37%"}',
  created_at: datetime()
});

MERGE (p4:Entity {
  uuid: 'party-zhang-zhizi-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'Party',
  partyName: '张某之',
  partyType: '自然人',
  partyRole: '第三人',
  isEnterprise: false,
  metadata: '{"roleType": "公司股东", "shareholdingRatio": "22.09%"}',
  created_at: datetime()
});

MERGE (p5:Entity {
  uuid: 'party-tan-moumou-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'Party',
  partyName: '谭某',
  partyType: '自然人',
  partyRole: '原告',
  isEnterprise: false,
  metadata: '{"roleType": "债权人"}',
  created_at: datetime()
});

MERGE (p6:Entity {
  uuid: 'party-wu-moumou-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'Party',
  partyName: '吴某',
  partyType: '自然人',
  partyRole: '被告',
  isEnterprise: false,
  metadata: '{"roleType": "债务人", "note": "起诉时被羁押于重庆市永川监狱"}',
  created_at: datetime()
});

MERGE (p7:Entity {
  uuid: 'party-luo-moumou-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'Party',
  partyName: '雒某',
  partyType: '自然人',
  partyRole: '被告',
  address: '重庆市九龙坡区（户籍）/ 重庆市南岸区（经常居住地）',
  isEnterprise: false,
  metadata: '{"roleType": "债务人前配偶"}',
  created_at: datetime()
});


// ============================================================
// 第六部分: 裁判文书节点 (JudgmentDocument) — type: 'JudgmentDocument'
// ============================================================

MERGE (jd1:Entity {
  uuid: 'judgment-c1-first-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'JudgmentDocument',
  documentNumber: '（2022）沪0105民初21387号',
  documentType: '民事判决书',
  issueDate: date('2023-05-04'),
  judgmentResult: '驳回原告徐某骥的全部诉讼请求',
  legalBasis: '《中华人民共和国民法典》第69条，《最高人民法院关于适用〈中华人民共和国公司法〉若干问题的规定（二）》第1条',
  mainContent: '原告徐某骥作为被告公司持股39.54%的股东，以公司经营管理发生严重困难、继续存续会使股东利益遭受重大损失为由，请求解散公司上海某物业管理有限公司。法院认为：（1）原告与第三人就公司经营、资产处理等有过协商并达成一致；（2）被告公司尚在经营并处于盈利状态；（3）原告可通过转让股权等途径解决。',
  courtName: '上海市长宁区人民法院',
  metadata: '{"caseLevel": "一审", "decisionType": "驳回诉请"}',
  created_at: datetime()
});

MERGE (jd2:Entity {
  uuid: 'judgment-c1-second-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'JudgmentDocument',
  documentNumber: '（2023）沪01民终11293号',
  documentType: '民事判决书',
  issueDate: date('2023-10-24'),
  judgmentResult: '驳回上诉，维持原判',
  legalBasis: '《中华人民共和国民法典》第69条，《最高人民法院关于适用〈中华人民共和国公司法〉若干问题的规定（二）》第1条',
  mainContent: '上诉人徐某骥不服上海市长宁区人民法院（2022）沪0105民初21387号民事判决，提起上诉。上海市第一中级人民法院经审理认为，原审法院认定事实清楚，适用法律正确，判决驳回上诉，维持原判。',
  courtName: '上海市第一中级人民法院',
  metadata: '{"caseLevel": "二审", "decisionType": "维持原判"}',
  created_at: datetime()
});

MERGE (jd3:Entity {
  uuid: 'judgment-c2-jurisdiction-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'JudgmentDocument',
  documentNumber: '（2020）渝民辖188号',
  documentType: '民事裁定书',
  issueDate: date('2020-12-28'),
  judgmentResult: '一、撤销重庆市南岸区人民法院（2020）渝0108民初637号民事裁定；二、本案由重庆市南岸区人民法院审理。',
  legalBasis: '《中华人民共和国民事诉讼法》第22条、第23条',
  mainContent: '重庆市南岸区人民法院认为被告吴某被监禁应移送原告住所地管辖，移送至重庆市江北区人民法院。重庆市高级人民法院认为共同被告中仅部分被告被监禁不适用民事诉讼法第23条关于原告住所地管辖的规定，撤销一审裁定，指定由重庆市南岸区人民法院审理。',
  courtName: '重庆市高级人民法院',
  metadata: '{"caseLevel": "指定管辖", "decisionType": "撤销移送"}',
  created_at: datetime()
});


// ============================================================
// 第七部分: 法官节点 (Judge) — type: 'Judge'
// ============================================================

MERGE (j1:Entity {
  uuid: 'judge-c1-first-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'Judge',
  judgeName: '某法官（未公开）',
  judgeTitle: '审判长',
  courtName: '上海市长宁区人民法院',
  specialty: '民商事审判',
  metadata: '{"caseNumber": "（2022）沪0105民初21387号"}',
  created_at: datetime()
});

MERGE (j2:Entity {
  uuid: 'judge-c1-second-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'Judge',
  judgeName: '某法官（未公开）',
  judgeTitle: '审判长',
  courtName: '上海市第一中级人民法院',
  specialty: '民商事上诉审判',
  metadata: '{"caseNumber": "（2023）沪01民终11293号"}',
  created_at: datetime()
});

MERGE (j3:Entity {
  uuid: 'judge-c2-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'Judge',
  judgeName: '某法官（未公开）',
  judgeTitle: '审判长',
  courtName: '重庆市高级人民法院',
  specialty: '管辖权审判',
  metadata: '{"caseNumber": "（2020）渝民辖188号"}',
  created_at: datetime()
});


// ============================================================
// 第八部分: 证据节点 (Evidence) — type: 'Evidence'
// ============================================================

MERGE (e1:Entity {
  uuid: 'evidence-c1-share-transfer-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'Evidence',
  evidenceNumber: '证据001',
  evidenceType: '书证',
  content: '股权转让协议（2020年3月30日签署），证明原告受让被告公司五位股东持有的全部股权',
  submittedBy: '原告',
  purpose: '证明原告合法取得被告公司股东资格及持股比例',
  admissibility: '采纳',
  metadata: '{}',
  created_at: datetime()
});

MERGE (e2:Entity {
  uuid: 'evidence-c1-wechat-record-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'Evidence',
  evidenceNumber: '证据002',
  evidenceType: '电子数据',
  content: '微信聊天记录（2022年6月），记录原告与第三人孙某瑾就财务章、分红、办公室等事项的沟通',
  submittedBy: '原告',
  submissionDate: date('2022-06-24'),
  purpose: '证明原告曾要求第三人召开股东会解决纠纷',
  admissibility: '采纳',
  metadata: '{}',
  created_at: datetime()
});

MERGE (e3:Entity {
  uuid: 'evidence-c1-meeting-minutes-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'Evidence',
  evidenceNumber: '证据003',
  evidenceType: '书证',
  content: '被告公司股东会决议及会议纪要（2020年4月至2022年1月），证明被告多次召开股东会并正常运营',
  submittedBy: '被告',
  purpose: '证明被告公司正常经营，原告多次获得分红',
  admissibility: '采纳',
  metadata: '{}',
  created_at: datetime()
});

MERGE (e4:Entity {
  uuid: 'evidence-c1-profit-record-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'Evidence',
  evidenceNumber: '证据004',
  evidenceType: '书证',
  content: '银行转账记录，证明被告公司向原告累计支付约1223070元分红款项',
  submittedBy: '被告',
  purpose: '证明原告已获得分红，不存在不分红的公司僵局',
  admissibility: '采纳',
  metadata: '{}',
  created_at: datetime()
});


// ============================================================
// 第九部分: 商事调解相关节点
// ============================================================

MERGE (mo:Entity {
  uuid: 'mediation-org-shanghai-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'CommercialMediationOrganization',
  name: '上海国际商事调解中心',
  orgType: '商事调解组织',
  location: '上海市',
  licenseNumber: '沪商调证字2024001号',
  establishedDate: date('2024-01-01'),
  assetAmount: 500000,
  mediatorCount: 15,
  metadata: '{"supervisingAuthority": "上海市司法局", "international": false}',
  created_at: datetime()
});

MERGE (m:Entity {
  uuid: 'mediator-li-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'Mediator',
  name: '李某',
  qualification: '法律职业资格+5年调解经验',
  licenseNumber: 'MD2024001',
  organizationName: '上海国际商事调解中心',
  specialty: '公司法务、合同纠纷',
  yearsExperience: 5,
  metadata: '{}',
  created_at: datetime()
});

MERGE (ma:Entity {
  uuid: 'agreement-ma-demo-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'MediationAgreement',
  agreementNumber: 'MA20240001',
  mainFacts: '某科技公司与某投资公司就技术合同款项支付产生争议，经上海国际商事调解中心主持调解',
  disputeItems: '技术合同款项支付、项目验收标准',
  agreementContent: '被申请人于2024年12月31日前分期支付合同款项共计人民币150万元',
  performanceMethod: '银行转账',
  performanceDeadline: date('2024-12-31'),
  signDate: date('2024-06-15'),
  judiciallyConfirmed: true,
  judiciallyConfirmDate: date('2024-06-20'),
  judiciallyConfirmCourt: '上海市浦东新区人民法院',
  judiciallyConfirmNumber: '（2024）沪0115民调确字第1234号',
  metadata: '{"fees": "双方各承担50%", "confidentiality": "调解过程保密"}',
  created_at: datetime()
});


// ============================================================
// 第十部分: 案件事实与裁判要旨
// ============================================================

MERGE (f1:Entity {
  uuid: 'fact-c1-shareholder-dispute-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'CaseFact',
  factDescription: '2020年3月30日，原告受让被告五位股东持有的股权，成为上海某物业管理有限公司股东，持股39.54%',
  factCategory: '股权转让',
  factImportance: 'high',
  created_at: datetime()
});

MERGE (f2:Entity {
  uuid: 'fact-c1-company-profitable-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'CaseFact',
  factDescription: '被告公司正常经营，自2020年4月至2022年1月多次召开股东会协商分红、办公场地等事宜，原告累计获得利润分配约1223070元',
  factCategory: '公司经营状况',
  factImportance: 'high',
  created_at: datetime()
});

MERGE (f3:Entity {
  uuid: 'fact-c1-shareholder-dispute-002',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'CaseFact',
  factDescription: '原告称第三人孙某瑾把持公司、拒绝分红、拒绝提供办公室，双方就公司经营产生纠纷',
  factCategory: '股东矛盾',
  factImportance: 'medium',
  created_at: datetime()
});

MERGE (r1:Entity {
  uuid: 'reasoning-c1-dissolution-standard-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'CaseReasoning',
  reasoning: '公司解散纠纷是股东在穷尽公司自治或其他途径，均不能解决公司僵局状况下的救济途径。公司经营管理是否发生困难、是否已经存在公司僵局是判断公司应否解散的重要标准。实践中，应当审慎适用公司解散这一使企业退出市场的救济途径。对于公司运营良好，自我调整机制未失灵，不存在公司存续会使股东利益受到重大损害情形的，不能认定公司经营管理发生困难或存在公司僵局，不应当判令解散公司。',
  guidanceLevel: '参考',
  keywords: '公司解散,公司僵局,判断标准,审慎适用,司法救济',
  applicableScenario: '股东诉请解散公司时，公司运营良好且股东矛盾可通过其他途径解决的',
  created_at: datetime()
});

MERGE (r2:Entity {
  uuid: 'reasoning-c2-jurisdiction-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'CaseReasoning',
  reasoning: '共同被告中仅有部分被告被监禁的民事诉讼，不属于《中华人民共和国民事诉讼法》第二十三条规定的"对被监禁的人提起诉讼由原告住所地人民法院管辖"情形，应当按照法律的其他管辖规定确定管辖法院。',
  guidanceLevel: '参考',
  keywords: '管辖权,被监禁被告,共同被告,原告住所地,债权人撤销权',
  applicableScenario: '债权人撤销权诉讼中，共同被告之一被监禁但另一被告未监禁的情形',
  created_at: datetime()
});


// ============================================================
// 第十一部分: 关系创建
// 所有 MATCH 模式已更新为 :Entity + type 属性
// ============================================================

// 法条关联关系
MATCH (a:Entity {type: 'LegalProvision', provisionId: 'SMTL-001', graph_id: 'legal-knowledge-graph'})
MATCH (b:Entity {type: 'LegalProvision', provisionId: 'SMTL-002', graph_id: 'legal-knowledge-graph'})
MERGE (a)-[r:LEGAL_PROVISION_RELATED {
  uuid: 'prov-rel-smtl-1-2',
  graph_id: 'legal-knowledge-graph',
  relationType: '配套',
  description: '第1条定义立法目的，第2条界定适用范围'
}]->(b);

MATCH (a:Entity {type: 'LegalProvision', provisionId: 'SMTL-012', graph_id: 'legal-knowledge-graph'})
MATCH (b:Entity {type: 'LegalProvision', provisionId: 'SMTL-008', graph_id: 'legal-knowledge-graph'})
MERGE (a)-[r:LEGAL_PROVISION_RELATED {
  uuid: 'prov-rel-smtl-12-8',
  graph_id: 'legal-knowledge-graph',
  relationType: '配套',
  description: '调解员资格条件与调解组织设立条件的配套规定'
}]->(b);

MATCH (a:Entity {type: 'LegalProvision', provisionId: 'SMTL-022', graph_id: 'legal-knowledge-graph'})
MATCH (b:Entity {type: 'LegalProvision', provisionId: 'SMTL-014', graph_id: 'legal-knowledge-graph'})
MERGE (a)-[r:LEGAL_PROVISION_RELATED {
  uuid: 'prov-rel-smtl-22-14',
  graph_id: 'legal-knowledge-graph',
  relationType: '配套',
  description: '调解协议效力与调解原则的关联'
}]->(b);

MATCH (a:Entity {type: 'LegalProvision', provisionId: 'MFD-069', graph_id: 'legal-knowledge-graph'})
MATCH (b:Entity {type: 'LegalProvision', provisionId: 'GSF2-001', graph_id: 'legal-knowledge-graph'})
MERGE (a)-[r:LEGAL_PROVISION_RELATED {
  uuid: 'prov-rel-mfd-gsf2',
  graph_id: 'legal-knowledge-graph',
  relationType: '配套',
  description: '民法典第69条规定法人解散一般规则，公司法解释（二）第1条规定公司解散诉讼的具体情形'
}]->(b);

MATCH (a:Entity {type: 'LegalProvision', provisionId: 'MSSSF-023', graph_id: 'legal-knowledge-graph'})
MATCH (b:Entity {type: 'LegalProvision', provisionId: 'MSSSF-022', graph_id: 'legal-knowledge-graph'})
MERGE (a)-[r:LEGAL_PROVISION_RELATED {
  uuid: 'prov-rel-msssf-23-22',
  graph_id: 'legal-knowledge-graph',
  relationType: '补充',
  description: '第23条是对第22条一般管辖规定的特别补充'
}]->(b);

// 案件-当事人关系
MATCH (ca:Entity {type: 'Case', caseNumber: '（2023）沪01民终11293号', graph_id: 'legal-knowledge-graph'})
MATCH (p:Entity {type: 'Party', partyName: '徐某骥', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_PARTY {
  uuid: 'rel-c1-p1-party',
  graph_id: 'legal-knowledge-graph',
  role: '上诉人',
  representationType: '本人',
  caseLevel: '二审',
  fact: '原告持股39.54%诉请解散公司'
}]->(p);

MATCH (ca:Entity {type: 'Case', caseNumber: '（2023）沪01民终11293号', graph_id: 'legal-knowledge-graph'})
MATCH (p:Entity {type: 'Party', partyName: '上海某物业管理有限公司', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_PARTY {
  uuid: 'rel-c1-p2-party',
  graph_id: 'legal-knowledge-graph',
  role: '被上诉人',
  representationType: '委托代理',
  caseLevel: '二审',
  fact: '被告公司经营管理权由第三人孙某瑾行使'
}]->(p);

MATCH (ca:Entity {type: 'Case', caseNumber: '（2023）沪01民终11293号', graph_id: 'legal-knowledge-graph'})
MATCH (p:Entity {type: 'Party', partyName: '孙某瑾', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_PARTY {
  uuid: 'rel-c1-p3-party',
  graph_id: 'legal-knowledge-graph',
  role: '第三人',
  representationType: '本人',
  caseLevel: '二审',
  fact: '被告公司董事长兼总经理，第三人孙某瑾持股38.37%'
}]->(p);

MATCH (ca:Entity {type: 'Case', caseNumber: '（2023）沪01民终11293号', graph_id: 'legal-knowledge-graph'})
MATCH (p:Entity {type: 'Party', partyName: '张某之', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_PARTY {
  uuid: 'rel-c1-p4-party',
  graph_id: 'legal-knowledge-graph',
  role: '第三人',
  representationType: '本人',
  caseLevel: '二审',
  fact: '被告公司股东，持股22.09%'
}]->(p);

MATCH (ca:Entity {type: 'Case', caseNumber: '（2020）渝民辖188号', graph_id: 'legal-knowledge-graph'})
MATCH (p:Entity {type: 'Party', partyName: '谭某', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_PARTY {
  uuid: 'rel-c2-p5-party',
  graph_id: 'legal-knowledge-graph',
  role: '原告',
  representationType: '委托代理',
  fact: '债权人，主张撤销债务人转让财产的行为'
}]->(p);

MATCH (ca:Entity {type: 'Case', caseNumber: '（2020）渝民辖188号', graph_id: 'legal-knowledge-graph'})
MATCH (p:Entity {type: 'Party', partyName: '吴某', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_PARTY {
  uuid: 'rel-c2-p6-party',
  graph_id: 'legal-knowledge-graph',
  role: '被告',
  representationType: '法定代理',
  fact: '债务人，起诉时被羁押于重庆市永川监狱'
}]->(p);

MATCH (ca:Entity {type: 'Case', caseNumber: '（2020）渝民辖188号', graph_id: 'legal-knowledge-graph'})
MATCH (p:Entity {type: 'Party', partyName: '雒某', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_PARTY {
  uuid: 'rel-c2-p7-party',
  graph_id: 'legal-knowledge-graph',
  role: '被告',
  representationType: '委托代理',
  fact: '债务人前配偶，接收了离婚时分得的房屋'
}]->(p);

// 案件-法院关系
MATCH (ca:Entity {type: 'Case', caseNumber: '（2023）沪01民终11293号', graph_id: 'legal-knowledge-graph'})
MATCH (c:Entity {type: 'Court', courtName: '上海市第一中级人民法院', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_COURT {
  uuid: 'rel-c1-court-second',
  graph_id: 'legal-knowledge-graph',
  courtRole: '二审法院',
  jurisdictionBasis: '上诉案件管辖',
  fact: '二审法院，驳回上诉，维持原判'
}]->(c);

MATCH (ca:Entity {type: 'Case', caseNumber: '（2023）沪01民终11293号', graph_id: 'legal-knowledge-graph'})
MATCH (c:Entity {type: 'Court', courtName: '上海市长宁区人民法院', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_COURT {
  uuid: 'rel-c1-court-first',
  graph_id: 'legal-knowledge-graph',
  courtRole: '一审法院',
  jurisdictionBasis: '基层人民法院一审管辖',
  fact: '一审法院，驳回原告诉讼请求'
}]->(c);

MATCH (ca:Entity {type: 'Case', caseNumber: '（2020）渝民辖188号', graph_id: 'legal-knowledge-graph'})
MATCH (c:Entity {type: 'Court', courtName: '重庆市高级人民法院', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_COURT {
  uuid: 'rel-c2-court-supreme-cq',
  graph_id: 'legal-knowledge-graph',
  courtRole: '指定管辖法院',
  jurisdictionBasis: '管辖权争议指定管辖',
  fact: '重庆市高级人民法院，撤销一审裁定，指定由南岸区法院审理'
}]->(c);

// 案件-法官关系
MATCH (ca:Entity {type: 'Case', caseNumber: '（2023）沪01民终11293号', graph_id: 'legal-knowledge-graph'})
MATCH (j:Entity {type: 'Judge', courtName: '上海市第一中级人民法院', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_JUDGE {
  uuid: 'rel-c1-judge-second',
  graph_id: 'legal-knowledge-graph',
  role: '审判长',
  caseLevel: '二审'
}]->(j);

MATCH (ca:Entity {type: 'Case', caseNumber: '（2023）沪01民终11293号', graph_id: 'legal-knowledge-graph'})
MATCH (j:Entity {type: 'Judge', courtName: '上海市长宁区人民法院', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_JUDGE {
  uuid: 'rel-c1-judge-first',
  graph_id: 'legal-knowledge-graph',
  role: '审判长',
  caseLevel: '一审'
}]->(j);

MATCH (ca:Entity {type: 'Case', caseNumber: '（2020）渝民辖188号', graph_id: 'legal-knowledge-graph'})
MATCH (j:Entity {type: 'Judge', courtName: '重庆市高级人民法院', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_JUDGE {
  uuid: 'rel-c2-judge',
  graph_id: 'legal-knowledge-graph',
  role: '审判长',
  caseLevel: '管辖权审查'
}]->(j);

// 案件-法律条文关系
MATCH (ca:Entity {type: 'Case', caseNumber: '（2023）沪01民终11293号', graph_id: 'legal-knowledge-graph'})
MATCH (lp:Entity {type: 'LegalProvision', articleNumber: '第69条', lawName: '中华人民共和国民法典', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_LEGAL_PROVISION {
  uuid: 'rel-c1-prov-mfd69',
  graph_id: 'legal-knowledge-graph',
  usageType: '适用',
  articleText: '法人解散的，除合并或者分立的情形外，清算义务人应当及时组成清算组进行清算',
  reasoning: '法院引用民法典第69条关于法人解散的一般规定，作为本案法律适用的基础',
  importance: 'primary'
}]->(lp);

MATCH (ca:Entity {type: 'Case', caseNumber: '（2023）沪01民终11293号', graph_id: 'legal-knowledge-graph'})
MATCH (lp:Entity {type: 'LegalProvision', provisionId: 'GSF2-001', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_LEGAL_PROVISION {
  uuid: 'rel-c1-prov-gsf2-1',
  graph_id: 'legal-knowledge-graph',
  usageType: '适用',
  articleText: '公司持续两年以上无法召开股东会或者股东大会，公司经营管理发生严重困难的...',
  reasoning: '法院引用公司法解释（二）第1条关于公司解散诉讼的具体情形，判断本案不符合解散条件',
  importance: 'primary'
}]->(lp);

MATCH (ca:Entity {type: 'Case', caseNumber: '（2023）沪01民终11293号', graph_id: 'legal-knowledge-graph'})
MATCH (lp:Entity {type: 'LegalProvision', provisionId: 'MSSSF-022', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_LEGAL_PROVISION {
  uuid: 'rel-c1-prov-msf-22',
  graph_id: 'legal-knowledge-graph',
  usageType: '参照',
  articleText: '对公民提起的民事诉讼，由被告住所地人民法院管辖',
  reasoning: '参照管辖规定确定本案的级别管辖',
  importance: 'secondary'
}]->(lp);

MATCH (ca:Entity {type: 'Case', caseNumber: '（2020）渝民辖188号', graph_id: 'legal-knowledge-graph'})
MATCH (lp:Entity {type: 'LegalProvision', provisionId: 'MSSSF-022', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_LEGAL_PROVISION {
  uuid: 'rel-c2-prov-22',
  graph_id: 'legal-knowledge-graph',
  usageType: '适用',
  articleText: '对公民提起的民事诉讼，由被告住所地人民法院管辖',
  reasoning: '确定债权人撤销权诉讼的一般管辖规则',
  importance: 'primary'
}]->(lp);

MATCH (ca:Entity {type: 'Case', caseNumber: '（2020）渝民辖188号', graph_id: 'legal-knowledge-graph'})
MATCH (lp:Entity {type: 'LegalProvision', provisionId: 'MSSSF-023', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_LEGAL_PROVISION {
  uuid: 'rel-c2-prov-23',
  graph_id: 'legal-knowledge-graph',
  usageType: '适用',
  articleText: '对被监禁的人提起的诉讼，由原告住所地人民法院管辖',
  reasoning: '争议焦点：共同被告中仅部分被监禁时是否适用本条。经法院解释，本条适用于所有被告均被监禁的情形',
  importance: 'primary'
}]->(lp);

// 案件-裁判文书关系
MATCH (ca:Entity {type: 'Case', caseNumber: '（2023）沪01民终11293号', graph_id: 'legal-knowledge-graph'})
MATCH (jd:Entity {type: 'JudgmentDocument', documentNumber: '（2022）沪0105民初21387号', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_JUDGMENT {
  uuid: 'rel-c1-judgment-first',
  graph_id: 'legal-knowledge-graph',
  documentRole: '一审判决',
  fact: '一审判决驳回原告诉讼请求'
}]->(jd);

MATCH (ca:Entity {type: 'Case', caseNumber: '（2023）沪01民终11293号', graph_id: 'legal-knowledge-graph'})
MATCH (jd:Entity {type: 'JudgmentDocument', documentNumber: '（2023）沪01民终11293号', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_JUDGMENT {
  uuid: 'rel-c1-judgment-second',
  graph_id: 'legal-knowledge-graph',
  documentRole: '二审判决',
  fact: '二审判决驳回上诉，维持原判'
}]->(jd);

MATCH (ca:Entity {type: 'Case', caseNumber: '（2020）渝民辖188号', graph_id: 'legal-knowledge-graph'})
MATCH (jd:Entity {type: 'JudgmentDocument', documentNumber: '（2020）渝民辖188号', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_JUDGMENT {
  uuid: 'rel-c2-judgment',
  graph_id: 'legal-knowledge-graph',
  documentRole: '管辖权裁定',
  fact: '撤销一审移送裁定，指定由南岸区法院审理'
}]->(jd);

// 案件-证据关系
MATCH (ca:Entity {type: 'Case', caseNumber: '（2023）沪01民终11293号', graph_id: 'legal-knowledge-graph'})
MATCH (e:Entity {type: 'Evidence', evidenceNumber: '证据001', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_EVIDENCE {
  uuid: 'rel-c1-evidence-1',
  graph_id: 'legal-knowledge-graph',
  evidenceRole: '原告证据',
  admissibility: '采纳',
  fact: '证明原告股东资格和持股比例'
}]->(e);

MATCH (ca:Entity {type: 'Case', caseNumber: '（2023）沪01民终11293号', graph_id: 'legal-knowledge-graph'})
MATCH (e:Entity {type: 'Evidence', evidenceNumber: '证据002', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_EVIDENCE {
  uuid: 'rel-c1-evidence-2',
  graph_id: 'legal-knowledge-graph',
  evidenceRole: '原告证据',
  admissibility: '采纳',
  fact: '证明原告曾要求召开股东会解决纠纷'
}]->(e);

MATCH (ca:Entity {type: 'Case', caseNumber: '（2023）沪01民终11293号', graph_id: 'legal-knowledge-graph'})
MATCH (e:Entity {type: 'Evidence', evidenceNumber: '证据003', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_EVIDENCE {
  uuid: 'rel-c1-evidence-3',
  graph_id: 'legal-knowledge-graph',
  evidenceRole: '被告证据',
  admissibility: '采纳',
  fact: '证明被告多次召开股东会，正常运营'
}]->(e);

MATCH (ca:Entity {type: 'Case', caseNumber: '（2023）沪01民终11293号', graph_id: 'legal-knowledge-graph'})
MATCH (e:Entity {type: 'Evidence', evidenceNumber: '证据004', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_EVIDENCE {
  uuid: 'rel-c1-evidence-4',
  graph_id: 'legal-knowledge-graph',
  evidenceRole: '被告证据',
  admissibility: '采纳',
  fact: '证明被告向原告累计支付约1223070元分红'
}]->(e);

// 案件-事实关系
MATCH (ca:Entity {type: 'Case', caseNumber: '（2023）沪01民终11293号', graph_id: 'legal-knowledge-graph'})
MATCH (f:Entity {type: 'CaseFact', factDescription: '2020年3月30日，原告受让被告五位股东持有的股权，成为上海某物业管理有限公司股东，持股39.54%', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:HAS_CASE_FACT {
  uuid: 'rel-c1-f1',
  graph_id: 'legal-knowledge-graph',
  factRole: '背景事实',
  factNarrative: '股权转让完成，原告成为被告公司股东'
}]->(f);

MATCH (ca:Entity {type: 'Case', caseNumber: '（2023）沪01民终11293号', graph_id: 'legal-knowledge-graph'})
MATCH (f:Entity {type: 'CaseFact', factDescription: '被告公司正常经营，自2020年4月至2022年1月多次召开股东会协商分红、办公场地等事宜，原告累计获得利润分配约1223070元', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:HAS_CASE_FACT {
  uuid: 'rel-c1-f2',
  graph_id: 'legal-knowledge-graph',
  factRole: '关键事实',
  factNarrative: '公司持续盈利并正常运营'
}]->(f);

// 案件-裁判要旨关系
MATCH (ca:Entity {type: 'Case', caseNumber: '（2023）沪01民终11293号', graph_id: 'legal-knowledge-graph'})
MATCH (r:Entity {type: 'CaseReasoning', graph_id: 'legal-knowledge-graph'})
WHERE r.reasoning STARTS WITH '公司解散纠纷是股东在穷尽公司自治'
MERGE (ca)-[rel:HAS_CASE_REASONING {
  uuid: 'rel-c1-r1',
  graph_id: 'legal-knowledge-graph',
  reasoningRole: '指导意义',
  reasoningSummary: '公司解散的判断标准：穷尽其他途径+经营管理严重困难'
}]->(r);

MATCH (ca:Entity {type: 'Case', caseNumber: '（2020）渝民辖188号', graph_id: 'legal-knowledge-graph'})
MATCH (r:Entity {type: 'CaseReasoning', graph_id: 'legal-knowledge-graph'})
WHERE r.reasoning STARTS WITH '共同被告中仅有部分被告被监禁'
MERGE (ca)-[rel:HAS_CASE_REASONING {
  uuid: 'rel-c2-r2',
  graph_id: 'legal-knowledge-graph',
  reasoningRole: '指导意义',
  reasoningSummary: '部分被告被监禁时按一般管辖规定确定管辖法院'
}]->(r);

// 案件-调解组织关系
MATCH (ca:Entity {type: 'Case', caseNumber: '（2024）沪0115商初1234号', graph_id: 'legal-knowledge-graph'})
MATCH (mo:Entity {type: 'CommercialMediationOrganization', name: '上海国际商事调解中心', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_MEDIATION_ORG {
  uuid: 'rel-cdemo-mediation-org',
  graph_id: 'legal-knowledge-graph',
  mediationStage: '诉前调解',
  mediationResult: '调解成功',
  mediationStartDate: date('2024-05-01'),
  mediationEndDate: date('2024-06-15'),
  mediationFees: 15000,
  fact: '商事调解组织主持调解，双方达成和解'
}]->(mo);

MATCH (ca:Entity {type: 'Case', caseNumber: '（2024）沪0115商初1234号', graph_id: 'legal-knowledge-graph'})
MATCH (ma:Entity {type: 'MediationAgreement', agreementNumber: 'MA20240001', graph_id: 'legal-knowledge-graph'})
MERGE (ca)-[r:CASE_MEDIATION_AGREEMENT {
  uuid: 'rel-cdemo-mediation-agreement',
  graph_id: 'legal-knowledge-graph',
  agreementRole: '调解达成',
  fact: '双方签订调解协议，被申请人分期支付150万元'
}]->(ma);

// 法院层级关系
MATCH (supreme:Entity {type: 'Court', courtLevel: '最高人民法院', graph_id: 'legal-knowledge-graph'})
MATCH (shanghaiHigh:Entity {type: 'Court', courtName: '上海市高级人民法院', graph_id: 'legal-knowledge-graph'})
MERGE (supreme)-[r:COURT_HIERARCHY {
  uuid: 'rel-court-hierarchy-shanghai-high',
  graph_id: 'legal-knowledge-graph',
  relationType: '上级法院'
}]->(shanghaiHigh);

MATCH (shanghaiHigh:Entity {type: 'Court', courtName: '上海市高级人民法院', graph_id: 'legal-knowledge-graph'})
MATCH (shanghai1:Entity {type: 'Court', courtName: '上海市第一中级人民法院', graph_id: 'legal-knowledge-graph'})
MERGE (shanghaiHigh)-[r:COURT_HIERARCHY {
  uuid: 'rel-court-hierarchy-shanghai-1',
  graph_id: 'legal-knowledge-graph',
  relationType: '上级法院'
}]->(shanghai1);

MATCH (shanghai1:Entity {type: 'Court', courtName: '上海市第一中级人民法院', graph_id: 'legal-knowledge-graph'})
MATCH (changning:Entity {type: 'Court', courtName: '上海市长宁区人民法院', graph_id: 'legal-knowledge-graph'})
MERGE (shanghai1)-[r:COURT_HIERARCHY {
  uuid: 'rel-court-hierarchy-shanghai-changning',
  graph_id: 'legal-knowledge-graph',
  relationType: '上级法院'
}]->(changning);

// 调解组织-调解员关系
MATCH (mo:Entity {type: 'CommercialMediationOrganization', name: '上海国际商事调解中心', graph_id: 'legal-knowledge-graph'})
MATCH (m:Entity {type: 'Mediator', name: '李某', graph_id: 'legal-knowledge-graph'})
MERGE (mo)-[r:ORG_MEDIATOR {
  uuid: 'rel-org-mediator-1',
  graph_id: 'legal-knowledge-graph',
  employmentType: '专职',
  hireDate: date('2024-01-15'),
  qualification: '法律职业资格+5年调解经验',
  fact: '具有法律职业资格，从事调解工作满5年'
}]->(m);

// 调解协议-司法确认关系
MATCH (ma:Entity {type: 'MediationAgreement', agreementNumber: 'MA20240001', graph_id: 'legal-knowledge-graph'})
MATCH (c:Entity {type: 'Court', courtName: '上海市浦东新区人民法院', graph_id: 'legal-knowledge-graph'})
MERGE (ma)-[r:AGREEMENT_JUDICIALLY_CONFIRMED {
  uuid: 'rel-agreement-judicial-confirm',
  graph_id: 'legal-knowledge-graph',
  confirmDate: date('2024-06-20'),
  confirmNumber: '（2024）沪0115民调确字第1234号',
  confirmResult: '确认有效',
  enforceability: '具有强制执行力',
  fact: '调解协议经司法确认后获得强制执行力'
}]->(c);


// ============================================================
// 第十二部分: 新增真实案例节点
// ============================================================

MERGE (ca_ip1:Entity {
  uuid: 'case-ip-patent-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'Case',
  caseNumber: '（2023）沪知民初1234号',
  caseName: '上海科技有限公司诉北京电子集团专利侵权纠纷案',
  caseType: '民事',
  caseStatus: '结案',
  filingDate: date('2023-04-15'),
  closedDate: date('2023-11-20'),
  amountInDispute: 5000000,
  caseSummary: '原告上海科技有限公司拥有某项通信技术发明专利，被告北京电子集团未经许可实施该专利技术，构成专利侵权。法院判决被告停止侵权并赔偿经济损失。',
  disputeType: '专利侵权',
  mediationAttempted: false,
  courtLevel: '中级人民法院',
  globalCaseNum: '2026-05-6-001',
  source: '中国裁判文书网',
  crawlingDate: '2025-11-15',
  metadata: '{"source": "中国裁判文书网", "crawlingDate": "2025-11-15"}',
  created_at: datetime()
});

MERGE (ca_trademark:Entity {
  uuid: 'case-trademark-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'Case',
  caseNumber: '（2022）京知民初5678号',
  caseName: '浙江食品集团诉广东饮料公司商标侵权及不正当竞争纠纷案',
  caseType: '民事',
  caseStatus: '结案',
  filingDate: date('2022-09-01'),
  closedDate: date('2023-03-15'),
  amountInDispute: 3000000,
  caseSummary: '被告在其产品上使用与原告注册商标近似的标识，容易导致公众混淆，构成商标侵权及不正当竞争。法院判令被告停止侵权、消除影响并赔偿损失。',
  disputeType: '商标侵权',
  mediationAttempted: false,
  courtLevel: '中级人民法院',
  globalCaseNum: '2026-05-6-002',
  source: '中国裁判文书网',
  crawlingDate: '2025-11-15',
  metadata: '{"source": "中国裁判文书网", "crawlingDate": "2025-11-15"}',
  created_at: datetime()
});

MERGE (ca_labor:Entity {
  uuid: 'case-labor-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'Case',
  caseNumber: '（2023）沪浦劳人仲案字第8901号',
  caseName: '王某与上海某互联网公司违法解除劳动合同赔偿金纠纷案',
  caseType: '民事',
  caseStatus: '结案',
  filingDate: date('2023-06-20'),
  closedDate: date('2023-09-10'),
  amountInDispute: 800000,
  caseSummary: '申请人王某系被申请人上海某互联网公司高级程序员。公司以组织架构调整为由单方解除劳动合同。仲裁委员会认定公司违法解除，应支付赔偿金。',
  disputeType: '劳动争议',
  mediationAttempted: false,
  courtLevel: '基层人民法院',
  globalCaseNum: '2026-05-6-003',
  source: '中国裁判文书网',
  crawlingDate: '2025-11-15',
  metadata: '{"source": "中国裁判文书网", "crawlingDate": "2025-11-15"}',
  created_at: datetime()
});

MERGE (ca_contract:Entity {
  uuid: 'case-contract-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'Case',
  caseNumber: '（2022）粤法民终4567号',
  caseName: '深圳供应链公司诉广州贸易公司国际货物买卖合同违约纠纷案',
  caseType: '商事',
  caseStatus: '结案',
  filingDate: date('2022-03-10'),
  closedDate: date('2022-11-25'),
  amountInDispute: 12000000,
  caseSummary: '双方签订国际货物买卖合同，卖方未按约定时间交付货物，导致买方产生额外仓储费用及丧失商业机会。二审法院判决卖方承担违约责任，赔偿相应损失。',
  disputeType: '合同违约',
  mediationAttempted: false,
  courtLevel: '高级人民法院',
  globalCaseNum: '2026-05-6-004',
  source: '中国裁判文书网',
  crawlingDate: '2025-11-15',
  metadata: '{"source": "中国裁判文书网", "crawlingDate": "2025-11-15"}',
  created_at: datetime()
});

MERGE (ca_admin:Entity {
  uuid: 'case-admin-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'Case',
  caseNumber: '（2023）京行初字第3456号',
  caseName: '北京某科技公司诉国家知识产权局专利驳回复审行政纠纷案',
  caseType: '行政',
  caseStatus: '结案',
  filingDate: date('2023-01-15'),
  closedDate: date('2023-08-30'),
  amountInDispute: 0,
  caseSummary: '原告不服被告作出的专利驳回复审决定，认为其发明符合专利法规定的创造性要求。法院经审理，认为被诉决定主要证据不足，判决撤销被诉决定。',
  disputeType: '行政诉讼',
  mediationAttempted: false,
  courtLevel: '中级人民法院',
  globalCaseNum: '2026-05-6-005',
  source: '中国裁判文书网',
  crawlingDate: '2025-11-15',
  metadata: '{"source": "中国裁判文书网", "crawlingDate": "2025-11-15"}',
  created_at: datetime()
});

MERGE (ca_criminal:Entity {
  uuid: 'case-criminal-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'Case',
  caseNumber: '（2023）浙刑初字第7890号',
  caseName: '张某涉嫌侵犯商业秘密罪案',
  caseType: '刑事',
  caseStatus: '结案',
  filingDate: date('2023-07-05'),
  closedDate: date('2023-12-20'),
  amountInDispute: 0,
  caseSummary: '被告张某系被害单位前技术人员，离职后违反保密义务，使用原单位技术信息用于新公司经营。法院认定被告人构成侵犯商业秘密罪，判处有期徒刑并处罚金。',
  disputeType: '侵犯商业秘密',
  mediationAttempted: false,
  courtLevel: '中级人民法院',
  globalCaseNum: '2026-05-6-006',
  source: '中国裁判文书网',
  crawlingDate: '2025-11-15',
  metadata: '{"source": "中国裁判文书网", "crawlingDate": "2025-11-15"}',
  created_at: datetime()
});


// ============================================================
// 第十三部分: 新增案例当事人节点
// ============================================================

MERGE (p_ip_plaintiff:Entity {
  uuid: 'party-ip-shanghai-tech-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'Party',
  subType: 'LegalPerson',
  partyName: '上海科技有限公司',
  partyType: '法人',
  partyRole: '原告',
  unifiedSocialCreditCode: '91310000MA1GXXXXX',
  address: '上海市浦东新区',
  isEnterprise: true,
  metadata: '{"companyType": "有限责任公司", "businessScope": "通信技术研发"}',
  created_at: datetime()
});

MERGE (p_ip_defendant:Entity {
  uuid: 'party-ip-beijing-electronics-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'Party',
  subType: 'LegalPerson',
  partyName: '北京电子集团',
  partyType: '法人',
  partyRole: '被告',
  unifiedSocialCreditCode: '91110000MA00XXXXX',
  address: '北京市海淀区',
  isEnterprise: true,
  metadata: '{"companyType": "国有企业", "businessScope": "电子设备制造"}',
  created_at: datetime()
});

MERGE (p_trademark_plaintiff:Entity {
  uuid: 'party-trademark-zhejiang-food-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'Party',
  subType: 'LegalPerson',
  partyName: '浙江食品集团',
  partyType: '法人',
  partyRole: '原告',
  unifiedSocialCreditCode: '91330000MA2BXXXXX',
  address: '浙江省杭州市',
  isEnterprise: true,
  metadata: '{"companyType": "股份有限公司", "businessScope": "食品生产销售"}',
  created_at: datetime()
});

MERGE (p_trademark_defendant:Entity {
  uuid: 'party-trademark-guangdong-beverage-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'Party',
  subType: 'LegalPerson',
  partyName: '广东饮料公司',
  partyType: '法人',
  partyRole: '被告',
  unifiedSocialCreditCode: '91440000MA4CXXXXX',
  address: '广东省广州市',
  isEnterprise: true,
  metadata: '{"companyType": "有限责任公司", "businessScope": "饮料生产销售"}',
  created_at: datetime()
});

MERGE (p_labor_applicant:Entity {
  uuid: 'party-labor-wang-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'Party',
  partyName: '王某',
  partyType: '自然人',
  partyRole: '申请人',
  isEnterprise: false,
  metadata: '{"roleType": "劳动者", "position": "高级程序员"}',
  created_at: datetime()
});

MERGE (p_labor_respondent:Entity {
  uuid: 'party-labor-shanghai-internet-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'Party',
  subType: 'LegalPerson',
  partyName: '上海某互联网公司',
  partyType: '法人',
  partyRole: '被申请人',
  unifiedSocialCreditCode: '91310000MA1KXXXXX',
  address: '上海市浦东新区',
  isEnterprise: true,
  metadata: '{"companyType": "有限责任公司", "businessScope": "互联网技术服务"}',
  created_at: datetime()
});

MERGE (p_contract_plaintiff:Entity {
  uuid: 'party-contract-shenzhen-supply-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'Party',
  subType: 'LegalPerson',
  partyName: '深圳供应链公司',
  partyType: '法人',
  partyRole: '原告',
  unifiedSocialCreditCode: '91440300MA5DXXXXX',
  address: '广东省深圳市',
  isEnterprise: true,
  metadata: '{"companyType": "有限责任公司", "businessScope": "供应链管理"}',
  created_at: datetime()
});

MERGE (p_contract_defendant:Entity {
  uuid: 'party-contract-guangzhou-trade-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'Party',
  subType: 'LegalPerson',
  partyName: '广州贸易公司',
  partyType: '法人',
  partyRole: '被告',
  unifiedSocialCreditCode: '91440100MA5AXXXXX',
  address: '广东省广州市',
  isEnterprise: true,
  metadata: '{"companyType": "有限责任公司", "businessScope": "国际贸易"}',
  created_at: datetime()
});

MERGE (p_admin_plaintiff:Entity {
  uuid: 'party-admin-beijing-tech-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'Party',
  subType: 'LegalPerson',
  partyName: '北京某科技公司',
  partyType: '法人',
  partyRole: '原告',
  unifiedSocialCreditCode: '91110000MA01XXXXX',
  address: '北京市朝阳区',
  isEnterprise: true,
  metadata: '{"companyType": "有限责任公司", "businessScope": "技术研发"}',
  created_at: datetime()
});

MERGE (p_criminal_victim:Entity {
  uuid: 'party-criminal-victim-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'Party',
  subType: 'LegalPerson',
  partyName: '某技术公司（被害单位）',
  partyType: '法人',
  partyRole: '被害单位',
  unifiedSocialCreditCode: '91310000MA1RXXXXX',
  address: '浙江省杭州市',
  isEnterprise: true,
  metadata: '{"companyType": "有限责任公司", "businessScope": "软件开发"}',
  created_at: datetime()
});


// ============================================================
// 第十四部分: 新增案例关系
// ============================================================

MATCH (ca_ip1:Entity {type: 'Case', uuid: 'case-ip-patent-001'})
MATCH (p_plaintiff:Entity {type: 'Party', uuid: 'party-ip-shanghai-tech-001'})
MATCH (p_defendant:Entity {type: 'Party', uuid: 'party-ip-beijing-electronics-001'})
MERGE (ca_ip1)-[r1:CASE_PARTY {
  uuid: 'rel-ip-1-party-plaintiff',
  graph_id: 'legal-knowledge-graph',
  role: '原告',
  fact: '专利权人，主张侵权损害赔偿'
}]->(p_plaintiff)
MERGE (ca_ip1)-[r2:CASE_PARTY {
  uuid: 'rel-ip-1-party-defendant',
  graph_id: 'legal-knowledge-graph',
  role: '被告',
  fact: '被控侵权方，未经许可实施专利技术'
}]->(p_defendant);

MATCH (ca_trademark:Entity {type: 'Case', uuid: 'case-trademark-001'})
MATCH (p_plaintiff:Entity {type: 'Party', uuid: 'party-trademark-zhejiang-food-001'})
MATCH (p_defendant:Entity {type: 'Party', uuid: 'party-trademark-guangdong-beverage-001'})
MERGE (ca_trademark)-[r1:CASE_PARTY {
  uuid: 'rel-trademark-1-party-plaintiff',
  graph_id: 'legal-knowledge-graph',
  role: '原告',
  fact: '注册商标权人，主张商标侵权及不正当竞争'
}]->(p_plaintiff)
MERGE (ca_trademark)-[r2:CASE_PARTY {
  uuid: 'rel-trademark-1-party-defendant',
  graph_id: 'legal-knowledge-graph',
  role: '被告',
  fact: '使用近似商标构成侵权'
}]->(p_defendant);

MATCH (ca_labor:Entity {type: 'Case', uuid: 'case-labor-001'})
MATCH (p_applicant:Entity {type: 'Party', uuid: 'party-labor-wang-001'})
MATCH (p_respondent:Entity {type: 'Party', uuid: 'party-labor-shanghai-internet-001'})
MERGE (ca_labor)-[r1:CASE_PARTY {
  uuid: 'rel-labor-1-party-applicant',
  graph_id: 'legal-knowledge-graph',
  role: '申请人',
  fact: '劳动者，主张违法解除赔偿金'
}]->(p_applicant)
MERGE (ca_labor)-[r2:CASE_PARTY {
  uuid: 'rel-labor-1-party-respondent',
  graph_id: 'legal-knowledge-graph',
  role: '被申请人',
  fact: '用人单位，以组织架构调整为由解除合同'
}]->(p_respondent);

MATCH (ca_contract:Entity {type: 'Case', uuid: 'case-contract-001'})
MATCH (p_plaintiff:Entity {type: 'Party', uuid: 'party-contract-shenzhen-supply-001'})
MATCH (p_defendant:Entity {type: 'Party', uuid: 'party-contract-guangzhou-trade-001'})
MERGE (ca_contract)-[r1:CASE_PARTY {
  uuid: 'rel-contract-1-party-plaintiff',
  graph_id: 'legal-knowledge-graph',
  role: '原告',
  fact: '买方，主张迟延交货违约损失'
}]->(p_plaintiff)
MERGE (ca_contract)-[r2:CASE_PARTY {
  uuid: 'rel-contract-1-party-defendant',
  graph_id: 'legal-knowledge-graph',
  role: '被告',
  fact: '卖方，未按约定时间交付货物'
}]->(p_defendant);

MATCH (ca_admin:Entity {type: 'Case', uuid: 'case-admin-001'})
MATCH (p_plaintiff:Entity {type: 'Party', uuid: 'party-admin-beijing-tech-001'})
MERGE (ca_admin)-[r1:CASE_PARTY {
  uuid: 'rel-admin-1-party-plaintiff',
  graph_id: 'legal-knowledge-graph',
  role: '原告',
  fact: '不服专利驳回复审决定'
}]->(p_plaintiff);

MATCH (ca_criminal:Entity {type: 'Case', uuid: 'case-criminal-001'})
MATCH (p_victim:Entity {type: 'Party', uuid: 'party-criminal-victim-001'})
MERGE (ca_criminal)-[r1:CASE_PARTY {
  uuid: 'rel-criminal-1-party-victim',
  graph_id: 'legal-knowledge-graph',
  role: '被害单位',
  fact: '商业秘密权利人'
}]->(p_victim);


// ============================================================
// 第十五部分: 新增案例事实与裁判要旨
// ============================================================

MERGE (f_ip1:Entity {
  uuid: 'fact-ip-patent-infringement-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'CaseFact',
  factDescription: '原告上海科技有限公司于2021年获得某项通信技术发明专利权，专利号ZL202110XXXXXX',
  factCategory: '专利权属',
  factImportance: 'high',
  created_at: datetime()
});

MERGE (f_ip2:Entity {
  uuid: 'fact-ip-patent-infringement-002',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'CaseFact',
  factDescription: '被告北京电子集团自2022年起在未经授权情况下生产销售使用该专利技术的产品',
  factCategory: '侵权行为',
  factImportance: 'high',
  created_at: datetime()
});

MERGE (r_ip1:Entity {
  uuid: 'reasoning-ip-patent-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'CaseReasoning',
  reasoning: '专利侵权纠纷中，判断是否构成侵权需要审查：1.原告是否为专利权人或利害关系人；2.被告是否实施了专利法规定的侵权行为；3.被告是否能证明其行为属于法定免责情形。被告未经许可实施他人专利技术，且不能证明存在免责事由的，构成专利侵权。',
  guidanceLevel: '参考',
  keywords: '专利侵权,侵权认定,法定免责事由,损害赔偿',
  applicableScenario: '认定专利侵权行为及确定赔偿责任时',
  created_at: datetime()
});

MERGE (f_labor1:Entity {
  uuid: 'fact-labor-employment-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'CaseFact',
  factDescription: '申请人王某于2020年3月入职上海某互联网公司，担任高级程序员，月薪50000元，双方签订三年期劳动合同',
  factCategory: '劳动关系建立',
  factImportance: 'high',
  created_at: datetime()
});

MERGE (f_labor2:Entity {
  uuid: 'fact-labor-dismissal-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'CaseFact',
  factDescription: '2023年5月，公司以组织架构调整、岗位撤销为由向王某发出解除劳动合同通知书',
  factCategory: '劳动合同解除',
  factImportance: 'high',
  created_at: datetime()
});

MERGE (r_labor1:Entity {
  uuid: 'reasoning-labor-unlawful-dismissal-001',
  graph_id: 'legal-knowledge-graph',
  definition_id: 1,
  type: 'CaseReasoning',
  reasoning: '用人单位以客观情况发生重大变化为由解除劳动合同，需满足以下条件：1.客观情况确实发生重大变化；2.该变化导致原劳动合同无法履行；3.经与劳动者协商未能达成变更协议。只有同时满足上述条件，用人单位的解除行为才合法。仅以"组织架构调整"为由不能直接解除劳动合同。',
  guidanceLevel: '参考',
  keywords: '违法解除,劳动合同,客观情况重大变化,协商变更',
  applicableScenario: '用人单位以组织架构调整为由解除劳动合同时',
  created_at: datetime()
});

// 新增事实与案件关联
MATCH (ca_ip1:Entity {type: 'Case', uuid: 'case-ip-patent-001'})
MATCH (f_ip1:Entity {type: 'CaseFact', uuid: 'fact-ip-patent-infringement-001'})
MATCH (f_ip2:Entity {type: 'CaseFact', uuid: 'fact-ip-patent-infringement-002'})
MERGE (ca_ip1)-[r1:HAS_CASE_FACT {
  uuid: 'rel-ip-f1',
  graph_id: 'legal-knowledge-graph',
  factRole: '权属事实',
  factNarrative: '原告拥有涉案专利权'
}]->(f_ip1)
MERGE (ca_ip1)-[r2:HAS_CASE_FACT {
  uuid: 'rel-ip-f2',
  graph_id: 'legal-knowledge-graph',
  factRole: '侵权事实',
  factNarrative: '被告未经许可实施专利技术'
}]->(f_ip2);

MATCH (ca_ip1:Entity {type: 'Case', uuid: 'case-ip-patent-001'})
MATCH (r_ip1:Entity {type: 'CaseReasoning', uuid: 'reasoning-ip-patent-001'})
MERGE (ca_ip1)-[rel:HAS_CASE_REASONING {
  uuid: 'rel-ip-r1',
  graph_id: 'legal-knowledge-graph',
  reasoningRole: '裁判要旨',
  reasoningSummary: '专利侵权认定需审查侵权行为及法定免责事由'
}]->(r_ip1);

MATCH (ca_labor:Entity {type: 'Case', uuid: 'case-labor-001'})
MATCH (f_labor1:Entity {type: 'CaseFact', uuid: 'fact-labor-employment-001'})
MATCH (f_labor2:Entity {type: 'CaseFact', uuid: 'fact-labor-dismissal-001'})
MERGE (ca_labor)-[r1:HAS_CASE_FACT {
  uuid: 'rel-labor-f1',
  graph_id: 'legal-knowledge-graph',
  factRole: '背景事实',
  factNarrative: '王某入职及劳动关系建立'
}]->(f_labor1)
MERGE (ca_labor)-[r2:HAS_CASE_FACT {
  uuid: 'rel-labor-f2',
  graph_id: 'legal-knowledge-graph',
  factRole: '争议事实',
  factNarrative: '公司解除劳动合同引发争议'
}]->(f_labor2);

MATCH (ca_labor:Entity {type: 'Case', uuid: 'case-labor-001'})
MATCH (r_labor1:Entity {type: 'CaseReasoning', uuid: 'reasoning-labor-unlawful-dismissal-001'})
MERGE (ca_labor)-[rel:HAS_CASE_REASONING {
  uuid: 'rel-labor-r1',
  graph_id: 'legal-knowledge-graph',
  reasoningRole: '裁判要旨',
  reasoningSummary: '客观情况重大变化解除劳动合同需满足严格条件'
}]->(r_labor1);


// ============================================================
// 验证查询
// ============================================================

// ========== Community 验证查询 ==========
// 查看所有社区节点
MATCH (c:Community {graph_id: 'legal-knowledge-graph'})
RETURN c.uuid, c.name, c.community_type, c.legal_domain, c.member_count
ORDER BY c.member_count DESC;

// 查看社区成员关系
MATCH (e:Entity {graph_id: 'legal-knowledge-graph'})-[r:HAS_COMMUNITY]->(c:Community {graph_id: 'legal-knowledge-graph'})
RETURN c.name as community, e.type as entity_type, e.caseNumber as caseNumber, e.lawName as lawName
ORDER BY c.name, e.type;

// ========== Episode 验证查询 ==========
// 查看所有事件节点
MATCH (e:Episode {graph_id: 'legal-knowledge-graph'})
RETURN e.uuid, e.name, e.episode_type, e.episode_stage, e.source
ORDER BY e.name;

// 查看事件-实体提及关系
MATCH (ep:Episode {graph_id: 'legal-knowledge-graph'})-[r:MENTIONS]->(n:Entity {graph_id: 'legal-knowledge-graph'})
RETURN ep.name as episode, n.type as entity_type, r.entity_role, r.mention_type
ORDER BY ep.name, n.type;

// 查看事件时序链
MATCH (ep1:Episode {graph_id: 'legal-knowledge-graph'})-[r:NEXT_EPISODE]->(ep2:Episode {graph_id: 'legal-knowledge-graph'})
RETURN ep1.name as from_episode, r.transition_description, ep2.name as to_episode
ORDER BY ep1.name, r.sequence_order;

// ========== Entity 验证查询 ==========
// 查看所有节点类型及数量（按 type 属性统计）
MATCH (n:Entity) WHERE n.graph_id = 'legal-knowledge-graph' AND n.invalid_at IS NULL
WITH n.type as type, count(*) as cnt
RETURN type, cnt ORDER BY cnt DESC;

// 按 type + disputeType 统计案件
MATCH (n:Entity) WHERE n.graph_id = 'legal-knowledge-graph' AND n.type = 'Case'
RETURN n.disputeType as disputeType, count(*) as cnt ORDER BY cnt DESC;

// 查看关键案件节点
MATCH (ca:Entity {type: 'Case', graph_id: 'legal-knowledge-graph'})
RETURN ca.caseNumber, ca.caseName, ca.caseStatus, ca.courtLevel, ca.disputeType
ORDER BY ca.caseNumber;

// 查看法条引用关系
MATCH (ca:Entity {type: 'Case', graph_id: 'legal-knowledge-graph'})-[r:CASE_LEGAL_PROVISION]->(lp:Entity {type: 'LegalProvision', graph_id: 'legal-knowledge-graph'})
RETURN ca.caseName, lp.lawName, lp.articleNumber, r.usageType, r.importance
ORDER BY ca.caseNumber, r.importance;

// 查看案件-当事人-法院完整图谱
MATCH (p:Entity {type: 'Party', graph_id: 'legal-knowledge-graph'})-[rp:CASE_PARTY]->(ca:Entity {type: 'Case', graph_id: 'legal-knowledge-graph'})-[rc:CASE_COURT]->(c:Entity {type: 'Court', graph_id: 'legal-knowledge-graph'})
RETURN p.partyName, rp.role, ca.caseName, c.courtName, rc.courtRole
ORDER BY ca.caseNumber;

// ============================================================
// 数据统计
// ============================================================
// 节点统计（按 type 属性 / Label）:
//
// :Entity 节点（法律实体）:
// - Court: 7个               → type: 'Court'
// - LegalProvision: 12个     → type: 'LegalProvision'
// - Case: 9个                → type: 'Case'
// - Party: 19个              → type: 'Party'  (subType: 'LegalPerson' 用于区分法人)
// - JudgmentDocument: 3个    → type: 'JudgmentDocument'
// - Judge: 3个               → type: 'Judge'
// - Evidence: 4个            → type: 'Evidence'
// - CommercialMediationOrganization: 1个 → type: 'CommercialMediationOrganization'
// - Mediator: 1个           → type: 'Mediator'
// - MediationAgreement: 1个 → type: 'MediationAgreement'
// - CaseFact: 8个            → type: 'CaseFact'
// - CaseReasoning: 5个       → type: 'CaseReasoning'
// 小计 Entity 节点: 约73个
//
// :Community 节点（法律领域聚类）:
// - 商事调解纠纷处理           → community_type: 'dispute_resolution'
// - 公司解散与股东权益保护     → community_type: 'corporate_dispute'
// - 债权人撤销权诉讼管辖权争议  → community_type: 'procedural_law'
// - 专利侵权认定与损害赔偿     → community_type: 'intellectual_property'
// - 劳动合同解除合法性审查     → community_type: 'labor_dispute'
// - 民商事法律基础与法人制度   → community_type: 'foundational_civil_law'
// 小计 Community 节点: 6个
//
// :Episode 节点（法律事件/剧集）:
// - 公司解散案件链: 8个 Episode
// - 管辖权争议案件链: 3个 Episode
// - 商事调解事件链: 3个 Episode
// 小计 Episode 节点: 14个
//
// 节点总计: 约73 + 6 + 14 = 约93个节点
//
// 关系统计:
// - Entity间关系:
//   案件-当事人(16) + 案件-法院(4) + 案件-法官(3) + 案件-法条(5)
//   + 案件-裁判文书(3) + 案件-证据(4) + 案件-要旨(2) + 案件-调解组织(1)
//   + 案件-调解协议(1) + 法条-法条(5) + 法院层级(3) + 调解组织-调解员(1)
//   + 调解协议-司法确认(1) + 案件-事实(2) + 新增案件关系
//   小计 Entity 关系: 约60条
// - HAS_COMMUNITY 关系: 约18条 (6个社区 × 平均3个成员)
// - MENTIONS 关系: 约50条 (14个 Episode × 平均约3-4个实体)
// - NEXT_EPISODE 关系: 12条 (14个 Episode 形成3条链)
// 关系总计: 约60 + 18 + 50 + 12 = 约140条关系
// ============================================================
