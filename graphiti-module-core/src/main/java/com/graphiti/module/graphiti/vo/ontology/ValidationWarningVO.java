package com.graphiti.module.graphiti.vo.ontology;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationWarningVO {
    private int layer;
    private String message;
    private String suggestion;
}
