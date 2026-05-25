package com.graphiti.module.graphiti.vo.episode;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;

/**
 * Episode 过滤请求 VO
 */
@Data
@Schema(description = "Episode 过滤请求")
public class EpisodeFilterReqVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "跳过数量（分页）")
    private Long skip = 0L;

    @Schema(description = "限制数量（分页）")
    private Long limit = 20L;

    @Schema(description = "创建时间下限（毫秒时间戳）")
    private Long createdAfter;

    @Schema(description = "创建时间上限（毫秒时间戳）")
    private Long createdBefore;
}
