package com.graphiti.module.graphiti.vo.legal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * LLM 提取的当事人 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedPartyVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 当事人名称 */
    private String name;
    /** 当事人类型: 自然人/法人/非法人组织 */
    private String partyType;
    /** 身份证号/统一社会信用代码 */
    private String idNumber;
    /** 诉讼角色: 原告/被告/第三人等 */
    private String role;
    /** 住所地 */
    private String address;
    /** 联系方式 */
    private String contact;
    /** 是否企业 */
    private Boolean isEnterprise;
    /** 唯一标识 */
    private String uuid;
}
