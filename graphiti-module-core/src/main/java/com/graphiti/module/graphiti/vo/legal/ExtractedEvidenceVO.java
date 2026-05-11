package com.graphiti.module.graphiti.vo.legal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * LLM 提取的证据 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedEvidenceVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 证据编号 */
    private String evidenceNumber;
    /** 证据类型 */
    private String evidenceType;
    /** 证据内容摘要 */
    private String content;
    /** 提交方 */
    private String submittedBy;
    /** 提交日期 */
    private String submissionDate;
    /** 证明目的 */
    private String purpose;
    /** 唯一标识 */
    private String uuid;
}
