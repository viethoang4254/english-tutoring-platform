package com.englishlearning.features.auth.dto;

import jakarta.validation.constraints.*;

public record StudentRegistrationRequest(@NotBlank @Email String email, @NotNull String password,
        @NotBlank String fullName, @NotBlank String username, @NotBlank String phone) {
    public StudentRegistrationRequest { if (email != null) email = email.trim(); }
    @Override public String toString() { return "StudentRegistrationRequest[REDACTED]"; }
}
