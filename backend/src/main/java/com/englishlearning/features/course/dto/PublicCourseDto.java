package com.englishlearning.features.course.dto;

import com.englishlearning.common.api.ApiId;
import com.englishlearning.common.api.ApiMoney;
import java.time.LocalDate;
import java.util.List;

// Public allowlist; never reuse the protected Course or Session response.
public record PublicCourseDto(ApiId id, String name, String description, ApiMoney tuition,
        Category category, Teacher teacher, LocalDate plannedStartDate, String timezone, int sessionCount) {
    public record Category(ApiId id, String name) {}
    public record Teacher(ApiId id, String fullName) {}
    public record Detail(PublicCourseDto course, List<ScheduleDto.Rule> rules, CourseOutline outline) {}
}
