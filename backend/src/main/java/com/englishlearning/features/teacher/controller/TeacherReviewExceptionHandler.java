package com.englishlearning.features.teacher.controller;

import com.englishlearning.common.api.ApiError;
import com.englishlearning.features.teacher.service.TeacherReviewFailure;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice(assignableTypes = TeacherApplicationReviewController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TeacherReviewExceptionHandler {
    @ExceptionHandler(TeacherReviewFailure.class)
    ResponseEntity<ApiError> failure(TeacherReviewFailure failure) {
        return switch (failure.reason()) {
            case DENIED -> error(403, "ACCESS_DENIED");
            case NOT_FOUND -> error(404, "APPLICATION_NOT_FOUND");
            case ALREADY_REVIEWED -> error(409, "APPLICATION_ALREADY_REVIEWED");
            case PROFILE_EXISTS -> error(409, "TEACHER_PROFILE_CONFLICT");
            case INVALID_APPLICANT -> error(409, "APPLICATION_NOT_REVIEWABLE");
            case INVALID_REQUEST -> error(400, "INVALID_REQUEST");
        };
    }
    private ResponseEntity<ApiError> error(int status, String code) {
        return ResponseEntity.status(status).body(new ApiError(code, "Teacher application request could not be processed."));
    }
}
