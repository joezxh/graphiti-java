package com.graphiti.module.graphiti.vo.episode;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 事件列表响应 VO
 */
@Data
@Schema(description = "事件列表响应")
public class EpisodeListRespVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "事件列表")
    private List<EpisodeInfoRespVO> episodes;

    @Schema(description = "总数")
    private long totalCount;

    @Schema(description = "本页数量")
    private int rowCount;
}
