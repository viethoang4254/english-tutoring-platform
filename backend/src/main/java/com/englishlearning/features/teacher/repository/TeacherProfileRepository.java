package com.englishlearning.features.teacher.repository;

import com.englishlearning.features.teacher.entity.TeacherProfile;
import java.time.Instant;
import org.springframework.data.repository.Repository;
import org.springframework.data.jpa.repository.*;

public interface TeacherProfileRepository extends Repository<TeacherProfile, Long> {
    // Insert-only: even an unexpected concurrent creator must never be overwritten.
    @Modifying
    @Query(value = "INSERT INTO public.teacher_profiles "
            + "(teacher_id,avatar_path,specialization,teaching_experience,introduction,created_at,updated_at) "
            + "VALUES (:id,NULL,:specialization,:experience,:introduction,:now,:now) "
            + "ON CONFLICT (teacher_id) DO NOTHING", nativeQuery = true)
    int insert(Long id, String specialization, String experience, String introduction, Instant now);
}
