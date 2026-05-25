package com.ontograph.module.graphiti.service;

import com.ontograph.module.graphiti.vo.ontology.SchemaOrgImportReqVO;
import java.util.List;
import java.util.Map;

public interface SchemaOrgImportService {

    Map<String, Integer> importFromSchemaOrg(String graphId, SchemaOrgImportReqVO reqVO);

    String exportAsJsonLd(String graphId);

    String exportAsTurtle(String graphId);
}
