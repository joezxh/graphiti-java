-- V20260620__add_okf_tables.sql
-- OKF (Ontology Knowledge Format) 核心表结构
-- 用于存储 LLM-Wiki Bundle 和 Concept 的元数据

-- ============================================================
-- 1. Bundle 元数据表
-- ============================================================
CREATE TABLE IF NOT EXISTS okf_bundle_meta (
    id              BIGSERIAL PRIMARY KEY,
    bundle_id       VARCHAR(64) NOT NULL UNIQUE,
    name            VARCHAR(256) NOT NULL,
    slug            VARCHAR(128) NOT NULL UNIQUE,
    description     TEXT,
    bundle_type     VARCHAR(32) NOT NULL DEFAULT 'standard',
    version         VARCHAR(32) NOT NULL DEFAULT '1.0.0',
    license         VARCHAR(64),
    author          VARCHAR(128),
    source_url      VARCHAR(512),
    graph_id        VARCHAR(64),
    status          VARCHAR(16) NOT NULL DEFAULT 'draft',
    is_featured     BOOLEAN NOT NULL DEFAULT FALSE,
    tags            JSONB,
    metadata        JSONB,
    concept_count   INT NOT NULL DEFAULT 0,
    download_count  INT NOT NULL DEFAULT 0,
    star_count      INT NOT NULL DEFAULT 0,
    create_by       BIGINT,
    update_by       BIGINT,
    create_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_okf_bundle_slug ON okf_bundle_meta(slug);
CREATE INDEX IF NOT EXISTS idx_okf_bundle_graph_id ON okf_bundle_meta(graph_id);
CREATE INDEX IF NOT EXISTS idx_okf_bundle_status ON okf_bundle_meta(status);
CREATE INDEX IF NOT EXISTS idx_okf_bundle_type ON okf_bundle_meta(bundle_type);
CREATE INDEX IF NOT EXISTS idx_okf_bundle_deleted ON okf_bundle_meta(deleted);

-- ============================================================
-- 2. Bundle 别名表（支持多语言/多名称）
-- ============================================================
CREATE TABLE IF NOT EXISTS okf_bundle_alias (
    id              BIGSERIAL PRIMARY KEY,
    bundle_id       VARCHAR(64) NOT NULL,
    alias           VARCHAR(256) NOT NULL,
    locale          VARCHAR(16) DEFAULT 'en',
    is_primary      BOOLEAN NOT NULL DEFAULT FALSE,
    create_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bundle_alias_bundle FOREIGN KEY (bundle_id) REFERENCES okf_bundle_meta(bundle_id) ON DELETE CASCADE,
    CONSTRAINT uk_bundle_alias UNIQUE (bundle_id, alias)
);

CREATE INDEX IF NOT EXISTS idx_okf_bundle_alias_bundle_id ON okf_bundle_alias(bundle_id);
CREATE INDEX IF NOT EXISTS idx_okf_bundle_alias_locale ON okf_bundle_alias(locale);

-- ============================================================
-- 3. 概念文档表
-- ============================================================
CREATE TABLE IF NOT EXISTS okf_concept (
    id              BIGSERIAL PRIMARY KEY,
    concept_id      VARCHAR(64) NOT NULL UNIQUE,
    bundle_id       VARCHAR(64) NOT NULL,
    name            VARCHAR(256) NOT NULL,
    slug            VARCHAR(128) NOT NULL,
    doc_type        VARCHAR(32) NOT NULL DEFAULT 'concept',
    frontmatter     JSONB NOT NULL DEFAULT '{}',
    content_md      TEXT,
    content_html    TEXT,
    summary         TEXT,
    ont_class_id    VARCHAR(64),
    is_published    BOOLEAN NOT NULL DEFAULT TRUE,
    is_featured     BOOLEAN NOT NULL DEFAULT FALSE,
    tags            JSONB,
    metadata        JSONB,
    link_count      INT NOT NULL DEFAULT 0,
    view_count      INT NOT NULL DEFAULT 0,
    create_by       BIGINT,
    update_by       BIGINT,
    create_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_concept_bundle FOREIGN KEY (bundle_id) REFERENCES okf_bundle_meta(bundle_id) ON DELETE CASCADE,
    CONSTRAINT uk_concept_slug_bundle UNIQUE (bundle_id, slug)
);

CREATE INDEX IF NOT EXISTS idx_okf_concept_bundle_id ON okf_concept(bundle_id);
CREATE INDEX IF NOT EXISTS idx_okf_concept_slug ON okf_concept(slug);
CREATE INDEX IF NOT EXISTS idx_okf_concept_ont_class ON okf_concept(ont_class_id);
CREATE INDEX IF NOT EXISTS idx_okf_concept_doc_type ON okf_concept(doc_type);
CREATE INDEX IF NOT EXISTS idx_okf_concept_deleted ON okf_concept(deleted);
CREATE INDEX IF NOT EXISTS idx_okf_concept_published ON okf_concept(is_published);

-- ============================================================
-- 4. 概念别名表
-- ============================================================
CREATE TABLE IF NOT EXISTS okf_concept_alias (
    id              BIGSERIAL PRIMARY KEY,
    concept_id      VARCHAR(64) NOT NULL,
    alias           VARCHAR(256) NOT NULL,
    locale          VARCHAR(16) DEFAULT 'en',
    is_primary      BOOLEAN NOT NULL DEFAULT FALSE,
    create_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_concept_alias_concept FOREIGN KEY (concept_id) REFERENCES okf_concept(concept_id) ON DELETE CASCADE,
    CONSTRAINT uk_concept_alias UNIQUE (concept_id, alias)
);

CREATE INDEX IF NOT EXISTS idx_okf_concept_alias_concept_id ON okf_concept_alias(concept_id);
CREATE INDEX IF NOT EXISTS idx_okf_concept_alias_locale ON okf_concept_alias(locale);

-- ============================================================
-- 5. Frontmatter 字段定义表
-- ============================================================
CREATE TABLE IF NOT EXISTS okf_frontmatter_field (
    id              BIGSERIAL PRIMARY KEY,
    field_name      VARCHAR(64) NOT NULL,
    field_label     VARCHAR(128) NOT NULL,
    field_type      VARCHAR(32) NOT NULL DEFAULT 'text',
    field_schema     JSONB,
    default_value   TEXT,
    is_required     BOOLEAN NOT NULL DEFAULT FALSE,
    is_system       BOOLEAN NOT NULL DEFAULT FALSE,
    validation_rule VARCHAR(512),
    sort_order      INT NOT NULL DEFAULT 0,
    status          VARCHAR(16) NOT NULL DEFAULT 'active',
    create_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted         BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_frontmatter_field_name UNIQUE (field_name)
);

CREATE INDEX IF NOT EXISTS idx_okf_frontmatter_field_type ON okf_frontmatter_field(field_type);
CREATE INDEX IF NOT EXISTS idx_okf_frontmatter_field_status ON okf_frontmatter_field(status);

-- ============================================================
-- 6. Bundle-Ontology 映射关系表
-- ============================================================
CREATE TABLE IF NOT EXISTS okf_bundle_ontology_mapping (
    id              BIGSERIAL PRIMARY KEY,
    bundle_id       VARCHAR(64) NOT NULL,
    ont_class_id    VARCHAR(64) NOT NULL,
    ont_property_id VARCHAR(64),
    mapping_type    VARCHAR(32) NOT NULL DEFAULT 'class',
    mapping_config  JSONB,
    confidence      DECIMAL(5,4),
    create_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_mapping_bundle FOREIGN KEY (bundle_id) REFERENCES okf_bundle_meta(bundle_id) ON DELETE CASCADE,
    CONSTRAINT uk_bundle_ontology_mapping UNIQUE (bundle_id, ont_class_id, mapping_type)
);

CREATE INDEX IF NOT EXISTS idx_okf_mapping_bundle ON okf_bundle_ontology_mapping(bundle_id);
CREATE INDEX IF NOT EXISTS idx_okf_mapping_ont_class ON okf_bundle_ontology_mapping(ont_class_id);
