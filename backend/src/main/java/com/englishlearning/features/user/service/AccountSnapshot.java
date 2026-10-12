package com.englishlearning.features.user.service;

import com.englishlearning.features.user.entity.User.Role;
import java.util.Objects;

public record AccountSnapshot(Long id, Role role, boolean locked, boolean emailVerified) {
    public AccountSnapshot {
        Objects.requireNonNull(id);
        Objects.requireNonNull(role);
    }
}
