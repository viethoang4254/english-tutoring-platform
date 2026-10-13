package com.englishlearning.features.teacher.service;

import com.englishlearning.features.teacher.dto.TeacherOnboardingStatus;
import com.englishlearning.features.teacher.repository.TeacherApplicationRepository;
import com.englishlearning.features.user.service.CurrentAccountService;
import com.englishlearning.features.user.entity.User.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeacherOnboardingService {
    private final CurrentAccountService accounts;
    private final TeacherApplicationRepository applications;
    public TeacherOnboardingService(CurrentAccountService accounts, TeacherApplicationRepository applications) {
        this.accounts = accounts; this.applications = applications;
    }
    @Transactional(readOnly = true)
    public TeacherOnboardingStatus ownStatus(Long authenticatedUserId) {
        var account = accounts.findById(authenticatedUserId).orElse(null);
        if (account == null || account.role() != Role.TEACHER || account.locked() || !account.emailVerified())
            throw new TeacherReviewFailure(TeacherReviewFailure.Reason.DENIED);
        // Account-only visibility deliberately does not require approved business eligibility.
        return applications.findFirstByTeacherIdOrderBySubmittedAtDescIdDesc(authenticatedUserId)
                .map(TeacherOnboardingStatus::from)
                .orElseThrow(() -> new TeacherReviewFailure(TeacherReviewFailure.Reason.ONBOARDING_NOT_FOUND));
    }
}
