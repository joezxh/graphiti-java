package com.graphiti.module.graphiti.service;

import com.graphiti.module.graphiti.vo.ontology.ConsistencyResultVO;
import java.util.List;
import java.util.Map;

public interface OntologyReasoner {

    void warmUp(String graphId);

    void shutdown(String graphId);

    List<String> getAncestorClasses(String graphId, String classUri);

    List<String> getDescendantClasses(String graphId, String classUri);

    List<String> inferTypes(String graphId, Map<String, Object> properties);

    ConsistencyResultVO checkConsistency(String graphId);

    boolean isSatisfiable(String graphId, String classUri);

    boolean isWarmedUp(String graphId);
}
