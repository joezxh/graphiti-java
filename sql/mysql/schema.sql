-- Graphiti 知识图谱系统数据库脚本
-- MySQL 8.0+
-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS graphiti 
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE graphiti;

-- ----------------------------
-- 表结构：系统用户
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` VARCHAR(64) NOT NULL COMMENT '用户名',
  `password` VARCHAR(128) NOT NULL COMMENT '密码（BCrypt加密）',
  `nickname` VARCHAR(64) DEFAULT NULL COMMENT '昵称',
  `email` VARCHAR(128) DEFAULT NULL COMMENT '邮箱',
  `mobile` VARCHAR(32) DEFAULT NULL COMMENT '手机号',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0=禁用，1=启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP 
                      ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` BIT(1) NOT NULL DEFAULT b'0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

-- ----------------------------
-- 表结构：系统角色
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` VARCHAR(64) NOT NULL COMMENT '角色名称',
  `code` VARCHAR(64) NOT NULL COMMENT '角色编码',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0=禁用，1=启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP 
                      ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` BIT(1) NOT NULL DEFAULT b'0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统角色表';

-- ----------------------------
-- 表结构：用户角色关联
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_user_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联表';

-- ----------------------------
-- 表结构：系统菜单
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_menu` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` VARCHAR(64) NOT NULL COMMENT '菜单名称',
  `permission` VARCHAR(128) DEFAULT NULL COMMENT '权限标识',
  `url` VARCHAR(255) DEFAULT NULL COMMENT '菜单URL',
  `parent_id` BIGINT DEFAULT 0 COMMENT '父菜单ID',
  `sort` INT DEFAULT 0 COMMENT '排序',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0=禁用，1=启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP 
                      ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` BIT(1) NOT NULL DEFAULT b'0' COMMENT '删除标志',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统菜单表';

-- ----------------------------
-- 表结构：角色菜单关联
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_role_menu` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `menu_id` BIGINT NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_menu` (`role_id`, `menu_id`),
  KEY `idx_role_id` (`role_id`),
  KEY `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关联表';

-- ----------------------------
-- 表结构：图谱元数据
-- ----------------------------
CREATE TABLE IF NOT EXISTS `graphiti_graph_metadata` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `graph_id` VARCHAR(64) NOT NULL COMMENT '图谱ID（UUID）',
  `name` VARCHAR(255) NOT NULL COMMENT '图谱名称',
  `description` TEXT DEFAULT NULL COMMENT '图谱描述',
  `node_count` INT NOT NULL DEFAULT 0 COMMENT '节点数量',
  `edge_count` INT NOT NULL DEFAULT 0 COMMENT '边数量',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP 
                      ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` BIT(1) NOT NULL DEFAULT b'0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_graph_id` (`graph_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='图谱元数据表';

-- ----------------------------
-- 表结构：本体定义
-- ----------------------------
CREATE TABLE IF NOT EXISTS `graphiti_ontology` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `graph_id` VARCHAR(64) NOT NULL COMMENT '图谱ID',
  `entities` JSON DEFAULT NULL COMMENT '实体类型定义（JSON数组）',
  `edges` JSON DEFAULT NULL COMMENT '关系类型定义（JSON数组）',
  `is_default` BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否默认本体',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP 
                      ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` BIT(1) NOT NULL DEFAULT b'0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_graph_id` (`graph_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='本体定义表';

-- ----------------------------
-- 表结构：自定义指令
-- ----------------------------
CREATE TABLE IF NOT EXISTS `graphiti_custom_instruction` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `graph_id` VARCHAR(64) NOT NULL COMMENT '图谱ID',
  `instruction` TEXT NOT NULL COMMENT '指令内容',
  `type` VARCHAR(32) NOT NULL DEFAULT 'entity' COMMENT '指令类型：entity/relation',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP 
                      ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` BIT(1) NOT NULL DEFAULT b'0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  KEY `idx_graph_id` (`graph_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自定义指令表';

-- ----------------------------
-- 表结构：系统操作日志
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_operation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT COMMENT '用户ID',
  `username` VARCHAR(50) COMMENT '用户名',
  `operation` VARCHAR(100) COMMENT '操作名称',
  `method` VARCHAR(200) COMMENT '请求方法和路径',
  `params` TEXT COMMENT '请求参数(JSON)',
  `ip` VARCHAR(50) COMMENT 'IP地址',
  `location` VARCHAR(100) COMMENT '地理位置',
  `status` TINYINT COMMENT '0-失败 1-成功',
  `error_msg` VARCHAR(500) COMMENT '错误信息',
  `duration` INT COMMENT '耗时(毫秒)',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_username` (`username`),
  KEY `idx_operation` (`operation`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统操作日志表';

-- ----------------------------
-- 表结构：系统配置
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_system_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `config_key` VARCHAR(100) NOT NULL COMMENT '配置键',
  `config_value` TEXT COMMENT '配置值',
  `config_name` VARCHAR(100) COMMENT '配置名称',
  `config_description` VARCHAR(500) COMMENT '配置描述',
  `config_type` TINYINT DEFAULT 1 COMMENT '1-文本 2-数字 3-布尔 4-JSON',
  `group_name` VARCHAR(50) COMMENT '分组名称',
  `sort_num` INT DEFAULT 0 COMMENT '排序',
  `status` TINYINT DEFAULT 1 COMMENT '0-禁用 1-启用',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` BIT(1) NOT NULL DEFAULT b'0' COMMENT '删除标志',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`),
  KEY `idx_group_name` (`group_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- ----------------------------
-- 表结构：搜索历史
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_search_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT COMMENT '用户ID',
  `query` VARCHAR(500) NOT NULL COMMENT '搜索词',
  `mode` VARCHAR(20) COMMENT '搜索模式',
  `result_count` INT DEFAULT 0 COMMENT '结果数量',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='搜索历史记录表';
