package com.graphiti.module.graphiti.vo.search;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;

/**
 * 事实结果 VO（对应边）
 */
@Data
@Schema(description = "事实结果（边）")
public class FactResultVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "边 UUID")
    private String uuid;

    @Schema(description = "边名称")
    private String name;

    @Schema(description = "事实描述")
    private String fact;

    @Schema(description = "源节点 UUID")
    private String sourceNodeUuid;

    @Schema(description = "目标节点 UUID")
    private String targetNodeUuid;

    @Schema(description = "所属图谱 ID")
    private String groupId;

    @Schema(description = "创建时间")
    private String createdAt;

    @Schema(description = "有效时间")
    private String validAt;

    @Schema(description = "失效时间")
    private String invalidAt;

    @Schema(description = "搜索得分")
    private Double score;

    @Schema(description = "相关性得分（0-1）")
    private Double relevance;
}
