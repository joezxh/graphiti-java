package com.ontograph.module.graphiti.vo.ontology;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomainRuleVO {
    private Long id;
    private Long definitionId;
    private String ruleName;
    private String ruleCode;
    private String spelExpression;
    private List<Long> applicableClassIds;
    private String severity;
    private String errorMessage;
    private String description;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
