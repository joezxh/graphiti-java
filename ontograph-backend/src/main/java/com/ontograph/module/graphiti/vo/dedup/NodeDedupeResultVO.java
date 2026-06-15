package com.ontograph.module.graphiti.vo.dedup;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

/**
 * 节点去重结果 VO
 * 对应 Python dedupe_nodes.py 中的 NodeResolutions 模型
 */
@Data
@Schema(description = "节点去重结果")
public class NodeDedupeResultVO {

    @Schema(description = "实体解析列表")
    private List<NodeDuplicate> entityResolutions;

    @Data
    @Schema(description = "节点重复信息")
    public static class NodeDuplicate {

        @Schema(description = "实体 ID")
        private Integer id;

        @Schema(description = "实体名称（最完整和描述性的名称）")
        private String name;

        @Schema(description = "匹配的现有实体 candidate_id，-1 表示无匹配")
        private Integer duplicateCandidateId;
    }
}
