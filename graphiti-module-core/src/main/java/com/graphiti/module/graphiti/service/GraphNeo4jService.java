package com.graphiti.module.graphiti.service;

import com.graphiti.module.graphiti.config.GraphNeo4jConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Result;
import org.neo4j.driver.Record;
import org.neo4j.driver.Values;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * Neo4j 数据访问服务类
 * 提供节点和关系的 CRUD 操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GraphNeo4jService {
    
    private final Driver neo4jDriver;
    private final GraphNeo4jConfig neo4jConfig;

    public Driver getNeo4jDriver() {
        return neo4jDriver;
    }
    
    /**
     * 创建实体节点（带嵌入向量）
     * @param graphId 图谱ID（用作 graph_id）
     * @param uuid 节点UUID
     * @param name 节点名称
     * @param type 节点类型（实体类型）
     * @param summary 节点摘要
     * @param embedding 嵌入向量
     * @param properties 属性 Map
     * @return 创建的节点信息
     */
    public Map<String, Object> createEntityNode(String graphId, String uuid, String name, String type,
                                                String summary, float[] embedding, Map<String, Object> properties) {
        // 根据 type 将 name 写入对应的类型专属字段
        String nameField = getTypeNameField(type);
        
        // props 中的类型专属名称字段应该被忽略，避免与 name 参数冲突
        // 构建排除列表
        List<String> nameFieldsToExclude = getAllTypeNameFields();
        Map<String, Object> safeProps = properties != null 
            ? properties.entrySet().stream()
                .filter(e -> !nameFieldsToExclude.contains(e.getKey()))
                .collect(java.util.stream.Collectors.toMap(java.util.Map.Entry::getKey, java.util.Map.Entry::getValue))
            : new HashMap<>();
        
        String cypher = "CREATE (n:Entity {graph_id: $graph_id, uuid: $uuid, type: $type, " +
                        "summary: $summary, embedding: $embedding, valid_at: timestamp(), invalid_at: null}) " +
                        "SET n." + nameField + " = $name SET n += $props RETURN n";

        Map<String, Object> params = new HashMap<>();
        params.put("graph_id", graphId);
        params.put("uuid", uuid);
        params.put("name", name);
        params.put("type", type);
        params.put("summary", summary != null ? summary : "");
        params.put("embedding", embedding != null ? toFloatList(embedding) : null);
        params.put("props", properties != null ? properties : new HashMap<>());

        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, params);
            if (result.hasNext()) {
                Record record = result.next();
                return record.get("n").asNode().asMap();
            }
        }
        return null;
    }
    
    /**
     * 根据节点类型获取对应的名称字段名
     */
    private String getTypeNameField(String type) {
        if (type == null) return "name";
        return switch (type) {
            case "Court" -> "courtName";
            case "Party" -> "partyName";
            case "Case" -> "caseName";
            case "LegalProvision" -> "articleNumber";
            case "Judge" -> "judgeName";
            case "JudgmentDocument" -> "documentNumber";
            case "MediationAgreement" -> "agreementNumber";
            case "CommercialMediationOrganization" -> "name";
            case "Mediator" -> "name";
            case "Evidence" -> "evidenceNumber";
            case "CaseReasoning" -> "reasoning";
            case "CaseFact" -> "factDescription";
            default -> "name";
        };
    }

    /**
     * 更新节点嵌入向量
     * @param graphId 图谱ID
     * @param uuid 节点UUID
     * @param embedding 嵌入向量
     */
    public void updateNodeEmbedding(String graphId, String uuid, float[] embedding) {
        String cypher = "MATCH (n:Entity {graph_id: $graph_id, uuid: $uuid}) " +
                        "SET n.embedding = $embedding";
        try (Session session = neo4jDriver.session()) {
            session.run(cypher, Values.parameters("graph_id", graphId, "uuid", uuid,
                    "embedding", embedding != null ? toFloatList(embedding) : null));
        }
    }
    
    /**
     * 创建关系边（带嵌入向量）
     * @param graphId 图谱ID
     * @param edgeUuid 关系UUID（可选，为 null 时自动生成）
     * @param sourceUuid 源节点UUID
     * @param targetUuid 目标节点UUID
     * @param type 关系类型
     * @param fact 事实描述
     * @param embedding 嵌入向量
     * @param properties 属性 Map
     * @return 创建的关系信息
     */
    public Map<String, Object> createRelationship(String graphId, String edgeUuid, String sourceUuid,
                                                    String targetUuid, String type, String fact,
                                                    float[] embedding, Map<String, Object> properties) {
        String relationType = (type != null && !type.isBlank()) ? type : "RELATES_TO";
        String cypher =
            "MATCH (a:Entity {graph_id: $graph_id, uuid: $sourceUuid}) " +
            "MATCH (b:Entity {graph_id: $graph_id, uuid: $targetUuid}) " +
            "CREATE (a)-[r:" + relationType + " {uuid: $edgeUuid, type: $type, fact: $fact, " +
            "embedding: $embedding, valid_at: timestamp(), invalid_at: null}]->(b) " +
            "SET r += $props RETURN r";

        Map<String, Object> props = properties != null ? new HashMap<>(properties) : new HashMap<>();
        if (!props.containsKey("uuid")) {
            props.put("uuid", edgeUuid != null ? edgeUuid : java.util.UUID.randomUUID().toString().replace("-", ""));
        }

        Map<String, Object> params = new HashMap<>();
        params.put("graph_id", graphId);
        params.put("sourceUuid", sourceUuid);
        params.put("targetUuid", targetUuid);
        params.put("type", type);
        params.put("fact", fact != null ? fact : "");
        params.put("embedding", embedding != null ? toFloatList(embedding) : null);
        params.put("edgeUuid", props.get("uuid"));
        params.put("props", props);

        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, params);
            if (result.hasNext()) {
                Record record = result.next();
                return record.get("r").asRelationship().asMap();
            }
        }
        return null;
    }

    /**
     * 创建关系边（支持自定义关系类型）
     * @param graphId 图谱ID
     * @param edgeUuid 关系UUID
     * @param sourceUuid 源节点UUID
     * @param targetUuid 目标节点UUID
     * @param relationType Neo4j 关系类型（如 RELATES_TO, HAS_COMMUNITY, NEXT_EPISODE 等）
     * @param type 业务类型
     * @param fact 事实描述
     * @param embedding 嵌入向量
     * @param properties 属性 Map
     * @return 创建的关系信息
     */
    public Map<String, Object> createRelationship(String graphId, String edgeUuid, String sourceUuid,
                                                    String targetUuid, String relationType, String type,
                                                    String fact, float[] embedding, Map<String, Object> properties) {
        String cypher =
            "MATCH (a:Entity {graph_id: $graph_id, uuid: $sourceUuid}) " +
            "MATCH (b:Entity {graph_id: $graph_id, uuid: $targetUuid}) " +
            "CREATE (a)-[r:" + relationType + " {uuid: $edgeUuid, type: $type, fact: $fact, " +
            "embedding: $embedding, valid_at: timestamp(), invalid_at: null}]->(b) " +
            "SET r += $props RETURN r";

        Map<String, Object> props = properties != null ? new HashMap<>(properties) : new HashMap<>();
        if (!props.containsKey("uuid")) {
            props.put("uuid", edgeUuid != null ? edgeUuid : java.util.UUID.randomUUID().toString().replace("-", ""));
        }

        Map<String, Object> params = new HashMap<>();
        params.put("graph_id", graphId);
        params.put("sourceUuid", sourceUuid);
        params.put("targetUuid", targetUuid);
        params.put("type", type);
        params.put("fact", fact != null ? fact : "");
        params.put("embedding", embedding != null ? toFloatList(embedding) : null);
        params.put("props", props);

        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, params);
            if (result.hasNext()) {
                Record record = result.next();
                return record.get("r").asRelationship().asMap();
            }
        }
        return null;
    }

    /**
     * 更新边嵌入向量
     * @param graphId 图谱ID
     * @param uuid 边UUID
     * @param embedding 嵌入向量
     */
    public void updateEdgeEmbedding(String graphId, String uuid, float[] embedding) {
        String cypher = "MATCH ()-[r:RELATES_TO {graph_id: $graph_id, uuid: $uuid}]->() " +
                        "SET r.embedding = $embedding";
        try (Session session = neo4jDriver.session()) {
            session.run(cypher, Values.parameters("graph_id", graphId, "uuid", uuid,
                    "embedding", embedding != null ? toFloatList(embedding) : null));
        }
    }
    
    /**
     * 根据 UUID 查询实体节点
     * @param graphId 图谱ID
     * @param uuid 节点UUID
     * @return 节点信息 Map
     */
    public Map<String, Object> getEntityNode(String graphId, String uuid) {
        String cypher = "MATCH (n:Entity {graph_id: $graph_id, uuid: $uuid}) RETURN n";
        
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, 
                Values.parameters("graph_id", graphId, "uuid", uuid));
            if (result.hasNext()) {
                Record record = result.next();
                return record.get("n").asNode().asMap();
            }
        }
        return null;
    }
    
    /**
     * 查询图谱中的所有节点（分页）
     * @param graphId 图谱ID
     * @param skip 跳过数量
     * @param limit 限制数量
     * @return 节点列表
     */
    public List<Map<String, Object>> listNodes(String graphId, long skip, long limit) {
        String cypher =
            "MATCH (n) WHERE n.graph_id = $graph_id AND (labels(n) = ['Entity'] OR labels(n) = ['Episode']) " +
            "RETURN n, labels(n)[0] as label SKIP $skip LIMIT $limit";

        List<Map<String, Object>> nodes = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher,
                Values.parameters("graph_id", graphId, "skip", skip, "limit", limit));
            while (result.hasNext()) {
                Record record = result.next();
                Map<String, Object> nodeMap = new HashMap<>(record.get("n").asNode().asMap());
                nodeMap.put("label", record.get("label").asString());
                nodes.add(nodeMap);
            }
        }
        return nodes;
    }
    
    /**
     * 查询图谱中的所有关系边（支持过滤和分页）
     * @param graphId 图谱ID
     * @param type 关系类型（可选）
     * @param source 源节点UUID（可选）
     * @param target 目标节点UUID（可选）
     * @param skip 跳过数量
     * @param limit 限制数量
     * @return 关系列表
     */
    public List<Map<String, Object>> listEdges(String graphId, String type, String source, String target, long skip, long limit) {
        StringBuilder cypher = new StringBuilder(
            "MATCH (a:Entity {graph_id: $graph_id})-[r:RELATES_TO]->(b:Entity) "
        );
        
        // 构建 WHERE 条件
        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        params.put("graph_id", graphId);
        
        if (type != null && !type.isEmpty()) {
            conditions.add("r.type = $type");
            params.put("type", type);
        }
        if (source != null && !source.isEmpty()) {
            conditions.add("a.uuid = $source");
            params.put("source", source);
        }
        if (target != null && !target.isEmpty()) {
            conditions.add("b.uuid = $target");
            params.put("target", target);
        }
        
        if (!conditions.isEmpty()) {
            cypher.append("WHERE ");
            cypher.append(String.join(" AND ", conditions));
            cypher.append(" ");
        }
        
        cypher.append("RETURN r, a.uuid as source, b.uuid as target SKIP $skip LIMIT $limit");
        params.put("skip", skip);
        params.put("limit", limit);
        
        List<Map<String, Object>> edges = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher.toString(), params);
            while (result.hasNext()) {
                Record record = result.next();
                Map<String, Object> edge = new HashMap<>(record.get("r").asRelationship().asMap());
                edge.put("source", record.get("source").asString());
                edge.put("target", record.get("target").asString());
                edges.add(edge);
            }
        }
        return edges;
    }
    
    /**
     * 根据 UUID 查询关系边
     * @param graphId 图谱ID
     * @param uuid 边UUID
     * @return 边信息 Map
     */
    public Map<String, Object> getEdgeByUuid(String graphId, String uuid) {
        String cypher = 
            "MATCH (a:Entity {graph_id: $graph_id})-[r:RELATES_TO {uuid: $uuid}]->(b:Entity) " +
            "RETURN r, a.uuid as source, b.uuid as target";
        
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, 
                Values.parameters("graph_id", graphId, "uuid", uuid));
            if (result.hasNext()) {
                Record record = result.next();
                Map<String, Object> edge = new HashMap<>(record.get("r").asRelationship().asMap());
                edge.put("source", record.get("source").asString());
                edge.put("target", record.get("target").asString());
                return edge;
            }
        }
        return null;
    }
    
    /**
     * 删除关系边
     * @param graphId 图谱ID
     * @param uuid 边UUID
     */
    public void deleteEdge(String graphId, String uuid) {
        String cypher = 
            "MATCH (a:Entity {graph_id: $graph_id})-[r:RELATES_TO {uuid: $uuid}]->(b:Entity) " +
            "DELETE r";
        
        try (Session session = neo4jDriver.session()) {
            session.run(cypher, 
                Values.parameters("graph_id", graphId, "uuid", uuid));
        }
    }
    
    // ==================== Episode 相关方法 ====================
    
    /**
     * 统计图谱中的 Episode 数量
     * @param graphId 图谱ID
     * @return Episode 数量
     */
    public long countEpisodesByGraphId(String graphId) {
        String cypher = "MATCH (e:Episode {graph_id: $graph_id}) RETURN count(e) as count";
        
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, Values.parameters("graph_id", graphId));
            if (result.hasNext()) {
                return result.next().get("count").asLong();
            }
        }
        return 0L;
    }
    
    /**
     * 查询图谱中的 Episode 列表（分页）
     * @param graphId 图谱ID
     * @param limit 限制数量
     * @param offset 偏移量
     * @return Episode 列表
     */
    public List<Map<String, Object>> getEpisodesByGraphId(String graphId, int limit, int offset) {
        String cypher =
            "MATCH (e:Episode {graph_id: $graph_id}) " +
            "RETURN e.uuid as uuid, e.name as name, e.source as source, " +
            "e.source_description as source_description, e.content as content, " +
            "e.created_at as created_at, e.valid_at as valid_at, e.graph_id as graph_id, " +
            // V3.0.0 fields
            "e.episode_type as episode_type, " +
            "e.legal_process as legal_process, " +
            "e.stage_label as stage_label, " +
            "e.court_level as court_level, " +
            "e.is_trial_stage as is_trial_stage, " +
            "e.start_time as start_time, " +
            "e.end_time as end_time, " +
            "e.case_id as case_id " +
            "SKIP $offset LIMIT $limit";

        List<Map<String, Object>> episodes = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher,
                Values.parameters("graph_id", graphId, "offset", offset, "limit", limit));
            while (result.hasNext()) {
                Record record = result.next();
                Map<String, Object> episode = new HashMap<>();
                episode.put("uuid", record.get("uuid").asString());
                episode.put("name", record.get("name").asString());
                episode.put("source", record.get("source").asString());
                episode.put("source_description", record.get("source_description").asString());
                episode.put("content", record.get("content").asString());
                episode.put("created_at", record.get("created_at").asLong());
                episode.put("valid_at", record.get("valid_at").asLong());
                episode.put("graph_id", record.get("graph_id").asString());
                // V3.0.0 fields
                episode.put("episode_type", record.get("episode_type").asString(null));
                episode.put("legal_process", record.get("legal_process").asString(null));
                episode.put("stage_label", record.get("stage_label").asString(null));
                episode.put("court_level", record.get("court_level").asString(null));
                episode.put("is_trial_stage", record.get("is_trial_stage").asBoolean(false));
                episode.put("start_time", record.get("start_time").asString(null));
                episode.put("end_time", record.get("end_time").asString(null));
                episode.put("case_id", record.get("case_id").asString(null));
                episodes.add(episode);
            }
        }
        return episodes;
    }
    
    /**
     * 根据 UUID 查询 Episode
     * @param graphId 图谱ID
     * @param episodeUuid Episode UUID
     * @return Episode 信息 Map
     */
    public Map<String, Object> getEpisodeByUuid(String graphId, String episodeUuid) {
        String cypher = 
            "MATCH (e:Episode {graph_id: $graph_id, uuid: $uuid}) " +
            "RETURN e.uuid as uuid, e.name as name, e.source as source, " +
            "e.source_description as source_description, e.content as content, " +
            "e.created_at as created_at, e.valid_at as valid_at, e.graph_id as graph_id, " +
            "e.processed as processed, " +
            // V3.0.0 fields
            "e.episode_type as episode_type, " +
            "e.legal_process as legal_process, " +
            "e.stage_label as stage_label, " +
            "e.court_level as court_level, " +
            "e.is_trial_stage as is_trial_stage, " +
            "e.start_time as start_time, " +
            "e.end_time as end_time, " +
            "e.case_id as case_id";
        
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, 
                Values.parameters("graph_id", graphId, "uuid", episodeUuid));
            if (result.hasNext()) {
                Record record = result.next();
                Map<String, Object> episode = new HashMap<>();
                episode.put("uuid", record.get("uuid").asString());
                episode.put("name", record.get("name").asString());
                episode.put("source", record.get("source").asString());
                episode.put("source_description", record.get("source_description").asString());
                episode.put("content", record.get("content").asString());
                episode.put("created_at", record.get("created_at").asLong());
                episode.put("valid_at", record.get("valid_at").asLong());
                episode.put("graph_id", record.get("graph_id").asString());
                episode.put("processed", record.get("processed").asBoolean());
                // V3.0.0 fields
                episode.put("episode_type", record.get("episode_type").asString(null));
                episode.put("legal_process", record.get("legal_process").asString(null));
                episode.put("stage_label", record.get("stage_label").asString(null));
                episode.put("court_level", record.get("court_level").asString(null));
                episode.put("is_trial_stage", record.get("is_trial_stage").asBoolean(false));
                episode.put("start_time", record.get("start_time").asString(null));
                episode.put("end_time", record.get("end_time").asString(null));
                episode.put("case_id", record.get("case_id").asString(null));
                return episode;
            }
        }
        return null;
    }
    
    /**
     * 查询 Episode 提及的节点和边
     * @param episodeUuid Episode UUID
     * @return 包含 nodes 和 edges 的 Map
     */
    public Map<String, List<Map<String, Object>>> getEpisodeMentions(String episodeUuid) {
        Map<String, List<Map<String, Object>>> result = new HashMap<>();
        List<Map<String, Object>> nodes = new ArrayList<>();
        List<Map<String, Object>> edges = new ArrayList<>();
        
        String cypher = 
            "MATCH (e:Episode {uuid: $uuid})-" +
            "[mentions:MENTIONS]->(n:Entity) " +
            "RETURN n.uuid as uuid, n.name as name, n.type as type, n.summary as summary, " +
            "labels(n) as labels, " +
            "n.courtName as courtName, n.partyName as partyName, " +
            "n.caseName as caseName, n.caseNumber as caseNumber, " +
            "n.articleNumber as articleNumber, n.lawName as lawName, " +
            "n.judgeName as judgeName, n.documentNumber as documentNumber, " +
            "n.agreementNumber as agreementNumber, n.evidenceNumber as evidenceNumber, " +
            "n.reasoning as reasoning, n.factDescription as factDescription";
        
        try (Session session = neo4jDriver.session()) {
            Result nodeResult = session.run(cypher, Values.parameters("uuid", episodeUuid));
            while (nodeResult.hasNext()) {
                Record record = nodeResult.next();
                Map<String, Object> node = new HashMap<>();
                node.put("uuid", record.get("uuid").asString());
                
                // 根据节点类型提取名称（需要显式转换为 Java 类型）
                String nodeType = record.get("type").isNull() ? null : record.get("type").asString();
                Map<String, Object> nodeData = new HashMap<>();
                nodeData.put("courtName", record.get("courtName").isNull() ? null : record.get("courtName").asString());
                nodeData.put("partyName", record.get("partyName").isNull() ? null : record.get("partyName").asString());
                nodeData.put("caseName", record.get("caseName").isNull() ? null : record.get("caseName").asString());
                nodeData.put("caseNumber", record.get("caseNumber").isNull() ? null : record.get("caseNumber").asString());
                nodeData.put("articleNumber", record.get("articleNumber").isNull() ? null : record.get("articleNumber").asString());
                nodeData.put("lawName", record.get("lawName").isNull() ? null : record.get("lawName").asString());
                nodeData.put("judgeName", record.get("judgeName").isNull() ? null : record.get("judgeName").asString());
                nodeData.put("documentNumber", record.get("documentNumber").isNull() ? null : record.get("documentNumber").asString());
                nodeData.put("agreementNumber", record.get("agreementNumber").isNull() ? null : record.get("agreementNumber").asString());
                nodeData.put("evidenceNumber", record.get("evidenceNumber").isNull() ? null : record.get("evidenceNumber").asString());
                nodeData.put("reasoning", record.get("reasoning").isNull() ? null : record.get("reasoning").asString());
                nodeData.put("factDescription", record.get("factDescription").isNull() ? null : record.get("factDescription").asString());
                nodeData.put("name", record.get("name").isNull() ? null : record.get("name").asString());
                nodeData.put("summary", record.get("summary"));
                node.put("name", extractNodeName(nodeType, nodeData));
                node.put("type", nodeType);
                node.put("summary", record.get("summary").isNull() ? null : record.get("summary").asString());
                node.put("labels", record.get("labels").asList());
                nodes.add(node);
            }
        }
        
        // 查询 Episode 直接提及的边（通过 MENTIONS 关系）
        String edgeCypher =
            "MATCH (e:Episode {uuid: $uuid})-[mentions:MENTIONS]->(r) " +
            "WHERE NOT labels(r)[0] IN ['Entity', 'Episode'] " +
            "RETURN r.uuid as uuid, type(mentions) as type, r.fact as fact, " +
            "startNode(mentions).uuid as source_node_uuid, endNode(mentions).uuid as target_node_uuid";

        try (Session session = neo4jDriver.session()) {
            Result edgeResult = session.run(edgeCypher, Values.parameters("uuid", episodeUuid));
            while (edgeResult.hasNext()) {
                Record record = edgeResult.next();
                Map<String, Object> edge = new HashMap<>();
                edge.put("uuid", record.get("uuid").asString());
                edge.put("type", record.get("type").asString());
                edge.put("fact", record.get("fact").asString());
                edge.put("source_node_uuid", record.get("source_node_uuid").asString());
                edge.put("target_node_uuid", record.get("target_node_uuid").asString());
                edges.add(edge);
            }
        }

        result.put("nodes", nodes);
        result.put("edges", edges);
        return result;
    }
    
    /**
     * 删除 Episode
     * @param graphId 图谱ID
     * @param episodeUuid Episode UUID
     */
    public void deleteEpisode(String graphId, String episodeUuid) {
        String cypher = 
            "MATCH (e:Episode {graph_id: $graph_id, uuid: $uuid}) " +
            "DETACH DELETE e";
        
        try (Session session = neo4jDriver.session()) {
            session.run(cypher, 
                Values.parameters("graph_id", graphId, "uuid", episodeUuid));
        }
    }
    
    /**
     * 创建 Episode
     * @param graphId 图谱ID
     * @param uuid Episode UUID
     * @param name Episode 名称
     * @param source 来源类型
     * @param sourceDescription 来源描述
     * @param content Episode 内容
     * @param properties 额外属性
     * @return 创建的 Episode 信息
     */
    public Map<String, Object> createEpisode(String graphId, String uuid, String name, 
            String source, String sourceDescription, String content, 
            Map<String, Object> properties) {
        String cypher = 
            "CREATE (e:Episode {graph_id: $graph_id, uuid: $uuid, name: $name, " +
            "source: $source, source_description: $source_description, " +
            "content: $content, created_at: timestamp(), valid_at: timestamp()}) " +
            "SET e += $props " +
            "RETURN e.uuid as uuid, e.name as name, e.source as source, " +
            "e.source_description as source_description, e.content as content, " +
            "e.created_at as created_at, e.valid_at as valid_at, e.graph_id as graph_id, " +
            // V3.0.0 fields
            "e.episode_type as episode_type, " +
            "e.legal_process as legal_process, " +
            "e.stage_label as stage_label, " +
            "e.court_level as court_level, " +
            "e.is_trial_stage as is_trial_stage, " +
            "e.start_time as start_time, " +
            "e.end_time as end_time, " +
            "e.case_id as case_id";
        
        Map<String, Object> params = new HashMap<>();
        params.put("graph_id", graphId);
        params.put("uuid", uuid);
        params.put("name", name);
        params.put("source", source != null ? source : "text");
        params.put("source_description", sourceDescription != null ? sourceDescription : "");
        params.put("content", content);
        params.put("props", properties != null ? properties : new HashMap<>());
        
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, params);
            if (result.hasNext()) {
                Record record = result.next();
                Map<String, Object> episode = new HashMap<>();
                episode.put("uuid", record.get("uuid").asString());
                episode.put("name", record.get("name").asString());
                episode.put("source", record.get("source").asString());
                episode.put("source_description", record.get("source_description").asString());
                episode.put("content", record.get("content").asString());
                episode.put("created_at", record.get("created_at").asLong());
                episode.put("valid_at", record.get("valid_at").asLong());
                episode.put("graph_id", record.get("graph_id").asString());
                // V3.0.0 fields
                episode.put("episode_type", record.get("episode_type").asString(null));
                episode.put("legal_process", record.get("legal_process").asString(null));
                episode.put("stage_label", record.get("stage_label").asString(null));
                episode.put("court_level", record.get("court_level").asString(null));
                episode.put("is_trial_stage", record.get("is_trial_stage").asBoolean(false));
                episode.put("start_time", record.get("start_time").asString(null));
                episode.put("end_time", record.get("end_time").asString(null));
                episode.put("case_id", record.get("case_id").asString(null));
                return episode;
            }
        }
        return null;
    }
    
    /**
     * 删除实体节点
     * @param graphId 图谱ID
     * @param uuid 节点UUID
     */
    public void deleteEntityNode(String graphId, String uuid) {
        String cypher = 
            "MATCH (n:Entity {graph_id: $graph_id, uuid: $uuid}) " +
            "DETACH DELETE n";
        
        try (Session session = neo4jDriver.session()) {
            session.run(cypher, 
                Values.parameters("graph_id", graphId, "uuid", uuid));
        }
    }
    
    /**
     * 获取图谱统计信息
     * @param graphId 图谱ID
     * @return 统计信息 Map（nodeCount, edgeCount）
     */
    public Map<String, Long> getGraphStats(String graphId) {
        Map<String, Long> stats = new HashMap<>();

        try (Session session = neo4jDriver.session()) {
            // V3.0.0: 统计节点数量（含 Entity 和 Episode）
            Result nodeResult = session.run(
                "MATCH (n) WHERE n.graph_id = $graph_id AND ((labels(n) = ['Entity']) OR (labels(n) = ['Episode'])) RETURN count(n) as nodeCount",
                Values.parameters("graph_id", graphId));
            if (nodeResult.hasNext()) {
                stats.put("nodeCount", nodeResult.next().get("nodeCount").asLong());
            } else {
                stats.put("nodeCount", 0L);
            }

            // 查询关系数量
            Result edgeResult = session.run(
                "MATCH ()-[r:RELATES_TO]->() WHERE r.graph_id = $graph_id RETURN count(r) as edgeCount",
                Values.parameters("graph_id", graphId));
            if (edgeResult.hasNext()) {
                stats.put("edgeCount", edgeResult.next().get("edgeCount").asLong());
            } else {
                stats.put("edgeCount", 0L);
            }

            // V3.0.0: 统计 Episode 节点数量
            Result episodeResult = session.run(
                "MATCH (e:Episode {graph_id: $graph_id}) RETURN count(e) as episodeCount",
                Values.parameters("graph_id", graphId));
            if (episodeResult.hasNext()) {
                stats.put("episodeCount", episodeResult.next().get("episodeCount").asLong());
            } else {
                stats.put("episodeCount", 0L);
            }
        }
        return stats;
    }
    
    /**
     * 清空图谱数据
     * @param graphId 图谱ID
     */
    public void clearGraphData(String graphId) {
        try (Session session = neo4jDriver.session()) {
            session.run(
                "MATCH (n) WHERE n.graph_id = $graph_id AND (labels(n) = ['Entity'] OR labels(n) = ['Episode']) DETACH DELETE n",
                Values.parameters("graph_id", graphId));
        }
    }
    
    // ==================== 搜索方法 ====================
    
    /**
     * 全文搜索边（关系）
     * @param query 搜索关键词
     * @param graphId 图谱ID
     * @param limit 限制数量
     * @return 边列表
     */
    public List<Map<String, Object>> searchEdgesByFulltext(String query, String graphId, int limit) {
        // 使用 Neo4j 全文索引搜索（需要先创建索引）
        // CREATE FULLTEXT INDEX edgeFactIndex FOR ()-[r:RELATES_TO]-() ON EACH [r.fact]
        String cypher = 
            "CALL db.index.fulltext.queryRelationships('edgeFactIndex', $query) " +
            "YIELD relationship, score " +
            "WHERE relationship.graph_id = $graph_id " +
            "WITH relationship, score " +
            "MATCH (a:Entity {graph_id: $graph_id})-[r]-() " +
            "WHERE elementId(r) = elementId(relationship) " +
            "RETURN r.uuid as uuid, r.fact as fact, r.type as type, " +
            "r.graph_id as graph_id, score " +
            "LIMIT $limit";
        
        List<Map<String, Object>> results = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, 
                Values.parameters("query", query + "*", "graph_id", graphId, "limit", limit));
            while (result.hasNext()) {
                Record record = result.next();
                Map<String, Object> edge = new HashMap<>();
                edge.put("uuid", record.get("uuid").asString());
                edge.put("fact", record.get("fact").asString());
                edge.put("type", record.get("type").asString());
                edge.put("graph_id", record.get("graph_id").asString());
                edge.put("score", record.get("score").asDouble());
                results.add(edge);
            }
        } catch (Exception e) {
            log.warn("全文搜索边失败（可能索引未创建）：{}", e.getMessage());
        }
        return results;
    }
    
    /**
     * 全文搜索节点（实体）
     * @param query 搜索关键词
     * @param graphId 图谱ID
     * @param limit 限制数量
     * @return 节点列表
     */
    public List<Map<String, Object>> searchNodesByFulltext(String query, String graphId, int limit) {
        // 使用 Neo4j 全文索引搜索（需要先创建索引）
        // CREATE FULLTEXT INDEX nodeNameIndex FOR (n:Entity) ON EACH [n.name, n.summary]
        String cypher = 
            "CALL db.index.fulltext.queryNodes('nodeNameIndex', $query) " +
            "YIELD node, score " +
            "WHERE node.graph_id = $graph_id " +
            "RETURN node.uuid as uuid, node.name as name, node.summary as summary, " +
            "node.type as type, node.graph_id as graph_id, score, " +
            "node.courtName as courtName, node.partyName as partyName, " +
            "node.caseName as caseName, node.caseNumber as caseNumber, " +
            "node.articleNumber as articleNumber, node.lawName as lawName, " +
            "node.judgeName as judgeName, node.documentNumber as documentNumber, " +
            "node.agreementNumber as agreementNumber, node.evidenceNumber as evidenceNumber, " +
            "node.reasoning as reasoning, node.factDescription as factDescription " +
            "LIMIT $limit";
        
        List<Map<String, Object>> results = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, 
                Values.parameters("query", query + "*", "graph_id", graphId, "limit", limit));
            while (result.hasNext()) {
                Record record = result.next();
                Map<String, Object> node = new HashMap<>();
                node.put("uuid", record.get("uuid").asString());
                node.put("type", record.get("type").asString());
                node.put("summary", record.get("summary").isNull() ? null : record.get("summary").asString());
                node.put("graph_id", record.get("graph_id").asString());
                node.put("score", record.get("score").asDouble());
                
                // 根据类型提取名称
                Map<String, Object> nodeData = new HashMap<>();
                nodeData.put("courtName", record.get("courtName"));
                nodeData.put("partyName", record.get("partyName"));
                nodeData.put("caseName", record.get("caseName"));
                nodeData.put("caseNumber", record.get("caseNumber"));
                nodeData.put("articleNumber", record.get("articleNumber"));
                nodeData.put("lawName", record.get("lawName"));
                nodeData.put("judgeName", record.get("judgeName"));
                nodeData.put("documentNumber", record.get("documentNumber"));
                nodeData.put("agreementNumber", record.get("agreementNumber"));
                nodeData.put("evidenceNumber", record.get("evidenceNumber"));
                nodeData.put("reasoning", record.get("reasoning"));
                nodeData.put("factDescription", record.get("factDescription"));
                nodeData.put("name", record.get("name"));
                nodeData.put("summary", record.get("summary"));
                node.put("name", extractNodeName(record.get("type").asString(), nodeData));
                
                results.add(node);
            }
        } catch (Exception e) {
            log.warn("全文搜索节点失败（可能索引未创建）：{}", e.getMessage());
        }
        return results;
    }
    
    // ==================== 向量搜索方法 ====================

    /**
     * 向量相似度搜索节点
     * @param graphId 图谱ID
     * @param embedding 查询向量
     * @param limit 返回数量限制
     * @return 节点列表（含 similarity 分数）
     */
    public List<Map<String, Object>> searchNodesByVector(String graphId, float[] embedding, int limit) {
        String cypher =
            "CALL db.index.vector.queryNodes('node_embedding_index', $k, $embedding) " +
            "YIELD node, score " +
            "WHERE node.graph_id = $graph_id " +
            "RETURN node.uuid as uuid, node.name as name, node.type as type, " +
            "node.summary as summary, score, " +
            "node.courtName as courtName, node.partyName as partyName, " +
            "node.caseName as caseName, node.caseNumber as caseNumber, " +
            "node.articleNumber as articleNumber, node.lawName as lawName, " +
            "node.judgeName as judgeName, node.documentNumber as documentNumber, " +
            "node.agreementNumber as agreementNumber, node.evidenceNumber as evidenceNumber, " +
            "node.reasoning as reasoning, node.factDescription as factDescription " +
            "LIMIT $limit";

        List<Map<String, Object>> results = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher,
                Values.parameters("graph_id", graphId, "k", limit,
                                  "embedding", toFloatList(embedding), "limit", limit));
            while (result.hasNext()) {
                Record record = result.next();
                Map<String, Object> node = new HashMap<>();
                node.put("uuid", record.get("uuid").asString());
                node.put("type", record.get("type").asString());
                node.put("summary", record.get("summary").isNull() ? null : record.get("summary").asString());
                node.put("score", record.get("score").asDouble());
                
                // 根据类型提取名称
                Map<String, Object> nodeData = new HashMap<>();
                nodeData.put("courtName", record.get("courtName"));
                nodeData.put("partyName", record.get("partyName"));
                nodeData.put("caseName", record.get("caseName"));
                nodeData.put("caseNumber", record.get("caseNumber"));
                nodeData.put("articleNumber", record.get("articleNumber"));
                nodeData.put("lawName", record.get("lawName"));
                nodeData.put("judgeName", record.get("judgeName"));
                nodeData.put("documentNumber", record.get("documentNumber"));
                nodeData.put("agreementNumber", record.get("agreementNumber"));
                nodeData.put("evidenceNumber", record.get("evidenceNumber"));
                nodeData.put("reasoning", record.get("reasoning"));
                nodeData.put("factDescription", record.get("factDescription"));
                nodeData.put("name", record.get("name"));
                nodeData.put("summary", record.get("summary"));
                node.put("name", extractNodeName(record.get("type").asString(), nodeData));
                
                results.add(node);
            }
        } catch (Exception e) {
            log.warn("向量搜索节点失败（可能索引未创建）：{}", e.getMessage());
        }
        return results;
    }

    /**
     * 向量相似度搜索边
     * @param graphId 图谱ID
     * @param embedding 查询向量
     * @param limit 返回数量限制
     * @return 边列表（含 similarity 分数）
     */
    public List<Map<String, Object>> searchEdgesByVector(String graphId, float[] embedding, int limit) {
        String cypher =
            "CALL db.index.vector.queryRelationships('edge_embedding_index', $k, $embedding) " +
            "YIELD relationship, score " +
            "WHERE relationship.graph_id = $graph_id " +
            "RETURN relationship.uuid as uuid, relationship.fact as fact, relationship.type as type, " +
            "score LIMIT $limit";

        List<Map<String, Object>> results = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher,
                Values.parameters("graph_id", graphId, "k", limit,
                                  "embedding", toFloatList(embedding), "limit", limit));
            while (result.hasNext()) {
                Record record = result.next();
                Map<String, Object> edge = new HashMap<>();
                edge.put("uuid", record.get("uuid").asString());
                edge.put("fact", record.get("fact").asString());
                edge.put("type", record.get("type").asString());
                edge.put("score", record.get("score").asDouble());
                results.add(edge);
            }
        } catch (Exception e) {
            log.warn("向量搜索边失败（可能索引未创建）：{}", e.getMessage());
        }
        return results;
    }

    /**
     * 初始化向量索引（节点 + 边）
     * 应在应用启动时调用一次
     */
    public void initVectorIndexes(int nodeDimensions, int edgeDimensions) {
        try (Session session = neo4jDriver.session()) {
            // 节点向量索引
            session.run(
                "CREATE VECTOR INDEX node_embedding_index IF NOT EXISTS " +
                "FOR (n:Entity) ON (n.embedding) " +
                "OPTIONS {indexConfig: {`vector.dimensions`: $dim, `vector.similarity_function`: 'cosine'}}",
                Values.parameters("dim", nodeDimensions)
            );
            log.info("节点向量索引创建/确认完成，维度：{}", nodeDimensions);

            // 边向量索引
            session.run(
                "CREATE VECTOR INDEX edge_embedding_index IF NOT EXISTS " +
                "FOR ()-[r:RELATES_TO]-() ON (r.embedding) " +
                "OPTIONS {indexConfig: {`vector.dimensions`: $dim, `vector.similarity_function`: 'cosine'}}",
                Values.parameters("dim", edgeDimensions)
            );
            log.info("边向量索引创建/确认完成，维度：{}", edgeDimensions);
        } catch (Exception e) {
            log.error("向量索引初始化失败：{}", e.getMessage(), e);
            throw new RuntimeException("向量索引初始化失败", e);
        }
    }

    // ==================== 工具方法 ====================

    /**
     * float[] 转 List<Float>（Neo4j 驱动要求）
     */
    private List<Float> toFloatList(float[] arr) {
        if (arr == null) return null;
        List<Float> list = new ArrayList<>(arr.length);
        for (float v : arr) {
            list.add(v);
        }
        return list;
    }

    /**
     * 根据 UUID 查询节点
     * @param uuid 节点UUID
     * @return 节点信息 Map
     */
    public Map<String, Object> getNodeByUuid(String uuid) {
        String cypher = "MATCH (n:Entity {uuid: $uuid}) RETURN n";
        
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, Values.parameters("uuid", uuid));
            if (result.hasNext()) {
                Record record = result.next();
                Map<String, Object> node = new HashMap<>(record.get("n").asNode().asMap());
                node.put("labels", record.get("n").asNode().labels());
                return node;
            }
        }
        return null;
    }

    // ==================== 时序管理方法 ====================

    /**
     * 失效指定名称的实体节点（将 invalid_at 设为当前时间）
     * @param graphId 图谱ID
     * @param entityNames 实体名称列表
     */
    public void invalidateNodesByName(String graphId, List<String> entityNames) {
        String cypher =
            "MATCH (n:Entity {graph_id: $graph_id}) " +
            "WHERE n.name IN $names AND n.invalid_at IS NULL " +
            "SET n.invalid_at = timestamp()";
        try (Session session = neo4jDriver.session()) {
            session.run(cypher, Values.parameters("graph_id", graphId, "names", entityNames));
            log.info("失效节点：graphId={}, names={}", graphId, entityNames);
        }
    }

    /**
     * 失效与指定节点相关的边
     * @param graphId 图谱ID
     * @param nodeUuids 节点UUID列表
     */
    public void invalidateEdgesByNodes(String graphId, List<String> nodeUuids) {
        String cypher =
            "MATCH (a:Entity {graph_id: $graph_id})-[r:RELATES_TO]->(b:Entity {graph_id: $graph_id}) " +
            "WHERE (a.uuid IN $uuids OR b.uuid IN $uuids) AND r.invalid_at IS NULL " +
            "SET r.invalid_at = timestamp()";
        try (Session session = neo4jDriver.session()) {
            session.run(cypher, Values.parameters("graph_id", graphId, "uuids", nodeUuids));
            log.info("失效边：graphId={}, nodeUuids={}", graphId, nodeUuids);
        }
    }

    /**
     * 查询当前有效的节点
     * @param graphId 图谱ID
     * @return 有效节点列表
     */
    public List<Map<String, Object>> getValidNodes(String graphId) {
        String cypher =
            "MATCH (n:Entity {graph_id: $graph_id}) " +
            "WHERE n.invalid_at IS NULL " +
            "RETURN n.uuid as uuid, n.name as name, n.type as type, n.summary as summary, " +
            "n.valid_at as valid_at";
        return executeNodeQuery(cypher, graphId);
    }

    /**
     * 查询指定时间点的有效节点
     * @param graphId 图谱ID
     * @param referenceTime 参考时间戳（毫秒）
     * @return 有效节点列表
     */
    public List<Map<String, Object>> getValidNodesAt(String graphId, long referenceTime) {
        String cypher =
            "MATCH (n:Entity {graph_id: $graph_id}) " +
            "WHERE n.valid_at <= $refTime AND (n.invalid_at IS NULL OR n.invalid_at > $refTime) " +
            "RETURN n.uuid as uuid, n.name as name, n.type as type, n.summary as summary, " +
            "n.valid_at as valid_at, n.invalid_at as invalid_at";
        return executeNodeQuery(cypher, graphId, referenceTime);
    }

    /**
     * 查询指定时间点的有效边
     * @param graphId 图谱ID
     * @param referenceTime 参考时间戳（毫秒）
     * @return 有效边列表
     */
    public List<Map<String, Object>> getValidEdgesAt(String graphId, long referenceTime) {
        String cypher =
            "MATCH (a:Entity {graph_id: $graph_id})-[r:RELATES_TO]->(b:Entity {graph_id: $graph_id}) " +
            "WHERE r.valid_at <= $refTime AND (r.invalid_at IS NULL OR r.invalid_at > $refTime) " +
            "RETURN r.uuid as uuid, r.fact as fact, r.type as type, " +
            "a.uuid as source, b.uuid as target, r.valid_at as valid_at";

        List<Map<String, Object>> results = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher,
                Values.parameters("graph_id", graphId, "refTime", referenceTime));
            while (result.hasNext()) {
                results.add(result.next().asMap());
            }
        }
        return results;
    }

    /**
     * 查询实体的历史版本（所有时间线）
     * @param graphId 图谱ID
     * @param entityName 实体名称
     * @return 历史版本列表
     */
    public List<Map<String, Object>> getFactVersions(String graphId, String entityName) {
        String cypher =
            "MATCH (n:Entity {graph_id: $graph_id, name: $name}) " +
            "RETURN n.uuid as uuid, n.name as name, n.type as type, n.summary as summary, " +
            "n.valid_at as valid_at, n.invalid_at as invalid_at " +
            "ORDER BY n.valid_at DESC";

        List<Map<String, Object>> results = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher,
                Values.parameters("graph_id", graphId, "name", entityName));
            while (result.hasNext()) {
                results.add(result.next().asMap());
            }
        }
        return results;
    }

    // ==================== 内部工具方法 ====================

    private List<Map<String, Object>> executeNodeQuery(String cypher, String graphId) {
        List<Map<String, Object>> results = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, Values.parameters("graph_id", graphId));
            while (result.hasNext()) {
                results.add(result.next().asMap());
            }
        }
        return results;
    }

    private List<Map<String, Object>> executeNodeQuery(String cypher, String graphId, long refTime) {
        List<Map<String, Object>> results = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher,
                Values.parameters("graph_id", graphId, "refTime", refTime));
            while (result.hasNext()) {
                results.add(result.next().asMap());
            }
        }
        return results;
    }

    public Map<String, Object> getEdgeByUuidOnly(String uuid) {
        String cypher =
            "MATCH (a)-[r {uuid: $uuid}]->(b) " +
            "RETURN r, a.uuid as source, b.uuid as target, type(r) as edgeType";

        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, Values.parameters("uuid", uuid));
            if (result.hasNext()) {
                Record record = result.next();
                Map<String, Object> edge = new HashMap<>(record.get("r").asRelationship().asMap());
                edge.put("source", record.get("source").asString());
                edge.put("target", record.get("target").asString());
                edge.put("edgeType", record.get("edgeType").asString());
                return edge;
            }
        }
        return null;
    }

    // ==================== 克隆与导出方法 ====================

    /**
     * 克隆图谱数据（将源 graph_id 的节点/边复制到目标 graph_id）
     * @param sourceGraphId 源图谱ID
     * @param targetGraphId 目标图谱ID
     */
    public void cloneGraphData(String sourceGraphId, String targetGraphId) {
        // 1. 克隆节点
        String cloneNodesCypher =
            "MATCH (n:Entity {graph_id: $source_id}) " +
            "CREATE (m:Entity) SET m = properties(n), m.graph_id = $target_id";
        try (Session session = neo4jDriver.session()) {
            session.run(cloneNodesCypher, Values.parameters("source_id", sourceGraphId, "target_id", targetGraphId));
        }

        // 2. 克隆边
        String cloneEdgesCypher =
            "MATCH (a:Entity {graph_id: $source_id})-[r:RELATES_TO]->(b:Entity {graph_id: $source_id}) " +
            "MATCH (na:Entity {graph_id: $target_id, uuid: a.uuid}) " +
            "MATCH (nb:Entity {graph_id: $target_id, uuid: b.uuid}) " +
            "CREATE (na)-[nr:RELATES_TO]->(nb) SET nr = properties(r), nr.uuid = apoc.create.uuid()";
        try (Session session = neo4jDriver.session()) {
            session.run(cloneEdgesCypher, Values.parameters("source_id", sourceGraphId, "target_id", targetGraphId));
        }

        log.info("图谱数据克隆完成：source={}, target={}", sourceGraphId, targetGraphId);
    }

    /**
     * 获取图谱的所有节点
     * @param graphId 图谱ID
     * @return 节点列表
     */
    public List<Map<String, Object>> getNodesByGraphId(String graphId) {
        String cypher =
            "MATCH (n:Entity {graph_id: $graph_id}) " +
            "RETURN n.uuid as uuid, n.name as name, n.type as type, n.summary as summary";
        return executeNodeQuery(cypher, graphId);
    }

    /**
     * 获取图谱的所有边
     * @param graphId 图谱ID
     * @return 边列表
     */
    public List<Map<String, Object>> getEdgesByGraphId(String graphId) {
        String cypher =
            "MATCH (a:Entity {graph_id: $graph_id})-[r:RELATES_TO]->(b:Entity {graph_id: $graph_id}) " +
            "RETURN r.uuid as uuid, r.fact as fact, r.type as type, " +
            "a.uuid as source, b.uuid as target";
        List<Map<String, Object>> results = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, Values.parameters("graph_id", graphId));
            while (result.hasNext()) {
                results.add(result.next().asMap());
            }
        }
        return results;
    }

    // ==================== 过滤查询方法 ====================

    /**
     * 按图谱ID和过滤条件查询节点（支持 label 过滤 + 时间范围）
     * @param graphId 图谱ID
     * @param labels Neo4j 标签列表（如 ["Entity", "Episode"]）
     * @param createdAfter 创建时间下限（毫秒）
     * @param createdBefore 创建时间上限（毫秒）
     * @param skip 跳过数量
     * @param limit 限制数量
     * @return 节点列表
     */
    public List<Map<String, Object>> findNodes(String graphId, List<String> labels,
                                                Long createdAfter, Long createdBefore,
                                                long skip, long limit) {
        StringBuilder cypher = new StringBuilder();
        cypher.append("MATCH (n) ");
        cypher.append("WHERE n.graph_id = $graph_id ");

        if (labels != null && !labels.isEmpty()) {
            String labelMatch = labels.stream()
                .map(l -> "'" + l + "'")
                .collect(java.util.stream.Collectors.joining(","));
            cypher.append("AND (labels(n) = [").append(labelMatch).append("]) ");
        }

        Map<String, Object> params = new HashMap<>();
        params.put("graph_id", graphId);

        if (createdAfter != null) {
            cypher.append("AND n.created_at >= $createdAfter ");
            params.put("createdAfter", createdAfter);
        }
        if (createdBefore != null) {
            cypher.append("AND n.created_at <= $createdBefore ");
            params.put("createdBefore", createdBefore);
        }

        cypher.append("RETURN n, labels(n)[0] as label SKIP $skip LIMIT $limit");
        params.put("skip", skip);
        params.put("limit", limit);

        List<Map<String, Object>> nodes = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher.toString(), params);
            while (result.hasNext()) {
                Record record = result.next();
                Map<String, Object> nodeMap = new HashMap<>(record.get("n").asNode().asMap());
                nodeMap.put("label", record.get("label").asString());
                nodes.add(nodeMap);
            }
        }
        return nodes;
    }

    /**
     * 按图谱ID和过滤条件查询边（支持边类型过滤 + 时间范围）
     * @param graphId 图谱ID
     * @param edgeTypes 边类型列表
     * @param createdAfter 创建时间下限（毫秒）
     * @param createdBefore 创建时间上限（毫秒）
     * @param skip 跳过数量
     * @param limit 限制数量
     * @return 边列表
     */
    public List<Map<String, Object>> findEdges(String graphId, List<String> edgeTypes,
                                                Long createdAfter, Long createdBefore,
                                                long skip, long limit) {
        StringBuilder cypher = new StringBuilder();
        cypher.append("MATCH (a:Entity {graph_id: $graph_id})-[r]->(b:Entity {graph_id: $graph_id}) ");

        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        params.put("graph_id", graphId);

        if (edgeTypes != null && !edgeTypes.isEmpty()) {
            conditions.add("type(r) IN $edgeTypes");
            params.put("edgeTypes", edgeTypes);
        }
        if (createdAfter != null) {
            conditions.add("r.created_at >= $createdAfter");
            params.put("createdAfter", createdAfter);
        }
        if (createdBefore != null) {
            conditions.add("r.created_at <= $createdBefore");
            params.put("createdBefore", createdBefore);
        }

        if (!conditions.isEmpty()) {
            cypher.append("WHERE ").append(String.join(" AND ", conditions)).append(" ");
        }

        cypher.append("RETURN r, type(r) as edgeType, a.uuid as source, b.uuid as target SKIP $skip LIMIT $limit");
        params.put("skip", skip);
        params.put("limit", limit);

        List<Map<String, Object>> edges = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher.toString(), params);
            while (result.hasNext()) {
                Record record = result.next();
                Map<String, Object> edge = new HashMap<>(record.get("r").asRelationship().asMap());
                edge.put("source", record.get("source").asString());
                edge.put("target", record.get("target").asString());
                edge.put("edgeType", record.get("edgeType").asString());
                edges.add(edge);
            }
        }
        return edges;
    }

    /**
     * 查询两节点间的所有边（双向）
     * @param sourceUuid 源节点UUID
     * @param targetUuid 目标节点UUID
     * @return 边列表
     */
    public List<Map<String, Object>> getEdgesBetweenNodes(String sourceUuid, String targetUuid) {
        String cypher =
            "MATCH (a:Entity {uuid: $sourceUuid})-[r]->(b:Entity {uuid: $targetUuid}) " +
            "RETURN r, type(r) as edgeType, a.uuid as source, b.uuid as target " +
            "UNION ALL " +
            "MATCH (a:Entity {uuid: $targetUuid})-[r]->(b:Entity {uuid: $sourceUuid}) " +
            "RETURN r, type(r) as edgeType, a.uuid as source, b.uuid as target";

        List<Map<String, Object>> edges = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher,
                Values.parameters("sourceUuid", sourceUuid, "targetUuid", targetUuid));
            while (result.hasNext()) {
                Record record = result.next();
                Map<String, Object> edge = new HashMap<>(record.get("r").asRelationship().asMap());
                edge.put("source", record.get("source").asString());
                edge.put("target", record.get("target").asString());
                edge.put("edgeType", record.get("edgeType").asString());
                edges.add(edge);
            }
        }
        return edges;
    }

    /**
     * 获取节点关联的所有边（双向）
     * @param nodeUuid 节点UUID
     * @param skip 跳过数量
     * @param limit 限制数量
     * @return 边列表
     */
    public List<Map<String, Object>> getNodeEdges(String nodeUuid, long skip, long limit) {
        // 双向查询：节点作为 source 或 target 的所有边
        String cypher =
            "MATCH (n:Entity {uuid: $nodeUuid})-[r]->(m:Entity) " +
            "WITH n, r, m, type(r) as edgeType " +
            "RETURN r, edgeType, n.uuid as source, m.uuid as target " +
            "UNION ALL " +
            "MATCH (n:Entity {uuid: $nodeUuid})<-[r]-(m:Entity) " +
            "WITH n, r, m, type(r) as edgeType " +
            "RETURN r, edgeType, m.uuid as source, n.uuid as target " +
            "SKIP $skip LIMIT $limit";

        List<Map<String, Object>> edges = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher,
                Values.parameters("nodeUuid", nodeUuid, "skip", skip, "limit", limit));
            while (result.hasNext()) {
                Record record = result.next();
                Map<String, Object> edge = new HashMap<>(record.get("r").asRelationship().asMap());
                edge.put("source", record.get("source").asString());
                edge.put("target", record.get("target").asString());
                edge.put("edgeType", record.get("edgeType").asString());
                edges.add(edge);
            }
        }
        return edges;
    }

    /**
     * 获取节点关联的 Episode 列表（通过 MENTIONS 关系）
     * @param nodeUuid 节点UUID
     * @param skip 跳过数量
     * @param limit 限制数量
     * @return Episode 列表
     */
    public List<Map<String, Object>> getNodeEpisodes(String nodeUuid, long skip, long limit) {
        String cypher =
            "MATCH (e:Episode)-[:MENTIONS]->(n:Entity {uuid: $nodeUuid}) " +
            "RETURN e.uuid as uuid, e.name as name, e.source as source, " +
            "e.content as content, e.created_at as created_at " +
            "SKIP $skip LIMIT $limit";

        List<Map<String, Object>> episodes = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher,
                Values.parameters("nodeUuid", nodeUuid, "skip", skip, "limit", limit));
            while (result.hasNext()) {
                Record record = result.next();
                Map<String, Object> ep = new HashMap<>();
                ep.put("uuid", record.get("uuid").asString());
                ep.put("name", record.get("name").asString());
                ep.put("source", record.get("source").asString());
                ep.put("content", record.get("content").asString());
                Object createdAt = record.get("created_at");
                if (createdAt != null) {
                    ep.put("created_at", createdAt);
                }
                episodes.add(ep);
            }
        }
        return episodes;
    }

    /**
     * 获取最近的 Episode 列表（按创建时间倒序）
     * @param graphId 图谱ID
     * @param lastN 返回数量
     * @return Episode 列表
     */
    public List<Map<String, Object>> getRecentEpisodes(String graphId, int lastN) {
        String cypher =
            "MATCH (e:Episode {graph_id: $graph_id}) " +
            "RETURN e.uuid as uuid, e.name as name, e.source as source, " +
            "e.source_description as source_description, e.content as content, " +
            "e.created_at as created_at, e.valid_at as valid_at " +
            "ORDER BY e.created_at DESC " +
            "LIMIT $lastN";

        List<Map<String, Object>> episodes = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher,
                Values.parameters("graph_id", graphId, "lastN", lastN));
            while (result.hasNext()) {
                Record record = result.next();
                Map<String, Object> ep = new HashMap<>();
                ep.put("uuid", record.get("uuid").asString());
                ep.put("name", record.get("name").asString());
                ep.put("source", record.get("source").asString());
                ep.put("source_description", record.get("source_description").asString());
                ep.put("content", record.get("content").asString());
                Object createdAt = record.get("created_at");
                if (createdAt != null) ep.put("created_at", createdAt);
                Object validAt = record.get("valid_at");
                if (validAt != null) ep.put("valid_at", validAt);
                episodes.add(ep);
            }
        }
        return episodes;
    }

    /**
     * 统计图谱中的节点总数
     * @param graphId 图谱ID
     * @param labels 标签列表（可选）
     * @return 节点数量
     */
    public long countNodes(String graphId, List<String> labels) {
        StringBuilder cypher = new StringBuilder();
        cypher.append("MATCH (n) WHERE n.graph_id = $graph_id ");
        if (labels != null && !labels.isEmpty()) {
            String labelMatch = labels.stream()
                .map(l -> "'" + l + "'")
                .collect(java.util.stream.Collectors.joining(","));
            cypher.append("AND (labels(n) = [").append(labelMatch).append("]) ");
        }
        cypher.append("RETURN count(n) as count");

        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher.toString(),
                Values.parameters("graph_id", graphId));
            if (result.hasNext()) {
                return result.next().get("count").asLong();
            }
        }
        return 0L;
    }

    /**
     * 统计图谱中的边总数
     * @param graphId 图谱ID
     * @param edgeTypes 边类型列表（可选）
     * @return 边数量
     */
    public long countEdges(String graphId, List<String> edgeTypes) {
        StringBuilder cypher = new StringBuilder();
        cypher.append("MATCH ()-[r]->() WHERE r.graph_id = $graph_id ");
        if (edgeTypes != null && !edgeTypes.isEmpty()) {
            cypher.append("AND type(r) IN $edgeTypes ");
        }
        cypher.append("RETURN count(r) as count");

        Map<String, Object> params = new HashMap<>();
        params.put("graph_id", graphId);
        if (edgeTypes != null && !edgeTypes.isEmpty()) {
            params.put("edgeTypes", edgeTypes);
        }

        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher.toString(), params);
            if (result.hasNext()) {
                return result.next().get("count").asLong();
            }
        }
        return 0L;
    }
    
    /**
     * 根据节点类型提取对应的名称属性
     * 不同类型的节点使用不同的 name 属性
     */
    private String extractNodeName(String type, Map<String, Object> nodeMap) {
        if (type == null) {
            return null;
        }
        
        return switch (type) {
            case "Court" -> (String) nodeMap.get("courtName");
            case "Party" -> (String) nodeMap.get("partyName");
            case "Case" -> (String) nodeMap.getOrDefault("caseName", nodeMap.get("caseNumber"));
            case "LegalProvision" -> {
                String articleNumber = (String) nodeMap.get("articleNumber");
                String lawName = (String) nodeMap.get("lawName");
                yield articleNumber != null && lawName != null 
                    ? lawName + " " + articleNumber 
                    : articleNumber != null ? articleNumber : lawName;
            }
            case "Judge" -> (String) nodeMap.get("judgeName");
            case "JudgmentDocument" -> (String) nodeMap.get("documentNumber");
            case "MediationAgreement" -> (String) nodeMap.get("agreementNumber");
            case "CommercialMediationOrganization" -> (String) nodeMap.get("name");
            case "Mediator" -> (String) nodeMap.get("name");
            case "Evidence" -> (String) nodeMap.get("evidenceNumber");
            case "CaseReasoning" -> {
                String reasoning = (String) nodeMap.get("reasoning");
                yield reasoning != null && reasoning.length() > 50 
                    ? reasoning.substring(0, 50) + "..." 
                    : reasoning;
            }
            case "CaseFact" -> {
                String description = (String) nodeMap.get("factDescription");
                yield description != null && description.length() > 50 
                    ? description.substring(0, 50) + "..." 
                    : description;
            }
            default -> {
                String name = (String) nodeMap.get("name");
                if (name == null || name.isBlank()) {
                    String summary = (String) nodeMap.get("summary");
                    name = summary != null && summary.length() > 50 
                        ? summary.substring(0, 50) + "..." 
                        : summary;
                }
                yield name;
            }
        };
    }
    
    /**
     * 获取所有类型专属的名称字段列表，用于在写入节点时排除 properties 中可能传入的同名字段
     */
    private List<String> getAllTypeNameFields() {
        return List.of(
            "courtName", "partyName", "caseName", "caseNumber",
            "articleNumber", "lawName", "judgeName", "documentNumber",
            "agreementNumber", "evidenceNumber", "reasoning", "factDescription",
            "name"
        );
    }
}
