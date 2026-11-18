package com.englishlearning.features.user.controller;

import com.englishlearning.features.auth.security.AccountPrincipal;
import com.englishlearning.features.user.service.AdminAnalyticsService;
import com.englishlearning.features.user.service.AdminUserService;
import com.englishlearning.common.api.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/analytics")
public class AdminAnalyticsController {
    private final AdminAnalyticsService service;
    public AdminAnalyticsController(AdminAnalyticsService service) { this.service=service; }
    @GetMapping("/overview")
    public ResponseEntity<?> overview(@AuthenticationPrincipal AccountPrincipal actor, HttpServletRequest request) throws java.io.IOException {
        if (request.getInputStream().read()!=-1 || request.getParameterMap().entrySet().stream()
                .anyMatch(e -> !e.getKey().equals("range") || e.getValue().length!=1)) throw new AdminUserService.Failure(400);
        String range=request.getParameter("range");
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.overview(actor.id(),range==null?"30":range));
    }
    @ExceptionHandler(AdminUserService.Failure.class)
    public ResponseEntity<ApiError> rejected(AdminUserService.Failure failure) {
        String code=switch(failure.status) { case 401 -> "AUTHENTICATION_REQUIRED"; case 403 -> "ACCESS_DENIED"; default -> "INVALID_REQUEST"; };
        return ResponseEntity.status(failure.status).cacheControl(CacheControl.noStore()).body(new ApiError(code,"Analytics request rejected."));
    }
    @ExceptionHandler({org.springframework.dao.DataAccessException.class,org.springframework.transaction.TransactionException.class})
    public ResponseEntity<ApiError> unavailable(Exception failure) {
        return ResponseEntity.status(503).cacheControl(CacheControl.noStore()).body(new ApiError("ANALYTICS_UNAVAILABLE","Analytics temporarily unavailable."));
    }
}
