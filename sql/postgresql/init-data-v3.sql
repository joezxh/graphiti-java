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

RAISE NOTICE 'V3.0.0 元数据初始化完成: ont_community_type, ont_episode_type, ont_entity_category, ont_relationship_meta';

END $$;
