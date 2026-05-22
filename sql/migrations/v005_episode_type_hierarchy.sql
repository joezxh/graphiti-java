-- ============================================
-- V005: Episode Type 层级化 + 清理向后兼容字段
-- ============================================

-- 1. 新增层级与审计字段
ALTER TABLE ont_episode_type
ADD COLUMN IF NOT EXISTS parent_type_code VARCHAR(50),
ADD COLUMN IF NOT EXISTS level INT DEFAULT 1,
ADD COLUMN IF NOT EXISTS created_by VARCHAR(64),
ADD COLUMN IF NOT EXISTS updated_by VARCHAR(64),
ADD COLUMN IF NOT EXISTS version INT DEFAULT 1;

-- 2. 增加约束
ALTER TABLE ont_episode_type
ADD CONSTRAINT uk_episode_type_code UNIQUE (definition_id, type_code),
ADD CONSTRAINT chk_episode_type_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'DEPRECATED')),
ADD CONSTRAINT chk_episode_type_level CHECK (level BETWEEN 1 AND 5);

-- 3. 增加索引
CREATE INDEX IF NOT EXISTS idx_episode_type_parent ON ont_episode_type(definition_id, parent_type_code);
CREATE INDEX IF NOT EXISTS idx_episode_type_process ON ont_episode_type(definition_id, process_type);
CREATE INDEX IF NOT EXISTS idx_episode_type_status ON ont_episode_type(status);

-- 4. 删除向后兼容字段（Phase 4 清理）
-- 先确认没有数据依赖这些字段后再执行
-- ALTER TABLE ont_episode_type DROP COLUMN IF EXISTS legal_process;
-- ALTER TABLE ont_episode_type DROP COLUMN IF EXISTS court_level;
-- ALTER TABLE ont_episode_type DROP COLUMN IF EXISTS is_trial_stage;
