package com.graphiti.module.graphiti.service;

import com.graphiti.module.graphiti.vo.ontology.ClassHierarchyVO;
import com.graphiti.module.graphiti.vo.ontology.OntClassVO;
import java.util.List;

public interface OntologyClassService {

    OntClassVO createClass(String graphId, OntClassVO reqVO);

    OntClassVO updateClass(String graphId, Long classId, OntClassVO reqVO);

    void deleteClass(String graphId, Long classId);

    OntClassVO getClass(String graphId, Long classId);

    List<OntClassVO> listClasses(String graphId);

    List<ClassHierarchyVO> getClassHierarchy(String graphId);

    List<String> getDescendantClasses(String graphId, Long classId);
}
