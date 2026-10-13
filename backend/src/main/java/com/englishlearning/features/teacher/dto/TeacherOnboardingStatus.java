package com.englishlearning.features.teacher.dto;

import com.englishlearning.common.api.ApiId;
import com.englishlearning.features.teacher.entity.TeacherApplication;
import java.time.Instant;

public record TeacherOnboardingStatus(ApiId applicationId, String status, Instant submittedAt,
        Instant reviewedAt, String rejectionReason) {
    public static TeacherOnboardingStatus from(TeacherApplication application) {
        return new TeacherOnboardingStatus(new ApiId(application.getId()), application.getStatus(),
                application.getSubmittedAt(), application.getReviewedAt(), application.getRejectionReason());
    }
    @Override public String toString() { return "TeacherOnboardingStatus[REDACTED]"; }
}
