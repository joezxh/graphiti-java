package com.ontograph.module.graphiti.vo.ontology;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OntPropertyVO {
    private Long id;
    private Long definitionId;
    private String propertyUri;
    private String localName;
    private String propertyType;
    private Long domainClassId;
    private String domainClassUri;
    private Long rangeClassId;
    private String rangeClassUri;
    private String rangeDataType;
    private Integer minCardinality;
    private Integer maxCardinality;
    private String defaultValue;
    private List<String> allowedValues;
    private Long parentPropertyId;
    private String parentPropertyUri;
    private List<String> equivalentTo;
    private Long inverseOfId;
    private String inverseOfUri;
    private Boolean isRequired;
    private Boolean isMultiple;
    private String pattern;
    private BigDecimal minValue;
    private BigDecimal maxValue;
    private String description;
    private String example;
    private String metadata;
    private java.time.LocalDateTime createdAt;
}
