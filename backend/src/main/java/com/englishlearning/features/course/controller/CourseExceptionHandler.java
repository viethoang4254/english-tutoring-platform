package com.englishlearning.features.course.controller;

import com.englishlearning.common.api.ApiError;
import com.englishlearning.features.course.service.CourseFailure;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.http.*;
import org.springframework.transaction.TransactionException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice(assignableTypes={CourseController.class,TeacherCalendarController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CourseExceptionHandler {
    @ExceptionHandler(CourseFailure.class)
    ResponseEntity<ApiError> rejected(CourseFailure failure) {
        return switch (failure.reason()) {
            case INVALID -> error(400, "INVALID_REQUEST");
            case DENIED -> error(403, "ACCESS_DENIED");
            case MISSING -> error(404, "COURSE_NOT_FOUND");
            case CATEGORY_MISSING -> error(404, "CATEGORY_NOT_FOUND");
            case CATEGORY_INACTIVE -> error(409, "CATEGORY_INACTIVE");
            case NOT_DRAFT -> error(409, "COURSE_NOT_DRAFT");
            case SCHEDULE_LOCKED -> error(409, "COURSE_SCHEDULE_LOCKED");
            case SCHEDULE_OVERLAP -> error(409, "SCHEDULE_OVERLAP");
            case SCHEDULE_TIME -> error(409, "SCHEDULE_TIME_CONFLICT");
            case GENERATION_LIMIT -> error(422, "SCHEDULE_GENERATION_LIMIT");
            case ALREADY_GENERATED -> error(409, "SESSIONS_ALREADY_GENERATED");
            case SESSION_MISSING -> error(404, "SESSION_NOT_FOUND");
            case SESSION_NOT_EDITABLE -> error(409, "SESSION_NOT_EDITABLE");
            case SESSION_CHANGED -> error(409, "SESSION_CHANGED");
            case CALENDAR_SYNC_REQUIRED -> error(409, "CALENDAR_SYNC_REQUIRED");
        };
    }
    @ExceptionHandler({DataAccessException.class, TransactionException.class})
    ResponseEntity<ApiError> unavailable(Exception failure) { return error(503, "COURSE_UNAVAILABLE"); }
    private ResponseEntity<ApiError> error(int status, String code) {
        return ResponseEntity.status(status).cacheControl(CacheControl.noStore())
                .body(new ApiError(code, "Course request could not be completed."));
    }
}
