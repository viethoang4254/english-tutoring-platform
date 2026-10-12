package com.englishlearning.features.auth.dto;

import java.time.Instant;

public record AccessTokenResponse(String accessToken, String tokenType, Instant issuedAt, Instant expiresAt) {
    @Override public String toString() { return "AccessTokenResponse[REDACTED]"; }
}
