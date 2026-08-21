package com.ljl.agent.security;

import com.ljl.agent.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CurrentUserTest {

    private final CurrentUser currentUser = new CurrentUser();

    @Test
    void shouldReadPositiveUserIdFromJwtSubject() {
        assertEquals(21L, currentUser.requireUserId(authentication("21")));
    }

    @Test
    void shouldRejectMissingNonNumericAndNonPositiveSubjects() {
        assertInvalid(null);
        assertInvalid("user-21");
        assertInvalid("0");
    }

    private void assertInvalid(String subject) {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> currentUser.requireUserId(authentication(subject))
        );
        assertEquals(40102, exception.getCode());
    }

    private JwtAuthenticationToken authentication(String subject) {
        Jwt.Builder builder = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .issuedAt(Instant.now().minusSeconds(1))
                .expiresAt(Instant.now().plusSeconds(60));
        if (subject != null) {
            builder.subject(subject);
        }
        return new JwtAuthenticationToken(
                builder.build(),
                java.util.List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
