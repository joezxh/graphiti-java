package com.ontograph.module.graphiti.vo.legal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * LLM 提取的法院 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractedCourtVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 法院名称 */
    private String name;
    /** 法院级别 */
    private String level;
    /** 所在地 */
    private String location;
    /** 管辖范围 */
    private String jurisdiction;
    /** 上级法院 */
    private String parentCourt;
    /** 唯一标识 */
    private String uuid;
}
