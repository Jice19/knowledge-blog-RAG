package com.jice19.blog.common;

import lombok.Getter;

/**
 * 统一状态码
 */
@Getter
public enum ResultCode {

    SUCCESS(0, "success"),
    PARAM_ERROR(40000, "参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    LOGIN_FAILED(40001, "用户名或密码错误"),
    SERVER_ERROR(500, "服务器内部错误");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
