package com.ljl.agent;

import com.ljl.agent.config.RateLimitProperties;
import com.ljl.agent.config.RedisScriptConfig;
import com.ljl.agent.exception.BusinessException;
import com.ljl.agent.redis.FixedWindowRateLimiter;
import com.ljl.agent.redis.TokenBlacklistService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfEnvironmentVariable(
        named = "REDIS_INTEGRATION_TEST",
        matches = "(?i)true",
        disabledReason = "未启用 REDIS_INTEGRATION_TEST，跳过真实 Redis 集成测试"
)
class RedisIntegrationTest {

    private LettuceConnectionFactory connectionFactory;
    private StringRedisTemplate redisTemplate;

    @BeforeAll
    void connect() {
        RedisStandaloneConfiguration configuration =
                new RedisStandaloneConfiguration(
                        environment("REDIS_HOST", "localhost"),
                        Integer.parseInt(environment("REDIS_PORT", "6379"))
                );
        configuration.setDatabase(Integer.parseInt(
                environment("REDIS_DATABASE", "0")
        ));
        String password = System.getenv("REDIS_PASSWORD");
        if (password != null && !password.isBlank()) {
            configuration.setPassword(RedisPassword.of(password));
        }

        connectionFactory = new LettuceConnectionFactory(configuration);
        connectionFactory.afterPropertiesSet();
        connectionFactory.start();
        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();
        assertEquals("PONG", redisTemplate.getConnectionFactory()
                .getConnection()
                .ping());
    }

    @AfterAll
    void disconnect() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @Test
    void shouldWriteBlacklistValueWithPositiveRemainingTtl() {
        Instant now = Instant.now();
        String jti = "integration-" + UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("not-stored-in-redis")
                .header("alg", "HS256")
                .subject("7")
                .claim("jti", jti)
                .issuedAt(now.minusSeconds(5))
                .expiresAt(now.plusSeconds(120))
                .build();
        TokenBlacklistService service = new TokenBlacklistService(
                redisTemplate,
                Clock.systemUTC()
        );
        String key = service.key(jti);
        try {
            service.revoke(jwt);

            assertEquals("1", redisTemplate.opsForValue().get(key));
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            assertTrue(ttl != null && ttl > 0 && ttl <= 120);
            assertTrue(service.isBlacklisted(jti));
        } finally {
            redisTemplate.delete(key);
        }
    }

    @Test
    void shouldExecuteLuaAtomicallyAndNeverCreateTtlMinusOneKey() {
        Instant now = Instant.now();
        Clock clock = Clock.fixed(now, ZoneOffset.UTC);
        RateLimitProperties properties = new RateLimitProperties(
                2,
                Duration.ofMinutes(1),
                Duration.ofSeconds(5)
        );
        FixedWindowRateLimiter rateLimiter = new FixedWindowRateLimiter(
                redisTemplate,
                new RedisScriptConfig().fixedWindowRateLimitScript(),
                properties,
                clock
        );
        long randomUserId = UUID.randomUUID().getMostSignificantBits()
                & Long.MAX_VALUE;
        final Long userId = randomUserId == 0 ? 1L : randomUserId;
        String key = rateLimiter.currentKey(userId);
        try {
            rateLimiter.check(userId);
            rateLimiter.check(userId);
            BusinessException exception = assertThrows(
                    BusinessException.class,
                    () -> rateLimiter.check(userId)
            );

            assertEquals(42901, exception.getCode());
            assertEquals("3", redisTemplate.opsForValue().get(key));
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            assertTrue(ttl != null && ttl > 0 && ttl <= 65);
            assertTrue(ttl != -1);
        } finally {
            redisTemplate.delete(key);
        }
    }

    private String environment(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
