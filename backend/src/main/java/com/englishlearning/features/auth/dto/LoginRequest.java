package com.englishlearning.features.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LoginRequest(@NotBlank @Email String email, @NotNull String password) {
    public LoginRequest { if (email != null) email = email.trim(); }
    @Override public String toString() { return "LoginRequest[REDACTED]"; }
}
