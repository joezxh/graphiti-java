package com.graphiti.module.graphiti.service.impl;

import com.graphiti.module.graphiti.service.EmbedderService;
import com.graphiti.module.graphiti.service.GraphNeo4jService;
import com.graphiti.module.graphiti.service.SearchService;
import com.graphiti.module.graphiti.vo.search.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 搜索服务实现类（混合检索：BM25 + 向量 + BFS + RRF + MMR）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final GraphNeo4jService graphNeo4jService;
    private final EmbedderService embedderService;

    @Override
    public SearchResultsRespVO search(SearchQueryReqVO reqVO) {
        SearchConfigVO config = reqVO.getConfig() != null ? reqVO.getConfig() : new SearchConfigVO();
        return doSearch(reqVO.getQuery(), reqVO.getGroupIds(),
                        reqVO.getMaxFacts() != null ? reqVO.getMaxFacts() : 10, config);
    }

    @Override
    public SearchResultsRespVO searchGraph(String graphId, SearchQueryReqVO reqVO) {
        List<String> groupIds = reqVO.getGroupIds() != null ? reqVO.getGroupIds() : new ArrayList<>();
        if (!groupIds.contains(graphId)) {
            groupIds.add(graphId);
        }
        SearchConfigVO config = reqVO.getConfig() != null ? reqVO.getConfig() : new SearchConfigVO();
        return doSearch(reqVO.getQuery(), groupIds,
                        reqVO.getMaxFacts() != null ? reqVO.getMaxFacts() : 10, config);
    }

    @Override
    public GetMemoryRespVO getMemory(GetMemoryReqVO reqVO) {
        String query = reqVO.getMessages().stream()
                .filter(m -> !"system".equalsIgnoreCase(m.getRole()))
                .reduce((first, second) -> second)
                .map(MessageQueryVO::getContent)
                .orElse("");

        if (query.isEmpty()) {
            GetMemoryRespVO respVO = new GetMemoryRespVO();
            respVO.setFacts(new ArrayList<>());
            respVO.setEntities(new ArrayList<>());
            respVO.setContext("");
            return respVO;
        }

        int maxFacts = reqVO.getMaxFacts() != null ? reqVO.getMaxFacts() : 10;
        SearchConfigVO config = new SearchConfigVO();
        config.setMode("hybrid");
        SearchResultsRespVO searchResult = doSearch(query, reqVO.getGroupIds(), maxFacts, config);

        StringBuilder contextBuilder = new StringBuilder("相关知识：\n");
        for (FactResultVO fact : searchResult.getFacts()) {
            contextBuilder.append("- ").append(fact.getFact()).append("\n");
        }

        GetMemoryRespVO respVO = new GetMemoryRespVO();
        respVO.setFacts(searchResult.getFacts());
        respVO.setEntities(searchResult.getNodes());
        respVO.setContext(contextBuilder.toString());

        return respVO;
    }

    // ==================== 核心混合检索 ====================

    private SearchResultsRespVO doSearch(String query, List<String> groupIds, int maxFacts, SearchConfigVO config) {
        log.info("执行混合检索：query={}, mode={}, groupIds={}", query, config.getMode(), groupIds);

        String mode = config.getMode() != null ? config.getMode() : "hybrid";
        List<FactResultVO> allFacts = new ArrayList<>();
        List<NodeResultVO> allNodes = new ArrayList<>();

        if (groupIds == null || groupIds.isEmpty()) {
            return emptyResult();
        }

        for (String graphId : groupIds) {
            switch (mode) {
                case "bm25" -> {
                    allFacts.addAll(searchEdgesByBm25(query, graphId, maxFacts));
                    allNodes.addAll(searchNodesByBm25(query, graphId, maxFacts));
                }
                case "vector" -> {
                    allFacts.addAll(searchEdgesByVector(query, graphId, maxFacts));
                    allNodes.addAll(searchNodesByVector(query, graphId, maxFacts));
                }
                case "hybrid" -> {
                    // BM25 + 向量 + RRF 融合
                    List<FactResultVO> bm25Facts = searchEdgesByBm25(query, graphId, maxFacts);
                    List<FactResultVO> vecFacts = searchEdgesByVector(query, graphId, maxFacts);
                    allFacts.addAll(fuseByRrf(bm25Facts, vecFacts, config.getRrfK() != null ? config.getRrfK() : 60, maxFacts));

                    List<NodeResultVO> bm25Nodes = searchNodesByBm25(query, graphId, maxFacts);
                    List<NodeResultVO> vecNodes = searchNodesByVector(query, graphId, maxFacts);
                    allNodes.addAll(fuseNodesByRrf(bm25Nodes, vecNodes, config.getRrfK() != null ? config.getRrfK() : 60, maxFacts));
                }
                case "bfs" -> {
                    // BFS 图遍历搜索（基于向量搜索种子节点）
                    allNodes.addAll(searchNodesByBfs(query, graphId, maxFacts, config));
                }
                default -> {
                    allFacts.addAll(searchEdgesByBm25(query, graphId, maxFacts));
                    allNodes.addAll(searchNodesByBm25(query, graphId, maxFacts));
                }
            }
        }

        // MMR 重排序（边）
        if (Boolean.TRUE.equals(config.getEnableMmr()) && !allFacts.isEmpty()) {
            allFacts = rerankByMmr(allFacts, config.getMmrLambda() != null ? config.getMmrLambda() : 0.5, maxFacts);
        }

        SearchResultsRespVO respVO = new SearchResultsRespVO();
        respVO.setFacts(allFacts);
        respVO.setTotalCount(allFacts.size());
        respVO.setNodes(allNodes);
        respVO.setNodeCount(allNodes.size());

        return respVO;
    }

    // ==================== BM25 全文搜索 ====================

    private List<FactResultVO> searchEdgesByBm25(String query, String graphId, int limit) {
        List<Map<String, Object>> edges = graphNeo4jService.searchEdgesByFulltext(query, graphId, limit);
        return edges.stream().map(this::convertToFactResult).collect(Collectors.toList());
    }

    private List<NodeResultVO> searchNodesByBm25(String query, String graphId, int limit) {
        List<Map<String, Object>> nodes = graphNeo4jService.searchNodesByFulltext(query, graphId, limit);
        return nodes.stream().map(this::convertToNodeResult).collect(Collectors.toList());
    }

    // ==================== 向量相似度搜索 ====================

    private List<FactResultVO> searchEdgesByVector(String query, String graphId, int limit) {
        float[] embedding = embedderService.embed(query);
        List<Map<String, Object>> edges = graphNeo4jService.searchEdgesByVector(graphId, embedding, limit);
        return edges.stream().map(this::convertToFactResult).collect(Collectors.toList());
    }

    private List<NodeResultVO> searchNodesByVector(String query, String graphId, int limit) {
        float[] embedding = embedderService.embed(query);
        List<Map<String, Object>> nodes = graphNeo4jService.searchNodesByVector(graphId, embedding, limit);
        return nodes.stream().map(this::convertToNodeResult).collect(Collectors.toList());
    }

    // ==================== BFS 图遍历搜索 ====================

    private List<NodeResultVO> searchNodesByBfs(String query, String graphId, int limit, SearchConfigVO config) {
        int depth = config.getBfsDepth() != null ? config.getBfsDepth() : 2;
        int maxNeighbors = config.getBfsMaxNeighbors() != null ? config.getBfsMaxNeighbors() : 5;

        // 1. 向量搜索获取种子节点
        List<NodeResultVO> seedNodes = searchNodesByVector(query, graphId, maxNeighbors);
        Set<String> visited = new HashSet<>();
        List<NodeResultVO> results = new ArrayList<>();

        for (NodeResultVO seed : seedNodes) {
            if (visited.contains(seed.getUuid())) continue;
            bfsTraverse(seed.getUuid(), graphId, depth, maxNeighbors, visited, results);
        }

        return results.stream().limit(limit).collect(Collectors.toList());
    }

    private void bfsTraverse(String startUuid, String graphId, int depth, int maxNeighbors,
                             Set<String> visited, List<NodeResultVO> results) {
        Queue<String> queue = new LinkedList<>();
        queue.offer(startUuid);
        visited.add(startUuid);
        int currentDepth = 0;

        while (!queue.isEmpty() && currentDepth < depth) {
            int levelSize = queue.size();
            for (int i = 0; i < levelSize; i++) {
                String uuid = queue.poll();
                Map<String, Object> node = graphNeo4jService.getEntityNode(graphId, uuid);
                if (node != null) {
                    results.add(convertToNodeResult(node));
                }
                // 获取邻居节点（通过关系查询）
                List<Map<String, Object>> neighbors = graphNeo4jService.listEdges(
                    graphId, null, uuid, null, 0, maxNeighbors);
                for (Map<String, Object> edge : neighbors) {
                    String targetUuid = (String) edge.get("target");
                    if (targetUuid != null && !visited.contains(targetUuid)) {
                        visited.add(targetUuid);
                        queue.offer(targetUuid);
                    }
                }
            }
            currentDepth++;
        }
    }

    // ==================== RRF 融合 ====================

    private List<FactResultVO> fuseByRrf(List<FactResultVO> list1, List<FactResultVO> list2, int k, int limit) {
        Map<String, Double> rrfScores = new HashMap<>();

        // 列表1的分数
        for (int i = 0; i < list1.size(); i++) {
            String uuid = list1.get(i).getUuid();
            rrfScores.merge(uuid, 1.0 / (k + i + 1), Double::sum);
        }

        // 列表2的分数
        for (int i = 0; i < list2.size(); i++) {
            String uuid = list2.get(i).getUuid();
            rrfScores.merge(uuid, 1.0 / (k + i + 1), Double::sum);
        }

        // 去重并合并
        Map<String, FactResultVO> factMap = new HashMap<>();
        for (FactResultVO f : list1) factMap.putIfAbsent(f.getUuid(), f);
        for (FactResultVO f : list2) factMap.putIfAbsent(f.getUuid(), f);

        return rrfScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(limit)
                .map(e -> {
                    FactResultVO vo = factMap.get(e.getKey());
                    vo.setScore(e.getValue());
                    return vo;
                })
                .collect(Collectors.toList());
    }

    private List<NodeResultVO> fuseNodesByRrf(List<NodeResultVO> list1, List<NodeResultVO> list2, int k, int limit) {
        Map<String, Double> rrfScores = new HashMap<>();

        for (int i = 0; i < list1.size(); i++) {
            String uuid = list1.get(i).getUuid();
            rrfScores.merge(uuid, 1.0 / (k + i + 1), Double::sum);
        }

        for (int i = 0; i < list2.size(); i++) {
            String uuid = list2.get(i).getUuid();
            rrfScores.merge(uuid, 1.0 / (k + i + 1), Double::sum);
        }

        Map<String, NodeResultVO> nodeMap = new HashMap<>();
        for (NodeResultVO n : list1) nodeMap.putIfAbsent(n.getUuid(), n);
        for (NodeResultVO n : list2) nodeMap.putIfAbsent(n.getUuid(), n);

        return rrfScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(limit)
                .map(e -> {
                    NodeResultVO vo = nodeMap.get(e.getKey());
                    vo.setScore(e.getValue());
                    return vo;
                })
                .collect(Collectors.toList());
    }

    // ==================== MMR 重排序 ====================

    private List<FactResultVO> rerankByMmr(List<FactResultVO> facts, double lambda, int limit) {
        if (facts.size() <= 1) return facts;

        List<FactResultVO> selected = new ArrayList<>();
        List<FactResultVO> remaining = new ArrayList<>(facts);

        // 选择第一个：相关性最高
        remaining.sort((a, b) -> Double.compare(b.getScore() != null ? b.getScore() : 0,
                                                  a.getScore() != null ? a.getScore() : 0));
        selected.add(remaining.remove(0));

        while (!remaining.isEmpty() && selected.size() < limit) {
            FactResultVO best = null;
            double bestMmrScore = -1;

            for (FactResultVO candidate : remaining) {
                double relevance = candidate.getScore() != null ? candidate.getScore() : 0;
                double maxSim = selected.stream()
                        .mapToDouble(s -> cosineSimilarity(candidate, s))
                        .max().orElse(0);
                double mmrScore = lambda * relevance - (1 - lambda) * maxSim;
                if (mmrScore > bestMmrScore) {
                    bestMmrScore = mmrScore;
                    best = candidate;
                }
            }

            if (best != null) {
                selected.add(best);
                remaining.remove(best);
            } else {
                break;
            }
        }

        return selected;
    }

    private double cosineSimilarity(FactResultVO a, FactResultVO b) {
        // 基于文本内容计算简单相似度（Jaccard 风格）
        String textA = (a.getFact() != null ? a.getFact() : "") + " " + (a.getName() != null ? a.getName() : "");
        String textB = (b.getFact() != null ? b.getFact() : "") + " " + (b.getName() != null ? b.getName() : "");
        Set<String> setA = new HashSet<>(Arrays.asList(textA.toLowerCase().split("\\s+")));
        Set<String> setB = new HashSet<>(Arrays.asList(textB.toLowerCase().split("\\s+")));
        if (setA.isEmpty() || setB.isEmpty()) return 0;

        Set<String> intersection = new HashSet<>(setA);
        intersection.retainAll(setB);
        return (double) intersection.size() / Math.sqrt(setA.size() * setB.size());
    }

    // ==================== 工具方法 ====================

    private SearchResultsRespVO emptyResult() {
        SearchResultsRespVO respVO = new SearchResultsRespVO();
        respVO.setFacts(new ArrayList<>());
        respVO.setTotalCount(0);
        respVO.setNodes(new ArrayList<>());
        respVO.setNodeCount(0);
        return respVO;
    }

    private FactResultVO convertToFactResult(Map<String, Object> edge) {
        FactResultVO vo = new FactResultVO();
        vo.setUuid((String) edge.get("uuid"));
        vo.setName((String) edge.get("name"));
        vo.setFact((String) edge.get("fact"));
        vo.setSourceNodeUuid((String) edge.get("source"));
        vo.setTargetNodeUuid((String) edge.get("target"));
        vo.setGroupId((String) edge.get("group_id"));

        Object score = edge.get("score");
        if (score != null) {
            vo.setScore(score instanceof Number ? ((Number) score).doubleValue() : null);
        }

        return vo;
    }

    private NodeResultVO convertToNodeResult(Map<String, Object> node) {
        NodeResultVO vo = new NodeResultVO();
        vo.setUuid((String) node.get("uuid"));
        vo.setName((String) node.get("name"));
        vo.setSummary((String) node.get("summary"));

        Object labels = node.get("labels");
        if (labels instanceof List) {
            vo.setLabels((List<String>) labels);
        }

        Object score = node.get("score");
        if (score != null) {
            vo.setScore(score instanceof Number ? ((Number) score).doubleValue() : null);
        }

        return vo;
    }
}
