package com.englishlearning.features.auth.controller;
import com.englishlearning.features.auth.security.AccountPrincipal;
import com.englishlearning.features.auth.service.PasswordChangeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController
@ConditionalOnWebApplication(type=ConditionalOnWebApplication.Type.SERVLET)
public class PasswordChangeController {
    private final PasswordChangeService service;
    public PasswordChangeController(PasswordChangeService service){this.service=service;}
    @PostMapping(value="/api/v1/auth/password/change",consumes=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> change(@AuthenticationPrincipal AccountPrincipal account,@Valid @RequestBody ChangeRequest body,HttpServletRequest request){
        if(request.getQueryString()!=null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        service.change(account.id(),AuthenticationController.refreshCookie(request),body.currentPassword(),body.newPassword());
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
    public record ChangeRequest(@NotNull String currentPassword,@NotNull String newPassword){
        @Override public String toString(){return "ChangeRequest[REDACTED]";}
    }
}
