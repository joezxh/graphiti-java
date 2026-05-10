package com.graphiti.module.graphiti.vo.temporal;

import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class TemporalNodeVO implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String uuid;
    private String name;
    private String type;
    private String summary;
    private LocalDateTime validAt;
    private LocalDateTime invalidAt;
    private Map<String, Object> properties;
}
