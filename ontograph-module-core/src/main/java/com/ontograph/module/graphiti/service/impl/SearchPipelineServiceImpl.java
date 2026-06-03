package com.ontograph.module.graphiti.service.impl;

import com.ontograph.module.graphiti.model.search.*;
import com.ontograph.module.graphiti.model.search.SearchResults.*;
import com.ontograph.module.graphiti.service.*;
import com.ontograph.module.graphiti.vo.search.FactResultVO;
import com.ontograph.module.graphiti.vo.search.NodeResultVO;
import com.ontograph.module.graphiti.vo.search.SearchResultsRespVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * 搜索 Pipeline 核心编排服务
 *
 * <p>参考 Python 实现：graphiti_core/search/search.py:search()
 *
 * <p>Pipeline 流程：
 * <pre>
 * 1. 条件生成查询向量（仅当需要 vector/MMR 时触发）
 * 2. 并行执行 4 个 Scope（CompletableFuture.allOf）：
 *    - edge_search: BM25 + Cosine + BFS → RRF/MMR/CrossEncoder
 *    - node_search: BM25 + Cosine + BFS → RRF/MMR/CrossEncoder
 *    - episode_search: BM25 → RRF/CrossEncoder
 *    - community_search: BM25 + Cosine → RRF/MMR/CrossEncoder
 * 3. 各 Scope 内搜索方法并行执行
 * 4. 聚合结果，返回 SearchResults
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchPipelineServiceImpl implements SearchPipelineService {

    private final GraphNeo4jService graphNeo4jService;
    private final EmbedderService embedderService;
    private final RrfRerankerService rrfRerankerService;
    private final MmrRerankerService mmrRerankerService;
    private final CrossEncoderRerankerService crossEncoderRerankerService;
    private final NodeDistanceRerankerService nodeDistanceRerankerService;
    private final EpisodeMentionsRerankerService episodeMentionsRerankerService;

    @Qualifier("searchExecutor")
    private final Executor searchExecutor;

    @Qualifier("searchRerankExecutor")
    private final Executor searchRerankExecutor;

    // ==================== 公开 API ====================

    /**
     * 执行搜索 Pipeline
     *
     * @param query 查询文本
     * @param graphId 图谱 ID
     * @param config 搜索配置
     * @param filters 过滤器（可选）
     * @param centerNodeUuid 中心节点 UUID（可选，用于 NodeDistance 重排）
     * @param bfsOriginUuids BFS 起始节点 UUID 列表（可选）
     * @return 搜索结果
     */
    public SearchResults search(
            String query,
            String graphId,
            SearchConfig config,
            SearchFilters filters,
            String centerNodeUuid,
            List<String> bfsOriginUuids) {

        if (query == null || query.isBlank()) {
            log.info("查询文本为空，返回空结果");
            return SearchResults.empty();
        }

        if (config == null) {
            config = SearchConfig.combinedHybridRrf();
        }
        if (filters == null) {
            filters = SearchFilters.empty();
        }

        final int finalLimit = config.getLimit() > 0 ? config.getLimit() : 10;
        final int methodLimit = finalLimit * 2;

        final SearchConfig finalConfig = config;
        final SearchFilters finalFilters = filters;
        final double[] finalQueryVectorDouble = toDoubleArray(maybeEmbedQuery(query, config));
        final String finalCenterNodeUuid = centerNodeUuid;
        final List<String> finalBfsOriginUuids = bfsOriginUuids;

        // ====== Step 2: 并行执行 4 个 Scope ======
        CompletableFuture<List<EdgeResult>> edgeFuture = CompletableFuture
                .supplyAsync(() -> searchEdges(query, finalQueryVectorDouble, graphId, finalConfig.getEdgeConfig(), finalFilters, finalCenterNodeUuid, finalBfsOriginUuids, methodLimit), searchExecutor);

        CompletableFuture<List<NodeResult>> nodeFuture = CompletableFuture
                .supplyAsync(() -> searchNodes(query, finalQueryVectorDouble, graphId, finalConfig.getNodeConfig(), finalFilters, finalCenterNodeUuid, finalBfsOriginUuids, methodLimit), searchExecutor);

        CompletableFuture<List<EpisodeResult>> episodeFuture = CompletableFuture
                .supplyAsync(() -> searchEpisodes(query, graphId, finalConfig.getEpisodeConfig(), finalFilters, methodLimit), searchExecutor);

        CompletableFuture<List<CommunityResult>> communityFuture = CompletableFuture
                .supplyAsync(() -> searchCommunities(query, finalQueryVectorDouble, graphId, finalConfig.getCommunityConfig(), finalFilters, methodLimit), searchExecutor);

        // 等待所有 Scope 完成
        CompletableFuture.allOf(edgeFuture, nodeFuture, episodeFuture, communityFuture).join();

        // ====== Step 3: 构建结果 ======
        SearchResults results = new SearchResults();
        results.setEdges(edgeFuture.join());
        results.setNodes(nodeFuture.join());
        results.setEpisodes(episodeFuture.join());
        results.setCommunities(communityFuture.join());

        // 计算总 RRF 分数（跨 Scope）
        computeCrossScopeScores(results, finalConfig.getRerankerMinScore());

        log.info("搜索完成: query={}, graphId={}, edges={}, nodes={}, episodes={}, communities={}",
                query, graphId,
                results.getEdges() != null ? results.getEdges().size() : 0,
                results.getNodes() != null ? results.getNodes().size() : 0,
                results.getEpisodes() != null ? results.getEpisodes().size() : 0,
                results.getCommunities() != null ? results.getCommunities().size() : 0);

        return results;
    }

    // ==================== Edge Search Scope ====================

    private List<EdgeResult> searchEdges(
            String query,
            double[] queryVector,
            String graphId,
            EdgeSearchConfig config,
            SearchFilters filters,
            String centerNodeUuid,
            List<String> bfsOriginUuids,
            int limit) {

        if (config == null) {
            return List.of();
        }

        // 并行执行所有搜索方法
        List<CompletableFuture<List<EdgeResult>>> methodFutures = new ArrayList<>();

        if (config.getSearchMethods() != null) {
            for (EdgeSearchMethod method : config.getSearchMethods()) {
                switch (method) {
                    case bm25 -> methodFutures.add(CompletableFuture.supplyAsync(
                            () -> searchEdgesBm25(query, graphId, limit), searchExecutor));
                    case cosine_similarity -> methodFutures.add(CompletableFuture.supplyAsync(
                            () -> searchEdgesVector(queryVector, graphId, limit), searchExecutor));
                    case bfs -> {
                        if (bfsOriginUuids != null && !bfsOriginUuids.isEmpty()) {
                            methodFutures.add(CompletableFuture.supplyAsync(
                                    () -> searchEdgesBfsCypher(graphId, bfsOriginUuids, config.getBfsMaxDepth(), limit), searchExecutor));
                        }
                    }
                }
            }
        }

        if (methodFutures.isEmpty()) {
            return List.of();
        }

        // 等待所有方法完成
        CompletableFuture.allOf(methodFutures.toArray(new CompletableFuture[0])).join();

        // 收集原始结果
        List<List<EdgeResult>> rawResults = methodFutures.stream()
                .map(CompletableFuture::join)
                .filter(list -> list != null && !list.isEmpty())
                .collect(Collectors.toList());

        if (rawResults.isEmpty()) {
            return List.of();
        }

        // ====== Reranking ======
        List<EdgeResult> reranked;
        switch (config.getReranker()) {
            case rrf -> {
                reranked = rrfRerankerService.rrfEdges(rawResults, 1);
            }
            case mmr -> {
                reranked = rerankEdgesByMmr(rawResults, queryVector, config.getMmrLambda(), limit);
            }
            case cross_encoder -> {
                reranked = rerankEdgesByCrossEncoder(rawResults, query, limit);
            }
            case node_distance -> {
                if (centerNodeUuid != null) {
                    reranked = rerankEdgesByNodeDistance(rawResults, centerNodeUuid, limit);
                } else {
                    reranked = rrfRerankerService.rrfEdges(rawResults, 1);
                }
            }
            case episode_mentions -> {
                reranked = rerankEdgesByEpisodeMentions(rawResults, limit);
            }
            default -> {
                reranked = rrfRerankerService.rrfEdges(rawResults, 1);
            }
        }

        return reranked.stream().limit(limit).collect(Collectors.toList());
    }

    private List<EdgeResult> searchEdgesBm25(String query, String graphId, int limit) {
        List<Map<String, Object>> raw = graphNeo4jService.searchEdgesByFulltext(query, graphId, limit);
        return raw.stream().map(this::mapToEdgeResult).collect(Collectors.toList());
    }

    private List<EdgeResult> searchEdgesVector(double[] queryVector, String graphId, int limit) {
        float[] emb = toFloatArray(queryVector);
        List<Map<String, Object>> raw = graphNeo4jService.searchEdgesByVector(graphId, emb, limit);
        return raw.stream().map(this::mapToEdgeResult).collect(Collectors.toList());
    }

    private List<EdgeResult> searchEdgesBfsCypher(String graphId, List<String> seedUuids, int depth, int limit) {
        List<Map<String, Object>> raw = graphNeo4jService.searchEdgesByBfsCypher(graphId, seedUuids, depth, limit);
        return raw.stream().map(this::mapToEdgeResult).collect(Collectors.toList());
    }

    // ==================== Node Search Scope ====================

    private List<NodeResult> searchNodes(
            String query,
            double[] queryVector,
            String graphId,
            NodeSearchConfig config,
            SearchFilters filters,
            String centerNodeUuid,
            List<String> bfsOriginUuids,
            int limit) {

        if (config == null) {
            return List.of();
        }

        List<CompletableFuture<List<NodeResult>>> methodFutures = new ArrayList<>();

        if (config.getSearchMethods() != null) {
            for (NodeSearchMethod method : config.getSearchMethods()) {
                switch (method) {
                    case bm25 -> methodFutures.add(CompletableFuture.supplyAsync(
                            () -> searchNodesBm25(query, graphId, limit), searchExecutor));
                    case cosine_similarity -> methodFutures.add(CompletableFuture.supplyAsync(
                            () -> searchNodesVector(queryVector, graphId, limit), searchExecutor));
                    case bfs -> {
                        if (bfsOriginUuids != null && !bfsOriginUuids.isEmpty()) {
                            methodFutures.add(CompletableFuture.supplyAsync(
                                    () -> searchNodesBfsCypher(graphId, bfsOriginUuids, config.getBfsMaxDepth(), limit), searchExecutor));
                        }
                    }
                }
            }
        }

        if (methodFutures.isEmpty()) {
            return List.of();
        }

        CompletableFuture.allOf(methodFutures.toArray(new CompletableFuture[0])).join();

        List<List<NodeResult>> rawResults = methodFutures.stream()
                .map(CompletableFuture::join)
                .filter(list -> list != null && !list.isEmpty())
                .collect(Collectors.toList());

        if (rawResults.isEmpty()) {
            return List.of();
        }

        List<NodeResult> reranked;
        switch (config.getReranker()) {
            case rrf -> {
                reranked = rrfRerankerService.rrfNodes(rawResults, 1);
            }
            case mmr -> {
                reranked = rerankNodesByMmr(rawResults, queryVector, config.getMmrLambda(), limit);
            }
            case cross_encoder -> {
                reranked = rerankNodesByCrossEncoder(rawResults, query, limit);
            }
            case episode_mentions -> {
                reranked = rerankNodesByEpisodeMentions(rawResults, limit);
            }
            default -> {
                reranked = rrfRerankerService.rrfNodes(rawResults, 1);
            }
        }

        return reranked.stream().limit(limit).collect(Collectors.toList());
    }

    private List<NodeResult> searchNodesBm25(String query, String graphId, int limit) {
        List<Map<String, Object>> raw = graphNeo4jService.searchNodesByFulltext(query, graphId, limit);
        return raw.stream().map(this::mapToNodeResult).collect(Collectors.toList());
    }

    private List<NodeResult> searchNodesVector(double[] queryVector, String graphId, int limit) {
        float[] emb = toFloatArray(queryVector);
        List<Map<String, Object>> raw = graphNeo4jService.searchNodesByVector(graphId, emb, limit);
        return raw.stream().map(this::mapToNodeResult).collect(Collectors.toList());
    }

    private List<NodeResult> searchNodesBfsCypher(String graphId, List<String> seedUuids, int depth, int limit) {
        List<Map<String, Object>> raw = graphNeo4jService.searchNodesByBfsCypher(graphId, seedUuids, depth, limit);
        return raw.stream().map(this::mapToNodeResult).collect(Collectors.toList());
    }

    // ==================== Episode Search Scope ====================

    private List<EpisodeResult> searchEpisodes(
            String query,
            String graphId,
            EpisodeSearchConfig config,
            SearchFilters filters,
            int limit) {

        if (config == null || config.getSearchMethods() == null) {
            return List.of();
        }

        List<CompletableFuture<List<EpisodeResult>>> methodFutures = new ArrayList<>();

        for (EpisodeSearchMethod method : config.getSearchMethods()) {
            switch (method) {
                case bm25 -> methodFutures.add(CompletableFuture.supplyAsync(
                        () -> searchEpisodesBm25(query, graphId, limit), searchExecutor));
            }
        }

        if (methodFutures.isEmpty()) {
            return List.of();
        }

        CompletableFuture.allOf(methodFutures.toArray(new CompletableFuture[0])).join();

        List<List<EpisodeResult>> rawResults = methodFutures.stream()
                .map(CompletableFuture::join)
                .filter(list -> list != null && !list.isEmpty())
                .collect(Collectors.toList());

        if (rawResults.isEmpty()) {
            return List.of();
        }

        List<EpisodeResult> reranked;
        switch (config.getReranker()) {
            case rrf -> {
                reranked = rrfRerankerService.rrfEpisodes(rawResults, 1);
            }
            case cross_encoder -> {
                reranked = rerankEpisodesByCrossEncoder(rawResults, query, limit);
            }
            default -> {
                reranked = rrfRerankerService.rrfEpisodes(rawResults, 1);
            }
        }

        return reranked.stream().limit(limit).collect(Collectors.toList());
    }

    private List<EpisodeResult> searchEpisodesBm25(String query, String graphId, int limit) {
        List<Map<String, Object>> raw = graphNeo4jService.searchEpisodesByFulltext(query, graphId, limit);
        return raw.stream().map(this::mapToEpisodeResult).collect(Collectors.toList());
    }

    // ==================== Community Search Scope ====================

    private List<CommunityResult> searchCommunities(
            String query,
            double[] queryVector,
            String graphId,
            CommunitySearchConfig config,
            SearchFilters filters,
            int limit) {

        if (config == null || config.getSearchMethods() == null) {
            return List.of();
        }

        List<CompletableFuture<List<CommunityResult>>> methodFutures = new ArrayList<>();

        for (CommunitySearchMethod method : config.getSearchMethods()) {
            switch (method) {
                case bm25 -> methodFutures.add(CompletableFuture.supplyAsync(
                        () -> searchCommunitiesBm25(query, graphId, limit), searchExecutor));
                case cosine_similarity -> methodFutures.add(CompletableFuture.supplyAsync(
                        () -> searchCommunitiesVector(queryVector, graphId, limit), searchExecutor));
            }
        }

        if (methodFutures.isEmpty()) {
            return List.of();
        }

        CompletableFuture.allOf(methodFutures.toArray(new CompletableFuture[0])).join();

        List<List<CommunityResult>> rawResults = methodFutures.stream()
                .map(CompletableFuture::join)
                .filter(list -> list != null && !list.isEmpty())
                .collect(Collectors.toList());

        if (rawResults.isEmpty()) {
            return List.of();
        }

        List<CommunityResult> reranked;
        switch (config.getReranker()) {
            case rrf, mmr, cross_encoder -> {
                reranked = rrfRerankerService.rrfCommunities(rawResults, 1);
            }
            default -> {
                reranked = rrfRerankerService.rrfCommunities(rawResults, 1);
            }
        }

        return reranked.stream().limit(limit).collect(Collectors.toList());
    }

    private List<CommunityResult> searchCommunitiesBm25(String query, String graphId, int limit) {
        List<Map<String, Object>> raw = graphNeo4jService.searchCommunitiesByFulltext(query, graphId, limit);
        return raw.stream().map(this::mapToCommunityResult).collect(Collectors.toList());
    }

    private List<CommunityResult> searchCommunitiesVector(double[] queryVector, String graphId, int limit) {
        float[] emb = toFloatArray(queryVector);
        List<Map<String, Object>> raw = graphNeo4jService.searchCommunitiesByVector(graphId, emb, limit);
        return raw.stream().map(this::mapToCommunityResult).collect(Collectors.toList());
    }

    // ==================== Reranking 方法 ====================

    private List<EdgeResult> rerankEdgesByMmr(
            List<List<EdgeResult>> rawResults,
            double[] queryVector,
            double lambda,
            int limit) {

        // 展平所有结果
        List<EdgeResult> allEdges = rawResults.stream()
                .flatMap(List::stream)
                .distinct()
                .collect(Collectors.toList());

        if (allEdges.isEmpty()) {
            return List.of();
        }

        // 获取嵌入向量
        List<String> uuids = allEdges.stream().map(EdgeResult::getUuid).collect(Collectors.toList());
        Map<String, float[]> embeddings = graphNeo4jService.getEdgeEmbeddings(uuids);
        Map<String, double[]> doubleEmbeddings = new HashMap<>();
        embeddings.forEach((k, v) -> doubleEmbeddings.put(k, toDoubleArray(v)));

        // 构建文本映射
        Map<String, String> factMap = new HashMap<>();
        allEdges.forEach(e -> factMap.put(e.getUuid(), e.getFact() != null ? e.getFact() : ""));

        // MMR 重排
        List<String> rankedUuids;
        if (doubleEmbeddings.size() > 10) {
            rankedUuids = mmrRerankerService.mmrEdges(
                    queryVector, uuids, doubleEmbeddings, factMap, lambda, Math.min(limit * 2, allEdges.size()));
        } else {
            rankedUuids = mmrRerankerService.mmrByText(uuids, factMap, lambda, Math.min(limit * 2, allEdges.size()));
        }

        // 重建结果顺序
        Map<String, EdgeResult> uuidMap = new HashMap<>();
        allEdges.forEach(e -> uuidMap.put(e.getUuid(), e));

        return rankedUuids.stream()
                .map(uuidMap::get)
                .filter(Objects::nonNull)
                .limit(limit)
                .collect(Collectors.toList());
    }

    private List<NodeResult> rerankNodesByMmr(
            List<List<NodeResult>> rawResults,
            double[] queryVector,
            double lambda,
            int limit) {

        List<NodeResult> allNodes = rawResults.stream()
                .flatMap(List::stream)
                .distinct()
                .collect(Collectors.toList());

        if (allNodes.isEmpty()) {
            return List.of();
        }

        List<String> uuids = allNodes.stream().map(NodeResult::getUuid).collect(Collectors.toList());
        Map<String, float[]> embeddings = graphNeo4jService.getNodeEmbeddings(uuids);
        Map<String, double[]> doubleEmbeddings = new HashMap<>();
        embeddings.forEach((k, v) -> doubleEmbeddings.put(k, toDoubleArray(v)));

        Map<String, String> nameMap = new HashMap<>();
        allNodes.forEach(n -> nameMap.put(n.getUuid(), (n.getName() != null ? n.getName() : "") + " " + (n.getSummary() != null ? n.getSummary() : "")));

        List<String> rankedUuids;
        if (doubleEmbeddings.size() > 10) {
            rankedUuids = mmrRerankerService.mmrNodes(queryVector, uuids, doubleEmbeddings, nameMap, lambda, Math.min(limit * 2, allNodes.size()));
        } else {
            rankedUuids = mmrRerankerService.mmrByText(uuids, nameMap, lambda, Math.min(limit * 2, allNodes.size()));
        }

        Map<String, NodeResult> uuidMap = new HashMap<>();
        allNodes.forEach(n -> uuidMap.put(n.getUuid(), n));

        return rankedUuids.stream()
                .map(uuidMap::get)
                .filter(Objects::nonNull)
                .limit(limit)
                .collect(Collectors.toList());
    }

    private List<EdgeResult> rerankEdgesByCrossEncoder(
            List<List<EdgeResult>> rawResults,
            String query,
            int limit) {

        List<EdgeResult> allEdges = rawResults.stream()
                .flatMap(List::stream)
                .distinct()
                .collect(Collectors.toList());

        if (allEdges.isEmpty()) {
            return List.of();
        }

        Map<String, String> factMap = new HashMap<>();
        allEdges.forEach(e -> factMap.put(e.getUuid(), e.getFact() != null ? e.getFact() : ""));

        List<String> rankedUuids = crossEncoderRerankerService.rankEdges(query, factMap, limit);

        Map<String, EdgeResult> uuidMap = new HashMap<>();
        allEdges.forEach(e -> uuidMap.put(e.getUuid(), e));

        return rankedUuids.stream()
                .map(uuidMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<NodeResult> rerankNodesByCrossEncoder(
            List<List<NodeResult>> rawResults,
            String query,
            int limit) {

        List<NodeResult> allNodes = rawResults.stream()
                .flatMap(List::stream)
                .distinct()
                .collect(Collectors.toList());

        if (allNodes.isEmpty()) {
            return List.of();
        }

        Map<String, String> nameMap = new HashMap<>();
        Map<String, String> summaryMap = new HashMap<>();
        allNodes.forEach(n -> {
            nameMap.put(n.getUuid(), n.getName() != null ? n.getName() : "");
            summaryMap.put(n.getUuid(), n.getSummary() != null ? n.getSummary() : "");
        });

        List<String> rankedUuids = crossEncoderRerankerService.rankNodes(query, nameMap, summaryMap, limit);

        Map<String, NodeResult> uuidMap = new HashMap<>();
        allNodes.forEach(n -> uuidMap.put(n.getUuid(), n));

        return rankedUuids.stream()
                .map(uuidMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<EpisodeResult> rerankEpisodesByCrossEncoder(
            List<List<EpisodeResult>> rawResults,
            String query,
            int limit) {

        List<EpisodeResult> all = rawResults.stream()
                .flatMap(List::stream)
                .distinct()
                .collect(Collectors.toList());

        if (all.isEmpty()) {
            return List.of();
        }

        Map<String, String> contentMap = new HashMap<>();
        all.forEach(e -> contentMap.put(e.getUuid(), (e.getContent() != null ? e.getContent() : "")));

        List<String> rankedUuids = crossEncoderRerankerService.rankEdges(query, contentMap, limit);

        Map<String, EpisodeResult> uuidMap = new HashMap<>();
        all.forEach(e -> uuidMap.put(e.getUuid(), e));

        return rankedUuids.stream()
                .map(uuidMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<EdgeResult> rerankEdgesByNodeDistance(
            List<List<EdgeResult>> rawResults,
            String centerNodeUuid,
            int limit) {

        List<EdgeResult> allEdges = rawResults.stream()
                .flatMap(List::stream)
                .distinct()
                .collect(Collectors.toList());

        if (allEdges.isEmpty()) {
            return List.of();
        }

        List<String> uuids = allEdges.stream().map(EdgeResult::getUuid).collect(Collectors.toList());
        Map<String, String> sourceMap = new HashMap<>();
        allEdges.forEach(e -> sourceMap.put(e.getUuid(), e.getSourceNodeUuid() != null ? e.getSourceNodeUuid() : ""));

        List<String> rankedUuids = nodeDistanceRerankerService.rerankEdgesByDistance(
                uuids, centerNodeUuid, sourceMap, limit);

        Map<String, EdgeResult> uuidMap = new HashMap<>();
        allEdges.forEach(e -> uuidMap.put(e.getUuid(), e));

        return rankedUuids.stream()
                .map(uuidMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<EdgeResult> rerankEdgesByEpisodeMentions(
            List<List<EdgeResult>> rawResults,
            int limit) {

        List<EdgeResult> allEdges = rawResults.stream()
                .flatMap(List::stream)
                .distinct()
                .collect(Collectors.toList());

        if (allEdges.isEmpty()) {
            return List.of();
        }

        List<String> uuids = allEdges.stream().map(EdgeResult::getUuid).collect(Collectors.toList());
        List<String> rankedUuids = episodeMentionsRerankerService.rerankEdgesByMentions(uuids, limit);

        Map<String, EdgeResult> uuidMap = new HashMap<>();
        allEdges.forEach(e -> uuidMap.put(e.getUuid(), e));

        return rankedUuids.stream()
                .map(uuidMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<NodeResult> rerankNodesByEpisodeMentions(
            List<List<NodeResult>> rawResults,
            int limit) {

        List<NodeResult> allNodes = rawResults.stream()
                .flatMap(List::stream)
                .distinct()
                .collect(Collectors.toList());

        if (allNodes.isEmpty()) {
            return List.of();
        }

        List<String> uuids = allNodes.stream().map(NodeResult::getUuid).collect(Collectors.toList());
        List<String> rankedUuids = episodeMentionsRerankerService.rerankNodesByMentions(uuids, limit);

        Map<String, NodeResult> uuidMap = new HashMap<>();
        allNodes.forEach(n -> uuidMap.put(n.getUuid(), n));

        return rankedUuids.stream()
                .map(uuidMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 对已有候选项进行重排
     */
    @Override
    public SearchResults rerank(
            String query,
            String reranker,
            Double mmrLambda,
            String centerNodeUuid,
            Integer limit,
            List<? extends EdgeResult> edges,
            List<? extends NodeResult> nodes) {

        if (limit == null || limit <= 0) {
            limit = 10;
        }
        if (mmrLambda == null) {
            mmrLambda = 0.5;
        }

        SearchResults results = new SearchResults();
        String rerankerKey = reranker != null ? reranker.toLowerCase() : "rrf";

        // 边重排
        if (edges != null && !edges.isEmpty()) {
            List<EdgeResult> edgesInput = edges.stream().map(e -> {
                EdgeResult copy = new EdgeResult();
                copy.setUuid(e.getUuid());
                copy.setName(e.getName());
                copy.setFact(e.getFact());
                copy.setSourceNodeUuid(e.getSourceNodeUuid());
                copy.setTargetNodeUuid(e.getTargetNodeUuid());
                copy.setGroupId(e.getGroupId());
                copy.setScore(e.getScore());
                return copy;
            }).collect(java.util.stream.Collectors.toList());

            List<EdgeResult> rerankedEdges;
            switch (rerankerKey) {
                case "mmr" -> {
                    double[] qVec = new double[embedderService.getDimensions()];
                    rerankedEdges = rerankEdgesByMmr(List.of(edgesInput), qVec, mmrLambda, limit);
                }
                case "cross_encoder" -> rerankedEdges = rerankEdgesByCrossEncoder(List.of(edgesInput), query, limit);
                case "node_distance" -> {
                    if (centerNodeUuid != null) {
                        rerankedEdges = rerankEdgesByNodeDistance(List.of(edgesInput), centerNodeUuid, limit);
                    } else {
                        rerankedEdges = rrfRerankerService.rrfEdges(List.of(edgesInput), 1);
                    }
                }
                case "episode_mentions" -> rerankedEdges = rerankEdgesByEpisodeMentions(List.of(edgesInput), limit);
                default -> rerankedEdges = rrfRerankerService.rrfEdges(List.of(edgesInput), 1);
            }

            results.setEdges(rerankedEdges);
        } else {
            results.setEdges(List.of());
        }

        // 节点重排
        if (nodes != null && !nodes.isEmpty()) {
            List<NodeResult> nodesInput = nodes.stream().map(n -> {
                NodeResult copy = new NodeResult();
                copy.setUuid(n.getUuid());
                copy.setName(n.getName());
                copy.setSummary(n.getSummary());
                copy.setLabels(n.getLabels());
                copy.setScore(n.getScore());
                return copy;
            }).collect(java.util.stream.Collectors.toList());

            List<NodeResult> rerankedNodes;
            switch (rerankerKey) {
                case "mmr" -> {
                    double[] qVec = new double[embedderService.getDimensions()];
                    rerankedNodes = rerankNodesByMmr(List.of(nodesInput), qVec, mmrLambda, limit);
                }
                case "cross_encoder" -> rerankedNodes = rerankNodesByCrossEncoder(List.of(nodesInput), query, limit);
                case "episode_mentions" -> rerankedNodes = rerankNodesByEpisodeMentions(List.of(nodesInput), limit);
                default -> rerankedNodes = rrfRerankerService.rrfNodes(List.of(nodesInput), 1);
            }

            results.setNodes(rerankedNodes);
        } else {
            results.setNodes(List.of());
        }

        log.info("重排完成: query={}, reranker={}, edges={}, nodes={}",
                query, reranker,
                results.getEdges() != null ? results.getEdges().size() : 0,
                results.getNodes() != null ? results.getNodes().size() : 0);

        return results;
    }

    // ==================== 辅助方法 ====================

    /**
     * 条件生成查询向量
     * 仅当配置需要向量搜索或 MMR 时才调用 embedder
     */
    private float[] maybeEmbedQuery(String query, SearchConfig config) {
        boolean needsVector = false;

        if (config.getEdgeConfig() != null && config.getEdgeConfig().getSearchMethods() != null) {
            for (EdgeSearchMethod m : config.getEdgeConfig().getSearchMethods()) {
                if (m == EdgeSearchMethod.cosine_similarity) {
                    needsVector = true;
                    break;
                }
            }
            if (!needsVector && config.getEdgeConfig().getReranker() == RerankerType.mmr) {
                needsVector = true;
            }
        }

        if (!needsVector && config.getNodeConfig() != null && config.getNodeConfig().getSearchMethods() != null) {
            for (NodeSearchMethod m : config.getNodeConfig().getSearchMethods()) {
                if (m == NodeSearchMethod.cosine_similarity) {
                    needsVector = true;
                    break;
                }
            }
        }

        if (!needsVector && config.getCommunityConfig() != null
                && config.getCommunityConfig().getSearchMethods() != null) {
            for (CommunitySearchMethod m : config.getCommunityConfig().getSearchMethods()) {
                if (m == CommunitySearchMethod.cosine_similarity) {
                    needsVector = true;
                    break;
                }
            }
        }

        if (needsVector) {
            try {
                return embedderService.embed(query.replace("\n", " ").trim());
            } catch (Exception e) {
                log.warn("查询嵌入失败: {}", e.getMessage());
            }
        }

        return new float[embedderService.getDimensions()];
    }

    private void computeCrossScopeScores(SearchResults results, double minScore) {
        // 跨 Scope 的 RRF 分数计算（简单加总）
        double edgeMax = results.getEdges() != null ?
                results.getEdges().stream()
                        .mapToDouble(e -> e.getScore() != null ? e.getScore() : 0.0)
                        .max().orElse(0.0) : 0.0;

        double nodeMax = results.getNodes() != null ?
                results.getNodes().stream()
                        .mapToDouble(n -> n.getScore() != null ? n.getScore() : 0.0)
                        .max().orElse(0.0) : 0.0;

        // 归一化分数
        if (edgeMax > 0 && results.getEdges() != null) {
            results.setEdgeRerankerScores(results.getEdges().stream()
                    .map(e -> {
                        double s = e.getScore() != null ? e.getScore() / edgeMax : 0.0;
                        e.setScore(s);
                        return s;
                    }).collect(Collectors.toList()));
        }

        if (nodeMax > 0 && results.getNodes() != null) {
            results.setNodeRerankerScores(results.getNodes().stream()
                    .map(n -> {
                        double s = n.getScore() != null ? n.getScore() / nodeMax : 0.0;
                        n.setScore(s);
                        return s;
                    }).collect(Collectors.toList()));
        }
    }

    // ==================== Map 转换 ====================

    private EdgeResult mapToEdgeResult(Map<String, Object> row) {
        EdgeResult r = new EdgeResult();
        r.setUuid((String) row.get("uuid"));
        r.setName((String) row.get("name"));
        r.setFact((String) row.get("fact"));
        r.setType((String) row.get("type"));
        r.setGroupId((String) row.get("graph_id"));
        r.setSourceNodeUuid((String) row.get("source"));
        r.setTargetNodeUuid((String) row.get("target"));
        if (row.get("score") instanceof Number) {
            r.setScore(((Number) row.get("score")).doubleValue());
        }
        return r;
    }

    private NodeResult mapToNodeResult(Map<String, Object> row) {
        NodeResult r = new NodeResult();
        r.setUuid((String) row.get("uuid"));
        r.setName((String) row.get("name"));
        if (row.get("summary") != null) {
            r.setSummary(row.get("summary").toString());
        }
        if (row.get("score") instanceof Number) {
            r.setScore(((Number) row.get("score")).doubleValue());
        }
        return r;
    }

    private EpisodeResult mapToEpisodeResult(Map<String, Object> row) {
        EpisodeResult r = new EpisodeResult();
        r.setUuid((String) row.get("uuid"));
        r.setName((String) row.get("name"));
        r.setContent(row.get("content") != null ? row.get("content").toString() : null);
        r.setSource(row.get("source") != null ? row.get("source").toString() : null);
        if (row.get("score") instanceof Number) {
            r.setScore(((Number) row.get("score")).doubleValue());
        }
        return r;
    }

    private CommunityResult mapToCommunityResult(Map<String, Object> row) {
        CommunityResult r = new CommunityResult();
        r.setUuid((String) row.get("uuid"));
        r.setName((String) row.get("name"));
        if (row.get("summary") != null) {
            r.setSummary(row.get("summary").toString());
        }
        if (row.get("score") instanceof Number) {
            r.setScore(((Number) row.get("score")).doubleValue());
        }
        return r;
    }

    private double[] toDoubleArray(float[] arr) {
        if (arr == null) return new double[0];
        double[] result = new double[arr.length];
        for (int i = 0; i < arr.length; i++) {
            result[i] = arr[i];
        }
        return result;
    }

    private float[] toFloatArray(double[] arr) {
        if (arr == null) return new float[0];
        float[] result = new float[arr.length];
        for (int i = 0; i < arr.length; i++) {
            result[i] = (float) arr[i];
        }
        return result;
    }
}
