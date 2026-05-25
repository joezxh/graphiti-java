package com.ontograph.module.graphiti.vo.ide;

import lombok.Data;
import lombok.Builder;
import java.io.Serializable;
import java.util.List;

/**
 * 级联编辑预览响应 VO
 */
@Data
public class CascadePreviewRespVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 匹配总数 */
    private Long totalMatch;

    /** 分布统计 */
    private List<DistributionItem> distribution;

    @Data
    @Builder
    public static class DistributionItem implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private String groupBy;
        private String value;
        private Long count;
    }
}
