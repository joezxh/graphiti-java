package com.graphiti.module.graphiti.service;

import com.graphiti.module.graphiti.dal.dataobject.GraphMetadataDO;
import com.graphiti.module.graphiti.dal.mysql.GraphMetadataMapper;
import com.graphiti.module.graphiti.vo.ide.GraphVisualizationRespVO;
import com.graphiti.module.graphiti.vo.node.NodeInfoRespVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Result;
import org.neo4j.driver.Record;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 图谱可视化服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GraphVisualizationService {

    private final Driver neo4jDriver;
    private final GraphMetadataMapper graphMetadataMapper;
    private final GraphNeo4jService graphNeo4jService;

    /**
     * 获取图谱可视化数据
     */
    public GraphVisualizationRespVO getVisualizationData(
            String graphId,
            String layout,
            Integer page,
            Integer pageSize,
            String classType,
            String keyword) {
        
        try (Session session = neo4jDriver.session()) {
            // 构建查询
            StringBuilder cypherBuilder = new StringBuilder();
            cypherBuilder.append("MATCH (n:Entity {group_id: $graphId}) ");
            
            Map<String, Object> params = new HashMap<>();
            params.put("graphId", graphId);
            
            // 类型过滤
            if (classType != null && !classType.isBlank()) {
                cypherBuilder.append("WHERE n.type = $classType ");
                params.put("classType", classType);
            }
            
            // 关键词过滤
            if (keyword != null && !keyword.isBlank()) {
                cypherBuilder.append("AND (n.name CONTAINS $keyword OR n.summary CONTAINS $keyword) ");
                params.put("keyword", keyword);
            }
            
            // 统计总数
            // ... existing code ...
            // 关键词过滤
            if (keyword != null && !keyword.isBlank()) {
                cypherBuilder.append("AND (n.name CONTAINS $keyword OR n.summary CONTAINS $keyword) ");
                params.put("keyword", keyword);
            }

            // 统计总数
            String baseQuery = cypherBuilder.toString();
            String countCypher = baseQuery.replaceAll("MATCH \\(n:Entity", "MATCH (n:Entity") + " RETURN count(n) as total";
            Result countResult = session.run(countCypher, params);

            long total = countResult.hasNext() ? countResult.next().get("total").asLong() : 0;
            
            // 分页查询节点
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
                
                GraphVisualizationRespVO.NodeVO nodeVO = GraphVisualizationRespVO.NodeVO.builder()
                        .uuid((String) nodeMap.get("uuid"))
                        .name((String) nodeMap.get("name"))
                        .type((String) nodeMap.get("type"))
                        .summary((String) nodeMap.get("summary"))
                        .properties(extractProperties(nodeMap))
                        .build();
                
                nodes.add(nodeVO);
                nodeUuids.add((String) nodeMap.get("uuid"));
            }
            
            // 查询边
            List<GraphVisualizationRespVO.EdgeVO> edges = new ArrayList<>();
            if (!nodeUuids.isEmpty()) {
                String edgeCypher = 
                    "MATCH (a:Entity {group_id: $graphId})-[r]->(b:Entity {group_id: $graphId}) " +
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
            
            // 统计聚合
            String aggCypher = 
                "MATCH (n:Entity {group_id: $graphId}) " +
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

    /**
     * 获取图谱元数据
     */
    public Map<String, Object> getGraphMetadata(String graphId) {
        GraphMetadataDO metadata = graphMetadataMapper.selectByGraphId(graphId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("graphId", graphId);
        
        if (metadata != null) {
            result.put("name", metadata.getName());
            result.put("description", metadata.getDescription());
            result.put("status", metadata.getStatus());
        }
        
        // 从 Neo4j 获取统计
        try (Session session = neo4jDriver.session()) {
            // 节点数
            String nodeCountCypher = "MATCH (n:Entity {group_id: $graphId}) WHERE n.invalid_at IS NULL RETURN count(n) as count";
            Result nodeResult = session.run(nodeCountCypher, Map.of("graphId", graphId));
            long nodeCount = nodeResult.hasNext() ? nodeResult.next().get("count").asLong() : 0;
            result.put("nodeCount", nodeCount);
            
            // 边数
            String edgeCountCypher = 
                "MATCH (a:Entity {group_id: $graphId})-[r]->(b:Entity {group_id: $graphId}) " +
                "WHERE a.invalid_at IS NULL AND b.invalid_at IS NULL " +
                "RETURN count(r) as count";
            Result edgeResult = session.run(edgeCountCypher, Map.of("graphId", graphId));
            long edgeCount = edgeResult.hasNext() ? edgeResult.next().get("count").asLong() : 0;
            result.put("edgeCount", edgeCount);
            
            // 类数量（通过类型统计）
            String classCountCypher = 
                "MATCH (n:Entity {group_id: $graphId}) WHERE n.invalid_at IS NULL " +
                "RETURN count(DISTINCT n.type) as count";
            Result classResult = session.run(classCountCypher, Map.of("graphId", graphId));
            long classCount = classResult.hasNext() ? classResult.next().get("count").asLong() : 0;
            result.put("classCount", classCount);
            
            // TODO: 从 Episode 表获取事件数
            result.put("episodeCount", 0);
            result.put("communityCount", 0);
        }
        
        return result;
    }

    /**
     * 获取节点详情
     */
    public NodeInfoRespVO getNodeDetail(String graphId, String nodeUuid) {
        if (nodeUuid == null || nodeUuid.isBlank() || "undefined".equals(nodeUuid)) {
            throw new IllegalArgumentException("invalidated Node UUID: " + nodeUuid);
        }
        try (Session session = neo4jDriver.session()) {
            String cypher = 
                "MATCH (n:Entity {group_id: $graphId, uuid: $uuid}) " +
                "RETURN n";
            
            Result result = session.run(cypher, Map.of("graphId", graphId, "uuid", nodeUuid));
            
            if (result.hasNext()) {
                Record record = result.next();
                var neo4jNode = record.get("n").asNode();
                Map<String, Object> nodeMap = neo4jNode.asMap();
                
                NodeInfoRespVO nodeVO = new NodeInfoRespVO();
                nodeVO.setUuid((String) nodeMap.get("uuid"));
                nodeVO.setName((String) nodeMap.get("name"));
                nodeVO.setType((String) nodeMap.get("type"));
                nodeVO.setSummary((String) nodeMap.get("summary"));
                nodeVO.setProperties(extractProperties(nodeMap));
                
                return nodeVO;
            }
            
            throw new RuntimeException("Node not found: " + nodeUuid);
        }
    }

    /**
     * 创建节点
     */
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

    /**
     * 更新节点
     */
    public NodeInfoRespVO updateNode(String graphId, String nodeUuid, Map<String, Object> nodeData) {
        try (Session session = neo4jDriver.session()) {
            StringBuilder cypher = new StringBuilder("MATCH (n:Entity {group_id: $graphId, uuid: $uuid}) ");
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

    /**
     * 删除节点
     */
    public void deleteNode(String graphId, String nodeUuid) {
        try (Session session = neo4jDriver.session()) {
            // 删除关联边
            String deleteEdgesCypher = 
                "MATCH (a:Entity {group_id: $graphId, uuid: $uuid})-[r]->(b:Entity {group_id: $graphId}) " +
                "DELETE r";
            session.run(deleteEdgesCypher, Map.of("graphId", graphId, "uuid", nodeUuid));
            
            String deleteEdgesCypher2 = 
                "MATCH (a:Entity {group_id: $graphId})-[r]->(b:Entity {group_id: $graphId, uuid: $uuid}) " +
                "DELETE r";
            session.run(deleteEdgesCypher2, Map.of("graphId", graphId, "uuid", nodeUuid));
            
            // 软删除节点
            String deleteNodeCypher = 
                "MATCH (n:Entity {group_id: $graphId, uuid: $uuid}) " +
                "SET n.invalid_at = timestamp()";
            session.run(deleteNodeCypher, Map.of("graphId", graphId, "uuid", nodeUuid));
        }
    }

    /**
     * 创建边
     */
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

    /**
     * 展开邻居节点
     */
    public GraphVisualizationRespVO expandNeighbors(String graphId, String nodeUuid, Map<String, Object> options) {
        try (Session session = neo4jDriver.session()) {
            int depth = options != null && options.containsKey("depth") 
                    ? ((Number) options.get("depth")).intValue() 
                    : 1;
            int maxNodes = options != null && options.containsKey("maxNodes")
                    ? ((Number) options.get("maxNodes")).intValue()
                    : 50;
            
            String cypher = 
                "MATCH (n:Entity {group_id: $graphId, uuid: $nodeUuid})-[r*1.." + depth + "]-(neighbor:Entity {group_id: $graphId}) " +
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
                
                GraphVisualizationRespVO.NodeVO nodeVO = GraphVisualizationRespVO.NodeVO.builder()
                        .uuid((String) nodeMap.get("uuid"))
                        .name((String) nodeMap.get("name"))
                        .type((String) nodeMap.get("type"))
                        .summary((String) nodeMap.get("summary"))
                        .properties(extractProperties(nodeMap))
                        .build();
                
                nodes.add(nodeVO);
                nodeUuids.add((String) nodeMap.get("uuid"));
            }
            
            // 查询新节点之间的边
            List<GraphVisualizationRespVO.EdgeVO> edges = new ArrayList<>();
            if (!nodeUuids.isEmpty()) {
                String edgeCypher = 
                    "MATCH (a:Entity {group_id: $graphId})-[r]->(b:Entity {group_id: $graphId}) " +
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

    /**
     * 从节点属性中提取业务属性（排除系统属性）
     */
    private Map<String, Object> extractProperties(Map<String, Object> nodeMap) {
        Set<String> systemProps = Set.of("uuid", "name", "type", "summary", "group_id", "embedding", "valid_at", "invalid_at");
        return nodeMap.entrySet().stream()
                .filter(e -> !systemProps.contains(e.getKey()))
                .filter(e -> e.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
