package com.graphiti.module.graphiti.service;

import com.graphiti.module.graphiti.vo.imports.AddDataBatchReqVO;
import com.graphiti.module.graphiti.vo.imports.AddDataReqVO;
import com.graphiti.module.graphiti.vo.imports.AddMessagesReqVO;
import com.graphiti.module.graphiti.vo.imports.FactTripleReqVO;

/**
 * 数据导入服务接口
 *
 * <p>定义了数据写入图谱的全部操作：
 * <ul>
 *   <li>添加单条数据（自动提取实体和关系）</li>
 *   <li>批量添加数据</li>
 *   <li>添加消息（对话历史写入图谱）</li>
 *   <li>添加事实三元组</li>
 *   <li>添加实体节点（直接写入，不经过 LLM 提取）</li>
 * </ul>
 */
public interface DataImportService {

    // ==================== 数据写入 ====================

    /**
     * 添加单条数据（自动提取实体和关系）
     * @param reqVO 请求参数
     */
    void addData(AddDataReqVO reqVO);

    /**
     * 批量添加数据
     * @param reqVO 请求参数
     */
    void addDataBatch(AddDataBatchReqVO reqVO);

    /**
     * 添加消息（对话历史写入图谱）
     * @param reqVO 请求参数
     */
    void addMessages(AddMessagesReqVO reqVO);

    /**
     * 添加事实三元组
     * @param reqVO 请求参数
     */
    void addFactTriple(FactTripleReqVO reqVO);

    /**
     * 添加实体节点（直接写入，不经过 LLM 提取）
     * @param graphId 图谱ID
     * @param nodeData 节点数据
     */
    void addEntityNode(String graphId, java.util.Map<String, Object> nodeData);

    // ==================== 删除操作 ====================

    /**
     * 删除实体边
     * @param edgeUuid 边UUID
     */
    void deleteEntityEdge(String edgeUuid);

    /**
     * 删除图谱（含 Neo4j 数据）
     * @param graphId 图谱ID
     */
    void deleteGroup(String graphId);

    /**
     * 删除 Episode
     * @param episodeUuid Episode UUID
     */
    void deleteEpisode(String episodeUuid);
}
