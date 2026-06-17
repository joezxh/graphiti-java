-- ============================================================
-- OntoGraph 系统菜单完整初始化数据 (PostgreSQL 版本)
-- 版本: 2026-06-16
-- 说明: 基于前端 Sidebar.vue 硬编码菜单配置生成
-- 包含: 完整菜单层级 + 超级管理员全权限分配
-- ============================================================

-- ============================================================
-- 第一部分: 清理现有菜单数据（可选，用于重新初始化）
-- ============================================================
-- 如需重新初始化，请先执行以下语句：
-- DELETE FROM sys_role_menu WHERE role_id = (SELECT id FROM sys_role WHERE code = 'SUPER_ADMIN');
-- DELETE FROM sys_menu;

-- ============================================================
-- 第二部分: 插入完整菜单数据
-- ============================================================

-- 2.1 顶级目录菜单（type=1，parent_id=0）
INSERT INTO sys_menu (name, permission, url, parent_id, sort, type, icon, status, deleted) VALUES
-- 1. Dashboard
('nav.dashboard', 'dashboard:view', '/dashboard', 0, 0, 1, 'DashboardOutlined', 1, FALSE),

-- 2. 图谱管理（目录）
('nav.graphManagement', 'graph:manage', '/graph', 0, 1, 1, 'ShareAltOutlined', 1, FALSE),

-- 3. 数据管理（目录）
('nav.dataManagement', 'data:manage', '/data', 0, 2, 1, 'DatabaseOutlined', 1, FALSE),

-- 4. 工具（目录）
('nav.tools', 'tools:manage', '/tools', 0, 3, 1, 'ToolOutlined', 1, FALSE),

-- 5. 系统管理（目录）
('nav.systemManagement', 'system:manage', '/system', 0, 4, 1, 'SettingOutlined', 1, FALSE);

-- 2.2 图谱管理子菜单（type=2，parent_id=图谱管理的ID）
INSERT INTO sys_menu (name, permission, url, parent_id, sort, type, icon, status, deleted) VALUES
-- 图谱列表
('nav.graphList', 'graph:list', '/graph/list', 2, 1, 2, 'UnorderedListOutlined', 1, FALSE),

-- 图谱 IDE
('nav.graphIDE', 'graph:ide', '/graph/ide', 2, 2, 2, 'CodeOutlined', 1, FALSE),

-- 时序历史
('nav.temporalHistory', 'graph:temporal', '/graph/temporal', 2, 3, 2, 'HistoryOutlined', 1, FALSE),

-- 社区检测
('nav.communityDetection', 'data:communities', '/data/communities', 2, 4, 2, 'ClusterOutlined', 1, FALSE);

-- 2.3 数据管理子菜单（type=2，parent_id=数据管理的ID）
INSERT INTO sys_menu (name, permission, url, parent_id, sort, type, icon, status, deleted) VALUES
-- 类管理
('nav.classManagement', 'data:classes', '/data/classes', 3, 1, 2, 'AppstoreOutlined', 1, FALSE),

-- 属性管理
('nav.propertyManagement', 'data:properties', '/data/properties', 3, 2, 2, 'TagOutlined', 1, FALSE),

-- 约束管理
('nav.constraintManagement', 'data:constraints', '/data/constraints', 3, 3, 2, 'SafetyCertificateOutlined', 1, FALSE),

-- 实体管理
('nav.entityManagement', 'data:entities', '/data/entities', 3, 4, 2, 'NodeIndexOutlined', 1, FALSE),

-- 边管理
('nav.edgeManagement', 'data:edges', '/data/edges', 3, 5, 2, 'LinkOutlined', 1, FALSE),

-- 社区Episode管理
('nav.communityEpisodeManagement', 'data:community-episode', '/data/community-episode', 3, 6, 2, 'ClusterOutlined', 1, FALSE),

-- Episode管理
('nav.episodeManagement', 'data:episodes', '/data/episodes', 3, 7, 2, 'FileTextOutlined', 1, FALSE),

-- 数据导入
('nav.dataImport', 'data:import', '/data/import', 3, 8, 2, 'ImportOutlined', 1, FALSE),

-- 数据导出
('nav.dataExport', 'data:export', '/data/export', 3, 9, 2, 'ExportOutlined', 1, FALSE),

-- 法律知识图谱
('nav.legalKnowledgeGraph', 'legal-kg:view', '/legal-kg', 3, 10, 2, 'AuditOutlined', 1, FALSE);

-- 2.4 工具子菜单（type=2，parent_id=工具的ID）
INSERT INTO sys_menu (name, permission, url, parent_id, sort, type, icon, status, deleted) VALUES
-- 混合搜索
('nav.hybridSearch', 'tools:search', '/search', 4, 1, 2, 'SearchOutlined', 1, FALSE),

-- 自定义指令
('nav.customInstructions', 'tools:custom-instructions', '/custom-instructions', 4, 2, 2, 'EditOutlined', 1, FALSE),

-- 提示词管理
('nav.promptManagement', 'tools:prompt', '/prompt', 4, 3, 2, 'MessageOutlined', 1, FALSE);

-- 2.5 系统管理子菜单（type=2，parent_id=系统管理的ID）
INSERT INTO sys_menu (name, permission, url, parent_id, sort, type, icon, status, deleted) VALUES
-- 用户管理
('nav.userManagement', 'system:user:list', '/system/user', 5, 1, 2, 'UserOutlined', 1, FALSE),

-- 角色管理
('nav.roleManagement', 'system:role:list', '/system/role', 5, 2, 2, 'TeamOutlined', 1, FALSE),

-- 菜单管理
('nav.menuManagement', 'system:menu:list', '/system/menu', 5, 3, 2, 'MenuOutlined', 1, FALSE),

-- 系统配置
('nav.systemConfig', 'system:config', '/system/config', 5, 4, 2, 'ToolOutlined', 1, FALSE),

-- 操作日志
('nav.operationLog', 'system:log', '/system/log', 5, 5, 2, 'FileTextOutlined', 1, FALSE),

-- 系统监控
('nav.systemMonitor', 'monitor:view', '/monitor', 5, 6, 2, 'MonitorOutlined', 1, FALSE);

-- ============================================================
-- 第三部分: 为超级管理员角色分配所有菜单权限
-- ============================================================

-- 获取超级管理员角色ID并分配所有菜单权限
DO $$
DECLARE
    super_admin_role_id BIGINT;
BEGIN
    -- 获取超级管理员角色ID
    SELECT id INTO super_admin_role_id 
    FROM sys_role 
    WHERE code = 'SUPER_ADMIN' 
    LIMIT 1;
    
    -- 分配所有菜单权限给超级管理员
    INSERT INTO sys_role_menu (role_id, menu_id)
    SELECT 
        super_admin_role_id,
        id
    FROM sys_menu
    WHERE deleted = FALSE;
    
    RAISE NOTICE '超级管理员菜单权限分配完成';
END $$;

-- ============================================================
-- 第四部分: 验证数据
-- ============================================================

-- 统计菜单数量（按类型）
SELECT 
    '目录菜单' AS menu_type, 
    COUNT(*) AS count 
FROM sys_menu 
WHERE type = 1 AND deleted = FALSE
UNION ALL
SELECT 
    '功能菜单' AS menu_type, 
    COUNT(*) AS count 
FROM sys_menu 
WHERE type = 2 AND deleted = FALSE
UNION ALL
SELECT 
    '总菜单数' AS menu_type, 
    COUNT(*) AS count 
FROM sys_menu 
WHERE deleted = FALSE;

-- 查看菜单层级结构
SELECT 
    m.id,
    m.name,
    m.permission,
    m.url,
    m.type,
    m.icon,
    m.sort,
    p.name AS parent_name
FROM sys_menu m
LEFT JOIN sys_menu p ON m.parent_id = p.id
WHERE m.deleted = FALSE
ORDER BY m.parent_id, m.sort;

-- 验证超级管理员权限数量
SELECT 
    '超级管理员菜单权限' AS description,
    COUNT(*) AS count
FROM sys_role_menu rm
INNER JOIN sys_role r ON rm.role_id = r.id
WHERE r.code = 'SUPER_ADMIN';

-- ============================================================
-- 初始化完成
-- ============================================================
