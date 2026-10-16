package com.englishlearning.features.user.controller;
import com.englishlearning.common.api.ApiError;
import com.englishlearning.features.user.service.ProfileFailure;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.TransactionException;
@RestControllerAdvice(assignableTypes=StudentProfileController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ProfileExceptionHandler {
    @ExceptionHandler(ProfileFailure.class)
    ResponseEntity<ApiError> rejected(ProfileFailure error){return switch(error.reason()){
        case INVALID->error(400,"INVALID_REQUEST");case UNAUTHENTICATED->error(401,"AUTHENTICATION_REQUIRED");case DENIED->error(403,"ACCESS_DENIED");};}
    @ExceptionHandler({DataAccessException.class,TransactionException.class})
    ResponseEntity<ApiError> unavailable(Exception error){return error(503,"PROFILE_UNAVAILABLE");}
    private ResponseEntity<ApiError> error(int status,String code){return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(new ApiError(code,"Profile request could not be completed."));}
}
