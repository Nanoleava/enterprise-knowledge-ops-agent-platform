package com.ljl.agent.security;

import com.ljl.agent.util.PasswordUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 把阶段 2 的 PBKDF2 存储格式适配到 Spring Security。
 */
@Component
public class ProjectPasswordEncoder implements PasswordEncoder {

    @Override
    public String encode(CharSequence rawPassword) {
        return PasswordUtils.hash(
                rawPassword == null ? null : rawPassword.toString()
        );
    }

    @Override
    public boolean matches(
            CharSequence rawPassword,
            String encodedPassword
    ) {
        return PasswordUtils.matches(
                rawPassword == null ? null : rawPassword.toString(),
                encodedPassword
        );
    }
}
