-- ============================================================
-- 通知模块表 (MySQL 版本)
-- 创建时间: 2026-05-10
-- ============================================================

-- 通知表
CREATE TABLE sys_notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    title VARCHAR(255) NOT NULL COMMENT '通知标题',
    content TEXT COMMENT '通知内容',
    type SMALLINT NOT NULL DEFAULT 1 COMMENT '通知类型: 1-系统通知 2-图谱通知 3-检索通知',
    is_read SMALLINT NOT NULL DEFAULT 0 COMMENT '已读状态: 0-未读 1-已读',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted SMALLINT DEFAULT 0 COMMENT '逻辑删除标记',
    INDEX idx_sys_notification_user_id (user_id),
    INDEX idx_sys_notification_type (type),
    INDEX idx_sys_notification_is_read (is_read),
    INDEX idx_sys_notification_deleted (deleted),
    INDEX idx_sys_notification_created_at (created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统通知表';

-- 用户通知设置表
CREATE TABLE sys_user_notification_settings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE COMMENT '用户ID',
    system_enabled SMALLINT NOT NULL DEFAULT 1 COMMENT '系统通知开关: 0-关闭 1-开启',
    graph_enabled SMALLINT NOT NULL DEFAULT 1 COMMENT '图谱通知开关: 0-关闭 1-开启',
    search_enabled SMALLINT NOT NULL DEFAULT 1 COMMENT '检索通知开关: 0-关闭 1-开启',
    email_enabled SMALLINT NOT NULL DEFAULT 0 COMMENT '邮件通知开关: 0-关闭 1-开启',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted SMALLINT DEFAULT 0 COMMENT '逻辑删除标记',
    INDEX idx_sys_user_notification_settings_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户通知设置表';


CREATE TABLE IF NOT EXISTS `custom_instruction` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `graph_id`     VARCHAR(64)  DEFAULT NULL    COMMENT '图谱ID（null表示全局指令）',
    `instruction`  TEXT         NOT NULL         COMMENT 'LLM抽取时的额外提示词',
    `enabled`      TINYINT(1)  DEFAULT 1        COMMENT '是否启用：1=启用，0=禁用',
    `created_at`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_graph_id` (`graph_id`),
    INDEX `idx_enabled`   (`enabled`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='自定义抽取指令表';