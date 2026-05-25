package com.ontograph.module.graphiti.dto.batch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Episode batch DTO for Neo4j UNWIND batch operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EpisodeBatchDTO {

    private String uuid;
    private String name;
    private String source;
    private String sourceDescription;
    private String content;
    private Map<String, Object> properties;

    /**
     * Convert to Map for Neo4j parameter binding.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("uuid", uuid);
        map.put("name", name);
        map.put("source", source);
        map.put("sourceDescription", sourceDescription);
        map.put("content", content);
        map.put("properties", properties != null ? properties : new HashMap<>());
        return map;
    }
}
