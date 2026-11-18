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
    @org.springframework.data.jpa.repository.Query(value = "SELECT id,teacher_id AS \"teacherId\",status,submitted_at AS \"submittedAt\" FROM public.teacher_applications WHERE status=:status ORDER BY submitted_at,id LIMIT :limit OFFSET :offset", nativeQuery=true)
    java.util.List<PendingRow> findStatus(String status,int offset,int limit);
    @org.springframework.data.jpa.repository.Query(value="SELECT a.id,a.teacher_id AS \"teacherId\",a.status,a.submitted_at AS \"submittedAt\" FROM public.teacher_applications a JOIN public.users u ON u.id=a.teacher_id WHERE a.status=:status AND (strpos(lower(u.full_name),:search)>0 OR strpos(lower(u.email),:search)>0) ORDER BY a.submitted_at,a.id LIMIT :limit OFFSET :offset",nativeQuery=true)
    java.util.List<PendingRow> searchStatus(String status,String search,int offset,int limit);
    boolean existsByTeacherIdAndStatus(Long teacherId, String status);
}
