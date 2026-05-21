-- ============================================================
-- MySQL DDL Migration for ont_episode_type column rename
-- Phase 4: Community System Generic Refactoring
--
-- Add new columns if they don't exist
-- Migrate data from old columns to new columns
-- Note: Old columns are kept for backward compatibility. DROP after Phase 3 migration is confirmed.
-- ============================================================

BEGIN;

-- 1. Add new columns (if they don't exist)
ALTER TABLE ont_episode_type
  ADD COLUMN IF NOT EXISTS `process_type` VARCHAR(32) DEFAULT NULL COMMENT '业务流程类型：business_process|workflow|lifecycle',
  ADD COLUMN IF NOT EXISTS `stage_level` VARCHAR(32) DEFAULT NULL COMMENT '阶段级别（通用，可配置）',
  ADD COLUMN IF NOT EXISTS `is_review_stage` TINYINT(1) DEFAULT 0 COMMENT '是否审查/评议阶段';

-- 2. Migrate data from old columns to new columns
UPDATE ont_episode_type SET `process_type` = `legal_process` WHERE `process_type` IS NULL AND `legal_process` IS NOT NULL;
UPDATE ont_episode_type SET `stage_level` = `court_level` WHERE `stage_level` IS NULL AND `court_level` IS NOT NULL;
UPDATE ont_episode_type SET `is_review_stage` = `is_trial_stage` WHERE `is_review_stage` = 0 AND `is_trial_stage` = 1;

-- 3. Validate migration results
SELECT 'process_type count' AS metric, COUNT(`process_type`) AS value FROM ont_episode_type
UNION ALL
SELECT 'stage_level count', COUNT(`stage_level`) FROM ont_episode_type
UNION ALL
SELECT 'is_review_stage TRUE count', COUNT(*) FROM ont_episode_type WHERE `is_review_stage` = TRUE;

-- 4. DEPRECATION: Add comments to old columns indicating they've been migrated
-- These columns will be dropped in Phase 3 after migration is confirmed
ALTER TABLE ont_episode_type MODIFY COLUMN `legal_process` VARCHAR(32) DEFAULT NULL COMMENT '[向后兼容] 已迁移到 process_type';
ALTER TABLE ont_episode_type MODIFY COLUMN `court_level` VARCHAR(32) DEFAULT NULL COMMENT '[向后兼容] 已迁移到 stage_level';
ALTER TABLE ont_episode_type MODIFY COLUMN `is_trial_stage` TINYINT(1) DEFAULT 0 COMMENT '[向后兼容] 已迁移到 is_review_stage';

COMMIT;

-- ============================================================
-- ROLLBACK SQL (if needed)
-- Execute only if migration needs to be reverted
-- ============================================================
-- BEGIN;
-- ALTER TABLE ont_episode_type DROP COLUMN IF EXISTS `process_type`;
-- ALTER TABLE ont_episode_type DROP COLUMN IF EXISTS `stage_level`;
-- ALTER TABLE ont_episode_type DROP COLUMN IF EXISTS `is_review_stage`;
-- ALTER TABLE ont_episode_type MODIFY COLUMN `legal_process` VARCHAR(32) DEFAULT NULL COMMENT 'litigation: 诉讼 | mediation: 调解 | arbitration: 仲裁 | execution: 执行';
-- ALTER TABLE ont_episode_type MODIFY COLUMN `court_level` VARCHAR(32) DEFAULT NULL COMMENT '一审 | 二审 | 再审 | 死刑复核（仅审判程序有值，ADR类为空）';
-- ALTER TABLE ont_episode_type MODIFY COLUMN `is_trial_stage` TINYINT(1) DEFAULT 0 COMMENT '是否审判阶段（庭审类为 1）';
-- COMMIT;
