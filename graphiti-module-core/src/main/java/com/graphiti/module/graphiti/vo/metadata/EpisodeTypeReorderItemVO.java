package com.graphiti.module.graphiti.vo.metadata;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "剧集类型排序项")
public class EpisodeTypeReorderItemVO {
    @Schema(description = "类型ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "类型ID不能为空")
    private Long id;

    @Schema(description = "排序值", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "排序值不能为空")
    private Integer sortOrder;

    @Schema(description = "父类型编码") private String parentTypeCode;
}
