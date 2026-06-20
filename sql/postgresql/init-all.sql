-- ============================================================
-- OntoGraph 一键初始化脚本 (PostgreSQL 完整版)
-- 版本: 2026-06-20
-- 说明: 合并全部 DDL + 迁移 + 初始化数据，一键执行即可搭建完整数据库
-- 要求: PostgreSQL 13+
-- 执行: psql -U postgres -d graphiti -f init-all.sql
-- ============================================================
-- 包含内容:
--   Part 1:  公共触发器函数
--   Part 2:  系统管理模块 DDL (10 张表)
--   Part 3:  图谱与本体管理 DDL (10 张表)
--   Part 4:  提示词与自定义指令 DDL (4 张表)
--   Part 5:  V3 元数据扩展 DDL (4 张表 + 迁移字段)
--   Part 6:  OKF Pipeline DDL (10 张表)
--   Part 7:  系统权限初始化数据
--   Part 8:  完整菜单初始化
--   Part 9:  图谱 + 本体定义初始化
--   Part 10: 提示词 + 自定义指令初始化
--   Part 11: V3 元数据初始化 (社区类型 / Episode类型 / 实体分类 / 关系元数据)
--   Part 12: 验证统计
-- ============================================================

SET client_encoding = 'UTF8';


-- ============================================================
-- Part 1: 公共触发器函数
-- ============================================================

DROP FUNCTION IF EXISTS update_updated_at_column() CASCADE;

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.update_time = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


-- ============================================================
-- Part 2: 系统管理模块 DDL (10 张表)
-- ============================================================

-- 2.1 系统用户表
DROP TABLE IF EXISTS sys_user CASCADE;
CREATE TABLE sys_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    nickname VARCHAR(50),
    email VARCHAR(100),
    mobile VARCHAR(32),
    status SMALLINT NOT NULL DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE TRIGGER update_sys_user_update_time BEFORE UPDATE ON sys_user FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE INDEX idx_sys_user_status ON sys_user(status);
CREATE INDEX idx_sys_user_deleted ON sys_user(deleted);
COMMENT ON TABLE sys_user IS '系统用户表';

-- 2.2 系统角色表
DROP TABLE IF EXISTS sys_role CASCADE;
CREATE TABLE sys_role (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    status SMALLINT NOT NULL DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE TRIGGER update_sys_role_update_time BEFORE UPDATE ON sys_role FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE INDEX idx_sys_role_deleted ON sys_role(deleted);
COMMENT ON TABLE sys_role IS '系统角色表';

-- 2.3 用户角色关联表
DROP TABLE IF EXISTS sys_user_role CASCADE;
CREATE TABLE sys_user_role (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE
);
CREATE INDEX idx_sys_user_role_user_id ON sys_user_role(user_id);
CREATE INDEX idx_sys_user_role_role_id ON sys_user_role(role_id);
COMMENT ON TABLE sys_user_role IS '用户角色关联表';

-- 2.4 系统菜单表
DROP TABLE IF EXISTS sys_menu CASCADE;
CREATE TABLE sys_menu (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    permission VARCHAR(100),
    url VARCHAR(200),
    parent_id BIGINT DEFAULT 0,
    sort INT DEFAULT 0,
    type SMALLINT NOT NULL DEFAULT 2,
    icon VARCHAR(100),
    component VARCHAR(200),
    status SMALLINT NOT NULL DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE TRIGGER update_sys_menu_update_time BEFORE UPDATE ON sys_menu FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE INDEX idx_sys_menu_parent_id ON sys_menu(parent_id);
CREATE INDEX idx_sys_menu_deleted ON sys_menu(deleted);
COMMENT ON TABLE sys_menu IS '系统菜单表';

-- 2.5 角色菜单关联表
DROP TABLE IF EXISTS sys_role_menu CASCADE;
CREATE TABLE sys_role_menu (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    CONSTRAINT fk_role_menu_role FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_menu_menu FOREIGN KEY (menu_id) REFERENCES sys_menu(id) ON DELETE CASCADE
);
CREATE INDEX idx_sys_role_menu_role_id ON sys_role_menu(role_id);
CREATE INDEX idx_sys_role_menu_menu_id ON sys_role_menu(menu_id);
COMMENT ON TABLE sys_role_menu IS '角色菜单关联表';

-- 2.6 系统操作日志表
DROP TABLE IF EXISTS sys_operation_log CASCADE;
CREATE TABLE sys_operation_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(50),
    operation VARCHAR(100),
    method VARCHAR(200),
    params TEXT,
    ip VARCHAR(50),
    location VARCHAR(100),
    status SMALLINT,
    error_msg VARCHAR(500),
    duration INT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_sys_operation_log_username ON sys_operation_log(username);
CREATE INDEX idx_sys_operation_log_operation ON sys_operation_log(operation);
CREATE INDEX idx_sys_operation_log_create_time ON sys_operation_log(create_time);
COMMENT ON TABLE sys_operation_log IS '系统操作日志表';

-- 2.7 系统配置表
DROP TABLE IF EXISTS sys_system_config CASCADE;
CREATE TABLE sys_system_config (
    id BIGSERIAL PRIMARY KEY,
    config_key VARCHAR(100) NOT NULL UNIQUE,
    config_value TEXT,
    config_name VARCHAR(100),
    config_description VARCHAR(500),
    config_type SMALLINT DEFAULT 1,
    group_name VARCHAR(50),
    sort_num INT DEFAULT 0,
    status SMALLINT DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE TRIGGER update_sys_system_config_update_time BEFORE UPDATE ON sys_system_config FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE INDEX idx_sys_system_config_group_name ON sys_system_config(group_name);
CREATE INDEX idx_sys_system_config_deleted ON sys_system_config(deleted);
COMMENT ON TABLE sys_system_config IS '系统配置表';

-- 2.8 搜索历史表
DROP TABLE IF EXISTS sys_search_history CASCADE;
CREATE TABLE sys_search_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    query VARCHAR(500) NOT NULL,
    mode VARCHAR(20),
    result_count INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_sys_search_history_user_id ON sys_search_history(user_id);
CREATE INDEX idx_sys_search_history_create_time ON sys_search_history(create_time);
COMMENT ON TABLE sys_search_history IS '搜索历史记录表';

-- 2.9 系统通知表
DROP TABLE IF EXISTS sys_notification CASCADE;
CREATE TABLE sys_notification (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    type SMALLINT NOT NULL DEFAULT 1,
    is_read SMALLINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE TRIGGER update_sys_notification_update_time BEFORE UPDATE ON sys_notification FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE INDEX idx_sys_notification_user_id ON sys_notification(user_id);
CREATE INDEX idx_sys_notification_type ON sys_notification(type);
CREATE INDEX idx_sys_notification_is_read ON sys_notification(is_read);
CREATE INDEX idx_sys_notification_deleted ON sys_notification(deleted);
COMMENT ON TABLE sys_notification IS '系统通知表';

-- 2.10 用户通知设置表
DROP TABLE IF EXISTS sys_user_notification_settings CASCADE;
CREATE TABLE sys_user_notification_settings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    system_enabled SMALLINT NOT NULL DEFAULT 1,
    graph_enabled SMALLINT NOT NULL DEFAULT 1,
    search_enabled SMALLINT NOT NULL DEFAULT 1,
    email_enabled SMALLINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE TRIGGER update_sys_user_notification_settings_update_time BEFORE UPDATE ON sys_user_notification_settings FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE INDEX idx_sys_user_notification_settings_user_id ON sys_user_notification_settings(user_id);
COMMENT ON TABLE sys_user_notification_settings IS '用户通知设置表';


-- ============================================================
-- Part 3: 图谱与本体管理 DDL (10 张表)
-- ============================================================

-- 3.1 图谱元数据表
DROP TABLE IF EXISTS graphiti_graph_metadata CASCADE;
CREATE TABLE graphiti_graph_metadata (
    id BIGSERIAL PRIMARY KEY,
    graph_id VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    node_count INTEGER DEFAULT 0,
    edge_count INTEGER DEFAULT 0,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE TRIGGER update_graphiti_graph_metadata_update_time BEFORE UPDATE ON graphiti_graph_metadata FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE UNIQUE INDEX uk_graphiti_graph_metadata_graph_id ON graphiti_graph_metadata(graph_id);
CREATE INDEX idx_graphiti_graph_metadata_deleted ON graphiti_graph_metadata(deleted);
COMMENT ON TABLE graphiti_graph_metadata IS '图谱元数据表';

-- 3.2 本体定义表
DROP TABLE IF EXISTS ont_definition CASCADE;
CREATE TABLE ont_definition (
    id BIGSERIAL PRIMARY KEY,
    graph_id VARCHAR(64) NOT NULL,
    namespace VARCHAR(255) DEFAULT 'http://legal-ai.cc/ontology',
    name VARCHAR(128) NOT NULL,
    version VARCHAR(32) NOT NULL DEFAULT '1.0.0',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    description TEXT,
    parent_version_id BIGINT,
    created_by VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_ont_def_graph_version UNIQUE (graph_id, version)
);
CREATE INDEX idx_ont_def_graph_id ON ont_definition(graph_id);
CREATE INDEX idx_ont_def_status ON ont_definition(status);
COMMENT ON TABLE ont_definition IS '本体定义表';

-- 3.3 本体类表
DROP TABLE IF EXISTS ont_class CASCADE;
CREATE TABLE ont_class (
    id BIGSERIAL PRIMARY KEY,
    definition_id BIGINT NOT NULL,
    class_uri VARCHAR(512) NOT NULL,
    local_name VARCHAR(128) NOT NULL,
    name_en VARCHAR(128),
    parent_class_id BIGINT,
    equivalent_to TEXT,
    disjoint_with TEXT,
    description TEXT,
    example TEXT,
    domain_hint VARCHAR(32),
    metadata TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_ont_class_uri UNIQUE (definition_id, class_uri),
    CONSTRAINT uk_ont_class_local_name UNIQUE (definition_id, local_name),
    CONSTRAINT fk_ont_class_definition FOREIGN KEY (definition_id) REFERENCES ont_definition(id) ON DELETE CASCADE,
    CONSTRAINT fk_ont_class_parent FOREIGN KEY (parent_class_id) REFERENCES ont_class(id) ON DELETE SET NULL
);
CREATE INDEX idx_ont_class_definition ON ont_class(definition_id);
CREATE INDEX idx_ont_class_parent ON ont_class(parent_class_id);
CREATE INDEX idx_ont_class_domain ON ont_class(domain_hint);
COMMENT ON TABLE ont_class IS '本体类表';

-- 3.4 本体类继承关系表
DROP TABLE IF EXISTS ont_class_inheritance CASCADE;
CREATE TABLE ont_class_inheritance (
    id BIGSERIAL PRIMARY KEY,
    class_id BIGINT NOT NULL,
    parent_class_id BIGINT NOT NULL,
    definition_id BIGINT NOT NULL,
    distance INTEGER DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_inheritance_class FOREIGN KEY (class_id) REFERENCES ont_class(id) ON DELETE CASCADE,
    CONSTRAINT fk_inheritance_parent FOREIGN KEY (parent_class_id) REFERENCES ont_class(id) ON DELETE CASCADE,
    CONSTRAINT fk_inheritance_definition FOREIGN KEY (definition_id) REFERENCES ont_definition(id) ON DELETE CASCADE,
    CONSTRAINT uk_ont_inheritance_pair UNIQUE (class_id, parent_class_id)
);
CREATE INDEX idx_ont_inheritance_class ON ont_class_inheritance(class_id);
CREATE INDEX idx_ont_inheritance_parent ON ont_class_inheritance(parent_class_id);
COMMENT ON TABLE ont_class_inheritance IS '本体类继承关系表';

-- 3.5 本体属性表
DROP TABLE IF EXISTS ont_property CASCADE;
CREATE TABLE ont_property (
    id BIGSERIAL PRIMARY KEY,
    definition_id BIGINT NOT NULL,
    property_uri VARCHAR(512) NOT NULL,
    local_name VARCHAR(128) NOT NULL,
    property_type VARCHAR(20) NOT NULL DEFAULT 'DATATYPE',
    domain_class_id BIGINT,
    range_class_id BIGINT,
    range_data_type VARCHAR(32),
    min_cardinality INTEGER DEFAULT 0,
    max_cardinality INTEGER,
    default_value VARCHAR(512),
    allowed_values TEXT,
    parent_property_id BIGINT,
    equivalent_to TEXT,
    inverse_of_id BIGINT,
    is_required BOOLEAN DEFAULT FALSE,
    is_multiple BOOLEAN DEFAULT FALSE,
    pattern VARCHAR(256),
    min_value DECIMAL(20,6),
    max_value DECIMAL(20,6),
    description TEXT,
    example TEXT,
    metadata TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_ont_prop_uri UNIQUE (definition_id, property_uri),
    CONSTRAINT fk_ont_prop_definition FOREIGN KEY (definition_id) REFERENCES ont_definition(id) ON DELETE CASCADE,
    CONSTRAINT fk_ont_prop_domain FOREIGN KEY (domain_class_id) REFERENCES ont_class(id) ON DELETE CASCADE,
    CONSTRAINT fk_ont_prop_range FOREIGN KEY (range_class_id) REFERENCES ont_class(id) ON DELETE SET NULL,
    CONSTRAINT fk_ont_prop_parent FOREIGN KEY (parent_property_id) REFERENCES ont_property(id) ON DELETE SET NULL,
    CONSTRAINT fk_ont_prop_inverse FOREIGN KEY (inverse_of_id) REFERENCES ont_property(id) ON DELETE SET NULL
);
CREATE INDEX idx_ont_prop_definition ON ont_property(definition_id);
CREATE INDEX idx_ont_prop_domain ON ont_property(domain_class_id);
CREATE INDEX idx_ont_prop_type ON ont_property(property_type);
COMMENT ON TABLE ont_property IS '本体属性表';

-- 3.6 本体约束表
DROP TABLE IF EXISTS ont_constraint CASCADE;
CREATE TABLE ont_constraint (
    id BIGSERIAL PRIMARY KEY,
    definition_id BIGINT NOT NULL,
    class_id BIGINT,
    property_id BIGINT,
    constraint_type VARCHAR(32) NOT NULL,
    value TEXT NOT NULL,
    error_message VARCHAR(512),
    severity VARCHAR(10) NOT NULL DEFAULT 'ERROR',
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ont_constraint_definition FOREIGN KEY (definition_id) REFERENCES ont_definition(id) ON DELETE CASCADE,
    CONSTRAINT fk_ont_constraint_class FOREIGN KEY (class_id) REFERENCES ont_class(id) ON DELETE CASCADE,
    CONSTRAINT fk_ont_constraint_property FOREIGN KEY (property_id) REFERENCES ont_property(id) ON DELETE CASCADE
);
CREATE INDEX idx_ont_constraint_definition ON ont_constraint(definition_id);
CREATE INDEX idx_ont_constraint_type ON ont_constraint(constraint_type);
COMMENT ON TABLE ont_constraint IS '本体约束表';

-- 3.7 本体版本历史表
DROP TABLE IF EXISTS ont_version_history CASCADE;
CREATE TABLE ont_version_history (
    id BIGSERIAL PRIMARY KEY,
    definition_id BIGINT NOT NULL,
    version VARCHAR(32) NOT NULL,
    change_type VARCHAR(32) NOT NULL,
    entity_type VARCHAR(20) NOT NULL,
    entity_id BIGINT,
    before_state TEXT,
    after_state TEXT,
    diff_summary VARCHAR(512),
    changed_by VARCHAR(64),
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ont_version_def FOREIGN KEY (definition_id) REFERENCES ont_definition(id) ON DELETE CASCADE
);
CREATE INDEX idx_ont_version_def ON ont_version_history(definition_id);
CREATE INDEX idx_ont_version_time ON ont_version_history(changed_at DESC);
COMMENT ON TABLE ont_version_history IS '本体版本历史表';

-- 3.8 本体映射表
DROP TABLE IF EXISTS ont_mapping CASCADE;
CREATE TABLE ont_mapping (
    id BIGSERIAL PRIMARY KEY,
    definition_id BIGINT NOT NULL,
    source_ontology VARCHAR(256),
    source_type VARCHAR(64),
    mapped_class_uri VARCHAR(512) NOT NULL,
    mapping_type VARCHAR(32) NOT NULL DEFAULT 'EQUIVALENT',
    confidence DECIMAL(5,4) DEFAULT 1.0000,
    metadata TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ont_mapping_def FOREIGN KEY (definition_id) REFERENCES ont_definition(id) ON DELETE CASCADE,
    CONSTRAINT uk_ont_mapping_src_tgt UNIQUE (definition_id, source_ontology, source_type, mapped_class_uri)
);
CREATE INDEX idx_ont_mapping_def ON ont_mapping(definition_id);
COMMENT ON TABLE ont_mapping IS '本体映射表';

-- 3.9 本体草稿表
DROP TABLE IF EXISTS ont_draft CASCADE;
CREATE TABLE ont_draft (
    id BIGSERIAL PRIMARY KEY,
    graph_id VARCHAR(64) NOT NULL,
    draft_name VARCHAR(128) NOT NULL,
    draft_type VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    source_info TEXT,
    generated_info TEXT,
    mock_data TEXT,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_by VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TRIGGER update_ont_draft_update_time BEFORE UPDATE ON ont_draft FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE INDEX idx_ont_draft_graph_id ON ont_draft(graph_id);
CREATE INDEX idx_ont_draft_status ON ont_draft(status);
COMMENT ON TABLE ont_draft IS '本体草稿表';

-- 3.10 领域规则表
DROP TABLE IF EXISTS ont_domain_rule CASCADE;
CREATE TABLE ont_domain_rule (
    id BIGSERIAL PRIMARY KEY,
    definition_id BIGINT NOT NULL,
    rule_name VARCHAR(255) NOT NULL,
    rule_code VARCHAR(100) NOT NULL UNIQUE,
    spel_expression TEXT NOT NULL,
    applicable_class_ids JSONB,
    severity VARCHAR(50) DEFAULT 'WARNING',
    error_message TEXT,
    description TEXT,
    enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_ont_domain_rule_definition_id ON ont_domain_rule(definition_id);
CREATE INDEX idx_ont_domain_rule_enabled ON ont_domain_rule(enabled);
COMMENT ON TABLE ont_domain_rule IS '领域规则定义表';


-- ============================================================
-- Part 4: 提示词与自定义指令 DDL (4 张表)
-- ============================================================

DROP TABLE IF EXISTS prompt_template CASCADE;
CREATE TABLE prompt_template (
    id BIGSERIAL PRIMARY KEY, code VARCHAR(64) NOT NULL UNIQUE, name VARCHAR(128) NOT NULL,
    description TEXT, type VARCHAR(32), system_prompt TEXT, user_prompt_template TEXT,
    response_format TEXT, enabled BOOLEAN DEFAULT TRUE, model VARCHAR(64), sort INT DEFAULT 0,
    tags TEXT, extra_config TEXT, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, created_by BIGINT, updated_by BIGINT
);
CREATE TRIGGER update_prompt_template_update_time BEFORE UPDATE ON prompt_template FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE INDEX idx_prompt_template_type ON prompt_template(type);
CREATE INDEX idx_prompt_template_enabled ON prompt_template(enabled);
COMMENT ON TABLE prompt_template IS '提示词模板表';

DROP TABLE IF EXISTS prompt_variable CASCADE;
CREATE TABLE prompt_variable (
    id BIGSERIAL PRIMARY KEY, template_id BIGINT NOT NULL, variable_name VARCHAR(64) NOT NULL,
    description TEXT, variable_type VARCHAR(32), required BOOLEAN DEFAULT TRUE,
    default_value VARCHAR(512), source VARCHAR(32), validation_rule VARCHAR(256),
    sort INT DEFAULT 0, remark TEXT, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_prompt_var_template FOREIGN KEY (template_id) REFERENCES prompt_template(id) ON DELETE CASCADE
);
CREATE TRIGGER update_prompt_variable_update_time BEFORE UPDATE ON prompt_variable FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE INDEX idx_prompt_var_template ON prompt_variable(template_id);
COMMENT ON TABLE prompt_variable IS '提示词变量表';

DROP TABLE IF EXISTS prompt_version CASCADE;
CREATE TABLE prompt_version (
    id BIGSERIAL PRIMARY KEY, template_id BIGINT NOT NULL, version INT NOT NULL,
    system_prompt TEXT, user_prompt_template TEXT, response_format TEXT, description TEXT,
    active BOOLEAN DEFAULT FALSE, created_by BIGINT, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_prompt_ver_template FOREIGN KEY (template_id) REFERENCES prompt_template(id) ON DELETE CASCADE
);
CREATE INDEX idx_prompt_ver_template ON prompt_version(template_id);
CREATE INDEX idx_prompt_ver_active ON prompt_version(active);
COMMENT ON TABLE prompt_version IS '提示词版本表';

DROP TABLE IF EXISTS custom_instruction CASCADE;
CREATE TABLE custom_instruction (
    id BIGSERIAL PRIMARY KEY, graph_id VARCHAR(64), instruction TEXT NOT NULL,
    enabled BOOLEAN DEFAULT TRUE, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TRIGGER update_custom_instruction_update_time BEFORE UPDATE ON custom_instruction FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE INDEX idx_custom_instruction_graph_id ON custom_instruction(graph_id);
COMMENT ON TABLE custom_instruction IS '自定义抽取指令表';


-- ============================================================
-- Part 5: V3 元数据扩展 DDL (4 张表，含全部迁移字段最终态)
-- ============================================================

DROP TABLE IF EXISTS ont_community_type CASCADE;
CREATE TABLE ont_community_type (
    id BIGSERIAL PRIMARY KEY, definition_id BIGINT NOT NULL, type_code VARCHAR(64) NOT NULL,
    type_name VARCHAR(128) NOT NULL, type_name_en VARCHAR(64), category VARCHAR(32) NOT NULL DEFAULT 'domain',
    description TEXT, parent_type_code VARCHAR(64), sort_order INT DEFAULT 0, metadata JSONB,
    status VARCHAR(20) DEFAULT 'ACTIVE', community_uuid VARCHAR(64), graph_id VARCHAR(64),
    region VARCHAR(32), scenario_type VARCHAR(32),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_community_type_code UNIQUE (definition_id, type_code),
    CONSTRAINT fk_community_type_definition FOREIGN KEY (definition_id) REFERENCES ont_definition(id) ON DELETE CASCADE
);
CREATE INDEX idx_community_type_definition ON ont_community_type(definition_id);
CREATE INDEX idx_community_type_category ON ont_community_type(category);
COMMENT ON TABLE ont_community_type IS '社区类型维度表';

DROP TABLE IF EXISTS ont_episode_type CASCADE;
CREATE TABLE ont_episode_type (
    id BIGSERIAL PRIMARY KEY, definition_id BIGINT NOT NULL, type_code VARCHAR(50) NOT NULL,
    type_name VARCHAR(128) NOT NULL, type_name_en VARCHAR(64),
    process_type VARCHAR(32), stage_label VARCHAR(32), stage_level VARCHAR(32),
    is_review_stage BOOLEAN DEFAULT FALSE, parent_type_code VARCHAR(50), level INT DEFAULT 1,
    created_by VARCHAR(64), updated_by VARCHAR(64), version INT DEFAULT 1,
    description TEXT, sort_order INT DEFAULT 0, metadata JSONB, status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_episode_type_code UNIQUE (definition_id, type_code),
    CONSTRAINT chk_episode_type_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'DEPRECATED')),
    CONSTRAINT chk_episode_type_level CHECK (level BETWEEN 1 AND 5),
    CONSTRAINT fk_episode_type_definition FOREIGN KEY (definition_id) REFERENCES ont_definition(id) ON DELETE CASCADE
);
CREATE INDEX idx_episode_type_definition ON ont_episode_type(definition_id);
COMMENT ON TABLE ont_episode_type IS '剧集类型维度表';

DROP TABLE IF EXISTS ont_entity_category CASCADE;
CREATE TABLE ont_entity_category (
    id BIGSERIAL PRIMARY KEY, definition_id BIGINT NOT NULL, category_code VARCHAR(32) NOT NULL,
    category_name VARCHAR(128) NOT NULL, category_level INT NOT NULL DEFAULT 1,
    parent_category_code VARCHAR(32), entity_type_scope TEXT, default_attributes JSONB,
    description TEXT, sort_order INT DEFAULT 0, metadata JSONB, status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_entity_category_code UNIQUE (definition_id, category_code),
    CONSTRAINT fk_entity_category_definition FOREIGN KEY (definition_id) REFERENCES ont_definition(id) ON DELETE CASCADE
);
CREATE INDEX idx_entity_category_definition ON ont_entity_category(definition_id);
COMMENT ON TABLE ont_entity_category IS '实体分类层次表';

DROP TABLE IF EXISTS ont_relationship_meta CASCADE;
CREATE TABLE ont_relationship_meta (
    id BIGSERIAL PRIMARY KEY, definition_id BIGINT NOT NULL, relationship_type VARCHAR(64) NOT NULL,
    relationship_name VARCHAR(128) NOT NULL, relationship_name_en VARCHAR(64),
    source_entity_types TEXT, target_entity_types TEXT,
    is_directional BOOLEAN DEFAULT TRUE, is_transitive BOOLEAN DEFAULT FALSE,
    multiplicity VARCHAR(16) DEFAULT 'many-to-many', default_weight DECIMAL(5,4) DEFAULT 1.0000,
    validity_period JSONB, description TEXT, example_cypher TEXT,
    sort_order INT DEFAULT 0, metadata JSONB, status VARCHAR(20) DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_relationship_type UNIQUE (definition_id, relationship_type),
    CONSTRAINT fk_relationship_meta_definition FOREIGN KEY (definition_id) REFERENCES ont_definition(id) ON DELETE CASCADE
);
CREATE INDEX idx_relationship_meta_definition ON ont_relationship_meta(definition_id);
COMMENT ON TABLE ont_relationship_meta IS '关系类型元数据表';


-- ============================================================
-- Part 6: OKF Pipeline DDL (10 张表 — 含 6 张新增补全)
-- ============================================================

DROP TABLE IF EXISTS okf_bundle_meta CASCADE;
CREATE TABLE okf_bundle_meta (
    id BIGSERIAL PRIMARY KEY, bundle_id VARCHAR(128) NOT NULL, name VARCHAR(255) NOT NULL,
    slug VARCHAR(128), description TEXT, bundle_type VARCHAR(32), version VARCHAR(32),
    license VARCHAR(64), author VARCHAR(128), source_url VARCHAR(512), graph_id VARCHAR(64),
    status VARCHAR(20) DEFAULT 'ACTIVE', is_featured BOOLEAN DEFAULT FALSE,
    tags JSONB, metadata JSONB, concept_count INT DEFAULT 0, download_count INT DEFAULT 0,
    star_count INT DEFAULT 0, create_by BIGINT, update_by BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE UNIQUE INDEX uk_okf_bundle_meta_bundle_id ON okf_bundle_meta(bundle_id);
COMMENT ON TABLE okf_bundle_meta IS 'OKF Bundle 元数据表';

DROP TABLE IF EXISTS okf_concept CASCADE;
CREATE TABLE okf_concept (
    id BIGSERIAL PRIMARY KEY, concept_id VARCHAR(128) NOT NULL, bundle_id VARCHAR(128) NOT NULL,
    name VARCHAR(255) NOT NULL, slug VARCHAR(128), doc_type VARCHAR(32),
    frontmatter JSONB, content_md TEXT, content_html TEXT, summary TEXT,
    ont_class_id VARCHAR(128), is_published BOOLEAN DEFAULT FALSE, is_featured BOOLEAN DEFAULT FALSE,
    tags JSONB, metadata JSONB, link_count INT DEFAULT 0, view_count INT DEFAULT 0,
    create_by BIGINT, update_by BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE UNIQUE INDEX uk_okf_concept_concept_id ON okf_concept(concept_id);
CREATE INDEX idx_okf_concept_bundle ON okf_concept(bundle_id);
COMMENT ON TABLE okf_concept IS 'OKF 概念文档表';

DROP TABLE IF EXISTS okf_bundle_alias CASCADE;
CREATE TABLE okf_bundle_alias (
    id BIGSERIAL PRIMARY KEY, bundle_id VARCHAR(128) NOT NULL, alias VARCHAR(255) NOT NULL,
    locale VARCHAR(16) DEFAULT 'zh-CN', is_primary BOOLEAN DEFAULT FALSE,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_okf_bundle_alias_bundle ON okf_bundle_alias(bundle_id);
COMMENT ON TABLE okf_bundle_alias IS 'OKF Bundle 别名表';

DROP TABLE IF EXISTS okf_concept_alias CASCADE;
CREATE TABLE okf_concept_alias (
    id BIGSERIAL PRIMARY KEY, concept_id VARCHAR(128) NOT NULL, alias VARCHAR(255) NOT NULL,
    locale VARCHAR(16) DEFAULT 'zh-CN', is_primary BOOLEAN DEFAULT FALSE,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_okf_concept_alias_concept ON okf_concept_alias(concept_id);
COMMENT ON TABLE okf_concept_alias IS 'OKF 概念别名表';

DROP TABLE IF EXISTS okf_bundle_ontology_mapping CASCADE;
CREATE TABLE okf_bundle_ontology_mapping (
    id BIGSERIAL PRIMARY KEY, bundle_id VARCHAR(128) NOT NULL, ont_class_id VARCHAR(128),
    ont_property_id VARCHAR(128), mapping_type VARCHAR(32), mapping_config JSONB,
    confidence DECIMAL(5,4) DEFAULT 1.0000, create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_okf_bundle_ont_mapping_bundle ON okf_bundle_ontology_mapping(bundle_id);
COMMENT ON TABLE okf_bundle_ontology_mapping IS 'OKF Bundle-本体映射表';

DROP TABLE IF EXISTS okf_frontmatter_field CASCADE;
CREATE TABLE okf_frontmatter_field (
    id BIGSERIAL PRIMARY KEY, field_name VARCHAR(128) NOT NULL, field_label VARCHAR(128),
    field_type VARCHAR(32), field_schema JSONB, default_value VARCHAR(512),
    is_required BOOLEAN DEFAULT FALSE, is_system BOOLEAN DEFAULT FALSE,
    validation_rule VARCHAR(256), sort_order INT DEFAULT 0, status VARCHAR(20) DEFAULT 'ACTIVE',
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);
COMMENT ON TABLE okf_frontmatter_field IS 'OKF Frontmatter 字段定义表';

DROP TABLE IF EXISTS okf_concept_link CASCADE;
CREATE TABLE okf_concept_link (
    id BIGSERIAL PRIMARY KEY, source_concept_id VARCHAR(512) NOT NULL, target_concept_id VARCHAR(512) NOT NULL,
    link_type VARCHAR(32) DEFAULT 'reference', link_text VARCHAR(512),
    detected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_okf_concept_link UNIQUE (source_concept_id, target_concept_id, link_type)
);
CREATE INDEX idx_okf_concept_link_source ON okf_concept_link(source_concept_id);
CREATE INDEX idx_okf_concept_link_target ON okf_concept_link(target_concept_id);
COMMENT ON TABLE okf_concept_link IS 'OKF 概念交叉链接图';

DROP TABLE IF EXISTS okf_pipeline_execution CASCADE;
CREATE TABLE okf_pipeline_execution (
    id BIGSERIAL PRIMARY KEY, execution_id VARCHAR(64) NOT NULL UNIQUE,
    trigger_type VARCHAR(32) NOT NULL DEFAULT 'manual', status VARCHAR(32) NOT NULL DEFAULT 'pending',
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, completed_at TIMESTAMP,
    total_nodes INT NOT NULL DEFAULT 9, completed_nodes INT NOT NULL DEFAULT 0,
    failed_node VARCHAR(64), error_message TEXT,
    documents_generated INT NOT NULL DEFAULT 0, documents_updated INT NOT NULL DEFAULT 0,
    execution_summary JSONB, create_by BIGINT,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, deleted BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_okf_pipeline_execution_status ON okf_pipeline_execution(status);
COMMENT ON TABLE okf_pipeline_execution IS 'OKF Pipeline 执行记录';

DROP TABLE IF EXISTS okf_pipeline_node_execution CASCADE;
CREATE TABLE okf_pipeline_node_execution (
    id BIGSERIAL PRIMARY KEY, execution_id VARCHAR(64) NOT NULL, node_name VARCHAR(64) NOT NULL,
    node_order INT NOT NULL, status VARCHAR(32) NOT NULL DEFAULT 'pending',
    started_at TIMESTAMP, completed_at TIMESTAMP,
    input_artifacts JSONB, output_artifacts JSONB, metrics JSONB,
    error_message TEXT, create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, deleted BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_okf_pipeline_node_exec ON okf_pipeline_node_execution(execution_id);
COMMENT ON TABLE okf_pipeline_node_execution IS 'OKF Pipeline 节点级执行记录';

DROP TABLE IF EXISTS okf_quality_report CASCADE;
CREATE TABLE okf_quality_report (
    id BIGSERIAL PRIMARY KEY, execution_id VARCHAR(64),
    report_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total_documents INT NOT NULL DEFAULT 0, okf_conformant INT NOT NULL DEFAULT 0,
    missing_type INT NOT NULL DEFAULT 0, broken_links INT NOT NULL DEFAULT 0,
    orphan_pages INT NOT NULL DEFAULT 0, stale_pages INT NOT NULL DEFAULT 0,
    quality_score DECIMAL(5,2) NOT NULL DEFAULT 0.00, report_json JSONB,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, deleted BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX idx_okf_quality_execution ON okf_quality_report(execution_id);
COMMENT ON TABLE okf_quality_report IS 'OKF 文档质量报告';

DO $$ BEGIN RAISE NOTICE '========== DDL 创建完成: 共 38 张表 =========='; END $$;


-- ============================================================
-- Part 7~11: 初始化数据
-- ============================================================
-- 以下使用 \i 引入同目录下的现有初始化脚本
-- 执行前请确保当前工作目录为 sql/postgresql/
--
--   cd sql/postgresql/
--   psql -U postgres -d graphiti -f init-all.sql
--
-- ============================================================

-- Part 7+8+9+10: 系统权限 + 图谱 + 本体 + 提示词 + 自定义指令
\i init-data.sql

-- Part 8 (完整菜单): 覆盖 Part 7 中的简版菜单，使用国际化 key 的完整菜单
-- 注意: 如需保留 Part 7 的菜单，可跳过此行
-- DELETE FROM sys_role_menu;
-- DELETE FROM sys_menu;
\i init-menu-complete.sql

-- Part 11: V3 元数据初始化 (社区类型 + Episode类型 + 实体分类 + 关系元数据)
\i init-data-v3.sql


-- ============================================================
-- Part 12: 最终验证统计
-- ============================================================

SELECT '系统用户' AS 模块, count(*) AS 数量 FROM sys_user
UNION ALL SELECT '系统角色', count(*) FROM sys_role
UNION ALL SELECT '系统菜单', count(*) FROM sys_menu WHERE deleted = FALSE
UNION ALL SELECT '角色菜单', count(*) FROM sys_role_menu
UNION ALL SELECT '图谱元数据', count(*) FROM graphiti_graph_metadata WHERE deleted = FALSE
UNION ALL SELECT '本体定义', count(*) FROM ont_definition
UNION ALL SELECT '本体类', count(*) FROM ont_class
UNION ALL SELECT '本体属性', count(*) FROM ont_property
UNION ALL SELECT '本体约束', count(*) FROM ont_constraint
UNION ALL SELECT '领域规则', count(*) FROM ont_domain_rule
UNION ALL SELECT '社区类型', count(*) FROM ont_community_type
UNION ALL SELECT '剧集类型', count(*) FROM ont_episode_type
UNION ALL SELECT '实体分类', count(*) FROM ont_entity_category
UNION ALL SELECT '关系元数据', count(*) FROM ont_relationship_meta
UNION ALL SELECT '提示词模板', count(*) FROM prompt_template
UNION ALL SELECT '自定义指令', count(*) FROM custom_instruction
ORDER BY 1;

DO $$ BEGIN RAISE NOTICE '========== OntoGraph 初始化完成! =========='; END $$;
