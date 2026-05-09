package com.graphiti.module.graphiti.vo.search;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 搜索请求 VO（混合检索：向量 + 全文）
 */
@Data
@Schema(description = "搜索请求")
public class SearchQueryReqVO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "搜索查询文本", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "查询不能为空")
    private String query;

    @Schema(description = "限定的 group_id 列表（图谱 ID）")
    private List<String> groupIds;

    @Schema(description = "最大返回事实数", example = "10")
    private Integer maxFacts = 10;

    @Schema(description = "是否启用重排序")
    private Boolean enableRerank = true;
}
