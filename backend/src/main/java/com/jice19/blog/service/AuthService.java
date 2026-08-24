package com.jice19.blog.service;

import com.jice19.blog.dto.LoginDTO;
import com.jice19.blog.vo.LoginVO;

public interface AuthService {

    LoginVO login(LoginDTO dto);

    void logout(String token);
}
