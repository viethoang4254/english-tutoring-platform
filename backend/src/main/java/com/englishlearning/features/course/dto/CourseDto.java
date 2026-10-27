package com.englishlearning.features.course.dto;

import com.englishlearning.common.api.ApiId;
import com.englishlearning.common.api.ApiMoney;
import java.time.Instant;
import java.time.LocalDate;

public record CourseDto(ApiId id, String name, String status, Category category, ApiMoney tuition,
        LocalDate plannedStartDate, String timezone, int sessionCount, int minStudents, int maxStudents,
        Instant createdAt, Instant updatedAt, String description, Discount discount) {
    public record Category(ApiId id, String name, boolean active) {}
    public record Discount(String percent, Instant startAt, Instant endAt) {}
    public record Summary(ApiId id, String name, String status, Category category, ApiMoney tuition,
            LocalDate plannedStartDate, String timezone, int sessionCount, int minStudents, int maxStudents,
            Instant createdAt, Instant updatedAt) {}
    public Summary summary() {
        return new Summary(id, name, status, category, tuition, plannedStartDate, timezone,
                sessionCount, minStudents, maxStudents, createdAt, updatedAt);
    }
}
