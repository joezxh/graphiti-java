package com.graphiti.module.graphiti.service.impl;

import com.graphiti.module.graphiti.service.CommunityService;
import com.graphiti.module.graphiti.service.LlmClientService;
import com.graphiti.module.graphiti.util.BinaryTreeSummarizer;
import com.graphiti.module.graphiti.util.LabelPropagation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 社区发现服务实现
 *
 * <p>参考 Python 实现：graphiti_core/utils/maintenance/community_operations.py
 *
 * <p>核心算法：
 * <ol>
 *   <li>加权标签传播算法（Weighted Label Propagation）</li>
 *   <li>二叉树合并摘要生成（Binary Tree Summarization）</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityServiceImpl implements CommunityService {

    private final Driver neo4jDriver;
    private final LlmClientService llmClientService;

    // 最大并发数
    private static final int MAX_COMMUNITY_BUILD_CONCURRENCY = 10;

    // 社区节点最小成员数
    private static final int MIN_COMMUNITY_SIZE = 2;

    @Override
    public Map<String, Object> buildCommunities(String graphId) {
        // 1. 清除现有社区
        removeCommunities(graphId);

        // 2. 执行加权标签传播算法
        LabelPropagation.CommunityResult communityResult = detectCommunitiesByLabelPropagation(graphId);
        Map<String, Set<String>> communities = communityResult.getCommunityMembers();

        log.info("标签传播完成：检测到 {} 个社区，迭代 {} 次",
                communities.size(), communityResult.getIterationCount());

        // 3. 使用二叉树合并策略生成社区摘要并创建社区节点
        int communityCount = buildCommunityNodes(graphId, communities);

        Map<String, Object> result = new HashMap<>();
        result.put("communityCount", communityCount);
        result.put("iterationCount", communityResult.getIterationCount());
        result.put("message", "社区构建完成");
        return result;
    }

    /**
     * 使用加权标签传播算法检测社区
     */
    private LabelPropagation.CommunityResult detectCommunitiesByLabelPropagation(String graphId) {
        // 构建图结构
        LabelPropagation.Graph graph = new LabelPropagation.Graph();

        String cypher =
            "MATCH (a:Entity {graph_id: $graph_id})-[r:RELATES_TO]->(b:Entity {graph_id: $graph_id}) " +
            "WHERE r.invalid_at IS NULL " +
            "RETURN a.uuid as source, b.uuid as target, count(r) as edge_count";

        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, Values.parameters("graph_id", graphId));
            while (result.hasNext()) {
                Record record = result.next();
                String source = record.get("source").asString();
                String target = record.get("target").asString();
                int edgeCount = record.get("edge_count").asInt();
                graph.addEdge(source, target, edgeCount);
            }
        }

        // 执行标签传播
        return LabelPropagation.detect(graph);
    }

    /**
     * 构建社区节点（使用二叉树合并策略生成摘要）
     */
    private int buildCommunityNodes(String graphId, Map<String, Set<String>> communities) {
        BinaryTreeSummarizer summarizer = new BinaryTreeSummarizer(llmClientService);
        int count = 0;

        try {
            // 并行构建社区
            List<Map.Entry<String, Set<String>>> communityList = new ArrayList<>(communities.entrySet());
            List<CompletableFuture<CommunityBuildResult>> futures = new ArrayList<>();

            for (Map.Entry<String, Set<String>> entry : communityList) {
                String communityId = entry.getKey();
                Set<String> memberUuids = entry.getValue();

                if (memberUuids.size() < MIN_COMMUNITY_SIZE) {
                    continue;  // 跳过太小的社区
                }

                final String commId = communityId;
                final Set<String> members = memberUuids;
                futures.add(CompletableFuture.supplyAsync(() ->
                    buildSingleCommunity(graphId, commId, members, summarizer)));
            }

            // 等待所有社区构建完成
            for (CompletableFuture<CommunityBuildResult> future : futures) {
                try {
                    CommunityBuildResult buildResult = future.join();
                    if (buildResult.success) {
                        count++;
                    }
                } catch (Exception e) {
                    log.error("社区构建失败：{}", e.getMessage());
                }
            }
        } finally {
            summarizer.shutdown();
        }

        return count;
    }

    /**
     * 构建单个社区
     */
    private CommunityBuildResult buildSingleCommunity(String graphId, String communityId,
                                                     Set<String> memberUuids, BinaryTreeSummarizer summarizer) {
        // 1. 获取成员信息
        List<String> memberUuidList = new ArrayList<>(memberUuids);

        Map<String, String> memberSummaries = getMemberSummaries(graphId, memberUuidList);

        // 2. 使用二叉树合并生成摘要
        List<String> summaries = new ArrayList<>(memberSummaries.values());
        String mergedSummary = summarizer.summarize(summaries);

        // 3. 生成社区名称
        String communityName = summarizer.generateCommunityName(mergedSummary);

        // 4. 创建社区节点并关联成员
        String communityUuid = UUID.randomUUID().toString().replace("-", "");

        String createCypher =
            "CREATE (c:Community {graph_id: $graph_id, uuid: $uuid, name: $name, " +
            "summary: $summary, member_count: $member_count, " +
            "community_type: $community_type, legal_domain: $legal_domain, " +
            "jurisdiction: $jurisdiction, practice_type: $practice_type}) " +
            "WITH c " +
            "UNWIND $member_uuids as memberUuid " +
            "MATCH (m:Entity {graph_id: $graph_id, uuid: memberUuid}) " +
            "CREATE (m)-[:HAS_COMMUNITY]->(c)";

        try (Session session = neo4jDriver.session()) {
            session.run(createCypher, Values.parameters(
                "graph_id", graphId,
                "uuid", communityUuid,
                "name", communityName,
                "summary", mergedSummary,
                "member_count", memberUuids.size(),
                "community_type", resolveCommunityType(communityName),
                "legal_domain", resolveLegalDomain(resolveCommunityType(communityName)),
                "jurisdiction", "JURISDICTION_CN",
                "practice_type", "PRACTICE_JUDICIAL",
                "member_uuids", memberUuidList
            ));

            log.debug("社区构建成功：uuid={}, name={}, members={}",
                    communityUuid, communityName, memberUuids.size());

            return new CommunityBuildResult(true, communityUuid);
        } catch (Exception e) {
            log.error("社区创建失败：{}", e.getMessage());
            return new CommunityBuildResult(false, null);
        }
    }

    /**
     * 获取成员的摘要信息
     */
    private Map<String, String> getMemberSummaries(String graphId, List<String> memberUuids) {
        Map<String, String> summaries = new HashMap<>();

        String cypher =
            "MATCH (m:Entity {graph_id: $graph_id}) " +
            "WHERE m.uuid IN $uuids " +
            "RETURN m.uuid as uuid, m.name as name, m.summary as summary";

        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, Values.parameters(
                "graph_id", graphId,
                "uuids", memberUuids
            ));

            while (result.hasNext()) {
                Record record = result.next();
                String uuid = record.get("uuid").asString();
                String name = record.get("name").asString();
                String summary = record.get("summary").isNull() ? "" : record.get("summary").asString();
                summaries.put(uuid, name + ": " + summary);
            }
        }

        return summaries;
    }

    /**
     * 根据社区名称自动推断 community_type（V3.0.0）
     */
    private String resolveCommunityType(String communityName) {
        if (communityName == null) return "top_level";
        if (communityName.contains("公司解散") || communityName.contains("股权转让") || communityName.contains("买卖合同"))
            return "corporate_dispute";
        if (communityName.contains("劳动") || communityName.contains("工资") || communityName.contains("社保"))
            return "labor_dispute";
        if (communityName.contains("专利") || communityName.contains("商标") || communityName.contains("著作权"))
            return "intellectual_property";
        if (communityName.contains("调解") || communityName.contains("和解") || communityName.contains("仲裁"))
            return "dispute_resolution";
        return "top_level";
    }

    /**
     * 根据 community_type 映射 legal_domain（V3.0.0）
     */
    private String resolveLegalDomain(String communityType) {
        return switch (communityType) {
            case "corporate_dispute", "dispute_resolution", "procedural_law" -> "DOMAIN_CIVIL";
            case "intellectual_property" -> "DOMAIN_IP";
            case "labor_dispute" -> "DOMAIN_LABOR";
            case "foundational_civil_law" -> "DOMAIN_CIVIL";
            case "top_level" -> "DOMAIN_ROOT";
            default -> "DOMAIN_ROOT";
        };
    }

    @Override
    public List<Map<String, Object>> listCommunities(String graphId) {
        String cypher =
            "MATCH (c:Community {graph_id: $graph_id}) " +
            "RETURN c.uuid as uuid, c.name as name, c.summary as summary, " +
            "       c.member_count as member_count, c.parent_community_uuid as parentCommunityUuid, " +
            "       c.community_type as communityType, c.legal_domain as legalDomain, " +
            "       c.jurisdiction as jurisdiction, c.practice_type as practiceType, " +
            "       c.created_at as createdAt " +
            "ORDER BY c.member_count DESC";

        List<Map<String, Object>> communities = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, Values.parameters("graph_id", graphId));
            while (result.hasNext()) {
                Record record = result.next();
                Map<String, Object> community = new HashMap<>();
                community.put("uuid", record.get("uuid").asString());
                community.put("name", record.get("name").asString());
                community.put("summary", record.get("summary").asString());
                community.put("memberCount", record.get("member_count").asInt());
                community.put("parentCommunityUuid",
                    record.get("parentCommunityUuid").isNull() ? null : record.get("parentCommunityUuid").asString());
                community.put("communityType",
                    record.get("communityType").isNull() ? null : record.get("communityType").asString());
                community.put("legalDomain",
                    record.get("legalDomain").isNull() ? null : record.get("legalDomain").asString());
                community.put("jurisdiction",
                    record.get("jurisdiction").isNull() ? null : record.get("jurisdiction").asString());
                community.put("practiceType",
                    record.get("practiceType").isNull() ? null : record.get("practiceType").asString());
                community.put("createdAt",
                    record.get("createdAt").isNull() ? null : record.get("createdAt").asString());
                communities.add(community);
            }
        }
        return communities;
    }

    @Override
    public List<Map<String, Object>> searchCommunities(String graphId, String query) {
        String cypher =
            "MATCH (c:Community {graph_id: $graph_id}) " +
            "WHERE c.name CONTAINS $query OR c.summary CONTAINS $query " +
            "RETURN c.uuid as uuid, c.name as name, c.summary as summary, " +
            "       c.member_count as member_count, c.community_type as communityType, " +
            "       c.legal_domain as legalDomain, c.jurisdiction as jurisdiction, " +
            "       c.practice_type as practiceType, c.created_at as createdAt " +
            "ORDER BY c.member_count DESC " +
            "LIMIT 10";

        List<Map<String, Object>> communities = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, Values.parameters(
                "graph_id", graphId,
                "query", query
            ));
            while (result.hasNext()) {
                Record record = result.next();
                Map<String, Object> community = new HashMap<>();
                community.put("uuid", record.get("uuid").asString());
                community.put("name", record.get("name").asString());
                community.put("summary", record.get("summary").asString());
                community.put("memberCount", record.get("member_count").asInt());
                community.put("communityType",
                    record.get("communityType").isNull() ? null : record.get("communityType").asString());
                community.put("legalDomain",
                    record.get("legalDomain").isNull() ? null : record.get("legalDomain").asString());
                community.put("jurisdiction",
                    record.get("jurisdiction").isNull() ? null : record.get("jurisdiction").asString());
                community.put("practiceType",
                    record.get("practiceType").isNull() ? null : record.get("practiceType").asString());
                community.put("createdAt",
                    record.get("createdAt").isNull() ? null : record.get("createdAt").asString());
                communities.add(community);
            }
        }
        return communities;
    }

    @Override
    public void removeCommunities(String graphId) {
        String cypher =
            "MATCH (c:Community {graph_id: $graph_id}) " +
            "DETACH DELETE c";

        try (Session session = neo4jDriver.session()) {
            session.run(cypher, Values.parameters("graph_id", graphId));
            log.info("已清除图谱 {} 的所有社区", graphId);
        }
    }

    /**
     * 社区构建结果
     */
    private static class CommunityBuildResult {
        final boolean success;
        final String communityUuid;

        CommunityBuildResult(boolean success, String communityUuid) {
            this.success = success;
            this.communityUuid = communityUuid;
        }
    }
}
