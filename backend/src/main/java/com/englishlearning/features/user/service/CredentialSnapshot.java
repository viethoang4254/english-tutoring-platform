package com.englishlearning.features.user.service;

/** Internal credential projection, never an API response. */
public record CredentialSnapshot(Long id, String email, String passwordHash,
        boolean locked, boolean emailVerified, com.englishlearning.features.user.entity.User.Role role, boolean studentLoginWithoutVerification) {
    public boolean loginEligible() {
        return new AccountSnapshot(id, role, locked, emailVerified, studentLoginWithoutVerification).loginEligible();
    }
    @Override public String toString() { return "CredentialSnapshot[REDACTED]"; }
}
