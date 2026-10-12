package com.englishlearning.features.user.service;

/** Internal credential projection, never an API response. */
public record CredentialSnapshot(Long id, String email, String passwordHash,
        boolean locked, boolean emailVerified) {
    @Override public String toString() { return "CredentialSnapshot[REDACTED]"; }
}
