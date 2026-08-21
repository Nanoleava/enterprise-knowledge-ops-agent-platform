package com.ljl.agent.security;

import com.ljl.agent.redis.BlacklistUnavailableException;
import com.ljl.agent.redis.TokenBlacklistService;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 在 Nimbus 完成签名、issuer 和时间校验后，统一检查 Redis jti 黑名单。
 */
public class BlacklistAwareJwtDecoder implements JwtDecoder {

    private static final Logger log = LoggerFactory.getLogger(BlacklistAwareJwtDecoder.class);

    private final JwtDecoder delegate;
    private final TokenBlacklistService blacklistService;

    public BlacklistAwareJwtDecoder(
            JwtDecoder delegate,
            TokenBlacklistService blacklistService
    ) {
        this.delegate = delegate;
        this.blacklistService = blacklistService;
    }

    @Override
    public Jwt decode(String token) {
        Jwt jwt = delegate.decode(token);
        String jti = jwt.getId();
        if (jti == null || jti.isBlank()) {
            throw new BadJwtException("JWT jti 缺失");
        }
        try {
            if (blacklistService.isBlacklisted(jti)) {
                throw new BadJwtException("JWT 已退出");
            }
        } catch (BlacklistUnavailableException exception) {
            log.error(
                    "JWT blacklist dependency unavailable: exceptionType={}",
                    exception.getClass().getSimpleName()
            );
            throw new JwtBlacklistDependencyException(exception);
        }
        return jwt;
    }
}
