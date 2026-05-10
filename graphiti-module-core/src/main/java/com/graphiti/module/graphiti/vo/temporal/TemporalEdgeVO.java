package com.graphiti.module.graphiti.vo.temporal;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class TemporalEdgeVO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String uuid;
    private String sourceUuid;
    private String targetUuid;
    private String type;
    private String fact;
    private LocalDateTime validAt;
    private LocalDateTime invalidAt;
    private Map<String, Object> properties;
}
