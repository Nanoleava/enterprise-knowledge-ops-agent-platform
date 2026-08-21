package com.ljl.agent.redis;

import com.ljl.agent.config.RateLimitProperties;
import com.ljl.agent.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class FixedWindowRateLimiterTest {

    private StringRedisTemplate redisTemplate;
    private DefaultRedisScript<Long> script;
    private FixedWindowRateLimiter rateLimiter;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        script = mock(DefaultRedisScript.class);
        RateLimitProperties properties = new RateLimitProperties(
                2,
                Duration.ofMinutes(1),
                Duration.ofSeconds(5)
        );
        rateLimiter = new FixedWindowRateLimiter(
                redisTemplate,
                script,
                properties,
                Clock.fixed(
                        Instant.ofEpochSecond(1_800_000_061L),
                        ZoneOffset.UTC
                )
        );
    }

    @Test
    void shouldUsePrincipalUserWindowKeyAndRejectLimitPlusOne() {
        String key = "rate:http:7:1800000060";
        when(redisTemplate.execute(
                eq(script),
                eq(List.of(key)),
                eq("65")
        )).thenReturn(1L, 2L, 3L);

        rateLimiter.check(7L);
        rateLimiter.check(7L);
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> rateLimiter.check(7L)
        );

        assertEquals(42901, exception.getCode());
        assertEquals(key, rateLimiter.currentKey(7L));
    }

    @Test
    void shouldFailOpenAndLogWhenRateLimitRedisIsUnavailable(
            CapturedOutput output
    ) {
        String key = "rate:http:7:1800000060";
        when(redisTemplate.execute(
                eq(script),
                eq(List.of(key)),
                eq("65")
        )).thenThrow(new RedisConnectionFailureException("down"));

        rateLimiter.check(7L);

        assertTrue(output.getAll().contains(
                "rate limit Redis unavailable; request allowed: userId=7"
        ));
        assertTrue(!output.getAll().contains("accessToken"));
    }
}
