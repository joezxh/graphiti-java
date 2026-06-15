package com.ontograph.module.graphiti.service.impl;

import com.ontograph.module.graphiti.dal.dataobject.metadata.OntCommunityTypeDO;
import com.ontograph.module.graphiti.dal.dataobject.ont.OntDefinitionDO;
import com.ontograph.module.graphiti.dal.mysql.metadata.CommunityMetadataMapper;
import com.ontograph.module.graphiti.dal.mysql.metadata.OntCommunityTypeMapper;
import com.ontograph.module.graphiti.dal.mysql.ont.OntDefinitionMapper;
import com.ontograph.module.graphiti.dto.CommunityCreateContext;
import com.ontograph.module.graphiti.dto.DomainInferenceResult;
import com.ontograph.module.graphiti.exception.CommunityMetadataCompensationException;
import com.ontograph.module.graphiti.service.CommunityService;
import com.ontograph.module.graphiti.service.DomainInferenceService;
import com.ontograph.module.graphiti.service.LlmClientService;
import com.ontograph.module.graphiti.util.BinaryTreeSummarizer;
import com.ontograph.module.graphiti.util.LabelPropagation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 社区发现服务实现（通用化改造 V3）
 *
 * <p>支持多领域（法律/金融/企业管理/医疗/社会治理），通过 ont_community_type 元数据表
 * 与 Neo4j 图数据库双写实现数据一致性。
 *
 * <p>跨库事务策略：MySQL 元数据表用 Spring @Transactional 保证原子性；
 * Neo4j 写入失败时抛 CommunityMetadataCompensationException 触发 MySQL 回滚。
 *
 * <p>参考 Python 实现：graphiti_core/utils/maintenance/community_operations.py
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommunityServiceImpl implements CommunityService {

    private final Driver neo4jDriver;
    private final LlmClientService llmClientService;
    private final OntDefinitionMapper definitionMapper;
    private final OntCommunityTypeMapper communityTypeMapper;
    private final CommunityMetadataMapper communityMetadataMapper;
    private final DomainInferenceService domainInferenceService;

    private static final int MAX_COMMUNITY_BUILD_CONCURRENCY = 10;
    private static final int MIN_COMMUNITY_SIZE = 2;

    // ==================== buildCommunities（标签传播，自动发现，不写元数据）====================

    @Override
    public Map<String, Object> buildCommunities(String graphId) {
        removeCommunities(graphId);
        LabelPropagation.CommunityResult communityResult = detectCommunitiesByLabelPropagation(graphId);
        Map<String, Set<String>> communities = communityResult.getCommunityMembers();

        log.info("标签传播完成：检测到 {} 个社区，迭代 {} 次",
                communities.size(), communityResult.getIterationCount());

        int communityCount = buildCommunityNodes(graphId, communities);

        Map<String, Object> result = new HashMap<>();
        result.put("communityCount", communityCount);
        result.put("iterationCount", communityResult.getIterationCount());
        result.put("message", "社区构建完成");
        return result;
    }

    private LabelPropagation.CommunityResult detectCommunitiesByLabelPropagation(String graphId) {
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
        return LabelPropagation.detect(graph);
    }

    private int buildCommunityNodes(String graphId, Map<String, Set<String>> communities) {
        BinaryTreeSummarizer summarizer = new BinaryTreeSummarizer(llmClientService);
        int count = 0;
        try {
            List<Map.Entry<String, Set<String>>> communityList = new ArrayList<>(communities.entrySet());
            List<CompletableFuture<CommunityBuildResult>> futures = new ArrayList<>();
            for (Map.Entry<String, Set<String>> entry : communityList) {
                String communityId = entry.getKey();
                Set<String> memberUuids = entry.getValue();
                if (memberUuids.size() < MIN_COMMUNITY_SIZE) continue;
                final String commId = communityId;
                final Set<String> members = memberUuids;
                futures.add(CompletableFuture.supplyAsync(() ->
                    buildSingleCommunity(graphId, commId, members, summarizer)));
            }
            for (CompletableFuture<CommunityBuildResult> future : futures) {
                try {
                    CommunityBuildResult buildResult = future.join();
                    if (buildResult.success) count++;
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
     * 构建单个社区（自动发现的社区，字段从 LLM 推断，不写元数据表）
     */
    private CommunityBuildResult buildSingleCommunity(String graphId, String communityId,
                                                     Set<String> memberUuids, BinaryTreeSummarizer summarizer) {
        List<String> memberUuidList = new ArrayList<>(memberUuids);
        Map<String, String> memberSummaries = getMemberSummaries(graphId, memberUuidList);

        List<String> summaries = new ArrayList<>(memberSummaries.values());
        String mergedSummary = summarizer.summarize(summaries);
        String communityName = summarizer.generateCommunityName(mergedSummary);

        String communityUuid = UUID.randomUUID().toString().replace("-", "");

        // 通用化字段：domain_type / region / scenario_type
        // 自动发现的社区通过 LLM 推断字段（不写元数据表）
        String domainType = inferDomainTypeFromName(communityName);
        String region = "REGION_CN";
        String scenarioType = "SCENARIO_ROOT";

        String createCypher =
            "CREATE (c:Community {graph_id: $graph_id, uuid: $uuid, name: $name, " +
            "summary: $summary, member_count: $member_count, " +
            "community_type: $community_type, domain_type: $domain_type, " +
            "region: $region, scenario_type: $scenario_type}) " +
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
                "community_type", domainType,
                "domain_type", domainType,
                "region", region,
                "scenario_type", scenarioType,
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
     * 从社区名称推断领域类型（自动发现场景的兜底）
     * 移除法律硬编码，改为通用关键词匹配
     */
    private String inferDomainTypeFromName(String communityName) {
        if (communityName == null) return "DOMAIN_ROOT";
        String lower = communityName.toLowerCase();
        if (lower.contains("医疗") || lower.contains("药品") || lower.contains("临床") || lower.contains("公共卫生"))
            return "DOMAIN_MEDICAL";
        if (lower.contains("金融") || lower.contains("银行") || lower.contains("保险") || lower.contains("证券") || lower.contains("信贷"))
            return "DOMAIN_FINANCE";
        if (lower.contains("人力") || lower.contains("企业") || lower.contains("合规") || lower.contains("治理"))
            return "DOMAIN_ENTERPRISE";
        if (lower.contains("婚恋") || lower.contains("家庭") || lower.contains("邻里") || lower.contains("侵权") ||
            lower.contains("劳动") || lower.contains("消费") || lower.contains("土地") || lower.contains("物业") ||
            lower.contains("行政") || lower.contains("咨询") || lower.contains("纠纷"))
            return "DOMAIN_SOCIAL_GOV";
        if (lower.contains("法律") || lower.contains("诉讼") || lower.contains("判决") || lower.contains("调解"))
            return "DOMAIN_LEGAL";
        return "DOMAIN_ROOT";
    }

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

    // ==================== 增删改查 — 双写 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Map<String, Object>> listCommunities(String graphId) {
        // 1. 从 ont_community_type 加载元数据
        Long definitionId = resolveDefinitionId(graphId);
        List<OntCommunityTypeDO> metaTypes = communityTypeMapper.selectActiveByDefinitionId(definitionId);
        Map<String, OntCommunityTypeDO> metaMap = metaTypes.stream()
                .collect(Collectors.toMap(OntCommunityTypeDO::getTypeCode, Function.identity(), (a, b) -> a));

        // 2. 从 Neo4j 查询社区实例
        String cypher =
            "MATCH (c:Community {graph_id: $graph_id}) " +
            "RETURN c.uuid as uuid, c.name as name, c.summary as summary, " +
            "       c.member_count as member_count, c.parent_community_uuid as parentCommunityUuid, " +
            "       c.community_type as communityType, c.domain_type as domainType, " +
            "       c.region as region, c.scenario_type as scenarioType, " +
            "       c.created_at as createdAt " +
            "ORDER BY c.member_count DESC";

        List<Map<String, Object>> communities = new ArrayList<>();
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, Values.parameters("graph_id", graphId));
            while (result.hasNext()) {
                Record record = result.next();
                Map<String, Object> community = new LinkedHashMap<>();
                community.put("uuid", record.get("uuid").asString());
                community.put("name", record.get("name").asString());
                community.put("summary", record.get("summary").asString());
                community.put("memberCount", record.get("member_count").asInt());
                community.put("parentCommunityUuid",
                    record.get("parentCommunityUuid").isNull() ? null : record.get("parentCommunityUuid").asString());
                community.put("communityType",
                    record.get("communityType").isNull() ? null : record.get("communityType").asString());
                community.put("domainType",
                    record.get("domainType").isNull() ? null : record.get("domainType").asString());
                community.put("region",
                    record.get("region").isNull() ? null : record.get("region").asString());
                community.put("scenarioType",
                    record.get("scenarioType").isNull() ? null : record.get("scenarioType").asString());
                community.put("createdAt",
                    record.get("createdAt").isNull() ? null : record.get("createdAt").asString());

                // join 元数据：补全 typeName / description / metadata（色彩）
                String typeCode = record.get("communityType").isNull() ? null : record.get("communityType").asString();
                if (typeCode != null) {
                    OntCommunityTypeDO meta = metaMap.get(typeCode);
                    if (meta != null) {
                        community.put("typeName", meta.getTypeName());
                        community.put("region", meta.getRegion() != null ? meta.getRegion() : community.get("region"));
                        community.put("scenarioType", meta.getScenarioType() != null ? meta.getScenarioType() : community.get("scenarioType"));
                        community.put("metadata", meta.getMetadata());
                    }
                }
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
            "       c.domain_type as domainType, c.region as region, " +
            "       c.scenario_type as scenarioType, c.created_at as createdAt " +
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
                Map<String, Object> community = new LinkedHashMap<>();
                community.put("uuid", record.get("uuid").asString());
                community.put("name", record.get("name").asString());
                community.put("summary", record.get("summary").asString());
                community.put("memberCount", record.get("member_count").asInt());
                community.put("communityType",
                    record.get("communityType").isNull() ? null : record.get("communityType").asString());
                community.put("domainType",
                    record.get("domainType").isNull() ? null : record.get("domainType").asString());
                community.put("region",
                    record.get("region").isNull() ? null : record.get("region").asString());
                community.put("scenarioType",
                    record.get("scenarioType").isNull() ? null : record.get("scenarioType").asString());
                community.put("createdAt",
                    record.get("createdAt").isNull() ? null : record.get("createdAt").asString());
                communities.add(community);
            }
        }
        return communities;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeCommunities(String graphId) {
        // 1. 从 Neo4j 删除所有社区节点
        String deleteCypher = "MATCH (c:Community {graph_id: $graph_id}) DETACH DELETE c";
        try (Session session = neo4jDriver.session()) {
            session.run(deleteCypher, Values.parameters("graph_id", graphId));
            log.info("已清除图谱 {} 的所有社区节点", graphId);
        }

        // 2. 软删除相关元数据记录
        Long definitionId = resolveDefinitionId(graphId);
        List<OntCommunityTypeDO> affected = communityMetadataMapper.selectByDefinitionIdWithCommunity(definitionId);
        for (OntCommunityTypeDO meta : affected) {
            communityMetadataMapper.softDeleteByCode(definitionId, meta.getTypeCode());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCommunity(String graphId, String communityUuid) {
        // 1. 读取 typeCode 备用
        String typeCode = resolveTypeCodeFromNeo4j(graphId, communityUuid);

        // 2. 删除 Neo4j 节点
        String cypher = "MATCH (c:Community {graph_id: $graph_id, uuid: $uuid}) DETACH DELETE c";
        try (Session session = neo4jDriver.session()) {
            session.run(cypher, Values.parameters("graph_id", graphId, "uuid", communityUuid));
            log.info("已删除图谱 {} 的社区 {}", graphId, communityUuid);
        } catch (Exception e) {
            throw new CommunityMetadataCompensationException(
                "Neo4j 删除社区节点失败: " + e.getMessage(), graphId, communityUuid, "deleteCommunity");
        }

        // 3. 检查该 typeCode 在 Neo4j 中的剩余数量
        int remaining = countNeo4jCommunitiesByType(graphId, typeCode);
        if (remaining == 0 && typeCode != null) {
            Long definitionId = resolveDefinitionId(graphId);
            communityMetadataMapper.softDeleteByCode(definitionId, typeCode);
            log.info("社区类型 {} 无剩余实例，软删除元数据记录", typeCode);
        }
    }

    @Override
    public List<Map<String, Object>> getCommunityHierarchy(String graphId, String dimension) {
        // 1. 从元数据表获取所有分类
        Long definitionId = resolveDefinitionId(graphId);
        List<OntCommunityTypeDO> allTypes = communityTypeMapper.selectActiveByDefinitionId(definitionId);

        // 2. 从 Neo4j 统计各类型的实例数量
        //    层级 1（domain_type）: 直接挂载在 DOMAIN_ROOT 下的类型，如 DOMAIN_LEGAL
        //    层级 2+（community_type）: 再往下分的具体社区类型
        //    level-1 类型还应额外聚合其所有子节点的 community_type 计数
        Map<String, Long> typeCountMap = countNeo4jCommunitiesByType(graphId, allTypes);

        // 3. 按 dimension 参数过滤：domain | region | scenario
        String filterCategory = normalizeDimension(dimension);
        List<OntCommunityTypeDO> filtered = filterCategory == null
                ? allTypes
                : allTypes.stream().filter(t -> filterCategory.equals(t.getCategory())).collect(Collectors.toList());

        // 预先构建 parent -> [children] 映射（仅针对 domain 维度，用于聚合子节点计数）
        Map<String, List<String>> domainChildrenMap = new HashMap<>();
        for (OntCommunityTypeDO t : filtered) {
            if ("domain".equals(t.getCategory())) {
                String parent = t.getParentTypeCode();
                if (parent == null) parent = "ROOT";
                domainChildrenMap.computeIfAbsent(parent, k -> new ArrayList<>()).add(t.getTypeCode());
            }
        }

        // 4. 按 parentTypeCode 分组构建树
        Map<String, List<OntCommunityTypeDO>> childrenMap = filtered.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getParentTypeCode() == null ? "ROOT" : t.getParentTypeCode()));

        List<Map<String, Object>> tree = new ArrayList<>();
        for (OntCommunityTypeDO root : filtered) {
            if (root.getParentTypeCode() == null || "ROOT".equals(root.getParentTypeCode())
                    || root.getParentTypeCode().endsWith("_ROOT")) {
                Map<String, Object> rootNode = buildHierarchyNode(root, childrenMap, typeCountMap, domainChildrenMap);
                tree.add(rootNode);
            }
        }
        return tree;
    }

    /**
     * 从 Neo4j 统计各类型的实例数量。
     * - 直接匹配 level-2+ 的 community_type
     * - 直接匹配 level-1 的 domain_type
     * - 对 DOMAIN_ROOT 类型的节点，聚合其所有子节点的计数（包含 level-2+ 的 community_type）
     */
    private Map<String, Long> countNeo4jCommunitiesByType(String graphId, List<OntCommunityTypeDO> allTypes) {
        Map<String, Long> counts = new HashMap<>();

        // 预计算 level-1 domain 类型（直接挂在 DOMAIN_ROOT 下的）
        Set<String> level1DomainCodes = allTypes.stream()
                .filter(t -> "domain".equals(t.getCategory()))
                .filter(t -> {
                    String parent = t.getParentTypeCode();
                    return parent == null || parent.endsWith("_ROOT");
                })
                .map(OntCommunityTypeDO::getTypeCode)
                .collect(Collectors.toSet());

        // 预计算所有 domain 子节点的 community_type（用于聚合到 DOMAIN_ROOT）
        Set<String> allDomainChildCodes = allTypes.stream()
                .filter(t -> "domain".equals(t.getCategory()))
                .map(OntCommunityTypeDO::getTypeCode)
                .collect(Collectors.toSet());

        // 查询 community_type 上的计数（level-2+）
        String communityTypeCypher =
            "MATCH (c:Community {graph_id: $graphId}) " +
            "WHERE c.community_type IS NOT NULL " +
            "RETURN c.community_type as typeCode, count(c) as cnt";
        // 查询 domain_type 上的计数（level-1）
        String domainTypeCypher =
            "MATCH (c:Community {graph_id: $graphId}) " +
            "WHERE c.domain_type IS NOT NULL " +
            "RETURN c.domain_type as typeCode, count(c) as cnt";

        try (Session session = neo4jDriver.session()) {
            // community_type 计数
            Result ctResult = session.run(communityTypeCypher, Values.parameters("graphId", graphId));
            while (ctResult.hasNext()) {
                Record record = ctResult.next();
                String tc = record.get("typeCode").asString();
                long cnt = record.get("cnt").asLong();
                counts.merge(tc, cnt, Long::sum);
            }
            // domain_type 计数
            Result dtResult = session.run(domainTypeCypher, Values.parameters("graphId", graphId));
            while (dtResult.hasNext()) {
                Record record = dtResult.next();
                String tc = record.get("typeCode").asString();
                long cnt = record.get("cnt").asLong();
                counts.merge(tc, cnt, Long::sum);
            }
        } catch (Exception e) {
            log.warn("统计各类型社区数量失败: {}", e.getMessage());
        }

        // 对 DOMAIN_ROOT 节点，聚合所有 domain 子类型（level-2+）的计数
        long domainRootTotal = 0;
        for (String childCode : allDomainChildCodes) {
            domainRootTotal += counts.getOrDefault(childCode, 0L);
        }
        counts.put("DOMAIN_ROOT", domainRootTotal);

        return counts;
    }

    private Map<String, Object> buildHierarchyNode(OntCommunityTypeDO node,
                                                    Map<String, List<OntCommunityTypeDO>> childrenMap,
                                                    Map<String, Long> typeCountMap,
                                                    Map<String, List<String>> domainChildrenMap) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("typeCode", node.getTypeCode());
        map.put("typeName", node.getTypeName());
        map.put("category", node.getCategory());
        map.put("region", node.getRegion());
        map.put("scenarioType", node.getScenarioType());
        map.put("metadata", node.getMetadata());
        map.put("sortOrder", node.getSortOrder());

        // 节点自身的实例计数
        long selfCount = typeCountMap.getOrDefault(node.getTypeCode(), 0L);
        long totalCount = selfCount;

        // 对于 DOMAIN_ROOT 类型，递归聚合所有子节点的计数
        if ("DOMAIN_ROOT".equals(node.getTypeCode()) && domainChildrenMap.containsKey("DOMAIN_ROOT")) {
            totalCount = countDescendants("DOMAIN_ROOT", domainChildrenMap, typeCountMap);
        } else if (node.getParentTypeCode() != null && node.getParentTypeCode().endsWith("_ROOT")
                && domainChildrenMap.containsKey(node.getParentTypeCode())) {
            // level-1 domain 节点（如 DOMAIN_LEGAL）也需要聚合其所有子孙节点的计数
            totalCount = countDescendants(node.getTypeCode(), domainChildrenMap, typeCountMap);
        }

        map.put("count", totalCount);

        List<OntCommunityTypeDO> children = childrenMap.get(node.getTypeCode());
        if (children != null && !children.isEmpty()) {
            List<Map<String, Object>> childNodes = children.stream()
                    .sorted(Comparator.comparing(OntCommunityTypeDO::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder())))
                    .map(child -> buildHierarchyNode(child, childrenMap, typeCountMap, domainChildrenMap))
                    .collect(Collectors.toList());
            map.put("children", childNodes);
        } else {
            map.put("children", new ArrayList<>());
        }
        return map;
    }

    /**
     * 递归计算某类型下所有子孙节点的实例计数之和。
     */
    private long countDescendants(String typeCode, Map<String, List<String>> domainChildrenMap,
                                  Map<String, Long> typeCountMap) {
        long total = typeCountMap.getOrDefault(typeCode, 0L);
        List<String> children = domainChildrenMap.get(typeCode);
        if (children != null) {
            for (String child : children) {
                total += countDescendants(child, domainChildrenMap, typeCountMap);
            }
        }
        return total;
    }

    private String normalizeDimension(String dimension) {
        if (dimension == null || dimension.isEmpty()) return null;
        return switch (dimension.toLowerCase()) {
            case "domain" -> "domain";
            case "region", "jurisdiction" -> "region";
            case "scenario", "practice" -> "scenario";
            default -> null;
        };
    }

    @Override
    public Map<String, Object> getCommunityDetail(String graphId, String communityUuid) {
        String cypher =
            "MATCH (c:Community {graph_id: $graph_id, uuid: $uuid}) " +
            "OPTIONAL MATCH (c)<-[:HAS_COMMUNITY]-(m:Entity) " +
            "OPTIONAL MATCH (c)-[:PARENT_OF]->(child:Community {graph_id: $graph_id}) " +
            "RETURN c.uuid as uuid, c.name as name, c.summary as summary, c.description as description, " +
            "       c.community_type as communityType, c.domain_type as domainType, " +
            "       c.region as region, c.scenario_type as scenarioType, " +
            "       c.member_count as memberCount, c.key_provisions as keyProvisions, " +
            "       c.created_at as createdAt, c.updated_at as updatedAt, " +
            "       collect(DISTINCT {uuid: m.uuid, name: m.name, type: labels(m)[0]}) as members, " +
            "       collect(DISTINCT {uuid: child.uuid, name: child.name}) as subCommunities";

        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, Values.parameters("graph_id", graphId, "uuid", communityUuid));
            if (result.hasNext()) {
                Record record = result.next();
                Map<String, Object> detail = new LinkedHashMap<>();
                detail.put("uuid", record.get("uuid").asString());
                detail.put("name", record.get("name").asString());
                detail.put("summary", record.get("summary").isNull() ? null : record.get("summary").asString());
                detail.put("description", record.get("description").isNull() ? null : record.get("description").asString());
                detail.put("communityType", record.get("communityType").isNull() ? null : record.get("communityType").asString());
                detail.put("domainType", record.get("domainType").isNull() ? null : record.get("domainType").asString());
                detail.put("region", record.get("region").isNull() ? null : record.get("region").asString());
                detail.put("scenarioType", record.get("scenarioType").isNull() ? null : record.get("scenarioType").asString());
                detail.put("memberCount", record.get("memberCount").isNull() ? null : record.get("memberCount").asInt());
                detail.put("keyProvisions", record.get("keyProvisions").isNull() ? null : record.get("keyProvisions").asString());
                detail.put("createdAt", record.get("createdAt").isNull() ? null : record.get("createdAt").asString());
                detail.put("updatedAt", record.get("updatedAt").isNull() ? null : record.get("updatedAt").asString());
                detail.put("members", record.get("members").asList(v -> {
                    if (v.isNull()) return null;
                    return Map.of("uuid", v.asMap().get("uuid"), "name", v.asMap().get("name"), "type", v.asMap().get("type"));
                }));
                detail.put("subCommunities", record.get("subCommunities").asList(v -> {
                    if (v.isNull()) return null;
                    return v.asMap();
                }));
                return detail;
            }
        }
        return Map.of("error", "Community not found");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createCommunity(String graphId, Map<String, Object> body) {
        Long definitionId = resolveDefinitionId(graphId);
        String uuid = UUID.randomUUID().toString();

        String name = String.valueOf(body.getOrDefault("name", "新建社区"));
        String communityType = String.valueOf(body.getOrDefault("communityType", ""));
        String domainType = String.valueOf(body.getOrDefault("domainType", "DOMAIN_ROOT"));
        String region = String.valueOf(body.getOrDefault("region", "REGION_ROOT"));
        String scenarioType = String.valueOf(body.getOrDefault("scenarioType", "SCENARIO_ROOT"));
        String summary = String.valueOf(body.getOrDefault("summary", ""));
        String description = String.valueOf(body.getOrDefault("description", ""));

        // 1. LLM 推断 + 用户覆盖
        DomainInferenceResult inference = null;
        if (body.containsKey("_doInfer") && Boolean.TRUE.equals(body.get("_doInfer"))) {
            try {
                inference = domainInferenceService.infer(name, null,
                        communityTypeMapper.selectActiveByDefinitionId(definitionId));
                if (inference.getDomainType() != null) domainType = inference.getDomainType();
                if (inference.getRegion() != null) region = inference.getRegion();
                if (inference.getScenarioType() != null) scenarioType = inference.getScenarioType();
            } catch (Exception e) {
                log.warn("LLM 领域推断失败，使用默认值: {}", e.getMessage());
            }
        }

        // 1b. 领域类型层级校验（最多 3 层）
        if (communityType != null && !communityType.isEmpty()) {
            int depth = computeTypeDepth(definitionId, communityType);
            if (depth > 3) {
                throw new IllegalArgumentException(
                    "社区类型层级不能超过 3 层。当前类型 " + communityType + " 处于第 " + depth + " 层。");
            }
        }

        // 2. 写入元数据表（upsert：若 typeCode 不存在则插入）
        upsertCommunityTypeMetadata(definitionId, graphId, uuid, communityType, domainType, region, scenarioType, name);

        // 3. 写 Neo4j Community 节点
        String cypher =
            "CREATE (c:Community {\n" +
            "  graph_id: $graphId, uuid: $uuid, name: $name,\n" +
            "  community_type: $communityType, domain_type: $domainType,\n" +
            "  region: $region, scenario_type: $scenarioType,\n" +
            "  summary: $summary, description: $description,\n" +
            "  member_count: 0, created_at: datetime(), updated_at: datetime()\n" +
            "})\n" +
            "RETURN c.uuid as uuid, c.name as name, c.community_type as communityType,\n" +
            "       c.domain_type as domainType, c.region as region,\n" +
            "       c.scenario_type as scenarioType, c.summary as summary,\n" +
            "       c.description as description, c.member_count as memberCount,\n" +
            "       c.created_at as createdAt";

        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, Values.parameters(
                "graphId", graphId, "uuid", uuid, "name", name,
                "communityType", communityType.isEmpty() ? null : communityType,
                "domainType", domainType.isEmpty() ? null : domainType,
                "region", region.isEmpty() ? null : region,
                "scenarioType", scenarioType.isEmpty() ? null : scenarioType,
                "summary", summary.isEmpty() ? null : summary,
                "description", description.isEmpty() ? null : description
            ));
            if (result.hasNext()) {
                Record record = result.next();
                Map<String, Object> created = new LinkedHashMap<>();
                created.put("uuid", record.get("uuid").asString());
                created.put("name", record.get("name").asString());
                created.put("communityType", record.get("communityType").isNull() ? null : record.get("communityType").asString());
                created.put("domainType", record.get("domainType").isNull() ? null : record.get("domainType").asString());
                created.put("region", record.get("region").isNull() ? null : record.get("region").asString());
                created.put("scenarioType", record.get("scenarioType").isNull() ? null : record.get("scenarioType").asString());
                created.put("summary", record.get("summary").isNull() ? null : record.get("summary").asString());
                created.put("description", record.get("description").isNull() ? null : record.get("description").asString());
                created.put("memberCount", record.get("memberCount").isNull() ? 0 : record.get("memberCount").asInt());
                created.put("createdAt", record.get("createdAt").isNull() ? null : record.get("createdAt").asString());
                if (inference != null) {
                    created.put("_inferenceConfidence", inference.getConfidence());
                }
                log.info("创建社区节点: graphId={}, uuid={}, name={}", graphId, uuid, name);
                return created;
            }
        } catch (Exception e) {
            throw new CommunityMetadataCompensationException(
                "Neo4j 创建社区节点失败: " + e.getMessage(), graphId, uuid, "createCommunity");
        }
        return Map.of("error", "Failed to create community");
    }

    /**
     * 元数据表 upsert：若 definitionId + typeCode 不存在则插入
     */
    private void upsertCommunityTypeMetadata(Long definitionId, String graphId, String communityUuid,
                                            String communityType, String domainType,
                                            String region, String scenarioType, String name) {
        if (communityType == null || communityType.isEmpty()) return;

        OntCommunityTypeDO existing = communityMetadataMapper.findByCode(definitionId, communityType);
        if (existing != null) {
            // 已存在，同步更新 instance 关联信息
            existing.setCommunityUuid(communityUuid);
            existing.setGraphId(graphId);
            existing.setRegion(region != null && !region.isEmpty() ? region : existing.getRegion());
            existing.setScenarioType(scenarioType != null && !scenarioType.isEmpty() ? scenarioType : existing.getScenarioType());
            communityMetadataMapper.updateByCode(existing);
        } else {
            // 不存在，插入新记录
            OntCommunityTypeDO entity = new OntCommunityTypeDO();
            entity.setDefinitionId(definitionId);
            entity.setGraphId(graphId);
            entity.setCommunityUuid(communityUuid);
            entity.setTypeCode(communityType);
            entity.setTypeName(name);
            entity.setCategory(determineCategory(communityType));
            entity.setRegion(region != null && !region.isEmpty() ? region : "REGION_ROOT");
            entity.setScenarioType(scenarioType != null && !scenarioType.isEmpty() ? scenarioType : "SCENARIO_ROOT");
            entity.setMetadata("{}");
            entity.setStatus("ACTIVE");
            communityMetadataMapper.insert(entity);
            log.info("Upsert 元数据: definitionId={}, typeCode={}", definitionId, communityType);
        }
    }

    private String determineCategory(String typeCode) {
        if (typeCode == null) return "domain";
        if (typeCode.startsWith("DOMAIN_")) return "domain";
        if (typeCode.startsWith("REGION_")) return "region";
        if (typeCode.startsWith("SCENARIO_")) return "scenario";
        return "domain";
    }

    /**
     * 计算领域类型的层级深度。
     * depth=1: 无父类型（根节点）
     * depth=2: 父类型是根节点
     * depth=3: 父类型的父类型是根节点
     * depth>3: 拒绝创建
     *
     * <p>若类型不存在于元数据表中（新类型），返回 1（允许作为根节点创建）。
     */
    private int computeTypeDepth(Long definitionId, String typeCode) {
        Set<String> visited = new HashSet<>();
        String current = typeCode;
        int depth = 1;
        while (current != null && !visited.contains(current)) {
            visited.add(current);
            // ROOT 类型视为树根，不再继续往上追溯
            if ("DOMAIN_ROOT".equals(current) || "REGION_ROOT".equals(current)
                    || "SCENARIO_ROOT".equals(current)) {
                break;
            }
            OntCommunityTypeDO meta = communityTypeMapper.findByCode(definitionId, current);
            if (meta == null) {
                // 新类型，不在表中，按根节点处理
                break;
            }
            String parent = meta.getParentTypeCode();
            if (parent == null || parent.isEmpty()) {
                break;
            }
            current = parent;
            depth++;
        }
        return depth;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateCommunity(String graphId, String communityUuid, Map<String, Object> body) {
        Long definitionId = resolveDefinitionId(graphId);

        List<String> setClauses = new ArrayList<>();
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("graphId", graphId);
        params.put("uuid", communityUuid);

        if (body.containsKey("name")) {
            setClauses.add("c.name = $name");
            params.put("name", String.valueOf(body.get("name")));
        }
        if (body.containsKey("communityType")) {
            String v = String.valueOf(body.get("communityType"));
            setClauses.add("c.community_type = $communityType");
            params.put("communityType", v.isEmpty() ? null : v);
        }
        if (body.containsKey("domainType")) {
            String v = String.valueOf(body.get("domainType"));
            setClauses.add("c.domain_type = $domainType");
            params.put("domainType", v.isEmpty() ? null : v);
        }
        if (body.containsKey("region")) {
            String v = String.valueOf(body.get("region"));
            setClauses.add("c.region = $region");
            params.put("region", v.isEmpty() ? null : v);
        }
        if (body.containsKey("scenarioType")) {
            String v = String.valueOf(body.get("scenarioType"));
            setClauses.add("c.scenario_type = $scenarioType");
            params.put("scenarioType", v.isEmpty() ? null : v);
        }
        if (body.containsKey("summary")) {
            String v = String.valueOf(body.get("summary"));
            setClauses.add("c.summary = $summary");
            params.put("summary", v.isEmpty() ? null : v);
        }
        if (body.containsKey("description")) {
            String v = String.valueOf(body.get("description"));
            setClauses.add("c.description = $description");
            params.put("description", v.isEmpty() ? null : v);
        }

        if (setClauses.isEmpty()) {
            return Map.of("error", "No fields to update");
        }
        setClauses.add("c.updated_at = datetime()");

        String cypher = "MATCH (c:Community {graph_id: $graphId, uuid: $uuid}) " +
            "SET " + String.join(", ", setClauses) + " " +
            "RETURN c.uuid as uuid, c.name as name, c.community_type as communityType,\n" +
            "       c.domain_type as domainType, c.region as region,\n" +
            "       c.scenario_type as scenarioType, c.summary as summary,\n" +
            "       c.description as description, c.member_count as memberCount,\n" +
            "       c.created_at as createdAt, c.updated_at as updatedAt";

        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, Values.parameters(params));
            if (result.hasNext()) {
                Record record = result.next();
                Map<String, Object> updated = new LinkedHashMap<>();
                updated.put("uuid", record.get("uuid").asString());
                updated.put("name", record.get("name").asString());
                updated.put("communityType", record.get("communityType").isNull() ? null : record.get("communityType").asString());
                updated.put("domainType", record.get("domainType").isNull() ? null : record.get("domainType").asString());
                updated.put("region", record.get("region").isNull() ? null : record.get("region").asString());
                updated.put("scenarioType", record.get("scenarioType").isNull() ? null : record.get("scenarioType").asString());
                updated.put("summary", record.get("summary").isNull() ? null : record.get("summary").asString());
                updated.put("description", record.get("description").isNull() ? null : record.get("description").asString());
                updated.put("memberCount", record.get("memberCount").isNull() ? 0 : record.get("memberCount").asInt());
                updated.put("createdAt", record.get("createdAt").isNull() ? null : record.get("createdAt").asString());
                updated.put("updatedAt", record.get("updatedAt").isNull() ? null : record.get("updatedAt").asString());
                log.info("更新社区节点: graphId={}, uuid={}", graphId, communityUuid);

                // 同步更新元数据表
                syncMetadataOnUpdate(definitionId, graphId, updated);
                return updated;
            }
        } catch (Exception e) {
            throw new CommunityMetadataCompensationException(
                "Neo4j 更新社区节点失败: " + e.getMessage(), graphId, communityUuid, "updateCommunity");
        }
        return Map.of("error", "Community not found");
    }

    private void syncMetadataOnUpdate(Long definitionId, String graphId, Map<String, Object> updated) {
        String typeCode = updated.get("communityType") != null ? String.valueOf(updated.get("communityType")) : null;
        if (typeCode == null || typeCode.isEmpty()) return;

        OntCommunityTypeDO existing = communityMetadataMapper.findByCode(definitionId, typeCode);
        if (existing == null) return;

        if (updated.get("region") != null) {
            existing.setRegion(String.valueOf(updated.get("region")));
        }
        if (updated.get("scenarioType") != null) {
            existing.setScenarioType(String.valueOf(updated.get("scenarioType")));
        }
        if (updated.get("domainType") != null) {
            // domainType 变化时更新 parent 引用
            existing.setParentTypeCode(String.valueOf(updated.get("domainType")));
        }
        communityMetadataMapper.updateByCode(existing);
    }

    // ==================== 辅助方法 ====================

    private Long resolveDefinitionId(String graphId) {
        OntDefinitionDO def = definitionMapper.selectByGraphId(graphId);
        if (def == null) {
            log.warn("未找到 graphId={} 对应的本体定义，返回 definitionId=1", graphId);
            return 1L;
        }
        return def.getId();
    }

    private String resolveTypeCodeFromNeo4j(String graphId, String communityUuid) {
        String cypher = "MATCH (c:Community {graph_id: $graph_id, uuid: $uuid}) RETURN c.community_type as ct";
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, Values.parameters("graph_id", graphId, "uuid", communityUuid));
            if (result.hasNext()) {
                return result.next().get("ct").isNull() ? null : result.next().get("ct").asString();
            }
        } catch (Exception e) {
            log.warn("查询 communityType 失败: {}", e.getMessage());
        }
        return null;
    }

    private int countNeo4jCommunitiesByType(String graphId, String typeCode) {
        if (typeCode == null) return 0;
        String cypher = "MATCH (c:Community {graph_id: $graph_id, community_type: $typeCode}) RETURN count(c) as cnt";
        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, Values.parameters("graph_id", graphId, "typeCode", typeCode));
            if (result.hasNext()) {
                return result.next().get("cnt").asInt();
            }
        } catch (Exception e) {
            log.warn("统计社区数量失败: {}", e.getMessage());
        }
        return 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> createCommunity(String graphId, CommunityCreateContext context) {
        Long definitionId = resolveDefinitionId(graphId);
        String uuid = UUID.randomUUID().toString();

        String name = context.getCommunityName() != null ? context.getCommunityName() : "新建社区";
        String communityType = context.getCommunityType() != null ? context.getCommunityType() : "";
        String domainType = context.getDomainType() != null ? context.getDomainType() : "DOMAIN_ROOT";
        String region = context.getRegion() != null ? context.getRegion() : "REGION_ROOT";
        String scenarioType = context.getScenarioType() != null ? context.getScenarioType() : "SCENARIO_ROOT";
        String summary = context.getSummary() != null ? context.getSummary() : "";
        String description = context.getDescription() != null ? context.getDescription() : "";

        // 若未传入 domainType，尝试 LLM 推断
        if (domainType.equals("DOMAIN_ROOT") && !context.isUserOverridden()) {
            try {
                DomainInferenceResult inference = domainInferenceService.infer(name, null,
                        communityTypeMapper.selectActiveByDefinitionId(definitionId));
                if (inference.getDomainType() != null) domainType = inference.getDomainType();
                if (inference.getRegion() != null) region = inference.getRegion();
                if (inference.getScenarioType() != null) scenarioType = inference.getScenarioType();
                context.setInferenceConfidence(inference.getConfidence());
                context.setDomainType(domainType);
                context.setRegion(region);
                context.setScenarioType(scenarioType);
            } catch (Exception e) {
                log.warn("LLM 领域推断失败，使用默认值: {}", e.getMessage());
            }
        }

        // 写元数据表（upsert）
        if (communityType != null && !communityType.isEmpty()) {
            int depth = computeTypeDepth(definitionId, communityType);
            if (depth > 3) {
                throw new IllegalArgumentException(
                    "社区类型层级不能超过 3 层。当前类型 " + communityType + " 处于第 " + depth + " 层。");
            }
        }
        upsertCommunityTypeMetadata(definitionId, graphId, uuid, communityType, domainType, region, scenarioType, name);

        // 写 Neo4j
        String cypher =
            "CREATE (c:Community {\n" +
            "  graph_id: $graphId, uuid: $uuid, name: $name,\n" +
            "  community_type: $communityType, domain_type: $domainType,\n" +
            "  region: $region, scenario_type: $scenarioType,\n" +
            "  summary: $summary, description: $description,\n" +
            "  member_count: $memberCount, created_at: datetime(), updated_at: datetime()\n" +
            "})\n" +
            "RETURN c.uuid as uuid, c.name as name, c.community_type as communityType,\n" +
            "       c.domain_type as domainType, c.region as region,\n" +
            "       c.scenario_type as scenarioType, c.summary as summary,\n" +
            "       c.description as description, c.member_count as memberCount,\n" +
            "       c.created_at as createdAt";

        try (Session session = neo4jDriver.session()) {
            Result result = session.run(cypher, Values.parameters(
                "graphId", graphId, "uuid", uuid, "name", name,
                "communityType", communityType.isEmpty() ? null : communityType,
                "domainType", domainType.isEmpty() ? null : domainType,
                "region", region.isEmpty() ? null : region,
                "scenarioType", scenarioType.isEmpty() ? null : scenarioType,
                "summary", summary.isEmpty() ? null : summary,
                "description", description.isEmpty() ? null : description,
                "memberCount", context.getMemberCount() != null ? context.getMemberCount() : 0
            ));
            if (result.hasNext()) {
                Record record = result.next();
                Map<String, Object> created = new LinkedHashMap<>();
                created.put("uuid", record.get("uuid").asString());
                created.put("name", record.get("name").asString());
                created.put("communityType", record.get("communityType").isNull() ? null : record.get("communityType").asString());
                created.put("domainType", record.get("domainType").isNull() ? null : record.get("domainType").asString());
                created.put("region", record.get("region").isNull() ? null : record.get("region").asString());
                created.put("scenarioType", record.get("scenarioType").isNull() ? null : record.get("scenarioType").asString());
                created.put("summary", record.get("summary").isNull() ? null : record.get("summary").asString());
                created.put("description", record.get("description").isNull() ? null : record.get("description").asString());
                created.put("memberCount", record.get("memberCount").isNull() ? 0 : record.get("memberCount").asInt());
                created.put("createdAt", record.get("createdAt").isNull() ? null : record.get("createdAt").asString());
                if (context.getInferenceConfidence() != null) {
                    created.put("_inferenceConfidence", context.getInferenceConfidence());
                }
                log.info("创建社区节点(CommunityCreateContext): graphId={}, uuid={}, name={}", graphId, uuid, name);
                return created;
            }
        } catch (Exception e) {
            throw new CommunityMetadataCompensationException(
                "Neo4j 创建社区节点失败: " + e.getMessage(), graphId, uuid, "createCommunity(CommunityCreateContext)");
        }
        return Map.of("error", "Failed to create community");
    }

    private static class CommunityBuildResult {
        final boolean success;
        final String communityUuid;
        CommunityBuildResult(boolean success, String communityUuid) {
            this.success = success;
            this.communityUuid = communityUuid;
        }
    }
}
