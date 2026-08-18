package com.ljl.agent.auth;

import com.ljl.agent.config.SecurityProperties;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/**
 * 只负责根据可信认证身份签发最小化 access token。
 */
@Service
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;
    private final SecurityProperties properties;
    private final Clock clock;

    public JwtTokenService(
            JwtEncoder jwtEncoder,
            SecurityProperties properties,
            Clock clock
    ) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    public IssuedToken issue(LoginUser user) {
        Instant issuedAt = clock.instant();
        Instant expiresAt = issuedAt.plus(properties.getAccessTokenTtl());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.getIssuer())
                .subject(user.getUserId().toString())
                .id(UUID.randomUUID().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("role", user.getRole())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256)
                .type("JWT")
                .build();
        String token = jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();

        return new IssuedToken(
                token,
                properties.getAccessTokenTtl().toSeconds()
        );
    }

    public record IssuedToken(String value, long expiresIn) {
    }
}
