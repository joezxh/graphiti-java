package com.ontograph.module.graphiti.service;

import com.ontograph.module.graphiti.vo.graph.CreateGraphReqVO;
import com.ontograph.module.graphiti.vo.graph.GraphDeletePreviewRespVO;
import com.ontograph.module.graphiti.vo.graph.GraphInfoRespVO;
import com.ontograph.module.graphiti.vo.graph.GraphListRespVO;
import com.ontograph.module.graphiti.vo.graph.GraphStatsRespVO;
import com.ontograph.module.graphiti.vo.graph.UpdateGraphReqVO;
import java.util.List;
import java.util.Map;

/**
 * 图谱管理服务接口
 */
public interface GraphitiService {
    /**
     * 创建图谱
     * @param reqVO CreateGraphReqVO
     * @return GraphInfoRespVO
     */
    GraphInfoRespVO createGraph(CreateGraphReqVO reqVO);
    /**
     * 获取图谱列表（分页）
     * @param limit 限制数量（默认100）
     * @param offset 偏移量（默认0）
     * @return GraphListRespVO（含 graphs[], totalCount, rowCount）
     */
    GraphListRespVO listGraphs(Long limit, Long offset);

    /**
     * 获取图谱列表（不分页）
     * @return GraphListRespVO（含 graphs[], totalCount, rowCount）
     */
    GraphListRespVO listGraphs();
    /**
     * 获取图谱详情
     * @param graphId 图谱ID
     * @return GraphInfoRespVO
     */
    GraphInfoRespVO getGraph(String graphId);
    /**
     * 更新图谱信息
     * @param graphId 图谱ID
     * @param reqVO UpdateGraphReqVO
     * @return GraphInfoRespVO
     */
    GraphInfoRespVO updateGraph(String graphId, UpdateGraphReqVO reqVO);
    /**
     * 删除图谱
     * @param graphId 图谱ID
     */
    void deleteGraph(String graphId);
    /**
     * 获取图谱删除预览信息
     * @param graphId 图谱ID
     * @return GraphDeletePreviewRespVO
     */
    GraphDeletePreviewRespVO getGraphDeletePreview(String graphId);
    /**
     * 清空图谱数据（删除 Neo4j 中的数据，保留 MySQL 元数据）
     * @param graphId 图谱ID
     */
    void clearGraph(String graphId);
    /**
     * 获取图谱统计信息
     * @return GraphStatsRespVO
     */
    GraphStatsRespVO getGraphStats();

    /**
     * 克隆图谱
     * @param graphId 源图谱ID
     * @return 新图谱信息
     */
    GraphInfoRespVO cloneGraph(String graphId);

    /**
     * 导出图谱数据
     * @param graphId 图谱ID
     * @return 图谱数据
     */
    Map<String, Object> exportGraph(String graphId);
}
