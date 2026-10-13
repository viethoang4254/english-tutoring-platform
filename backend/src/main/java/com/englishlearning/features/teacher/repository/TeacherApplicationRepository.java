package com.englishlearning.features.teacher.repository;

import com.englishlearning.features.teacher.entity.TeacherApplication;
import org.springframework.data.repository.Repository;

public interface TeacherApplicationRepository extends Repository<TeacherApplication, Long> {
    TeacherApplication saveAndFlush(TeacherApplication application);
    java.util.Optional<TeacherApplication> findById(Long id);
    @org.springframework.data.jpa.repository.Query("select a.teacherId from TeacherApplication a where a.id = :id")
    java.util.Optional<Long> findTeacherId(Long id);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select a from TeacherApplication a where a.id = :id")
    java.util.Optional<TeacherApplication> lockById(Long id);
    java.util.Optional<TeacherApplication> findFirstByTeacherIdOrderBySubmittedAtDescIdDesc(Long teacherId);
    interface PendingRow {
        Long getId();
        Long getTeacherId();
        String getStatus();
        java.time.Instant getSubmittedAt();
    }
    @org.springframework.data.jpa.repository.Query(value = """
            SELECT id, teacher_id AS "teacherId", status, submitted_at AS "submittedAt"
            FROM public.teacher_applications WHERE status = 'PENDING'
            ORDER BY submitted_at ASC, id ASC LIMIT :limit OFFSET :offset
            """, nativeQuery = true)
    java.util.List<PendingRow> findPending(int offset, int limit);
    boolean existsByTeacherIdAndStatus(Long teacherId, String status);
}
