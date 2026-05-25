package com.ontograph.module.graphiti.vo.ide;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * Schema 属性定义响应 VO
 */
@Data
public class SchemaPropertyRespVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long definitionId;
    private String localName;
    private String propertyType;
    private String rangeDataType;
    private Long domainClassId;
    private Long rangeClassId;
    private Boolean isRequired;
    private Boolean isMultiple;
    private Object defaultValue;
    private List<Object> allowedValues;
    private String pattern;
    private Double minValue;
    private Double maxValue;
    private String description;
}
