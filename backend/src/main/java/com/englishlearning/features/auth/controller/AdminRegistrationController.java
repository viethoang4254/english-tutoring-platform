package com.englishlearning.features.auth.controller;

import com.englishlearning.features.auth.security.PasswordInput;
import com.englishlearning.features.auth.security.StudentRegistrationInput;
import com.englishlearning.features.auth.service.RegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminRegistrationController {
    private final RegistrationService registration;

    public AdminRegistrationController(RegistrationService registration) {
        this.registration = registration;
    }

    public record Request(@NotBlank @Email String email, String fullName, String role,
                          String password, String confirmPassword, String username, String phone,
                          String specialization, String teachingExperience, String introduction) {
        @Override public String toString() { return "AdminRegistrationRequest[REDACTED]"; }
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StudentRegistrationController.RegistrationResponse> create(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.englishlearning.features.auth.security.AccountPrincipal actor,
            @Valid @RequestBody Request body, HttpServletRequest request) {
        if (request.getQueryString() != null || !PasswordInput.isValid(body.password())
                || !body.password().equals(body.confirmPassword())) throw invalid();
        String name = required(body.fullName());
        boolean teacher = "TEACHER".equals(body.role());
        if (teacher) {
            registration.registerTeacher(actor.id(), body.email(), body.password(), name, required(body.specialization()),
                    required(body.teachingExperience()), required(body.introduction()));
        } else if ("STUDENT".equals(body.role())) {
            String username, phone;
            try {
                username = StudentRegistrationInput.username(body.username());
                phone = StudentRegistrationInput.phone(body.phone());
            } catch (IllegalArgumentException e) { throw invalid(); }
            registration.register(actor.id(), body.email(), body.password(), name, username, phone);
        } else throw invalid();
        return ResponseEntity.status(201).cacheControl(CacheControl.noStore())
                .body(new StudentRegistrationController.RegistrationResponse(false));
    }

    private static String required(String value) {
        if (value == null || value.isBlank() || value.indexOf(0) >= 0) throw invalid();
        return value.strip();
    }
    private static ResponseStatusException invalid() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST);
    }
}
