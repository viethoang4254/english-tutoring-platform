package com.englishlearning.features.auth.security;

import com.englishlearning.features.user.entity.User.Role;
import java.security.Principal;

public record AccountPrincipal(Long id, Role role, boolean emailVerified, boolean teacherApproved) implements Principal {
    public AccountPrincipal(Long id, Role role, boolean emailVerified) { this(id, role, emailVerified, false); }
    @Override public String getName() { return id.toString(); }
}
