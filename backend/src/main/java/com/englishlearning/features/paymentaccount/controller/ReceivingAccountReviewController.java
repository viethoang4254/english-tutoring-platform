package com.englishlearning.features.paymentaccount.controller;

import com.englishlearning.common.api.ApiId;
import com.englishlearning.features.auth.security.AccountPrincipal;
import com.englishlearning.features.paymentaccount.dto.ReceivingAccountReview;

import com.englishlearning.features.paymentaccount.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.io.IOException;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/payment-accounts")
public class ReceivingAccountReviewController {
    private final ReceivingAccountReviewService service;
    public ReceivingAccountReviewController(ReceivingAccountReviewService service) { this.service = service; }
    @GetMapping("/pending")
    public ResponseEntity<ReceivingAccountReviewService.Page> pending(@AuthenticationPrincipal AccountPrincipal principal,
            HttpServletRequest request) throws IOException {
        if (request.getParameterMap().keySet().stream().anyMatch(key -> !key.equals("page") && !key.equals("size"))
                || request.getInputStream().read() != -1) throw invalid();
        int page = unsignedParameter(request, "page", 0);
        int size = unsignedParameter(request, "size", 20);
        if (size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE) throw invalid();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.pending(principal.id(), page, size));
    }
    private static int unsignedParameter(HttpServletRequest request, String name, int fallback) {
        String[] values = request.getParameterValues(name);
        if (values == null) return fallback;
        if (values.length != 1 || !values[0].matches("[0-9]+")) throw invalid();
        try { return Integer.parseInt(values[0]); }
        catch (NumberFormatException exception) { throw invalid(); }
    }
    @GetMapping("/{id}")
    public ResponseEntity<ReceivingAccountReview> detail(@AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable String id, HttpServletRequest request) throws IOException {
        noInput(request, true);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.detail(principal.id(), id(id)));
    }
    @PostMapping("/{id}/approve")
    public ResponseEntity<Void> approve(@AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable String id, HttpServletRequest request) throws IOException {
        noInput(request, true);
        service.review(principal.id(), id(id), true, null);
        return ResponseEntity.noContent().build();
    }
    @PostMapping(value = "/{id}/reject", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> reject(@AuthenticationPrincipal AccountPrincipal principal,
            @PathVariable String id, @Valid @RequestBody RejectionRequest body,
            HttpServletRequest request) throws IOException {
        noInput(request, false);
        if (body.reason().length()>2000 || body.reason().codePoints().anyMatch(Character::isISOControl)) throw invalid();
        service.review(principal.id(), id(id), false, body.reason());
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
    private static ReceivingFailure invalid() { return new ReceivingFailure(ReceivingFailure.Reason.INVALID); }
}
