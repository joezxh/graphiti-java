-- V20260616__add_menu_type_component_icon.sql
-- Add menu category field (1-directory 2-menu 3-button) and component/icon columns to sys_menu.
-- Reason: enable tree structure rendering (parent/child menus) and proper menu category display.

ALTER TABLE sys_menu
    ADD COLUMN IF NOT EXISTS type SMALLINT NOT NULL DEFAULT 2,
    ADD COLUMN IF NOT EXISTS component VARCHAR(200),
    ADD COLUMN IF NOT EXISTS icon VARCHAR(100);

COMMENT ON COLUMN sys_menu.type IS '菜单类型（1-目录 2-菜单 3-按钮）';
COMMENT ON COLUMN sys_menu.component IS '前端组件路径';
COMMENT ON COLUMN sys_menu.icon IS '菜单图标';

-- Backfill: treat existing entries as 'menu' (type=2).
-- 提示词模板, 提示词管理, 图谱列表, 图谱管理, 本体定义, 本体管理, 系统管理, 用户管理,
-- 角色管理, 菜单管理 等都标记为 type=2。
UPDATE sys_menu SET type = 2 WHERE type IS NULL OR type = 0;

-- Seed a few sub-menu examples so the tree structure is visible:
-- 1) 在「系统管理」(id=1) 下加 3 个子菜单
INSERT INTO sys_menu (name, permission, url, parent_id, sort, status, type, icon)
SELECT '系统监控', 'system:monitor:list', '/system/monitor', 1, 1, 1, 2, 'MonitorOutlined'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'system:monitor:list');

INSERT INTO sys_menu (name, permission, url, parent_id, sort, status, type, icon)
SELECT '操作日志', 'system:log:list', '/system/log', 1, 2, 1, 2, 'FileTextOutlined'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'system:log:list');

INSERT INTO sys_menu (name, permission, url, parent_id, sort, status, type, icon)
SELECT '系统配置', 'system:config:list', '/system/config', 1, 3, 1, 2, 'SettingOutlined'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'system:config:list');

-- 2) 在「图谱管理」(id=5) 下加 2 个子菜单
INSERT INTO sys_menu (name, permission, url, parent_id, sort, status, type, icon)
SELECT '图谱导入', 'graph:import', '/graph/import', 5, 1, 1, 2, 'ImportOutlined'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'graph:import');

INSERT INTO sys_menu (name, permission, url, parent_id, sort, status, type, icon)
SELECT '图谱导出', 'graph:export', '/graph/export', 5, 2, 1, 2, 'ExportOutlined'
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE permission = 'graph:export');

-- Grant super_admin access to the new sub-menus
DO $$
DECLARE
    super_admin_role_id BIGINT;
BEGIN
    SELECT id INTO super_admin_role_id FROM sys_role WHERE code = 'super_admin' LIMIT 1;
    IF super_admin_role_id IS NOT NULL THEN
        INSERT INTO sys_role_menu (role_id, menu_id)
        SELECT super_admin_role_id, m.id
        FROM sys_menu m
        WHERE m.permission IN (
                'system:monitor:list', 'system:log:list', 'system:config:list',
                'graph:import', 'graph:export'
              )
          AND NOT EXISTS (
                SELECT 1 FROM sys_role_menu rm
                WHERE rm.role_id = super_admin_role_id AND rm.menu_id = m.id
          );
    END IF;
END $$;
