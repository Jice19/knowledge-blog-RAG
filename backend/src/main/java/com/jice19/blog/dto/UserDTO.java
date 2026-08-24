package com.jice19.blog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户新增/修改入参
 */
@Data
public class UserDTO {

    @NotBlank(message = "用户名不能为空")
    private String username;

    /** 新增时必填；修改时留空表示不修改密码 */
    private String password;

    private String nickname;

    /** ADMIN / USER，留空默认 USER */
    private String role;
}
