package com.ontograph.module.graphiti.service.impl;

import com.ontograph.module.graphiti.service.SagaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Saga 管理服务实现
 * 构建 Episode 之间的 NEXT_EPISODE 时序链
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SagaServiceImpl implements SagaService {

    private final Driver neo4jDriver;

    @Override
    public Map<String, Object> buildSaga(String graphId) {
        // 1. 清除现有 Saga 链
        clearSaga(graphId);

        // 2. 按 valid_at 排序获取所有 Episode
        String getEpisodes =
            "MATCH (e:Episode {graph_id: $graph_id}) " +
            "RETURN e.uuid as uuid, e.name as name, e.valid_at as valid_at " +
            "ORDER BY e.valid_at ASC";

        List<Map<String, Object>> episodes = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(getEpisodes, Values.parameters("graph_id", graphId));
            while (result.hasNext()) {
                episodes.add(result.next().asMap());
            }
        }

        // 3. 创建 NEXT_EPISODE 链
        int linkCount = 0;
        try (Session session = neo4jDriver.session()) {
            for (int i = 0; i < episodes.size() - 1; i++) {
                String currentUuid = (String) episodes.get(i).get("uuid");
                String nextUuid = (String) episodes.get(i + 1).get("uuid");

                String createLink =
                    "MATCH (current:Episode {graph_id: $graph_id, uuid: $current}) " +
                    "MATCH (next:Episode {graph_id: $graph_id, uuid: $next}) " +
                    "CREATE (current)-[:NEXT_EPISODE]->(next)";

                session.run(createLink, Values.parameters(
                    "graph_id", graphId,
                    "current", currentUuid,
                    "next", nextUuid
                ));
                linkCount++;
            }
        }

        Map<String, Object> report = new HashMap<>();
        report.put("episodeCount", episodes.size());
        report.put("linkCount", linkCount);
        report.put("message", "Saga 时序链构建完成");
        return report;
    }

    @Override
    public Map<String, Object> getSagaContext(String graphId, String episodeUuid) {
        Map<String, Object> context = new HashMap<>();

        // 获取前一个 Episode
        String prevCypher =
            "MATCH (prev:Episode {graph_id: $graph_id})-[:NEXT_EPISODE]->(current:Episode {graph_id: $graph_id, uuid: $uuid}) " +
            "RETURN prev.uuid as uuid, prev.name as name, prev.valid_at as valid_at";

        try (Session session = neo4jDriver.session()) {
            Result result = session.run(prevCypher, Values.parameters("graph_id", graphId, "uuid", episodeUuid));
            if (result.hasNext()) {
                context.put("previous", result.next().asMap());
            }
        }

        // 获取后一个 Episode
        String nextCypher =
            "MATCH (current:Episode {graph_id: $graph_id, uuid: $uuid})-[:NEXT_EPISODE]->(next:Episode {graph_id: $graph_id}) " +
            "RETURN next.uuid as uuid, next.name as name, next.valid_at as valid_at";

        try (Session session = neo4jDriver.session()) {
            Result result = session.run(nextCypher, Values.parameters("graph_id", graphId, "uuid", episodeUuid));
            if (result.hasNext()) {
                context.put("next", result.next().asMap());
            }
        }

        // 获取当前 Episode
        String currentCypher =
            "MATCH (e:Episode {graph_id: $graph_id, uuid: $uuid}) " +
            "RETURN e.uuid as uuid, e.name as name, e.valid_at as valid_at";

        try (Session session = neo4jDriver.session()) {
            Result result = session.run(currentCypher, Values.parameters("graph_id", graphId, "uuid", episodeUuid));
            if (result.hasNext()) {
                context.put("current", result.next().asMap());
            }
        }

        return context;
    }

    @Override
    public List<Map<String, Object>> getSagaTimeline(String graphId) {
        String cypher =
            "MATCH (e:Episode {graph_id: $graph_id}) " +
            "RETURN e.uuid as uuid, e.name as name, e.valid_at as valid_at, e.content as content " +
            "ORDER BY e.valid_at ASC";

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
    public void clearSaga(String graphId) {
        String cypher =
            "MATCH (e:Episode {graph_id: $graph_id})-[r:NEXT_EPISODE]->() " +
            "DELETE r";

        try (Session session = neo4jDriver.session()) {
            session.run(cypher, Values.parameters("graph_id", graphId));
            log.info("已清除图谱 {} 的 Saga 链", graphId);
        }
    }
}
