package com.englishlearning.features.auth.controller;

import com.englishlearning.common.api.ApiError;
import com.englishlearning.features.auth.service.VerificationFailure;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice(assignableTypes = {StudentRegistrationController.class, TeacherRegistrationController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class VerificationExceptionHandler {
    @ExceptionHandler(VerificationFailure.class)
    ResponseEntity<ApiError> failure(VerificationFailure failure) {
        int status; String code, message;
        switch (failure.reason()) {
            case CONFLICT -> { status = 409; code = "REGISTRATION_CONFLICT"; message = "Registration conflicts with an existing account."; }
            case INVALID -> { status = 400; code = "INVALID_VERIFICATION_CREDENTIAL"; message = "Invalid verification credential."; }
            case RATE_LIMITED -> { status = 429; code = "VERIFICATION_RATE_LIMITED"; message = "Verification resend limit reached."; }
            case DELIVERY -> { status = 503; code = "VERIFICATION_DELIVERY_UNAVAILABLE"; message = "Email delivery failed; any created account remains unverified. Request a resend after the cooldown."; }
            case UNAVAILABLE -> { status = 503; code = "AUTHENTICATION_UNAVAILABLE"; message = "Authentication temporarily unavailable."; }
            case OUTCOME_UNKNOWN -> { status = 503; code = "AUTHENTICATION_OUTCOME_UNKNOWN"; message = "Authentication outcome could not be confirmed."; }
            default -> { status = 500; code = "INTERNAL_ERROR"; message = "An unexpected error occurred."; }
        }
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(new ApiError(code, message));
    }
}
