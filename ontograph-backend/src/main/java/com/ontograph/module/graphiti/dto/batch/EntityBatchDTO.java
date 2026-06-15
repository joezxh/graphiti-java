package com.ontograph.module.graphiti.dto.batch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Entity batch DTO for Neo4j UNWIND batch operations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityBatchDTO {

    private String uuid;
    private String name;
    private String type;
    private String summary;
    private float[] embedding;
    private Map<String, Object> properties;

    /**
     * Convert to Map for Neo4j parameter binding.
     * float[] is converted to List&lt;Float&gt; because Neo4j doesn't support float[] directly.
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("uuid", uuid);
        map.put("name", name);
        map.put("type", type);
        map.put("summary", summary != null ? summary : "");
        if (embedding != null) {
            map.put("embedding", toFloatList(embedding));
        } else {
            map.put("embedding", null);
        }
        map.put("properties", properties != null ? properties : new HashMap<>());
        return map;
    }

    private static List<Float> toFloatList(float[] arr) {
        if (arr == null) return null;
        List<Float> list = new java.util.ArrayList<>(arr.length);
        for (float v : arr) list.add(v);
        return list;
    }
}
