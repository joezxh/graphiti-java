package com.graphiti.module.graphiti.vo.llm;

import lombok.Data;
import java.io.Serializable;

@Data
public class ExtractedRelationVO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String sourceEntityName;
    private String targetEntityName;
    private String relationType;
    private String fact;
}
