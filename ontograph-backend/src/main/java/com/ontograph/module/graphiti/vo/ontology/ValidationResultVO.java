package com.ontograph.module.graphiti.vo.ontology;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResultVO {
    private boolean passed;
    private int level;
    @Builder.Default
    private List<ValidationErrorVO> errors = new ArrayList<>();
    @Builder.Default
    private List<ValidationWarningVO> warnings = new ArrayList<>();
    private Map<String, Object> enrichedProperties;

    public static ValidationResultVO pass() {
        return ValidationResultVO.builder().passed(true).level(0).build();
    }

    public static ValidationResultVO passWithWarnings(List<ValidationWarningVO> warnings) {
        return ValidationResultVO.builder().passed(true).level(0).warnings(warnings).build();
    }

    public static ValidationResultVO fail(int level, List<ValidationErrorVO> errors) {
        return ValidationResultVO.builder().passed(false).level(level).errors(errors).build();
    }
}
