package com.graphiti.module.graphiti.vo.ide;

import lombok.Data;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 级联编辑执行请求 VO
 */
@Data
public class CascadeExecuteReqVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 节点类型 */
    private String classType;

    /** 条件列表 */
    private List<CascadeFilterReqVO.PropertyCondition> conditions;

    /** 逻辑运算符 */
    private String logic = "AND";

    /** 要更新的属性 */
    private Map<String, Object> updates;
}
