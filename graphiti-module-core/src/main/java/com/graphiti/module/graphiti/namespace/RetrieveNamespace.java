package com.graphiti.module.graphiti.namespace;

import com.graphiti.module.graphiti.service.SearchService;
import com.graphiti.module.graphiti.vo.search.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.List;
import java.util.Map;

/**
 * 检索命名空间
 * 对应 Python: graphiti.retrieve
 *
 * <p>封装搜索和记忆检索的核心能力：
 * <ul>
 *   <li>混合搜索（BM25 + 向量 + RRF）</li>
 *   <li>BFS 图遍历搜索</li>
 *   <li>基于对话历史的 Memory 检索</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
public class RetrieveNamespace {

    private final SearchService searchService;

    /**
     * 混合检索（BM25 + 向量 + RRF 融合）
     */
    public SearchResultsRespVO search(String query, String graphId, Integer maxFacts) {
        log.debug("RetrieveNamespace.search: query={}, graphId={}", query, graphId);
        SearchQueryReqVO reqVO = new SearchQueryReqVO();
        reqVO.setQuery(query);
        reqVO.setMaxFacts(maxFacts != null ? maxFacts : 10);
        if (graphId != null) {
            reqVO.setGroupIds(List.of(graphId));
        }
        SearchConfigVO config = new SearchConfigVO();
        config.setMode("hybrid");
        reqVO.setConfig(config);
        return searchService.search(reqVO);
    }

    /**
     * 向量语义搜索
     */
    public SearchResultsRespVO semanticSearch(String query, String graphId, Integer maxFacts) {
        SearchQueryReqVO reqVO = new SearchQueryReqVO();
        reqVO.setQuery(query);
        reqVO.setMaxFacts(maxFacts != null ? maxFacts : 10);
        if (graphId != null) {
            reqVO.setGroupIds(List.of(graphId));
        }
        SearchConfigVO config = new SearchConfigVO();
        config.setMode("vector");
        reqVO.setConfig(config);
        return searchService.search(reqVO);
    }

    /**
     * 图谱级别搜索
     */
    public SearchResultsRespVO searchGraph(String graphId, SearchQueryReqVO reqVO) {
        return searchService.searchGraph(graphId, reqVO);
    }

    /**
     * 基于消息的 Memory 检索
     */
    public GetMemoryRespVO getMemory(GetMemoryReqVO reqVO) {
        log.debug("RetrieveNamespace.getMemory: groupIds={}", reqVO.getGroupIds());
        return searchService.getMemory(reqVO);
    }
}
