package com.englishlearning.features.payment.controller;

import com.englishlearning.common.api.ApiError;
import com.englishlearning.features.payment.service.PaymentFailure;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice(assignableTypes={PaymentController.class,ReconciliationController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PaymentExceptionHandler {
    @ExceptionHandler(PaymentFailure.class)
    ResponseEntity<ApiError> failure(PaymentFailure f) {
        int status=switch(f.reason()) {
            case INVALID_REQUEST -> 400;
            case ACCESS_DENIED -> 403;
            case PAYMENT_NOT_FOUND,ENROLLMENT_NOT_FOUND -> 404;
            default -> 409;
        };
        return error(status,f.reason().name());
    }
    @ExceptionHandler({DataAccessException.class,TransactionException.class,IllegalStateException.class})
    ResponseEntity<ApiError> unavailable(Exception e) {return error(503,"PAYMENT_UNAVAILABLE");}
    @ExceptionHandler({org.springframework.http.converter.HttpMessageNotReadableException.class,
        org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    ResponseEntity<ApiError> invalid(Exception e) {return error(400,"INVALID_REQUEST");}
    private static ResponseEntity<ApiError> error(int status,String code) {
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(new ApiError(code,"Payment request could not be completed."));
    }
}
