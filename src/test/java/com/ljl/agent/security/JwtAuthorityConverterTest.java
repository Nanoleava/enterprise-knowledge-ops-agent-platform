package com.ljl.agent.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtAuthorityConverterTest {

    private final JwtAuthorityConverter converter =
            new JwtAuthorityConverter();

    @Test
    void shouldMapOnlySupportedRolesWithoutDoublePrefix() {
        assertEquals(
                "ROLE_USER",
                converter.convert(jwt("USER")).iterator().next().getAuthority()
        );
        assertEquals(
                "ROLE_ADMIN",
                converter.convert(jwt("ADMIN")).iterator().next().getAuthority()
        );
        assertTrue(converter.convert(jwt("ROLE_ADMIN")).isEmpty());
        assertTrue(converter.convert(jwt("SUPER_ADMIN")).isEmpty());
    }

    private Jwt jwt(String role) {
        Instant now = Instant.now();
        return Jwt.withTokenValue("test-only")
                .header("alg", "HS256")
                .subject("1")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(60))
                .claim("role", role)
                .build();
    }
}
