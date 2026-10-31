package com.englishlearning.features.enrollment.controller;

import com.englishlearning.common.api.ApiError;
import com.englishlearning.features.enrollment.service.EnrollmentFailure;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice(assignableTypes=EnrollmentController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EnrollmentExceptionHandler {
    @ExceptionHandler(EnrollmentFailure.class)
    ResponseEntity<ApiError> failure(EnrollmentFailure failure) {
        int status=switch(failure.reason()) {
            case INVALID_REQUEST -> 400;
            case ACCESS_DENIED -> 403;
            case COURSE_NOT_FOUND, ENROLLMENT_NOT_FOUND -> 404;
            default -> 409;
        };
        return error(status,failure.reason().name());
    }
    @ExceptionHandler({DataAccessException.class,TransactionException.class})
    ResponseEntity<ApiError> unavailable(Exception failure) { return error(503,"ENROLLMENT_UNAVAILABLE"); }
    private ResponseEntity<ApiError> error(int status,String code) {
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(new ApiError(code,"Enrollment request could not be completed."));
    }
}
