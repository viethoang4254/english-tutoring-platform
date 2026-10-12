package com.englishlearning.features.auth.security;

import com.englishlearning.features.user.entity.User.Role;
import java.security.Principal;

public record AccountPrincipal(Long id, Role role, boolean emailVerified) implements Principal {
    @Override public String getName() { return id.toString(); }
}
