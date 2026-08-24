package com.jice19.blog.controller;

import com.jice19.blog.common.PageResult;
import com.jice19.blog.common.Result;
import com.jice19.blog.dto.UserDTO;
import com.jice19.blog.service.UserService;
import com.jice19.blog.vo.UserVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户管理（管理员）
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 分页查询用户 */
    @GetMapping
    public Result<PageResult<UserVO>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword) {
        return Result.success(userService.pageUsers(page, size, keyword));
    }

    /** 新增用户 */
    @PostMapping
    public Result<Void> create(@RequestBody @Valid UserDTO dto) {
        userService.createUser(dto);
        return Result.success();
    }

    /** 修改用户 */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid UserDTO dto) {
        userService.updateUser(id, dto);
        return Result.success();
    }

    /** 删除用户 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success();
    }
}
