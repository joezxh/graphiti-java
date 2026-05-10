package com.graphiti.module.graphiti.vo.llm;

import lombok.Data;
import java.io.Serializable;
import java.util.Map;

@Data
public class ExtractedEntityVO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String name;
    private String type;
    private String summary;
    private Map<String, Object> attributes;
}
