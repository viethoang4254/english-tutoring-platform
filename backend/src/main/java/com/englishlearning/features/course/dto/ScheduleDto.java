package com.englishlearning.features.course.dto;

import com.englishlearning.common.api.ApiId;
import java.time.LocalDate;
import java.util.List;

public record ScheduleDto(ApiId courseId, LocalDate plannedStartDate, String timezone, int sessionCount,
        long generatedSessionCount, List<Rule> rules) {
    public record Rule(ApiId id, int dayOfWeek, String startTime, String endTime) {}
    public record Generated(ApiId courseId, int generatedSessionCount) {}
}
