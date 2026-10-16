package com.englishlearning.features.auth.controller;

import com.englishlearning.features.auth.security.PasswordInput;
import com.englishlearning.features.auth.service.PasswordRecoveryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth/password")
@ConditionalOnWebApplication(type=ConditionalOnWebApplication.Type.SERVLET)
public class PasswordRecoveryController {
    private final PasswordRecoveryService service;
    public PasswordRecoveryController(PasswordRecoveryService service){this.service=service;}
    @PostMapping(value="/forgot",consumes=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> forgot(@Valid @RequestBody ForgotRequest body,HttpServletRequest request) {
        noQuery(request);service.forgot(body.email());return ResponseEntity.accepted().cacheControl(CacheControl.noStore()).build();
    }
    @PostMapping(value="/reset",consumes=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> reset(@Valid @RequestBody ResetRequest body,HttpServletRequest request) {
        noQuery(request);if(!PasswordInput.isValid(body.password())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        service.reset(body.email(),body.code(),body.password());return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
    private void noQuery(HttpServletRequest request){if(request.getQueryString()!=null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);}
    public record ForgotRequest(@NotBlank @Email String email) {
        public ForgotRequest {if(email!=null) email=email.trim();}
        @Override public String toString(){return "ForgotRequest[REDACTED]";}
    }
    public record ResetRequest(@NotBlank @Email String email,@NotNull String code,@NotNull String password) {
        public ResetRequest {if(email!=null) email=email.trim();}
        @Override public String toString(){return "ResetRequest[REDACTED]";}
    }
}
