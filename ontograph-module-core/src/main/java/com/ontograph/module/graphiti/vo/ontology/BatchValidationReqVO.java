package com.ontograph.module.graphiti.vo.ontology;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class BatchValidationReqVO {
    private List<NodeValidationItem> nodes;
    private List<EdgeValidationItem> edges;

    @Data
    public static class NodeValidationItem {
        private String nodeType;
        private Map<String, Object> properties;
    }

    @Data
    public static class EdgeValidationItem {
        private String edgeType;
        private Map<String, Object> properties;
    }
}
