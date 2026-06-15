package com.ontograph.common.response;

import com.ontograph.common.constants.ResultCode;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 统一响应结果类
 * @param <T> 数据类型
 */
@Data
public class CommonResult<T> implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /** 错误码 */
    private int code;
    
    /** 返回消息 */
    private String message;
    
    /** 返回数据 */
    private T data;
    
    /** 时间戳 */
    private String timestamp;
    
    public CommonResult() {
        this.timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
    
    /**
     * 成功响应
     * @param data 数据
     * @return CommonResult<T>
     */
    public static <T> CommonResult<T> success(T data) {
        CommonResult<T> result = new CommonResult<>();
        result.setCode(ResultCode.SUCCESS);
        result.setMessage("success");
        result.setData(data);
        return result;
    }
    
    /**
     * 成功响应（无数据）
     * @return CommonResult<T>
     */
    public static <T> CommonResult<T> success() {
        return success(null);
    }
    
    /**
     * 错误响应
     * @param code 错误码
     * @param message 错误信息
     * @return CommonResult<T>
     */
    public static <T> CommonResult<T> error(int code, String message) {
        CommonResult<T> result = new CommonResult<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}
