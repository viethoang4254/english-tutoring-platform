package com.englishlearning.features.user.dto;

import com.englishlearning.common.api.ApiId;
import com.englishlearning.common.api.ApiMoney;
import java.time.Instant;
import java.util.List;

public record AdminAnalyticsDto(String range, String timezone, Instant from, Instant asOf,
        Totals totals, Activity period, List<Day> days, List<CourseTuition> courseTuition, boolean moreCourses, List<TeacherTuition> teacherTuition, boolean moreTeachers) {
    public record Totals(long users, long students, long teachers, long approvedTeachers,
            long pendingApplications, long courses, long publishedCourses, long draftCourses,
            long enrollments, long payments, long confirmedPayments, long pendingPayments,
            long expiredPayments, long cancelledPayments, ApiMoney confirmedTuition) {}
    public record Activity(long users, long enrollments, long courses, long confirmedPayments, ApiMoney confirmedTuition) {}
    public record Day(String date, long users, long enrollments, long courses, long confirmedPayments, ApiMoney confirmedTuition) {}
    public record TeacherTuition(ApiId teacherId, String teacherName, long confirmedPayments, ApiMoney confirmedTuition) {}
    public record CourseTuition(ApiId courseId, String courseName, ApiId teacherId, String teacherName,
            long confirmedPayments, ApiMoney confirmedTuition) {}
}
