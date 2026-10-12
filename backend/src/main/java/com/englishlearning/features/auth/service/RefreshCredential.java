package com.englishlearning.features.auth.service;

import java.time.Instant;

// Internal result only: possession is not a statement of account/login eligibility.
public record RefreshCredential(Long sessionId, Long userId, String credential, Instant expiresAt) {
    @Override public String toString() { return "RefreshCredential[REDACTED]"; }
}
