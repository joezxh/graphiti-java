// Neo4j 向量索引初始化脚本
// 在 Neo4j 5.x 中执行以下 Cypher 语句创建向量索引

// 1. 节点向量索引（Entity 节点的 embedding 属性）
CREATE VECTOR INDEX node_embedding_index IF NOT EXISTS
FOR (n:Entity) ON (n.embedding)
OPTIONS {indexConfig: {
    `vector.dimensions`: 1536,
    `vector.similarity_function`: 'cosine'
}};

// 2. 边向量索引（RELATES_TO 关系的 embedding 属性）
CREATE VECTOR INDEX edge_embedding_index IF NOT EXISTS
FOR ()-[r:RELATES_TO]-() ON (r.embedding)
OPTIONS {indexConfig: {
    `vector.dimensions`: 1536,
    `vector.similarity_function`: 'cosine'
}};

// 3. 全文索引（节点名称和摘要）
CREATE FULLTEXT INDEX nodeNameIndex IF NOT EXISTS
FOR (n:Entity) ON EACH [n.name, n.summary];

// 4. 全文索引（边的事实描述）
CREATE FULLTEXT INDEX edgeFactIndex IF NOT EXISTS
FOR ()-[r:RELATES_TO]-() ON EACH [r.fact];
