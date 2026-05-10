package com.graphiti.module.graphiti.service;

import com.graphiti.module.graphiti.vo.ontology.OntPropertyVO;
import java.util.List;

public interface OntologyPropertyService {

    OntPropertyVO createProperty(String graphId, OntPropertyVO reqVO);

    OntPropertyVO updateProperty(String graphId, Long propertyId, OntPropertyVO reqVO);

    void deleteProperty(String graphId, Long propertyId);

    OntPropertyVO getProperty(String graphId, Long propertyId);

    List<OntPropertyVO> listProperties(String graphId);

    List<OntPropertyVO> getPropertiesForClass(String graphId, Long classId);

    List<String> getPropertyAncestors(String graphId, Long propertyId);
}
