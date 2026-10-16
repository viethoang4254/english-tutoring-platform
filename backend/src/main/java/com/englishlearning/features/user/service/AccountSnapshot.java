package com.englishlearning.features.user.service;

import com.englishlearning.features.user.entity.User.Role;
import java.util.Objects;

public record AccountSnapshot(Long id, Role role, boolean locked, boolean emailVerified, boolean studentLoginWithoutVerification) {
    public boolean loginEligible() {
        return !locked && (emailVerified || role == Role.STUDENT && studentLoginWithoutVerification);
    }
    public AccountSnapshot {
        Objects.requireNonNull(id);
        Objects.requireNonNull(role);
    }
}
