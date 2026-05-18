package com.graphiti.module.graphiti.vo.ide;

import lombok.Data;
import java.io.Serializable;
import java.util.Map;

/**
 * 类实例数据响应 VO
 */
@Data
public class ClassInstanceRespVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String uuid;
    private String name;
    private String type;
    private Map<String, Object> properties;
    private String summary;
    private String createdAt;
    private String updatedAt;
}
