package com.ljl.agent;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigurationSafetyTest {

    @Test
    void shouldRequireExplicitDevProfileAndEnvironmentPassword()
            throws IOException {
        String common = resource("application.yml");
        String dev = resource("application-dev.yml");

        assertFalse(common.contains("active: dev"));
        assertTrue(dev.contains("password: ${DB_PASSWORD}"));
        assertTrue(common.contains("secret-base64: ${JWT_SECRET_BASE64}"));
        assertTrue(common.contains("password: ${REDIS_PASSWORD:}"));
        assertTrue(common.contains("connect-timeout: ${REDIS_CONNECT_TIMEOUT:2s}"));
        assertFalse(common.contains("JWT_SECRET_BASE64:"));
        assertTrue(common.contains(
                "storage-root: ${DOCUMENT_STORAGE_ROOT:./storage/uploads}"
        ));
        assertTrue(common.contains(
                "allowed-types: ${DOCUMENT_ALLOWED_TYPES:TXT,MARKDOWN}"
        ));
    }

    @Test
    void shouldKeepJwtSecretOutOfTrackedConfiguration() throws IOException {
        String common = resource("application.yml");
        String dev = resource("application-dev.yml");

        assertTrue(common.contains("issuer: ${JWT_ISSUER:"));
        assertTrue(common.contains("access-token-ttl: ${JWT_ACCESS_TOKEN_TTL:"));
        assertFalse(common.contains("secret-base64: ey"));
        assertFalse(dev.contains("secret-base64:"));
    }

    @Test
    void shouldNotEnableSensitiveMyBatisParameterLoggingByDefault()
            throws IOException {
        String dev = resource("application-dev.yml");

        assertTrue(dev.contains("com.ljl.agent.mapper: info"));
        assertFalse(dev.contains("com.ljl.agent.mapper: debug"));
    }

    private String resource(String name) throws IOException {
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream(name)) {
            if (input == null) {
                throw new IllegalStateException("缺少配置资源：" + name);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
