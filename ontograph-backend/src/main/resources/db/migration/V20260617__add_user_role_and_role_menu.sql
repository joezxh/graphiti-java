-- V20260617__add_role_menu_and_user_role.sql
-- Create sys_role_menu table for role-menu association
-- Note: sys_user_role table should already exist from initial schema

-- Create sys_role_menu table if not exists
CREATE TABLE IF NOT EXISTS sys_role_menu (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    role_id BIGINT NOT NULL COMMENT '角色ID',
    menu_id BIGINT NOT NULL COMMENT '菜单ID',
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_role_id (role_id),
    INDEX idx_menu_id (menu_id),
    UNIQUE KEY uk_role_menu (role_id, menu_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

-- Initialize super_admin role permissions (grant all menus to super_admin)
-- Assuming super_admin role_id = 1
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, m.id
FROM sys_menu m
WHERE m.deleted = false AND m.status = 1
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_menu rm WHERE rm.role_id = 1 AND rm.menu_id = m.id
  );

-- Ensure admin user is associated with super_admin role in sys_user_role
-- Assuming admin user_id = 1 and super_admin role_id = 1
INSERT INTO sys_user_role (user_id, role_id)
SELECT 1, 1
WHERE NOT EXISTS (
    SELECT 1 FROM sys_user_role WHERE user_id = 1 AND role_id = 1
);
