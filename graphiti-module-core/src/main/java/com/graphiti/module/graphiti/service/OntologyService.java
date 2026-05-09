package com.graphiti.module.graphiti.service;

import com.graphiti.module.graphiti.vo.ontology.OntologyRespVO;
import com.graphiti.module.graphiti.vo.ontology.SetOntologyReqVO;

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
}
