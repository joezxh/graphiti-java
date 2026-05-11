package com.graphiti.module.graphiti.vo.legal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * LLM 提取的律师 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedLawyerVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 律师姓名 */
    private String name;
    /** 执业证号 */
    private String licenseNumber;
    /** 所属律所 */
    private String firmName;
    /** 专业领域 */
    private String specialty;
    /** 联系方式 */
    private String contact;
    /** 唯一标识 */
    private String uuid;
}
