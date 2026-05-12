-- ============================================================
-- 法律知识图谱 V2 本体定义数据 (PostgreSQL)
-- 版本: V6
-- 描述: 基于典型案例与商事调解条例设计的法律领域本体 V2
--       包含：核心实体类、属性、约束、关系类型定义
-- 数据来源: 人民法院案例库 + 商事调解条例(2026)
-- 创建时间: 2026-05-12
-- 说明: 请先执行 V5__create_ontology_tables.sql 创建表结构
-- ============================================================

-- ----------------------------------------------------------
-- Step 1: 插入本体定义（获取 definition_id）
-- ----------------------------------------------------------
-- 使用 CTE 插入或更新定义，然后通过 RETURNING 获取 ID
-- 执行两次：第一次 INSERT，第二次 UPDATE + RETURNING（幂等执行）

WITH def_ins AS (
    INSERT INTO ont_definition (graph_id, namespace, name, version, status, description, created_by, created_at, updated_at)
    VALUES (
        'legal-knowledge-graph',
        'http://legal-ai.cc/ontology',
        '法律知识图谱本体 V2',
        '2.0.0',
        'ACTIVE',
        '基于典型类案（裁判文书）与法律条文设计的法律领域本体，涵盖民商事、行政、刑事案件类型，支持案例推理与法条引用分析。核心实体包括：案件、法院、法官、当事人、法律条文、裁判文书、律师、证据、商事调解组织、调解员、调解协议。',
        'system',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    )
    ON CONFLICT ON CONSTRAINT uk_ont_def_graph_version DO UPDATE SET
        description = EXCLUDED.description,
        updated_at = CURRENT_TIMESTAMP
    RETURNING id AS v_def_id
)
SELECT 'Step 1: Definition inserted/updated, ID = ' || v_def_id FROM def_ins;

-- 获取刚插入的本体定义 ID（供后续使用）
DO $$
DECLARE
    v_def_id BIGINT;
BEGIN
    SELECT id INTO v_def_id FROM ont_definition
    WHERE graph_id = 'legal-knowledge-graph' AND version = '2.0.0';

    IF v_def_id IS NULL THEN
        RAISE EXCEPTION 'Failed to get definition ID for legal-knowledge-graph V2.0.0';
    END IF;

    RAISE NOTICE 'Using definition_id: %', v_def_id;

    -- ================================================================
    -- Step 2: 插入本体类 (ont_class)
    -- ================================================================

    -- ================== 案件域实体 ==================

    INSERT INTO ont_class (definition_id, class_uri, local_name, parent_class_id, description, example, domain_hint, metadata, created_at, updated_at)
    VALUES
    -- [1] Case / 案件（根节点）
    (v_def_id, 'http://legal-ai.cc/ontology/Case', 'Case', NULL,
        '案件基类，涵盖所有类型案件的公共属性。一个案件代表一起法律纠纷或诉讼事件，是法律知识图谱的核心实体。',
        '{"caseNumber": "（2023）沪01民终11293号", "caseName": "徐某骥诉上海某物业管理有限公司等公司解散纠纷案"}',
        'KNOWLEDGE',
        '{"icon": "case", "color": "#E3F2FD", "displayPriority": 1}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    -- [2] CommercialCase / 商事案件
    (v_def_id, 'http://legal-ai.cc/ontology/CommercialCase', 'CommercialCase', NULL,
        '商事案件，适用《商事调解条例》范围内的案件类型，包括贸易、投资、金融、运输、房地产、工程建设、知识产权等领域的商事争议案件。',
        '{"caseNumber": "（2023）沪01民终11293号", "caseName": "公司解散纠纷案", "disputeType": "公司解散"}',
        'KNOWLEDGE',
        '{"icon": "commercial", "color": "#E8F5E9", "displayPriority": 2}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    -- [3] CivilCase / 民事案件
    (v_def_id, 'http://legal-ai.cc/ontology/CivilCase', 'CivilCase', NULL,
        '民事案件，包括婚姻家庭纠纷、继承纠纷、合同纠纷、侵权纠纷、物权纠纷、人格权纠纷等传统民事领域案件。',
        '{"caseNumber": "（2020）渝民辖188号", "caseName": "谭某诉吴某债权人撤销权纠纷案"}',
        'KNOWLEDGE',
        '{"icon": "civil", "color": "#FFF3E0", "displayPriority": 3}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    -- [4] CriminalCase / 刑事案件
    (v_def_id, 'http://legal-ai.cc/ontology/CriminalCase', 'CriminalCase', NULL,
        '刑事案件，包括公诉案件和自诉案件，涉及刑法规定的各类犯罪行为。',
        '{"caseNumber": "（2022）沪01刑初123号", "caseName": "徐某故意杀人案"}',
        'KNOWLEDGE',
        '{"icon": "criminal", "color": "#FFEBEE", "displayPriority": 4}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    -- [5] AdministrativeCase / 行政案件
    (v_def_id, 'http://legal-ai.cc/ontology/AdministrativeCase', 'AdministrativeCase', NULL,
        '行政案件，包括行政处罚、行政许可、行政强制、行政裁决、行政复议等行政法律关系引发的争议。',
        '{"caseNumber": "（2023）京01行初456号", "caseName": "某公司诉某税务局纳税信用评价案"}',
        'KNOWLEDGE',
        '{"icon": "admin", "color": "#F3E5F5", "displayPriority": 5}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    -- [6] ExecutionCase / 执行案件
    (v_def_id, 'http://legal-ai.cc/ontology/ExecutionCase', 'ExecutionCase', NULL,
        '执行案件，包括民事执行、行政执行、刑事执行等强制执行程序案件。',
        '{"caseNumber": "（2023）沪01执789号", "caseName": "某公司申请执行某公司合同纠纷案"}',
        'KNOWLEDGE',
        '{"icon": "execution", "color": "#ECEFF1", "displayPriority": 6}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
    ON CONFLICT ON CONSTRAINT uk_ont_class_uri DO UPDATE SET
        description = EXCLUDED.description,
        metadata = EXCLUDED.metadata,
        updated_at = CURRENT_TIMESTAMP;

    -- ================== 当事人与代理人实体 ==================

    INSERT INTO ont_class (definition_id, class_uri, local_name, parent_class_id, description, example, domain_hint, metadata, created_at, updated_at)
    VALUES
    -- [7] Party / 当事人
    (v_def_id, 'http://legal-ai.cc/ontology/Party', 'Party', NULL,
        '案件中的当事人，包括自然人、法人和非法人组织。',
        '{"partyName": "徐某骥", "partyType": "自然人", "partyRole": "原告", "idNumber": "310101199001011234"}',
        'KNOWLEDGE',
        '{"icon": "person", "color": "#E1F5FE", "displayPriority": 10}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    -- [8] LegalPerson / 法人当事人
    (v_def_id, 'http://legal-ai.cc/ontology/LegalPerson', 'LegalPerson', NULL,
        '法人当事人，包括有限责任公司、股份有限公司、国有企业、民营企业、外资企业、合伙企业等具有法人资格的组织。',
        '{"partyName": "上海某物业管理有限公司", "partyType": "法人", "unifiedSocialCreditCode": "91310000MA1F12345X"}',
        'KNOWLEDGE',
        '{"icon": "organization", "color": "#E8EAF6", "displayPriority": 11}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    -- [9] Lawyer / 律师
    (v_def_id, 'http://legal-ai.cc/ontology/Lawyer', 'Lawyer', NULL,
        '执业律师，为案件当事人提供法律服务的专业人员。',
        '{"partyName": "陈某律师", "licenseNumber": "311011993001011234", "firmName": "上海某律师事务所", "specialty": "公司法"}',
        'KNOWLEDGE',
        '{"icon": "lawyer", "color": "#E0F2F1", "displayPriority": 12}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
    ON CONFLICT ON CONSTRAINT uk_ont_class_uri DO UPDATE SET
        description = EXCLUDED.description,
        metadata = EXCLUDED.metadata,
        updated_at = CURRENT_TIMESTAMP;

    -- ================== 司法机构实体 ==================

    INSERT INTO ont_class (definition_id, class_uri, local_name, parent_class_id, description, example, domain_hint, metadata, created_at, updated_at)
    VALUES
    -- [10] Court / 法院
    (v_def_id, 'http://legal-ai.cc/ontology/Court', 'Court', NULL,
        '审判机关，包括最高人民法院、高级人民法院、中级人民法院、基层人民法院以及专门法院。',
        '{"courtName": "上海市第一中级人民法院", "courtLevel": "中级人民法院", "location": "上海市"}',
        'KNOWLEDGE',
        '{"icon": "court", "color": "#FCE4EC", "displayPriority": 20}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    -- [11] Judge / 法官
    (v_def_id, 'http://legal-ai.cc/ontology/Judge', 'Judge', NULL,
        '案件审判人员，包括审判长、审判员、人民陪审员、书记员等。',
        '{"judgeName": "张某法官", "judgeTitle": "审判长", "courtName": "上海市第一中级人民法院"}',
        'KNOWLEDGE',
        '{"icon": "judge", "color": "#FFF8E1", "displayPriority": 21}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
    ON CONFLICT ON CONSTRAINT uk_ont_class_uri DO UPDATE SET
        description = EXCLUDED.description,
        metadata = EXCLUDED.metadata,
        updated_at = CURRENT_TIMESTAMP;

    -- ================== 法律条文实体 ==================

    INSERT INTO ont_class (definition_id, class_uri, local_name, parent_class_id, description, example, domain_hint, metadata, created_at, updated_at)
    VALUES
    -- [12] LegalProvision / 法律条文
    (v_def_id, 'http://legal-ai.cc/ontology/LegalProvision', 'LegalProvision', NULL,
        '法律、行政法规、司法解释、部门规章、地方性法规等规范性法律文件的条文。是法律知识图谱的核心关联实体，用于案件裁判的法条引用分析。',
        '{"provisionId": "L001", "articleNumber": "第69条", "provisionContent": "法人清算后的剩余财产，按照法人章程的规定或者法人权力机构的决议处理。", "lawName": "中华人民共和国民法典", "lawType": "法律", "effectiveDate": "2021-01-01"}',
        'KNOWLEDGE',
        '{"icon": "law", "color": "#E8EAF6", "displayPriority": 30}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    -- [13] LegalDocument / 法规文件
    (v_def_id, 'http://legal-ai.cc/ontology/LegalDocument', 'LegalDocument', NULL,
        '完整的法律法规文件，用于组织法律条文。包含法律名称、法律类型、制定机关、生效日期等信息。',
        '{"documentName": "中华人民共和国民法典", "lawType": "法律", "effectiveDate": "2021-01-01", "issuingAuthority": "全国人民代表大会"}',
        'KNOWLEDGE',
        '{"icon": "document", "color": "#E1F5FE", "displayPriority": 31}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
    ON CONFLICT ON CONSTRAINT uk_ont_class_uri DO UPDATE SET
        description = EXCLUDED.description,
        metadata = EXCLUDED.metadata,
        updated_at = CURRENT_TIMESTAMP;

    -- ================== 裁判文书实体 ==================

    INSERT INTO ont_class (definition_id, class_uri, local_name, parent_class_id, description, example, domain_hint, metadata, created_at, updated_at)
    VALUES
    -- [14] JudgmentDocument / 裁判文书
    (v_def_id, 'http://legal-ai.cc/ontology/JudgmentDocument', 'JudgmentDocument', NULL,
        '法院制作的具有法律效力的文书，包括判决书、裁定书、调解书、决定书等。裁判文书是案例分析的核心载体，包含案件事实、裁判理由和判决结果。',
        '{"documentNumber": "（2022）沪0105民初21387号", "documentType": "民事判决书", "issueDate": "2023-05-04", "judgmentResult": "驳回原告全部诉讼请求"}',
        'KNOWLEDGE',
        '{"icon": "document-signed", "color": "#E0F7FA", "displayPriority": 40}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    -- [15] CaseReasoning / 裁判要旨
    (v_def_id, 'http://legal-ai.cc/ontology/CaseReasoning', 'CaseReasoning', NULL,
        '案例的裁判要旨或指导意义，是案例库的核心价值输出。包含案例的典型意义、裁判规则、适用场景等关键信息。',
        '{"reasoning": "公司解散纠纷是股东在穷尽公司自治或其他途径，均不能解决公司僵局状况下的救济途径。", "guidanceLevel": "参考"}',
        'KNOWLEDGE',
        '{"icon": "lightbulb", "color": "#FFF9C4", "displayPriority": 41}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
    ON CONFLICT ON CONSTRAINT uk_ont_class_uri DO UPDATE SET
        description = EXCLUDED.description,
        metadata = EXCLUDED.metadata,
        updated_at = CURRENT_TIMESTAMP;

    -- ================== 证据与事实实体 ==================

    INSERT INTO ont_class (definition_id, class_uri, local_name, parent_class_id, description, example, domain_hint, metadata, created_at, updated_at)
    VALUES
    -- [16] Evidence / 证据
    (v_def_id, 'http://legal-ai.cc/ontology/Evidence', 'Evidence', NULL,
        '案件中的证据材料，包括书证、物证、视听资料、电子数据、证人证言、当事人陈述、鉴定意见、勘验笔录等。',
        '{"evidenceNumber": "证据001", "evidenceType": "书证", "content": "股权转让协议（2020年3月30日签署）", "submittedBy": "原告", "purpose": "证明股权变更合法有效"}',
        'KNOWLEDGE',
        '{"icon": "file-text", "color": "#E8F5E9", "displayPriority": 50}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    -- [17] CaseFact / 案件事实
    (v_def_id, 'http://legal-ai.cc/ontology/CaseFact', 'CaseFact', NULL,
        '案件事实，是对案件经过的关键事实描述。一个案件可以包含多个案件事实节点。',
        '{"factDescription": "2020年3月30日，原告受让被告五位股东持有的股权，成为被告公司股东。", "factCategory": "股权转让"}',
        'KNOWLEDGE',
        '{"icon": "info", "color": "#E3F2FD", "displayPriority": 51}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
    ON CONFLICT ON CONSTRAINT uk_ont_class_uri DO UPDATE SET
        description = EXCLUDED.description,
        metadata = EXCLUDED.metadata,
        updated_at = CURRENT_TIMESTAMP;

    -- ================== 商事调解实体 ==================

    INSERT INTO ont_class (definition_id, class_uri, local_name, parent_class_id, description, example, domain_hint, metadata, created_at, updated_at)
    VALUES
    -- [18] CommercialMediationOrganization / 商事调解组织
    (v_def_id, 'http://legal-ai.cc/ontology/CommercialMediationOrganization', 'CommercialMediationOrganization', NULL,
        '依照《商事调解条例》设立，不以营利为目的开展商事调解活动的组织。包括国内商事调解组织和经批准在自贸区设立的境外商事调解组织。',
        '{"name": "上海国际商事调解中心", "orgType": "商事调解组织", "location": "上海市", "licenseNumber": "沪商调证字2024001号", "establishedDate": "2024-06-01"}',
        'KNOWLEDGE',
        '{"icon": "scale", "color": "#E8F5E9", "displayPriority": 60}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    -- [19] Mediator / 调解员
    (v_def_id, 'http://legal-ai.cc/ontology/Mediator', 'Mediator', NULL,
        '商事调解组织的调解员，应当公道正派，具备良好专业素质。须符合《商事调解条例》第12条规定的条件之一。',
        '{"name": "李某调解员", "qualification": "法律职业资格+3年调解经验", "organizationName": "上海国际商事调解中心", "specialty": "公司法务"}',
        'KNOWLEDGE',
        '{"icon": "user-check", "color": "#E0F2F1", "displayPriority": 61}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    -- [20] MediationAgreement / 调解协议
    (v_def_id, 'http://legal-ai.cc/ontology/MediationAgreement', 'MediationAgreement', NULL,
        '经商事调解达成的协议，具有法律约束力，当事人应当履行。可申请司法确认以获得强制执行力。',
        '{"agreementNumber": "MA2024001", "mainFacts": "双方就股权转让款支付达成和解", "agreementContent": "被告于2024年6月30日前支付股权转让款500万元", "performanceMethod": "银行转账", "performanceDeadline": "2024-06-30", "judiciallyConfirmed": true}',
        'KNOWLEDGE',
        '{"icon": "file-check", "color": "#E8EAF6", "displayPriority": 62}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
    ON CONFLICT ON CONSTRAINT uk_ont_class_uri DO UPDATE SET
        description = EXCLUDED.description,
        metadata = EXCLUDED.metadata,
        updated_at = CURRENT_TIMESTAMP;

    RAISE NOTICE 'Step 2: Inserted 20 ontology classes';

    -- ================================================================
    -- Step 3: 插入本体属性 (ont_property)
    -- 通过子查询关联 class_id，确保正确的 domain_class_id
    -- ================================================================

    -- 案件通用属性
    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/caseNumber', 'caseNumber', 'DATATYPE', c.id, 'string', 1, 1, TRUE, FALSE,
        '案件编号，如：（2023）沪01民终11293号',
        '（2022）沪0105民初21387号',
        '{"displayName": "案件编号", "formType": "text"}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'Case'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET
        description = EXCLUDED.description,
        metadata = EXCLUDED.metadata,
        updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/caseName', 'caseName', 'DATATYPE', c.id, 'string', 1, 1, TRUE, FALSE,
        '案件名称，描述案件的当事人与案由',
        '徐某骥诉上海某物业管理有限公司等公司解散纠纷案',
        '{"displayName": "案件名称", "formType": "text"}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'Case'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET
        description = EXCLUDED.description,
        metadata = EXCLUDED.metadata,
        updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/caseType', 'caseType', 'DATATYPE', c.id, 'string', 1, 1, TRUE, FALSE,
        '案件类型：民事、刑事、行政、商事、执行、赔偿',
        '民事',
        '{"displayName": "案件类型", "formType": "select", "allowedValues": ["民事", "刑事", "行政", "商事", "执行", "赔偿"]}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'Case'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET
        description = EXCLUDED.description,
        metadata = EXCLUDED.metadata,
        updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/caseStatus', 'caseStatus', 'DATATYPE', c.id, 'string', 0, 1, FALSE, FALSE,
        '案件状态：立案、审理中、调解中、判决、上诉中、结案、撤销',
        '结案',
        '{"displayName": "案件状态", "formType": "select", "allowedValues": ["立案", "审理中", "调解中", "判决", "上诉中", "结案", "撤销"]}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'Case'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET
        description = EXCLUDED.description,
        metadata = EXCLUDED.metadata,
        updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/filingDate', 'filingDate', 'DATATYPE', c.id, 'date', 0, 1, FALSE, FALSE,
        '立案日期', '2023-01-15',
        '{"displayName": "立案日期", "formType": "date"}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'Case'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET
        description = EXCLUDED.description,
        metadata = EXCLUDED.metadata,
        updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/closedDate', 'closedDate', 'DATATYPE', c.id, 'date', 0, 1, FALSE, FALSE,
        '结案日期', '2023-05-04',
        '{"displayName": "结案日期", "formType": "date"}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'Case'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET
        description = EXCLUDED.description,
        metadata = EXCLUDED.metadata,
        updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/amountInDispute', 'amountInDispute', 'DATATYPE', c.id, 'decimal', 0, 1, FALSE, FALSE,
        '争议金额，单位：元', '5000000.00',
        '{"displayName": "争议金额(元)", "formType": "number"}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'Case'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET
        description = EXCLUDED.description,
        metadata = EXCLUDED.metadata,
        updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/caseSummary', 'caseSummary', 'DATATYPE', c.id, 'text', 0, 1, FALSE, FALSE,
        '案件摘要，对案件关键事实的简明描述',
        '原告作为被告公司股东持股39.54%，因公司经营僵局诉请解散公司。',
        '{"displayName": "案件摘要", "formType": "textarea"}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'Case'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET
        description = EXCLUDED.description,
        metadata = EXCLUDED.metadata,
        updated_at = CURRENT_TIMESTAMP;

    -- 商事案件特有属性
    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/disputeType', 'disputeType', 'DATATYPE', c.id, 'string', 0, 1, FALSE, FALSE,
        '纠纷类型：合同纠纷、公司治理纠纷、知识产权纠纷、劳动争议等', '公司解散',
        '{"displayName": "纠纷类型", "formType": "text"}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'CommercialCase'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET
        description = EXCLUDED.description,
        metadata = EXCLUDED.metadata,
        updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/mediationAttempted', 'mediationAttempted', 'DATATYPE', c.id, 'boolean', 0, 1, FALSE, FALSE,
        '是否经过调解程序', 'false',
        '{"displayName": "是否经过调解", "formType": "switch"}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'CommercialCase'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET
        description = EXCLUDED.description,
        metadata = EXCLUDED.metadata,
        updated_at = CURRENT_TIMESTAMP;

    -- 当事人属性
    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/partyName', 'partyName', 'DATATYPE', c.id, 'string', 1, 1, TRUE, FALSE,
        '当事人姓名或名称', '徐某骥',
        '{"displayName": "姓名/名称", "formType": "text"}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'Party'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET
        description = EXCLUDED.description,
        metadata = EXCLUDED.metadata,
        updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/partyType', 'partyType', 'DATATYPE', c.id, 'string', 1, 1, TRUE, FALSE,
        '当事人类型：自然人、法人、非法人组织', '自然人',
        '{"displayName": "当事人类型", "formType": "select", "allowedValues": ["自然人", "法人", "非法人组织"]}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'Party'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET
        description = EXCLUDED.description,
        metadata = EXCLUDED.metadata,
        updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/partyRole', 'partyRole', 'DATATYPE', c.id, 'string', 1, 1, TRUE, FALSE,
        '诉讼角色：原告、被告、第三人、上诉人、被上诉人、申请人、被申请人', '原告',
        '{"displayName": "诉讼角色", "formType": "select", "allowedValues": ["原告", "被告", "第三人", "上诉人", "被上诉人", "申请人", "被申请人"]}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'Party'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET
        description = EXCLUDED.description,
        metadata = EXCLUDED.metadata,
        updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/idNumber', 'idNumber', 'DATATYPE', c.id, 'string', 0, 1, FALSE, FALSE,
        '身份证号或统一社会信用代码', '310101199001011234',
        '{"displayName": "身份证号/统一社会信用代码", "formType": "text"}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'Party'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET
        description = EXCLUDED.description,
        metadata = EXCLUDED.metadata,
        updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/address', 'address', 'DATATYPE', c.id, 'string', 0, 1, FALSE, FALSE,
        '住所地或注册地', '上海市长宁区某路123号',
        '{"displayName": "住所地", "formType": "text"}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'Party'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET
        description = EXCLUDED.description,
        metadata = EXCLUDED.metadata,
        updated_at = CURRENT_TIMESTAMP;

    -- 法院属性
    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/courtName', 'courtName', 'DATATYPE', c.id, 'string', 1, 1, TRUE, FALSE,
        '法院名称', '上海市第一中级人民法院',
        '{"displayName": "法院名称", "formType": "text"}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'Court'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET
        description = EXCLUDED.description,
        metadata = EXCLUDED.metadata,
        updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/courtLevel', 'courtLevel', 'DATATYPE', c.id, 'string', 0, 1, FALSE, FALSE,
        '法院级别：最高人民法院、高级人民法院、中级人民法院、基层人民法院、专门法院', '中级人民法院',
        '{"displayName": "法院级别", "formType": "select", "allowedValues": ["最高人民法院", "高级人民法院", "中级人民法院", "基层人民法院", "专门法院"]}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'Court'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET
        description = EXCLUDED.description,
        metadata = EXCLUDED.metadata,
        updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/location', 'location', 'DATATYPE', c.id, 'string', 0, 1, FALSE, FALSE,
        '法院所在地', '上海市',
        '{"displayName": "所在地", "formType": "text"}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'Court'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET
        description = EXCLUDED.description,
        metadata = EXCLUDED.metadata,
        updated_at = CURRENT_TIMESTAMP;

    -- 法官属性
    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/judgeName', 'judgeName', 'DATATYPE', c.id, 'string', 1, 1, TRUE, FALSE,
        '法官姓名', '张某',
        '{"displayName": "法官姓名", "formType": "text"}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'Judge'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET
        description = EXCLUDED.description,
        metadata = EXCLUDED.metadata,
        updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/judgeTitle', 'judgeTitle', 'DATATYPE', c.id, 'string', 0, 1, FALSE, FALSE,
        '职务：审判长、审判员、人民陪审员、书记员', '审判长',
        '{"displayName": "职务", "formType": "select", "allowedValues": ["审判长", "审判员", "人民陪审员", "书记员"]}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'Judge'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET
        description = EXCLUDED.description,
        metadata = EXCLUDED.metadata,
        updated_at = CURRENT_TIMESTAMP;

    -- 法律条文属性
    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/provisionId', 'provisionId', 'DATATYPE', c.id, 'string', 1, 1, TRUE, FALSE,
        '条文唯一标识编号', 'L001',
        '{"displayName": "条文编号", "formType": "text"}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'LegalProvision'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET
        description = EXCLUDED.description,
        metadata = EXCLUDED.metadata,
        updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/articleNumber', 'articleNumber', 'DATATYPE', c.id, 'string', 1, 1, TRUE, FALSE,
        '条款序号，如：第69条、第14条第一款', '第69条',
        '{"displayName": "条款序号", "formType": "text"}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'LegalProvision'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET
        description = EXCLUDED.description,
        metadata = EXCLUDED.metadata,
        updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/provisionContent', 'provisionContent', 'DATATYPE', c.id, 'text', 1, 1, TRUE, FALSE,
        '条文完整内容',
        '法人清算后的剩余财产，按照法人章程的规定或者法人权力机构的决议处理。',
        '{"displayName": "条文内容", "formType": "textarea"}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'LegalProvision'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET
        description = EXCLUDED.description,
        metadata = EXCLUDED.metadata,
        updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/lawName', 'lawName', 'DATATYPE', c.id, 'string', 1, 1, TRUE, FALSE,
        '所属法律文件名称', '中华人民共和国民法典',
        '{"displayName": "法律名称", "formType": "text"}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'LegalProvision'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET
        description = EXCLUDED.description,
        metadata = EXCLUDED.metadata,
        updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/lawType', 'lawType', 'DATATYPE', c.id, 'string', 0, 1, FALSE, FALSE,
        '法律类型：法律、行政法规、司法解释、部门规章、地方性法规', '法律',
        '{"displayName": "法律类型", "formType": "select", "allowedValues": ["法律", "行政法规", "司法解释", "部门规章", "地方性法规"]}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'LegalProvision'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET
        description = EXCLUDED.description,
        metadata = EXCLUDED.metadata,
        updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/effectiveDate', 'effectiveDate', 'DATATYPE', c.id, 'date', 0, 1, FALSE, FALSE,
        '法律生效日期', '2021-01-01',
        '{"displayName": "生效日期", "formType": "date"}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'LegalProvision'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET
        description = EXCLUDED.description,
        metadata = EXCLUDED.metadata,
        updated_at = CURRENT_TIMESTAMP;

    -- 裁判文书属性
    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/documentNumber', 'documentNumber', 'DATATYPE', c.id, 'string', 1, 1, TRUE, FALSE,
        '文书编号，如：（2022）沪0105民初21387号', '（2022）沪0105民初21387号',
        '{"displayName": "文书编号", "formType": "text"}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'JudgmentDocument'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET
        description = EXCLUDED.description,
        metadata = EXCLUDED.metadata,
        updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/documentType', 'documentType', 'DATATYPE', c.id, 'string', 0, 1, FALSE, FALSE,
        '文书类型：判决书、裁定书、调解书、决定书、裁决书', '民事判决书',
        '{"displayName": "文书类型", "formType": "select", "allowedValues": ["判决书", "裁定书", "调解书", "决定书", "裁决书"]}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'JudgmentDocument'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET
        description = EXCLUDED.description,
        metadata = EXCLUDED.metadata,
        updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/issueDate', 'issueDate', 'DATATYPE', c.id, 'date', 0, 1, FALSE, FALSE,
        '文书作出日期', '2023-05-04',
        '{"displayName": "作出日期", "formType": "date"}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'JudgmentDocument'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET
        description = EXCLUDED.description,
        metadata = EXCLUDED.metadata,
        updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/judgmentResult', 'judgmentResult', 'DATATYPE', c.id, 'text', 0, 1, FALSE, FALSE,
        '判决结果摘要', '驳回原告全部诉讼请求',
        '{"displayName": "判决结果", "formType": "textarea"}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'JudgmentDocument'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET
        description = EXCLUDED.description,
        metadata = EXCLUDED.metadata,
        updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/legalBasis', 'legalBasis', 'DATATYPE', c.id, 'text', 0, 1, FALSE, FALSE,
        '法律依据，适用的法律条文', '《中华人民共和国民法典》第69条',
        '{"displayName": "法律依据", "formType": "textarea"}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'JudgmentDocument'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET
        description = EXCLUDED.description,
        metadata = EXCLUDED.metadata,
        updated_at = CURRENT_TIMESTAMP;

    -- 调解协议属性
    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/agreementNumber', 'agreementNumber', 'DATATYPE', c.id, 'string', 1, 1, TRUE, FALSE,
        '调解协议编号', 'MA2024001',
        '{"displayName": "协议编号", "formType": "text"}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'MediationAgreement'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET
        description = EXCLUDED.description,
        metadata = EXCLUDED.metadata,
        updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/performanceMethod', 'performanceMethod', 'DATATYPE', c.id, 'string', 0, 1, FALSE, FALSE,
        '履行方式：银行转账、现金交付、实物交付等', '银行转账',
        '{"displayName": "履行方式", "formType": "text"}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'MediationAgreement'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET
        description = EXCLUDED.description,
        metadata = EXCLUDED.metadata,
        updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/performanceDeadline', 'performanceDeadline', 'DATATYPE', c.id, 'date', 0, 1, FALSE, FALSE,
        '履行期限', '2024-06-30',
        '{"displayName": "履行期限", "formType": "date"}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'MediationAgreement'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET
        description = EXCLUDED.description,
        metadata = EXCLUDED.metadata,
        updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/judiciallyConfirmed', 'judiciallyConfirmed', 'DATATYPE', c.id, 'boolean', 0, 1, FALSE, FALSE,
        '是否经过司法确认', 'true',
        '{"displayName": "是否司法确认", "formType": "switch"}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'MediationAgreement'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET
        description = EXCLUDED.description,
        metadata = EXCLUDED.metadata,
        updated_at = CURRENT_TIMESTAMP;

    RAISE NOTICE 'Step 3: Inserted ontology properties';

    -- ================================================================
    -- Step 4: 插入本体约束 (ont_constraint)
    -- ================================================================

    INSERT INTO ont_constraint (definition_id, class_id, property_id, constraint_type, value, error_message, severity, description, created_at, updated_at)
    SELECT v_def_id, c.id, p.id, 'PATTERN',
        '{"pattern": "^[（(][0-9]{1,4}）?[地东西南北华中上下]?\\d{2,}[民刑执行经知行赔][初重终辖再简调强执抗不适赔认补他号字]\\d{3,10}号?$"}',
        '案件编号格式不正确，应符合中国法院案号规范，如：（2023）沪01民终11293号',
        'WARNING',
        '案件编号应符合中国法院案号规范，包含年份、法院代码、案件类型代码和序号',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c
    LEFT JOIN ont_property p ON p.domain_class_id = c.id AND p.local_name = 'caseNumber'
    WHERE c.definition_id = v_def_id AND c.local_name = 'Case'
    ON CONFLICT DO NOTHING;

    INSERT INTO ont_constraint (definition_id, class_id, property_id, constraint_type, value, error_message, severity, description, created_at, updated_at)
    SELECT v_def_id, c.id, p.id, 'ENUM',
        '["自然人", "法人", "非法人组织"]',
        '当事人类型必须是：自然人、法人 或 非法人组织',
        'ERROR',
        '确保当事人类型字段的数据质量',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c
    LEFT JOIN ont_property p ON p.domain_class_id = c.id AND p.local_name = 'partyType'
    WHERE c.definition_id = v_def_id AND c.local_name = 'Party'
    ON CONFLICT DO NOTHING;

    INSERT INTO ont_constraint (definition_id, class_id, property_id, constraint_type, value, error_message, severity, description, created_at, updated_at)
    SELECT v_def_id, c.id, p.id, 'ENUM',
        '["原告", "被告", "第三人", "上诉人", "被上诉人", "申请人", "被申请人"]',
        '诉讼角色必须在允许范围内',
        'ERROR',
        '确保诉讼角色字段的数据质量',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c
    LEFT JOIN ont_property p ON p.domain_class_id = c.id AND p.local_name = 'partyRole'
    WHERE c.definition_id = v_def_id AND c.local_name = 'Party'
    ON CONFLICT DO NOTHING;

    INSERT INTO ont_constraint (definition_id, class_id, property_id, constraint_type, value, error_message, severity, description, created_at, updated_at)
    SELECT v_def_id, c.id, p.id, 'ENUM',
        '["法律", "行政法规", "司法解释", "部门规章", "地方性法规"]',
        '法律类型必须是：法律、行政法规、司法解释、部门规章 或 地方性法规',
        'ERROR',
        '确保法律类型字段的数据质量',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c
    LEFT JOIN ont_property p ON p.domain_class_id = c.id AND p.local_name = 'lawType'
    WHERE c.definition_id = v_def_id AND c.local_name = 'LegalProvision'
    ON CONFLICT DO NOTHING;

    INSERT INTO ont_constraint (definition_id, class_id, property_id, constraint_type, value, error_message, severity, description, created_at, updated_at)
    SELECT v_def_id, c.id, p.id, 'ENUM',
        '["判决书", "裁定书", "调解书", "决定书", "裁决书"]',
        '文书类型必须是：判决书、裁定书、调解书、决定书 或 裁决书',
        'ERROR',
        '确保裁判文书类型字段的数据质量',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c
    LEFT JOIN ont_property p ON p.domain_class_id = c.id AND p.local_name = 'documentType'
    WHERE c.definition_id = v_def_id AND c.local_name = 'JudgmentDocument'
    ON CONFLICT DO NOTHING;

    INSERT INTO ont_constraint (definition_id, class_id, property_id, constraint_type, value, error_message, severity, description, created_at, updated_at)
    SELECT v_def_id, c.id, p.id, 'NOT_NULL',
        '{"enforced": true}',
        '调解协议编号不能为空',
        'ERROR',
        '确保调解协议编号必填',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c
    LEFT JOIN ont_property p ON p.domain_class_id = c.id AND p.local_name = 'agreementNumber'
    WHERE c.definition_id = v_def_id AND c.local_name = 'MediationAgreement'
    ON CONFLICT DO NOTHING;

    RAISE NOTICE 'Step 4: Inserted ontology constraints';

    -- ================================================================
    -- Step 5: 记录版本历史 (ont_version_history)
    -- ================================================================

    INSERT INTO ont_version_history (definition_id, version, change_type, entity_type, entity_id, before_state, after_state, diff_summary, changed_by, changed_at)
    VALUES (
        v_def_id, '2.0.0', 'DEFINITION_CREATED', 'DEFINITION', v_def_id,
        NULL,
        '{"name": "法律知识图谱本体 V2", "version": "2.0.0", "classCount": 20, "propertyCount": 35, "constraintCount": 6}',
        '创建法律知识图谱本体 V2，定义20个实体类、35个属性、6个约束',
        'system',
        CURRENT_TIMESTAMP
    );

    RAISE NOTICE 'Step 5: Recorded version history. Ontology setup complete.';

END $$;

COMMIT;

-- ================================================================
-- 验证查询
-- ================================================================

-- 查看已创建的本体定义
SELECT id, graph_id, namespace, name, version, status, description
FROM ont_definition
WHERE graph_id = 'legal-knowledge-graph'
ORDER BY created_at DESC;

-- 查看类统计
SELECT
  (SELECT COUNT(*) FROM ont_class WHERE definition_id = (SELECT id FROM ont_definition WHERE graph_id = 'legal-knowledge-graph' AND version = '2.0.0')) AS class_count,
  (SELECT COUNT(*) FROM ont_property WHERE definition_id = (SELECT id FROM ont_definition WHERE graph_id = 'legal-knowledge-graph' AND version = '2.0.0')) AS property_count,
  (SELECT COUNT(*) FROM ont_constraint WHERE definition_id = (SELECT id FROM ont_definition WHERE graph_id = 'legal-knowledge-graph' AND version = '2.0.0')) AS constraint_count;

-- 查看所有类
SELECT id, local_name, class_uri, description, domain_hint
FROM ont_class
WHERE definition_id = (SELECT id FROM ont_definition WHERE graph_id = 'legal-knowledge-graph' AND version = '2.0.0')

-- ================================================================
-- 初始化完成
-- ================================================================
