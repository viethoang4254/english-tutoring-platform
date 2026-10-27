package com.englishlearning.features.course.dto;

import com.englishlearning.common.api.ApiId;
import java.time.Instant;

public record SessionDto(ApiId id, ApiId courseId, int sessionNumber, String title,
        Instant scheduledStartAt, Instant scheduledEndAt, String status, String content,
        String cancellationReason, Instant createdAt, Instant updatedAt) {
    public record Summary(ApiId id, ApiId courseId, int sessionNumber, String title,
            Instant scheduledStartAt, Instant scheduledEndAt, String status) {}
    public Summary summary() {
        return new Summary(id,courseId,sessionNumber,title,scheduledStartAt,scheduledEndAt,status);
    }
}
