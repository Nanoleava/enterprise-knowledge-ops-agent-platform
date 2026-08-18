package com.ljl.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.time.Duration;
import java.util.Base64;

/**
 * JWT 安全配置。构造时立即校验，避免使用非法密钥启动应用。
 */
@ConfigurationProperties(prefix = "app.security.jwt")
public final class SecurityProperties {

    private static final int MIN_SECRET_BYTES = 32;

    private final String issuer;
    private final byte[] secretBytes;
    private final Duration accessTokenTtl;

    public SecurityProperties(
            String issuer,
            String secretBase64,
            Duration accessTokenTtl
    ) {
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException("JWT issuer 不能为空");
        }
        String normalizedIssuer = issuer.trim();
        URI issuerUri;
        try {
            issuerUri = URI.create(normalizedIssuer);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "JWT issuer 必须是合法 URL",
                    exception
            );
        }
        if (!issuerUri.isAbsolute()
                || issuerUri.getHost() == null
                || (!("http".equalsIgnoreCase(issuerUri.getScheme()))
                && !("https".equalsIgnoreCase(issuerUri.getScheme())))) {
            throw new IllegalArgumentException(
                    "JWT issuer 必须是绝对 HTTP(S) URL"
            );
        }
        if (secretBase64 == null || secretBase64.isBlank()) {
            throw new IllegalArgumentException(
                    "缺少 JWT_SECRET_BASE64 环境变量"
            );
        }

        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(secretBase64.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "JWT_SECRET_BASE64 不是合法 Base64；请重新生成，"
                            + "或使用 scripts/run-dev.ps1 启动开发环境"
            );
        }
        if (decoded.length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException(
                    "JWT_SECRET_BASE64 解码后至少需要32字节；请重新生成，"
                            + "或使用 scripts/run-dev.ps1 启动开发环境"
            );
        }
        if (accessTokenTtl == null
                || accessTokenTtl.isZero()
                || accessTokenTtl.isNegative()) {
            throw new IllegalArgumentException(
                    "JWT access token TTL 必须大于0"
            );
        }

        this.issuer = normalizedIssuer;
        this.secretBytes = decoded.clone();
        this.accessTokenTtl = accessTokenTtl;
    }

    public String getIssuer() {
        return issuer;
    }

    public Duration getAccessTokenTtl() {
        return accessTokenTtl;
    }

    public SecretKey createSecretKey() {
        return new SecretKeySpec(secretBytes.clone(), "HmacSHA256");
    }
}
