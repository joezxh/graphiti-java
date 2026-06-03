package com.ontograph.module.graphiti.service.impl;

import com.ontograph.module.graphiti.service.EpisodeMentionsRerankerService;
import com.ontograph.module.graphiti.service.GraphNeo4jService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Values;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Episode Mentions 重排服务实现
 *
 * <p>参考 Python 实现：graphiti_core/search/search_utils.py:episode_mentions_reranker()
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EpisodeMentionsRerankerServiceImpl implements EpisodeMentionsRerankerService {

    private final GraphNeo4jService graphNeo4jService;

    @Override
    public List<String> rerankEdgesByMentions(List<String> candidateUuids, int limit) {
        Map<String, Integer> counts = getMentionCounts(candidateUuids);
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> rerankNodesByMentions(List<String> candidateUuids, int limit) {
        // 节点通过关系间接提及，统计 MENTIONS 关系数量
        Map<String, Integer> counts = getNodeMentionCounts(candidateUuids);
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, Integer> getMentionCounts(List<String> candidateUuids) {
        if (candidateUuids == null || candidateUuids.isEmpty()) {
            return Map.of();
        }

        String cypher =
            "MATCH (e:Episode)-[:MENTIONS]->(r) " +
            "WHERE r.uuid IN $uuids " +
            "RETURN r.uuid AS uuid, count(e) AS mentionCount";

        Map<String, Integer> counts = new HashMap<>();

        try (var session = graphNeo4jService.getNeo4jDriver().session()) {
            var result = session.run(cypher, Values.parameters("uuids", candidateUuids));
            while (result.hasNext()) {
                var record = result.next();
                String uuid = record.get("uuid").asString();
                int count = record.get("mentionCount").asInt();
                counts.put(uuid, count);
            }
        }

        // 补充未提及的 UUID（次数为 0）
        for (String uuid : candidateUuids) {
            counts.putIfAbsent(uuid, 0);
        }

        return counts;
    }

    private Map<String, Integer> getNodeMentionCounts(List<String> nodeUuids) {
        if (nodeUuids == null || nodeUuids.isEmpty()) {
            return Map.of();
        }

        // 节点被 Episode 通过 MENTIONS 关系直接提及
        String cypher =
            "MATCH (e:Episode)-[:MENTIONS]->(n:Entity) " +
            "WHERE n.uuid IN $uuids " +
            "RETURN n.uuid AS uuid, count(e) AS mentionCount";

        Map<String, Integer> counts = new HashMap<>();

        try (var session = graphNeo4jService.getNeo4jDriver().session()) {
            var result = session.run(cypher, Values.parameters("uuids", nodeUuids));
            while (result.hasNext()) {
                var record = result.next();
                String uuid = record.get("uuid").asString();
                int count = record.get("mentionCount").asInt();
                counts.put(uuid, count);
            }
        }

        for (String uuid : nodeUuids) {
            counts.putIfAbsent(uuid, 0);
        }

        return counts;
    }
}
