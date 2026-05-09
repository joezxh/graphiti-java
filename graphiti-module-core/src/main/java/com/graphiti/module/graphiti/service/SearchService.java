package com.graphiti.module.graphiti.service;

import com.graphiti.module.graphiti.vo.search.GetMemoryReqVO;
import com.graphiti.module.graphiti.vo.search.GetMemoryRespVO;
import com.graphiti.module.graphiti.vo.search.SearchQueryReqVO;
import com.graphiti.module.graphiti.vo.search.SearchResultsRespVO;

/**
 * 搜索检索服务接口
 *
 * <p>定义了与 Graphiti 服务功能等价的全部操作：
 * <ul>
 *   <li>全局搜索（多 groupId）</li>
 *   <li>图谱级别搜索</li>
 *   <li>获取记忆（基于对话历史重建上下文）</li>
 * </ul>
 */
public interface SearchService {

    /**
     * 全局搜索（多 groupId）
     * @param reqVO 搜索请求
     * @return 搜索结果
     */
    SearchResultsRespVO search(SearchQueryReqVO reqVO);

    /**
     * 图谱级别搜索
     * @param graphId 图谱ID
     * @param reqVO 搜索请求
     * @return 搜索结果
     */
    SearchResultsRespVO searchGraph(String graphId, SearchQueryReqVO reqVO);

    /**
     * 获取记忆（基于对话历史重建上下文）
     * @param reqVO 获取记忆请求
     * @return 记忆响应
     */
    GetMemoryRespVO getMemory(GetMemoryReqVO reqVO);
}
