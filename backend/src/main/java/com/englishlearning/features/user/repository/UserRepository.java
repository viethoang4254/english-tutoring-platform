package com.englishlearning.features.user.repository;

import com.englishlearning.features.user.entity.User;
import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface UserRepository extends Repository<User, Long> {
    @org.springframework.data.jpa.repository.Query(value = "SELECT EXISTS (SELECT 1 FROM public.teacher_applications WHERE teacher_id=:id AND status='APPROVED')", nativeQuery = true)
    boolean approvedTeacher(@org.springframework.data.repository.query.Param("id") Long id);
    Optional<User> findById(Long id);
}
