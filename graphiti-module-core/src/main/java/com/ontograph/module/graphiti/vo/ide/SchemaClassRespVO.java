package com.graphiti.module.graphiti.vo.ide;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * Schema 类定义响应 VO
 */
@Data
public class SchemaClassRespVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long definitionId;
    private String classUri;
    private String localName;
    private String nameEn;
    private String description;
    private List<Long> parentClassIds;
    private Integer propertyCount;
}
