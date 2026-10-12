package com.englishlearning.features.auth.security;

import java.time.Instant;

public record AccessToken(String value, Instant issuedAt, Instant expiresAt) {
    @Override public String toString() { return "AccessToken[REDACTED]"; }
}
