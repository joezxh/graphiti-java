package com.graphiti.module.graphiti.namespace.graph;

import com.graphiti.module.graphiti.service.OntologyClassService;
import com.graphiti.module.graphiti.vo.ontology.OntDefinitionVO;
import com.graphiti.module.graphiti.vo.ontology.OntologyFullVO;
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

    private final OntologyClassService ontologyClassService;

    /**
     * 获取本体定义
     */
    public OntDefinitionVO get(String graphId) {
        log.debug("OntologyNamespace.get: graphId={}", graphId);
        return ontologyClassService.getDefinition(graphId);
    }

    /**
     * 获取完整本体信息
     */
    public OntologyFullVO getFullOntology(String graphId) {
        log.debug("OntologyNamespace.getFullOntology: graphId={}", graphId);
        return ontologyClassService.getFullOntology(graphId);
    }

    /**
     * 创建本体定义
     */
    public OntDefinitionVO create(String graphId, OntDefinitionVO reqVO) {
        log.info("OntologyNamespace.create: graphId={}, name={}", graphId, reqVO.getName());
        return ontologyClassService.createDefinition(graphId, reqVO);
    }
}
