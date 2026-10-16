package com.englishlearning.features.teacher.controller;

import com.englishlearning.features.auth.security.AccountPrincipal;
import com.englishlearning.features.teacher.dto.TeacherOnboardingStatus;
import com.englishlearning.features.teacher.service.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import com.englishlearning.common.api.ApiId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class TeacherOnboardingController {
    private final TeacherOnboardingService service;
    public TeacherOnboardingController(TeacherOnboardingService service) { this.service = service; }
    @PostMapping(value = "/api/v1/teacher/applications/{applicationId}/resubmit", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TeacherOnboardingStatus> resubmit(@AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable String applicationId, @Valid @RequestBody ResubmissionRequest body,
            HttpServletRequest request) {
        if (request.getQueryString() != null || !request.getParameterMap().isEmpty()) throw invalid();
        long id;
        try { id = ApiId.parse(applicationId).value(); }
        catch (IllegalArgumentException exception) { throw invalid(); }
        if (id <= 0) throw invalid();
        for (String text : new String[]{body.specialization(), body.teachingExperience(), body.introduction()})
            if (text.indexOf(0) >= 0) throw invalid();
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore()).body(
                service.resubmit(principal.id(), id, body.specialization(), body.teachingExperience(), body.introduction()));
    }
    public record ResubmissionRequest(@NotBlank String specialization, @NotBlank String teachingExperience,
            @NotBlank String introduction) {
        @Override public String toString() { return "ResubmissionRequest[REDACTED]"; }
    }
    private static TeacherReviewFailure invalid() { return new TeacherReviewFailure(TeacherReviewFailure.Reason.INVALID_REQUEST); }
    @GetMapping("/api/v1/teacher/onboarding")
    public ResponseEntity<TeacherOnboardingStatus> ownStatus(@AuthenticationPrincipal AccountPrincipal principal,
            HttpServletRequest request) throws IOException {
        if (request.getQueryString() != null || !request.getParameterMap().isEmpty()
                || request.getInputStream().read() != -1)
            throw new TeacherReviewFailure(TeacherReviewFailure.Reason.INVALID_REQUEST);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.ownStatus(principal.id()));
    }
}
