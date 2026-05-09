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
    phone VARCHAR(20),
    status SMALLINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT DEFAULT 0
);

-- 创建更新时间触发器函数
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 为 sys_user 表创建更新时间触发器
CREATE TRIGGER update_sys_user_updated_at
    BEFORE UPDATE ON sys_user
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- 角色表
CREATE TABLE sys_role (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    status SMALLINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT DEFAULT 0
);

-- 为 sys_role 表创建更新时间触发器
CREATE TRIGGER update_sys_role_updated_at
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
    sort SMALLINT DEFAULT 0,
    status SMALLINT NOT NULL DEFAULT 1,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    deleted SMALLINT DEFAULT 0
);

-- 为 sys_menu 表创建更新时间触发器
CREATE TRIGGER update_sys_menu_updated_at
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

-- 创建索引
CREATE INDEX idx_graphiti_graph_metadata_graph_id ON graphiti_graph_metadata(graph_id);
CREATE INDEX idx_graphiti_graph_metadata_deleted ON graphiti_graph_metadata(deleted);

-- 添加注释
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

-- 创建索引
CREATE INDEX idx_graphiti_ontology_graph_id ON graphiti_ontology(graph_id);
CREATE INDEX idx_graphiti_ontology_deleted ON graphiti_ontology(deleted);

-- 添加注释
COMMENT ON TABLE graphiti_ontology IS '本体定义表';
COMMENT ON COLUMN graphiti_ontology.id IS '主键ID';
COMMENT ON COLUMN graphiti_ontology.graph_id IS '图谱ID';
COMMENT ON COLUMN graphiti_ontology.entities IS '实体类型定义（JSON 数组）';
COMMENT ON COLUMN graphiti_ontology.edges IS '关系类型定义（JSON 数组）';
COMMENT ON COLUMN graphiti_ontology.is_default IS '是否默认本体';
COMMENT ON COLUMN graphiti_ontology.create_time IS '创建时间';
COMMENT ON COLUMN graphiti_ontology.update_time IS '更新时间';
COMMENT ON COLUMN graphiti_ontology.deleted IS '删除标志';


-- 为 graphiti_graph_metadata 表创建更新时间触发器
CREATE TRIGGER update_graph_metadata_updated_at
    BEFORE UPDATE ON graphiti_graph_metadata
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- 为 graphiti_ontology 表创建更新时间触发器
CREATE TRIGGER update_graph_ontology_updated_at
    BEFORE UPDATE ON graphiti_ontology
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- 索引创建
-- ============================================================

-- sys_user 表索引
CREATE INDEX idx_sys_user_username ON sys_user(username);
CREATE INDEX idx_sys_user_status ON sys_user(status);

-- sys_role 表索引
CREATE INDEX idx_sys_role_code ON sys_role(code);

-- sys_menu 表索引
CREATE INDEX idx_sys_menu_parent_id ON sys_menu(parent_id);

-- graphiti_graph_metadata 表索引
CREATE INDEX idx_graph_metadata_name ON graphiti_graph_metadata(name);
CREATE INDEX idx_graph_metadata_status ON graphiti_graph_metadata(status);

-- graphiti_ontology 表索引
CREATE INDEX idx_graph_ontology_graph_id ON graphiti_ontology(graph_id);
CREATE INDEX idx_graph_ontology_name ON graphiti_ontology(name);

-- ============================================================
-- 注释（PostgreSQL 风格）
-- ============================================================

COMMENT ON TABLE sys_user IS '系统用户表';
COMMENT ON COLUMN sys_user.id IS '用户ID';
COMMENT ON COLUMN sys_user.username IS '用户名';
COMMENT ON COLUMN sys_user.password IS '密码（加密存储）';
COMMENT ON COLUMN sys_user.nickname IS '昵称';
COMMENT ON COLUMN sys_user.email IS '邮箱';
COMMENT ON COLUMN sys_user.phone IS '手机号';
COMMENT ON COLUMN sys_user.status IS '状态（0-禁用，1-启用）';
COMMENT ON COLUMN sys_user.created_at IS '创建时间';
COMMENT ON COLUMN sys_user.updated_at IS '更新时间';
COMMENT ON COLUMN sys_user.deleted IS '逻辑删除标记';

COMMENT ON TABLE sys_role IS '系统角色表';
COMMENT ON COLUMN sys_role.id IS '角色ID';
COMMENT ON COLUMN sys_role.name IS '角色名称';
COMMENT ON COLUMN sys_role.code IS '角色编码';
COMMENT ON COLUMN sys_role.status IS '状态（0-禁用，1-启用）';

COMMENT ON TABLE sys_menu IS '系统菜单表';
COMMENT ON COLUMN sys_menu.id IS '菜单ID';
COMMENT ON COLUMN sys_menu.name IS '菜单名称';
COMMENT ON COLUMN sys_menu.permission IS '权限标识';
COMMENT ON COLUMN sys_menu.url IS '菜单URL';
COMMENT ON COLUMN sys_menu.parent_id IS '父菜单ID';

COMMENT ON TABLE graphiti_graph_metadata IS '图谱元数据表';
COMMENT ON COLUMN graphiti_graph_metadata.id IS '图谱ID';
COMMENT ON COLUMN graphiti_graph_metadata.name IS '图谱名称';
COMMENT ON COLUMN graphiti_graph_metadata.description IS '图谱描述';
COMMENT ON COLUMN graphiti_graph_metadata.config IS '图谱配置（JSON格式）';
COMMENT ON COLUMN graphiti_graph_metadata.status IS '状态（0-禁用，1-启用）';

COMMENT ON TABLE graphiti_ontology IS '本体定义表';
COMMENT ON COLUMN graphiti_ontology.id IS '本体ID';
COMMENT ON COLUMN graphiti_ontology.graph_id IS '所属图谱ID';
COMMENT ON COLUMN graphiti_ontology.name IS '本体名称';
COMMENT ON COLUMN graphiti_ontology.definition IS '本体定义（JSON格式）';
COMMENT ON COLUMN graphiti_ontology.status IS '状态（0-禁用，1-启用）';
