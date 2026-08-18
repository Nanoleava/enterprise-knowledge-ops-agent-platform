package com.ljl.agent;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * 为需要完整 Spring 上下文的测试提供运行时随机 JWT 测试密钥。
 */
abstract class AbstractIntegrationTest {

    private static final String TEST_JWT_SECRET = randomSecret();

    @DynamicPropertySource
    static void jwtTestProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "app.security.jwt.secret-base64",
                () -> TEST_JWT_SECRET
        );
    }

    private static String randomSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
