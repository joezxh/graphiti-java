package com.ontograph.module.graphiti.vo.ontology;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@lombok.Builder
public class ValidationErrorVO {
    private int layer;
    private String code;      // ONT001 - ONT005
    private String message;
    private String property;
    private Object attemptedValue;

    public static ValidationErrorVO of(int layer, String code, String message, String property, Object value) {
        return new ValidationErrorVO(layer, code, message, property, value);
    }
}

