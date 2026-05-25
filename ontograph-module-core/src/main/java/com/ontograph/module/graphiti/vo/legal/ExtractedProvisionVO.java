package com.ontograph.module.graphiti.vo.legal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * LLM 提取的法律条文 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedProvisionVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 条文编号 */
    private String provisionId;
    /** 条款序号 */
    private String articleNumber;
    /** 条文内容 */
    private String content;
    /** 所属法律名称 */
    private String lawName;
    /** 法律类型 */
    private String lawType;
    /** 生效日期 */
    private String effectiveDate;
    /** 关键词 */
    private String keywords;
    /** 唯一标识 */
    private String uuid;
}
