package com.ontograph.module.graphiti.vo.legal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * LLM 完整提取法律知识图谱的响应 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LegalExtractResultVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 提取的案件列表
     */
    private List<ExtractedCaseVO> cases;

    /**
     * 提取的当事人列表
     */
    private List<ExtractedPartyVO> parties;

    /**
     * 提取的法院列表
     */
    private List<ExtractedCourtVO> courts;

    /**
     * 提取的法官列表
     */
    private List<ExtractedJudgeVO> judges;

    /**
     * 提取的法律条文列表
     */
    private List<ExtractedProvisionVO> provisions;

    /**
     * 提取的律师列表
     */
    private List<ExtractedLawyerVO> lawyers;

    /**
     * 提取的证据列表
     */
    private List<ExtractedEvidenceVO> evidences;

    /**
     * 提取的裁判文书列表
     */
    private List<ExtractedJudgmentVO> judgments;

    /**
     * 提取失败的信息
     */
    private List<String> errors;

    /**
     * 原始文件名称
     */
    private String sourceFileName;

    /**
     * 总节点数
     */
    private int totalNodes;

    /**
     * 总边数
     */
    private int totalEdges;
}
