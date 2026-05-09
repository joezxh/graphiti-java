package com.graphiti.module.graphiti.service;

import com.graphiti.module.graphiti.config.GraphNeo4jConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Result;
import org.neo4j.driver.Record;
import org.neo4j.driver.Value;
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
    
    /**
     * 创建实体节点
     * @param graphId 图谱ID（用作 group_id）
     * @param uuid 节点UUID
     * @param name 节点名称
     * @param type 节点类型（实体类型）
     * @param properties 属性 Map
     * @return 创建的节点信息
     */
    public Map<String, Object> createEntityNode(String graphId, String uuid, String name, String type, Map<String, Object> properties) {
        String cypher = "CREATE (n:Entity {group_id: $group_id, uuid: $uuid, name: $name, type: $type}) SET n += $props RETURN n";
        
        Map<String, Object> params = new HashMap<>();
        params.put("group_id", graphId);
        params.put("uuid", uuid);
        params.put("name", name);
        params.put("type", type);
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
     * 创建关系边
     * @param graphId 图谱ID
     * @param edgeUuid 关系UUID（可选，为 null 时自动生成）
     * @param sourceUuid 源节点UUID
     * @param targetUuid 目标节点UUID
     * @param type 关系类型
     * @param properties 属性 Map
     * @return 创建的关系信息
     */
    public Map<String, Object> createRelationship(String graphId, String edgeUuid, String sourceUuid, String targetUuid, String type, Map<String, Object> properties) {
        String cypher =
            "MATCH (a:Entity {group_id: $group_id, uuid: $sourceUuid}) " +
            "MATCH (b:Entity {group_id: $group_id, uuid: $targetUuid}) " +
            "CREATE (a)-[r:RELATES_TO {uuid: $edgeUuid, type: $type}]- SET r += $props RETURN r";

        Map<String, Object> props = properties != null ? new HashMap<>(properties) : new HashMap<>();
        if (!props.containsKey("uuid")) {
            props.put("uuid", edgeUuid != null ? edgeUuid : java.util.UUID.randomUUID().toString().replace("-", ""));
        }

        Map<String, Object> params = new HashMap<>();
        params.put("group_id", graphId);
        params.put("sourceUuid", sourceUuid);
        params.put("targetUuid", targetUuid);
        params.put("type", type);
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
     * 根据 UUID 查询实体节点
     * @param graphId 图谱ID
     * @param uuid 节点UUID
     * @return 节点信息 Map
     */
    public Map<String, Object> getEntityNode(String graphId, String uuid) {
        String cypher = "MATCH (n:Entity {group_id: $group_id, uuid: $uuid}) RETURN n";
        
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, 
                Values.parameters("group_id", graphId, "uuid", uuid));
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
        String cypher = "MATCH (n:Entity {group_id: $group_id}) RETURN n SKIP $skip LIMIT $limit";
        
        List<Map<String, Object>> nodes = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, 
                Values.parameters("group_id", graphId, "skip", skip, "limit", limit));
            while (result.hasNext()) {
                Record record = result.next();
                nodes.add(record.get("n").asNode().asMap());
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
            "MATCH (a:Entity {group_id: $group_id})-[r:RELATES_TO]->(b:Entity) "
        );
        
        // 构建 WHERE 条件
        List<String> conditions = new ArrayList<>();
        Map<String, Object> params = new HashMap<>();
        params.put("group_id", graphId);
        
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
            "MATCH (a:Entity {group_id: $group_id})-[r:RELATES_TO {uuid: $uuid}]->(b:Entity) " +
            "RETURN r, a.uuid as source, b.uuid as target";
        
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, 
                Values.parameters("group_id", graphId, "uuid", uuid));
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
            "MATCH (a:Entity {group_id: $group_id})-[r:RELATES_TO {uuid: $uuid}]->(b:Entity) " +
            "DELETE r";
        
        try (Session session = neo4jDriver.session()) {
            session.run(cypher, 
                Values.parameters("group_id", graphId, "uuid", uuid));
        }
    }
    
    // ==================== Episode 相关方法 ====================
    
    /**
     * 统计图谱中的 Episode 数量
     * @param graphId 图谱ID
     * @return Episode 数量
     */
    public long countEpisodesByGraphId(String graphId) {
        String cypher = "MATCH (e:Episode {group_id: $group_id}) RETURN count(e) as count";
        
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, Values.parameters("group_id", graphId));
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
            "MATCH (e:Episode {group_id: $group_id}) " +
            "RETURN e.uuid as uuid, e.name as name, e.source as source, " +
            "e.source_description as source_description, e.content as content, " +
            "e.created_at as created_at, e.valid_at as valid_at, e.group_id as group_id " +
            "SKIP $offset LIMIT $limit";
        
        List<Map<String, Object>> episodes = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, 
                Values.parameters("group_id", graphId, "offset", offset, "limit", limit));
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
                episode.put("group_id", record.get("group_id").asString());
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
            "MATCH (e:Episode {group_id: $group_id, uuid: $uuid}) " +
            "RETURN e.uuid as uuid, e.name as name, e.source as source, " +
            "e.source_description as source_description, e.content as content, " +
            "e.created_at as created_at, e.valid_at as valid_at, e.group_id as group_id, " +
            "e.processed as processed";
        
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, 
                Values.parameters("group_id", graphId, "uuid", episodeUuid));
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
                episode.put("group_id", record.get("group_id").asString());
                episode.put("processed", record.get("processed").asBoolean());
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
            "labels(n) as labels";
        
        try (Session session = neo4jDriver.session()) {
            Result nodeResult = session.run(cypher, Values.parameters("uuid", episodeUuid));
            while (nodeResult.hasNext()) {
                Record record = nodeResult.next();
                Map<String, Object> node = new HashMap<>();
                node.put("uuid", record.get("uuid").asString());
                node.put("name", record.get("name").asString());
                node.put("type", record.get("type").asString());
                node.put("summary", record.get("summary").asString());
                node.put("labels", record.get("labels").asList());
                nodes.add(node);
            }
        }
        
        // TODO: 查询提及的边
        
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
            "MATCH (e:Episode {group_id: $group_id, uuid: $uuid}) " +
            "DETACH DELETE e";
        
        try (Session session = neo4jDriver.session()) {
            session.run(cypher, 
                Values.parameters("group_id", graphId, "uuid", episodeUuid));
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
            "CREATE (e:Episode {group_id: $group_id, uuid: $uuid, name: $name, " +
            "source: $source, source_description: $source_description, " +
            "content: $content, created_at: timestamp(), valid_at: timestamp()}) " +
            "SET e += $props " +
            "RETURN e.uuid as uuid, e.name as name, e.source as source, " +
            "e.source_description as source_description, e.content as content, " +
            "e.created_at as created_at, e.valid_at as valid_at, e.group_id as group_id";
        
        Map<String, Object> params = new HashMap<>();
        params.put("group_id", graphId);
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
                episode.put("group_id", record.get("group_id").asString());
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
            "MATCH (n:Entity {group_id: $group_id, uuid: $uuid}) " +
            "DETACH DELETE n";
        
        try (Session session = neo4jDriver.session()) {
            session.run(cypher, 
                Values.parameters("group_id", graphId, "uuid", uuid));
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
            // 查询节点数量
            Result nodeResult = session.run(
                "MATCH (n:Entity {group_id: $group_id}) RETURN count(n) as nodeCount",
                Values.parameters("group_id", graphId));
            if (nodeResult.hasNext()) {
                stats.put("nodeCount", nodeResult.next().get("nodeCount").asLong());
            } else {
                stats.put("nodeCount", 0L);
            }
            
            // 查询关系数量
            Result edgeResult = session.run(
                "MATCH ()-[r:RELATES_TO]->() WHERE r.group_id = $group_id RETURN count(r) as edgeCount",
                Values.parameters("group_id", graphId));
            if (edgeResult.hasNext()) {
                stats.put("edgeCount", edgeResult.next().get("edgeCount").asLong());
            } else {
                stats.put("edgeCount", 0L);
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
                "MATCH (n:Entity {group_id: $group_id}) DETACH DELETE n",
                Values.parameters("group_id", graphId));
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
            "WHERE relationship.group_id = $group_id " +
            "WITH relationship, score " +
            "MATCH (a:Entity {group_id: $group_id})-[r]-() " +
            "WHERE elementId(r) = elementId(relationship) " +
            "RETURN r.uuid as uuid, r.fact as fact, r.type as type, " +
            "r.group_id as group_id, score " +
            "LIMIT $limit";
        
        List<Map<String, Object>> results = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, 
                Values.parameters("query", query + "*", "group_id", graphId, "limit", limit));
            while (result.hasNext()) {
                Record record = result.next();
                Map<String, Object> edge = new HashMap<>();
                edge.put("uuid", record.get("uuid").asString());
                edge.put("fact", record.get("fact").asString());
                edge.put("type", record.get("type").asString());
                edge.put("group_id", record.get("group_id").asString());
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
            "WHERE node.group_id = $group_id " +
            "RETURN node.uuid as uuid, node.name as name, node.summary as summary, " +
            "node.type as type, node.group_id as group_id, score " +
            "LIMIT $limit";
        
        List<Map<String, Object>> results = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, 
                Values.parameters("query", query + "*", "group_id", graphId, "limit", limit));
            while (result.hasNext()) {
                Record record = result.next();
                Map<String, Object> node = new HashMap<>();
                node.put("uuid", record.get("uuid").asString());
                node.put("name", record.get("name").asString());
                node.put("summary", record.get("summary").asString());
                node.put("type", record.get("type").asString());
                node.put("group_id", record.get("group_id").asString());
                node.put("score", record.get("score").asDouble());
                results.add(node);
            }
        } catch (Exception e) {
            log.warn("全文搜索节点失败（可能索引未创建）：{}", e.getMessage());
        }
        return results;
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
}
