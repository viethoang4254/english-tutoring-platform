package com.englishlearning.features.enrollment.dto;

import com.englishlearning.common.api.ApiId;
import java.time.Instant;

public record StudentCalendarSessionDto(ApiId id, ApiId courseId, String courseTitle,
        String teacherDisplayName, int sessionNumber, String title, Instant startsAt,
        Instant endsAt, String timezone, String status, boolean rescheduled) {}
