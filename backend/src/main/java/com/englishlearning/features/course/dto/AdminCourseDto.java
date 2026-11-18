package com.englishlearning.features.course.dto;
import com.englishlearning.common.api.*;
import java.time.Instant;
public record AdminCourseDto(ApiId id, String name, String description, ApiMoney tuition,
    PublicCourseDto.Teacher teacher, PublicCourseDto.Category category, String status, String timezone,
    long enrollmentCount, long activeEnrollmentCount, int plannedSessions, long recordedSessions,
    long scheduledSessions, Instant firstSessionAt, Instant lastSessionAt) {}
