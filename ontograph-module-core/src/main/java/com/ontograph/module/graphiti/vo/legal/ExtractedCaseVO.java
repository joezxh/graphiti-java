package com.ontograph.module.graphiti.vo.legal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * LLM 提取的案件 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedCaseVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 案件名称 */
    private String caseName;
    /** 案件编号 */
    private String caseNumber;
    /** 案件类型: 民事/刑事/行政/商事 */
    private String caseType;
    /** 案件状态 */
    private String caseStatus;
    /** 立案日期 */
    private String filingDate;
    /** 结案日期 */
    private String closedDate;
    /** 争议金额 */
    private Double amountInDispute;
    /** 案件摘要 */
    private String summary;
    /** 完整描述（LLM 直接生成的描述） */
    private String description;
    /** 案件唯一标识 */
    private String uuid;
}
