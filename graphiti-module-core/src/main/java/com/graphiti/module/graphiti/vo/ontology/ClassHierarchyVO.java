package com.graphiti.module.graphiti.vo.ontology;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassHierarchyVO {
    private String classUri;
    private String localName;
    private String nameEn;
    private String description;
    private String domainHint;
    @Builder.Default
    private List<ClassHierarchyVO> children = new ArrayList<>();
}
