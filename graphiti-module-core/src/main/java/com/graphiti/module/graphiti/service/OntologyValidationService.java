package com.graphiti.module.graphiti.service;

import com.graphiti.module.graphiti.vo.ontology.BatchValidationReqVO;
import com.graphiti.module.graphiti.vo.ontology.BatchValidationRespVO;
import com.graphiti.module.graphiti.vo.ontology.ValidationResultVO;
import java.util.Map;

/**
 * 本体验证引擎服务
 * 负责 6 层验证：
 * Layer 1: 类型存在性
 * Layer 2: 属性必填
 * Layer 3: 数据类型
 * Layer 4: 约束规则
 * Layer 5: OWL 约束（预留）
 * Layer 6: 推理扩展（预留）
 */
public interface OntologyValidationService {

    /**
     * 验证节点是否符合本体定义
     */
    ValidationResultVO validateNode(String graphId, String nodeType, Map<String, Object> properties);

    /**
     * 验证边是否符合本体定义
     */
    ValidationResultVO validateEdge(String graphId, String edgeType, Map<String, Object> properties);

    /**
     * 批量验证节点和边
     */
    BatchValidationRespVO validateBatch(String graphId, BatchValidationReqVO reqVO);

    /**
     * 检查本体是否已定义（向后兼容用）
     */
    boolean hasOntology(String graphId);
}
