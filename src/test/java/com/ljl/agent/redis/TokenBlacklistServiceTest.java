package com.ljl.agent.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TokenBlacklistServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-19T06:00:00Z");

    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private TokenBlacklistService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        service = new TokenBlacklistService(
                redisTemplate,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void shouldStoreOnlyJtiWithRemainingTokenLifetime() {
        service.revoke(jwt("jti-123", NOW.plusSeconds(300)));

        verify(valueOperations).set(
                "auth:blacklist:jti-123",
                "1",
                Duration.ofSeconds(300)
        );
    }

    @Test
    void shouldNotCreateBlacklistKeyForExpiredToken() {
        service.revoke(jwt("expired", NOW.minusSeconds(1)));

        verify(valueOperations, never()).set(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(Duration.class)
        );
    }

    @Test
    void shouldCheckBlacklistAndFailClosedWhenRedisIsUnavailable() {
        when(redisTemplate.hasKey("auth:blacklist:active"))
                .thenReturn(true);
        assertTrue(service.isBlacklisted("active"));

        when(redisTemplate.hasKey("auth:blacklist:other"))
                .thenReturn(false);
        assertFalse(service.isBlacklisted("other"));

        when(redisTemplate.hasKey("auth:blacklist:down"))
                .thenThrow(new RedisConnectionFailureException("down"));
        assertThrows(
                BlacklistUnavailableException.class,
                () -> service.isBlacklisted("down")
        );
    }

    private Jwt jwt(String jti, Instant expiresAt) {
        return Jwt.withTokenValue("not-stored")
                .header("alg", "HS256")
                .subject("7")
                .claim("jti", jti)
                .issuedAt(NOW.minusSeconds(10))
                .expiresAt(expiresAt)
                .build();
    }
}
