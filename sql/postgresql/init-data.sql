-- Graphiti 初始化数据 (PostgreSQL 版本)
-- 创建时间: 2026-05-08
-- 说明: 从 MySQL 迁移到 PostgreSQL

-- ============================================================
-- 初始化系统角色
-- ============================================================

INSERT INTO sys_role (name, code, status) VALUES
('超级管理员', 'SUPER_ADMIN', 1),
('管理员', 'ADMIN', 1),
('普通用户', 'USER', 1);

-- ============================================================
-- 初始化系统用户（密码：admin123）
-- 注意：PostgreSQL 的 BIGSERIAL 会自动生成 ID
-- ============================================================

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

-- ============================================================
-- 初始化用户角色关联
-- ============================================================

-- 获取 admin 用户的 ID 和 SUPER_ADMIN 角色的 ID
DO $$
DECLARE
    admin_user_id BIGINT;
    super_admin_role_id BIGINT;
BEGIN
    SELECT id INTO admin_user_id FROM sys_user WHERE username = 'admin';
    SELECT id INTO super_admin_role_id FROM sys_role WHERE code = 'SUPER_ADMIN';
    
    INSERT INTO sys_user_role (user_id, role_id) 
    VALUES (admin_user_id, super_admin_role_id);
END $$;

-- ============================================================
-- 初始化系统菜单
-- ============================================================

INSERT INTO sys_menu (name, permission, url, parent_id, sort, status) VALUES
('系统管理', 'system:manage', '/system', 0, 1, 1),
('用户管理', 'system:user:list', '/system/user', 1, 1, 1),
('角色管理', 'system:role:list', '/system/role', 1, 2, 1),
('菜单管理', 'system:menu:list', '/system/menu', 1, 3, 1);

-- ============================================================
-- 初始化角色菜单关联
-- ============================================================

DO $$
DECLARE
    super_admin_role_id BIGINT;
    menu_id_1 BIGINT;
    menu_id_2 BIGINT;
    menu_id_3 BIGINT;
    menu_id_4 BIGINT;
BEGIN
    SELECT id INTO super_admin_role_id FROM sys_role WHERE code = 'SUPER_ADMIN';
    SELECT id INTO menu_id_1 FROM sys_menu WHERE name = '系统管理';
    SELECT id INTO menu_id_2 FROM sys_menu WHERE name = '用户管理';
    SELECT id INTO menu_id_3 FROM sys_menu WHERE name = '角色管理';
    SELECT id INTO menu_id_4 FROM sys_menu WHERE name = '菜单管理';
    
    INSERT INTO sys_role_menu (role_id, menu_id) VALUES
    (super_admin_role_id, menu_id_1),
    (super_admin_role_id, menu_id_2),
    (super_admin_role_id, menu_id_3),
    (super_admin_role_id, menu_id_4);
END $$;

-- ============================================================
-- 初始化示例图谱
-- ============================================================

INSERT INTO graphiti_graph_metadata (graph_id, name, description, node_count, edge_count, deleted)
VALUES (
    'example-graph',
    '示例图谱',
    '这是一个示例知识图谱',
    0,
    0,
    FALSE
);

-- 验证数据插入
SELECT 'Users' as table_name, count(*) as count FROM sys_user
UNION ALL
SELECT 'Roles', count(*) FROM sys_role
UNION ALL
SELECT 'Menus', count(*) FROM sys_menu
UNION ALL
SELECT 'UserRoles', count(*) FROM sys_user_role
UNION ALL
SELECT 'RoleMenus', count(*) FROM sys_role_menu
UNION ALL
SELECT 'Graphs', count(*) FROM graphiti_graph_metadata;
