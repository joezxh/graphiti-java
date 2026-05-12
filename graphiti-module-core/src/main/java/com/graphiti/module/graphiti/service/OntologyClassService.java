package com.graphiti.module.graphiti.service;

import com.graphiti.module.graphiti.vo.ontology.ClassHierarchyVO;
import com.graphiti.module.graphiti.vo.ontology.OntClassVO;
import com.graphiti.module.graphiti.vo.ontology.OntDefinitionVO;
import com.graphiti.module.graphiti.vo.ontology.OntologyFullVO;
import java.util.List;

public interface OntologyClassService {

    // ==================== 本体定义管理 ====================

    OntDefinitionVO createDefinition(String graphId, OntDefinitionVO reqVO);

    OntDefinitionVO getDefinition(String graphId);

    OntologyFullVO getFullOntology(String graphId);

    // ==================== 类管理 ====================

    OntClassVO createClass(String graphId, OntClassVO reqVO);

    OntClassVO updateClass(String graphId, Long classId, OntClassVO reqVO);

    void deleteClass(String graphId, Long classId);

    OntClassVO getClass(String graphId, Long classId);

    List<OntClassVO> listClasses(String graphId);

    List<ClassHierarchyVO> getClassHierarchy(String graphId);

    List<String> getDescendantClasses(String graphId, Long classId);
}
