package com.graphiti.module.graphiti.dal.dataobject.ont;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.graphiti.module.graphiti.typehandler.PgJsonbTypeHandler;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("ont_domain_rule")
public class OntDomainRuleDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long definitionId;
    private String ruleName;
    private String ruleCode;
    private String spelExpression;
    @TableField(typeHandler = PgJsonbTypeHandler.class)
    private List<Long> applicableClassIds; // 存储为 PostgreSQL JSONB
    private String severity;
    private String errorMessage;
    private String description;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
