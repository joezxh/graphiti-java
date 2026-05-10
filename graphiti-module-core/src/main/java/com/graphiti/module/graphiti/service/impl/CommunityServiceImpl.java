package com.graphiti.module.graphiti.service.impl;

import com.graphiti.module.graphiti.service.CommunityService;
import com.graphiti.module.graphiti.service.LlmClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityServiceImpl implements CommunityService {

    private final Driver neo4jDriver;
    private final LlmClientService llmClientService;

    @Override
    public Map<String, Object> buildCommunities(String graphId) {
        removeCommunities(graphId);

        String cypher =
            "MATCH (n:Entity {group_id: $group_id})-[r:RELATES_TO]->(m:Entity {group_id: $group_id}) " +
            "WITH n, m " +
            "WHERE n.type = m.type OR EXISTS { " +
            "  MATCH (n)-[:RELATES_TO]->(x:Entity {group_id: $group_id})<-[:RELATES_TO]-(m) " +
            "} " +
            "WITH n, collect(m) as neighbors " +
            "WHERE size(neighbors) >= 2 " +
            "RETURN n.uuid as center_uuid, n.name as center_name, n.type as center_type, " +
            "       [neighbor in neighbors | neighbor.uuid] as member_uuids, " +
            "       [neighbor in neighbors | neighbor.name] as member_names " +
            "LIMIT 50";

        List<Map<String, Object>> communities = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, Values.parameters("group_id", graphId));
            while (result.hasNext()) {
                Record record = result.next();
                Map<String, Object> community = new HashMap<>();
                community.put("centerUuid", record.get("center_uuid").asString());
                community.put("centerName", record.get("center_name").asString());
                community.put("memberUuids", record.get("member_uuids").asList());
                community.put("memberNames", record.get("member_names").asList());
                communities.add(community);
            }
        }

        int communityCount = 0;
        for (Map<String, Object> community : communities) {
            List<String> memberNames = (List<String>) community.get("memberNames");
            String summary = llmClientService.generateCommunitySummary(memberNames);

            String communityUuid = UUID.randomUUID().toString().replace("-", "");

            String createCypher =
                "CREATE (c:Community {group_id: $group_id, uuid: $uuid, name: $name, " +
                "summary: $summary, member_count: $member_count}) " +
                "WITH c " +
                "UNWIND $member_uuids as memberUuid " +
                "MATCH (m:Entity {group_id: $group_id, uuid: memberUuid}) " +
                "CREATE (m)-[:HAS_COMMUNITY]->(c)";

            try (Session session = neo4jDriver.session()) {
                session.run(createCypher, Values.parameters(
                    "group_id", graphId,
                    "uuid", communityUuid,
                    "name", "Community-" + community.get("centerName"),
                    "summary", summary,
                    "member_count", memberNames.size(),
                    "member_uuids", community.get("memberUuids")
                ));
                communityCount++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("communityCount", communityCount);
        result.put("message", "社区构建完成");
        return result;
    }

    @Override
    public List<Map<String, Object>> listCommunities(String graphId) {
        String cypher =
            "MATCH (c:Community {group_id: $group_id}) " +
            "RETURN c.uuid as uuid, c.name as name, c.summary as summary, " +
            "       c.member_count as member_count " +
            "ORDER BY c.member_count DESC";

        List<Map<String, Object>> communities = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, Values.parameters("group_id", graphId));
            while (result.hasNext()) {
                Record record = result.next();
                Map<String, Object> community = new HashMap<>();
                community.put("uuid", record.get("uuid").asString());
                community.put("name", record.get("name").asString());
                community.put("summary", record.get("summary").asString());
                community.put("memberCount", record.get("member_count").asInt());
                communities.add(community);
            }
        }
        return communities;
    }

    @Override
    public List<Map<String, Object>> searchCommunities(String graphId, String query) {
        String cypher =
            "MATCH (c:Community {group_id: $group_id}) " +
            "WHERE c.name CONTAINS $query OR c.summary CONTAINS $query " +
            "RETURN c.uuid as uuid, c.name as name, c.summary as summary, " +
            "       c.member_count as member_count " +
            "ORDER BY c.member_count DESC " +
            "LIMIT 10";

        List<Map<String, Object>> communities = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, Values.parameters(
                "group_id", graphId,
                "query", query
            ));
            while (result.hasNext()) {
                Record record = result.next();
                Map<String, Object> community = new HashMap<>();
                community.put("uuid", record.get("uuid").asString());
                community.put("name", record.get("name").asString());
                community.put("summary", record.get("summary").asString());
                community.put("memberCount", record.get("member_count").asInt());
                communities.add(community);
            }
        }
        return communities;
    }

    @Override
    public void removeCommunities(String graphId) {
        String cypher =
            "MATCH (c:Community {group_id: $group_id}) " +
            "DETACH DELETE c";

        try (Session session = neo4jDriver.session()) {
            session.run(cypher, Values.parameters("group_id", graphId));
            log.info("已清除图谱 {} 的所有社区", graphId);
        }
    }
}
