package com.englishlearning.features.teacher.controller;

import com.englishlearning.features.auth.security.AccountPrincipal;
import com.englishlearning.features.teacher.dto.TeacherOnboardingStatus;
import com.englishlearning.features.teacher.service.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class TeacherOnboardingController {
    private final TeacherOnboardingService service;
    public TeacherOnboardingController(TeacherOnboardingService service) { this.service = service; }
    @GetMapping("/api/v1/teacher/onboarding")
    public ResponseEntity<TeacherOnboardingStatus> ownStatus(@AuthenticationPrincipal AccountPrincipal principal,
            HttpServletRequest request) throws IOException {
        if (request.getQueryString() != null || !request.getParameterMap().isEmpty()
                || request.getInputStream().read() != -1)
            throw new TeacherReviewFailure(TeacherReviewFailure.Reason.INVALID_REQUEST);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.ownStatus(principal.id()));
    }
}
