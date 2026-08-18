package com.ljl.agent.security;

import com.ljl.agent.auth.LoginUser;
import com.ljl.agent.entity.User;
import com.ljl.agent.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProjectUserDetailsServiceTest {

    private UserMapper userMapper;
    private ProjectUserDetailsService service;

    @BeforeEach
    void setUp() {
        userMapper = mock(UserMapper.class);
        service = new ProjectUserDetailsService(userMapper);
    }

    @Test
    void shouldLoadEnabledUserAndAdminAuthorities() {
        when(userMapper.selectByUsername("user"))
                .thenReturn(user("user", User.ROLE_USER, true));
        when(userMapper.selectByUsername("admin"))
                .thenReturn(user("admin", User.ROLE_ADMIN, true));

        LoginUser normal = (LoginUser) service.loadUserByUsername("user");
        LoginUser admin = (LoginUser) service.loadUserByUsername("admin");

        assertTrue(normal.isEnabled());
        assertEquals(
                "ROLE_USER",
                normal.getAuthorities().iterator().next().getAuthority()
        );
        assertEquals(
                "ROLE_ADMIN",
                admin.getAuthorities().iterator().next().getAuthority()
        );
    }

    @Test
    void shouldExposeDisabledStatusToAuthenticationProvider() {
        when(userMapper.selectByUsername("disabled"))
                .thenReturn(user("disabled", User.ROLE_USER, false));

        LoginUser disabled =
                (LoginUser) service.loadUserByUsername("disabled");

        assertFalse(disabled.isEnabled());
    }

    @Test
    void shouldRejectMissingAndUnsupportedRoleUsers() {
        assertThrows(
                UsernameNotFoundException.class,
                () -> service.loadUserByUsername("missing")
        );
        when(userMapper.selectByUsername("invalid"))
                .thenReturn(user("invalid", "ROLE_ADMIN", true));
        assertThrows(
                UsernameNotFoundException.class,
                () -> service.loadUserByUsername("invalid")
        );
    }

    private User user(String username, String role, boolean enabled) {
        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setPasswordHash("test-only-hash");
        user.setRole(role);
        user.setStatus(enabled ? User.STATUS_ENABLED : User.STATUS_DISABLED);
        return user;
    }
}
