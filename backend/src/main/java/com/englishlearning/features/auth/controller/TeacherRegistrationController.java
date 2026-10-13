package com.englishlearning.features.auth.controller;

import com.englishlearning.features.auth.dto.TeacherRegistrationRequest;
import com.englishlearning.features.auth.security.PasswordInput;
import com.englishlearning.features.auth.service.RegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class TeacherRegistrationController {
    private final RegistrationService service;
    public TeacherRegistrationController(RegistrationService service) { this.service = service; }
    @PostMapping(value = "/api/v1/auth/register/teacher", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StudentRegistrationController.RegistrationResponse> register(
            @Valid @RequestBody TeacherRegistrationRequest body, HttpServletRequest request) {
        if (request.getQueryString() != null || !request.getParameterMap().isEmpty()
                || !PasswordInput.isValid(body.password())) throw invalid();
        for (String text : new String[]{body.fullName(), body.specialization(), body.teachingExperience(), body.introduction()}) {
            if (text.indexOf(0) >= 0) throw invalid();
        }
        service.registerTeacher(body.email(), body.password(), body.fullName(), body.specialization(),
                body.teachingExperience(), body.introduction());
        return ResponseEntity.status(201).cacheControl(CacheControl.noStore())
                .body(new StudentRegistrationController.RegistrationResponse(true));
    }
    private static ResponseStatusException invalid() { return new ResponseStatusException(HttpStatus.BAD_REQUEST); }
}
