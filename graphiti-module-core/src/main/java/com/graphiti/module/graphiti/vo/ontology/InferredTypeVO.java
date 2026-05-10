package com.graphiti.module.graphiti.vo.ontology;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class InferredTypeVO {
    private String type;
    private String classUri;
    private double confidence;
    private String reason;
}
