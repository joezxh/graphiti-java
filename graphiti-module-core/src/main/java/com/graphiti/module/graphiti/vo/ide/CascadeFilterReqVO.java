package com.graphiti.module.graphiti.vo.ide;

import lombok.Data;
import java.io.Serializable;
import java.util.List;

/**
 * 级联编辑筛选条件
 */
@Data
public class CascadeFilterReqVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 节点类型 */
    private String classType;

    /** 条件列表 */
    private List<PropertyCondition> conditions;

    /** 逻辑运算符 */
    private String logic = "AND";

    @Data
    public static class PropertyCondition implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private String propertyName;
        private String operator;
        private Object value;
    }
}
