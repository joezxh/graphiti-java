package com.graphiti.module.graphiti.service.impl;

import com.graphiti.module.graphiti.dal.dataobject.GraphMetadataDO;
import com.graphiti.module.graphiti.dal.mysql.GraphMetadataMapper;
import com.graphiti.module.graphiti.service.GraphNeo4jService;
import com.graphiti.module.graphiti.service.GraphVisualizationService;
import com.graphiti.module.graphiti.vo.ide.GraphVisualizationRespVO;
import com.graphiti.module.graphiti.vo.node.NodeInfoRespVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 图谱可视化服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GraphVisualizationServiceImpl implements GraphVisualizationService {

    private final Driver neo4jDriver;
    private final GraphMetadataMapper graphMetadataMapper;
    private final GraphNeo4jService graphNeo4jService;

    @Override
    public GraphVisualizationRespVO getVisualizationData(
            String graphId,
            String layout,
            Integer page,
            Integer pageSize,
            String classType,
            String keyword) {

        try (Session session = neo4jDriver.session()) {
            StringBuilder cypherBuilder = new StringBuilder();
            cypherBuilder.append("MATCH (n:Entity {graph_id: $graphId}) ");

            Map<String, Object> params = new HashMap<>();
            params.put("graphId", graphId);

            if (classType != null && !classType.isBlank()) {
                cypherBuilder.append("WHERE n.type = $classType ");
                params.put("classType", classType);
            }

            if (keyword != null && !keyword.isBlank()) {
                cypherBuilder.append("AND (n.name CONTAINS $keyword OR n.summary CONTAINS $keyword) ");
                params.put("keyword", keyword);
            }

            String baseQuery = cypherBuilder.toString();
            String countCypher = baseQuery.replaceAll("MATCH \\(n:Entity", "MATCH (n:Entity") + " RETURN count(n) as total";
            Result countResult = session.run(countCypher, params);

            long total = countResult.hasNext() ? countResult.next().get("total").asLong() : 0;

            cypherBuilder.append("RETURN n ORDER BY n.valid_at DESC SKIP $skip LIMIT $limit");
            params.put("skip", (page - 1) * pageSize);
            params.put("limit", pageSize);

            Result nodeResult = session.run(cypherBuilder.toString(), params);

            List<GraphVisualizationRespVO.NodeVO> nodes = new ArrayList<>();
            List<String> nodeUuids = new ArrayList<>();

            while (nodeResult.hasNext()) {
                Record record = nodeResult.next();
                var neo4jNode = record.get("n").asNode();
                Map<String, Object> nodeMap = neo4jNode.asMap();

                String nodeType = (String) nodeMap.get("type");
                String nodeName = extractNodeName(nodeType, nodeMap);

                GraphVisualizationRespVO.NodeVO nodeVO = GraphVisualizationRespVO.NodeVO.builder()
                        .uuid((String) nodeMap.get("uuid"))
                        .name(nodeName)
                        .type(nodeType)
                        .summary((String) nodeMap.get("summary"))
                        .properties(extractProperties(nodeMap))
                        .build();

                nodes.add(nodeVO);
                nodeUuids.add((String) nodeMap.get("uuid"));
            }

            List<GraphVisualizationRespVO.EdgeVO> edges = new ArrayList<>();
            if (!nodeUuids.isEmpty()) {
                String edgeCypher =
                    "MATCH (a:Entity {graph_id: $graphId})-[r]->(b:Entity {graph_id: $graphId}) " +
                    "WHERE a.uuid IN $uuids OR b.uuid IN $uuids " +
                    "RETURN a.uuid as source, b.uuid as target, type(r) as type, r.uuid as uuid, r.fact as fact";

                Map<String, Object> edgeParams = new HashMap<>();
                edgeParams.put("graphId", graphId);
                edgeParams.put("uuids", nodeUuids);

                Result edgeResult = session.run(edgeCypher, edgeParams);
                while (edgeResult.hasNext()) {
                    Record record = edgeResult.next();
                    GraphVisualizationRespVO.EdgeVO edgeVO = GraphVisualizationRespVO.EdgeVO.builder()
                            .uuid(record.get("uuid").asString())
                            .source(record.get("source").asString())
                            .target(record.get("target").asString())
                            .type(record.get("type").asString())
                            .fact(record.containsKey("fact") ? record.get("fact").asString() : null)
                            .build();
                    edges.add(edgeVO);
                }
            }

            String aggCypher =
                "MATCH (n:Entity {graph_id: $graphId}) " +
                "WHERE n.invalid_at IS NULL " +
                "RETURN n.type as type, count(n) as count ORDER BY count DESC";

            List<GraphVisualizationRespVO.ClassCount> aggregations = new ArrayList<>();
            Result aggResult = session.run(aggCypher, Map.of("graphId", graphId));
            while (aggResult.hasNext()) {
                Record record = aggResult.next();
                aggregations.add(GraphVisualizationRespVO.ClassCount.builder()
                        .type(record.get("type").asString())
                        .count(record.get("count").asLong())
                        .build());
            }

            return GraphVisualizationRespVO.builder()
                    .nodes(nodes)
                    .edges(edges)
                    .pagination(GraphVisualizationRespVO.PaginationVO.builder()
                            .page(page)
                            .pageSize(pageSize)
                            .total(total)
                            .totalPages((int) Math.ceil((double) total / pageSize))
                            .build())
                    .aggregations(GraphVisualizationRespVO.AggregationVO.builder()
                            .byClass(aggregations)
                            .build())
                    .build();
        }
    }

    @Override
    public Map<String, Object> getGraphMetadata(String graphId) {
        GraphMetadataDO metadata = graphMetadataMapper.selectByGraphId(graphId);

        Map<String, Object> result = new HashMap<>();
        result.put("graphId", graphId);

        if (metadata != null) {
            result.put("name", metadata.getName());
            result.put("description", metadata.getDescription());
            result.put("status", metadata.getStatus());
        }

        try (Session session = neo4jDriver.session()) {
            String nodeCountCypher = "MATCH (n:Entity {graph_id: $graphId}) WHERE n.invalid_at IS NULL RETURN count(n) as count";
            Result nodeResult = session.run(nodeCountCypher, Map.of("graphId", graphId));
            long nodeCount = nodeResult.hasNext() ? nodeResult.next().get("count").asLong() : 0;
            result.put("nodeCount", nodeCount);

            String edgeCountCypher =
                "MATCH (a:Entity {graph_id: $graphId})-[r]->(b:Entity {graph_id: $graphId}) " +
                "WHERE a.invalid_at IS NULL AND b.invalid_at IS NULL " +
                "RETURN count(r) as count";
            Result edgeResult = session.run(edgeCountCypher, Map.of("graphId", graphId));
            long edgeCount = edgeResult.hasNext() ? edgeResult.next().get("count").asLong() : 0;
            result.put("edgeCount", edgeCount);

            String classCountCypher =
                "MATCH (n:Entity {graph_id: $graphId}) WHERE n.invalid_at IS NULL " +
                "RETURN count(DISTINCT n.type) as count";
            Result classResult = session.run(classCountCypher, Map.of("graphId", graphId));
            long classCount = classResult.hasNext() ? classResult.next().get("count").asLong() : 0;
            result.put("classCount", classCount);

            result.put("episodeCount", 0);
            result.put("communityCount", 0);
        }

        return result;
    }

    @Override
    public NodeInfoRespVO getNodeDetail(String graphId, String nodeUuid) {
        if (nodeUuid == null || nodeUuid.isBlank() || "undefined".equals(nodeUuid)) {
            throw new IllegalArgumentException("invalidated Node UUID: " + nodeUuid);
        }
        try (Session session = neo4jDriver.session()) {
            String cypher =
                "MATCH (n {graph_id: $graphId, uuid: $uuid}) " +
                "WHERE n:Entity OR n:Episode " +
                "RETURN n";

            Result result = session.run(cypher, Map.of("graphId", graphId, "uuid", nodeUuid));

            if (result.hasNext()) {
                Record record = result.next();
                var neo4jNode = record.get("n").asNode();
                Map<String, Object> nodeMap = neo4jNode.asMap();

                String nodeType = (String) nodeMap.get("type");
                String nodeName = extractNodeName(nodeType, nodeMap);

                NodeInfoRespVO nodeVO = new NodeInfoRespVO();
                nodeVO.setUuid((String) nodeMap.get("uuid"));
                nodeVO.setName(nodeName);
                nodeVO.setType(nodeType);
                nodeVO.setSummary((String) nodeMap.get("summary"));
                nodeVO.setProperties(extractProperties(nodeMap));

                return nodeVO;
            }

            throw new RuntimeException("Node not found: " + nodeUuid);
        }
    }

    @Override
    public NodeInfoRespVO createNode(String graphId, Map<String, Object> nodeData) {
        String uuid = UUID.randomUUID().toString();
        String name = (String) nodeData.get("name");
        String type = (String) nodeData.get("type");
        String summary = (String) nodeData.get("summary");
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) nodeData.getOrDefault("properties", new HashMap<>());

        graphNeo4jService.createEntityNode(graphId, uuid, name, type, summary, null, properties);

        NodeInfoRespVO nodeVO = new NodeInfoRespVO();
        nodeVO.setUuid(uuid);
        nodeVO.setName(name);
        nodeVO.setType(type);
        nodeVO.setSummary(summary);
        nodeVO.setProperties(properties);

        return nodeVO;
    }

    @Override
    public NodeInfoRespVO updateNode(String graphId, String nodeUuid, Map<String, Object> nodeData) {
        try (Session session = neo4jDriver.session()) {
            StringBuilder cypher = new StringBuilder("MATCH (n:Entity {graph_id: $graphId, uuid: $uuid}) ");
            Map<String, Object> params = new HashMap<>();
            params.put("graphId", graphId);
            params.put("uuid", nodeUuid);

            List<String> setClauses = new ArrayList<>();

            if (nodeData.containsKey("name")) {
                setClauses.add("n.name = $name");
                params.put("name", nodeData.get("name"));
            }

            if (nodeData.containsKey("summary")) {
                setClauses.add("n.summary = $summary");
                params.put("summary", nodeData.get("summary"));
            }

            if (nodeData.containsKey("properties")) {
                setClauses.add("n += $props");
                @SuppressWarnings("unchecked")
                Map<String, Object> props = (Map<String, Object>) nodeData.get("properties");
                params.put("props", props);
            }

            if (!setClauses.isEmpty()) {
                cypher.append("SET ").append(String.join(", ", setClauses));
                cypher.append(" RETURN n");

                session.run(cypher.toString(), params);
            }

            return getNodeDetail(graphId, nodeUuid);
        }
    }

    @Override
    public void deleteNode(String graphId, String nodeUuid) {
        try (Session session = neo4jDriver.session()) {
            String deleteEdgesCypher =
                "MATCH (a:Entity {graph_id: $graphId, uuid: $uuid})-[r]->(b:Entity {graph_id: $graphId}) " +
                "DELETE r";
            session.run(deleteEdgesCypher, Map.of("graphId", graphId, "uuid", nodeUuid));

            String deleteEdgesCypher2 =
                "MATCH (a:Entity {graph_id: $graphId})-[r]->(b:Entity {graph_id: $graphId, uuid: $uuid}) " +
                "DELETE r";
            session.run(deleteEdgesCypher2, Map.of("graphId", graphId, "uuid", nodeUuid));

            String deleteNodeCypher =
                "MATCH (n:Entity {graph_id: $graphId, uuid: $uuid}) " +
                "SET n.invalid_at = timestamp()";
            session.run(deleteNodeCypher, Map.of("graphId", graphId, "uuid", nodeUuid));
        }
    }

    @Override
    public GraphVisualizationRespVO.EdgeVO createEdge(String graphId, Map<String, Object> edgeData) {
        String sourceUuid = (String) edgeData.get("sourceUuid");
        String targetUuid = (String) edgeData.get("targetUuid");
        String type = (String) edgeData.get("type");
        String fact = (String) edgeData.get("fact");
        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) edgeData.getOrDefault("properties", new HashMap<>());

        String uuid = UUID.randomUUID().toString();

        graphNeo4jService.createRelationship(graphId, uuid, sourceUuid, targetUuid, type, fact, null, properties);

        return GraphVisualizationRespVO.EdgeVO.builder()
                .uuid(uuid)
                .source(sourceUuid)
                .target(targetUuid)
                .type(type)
                .fact(fact)
                .properties(properties)
                .build();
    }

    @Override
    public GraphVisualizationRespVO expandNeighbors(String graphId, String nodeUuid, Map<String, Object> options) {
        try (Session session = neo4jDriver.session()) {
            int depth = options != null && options.containsKey("depth")
                    ? ((Number) options.get("depth")).intValue()
                    : 1;
            int maxNodes = options != null && options.containsKey("maxNodes")
                    ? ((Number) options.get("maxNodes")).intValue()
                    : 50;

            String cypher =
                "MATCH (n:Entity {graph_id: $graphId, uuid: $nodeUuid})-[r*1.." + depth + "]-(neighbor:Entity {graph_id: $graphId}) " +
                "WHERE neighbor.invalid_at IS NULL " +
                "WITH neighbor, r LIMIT $maxNodes " +
                "RETURN DISTINCT neighbor, size(r) as distance";

            Result result = session.run(cypher, Map.of(
                    "graphId", graphId,
                    "nodeUuid", nodeUuid,
                    "maxNodes", maxNodes
            ));

            List<GraphVisualizationRespVO.NodeVO> nodes = new ArrayList<>();
            Set<String> nodeUuids = new HashSet<>();

            while (result.hasNext()) {
                Record record = result.next();
                var neo4jNode = record.get("neighbor").asNode();
                Map<String, Object> nodeMap = neo4jNode.asMap();

                String uuid = (String) nodeMap.get("uuid");
                if (nodeUuids.contains(uuid)) {
                    continue;
                }
                nodeUuids.add(uuid);

                String nodeType = (String) nodeMap.get("type");
                String nodeName = extractNodeName(nodeType, nodeMap);

                GraphVisualizationRespVO.NodeVO nodeVO = GraphVisualizationRespVO.NodeVO.builder()
                        .uuid(uuid)
                        .name(nodeName)
                        .type(nodeType)
                        .summary((String) nodeMap.get("summary"))
                        .properties(extractProperties(nodeMap))
                        .build();

                nodes.add(nodeVO);
            }

            List<GraphVisualizationRespVO.EdgeVO> edges = new ArrayList<>();
            if (!nodeUuids.isEmpty()) {
                String edgeCypher =
                    "MATCH (a:Entity {graph_id: $graphId})-[r]->(b:Entity {graph_id: $graphId}) " +
                    "WHERE a.uuid IN $uuids AND b.uuid IN $uuids AND a.invalid_at IS NULL AND b.invalid_at IS NULL " +
                    "RETURN a.uuid as source, b.uuid as target, type(r) as type, r.uuid as uuid";

                Result edgeResult = session.run(edgeCypher, Map.of("graphId", graphId, "uuids", new ArrayList<>(nodeUuids)));
                while (edgeResult.hasNext()) {
                    Record record = edgeResult.next();
                    GraphVisualizationRespVO.EdgeVO edgeVO = GraphVisualizationRespVO.EdgeVO.builder()
                            .uuid(record.get("uuid").asString())
                            .source(record.get("source").asString())
                            .target(record.get("target").asString())
                            .type(record.get("type").asString())
                            .build();
                    edges.add(edgeVO);
                }
            }

            return GraphVisualizationRespVO.builder()
                    .nodes(nodes)
                    .edges(edges)
                    .build();
        }
    }

    @Override
    public GraphVisualizationRespVO getVisualizationDataByTypes(
            String graphId,
            String layout,
            Integer page,
            Integer pageSize,
            List<String> classTypes,
            String keyword) {

        try (Session session = neo4jDriver.session()) {
            StringBuilder cypherBuilder = new StringBuilder();
            cypherBuilder.append("MATCH (n:Entity {graph_id: $graphId}) ");

            Map<String, Object> params = new HashMap<>();
            params.put("graphId", graphId);

            if (classTypes != null && !classTypes.isEmpty()) {
                String placeholders = classTypes.stream()
                        .map(t -> "'" + t.replace("'", "\\'") + "'")
                        .collect(Collectors.joining(", "));
                cypherBuilder.append("WHERE n.type IN [").append(placeholders).append("] ");
            }

            if (keyword != null && !keyword.isBlank()) {
                if (classTypes == null || classTypes.isEmpty()) {
                    cypherBuilder.append("WHERE ");
                } else {
                    cypherBuilder.append("AND ");
                }
                cypherBuilder.append("(n.courtName CONTAINS $keyword OR n.partyName CONTAINS $keyword OR ")
                        .append("n.caseName CONTAINS $keyword OR n.caseNumber CONTAINS $keyword OR ")
                        .append("n.lawName CONTAINS $keyword OR n.judgeName CONTAINS $keyword OR ")
                        .append("n.name CONTAINS $keyword OR n.summary CONTAINS $keyword) ");
                params.put("keyword", keyword);
            }

            String baseQuery = cypherBuilder.toString();
            String countCypher = baseQuery + " RETURN count(n) as total";
            Result countResult = session.run(countCypher, params);
            long total = countResult.hasNext() ? countResult.next().get("total").asLong() : 0;

            cypherBuilder.append("RETURN n ORDER BY n.valid_at DESC SKIP $skip LIMIT $limit");
            params.put("skip", (page - 1) * pageSize);
            params.put("limit", pageSize);

            Result nodeResult = session.run(cypherBuilder.toString(), params);

            List<GraphVisualizationRespVO.NodeVO> nodes = new ArrayList<>();
            List<String> nodeUuids = new ArrayList<>();

            while (nodeResult.hasNext()) {
                Record record = nodeResult.next();
                var neo4jNode = record.get("n").asNode();
                Map<String, Object> nodeMap = neo4jNode.asMap();

                String nodeType = (String) nodeMap.get("type");
                String nodeName = extractNodeName(nodeType, nodeMap);

                GraphVisualizationRespVO.NodeVO nodeVO = GraphVisualizationRespVO.NodeVO.builder()
                        .uuid((String) nodeMap.get("uuid"))
                        .name(nodeName)
                        .type(nodeType)
                        .summary((String) nodeMap.get("summary"))
                        .properties(extractProperties(nodeMap))
                        .build();
                nodes.add(nodeVO);
                nodeUuids.add((String) nodeMap.get("uuid"));
            }

            List<GraphVisualizationRespVO.EdgeVO> edges = new ArrayList<>();
            if (!nodeUuids.isEmpty()) {
                String edgeCypher =
                    "MATCH (a:Entity {graph_id: $graphId})-[r]->(b:Entity {graph_id: $graphId}) " +
                    "WHERE a.uuid IN $uuids OR b.uuid IN $uuids " +
                    "RETURN a.uuid as source, b.uuid as target, type(r) as type, r.uuid as uuid, r.fact as fact";

                Map<String, Object> edgeParams = new HashMap<>();
                edgeParams.put("graphId", graphId);
                edgeParams.put("uuids", nodeUuids);

                Result edgeResult = session.run(edgeCypher, edgeParams);
                while (edgeResult.hasNext()) {
                    Record record = edgeResult.next();
                    GraphVisualizationRespVO.EdgeVO edgeVO = GraphVisualizationRespVO.EdgeVO.builder()
                            .uuid(record.get("uuid").asString())
                            .source(record.get("source").asString())
                            .target(record.get("target").asString())
                            .type(record.get("type").asString())
                            .fact(record.containsKey("fact") && !record.get("fact").isNull() ? record.get("fact").asString() : null)
                            .build();
                    edges.add(edgeVO);
                }
            }

            return GraphVisualizationRespVO.builder()
                    .nodes(nodes)
                    .edges(edges)
                    .pagination(GraphVisualizationRespVO.PaginationVO.builder()
                            .page(page)
                            .pageSize(pageSize)
                            .total(total)
                            .totalPages((int) Math.ceil((double) total / pageSize))
                            .build())
                    .build();
        }
    }

    @Override
    public GraphVisualizationRespVO getInstances(String graphId, String classType, int page, int pageSize) {
        try (Session session = neo4jDriver.session()) {
            StringBuilder cypherBuilder = new StringBuilder();
            cypherBuilder.append("MATCH (n:Entity {graph_id: $graphId, type: $classType}) ");
            cypherBuilder.append("WHERE n.invalid_at IS NULL ");

            Map<String, Object> params = new HashMap<>();
            params.put("graphId", graphId);
            params.put("classType", classType);

            String countCypher = cypherBuilder + " RETURN count(n) as total";
            Result countResult = session.run(countCypher, params);
            long total = countResult.hasNext() ? countResult.next().get("total").asLong() : 0;

            int effectiveLimit = Math.min(pageSize, 500);
            cypherBuilder.append("RETURN n ORDER BY n.valid_at DESC SKIP $skip LIMIT $limit");
            params.put("skip", (page - 1) * effectiveLimit);
            params.put("limit", effectiveLimit);

            Result nodeResult = session.run(cypherBuilder.toString(), params);

            List<GraphVisualizationRespVO.NodeVO> nodes = new ArrayList<>();
            List<String> nodeUuids = new ArrayList<>();

            while (nodeResult.hasNext()) {
                Record record = nodeResult.next();
                var neo4jNode = record.get("n").asNode();
                Map<String, Object> nodeMap = neo4jNode.asMap();

                String nodeType = (String) nodeMap.get("type");
                String nodeName = extractNodeName(nodeType, nodeMap);

                GraphVisualizationRespVO.NodeVO nodeVO = GraphVisualizationRespVO.NodeVO.builder()
                        .uuid((String) nodeMap.get("uuid"))
                        .name(nodeName)
                        .type(nodeType)
                        .summary((String) nodeMap.get("summary"))
                        .properties(extractProperties(nodeMap))
                        .build();
                nodes.add(nodeVO);
                nodeUuids.add((String) nodeMap.get("uuid"));
            }

            List<GraphVisualizationRespVO.EdgeVO> edges = new ArrayList<>();
            if (!nodeUuids.isEmpty()) {
                String edgeCypher =
                    "MATCH (a:Entity {graph_id: $graphId})-[r]->(b:Entity {graph_id: $graphId}) " +
                    "WHERE a.uuid IN $uuids AND b.uuid IN $uuids " +
                    "AND a.invalid_at IS NULL AND b.invalid_at IS NULL " +
                    "RETURN a.uuid as source, b.uuid as target, type(r) as type, r.uuid as uuid, r.fact as fact";

                Map<String, Object> edgeParams = new HashMap<>();
                edgeParams.put("graphId", graphId);
                edgeParams.put("uuids", nodeUuids);

                Result edgeResult = session.run(edgeCypher, edgeParams);
                while (edgeResult.hasNext()) {
                    Record record = edgeResult.next();
                    GraphVisualizationRespVO.EdgeVO edgeVO = GraphVisualizationRespVO.EdgeVO.builder()
                            .uuid(record.get("uuid").asString())
                            .source(record.get("source").asString())
                            .target(record.get("target").asString())
                            .type(record.get("type").asString())
                            .fact(record.containsKey("fact") && !record.get("fact").isNull() ? record.get("fact").asString() : null)
                            .build();
                    edges.add(edgeVO);
                }
            }

            return GraphVisualizationRespVO.builder()
                    .nodes(nodes)
                    .edges(edges)
                    .pagination(GraphVisualizationRespVO.PaginationVO.builder()
                            .page(page)
                            .pageSize(effectiveLimit)
                            .total(total)
                            .totalPages((int) Math.ceil((double) total / effectiveLimit))
                            .build())
                    .build();
        }
    }

    @Override
    public GraphVisualizationRespVO getEdges(String graphId, int limit) {
        try (Session session = neo4jDriver.session()) {
            int effectiveLimit = Math.min(limit, 500);

            String edgeCypher =
                "MATCH (a:Entity {graph_id: $graphId})-[r]->(b:Entity {graph_id: $graphId}) " +
                "WHERE a.invalid_at IS NULL AND b.invalid_at IS NULL " +
                "RETURN a.uuid as source, b.uuid as target, type(r) as type, r.uuid as uuid, r.fact as fact, " +
                "       a.name as sourceName, b.name as targetName, a.type as sourceType, b.type as targetType " +
                "LIMIT $limit";

            Map<String, Object> edgeParams = new HashMap<>();
            edgeParams.put("graphId", graphId);
            edgeParams.put("limit", effectiveLimit);

            List<GraphVisualizationRespVO.EdgeVO> edges = new ArrayList<>();
            Set<String> nodeUuids = new LinkedHashSet<>();

            Result edgeResult = session.run(edgeCypher, edgeParams);
            while (edgeResult.hasNext()) {
                Record record = edgeResult.next();
                GraphVisualizationRespVO.EdgeVO edgeVO = GraphVisualizationRespVO.EdgeVO.builder()
                        .uuid(record.get("uuid").asString())
                        .source(record.get("source").asString())
                        .target(record.get("target").asString())
                        .type(record.get("type").asString())
                        .fact(record.containsKey("fact") && !record.get("fact").isNull() ? record.get("fact").asString() : null)
                        .build();
                edges.add(edgeVO);
                nodeUuids.add(record.get("source").asString());
                nodeUuids.add(record.get("target").asString());
            }

            List<GraphVisualizationRespVO.NodeVO> nodes = new ArrayList<>();
            if (!nodeUuids.isEmpty()) {
                String nodeCypher =
                    "MATCH (n:Entity {graph_id: $graphId}) " +
                    "WHERE n.uuid IN $uuids AND n.invalid_at IS NULL " +
                    "RETURN n";

                Result nodeResult = session.run(nodeCypher, Map.of("graphId", graphId, "uuids", new ArrayList<>(nodeUuids)));
                while (nodeResult.hasNext()) {
                    Record record = nodeResult.next();
                    var neo4jNode = record.get("n").asNode();
                    Map<String, Object> nodeMap = neo4jNode.asMap();

                    String nodeType = (String) nodeMap.get("type");
                    String nodeName = extractNodeName(nodeType, nodeMap);

                    GraphVisualizationRespVO.NodeVO nodeVO = GraphVisualizationRespVO.NodeVO.builder()
                            .uuid((String) nodeMap.get("uuid"))
                            .name(nodeName)
                            .type(nodeType)
                            .summary((String) nodeMap.get("summary"))
                            .properties(extractProperties(nodeMap))
                            .build();
                    nodes.add(nodeVO);
                }
            }

            return GraphVisualizationRespVO.builder()
                    .nodes(nodes)
                    .edges(edges)
                    .build();
        }
    }

    @Override
    public GraphVisualizationRespVO getEpisodesVisualization(String graphId, int limit) {
        try (Session session = neo4jDriver.session()) {
            int effectiveLimit = Math.min(limit, 500);

            String episodeCypher =
                "MATCH (e:Episode {graph_id: $graphId}) " +
                "RETURN e.uuid as uuid, e.name as name, e.source as source, e.summary as summary, e.content as content " +
                "ORDER BY e.created_at DESC " +
                "LIMIT $limit";

            List<GraphVisualizationRespVO.NodeVO> nodes = new ArrayList<>();
            List<GraphVisualizationRespVO.EdgeVO> edges = new ArrayList<>();
            List<String> episodeUuids = new ArrayList<>();

            Result episodeResult = session.run(episodeCypher, Map.of("graphId", graphId, "limit", effectiveLimit));
            while (episodeResult.hasNext()) {
                Record record = episodeResult.next();
                String uuid = record.get("uuid").asString();
                episodeUuids.add(uuid);
                nodes.add(GraphVisualizationRespVO.NodeVO.builder()
                        .uuid(uuid)
                        .name(record.get("name").isNull() ? "Episode" : record.get("name").asString())
                        .type("Episode")
                        .summary(record.get("summary").isNull() ? null : record.get("summary").asString())
                        .build());
            }

            if (!episodeUuids.isEmpty()) {
                Set<String> seenNodeUuids = new java.util.HashSet<>();
                String mentionsCypher =
                    "MATCH (e:Episode {graph_id: $graphId})-[mentions:MENTIONS]->(n:Entity) " +
                    "WHERE e.uuid IN $uuids AND n.invalid_at IS NULL " +
                    "RETURN DISTINCT n.uuid as uuid, n.type as type, n.summary as summary, " +
                    "       e.uuid as episodeUuid, " +
                    "       n.courtName as courtName, n.partyName as partyName, " +
                    "       n.caseName as caseName, n.caseNumber as caseNumber, " +
                    "       n.articleNumber as articleNumber, n.lawName as lawName, " +
                    "       n.judgeName as judgeName, n.documentNumber as documentNumber, " +
                    "       n.agreementNumber as agreementNumber, n.evidenceNumber as evidenceNumber, " +
                    "       n.reasoning as reasoning, n.factDescription as factDescription, " +
                    "       n.name as genericName " +
                    "LIMIT 500";

                Map<String, Object> params = new HashMap<>();
                params.put("graphId", graphId);
                params.put("uuids", episodeUuids);

                Result mentionsResult = session.run(mentionsCypher, params);
                while (mentionsResult.hasNext()) {
                    Record record = mentionsResult.next();
                    String nodeUuid = record.get("uuid").asString();
                    if (seenNodeUuids.contains(nodeUuid)) {
                        continue;
                    }
                    seenNodeUuids.add(nodeUuid);

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
                    nodeData.put("name", record.get("genericName").isNull() ? null : record.get("genericName").asString());

                    String nodeType = record.get("type").isNull() ? null : record.get("type").asString();
                    String nodeName = extractNodeName(nodeType, nodeData);

                    nodes.add(GraphVisualizationRespVO.NodeVO.builder()
                            .uuid(nodeUuid)
                            .name(nodeName)
                            .type(nodeType)
                            .summary(record.get("summary").isNull() ? null : record.get("summary").asString())
                            .build());

                    edges.add(GraphVisualizationRespVO.EdgeVO.builder()
                            .uuid(java.util.UUID.randomUUID().toString())
                            .source(record.get("episodeUuid").asString())
                            .target(nodeUuid)
                            .type("MENTIONS")
                            .build());
                }
            }

            return GraphVisualizationRespVO.builder()
                    .nodes(nodes)
                    .edges(edges)
                    .build();
        }
    }

    @Override
    public GraphVisualizationRespVO getEpisodesVisualizationByType(
            String graphId,
            String typeCode,
            Integer page,
            Integer pageSize,
            Integer depth) {

        int effectivePage = page != null && page > 0 ? page : 1;
        int effectivePageSize = pageSize != null && pageSize > 0 ? pageSize : 20;
        int effectiveDepth = depth != null && depth >= 1 && depth <= 3 ? depth : 2;
        int skip = (effectivePage - 1) * effectivePageSize;

        try (Session session = neo4jDriver.session()) {
            // 阶段 1: 统计总数
            String countCypher =
                "MATCH (n:Episode {graph_id: $graphId, episode_type: $typeCode}) " +
                "WHERE n.invalid_at IS NULL " +
                "RETURN count(n) as total";
            Result countResult = session.run(countCypher,
                Map.of("graphId", graphId, "typeCode", typeCode));
            long total = countResult.hasNext() ? countResult.next().get("total").asLong() : 0;

            // 阶段 2: 分页查询中心节点
            String centerCypher =
                "MATCH (center:Episode {graph_id: $graphId, episode_type: $typeCode}) " +
                "WHERE center.invalid_at IS NULL " +
                "WITH center ORDER BY center.valid_at DESC SKIP $skip LIMIT $limit " +
                "RETURN collect(center) as centers";

            Map<String, Object> centerParams = new HashMap<>();
            centerParams.put("graphId", graphId);
            centerParams.put("typeCode", typeCode);
            centerParams.put("skip", skip);
            centerParams.put("limit", effectivePageSize);

            Result centerResult = session.run(centerCypher, centerParams);
            List<GraphVisualizationRespVO.NodeVO> nodes = new ArrayList<>();
            List<GraphVisualizationRespVO.EdgeVO> edges = new ArrayList<>();
            Set<String> nodeUuids = new LinkedHashSet<>();
            Set<String> edgeUuidSet = new HashSet<>();

            List<String> centerUuids = new ArrayList<>();
            if (centerResult.hasNext()) {
                Record record = centerResult.next();
                List<Object> centers = record.get("centers").asList();
                for (Object obj : centers) {
                    var neo4jNode = ((org.neo4j.driver.types.Node) obj);
                    Map<String, Object> nodeMap = neo4jNode.asMap();
                    String uuid = (String) nodeMap.get("uuid");
                    if (uuid != null && !nodeUuids.contains(uuid)) {
                        nodeUuids.add(uuid);
                        centerUuids.add(uuid);
                        nodes.add(buildNodeVO(nodeMap));
                    }
                }
            }

            // 阶段 3: 扩展 N 跳邻居（双向）
            if (!centerUuids.isEmpty()) {
                String expandCypher =
                    "MATCH (center:Episode) " +
                    "WHERE center.uuid IN $uuids " +
                    "MATCH path = (center)-[*1.." + effectiveDepth + "]-(n) " +
                    "WHERE n.graph_id = $graphId AND n.invalid_at IS NULL AND n <> center " +
                    "UNWIND nodes(path) as node " +
                    "WITH DISTINCT node " +
                    "RETURN node";

                Result expandResult = session.run(expandCypher,
                    Map.of("graphId", graphId, "uuids", centerUuids));
                while (expandResult.hasNext()) {
                    Record record = expandResult.next();
                    var neo4jNode = record.get("node").asNode();
                    Map<String, Object> nodeMap = neo4jNode.asMap();
                    String uuid = (String) nodeMap.get("uuid");
                    if (uuid != null && !nodeUuids.contains(uuid)) {
                        nodeUuids.add(uuid);
                        nodes.add(buildNodeVO(nodeMap));
                    }
                }

                // 阶段 4: 查询所有节点间的关系
                if (nodeUuids.size() > 1) {
                    String edgeCypher =
                        "MATCH (a)-[r]-(b) " +
                        "WHERE a.uuid IN $uuids AND b.uuid IN $uuids " +
                        "AND a.graph_id = $graphId AND b.graph_id = $graphId " +
                        "AND a.invalid_at IS NULL AND b.invalid_at IS NULL " +
                        "RETURN DISTINCT r, startNode(r).uuid as source, endNode(r).uuid as target " +
                        "LIMIT 1000";

                    Result edgeResult = session.run(edgeCypher,
                        Map.of("graphId", graphId, "uuids", new ArrayList<>(nodeUuids)));
                    while (edgeResult.hasNext()) {
                        Record record = edgeResult.next();
                        var rel = record.get("r").asRelationship();
                        String uuid = rel.get("uuid").asString();
                        if (uuid == null) {
                            uuid = java.util.UUID.randomUUID().toString();
                        }
                        if (!edgeUuidSet.contains(uuid)) {
                            edgeUuidSet.add(uuid);
                            edges.add(GraphVisualizationRespVO.EdgeVO.builder()
                                    .uuid(uuid)
                                    .source(record.get("source").asString())
                                    .target(record.get("target").asString())
                                    .type(rel.type())
                                    .fact(rel.containsKey("fact") ? rel.get("fact").asString() : null)
                                    .build());
                        }
                    }
                }
            }

            int totalPages = (int) Math.ceil((double) total / effectivePageSize);

            return GraphVisualizationRespVO.builder()
                    .nodes(nodes)
                    .edges(edges)
                    .pagination(GraphVisualizationRespVO.PaginationVO.builder()
                            .page(effectivePage)
                            .pageSize(effectivePageSize)
                            .total(total)
                            .totalPages(totalPages)
                            .build())
                    .build();
        }
    }

    @Override
    public GraphVisualizationRespVO getEntitiesVisualizationByClass(
            String graphId,
            String className,
            Integer page,
            Integer pageSize,
            Integer depth) {

        int effectivePage = page != null && page > 0 ? page : 1;
        int effectivePageSize = pageSize != null && pageSize > 0 ? pageSize : 20;
        int effectiveDepth = depth != null && depth >= 1 && depth <= 3 ? depth : 2;
        int skip = (effectivePage - 1) * effectivePageSize;

        try (Session session = neo4jDriver.session()) {
            // 阶段 1: 统计总数
            String countCypher =
                "MATCH (n:Entity {graph_id: $graphId, type: $className}) " +
                "WHERE n.invalid_at IS NULL " +
                "RETURN count(n) as total";
            Result countResult = session.run(countCypher,
                Map.of("graphId", graphId, "className", className));
            long total = countResult.hasNext() ? countResult.next().get("total").asLong() : 0;

            // 阶段 2: 分页查询中心节点
            String centerCypher =
                "MATCH (center:Entity {graph_id: $graphId, type: $className}) " +
                "WHERE center.invalid_at IS NULL " +
                "WITH center ORDER BY center.valid_at DESC SKIP $skip LIMIT $limit " +
                "RETURN collect(center) as centers";

            Map<String, Object> centerParams = new HashMap<>();
            centerParams.put("graphId", graphId);
            centerParams.put("className", className);
            centerParams.put("skip", skip);
            centerParams.put("limit", effectivePageSize);

            Result centerResult = session.run(centerCypher, centerParams);
            List<GraphVisualizationRespVO.NodeVO> nodes = new ArrayList<>();
            List<GraphVisualizationRespVO.EdgeVO> edges = new ArrayList<>();
            Set<String> nodeUuids = new LinkedHashSet<>();
            Set<String> edgeUuidSet = new HashSet<>();

            List<String> centerUuids = new ArrayList<>();
            if (centerResult.hasNext()) {
                Record record = centerResult.next();
                List<Object> centers = record.get("centers").asList();
                for (Object obj : centers) {
                    var neo4jNode = ((org.neo4j.driver.types.Node) obj);
                    Map<String, Object> nodeMap = neo4jNode.asMap();
                    String uuid = (String) nodeMap.get("uuid");
                    if (uuid != null && !nodeUuids.contains(uuid)) {
                        nodeUuids.add(uuid);
                        centerUuids.add(uuid);
                        nodes.add(buildNodeVO(nodeMap));
                    }
                }
            }

            // 阶段 3: 扩展 N 跳邻居（双向）
            if (!centerUuids.isEmpty()) {
                String expandCypher =
                    "MATCH (center:Entity) " +
                    "WHERE center.uuid IN $uuids " +
                    "MATCH path = (center)-[*1.." + effectiveDepth + "]-(n:Entity) " +
                    "WHERE n.graph_id = $graphId AND n.invalid_at IS NULL AND n <> center " +
                    "UNWIND nodes(path) as node " +
                    "WITH DISTINCT node " +
                    "RETURN node";

                Result expandResult = session.run(expandCypher,
                    Map.of("graphId", graphId, "uuids", centerUuids));
                while (expandResult.hasNext()) {
                    Record record = expandResult.next();
                    var neo4jNode = record.get("node").asNode();
                    Map<String, Object> nodeMap = neo4jNode.asMap();
                    String uuid = (String) nodeMap.get("uuid");
                    if (uuid != null && !nodeUuids.contains(uuid)) {
                        nodeUuids.add(uuid);
                        nodes.add(buildNodeVO(nodeMap));
                    }
                }

                // 阶段 4: 查询所有节点间的关系
                if (nodeUuids.size() > 1) {
                    String edgeCypher =
                        "MATCH (a:Entity)-[r]-(b:Entity) " +
                        "WHERE a.uuid IN $uuids AND b.uuid IN $uuids " +
                        "AND a.graph_id = $graphId AND b.graph_id = $graphId " +
                        "AND a.invalid_at IS NULL AND b.invalid_at IS NULL " +
                        "RETURN DISTINCT r, startNode(r).uuid as source, endNode(r).uuid as target " +
                        "LIMIT 1000";

                    Result edgeResult = session.run(edgeCypher,
                        Map.of("graphId", graphId, "uuids", new ArrayList<>(nodeUuids)));
                    while (edgeResult.hasNext()) {
                        Record record = edgeResult.next();
                        var rel = record.get("r").asRelationship();
                        String uuid = rel.get("uuid").asString();
                        if (uuid == null) {
                            uuid = java.util.UUID.randomUUID().toString();
                        }
                        if (!edgeUuidSet.contains(uuid)) {
                            edgeUuidSet.add(uuid);
                            edges.add(GraphVisualizationRespVO.EdgeVO.builder()
                                    .uuid(uuid)
                                    .source(record.get("source").asString())
                                    .target(record.get("target").asString())
                                    .type(rel.type())
                                    .fact(rel.containsKey("fact") ? rel.get("fact").asString() : null)
                                    .build());
                        }
                    }
                }
            }

            int totalPages = (int) Math.ceil((double) total / effectivePageSize);

            return GraphVisualizationRespVO.builder()
                    .nodes(nodes)
                    .edges(edges)
                    .pagination(GraphVisualizationRespVO.PaginationVO.builder()
                            .page(effectivePage)
                            .pageSize(effectivePageSize)
                            .total(total)
                            .totalPages(totalPages)
                            .build())
                    .build();
        }
    }

    private GraphVisualizationRespVO.NodeVO buildNodeVO(Map<String, Object> nodeMap) {
        String nodeType = (String) nodeMap.get("type");
        String nodeName = extractNodeName(nodeType, nodeMap);
        return GraphVisualizationRespVO.NodeVO.builder()
                .uuid((String) nodeMap.get("uuid"))
                .name(nodeName)
                .type(nodeType)
                .summary((String) nodeMap.get("summary"))
                .properties(extractProperties(nodeMap))
                .build();
    }

    @Override
    public GraphVisualizationRespVO getCommunityVisualization(String graphId, int limit) {
        try (Session session = neo4jDriver.session()) {
            int effectiveLimit = Math.min(limit, 500);

            String communityCypher =
                "MATCH (c:Community {graph_id: $graphId}) " +
                "RETURN c.uuid as uuid, c.name as name, c.summary as summary " +
                "ORDER BY c.member_count DESC " +
                "LIMIT $limit";

            List<GraphVisualizationRespVO.NodeVO> nodes = new ArrayList<>();
            List<GraphVisualizationRespVO.EdgeVO> edges = new ArrayList<>();
            List<String> communityUuids = new ArrayList<>();

            Result communityResult = session.run(communityCypher, Map.of("graphId", graphId, "limit", effectiveLimit));
            while (communityResult.hasNext()) {
                Record record = communityResult.next();
                String uuid = record.get("uuid").asString();
                communityUuids.add(uuid);
                nodes.add(GraphVisualizationRespVO.NodeVO.builder()
                        .uuid(uuid)
                        .name(record.get("name").isNull() ? "Community" : record.get("name").asString())
                        .type("Community")
                        .summary(record.get("summary").isNull() ? null : record.get("summary").asString())
                        .build());
            }

            if (!communityUuids.isEmpty()) {
                Set<String> seenMemberUuids = new java.util.HashSet<>();
                String memberCypher =
                    "MATCH (c:Community {graph_id: $graphId})-[r:HAS_MEMBER]->(n:Entity) " +
                    "WHERE c.uuid IN $uuids AND n.invalid_at IS NULL " +
                    "RETURN c.uuid as communityUuid, n.uuid as uuid, n.type as type, n.summary as summary, " +
                    "       n.courtName as courtName, n.partyName as partyName, " +
                    "       n.caseName as caseName, n.caseNumber as caseNumber, " +
                    "       n.articleNumber as articleNumber, n.lawName as lawName, " +
                    "       n.judgeName as judgeName, n.documentNumber as documentNumber, " +
                    "       n.agreementNumber as agreementNumber, n.evidenceNumber as evidenceNumber, " +
                    "       n.reasoning as reasoning, n.factDescription as factDescription, " +
                    "       n.name as genericName " +
                    "LIMIT 500";

                Result memberResult = session.run(memberCypher, Map.of("graphId", graphId, "uuids", communityUuids));
                while (memberResult.hasNext()) {
                    Record record = memberResult.next();
                    String nodeUuid = record.get("uuid").asString();
                    if (seenMemberUuids.contains(nodeUuid)) {
                        continue;
                    }
                    seenMemberUuids.add(nodeUuid);

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
                    nodeData.put("name", record.get("genericName").isNull() ? null : record.get("genericName").asString());

                    String nodeType = record.get("type").isNull() ? null : record.get("type").asString();
                    String nodeName = extractNodeName(nodeType, nodeData);

                    nodes.add(GraphVisualizationRespVO.NodeVO.builder()
                            .uuid(nodeUuid)
                            .name(nodeName)
                            .type(nodeType)
                            .summary(record.get("summary").isNull() ? null : record.get("summary").asString())
                            .build());

                    edges.add(GraphVisualizationRespVO.EdgeVO.builder()
                            .uuid(java.util.UUID.randomUUID().toString())
                            .source(record.get("communityUuid").asString())
                            .target(nodeUuid)
                            .type("HAS_MEMBER")
                            .build());
                }

                Set<String> memberUuids = nodes.stream()
                        .filter(n -> !"Community".equals(n.getType()))
                        .map(GraphVisualizationRespVO.NodeVO::getUuid)
                        .collect(Collectors.toSet());

                if (!memberUuids.isEmpty()) {
                    String edgeCypher =
                        "MATCH (a:Entity {graph_id: $graphId})-[r]->(b:Entity {graph_id: $graphId}) " +
                        "WHERE a.uuid IN $uuids AND b.uuid IN $uuids " +
                        "AND a.invalid_at IS NULL AND b.invalid_at IS NULL " +
                        "RETURN a.uuid as source, b.uuid as target, type(r) as type, r.uuid as uuid, r.fact as fact " +
                        "LIMIT 500";

                    Result edgeResult = session.run(edgeCypher, Map.of("graphId", graphId, "uuids", new ArrayList<>(memberUuids)));
                    while (edgeResult.hasNext()) {
                        Record record = edgeResult.next();
                        edges.add(GraphVisualizationRespVO.EdgeVO.builder()
                                .uuid(record.get("uuid").asString())
                                .source(record.get("source").asString())
                                .target(record.get("target").asString())
                                .type(record.get("type").asString())
                                .fact(record.containsKey("fact") && !record.get("fact").isNull() ? record.get("fact").asString() : null)
                                .build());
                    }
                }
            }

            return GraphVisualizationRespVO.builder()
                    .nodes(nodes)
                    .edges(edges)
                    .build();
        }
    }

    private Map<String, Object> extractProperties(Map<String, Object> nodeMap) {
        Set<String> systemProps = Set.of("uuid", "name", "type", "summary", "graph_id", "embedding", "valid_at", "invalid_at");
        return nodeMap.entrySet().stream()
                .filter(e -> !systemProps.contains(e.getKey()))
                .filter(e -> e.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
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
            case "CommercialMediationOrganization", "Mediator" -> (String) nodeMap.get("name");
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
}
