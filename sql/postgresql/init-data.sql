-- ============================================================
-- Graphiti 初始化数据 (PostgreSQL 版本)
-- 版本: 2026-05-18
-- 说明: 系统权限 + 法律本体 + 提示词模板 + 示例图谱
-- ============================================================

-- ============================================================
-- 第一部分: 系统权限数据
-- ============================================================

-- 初始化系统角色
INSERT INTO sys_role (name, code, status) VALUES
('超级管理员', 'SUPER_ADMIN', 1),
('管理员', 'ADMIN', 1),
('普通用户', 'USER', 1);

-- 初始化系统用户（密码：admin123，BCrypt加密）
INSERT INTO sys_user (username, password, nickname, email, mobile, status, create_time, update_time, deleted)
VALUES (
    'admin',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi',
    '系统管理员',
    'admin@graphiti.com',
    NULL,
    1,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    FALSE
);

-- 初始化用户角色关联
DO $$
DECLARE
    admin_user_id BIGINT;
    super_admin_role_id BIGINT;
BEGIN
    SELECT id INTO admin_user_id FROM sys_user WHERE username = 'admin';
    SELECT id INTO super_admin_role_id FROM sys_role WHERE code = 'SUPER_ADMIN';
    INSERT INTO sys_user_role (user_id, role_id) VALUES (admin_user_id, super_admin_role_id);
END $$;

-- 初始化系统菜单
-- parent_id: 0 = 顶级菜单, >0 = 指向父菜单ID
INSERT INTO sys_menu (name, permission, url, parent_id, sort, status) VALUES
('系统管理', 'system:manage', '/system', 0, 2, 1),
('用户管理', 'system:user:list', '/system/user', 0, 1, 1),
('角色管理', 'system:role:list', '/system/role', 0, 2, 1),
('菜单管理', 'system:menu:list', '/system/menu', 0, 3, 1),
('图谱管理', 'graph:manage', '/graph', 0, 4, 1),
('图谱列表', 'graph:list', '/graph/list', 0, 1, 1),
('本体管理', 'ontology:manage', '/ontology', 0, 5, 1),
('本体定义', 'ontology:definition:list', '/ontology/definition', 0, 1, 1),
('提示词管理', 'prompt:manage', '/prompt', 0, 6, 1),
('提示词模板', 'prompt:template:list', '/prompt/template', 0, 1, 1);

-- 初始化角色菜单关联
DO $$
DECLARE
    super_admin_role_id BIGINT;
    menu_id_1 BIGINT;
    menu_id_2 BIGINT;
    menu_id_3 BIGINT;
    menu_id_4 BIGINT;
    menu_id_5 BIGINT;
    menu_id_6 BIGINT;
    menu_id_7 BIGINT;
    menu_id_8 BIGINT;
    menu_id_9 BIGINT;
    menu_id_10 BIGINT;
BEGIN
    SELECT id INTO super_admin_role_id FROM sys_role WHERE code = 'SUPER_ADMIN';
    SELECT id INTO menu_id_1 FROM sys_menu WHERE name = '系统管理';
    SELECT id INTO menu_id_2 FROM sys_menu WHERE name = '用户管理';
    SELECT id INTO menu_id_3 FROM sys_menu WHERE name = '角色管理';
    SELECT id INTO menu_id_4 FROM sys_menu WHERE name = '菜单管理';
    SELECT id INTO menu_id_5 FROM sys_menu WHERE name = '图谱管理';
    SELECT id INTO menu_id_6 FROM sys_menu WHERE name = '图谱列表';
    SELECT id INTO menu_id_7 FROM sys_menu WHERE name = '本体管理';
    SELECT id INTO menu_id_8 FROM sys_menu WHERE name = '本体定义';
    SELECT id INTO menu_id_9 FROM sys_menu WHERE name = '提示词管理';
    SELECT id INTO menu_id_10 FROM sys_menu WHERE name = '提示词模板';
    INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (super_admin_role_id, menu_id_1),
    (super_admin_role_id, menu_id_2),
    (super_admin_role_id, menu_id_3),
    (super_admin_role_id, menu_id_4),
    (super_admin_role_id, menu_id_5),
    (super_admin_role_id, menu_id_6),
    (super_admin_role_id, menu_id_7),
    (super_admin_role_id, menu_id_8),
    (super_admin_role_id, menu_id_9),
    (super_admin_role_id, menu_id_10);
END $$;


-- ============================================================
-- 第二部分: 示例图谱
-- ============================================================

INSERT INTO graphiti_graph_metadata (graph_id, name, description, node_count, edge_count, status, deleted)
VALUES ('example-graph', '示例图谱', '这是一个示例知识图谱', 0, 0, 'ACTIVE', FALSE);

INSERT INTO graphiti_graph_metadata (graph_id, name, description, node_count, edge_count, status, deleted)
VALUES ('legal-knowledge-graph', '法律知识图谱', '基于典型案例和法律条文设计的法律领域本体图谱', 0, 0, 'ACTIVE', FALSE);


-- ============================================================
-- 第三部分: 法律知识图谱本体定义 V2.0.0
-- ============================================================

DO $$
DECLARE
    v_def_id BIGINT;
BEGIN
    -- 插入本体定义
    INSERT INTO ont_definition (graph_id, namespace, name, version, status, description, created_by, created_at, updated_at)
    VALUES (
        'legal-knowledge-graph',
        'http://legal-ai.cc/ontology',
        '法律知识图谱本体 V2',
        '2.0.0',
        'ACTIVE',
        '基于典型类案（裁判文书）与法律条文设计的法律领域本体，涵盖民商事、行政、刑事案件类型，支持案例推理与法条引用分析。',
        'system',
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP
    )
    ON CONFLICT ON CONSTRAINT uk_ont_def_graph_version DO UPDATE SET
        description = EXCLUDED.description,
        updated_at = CURRENT_TIMESTAMP
    RETURNING id INTO v_def_id;

    IF v_def_id IS NULL THEN
        SELECT id INTO v_def_id FROM ont_definition
        WHERE graph_id = 'legal-knowledge-graph' AND version = '2.0.0';
    END IF;

    RAISE NOTICE 'Using definition_id: %', v_def_id;

    -- ----------------------------------------------------------
    -- 案件域实体 (6个)
    -- ----------------------------------------------------------
    INSERT INTO ont_class (definition_id, class_uri, local_name, parent_class_id, description, example, domain_hint, metadata, created_at, updated_at)
    VALUES
    (v_def_id, 'http://legal-ai.cc/ontology/Case', 'Case', NULL,
        '案件基类，涵盖所有类型案件的公共属性。',
        '{"caseNumber": "（2023）沪01民终11293号", "caseName": "徐某骥诉上海某物业管理有限公司等公司解散纠纷案"}',
        'KNOWLEDGE', '{"icon": "case", "color": "#E3F2FD", "displayPriority": 1}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (v_def_id, 'http://legal-ai.cc/ontology/CommercialCase', 'CommercialCase', NULL,
        '商事案件，适用《商事调解条例》范围内的案件类型。',
        '{"caseNumber": "（2023）沪01民终11293号", "caseName": "公司解散纠纷案", "disputeType": "公司解散"}',
        'KNOWLEDGE', '{"icon": "commercial", "color": "#E8F5E9", "displayPriority": 2}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (v_def_id, 'http://legal-ai.cc/ontology/CivilCase', 'CivilCase', NULL,
        '民事案件，包括婚姻家庭纠纷、继承纠纷、合同纠纷等传统民事领域案件。',
        '{"caseNumber": "（2020）渝民辖188号", "caseName": "谭某诉吴某债权人撤销权纠纷案"}',
        'KNOWLEDGE', '{"icon": "civil", "color": "#FFF3E0", "displayPriority": 3}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (v_def_id, 'http://legal-ai.cc/ontology/CriminalCase', 'CriminalCase', NULL,
        '刑事案件，包括公诉案件和自诉案件。',
        '{"caseNumber": "（2022）沪01刑初123号", "caseName": "徐某故意杀人案"}',
        'KNOWLEDGE', '{"icon": "criminal", "color": "#FFEBEE", "displayPriority": 4}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (v_def_id, 'http://legal-ai.cc/ontology/AdministrativeCase', 'AdministrativeCase', NULL,
        '行政案件，包括行政处罚、行政许可、行政强制等。',
        '{"caseNumber": "（2023）京01行初456号", "caseName": "某公司诉某税务局纳税信用评价案"}',
        'KNOWLEDGE', '{"icon": "admin", "color": "#F3E5F5", "displayPriority": 5}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (v_def_id, 'http://legal-ai.cc/ontology/ExecutionCase', 'ExecutionCase', NULL,
        '执行案件，包括民事执行、行政执行、刑事执行等。',
        '{"caseNumber": "（2023）沪01执789号", "caseName": "某公司申请执行某公司合同纠纷案"}',
        'KNOWLEDGE', '{"icon": "execution", "color": "#ECEFF1", "displayPriority": 6}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
    ON CONFLICT ON CONSTRAINT uk_ont_class_uri DO UPDATE SET
        description = EXCLUDED.description,
        metadata = EXCLUDED.metadata,
        updated_at = CURRENT_TIMESTAMP;

    -- ----------------------------------------------------------
    -- 当事人与代理人实体 (3个)
    -- ----------------------------------------------------------
    INSERT INTO ont_class (definition_id, class_uri, local_name, parent_class_id, description, example, domain_hint, metadata, created_at, updated_at)
    VALUES
    (v_def_id, 'http://legal-ai.cc/ontology/Party', 'Party', NULL,
        '案件中的当事人，包括自然人、法人和非法人组织。',
        '{"partyName": "徐某骥", "partyType": "自然人", "partyRole": "原告"}',
        'KNOWLEDGE', '{"icon": "person", "color": "#E1F5FE", "displayPriority": 10}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (v_def_id, 'http://legal-ai.cc/ontology/LegalPerson', 'LegalPerson', NULL,
        '法人当事人，包括有限责任公司、股份有限公司等。',
        '{"partyName": "上海某物业管理有限公司", "partyType": "法人"}',
        'KNOWLEDGE', '{"icon": "organization", "color": "#E8EAF6", "displayPriority": 11}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (v_def_id, 'http://legal-ai.cc/ontology/Lawyer', 'Lawyer', NULL,
        '执业律师，为案件当事人提供法律服务的专业人员。',
        '{"partyName": "陈某律师", "licenseNumber": "311011993001011234", "firmName": "上海某律师事务所"}',
        'KNOWLEDGE', '{"icon": "lawyer", "color": "#E0F2F1", "displayPriority": 12}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
    ON CONFLICT ON CONSTRAINT uk_ont_class_uri DO UPDATE SET
        description = EXCLUDED.description,
        metadata = EXCLUDED.metadata,
        updated_at = CURRENT_TIMESTAMP;

    -- ----------------------------------------------------------
    -- 司法机构实体 (2个)
    -- ----------------------------------------------------------
    INSERT INTO ont_class (definition_id, class_uri, local_name, parent_class_id, description, example, domain_hint, metadata, created_at, updated_at)
    VALUES
    (v_def_id, 'http://legal-ai.cc/ontology/Court', 'Court', NULL,
        '审判机关，包括最高人民法院、高级人民法院、中级人民法院、基层人民法院以及专门法院。',
        '{"courtName": "上海市第一中级人民法院", "courtLevel": "中级人民法院", "location": "上海市"}',
        'KNOWLEDGE', '{"icon": "court", "color": "#FCE4EC", "displayPriority": 20}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (v_def_id, 'http://legal-ai.cc/ontology/Judge', 'Judge', NULL,
        '案件审判人员，包括审判长、审判员、人民陪审员、书记员等。',
        '{"judgeName": "张某法官", "judgeTitle": "审判长", "courtName": "上海市第一中级人民法院"}',
        'KNOWLEDGE', '{"icon": "judge", "color": "#FFF8E1", "displayPriority": 21}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
    ON CONFLICT ON CONSTRAINT uk_ont_class_uri DO UPDATE SET
        description = EXCLUDED.description,
        metadata = EXCLUDED.metadata,
        updated_at = CURRENT_TIMESTAMP;

    -- ----------------------------------------------------------
    -- 法律条文实体 (2个)
    -- ----------------------------------------------------------
    INSERT INTO ont_class (definition_id, class_uri, local_name, parent_class_id, description, example, domain_hint, metadata, created_at, updated_at)
    VALUES
    (v_def_id, 'http://legal-ai.cc/ontology/LegalProvision', 'LegalProvision', NULL,
        '法律、行政法规、司法解释、部门规章、地方性法规等规范性法律文件的条文。',
        '{"provisionId": "L001", "articleNumber": "第69条", "provisionContent": "法人解散的...", "lawName": "中华人民共和国民法典"}',
        'KNOWLEDGE', '{"icon": "law", "color": "#E8EAF6", "displayPriority": 30}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (v_def_id, 'http://legal-ai.cc/ontology/LegalDocument', 'LegalDocument', NULL,
        '完整的法律法规文件，用于组织法律条文。',
        '{"documentName": "中华人民共和国民法典", "lawType": "法律", "effectiveDate": "2021-01-01"}',
        'KNOWLEDGE', '{"icon": "document", "color": "#E1F5FE", "displayPriority": 31}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
    ON CONFLICT ON CONSTRAINT uk_ont_class_uri DO UPDATE SET
        description = EXCLUDED.description,
        metadata = EXCLUDED.metadata,
        updated_at = CURRENT_TIMESTAMP;

    -- ----------------------------------------------------------
    -- 裁判文书实体 (2个)
    -- ----------------------------------------------------------
    INSERT INTO ont_class (definition_id, class_uri, local_name, parent_class_id, description, example, domain_hint, metadata, created_at, updated_at)
    VALUES
    (v_def_id, 'http://legal-ai.cc/ontology/JudgmentDocument', 'JudgmentDocument', NULL,
        '法院制作的具有法律效力的文书，包括判决书、裁定书、调解书、决定书等。',
        '{"documentNumber": "（2022）沪0105民初21387号", "documentType": "民事判决书"}',
        'KNOWLEDGE', '{"icon": "document-signed", "color": "#E0F7FA", "displayPriority": 40}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (v_def_id, 'http://legal-ai.cc/ontology/CaseReasoning', 'CaseReasoning', NULL,
        '案例的裁判要旨或指导意义，是案例库的核心价值输出。',
        '{"reasoning": "公司解散纠纷是股东在穷尽公司自治...", "guidanceLevel": "参考"}',
        'KNOWLEDGE', '{"icon": "lightbulb", "color": "#FFF9C4", "displayPriority": 41}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
    ON CONFLICT ON CONSTRAINT uk_ont_class_uri DO UPDATE SET
        description = EXCLUDED.description,
        metadata = EXCLUDED.metadata,
        updated_at = CURRENT_TIMESTAMP;

    -- ----------------------------------------------------------
    -- 证据与事实实体 (2个)
    -- ----------------------------------------------------------
    INSERT INTO ont_class (definition_id, class_uri, local_name, parent_class_id, description, example, domain_hint, metadata, created_at, updated_at)
    VALUES
    (v_def_id, 'http://legal-ai.cc/ontology/Evidence', 'Evidence', NULL,
        '案件中的证据材料，包括书证、物证、视听资料、电子数据等。',
        '{"evidenceNumber": "证据001", "evidenceType": "书证", "content": "股权转让协议"}',
        'KNOWLEDGE', '{"icon": "file-text", "color": "#E8F5E9", "displayPriority": 50}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (v_def_id, 'http://legal-ai.cc/ontology/CaseFact', 'CaseFact', NULL,
        '案件事实，是对案件经过的关键事实描述。',
        '{"factDescription": "2020年3月30日，原告受让被告股权...", "factCategory": "股权转让"}',
        'KNOWLEDGE', '{"icon": "info", "color": "#E3F2FD", "displayPriority": 51}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
    ON CONFLICT ON CONSTRAINT uk_ont_class_uri DO UPDATE SET
        description = EXCLUDED.description,
        metadata = EXCLUDED.metadata,
        updated_at = CURRENT_TIMESTAMP;

    -- ----------------------------------------------------------
    -- 商事调解实体 (3个)
    -- ----------------------------------------------------------
    INSERT INTO ont_class (definition_id, class_uri, local_name, parent_class_id, description, example, domain_hint, metadata, created_at, updated_at)
    VALUES
    (v_def_id, 'http://legal-ai.cc/ontology/CommercialMediationOrganization', 'CommercialMediationOrganization', NULL,
        '依照《商事调解条例》设立，不以营利为目的开展商事调解活动的组织。',
        '{"name": "上海国际商事调解中心", "orgType": "商事调解组织", "licenseNumber": "沪商调证字2024001号"}',
        'KNOWLEDGE', '{"icon": "scale", "color": "#E8F5E9", "displayPriority": 60}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (v_def_id, 'http://legal-ai.cc/ontology/Mediator', 'Mediator', NULL,
        '商事调解组织的调解员，应当公道正派，具备良好专业素质。',
        '{"name": "李某调解员", "qualification": "法律职业资格+3年调解经验"}',
        'KNOWLEDGE', '{"icon": "user-check", "color": "#E0F2F1", "displayPriority": 61}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (v_def_id, 'http://legal-ai.cc/ontology/MediationAgreement', 'MediationAgreement', NULL,
        '经商事调解达成的协议，具有法律约束力，可申请司法确认。',
        '{"agreementNumber": "MA2024001", "mainFacts": "双方就股权转让款支付达成和解"}',
        'KNOWLEDGE', '{"icon": "file-check", "color": "#E8EAF6", "displayPriority": 62}',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
    ON CONFLICT ON CONSTRAINT uk_ont_class_uri DO UPDATE SET
        description = EXCLUDED.description,
        metadata = EXCLUDED.metadata,
        updated_at = CURRENT_TIMESTAMP;

    RAISE NOTICE 'Inserted 20 ontology classes';

    -- ----------------------------------------------------------
    -- 本体类继承关系填充
    -- ----------------------------------------------------------

    RAISE NOTICE 'Filling ontology class inheritance relationships';

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
    SELECT c.id, p.id, v_def_id, 1, CURRENT_TIMESTAMP
    FROM (VALUES
        ('CivilCase', 'Case'),
        ('CriminalCase', 'Case'),
        ('AdministrativeCase', 'Case'),
        ('CommercialCase', 'Case'),
        ('ExecutionCase', 'Case'),
        ('LegalPerson', 'Party')
    ) AS inheritance(sub_class, parent_class)
    JOIN ont_class c ON c.definition_id = v_def_id AND c.local_name = inheritance.sub_class
    JOIN ont_class p ON p.definition_id = v_def_id AND p.local_name = inheritance.parent_class
    ON CONFLICT ON CONSTRAINT uk_ont_inheritance_pair DO NOTHING;

    -- 验证 ont_class.parent_class_id
    PERFORM local_name FROM ont_class WHERE definition_id = v_def_id AND parent_class_id IS NOT NULL;

    -- 验证 ont_class_inheritance 记录数
    PERFORM COUNT(*) FROM ont_class_inheritance WHERE definition_id = v_def_id;

    -- ----------------------------------------------------------
    -- 本体属性 - Case
    -- ----------------------------------------------------------
    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/caseNumber', 'caseNumber', 'DATATYPE', c.id, 'string', 0, 1, TRUE, FALSE,
        '案件编号，如：（2023）沪01民终11293号', '（2022）沪0105民初21387号',
        '{"displayName": "案件编号", "formType": "text"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'Case'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/caseName', 'caseName', 'DATATYPE', c.id, 'string', 0, 1, TRUE, FALSE,
        '案件名称', '徐某骥诉上海某物业管理有限公司等公司解散纠纷案',
        '{"displayName": "案件名称", "formType": "text"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'Case'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/caseType', 'caseType', 'DATATYPE', c.id, 'string', 0, 1, TRUE, FALSE,
        '案件类型：民事、刑事、行政、商事、执行、赔偿', '民事',
        '{"displayName": "案件类型", "formType": "select", "allowedValues": ["民事", "刑事", "行政", "商事", "执行", "赔偿"]}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'Case'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/caseStatus', 'caseStatus', 'DATATYPE', c.id, 'string', 0, 1, FALSE, FALSE,
        '案件状态', '结案',
        '{"displayName": "案件状态", "formType": "select", "allowedValues": ["立案", "审理中", "调解中", "判决", "上诉中", "结案", "撤销"]}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'Case'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/filingDate', 'filingDate', 'DATATYPE', c.id, 'date', 0, 1, FALSE, TRUE,
        '立案日期', '2023-01-15',
        '{"displayName": "立案日期", "formType": "date"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'Case'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/closedDate', 'closedDate', 'DATATYPE', c.id, 'date', 0, 1, FALSE, TRUE,
        '结案日期', '2023-05-04',
        '{"displayName": "结案日期", "formType": "date"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'Case'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/amountInDispute', 'amountInDispute', 'DATATYPE', c.id, 'decimal', 0, 1, FALSE, FALSE,
        '争议金额，单位：元', '5000000.00',
        '{"displayName": "争议金额(元)", "formType": "number"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'Case'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/caseSummary', 'caseSummary', 'DATATYPE', c.id, 'text', 0, 1, FALSE, FALSE,
        '案件摘要', '原告作为被告公司股东持股39.54%，因公司经营僵局诉请解散公司。',
        '{"displayName": "案件摘要", "formType": "textarea"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'Case'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    -- ----------------------------------------------------------
    -- 本体属性 - CommercialCase
    -- ----------------------------------------------------------
    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/disputeType', 'disputeType', 'DATATYPE', c.id, 'string', 0, 1, FALSE, FALSE,
        '纠纷类型', '公司解散',
        '{"displayName": "纠纷类型", "formType": "text"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'CommercialCase'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/mediationAttempted', 'mediationAttempted', 'DATATYPE', c.id, 'boolean', 0, 1, FALSE, FALSE,
        '是否经过调解程序', 'false',
        '{"displayName": "是否经过调解", "formType": "switch"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'CommercialCase'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    -- ----------------------------------------------------------
    -- 本体属性 - Party
    -- ----------------------------------------------------------
    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/partyName', 'partyName', 'DATATYPE', c.id, 'string', 0, 1, TRUE, FALSE,
        '当事人姓名或名称', '徐某骥',
        '{"displayName": "姓名/名称", "formType": "text"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'Party'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/partyType', 'partyType', 'DATATYPE', c.id, 'string', 0, 1, TRUE, FALSE,
        '当事人类型：自然人、法人、非法人组织', '自然人',
        '{"displayName": "当事人类型", "formType": "select", "allowedValues": ["自然人", "法人", "非法人组织"]}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'Party'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/partyRole', 'partyRole', 'DATATYPE', c.id, 'string', 0, 1, TRUE, FALSE,
        '诉讼角色', '原告',
        '{"displayName": "诉讼角色", "formType": "select", "allowedValues": ["原告", "被告", "第三人", "上诉人", "被上诉人", "申请人", "被申请人"]}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'Party'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/idNumber', 'idNumber', 'DATATYPE', c.id, 'string', 0, 1, FALSE, FALSE,
        '身份证号或统一社会信用代码', '310101199001011234',
        '{"displayName": "身份证号/统一社会信用代码", "formType": "text"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'Party'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/address', 'address', 'DATATYPE', c.id, 'string', 0, 1, FALSE, FALSE,
        '住所地或注册地', '上海市长宁区某路123号',
        '{"displayName": "住所地", "formType": "text"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'Party'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    -- ----------------------------------------------------------
    -- 本体属性 - Court
    -- ----------------------------------------------------------
    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/courtName', 'courtName', 'DATATYPE', c.id, 'string', 0, 1, TRUE, FALSE,
        '法院名称', '上海市第一中级人民法院',
        '{"displayName": "法院名称", "formType": "text"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'Court'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/courtLevel', 'courtLevel', 'DATATYPE', c.id, 'string', 0, 1, FALSE, FALSE,
        '法院级别', '中级人民法院',
        '{"displayName": "法院级别", "formType": "select", "allowedValues": ["最高人民法院", "高级人民法院", "中级人民法院", "基层人民法院", "专门法院"]}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'Court'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/location', 'location', 'DATATYPE', c.id, 'string', 0, 1, FALSE, FALSE,
        '法院所在地', '上海市',
        '{"displayName": "所在地", "formType": "text"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'Court'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    -- ----------------------------------------------------------
    -- 本体属性 - Judge
    -- ----------------------------------------------------------
    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/judgeName', 'judgeName', 'DATATYPE', c.id, 'string', 0, 1, TRUE, FALSE,
        '法官姓名', '张某',
        '{"displayName": "法官姓名", "formType": "text"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'Judge'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/judgeTitle', 'judgeTitle', 'DATATYPE', c.id, 'string', 0, 1, FALSE, FALSE,
        '职务', '审判长',
        '{"displayName": "职务", "formType": "select", "allowedValues": ["审判长", "审判员", "人民陪审员", "书记员"]}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'Judge'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    -- ----------------------------------------------------------
    -- 本体属性 - LegalProvision
    -- ----------------------------------------------------------
    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/provisionId', 'provisionId', 'DATATYPE', c.id, 'string', 0, 1, TRUE, FALSE,
        '条文唯一标识编号', 'L001',
        '{"displayName": "条文编号", "formType": "text"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'LegalProvision'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/articleNumber', 'articleNumber', 'DATATYPE', c.id, 'string', 0, 1, TRUE, FALSE,
        '条款序号', '第69条',
        '{"displayName": "条款序号", "formType": "text"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'LegalProvision'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/provisionContent', 'provisionContent', 'DATATYPE', c.id, 'text', 1, 1, TRUE, FALSE,
        '条文完整内容', '法人清算后的剩余财产，按照法人章程的规定或者法人权力机构的决议处理。',
        '{"displayName": "条文内容", "formType": "textarea"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'LegalProvision'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/lawName', 'lawName', 'DATATYPE', c.id, 'string', 0, 1, TRUE, FALSE,
        '所属法律文件名称', '中华人民共和国民法典',
        '{"displayName": "法律名称", "formType": "text"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'LegalProvision'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/lawType', 'lawType', 'DATATYPE', c.id, 'string', 0, 1, FALSE, FALSE,
        '法律类型', '法律',
        '{"displayName": "法律类型", "formType": "select", "allowedValues": ["法律", "行政法规", "司法解释", "部门规章", "地方性法规"]}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'LegalProvision'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/effectiveDate', 'effectiveDate', 'DATATYPE', c.id, 'date', 0, 1, FALSE, TRUE,
        '法律生效日期', '2021-01-01',
        '{"displayName": "生效日期", "formType": "date"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'LegalProvision'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    -- ----------------------------------------------------------
    -- 本体属性 - JudgmentDocument
    -- ----------------------------------------------------------
    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/documentNumber', 'documentNumber', 'DATATYPE', c.id, 'string', 0, 1, TRUE, FALSE,
        '文书编号', '（2022）沪0105民初21387号',
        '{"displayName": "文书编号", "formType": "text"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'JudgmentDocument'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/documentType', 'documentType', 'DATATYPE', c.id, 'string', 0, 1, FALSE, FALSE,
        '文书类型', '民事判决书',
        '{"displayName": "文书类型", "formType": "select", "allowedValues": ["判决书", "裁定书", "调解书", "决定书", "裁决书"]}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'JudgmentDocument'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/issueDate', 'issueDate', 'DATATYPE', c.id, 'date', 0, 1, FALSE, TRUE,
        '文书作出日期', '2023-05-04',
        '{"displayName": "作出日期", "formType": "date"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'JudgmentDocument'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/judgmentResult', 'judgmentResult', 'DATATYPE', c.id, 'text', 0, 1, FALSE, FALSE,
        '判决结果摘要', '驳回原告全部诉讼请求',
        '{"displayName": "判决结果", "formType": "textarea"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'JudgmentDocument'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/legalBasis', 'legalBasis', 'DATATYPE', c.id, 'text', 0, 1, FALSE, FALSE,
        '法律依据', '《中华人民共和国民法典》第69条',
        '{"displayName": "法律依据", "formType": "textarea"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'JudgmentDocument'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    -- ----------------------------------------------------------
    -- 本体属性 - MediationAgreement
    -- ----------------------------------------------------------
    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/agreementNumber', 'agreementNumber', 'DATATYPE', c.id, 'string', 0, 1, TRUE, FALSE,
        '调解协议编号', 'MA2024001',
        '{"displayName": "协议编号", "formType": "text"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'MediationAgreement'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/performanceMethod', 'performanceMethod', 'DATATYPE', c.id, 'string', 0, 1, FALSE, FALSE,
        '履行方式', '银行转账',
        '{"displayName": "履行方式", "formType": "text"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'MediationAgreement'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/performanceDeadline', 'performanceDeadline', 'DATATYPE', c.id, 'date', 0, 1, FALSE, TRUE,
        '履行期限', '2024-06-30',
        '{"displayName": "履行期限", "formType": "date"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'MediationAgreement'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/judiciallyConfirmed', 'judiciallyConfirmed', 'DATATYPE', c.id, 'boolean', 0, 1, FALSE, FALSE,
        '是否经过司法确认', 'true',
        '{"displayName": "是否司法确认", "formType": "switch"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'MediationAgreement'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    -- ----------------------------------------------------------
    -- 新增本体属性（Court 扩展）
    -- ----------------------------------------------------------
    UPDATE ont_class SET id = id WHERE definition_id = v_def_id AND local_name = 'Court';
    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/jurisdiction', 'jurisdiction', 'DATATYPE', c.id, 'string', 0, 1, FALSE, FALSE, '管辖范围', '上海市辖区内的重大案件', '{"displayName": "管辖范围", "formType": "text"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'Court'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/parentCourt', 'parentCourt', 'DATATYPE', c.id, 'string', 0, 1, FALSE, FALSE, '上级法院名称', '上海市高级人民法院', '{"displayName": "上级法院", "formType": "text"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'Court'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    -- ----------------------------------------------------------
    -- 新增本体属性（Judge 扩展）
    -- ----------------------------------------------------------
    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/specialty', 'specialty', 'DATATYPE', c.id, 'string', 0, 1, FALSE, FALSE, '专业领域', '民商事审判', '{"displayName": "专业领域", "formType": "text"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'Judge'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    -- ----------------------------------------------------------
    -- 新增本体属性（LegalProvision 扩展）
    -- ----------------------------------------------------------
    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/keywords', 'keywords', 'DATATYPE', c.id, 'string', 0, 1, FALSE, FALSE, '关键词标签', '公司解散,公司僵局,判断标准', '{"displayName": "关键词", "formType": "text"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'LegalProvision'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    -- ----------------------------------------------------------
    -- 新增本体属性（JudgmentDocument 扩展）
    -- ----------------------------------------------------------
    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/mainContent', 'mainContent', 'DATATYPE', c.id, 'text', 0, 1, FALSE, FALSE, '主要内容摘要', '经审理查明...', '{"displayName": "正文摘要", "formType": "textarea"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'JudgmentDocument'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/judgmentCourtName', 'judgmentCourtName', 'DATATYPE', c.id, 'string', 0, 1, FALSE, FALSE, '作出法院名称', '上海市第一中级人民法院', '{"displayName": "作出法院", "formType": "text"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'JudgmentDocument'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    -- ----------------------------------------------------------
    -- 新增本体属性（CaseReasoning）
    -- ----------------------------------------------------------
    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/reasoning', 'reasoning', 'DATATYPE', c.id, 'text', 1, 1, TRUE, FALSE, '裁判要旨内容', '公司解散纠纷是股东在穷尽公司自治或其他途径...', '{"displayName": "裁判要旨", "formType": "textarea"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'CaseReasoning'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/guidanceLevel', 'guidanceLevel', 'DATATYPE', c.id, 'string', 0, 1, FALSE, FALSE, '指导级别', '参考', '{"displayName": "指导级别", "formType": "select", "allowedValues": ["典型", "参考", "备查"]}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'CaseReasoning'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/applicableScenario', 'applicableScenario', 'DATATYPE', c.id, 'text', 0, 1, FALSE, FALSE, '适用场景', '股东诉请解散公司时，公司运营良好且股东矛盾可通过其他途径解决的', '{"displayName": "适用场景", "formType": "textarea"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'CaseReasoning'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    -- ----------------------------------------------------------
    -- 新增本体属性（CaseFact）
    -- ----------------------------------------------------------
    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/factDescription', 'factDescription', 'DATATYPE', c.id, 'text', 1, 1, TRUE, FALSE, '事实描述', '2020年3月30日，原告受让被告五位股东持有的股权...', '{"displayName": "事实描述", "formType": "textarea"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'CaseFact'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/factCategory', 'factCategory', 'DATATYPE', c.id, 'string', 0, 1, FALSE, FALSE, '事实类别', '股权转让', '{"displayName": "事实类别", "formType": "text"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'CaseFact'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/factImportance', 'factImportance', 'DATATYPE', c.id, 'string', 0, 1, FALSE, FALSE, '重要程度', 'high', '{"displayName": "重要程度", "formType": "select", "allowedValues": ["high", "medium", "low"]}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'CaseFact'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    -- ----------------------------------------------------------
    -- 新增本体属性（CommercialMediationOrganization）
    -- ----------------------------------------------------------
    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/orgType', 'orgType', 'DATATYPE', c.id, 'string', 0, 1, FALSE, FALSE, '组织类型', '商事调解组织', '{"displayName": "组织类型", "formType": "text"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'CommercialMediationOrganization'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/licenseNumber', 'licenseNumber', 'DATATYPE', c.id, 'string', 0, 1, FALSE, FALSE, '证照编号', '沪商调证字2024001号', '{"displayName": "证照编号", "formType": "text"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'CommercialMediationOrganization'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/establishedDate', 'establishedDate', 'DATATYPE', c.id, 'date', 0, 1, FALSE, TRUE, '成立日期', '2024-01-01', '{"displayName": "成立日期", "formType": "date"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'CommercialMediationOrganization'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/assetAmount', 'assetAmount', 'DATATYPE', c.id, 'decimal', 0, 1, FALSE, FALSE, '资产金额', '500000', '{"displayName": "资产金额", "formType": "number"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'CommercialMediationOrganization'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/mediatorCount', 'mediatorCount', 'DATATYPE', c.id, 'integer', 0, 1, FALSE, FALSE, '调解员数量', '15', '{"displayName": "调解员数量", "formType": "number"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'CommercialMediationOrganization'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    -- ----------------------------------------------------------
    -- 新增本体属性（Mediator）
    -- ----------------------------------------------------------
    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/qualification', 'qualification', 'DATATYPE', c.id, 'string', 0, 1, FALSE, FALSE, '资质类型', '法律职业资格+5年调解经验', '{"displayName": "资质", "formType": "text"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'Mediator'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/organizationName', 'organizationName', 'DATATYPE', c.id, 'string', 0, 1, FALSE, FALSE, '所属组织', '上海国际商事调解中心', '{"displayName": "所属组织", "formType": "text"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'Mediator'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    INSERT INTO ont_property (definition_id, property_uri, local_name, property_type, domain_class_id, range_data_type, min_cardinality, max_cardinality, is_required, is_multiple, description, example, metadata, created_at, updated_at)
    SELECT v_def_id, 'http://legal-ai.cc/ontology/property/yearsExperience', 'yearsExperience', 'DATATYPE', c.id, 'integer', 0, 1, FALSE, FALSE, '从业年限', '5', '{"displayName": "从业年限", "formType": "number"}', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c WHERE c.definition_id = v_def_id AND c.local_name = 'Mediator'
    ON CONFLICT ON CONSTRAINT uk_ont_prop_uri DO UPDATE SET description = EXCLUDED.description, metadata = EXCLUDED.metadata, updated_at = CURRENT_TIMESTAMP;

    RAISE NOTICE 'Inserted ontology properties';

    -- ----------------------------------------------------------
    -- 本体约束
    -- ----------------------------------------------------------
    INSERT INTO ont_constraint (definition_id, class_id, property_id, constraint_type, value, error_message, severity, description, created_at, updated_at)
    SELECT v_def_id, c.id, p.id, 'PATTERN',
        '{"pattern": "^[（(][0-9]{1,4}）)?[地东西南北华中上下]?\\d{2,}[民刑执行经知行赔][初重终辖再简调强执抗不适赔认补他号字]\\d{3,10}号?$"}',
        '案件编号格式不正确', 'WARNING', '案件编号应符合中国法院案号规范',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c LEFT JOIN ont_property p ON p.domain_class_id = c.id AND p.local_name = 'caseNumber'
    WHERE c.definition_id = v_def_id AND c.local_name = 'Case'
    ON CONFLICT DO NOTHING;

    INSERT INTO ont_constraint (definition_id, class_id, property_id, constraint_type, value, error_message, severity, description, created_at, updated_at)
    SELECT v_def_id, c.id, p.id, 'ENUM', '["自然人", "法人", "非法人组织"]',
        '当事人类型必须是：自然人、法人 或 非法人组织', 'ERROR', '确保当事人类型字段的数据质量',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c LEFT JOIN ont_property p ON p.domain_class_id = c.id AND p.local_name = 'partyType'
    WHERE c.definition_id = v_def_id AND c.local_name = 'Party'
    ON CONFLICT DO NOTHING;

    INSERT INTO ont_constraint (definition_id, class_id, property_id, constraint_type, value, error_message, severity, description, created_at, updated_at)
    SELECT v_def_id, c.id, p.id, 'ENUM', '["原告", "被告", "第三人", "上诉人", "被上诉人", "申请人", "被申请人"]',
        '诉讼角色必须在允许范围内', 'ERROR', '确保诉讼角色字段的数据质量',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c LEFT JOIN ont_property p ON p.domain_class_id = c.id AND p.local_name = 'partyRole'
    WHERE c.definition_id = v_def_id AND c.local_name = 'Party'
    ON CONFLICT DO NOTHING;

    INSERT INTO ont_constraint (definition_id, class_id, property_id, constraint_type, value, error_message, severity, description, created_at, updated_at)
    SELECT v_def_id, c.id, p.id, 'ENUM', '["法律", "行政法规", "司法解释", "部门规章", "地方性法规"]',
        '法律类型必须在允许范围内', 'ERROR', '确保法律类型字段的数据质量',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c LEFT JOIN ont_property p ON p.domain_class_id = c.id AND p.local_name = 'lawType'
    WHERE c.definition_id = v_def_id AND c.local_name = 'LegalProvision'
    ON CONFLICT DO NOTHING;

    INSERT INTO ont_constraint (definition_id, class_id, property_id, constraint_type, value, error_message, severity, description, created_at, updated_at)
    SELECT v_def_id, c.id, p.id, 'ENUM', '["判决书", "裁定书", "调解书", "决定书", "裁决书"]',
        '文书类型必须在允许范围内', 'ERROR', '确保裁判文书类型字段的数据质量',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c LEFT JOIN ont_property p ON p.domain_class_id = c.id AND p.local_name = 'documentType'
    WHERE c.definition_id = v_def_id AND c.local_name = 'JudgmentDocument'
    ON CONFLICT DO NOTHING;

    INSERT INTO ont_constraint (definition_id, class_id, property_id, constraint_type, value, error_message, severity, description, created_at, updated_at)
    SELECT v_def_id, c.id, p.id, 'NOT_NULL', '{"enforced": true}',
        '调解协议编号不能为空', 'ERROR', '确保调解协议编号必填',
        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
    FROM ont_class c LEFT JOIN ont_property p ON p.domain_class_id = c.id AND p.local_name = 'agreementNumber'
    WHERE c.definition_id = v_def_id AND c.local_name = 'MediationAgreement'
    ON CONFLICT DO NOTHING;

    RAISE NOTICE 'Inserted ontology constraints';

    -- ----------------------------------------------------------
    -- 版本历史
    -- ----------------------------------------------------------
    INSERT INTO ont_version_history (definition_id, version, change_type, entity_type, entity_id, before_state, after_state, diff_summary, changed_by, changed_at)
    VALUES (
        v_def_id, '2.0.0', 'DEFINITION_CREATED', 'DEFINITION', v_def_id,
        NULL,
        '{"name": "法律知识图谱本体 V2", "version": "2.0.0", "classCount": 20, "propertyCount": 55, "constraintCount": 6}',
        '创建法律知识图谱本体 V2，定义20个实体类、55个属性、6个约束',
        'system', CURRENT_TIMESTAMP
    );

    RAISE NOTICE 'Ontology setup complete for definition_id=%', v_def_id;

END $$;


-- ============================================================
-- 第四部分: 提示词模板初始化
-- ============================================================

INSERT INTO prompt_template (code, name, description, type, system_prompt, user_prompt_template, response_format, enabled, model, sort, tags, extra_config, created_at, updated_at)
VALUES
('LEGAL_ENTITY_EXTRACT', '法律实体抽取', '从裁判文书中抽取法律领域实体', 'entity_extract',
 '你是一名专业的法律信息抽取专家。请从给定的裁判文书中抽取所有实体。',
 '请从以下裁判文书中抽取实体：\n\n{content}\n\n要求：\n1. 抽取案件、当事人、法院、法官、法律条文等实体\n2. 每个实体需包含类型和属性\n3. 以JSON格式返回',
 '{"type": "object", "properties": {"entities": {"type": "array", "items": {"type": "object", "properties": {"type": {"type": "string"}, "name": {"type": "string"}, "properties": {"type": "object"}}}}}}',
 TRUE, NULL, 1, '["legal", "entity"]', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('LEGAL_EDGE_EXTRACT', '法律关系抽取', '从裁判文书中抽取实体之间的关系', 'edge_extract',
 '你是一名专业的法律关系分析专家。请分析给定裁判文书中实体之间的关系。',
 '请分析以下裁判文书中实体之间的关系：\n\n{content}\n\n要求：\n1. 识别实体之间的法律关系\n2. 每个关系需包含类型、源实体、目标实体\n3. 以JSON格式返回',
 '{"type": "object", "properties": {"edges": {"type": "array", "items": {"type": "object", "properties": {"type": {"type": "string"}, "source": {"type": "string"}, "target": {"type": "string"}, "properties": {"type": "object"}}}}}}',
 TRUE, NULL, 2, '["legal", "edge"]', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('LEGAL_SUMMARY', '法律文书摘要', '生成裁判文书的摘要', 'summary',
 '你是一名专业的法律文书摘要专家。请为给定的裁判文书生成简明摘要。',
 '请为以下裁判文书生成摘要（200字以内）：\n\n{content}',
 '{"type": "object", "properties": {"summary": {"type": "string"}, "keyPoints": {"type": "array", "items": {"type": "string"}}}}',
 TRUE, NULL, 3, '["legal", "summary"]', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

('LEGAL_DEDUPE', '实体去重', '识别并合并重复的实体', 'dedupe',
 '你是一名专业的法律知识图谱工程师。请识别并合并重复的实体。',
 '请识别以下实体列表中的重复项并合并：\n\n{entities}\n\n要求：\n1. 基于名称、类型等特征识别重复实体\n2. 保留最完整的实体信息\n3. 以JSON格式返回合并结果',
 '{"type": "object", "properties": {"merged": {"type": "array"}, "duplicates": {"type": "array"}}}',
 TRUE, NULL, 4, '["legal", "dedupe"]', NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);


-- ============================================================
-- 第五部分: 全局自定义指令
-- ============================================================

INSERT INTO custom_instruction (graph_id, instruction, enabled, created_at, updated_at)
VALUES
(NULL, '请在抽取法律实体时，确保准确识别当事人是自然人还是法人组织。', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
(NULL, '案件编号必须符合中国法院案号规范：年份+法院代码+案件类型+序号。', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);


-- ============================================================
-- 验证数据统计
-- ============================================================

SELECT 'Users' as table_name, count(*) as count FROM sys_user
UNION ALL SELECT 'Roles', count(*) FROM sys_role
UNION ALL SELECT 'Menus', count(*) FROM sys_menu
UNION ALL SELECT 'UserRoles', count(*) FROM sys_user_role
UNION ALL SELECT 'RoleMenus', count(*) FROM sys_role_menu
UNION ALL SELECT 'Graphs', count(*) FROM graphiti_graph_metadata
UNION ALL SELECT 'OntDefinitions', count(*) FROM ont_definition
UNION ALL SELECT 'OntClasses', count(*) FROM ont_class
UNION ALL SELECT 'OntProperties', count(*) FROM ont_property
UNION ALL SELECT 'OntConstraints', count(*) FROM ont_constraint
UNION ALL SELECT 'OntVersionHistory', count(*) FROM ont_version_history
UNION ALL SELECT 'PromptTemplates', count(*) FROM prompt_template
UNION ALL SELECT 'CustomInstructions', count(*) FROM custom_instruction;


-- ============================================================
-- 初始化完成
-- ============================================================
