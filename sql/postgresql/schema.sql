-- Graphiti 数据库 Schema (PostgreSQL 版本)
-- 创建时间: 2026-05-08
-- 说明: 从 MySQL 迁移到 PostgreSQL

-- 设置客户端编码
SET client_encoding = 'UTF8';

-- 如果表已存在则删除（注意顺序：先删子表，再删主表）
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_role_menu;
DROP TABLE IF EXISTS sys_user;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_menu;
DROP TABLE IF EXISTS graphiti_graph_metadata;
DROP TABLE IF EXISTS graphiti_ontology;

-- ============================================================
-- 系统管理模块表
-- ============================================================

-- 用户表
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

-- 创建更新时间触发器函数
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.update_time = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 为 sys_user 表创建更新时间触发器
CREATE TRIGGER update_sys_user_update_time
    BEFORE UPDATE ON sys_user
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- 角色表
CREATE TABLE sys_role (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    status SMALLINT NOT NULL DEFAULT 1,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- 为 sys_role 表创建更新时间触发器
CREATE TRIGGER update_sys_role_update_time
    BEFORE UPDATE ON sys_role
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- 用户角色关联表
CREATE TABLE sys_user_role (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user(id) ON DELETE CASCADE,
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE
);

-- 菜单表
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

-- 为 sys_menu 表创建更新时间触发器
CREATE TRIGGER update_sys_menu_update_time
    BEFORE UPDATE ON sys_menu
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- 角色菜单关联表
CREATE TABLE sys_role_menu (
    id BIGSERIAL PRIMARY KEY,
    role_id BIGINT NOT NULL,
    menu_id BIGINT NOT NULL,
    CONSTRAINT fk_role_menu_role FOREIGN KEY (role_id) REFERENCES sys_role(id) ON DELETE CASCADE,
    CONSTRAINT fk_role_menu_menu FOREIGN KEY (menu_id) REFERENCES sys_menu(id) ON DELETE CASCADE
);

-- ============================================================
-- 图谱管理模块表
-- ============================================================

-- 图谱元数据表
CREATE TABLE graphiti_graph_metadata (
    id BIGSERIAL PRIMARY KEY,
    graph_id VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    node_count INTEGER DEFAULT 0,
    edge_count INTEGER DEFAULT 0,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- 为 graphiti_graph_metadata 表创建更新时间触发器
CREATE TRIGGER update_graph_metadata_update_time
    BEFORE UPDATE ON graphiti_graph_metadata
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- 本体定义表
CREATE TABLE graphiti_ontology (
    id BIGSERIAL PRIMARY KEY,
    graph_id VARCHAR(64) NOT NULL,
    entities TEXT,
    edges TEXT,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    create_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- 为 graphiti_ontology 表创建更新时间触发器
CREATE TRIGGER update_graph_ontology_update_time
    BEFORE UPDATE ON graphiti_ontology
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- 索引创建
-- ============================================================

-- sys_user 表索引
CREATE UNIQUE INDEX uk_sys_user_username ON sys_user(username);
CREATE INDEX idx_sys_user_status ON sys_user(status);
CREATE INDEX idx_sys_user_deleted ON sys_user(deleted);

-- sys_role 表索引
CREATE UNIQUE INDEX uk_sys_role_code ON sys_role(code);
CREATE INDEX idx_sys_role_deleted ON sys_role(deleted);

-- sys_menu 表索引
CREATE INDEX idx_sys_menu_parent_id ON sys_menu(parent_id);
CREATE INDEX idx_sys_menu_deleted ON sys_menu(deleted);

-- sys_user_role 索引
CREATE INDEX idx_sys_user_role_user_id ON sys_user_role(user_id);
CREATE INDEX idx_sys_user_role_role_id ON sys_user_role(role_id);

-- sys_role_menu 索引
CREATE INDEX idx_sys_role_menu_role_id ON sys_role_menu(role_id);
CREATE INDEX idx_sys_role_menu_menu_id ON sys_role_menu(menu_id);

-- graphiti_graph_metadata 表索引
CREATE UNIQUE INDEX uk_graphiti_graph_metadata_graph_id ON graphiti_graph_metadata(graph_id);
CREATE INDEX idx_graphiti_graph_metadata_deleted ON graphiti_graph_metadata(deleted);

-- graphiti_ontology 表索引
CREATE INDEX idx_graphiti_ontology_graph_id ON graphiti_ontology(graph_id);
CREATE INDEX idx_graphiti_ontology_deleted ON graphiti_ontology(deleted);

-- ============================================================
-- 注释（PostgreSQL 风格）
-- ============================================================

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

COMMENT ON TABLE sys_role IS '系统角色表';
COMMENT ON COLUMN sys_role.id IS '角色ID';
COMMENT ON COLUMN sys_role.name IS '角色名称';
COMMENT ON COLUMN sys_role.code IS '角色编码';
COMMENT ON COLUMN sys_role.status IS '状态（0-禁用，1-启用）';
COMMENT ON COLUMN sys_role.create_time IS '创建时间';
COMMENT ON COLUMN sys_role.update_time IS '更新时间';
COMMENT ON COLUMN sys_role.deleted IS '逻辑删除标记';

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

COMMENT ON TABLE graphiti_graph_metadata IS '图谱元数据表';
COMMENT ON COLUMN graphiti_graph_metadata.id IS '主键ID';
COMMENT ON COLUMN graphiti_graph_metadata.graph_id IS '图谱ID（UUID）';
COMMENT ON COLUMN graphiti_graph_metadata.name IS '图谱名称';
COMMENT ON COLUMN graphiti_graph_metadata.description IS '图谱描述';
COMMENT ON COLUMN graphiti_graph_metadata.node_count IS '节点数量';
COMMENT ON COLUMN graphiti_graph_metadata.edge_count IS '边数量';
COMMENT ON COLUMN graphiti_graph_metadata.create_time IS '创建时间';
COMMENT ON COLUMN graphiti_graph_metadata.update_time IS '更新时间';
COMMENT ON COLUMN graphiti_graph_metadata.deleted IS '删除标志';

COMMENT ON TABLE graphiti_ontology IS '本体定义表';
COMMENT ON COLUMN graphiti_ontology.id IS '本体ID';
COMMENT ON COLUMN graphiti_ontology.graph_id IS '所属图谱ID';
COMMENT ON COLUMN graphiti_ontology.entities IS '实体类型定义（JSON 数组）';
COMMENT ON COLUMN graphiti_ontology.edges IS '关系类型定义（JSON 数组）';
COMMENT ON COLUMN graphiti_ontology.is_default IS '是否默认本体';
COMMENT ON COLUMN graphiti_ontology.create_time IS '创建时间';
COMMENT ON COLUMN graphiti_ontology.update_time IS '更新时间';
COMMENT ON COLUMN graphiti_ontology.deleted IS '删除标志';

-- ============================================================
-- 新增表：系统操作日志、搜索历史、系统配置
-- ============================================================

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
CREATE INDEX idx_sys_operation_log_username ON sys_operation_log(username);
CREATE INDEX idx_sys_operation_log_operation ON sys_operation_log(operation);
CREATE INDEX idx_sys_operation_log_create_time ON sys_operation_log(create_time);
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
CREATE INDEX idx_sys_system_config_group_name ON sys_system_config(group_name);
CREATE INDEX idx_sys_system_config_deleted ON sys_system_config(deleted);
COMMENT ON TABLE sys_system_config IS '系统配置表';
COMMENT ON COLUMN sys_system_config.config_key IS '配置键';
COMMENT ON COLUMN sys_system_config.config_value IS '配置值';
COMMENT ON COLUMN sys_system_config.config_name IS '配置名称';
COMMENT ON COLUMN sys_system_config.config_description IS '配置描述';
COMMENT ON COLUMN sys_system_config.config_type IS '1-文本 2-数字 3-布尔 4-JSON';
COMMENT ON COLUMN sys_system_config.group_name IS '分组名称';
COMMENT ON COLUMN sys_system_config.sort_num IS '排序';
COMMENT ON COLUMN sys_system_config.status IS '0-禁用 1-启用';

-- 搜索历史表
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
COMMENT ON COLUMN sys_search_history.user_id IS '用户ID';
COMMENT ON COLUMN sys_search_history.query IS '搜索词';
COMMENT ON COLUMN sys_search_history.mode IS '搜索模式';
COMMENT ON COLUMN sys_search_history.result_count IS '结果数量';
COMMENT ON COLUMN sys_search_history.create_time IS '创建时间';
