package com.ljl.agent.security;

import org.springframework.security.oauth2.jwt.BadJwtException;

/**
 * JWT 验证因 Redis 黑名单依赖不可用而无法完成。
 */
public class JwtBlacklistDependencyException extends BadJwtException {

    public JwtBlacklistDependencyException(Throwable cause) {
        super("JWT blacklist Redis unavailable", cause);
    }
}
