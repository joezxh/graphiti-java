-- ============================================================
-- Migration v007: OKF Pipeline 文档生成支持表
-- 范围: okf_concept_link / okf_pipeline_execution / okf_pipeline_node_execution / okf_quality_report
-- 依赖: okf_bundle_meta / okf_concept 已存在（Phase 1 已交付）
-- 兼容: PostgreSQL 13+ / MySQL 8.0+
-- ============================================================

-- ---------- PostgreSQL ----------

CREATE TABLE IF NOT EXISTS okf_concept_link (
    id              BIGSERIAL PRIMARY KEY,
    source_concept_id VARCHAR(512) NOT NULL,
    target_concept_id VARCHAR(512) NOT NULL,
    link_type       VARCHAR(32) DEFAULT 'reference',
    link_text       VARCHAR(512),
    detected_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_okf_concept_link UNIQUE (source_concept_id, target_concept_id, link_type)
);
CREATE INDEX IF NOT EXISTS idx_okf_concept_link_source ON okf_concept_link (source_concept_id);
CREATE INDEX IF NOT EXISTS idx_okf_concept_link_target ON okf_concept_link (target_concept_id);
COMMENT ON TABLE okf_concept_link IS 'OKF 概念交叉链接图';

CREATE TABLE IF NOT EXISTS okf_pipeline_execution (
    id                  BIGSERIAL PRIMARY KEY,
    execution_id        VARCHAR(64) NOT NULL UNIQUE,
    trigger_type        VARCHAR(32) NOT NULL DEFAULT 'manual',
    status              VARCHAR(32) NOT NULL DEFAULT 'pending',
    started_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at        TIMESTAMP,
    total_nodes         INT NOT NULL DEFAULT 9,
    completed_nodes     INT NOT NULL DEFAULT 0,
    failed_node         VARCHAR(64),
    error_message       TEXT,
    documents_generated INT NOT NULL DEFAULT 0,
    documents_updated   INT NOT NULL DEFAULT 0,
    execution_summary   JSONB,
    create_by           BIGINT,
    create_time         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted             BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS idx_okf_pipeline_execution_status ON okf_pipeline_execution (status);
CREATE INDEX IF NOT EXISTS idx_okf_pipeline_execution_started ON okf_pipeline_execution (started_at);
COMMENT ON TABLE okf_pipeline_execution IS 'OKF Pipeline 执行记录';

CREATE TABLE IF NOT EXISTS okf_pipeline_node_execution (
    id               BIGSERIAL PRIMARY KEY,
    execution_id     VARCHAR(64) NOT NULL,
    node_name        VARCHAR(64) NOT NULL,
    node_order       INT NOT NULL,
    status           VARCHAR(32) NOT NULL DEFAULT 'pending',
    started_at       TIMESTAMP,
    completed_at     TIMESTAMP,
    input_artifacts  JSONB,
    output_artifacts JSONB,
    metrics          JSONB,
    error_message    TEXT,
    create_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted          BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS idx_okf_pipeline_node_exec ON okf_pipeline_node_execution (execution_id);
CREATE INDEX IF NOT EXISTS idx_okf_pipeline_node_status ON okf_pipeline_node_execution (status);
COMMENT ON TABLE okf_pipeline_node_execution IS 'OKF Pipeline 节点级执行记录';

CREATE TABLE IF NOT EXISTS okf_quality_report (
    id              BIGSERIAL PRIMARY KEY,
    execution_id    VARCHAR(64),
    report_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    total_documents INT NOT NULL DEFAULT 0,
    okf_conformant  INT NOT NULL DEFAULT 0,
    missing_type    INT NOT NULL DEFAULT 0,
    broken_links    INT NOT NULL DEFAULT 0,
    orphan_pages    INT NOT NULL DEFAULT 0,
    stale_pages     INT NOT NULL DEFAULT 0,
    quality_score   DECIMAL(5,2) NOT NULL DEFAULT 0.00,
    report_json     JSONB,
    create_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         BOOLEAN NOT NULL DEFAULT FALSE
);
CREATE INDEX IF NOT EXISTS idx_okf_quality_execution ON okf_quality_report (execution_id);
CREATE INDEX IF NOT EXISTS idx_okf_quality_report_time ON okf_quality_report (report_time);
COMMENT ON TABLE okf_quality_report IS 'OKF 文档质量报告';

-- ---------- MySQL ----------
-- CREATE TABLE IF NOT EXISTS okf_concept_link (
--     id BIGINT AUTO_INCREMENT PRIMARY KEY,
--     source_concept_id VARCHAR(512) NOT NULL,
--     target_concept_id VARCHAR(512) NOT NULL,
--     link_type VARCHAR(32) DEFAULT 'reference',
--     link_text VARCHAR(512),
--     detected_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
--     deleted TINYINT(1) NOT NULL DEFAULT 0,
--     UNIQUE KEY uk_okf_concept_link (source_concept_id, target_concept_id, link_type),
--     INDEX idx_okf_concept_link_source (source_concept_id),
--     INDEX idx_okf_concept_link_target (target_concept_id)
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
-- COMMENT 'OKF 概念交叉链接图';
--
-- CREATE TABLE IF NOT EXISTS okf_pipeline_execution (
--     id BIGINT AUTO_INCREMENT PRIMARY KEY,
--     execution_id VARCHAR(64) NOT NULL UNIQUE,
--     trigger_type VARCHAR(32) NOT NULL DEFAULT 'manual',
--     status VARCHAR(32) NOT NULL DEFAULT 'pending',
--     started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
--     completed_at DATETIME,
--     total_nodes INT NOT NULL DEFAULT 9,
--     completed_nodes INT NOT NULL DEFAULT 0,
--     failed_node VARCHAR(64),
--     error_message TEXT,
--     documents_generated INT NOT NULL DEFAULT 0,
--     documents_updated INT NOT NULL DEFAULT 0,
--     execution_summary JSON,
--     create_by BIGINT,
--     create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
--     deleted TINYINT(1) NOT NULL DEFAULT 0,
--     INDEX idx_okf_pipeline_execution_status (status),
--     INDEX idx_okf_pipeline_execution_started (started_at)
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
-- COMMENT 'OKF Pipeline 执行记录';
--
-- CREATE TABLE IF NOT EXISTS okf_pipeline_node_execution (
--     id BIGINT AUTO_INCREMENT PRIMARY KEY,
--     execution_id VARCHAR(64) NOT NULL,
--     node_name VARCHAR(64) NOT NULL,
--     node_order INT NOT NULL,
--     status VARCHAR(32) NOT NULL DEFAULT 'pending',
--     started_at DATETIME,
--     completed_at DATETIME,
--     input_artifacts JSON,
--     output_artifacts JSON,
--     metrics JSON,
--     error_message TEXT,
--     create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
--     deleted TINYINT(1) NOT NULL DEFAULT 0,
--     INDEX idx_okf_pipeline_node_exec (execution_id),
--     INDEX idx_okf_pipeline_node_status (status)
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
-- COMMENT 'OKF Pipeline 节点级执行记录';
--
-- CREATE TABLE IF NOT EXISTS okf_quality_report (
--     id BIGINT AUTO_INCREMENT PRIMARY KEY,
--     execution_id VARCHAR(64),
--     report_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
--     total_documents INT NOT NULL DEFAULT 0,
--     okf_conformant INT NOT NULL DEFAULT 0,
--     missing_type INT NOT NULL DEFAULT 0,
--     broken_links INT NOT NULL DEFAULT 0,
--     orphan_pages INT NOT NULL DEFAULT 0,
--     stale_pages INT NOT NULL DEFAULT 0,
--     quality_score DECIMAL(5,2) NOT NULL DEFAULT 0.00,
--     report_json JSON,
--     create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
--     deleted TINYINT(1) NOT NULL DEFAULT 0,
--     INDEX idx_okf_quality_execution (execution_id),
--     INDEX idx_okf_quality_report_time (report_time)
-- ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
-- COMMENT 'OKF 文档质量报告';
