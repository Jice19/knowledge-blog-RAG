package com.jice19.blog.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录出参
 */
@Data
@AllArgsConstructor
public class LoginVO {

    private String token;
    private Long userId;
    private String username;
    private String nickname;
}
