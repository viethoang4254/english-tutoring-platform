package com.englishlearning.features.course.dto;
import com.englishlearning.common.api.ApiId;
import java.time.Instant;
public record CalendarSessionDto(ApiId sessionId, ApiId courseId, String courseTitle,
        int sessionNumber, String sessionTitle, Instant startsAt, Instant endsAt,
        String courseTimezone, String status) {}
