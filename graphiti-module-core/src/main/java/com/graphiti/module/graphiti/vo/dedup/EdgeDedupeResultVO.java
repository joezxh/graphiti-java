package com.graphiti.module.graphiti.vo.dedup;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

/**
 * 边去重结果 VO
 * 对应 Python dedupe_edges.py 中的 EdgeDuplicate 模型
 */
@Data
@Schema(description = "边去重结果")
public class EdgeDedupeResultVO {

    @Schema(description = "重复事实的索引列表（仅来自 EXISTING FACTS 范围）")
    private List<Integer> duplicateFacts;

    @Schema(description = "矛盾事实的索引列表（来自完整索引范围）")
    private List<Integer> contradictedFacts;
}
