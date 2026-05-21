// ============================================================
// Graphiti Neo4j 迁移脚本
// 版本: 2026-05-20
// 说明: Community 节点属性通用化重命名 + Episode 节点属性重命名
//       legal_domain → domain_type
//       jurisdiction → region
//       practice_type → scenario_type
//       legal_process → process_type
//       court_level → stage_level
//       is_trial_stage → is_review_stage
// 执行: cypher-shell -u neo4j -p password < v004_community_generic_rename.cypher
// ============================================================

// ---------- Community 节点属性重命名 ----------

// legal_domain → domain_type
MATCH (c:Community)
WHERE c.legal_domain IS NOT NULL
SET c.domain_type = c.legal_domain
REMOVE c.legal_domain;

// jurisdiction → region
MATCH (c:Community)
WHERE c.jurisdiction IS NOT NULL
SET c.region = c.jurisdiction
REMOVE c.jurisdiction;

// jurisdiction 为 NULL 时设置默认值
MATCH (c:Community)
WHERE c.region IS NULL
SET c.region = 'REGION_ROOT';

// practice_type → scenario_type
MATCH (c:Community)
WHERE c.practice_type IS NOT NULL
SET c.scenario_type = c.practice_type
REMOVE c.practice_type;

// scenario_type 为 NULL 时设置默认值
MATCH (c:Community)
WHERE c.scenario_type IS NULL
SET c.scenario_type = 'SCENARIO_ROOT';

// ---------- Episode 节点属性重命名 ----------

// legal_process → process_type
MATCH (e:Episode)
WHERE e.legal_process IS NOT NULL
SET e.process_type = e.legal_process
REMOVE e.legal_process;

// court_level → stage_level
MATCH (e:Episode)
WHERE e.court_level IS NOT NULL
SET e.stage_level = e.court_level
REMOVE e.court_level;

// is_trial_stage → is_review_stage
MATCH (e:Episode)
WHERE e.is_trial_stage IS NOT NULL
SET e.is_review_stage = e.is_trial_stage
REMOVE e.is_trial_stage;

// ---------- 索引重建（移除旧索引，创建新索引） ----------

// 删除旧索引（忽略不存在错误）
DROP INDEX community_type_v3 IF EXISTS;
DROP INDEX community_legal_domain_v3 IF EXISTS;

// 创建新索引
CREATE INDEX community_domain_type_v3 IF NOT EXISTS FOR (n:Community) ON (n.domain_type);
CREATE INDEX community_region_v3 IF NOT EXISTS FOR (n:Community) ON (n.region);
CREATE INDEX community_scenario_type_v3 IF NOT EXISTS FOR (n:Community) ON (n.scenario_type);

// 创建 episode 新字段索引
CREATE INDEX episode_process_type_v3 IF NOT EXISTS FOR (n:Episode) ON (n.process_type);
CREATE INDEX episode_stage_level_v3 IF NOT EXISTS FOR (n:Episode) ON (n.stage_level);

// ---------- 验证查询 ----------

// 验证 Community 迁移
MATCH (c:Community)
RETURN count(c) as total_communities,
       count(c.domain_type) as with_domain_type,
       count(c.region) as with_region,
       count(c.scenario_type) as with_scenario_type,
       count(c.legal_domain) as remaining_legal_domain,
       count(c.jurisdiction) as remaining_jurisdiction,
       count(c.practice_type) as remaining_practice_type;

// 验证 Episode 迁移
MATCH (e:Episode)
RETURN count(e) as total_episodes,
       count(e.process_type) as with_process_type,
       count(e.stage_level) as with_stage_level,
       count(e.is_review_stage) as with_is_review_stage,
       count(e.legal_process) as remaining_legal_process,
       count(e.court_level) as remaining_court_level,
       count(e.is_trial_stage) as remaining_is_trial_stage;
