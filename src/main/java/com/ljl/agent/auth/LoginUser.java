package com.ljl.agent.auth;

import com.ljl.agent.dto.response.UserVO;
import com.ljl.agent.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * 数据库 User 在 Spring Security 认证链中的可信身份。
 */
public final class LoginUser implements UserDetails {

    private final Long userId;
    private final String username;
    private final String passwordHash;
    private final String email;
    private final String role;
    private final Integer status;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final List<GrantedAuthority> authorities;

    public LoginUser(User user) {
        this.userId = user.getId();
        this.username = user.getUsername();
        this.passwordHash = user.getPasswordHash();
        this.email = user.getEmail();
        this.role = user.getRole();
        this.status = user.getStatus();
        this.createdAt = user.getCreatedAt();
        this.updatedAt = user.getUpdatedAt();
        this.authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + role)
        );
    }

    public Long getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isEnabled() {
        return Integer.valueOf(User.STATUS_ENABLED).equals(status);
    }

    public UserVO toUserVO() {
        UserVO user = new UserVO();
        user.setId(userId);
        user.setUsername(username);
        user.setEmail(email);
        user.setRole(role);
        user.setStatus(status);
        user.setCreatedAt(createdAt);
        user.setUpdatedAt(updatedAt);
        return user;
    }
}
