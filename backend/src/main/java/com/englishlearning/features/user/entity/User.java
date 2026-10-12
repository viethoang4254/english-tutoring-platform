package com.englishlearning.features.user.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

// This slice reads account state only; credential/profile fields are intentionally not loaded.
@Entity
@Table(name = "users", schema = "public")
@Immutable
public class User {
    public enum Role { STUDENT, TEACHER, ADMIN }

    @Id private Long id;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false) private Role role;
    @Column(nullable = false) private boolean locked;
    @Column(name = "email_verified", nullable = false) private boolean emailVerified;

    protected User() {}

    public Long getId() { return id; }
    public Role getRole() { return role; }
    public boolean isLocked() { return locked; }
    public boolean isEmailVerified() { return emailVerified; }
}
