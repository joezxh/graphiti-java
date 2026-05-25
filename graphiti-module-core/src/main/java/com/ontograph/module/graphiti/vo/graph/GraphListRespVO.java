package com.graphiti.module.graphiti.vo.graph;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 图谱列表响应 VO（含分页信息，对齐 Python API）
 */
@Data
@Schema(description = "图谱列表响应")
public class GraphListRespVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "图谱列表")
    private List<GraphInfoVO> graphs;

    @Schema(description = "总数量")
    private long totalCount;

    @Schema(description = "本页数量")
    private int rowCount;
}
