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
    boolean existsByTeacherIdAndStatus(Long teacherId, String status);
}
