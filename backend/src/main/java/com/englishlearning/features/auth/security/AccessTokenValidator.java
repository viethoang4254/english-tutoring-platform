package com.englishlearning.features.auth.security;

import java.time.Clock;
import java.time.Duration;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.Jwt;

/** Required access-token claims beyond signature, issuer, audience and timestamp checks. */
final class AccessTokenValidator implements OAuth2TokenValidator<Jwt> {
    private final Clock clock;
    private final Duration tolerance;

    AccessTokenValidator(Clock clock, Duration tolerance) {
        if (tolerance.isNegative()) throw new IllegalArgumentException("Negative JWT clock tolerance");
        this.clock = clock;
        this.tolerance = tolerance;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        try {
            String subject = jwt.getSubject();
            if (subject == null || !subject.matches("[1-9][0-9]*") || Long.parseLong(subject) <= 0
                    || jwt.getIssuedAt() == null || jwt.getExpiresAt() == null
                    || !jwt.getExpiresAt().isAfter(jwt.getIssuedAt())
                    || jwt.getIssuedAt().isAfter(clock.instant().plus(tolerance))
                    || (jwt.getNotBefore() != null && !jwt.getExpiresAt().isAfter(jwt.getNotBefore()))) {
                return invalid();
            }
            return OAuth2TokenValidatorResult.success();
        } catch (IllegalArgumentException exception) {
            return invalid();
        }
    }

    private OAuth2TokenValidatorResult invalid() {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid access token.", null));
    }
}
