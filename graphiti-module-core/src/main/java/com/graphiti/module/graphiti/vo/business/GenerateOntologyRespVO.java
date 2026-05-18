package com.graphiti.module.graphiti.vo.business;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 生成本体定义响应 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "生成本体定义响应")
public class GenerateOntologyRespVO {

    @Schema(description = "草稿ID（如果保存了草稿）")
    private Long draftId;

    @Schema(description = "本体定义")
    private OntologyDefinitionVO definition;

    @Schema(description = "生成的类列表")
    private List<OntologyClassVO> classes;

    @Schema(description = "生成的属性列表")
    private List<OntologyPropertyVO> properties;

    @Schema(description = "生成的关系列表")
    private List<OntologyRelationshipVO> relationships;

    @Schema(description = "生成状态")
    private String status;

    @Schema(description = "生成时间")
    private String generatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OntologyDefinitionVO {
        private String name;
        private String namespace;
        private String version;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OntologyClassVO {
        private String localName;
        private String classUri;
        private String parentClass;
        private String description;
        private String example;
        private String domainHint;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OntologyPropertyVO {
        private String localName;
        private String propertyUri;
        private String propertyType;
        private String domainClass;
        private String rangeClass;
        private String rangeDataType;
        private Boolean isRequired;
        private Boolean isMultiple;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OntologyRelationshipVO {
        private String sourceClass;
        private String targetClass;
        private String relationshipType;
        private String description;
    }
}
