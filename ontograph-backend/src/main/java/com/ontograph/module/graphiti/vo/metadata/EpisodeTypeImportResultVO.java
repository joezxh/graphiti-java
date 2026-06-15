package com.ontograph.module.graphiti.vo.metadata;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
@Schema(description = "剧集类型批量导入结果")
public class EpisodeTypeImportResultVO {
    @Schema(description = "总数") private Integer total;
    @Schema(description = "成功数") private Integer success;
    @Schema(description = "失败数") private Integer failed;
    @Schema(description = "错误信息列表") private List<String> errors;
}
