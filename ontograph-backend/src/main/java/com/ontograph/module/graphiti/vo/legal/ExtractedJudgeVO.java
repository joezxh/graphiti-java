package com.ontograph.module.graphiti.vo.legal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * LLM 提取的法官 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedJudgeVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 法官姓名 */
    private String name;
    /** 职务 */
    private String title;
    /** 所属法院 */
    private String courtName;
    /** 专业领域 */
    private String specialty;
    /** 唯一标识 */
    private String uuid;
}
