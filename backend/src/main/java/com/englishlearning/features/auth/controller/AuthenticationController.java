package com.englishlearning.features.auth.controller;

import com.englishlearning.features.auth.dto.*;
import com.englishlearning.features.auth.security.BrowserAuthSettings;
import com.englishlearning.features.auth.security.PasswordInput;
import com.englishlearning.features.auth.service.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping(BrowserAuthSettings.PATH)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class AuthenticationController {
    private final AuthenticationWorkflowService workflows;
    private final BrowserAuthSettings browser;
    private final Clock clock;

    public AuthenticationController(AuthenticationWorkflowService workflows, BrowserAuthSettings browser,
            @Qualifier("refreshSessionClock") Clock clock) {
        this.workflows = workflows; this.browser = browser; this.clock = clock;
    }
    @GetMapping("/csrf")
    public CsrfResponse csrf(HttpServletRequest request, CsrfToken token) throws IOException {
        emptyInput(request);
        return new CsrfResponse(token.getToken());
    }
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AccessTokenResponse> login(@Valid @RequestBody LoginRequest body,
            HttpServletRequest request) {
        noQuery(request);
        if (!PasswordInput.isValid(body.password())) throw invalid();
        return issued(workflows.login(body.email(), body.password()));
    }
    @PostMapping("/refresh")
    public ResponseEntity<AccessTokenResponse> refresh(HttpServletRequest request) throws IOException {
        emptyInput(request);
        return issued(workflows.refresh(refreshCookie(request)));
    }
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) throws IOException {
        emptyInput(request);
        workflows.logout(refreshCookie(request));
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie("", 0).toString()).build();
    }
    private ResponseEntity<AccessTokenResponse> issued(AuthenticationResult result) {
        var token = result.accessToken();
        long remaining = Math.max(0, Duration.between(clock.instant(), result.refreshCredential().expiresAt()).getSeconds());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.SET_COOKIE, cookie(result.refreshCredential().credential(), remaining).toString())
                .body(new AccessTokenResponse(token.value(), "Bearer", token.issuedAt(), token.expiresAt()));
    }
    private ResponseCookie cookie(String value, long remaining) {
        return ResponseCookie.from(BrowserAuthSettings.REFRESH_COOKIE, value)
                .httpOnly(true).secure(browser.secure()).sameSite("Lax")
                .path(BrowserAuthSettings.PATH).maxAge(remaining).build();
    }
    private static String refreshCookie(HttpServletRequest request) {
        String credential = null;
        if (request.getCookies() != null) for (Cookie cookie : request.getCookies()) {
            if (BrowserAuthSettings.REFRESH_COOKIE.equals(cookie.getName())) {
                if (credential != null) throw invalid();
                credential = cookie.getValue();
            }
        }
        return credential;
    }
    private static void noQuery(HttpServletRequest request) {
        if (request.getQueryString() != null || !request.getParameterMap().isEmpty()) throw invalid();
    }
    private static void emptyInput(HttpServletRequest request) throws IOException {
        noQuery(request);
        if (request.getInputStream().read() != -1) throw invalid();
    }
    private static ResponseStatusException invalid() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST);
    }
}
