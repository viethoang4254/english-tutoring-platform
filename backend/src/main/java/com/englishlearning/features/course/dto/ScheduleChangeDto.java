package com.englishlearning.features.course.dto;

import com.englishlearning.common.api.ApiId;
import java.time.Instant;

public record ScheduleChangeDto(ApiId id,ApiId sessionId,Instant oldStartAt,Instant oldEndAt,
        Instant newStartAt,Instant newEndAt,Instant changedAt) {}
