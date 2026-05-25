package com.ontograph.module.graphiti.dto.batch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Relation batch DTO for Neo4j UNWIND batch operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelationBatchDTO {

    private String edgeUuid;
    private String sourceUuid;
    private String targetUuid;
    private String type;
    private String fact;
    private float[] embedding;
    private Map<String, Object> properties;

    /**
     * Convert to Map for Neo4j parameter binding.
     * float[] is converted to List<Float> because Neo4j doesn't support float[] directly.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("edgeUuid", edgeUuid);
        map.put("sourceUuid", sourceUuid);
        map.put("targetUuid", targetUuid);
        map.put("type", type);
        map.put("fact", fact);
        if (embedding != null) {
            map.put("embedding", toFloatList(embedding));
        } else {
            map.put("embedding", null);
        }
        map.put("properties", properties != null ? properties : new HashMap<>());
        return map;
    }

    private static List<Float> toFloatList(float[] array) {
        return java.util.Arrays.stream(array).boxed().toList();
    }
}
