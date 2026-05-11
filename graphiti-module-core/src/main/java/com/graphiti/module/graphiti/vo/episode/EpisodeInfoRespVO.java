package com.graphiti.module.graphiti.vo.episode;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 事件详情响应 VO
 */
@Data
@Schema(description = "事件（Episode）详情")
public class EpisodeInfoRespVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "事件 UUID")
    private String uuid;

    @Schema(description = "事件名称")
    private String name;

    @Schema(description = "所属图谱 ID")
    private String groupId;

    @Schema(description = "来源类型")
    private String source;

    @Schema(description = "来源描述")
    private String sourceDescription;

    @Schema(description = "事件内容")
    private String content;

    @Schema(description = "创建时间")
    private String createdAt;

    @Schema(description = "有效时间")
    private String validAt;

    @Schema(description = "失效时间")
    private String invalidAt;

    @Schema(description = "关联实体 UUID 列表")
    private List<String> entityEdges;

    @Schema(description = "元数据")
    private Map<String, Object> metadata;

    @Schema(description = "是否已处理")
    private Boolean processed;
}
