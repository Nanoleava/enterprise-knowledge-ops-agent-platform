package com.ljl.agent.redis;

/**
 * Redis 无法完成 JWT 撤销状态判断。
 */
public class BlacklistUnavailableException extends RuntimeException {

    public BlacklistUnavailableException(Throwable cause) {
        super("JWT blacklist Redis unavailable", cause);
    }
}
