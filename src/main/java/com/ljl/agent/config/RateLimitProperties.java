package com.ljl.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 聊天消息代表性写接口的固定窗口限流配置。
 */
@ConfigurationProperties(prefix = "app.rate-limit.chat-message")
public final class RateLimitProperties {

    private final int limit;
    private final Duration window;
    private final Duration keyTtlBuffer;

    public RateLimitProperties(
            int limit,
            Duration window,
            Duration keyTtlBuffer
    ) {
        if (limit <= 0) {
            throw new IllegalArgumentException("限流阈值必须大于0");
        }
        if (window == null || window.toSeconds() <= 0) {
            throw new IllegalArgumentException("限流窗口必须至少为1秒");
        }
        if (keyTtlBuffer == null || keyTtlBuffer.isNegative()) {
            throw new IllegalArgumentException("限流 key TTL 缓冲不能为负数");
        }
        this.limit = limit;
        this.window = window;
        this.keyTtlBuffer = keyTtlBuffer;
    }

    public int getLimit() {
        return limit;
    }

    public Duration getWindow() {
        return window;
    }

    public Duration getKeyTtlBuffer() {
        return keyTtlBuffer;
    }
}
