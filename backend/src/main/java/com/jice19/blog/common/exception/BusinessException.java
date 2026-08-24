package com.jice19.blog.common.exception;

import com.jice19.blog.common.ResultCode;
import lombok.Getter;

/**
 * 业务异常：由全局异常处理器统一转成 Result 返回
 */
@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(ResultCode rc) {
        super(rc.getMessage());
        this.code = rc.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
