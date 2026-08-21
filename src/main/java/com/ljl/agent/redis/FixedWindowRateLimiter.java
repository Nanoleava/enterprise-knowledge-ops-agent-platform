package com.ljl.agent.redis;

import com.ljl.agent.common.ErrorCode;
import com.ljl.agent.config.RateLimitProperties;
import com.ljl.agent.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.List;

/**
 * Redis Lua 固定窗口限流；Redis 故障时按可用性优先策略临时放行。
 */
@Service
public class FixedWindowRateLimiter {

    public static final String KEY_PREFIX = "rate:http:";

    private static final Logger LOGGER =
            LoggerFactory.getLogger(FixedWindowRateLimiter.class);

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> script;
    private final RateLimitProperties properties;
    private final Clock clock;

    public FixedWindowRateLimiter(
            StringRedisTemplate redisTemplate,
            DefaultRedisScript<Long> fixedWindowRateLimitScript,
            RateLimitProperties properties,
            Clock clock
    ) {
        this.redisTemplate = redisTemplate;
        this.script = fixedWindowRateLimitScript;
        this.properties = properties;
        this.clock = clock;
    }

    public void check(Long currentUserId) {
        if (currentUserId == null || currentUserId <= 0) {
            throw new BusinessException(
                    ErrorCode.AUTHENTICATION_REQUIRED,
                    "当前用户ID无效"
            );
        }

        String key = currentKey(currentUserId);
        long ttlSeconds = properties.getWindow().toSeconds()
                + properties.getKeyTtlBuffer().toSeconds();
        Long count;
        try {
            count = redisTemplate.execute(
                    script,
                    List.of(key),
                    Long.toString(ttlSeconds)
            );
        } catch (DataAccessException exception) {
            LOGGER.error(
                    "rate limit Redis unavailable; request allowed: userId={}, exceptionType={}",
                    currentUserId,
                    exception.getClass().getSimpleName()
            );
            return;
        }

        if (count == null) {
            LOGGER.error(
                    "rate limit Redis returned no count; request allowed: userId={}",
                    currentUserId
            );
            return;
        }
        if (count > properties.getLimit()) {
            LOGGER.warn(
                    "rate limit exceeded: userId={}, count={}, limit={}",
                    currentUserId,
                    count,
                    properties.getLimit()
            );
            throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED);
        }
    }

    public String currentKey(Long currentUserId) {
        long windowSeconds = properties.getWindow().toSeconds();
        long now = clock.instant().getEpochSecond();
        long windowStart = (now / windowSeconds) * windowSeconds;
        return KEY_PREFIX + currentUserId + ":" + windowStart;
    }
}
