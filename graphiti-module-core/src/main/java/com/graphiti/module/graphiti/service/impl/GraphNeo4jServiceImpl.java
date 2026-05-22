package com.graphiti.module.graphiti.service.impl;

import com.graphiti.module.graphiti.config.GraphNeo4jConfig;
import com.graphiti.module.graphiti.service.GraphNeo4jService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Neo4j 数据访问服务实现类
 * 提供节点和关系的 CRUD 操作
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GraphNeo4jServiceImpl implements GraphNeo4jService {

    private final Driver neo4jDriver;
    private final GraphNeo4jConfig neo4jConfig;

    @Override
    public Driver getNeo4jDriver() {
        return neo4jDriver;
    }

    @Override
    public Map<String, Object> createEntityNode(String graphId, String uuid, String name, String type,
                                                 String summary, float[] embedding, Map<String, Object> properties) {
        String nameField = getTypeNameField(type);

        List<String> nameFieldsToExclude = getAllTypeNameFields();
        Map<String, Object> safeProps = properties != null
            ? properties.entrySet().stream()
                .filter(e -> !nameFieldsToExclude.contains(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
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

    @Override
    public void updateNodeEmbedding(String graphId, String uuid, float[] embedding) {
        String cypher = "MATCH (n:Entity {graph_id: $graph_id, uuid: $uuid}) " +
                        "SET n.embedding = $embedding";
        try (Session session = neo4jDriver.session()) {
            session.run(cypher, Values.parameters("graph_id", graphId, "uuid", uuid,
                    "embedding", embedding != null ? toFloatList(embedding) : null));
        }
    }

    @Override
    public Map<String, Object> createRelationship(String graphId, String edgeUuid, String sourceUuid,
                                                  String targetUuid, String type,
                                                  String fact, float[] embedding, Map<String, Object> properties) {
        String relationType = (type != null && !type.isBlank()) ? type : "RELATES_TO";
        String cypher =
            "MATCH (a:Entity {graph_id: $graph_id, uuid: $sourceUuid}) " +
            "MATCH (b:Entity {graph_id: $graph_id, uuid: $targetUuid}) " +
            "CREATE (a)-[r:" + relationType + " {uuid: $edgeUuid, type: $type, fact: $fact, " +
            "embedding: $embedding, valid_at: timestamp(), invalid_at: null}]->(b) " +
            "SET r += $props RETURN r";

        Map<String, Object> props = properties != null ? new HashMap<>(properties) : new HashMap<>();
        if (!props.containsKey("uuid")) {
            props.put("uuid", edgeUuid != null ? edgeUuid : UUID.randomUUID().toString().replace("-", ""));
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

    @Override
    public Map<String, Object> createRelationship(String graphId, String edgeUuid, String sourceUuid,
                                                  String targetUuid, String type, String fact) {
        return createRelationship(graphId, edgeUuid, sourceUuid, targetUuid, type, fact, null, null);
    }

    @Override
    public void updateEdgeEmbedding(String graphId, String uuid, float[] embedding) {
        String cypher = "MATCH ()-[r:RELATES_TO {graph_id: $graph_id, uuid: $uuid}]->() " +
                        "SET r.embedding = $embedding";
        try (Session session = neo4jDriver.session()) {
            session.run(cypher, Values.parameters("graph_id", graphId, "uuid", uuid,
                    "embedding", embedding != null ? toFloatList(embedding) : null));
        }
    }

    @Override
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

    @Override
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

    @Override
    public List<Map<String, Object>> listEdges(String graphId, String type, String source, String target, long skip, long limit) {
        StringBuilder cypher = new StringBuilder(
            "MATCH (a:Entity {graph_id: $graph_id})-[r:RELATES_TO]->(b:Entity) "
        );

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

    @Override
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

    @Override
    public void deleteEdge(String graphId, String uuid) {
        String cypher =
            "MATCH (a:Entity {graph_id: $graph_id})-[r:RELATES_TO {uuid: $uuid}]->(b:Entity) " +
            "DELETE r";

        try (Session session = neo4jDriver.session()) {
            session.run(cypher,
                Values.parameters("graph_id", graphId, "uuid", uuid));
        }
    }

    @Override
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
    
    @Override
    public long countEpisodesByType(String graphId, String episodeType) {
        String cypher = "MATCH (e:Episode {graph_id: $graph_id, episode_type: $episode_type}) RETURN count(e) as count";

        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, 
                Values.parameters("graph_id", graphId, "episode_type", episodeType));
            if (result.hasNext()) {
                return result.next().get("count").asLong();
            }
        }
        return 0L;
    }

    @Override
    public List<Map<String, Object>> getEpisodesByGraphId(String graphId, int limit, int offset) {
        String cypher =
            "MATCH (e:Episode {graph_id: $graph_id}) " +
            "RETURN e.uuid as uuid, e.name as name, e.source as source, " +
            "e.source_description as source_description, e.content as content, " +
            "e.created_at as created_at, e.valid_at as valid_at, e.graph_id as graph_id, " +
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

    @Override
    public Map<String, Object> getEpisodeByUuid(String graphId, String episodeUuid) {
        String cypher =
            "MATCH (e:Episode {graph_id: $graph_id, uuid: $uuid}) " +
            "RETURN e.uuid as uuid, e.name as name, e.source as source, " +
            "e.source_description as source_description, e.content as content, " +
            "e.created_at as created_at, e.valid_at as valid_at, e.graph_id as graph_id, " +
            "e.processed as processed, " +
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

    @Override
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

    @Override
    public void deleteEpisode(String graphId, String episodeUuid) {
        String cypher =
            "MATCH (e:Episode {graph_id: $graph_id, uuid: $uuid}) " +
            "DETACH DELETE e";

        try (Session session = neo4jDriver.session()) {
            session.run(cypher,
                Values.parameters("graph_id", graphId, "uuid", episodeUuid));
        }
    }

    @Override
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

    @Override
    public void deleteEntityNode(String graphId, String uuid) {
        String cypher =
            "MATCH (n:Entity {graph_id: $graph_id, uuid: $uuid}) " +
            "DETACH DELETE n";

        try (Session session = neo4jDriver.session()) {
            session.run(cypher,
                Values.parameters("graph_id", graphId, "uuid", uuid));
        }
    }

    @Override
    public Map<String, Long> getGraphStats(String graphId) {
        Map<String, Long> stats = new HashMap<>();

        try (Session session = neo4jDriver.session()) {
            Result nodeResult = session.run(
                "MATCH (n) WHERE n.graph_id = $graph_id AND ((labels(n) = ['Entity']) OR (labels(n) = ['Episode'])) RETURN count(n) as nodeCount",
                Values.parameters("graph_id", graphId));
            if (nodeResult.hasNext()) {
                stats.put("nodeCount", nodeResult.next().get("nodeCount").asLong());
            } else {
                stats.put("nodeCount", 0L);
            }

            Result edgeResult = session.run(
                "MATCH ()-[r:RELATES_TO]->() WHERE r.graph_id = $graph_id RETURN count(r) as edgeCount",
                Values.parameters("graph_id", graphId));
            if (edgeResult.hasNext()) {
                stats.put("edgeCount", edgeResult.next().get("edgeCount").asLong());
            } else {
                stats.put("edgeCount", 0L);
            }

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

    @Override
    public void clearGraphData(String graphId) {
        try (Session session = neo4jDriver.session()) {
            session.run(
                "MATCH (n) WHERE n.graph_id = $graph_id AND (labels(n) = ['Entity'] OR labels(n) = ['Episode']) DETACH DELETE n",
                Values.parameters("graph_id", graphId));
        }
    }

    @Override
    public List<Map<String, Object>> searchEdgesByFulltext(String query, String graphId, int limit) {
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

    @Override
    public List<Map<String, Object>> searchNodesByFulltext(String query, String graphId, int limit) {
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

    @Override
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

    @Override
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

    @Override
    public void initVectorIndexes(int nodeDimensions, int edgeDimensions) {
        try (Session session = neo4jDriver.session()) {
            session.run(
                "CREATE VECTOR INDEX node_embedding_index IF NOT EXISTS " +
                "FOR (n:Entity) ON (n.embedding) " +
                "OPTIONS {indexConfig: {`vector.dimensions`: $dim, `vector.similarity_function`: 'cosine'}}",
                Values.parameters("dim", nodeDimensions)
            );
            log.info("节点向量索引创建/确认完成，维度：{}", nodeDimensions);

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

    private List<Float> toFloatList(float[] arr) {
        if (arr == null) return null;
        List<Float> list = new ArrayList<>(arr.length);
        for (float v : arr) {
            list.add(v);
        }
        return list;
    }

    @Override
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

    @Override
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

    @Override
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

    @Override
    public List<Map<String, Object>> getValidNodes(String graphId) {
        String cypher =
            "MATCH (n:Entity {graph_id: $graph_id}) " +
            "WHERE n.invalid_at IS NULL " +
            "RETURN n.uuid as uuid, n.name as name, n.type as type, n.summary as summary, " +
            "n.valid_at as valid_at";
        return executeNodeQuery(cypher, graphId);
    }

    @Override
    public List<Map<String, Object>> getValidNodesAt(String graphId, long referenceTime) {
        String cypher =
            "MATCH (n:Entity {graph_id: $graph_id}) " +
            "WHERE n.valid_at <= $refTime AND (n.invalid_at IS NULL OR n.invalid_at > $refTime) " +
            "RETURN n.uuid as uuid, n.name as name, n.type as type, n.summary as summary, " +
            "n.valid_at as valid_at, n.invalid_at as invalid_at";
        return executeNodeQuery(cypher, graphId, referenceTime);
    }

    @Override
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

    @Override
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

    @Override
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

    @Override
    public void cloneGraphData(String sourceGraphId, String targetGraphId) {
        String cloneNodesCypher =
            "MATCH (n:Entity {graph_id: $source_id}) " +
            "CREATE (m:Entity) SET m = properties(n), m.graph_id = $target_id";
        try (Session session = neo4jDriver.session()) {
            session.run(cloneNodesCypher, Values.parameters("source_id", sourceGraphId, "target_id", targetGraphId));
        }

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

    @Override
    public List<Map<String, Object>> getNodesByGraphId(String graphId) {
        String cypher =
            "MATCH (n:Entity {graph_id: $graph_id}) " +
            "RETURN n.uuid as uuid, n.name as name, n.type as type, n.summary as summary";
        return executeNodeQuery(cypher, graphId);
    }

    @Override
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

    @Override
    public List<Map<String, Object>> findNodes(String graphId, List<String> labels,
                                                Map<String, Object> properties,
                                                long skip, long limit) {
        StringBuilder cypher = new StringBuilder();
        cypher.append("MATCH (n) ");
        cypher.append("WHERE n.graph_id = $graph_id ");

        if (labels != null && !labels.isEmpty()) {
            String labelMatch = labels.stream()
                .map(l -> "'" + l + "'")
                .collect(Collectors.joining(","));
            cypher.append("AND (labels(n) = [").append(labelMatch).append("]) ");
        }

        Map<String, Object> params = new HashMap<>();
        params.put("graph_id", graphId);

        if (properties != null) {
            if (properties.containsKey("createdAfter")) {
                cypher.append("AND n.created_at >= $createdAfter ");
                params.put("createdAfter", properties.get("createdAfter"));
            }
            if (properties.containsKey("createdBefore")) {
                cypher.append("AND n.created_at <= $createdBefore ");
                params.put("createdBefore", properties.get("createdBefore"));
            }
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

    @Override
    public List<Map<String, Object>> findEdges(String graphId, List<String> edgeTypes,
                                                Map<String, Object> properties,
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
        if (properties != null) {
            if (properties.containsKey("createdAfter")) {
                conditions.add("r.created_at >= $createdAfter");
                params.put("createdAfter", properties.get("createdAfter"));
            }
            if (properties.containsKey("createdBefore")) {
                conditions.add("r.created_at <= $createdBefore");
                params.put("createdBefore", properties.get("createdBefore"));
            }
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

    @Override
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

    @Override
    public List<Map<String, Object>> getNodeEdges(String nodeUuid, long skip, long limit) {
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

    @Override
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

    @Override
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

    @Override
    public long countNodes(String graphId, List<String> labels) {
        StringBuilder cypher = new StringBuilder();
        cypher.append("MATCH (n) WHERE n.graph_id = $graph_id ");
        if (labels != null && !labels.isEmpty()) {
            String labelMatch = labels.stream()
                .map(l -> "'" + l + "'")
                .collect(Collectors.joining(","));
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

    @Override
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

    @Override
    public long countCommunitiesByGraphId(String graphId) {
        String cypher = "MATCH (c:Community {graph_id: $graph_id}) RETURN count(c) as count";
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, Values.parameters("graph_id", graphId));
            if (result.hasNext()) {
                return result.next().get("count").asLong();
            }
        }
        return 0L;
    }

    @Override
    public void clearAllRelationships(String graphId) {
        try (Session session = neo4jDriver.session()) {
            // 删除所有 RELATES_TO 关系
            session.run(
                "MATCH ()-[r:RELATES_TO {graph_id: $graph_id}]->() DELETE r",
                Values.parameters("graph_id", graphId));
            // 删除所有 MENTIONS 关系
            session.run(
                "MATCH ()-[r:MENTIONS {graph_id: $graph_id}]->() DELETE r",
                Values.parameters("graph_id", graphId));
            // 删除所有 HAS_COMMUNITY 关系
            session.run(
                "MATCH ()-[r:HAS_COMMUNITY {graph_id: $graph_id}]->() DELETE r",
                Values.parameters("graph_id", graphId));
        }
        log.info("已清除图谱所有关系数据：graphId={}", graphId);
    }

    @Override
    public void deleteAllCommunities(String graphId) {
        String cypher =
            "MATCH (c:Community {graph_id: $graph_id}) DETACH DELETE c";
        try (Session session = neo4jDriver.session()) {
            session.run(cypher, Values.parameters("graph_id", graphId));
        }
        log.info("已删除图谱所有社区节点：graphId={}", graphId);
    }

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

    private List<String> getAllTypeNameFields() {
        return List.of(
            "courtName", "partyName", "caseName", "caseNumber",
            "articleNumber", "lawName", "judgeName", "documentNumber",
            "agreementNumber", "evidenceNumber", "reasoning", "factDescription",
            "name"
        );
    }
}
