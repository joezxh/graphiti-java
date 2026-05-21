package com.graphiti.module.graphiti.vo.ontology;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InferredTypeVO {
    private String className;
    private String classUri;
    private Double confidence;
    private String reason;
}
