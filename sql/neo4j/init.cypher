// Graphiti 知识图谱系统 Neo4j 初始化脚本
// Neo4j 5.x
// 创建唯一性约束：实体节点的 uuid 属性必须唯一
CREATE CONSTRAINT entity_uuid IF NOT EXISTS 
FOR (n:Entity) REQUIRE n.uuid IS UNIQUE;

// 创建唯一性约束：事件节点的 uuid 属性必须唯一
CREATE CONSTRAINT episode_uuid IF NOT EXISTS 
FOR (n:Episode) REQUIRE n.uuid IS UNIQUE;

// 创建索引：实体节点的 group_id 属性（用于多租户隔离）
CREATE INDEX entity_group_id IF NOT EXISTS 
FOR (n:Entity) ON (n.group_id);

// 创建索引：实体节点的 name 属性（用于按名称查询）
CREATE INDEX entity_name IF NOT EXISTS 
FOR (n:Entity) ON (n.name);

// 创建全文索引：实体节点的 name 和 summary 属性（用于全文检索）
CREATE FULLTEXT INDEX entity_search IF NOT EXISTS 
FOR (n:Entity) ON EACH [n.name, n.summary];

// 创建索引：事件节点的 group_id 属性
CREATE INDEX episode_group_id IF NOT EXISTS 
FOR (n:Episode) ON (n.group_id);

// 创建索引：关系边的 type 属性（用于按关系类型查询）
CREATE INDEX relation_type IF NOT EXISTS 
FOR ()-[r:RELATES_TO]-() ON (r.type);

// 完成提示
RETURN 'Neo4j initialization completed successfully' AS message;
