-- ============================================================
-- Migration v006: ont_class 表国际化字段扩展
-- 说明: 为 ont_class 增加 name_en 字段，支持本地语言与英文双语存储
-- 兼容: PostgreSQL 13+ / MySQL 8.0+
-- ============================================================

-- ---------- PostgreSQL ----------
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'ont_class' AND column_name = 'name_en'
    ) THEN
        ALTER TABLE ont_class ADD COLUMN name_en VARCHAR(128);
        COMMENT ON COLUMN ont_class.name_en IS '类的英文名称';
    END IF;
END $$;

-- 将现有 local_name 复制到 name_en 作为初始值（保持向后兼容）
UPDATE ont_class SET name_en = local_name WHERE name_en IS NULL;

-- ---------- MySQL ----------
-- ALTER TABLE ont_class ADD COLUMN IF NOT EXISTS name_en VARCHAR(128) COMMENT '类的英文名称';
-- UPDATE ont_class SET name_en = local_name WHERE name_en IS NULL;
