-- sql/postgresql/V1__create_ontology_tables.sql

-- 本体定义主表
CREATE TABLE ont_definition (
    id                  BIGSERIAL PRIMARY KEY,
    graph_id            VARCHAR(64) NOT NULL,
    namespace           VARCHAR(255) DEFAULT 'default',
    name                VARCHAR(128) NOT NULL,
    version             VARCHAR(32) NOT NULL DEFAULT '1.0.0',
    status              VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    description         TEXT,
    parent_version_id   BIGINT REFERENCES ont_definition(id),
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by          VARCHAR(64),
    UNIQUE (graph_id, namespace, name, version)
);
CREATE INDEX idx_ont_def_graph_id ON ont_definition(graph_id);
CREATE INDEX idx_ont_def_status  ON ont_definition(status);

-- 类定义表
CREATE TABLE ont_class (
    id              BIGSERIAL PRIMARY KEY,
    definition_id   BIGINT NOT NULL REFERENCES ont_definition(id) ON DELETE CASCADE,
    class_uri       VARCHAR(512) NOT NULL,
    local_name      VARCHAR(128) NOT NULL,
    parent_class_id BIGINT REFERENCES ont_class(id) ON DELETE CASCADE,
    equivalent_to   TEXT,
    disjoint_with   TEXT,
    description     TEXT,
    example         TEXT,
    domain_hint     VARCHAR(32),
    metadata        TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (definition_id, class_uri)
);
CREATE INDEX idx_ont_class_def     ON ont_class(definition_id);
CREATE INDEX idx_ont_class_parent  ON ont_class(parent_class_id);
CREATE INDEX idx_ont_class_domain  ON ont_class(domain_hint);

-- 属性定义表
CREATE TABLE ont_property (
    id                  BIGSERIAL PRIMARY KEY,
    definition_id       BIGINT NOT NULL REFERENCES ont_definition(id) ON DELETE CASCADE,
    property_uri        VARCHAR(512) NOT NULL,
    local_name          VARCHAR(128) NOT NULL,
    property_type       VARCHAR(16) NOT NULL,
    domain_class_id     BIGINT REFERENCES ont_class(id),
    range_class_id      BIGINT REFERENCES ont_class(id),
    range_data_type     VARCHAR(32),
    min_cardinality     INTEGER,
    max_cardinality     INTEGER,
    default_value       TEXT,
    allowed_values      TEXT,
    parent_property_id  BIGINT REFERENCES ont_property(id) ON DELETE CASCADE,
    equivalent_to       TEXT,
    inverse_of_id       BIGINT REFERENCES ont_property(id) ON DELETE SET NULL,
    is_required         BOOLEAN NOT NULL DEFAULT FALSE,
    is_multiple         BOOLEAN NOT NULL DEFAULT FALSE,
    pattern             VARCHAR(256),
    min_value           NUMERIC,
    max_value           NUMERIC,
    description         TEXT,
    example             TEXT,
    metadata            TEXT,
    created_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (definition_id, property_uri)
);
CREATE INDEX idx_ont_prop_def     ON ont_property(definition_id);
CREATE INDEX idx_ont_prop_type    ON ont_property(property_type);
CREATE INDEX idx_ont_prop_domain  ON ont_property(domain_class_id);
CREATE INDEX idx_ont_prop_range   ON ont_property(range_class_id);
CREATE INDEX idx_ont_prop_parent  ON ont_property(parent_property_id);

-- 约束定义表
CREATE TABLE ont_constraint (
    id              BIGSERIAL PRIMARY KEY,
    definition_id   BIGINT NOT NULL REFERENCES ont_definition(id) ON DELETE CASCADE,
    class_id        BIGINT REFERENCES ont_class(id) ON DELETE CASCADE,
    property_id     BIGINT REFERENCES ont_property(id) ON DELETE CASCADE,
    constraint_type VARCHAR(32) NOT NULL,
    value           TEXT NOT NULL,
    error_message   VARCHAR(512),
    severity        VARCHAR(16) NOT NULL DEFAULT 'ERROR',
    description     TEXT,
    created_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (class_id, property_id, constraint_type)
);
CREATE INDEX idx_ont_constraint_def   ON ont_constraint(definition_id);
CREATE INDEX idx_ont_constraint_class ON ont_constraint(class_id);

-- 版本历史表
CREATE TABLE ont_version_history (
    id              BIGSERIAL PRIMARY KEY,
    definition_id   BIGINT NOT NULL REFERENCES ont_definition(id) ON DELETE CASCADE,
    version         VARCHAR(32) NOT NULL,
    change_type     VARCHAR(32) NOT NULL,
    entity_type     VARCHAR(16) NOT NULL,
    entity_id       BIGINT,
    before_state    TEXT,
    after_state     TEXT,
    diff_summary    TEXT,
    changed_by      VARCHAR(64),
    changed_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_ont_version_def  ON ont_version_history(definition_id);
CREATE INDEX idx_ont_version_time ON ont_version_history(changed_at DESC);
