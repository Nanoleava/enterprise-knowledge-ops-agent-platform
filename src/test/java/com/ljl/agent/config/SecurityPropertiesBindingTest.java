package com.ljl.agent.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import java.security.SecureRandom;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityPropertiesBindingTest {

    @Test
    void shouldFailContextEarlyWhenSecretIsMissingOrInvalid() {
        runner().run(context -> assertNotNull(context.getStartupFailure()));
        runner()
                .withPropertyValues("app.security.jwt.secret-base64=bad$secret")
                .run(context -> {
                    Throwable failure = context.getStartupFailure();
                    assertNotNull(failure);
                    assertTrue(
                            containsMessage(
                                    failure,
                                    "JWT_SECRET_BASE64 不是合法 Base64"
                            )
                    );
                });
    }

    @Test
    void shouldBindValidRuntimeConfiguration() {
        runner()
                .withPropertyValues(
                        "app.security.jwt.secret-base64=" + randomSecret()
                )
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    assertNotNull(context.getBean(SecurityProperties.class));
                });
    }

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner()
                .withUserConfiguration(PropertiesConfiguration.class)
                .withPropertyValues(
                        "app.security.jwt.issuer=https://ljl-agent-backend.local",
                        "app.security.jwt.access-token-ttl=30m"
                );
    }

    private String randomSecret() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private boolean containsMessage(Throwable throwable, String expected) {
        Throwable current = throwable;
        while (current != null) {
            if (current.getMessage() != null
                    && current.getMessage().contains(expected)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(SecurityProperties.class)
    static class PropertiesConfiguration {
    }
}
