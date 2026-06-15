package com.ontograph.module.graphiti.vo.metadata;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "剧集类型删除检查响应")
public class EpisodeTypeDeleteCheckVO {
    @Schema(description = "是否可以删除") private Boolean canDelete;
    @Schema(description = "不可删除原因") private String reason;
    @Schema(description = "子类型数量") private Long childCount;
    @Schema(description = "实例引用数量") private Long instanceCount;
}
