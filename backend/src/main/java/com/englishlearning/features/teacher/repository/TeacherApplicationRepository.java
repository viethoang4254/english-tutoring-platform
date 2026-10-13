package com.englishlearning.features.teacher.repository;

import com.englishlearning.features.teacher.entity.TeacherApplication;
import org.springframework.data.repository.Repository;

public interface TeacherApplicationRepository extends Repository<TeacherApplication, Long> {
    TeacherApplication saveAndFlush(TeacherApplication application);
}
