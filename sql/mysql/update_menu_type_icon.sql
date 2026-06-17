-- ============================================================
-- 更新菜单数据：添加 type 和 icon 字段
-- 执行时机：在数据库已初始化但菜单数据缺少 type 和 icon 时执行
-- ============================================================

-- MySQL 版本
-- 更新目录类型菜单
UPDATE sys_menu SET type = 1, icon = 'SettingOutlined' WHERE name = '系统管理' AND parent_id = 0;
UPDATE sys_menu SET type = 1, icon = 'ShareAltOutlined' WHERE name = '图谱管理' AND parent_id = 0;
UPDATE sys_menu SET type = 1, icon = 'ApartmentOutlined' WHERE name = '本体管理' AND parent_id = 0;
UPDATE sys_menu SET type = 1, icon = 'MessageOutlined' WHERE name = '提示词管理' AND parent_id = 0;

-- 更新菜单类型菜单
UPDATE sys_menu SET type = 2, icon = 'UserOutlined' WHERE name = '用户管理';
UPDATE sys_menu SET type = 2, icon = 'TeamOutlined' WHERE name = '角色管理';
UPDATE sys_menu SET type = 2, icon = 'MenuOutlined' WHERE name = '菜单管理';
UPDATE sys_menu SET type = 2, icon = 'UnorderedListOutlined' WHERE name = '图谱列表';
UPDATE sys_menu SET type = 2, icon = 'BookOutlined' WHERE name = '本体定义';
UPDATE sys_menu SET type = 2, icon = 'FileTextOutlined' WHERE name = '提示词模板';

-- 验证更新结果
SELECT id, name, type, icon, parent_id, sort FROM sys_menu ORDER BY sort;
