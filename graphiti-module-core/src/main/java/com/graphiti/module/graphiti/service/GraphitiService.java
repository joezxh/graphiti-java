package com.graphiti.module.graphiti.service;

import com.graphiti.module.graphiti.vo.graph.CreateGraphReqVO;
import com.graphiti.module.graphiti.vo.graph.GraphInfoRespVO;
import com.graphiti.module.graphiti.vo.graph.GraphListRespVO;
import com.graphiti.module.graphiti.vo.graph.GraphStatsRespVO;
import com.graphiti.module.graphiti.vo.graph.UpdateGraphReqVO;
import java.util.List;

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
     * 获取图谱列表
     * @return List<GraphListRespVO>
     */
    List<GraphListRespVO> listGraphs();
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
     * 清空图谱数据（删除 Neo4j 中的数据，保留 MySQL 元数据）
     * @param graphId 图谱ID
     */
    void clearGraph(String graphId);
    /**
     * 获取图谱统计信息
     * @return GraphStatsRespVO
     */
    GraphStatsRespVO getGraphStats();
}
