package com.jice19.blog.controller;

import com.jice19.blog.common.Result;
import com.jice19.blog.dto.LoginDTO;
import com.jice19.blog.security.LoginUser;
import com.jice19.blog.security.UserContext;
import com.jice19.blog.service.AuthService;
import com.jice19.blog.vo.LoginVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 登录：返回 JWT */
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Valid LoginDTO dto) {
        return Result.success(authService.login(dto));
    }

    /** 获取当前登录用户 */
    @GetMapping("/me")
    public Result<LoginUser> me() {
        return Result.success(UserContext.get());
    }

    /** 退出登录：token 拉黑 */
    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader("Authorization") String auth) {
        String token = auth.startsWith("Bearer ") ? auth.substring(7) : auth;
        authService.logout(token);
        return Result.success();
    }
}
