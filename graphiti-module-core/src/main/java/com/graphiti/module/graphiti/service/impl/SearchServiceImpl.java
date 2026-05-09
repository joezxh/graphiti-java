package com.graphiti.module.graphiti.service.impl;

import com.graphiti.module.graphiti.service.GraphNeo4jService;
import com.graphiti.module.graphiti.service.SearchService;
import com.graphiti.module.graphiti.vo.search.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 搜索服务实现类（简化版 - 仅实现基础全文搜索）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final GraphNeo4jService graphNeo4jService;

    @Override
    public SearchResultsRespVO search(SearchQueryReqVO reqVO) {
        return doSearch(reqVO.getQuery(), 
                       reqVO.getGroupIds(), 
                       reqVO.getMaxFacts() != null ? reqVO.getMaxFacts() : 10);
    }

    @Override
    public SearchResultsRespVO searchGraph(String graphId, SearchQueryReqVO reqVO) {
        List<String> groupIds = reqVO.getGroupIds() != null ? reqVO.getGroupIds() : new ArrayList<>();
        if (!groupIds.contains(graphId)) {
            groupIds.add(graphId);
        }
        return doSearch(reqVO.getQuery(), groupIds, 
                       reqVO.getMaxFacts() != null ? reqVO.getMaxFacts() : 10);
    }

    @Override
    public GetMemoryRespVO getMemory(GetMemoryReqVO reqVO) {
        // 1. 从最后N条消息提取 query（取最后一条用户消息）
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
        SearchResultsRespVO searchResult = doSearch(query, reqVO.getGroupIds(), maxFacts);
        
        // 2. 构建 context 字符串
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

    // ==================== 私有方法 ====================

    /**
     * 核心搜索方法（简化版 - 仅全文搜索）
     */
    private SearchResultsRespVO doSearch(String query, List<String> groupIds, int maxFacts) {
        // TODO: 集成向量检索和 AI Embedding
        log.info("执行搜索：query={}, groupIds={}", query, groupIds);
        
        List<FactResultVO> facts = new ArrayList<>();
        List<NodeResultVO> nodes = new ArrayList<>();
        
        // 简化实现：如果指定了 groupId，执行基本的 Neo4j 全文搜索
        if (groupIds != null && !groupIds.isEmpty()) {
            for (String graphId : groupIds) {
                // 搜索边（事实）
                List<Map<String, Object>> edges = graphNeo4jService.searchEdgesByFulltext(query, graphId, maxFacts);
                for (Map<String, Object> edge : edges) {
                    facts.add(convertToFactResult(edge));
                }
                
                // 搜索节点（实体）
                List<Map<String, Object>> searchNodes = graphNeo4jService.searchNodesByFulltext(query, graphId, maxFacts);
                for (Map<String, Object> node : searchNodes) {
                    nodes.add(convertToNodeResult(node));
                }
            }
        }
        
        SearchResultsRespVO respVO = new SearchResultsRespVO();
        respVO.setFacts(facts);
        respVO.setTotalCount(facts.size());
        respVO.setNodes(nodes);
        respVO.setNodeCount(nodes.size());
        
        return respVO;
    }

    /**
     * 转换为 FactResultVO
     */
    private FactResultVO convertToFactResult(Map<String, Object> edge) {
        FactResultVO vo = new FactResultVO();
        vo.setUuid((String) edge.get("uuid"));
        vo.setName((String) edge.get("name"));
        vo.setFact((String) edge.get("fact"));
        vo.setSourceNodeUuid((String) edge.get("source_node_uuid"));
        vo.setTargetNodeUuid((String) edge.get("target_node_uuid"));
        vo.setGroupId((String) edge.get("group_id"));
        
        Object score = edge.get("score");
        if (score != null) {
            vo.setScore(score instanceof Number ? ((Number) score).doubleValue() : null);
        }
        
        return vo;
    }

    /**
     * 转换为 NodeResultVO
     */
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
