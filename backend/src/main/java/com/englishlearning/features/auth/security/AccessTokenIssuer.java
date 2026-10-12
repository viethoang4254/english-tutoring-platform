package com.englishlearning.features.auth.security;

import java.time.Clock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;

public final class AccessTokenIssuer {
    private final JwtEncoder encoder;
    private final String issuer;
    private final String audience;
    private final Duration lifetime;
    private final Clock clock;

    AccessTokenIssuer(JwtEncoder encoder, String issuer, String audience, Duration lifetime, Clock clock) {
        if (issuer == null || issuer.isBlank() || audience == null || audience.isBlank()
                || lifetime == null || lifetime.isNegative() || lifetime.isZero() || lifetime.getNano() != 0) {
            throw new IllegalArgumentException("Invalid access-token configuration.");
        }
        this.encoder = encoder;
        this.issuer = issuer;
        this.audience = audience;
        this.lifetime = lifetime;
        this.clock = clock;
    }

    // Only trusted authentication workflows may call this after their full eligibility checks.
    public AccessToken issue(Long userId) {
        if (userId == null || userId <= 0) throw new IllegalArgumentException("Invalid User identity.");
        try {
            var issuedAt = clock.instant().truncatedTo(ChronoUnit.SECONDS);
            var expiresAt = issuedAt.plus(lifetime);
            var claims = JwtClaimsSet.builder().subject(userId.toString()).issuer(issuer)
                    .audience(List.of(audience)).issuedAt(issuedAt).expiresAt(expiresAt).build();
            var headers = JwsHeader.with(SignatureAlgorithm.RS256).type("JWT").build();
            var signed = encoder.encode(JwtEncoderParameters.from(headers, claims));
            return new AccessToken(signed.getTokenValue(), issuedAt, expiresAt);
        } catch (RuntimeException failure) {
            throw new IllegalStateException("Access token issuance failed.");
        }
    }
}
