package com.ljl.agent.redis;

import com.ljl.agent.common.ErrorCode;
import com.ljl.agent.exception.BusinessException;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

/**
 * 只保存 JWT jti 及其剩余有效期，不保存完整 token。
 */
@Service
public class TokenBlacklistService {

    public static final String KEY_PREFIX = "auth:blacklist:";

    private final StringRedisTemplate redisTemplate;
    private final Clock clock;

    public TokenBlacklistService(
            StringRedisTemplate redisTemplate,
            Clock clock
    ) {
        this.redisTemplate = redisTemplate;
        this.clock = clock;
    }

    public void revoke(Jwt jwt) {
        String jti = requireJti(jwt);
        Instant expiresAt = jwt.getExpiresAt();
        if (expiresAt == null) {
            throw invalidToken("JWT exp 缺失");
        }

        Duration remaining = Duration.between(clock.instant(), expiresAt);
        if (remaining.isZero() || remaining.isNegative()) {
            return;
        }

        try {
            redisTemplate.opsForValue().set(
                    key(jti),
                    "1",
                    remaining
            );
        } catch (DataAccessException exception) {
            throw new BlacklistUnavailableException(exception);
        }
    }

    public boolean isBlacklisted(String jti) {
        if (jti == null || jti.isBlank()) {
            throw invalidToken("JWT jti 缺失");
        }
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(key(jti)));
        } catch (DataAccessException exception) {
            throw new BlacklistUnavailableException(exception);
        }
    }

    public String key(String jti) {
        return KEY_PREFIX + jti;
    }

    private String requireJti(Jwt jwt) {
        if (jwt == null || jwt.getId() == null || jwt.getId().isBlank()) {
            throw invalidToken("JWT jti 缺失");
        }
        return jwt.getId();
    }

    private BusinessException invalidToken(String message) {
        return new BusinessException(
                ErrorCode.AUTHENTICATION_REQUIRED,
                message
        );
    }
}
