package com.graphiti.module.graphiti.vo.ontology;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class ConsistencyResultVO {
    private boolean consistent;
    private List<String> inconsistencies;
    private List<String> satisfiableClasses;
    private List<String> unsatisfiableClasses;
}
