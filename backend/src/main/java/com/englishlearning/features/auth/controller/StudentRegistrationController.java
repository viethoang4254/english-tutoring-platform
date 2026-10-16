package com.englishlearning.features.auth.controller;

import com.englishlearning.features.auth.dto.StudentRegistrationRequest;
import com.englishlearning.features.auth.security.PasswordInput;
import com.englishlearning.features.auth.security.StudentRegistrationInput;
import com.englishlearning.features.auth.service.RegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth")
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class StudentRegistrationController {
    private final RegistrationService service;
    public StudentRegistrationController(RegistrationService service) { this.service = service; }
    @PostMapping(value = "/register/student", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RegistrationResponse> register(@Valid @RequestBody StudentRegistrationRequest body,
            HttpServletRequest request) {
        noQuery(request);
        if (!PasswordInput.isValid(body.password()) || body.fullName().indexOf(0) >= 0) throw invalid();
        final String username, phone;
        try {
            username = StudentRegistrationInput.username(body.username());
            phone = StudentRegistrationInput.phone(body.phone());
        } catch (IllegalArgumentException invalidInput) { throw invalid(); }
        service.register(body.email(), body.password(), body.fullName(), username, phone);
        return ResponseEntity.status(201).cacheControl(CacheControl.noStore()).body(new RegistrationResponse(false));
    }
    @PostMapping(value = "/email-verification", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> verify(@Valid @RequestBody VerificationRequest body, HttpServletRequest request) {
        noQuery(request); service.verify(body.token()); return ResponseEntity.noContent().build();
    }
    @PostMapping(value = "/email-verification/resend", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> resend(@Valid @RequestBody ResendRequest body, HttpServletRequest request) {
        noQuery(request); service.resend(body.email()); return ResponseEntity.accepted().build();
    }
    private static void noQuery(HttpServletRequest request) {
        if (request.getQueryString() != null || !request.getParameterMap().isEmpty()) throw invalid();
    }
    private static ResponseStatusException invalid() { return new ResponseStatusException(HttpStatus.BAD_REQUEST); }
    public record RegistrationResponse(boolean verificationRequired) {}
    public record VerificationRequest(@NotBlank String token) {
        @Override public String toString() { return "VerificationRequest[REDACTED]"; }
    }
    public record ResendRequest(@NotBlank @Email String email) {
        public ResendRequest { if (email != null) email = email.trim(); }
        @Override public String toString() { return "ResendRequest[REDACTED]"; }
    }
}
