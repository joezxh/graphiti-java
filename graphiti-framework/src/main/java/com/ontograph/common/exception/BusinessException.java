package com.ontograph.common.exception;

import com.ontograph.common.constants.ResultCode;
import lombok.Getter;

/**
 * 业务异常类
 * 用于抛出业务逻辑异常，配合 GlobalExceptionHandler 统一处理
 */
@Getter
public class BusinessException extends RuntimeException {
    /** 错误码 */
    private final int code;
    
    /**
     * 构造器
     * @param code 错误码
     * @param message 错误信息
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
    
    /**
     * 构造器（默认 code=500）
     * @param message 错误信息
     */
    public BusinessException(String message) {
        this(ResultCode.INTERNAL_SERVER_ERROR, message);
    }
}
