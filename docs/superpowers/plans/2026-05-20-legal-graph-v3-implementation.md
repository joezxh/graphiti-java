# 法律知识图谱 V3.0.0 实现规划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将法律知识图谱从 V2.0.0 升级至 V3.0.0，完成数据库 Schema 扩展、Neo4j 索引增强、后端 Java 服务 V3 字段支持、前端 Vue/TypeScript 类型与 UI 适配。

**Architecture:** 采用增量式架构变更：数据库层独立先行（Phase 1），后端核心层（Phase 2-3）扩展现有 Cypher 查询和 VO 类，前端层（Phase 4）扩展 TypeScript 类型和 Vue 组件。向后兼容现有字段，新字段可选填充。

**Tech Stack:** Java/Spring Boot (后端), Neo4j/Cypher (图数据库), PostgreSQL/MySQL (元数据), Vue 3/TypeScript/Ant Design Vue (前端)

---

## 前置条件确认

在开始前，请确认以下文件已被审阅：

- 设计文档：`docs/superpowers/specs/2026-05-20-legal-graph-v3-design.md`（特别是第 11-14 章）
- 现有 Neo4j Schema：`sql/neo4j/init.cypher`（已有 V3 属性字段定义）
- 现有 Java 服务：`graphiti-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/EpisodeServiceImpl.java`
- 现有前端 API：`graphiti-web/src/api/graph.ts`

---

## 实现顺序总览

```
Phase 1: SQL Schema 扩展（独立于代码，可先执行）
Phase 2: 后端核心层 - Episode V3 字段 + Cypher 更新
Phase 3: 后端增强层 - Community V3 字段 + 元数据 API
Phase 4: 前端层 - TypeScript 类型 + Vue 组件适配
```

---

## Phase 1: SQL Schema 扩展

> 此阶段独立于代码变更，可最先执行。数据库向后兼容，不影响现有业务。

### Task 1.1: PostgreSQL 新增 4 张元数据表

**文件:**
- 创建: `sql/postgresql/schema-v3.sql`（V3 Schema 增量文件，不修改原 schema.sql）
- 修改: `sql/postgresql/init-data.sql`（追加 V3.0.0 元数据初始化）

**步骤:**

- [ ] **Step 1: 创建 `sql/postgresql/schema-v3.sql`**

```sql
-- ============================================================
-- 法律知识图谱 V3.0.0: PostgreSQL Schema 增量扩展
-- 仅包含新增的 4 张元数据表，不修改现有表
-- ============================================================

-- ---------- ont_community_type: 社区类型维度表 ----------
CREATE TABLE ont_community_type (
    id BIGSERIAL PRIMARY KEY,
    definition_id BIGINT NOT NULL,
    type_code VARCHAR(32) NOT NULL,
    type_name VARCHAR(128) NOT NULL,
    type_name_en VARCHAR(64),
    category VARCHAR(32) NOT NULL DEFAULT 'domain',
        -- domain: 法律领域（层级嵌套）
        -- jurisdiction: 司法管辖区
        -- practice: 应用场景
    description TEXT,
    parent_type_code VARCHAR(32),
    sort_order INT DEFAULT 0,
    metadata JSONB,
        -- {icon, color, displayPriority}
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_community_type_code UNIQUE (definition_id, type_code),
    CONSTRAINT fk_community_type_definition FOREIGN KEY (definition_id) REFERENCES ont_definition(id) ON DELETE CASCADE
);

CREATE INDEX idx_community_type_definition ON ont_community_type(definition_id);
CREATE INDEX idx_community_type_category ON ont_community_type(category);
CREATE INDEX idx_community_type_parent ON ont_community_type(parent_type_code);

COMMENT ON TABLE ont_community_type IS '社区类型维度表 — 定义法律知识图谱中社区的分类体系';

-- ---------- ont_episode_type: 剧集类型维度表 ----------
CREATE TABLE ont_episode_type (
    id BIGSERIAL PRIMARY KEY,
    definition_id BIGINT NOT NULL,
    type_code VARCHAR(32) NOT NULL,
    type_name VARCHAR(128) NOT NULL,
    type_name_en VARCHAR(64),
    legal_process VARCHAR(32),
        -- litigation: 诉讼 | mediation: 调解 | arbitration: 仲裁 | execution: 执行
    stage_label VARCHAR(32),
        -- 立案 | 庭审 | 调解 | 判决 | 执行
    court_level VARCHAR(32),
        -- 一审 | 二审 | 再审 | 死刑复核（仅审判程序有值，ADR类为空）
    is_trial_stage BOOLEAN DEFAULT FALSE,
    description TEXT,
    sort_order INT DEFAULT 0,
    metadata JSONB,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_episode_type_code UNIQUE (definition_id, type_code),
    CONSTRAINT fk_episode_type_definition FOREIGN KEY (definition_id) REFERENCES ont_definition(id) ON DELETE CASCADE
);

CREATE INDEX idx_episode_type_definition ON ont_episode_type(definition_id);
CREATE INDEX idx_episode_type_legal_process ON ont_episode_type(legal_process);
CREATE INDEX idx_episode_type_court_level ON ont_episode_type(court_level);

COMMENT ON TABLE ont_episode_type IS '剧集类型维度表 — 定义法律过程中事件的分类体系';

-- ---------- ont_entity_category: 实体分类层次表 ----------
CREATE TABLE ont_entity_category (
    id BIGSERIAL PRIMARY KEY,
    definition_id BIGINT NOT NULL,
    category_code VARCHAR(32) NOT NULL,
    category_name VARCHAR(128) NOT NULL,
    category_level INT NOT NULL DEFAULT 1,
        -- 1=一级, 2=二级
    parent_category_code VARCHAR(32),
    entity_type_scope TEXT,
        -- JSON数组: ["Case", "Court"]
    default_attributes JSONB,
    description TEXT,
    sort_order INT DEFAULT 0,
    metadata JSONB,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_entity_category_code UNIQUE (definition_id, category_code),
    CONSTRAINT fk_entity_category_definition FOREIGN KEY (definition_id) REFERENCES ont_definition(id) ON DELETE CASCADE
);

CREATE INDEX idx_entity_category_definition ON ont_entity_category(definition_id);
CREATE INDEX idx_entity_category_level ON ont_entity_category(category_level);
CREATE INDEX idx_entity_category_parent ON ont_entity_category(parent_category_code);

COMMENT ON TABLE ont_entity_category IS '实体分类层次表 — 定义法律实体的层级分类体系';

-- ---------- ont_relationship_meta: 关系类型元数据表 ----------
CREATE TABLE ont_relationship_meta (
    id BIGSERIAL PRIMARY KEY,
    definition_id BIGINT NOT NULL,
    relationship_type VARCHAR(64) NOT NULL,
    relationship_name VARCHAR(128) NOT NULL,
    relationship_name_en VARCHAR(64),
    source_entity_types TEXT,
        -- JSON数组: ["Case", "JudgmentDocument"]
    target_entity_types TEXT,
        -- JSON数组: ["LegalProvision"]
    is_directional BOOLEAN DEFAULT TRUE,
    is_transitive BOOLEAN DEFAULT FALSE,
    multiplicity VARCHAR(16) DEFAULT 'many-to-many',
        -- one-to-one | one-to-many | many-to-many
    default_weight DECIMAL(5,4) DEFAULT 1.0000,
    validity_period JSONB,
        -- {hasPeriod: boolean, defaultDays: integer|null}
    description TEXT,
    example_cypher TEXT,
    sort_order INT DEFAULT 0,
    metadata JSONB,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_relationship_type UNIQUE (definition_id, relationship_type),
    CONSTRAINT fk_relationship_meta_definition FOREIGN KEY (definition_id) REFERENCES ont_definition(id) ON DELETE CASCADE
);

CREATE INDEX idx_relationship_meta_definition ON ont_relationship_meta(definition_id);

COMMENT ON TABLE ont_relationship_meta IS '关系类型元数据表 — 定义预置关系类型的语义属性和约束';
```

- [ ] **Step 2: 创建 `sql/postgresql/init-data-v3.sql`（V3 元数据初始化脚本）**

> 此文件包含完整的 V3.0.0 元数据初始化语句，可在 `schema-v3.sql` 执行后独立运行。

```sql
-- ============================================================
-- 法律知识图谱 V3.0.0: 元数据初始化
-- 适用于 PostgreSQL 13+
-- 依赖: schema-v3.sql 已执行完毕
-- ============================================================

DO $$
DECLARE
    v_def_id BIGINT := 1;  -- legal-knowledge-graph 的 definition_id
BEGIN

-- ====== 一、ont_community_type 初始化 ======

-- 1.1 法律领域 (domain) - 层级嵌套
INSERT INTO ont_community_type (definition_id, type_code, type_name, type_name_en, category, parent_type_code, sort_order, metadata, description) VALUES
(v_def_id, 'DOMAIN_ROOT', '法律领域', 'Legal Domain', 'domain', NULL, 0,
 '{"icon": "book", "color": "#1565C0"}', '法律AI顶级社区，所有法律专题子社区的父社区'),

(v_def_id, 'DOMAIN_CIVIL', '民商事', 'Civil & Commercial', 'domain', 'DOMAIN_ROOT', 1,
 '{"icon": "scale", "color": "#2E7D32"}', '涵盖民法、商法范围内的纠纷解决'),

(v_def_id, 'DOMAIN_CRIMINAL', '刑事法律', 'Criminal Law', 'domain', 'DOMAIN_ROOT', 2,
 '{"icon": "shield", "color": "#C62828"}', '涵盖刑法及刑事诉讼法相关'),

(v_def_id, 'DOMAIN_ADMIN', '行政法律', 'Administrative Law', 'domain', 'DOMAIN_ROOT', 3,
 '{"icon": "building", "color": "#6A1B9A"}', '涵盖行政法、行政诉讼法'),

(v_def_id, 'DOMAIN_IP', '知识产权', 'Intellectual Property', 'domain', 'DOMAIN_ROOT', 4,
 '{"icon": "lightbulb", "color": "#F57F17"}', '涵盖专利、商标、著作权'),

(v_def_id, 'DOMAIN_LABOR', '劳动法律', 'Labor Law', 'domain', 'DOMAIN_ROOT', 5,
 '{"icon": "briefcase", "color": "#00838F"}', '涵盖劳动法、劳动合同法'),

(v_def_id, 'DOMAIN_MEDIATION', '商事调解', 'Commercial Mediation', 'domain', 'DOMAIN_ROOT', 6,
 '{"icon": "handshake", "color": "#AD1457"}', '涵盖多元化纠纷解决机制（ADR）'),

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
     is_directional, is_transitive, multiplicity, default_weight, validity_period, description, example_cypher)
VALUES
-- HAS_COMMUNITY
(v_def_id, 'HAS_COMMUNITY', '所属社区',
 '["Case", "LegalProvision", "Court", "JudgmentDocument"]', '["Community"]',
 TRUE, FALSE, 'many-to-many', 0.9000,
 '{"hasPeriod": false}',
 '法律实体归入对应社区',
 'MATCH (c:Case {caseNumber: "（2023）沪01民终11293号"})-[:HAS_COMMUNITY]->(comm:Community {name: "公司解散纠纷"})'),

-- PARENT_OF
(v_def_id, 'PARENT_OF', '父社区',
 '["Community"]', '["Community"]',
 TRUE, FALSE, 'one-to-many', 1.0000,
 '{"hasPeriod": false}',
 '社区层级关系',
 'MATCH (parent:Community)-[:PARENT_OF]->(child:Community {name: "公司解散纠纷"})'),

-- MENTIONS
(v_def_id, 'MENTIONS', '涉及/提及',
 '["Episode"]', '["Case", "Party", "Court", "LegalProvision", "Evidence", "JudgmentDocument"]',
 TRUE, FALSE, 'many-to-many', 1.0000,
 '{"hasPeriod": false}',
 '事件提及/涉及的关键法律实体',
 'MATCH (ep:Episode)-[:MENTIONS {entity_role: "诉讼标的"}]->(ca:Case)'),

-- NEXT_EPISODE
(v_def_id, 'NEXT_EPISODE', '后续事件',
 '["Episode"]', '["Episode"]',
 TRUE, FALSE, 'one-to-many', 1.0000,
 '{"hasPeriod": false}',
 '法律过程中事件的时序关系',
 'MATCH (ep1:Episode)-[:NEXT_EPISODE {sequence_order: 3}]->(ep2:Episode)'),

-- CITES
(v_def_id, 'CITES', '引用/依据',
 '["JudgmentDocument", "CaseReasoning"]', '["LegalProvision"]',
 TRUE, FALSE, 'many-to-many', 0.9500,
 '{"hasPeriod": true, "defaultDays": null}',
 '裁判文书引用法条作为判决依据',
 'MATCH (jd:JudgmentDocument)-[:CITES {basisType: "判决依据"}]->(lp:LegalProvision)'),

-- INVOLVES
(v_def_id, 'INVOLVES', '涉及',
 '["Case", "Episode"]', '["Party", "LegalProvision"]',
 TRUE, FALSE, 'many-to-many', 0.8000,
 '{"hasPeriod": false}',
 '案件或事件涉及的当事人或法条',
 'MATCH (ca:Case)-[:INVOLVES {partyRole: "被告"}]->(p:Party)'),

-- BELONGS_TO
(v_def_id, 'BELONGS_TO', '属于',
 '["LegalProvision"]', '["LegalDocument"]',
 TRUE, FALSE, 'many-to-many', 1.0000,
 '{"hasPeriod": true, "defaultDays": null}',
 '法条属于某法律法规文件',
 'MATCH (lp:LegalProvision)-[:BELONGS_TO]->(ld:LegalDocument)'),

-- PRECEDES
(v_def_id, 'PRECEDES', '先例',
 '["Case", "JudgmentDocument"]', '["Case", "JudgmentDocument"]',
 TRUE, TRUE, 'many-to-many', 0.7000,
 '{"hasPeriod": false}',
 '案件或裁判作为后续案件参考的先例',
 'MATCH (ca1:Case)-[:PRECEDES {citation_count: 15}]->(ca2:Case)'),

-- REPRESENTS
(v_def_id, 'REPRESENTS', '代理',
 '["Lawyer"]', '["Party"]',
 TRUE, FALSE, 'many-to-many', 1.0000,
 '{"hasPeriod": true, "defaultDays": null}',
 '律师代理当事人参与诉讼',
 'MATCH (law:Lawyer)-[:REPRESENTS {caseNumber: "（2023）沪01民终11293号"}]->(p:Party)'),

-- PRESIDES_OVER
(v_def_id, 'PRESIDES_OVER', '主持审理',
 '["Judge"]', '["Case"]',
 TRUE, FALSE, 'many-to-many', 1.0000,
 '{"hasPeriod": true, "defaultDays": null}',
 '法官主持案件审理',
 'MATCH (j:Judge)-[:PRESIDES_OVER {courtLevel: "二审"}]->(ca:Case)'),

-- PARTY_OF
(v_def_id, 'PARTY_OF', '当事人关系',
 '["Party"]', '["Party"]',
 FALSE, FALSE, 'many-to-many', 1.0000,
 '{"hasPeriod": true, "defaultDays": null}',
 '案件中当事人之间的关系（原告/被告/第三人）',
 'MATCH (p1:Party)-[:PARTY_OF {relationship: "原告", caseNumber: "（2023）"}]->(p2:Party)'),

-- SUBSTANTIATES
(v_def_id, 'SUBSTANTIATES', '证明',
 '["Evidence"]', '["CaseFact"]',
 TRUE, FALSE, 'many-to-many', 0.9000,
 '{"hasPeriod": false}',
 '证据用于证明案件事实',
 'MATCH (ev:Evidence)-[:SUBSTANTIATES {weight: 0.8}]->(f:CaseFact)'),

-- AFFIRMED_BY
(v_def_id, 'AFFIRMED_BY', '被司法确认',
 '["MediationAgreement"]', '["JudgmentDocument"]',
 FALSE, FALSE, 'one-to-one', 1.0000,
 '{"hasPeriod": true, "defaultDays": null}',
 '调解协议经法院确认获得强制执行力',
 'MATCH (ma:MediationAgreement)-[:AFFIRMED_BY]->(jd:JudgmentDocument)');

END $$;
```

- [ ] **Step 3: 创建 MySQL 版本的 `sql/mysql/schema-v3.sql`**

> MySQL 语法差异：使用 `BIGINT AUTO_INCREMENT` 替代 `BIGSERIAL`，使用 `JSON` 替代 `JSONB`，移除 `COMMENT ON` 语句。

- [ ] **Step 4: 创建 MySQL 版本的 `sql/mysql/init-data-v3.sql`**

> 同样使用 MySQL 兼容语法，JSON 字段使用双引号字符串（MySQL JSON 函数）。

- [ ] **Step 5: 在 `sql/neo4j/init.cypher` 中追加 V3 增强部分（如果尚未存在）**

> 检查 `sql/neo4j/init.cypher` 是否已有以下内容，如有则跳过，如无则追加：
> - `PARENT_OF` 关系初始化
> - `community_type` → `legal_domain` 映射迁移 Cypher
> - Episode 节点回填 `episode_type` / `legal_process` / `court_level` 的 CASE 语句

- [ ] **Step 6: Commit**

```bash
git add sql/postgresql/schema-v3.sql sql/postgresql/init-data-v3.sql sql/mysql/schema-v3.sql sql/mysql/init-data-v3.sql
git commit -m "feat(schema): add V3.0.0 metadata tables (ont_community_type, ont_episode_type, ont_entity_category, ont_relationship_meta)"
```

---

## Phase 2: 后端核心层 — Episode V3 字段 + Cypher 更新

### Task 2.1: 扩展 `EpisodeServiceImpl.java` — `createEpisode` 方法支持 V3 字段

**文件:**
- 修改: `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/EpisodeServiceImpl.java`

**前置阅读:**
- 文件中的 `createEpisode` 方法（当前签名：`public EpisodeInfoRespVO createEpisode(String graphId, Map<String, Object> episodeData)`）
- `convertToEpisodeInfo` 辅助方法

**步骤:**

- [ ] **Step 1: 在 `createEpisode` 方法中，从 `episodeData` 提取 V3 字段**

找到现有代码中的 `String name = (String) episodeData.get("name");` 这一行，在其后添加：

```java
// V3.0.0 新增字段提取
String episodeType = (String) episodeData.get("episode_type");
String legalProcess = (String) episodeData.get("legal_process");
String stageLabel = (String) episodeData.get("stage_label");
String courtLevel = (String) episodeData.get("court_level");
Boolean isTrialStage = episodeData.get("is_trial_stage") != null
    ? (Boolean) episodeData.get("is_trial_stage") : false;
String startTimeStr = (String) episodeData.get("start_time");
String endTimeStr = (String) episodeData.get("end_time");
String caseId = (String) episodeData.get("case_id");
```

- [ ] **Step 2: 在调用 `graphNeo4jService.createEpisode` 时传入 V3 参数**

找到现有的：
```java
Map<String, Object> createdEpisode = graphNeo4jService.createEpisode(
    graphId, uuid, name != null ? name : "", source, sourceDescription, content, properties);
```

替换为（保持原有参数签名，向后兼容）：

```java
// V3.0.0: 扩展 Map 参数传入 V3 字段
Map<String, Object> episodeParams = new HashMap<>(properties);
episodeParams.put("episode_type", episodeType);
episodeParams.put("legal_process", legalProcess);
episodeParams.put("stage_label", stageLabel);
episodeParams.put("court_level", courtLevel);
episodeParams.put("is_trial_stage", isTrialStage);
episodeParams.put("start_time", startTimeStr);
episodeParams.put("end_time", endTimeStr);
episodeParams.put("case_id", caseId);

Map<String, Object> createdEpisode = graphNeo4jService.createEpisodeV3(
    graphId, uuid, name != null ? name : "", source, sourceDescription, content, episodeParams);
```

> 注意：如果 `graphNeo4jService` 已有可扩展的 `createEpisode` 重载，直接使用现有方法。否则跳至 Task 2.2 先更新 `GraphNeo4jService`。

- [ ] **Step 3: 在 `convertToEpisodeInfo` 方法中添加 V3 字段映射**

找到 `convertToEpisodeInfo` 方法，在 `respVO.setContent(...)` 后添加：

```java
// V3.0.0 字段映射
respVO.setEpisodeType((String) row.get("episode_type"));
respVO.setLegalProcess((String) row.get("legal_process"));
respVO.setStageLabel((String) row.get("stage_label"));
respVO.setCourtLevel((String) row.get("court_level"));
respVO.setIsTrialStage(row.get("is_trial_stage") != null ? (Boolean) row.get("is_trial_stage") : false);

// 处理 start_time / end_time 时间戳
if (row.get("start_time") != null) {
    Object st = row.get("start_time");
    if (st instanceof java.time.LocalDateTime) {
        respVO.setStartTime((java.time.LocalDateTime) st);
    } else if (st instanceof java.util.Date) {
        respVO.setStartTime(((java.util.Date) st).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
    }
}
if (row.get("end_time") != null) {
    Object et = row.get("end_time");
    if (et instanceof java.time.LocalDateTime) {
        respVO.setEndTime((java.time.LocalDateTime) et);
    } else if (et instanceof java.util.Date) {
        respVO.setEndTime(((java.util.Date) et).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
    }
}
```

- [ ] **Step 4: 更新 `EpisodeInfoRespVO.java` — 新增 V3 字段**

**文件:**
- 修改: `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/vo/episode/EpisodeInfoRespVO.java`

在现有字段（uuid, name, groupId, source, sourceDescription, content 等）后追加：

```java
// V3.0.0 新增字段
@Schema(description = "Episode 类型代码 (V3): EP_TRIAL_1ST, EP_MEDIATION_NEGOTIATION, etc.")
private String episodeType;

@Schema(description = "法律程序 (V3): litigation|mediation|arbitration|execution")
private String legalProcess;

@Schema(description = "阶段标签 (V3): 立案|庭审|调解|判决|执行")
private String stageLabel;

@Schema(description = "审级 (V3): 一审|二审|再审|null")
private String courtLevel;

@Schema(description = "是否审判阶段 (V3)")
private Boolean isTrialStage;

@Schema(description = "开始时间 (V3)")
private LocalDateTime startTime;

@Schema(description = "结束时间 (V3)")
private LocalDateTime endTime;
```

同时确保文件顶部有：
```java
import java.time.LocalDateTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;
```

- [ ] **Step 5: Commit**

```bash
git add graphiti-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/EpisodeServiceImpl.java graphiti-module-core/src/main/java/com/graphiti/module/graphiti/vo/episode/EpisodeInfoRespVO.java
git commit -m "feat(episode): add V3 fields to createEpisode and EpisodeInfoRespVO"
```

---

### Task 2.2: 扩展 `GraphNeo4jService.java` — Episode 查询 Cypher 增加 V3 字段

**文件:**
- 修改: `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/service/GraphNeo4jService.java`

**前置阅读:**
- `getEpisodesByGraphId` 方法（当前 RETURN 子句）
- `getEpisodeByUuid` 方法
- `createEpisode` 方法
- `getGraphStats` 方法

**步骤:**

- [ ] **Step 1: 在 `GraphNeo4jService` 中新增 `createEpisodeV3` 方法**

在 `createEpisode` 方法后添加：

```java
/**
 * 创建 Episode 节点（V3.0.0 扩展版，支持法律专业字段）
 * @param graphId 图谱ID
 * @param uuid 节点UUID
 * @param name 事件名称
 * @param source 来源
 * @param sourceDescription 来源描述
 * @param content 事件内容（叙事性描述）
 * @param v3Params V3.0.0 扩展参数 Map，包含：
 *                  episode_type, legal_process, stage_label, court_level,
 *                  is_trial_stage, start_time, end_time, case_id
 * @return 创建结果 Map
 */
public Map<String, Object> createEpisodeV3(String graphId, String uuid, String name,
        String source, String sourceDescription, String content,
        Map<String, Object> v3Params) {
    Map<String, Object> params = new HashMap<>();
    params.put("graphId", graphId);
    params.put("uuid", uuid);
    params.put("name", name);
    params.put("source", source != null ? source : "");
    params.put("sourceDescription", sourceDescription != null ? sourceDescription : "");
    params.put("content", content != null ? content : "");
    params.put("validAt", System.currentTimeMillis());

    StringBuilder cypherBuilder = new StringBuilder();
    cypherBuilder.append("CREATE (e:Episode {");
    cypherBuilder.append("graph_id: $graphId, uuid: $uuid, name: $name, ");
    cypherBuilder.append("source: $source, source_description: $sourceDescription, ");
    cypherBuilder.append("content: $content, ");
    cypherBuilder.append("valid_at: $validAt, created_at: timestamp(), ");
    cypherBuilder.append("episode_type: $episode_type, ");
    cypherBuilder.append("legal_process: $legal_process, ");
    cypherBuilder.append("stage_label: $stage_label, ");
    cypherBuilder.append("court_level: $court_level, ");
    cypherBuilder.append("is_trial_stage: $is_trial_stage, ");
    cypherBuilder.append("case_id: $case_id");
    cypherBuilder.append("}) ");
    cypherBuilder.append("RETURN e.uuid as uuid, e.name as name, ");
    cypherBuilder.append("e.source as source, e.source_description as source_description, ");
    cypherBuilder.append("e.content as content, e.created_at as created_at, ");
    cypherBuilder.append("e.valid_at as valid_at, e.graph_id as graph_id, ");
    cypherBuilder.append("e.episode_type as episode_type, ");
    cypherBuilder.append("e.legal_process as legal_process, ");
    cypherBuilder.append("e.stage_label as stage_label, ");
    cypherBuilder.append("e.court_level as court_level, ");
    cypherBuilder.append("e.is_trial_stage as is_trial_stage, ");
    cypherBuilder.append("e.start_time as start_time, ");
    cypherBuilder.append("e.end_time as end_time, ");
    cypherBuilder.append("e.case_id as case_id");

    params.put("episode_type", v3Params.get("episode_type"));
    params.put("legal_process", v3Params.get("legal_process"));
    params.put("stage_label", v3Params.get("stage_label"));
    params.put("court_level", v3Params.get("court_level"));
    params.put("is_trial_stage", v3Params.getOrDefault("is_trial_stage", false));
    params.put("case_id", v3Params.get("case_id"));

    // 处理 start_time / end_time 时间转换
    Object startTimeRaw = v3Params.get("start_time");
    Object endTimeRaw = v3Params.get("end_time");
    if (startTimeRaw != null) {
        params.put("start_time", parseDateTimeParam(startTimeRaw));
    }
    if (endTimeRaw != null) {
        params.put("end_time", parseDateTimeParam(endTimeRaw));
    }

    try (Transaction tx = driver.session()) {
        Result result = tx.run(cypherBuilder.toString(), params);
        if (result.hasNext()) {
            return result.next().asMap();
        }
        return new HashMap<>();
    } catch (Exception e) {
        log.error("Failed to create episode V3 in graph [{}]: {}", graphId, e.getMessage(), e);
        throw new RuntimeException("创建 Episode 失败: " + e.getMessage(), e);
    }
}

/**
 * 解析日期时间参数，支持 String (ISO-8601) 和 LocalDateTime
 */
private Object parseDateTimeParam(Object value) {
    if (value == null) return null;
    if (value instanceof String) {
        try {
            return LocalDateTime.parse((String) value);
        } catch (Exception e) {
            return value;
        }
    }
    if (value instanceof LocalDateTime) {
        return value;
    }
    return value.toString();
}
```

- [ ] **Step 2: 更新 `getEpisodesByGraphId` 的 RETURN 子句**

找到 `getEpisodesByGraphId` 中的 Cypher RETURN 行：

现有（需确认）：
```java
"RETURN e.uuid as uuid, e.name as name, e.source as source, " +
"e.source_description as source_description, e.content as content, " +
"e.created_at as created_at, e.valid_at as valid_at, e.graph_id as graph_id ";
```

替换为：
```java
"RETURN e.uuid as uuid, e.name as name, e.source as source, " +
"e.source_description as source_description, e.content as content, " +
"e.created_at as created_at, e.valid_at as valid_at, e.graph_id as graph_id, " +
"e.episode_type as episode_type, " +
"e.legal_process as legal_process, " +
"e.stage_label as stage_label, " +
"e.court_level as court_level, " +
"e.is_trial_stage as is_trial_stage, " +
"e.start_time as start_time, " +
"e.end_time as end_time, " +
"e.case_id as case_id ";
```

- [ ] **Step 3: 更新 `getEpisodeByUuid` 的 RETURN 子句**

找到 `getEpisodeByUuid` 中的 Cypher RETURN 行，在现有字段后追加相同的 V3 字段。

- [ ] **Step 4: 修复 `getGraphStats` — 统计 Episode 数量**

找到 `getGraphStats` 方法（或 `getGraphStatsCypher` 常量），在现有 RETURN 子句中追加 `count(e) as episodeCount`：

```java
// 在 entityCount 和 edgeCount 的查询后追加
"MATCH (e:Episode {graph_id: $graphId}) " +
"WITH entityCount, edgeCount, count(e) AS episodeCount " +
"MATCH (c:Community {graph_id: $graphId}) " +
"RETURN entityCount, edgeCount, episodeCount, count(c) AS communityCount";
```

- [ ] **Step 5: Commit**

```bash
git add graphiti-module-core/src/main/java/com/graphiti/module/graphiti/service/GraphNeo4jService.java
git commit -m "feat(neo4j): add V3 Episode fields to createEpisodeV3 and query methods"
```

---

## Phase 3: 后端增强层 — Community V3 + 元数据 API

### Task 3.1: 创建 V3 Community VO 类

**文件:**
- 创建: `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/vo/community/CommunityInfoRespVO.java`
- 创建: `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/vo/community/CommunityFilterReqVO.java`

**步骤:**

- [ ] **Step 1: 创建 `CommunityInfoRespVO.java`**

```java
package com.graphiti.module.graphiti.vo.community;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Accessors(chain = true)
@Schema(description = "社区信息响应 (V3.0.0)")
public class CommunityInfoRespVO {

    @Schema(description = "UUID")
    private String uuid;

    @Schema(description = "社区名称")
    private String name;

    @Schema(description = "社区类型（旧字段，兼容用）")
    private String communityType;

    // V3.0.0 新增字段
    @Schema(description = "法律领域代码: DOMAIN_CIVIL, DOMAIN_CRIMINAL, etc.")
    private String legalDomain;

    @Schema(description = "司法管辖区代码: JURISDICTION_CN, etc.")
    private String jurisdiction;

    @Schema(description = "应用场景代码: PRACTICE_JUDICIAL, PRACTICE_MEDIATION, etc.")
    private String practiceType;

    @Schema(description = "父社区 UUID")
    private String parentCommunityUuid;

    @Schema(description = "摘要（LLM 生成）")
    private String summary;

    @Schema(description = "成员数量")
    private Integer memberCount;

    @Schema(description = "关键法条 ID 列表")
    private List<String> keyProvisions;

    @Schema(description = "元数据: {icon, color, displayPriority}")
    private Map<String, Object> metadata;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 2: 创建 `CommunityFilterReqVO.java`**

```java
package com.graphiti.module.graphiti.vo.community;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "社区过滤请求 (V3.0.0)")
public class CommunityFilterReqVO {

    @Schema(description = "法律领域过滤: DOMAIN_CIVIL, etc.")
    private String legalDomain;

    @Schema(description = "司法管辖区过滤: JURISDICTION_CN, etc.")
    private String jurisdiction;

    @Schema(description = "应用场景过滤: PRACTICE_JUDICIAL, etc.")
    private String practiceType;

    @Schema(description = "父社区 UUID")
    private String parentCommunityUuid;

    @Schema(description = "关键词搜索（匹配 name）")
    private String keyword;
}
```

- [ ] **Step 3: Commit**

```bash
git add graphiti-module-core/src/main/java/com/graphiti/module/graphiti/vo/community/CommunityInfoRespVO.java graphiti-module-core/src/main/java/com/graphiti/module/graphiti/vo/community/CommunityFilterReqVO.java
git commit -m "feat(community): add V3.0.0 CommunityInfoRespVO and CommunityFilterReqVO"
```

---

### Task 3.2: 扩展 `CommunityServiceImpl.java` — Community 查询 RETURN V3 字段

**文件:**
- 修改: `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/CommunityServiceImpl.java`

**前置阅读:**
- `listCommunities` 方法中的 Cypher RETURN 子句
- `searchCommunities` 方法中的 Cypher RETURN 子句
- `buildSingleCommunity` 方法（如存在）

**步骤:**

- [ ] **Step 1: 更新 `listCommunities` 的 RETURN 子句**

找到类似以下的 RETURN 行：
```java
"RETURN c.uuid as uuid, c.name as name, c.summary as summary, " +
"c.member_count as member_count, c.parent_community_uuid as parentCommunityUuid " +
"ORDER BY c.member_count DESC"
```

替换为：
```java
"RETURN c.uuid as uuid, c.name as name, c.summary as summary, " +
"c.member_count as member_count, c.parent_community_uuid as parentCommunityUuid, " +
"c.community_type as communityType, " +
"c.legal_domain as legalDomain, " +
"c.jurisdiction as jurisdiction, " +
"c.practice_type as practiceType, " +
"c.created_at as createdAt " +
"ORDER BY c.member_count DESC"
```

- [ ] **Step 2: 更新 `searchCommunities` 的 RETURN 子句**

同样追加 V3 字段到 RETURN 子句。

- [ ] **Step 3: 添加辅助方法 `resolveCommunityType`**

在类的辅助方法区域添加：

```java
/**
 * 根据社区名称自动推断 community_type（V3.0.0 向后兼容）
 */
private String resolveCommunityType(String communityName) {
    if (communityName == null) return "top_level";
    if (communityName.contains("公司解散") || communityName.contains("股权转让") || communityName.contains("买卖合同"))
        return "corporate_dispute";
    if (communityName.contains("劳动") || communityName.contains("工资") || communityName.contains("社保"))
        return "labor_dispute";
    if (communityName.contains("专利") || communityName.contains("商标") || communityName.contains("著作权"))
        return "intellectual_property";
    if (communityName.contains("调解") || communityName.contains("和解") || communityName.contains("仲裁"))
        return "dispute_resolution";
    return "top_level";
}

/**
 * 根据 community_type 映射 legal_domain（V3.0.0）
 */
private String resolveLegalDomain(String communityType) {
    Map<String, String> typeToDomain = Map.ofEntries(
        Map.entry("corporate_dispute", "DOMAIN_CIVIL"),
        Map.entry("dispute_resolution", "DOMAIN_MEDIATION"),
        Map.entry("procedural_law", "DOMAIN_CIVIL"),
        Map.entry("intellectual_property", "DOMAIN_IP"),
        Map.entry("labor_dispute", "DOMAIN_LABOR"),
        Map.entry("foundational_civil_law", "DOMAIN_CIVIL"),
        Map.entry("top_level", "DOMAIN_ROOT")
    );
    return typeToDomain.getOrDefault(communityType, "DOMAIN_ROOT");
}
```

- [ ] **Step 4: 在 `buildCommunities` 构建方法中补充 V3 字段**

找到社区节点创建时设置属性的部分，添加 V3 字段到 CREATE 语句（如果现有 CREATE 语句已包含这些字段则跳过）：

```java
// 在现有属性后追加
"community_type: $communityType, " +
"legal_domain: $legalDomain, " +
"jurisdiction: $jurisdiction, " +
"practice_type: $practiceType, " +
"key_provisions: $keyProvisions, " +
```

同时在设置参数 Map 时追加：
```java
params.put("communityType", resolveCommunityType(communityName));
params.put("legalDomain", resolveLegalDomain(resolveCommunityType(communityName)));
params.put("jurisdiction", "JURISDICTION_CN");  // 默认中国法律体系
params.put("practiceType", "PRACTICE_JUDICIAL"); // 默认司法实践
params.put("keyProvisions", new ArrayList<String>());
```

- [ ] **Step 5: Commit**

```bash
git add graphiti-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/CommunityServiceImpl.java
git commit -m "feat(community): add V3 fields to listCommunities and buildCommunities"
```

---

### Task 3.3: 扩展 `GraphVisualizationService.java` — Episode/Community 可视化 V3 字段

**文件:**
- 修改: `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/service/GraphVisualizationService.java`

**前置阅读:**
- `getEpisodesVisualization` 方法的 Cypher 查询
- `getCommunityVisualization` 方法的 Cypher 查询

**步骤:**

- [ ] **Step 1: 更新 `getEpisodesVisualization` 中的 Episode RETURN 子句**

找到 `getEpisodesVisualization` 中的 Cypher RETURN 行，追加：
```java
"e.episode_type as episodeType, " +
"e.legal_process as legalProcess, " +
"e.stage_label as stageLabel, " +
"e.court_level as courtLevel, " +
"e.is_trial_stage as isTrialStage, " +
"e.start_time as startTime, " +
"e.end_time as endTime, " +
"e.case_id as caseId, " +
```

- [ ] **Step 2: 更新 `getCommunityVisualization` 中的 Community RETURN 子句**

找到 `getCommunityVisualization` 中的 Cypher RETURN 行，追加：
```java
"c.community_type as communityType, " +
"c.legal_domain as legalDomain, " +
"c.jurisdiction as jurisdiction, " +
"c.practice_type as practiceType, " +
```

- [ ] **Step 3: Commit**

```bash
git add graphiti-module-core/src/main/java/com/graphiti/module/graphiti/service/GraphVisualizationService.java
git commit -m "feat(visualization): add V3 fields to Episode and Community visualization queries"
```

---

### Task 3.4: 扩展 `GraphNeo4jService.getTypeNameField` — 支持 V3 实体类型

**文件:**
- 修改: `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/service/GraphNeo4jService.java`

**前置阅读:**
- `getTypeNameField` 方法的 switch 语句

**步骤:**

- [ ] **Step 1: 在 `getTypeNameField` switch 中追加 V3 类型**

找到现有 switch 语句，在 `case "JudgmentDocument"`, `case "MediationAgreement"` 等行后追加：

```java
case "CommercialMediationOrganization" -> "orgName";  // V3: 新增
case "Mediator" -> "mediatorName";                  // V3: 新增
case "Evidence" -> "evidenceNumber";                 // V3: 新增
case "CaseFact" -> "factCategory";                  // V3: 新增
case "CaseReasoning" -> "reasoning";               // V3: 新增
// Case 子类型（V3 案件分类）
case "CivilCase", "CriminalCase", "CommercialCase",
     "AdministrativeCase", "ExecutionCase" -> "caseNumber";  // V3: 新增
```

- [ ] **Step 2: Commit**

```bash
git add graphiti-module-core/src/main/java/com/graphiti/module/graphiti/service/GraphNeo4jService.java
git commit -m "feat(neo4j): add V3 entity types to getTypeNameField"
```

---

### Task 3.5: 在 `GraphIDEController.java` 中新增 V3 API 端点

**文件:**
- 修改: `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/controller/admin/GraphIDEController.java`

**前置阅读:**
- 现有 `@GetMapping` 端点模式
- `CommonResult.success()` 包装模式

**步骤:**

- [ ] **Step 1: 在 `GraphIDEController` 中注入必要 Service**

找到现有 `@Autowired` 注入区域，追加（如尚不存在）：
```java
@Autowired
private CommunityService communityService;

@Autowired
private SchemaManagementService schemaManagementService;
```

- [ ] **Step 2: 在现有端点方法后添加 4 个 V3 端点**

在类中找一个合适位置（如现有 `/ontology/classes` 端点附近），添加：

```java
/**
 * V3.0.0: 获取社区层级结构（PARENT_OF 树）
 */
@GetMapping("/{graphId}/communities/hierarchy")
@Operation(summary = "获取社区层级结构（PARENT_OF 树）")
public CommonResult<List<CommunityInfoRespVO>> getCommunityHierarchy(
        @PathVariable @Parameter(description = "图谱ID") String graphId,
        @RequestParam(required = false) @Parameter(description = "维度: domain|jurisdiction|practice") String dimension) {
    List<CommunityInfoRespVO> result = communityService.getCommunityHierarchy(graphId, dimension);
    return CommonResult.success(result);
}

/**
 * V3.0.0: 按法律领域过滤社区
 */
@GetMapping("/{graphId}/communities/by-domain")
@Operation(summary = "按法律领域过滤社区")
public CommonResult<List<CommunityInfoRespVO>> getCommunitiesByDomain(
        @PathVariable String graphId,
        @RequestParam @Parameter(description = "法律领域代码") String domain) {
    List<CommunityInfoRespVO> result = communityService.listByDomain(graphId, domain);
    return CommonResult.success(result);
}

/**
 * V3.0.0: 获取 Episode 类型元数据
 */
@GetMapping("/{graphId}/episode-types")
@Operation(summary = "获取 Episode 类型元数据")
public CommonResult<List<Map<String, Object>>> getEpisodeTypes(
        @PathVariable String graphId) {
    List<Map<String, Object>> result = schemaManagementService.getEpisodeTypes(graphId);
    return CommonResult.success(result);
}

/**
 * V3.0.0: 获取关系类型元数据
 */
@GetMapping("/{graphId}/relationships/metadata")
@Operation(summary = "获取关系类型元数据")
public CommonResult<List<Map<String, Object>>> getRelationshipMetadata(
        @PathVariable String graphId) {
    List<Map<String, Object>> result = schemaManagementService.getRelationshipMetadata(graphId);
    return CommonResult.success(result);
}
```

- [ ] **Step 3: 在 `CommunityService` 接口中添加新方法声明**

**文件:**
- 修改: `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/service/CommunityService.java`

在接口中添加：
```java
List<CommunityInfoRespVO> getCommunityHierarchy(String graphId, String dimension);
List<CommunityInfoRespVO> listByDomain(String graphId, String domain);
```

- [ ] **Step 4: 在 `CommunityServiceImpl` 中实现新方法**

在 `CommunityServiceImpl` 中实现：
```java
@Override
public List<CommunityInfoRespVO> getCommunityHierarchy(String graphId, String dimension) {
    // 根据 dimension 过滤返回社区列表（已由 listCommunities 返回包含 V3 字段的数据）
    List<Map<String, Object>> raw = listCommunities(graphId);
    return raw.stream().map(this::convertToCommunityInfo).collect(Collectors.toList());
}

@Override
public List<CommunityInfoRespVO> listByDomain(String graphId, String domain) {
    List<Map<String, Object>> raw = listCommunities(graphId);
    return raw.stream()
        .filter(m -> domain.equals(m.get("legalDomain")))
        .map(this::convertToCommunityInfo)
        .collect(Collectors.toList());
}

private CommunityInfoRespVO convertToCommunityInfo(Map<String, Object> row) {
    CommunityInfoRespVO vo = new CommunityInfoRespVO();
    vo.setUuid((String) row.get("uuid"));
    vo.setName((String) row.get("name"));
    vo.setCommunityType((String) row.get("communityType"));
    vo.setLegalDomain((String) row.get("legalDomain"));
    vo.setJurisdiction((String) row.get("jurisdiction"));
    vo.setPracticeType((String) row.get("practiceType"));
    vo.setParentCommunityUuid((String) row.get("parentCommunityUuid"));
    vo.setSummary((String) row.get("summary"));
    Object mc = row.get("memberCount");
    vo.setMemberCount(mc != null ? ((Number) mc).intValue() : null);
    vo.setCreatedAt(parseTimestamp(row.get("createdAt")));
    return vo;
}

private LocalDateTime parseTimestamp(Object val) {
    if (val == null) return null;
    if (val instanceof java.util.Date) {
        return ((java.util.Date) val).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
    }
    if (val instanceof java.time.LocalDateTime) return (java.time.LocalDateTime) val;
    return null;
}
```

- [ ] **Step 5: 在 `SchemaManagementService` 中实现元数据查询方法**

**文件:**
- 修改: `graphiti-module-core/src/main/java/com/graphiti/module/graphiti/service/SchemaManagementService.java`

在类中添加两个方法：

```java
/**
 * V3.0.0: 获取 Episode 类型元数据列表
 */
public List<Map<String, Object>> getEpisodeTypes(String graphId) {
    String cypher = """
        MATCH (e:Episode {graph_id: $graphId})
        WHERE e.episode_type IS NOT NULL
        RETURN e.episode_type as typeCode, e.legal_process as legalProcess,
               e.stage_label as stageLabel, e.court_level as courtLevel,
               e.is_trial_stage as isTrialStage, count(*) as count
        ORDER BY count DESC
        """;
    Map<String, Object> params = Map.of("graphId", graphId);
    try (Transaction tx = neo4jDriverAdapter.getSession().beginTransaction()) {
        Result result = tx.run(cypher, params);
        List<Map<String, Object>> list = new ArrayList<>();
        while (result.hasNext()) {
            list.add(result.next().asMap());
        }
        return list;
    }
}

/**
 * V3.0.0: 获取关系类型元数据（从 ont_relationship_meta 表查询）
 */
public List<Map<String, Object>> getRelationshipMetadata(String graphId) {
    // 从 PostgreSQL ont_relationship_meta 表查询
    // 需要注入 JdbcTemplate 或使用现有 JDBC 连接
    // 此处先返回空列表，实际实现需注入 JdbcTemplate
    return jdbcTemplate.queryForList(
        "SELECT id, relationship_type, relationship_name, source_entity_types, " +
        "target_entity_types, is_directional, is_transitive, multiplicity, " +
        "default_weight, description FROM ont_relationship_meta " +
        "WHERE definition_id = 1 AND status = 'ACTIVE' ORDER BY sort_order"
    );
}
```

- [ ] **Step 6: Commit**

```bash
git add graphiti-module-core/src/main/java/com/graphiti/module/graphiti/controller/admin/GraphIDEController.java graphiti-module-core/src/main/java/com/graphiti/module/graphiti/service/CommunityService.java graphiti-module-core/src/main/java/com/graphiti/module/graphiti/service/impl/CommunityServiceImpl.java graphiti-module-core/src/main/java/com/graphiti/module/graphiti/service/SchemaManagementService.java
git commit -m "feat(api): add V3 REST endpoints for community hierarchy, episode types, and relationship metadata"
```

---

## Phase 4: 前端层 — TypeScript 类型 + Vue 组件适配

### Task 4.1: 创建 V3 TypeScript 类型定义文件

**文件:**
- 创建: `graphiti-web/src/types/legal-graph-v3.ts`

**步骤:**

- [ ] **Step 1: 创建 `graphiti-web/src/types/legal-graph-v3.ts`**

```typescript
/**
 * 法律知识图谱 V3.0.0 TypeScript 类型定义
 * 包含 Community、Episode、Entity、Relationship 的 V3 扩展类型
 */

// ========== Community V3 ==========

/** 社区节点 (V3.0.0) */
export interface CommunityV3 {
  uuid: string
  name: string
  communityType: string          // 兼容旧字段
  legalDomain: string           // DOMAIN_CIVIL, DOMAIN_CRIMINAL, etc.
  jurisdiction: string           // JURISDICTION_CN, etc.
  practiceType: string           // PRACTICE_JUDICIAL, PRACTICE_MEDIATION, etc.
  parentCommunityUuid?: string    // PARENT_OF 父社区
  summary?: string
  memberCount?: number
  keyProvisions?: string[]        // 关键法条 ID 列表
  description?: string
  metadata?: CommunityMetadata
  createdAt?: string
  updatedAt?: string
}

export interface CommunityMetadata {
  icon?: string
  color?: string
  displayPriority?: number
}

/** 社区类型元数据（来自 ont_community_type 表） */
export interface CommunityTypeMeta {
  id: number
  typeCode: string                // DOMAIN_CIVIL, PRACTICE_JUDICIAL, JURISDICTION_CN
  typeName: string
  typeNameEn?: string
  category: 'domain' | 'jurisdiction' | 'practice'
  parentTypeCode?: string
  sortOrder: number
  metadata: CommunityMetadata
  description?: string
}

/** 社区树节点（用于 a-tree 组件） */
export interface CommunityTreeNode {
  key: string
  title: string
  icon?: string
  color?: string
  legalDomain?: string
  jurisdiction?: string
  practiceType?: string
  isLeaf?: boolean
  children?: CommunityTreeNode[]
}

// ========== Episode V3 ==========

/** Episode 节点 (V3.0.0) */
export interface EpisodeV3 {
  uuid: string
  name: string
  episodeType: string            // EP_TRIAL_1ST, EP_MEDIATION_NEGOTIATION, etc.
  legalProcess: string           // litigation | mediation | arbitration | execution
  stageLabel: string             // 立案 | 庭审 | 调解 | 判决 | 执行
  courtLevel: string | null       // 一审 | 二审 | 再审 | null
  isTrialStage: boolean           // 是否审判阶段
  caseId?: string
  startTime?: string              // ISO-8601
  endTime?: string                // ISO-8601
  content?: string
  source?: string
  sourceDescription?: string
  createdAt?: string
  groupId?: string
}

/** Episode 类型元数据（来自 ont_episode_type 表） */
export interface EpisodeTypeMeta {
  id: number
  typeCode: string                // EP_TRIAL_1ST, etc.
  typeName: string
  legalProcess: string            // litigation | mediation | arbitration | execution
  stageLabel: string
  courtLevel: string | null
  isTrialStage: boolean
  sortOrder: number
  description?: string
}

/** 法律程序分组（用于前端 Accordion 分组展示） */
export type LegalProcessGroup = 'litigation' | 'mediation' | 'arbitration' | 'execution'

export const LEGAL_PROCESS_LABELS: Record<LegalProcessGroup, string> = {
  litigation: '诉讼程序',
  mediation: '调解程序',
  arbitration: '仲裁程序',
  execution: '执行程序',
}

// ========== Entity V3 ==========

/** 实体节点 (V3.0.0) */
export interface EntityV3 {
  uuid: string
  name: string
  type: string                   // Neo4j 标签名
  category: string               // CASE, PARTY_NATURAL, COURT_ORG, etc.
  categoryLevel?: number          // 1 (一级) or 2 (二级)
  properties?: Record<string, any>
  summary?: string
  createdAt?: string
  updatedAt?: string
}

/** 实体分类元数据（来自 ont_entity_category 表） */
export interface EntityCategoryMeta {
  id: number
  categoryCode: string           // CASE, PARTY_NATURAL, COURT_ORG, etc.
  categoryName: string
  categoryLevel: number
  parentCategoryCode?: string
  entityTypeScope: string[]      // 适用的 Neo4j 实体类型标签
  sortOrder: number
  description?: string
}

// ========== Relationship V3 ==========

/** 关系 (V3.0.0) */
export interface RelationshipV3 {
  uuid: string
  source: string
  target: string
  type: string                   // HAS_COMMUNITY, MENTIONS, CITES, PRECEDES, etc.
  name: string                   // 中文名
  isDirectional: boolean         // 有向 or 无向
  defaultWeight: number          // 0.0000 - 1.0000
  properties?: Record<string, any>
  fact?: string
  createdAt?: string
}

/** 关系元数据（来自 ont_relationship_meta 表） */
export interface RelationshipMeta {
  id: number
  relationshipType: string       // HAS_COMMUNITY, CITES, PRECEDES, etc.
  relationshipName: string        // 中文名
  sourceEntityTypes: string[]
  targetEntityTypes: string[]
  isDirectional: boolean
  isTransitive: boolean
  multiplicity: string            // one-to-one | one-to-many | many-to-many
  defaultWeight: number
  validityPeriod: { hasPeriod: boolean; defaultDays: number | null }
  description: string
  exampleCypher?: string
}

// ========== 颜色体系 (V3) ==========

/** 法律领域色彩映射 */
export const LEGAL_DOMAIN_COLORS: Record<string, string> = {
  DOMAIN_CIVIL: '#2E7D32',
  DOMAIN_CRIMINAL: '#C62828',
  DOMAIN_ADMIN: '#6A1B9A',
  DOMAIN_IP: '#F57F17',
  DOMAIN_LABOR: '#00838F',
  DOMAIN_MEDIATION: '#AD1457',
  DOMAIN_EXECUTION: '#37474F',
  DOMAIN_ROOT: '#1565C0',
}

/** 司法管辖区色彩映射 */
export const JURISDICTION_COLORS: Record<string, string> = {
  JURISDICTION_CN: '#C62828',
  JURISDICTION_INTERNATIONAL: '#0277BD',
}

/** 应用场景色彩映射 */
export const PRACTICE_COLORS: Record<string, string> = {
  PRACTICE_JUDICIAL: '#1565C0',
  PRACTICE_ARBITRATION: '#0288D1',
  PRACTICE_MEDIATION: '#AD1457',
  PRACTICE_COMPLIANCE: '#558B2F',
}

/** Episode 类型色彩映射 */
export const EPISODE_TYPE_COLORS: Record<string, string> = {
  EP_FILING: '#42A5F5',
  EP_SERVING: '#64B5F6',
  EP_TRIAL_1ST: '#2E7D32',
  EP_JUDGMENT_1ST: '#1B5E20',
  EP_APPEAL: '#FFA726',
  EP_TRIAL_2ND: '#388E3C',
  EP_JUDGMENT_2ND: '#1B5E20',
  EP_RETRIAL: '#D32F2F',
  EP_EXECUTION: '#37474F',
  EP_MEDIATION_ACCEPT: '#EC407A',
  EP_MEDIATION_NEGOTIATION: '#F48FB1',
  EP_MEDIATION_AGREEMENT: '#CE93D8',
  EP_MEDIATION_CONFIRM: '#AB47BC',
}

/** 关系类型色彩映射 */
export const RELATIONSHIP_COLORS: Record<string, string> = {
  HAS_COMMUNITY: '#1565C0',
  PARENT_OF: '#5E35B1',
  MENTIONS: '#0288D1',
  NEXT_EPISODE: '#00838F',
  CITES: '#2E7D32',
  INVOLVES: '#F57F17',
  BELONGS_TO: '#6A1B9A',
  PRECEDES: '#AD1457',
  REPRESENTS: '#00838F',
  PRESIDES_OVER: '#C62828',
  PARTY_OF: '#6D4C41',
  SUBSTANTIATES: '#546E7A',
  AFFIRMED_BY: '#AD1457',
}
```

- [ ] **Step 2: Commit**

```bash
git add graphiti-web/src/types/legal-graph-v3.ts
git commit -m "feat(types): add V3.0.0 TypeScript type definitions for legal graph"
```

---

### Task 4.2: 扩展前端 API 层 — 新增 V3 API 接口

**文件:**
- 修改: `graphiti-web/src/api/graph.ts`

**前置阅读:**
- 现有 `getCommunitiesVisualization`, `getEpisodesVisualization` 等方法

**步骤:**

- [ ] **Step 1: 在 `graphiti-web/src/api/graph.ts` 末尾追加 V3 API 函数**

在文件末尾找到最后一个 export 函数后添加：

```typescript
// ============================================================
// 法律知识图谱 V3.0.0 API
// ============================================================

import type {
  CommunityV3,
  EpisodeV3,
  CommunityTypeMeta,
  EpisodeTypeMeta,
  RelationshipMeta,
} from '@/types/legal-graph-v3'

/**
 * V3.0.0: 获取社区层级结构（PARENT_OF 树）
 */
export const getCommunityHierarchy = (graphId: string, dimension?: string) =>
  request.get<CommunityV3[]>(`/graph/${graphId}/communities/hierarchy`, {
    params: dimension ? { dimension } : undefined,
  })

/**
 * V3.0.0: 按法律领域过滤社区
 */
export const getCommunitiesByDomain = (graphId: string, domain: string) =>
  request.get<CommunityV3[]>(`/graph/${graphId}/communities/by-domain`, {
    params: { domain },
  })

/**
 * V3.0.0: 获取社区类型元数据（ont_community_type）
 */
export const getCommunityTypes = (graphId: string) =>
  request.get<CommunityTypeMeta[]>(`/graph/${graphId}/community-types`)

/**
 * V3.0.0: 获取 Episode 类型元数据（ont_episode_type）
 */
export const getEpisodeTypes = (graphId: string) =>
  request.get<EpisodeTypeMeta[]>(`/graph/${graphId}/episode-types`)

/**
 * V3.0.0: 获取关系类型元数据（ont_relationship_meta）
 */
export const getRelationshipMetadata = (graphId: string) =>
  request.get<RelationshipMeta[]>(`/graph/${graphId}/relationships/metadata`)

/**
 * V3.0.0: 获取实体分类元数据（ont_entity_category）
 */
export const getEntityCategories = (graphId: string) =>
  request.get<{ data: any[] }>(`/graph/${graphId}/entity-categories`)
```

- [ ] **Step 2: Commit**

```bash
git add graphiti-web/src/api/graph.ts
git commit -m "feat(api): add V3.0.0 API endpoints for community hierarchy and metadata"
```

---

### Task 4.3: 扩展 `ide.vue` — 社区树形视图 + Episode 按程序分组

**文件:**
- 修改: `graphiti-web/src/views/graph/ide.vue`

**前置阅读:**
- 现有 `handleEpisodesClick` 和 `handleCommunitiesClick` 方法
- 现有 `activeTreeItem` 和 `treeViewMode` 状态
- 现有右侧面板模板结构

**步骤:**

- [ ] **Step 1: 在 `<script setup lang="ts">` 中添加 V3 相关状态**

找到现有 ref 声明区域，添加：

```typescript
import {
  LEGAL_PROCESS_LABELS,
  LEGAL_DOMAIN_COLORS,
  EPISODE_TYPE_COLORS,
  type CommunityV3,
  type EpisodeV3,
} from '@/types/legal-graph-v3'

// === V3.0.0 新增状态 ===

/** 社区多维度过滤 */
type CommunityFilterDimension = 'domain' | 'jurisdiction' | 'practice'
const communityFilterDimension = ref<CommunityFilterDimension>('domain')

/** 社区树数据 */
const communityTreeData = ref<any[]>([])

/** 社区搜索文本 */
const communitySearchText = ref('')

/** Episode 按法律程序分组 */
const episodesByLegalProcess = computed(() => {
  if (!episodes.value || !Array.isArray(episodes.value)) return {}
  return (episodes.value as EpisodeV3[]).reduce((acc, ep) => {
    const key = ep.legalProcess || 'unknown'
    if (!acc[key]) acc[key] = []
    acc[key].push(ep)
    return acc
  }, {} as Record<string, EpisodeV3[]>)
})

/** 当前选中的 Episode */
const selectedEpisode = ref<EpisodeV3 | null>(null)
```

- [ ] **Step 2: 添加 V3 辅助函数**

找到现有辅助函数区域（`const formatDate`, `const handleNodesExpand` 等附近），添加：

```typescript
/** 根据法律领域获取社区颜色 */
const getCommunityColor = (domain?: string): string => {
  if (!domain) return '#999'
  return LEGAL_DOMAIN_COLORS[domain] || '#999'
}

/** 根据 Episode 类型获取颜色 */
const getEpisodeColor = (type?: string): string => {
  if (!type) return 'blue'
  return EPISODE_TYPE_COLORS[type] || 'blue'
}

/** 格式化 Episode 时间 */
const formatEpisodeTime = (timeStr?: string): string => {
  if (!timeStr) return ''
  try {
    const d = new Date(timeStr)
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  } catch {
    return timeStr
  }
}
```

- [ ] **Step 3: 更新 `handleCommunitiesClick` 方法**

找到现有 `handleCommunitiesClick` 方法，替换为：

```typescript
const handleCommunitiesClick = async () => {
  activeTreeItem.value = 'communities'
  treeViewMode.value = 'communities'
  try {
    // V3.0.0: 调用新的社区层级 API
    const data = await graphApi.getCommunityHierarchy(effectiveGraphId.value, communityFilterDimension.value)
    communityTreeData.value = buildCommunityTree(data)
    // 同时保留原有可视化数据用于图谱展示
    const vizData = await graphApi.getCommunitiesVisualization(effectiveGraphId.value, 100)
    nodes.value = vizData.nodes || []
    edges.value = vizData.edges || []
  } catch (err) {
    console.error('Failed to load communities:', err)
  }
}

/** 构建社区树形数据（用于 a-tree 组件） */
const buildCommunityTree = (communities: CommunityV3[]): any[] => {
  if (!communities || !communities.length) return []
  const map = new Map<string, any>()
  communities.forEach(c => {
    map.set(c.uuid, {
      key: c.uuid,
      title: c.name,
      icon: c.metadata?.icon,
      color: getCommunityColor(c.legalDomain),
      legalDomain: c.legalDomain,
      jurisdiction: c.jurisdiction,
      practiceType: c.practiceType,
      isLeaf: true,
      children: [],
    })
  })
  // 构建父子关系
  const roots: any[] = []
  communities.forEach(c => {
    const node = map.get(c.uuid)!
    if (c.parentCommunityUuid && map.has(c.parentCommunityUuid)) {
      const parent = map.get(c.parentCommunityUuid)!
      parent.isLeaf = false
      parent.children = parent.children || []
      parent.children.push(node)
    } else {
      roots.push(node)
    }
  })
  return roots
}
```

- [ ] **Step 4: 更新 `handleEpisodesClick` 方法**

找到现有 `handleEpisodesClick` 方法，替换为：

```typescript
const handleEpisodesClick = async () => {
  activeTreeItem.value = 'episodes'
  treeViewMode.value = 'episodes'
  selectedEpisode.value = null
  try {
    // V3.0.0: 调用可视化 API（已有 V3 字段）
    const data = await graphApi.getEpisodesVisualization(effectiveGraphId.value, 100)
    nodes.value = data.nodes || []
    edges.value = data.edges || []
  } catch (err) {
    console.error('Failed to load episodes:', err)
  }
}
```

- [ ] **Step 5: 在右侧面板中添加 Episode 详情视图**

找到右侧面板模板（`<div class="ide-panel">` 区域），在面板切换逻辑中添加 Episode 详情展示：

```vue
<!-- 右侧面板 - Episode 详情 (V3.0.0) -->
<div v-if="treeViewMode === 'episodes' && selectedEpisode" class="panel-content">
  <div class="panel-header">
    <h3>事件详情</h3>
    <a-button type="text" @click="selectedEpisode = null">关闭</a-button>
  </div>

  <a-descriptions :column="2" bordered size="small">
    <a-descriptions-item label="名称" :span="2">
      {{ selectedEpisode.name }}
    </a-descriptions-item>
    <!-- V3.0.0 新增字段 -->
    <a-descriptions-item label="类型">
      <a-tag :color="getEpisodeColor(selectedEpisode.episodeType)">
        {{ selectedEpisode.episodeType }}
      </a-tag>
    </a-descriptions-item>
    <a-descriptions-item label="法律程序">
      <a-tag>{{ selectedEpisode.legalProcess }}</a-tag>
    </a-descriptions-item>
    <a-descriptions-item v-if="selectedEpisode.courtLevel" label="审级">
      <a-tag color="purple">{{ selectedEpisode.courtLevel }}</a-tag>
    </a-descriptions-item>
    <a-descriptions-item label="阶段">
      <a-tag>{{ selectedEpisode.stageLabel }}</a-tag>
    </a-descriptions-item>
    <a-descriptions-item label="审判阶段">
      <a-tag :color="selectedEpisode.isTrialStage ? 'green' : 'default'">
        {{ selectedEpisode.isTrialStage ? '是' : '否' }}
      </a-tag>
    </a-descriptions-item>
    <a-descriptions-item label="开始时间" :span="2">
      {{ formatEpisodeTime(selectedEpisode.startTime) }}
    </a-descriptions-item>
    <a-descriptions-item label="结束时间" :span="2">
      {{ formatEpisodeTime(selectedEpisode.endTime) }}
    </a-descriptions-item>
    <a-descriptions-item label="内容" :span="2">
      <div class="episode-content">{{ selectedEpisode.content }}</div>
    </a-descriptions-item>
  </a-descriptions>
</div>
```

- [ ] **Step 6: 在 `<style>` 中添加 Episode 详情样式**

在样式区域添加：

```css
.episode-content {
  max-height: 200px;
  overflow-y: auto;
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  color: var(--text-secondary, #999);
}

.episode-time {
  font-size: 12px;
  color: var(--text-secondary, #999);
  margin-top: 2px;
}
```

- [ ] **Step 7: Commit**

```bash
git add graphiti-web/src/views/graph/ide.vue
git commit -m "feat(ide): add V3 Episode detail panel and community tree support"
```

---

### Task 4.4: 扩展前端 Vue 组件 — Episodes 列表页 V3 字段

**文件:**
- 修改: `graphiti-web/src/views/episodes/index.vue`

**前置阅读:**
- 现有 `a-table` columns 定义

**步骤:**

- [ ] **Step 1: 在 Episodes 列表 `a-table` 中追加 V3 列**

找到现有 table columns 定义区域，在 `content` 列后追加：

```vue
<!-- Episode 类型 (V3) -->
<a-table-column
  title="类型"
  dataIndex="episodeType"
  width="160"
  key="episodeType"
>
  <template #default="{ record }">
    <a-tag
      v-if="record.episodeType"
      :color="getEpisodeColor(record.episodeType)"
    >
      {{ record.episodeType }}
    </a-tag>
    <span v-else style="color: #999">-</span>
  </template>
</a-table-column>

<!-- 法律程序 (V3) -->
<a-table-column
  title="法律程序"
  dataIndex="legalProcess"
  width="100"
  key="legalProcess"
>
  <template #default="{ record }">
    <a-tag v-if="record.legalProcess" :color="getLegalProcessColor(record.legalProcess)">
      {{ LEGAL_PROCESS_LABELS[record.legalProcess] || record.legalProcess }}
    </a-tag>
  </template>
</a-table-column>

<!-- 审级 (V3) -->
<a-table-column
  title="审级"
  dataIndex="courtLevel"
  width="80"
  key="courtLevel"
>
  <template #default="{ record }">
    <a-tag v-if="record.courtLevel" color="purple">{{ record.courtLevel }}</a-tag>
    <span v-else>-</span>
  </template>
</a-table-column>

<!-- 审判阶段 (V3) -->
<a-table-column
  title="审判阶段"
  dataIndex="isTrialStage"
  width="90"
  key="isTrialStage"
>
  <template #default="{ record }">
    <a-tag :color="record.isTrialStage ? 'green' : 'default'" size="small">
      {{ record.isTrialStage ? '是' : '否' }}
    </a-tag>
  </template>
</a-table-column>

<!-- 时间范围 (V3) -->
<a-table-column
  title="时间范围"
  width="180"
  key="timeRange"
>
  <template #default="{ record }">
    <span v-if="record.startTime || record.endTime">
      {{ formatEpisodeTime(record.startTime) }}
      <template v-if="record.startTime && record.endTime"> ~ </template>
      {{ formatEpisodeTime(record.endTime) }}
    </span>
    <span v-else>-</span>
  </template>
</a-table-column>
```

- [ ] **Step 2: 在 `<script setup>` 中导入 V3 类型和常量**

找到 script 区域，添加导入：

```typescript
import { LEGAL_PROCESS_LABELS, EPISODE_TYPE_COLORS, type EpisodeV3 } from '@/types/legal-graph-v3'

const getEpisodeColor = (type?: string): string => {
  if (!type) return 'default'
  return EPISODE_TYPE_COLORS[type] || 'default'
}

const getLegalProcessColor = (process?: string): string => {
  const map: Record<string, string> = {
    litigation: 'blue',
    mediation: 'pink',
    arbitration: 'orange',
    execution: 'gray',
  }
  return map[process || ''] || 'default'
}
```

- [ ] **Step 3: Commit**

```bash
git add graphiti-web/src/views/episodes/index.vue
git commit -m "feat(episodes-page): add V3 columns (episodeType, legalProcess, courtLevel, isTrialStage) to episodes table"
```

---

### Task 4.5: 扩展前端 Vue 组件 — Edges 关系类型选择器 V3

**文件:**
- 修改: `graphiti-web/src/views/edges/index.vue`

**前置阅读:**
- 现有的关系创建/编辑 `a-modal` 中的 `a-select` 组件

**步骤:**

- [ ] **Step 1: 在 `<script setup>` 中加载关系元数据**

找到 `const formState = ref({})` 附近，添加：

```typescript
import { RELATIONSHIP_COLORS, type RelationshipMeta } from '@/types/legal-graph-v3'

/** 关系类型元数据 (V3) */
const relationshipMetaList = ref<RelationshipMeta[]>([])

/** 当前选中的关系元数据 */
const selectedRelationshipMeta = computed<RelationshipMeta | null>(() => {
  const type = formState.value.relationshipType
  if (!type) return null
  return relationshipMetaList.value.find(m => m.relationshipType === type) || null
})

/** 加载关系元数据 (V3) */
const loadRelationshipMetadata = async () => {
  try {
    const res = await graphApi.getRelationshipMetadata(effectiveGraphId.value)
    relationshipMetaList.value = res.data || res || []
  } catch (err) {
    console.error('Failed to load relationship metadata:', err)
  }
}
```

- [ ] **Step 2: 在组件挂载时加载元数据**

找到 `onMounted` 或初始化函数，添加：

```typescript
onMounted(async () => {
  await loadGraphs()
  if (effectiveGraphId.value) {
    loadRelationshipMetadata()  // V3: 加载关系元数据
  }
})
```

- [ ] **Step 3: 在关系创建/编辑表单中替换 `a-select` 为元数据驱动版本**

找到关系类型 `a-select` 组件，替换为：

```vue
<!-- 关系类型选择 (V3.0.0: 元数据驱动) -->
<a-form-item
  label="关系类型"
  name="relationshipType"
  :rules="[{ required: true, message: '请选择关系类型' }]"
>
  <a-select
    v-model:value="formState.relationshipType"
    placeholder="请选择关系类型"
    show-search
    :filter-option="(input, option) =>
      (option?.label ?? '').toLowerCase().includes(input.toLowerCase())"
    @change="onRelationshipTypeChange"
  >
    <a-select-option
      v-for="meta in relationshipMetaList"
      :key="meta.id"
      :value="meta.relationshipType"
      :label="`${meta.relationshipName} (${meta.relationshipType})`"
    >
      <div style="display: flex; align-items: center; gap: 8px">
        <span
          :style="{
            width: 8,
            height: 8,
            borderRadius: '50%',
            backgroundColor: RELATIONSHIP_COLORS[meta.relationshipType] || '#999',
            display: 'inline-block',
            flexShrink: 0,
          }"
        />
        <span>{{ meta.relationshipName }}</span>
        <span style="color: #999; font-size: 12px; margin-left: 4px">
          {{ meta.relationshipType }}
        </span>
      </div>
    </a-select-option>
  </a-select>
</a-form-item>

<!-- 关系元数据说明 (V3.0.0) -->
<a-form-item v-if="selectedRelationshipMeta" :style="{ marginBottom: 0 }">
  <a-alert
    :message="selectedRelationshipMeta.description"
    type="info"
    show-icon
    :style="{ marginTop: -8 }"
  >
    <template #action>
      <a-tooltip v-if="selectedRelationshipMeta.exampleCypher">
        <template #title>
          <code style="font-size: 11px">{{ selectedRelationshipMeta.exampleCypher }}</code>
        </template>
        <a-button size="small">Cypher 示例</a-button>
      </a-tooltip>
    </template>
  </a-alert>
</a-form-item>

<!-- 关系权重 (V3.0.0) -->
<a-form-item
  label="关系权重"
  name="weight"
  extra="权重越高表示该关系在图谱推理中越重要"
>
  <a-input-number
    v-model:value="formState.weight"
    :min="0"
    :max="1"
    :step="0.0001"
    :precision="4"
    style="width: 200px"
  />
  <span style="margin-left: 8px; color: #999; font-size: 12px">
    默认: {{ selectedRelationshipMeta?.defaultWeight?.toFixed(4) || '1.0000' }}
  </span>
</a-form-item>
```

- [ ] **Step 4: 添加 `onRelationshipTypeChange` 处理函数**

```typescript
/** 关系类型变更时自动填充默认值 (V3) */
const onRelationshipTypeChange = (type: string) => {
  const meta = selectedRelationshipMeta.value
  if (meta && !formState.value.weight) {
    formState.value.weight = meta.defaultWeight
  }
}
```

- [ ] **Step 5: Commit**

```bash
git add graphiti-web/src/views/edges/index.vue
git commit -m "feat(edges): add V3 relationship metadata selector with color indicators and weight control"
```

---

## Phase 5: 集成测试与验证

### Task 5.1: 验证后端编译

- [ ] **Step 1: 编译后端模块**

```bash
cd d:\projects\ontograph-java
mvn compile -pl graphiti-module-core -am -q
```

期望：无编译错误（新增字段、VO 类、新方法均正确编译）

- [ ] **Step 2: 验证前端 TypeScript 类型**

```bash
cd d:\projects\ontograph-java\graphiti-web
npx vue-tsc --noEmit --skipLibCheck
```

期望：无 TypeScript 类型错误（新增 V3 类型文件无错误）

- [ ] **Step 3: Commit Phase 1-4 所有变更**

```bash
git add -A
git commit -m "feat: implement V3.0.0 legal graph design - phases 1-4 complete"
```

---

## 自查清单

完成所有 Task 后，运行以下检查：

**1. 设计文档覆盖率检查**

对照 `docs/superpowers/specs/2026-05-20-legal-graph-v3-design.md` 第 11-14 章：

| 设计章节 | 是否有对应实现 Task |
|---------|------------------|
| 11.1 变更概览（前端） | Task 4.1-4.5 ✓ |
| 11.2 TypeScript 类型 | Task 4.1 ✓ |
| 11.3 API 接口 | Task 4.2 ✓ |
| 11.4 ide.vue 变更 | Task 4.3 ✓ |
| 11.5-11.7 组件变更 | Task 4.3-4.5 ✓ |
| 12.1 变更概览（后端） | Task 2.1-3.5 ✓ |
| 12.2 CommunityServiceImpl | Task 3.2 ✓ |
| 12.3 EpisodeServiceImpl | Task 2.1 ✓ |
| 12.4 GraphNeo4jService | Task 2.2, 3.4 ✓ |
| 12.5-12.7 VO/Controller | Task 3.1, 3.3, 3.5 ✓ |
| Phase 1 SQL | Task 1.1 ✓ |

**2. 类型一致性检查**

- `EpisodeInfoRespVO.java` 中的字段名（camelCase）是否与前端 `EpisodeV3.ts` 中的字段名一致？
  - `episodeType` vs `episodeType` ✓
  - `legalProcess` vs `legalProcess` ✓
  - `courtLevel` vs `courtLevel` ✓
  - `isTrialStage` vs `isTrialStage` ✓
  - `startTime` vs `startTime` ✓
  - `endTime` vs `endTime` ✓

- `CommunityInfoRespVO.java` 中的字段名是否与前端 `CommunityV3.ts` 一致？
  - `legalDomain` vs `legalDomain` ✓
  - `jurisdiction` vs `jurisdiction` ✓
  - `practiceType` vs `practiceType` ✓
  - `parentCommunityUuid` vs `parentCommunityUuid` ✓
  - `memberCount` vs `memberCount` ✓

**3. 占位符检查**

搜索以下关键词，确保没有遗留：
- `TBD` / `TODO` / `FIXME` — 不应出现在实现代码中
- `// 后续实现` / `// 待填充` — 不应出现

---

## 执行选项

**Plan complete and saved to `docs/superpowers/plans/2026-05-20-legal-graph-v3-implementation.md`.**

**Two execution options:**

**1. Subagent-Driven (recommended)** — I dispatch a fresh subagent per task, review between tasks, fast iteration. This plan is ideal for subagent-driven because:
- Phase 1 (SQL) and Phase 2-3 (Java backend) are independent and can run in parallel with Phase 4 (frontend)
- Each Task is a self-contained commit unit
- Tasks have clear file boundaries and Cypher code to verify

**2. Inline Execution** — Execute tasks in this session using executing-plans, batch execution with checkpoints. Best for quick prototyping or when you want direct oversight of each change.

**Which approach?**

---

*本文档由 Cursor AI 根据法律知识图谱 V3.0.0 设计文档生成*
*关联设计文档: `docs/superpowers/specs/2026-05-20-legal-graph-v3-design.md`*
