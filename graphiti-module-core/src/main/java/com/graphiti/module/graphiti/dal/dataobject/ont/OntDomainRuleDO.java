package com.graphiti.module.graphiti.dal.dataobject.ont;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ont_domain_rule")
public class OntDomainRuleDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long definitionId;
    private String ruleName;
    private String ruleCode;
    private String spelExpression;
    private String applicableClassIds; // JSON 数组字符串
    private String severity;
    private String errorMessage;
    private String description;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
