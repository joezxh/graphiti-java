package com.ontograph.module.graphiti.vo.prompt;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 生成范例数据请求 VO
 */
@Data
@Schema(description = "生成范例数据请求")
public class GenerateSampleDataReqVO {

    @Schema(description = "模板ID")
    @NotBlank(message = "模板ID不能为空")
    private String templateId;

    @Schema(description = "数据类型：legal-法律, medical-医疗, financial-金融, general-通用")
    @NotBlank(message = "数据类型不能为空")
    private String dataType;

    @Schema(description = "生成数量")
    private Integer count = 3;

    @Schema(description = "具体场景描述")
    private String scenario;

    @Schema(description = "格式要求：json-标准JSON, xml-XML格式")
    private String format = "json";

    @Schema(description = "额外说明")
    private String additionalInstructions;
}
