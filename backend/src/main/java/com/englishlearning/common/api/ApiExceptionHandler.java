package com.englishlearning.common.api;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        // Do not echo rejected values or custom validator messages.
        exception.getBindingResult().getFieldErrors().forEach(
                error -> fields.put(error.getField(), "Invalid value."));
        return new ResponseEntity<>(
                new ApiError("INVALID_REQUEST", "Request validation failed.",
                        fields.isEmpty() ? null : fields), headers, status);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception, Object body, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        // Preserve framework status/headers; do not choose authorization/privacy policy.
        String code = status.value() == 400 ? "INVALID_REQUEST" : "REQUEST_REJECTED";
        return new ResponseEntity<>(
                new ApiError(code, "Request could not be processed."), headers, status);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception exception) {
        // Raw exception messages can contain credentials, SQL or private records.
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError("INTERNAL_ERROR", "An unexpected error occurred."));
    }
}
