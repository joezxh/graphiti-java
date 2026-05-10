package com.graphiti.module.graphiti.service;

import com.graphiti.module.graphiti.vo.ontology.OntologyRespVO;
import com.graphiti.module.graphiti.vo.ontology.SetOntologyReqVO;

import java.util.List;
import java.util.Map;

/**
 * 本体管理服务接口
 */
public interface OntologyService {
    /**
     * 获取指定图谱的本体定义
     * @param graphId 图谱ID
     * @return OntologyRespVO
     */
    OntologyRespVO getOntology(String graphId);

    /**
     * 设置指定图谱的本体定义
     * @param graphId 图谱ID
     * @param reqVO SetOntologyReqVO
     * @return OntologyRespVO
     */
    OntologyRespVO setOntology(String graphId, SetOntologyReqVO reqVO);

    /**
     * 验证节点是否符合本体定义
     * @param graphId 图谱ID
     * @param nodeType 节点类型
     * @param properties 节点属性
     * @return 验证结果
     */
    Map<String, Object> validateNode(String graphId, String nodeType, Map<String, Object> properties);

    /**
     * 验证边是否符合本体定义
     * @param graphId 图谱ID
     * @param edgeType 边类型
     * @param properties 边属性
     * @return 验证结果
     */
    Map<String, Object> validateEdge(String graphId, String edgeType, Map<String, Object> properties);

    /**
     * 获取指定类型的字段定义
     * @param graphId 图谱ID
     * @param typeName 类型名称
     * @return 字段定义列表
     */
    List<Map<String, Object>> getFieldDefinitions(String graphId, String typeName);
}
