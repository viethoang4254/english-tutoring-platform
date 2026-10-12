package com.englishlearning.features.user.controller;

import com.englishlearning.common.api.ApiError;
import com.englishlearning.common.api.ApiId;
import com.englishlearning.features.auth.security.AccountPrincipal;
import com.englishlearning.features.user.dto.CurrentAccountResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CurrentAccountController {
    @GetMapping("/api/v1/users/me")
    public ResponseEntity<?> currentAccount(@AuthenticationPrincipal AccountPrincipal account,
            HttpServletRequest request) throws IOException {
        // Security filters resolve the current account before input is inspected here.
        if ((request.getQueryString() != null && !request.getQueryString().isEmpty())
                || !request.getParameterMap().isEmpty() || request.getInputStream().read() != -1) {
            return ResponseEntity.badRequest()
                    .body(new ApiError("INVALID_REQUEST", "Request could not be processed."));
        }
        return ResponseEntity.ok(new CurrentAccountResponse(
                new ApiId(account.id()), account.role(), account.emailVerified()));
    }
}
