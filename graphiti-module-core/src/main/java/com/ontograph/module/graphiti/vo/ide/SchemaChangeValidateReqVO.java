package com.graphiti.module.graphiti.vo.ide;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * Schema 变更验证请求
 */
@Data
@Schema(description = "Schema 变更验证请求")
public class SchemaChangeValidateReqVO {

    @Schema(description = "变更类型: UPDATE_CLASS, DELETE_CLASS, UPDATE_PROPERTY, DELETE_PROPERTY, ADD_REQUIRED_PROPERTY")
    private String type;

    @Schema(description = "类ID")
    private Long classId;

    @Schema(description = "属性ID")
    private Long propertyId;

    @Schema(description = "变更详情")
    private ChangeDetail changes;

    @Data
    @Schema(description = "变更详情")
    public static class ChangeDetail {
        // 类变更
        @Schema(description = "新类名称")
        private String newLocalName;
        
        // 属性变更
        @Schema(description = "是否必填 (变更前)")
        private Boolean oldIsRequired;
        
        @Schema(description = "是否必填 (变更后)")
        private Boolean newIsRequired;
        
        @Schema(description = "新数据类型")
        private String newRangeDataType;
        
        @Schema(description = "新枚举值")
        private List<String> newAllowedValues;
        
        @Schema(description = "新正则表达式")
        private String newPattern;
        
        @Schema(description = "新最小值")
        private Double newMinValue;
        
        @Schema(description = "新最大值")
        private Double newMaxValue;
    }
}
