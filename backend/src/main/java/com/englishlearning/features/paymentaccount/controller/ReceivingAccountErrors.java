package com.englishlearning.features.paymentaccount.controller;
import com.englishlearning.common.api.ApiError;
import com.englishlearning.features.paymentaccount.service.ReceivingFailure;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.core.annotation.Order;
@RestControllerAdvice(assignableTypes=ReceivingAccountController.class)
@Order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE)
public class ReceivingAccountErrors {
    @ExceptionHandler(ReceivingFailure.class) ResponseEntity<ApiError> failure(ReceivingFailure e){return switch(e.reason()) {
        case INVALID->error(400,"INVALID_REQUEST");case DENIED->error(403,"ACCESS_DENIED");case MISSING->error(404,"RECEIVING_ACCOUNT_NOT_FOUND");
        case CONFLICT->error(409,"PENDING_SUBMISSION_EXISTS");case IDEMPOTENCY_CONFLICT->error(409,"IDEMPOTENCY_CONFLICT");case UNAVAILABLE->error(503,"RECEIVING_ACCOUNT_UNAVAILABLE");};}
    @ExceptionHandler({org.springframework.dao.DataAccessException.class,org.springframework.transaction.TransactionException.class,java.io.IOException.class})
    ResponseEntity<ApiError> unavailable(Exception e){return error(503,"RECEIVING_ACCOUNT_UNAVAILABLE");}
    @ExceptionHandler(org.springframework.web.multipart.MultipartException.class)
    ResponseEntity<ApiError> multipart(Exception e){return error(400,"INVALID_REQUEST");}
    private ResponseEntity<ApiError> error(int status,String code){return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(new ApiError(code,"Receiving account request could not be completed."));}
}
