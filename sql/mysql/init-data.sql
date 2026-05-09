-- Graphiti 知识图谱系统初始数据
-- 插入系统用户（密码：admin123，使用 BCrypt 加密）
-- BCrypt 加密后的密码（admin123）
INSERT INTO `sys_user` (`username`, `password`, `nickname`, `status`) 
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 
        '系统管理员', 1);

-- 插入系统角色
INSERT INTO `sys_role` (`name`, `code`, `status`) VALUES
('超级管理员', 'SUPER_ADMIN', 1),
('管理员', 'ADMIN', 1),
('普通用户', 'USER', 1);

-- 插入默认图谱（可选，用于测试）
-- INSERT INTO `graphiti_graph_metadata` (`graph_id`, `name`, `description`) 
-- VALUES ('default-graph', '默认图谱', '系统默认图谱');
