package com.ontograph.module.graphiti.vo.ontology;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 完整本体信息 VO
 * 包含类、属性、约束等全部本体元素
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "完整本体信息")
public class OntologyFullVO {

    @Schema(description = "本体定义")
    private OntDefinitionVO definition;

    @Schema(description = "所有类定义（平铺）")
    private List<OntClassVO> classes;

    @Schema(description = "类层次树")
    private List<ClassHierarchyVO> classHierarchy;

    @Schema(description = "所有属性定义")
    private List<OntPropertyVO> properties;

    @Schema(description = "所有约束定义")
    private List<OntConstraintVO> constraints;
}
