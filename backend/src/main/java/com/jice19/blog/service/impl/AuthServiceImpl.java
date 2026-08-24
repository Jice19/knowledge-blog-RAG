package com.jice19.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jice19.blog.common.ResultCode;
import com.jice19.blog.common.exception.BusinessException;
import com.jice19.blog.dto.LoginDTO;
import com.jice19.blog.entity.User;
import com.jice19.blog.mapper.UserMapper;
import com.jice19.blog.security.JwtUtil;
import com.jice19.blog.service.AuthService;
import com.jice19.blog.vo.LoginVO;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;

    @Override
    public LoginVO login(LoginDTO dto) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.LOGIN_FAILED);
        }
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        return new LoginVO(token, user.getId(), user.getUsername(), user.getNickname(), user.getRole());
    }

    @Override
    public void logout(String token) {
        Claims claims = jwtUtil.parseToken(token);
        long remain = claims.getExpiration().getTime() - System.currentTimeMillis();
        if (remain > 0) {
            redisTemplate.opsForValue().set(BLACKLIST_PREFIX + token, "1", remain, TimeUnit.MILLISECONDS);
        }
    }
}
