package com.graphiti.module.graphiti.namespace.graph;

import com.graphiti.module.graphiti.service.OntologyService;
import com.graphiti.module.graphiti.vo.ontology.OntologyRespVO;
import com.graphiti.module.graphiti.vo.ontology.SetOntologyReqVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 本体论命名空间
 * 对应 Python: graphiti.graphs.ontology
 *
 * <p>封装实体类型（Entity Classes）和关系类型（Edge Types）的
 * Schema 定义管理。
 */
@Slf4j
@RequiredArgsConstructor
public class OntologyNamespace {

    private final OntologyService ontologyService;

    /**
     * 获取本体定义
     */
    public OntologyRespVO get(String graphId) {
        log.debug("OntologyNamespace.get: graphId={}", graphId);
        return ontologyService.getOntology(graphId);
    }

    /**
     * 设置本体定义
     */
    public OntologyRespVO set(String graphId, SetOntologyReqVO reqVO) {
        log.info("OntologyNamespace.set: graphId={}", graphId);
        return ontologyService.setOntology(graphId, reqVO);
    }
}
