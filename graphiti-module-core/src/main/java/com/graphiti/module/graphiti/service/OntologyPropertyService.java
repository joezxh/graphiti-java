package com.graphiti.module.graphiti.service;

import com.graphiti.module.graphiti.vo.ontology.OntConstraintVO;
import com.graphiti.module.graphiti.vo.ontology.OntPropertyVO;
import com.graphiti.module.graphiti.vo.ontology.OntVersionHistoryVO;
import java.util.List;

public interface OntologyPropertyService {

    // ==================== 属性管理 ====================

    OntPropertyVO createProperty(String graphId, OntPropertyVO reqVO);

    OntPropertyVO updateProperty(String graphId, Long propertyId, OntPropertyVO reqVO);

    void deleteProperty(String graphId, Long propertyId);

    OntPropertyVO getProperty(String graphId, Long propertyId);

    List<OntPropertyVO> listProperties(String graphId);

    List<OntPropertyVO> getPropertiesForClass(String graphId, Long classId);

    List<String> getPropertyAncestors(String graphId, Long propertyId);

    // ==================== 约束管理 ====================

    List<OntConstraintVO> listConstraints(String graphId);

    OntConstraintVO createConstraint(String graphId, OntConstraintVO reqVO);

    void deleteConstraint(String graphId, Long constraintId);

    // ==================== 版本历史 ====================

    List<OntVersionHistoryVO> getVersionHistory(String graphId);
}
