package com.ontograph.module.graphiti.vo.ontology;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphIntegrityResultVO {
    private boolean passed;
    private String checkType;    // ISOLATED_NODE / REQUIRED_RELATION / DOMAIN_RANGE
    private int violationCount;
    private List<ViolationVO> violations;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ViolationVO {
        private String nodeUuid;
        private String nodeName;
        private String nodeType;
        private String violationType;
        private String description;
        private Map<String, Object> context;
    }
}
