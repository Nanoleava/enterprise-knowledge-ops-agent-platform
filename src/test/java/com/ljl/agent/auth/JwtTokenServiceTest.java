package com.ljl.agent.auth;

import com.ljl.agent.config.SecurityProperties;
import com.ljl.agent.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JwtTokenServiceTest {

    @Test
    void shouldIssueMinimalClaimsWithConfiguredTtlAndUniqueJti() {
        byte[] secretBytes = new byte[32];
        new SecureRandom().nextBytes(secretBytes);
        SecurityProperties properties = new SecurityProperties(
                "https://ljl-agent-backend.local",
                Base64.getEncoder().encodeToString(secretBytes),
                Duration.ofMinutes(30)
        );
        SecretKey key = properties.createSecretKey();
        NimbusJwtEncoder encoder = NimbusJwtEncoder.withSecretKey(key)
                .algorithm(MacAlgorithm.HS256)
                .build();
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(
                JwtValidators.createDefaultWithIssuer(
                        "https://ljl-agent-backend.local"
                )
        );
        Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        JwtTokenService service = new JwtTokenService(
                encoder,
                properties,
                Clock.fixed(now, ZoneOffset.UTC)
        );
        LoginUser user = new LoginUser(user());

        JwtTokenService.IssuedToken first = service.issue(user);
        JwtTokenService.IssuedToken second = service.issue(user);
        Jwt jwt = ((JwtDecoder) decoder).decode(first.value());

        assertEquals("21", jwt.getSubject());
        assertEquals("USER", jwt.getClaimAsString("role"));
        assertEquals(
                "https://ljl-agent-backend.local",
                jwt.getIssuer().toString()
        );
        assertEquals(now, jwt.getIssuedAt());
        assertEquals(now.plusSeconds(1800), jwt.getExpiresAt());
        assertNotNull(jwt.getId());
        assertNotEquals(
                jwt.getId(),
                decoder.decode(second.value()).getId()
        );
        assertEquals(1800, first.expiresIn());
        assertFalse(jwt.hasClaim("password"));
        assertFalse(jwt.hasClaim("passwordHash"));
        assertFalse(jwt.hasClaim("secret"));
        assertEquals(6, jwt.getClaims().size());
    }

    private User user() {
        User user = new User();
        user.setId(21L);
        user.setUsername("login_user");
        user.setPasswordHash("not-exposed");
        user.setRole(User.ROLE_USER);
        user.setStatus(User.STATUS_ENABLED);
        return user;
    }
}
