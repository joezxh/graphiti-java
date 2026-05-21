-- ============================================================
-- 法律知识图谱 V3.0.0: 元数据初始化（PostgreSQL）
-- 版本: 2026-05-20
-- 说明: 适用于 PostgreSQL 13+
-- 依赖: schema-v3.sql 已执行完毕，ont_definition 表已有数据
--       definition_id = 1 对应 legal-knowledge-graph
-- ============================================================

DO $$
DECLARE
    v_def_id BIGINT := 1;  -- legal-knowledge-graph 的 definition_id
BEGIN

-- ====== 一、ont_community_type 初始化 ======

-- 1.1 法律领域 (domain) - 层级嵌套
INSERT INTO ont_community_type (definition_id, type_code, type_name, type_name_en, category, parent_type_code, sort_order, metadata, description) VALUES
-- 顶级父类型
(v_def_id, 'DOMAIN_ROOT', '法律领域', 'Legal Domain', 'domain', NULL, 0,
 '{"icon": "book", "color": "#1565C0"}', '法律AI顶级社区，所有法律专题子社区的父社区'),

-- 民商事领域
(v_def_id, 'DOMAIN_CIVIL', '民商事', 'Civil & Commercial', 'domain', 'DOMAIN_ROOT', 1,
 '{"icon": "scale", "color": "#2E7D32"}', '涵盖民法、商法范围内的纠纷解决'),

-- 刑事领域
(v_def_id, 'DOMAIN_CRIMINAL', '刑事法律', 'Criminal Law', 'domain', 'DOMAIN_ROOT', 2,
 '{"icon": "shield", "color": "#C62828"}', '涵盖刑法及刑事诉讼法相关'),

-- 行政法律
(v_def_id, 'DOMAIN_ADMIN', '行政法律', 'Administrative Law', 'domain', 'DOMAIN_ROOT', 3,
 '{"icon": "building", "color": "#6A1B9A"}', '涵盖行政法、行政诉讼法'),

-- 知识产权
(v_def_id, 'DOMAIN_IP', '知识产权', 'Intellectual Property', 'domain', 'DOMAIN_ROOT', 4,
 '{"icon": "lightbulb", "color": "#F57F17"}', '涵盖专利、商标、著作权'),

-- 劳动法律
(v_def_id, 'DOMAIN_LABOR', '劳动法律', 'Labor Law', 'domain', 'DOMAIN_ROOT', 5,
 '{"icon": "briefcase", "color": "#00838F"}', '涵盖劳动法、劳动合同法'),

-- 商事调解
(v_def_id, 'DOMAIN_MEDIATION', '商事调解', 'Commercial Mediation', 'domain', 'DOMAIN_ROOT', 6,
 '{"icon": "handshake", "color": "#AD1457"}', '涵盖多元化纠纷解决机制（ADR）'),

-- 执行程序
(v_def_id, 'DOMAIN_EXECUTION', '执行程序', 'Execution Procedure', 'domain', 'DOMAIN_ROOT', 7,
 '{"icon": "gavel", "color": "#37474F"}', '涵盖民事执行、刑事执行');

-- 1.2 应用场景 (practice) - 独立维度
INSERT INTO ont_community_type (definition_id, type_code, type_name, type_name_en, category, parent_type_code, sort_order, metadata) VALUES
(v_def_id, 'PRACTICE_JUDICIAL', '司法实践', 'Judicial Practice', 'practice', NULL, 10,
 '{"icon": "court", "color": "#1565C0"}'),
(v_def_id, 'PRACTICE_ARBITRATION', '仲裁实践', 'Arbitration Practice', 'practice', NULL, 11,
 '{"icon": "scale", "color": "#0288D1"}'),
(v_def_id, 'PRACTICE_MEDIATION', '调解实践', 'Mediation Practice', 'practice', NULL, 12,
 '{"icon": "handshake", "color": "#AD1457"}'),
(v_def_id, 'PRACTICE_COMPLIANCE', '企业合规', 'Corporate Compliance', 'practice', NULL, 13,
 '{"icon": "shield", "color": "#558B2F"}');

-- 1.3 司法管辖区 (jurisdiction) - 独立维度
INSERT INTO ont_community_type (definition_id, type_code, type_name, type_name_en, category, sort_order, metadata) VALUES
(v_def_id, 'JURISDICTION_CN', '中国法律体系', 'China Legal System', 'jurisdiction', 20,
 '{"icon": "flag", "color": "#C62828"}'),
(v_def_id, 'JURISDICTION_INTERNATIONAL', '国际法律体系', 'International Law', 'jurisdiction', 21,
 '{"icon": "globe", "color": "#0277BD"}');

-- ====== 二、ont_episode_type 初始化 ======

-- 2.1 诉讼程序 (litigation)
INSERT INTO ont_episode_type (definition_id, type_code, type_name, legal_process, stage_label, court_level, is_trial_stage, sort_order, description) VALUES
-- 立案阶段
(v_def_id, 'EP_FILING', '立案', 'litigation', '立案', NULL, FALSE, 1, '原告向法院提交诉状，法院审查受理'),
(v_def_id, 'EP_SERVING', '送达', 'litigation', '立案', NULL, FALSE, 2, '法院向被告送达起诉状副本'),

-- 一审阶段
(v_def_id, 'EP_TRIAL_1ST', '一审庭审', 'litigation', '庭审', '一审', TRUE, 10, '一审法院开庭审理，当事人举证质证'),
(v_def_id, 'EP_JUDGMENT_1ST', '一审判决', 'litigation', '判决', '一审', TRUE, 11, '一审法院作出判决'),
(v_def_id, 'EP_APPEAL', '提起上诉', 'litigation', '上诉', NULL, FALSE, 20, '当事人不服一审判决提起上诉'),

-- 二审阶段
(v_def_id, 'EP_TRIAL_2ND', '二审审理', 'litigation', '庭审', '二审', TRUE, 30, '二审法院审理上诉案件'),
(v_def_id, 'EP_JUDGMENT_2ND', '二审判决', 'litigation', '判决', '二审', TRUE, 31, '二审法院作出终审判决'),

-- 再审
(v_def_id, 'EP_RETRIAL', '再审', 'litigation', '再审', '再审', TRUE, 40, '法院依申请或依职权启动再审程序'),

-- 执行
(v_def_id, 'EP_EXECUTION', '判决执行', 'execution', '执行', NULL, FALSE, 50, '胜诉方向法院申请强制执行');

-- 2.2 调解程序 (mediation) - 扁平化
INSERT INTO ont_episode_type (definition_id, type_code, type_name, legal_process, stage_label, court_level, is_trial_stage, sort_order, description) VALUES
(v_def_id, 'EP_MEDIATION_ACCEPT', '调解受理', 'mediation', '调解启动', NULL, FALSE, 60, '调解机构受理调解申请'),
(v_def_id, 'EP_MEDIATION_NEGOTIATION', '调解协商', 'mediation', '调解进行', NULL, FALSE, 61, '调解员主持当事人协商'),
(v_def_id, 'EP_MEDIATION_AGREEMENT', '调解协议', 'mediation', '调解完成', NULL, FALSE, 62, '双方达成调解协议'),
(v_def_id, 'EP_MEDIATION_CONFIRM', '司法确认', 'mediation', '执行', NULL, FALSE, 63, '调解协议经法院确认获得强制执行力');

-- ====== 三、ont_entity_category 初始化 ======

-- 一级分类
INSERT INTO ont_entity_category (definition_id, category_code, category_name, category_level, parent_category_code, entity_type_scope, sort_order, description) VALUES
(v_def_id, 'CASE', '案件', 1, NULL, '["Case"]', 1, '所有类型案件的公共基类'),
(v_def_id, 'PARTY', '当事人', 1, NULL, '["Party", "LegalPerson", "Lawyer"]', 2, '案件中的自然人、法人及代理人'),
(v_def_id, 'COURT', '司法机构', 1, NULL, '["Court", "Judge"]', 3, '审判机关及审判人员'),
(v_def_id, 'LAW', '法律规范', 1, NULL, '["LegalProvision", "LegalDocument"]', 4, '法律法规条文及文件'),
(v_def_id, 'DOCUMENT', '案件文书', 1, NULL, '["JudgmentDocument", "MediationAgreement"]', 5, '裁判文书及调解文书'),
(v_def_id, 'EVIDENCE', '证据材料', 1, NULL, '["Evidence", "CaseFact"]', 6, '证据及案件事实'),
(v_def_id, 'REASONING', '裁判要旨', 1, NULL, '["CaseReasoning"]', 7, '案例的裁判要旨和指导意义'),
(v_def_id, 'MEDIATION', '调解主体', 1, NULL, '["CommercialMediationOrganization", "Mediator"]', 8, '商事调解组织和调解员');

-- 二级分类 - 案件
INSERT INTO ont_entity_category (definition_id, category_code, category_name, category_level, parent_category_code, entity_type_scope, sort_order) VALUES
(v_def_id, 'CASE_CIVIL', '民事案件', 2, 'CASE', '["CivilCase"]', 11),
(v_def_id, 'CASE_CRIMINAL', '刑事案件', 2, 'CASE', '["CriminalCase"]', 12),
(v_def_id, 'CASE_COMMERCIAL', '商事案件', 2, 'CASE', '["CommercialCase"]', 13),
(v_def_id, 'CASE_ADMIN', '行政案件', 2, 'CASE', '["AdministrativeCase"]', 14),
(v_def_id, 'CASE_EXECUTION', '执行案件', 2, 'CASE', '["ExecutionCase"]', 15),
-- 二级分类 - 当事人
(v_def_id, 'PARTY_NATURAL', '自然人', 2, 'PARTY', '["Party"]', 21),
(v_def_id, 'PARTY_LEGAL', '法人', 2, 'PARTY', '["LegalPerson"]', 22),
(v_def_id, 'PARTY_ATTORNEY', '诉讼代理人', 2, 'PARTY', '["Lawyer"]', 23),
-- 二级分类 - 司法机构
(v_def_id, 'COURT_ORG', '法院', 2, 'COURT', '["Court"]', 31),
(v_def_id, 'JUDGE', '法官', 2, 'COURT', '["Judge"]', 32),
-- 二级分类 - 法律规范
(v_def_id, 'LAW_PROVISION', '法律条文', 2, 'LAW', '["LegalProvision"]', 41),
(v_def_id, 'LAW_DOC', '法律法规文件', 2, 'LAW', '["LegalDocument"]', 42),
-- 二级分类 - 案件文书
(v_def_id, 'DOCUMENT_JUDGMENT', '裁判文书', 2, 'DOCUMENT', '["JudgmentDocument"]', 51),
(v_def_id, 'DOCUMENT_MED_AGREEMENT', '调解协议', 2, 'DOCUMENT', '["MediationAgreement"]', 52),
-- 二级分类 - 证据材料
(v_def_id, 'EVID_EVIDENCE', '证据', 2, 'EVIDENCE', '["Evidence"]', 61),
(v_def_id, 'EVID_FACT', '案件事实', 2, 'EVIDENCE', '["CaseFact"]', 62),
-- 二级分类 - 裁判要旨
(v_def_id, 'REAS_REASONING', '案例裁判要旨', 2, 'REASONING', '["CaseReasoning"]', 71),
-- 二级分类 - 调解主体
(v_def_id, 'MED_ORG', '商事调解组织', 2, 'MEDIATION', '["CommercialMediationOrganization"]', 81),
(v_def_id, 'MEDIATOR', '调解员', 2, 'MEDIATION', '["Mediator"]', 82);

-- ====== 四、ont_relationship_meta 初始化 ======

INSERT INTO ont_relationship_meta
    (definition_id, relationship_type, relationship_name, source_entity_types, target_entity_types,
     is_directional, is_transitive, multiplicity, default_weight, validity_period, description, example_cypher, sort_order)
VALUES
-- HAS_COMMUNITY
(v_def_id, 'HAS_COMMUNITY', '所属社区',
 '["Case", "LegalProvision", "Court", "JudgmentDocument"]', '["Community"]',
 TRUE, FALSE, 'many-to-many', 0.9000,
 '{"hasPeriod": false}',
 '法律实体归入对应社区，便于按法律领域聚合分析',
 'MATCH (c:Case {caseNumber: "（2023）沪01民终11293号"})-[:HAS_COMMUNITY]->(comm:Community {name: "公司解散纠纷"})',
 1),

-- PARENT_OF
(v_def_id, 'PARENT_OF', '父社区',
 '["Community"]', '["Community"]',
 TRUE, FALSE, 'one-to-many', 1.0000,
 '{"hasPeriod": false}',
 '社区层级关系，表达法律领域分类树',
 'MATCH (parent:Community)-[:PARENT_OF]->(child:Community {name: "公司解散纠纷"})',
 2),

-- MENTIONS
(v_def_id, 'MENTIONS', '涉及/提及',
 '["Episode"]', '["Case", "Party", "Court", "LegalProvision", "Evidence", "JudgmentDocument"]',
 TRUE, FALSE, 'many-to-many', 1.0000,
 '{"hasPeriod": false}',
 '事件提及/涉及的关键法律实体，含角色属性',
 'MATCH (ep:Episode)-[:MENTIONS {entity_role: "诉讼标的"}]->(ca:Case)',
 3),

-- NEXT_EPISODE
(v_def_id, 'NEXT_EPISODE', '后续事件',
 '["Episode"]', '["Episode"]',
 TRUE, FALSE, 'one-to-many', 1.0000,
 '{"hasPeriod": false}',
 '法律过程中事件的时序关系，含顺序属性',
 'MATCH (ep1:Episode)-[:NEXT_EPISODE {sequence_order: 3}]->(ep2:Episode)',
 4),

-- CITES
(v_def_id, 'CITES', '引用/依据',
 '["JudgmentDocument", "CaseReasoning"]', '["LegalProvision"]',
 TRUE, FALSE, 'many-to-many', 0.9500,
 '{"hasPeriod": true, "defaultDays": null}',
 '裁判文书引用法条作为判决依据，含依据类型',
 'MATCH (jd:JudgmentDocument)-[:CITES {basisType: "判决依据"}]->(lp:LegalProvision)',
 5),

-- INVOLVES
(v_def_id, 'INVOLVES', '涉及',
 '["Case", "Episode"]', '["Party", "LegalProvision"]',
 TRUE, FALSE, 'many-to-many', 0.8000,
 '{"hasPeriod": false}',
 '案件或事件涉及的当事人或法条，含当事人角色',
 'MATCH (ca:Case)-[:INVOLVES {partyRole: "被告"}]->(p:Party)',
 6),

-- BELONGS_TO
(v_def_id, 'BELONGS_TO', '属于',
 '["LegalProvision"]', '["LegalDocument"]',
 TRUE, FALSE, 'many-to-many', 1.0000,
 '{"hasPeriod": true, "defaultDays": null}',
 '法条属于某法律法规文件，含时效性',
 'MATCH (lp:LegalProvision)-[:BELONGS_TO]->(ld:LegalDocument)',
 7),

-- PRECEDES
(v_def_id, 'PRECEDES', '先例',
 '["Case", "JudgmentDocument"]', '["Case", "JudgmentDocument"]',
 TRUE, TRUE, 'many-to-many', 0.7000,
 '{"hasPeriod": false}',
 '案件或裁判作为后续案件参考的先例，可传递',
 'MATCH (ca1:Case)-[:PRECEDES {citation_count: 15}]->(ca2:Case)',
 8),

-- REPRESENTS
(v_def_id, 'REPRESENTS', '代理',
 '["Lawyer"]', '["Party"]',
 TRUE, FALSE, 'many-to-many', 1.0000,
 '{"hasPeriod": true, "defaultDays": null}',
 '律师代理当事人参与诉讼，含案件号',
 'MATCH (law:Lawyer)-[:REPRESENTS {caseNumber: "（2023）沪01民终11293号"}]->(p:Party)',
 9),

-- PRESIDES_OVER
(v_def_id, 'PRESIDES_OVER', '主持审理',
 '["Judge"]', '["Case"]',
 TRUE, FALSE, 'many-to-many', 1.0000,
 '{"hasPeriod": true, "defaultDays": null}',
 '法官主持案件审理，含审级',
 'MATCH (j:Judge)-[:PRESIDES_OVER {courtLevel: "二审"}]->(ca:Case)',
 10),

-- PARTY_OF
(v_def_id, 'PARTY_OF', '当事人关系',
 '["Party"]', '["Party"]',
 FALSE, FALSE, 'many-to-many', 1.0000,
 '{"hasPeriod": true, "defaultDays": null}',
 '案件中当事人之间的关系（原告/被告/第三人），无向关系',
 'MATCH (p1:Party)-[:PARTY_OF {relationship: "原告", caseNumber: "（2023）"}]->(p2:Party)',
 11),

-- SUBSTANTIATES
(v_def_id, 'SUBSTANTIATES', '证明',
 '["Evidence"]', '["CaseFact"]',
 TRUE, FALSE, 'many-to-many', 0.9000,
 '{"hasPeriod": false}',
 '证据用于证明案件事实，含权重',
 'MATCH (ev:Evidence)-[:SUBSTANTIATES {weight: 0.8}]->(f:CaseFact)',
 12),

-- AFFIRMED_BY
(v_def_id, 'AFFIRMED_BY', '被司法确认',
 '["MediationAgreement"]', '["JudgmentDocument"]',
 FALSE, FALSE, 'one-to-one', 1.0000,
 '{"hasPeriod": true, "defaultDays": null}',
 '调解协议经法院确认获得强制执行力，无向关系',
 'MATCH (ma:MediationAgreement)-[:AFFIRMED_BY]->(jd:JudgmentDocument)',
 13);

-- 五领域通用分类体系初始数据
-- Phase 6: 社区系统通用化改造
-- ============================================================

-- 1. 顶层领域（domain）
INSERT INTO ont_community_type (definition_id, type_code, type_name, category, parent_type_code, sort_order, metadata, status)
VALUES
  (1, 'DOMAIN_ROOT', '知识领域', 'domain', NULL, 0, '{"color": "#607D8B"}', 'ACTIVE'),
  (1, 'DOMAIN_LEGAL', '法律', 'domain', 'DOMAIN_ROOT', 1, '{"color": "#1565C0"}', 'ACTIVE'),
  (1, 'DOMAIN_FINANCE', '金融', 'domain', 'DOMAIN_ROOT', 10, '{"color": "#2E7D32"}', 'ACTIVE'),
  (1, 'DOMAIN_ENTERPRISE', '企业管理', 'domain', 'DOMAIN_ROOT', 20, '{"color": "#E65100"}', 'ACTIVE'),
  (1, 'DOMAIN_MEDICAL', '医疗', 'domain', 'DOMAIN_ROOT', 30, '{"color": "#C62828"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_GOV', '社会治理', 'domain', 'DOMAIN_ROOT', 40, '{"color": "#6A1B9A"}', 'ACTIVE')
ON CONFLICT (definition_id, type_code) DO NOTHING;

-- 2. 法律子领域
INSERT INTO ont_community_type (definition_id, type_code, type_name, category, parent_type_code, sort_order, metadata, status)
VALUES
  (1, 'DOMAIN_CIVIL', '民商事', 'domain', 'DOMAIN_LEGAL', 1, '{"color": "#1976D2"}', 'ACTIVE'),
  (1, 'DOMAIN_CRIMINAL', '刑事法律', 'domain', 'DOMAIN_LEGAL', 2, '{"color": "#D32F2F"}', 'ACTIVE'),
  (1, 'DOMAIN_ADMIN', '行政法律', 'domain', 'DOMAIN_LEGAL', 3, '{"color": "#F57C00"}', 'ACTIVE'),
  (1, 'DOMAIN_IP', '知识产权', 'domain', 'DOMAIN_LEGAL', 4, '{"color": "#7B1FA2"}', 'ACTIVE'),
  (1, 'DOMAIN_LABOR', '劳动法律', 'domain', 'DOMAIN_LEGAL', 5, '{"color": "#388E3C"}', 'ACTIVE')
ON CONFLICT (definition_id, type_code) DO NOTHING;

-- 3. 金融子领域
INSERT INTO ont_community_type (definition_id, type_code, type_name, category, parent_type_code, sort_order, metadata, status)
VALUES
  (1, 'DOMAIN_BANKING', '银行与信贷', 'domain', 'DOMAIN_FINANCE', 1, '{"color": "#1B5E20"}', 'ACTIVE'),
  (1, 'DOMAIN_SECURITIES', '证券与投资', 'domain', 'DOMAIN_FINANCE', 2, '{"color": "#004D40"}', 'ACTIVE'),
  (1, 'DOMAIN_INSURANCE', '保险业务', 'domain', 'DOMAIN_FINANCE', 3, '{"color": "#006064"}', 'ACTIVE'),
  (1, 'DOMAIN_RISK', '风险管控', 'domain', 'DOMAIN_FINANCE', 4, '{"color": "#263238"}', 'ACTIVE')
ON CONFLICT (definition_id, type_code) DO NOTHING;

-- 4. 企业管理子领域
INSERT INTO ont_community_type (definition_id, type_code, type_name, category, parent_type_code, sort_order, metadata, status)
VALUES
  (1, 'DOMAIN_HR', '人力资源', 'domain', 'DOMAIN_ENTERPRISE', 1, '{"color": "#E65100"}', 'ACTIVE'),
  (1, 'DOMAIN_FINANCE_MGMT', '财务管理', 'domain', 'DOMAIN_ENTERPRISE', 2, '{"color": "#BF360C"}', 'ACTIVE'),
  (1, 'DOMAIN_COMPLIANCE', '企业合规', 'domain', 'DOMAIN_ENTERPRISE', 3, '{"color": "#E64A19"}', 'ACTIVE'),
  (1, 'DOMAIN_GOVERNANCE', '公司治理', 'domain', 'DOMAIN_ENTERPRISE', 4, '{"color": "#D84315"}', 'ACTIVE')
ON CONFLICT (definition_id, type_code) DO NOTHING;

-- 5. 医疗子领域
INSERT INTO ont_community_type (definition_id, type_code, type_name, category, parent_type_code, sort_order, metadata, status)
VALUES
  (1, 'DOMAIN_CLINICAL', '临床诊疗', 'domain', 'DOMAIN_MEDICAL', 1, '{"color": "#B71C1C"}', 'ACTIVE'),
  (1, 'DOMAIN_DRUG', '药品与器械', 'domain', 'DOMAIN_MEDICAL', 2, '{"color": "#880E4F"}', 'ACTIVE'),
  (1, 'DOMAIN_PUBLIC_HEALTH', '公共卫生', 'domain', 'DOMAIN_MEDICAL', 3, '{"color": "#4A148C"}', 'ACTIVE')
ON CONFLICT (definition_id, type_code) DO NOTHING;

-- 6. 社会治理一级分类（10 个）
INSERT INTO ont_community_type (definition_id, type_code, type_name, category, parent_type_code, sort_order, metadata, status)
VALUES
  (1, 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE', '婚恋家庭纠纷', 'domain', 'DOMAIN_SOCIAL_GOV', 1, '{"color": "#AD1457"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_LABOR', '劳动人事争议纠纷', 'domain', 'DOMAIN_SOCIAL_GOV', 2, '{"color": "#6A1B9A"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_TORT', '侵权责任纠纷', 'domain', 'DOMAIN_SOCIAL_GOV', 3, '{"color": "#4527A0"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_NEIGHBOR', '邻里关系纠纷', 'domain', 'DOMAIN_SOCIAL_GOV', 4, '{"color": "#283593"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_PROPERTY', '房屋物业纠纷', 'domain', 'DOMAIN_SOCIAL_GOV', 5, '{"color": "#1565C0"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_LAND', '山林土地水利纠纷', 'domain', 'DOMAIN_SOCIAL_GOV', 6, '{"color": "#00838F"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_CONSUMER', '消费服务纠纷', 'domain', 'DOMAIN_SOCIAL_GOV', 7, '{"color": "#00695C"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_ECONOMIC', '经济金融活动纠纷', 'domain', 'DOMAIN_SOCIAL_GOV', 8, '{"color": "#2E7D32"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION', '行政纠纷与信访维权', 'domain', 'DOMAIN_SOCIAL_GOV', 9, '{"color": "#558B2F"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_CONSULT_SERVICE', '咨询与公证服务', 'domain', 'DOMAIN_SOCIAL_GOV', 10, '{"color": "#F9A825"}', 'ACTIVE')
ON CONFLICT (definition_id, type_code) DO NOTHING;

-- 7. 婚恋家庭纠纷二级分类（11 项）
INSERT INTO ont_community_type (definition_id, type_code, type_name, category, parent_type_code, sort_order, metadata, status)
VALUES
  (1, 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE_01', '夫妻关系矛盾纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE', 1, '{"color": "#F48FB1"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE_02', '离异夫妻矛盾纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE', 2, '{"color": "#F06292"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE_03', '未婚恋爱纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE', 3, '{"color": "#EC407A"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE_04', '同居关系纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE', 4, '{"color": "#E91E63"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE_05', '分家、继承与赡养纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE', 5, '{"color": "#D81B60"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE_06', '父母子女矛盾纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE', 6, '{"color": "#C2185B"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE_07', '兄弟姐妹矛盾纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE', 7, '{"color": "#AD1457"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE_08', '家庭其它成员矛盾纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE', 8, '{"color": "#880E4F"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE_09', '婚姻自主权纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE', 9, '{"color": "#7B1FA2"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE_10', '宣告失踪、死亡纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE', 10, '{"color": "#6A1B9A"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE_11', '认定无民事行为能力纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_MARRIAGE', 11, '{"color": "#4A148C"}', 'ACTIVE')
ON CONFLICT (definition_id, type_code) DO NOTHING;

-- 8. 劳动人事争议纠纷二级分类（8 项）
INSERT INTO ont_community_type (definition_id, type_code, type_name, category, parent_type_code, sort_order, metadata, status)
VALUES
  (1, 'DOMAIN_SOCIAL_DISPUTE_LABOR_01', '劳动报酬追索纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_LABOR', 1, '{"color": "#CE93D8"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_LABOR_02', '经济补偿与赔偿纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_LABOR', 2, '{"color": "#BA68C8"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_LABOR_03', '福利待遇纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_LABOR', 3, '{"color": "#AB47BC"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_LABOR_04', '招聘录用纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_LABOR', 4, '{"color": "#9C27B0"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_LABOR_05', '人事任免纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_LABOR', 5, '{"color": "#8E24AA"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_LABOR_06', '劳动合同纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_LABOR', 6, '{"color": "#7B1FA2"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_LABOR_07', '临时用工纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_LABOR', 7, '{"color": "#6A1B9A"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_LABOR_08', '竞业限制纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_LABOR', 8, '{"color": "#4A148C"}', 'ACTIVE')
ON CONFLICT (definition_id, type_code) DO NOTHING;

-- 9. 侵权责任纠纷二级分类（18 项）
INSERT INTO ont_community_type (definition_id, type_code, type_name, category, parent_type_code, sort_order, metadata, status)
VALUES
  (1, 'DOMAIN_SOCIAL_DISPUTE_TORT_01', '医疗医美损害责任纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_TORT', 1, '{"color": "#B39DDB"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_TORT_02', '人身安全与健康权纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_TORT', 2, '{"color": "#9575CD"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_TORT_03', '姓名权、肖像权、声音权纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_TORT', 3, '{"color": "#7E57C2"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_TORT_04', '名誉权、荣誉权纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_TORT', 4, '{"color": "#673AB7"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_TORT_05', '隐私和个人信息保护纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_TORT', 5, '{"color": "#5E35B1"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_TORT_06', '财物返还及损害赔偿纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_TORT', 6, '{"color": "#512DA8"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_TORT_07', '网络侵权纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_TORT', 7, '{"color": "#4527A0"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_TORT_08', '群众性活动纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_TORT', 8, '{"color": "#311B92"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_TORT_09', '学校及教育机构纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_TORT', 9, '{"color": "#283593"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_TORT_10', '交通事故责任纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_TORT', 10, '{"color": "#1A237E"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_TORT_11', '医疗事故责任纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_TORT', 11, '{"color": "#1565C0"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_TORT_12', '性骚扰损害责任纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_TORT', 12, '{"color": "#0D47A1"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_TORT_13', '环境与生态环境责任纠纷及公益诉讼', 'domain', 'DOMAIN_SOCIAL_DISPUTE_TORT', 13, '{"color": "#0277BD"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_TORT_14', '食品药品安全责任纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_TORT', 14, '{"color": "#00838F"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_TORT_15', '饲养动物损害责任纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_TORT', 15, '{"color": "#00695C"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_TORT_16', '国家赔偿纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_TORT', 16, '{"color": "#2E7D32"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_TORT_17', '口角琐事纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_TORT', 17, '{"color": "#558B2F"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_TORT_18', '知识产权纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_TORT', 18, '{"color": "#827717"}', 'ACTIVE')
ON CONFLICT (definition_id, type_code) DO NOTHING;

-- 10. 邻里关系纠纷二级分类（5 项）
INSERT INTO ont_community_type (definition_id, type_code, type_name, category, parent_type_code, sort_order, metadata, status)
VALUES
  (1, 'DOMAIN_SOCIAL_DISPUTE_NEIGHBOR_01', '相邻用水、排水、通行、通风、采光纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_NEIGHBOR', 1, '{"color": "#80DEEA"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_NEIGHBOR_02', '相邻土地利用与建筑物纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_NEIGHBOR', 2, '{"color": "#4DD0E1"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_NEIGHBOR_03', '相邻污染损害防免纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_NEIGHBOR', 3, '{"color": "#26C6DA"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_NEIGHBOR_04', '高空抛物责任纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_NEIGHBOR', 4, '{"color": "#00BCD4"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_NEIGHBOR_05', '邻里口角琐事纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_NEIGHBOR', 5, '{"color": "#00ACC1"}', 'ACTIVE')
ON CONFLICT (definition_id, type_code) DO NOTHING;

-- 11. 房屋物业纠纷二级分类（7 项）
INSERT INTO ont_community_type (definition_id, type_code, type_name, category, parent_type_code, sort_order, metadata, status)
VALUES
  (1, 'DOMAIN_SOCIAL_DISPUTE_PROPERTY_01', '物业管理纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_PROPERTY', 1, '{"color": "#4FC3F7"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_PROPERTY_02', '业主与业委会纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_PROPERTY', 2, '{"color": "#29B6F6"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_PROPERTY_03', '不动产登记纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_PROPERTY', 3, '{"color": "#03A9F4"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_PROPERTY_04', '车位车库使用权纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_PROPERTY', 4, '{"color": "#039BE5"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_PROPERTY_05', '居住权纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_PROPERTY', 5, '{"color": "#0288D1"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_PROPERTY_06', '房屋买卖与租赁纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_PROPERTY', 6, '{"color": "#0277BD"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_PROPERTY_07', '建筑质量损害纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_PROPERTY', 7, '{"color": "#01579B"}', 'ACTIVE')
ON CONFLICT (definition_id, type_code) DO NOTHING;

-- 12. 山林土地水利纠纷二级分类（6 项）
INSERT INTO ont_community_type (definition_id, type_code, type_name, category, parent_type_code, sort_order, metadata, status)
VALUES
  (1, 'DOMAIN_SOCIAL_DISPUTE_LAND_01', '土地承包经营权纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_LAND', 1, '{"color": "#AED581"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_LAND_02', '宅基地使用权纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_LAND', 2, '{"color": "#9CCC65"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_LAND_03', '取水、养殖、捕捞权纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_LAND', 3, '{"color": "#8BC34A"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_LAND_04', '建设用地使用权纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_LAND', 4, '{"color": "#7CB342"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_LAND_05', '探矿权、采矿权纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_LAND', 5, '{"color": "#689F38"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_LAND_06', '侵害集体经济组织权益纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_LAND', 6, '{"color": "#558B2F"}', 'ACTIVE')
ON CONFLICT (definition_id, type_code) DO NOTHING;

-- 13. 消费服务纠纷二级分类（12 项）
INSERT INTO ont_community_type (definition_id, type_code, type_name, category, parent_type_code, sort_order, metadata, status)
VALUES
  (1, 'DOMAIN_SOCIAL_DISPUTE_CONSUMER_01', '商品买卖与质量纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_CONSUMER', 1, '{"color": "#FFCC80"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_CONSUMER_02', '交通出行服务纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_CONSUMER', 2, '{"color": "#FFB74D"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_CONSUMER_03', '住宿餐饮服务纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_CONSUMER', 3, '{"color": "#FFA726"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_CONSUMER_04', '邮政快递与跑腿服务纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_CONSUMER', 4, '{"color": "#FF9800"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_CONSUMER_05', '通信与网络服务纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_CONSUMER', 5, '{"color": "#FB8C00"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_CONSUMER_06', '公用事业服务纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_CONSUMER', 6, '{"color": "#F57C00"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_CONSUMER_07', '旅游服务纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_CONSUMER', 7, '{"color": "#EF6C00"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_CONSUMER_08', '家政服务纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_CONSUMER', 8, '{"color": "#E65100"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_CONSUMER_09', '养老服务纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_CONSUMER', 9, '{"color": "#BF360C"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_CONSUMER_10', '美容保健服务纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_CONSUMER', 10, '{"color": "#D84315"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_CONSUMER_11', '培训服务纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_CONSUMER', 11, '{"color": "#BF360C"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_CONSUMER_12', '房地产服务纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_CONSUMER', 12, '{"color": "#E64A19"}', 'ACTIVE')
ON CONFLICT (definition_id, type_code) DO NOTHING;

-- 14. 经济金融活动纠纷二级分类（12 项）
INSERT INTO ont_community_type (definition_id, type_code, type_name, category, parent_type_code, sort_order, metadata, status)
VALUES
  (1, 'DOMAIN_SOCIAL_DISPUTE_ECONOMIC_01', '借贷担保纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_ECONOMIC', 1, '{"color": "#A5D6A7"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_ECONOMIC_02', '储蓄存款纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_ECONOMIC', 2, '{"color": "#81C784"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_ECONOMIC_03', '投资、信托理财纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_ECONOMIC', 3, '{"color": "#66BB6A"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_ECONOMIC_04', '证券、基金、期货纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_ECONOMIC', 4, '{"color": "#4CAF50"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_ECONOMIC_05', '保险理赔纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_ECONOMIC', 5, '{"color": "#43A047"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_ECONOMIC_06', '票据与信用证纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_ECONOMIC', 6, '{"color": "#388E3C"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_ECONOMIC_07', '政府类债务纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_ECONOMIC', 7, '{"color": "#2E7D32"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_ECONOMIC_08', '非法融资纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_ECONOMIC', 8, '{"color": "#1B5E20"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_ECONOMIC_09', '公司企业生产经营纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_ECONOMIC', 9, '{"color": "#1B5E20"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_ECONOMIC_10', '拖欠企业账款纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_ECONOMIC', 10, '{"color": "#2E7D32"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_ECONOMIC_11', '房地产纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_ECONOMIC', 11, '{"color": "#388E3C"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_DISPUTE_ECONOMIC_12', '涉众经济金融纠纷', 'domain', 'DOMAIN_SOCIAL_DISPUTE_ECONOMIC', 12, '{"color": "#43A047"}', 'ACTIVE')
ON CONFLICT (definition_id, type_code) DO NOTHING;

-- 15. 行政纠纷与信访维权二级分类（26 项）
INSERT INTO ont_community_type (definition_id, type_code, type_name, category, parent_type_code, sort_order, metadata, status)
VALUES
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION_01', '公安治安管理纠纷', 'domain', 'DOMAIN_SOCIAL_ADMIN_PETITION', 1, '{"color": "#C5E1A5"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION_02', '道路交通管理纠纷', 'domain', 'DOMAIN_SOCIAL_ADMIN_PETITION', 2, '{"color": "#AED581"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION_03', '劳动和社会保障行政管理纠纷', 'domain', 'DOMAIN_SOCIAL_ADMIN_PETITION', 3, '{"color": "#9CCC65"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION_04', '民政行政管理纠纷', 'domain', 'DOMAIN_SOCIAL_ADMIN_PETITION', 4, '{"color": "#8BC34A"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION_05', '工商行政管理纠纷', 'domain', 'DOMAIN_SOCIAL_ADMIN_PETITION', 5, '{"color": "#7CB342"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION_06', '规划、拆迁、房屋登记等城乡建设行政管理纠纷', 'domain', 'DOMAIN_SOCIAL_ADMIN_PETITION', 6, '{"color": "#689F38"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION_07', '教育行政管理纠纷', 'domain', 'DOMAIN_SOCIAL_ADMIN_PETITION', 7, '{"color": "#558B2F"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION_08', '卫生行政管理纠纷', 'domain', 'DOMAIN_SOCIAL_ADMIN_PETITION', 8, '{"color": "#33691E"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION_09', '食品药品安全行政管理纠纷', 'domain', 'DOMAIN_SOCIAL_ADMIN_PETITION', 9, '{"color": "#827717"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION_10', '税务行政管理纠纷', 'domain', 'DOMAIN_SOCIAL_ADMIN_PETITION', 10, '{"color": "#F9A825"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION_11', '环境保护行政管理纠纷', 'domain', 'DOMAIN_SOCIAL_ADMIN_PETITION', 11, '{"color": "#F57F17"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION_12', '金融行政管理纠纷', 'domain', 'DOMAIN_SOCIAL_ADMIN_PETITION', 12, '{"color": "#FFD600"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION_13', '海关行政管理纠纷', 'domain', 'DOMAIN_SOCIAL_ADMIN_PETITION', 13, '{"color": "#FFEA00"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION_14', '乡政府管理', 'domain', 'DOMAIN_SOCIAL_ADMIN_PETITION', 14, '{"color": "#FFC400"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION_15', '村（社区、居）务管理纠纷', 'domain', 'DOMAIN_SOCIAL_ADMIN_PETITION', 15, '{"color": "#FFB300"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION_16', '行政复议纠纷', 'domain', 'DOMAIN_SOCIAL_ADMIN_PETITION', 16, '{"color": "#FFA000"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION_17', '纪检监察举报申诉', 'domain', 'DOMAIN_SOCIAL_ADMIN_PETITION', 17, '{"color": "#FF8F00"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION_18', '综合行政执法举报投诉', 'domain', 'DOMAIN_SOCIAL_ADMIN_PETITION', 18, '{"color": "#FF6F00"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION_19', '市场监督执法举报投诉', 'domain', 'DOMAIN_SOCIAL_ADMIN_PETITION', 19, '{"color": "#FF6F00"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION_20', '涉诉涉法举报申诉', 'domain', 'DOMAIN_SOCIAL_ADMIN_PETITION', 20, '{"color": "#E65100"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION_21', '其他投诉举报', 'domain', 'DOMAIN_SOCIAL_ADMIN_PETITION', 21, '{"color": "#BF360C"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION_22', '检举控告类事项', 'domain', 'DOMAIN_SOCIAL_ADMIN_PETITION', 22, '{"color": "#D84315"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION_23', '建议意见类事项', 'domain', 'DOMAIN_SOCIAL_ADMIN_PETITION', 23, '{"color": "#BF360C"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION_24', '申诉求决类事项', 'domain', 'DOMAIN_SOCIAL_ADMIN_PETITION', 24, '{"color": "#BF360C"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION_25', '涉法涉诉信访', 'domain', 'DOMAIN_SOCIAL_ADMIN_PETITION', 25, '{"color": "#E65100"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_ADMIN_PETITION_26', '涉军维权事项', 'domain', 'DOMAIN_SOCIAL_ADMIN_PETITION', 26, '{"color": "#BF360C"}', 'ACTIVE')
ON CONFLICT (definition_id, type_code) DO NOTHING;

-- 16. 咨询与公证服务二级分类（12 项）
INSERT INTO ont_community_type (definition_id, type_code, type_name, category, parent_type_code, sort_order, metadata, status)
VALUES
  (1, 'DOMAIN_SOCIAL_CONSULT_LEGAL', '法律咨询', 'domain', 'DOMAIN_SOCIAL_CONSULT_SERVICE', 1, '{"color": "#FFF9C4"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_CONSULT_PSYCH', '心理咨询', 'domain', 'DOMAIN_SOCIAL_CONSULT_SERVICE', 2, '{"color": "#FFF59D"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_CONSULT_PETITION', '信访咨询', 'domain', 'DOMAIN_SOCIAL_CONSULT_SERVICE', 3, '{"color": "#FFF176"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_CONSULT_POLICE', '涉警咨询', 'domain', 'DOMAIN_SOCIAL_CONSULT_SERVICE', 4, '{"color": "#FFEE58"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_CONSULT_NOTARY', '公证咨询', 'domain', 'DOMAIN_SOCIAL_CONSULT_SERVICE', 5, '{"color": "#FFD54F"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_CONSULT_LEGAL_AID', '法律援助咨询', 'domain', 'DOMAIN_SOCIAL_CONSULT_SERVICE', 6, '{"color": "#FFCA28"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_SERVICE_NOTARY', '公证服务', 'domain', 'DOMAIN_SOCIAL_CONSULT_SERVICE', 7, '{"color": "#FFC107"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_SERVICE_APPRAISAL', '司法鉴定', 'domain', 'DOMAIN_SOCIAL_CONSULT_SERVICE', 8, '{"color": "#FFB300"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_SERVICE_LEGAL_AID', '法律援助', 'domain', 'DOMAIN_SOCIAL_CONSULT_SERVICE', 9, '{"color": "#FFA000"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_SERVICE_WORK_INJURY', '工伤认定', 'domain', 'DOMAIN_SOCIAL_CONSULT_SERVICE', 10, '{"color": "#FF8F00"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_CONSULT_OTHER', '其他咨询与服务', 'domain', 'DOMAIN_SOCIAL_CONSULT_SERVICE', 11, '{"color": "#FF6F00"}', 'ACTIVE'),
  (1, 'DOMAIN_SOCIAL_SUGGESTION', '意见建议', 'domain', 'DOMAIN_SOCIAL_CONSULT_SERVICE', 12, '{"color": "#E65100"}', 'ACTIVE')
ON CONFLICT (definition_id, type_code) DO NOTHING;

-- 17. 区域分类（region）
INSERT INTO ont_community_type (definition_id, type_code, type_name, category, parent_type_code, sort_order, metadata, status)
VALUES
  (1, 'REGION_ROOT', '全球/通用', 'region', NULL, 0, '{"color": "#9E9E9E"}', 'ACTIVE'),
  (1, 'REGION_CN', '中国', 'region', NULL, 1, '{"color": "#D32F2F"}', 'ACTIVE'),
  (1, 'REGION_US', '美国', 'region', NULL, 2, '{"color": "#1565C0"}', 'ACTIVE'),
  (1, 'REGION_EU', '欧洲', 'region', NULL, 3, '{"color": "#1976D2"}', 'ACTIVE'),
  (1, 'REGION_INTERNATIONAL', '国际', 'region', NULL, 4, '{"color": "#7B1FA2"}', 'ACTIVE')
ON CONFLICT (definition_id, type_code) DO NOTHING;

-- 18. 场景分类（scenario）
INSERT INTO ont_community_type (definition_id, type_code, type_name, category, parent_type_code, sort_order, metadata, status)
VALUES
  (1, 'SCENARIO_ROOT', '通用场景', 'scenario', NULL, 0, '{"color": "#607D8B"}', 'ACTIVE'),
  (1, 'SCENARIO_JUDICIAL', '司法实践', 'scenario', NULL, 1, '{"color": "#1565C0"}', 'ACTIVE'),
  (1, 'SCENARIO_COMPLIANCE', '合规管理', 'scenario', NULL, 2, '{"color": "#2E7D32"}', 'ACTIVE'),
  (1, 'SCENARIO_RISK', '风险管控', 'scenario', NULL, 3, '{"color": "#E65100"}', 'ACTIVE'),
  (1, 'SCENARIO_LIFECYCLE', '生命周期', 'scenario', NULL, 4, '{"color": "#C62828"}', 'ACTIVE'),
  (1, 'SCENARIO_LAW_REGULATE', '依法调解', 'scenario', NULL, 5, '{"color": "#6A1B9A"}', 'ACTIVE'),
  (1, 'SCENARIO_FEEDBACK', '反馈处置', 'scenario', NULL, 6, '{"color": "#00838F"}', 'ACTIVE'),
  (1, 'SCENARIO_GOVERNANCE', '综合治理', 'scenario', NULL, 7, '{"color": "#558B2F"}', 'ACTIVE'),
  (1, 'SCENARIO_PREVENTION', '预防预警', 'scenario', NULL, 8, '{"color": "#F9A825"}', 'ACTIVE')
ON CONFLICT (definition_id, type_code) DO NOTHING;

RAISE NOTICE 'V3.0.0 元数据初始化完成: ont_community_type, ont_episode_type, ont_entity_category, ont_relationship_meta';

END $$;

-- ============================================================
-- Episode 类型初始数据（通用 + 社会治理 + 法律）
-- Phase 7: 社区系统通用化改造
-- ============================================================

-- 1. 通用流程类型（lifecycle/workflow，跨领域适用）
INSERT INTO ont_episode_type (definition_id, type_code, type_name, process_type, stage_label, stage_level, is_review_stage, sort_order, metadata, status)
VALUES
  (1, 'EP_INITIATION', '发起/启动', 'lifecycle', '启动', NULL, FALSE, 1, '{"color": "#4CAF50"}', 'ACTIVE'),
  (1, 'EP_EVALUATION', '评估/审查', 'lifecycle', '审查', NULL, TRUE, 2, '{"color": "#FF9800"}', 'ACTIVE'),
  (1, 'EP_EXECUTION', '执行/实施', 'lifecycle', '执行', NULL, FALSE, 3, '{"color": "#2196F3"}', 'ACTIVE'),
  (1, 'EP_RESOLUTION', '解决/终结', 'lifecycle', '终结', NULL, FALSE, 4, '{"color": "#9C27B0"}', 'ACTIVE'),
  (1, 'EP_WORKFLOW_START', '流程启动', 'workflow', '启动', NULL, FALSE, 10, '{"color": "#00BCD4"}', 'ACTIVE'),
  (1, 'EP_WORKFLOW_NODE', '流程节点', 'workflow', '流转', NULL, FALSE, 11, '{"color": "#3F51B5"}', 'ACTIVE'),
  (1, 'EP_WORKFLOW_END', '流程结束', 'workflow', '结束', NULL, FALSE, 12, '{"color": "#795548"}', 'ACTIVE')
ON CONFLICT (definition_id, type_code) DO NOTHING;

-- 2. 社会治理领域 Episode 类型
INSERT INTO ont_episode_type (definition_id, type_code, type_name, process_type, stage_label, stage_level, is_review_stage, sort_order, metadata, status)
VALUES
  (1, 'EP_REPORT_RECEIVE', '事件接收', 'lifecycle', '接收', NULL, FALSE, 20, '{"color": "#E91E63"}', 'ACTIVE'),
  (1, 'EP_TRIAGE_ASSESS', '事件分流评估', 'workflow', '评估', NULL, TRUE, 21, '{"color": "#FF5722"}', 'ACTIVE'),
  (1, 'EP_MEDIATION', '调解处理', 'workflow', '调解', NULL, FALSE, 22, '{"color": "#9C27B0"}', 'ACTIVE'),
  (1, 'EP_COORDINATION', '协调处置', 'workflow', '协调', NULL, FALSE, 23, '{"color": "#673AB7"}', 'ACTIVE'),
  (1, 'EP_FEEDBACK', '结果反馈', 'lifecycle', '反馈', NULL, FALSE, 24, '{"color": "#2196F3"}', 'ACTIVE'),
  (1, 'EP_FOLLOW_UP', '跟踪回访', 'lifecycle', '回访', NULL, FALSE, 25, '{"color": "#00BCD4"}', 'ACTIVE'),
  (1, 'EP_CLOSE', '事件办结', 'lifecycle', '办结', NULL, FALSE, 26, '{"color": "#4CAF50"}', 'ACTIVE')
ON CONFLICT (definition_id, type_code) DO NOTHING;

-- 3. 法律领域 Episode 类型（带 stage_level，体现领域特色）
INSERT INTO ont_episode_type (definition_id, type_code, type_name, process_type, stage_label, stage_level, is_review_stage, sort_order, metadata, status)
VALUES
  (1, 'EP_FILING', '立案', 'business_process', '立案', NULL, FALSE, 30, '{"color": "#1976D2"}', 'ACTIVE'),
  (1, 'EP_TRIAL_1ST', '一审庭审', 'business_process', '庭审', '一审', TRUE, 31, '{"color": "#F57C00"}', 'ACTIVE'),
  (1, 'EP_JUDGMENT_1ST', '一审判决', 'business_process', '判决', '一审', TRUE, 32, '{"color": "#D32F2F"}', 'ACTIVE'),
  (1, 'EP_APPEAL', '上诉', 'business_process', '上诉', '二审', TRUE, 33, '{"color": "#7B1FA2"}', 'ACTIVE'),
  (1, 'EP_TRIAL_2ND', '二审庭审', 'business_process', '庭审', '二审', TRUE, 34, '{"color": "#F57C00"}', 'ACTIVE'),
  (1, 'EP_JUDGMENT_2ND', '二审判决', 'business_process', '判决', '二审', TRUE, 35, '{"color": "#D32F2F"}', 'ACTIVE'),
  (1, 'EP_EXECUTION_LEGAL', '执行', 'business_process', '执行', NULL, FALSE, 36, '{"color": "#388E3C"}', 'ACTIVE')
ON CONFLICT (definition_id, type_code) DO NOTHING;
