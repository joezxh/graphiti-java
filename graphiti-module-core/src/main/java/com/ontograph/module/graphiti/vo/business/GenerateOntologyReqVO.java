package com.graphiti.module.graphiti.vo.business;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 生成本体定义请求 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "生成本体定义请求")
public class GenerateOntologyReqVO {

    @Schema(description = "草稿名称")
    private String draftName;

    @Schema(description = "业务场景描述")
    private String businessScenario;

    @Schema(description = "领域类型: FINANCIAL | MEDICAL | ECOMMERCE | LEGAL | KNOWLEDGE | GENERAL | GOVERNANCE")
    private String domainHint;

    @Schema(description = "用户原始输入")
    private String userInput;

    @Schema(description = "命名空间（可选）")
    private String namespace;

    @Schema(description = "版本号（可选）")
    private String version;

    @Schema(description = "是否保存为草稿")
    @Builder.Default
    private boolean saveAsDraft = false;
}
