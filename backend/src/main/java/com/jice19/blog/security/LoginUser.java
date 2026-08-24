package com.jice19.blog.security;

/**
 * 当前登录用户信息（存于 JWT / ThreadLocal）
 */
public record LoginUser(Long userId, String username, String role) {
}
