package com.ljl.agent.security;

import com.ljl.agent.auth.LoginUser;
import com.ljl.agent.entity.User;
import com.ljl.agent.mapper.UserMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 从项目 user 表加载 Spring Security 用户身份。
 */
@Service
public class ProjectUserDetailsService implements UserDetailsService {

    private final UserMapper userMapper;

    public ProjectUserDetailsService(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {
        User user = userMapper.selectByUsername(username);
        if (user == null || !isSupportedRole(user.getRole())) {
            throw new UsernameNotFoundException("用户名或密码错误");
        }
        return new LoginUser(user);
    }

    private boolean isSupportedRole(String role) {
        return User.ROLE_USER.equals(role) || User.ROLE_ADMIN.equals(role);
    }
}
