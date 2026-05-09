package com.graphiti.module.graphiti.vo.search;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 获取记忆响应 VO（基于对话历史重建上下文）
 */
@Data
@Schema(description = "获取记忆响应")
public class GetMemoryRespVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "相关事实列表")
    private List<FactResultVO> facts;

    @Schema(description = "相关实体列表")
    private List<NodeResultVO> entities;

    @Schema(description = "重建的上下文字符串")
    private String context;
}
