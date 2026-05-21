-- ============================================================
-- v004_episode_type_column_rename.sql
-- 将 ont_episode_type 表的 legal_process/court_level/is_trial_stage
-- 重命名为 process_type/stage_level/is_review_stage
-- 执行前请确认无活跃长事务
-- ============================================================

BEGIN;

-- 1. 添加新字段（如果不存在）
ALTER TABLE ont_episode_type ADD COLUMN IF NOT EXISTS process_type VARCHAR(32);
ALTER TABLE ont_episode_type ADD COLUMN IF NOT EXISTS stage_level VARCHAR(32);
ALTER TABLE ont_episode_type ADD COLUMN IF NOT EXISTS is_review_stage BOOLEAN DEFAULT FALSE;

-- 2. 从旧字段迁移数据到新字段
UPDATE ont_episode_type SET process_type = legal_process WHERE process_type IS NULL AND legal_process IS NOT NULL;
UPDATE ont_episode_type SET stage_level = court_level WHERE stage_level IS NULL AND court_level IS NOT NULL;
UPDATE ont_episode_type SET is_review_stage = is_trial_stage WHERE is_review_stage = FALSE AND is_trial_stage = TRUE;

-- 3. 验证迁移结果
SELECT 'process_type count' as metric, count(process_type) as value FROM ont_episode_type
UNION ALL
SELECT 'stage_level count', count(stage_level) FROM ont_episode_type
UNION ALL
SELECT 'is_review_stage TRUE count', count(*) FROM ont_episode_type WHERE is_review_stage = TRUE;

-- 4. 删除旧字段（确认迁移无误后执行，Phase 3 执行）
-- ALTER TABLE ont_episode_type DROP COLUMN IF EXISTS legal_process;
-- ALTER TABLE ont_episode_type DROP COLUMN IF EXISTS court_level;
-- ALTER TABLE ont_episode_type DROP COLUMN IF EXISTS is_trial_stage;

COMMIT;
