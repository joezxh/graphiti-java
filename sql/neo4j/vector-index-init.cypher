// ============================================================
// Graphiti Neo4j 向量索引初始化脚本
// 版本: 2026-05-18
// 说明: 用于图谱语义检索的向量索引定义
// 要求: Neo4j 5.x (Vector Search Plugin)
// ============================================================

// ============================================================
// 向量索引说明
// ============================================================
// Neo4j 5.x 支持原生向量索引，可用于：
// 1. Entity 节点的语义相似度检索
// 2. RELATES_TO 边的语义相似度检索
//
// 向量维度 (dimensions) 和相似度函数 (similarity_function)
// 需要与 EmbedderService 配置保持一致
//
// 典型配置：
// - OpenAI text-embedding-ada-002: dimensions=1536
// - OpenAI text-embedding-3-small: dimensions=1536
// - OpenAI text-embedding-3-large: dimensions=3072
// - Local models: 根据模型配置
// ============================================================


// ============================================================
// 第一部分: Entity 节点向量索引
// ============================================================

// 节点名称向量索引（用于按名称语义搜索）
CREATE VECTOR INDEX entity_name_embedding_index IF NOT EXISTS
FOR (n:Entity) ON (n.nameEmbedding)
OPTIONS {
  indexConfig: {
    `vector.dimensions`: 1536,
    `vector.similarity_function`: 'cosine'
  },
  indexProvider: 'vector-1.0'
}

// 节点摘要向量索引（用于按描述语义搜索）
CREATE VECTOR INDEX entity_summary_embedding_index IF NOT EXISTS
FOR (n:Entity) ON (n.summaryEmbedding)
OPTIONS {
  indexConfig: {
    `vector.dimensions`: 1536,
    `vector.similarity_function`: 'cosine'
  },
  indexProvider: 'vector-1.0'
}


// ============================================================
// 第二部分: RELATES_TO 边向量索引
// ============================================================

// 边事实描述向量索引（用于按事实语义搜索关系）
CREATE VECTOR INDEX edge_fact_embedding_index IF NOT EXISTS
FOR ()-[r:RELATES_TO]-() ON (r.factEmbedding)
OPTIONS {
  indexConfig: {
    `vector.dimensions`: 1536,
    `vector.similarity_function`: 'cosine'
  },
  indexProvider: 'vector-1.0'
}


// ============================================================
// 第三部分: 通用向量检索示例查询
// ============================================================

// 示例1: 按名称语义搜索 Entity 节点
// WITH [...] AS queryEmbedding
// MATCH (n:Entity)
// WHERE n.graph_id = 'legal-knowledge-graph'
// WITH n, gds.ml.similarity.cosine(n.nameEmbedding, queryEmbedding) AS similarity
// WHERE similarity > 0.7
// RETURN n, similarity
// ORDER BY similarity DESC
// LIMIT 10;

// 示例2: 按摘要语义搜索 Entity 节点
// WITH [...] AS queryEmbedding
// MATCH (n:Entity)
// WHERE n.graph_id = 'legal-knowledge-graph'
// WITH n, gds.ml.similarity.cosine(n.summaryEmbedding, queryEmbedding) AS similarity
// WHERE similarity > 0.7
// RETURN n, similarity
// ORDER BY similarity DESC
// LIMIT 10;

// 示例3: 语义相近的关系搜索
// WITH [...] AS queryEmbedding
// MATCH ()-[r:RELATES_TO]->()
// WHERE r.graph_id = 'legal-knowledge-graph'
// WITH r, gds.ml.similarity.cosine(r.factEmbedding, queryEmbedding) AS similarity
// WHERE similarity > 0.7
// RETURN r, similarity
// ORDER BY similarity DESC
// LIMIT 10;

// 示例4: 混合检索（名称+摘要）
// WITH [...] AS nameEmbedding, [...] AS summaryEmbedding
// MATCH (n:Entity)
// WHERE n.graph_id = 'legal-knowledge-graph'
// WITH n,
    // gds.ml.similarity.cosine(n.nameEmbedding, nameEmbedding) AS nameSim,
    // gds.ml.similarity.cosine(n.summaryEmbedding, summaryEmbedding) AS summarySim,
    // (nameSim + summarySim) / 2 AS combinedSim
// WHERE combinedSim > 0.7
// RETURN n, nameSim, summarySim, combinedSim
// ORDER BY combinedSim DESC
// LIMIT 10;


// ============================================================
// 第四部分: 向量索引管理
// ============================================================

// 查看所有向量索引
// SHOW INDEXES YIELD name, type, labelsOrTypes, properties, state
// WHERE type = 'VECTOR'

// 删除向量索引（如需重建）
// DROP INDEX entity_name_embedding_index IF EXISTS;
// DROP INDEX entity_summary_embedding_index IF EXISTS;
// DROP INDEX edge_fact_embedding_index IF EXISTS;


// ============================================================
// 第五部分: 注意事项
// ============================================================

// 1. 向量维度必须与 EmbedderService 使用的模型一致
// 2. 相似度函数推荐使用 cosine（余弦相似度）
// 3. 向量索引创建后，需要在实际数据导入时填充 embedding
// 4. embedding 字段在 Entity 节点和 RELATES_TO 边上都是可选的
// 5. 如果 embedding 为 null，该节点/边不会被向量索引包含
// 6. 向量索引不支持 NULL 值，如果字段为 NULL，需要先填充默认值或过滤

// ============================================================
// 向量索引初始化完成
// ============================================================
