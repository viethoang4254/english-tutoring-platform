package com.englishlearning.features.teacher.dto;

import com.englishlearning.common.api.ApiId;
import com.englishlearning.features.teacher.entity.TeacherApplication;
import java.time.Instant;

public record TeacherApplicationDetail(ApiId id, ApiId teacherId, String specialization,
        String teachingExperience, String introduction, String status, Instant submittedAt,
        ApiId reviewedBy, Instant reviewedAt, String rejectionReason) {
    public static TeacherApplicationDetail from(TeacherApplication a) {
        return new TeacherApplicationDetail(new ApiId(a.getId()), new ApiId(a.getTeacherId()),
                a.getSpecialization(), a.getTeachingExperience(), a.getIntroduction(), a.getStatus(),
                a.getSubmittedAt(), a.getReviewedBy() == null ? null : new ApiId(a.getReviewedBy()),
                a.getReviewedAt(), a.getRejectionReason());
    }
    @Override public String toString() { return "TeacherApplicationDetail[REDACTED]"; }
}
