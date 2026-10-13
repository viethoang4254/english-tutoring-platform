package com.englishlearning.features.teacher.controller;

import com.englishlearning.common.api.ApiId;
import com.englishlearning.features.auth.security.AccountPrincipal;
import com.englishlearning.features.teacher.dto.TeacherApplicationDetail;
import com.englishlearning.features.teacher.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.io.IOException;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/teacher-applications")
public class TeacherApplicationReviewController {
    private final TeacherApplicationReviewService service;
    public TeacherApplicationReviewController(TeacherApplicationReviewService service) { this.service = service; }
    @GetMapping("/{applicationId}")
    public ResponseEntity<TeacherApplicationDetail> detail(@AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable String applicationId, HttpServletRequest request) throws IOException {
        noInput(request, true);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.detail(principal.id(), id(applicationId)));
    }
    @PostMapping("/{applicationId}/approve")
    public ResponseEntity<Void> approve(@AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable String applicationId, HttpServletRequest request) throws IOException {
        noInput(request, true);
        service.review(principal.id(), id(applicationId), true, null);
        return ResponseEntity.noContent().build();
    }
    @PostMapping(value = "/{applicationId}/reject", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> reject(@AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable String applicationId, @Valid @RequestBody RejectionRequest body,
            HttpServletRequest request) throws IOException {
        noInput(request, false);
        if (body.reason().indexOf(0) >= 0) throw invalid();
        service.review(principal.id(), id(applicationId), false, body.reason());
        return ResponseEntity.noContent().build();
    }
    public record RejectionRequest(@NotBlank String reason) {
        @Override public String toString() { return "RejectionRequest[REDACTED]"; }
    }
    private static void noInput(HttpServletRequest request, boolean noBody) throws IOException {
        if (request.getQueryString() != null || !request.getParameterMap().isEmpty()
                || (noBody && request.getInputStream().read() != -1)) throw invalid();
    }
    private static Long id(String input) {
        try { long id = ApiId.parse(input).value(); if (id > 0) return id; }
        catch (IllegalArgumentException ignored) { }
        throw invalid();
    }
    private static TeacherReviewFailure invalid() { return new TeacherReviewFailure(TeacherReviewFailure.Reason.INVALID_REQUEST); }
}
