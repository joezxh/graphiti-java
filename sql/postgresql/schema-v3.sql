-- ============================================================
-- 法律知识图谱 V3.0.0: PostgreSQL Schema 增量扩展
-- 版本: 2026-05-20
-- 说明: 仅包含新增的 4 张元数据表，不修改现有表
-- 要求: PostgreSQL 13+
-- 依赖: ont_definition 表已存在（definition_id FK 引用）
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
        -- 自引用：子类型指向父类型代码（如 DOMAIN_CIVIL 指向 DOMAIN_ROOT）
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
CREATE INDEX idx_community_type_sort ON ont_community_type(sort_order);

COMMENT ON TABLE ont_community_type IS '社区类型维度表 — 定义法律知识图谱中社区的分类体系，支持法律领域层级嵌套、辖区、应用场景三个正交维度';
COMMENT ON COLUMN ont_community_type.category IS '分类维度: domain(法律领域)|jurisdiction(管辖区)|practice(应用场景)';

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
        -- 是否审判阶段（庭审类为 true）
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
CREATE INDEX idx_episode_type_sort ON ont_episode_type(sort_order);

COMMENT ON TABLE ont_episode_type IS '剧集类型维度表 — 定义法律过程中事件的分类体系，兼容诉讼审级嵌套和 ADR 扁平化结构';
COMMENT ON COLUMN ont_episode_type.legal_process IS '法律程序: litigation(诉讼)|mediation(调解)|arbitration(仲裁)|execution(执行)';
COMMENT ON COLUMN ont_episode_type.court_level IS '审级: 一审|二审|再审|死刑复核（仅审判程序有值，ADR类为空）';

-- ---------- ont_entity_category: 实体分类层次表 ----------
CREATE TABLE ont_entity_category (
    id BIGSERIAL PRIMARY KEY,
    definition_id BIGINT NOT NULL,
    category_code VARCHAR(32) NOT NULL,
    category_name VARCHAR(128) NOT NULL,
    category_level INT NOT NULL DEFAULT 1,
        -- 1=一级, 2=二级, 3=三级
    parent_category_code VARCHAR(32),
        -- 自引用父分类
    entity_type_scope TEXT,
        -- JSON数组: ["Case", "Court"]
    default_attributes JSONB,
        -- 该分类下实体的默认属性模板
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
CREATE INDEX idx_entity_category_sort ON ont_entity_category(sort_order);

COMMENT ON TABLE ont_entity_category IS '实体分类层次表 — 定义法律实体的层级分类体系，与 ont_class 一一映射并扩展法律专项分类';
COMMENT ON COLUMN ont_entity_category.entity_type_scope IS 'JSON数组，适用的Neo4j实体类型标签，如 ["Case", "Court"]';

-- ---------- ont_relationship_meta: 关系类型元数据表 ----------
CREATE TABLE ont_relationship_meta (
    id BIGSERIAL PRIMARY KEY,
    definition_id BIGINT NOT NULL,
    relationship_type VARCHAR(64) NOT NULL,
        -- 对应 Neo4j 关系类型名
    relationship_name VARCHAR(128) NOT NULL,
        -- 中文显示名
    relationship_name_en VARCHAR(64),
        -- 英文名
    source_entity_types TEXT,
        -- JSON数组: ["Case", "JudgmentDocument"]
    target_entity_types TEXT,
        -- JSON数组: ["LegalProvision"]
    is_directional BOOLEAN DEFAULT TRUE,
        -- 是否有向（无向关系如 PARTY_OF, AFFIRMED_BY 为 false）
    is_transitive BOOLEAN DEFAULT FALSE,
        -- 是否可传递（仅 PRECEDES 支持）
    multiplicity VARCHAR(16) DEFAULT 'many-to-many',
        -- one-to-one | one-to-many | many-to-many
    default_weight DECIMAL(5,4) DEFAULT 1.0000,
        -- 默认权重（0.0000 - 1.0000），用于关系驱动的排序和推理
    validity_period JSONB,
        -- {hasPeriod: boolean, defaultDays: integer|null}
    description TEXT,
    example_cypher TEXT,
        -- 示例 Cypher 语句
    sort_order INT DEFAULT 0,
    metadata JSONB,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_relationship_type UNIQUE (definition_id, relationship_type),
    CONSTRAINT fk_relationship_meta_definition FOREIGN KEY (definition_id) REFERENCES ont_definition(id) ON DELETE CASCADE
);

CREATE INDEX idx_relationship_meta_definition ON ont_relationship_meta(definition_id);
CREATE INDEX idx_relationship_meta_sort ON ont_relationship_meta(sort_order);
CREATE INDEX idx_relationship_meta_directional ON ont_relationship_meta(is_directional);

COMMENT ON TABLE ont_relationship_meta IS '关系类型元数据表 — 定义预置关系类型的语义属性和约束，用于前端类型选择和后端关系推理';
COMMENT ON COLUMN ont_relationship_meta.default_weight IS '默认权重，范围 0.0000 - 1.0000，用于关系驱动的排序和推理';
