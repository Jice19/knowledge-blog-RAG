package com.jice19.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jice19.blog.common.PageResult;
import com.jice19.blog.common.ResultCode;
import com.jice19.blog.common.exception.BusinessException;
import com.jice19.blog.dto.UserDTO;
import com.jice19.blog.entity.User;
import com.jice19.blog.mapper.UserMapper;
import com.jice19.blog.security.LoginUser;
import com.jice19.blog.security.UserContext;
import com.jice19.blog.service.UserService;
import com.jice19.blog.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public PageResult<UserVO> pageUsers(long page, long size, String keyword) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or()
                    .like(User::getNickname, keyword));
        }
        wrapper.orderByDesc(User::getId);

        Page<User> p = userMapper.selectPage(new Page<>(page, size), wrapper);
        List<UserVO> records = p.getRecords().stream().map(this::toVO).toList();
        return new PageResult<>(p.getTotal(), p.getCurrent(), p.getSize(), records);
    }

    @Override
    public void createUser(UserDTO dto) {
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "密码不能为空");
        }
        Long exists = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "用户名已存在");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setRole(dto.getRole() == null || dto.getRole().isBlank() ? "USER" : dto.getRole());
        userMapper.insert(user);
    }

    @Override
    public void updateUser(Long id, UserDTO dto) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "用户不存在");
        }
        user.setNickname(dto.getNickname());
        if (dto.getRole() != null && !dto.getRole().isBlank()) {
            user.setRole(dto.getRole());
        }
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(dto.getPassword()));
        }
        userMapper.updateById(user);
    }

    @Override
    public void deleteUser(Long id) {
        LoginUser current = UserContext.get();
        if (current != null && current.userId() != null && current.userId().equals(id)) {
            throw new BusinessException(ResultCode.PARAM_ERROR.getCode(), "不能删除当前登录用户");
        }
        userMapper.deleteById(id);
    }

    private UserVO toVO(User u) {
        UserVO vo = new UserVO();
        vo.setId(u.getId());
        vo.setUsername(u.getUsername());
        vo.setNickname(u.getNickname());
        vo.setAvatar(u.getAvatar());
        vo.setRole(u.getRole());
        vo.setCreateTime(u.getCreateTime());
        return vo;
    }
}
