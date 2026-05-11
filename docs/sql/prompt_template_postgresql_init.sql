-- ============================================================
-- 提示词模板相关表结构 (PostgreSQL 版本)
-- ============================================================

-- 提示词模板表
CREATE TABLE IF NOT EXISTS prompt_template (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(500),
    type VARCHAR(50) NOT NULL,
    system_prompt TEXT NOT NULL,
    user_prompt_template TEXT NOT NULL,
    response_format TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    model VARCHAR(100),
    sort INT NOT NULL DEFAULT 0,
    tags VARCHAR(500),
    extra_config TEXT,
    created_by BIGINT,
    updated_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE prompt_template IS '提示词模板表';
COMMENT ON COLUMN prompt_template.id IS '主键ID';
COMMENT ON COLUMN prompt_template.code IS '模板编码（唯一标识）';
COMMENT ON COLUMN prompt_template.name IS '模板名称';
COMMENT ON COLUMN prompt_template.description IS '模板描述';
COMMENT ON COLUMN prompt_template.type IS '模板类型：entity_extract-实体抽取, edge_extract-关系抽取, dedupe-去重, summary-摘要';
COMMENT ON COLUMN prompt_template.system_prompt IS '系统提示词';
COMMENT ON COLUMN prompt_template.user_prompt_template IS '用户提示词模板（支持 {variable} 占位符）';
COMMENT ON COLUMN prompt_template.response_format IS '响应格式定义（JSON Schema）';
COMMENT ON COLUMN prompt_template.enabled IS '是否启用：true-启用, false-禁用';
COMMENT ON COLUMN prompt_template.model IS '所属模型';
COMMENT ON COLUMN prompt_template.sort IS '排序值';
COMMENT ON COLUMN prompt_template.tags IS '标签（JSON数组格式）';
COMMENT ON COLUMN prompt_template.extra_config IS '额外配置（JSON格式）';
COMMENT ON COLUMN prompt_template.created_by IS '创建人ID';
COMMENT ON COLUMN prompt_template.updated_by IS '更新人ID';
COMMENT ON COLUMN prompt_template.created_at IS '创建时间';
COMMENT ON COLUMN prompt_template.updated_at IS '更新时间';

CREATE UNIQUE INDEX IF NOT EXISTS uk_prompt_template_code ON prompt_template(code);
CREATE INDEX IF NOT EXISTS idx_prompt_template_type ON prompt_template(type);
CREATE INDEX IF NOT EXISTS idx_prompt_template_enabled ON prompt_template(enabled);

-- 提示词变量表
CREATE TABLE IF NOT EXISTS prompt_variable (
    id BIGSERIAL PRIMARY KEY,
    template_id BIGINT NOT NULL,
    variable_name VARCHAR(100) NOT NULL,
    description VARCHAR(200),
    variable_type VARCHAR(20) NOT NULL DEFAULT 'string',
    required BOOLEAN NOT NULL DEFAULT TRUE,
    default_value VARCHAR(500),
    source VARCHAR(20) NOT NULL DEFAULT 'context',
    validation_rule VARCHAR(200),
    sort INT NOT NULL DEFAULT 0,
    remark VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE prompt_variable IS '提示词变量表';
COMMENT ON COLUMN prompt_variable.template_id IS '所属模板ID';
COMMENT ON COLUMN prompt_variable.variable_name IS '变量名称（对应 {variable} 占位符）';
COMMENT ON COLUMN prompt_variable.description IS '变量描述';
COMMENT ON COLUMN prompt_variable.variable_type IS '变量类型：string-字符串, list-列表, json-JSON对象, text-长文本';
COMMENT ON COLUMN prompt_variable.required IS '是否必需：true-必需, false-可选';
COMMENT ON COLUMN prompt_variable.default_value IS '默认值';
COMMENT ON COLUMN prompt_variable.source IS '变量来源：context-上下文, static-静态值, llm-动态生成';
COMMENT ON COLUMN prompt_variable.validation_rule IS '验证规则';

CREATE INDEX IF NOT EXISTS idx_prompt_variable_template_id ON prompt_variable(template_id);

-- 提示词版本表
CREATE TABLE IF NOT EXISTS prompt_version (
    id BIGSERIAL PRIMARY KEY,
    template_id BIGINT NOT NULL,
    version INT NOT NULL,
    system_prompt TEXT NOT NULL,
    user_prompt_template TEXT NOT NULL,
    response_format TEXT,
    description VARCHAR(500),
    active BOOLEAN NOT NULL DEFAULT FALSE,
    created_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE prompt_version IS '提示词版本表';
COMMENT ON COLUMN prompt_version.template_id IS '所属模板ID';
COMMENT ON COLUMN prompt_version.version IS '版本号';
COMMENT ON COLUMN prompt_version.system_prompt IS '系统提示词';
COMMENT ON COLUMN prompt_version.user_prompt_template IS '用户提示词模板';
COMMENT ON COLUMN prompt_version.response_format IS '响应格式';
COMMENT ON COLUMN prompt_version.description IS '版本描述';
COMMENT ON COLUMN prompt_version.active IS '是否为当前活跃版本：true-活跃, false-非活跃';

CREATE INDEX IF NOT EXISTS idx_prompt_version_template_id ON prompt_version(template_id);
CREATE INDEX IF NOT EXISTS idx_prompt_version_template_version ON prompt_version(template_id, version);

-- ============================================================
-- 创建更新时间触发器函数
-- ============================================================
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- 为表添加更新时间触发器
DROP TRIGGER IF EXISTS update_prompt_template_updated_at ON prompt_template;
CREATE TRIGGER update_prompt_template_updated_at
    BEFORE UPDATE ON prompt_template
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

DROP TRIGGER IF EXISTS update_prompt_variable_updated_at ON prompt_variable;
CREATE TRIGGER update_prompt_variable_updated_at
    BEFORE UPDATE ON prompt_variable
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- 初始化默认提示词模板
-- ============================================================

-- 通用实体提取模板（文本）
INSERT INTO prompt_template (code, name, description, type, system_prompt, user_prompt_template, response_format, enabled, model, sort, tags)
VALUES (
    'EXTRACT_TEXT_NODES',
    '文本实体提取',
    '从文本内容中提取实体',
    'entity_extract',
    '你是一个实体提取专家。请从给定的文本中提取所有实体（人物、组织、地点、概念、事件等）。

提取规则：
1. 只提取文本中明确提及的实体
2. 不要提取抽象概念、日期时间或泛化的词汇
3. 实体名称要具体明确
4. 同一实体在一个消息中只提取一次
5. 排除代词（他、她、它等）和通用词

{extras}',
    '请从以下文本中提取实体：

{episode_content}',
    '{"type": "object", "properties": {"entities": {"type": "array", "items": {"type": "object", "properties": {"name": {"type": "string"}, "entity_type": {"type": "string"}, "summary": {"type": "string"}, "episode_indices": {"type": "array", "items": {"type": "integer"}}}, "required": ["name"]}}}}, "required": ["entities"]}',
    TRUE,
    'gpt-4o-mini',
    10,
    '["通用", "文本", "实体提取"]'
);

-- 通用实体提取模板（JSON）
INSERT INTO prompt_template (code, name, description, type, system_prompt, user_prompt_template, response_format, enabled, model, sort, tags)
VALUES (
    'EXTRACT_JSON_NODES',
    'JSON实体提取',
    '从JSON数据中提取实体',
    'entity_extract',
    '你是一个实体提取专家。请从给定的JSON数据中提取所有实体。

提取规则：
1. 只提取JSON中明确存在的实体
2. 实体名称使用JSON字段的值
3. 实体类型根据数据内容判断
4. 不要提取ID、日期等标识性字段

{extras}',
    '数据源描述：{source_description}

JSON数据：
{episode_content}',
    '{"type": "object", "properties": {"entities": {"type": "array", "items": {"type": "object", "properties": {"name": {"type": "string"}, "entity_type": {"type": "string"}, "summary": {"type": "string"}, "episode_indices": {"type": "array", "items": {"type": "integer"}}}, "required": ["name"]}}}}, "required": ["entities"]}',
    TRUE,
    'gpt-4o-mini',
    11,
    '["通用", "JSON", "实体提取"]'
);

-- 通用关系提取模板
INSERT INTO prompt_template (code, name, description, type, system_prompt, user_prompt_template, response_format, enabled, model, sort, tags)
VALUES (
    'EXTRACT_EDGES',
    '关系提取',
    '从文本和实体列表中提取关系',
    'edge_extract',
    '你是一个关系提取专家。给定一段文本和已识别的实体列表，请提取实体之间的关系。

提取规则：
1. source_entity_name 和 target_entity_name 必须使用 ENTITIES 列表中的实体名称
2. 每个关系必须涉及两个不同的实体
3. 关系类型使用 SCREAMING_SNAKE_CASE 格式
4. fact 应该是描述关系的自然语言陈述
5. 只提取文本中明确表达的关系
6. 不要虚构或推断关系

{extras}',
    '参考时间：{reference_time}

实体列表：
{nodes}

文本内容：
{episode_content}',
    '{"type": "object", "properties": {"edges": {"type": "array", "items": {"type": "object", "properties": {"source_entity_name": {"type": "string"}, "target_entity_name": {"type": "string"}, "relation_type": {"type": "string"}, "fact": {"type": "string"}, "valid_at": {"type": "string"}, "invalid_at": {"type": "string"}, "episode_indices": {"type": "array", "items": {"type": "integer"}}}, "required": ["source_entity_name", "target_entity_name", "relation_type", "fact"]}}}}, "required": ["edges"]}',
    TRUE,
    'gpt-4o-mini',
    20,
    '["通用", "关系提取"]'
);

-- 法律案件实体提取模板
INSERT INTO prompt_template (code, name, description, type, system_prompt, user_prompt_template, response_format, enabled, model, sort, tags)
VALUES (
    'LEGAL_CASE_EXTRACT',
    '法律案件实体提取',
    '从法律文书中提取案件相关实体',
    'entity_extract',
    '你是一个专业的法律知识图谱构建助手。请从给定的法律文书中提取结构化的法律实体信息。

实体类型：
1. Case（案件）：案件名称、案号、案件类型、案件状态、立案日期、结案日期、争议金额等
2. Party（当事人）：当事人名称，当事人类型（自然人/法人）、角色（原告/被告/第三人）、住所地等
3. Court（法院）：法院名称、法院级别（最高/高级/中级/基层）、所在地
4. Judge（法官）：法官姓名、职务、所属法院
5. LegalProvision（法律条文）：条文编号、法律名称、条款序号、内容
6. Lawyer（律师）：律师姓名、执业证号、所属律所
7. Evidence（证据）：证据编号、证据类型、内容摘要、提交方
8. JudgmentDocument（裁判文书）：文书编号、文书类型、判决结果

提取规则：
- 严格按照JSON字段映射关系提取
- 日期格式统一使用 YYYY-MM-DD
- 金额统一为数字类型（单位：元）
- 只提取在JSON中能找到对应值的字段

{extras}',
    'JSON数据：
{episode_content}',
    '{"type": "object", "properties": {"entities": {"type": "array", "items": {"type": "object", "properties": {"name": {"type": "string"}, "entity_type": {"type": "string"}, "attributes": {"type": "object"}, "episode_indices": {"type": "array", "items": {"type": "integer"}}}, "required": ["name", "entity_type"]}}}}, "required": ["entities"]}',
    TRUE,
    'gpt-4o-mini',
    100,
    '["法律", "案件", "实体提取"]'
);

-- 法律关系提取模板
INSERT INTO prompt_template (code, name, description, type, system_prompt, user_prompt_template, response_format, enabled, model, sort, tags)
VALUES (
    'LEGAL_RELATION_EXTRACT',
    '法律关系提取',
    '从法律文书和实体中提取法律关系',
    'edge_extract',
    '你是一个法律关系提取专家。给定法律文书内容和已识别的实体列表，请提取实体之间的法律关系。

常见法律关系类型：
- CASE_PARTY：案件-当事人关系（含角色属性：原告/被告/第三人）
- CASE_COURT：案件-法院关系（含法院角色：立案法院/审理法院/执行法院）
- CASE_JUDGE：案件-法官关系（含职务：审判长/审判员/陪审员）
- PARTY_LAWYER：当事人-律师关系（代理关系）
- CASE_PROVISION：案件-法律条文关系（法律依据）
- EVIDENCE_PARTY：证据-提交方关系
- COURT_PARENT：法院-上级法院关系（上下级）

提取规则：
1. source_entity_name 和 target_entity_name 必须使用 ENTITIES 列表中的实体名称
2. 每个关系必须涉及两个不同的实体
3. fact 应该是描述关系的自然语言陈述
4. 如果有角色或职务信息，放在 fact 中描述

{extras}',
    '参考时间：{reference_time}

实体列表：
{nodes}

法律文书内容：
{episode_content}',
    '{"type": "object", "properties": {"edges": {"type": "array", "items": {"type": "object", "properties": {"source_entity_name": {"type": "string"}, "target_entity_name": {"type": "string"}, "relation_type": {"type": "string"}, "fact": {"type": "string"}, "valid_at": {"type": "string"}, "invalid_at": {"type": "string"}, "episode_indices": {"type": "array", "items": {"type": "integer"}}}, "required": ["source_entity_name", "target_entity_name", "relation_type", "fact"]}}}}, "required": ["edges"]}',
    TRUE,
    'gpt-4o-mini',
    101,
    '["法律", "关系提取"]'
);

-- ============================================================
-- 初始化提示词变量
-- ============================================================

-- 为通用实体提取模板（文本）添加变量
INSERT INTO prompt_variable (template_id, variable_name, description, variable_type, required, default_value, source, sort)
VALUES
    (1, 'episode_content', '待提取的文本内容', 'text', TRUE, NULL, 'context', 1),
    (1, 'extras', '额外的提取指令', 'text', FALSE, '', 'static', 2),
    (1, 'entity_types', '实体类型定义', 'text', TRUE, '1. Person - 人物\n2. Organization - 组织\n3. Location - 地点\n4. Event - 事件\n5. Entity - 通用实体', 'static', 3);

-- 为通用实体提取模板（JSON）添加变量
INSERT INTO prompt_variable (template_id, variable_name, description, variable_type, required, default_value, source, sort)
VALUES
    (2, 'episode_content', '待提取的JSON内容', 'json', TRUE, NULL, 'context', 1),
    (2, 'source_description', '数据源描述', 'string', FALSE, '', 'context', 2),
    (2, 'extras', '额外的提取指令', 'text', FALSE, '', 'static', 3);

-- 为通用关系提取模板添加变量
INSERT INTO prompt_variable (template_id, variable_name, description, variable_type, required, default_value, source, sort)
VALUES
    (3, 'episode_content', '待处理的文本内容', 'text', TRUE, NULL, 'context', 1),
    (3, 'nodes', '实体列表（JSON）', 'json', TRUE, NULL, 'context', 2),
    (3, 'reference_time', '参考时间（ISO格式）', 'string', FALSE, NULL, 'context', 3),
    (3, 'extras', '额外的关系类型定义', 'text', FALSE, '', 'static', 4);

-- 为法律案件实体提取模板添加变量
INSERT INTO prompt_variable (template_id, variable_name, description, variable_type, required, default_value, source, sort)
VALUES
    (4, 'episode_content', '法律文书JSON数据', 'json', TRUE, NULL, 'context', 1),
    (4, 'extras', '额外的提取指令', 'text', FALSE, '', 'static', 2);

-- 为法律关系提取模板添加变量
INSERT INTO prompt_variable (template_id, variable_name, description, variable_type, required, default_value, source, sort)
VALUES
    (5, 'episode_content', '法律文书内容', 'text', TRUE, NULL, 'context', 1),
    (5, 'nodes', '法律实体列表（JSON）', 'json', TRUE, NULL, 'context', 2),
    (5, 'reference_time', '参考时间', 'string', FALSE, NULL, 'context', 3),
    (5, 'extras', '额外的关系类型', 'text', FALSE, '', 'static', 4);

-- ============================================================
-- 初始化版本记录
-- ============================================================

-- 为每个模板创建初始版本
INSERT INTO prompt_version (template_id, version, system_prompt, user_prompt_template, response_format, description, active, created_by)
SELECT
    id,
    1,
    system_prompt,
    user_prompt_template,
    response_format,
    '初始版本',
    TRUE,
    1
FROM prompt_template
WHERE code = 'EXTRACT_TEXT_NODES';

INSERT INTO prompt_version (template_id, version, system_prompt, user_prompt_template, response_format, description, active, created_by)
SELECT
    id,
    1,
    system_prompt,
    user_prompt_template,
    response_format,
    '初始版本',
    TRUE,
    1
FROM prompt_template
WHERE code = 'EXTRACT_JSON_NODES';

INSERT INTO prompt_version (template_id, version, system_prompt, user_prompt_template, response_format, description, active, created_by)
SELECT
    id,
    1,
    system_prompt,
    user_prompt_template,
    response_format,
    '初始版本',
    TRUE,
    1
FROM prompt_template
WHERE code = 'EXTRACT_EDGES';

INSERT INTO prompt_version (template_id, version, system_prompt, user_prompt_template, response_format, description, active, created_by)
SELECT
    id,
    1,
    system_prompt,
    user_prompt_template,
    response_format,
    '初始版本',
    TRUE,
    1
FROM prompt_template
WHERE code = 'LEGAL_CASE_EXTRACT';

INSERT INTO prompt_version (template_id, version, system_prompt, user_prompt_template, response_format, description, active, created_by)
SELECT
    id,
    1,
    system_prompt,
    user_prompt_template,
    response_format,
    '初始版本',
    TRUE,
    1
FROM prompt_template
WHERE code = 'LEGAL_RELATION_EXTRACT';

-- ============================================================
-- 初始化去重提示词模板
-- ============================================================

-- 节点去重模板（单实体 vs 现有实体）
INSERT INTO prompt_template (code, name, description, type, system_prompt, user_prompt_template, response_format, enabled, model, sort, tags)
VALUES (
    'DEDUPE_NODES_SINGLE',
    '单实体去重',
    '将新提取的单个实体与现有实体进行去重',
    'dedupe',
    'You are an entity deduplication assistant. NEVER fabricate entity names or mark distinct entities as duplicates.',
    '<PREVIOUS MESSAGES>
{previous_episodes}
</PREVIOUS MESSAGES>

<CURRENT MESSAGE>
{episode_content}
</CURRENT MESSAGE>

<NEW ENTITY>
{extracted_node}
</NEW ENTITY>

<ENTITY TYPE DESCRIPTION>
{entity_type_description}
</ENTITY TYPE DESCRIPTION>

<EXISTING ENTITIES>
{existing_nodes}
</EXISTING ENTITIES>

Entities should only be considered duplicates if they refer to the *same real-world object or concept*.
Semantic Equivalence: if a descriptive label in EXISTING ENTITIES clearly refers to a named entity in context, treat them as duplicates.

NEVER mark entities as duplicates if:
- They are related but distinct.
- They have similar names or purposes but refer to separate instances or concepts.

Task:
1. Compare the NEW ENTITY against each EXISTING ENTITY (identified by `candidate_id`).
2. If it refers to the same real-world object or concept, return the `candidate_id` of that match.
3. Return `duplicate_candidate_id = -1` when there is no match or you are unsure.',
    '{"type": "object", "properties": {"id": {"type": "integer", "description": "integer id of the entity"}, "name": {"type": "string", "description": "Name of the entity"}, "duplicate_candidate_id": {"type": "integer", "description": "candidate_id of the matching EXISTING ENTITY, or -1 if no duplicate exists"}}, "required": ["id", "name", "duplicate_candidate_id"]}',
    TRUE,
    'gpt-4o-mini',
    30,
    '["去重", "实体", "单实体"]'
);

-- 节点去重模板（批量实体 vs 现有实体）
INSERT INTO prompt_template (code, name, description, type, system_prompt, user_prompt_template, response_format, enabled, model, sort, tags)
VALUES (
    'DEDUPE_NODES_BATCH',
    '批量实体去重',
    '将新提取的多个实体与现有实体进行去重',
    'dedupe',
    'You are an entity deduplication assistant. NEVER fabricate entity names or mark distinct entities as duplicates.',
    '<PREVIOUS MESSAGES>
{previous_episodes}
</PREVIOUS MESSAGES>

<CURRENT MESSAGE>
{episode_content}
</CURRENT MESSAGE>

<ENTITIES>
{extracted_nodes}
</ENTITIES>

<EXISTING ENTITIES>
{existing_nodes}
</EXISTING ENTITIES>

Each of the above ENTITIES was extracted from the CURRENT MESSAGE.
For each entity, determine if it is a duplicate of any EXISTING ENTITY.
Entities should only be considered duplicates if they refer to the *same real-world object or concept*.

NEVER mark entities as duplicates if:
- They are related but distinct.
- They have similar names or purposes but refer to separate instances or concepts.

Task:
ENTITIES contains {entity_count} entities with IDs 0 through {entity_count_minus_1}.
Your response MUST include EXACTLY {entity_count} resolutions with IDs 0 through {entity_count_minus_1}. Do not skip or add IDs.

For every entity, provide:
- `id`: integer id from ENTITIES
- `name`: the best full name for the entity (preserve the original name unless a duplicate has a more complete name)
- `duplicate_candidate_id`: the `candidate_id` of the EXISTING ENTITY that is the best duplicate match, or -1 if there is no duplicate',
    '{"type": "object", "properties": {"entity_resolutions": {"type": "array", "items": {"type": "object", "properties": {"id": {"type": "integer"}, "name": {"type": "string"}, "duplicate_candidate_id": {"type": "integer"}}}, "required": ["id", "name", "duplicate_candidate_id"]}}, "required": ["entity_resolutions"]}',
    TRUE,
    'gpt-4o-mini',
    31,
    '["去重", "实体", "批量"]'
);

-- 节点去重模板（节点列表分组）
INSERT INTO prompt_template (code, name, description, type, system_prompt, user_prompt_template, response_format, enabled, model, sort, tags)
VALUES (
    'DEDUPE_NODES_GROUP',
    '节点列表去重分组',
    '对节点列表进行去重分组',
    'dedupe',
    'You are an entity deduplication assistant that groups duplicate nodes by UUID.',
    'Given the following context, deduplicate a list of nodes:

<NODES>
{nodes}
</NODES>

Task:
1. Group nodes together such that all duplicate nodes are in the same list of uuids.
2. All duplicate uuids should be grouped together in the same list.
3. Also return a new summary that synthesizes the summaries into a new short summary.

Guidelines:
1. Each uuid from the list of nodes should appear EXACTLY once in your response.
2. If a node has no duplicates, it should appear in the response in a list of only one uuid.',
    '{"type": "array", "items": {"type": "object", "properties": {"uuids": {"type": "array", "items": {"type": "string"}}, "summary": {"type": "string"}}, "required": ["uuids", "summary"]}}',
    TRUE,
    'gpt-4o-mini',
    32,
    '["去重", "实体", "分组"]'
);

-- 边去重模板
INSERT INTO prompt_template (code, name, description, type, system_prompt, user_prompt_template, response_format, enabled, model, sort, tags)
VALUES (
    'DEDUPE_EDGES',
    '边去重',
    '检测新边与现有边是否重复或矛盾',
    'dedupe',
    'You are a fact deduplication assistant. NEVER mark facts with key differences as duplicates.',
    'NEVER mark facts as duplicates if they have key differences, particularly around numeric values, dates, or key qualifiers.

IMPORTANT constraints:
- duplicate_facts: ONLY idx values from EXISTING FACTS (NEVER include FACT INVALIDATION CANDIDATES)
- contradicted_facts: idx values from EITHER list (EXISTING FACTS or FACT INVALIDATION CANDIDATES)
- The idx values are continuous across both lists (INVALIDATION CANDIDATES start where EXISTING FACTS end)

<EXISTING FACTS>
{existing_edges}
</EXISTING FACTS>

<FACT INVALIDATION CANDIDATES>
{edge_invalidation_candidates}
</FACT INVALIDATION CANDIDATES>

<NEW FACT>
{new_edge}
</NEW FACT>

You will receive TWO lists of facts with CONTINUOUS idx numbering across both lists.
EXISTING FACTS are indexed first, followed by FACT INVALIDATION CANDIDATES.

1. DUPLICATE DETECTION:
   - If the NEW FACT represents identical factual information as any fact in EXISTING FACTS, return those idx values in duplicate_facts.
   - If no duplicates, return an empty list for duplicate_facts.

2. CONTRADICTION DETECTION:
   - Determine which facts the NEW FACT contradicts from either list.
   - A fact from EXISTING FACTS can be both a duplicate AND contradicted (e.g., semantically the same but the new fact updates/supersedes it).
   - Return all contradicted idx values in contradicted_facts.
   - If no contradictions, return an empty list for contradicted_facts.',
    '{"type": "object", "properties": {"duplicate_facts": {"type": "array", "items": {"type": "integer"}, "description": "List of idx values of duplicate facts (only from EXISTING FACTS range). Empty list if none."}, "contradicted_facts": {"type": "array", "items": {"type": "integer"}, "description": "List of idx values of contradicted facts (from full idx range). Empty list if none."}}, "required": ["duplicate_facts", "contradicted_facts"]}',
    TRUE,
    'gpt-4o-mini',
    40,
    '["去重", "边", "关系"]'
);

-- ============================================================
-- 初始化去重模板变量
-- ============================================================

-- DEDUPE_NODES_SINGLE 变量
INSERT INTO prompt_variable (template_id, variable_name, description, variable_type, required, default_value, source, sort)
SELECT id, 'previous_episodes', '历史上下文', 'text', FALSE, '', 'context', 1
FROM prompt_template WHERE code = 'DEDUPE_NODES_SINGLE';

INSERT INTO prompt_variable (template_id, variable_name, description, variable_type, required, default_value, source, sort)
SELECT id, 'episode_content', '当前消息内容', 'text', TRUE, NULL, 'context', 2
FROM prompt_template WHERE code = 'DEDUPE_NODES_SINGLE';

INSERT INTO prompt_variable (template_id, variable_name, description, variable_type, required, default_value, source, sort)
SELECT id, 'extracted_node', '新提取的实体', 'json', TRUE, NULL, 'context', 3
FROM prompt_template WHERE code = 'DEDUPE_NODES_SINGLE';

INSERT INTO prompt_variable (template_id, variable_name, description, variable_type, required, default_value, source, sort)
SELECT id, 'entity_type_description', '实体类型描述', 'text', TRUE, '通用实体类型', 'context', 4
FROM prompt_template WHERE code = 'DEDUPE_NODES_SINGLE';

INSERT INTO prompt_variable (template_id, variable_name, description, variable_type, required, default_value, source, sort)
SELECT id, 'existing_nodes', '现有实体列表', 'json', TRUE, NULL, 'context', 5
FROM prompt_template WHERE code = 'DEDUPE_NODES_SINGLE';

-- DEDUPE_NODES_BATCH 变量
INSERT INTO prompt_variable (template_id, variable_name, description, variable_type, required, default_value, source, sort)
SELECT id, 'previous_episodes', '历史上下文', 'text', FALSE, '', 'context', 1
FROM prompt_template WHERE code = 'DEDUPE_NODES_BATCH';

INSERT INTO prompt_variable (template_id, variable_name, description, variable_type, required, default_value, source, sort)
SELECT id, 'episode_content', '当前消息内容', 'text', TRUE, NULL, 'context', 2
FROM prompt_template WHERE code = 'DEDUPE_NODES_BATCH';

INSERT INTO prompt_variable (template_id, variable_name, description, variable_type, required, default_value, source, sort)
SELECT id, 'extracted_nodes', '新提取的实体列表', 'json', TRUE, NULL, 'context', 3
FROM prompt_template WHERE code = 'DEDUPE_NODES_BATCH';

INSERT INTO prompt_variable (template_id, variable_name, description, variable_type, required, default_value, source, sort)
SELECT id, 'existing_nodes', '现有实体列表', 'json', TRUE, NULL, 'context', 4
FROM prompt_template WHERE code = 'DEDUPE_NODES_BATCH';

INSERT INTO prompt_variable (template_id, variable_name, description, variable_type, required, default_value, source, sort)
SELECT id, 'entity_count', '实体数量', 'string', TRUE, NULL, 'llm', 5
FROM prompt_template WHERE code = 'DEDUPE_NODES_BATCH';

INSERT INTO prompt_variable (template_id, variable_name, description, variable_type, required, default_value, source, sort)
SELECT id, 'entity_count_minus_1', '实体数量减1', 'string', TRUE, NULL, 'llm', 6
FROM prompt_template WHERE code = 'DEDUPE_NODES_BATCH';

-- DEDUPE_NODES_GROUP 变量
INSERT INTO prompt_variable (template_id, variable_name, description, variable_type, required, default_value, source, sort)
SELECT id, 'nodes', '待分组的节点列表', 'json', TRUE, NULL, 'context', 1
FROM prompt_template WHERE code = 'DEDUPE_NODES_GROUP';

-- DEDUPE_EDGES 变量
INSERT INTO prompt_variable (template_id, variable_name, description, variable_type, required, default_value, source, sort)
SELECT id, 'existing_edges', '现有边列表', 'json', TRUE, NULL, 'context', 1
FROM prompt_template WHERE code = 'DEDUPE_EDGES';

INSERT INTO prompt_variable (template_id, variable_name, description, variable_type, required, default_value, source, sort)
SELECT id, 'edge_invalidation_candidates', '边失效候选列表', 'json', TRUE, NULL, 'context', 2
FROM prompt_template WHERE code = 'DEDUPE_EDGES';

INSERT INTO prompt_variable (template_id, variable_name, description, variable_type, required, default_value, source, sort)
SELECT id, 'new_edge', '新边', 'json', TRUE, NULL, 'context', 3
FROM prompt_template WHERE code = 'DEDUPE_EDGES';

-- ============================================================
-- 初始化去重版本记录
-- ============================================================

INSERT INTO prompt_version (template_id, version, system_prompt, user_prompt_template, response_format, description, active, created_by)
SELECT id, 1, system_prompt, user_prompt_template, response_format, '初始版本', TRUE, 1
FROM prompt_template WHERE code = 'DEDUPE_NODES_SINGLE';

INSERT INTO prompt_version (template_id, version, system_prompt, user_prompt_template, response_format, description, active, created_by)
SELECT id, 1, system_prompt, user_prompt_template, response_format, '初始版本', TRUE, 1
FROM prompt_template WHERE code = 'DEDUPE_NODES_BATCH';

INSERT INTO prompt_version (template_id, version, system_prompt, user_prompt_template, response_format, description, active, created_by)
SELECT id, 1, system_prompt, user_prompt_template, response_format, '初始版本', TRUE, 1
FROM prompt_template WHERE code = 'DEDUPE_NODES_GROUP';

INSERT INTO prompt_version (template_id, version, system_prompt, user_prompt_template, response_format, description, active, created_by)
SELECT id, 1, system_prompt, user_prompt_template, response_format, '初始版本', TRUE, 1
FROM prompt_template WHERE code = 'DEDUPE_EDGES';

-- ============================================================
-- 添加外键约束
-- ============================================================

-- prompt_variable 表添加外键
ALTER TABLE prompt_variable
ADD CONSTRAINT fk_prompt_variable_template
FOREIGN KEY (template_id)
REFERENCES prompt_template(id)
ON DELETE CASCADE;

-- prompt_version 表添加外键
ALTER TABLE prompt_version
ADD CONSTRAINT fk_prompt_version_template
FOREIGN KEY (template_id)
REFERENCES prompt_template(id)
ON DELETE CASCADE;

-- ============================================================
-- 验证查询
-- ============================================================

-- 查看所有模板
-- SELECT * FROM prompt_template;

-- 查看模板变量
-- SELECT pv.*, pt.code as template_code
-- FROM prompt_variable pv
-- JOIN prompt_template pt ON pv.template_id = pt.id
-- ORDER BY pv.template_id, pv.sort;

-- 查看版本历史
-- SELECT pv.*, pt.code as template_code
-- FROM prompt_version pv
-- JOIN prompt_template pt ON pv.template_id = pt.id
-- ORDER BY pv.template_id, pv.version DESC;
