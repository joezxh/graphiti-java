package com.graphiti.module.graphiti.vo.legal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * LLM 提取的裁判文书 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedJudgmentVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 文书编号 */
    private String documentNumber;
    /** 文书类型 */
    private String documentType;
    /** 作出日期 */
    private String issueDate;
    /** 主要内容摘要 */
    private String mainContent;
    /** 判决结果 */
    private String judgmentResult;
    /** 法律依据 */
    private String legalBasis;
    /** 唯一标识 */
    private String uuid;
}
