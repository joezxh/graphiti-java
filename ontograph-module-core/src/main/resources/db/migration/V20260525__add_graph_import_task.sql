-- V20260525__add_graph_import_task.sql
-- Import task tracking table (reference for future DB persistence)
-- Currently uses ConcurrentHashMap in ImportTaskRepository
CREATE TABLE IF NOT EXISTS graph_import_task (
    task_id           VARCHAR(64) PRIMARY KEY,
    graph_id         VARCHAR(64) NOT NULL,
    total_items      INT NOT NULL DEFAULT 0,
    processed_items  INT NOT NULL DEFAULT 0,
    failed_items     INT NOT NULL DEFAULT 0,
    entities_created INT NOT NULL DEFAULT 0,
    relations_created INT NOT NULL DEFAULT 0,
    status           VARCHAR(32) NOT NULL DEFAULT 'PROCESSING',
    error_details    TEXT,
    duration_ms      BIGINT,
    create_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_graph_import_task_graph_id ON graph_import_task(graph_id);
CREATE INDEX IF NOT EXISTS idx_graph_import_task_status ON graph_import_task(status);
CREATE INDEX IF NOT EXISTS idx_graph_import_task_create_time ON graph_import_task(create_time);
