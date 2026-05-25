package com.graphiti.module.graphiti.vo.imports;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.io.Serializable;

/**
 * 批量数据项 VO
 */
@Data
@Schema(description = "批量数据项")
public class BatchDataItemVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "数据内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "数据内容不能为空")
    private String content;

    @Schema(description = "来源类型")
    private String sourceType = "text";

    @Schema(description = "来源描述")
    private String sourceDescription;

    @Schema(description = "episode 名称")
    private String name;
}
