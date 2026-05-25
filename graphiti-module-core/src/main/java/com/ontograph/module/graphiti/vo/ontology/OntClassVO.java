package com.graphiti.module.graphiti.vo.ontology;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OntClassVO {
    private Long id;
    private Long definitionId;
    private String classUri;
    private String localName;
    private String nameEn;
    private Long parentClassId;
    private String parentClassUri;
    private List<String> equivalentTo;
    private List<String> disjointWith;
    private String description;
    private String example;
    private String domainHint;
    private String metadata;
    private LocalDateTime createdAt;
}
