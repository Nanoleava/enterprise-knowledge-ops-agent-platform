package com.ljl.agent.config;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityPropertiesTest {

    @Test
    void shouldRejectMissingInvalidAndShortSecrets() {
        assertThrows(IllegalArgumentException.class, () -> properties(null));
        IllegalArgumentException invalidBase64 = assertThrows(
                IllegalArgumentException.class,
                () -> properties("bad$secret")
        );
        assertEquals(
                "JWT_SECRET_BASE64 不是合法 Base64；请重新生成，"
                        + "或使用 scripts/run-dev.ps1 启动开发环境",
                invalidBase64.getMessage()
        );
        assertTrue(invalidBase64.getCause() == null);

        IllegalArgumentException shortSecret = assertThrows(
                IllegalArgumentException.class,
                () -> properties(Base64.getEncoder().encodeToString(new byte[16]))
        );
        assertTrue(shortSecret.getMessage().contains("至少需要32字节"));
    }

    @Test
    void shouldRejectInvalidIssuerAndTtl() {
        String secret = randomSecret();
        assertThrows(
                IllegalArgumentException.class,
                () -> new SecurityProperties(" ", secret, Duration.ofMinutes(30))
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new SecurityProperties(
                        "ljl-agent-backend",
                        secret,
                        Duration.ofMinutes(30)
                )
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> new SecurityProperties("issuer", secret, Duration.ZERO)
        );
    }

    @Test
    void shouldCreateHs256KeyFromValidConfiguration() {
        SecurityProperties properties = properties(randomSecret());

        assertEquals("HmacSHA256", properties.createSecretKey().getAlgorithm());
        assertEquals(1800, properties.getAccessTokenTtl().toSeconds());
    }

    private SecurityProperties properties(String secret) {
        return new SecurityProperties(
                "https://ljl-agent-backend.local",
                secret,
                Duration.ofMinutes(30)
        );
    }

    private String randomSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
