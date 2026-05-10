package com.graphiti.module.graphiti.vo.search;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.io.Serializable;

/**
 * 搜索配置 VO
 * 控制混合检索的行为参数
 */
@Data
@Schema(description = "搜索配置")
public class SearchConfigVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "搜索模式：bm25 | vector | hybrid | bfs", example = "hybrid")
    private String mode = "hybrid";

    @Schema(description = "BM25 结果权重", example = "0.3")
    private Double bm25Weight = 0.3;

    @Schema(description = "向量搜索权重", example = "0.7")
    private Double vectorWeight = 0.7;

    @Schema(description = "RRF 融合参数 k", example = "60")
    private Integer rrfK = 60;

    @Schema(description = "MMR 重排序 lambda（相关性 vs 多样性平衡）", example = "0.5")
    private Double mmrLambda = 0.5;

    @Schema(description = "是否启用 MMR 重排序", example = "true")
    private Boolean enableMmr = true;

    @Schema(description = "BFS 遍历深度", example = "2")
    private Integer bfsDepth = 2;

    @Schema(description = "BFS 每节点最大邻居数", example = "5")
    private Integer bfsMaxNeighbors = 5;
}
