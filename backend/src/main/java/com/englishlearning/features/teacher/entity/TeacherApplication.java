package com.englishlearning.features.teacher.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "teacher_applications", schema = "public")
public class TeacherApplication {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "teacher_id", nullable = false) private Long teacherId;
    @Column(nullable = false) private String specialization;
    @Column(name = "teaching_experience", nullable = false) private String teachingExperience;
    @Column(nullable = false) private String introduction;
    @Column(nullable = false) private String status;
    @Column(name = "submitted_at", nullable = false) private Instant submittedAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Column(name = "reviewed_by") private Long reviewedBy;
    @Column(name = "reviewed_at") private Instant reviewedAt;
    @Column(name = "rejection_reason") private String rejectionReason;
    public Long getId() { return id; }
    public Long getTeacherId() { return teacherId; }
    public String getSpecialization() { return specialization; }
    public String getTeachingExperience() { return teachingExperience; }
    public String getIntroduction() { return introduction; }
    public String getStatus() { return status; }
    public Instant getSubmittedAt() { return submittedAt; }
    public Long getReviewedBy() { return reviewedBy; }
    public Instant getReviewedAt() { return reviewedAt; }
    public String getRejectionReason() { return rejectionReason; }
    public void review(Long reviewer, boolean approved, String reason, Instant now) {
        if (!"PENDING".equals(status)) throw new IllegalStateException("Application already reviewed.");
        if (!approved && (reason == null || reason.isBlank() || reason.indexOf(0) >= 0))
            throw new IllegalArgumentException("Invalid rejection reason.");
        status = approved ? "APPROVED" : "REJECTED";
        reviewedBy = reviewer; reviewedAt = now; updatedAt = now;
        rejectionReason = approved ? null : reason;
    }
    protected TeacherApplication() {}
    public TeacherApplication(Long teacherId, String specialization, String teachingExperience,
            String introduction, Instant now) {
        for (String text : new String[]{specialization, teachingExperience, introduction}) {
            if (text == null || text.isBlank() || text.indexOf(0) >= 0) {
                throw new IllegalArgumentException("Invalid application text.");
            }
        }
        this.teacherId = teacherId; this.specialization = specialization;
        this.teachingExperience = teachingExperience; this.introduction = introduction;
        status = "PENDING"; submittedAt = now; createdAt = now; updatedAt = now;
    }
}
