package com.englishlearning.features.teacher.service;

import com.englishlearning.features.teacher.dto.TeacherOnboardingStatus;
import com.englishlearning.features.teacher.repository.TeacherApplicationRepository;
import com.englishlearning.features.user.service.CurrentAccountService;
import com.englishlearning.features.user.entity.User.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.beans.factory.annotation.Qualifier;
import com.englishlearning.features.user.service.UserCredentialService;
import com.englishlearning.features.teacher.repository.TeacherProfileRepository;
import com.englishlearning.features.teacher.entity.TeacherApplication;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import static com.englishlearning.features.teacher.service.TeacherReviewFailure.Reason.*;

@Service
public class TeacherOnboardingService {
    private final CurrentAccountService accounts;
    private final TeacherApplicationRepository applications;
    private final UserCredentialService users;
    private final TeacherProfileRepository profiles;
    private final Clock clock;
    public TeacherOnboardingService(CurrentAccountService accounts, TeacherApplicationRepository applications,
            UserCredentialService users, TeacherProfileRepository profiles, @Qualifier("refreshSessionClock") Clock clock) {
        this.accounts = accounts; this.applications = applications;
        this.users = users; this.profiles = profiles; this.clock = clock;
    }
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TeacherOnboardingStatus resubmit(Long teacherId, Long applicationId,
            String specialization, String teachingExperience, String introduction) {
        var account = users.lockAccountById(teacherId).orElse(null);
        if (account == null || account.role() != Role.TEACHER || account.locked() || !account.emailVerified())
            throw new TeacherReviewFailure(DENIED);
        for (String text : new String[]{specialization, teachingExperience, introduction})
            if (text == null || text.isBlank() || text.indexOf(0) >= 0) throw new TeacherReviewFailure(INVALID_REQUEST);
        var target = applications.findById(applicationId)
                .filter(application -> teacherId.equals(application.getTeacherId()))
                .orElseThrow(() -> new TeacherReviewFailure(NOT_FOUND));
        if (applications.existsByTeacherIdAndStatus(teacherId, "PENDING"))
            throw new TeacherReviewFailure(ALREADY_PENDING);
        var latest = applications.findFirstByTeacherIdOrderBySubmittedAtDescIdDesc(teacherId).orElseThrow();
        if (!target.getId().equals(latest.getId()) || !"REJECTED".equals(target.getStatus())
                || applications.existsByTeacherIdAndStatus(teacherId, "APPROVED"))
            throw new TeacherReviewFailure(RESUBMISSION_NOT_ALLOWED);
        if (profiles.existsById(teacherId)) throw new TeacherReviewFailure(PROFILE_EXISTS);
        var now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        // Keep latest-by-submission ordering valid even if the clock moves backwards.
        if (now.isBefore(latest.getSubmittedAt())) now = latest.getSubmittedAt();
        var application = applications.saveAndFlush(new TeacherApplication(
                teacherId, specialization, teachingExperience, introduction, now));
        return TeacherOnboardingStatus.from(application);
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
