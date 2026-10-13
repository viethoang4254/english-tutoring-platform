package com.englishlearning.features.teacher.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "teacher_profiles", schema = "public")
public class TeacherProfile {
    @Id @Column(name = "teacher_id") private Long teacherId;
    @Column(name = "avatar_path") private String avatarPath;
    @Column(nullable = false) private String specialization;
    @Column(name = "teaching_experience", nullable = false) private String teachingExperience;
    @Column(nullable = false) private String introduction;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    protected TeacherProfile() {}
}
