package com.graphiti.module.graphiti.vo.imports;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import com.graphiti.module.graphiti.vo.imports.BatchDataItemVO;
/**
 * 批量添加数据请求 VO
 */
@Data
@Schema(description = "批量添加数据请求")
public class AddDataBatchReqVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "目标图谱 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "图谱 ID 不能为空")
    private String graphId;

    @Schema(description = "数据列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "数据列表不能为空")
    private List<BatchDataItemVO> items;

    @Schema(description = "参考时间（批次整体的时间基准）")
    private LocalDateTime referenceTime;

    @Schema(description = "是否更新社区")
    private Boolean updateCommunities = false;
}
