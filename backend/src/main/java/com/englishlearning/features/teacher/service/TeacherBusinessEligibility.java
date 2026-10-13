package com.englishlearning.features.teacher.service;

import com.englishlearning.features.teacher.repository.TeacherApplicationRepository;
import com.englishlearning.features.user.service.CurrentAccountService;
import com.englishlearning.features.user.entity.User.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeacherBusinessEligibility {
    private final CurrentAccountService accounts;
    private final TeacherApplicationRepository applications;
    public TeacherBusinessEligibility(CurrentAccountService accounts, TeacherApplicationRepository applications) {
        this.accounts = accounts; this.applications = applications;
    }
    @Transactional(readOnly = true)
    public void requireEligible(Long userId) {
        var user = accounts.findById(userId).orElse(null);
        if (user == null || user.role() != Role.TEACHER || user.locked() || !user.emailVerified()
                || !applications.existsByTeacherIdAndStatus(userId, "APPROVED"))
            throw new TeacherReviewFailure(TeacherReviewFailure.Reason.DENIED);
    }
}
