package com.englishlearning.features.auth.controller;

import com.englishlearning.common.api.ApiError;
import com.englishlearning.features.auth.service.AuthenticationFailure;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {AuthenticationController.class,PasswordChangeController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthenticationExceptionHandler {
    @ExceptionHandler(AuthenticationFailure.class)
    ResponseEntity<ApiError> authenticationFailure(AuthenticationFailure failure) {
        int status;
        String code, message;
        switch (failure.reason()) {
            case ACCESS_DENIED -> {status=403;code="ACCESS_DENIED";message="Access denied.";}
            case INVALID_REQUEST -> {status=400;code="INVALID_REQUEST";message="Invalid request.";}
            case INVALID_CREDENTIAL -> {
                status = 401; code = "AUTHENTICATION_REQUIRED"; message = "Authentication required.";
            }
            case EMAIL_VERIFICATION_REQUIRED -> {
                status = 403; code = "EMAIL_VERIFICATION_REQUIRED"; message = "Email verification required.";
            }
            case UNAVAILABLE -> {
                status = 503; code = "AUTHENTICATION_UNAVAILABLE"; message = "Authentication temporarily unavailable.";
            }
            case COMMIT_UNCERTAIN -> {
                status = 503; code = "AUTHENTICATION_OUTCOME_UNKNOWN"; message = "Authentication outcome could not be confirmed.";
            }
            default -> {
                status = 500; code = "INTERNAL_ERROR"; message = "An unexpected error occurred.";
            }
        }
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(new ApiError(code, message));
    }
}
