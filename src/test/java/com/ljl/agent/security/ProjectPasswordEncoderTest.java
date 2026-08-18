package com.ljl.agent.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectPasswordEncoderTest {

    private final ProjectPasswordEncoder encoder =
            new ProjectPasswordEncoder();

    @Test
    void shouldReuseProjectPbkdf2Format() {
        String encoded = encoder.encode("Test@123456");

        assertTrue(encoded.startsWith("pbkdf2_sha256$"));
        assertTrue(encoder.matches("Test@123456", encoded));
        assertFalse(encoder.matches("wrong-password", encoded));
        assertFalse(encoder.matches(encoded, encoded));
    }
}
