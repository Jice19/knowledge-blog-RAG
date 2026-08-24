package com.jice19.blog.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户出参（不含密码）
 */
@Data
public class UserVO {

    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String role;
    private LocalDateTime createTime;
}
