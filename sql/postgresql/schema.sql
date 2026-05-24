-- ============================================================
-- Graphiti 数据库 Schema (PostgreSQL 版本)
-- 版本: 2026-05-18
-- 说明: 完整的23张表结构定义，与当前DO类实现一致
-- ============================================================

-- 设置客户端编码
SET client_encoding = 'UTF8';

-- ============================================================
-- 第一步：删除已存在的表（严格按外键依赖倒序）
-- ============================================================

-- 子表（有关联外键的表）
DROP TABLE IF EXISTS sys_role_menu;
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_user_notification_settings;
DROP TABLE IF EXISTS sys_notification;
DROP TABLE IF EXISTS sys_search_history;
DROP TABLE IF EXISTS sys_operation_log;
DROP TABLE IF EXISTS sys_system_config;
DROP TABLE IF EXISTS ont_mapping;
DROP TABLE IF EXISTS ont_class_inheritance;
DROP TABLE IF EXISTS ont_version_history;
DROP TABLE IF EXISTS ont_constraint;
DROP TABLE IF EXISTS ont_property;
DROP TABLE IF EXISTS ont_class;
DROP TABLE IF EXISTS ont_definition;
DROP TABLE IF EXISTS ont_draft;
DROP TABLE IF EXISTS prompt_version;
DROP TABLE IF EXISTS prompt_variable;
DROP TABLE IF EXISTS prompt_template;
DROP TABLE IF EXISTS custom_instruction;

-- 主表（无外键或仅被引用的表）
DROP TABLE IF EXISTS graphiti_graph_metadata;
DROP TABLE IF EXISTS sys_menu;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_user;

-- 删除触发器函数（如果存在）
DROP FUNCTION IF EXISTS update_updated_at_column();


-- ============================================================
-- 第二步：创建公共触发器函数
-- ============================================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.update_time = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;


-- ============================================================
-- 第三步：系统管理模块表
-- ============================================================

-- 系统用户表
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

CREATE TRIGGER update_sys_user_update_time
    BEFORE UPDATE ON sys_user
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE sys_user IS '系统用户表';
COMMENT ON COLUMN sys_user.id IS '用户ID';
COMMENT ON COLUMN sys_user.username IS '用户名';
COMMENT ON COLUMN sys_user.password IS '密码（加密存储）';
COMMENT ON COLUMN sys_user.nickname IS '昵称';
COMMENT ON COLUMN sys_user.email IS '邮箱';
COMMENT ON COLUMN sys_user.mobile IS '手机号';
COMMENT ON COLUMN sys_user.status IS '状态（0-禁用，1-启用）';
COMMENT ON COLUMN sys_user.create_time IS '创建时间';
COMMENT ON COLUMN sys_user.update_time IS '更新时间';
COMMENT ON COLUMN sys_user.deleted IS '逻辑删除标记';

CREATE INDEX idx_sys_user_status ON sys_user(status);
CREATE INDEX idx_sys_user_deleted ON sys_user(deleted);


-- 系统角色表
CREATE TABLE sys_role (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    status SMALLINT NOT NULL DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TRIGGER update_sys_role_update_time
    BEFORE UPDATE ON sys_role
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE sys_role IS '系统角色表';
COMMENT ON COLUMN sys_role.id IS '角色ID';
COMMENT ON COLUMN sys_role.name IS '角色名称';
COMMENT ON COLUMN sys_role.code IS '角色编码';
COMMENT ON COLUMN sys_role.status IS '状态（0-禁用，1-启用）';
COMMENT ON COLUMN sys_role.create_time IS '创建时间';
COMMENT ON COLUMN sys_role.update_time IS '更新时间';
COMMENT ON COLUMN sys_role.deleted IS '逻辑删除标记';

CREATE INDEX idx_sys_role_deleted ON sys_role(deleted);


-- 用户角色关联表
CREATE TABLE sys_user_role (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE
);

COMMENT ON TABLE sys_user_role IS '用户角色关联表';
COMMENT ON COLUMN sys_user_role.id IS '关联ID';
COMMENT ON COLUMN sys_user_role.user_id IS '用户ID';
COMMENT ON COLUMN sys_user_role.role_id IS '角色ID';

CREATE INDEX idx_sys_user_role_user_id ON sys_user_role(user_id);
CREATE INDEX idx_sys_user_role_role_id ON sys_user_role(role_id);


-- 系统菜单表
CREATE TABLE sys_menu (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    permission VARCHAR(100),
    url VARCHAR(200),
    parent_id BIGINT DEFAULT 0,
    sort INT DEFAULT 0,
    status SMALLINT NOT NULL DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TRIGGER update_sys_menu_update_time
    BEFORE UPDATE ON sys_menu
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE sys_menu IS '系统菜单表';
COMMENT ON COLUMN sys_menu.id IS '菜单ID';
COMMENT ON COLUMN sys_menu.name IS '菜单名称';
COMMENT ON COLUMN sys_menu.permission IS '权限标识';
COMMENT ON COLUMN sys_menu.url IS '菜单URL';
COMMENT ON COLUMN sys_menu.parent_id IS '父菜单ID';
COMMENT ON COLUMN sys_menu.sort IS '排序';
COMMENT ON COLUMN sys_menu.status IS '状态（0-禁用，1-启用）';
COMMENT ON COLUMN sys_menu.create_time IS '创建时间';
COMMENT ON COLUMN sys_menu.update_time IS '更新时间';
COMMENT ON COLUMN sys_menu.deleted IS '逻辑删除标记';

CREATE INDEX idx_sys_menu_parent_id ON sys_menu(parent_id);
CREATE INDEX idx_sys_menu_deleted ON sys_menu(deleted);


-- 角色菜单关联表
CREATE TABLE sys_role_menu (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    CONSTRAINT fk_role_menu_role FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_menu_menu FOREIGN KEY (menu_id) REFERENCES sys_menu(id) ON DELETE CASCADE
);

COMMENT ON TABLE sys_role_menu IS '角色菜单关联表';
COMMENT ON COLUMN sys_role_menu.id IS '关联ID';
COMMENT ON COLUMN sys_role_menu.role_id IS '角色ID';
COMMENT ON COLUMN sys_role_menu.menu_id IS '菜单ID';

CREATE INDEX idx_sys_role_menu_role_id ON sys_role_menu(role_id);
CREATE INDEX idx_sys_role_menu_menu_id ON sys_role_menu(menu_id);


-- 系统操作日志表
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

COMMENT ON TABLE sys_operation_log IS '系统操作日志表';
COMMENT ON COLUMN sys_operation_log.user_id IS '用户ID';
COMMENT ON COLUMN sys_operation_log.username IS '用户名';
COMMENT ON COLUMN sys_operation_log.operation IS '操作名称';
COMMENT ON COLUMN sys_operation_log.method IS '请求方法和路径';
COMMENT ON COLUMN sys_operation_log.params IS '请求参数(JSON)';
COMMENT ON COLUMN sys_operation_log.ip IS 'IP地址';
COMMENT ON COLUMN sys_operation_log.location IS '地理位置';
COMMENT ON COLUMN sys_operation_log.status IS '0-失败 1-成功';
COMMENT ON COLUMN sys_operation_log.error_msg IS '错误信息';
COMMENT ON COLUMN sys_operation_log.duration IS '耗时(毫秒)';
COMMENT ON COLUMN sys_operation_log.create_time IS '创建时间';

CREATE INDEX idx_sys_operation_log_username ON sys_operation_log(username);
CREATE INDEX idx_sys_operation_log_operation ON sys_operation_log(operation);
CREATE INDEX idx_sys_operation_log_create_time ON sys_operation_log(create_time);


-- 系统配置表
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

CREATE TRIGGER update_sys_system_config_update_time
    BEFORE UPDATE ON sys_system_config
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE sys_system_config IS '系统配置表';
COMMENT ON COLUMN sys_system_config.config_key IS '配置键';
COMMENT ON COLUMN sys_system_config.config_value IS '配置值';
COMMENT ON COLUMN sys_system_config.config_name IS '配置名称';
COMMENT ON COLUMN sys_system_config.config_description IS '配置描述';
COMMENT ON COLUMN sys_system_config.config_type IS '1-文本 2-数字 3-布尔 4-JSON';
COMMENT ON COLUMN sys_system_config.group_name IS '分组名称';
COMMENT ON COLUMN sys_system_config.sort_num IS '排序';
COMMENT ON COLUMN sys_system_config.status IS '0-禁用 1-启用';

CREATE INDEX idx_sys_system_config_group_name ON sys_system_config(group_name);
CREATE INDEX idx_sys_system_config_deleted ON sys_system_config(deleted);


-- 搜索历史表
CREATE TABLE sys_search_history (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    query VARCHAR(500) NOT NULL,
    mode VARCHAR(20),
    result_count INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_search_history IS '搜索历史记录表';
COMMENT ON COLUMN sys_search_history.user_id IS '用户ID';
COMMENT ON COLUMN sys_search_history.query IS '搜索词';
COMMENT ON COLUMN sys_search_history.mode IS '搜索模式';
COMMENT ON COLUMN sys_search_history.result_count IS '结果数量';
COMMENT ON COLUMN sys_search_history.create_time IS '创建时间';

CREATE INDEX idx_sys_search_history_user_id ON sys_search_history(user_id);
CREATE INDEX idx_sys_search_history_create_time ON sys_search_history(create_time);


-- 系统通知表
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

CREATE TRIGGER update_sys_notification_update_time
    BEFORE UPDATE ON sys_notification
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE sys_notification IS '系统通知表';
COMMENT ON COLUMN sys_notification.id IS '通知ID';
COMMENT ON COLUMN sys_notification.user_id IS '用户ID';
COMMENT ON COLUMN sys_notification.title IS '通知标题';
COMMENT ON COLUMN sys_notification.content IS '通知内容';
COMMENT ON COLUMN sys_notification.type IS '通知类型: 1-系统通知 2-图谱通知 3-检索通知';
COMMENT ON COLUMN sys_notification.is_read IS '已读状态: 0-未读 1-已读';
COMMENT ON COLUMN sys_notification.create_time IS '创建时间';
COMMENT ON COLUMN sys_notification.update_time IS '更新时间';
COMMENT ON COLUMN sys_notification.deleted IS '删除标志';

CREATE INDEX idx_sys_notification_user_id ON sys_notification(user_id);
CREATE INDEX idx_sys_notification_type ON sys_notification(type);
CREATE INDEX idx_sys_notification_is_read ON sys_notification(is_read);
CREATE INDEX idx_sys_notification_deleted ON sys_notification(deleted);
CREATE INDEX idx_sys_notification_create_time ON sys_notification(create_time DESC);


-- 用户通知设置表
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

CREATE TRIGGER update_sys_user_notification_settings_update_time
    BEFORE UPDATE ON sys_user_notification_settings
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE sys_user_notification_settings IS '用户通知设置表';
COMMENT ON COLUMN sys_user_notification_settings.id IS '设置ID';
COMMENT ON COLUMN sys_user_notification_settings.user_id IS '用户ID';
COMMENT ON COLUMN sys_user_notification_settings.system_enabled IS '系统通知开关: 0-关闭 1-开启';
COMMENT ON COLUMN sys_user_notification_settings.graph_enabled IS '图谱通知开关: 0-关闭 1-开启';
COMMENT ON COLUMN sys_user_notification_settings.search_enabled IS '检索通知开关: 0-关闭 1-开启';
COMMENT ON COLUMN sys_user_notification_settings.email_enabled IS '邮件通知开关: 0-关闭 1-开启';
COMMENT ON COLUMN sys_user_notification_settings.create_time IS '创建时间';
COMMENT ON COLUMN sys_user_notification_settings.update_time IS '更新时间';
COMMENT ON COLUMN sys_user_notification_settings.deleted IS '删除标志';

CREATE INDEX idx_sys_user_notification_settings_user_id ON sys_user_notification_settings(user_id);
CREATE INDEX idx_sys_user_notification_settings_deleted ON sys_user_notification_settings(deleted);


-- ============================================================
-- 第四步：图谱管理模块表
-- ============================================================

-- 图谱元数据表
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

CREATE TRIGGER update_graphiti_graph_metadata_update_time
    BEFORE UPDATE ON graphiti_graph_metadata
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE graphiti_graph_metadata IS '图谱元数据表';
COMMENT ON COLUMN graphiti_graph_metadata.id IS '主键ID';
COMMENT ON COLUMN graphiti_graph_metadata.graph_id IS '图谱ID（UUID）';
COMMENT ON COLUMN graphiti_graph_metadata.name IS '图谱名称';
COMMENT ON COLUMN graphiti_graph_metadata.description IS '图谱描述';
COMMENT ON COLUMN graphiti_graph_metadata.node_count IS '节点数量';
COMMENT ON COLUMN graphiti_graph_metadata.edge_count IS '边数量';
COMMENT ON COLUMN graphiti_graph_metadata.status IS '图谱状态';
COMMENT ON COLUMN graphiti_graph_metadata.create_time IS '创建时间';
COMMENT ON COLUMN graphiti_graph_metadata.update_time IS '更新时间';
COMMENT ON COLUMN graphiti_graph_metadata.deleted IS '删除标志';

CREATE UNIQUE INDEX uk_graphiti_graph_metadata_graph_id ON graphiti_graph_metadata(graph_id);
CREATE INDEX idx_graphiti_graph_metadata_deleted ON graphiti_graph_metadata(deleted);


-- ============================================================
-- 第五步：本体管理模块表
-- ============================================================

-- 本体定义表
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

COMMENT ON TABLE ont_definition IS '本体定义表 - 存储每个图谱的本体定义版本信息';
COMMENT ON COLUMN ont_definition.id IS '主键ID，自增序列';
COMMENT ON COLUMN ont_definition.graph_id IS '所属图谱的唯一标识符';
COMMENT ON COLUMN ont_definition.namespace IS '本体命名空间';
COMMENT ON COLUMN ont_definition.name IS '本体名称';
COMMENT ON COLUMN ont_definition.version IS '语义化版本号';
COMMENT ON COLUMN ont_definition.status IS '本体状态: DRAFT/ACTIVE/DEPRECATED/ARCHIVED';
COMMENT ON COLUMN ont_definition.description IS '本体的详细描述';
COMMENT ON COLUMN ont_definition.parent_version_id IS '父版本ID';
COMMENT ON COLUMN ont_definition.created_by IS '创建该版本的用户ID';
COMMENT ON COLUMN ont_definition.created_at IS '创建时间';
COMMENT ON COLUMN ont_definition.updated_at IS '更新时间';


-- 本体类表
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

COMMENT ON TABLE ont_class IS '本体类表 - 定义知识图谱中的实体类型';
COMMENT ON COLUMN ont_class.id IS '主键ID';
COMMENT ON COLUMN ont_class.definition_id IS '所属本体定义的ID';
COMMENT ON COLUMN ont_class.class_uri IS '类的完整URI';
COMMENT ON COLUMN ont_class.local_name IS '类的本地名称';
COMMENT ON COLUMN ont_class.name_en IS '类的英文名称';
COMMENT ON COLUMN ont_class.parent_class_id IS '父类ID';
COMMENT ON COLUMN ont_class.equivalent_to IS '等价类列表(JSON数组)';
COMMENT ON COLUMN ont_class.disjoint_with IS '互斥类列表(JSON数组)';
COMMENT ON COLUMN ont_class.description IS '类的详细描述';
COMMENT ON COLUMN ont_class.example IS '类的使用示例';
COMMENT ON COLUMN ont_class.domain_hint IS '领域分类标记';
COMMENT ON COLUMN ont_class.metadata IS '扩展元数据(JSON格式)';
COMMENT ON COLUMN ont_class.created_at IS '创建时间';
COMMENT ON COLUMN ont_class.updated_at IS '更新时间';


-- 本体类继承关系表（支持多继承）
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

COMMENT ON TABLE ont_class_inheritance IS '本体类继承关系表 - 支持多继承';
COMMENT ON COLUMN ont_class_inheritance.id IS '主键ID';
COMMENT ON COLUMN ont_class_inheritance.class_id IS '类ID';
COMMENT ON COLUMN ont_class_inheritance.parent_class_id IS '父类ID';
COMMENT ON COLUMN ont_class_inheritance.definition_id IS '所属本体定义ID';
COMMENT ON COLUMN ont_class_inheritance.distance IS '继承距离（1=直接父类）';
COMMENT ON COLUMN ont_class_inheritance.created_at IS '创建时间';


-- 本体属性表
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
CREATE INDEX idx_ont_prop_range ON ont_property(range_class_id);
CREATE INDEX idx_ont_prop_type ON ont_property(property_type);

COMMENT ON TABLE ont_property IS '本体属性表 - 定义类具有的属性';
COMMENT ON COLUMN ont_property.id IS '主键ID';
COMMENT ON COLUMN ont_property.definition_id IS '所属本体定义ID';
COMMENT ON COLUMN ont_property.property_uri IS '属性完整URI';
COMMENT ON COLUMN ont_property.local_name IS '属性本地名称';
COMMENT ON COLUMN ont_property.property_type IS '属性类型: OBJECT/DATATYPE/ANNOTATION/TRANSITIVE/SYMMETRIC/FUNCTIONAL';
COMMENT ON COLUMN ont_property.domain_class_id IS '定义域，属性所属的类';
COMMENT ON COLUMN ont_property.range_class_id IS '值域，OBJECT属性的目标类';
COMMENT ON COLUMN ont_property.range_data_type IS '值域数据类型: string/integer/float/boolean/date/datetime/json';
COMMENT ON COLUMN ont_property.min_cardinality IS '最小出现次数';
COMMENT ON COLUMN ont_property.max_cardinality IS '最大出现次数';
COMMENT ON COLUMN ont_property.default_value IS '默认值';
COMMENT ON COLUMN ont_property.allowed_values IS '允许值枚举(JSON数组)';
COMMENT ON COLUMN ont_property.parent_property_id IS '父属性ID';
COMMENT ON COLUMN ont_property.equivalent_to IS '等价属性列表';
COMMENT ON COLUMN ont_property.inverse_of_id IS '逆属性ID';
COMMENT ON COLUMN ont_property.is_required IS '是否必填';
COMMENT ON COLUMN ont_property.is_multiple IS '是否允许多值';
COMMENT ON COLUMN ont_property.pattern IS '正则表达式约束';
COMMENT ON COLUMN ont_property.min_value IS '数值类型最小值约束';
COMMENT ON COLUMN ont_property.max_value IS '数值类型最大值约束';
COMMENT ON COLUMN ont_property.description IS '属性详细说明';
COMMENT ON COLUMN ont_property.example IS '属性使用示例';
COMMENT ON COLUMN ont_property.metadata IS '扩展元数据(JSON格式)';
COMMENT ON COLUMN ont_property.created_at IS '创建时间';
COMMENT ON COLUMN ont_property.updated_at IS '更新时间';


-- 本体约束表
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
CREATE INDEX idx_ont_constraint_class ON ont_constraint(class_id);
CREATE INDEX idx_ont_constraint_property ON ont_constraint(property_id);
CREATE INDEX idx_ont_constraint_type ON ont_constraint(constraint_type);

COMMENT ON TABLE ont_constraint IS '本体约束表 - 定义复杂的数据验证规则';
COMMENT ON COLUMN ont_constraint.id IS '主键ID';
COMMENT ON COLUMN ont_constraint.definition_id IS '所属本体定义ID';
COMMENT ON COLUMN ont_constraint.class_id IS '约束应用的类ID';
COMMENT ON COLUMN ont_constraint.property_id IS '约束应用的属性ID';
COMMENT ON COLUMN ont_constraint.constraint_type IS '约束类型: CARDINALITY/PATTERN/RANGE/ENUM/NOT_NULL/CUSTOM_SPARQL/UNIQUE/LENGTH';
COMMENT ON COLUMN ont_constraint.value IS '约束值(JSON格式)';
COMMENT ON COLUMN ont_constraint.error_message IS '用户友好的错误提示';
COMMENT ON COLUMN ont_constraint.severity IS '严重级别: ERROR/WARNING/INFO';
COMMENT ON COLUMN ont_constraint.description IS '约束的业务说明';
COMMENT ON COLUMN ont_constraint.created_at IS '创建时间';
COMMENT ON COLUMN ont_constraint.updated_at IS '更新时间';


-- 本体版本历史表
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
CREATE INDEX idx_ont_version_type ON ont_version_history(change_type);
CREATE INDEX idx_ont_version_time ON ont_version_history(changed_at DESC);

COMMENT ON TABLE ont_version_history IS '本体版本历史表 - 记录所有本体变更操作';
COMMENT ON COLUMN ont_version_history.id IS '主键ID';
COMMENT ON COLUMN ont_version_history.definition_id IS '关联的本体定义ID';
COMMENT ON COLUMN ont_version_history.version IS '变更时的版本号';
COMMENT ON COLUMN ont_version_history.change_type IS '变更操作类型: CREATED/UPDATED/DELETED/ACTIVATED/DEPRECATED';
COMMENT ON COLUMN ont_version_history.entity_type IS '被修改实体的类型: CLASS/PROPERTY/CONSTRAINT/DEFINITION';
COMMENT ON COLUMN ont_version_history.entity_id IS '被修改实体的ID';
COMMENT ON COLUMN ont_version_history.before_state IS '变更前的完整状态JSON';
COMMENT ON COLUMN ont_version_history.after_state IS '变更后的完整状态JSON';
COMMENT ON COLUMN ont_version_history.diff_summary IS '变更摘要';
COMMENT ON COLUMN ont_version_history.changed_by IS '执行变更的用户ID';
COMMENT ON COLUMN ont_version_history.changed_at IS '变更时间';


-- 本体映射表
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
CREATE INDEX idx_ont_mapping_source ON ont_mapping(source_ontology);
CREATE INDEX idx_ont_mapping_confidence ON ont_mapping(confidence);

COMMENT ON TABLE ont_mapping IS '本体映射表 - 存储本体之间的对齐关系';
COMMENT ON COLUMN ont_mapping.id IS '主键ID';
COMMENT ON COLUMN ont_mapping.definition_id IS '所属本体定义ID';
COMMENT ON COLUMN ont_mapping.source_ontology IS '源本体URI';
COMMENT ON COLUMN ont_mapping.source_type IS '源本体中的类型名称';
COMMENT ON COLUMN ont_mapping.mapped_class_uri IS '映射到本地本体的类URI';
COMMENT ON COLUMN ont_mapping.mapping_type IS '映射类型: EQUIVALENT/SUB_CLASS/SUPER_CLASS/RELATED';
COMMENT ON COLUMN ont_mapping.confidence IS '映射置信度';
COMMENT ON COLUMN ont_mapping.metadata IS '扩展信息';
COMMENT ON COLUMN ont_mapping.created_at IS '创建时间';


-- 本体草稿表
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

CREATE TRIGGER update_ont_draft_update_time
    BEFORE UPDATE ON ont_draft
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE ont_draft IS '本体草稿表 - 存储LLM生成的本体定义草稿和模拟数据';
COMMENT ON COLUMN ont_draft.id IS '主键ID';
COMMENT ON COLUMN ont_draft.graph_id IS '所属图谱ID';
COMMENT ON COLUMN ont_draft.draft_name IS '草稿名称';
COMMENT ON COLUMN ont_draft.draft_type IS '草稿类型: DRAFT/OPTIMIZED/GENERATED';
COMMENT ON COLUMN ont_draft.source_info IS '原始业务信息(JSON)';
COMMENT ON COLUMN ont_draft.generated_info IS 'LLM生成的本体定义(JSON)';
COMMENT ON COLUMN ont_draft.mock_data IS '生成的模拟数据(JSON): 节点+边';
COMMENT ON COLUMN ont_draft.status IS '状态: PENDING/APPROVED/REJECTED/APPLIED';
COMMENT ON COLUMN ont_draft.created_by IS '创建人';
COMMENT ON COLUMN ont_draft.created_at IS '创建时间';
COMMENT ON COLUMN ont_draft.updated_at IS '更新时间';

CREATE INDEX idx_ont_draft_graph_id ON ont_draft(graph_id);
CREATE INDEX idx_ont_draft_status ON ont_draft(status);
CREATE INDEX idx_ont_draft_type ON ont_draft(draft_type);


-- ============================================================
-- 第六步：提示词管理模块表
-- ============================================================

-- 提示词模板表
CREATE TABLE prompt_template (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    description TEXT,
    type VARCHAR(32),
    system_prompt TEXT,
    user_prompt_template TEXT,
    response_format TEXT,
    enabled BOOLEAN DEFAULT TRUE,
    model VARCHAR(64),
    sort INT DEFAULT 0,
    tags TEXT,
    extra_config TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

CREATE TRIGGER update_prompt_template_update_time
    BEFORE UPDATE ON prompt_template
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE prompt_template IS '提示词模板表';
COMMENT ON COLUMN prompt_template.id IS '主键ID';
COMMENT ON COLUMN prompt_template.code IS '模板编码（唯一标识）';
COMMENT ON COLUMN prompt_template.name IS '模板名称';
COMMENT ON COLUMN prompt_template.description IS '模板描述';
COMMENT ON COLUMN prompt_template.type IS '模板类型: entity_extract/edge_extract/dedupe/summary';
COMMENT ON COLUMN prompt_template.system_prompt IS '系统提示词';
COMMENT ON COLUMN prompt_template.user_prompt_template IS '用户提示词模板';
COMMENT ON COLUMN prompt_template.response_format IS '响应格式定义';
COMMENT ON COLUMN prompt_template.enabled IS '是否启用';
COMMENT ON COLUMN prompt_template.model IS '所属模型';
COMMENT ON COLUMN prompt_template.sort IS '排序值';
COMMENT ON COLUMN prompt_template.tags IS '标签（JSON数组）';
COMMENT ON COLUMN prompt_template.extra_config IS '额外配置（JSON格式）';
COMMENT ON COLUMN prompt_template.created_at IS '创建时间';
COMMENT ON COLUMN prompt_template.updated_at IS '更新时间';
COMMENT ON COLUMN prompt_template.created_by IS '创建人ID';
COMMENT ON COLUMN prompt_template.updated_by IS '更新人ID';

CREATE INDEX idx_prompt_template_type ON prompt_template(type);
CREATE INDEX idx_prompt_template_enabled ON prompt_template(enabled);


-- 提示词变量表
CREATE TABLE prompt_variable (
    id BIGSERIAL PRIMARY KEY,
    template_id BIGINT NOT NULL,
    variable_name VARCHAR(64) NOT NULL,
    description TEXT,
    variable_type VARCHAR(32),
    required BOOLEAN DEFAULT TRUE,
    default_value VARCHAR(512),
    source VARCHAR(32),
    validation_rule VARCHAR(256),
    sort INT DEFAULT 0,
    remark TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_prompt_var_template FOREIGN KEY (template_id) REFERENCES prompt_template(id) ON DELETE CASCADE
);

CREATE TRIGGER update_prompt_variable_update_time
    BEFORE UPDATE ON prompt_variable
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE prompt_variable IS '提示词变量表';
COMMENT ON COLUMN prompt_variable.id IS '主键ID';
COMMENT ON COLUMN prompt_variable.template_id IS '所属模板ID';
COMMENT ON COLUMN prompt_variable.variable_name IS '变量名称';
COMMENT ON COLUMN prompt_variable.description IS '变量描述';
COMMENT ON COLUMN prompt_variable.variable_type IS '变量类型: string/list/json/text';
COMMENT ON COLUMN prompt_variable.required IS '是否必需';
COMMENT ON COLUMN prompt_variable.default_value IS '默认值';
COMMENT ON COLUMN prompt_variable.source IS '变量来源: context/static/llm';
COMMENT ON COLUMN prompt_variable.validation_rule IS '验证规则';
COMMENT ON COLUMN prompt_variable.sort IS '排序值';
COMMENT ON COLUMN prompt_variable.remark IS '备注';
COMMENT ON COLUMN prompt_variable.created_at IS '创建时间';
COMMENT ON COLUMN prompt_variable.updated_at IS '更新时间';

CREATE INDEX idx_prompt_var_template ON prompt_variable(template_id);


-- 提示词版本表
CREATE TABLE prompt_version (
    id BIGSERIAL PRIMARY KEY,
    template_id BIGINT NOT NULL,
    version INT NOT NULL,
    system_prompt TEXT,
    user_prompt_template TEXT,
    response_format TEXT,
    description TEXT,
    active BOOLEAN DEFAULT FALSE,
    created_by BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_prompt_ver_template FOREIGN KEY (template_id) REFERENCES prompt_template(id) ON DELETE CASCADE
);

COMMENT ON TABLE prompt_version IS '提示词版本表';
COMMENT ON COLUMN prompt_version.id IS '主键ID';
COMMENT ON COLUMN prompt_version.template_id IS '所属模板ID';
COMMENT ON COLUMN prompt_version.version IS '版本号';
COMMENT ON COLUMN prompt_version.system_prompt IS '系统提示词';
COMMENT ON COLUMN prompt_version.user_prompt_template IS '用户提示词模板';
COMMENT ON COLUMN prompt_version.response_format IS '响应格式';
COMMENT ON COLUMN prompt_version.description IS '版本描述';
COMMENT ON COLUMN prompt_version.active IS '是否为当前活跃版本';
COMMENT ON COLUMN prompt_version.created_by IS '创建人';
COMMENT ON COLUMN prompt_version.created_at IS '创建时间';

CREATE INDEX idx_prompt_ver_template ON prompt_version(template_id);
CREATE INDEX idx_prompt_ver_active ON prompt_version(active);


-- ============================================================
-- 第七步：自定义指令模块表
-- ============================================================

-- 自定义抽取指令表
CREATE TABLE custom_instruction (
    id BIGSERIAL PRIMARY KEY,
    graph_id VARCHAR(64),
    instruction TEXT NOT NULL,
    enabled BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TRIGGER update_custom_instruction_update_time
    BEFORE UPDATE ON custom_instruction
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

COMMENT ON TABLE custom_instruction IS '自定义抽取指令表';
COMMENT ON COLUMN custom_instruction.id IS '主键ID';
COMMENT ON COLUMN custom_instruction.graph_id IS '图谱ID（null表示全局指令）';
COMMENT ON COLUMN custom_instruction.instruction IS 'LLM抽取时的额外提示词';
COMMENT ON COLUMN custom_instruction.enabled IS '是否启用';
COMMENT ON COLUMN custom_instruction.created_at IS '创建时间';
COMMENT ON COLUMN custom_instruction.updated_at IS '更新时间';

CREATE INDEX idx_custom_instruction_graph_id ON custom_instruction(graph_id);
CREATE INDEX idx_custom_instruction_enabled ON custom_instruction(enabled);


-- ============================================================
-- 初始化完成
-- ============================================================
