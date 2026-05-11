package com.graphiti.module.graphiti.vo.prompt;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

/**
 * 生成范例数据响应 VO
 */
@Data
@Schema(description = "生成范例数据响应")
public class GenerateSampleDataRespVO {

    @Schema(description = "是否成功")
    private Boolean success;

    @Schema(description = "生成的样本数据列表")
    private List<SampleData> samples;

    @Schema(description = "错误信息")
    private String errorMessage;

    @Data
    @Schema(description = "样本数据项")
    public static class SampleData {
        @Schema(description = "样本索引")
        private Integer index;

        @Schema(description = "样本内容")
        private String content;

        @Schema(description = "样本类型")
        private String type;

        @Schema(description = "所属领域")
        private String domain;

        @Schema(description = "元数据")
        private String metadata;
    }
}
