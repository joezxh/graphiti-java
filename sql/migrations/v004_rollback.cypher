// ============================================================
// v004_rollback.cypher
// 回滚脚本（如果迁移出错，执行此脚本）
// 将 domain_type/region/scenario_type 回滚为 legal_domain/jurisdiction/practice_type
// ============================================================

// Community 回滚
MATCH (c:Community)
WHERE c.domain_type IS NOT NULL
SET c.legal_domain = c.domain_type
REMOVE c.domain_type
WITH c
SET c.jurisdiction = c.region
REMOVE c.region
WITH c
SET c.practice_type = c.scenario_type
REMOVE c.scenario_type;

// Episode 回滚
MATCH (e:Episode)
WHERE e.process_type IS NOT NULL
SET e.legal_process = e.process_type
REMOVE e.process_type
WITH e
SET e.court_level = e.stage_level
REMOVE e.stage_level
WITH e
SET e.is_trial_stage = e.is_review_stage
REMOVE e.is_review_stage;
