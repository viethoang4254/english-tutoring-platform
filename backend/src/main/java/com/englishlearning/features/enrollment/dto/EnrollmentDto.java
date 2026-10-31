package com.englishlearning.features.enrollment.dto;

import com.englishlearning.common.api.ApiId;
import java.time.Instant;

public record EnrollmentDto(ApiId id, ApiId courseId, String courseTitle, String status,
        Instant enrolledAt, Instant seatReservedUntil, boolean reservationExpired,
        Instant cancelledAt, String cancellationReason, Instant createdAt, Instant updatedAt) {}
