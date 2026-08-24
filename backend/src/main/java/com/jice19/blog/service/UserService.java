package com.jice19.blog.service;

import com.jice19.blog.common.PageResult;
import com.jice19.blog.dto.UserDTO;
import com.jice19.blog.vo.UserVO;

public interface UserService {

    PageResult<UserVO> pageUsers(long page, long size, String keyword);

    void createUser(UserDTO dto);

    void updateUser(Long id, UserDTO dto);

    void deleteUser(Long id);
}
