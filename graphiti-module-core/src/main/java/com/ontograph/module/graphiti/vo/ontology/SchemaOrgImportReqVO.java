package com.graphiti.module.graphiti.vo.ontology;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class SchemaOrgImportReqVO {
    private List<String> domains;
    @Builder.Default
    private String language = "zh-CN";
    @Builder.Default
    private boolean includeInferred = false;
    @Builder.Default
    private int hierarchyDepth = 3;
    private String domainHint;
}
