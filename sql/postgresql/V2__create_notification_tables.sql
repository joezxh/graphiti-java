-- ============================================================
-- 通知模块表
-- 创建时间: 2026-05-10
-- ============================================================

-- 通知表
CREATE TABLE sys_notification (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT,
    type SMALLINT NOT NULL DEFAULT 1,
    is_read SMALLINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- 为 sys_notification 表创建更新时间触发器
CREATE TRIGGER update_sys_notification_update_time
    BEFORE UPDATE ON sys_notification
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- 用户通知设置表
CREATE TABLE sys_user_notification_settings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    system_enabled SMALLINT NOT NULL DEFAULT 1,
    graph_enabled SMALLINT NOT NULL DEFAULT 1,
    search_enabled SMALLINT NOT NULL DEFAULT 1,
    email_enabled SMALLINT NOT NULL DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN NOT NULL DEFAULT FALSE
);

-- 为 sys_user_notification_settings 表创建更新时间触发器
CREATE TRIGGER update_sys_user_notification_settings_update_time
    BEFORE UPDATE ON sys_user_notification_settings
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- ============================================================
-- 索引创建
-- ============================================================
CREATE INDEX idx_sys_notification_user_id ON sys_notification(user_id);
CREATE INDEX idx_sys_notification_type ON sys_notification(type);
CREATE INDEX idx_sys_notification_is_read ON sys_notification(is_read);
CREATE INDEX idx_sys_notification_deleted ON sys_notification(deleted);
CREATE INDEX idx_sys_notification_create_time ON sys_notification(create_time DESC);

CREATE INDEX idx_sys_user_notification_settings_user_id ON sys_user_notification_settings(user_id);
CREATE INDEX idx_sys_user_notification_settings_deleted ON sys_user_notification_settings(deleted);

-- ============================================================
-- 注释
-- ============================================================
COMMENT ON TABLE sys_notification IS '系统通知表';
COMMENT ON COLUMN sys_notification.id IS '通知ID';
COMMENT ON COLUMN sys_notification.user_id IS '用户ID';
COMMENT ON COLUMN sys_notification.title IS '通知标题';
COMMENT ON COLUMN sys_notification.content IS '通知内容';
COMMENT ON COLUMN sys_notification.type IS '通知类型: 1-系统通知 2-图谱通知 3-检索通知';
COMMENT ON COLUMN sys_notification.is_read IS '已读状态: 0-未读 1-已读';
COMMENT ON COLUMN sys_notification.create_time IS '创建时间';
COMMENT ON COLUMN sys_notification.update_time IS '更新时间';
COMMENT ON COLUMN sys_notification.deleted IS '删除标志';

COMMENT ON TABLE sys_user_notification_settings IS '用户通知设置表';
COMMENT ON COLUMN sys_user_notification_settings.id IS '设置ID';
COMMENT ON COLUMN sys_user_notification_settings.user_id IS '用户ID';
COMMENT ON COLUMN sys_user_notification_settings.system_enabled IS '系统通知开关: 0-关闭 1-开启';
COMMENT ON COLUMN sys_user_notification_settings.graph_enabled IS '图谱通知开关: 0-关闭 1-开启';
COMMENT ON COLUMN sys_user_notification_settings.search_enabled IS '检索通知开关: 0-关闭 1-开启';
COMMENT ON COLUMN sys_user_notification_settings.email_enabled IS '邮件通知开关: 0-关闭 1-开启';
COMMENT ON COLUMN sys_user_notification_settings.create_time IS '创建时间';
COMMENT ON COLUMN sys_user_notification_settings.update_time IS '更新时间';
COMMENT ON COLUMN sys_user_notification_settings.deleted IS '删除标志';

CREATE TABLE IF NOT EXISTS "public"."custom_instruction" (
                                                             "id"          BIGSERIAL    PRIMARY KEY,
                                                             "graph_id"    VARCHAR(64)  DEFAULT NULL,
    "instruction" TEXT         NOT NULL,
    "enabled"     BOOLEAN     DEFAULT TRUE,
    "created_at"  TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    "updated_at"  TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
    );

COMMENT ON TABLE "public"."custom_instruction" IS '自定义抽取指令表';
COMMENT ON COLUMN "public"."custom_instruction"."id"          IS '主键ID';
COMMENT ON COLUMN "public"."custom_instruction"."graph_id"    IS '图谱ID（null表示全局指令）';
COMMENT ON COLUMN "public"."custom_instruction"."instruction" IS 'LLM抽取时的额外提示词';
COMMENT ON COLUMN "public"."custom_instruction"."enabled"    IS '是否启用';
COMMENT ON COLUMN "public"."custom_instruction"."created_at"  IS '创建时间';
COMMENT ON COLUMN "public"."custom_instruction"."updated_at"  IS '更新时间';

CREATE INDEX IF NOT EXISTS "idx_custom_instruction_graph_id" ON "public"."custom_instruction" ("graph_id");
CREATE INDEX IF NOT EXISTS "idx_custom_instruction_enabled" ON "public"."custom_instruction" ("enabled");

-- 更新时间触发器
CREATE OR REPLACE FUNCTION "update_updated_at_column"()
RETURNS TRIGGER AS $$
BEGIN
    NEW."updated_at" = CURRENT_TIMESTAMP;
RETURN NEW;
END;
$$ language 'plpgsql';

DROP TRIGGER IF EXISTS "trigger_custom_instruction_updated_at" ON "public"."custom_instruction";
CREATE TRIGGER "trigger_custom_instruction_updated_at"
    BEFORE UPDATE ON "public"."custom_instruction"
    FOR EACH ROW EXECUTE FUNCTION "update_updated_at_column"();
