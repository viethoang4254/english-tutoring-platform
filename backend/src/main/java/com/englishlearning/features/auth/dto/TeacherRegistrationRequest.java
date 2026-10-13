package com.englishlearning.features.auth.dto;

import jakarta.validation.constraints.*;

public record TeacherRegistrationRequest(@NotBlank @Email String email, @NotNull String password,
        @NotBlank String fullName, @NotBlank String specialization,
        @NotBlank String teachingExperience, @NotBlank String introduction) {
    public TeacherRegistrationRequest { if (email != null) email = email.trim(); }
    @Override public String toString() { return "TeacherRegistrationRequest[REDACTED]"; }
}
