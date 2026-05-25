package com.graphiti.module.graphiti.vo.imports;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 添加单条数据请求 VO（自动提取实体和关系）
 */
@Data
@Schema(description = "添加数据请求")
public class AddDataReqVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "目标图谱 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "图谱 ID 不能为空")
    private String graphId;

    @Schema(description = "数据内容", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "数据内容不能为空")
    private String content;

    @Schema(description = "来源类型，如 text/message/json")
    private String sourceType = "text";

    @Schema(description = "来源描述")
    private String sourceDescription;

    @Schema(description = "episode 名称（留空则自动生成）")
    private String name;

    @Schema(description = "参考时间（影响图谱时序）")
    private LocalDateTime referenceTime;

    @Schema(description = "是否更新社区")
    private Boolean updateCommunities = false;
}
