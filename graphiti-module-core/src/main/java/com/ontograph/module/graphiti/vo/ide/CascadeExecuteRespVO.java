package com.graphiti.module.graphiti.vo.ide;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 级联编辑执行响应 VO
 */
@Data
public class CascadeExecuteRespVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 是否成功 */
    private Boolean success;

    /** 影响的节点数 */
    private Long affectedCount;

    /** 失败数量 */
    private Long failedCount;

    /** 错误列表 */
    private List<String> errors;
}
