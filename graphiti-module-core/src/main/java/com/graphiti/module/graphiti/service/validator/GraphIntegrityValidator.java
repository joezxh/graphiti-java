package com.graphiti.module.graphiti.service.validator;

import com.graphiti.module.graphiti.service.GraphNeo4jService;
import com.graphiti.module.graphiti.vo.ontology.GraphIntegrityResultVO;
import com.graphiti.module.graphiti.vo.ontology.GraphIntegrityResultVO.ViolationVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * L6 图谱完整性验证器
 * 通过 Neo4j Cypher 查询检测图谱中的完整性问题
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GraphIntegrityValidator {

    private final GraphNeo4jService graphNeo4jService;

    /**
     * 执行完整性检查
     * @param graphId 图谱ID
     * @param checkTypes 检查类型列表（ISOLATED_NODE, REQUIRED_RELATION, DOMAIN_RANGE）
     * @return 检查结果列表
     */
    public List<GraphIntegrityResultVO> validate(String graphId, List<String> checkTypes) {
        List<GraphIntegrityResultVO> results = new ArrayList<>();

        if (checkTypes == null || checkTypes.isEmpty()) {
            checkTypes = List.of("ISOLATED_NODE", "REQUIRED_RELATION", "DOMAIN_RANGE");
        }

        Driver driver = graphNeo4jService.getNeo4jDriver();

        for (String checkType : checkTypes) {
            try {
                GraphIntegrityResultVO result = switch (checkType) {
                    case "ISOLATED_NODE" -> checkIsolatedNodes(graphId, driver);
                    case "REQUIRED_RELATION" -> checkRequiredRelations(graphId, driver);
                    case "DOMAIN_RANGE" -> checkDomainRange(graphId, driver);
                    default -> {
                        log.warn("未知的检查类型: {}", checkType);
                        yield null;
                    }
                };

                if (result != null) {
                    results.add(result);
                }
            } catch (Exception e) {
                log.error("完整性检查失败: checkType={}, error={}", checkType, e.getMessage(), e);
                results.add(GraphIntegrityResultVO.builder()
                        .checkType(checkType)
                        .passed(false)
                        .violationCount(0)
                        .violations(List.of())
                        .build());
            }
        }

        return results;
    }

    /**
     * 检查孤立节点（没有任何关系的节点）
     */
    private GraphIntegrityResultVO checkIsolatedNodes(String graphId, Driver driver) {
        String cypher = """
                MATCH (n:Entity {graph_id: $graphId})
                WHERE NOT (n)--()
                RETURN n.uuid AS uuid, n.name AS name, n.type AS type
                LIMIT 1000
                """;

        List<ViolationVO> violations = new ArrayList<>();
        try (Session session = driver.session()) {
            Result result = session.run(cypher, Map.of("graphId", graphId));
            while (result.hasNext()) {
                Record record = result.next();
                violations.add(ViolationVO.builder()
                        .nodeUuid(record.get("uuid").asString(""))
                        .nodeName(record.get("name").asString("Unknown"))
                        .nodeType(record.get("type").asString("Entity"))
                        .violationType("ISOLATED_NODE")
                        .description("节点没有任何关系，可能是孤立节点")
                        .context(Map.of("graphId", graphId))
                        .build());
            }
        }

        boolean passed = violations.isEmpty();
        log.info("孤立节点检查完成: graphId={}, passed={}, count={}", graphId, passed, violations.size());

        return GraphIntegrityResultVO.builder()
                .checkType("ISOLATED_NODE")
                .passed(passed)
                .violationCount(violations.size())
                .violations(violations)
                .build();
    }

    /**
     * 检查必需关系缺失（例如：Person 节点必须有 name 属性）
     */
    private GraphIntegrityResultVO checkRequiredRelations(String graphId, Driver driver) {
        // 示例：检查所有 Person 类型节点是否有至少一个 RELATES_TO 关系
        String cypher = """
                MATCH (n:Entity {graph_id: $graphId})
                WHERE n.type = 'Person'
                AND NOT (n)-[:RELATES_TO]-()
                RETURN n.uuid AS uuid, n.name AS name, n.type AS type
                LIMIT 1000
                """;

        List<ViolationVO> violations = new ArrayList<>();
        try (Session session = driver.session()) {
            Result result = session.run(cypher, Map.of("graphId", graphId));
            while (result.hasNext()) {
                Record record = result.next();
                violations.add(ViolationVO.builder()
                        .nodeUuid(record.get("uuid").asString(""))
                        .nodeName(record.get("name").asString("Unknown"))
                        .nodeType(record.get("type").asString("Person"))
                        .violationType("REQUIRED_RELATION")
                        .description("Person 类型节点缺少 RELATES_TO 关系")
                        .context(Map.of("expectedRelation", "RELATES_TO"))
                        .build());
            }
        }

        boolean passed = violations.isEmpty();
        log.info("必需关系检查完成: graphId={}, passed={}, count={}", graphId, passed, violations.size());

        return GraphIntegrityResultVO.builder()
                .checkType("REQUIRED_RELATION")
                .passed(passed)
                .violationCount(violations.size())
                .violations(violations)
                .build();
    }

    /**
     * 检查 domain/range 违规（关系的两端节点类型是否符合本体定义）
     */
    private GraphIntegrityResultVO checkDomainRange(String graphId, Driver driver) {
        // 示例：检查 KNOWS 关系是否只连接 Person -> Person
        String cypher = """
                MATCH (a:Entity {graph_id: $graphId})-[r:RELATES_TO]->(b:Entity {graph_id: $graphId})
                WHERE r.type = 'KNOWS'
                AND (a.type <> 'Person' OR b.type <> 'Person')
                RETURN a.uuid AS sourceUuid, a.name AS sourceName, a.type AS sourceType,
                       b.uuid AS targetUuid, b.name AS targetName, b.type AS targetType,
                       r.type AS relationType
                LIMIT 1000
                """;

        List<ViolationVO> violations = new ArrayList<>();
        try (Session session = driver.session()) {
            Result result = session.run(cypher, Map.of("graphId", graphId));
            while (result.hasNext()) {
                Record record = result.next();
                violations.add(ViolationVO.builder()
                        .nodeUuid(record.get("sourceUuid").asString(""))
                        .nodeName(record.get("sourceName").asString("Unknown"))
                        .nodeType(record.get("sourceType").asString("Entity"))
                        .violationType("DOMAIN_RANGE")
                        .description(String.format("KNOWS 关系连接了非 Person 节点: %s(%s) -> %s(%s)",
                                record.get("sourceName").asString(""),
                                record.get("sourceType").asString(""),
                                record.get("targetName").asString(""),
                                record.get("targetType").asString("")))
                        .context(Map.of(
                                "relationType", record.get("relationType").asString("KNOWS"),
                                "targetUuid", record.get("targetUuid").asString(""),
                                "expectedDomain", "Person",
                                "expectedRange", "Person"
                        ))
                        .build());
            }
        }

        boolean passed = violations.isEmpty();
        log.info("Domain/Range 检查完成: graphId={}, passed={}, count={}", graphId, passed, violations.size());

        return GraphIntegrityResultVO.builder()
                .checkType("DOMAIN_RANGE")
                .passed(passed)
                .violationCount(violations.size())
                .violations(violations)
                .build();
    }
}
