package com.englishlearning.features.teacher.service;

import com.englishlearning.features.teacher.dto.TeacherApplicationDetail;
import com.englishlearning.features.teacher.repository.*;
import com.englishlearning.features.user.service.*;
import com.englishlearning.features.user.entity.User.Role;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import static com.englishlearning.features.teacher.service.TeacherReviewFailure.Reason.*;

@Service
public class TeacherApplicationReviewService {
    private final TeacherApplicationRepository applications;
    private final TeacherProfileRepository profiles;
    private final UserCredentialService users;
    private final CurrentAccountService accounts;
    private final Clock clock;
    public TeacherApplicationReviewService(TeacherApplicationRepository applications, TeacherProfileRepository profiles,
            UserCredentialService users, CurrentAccountService accounts, @Qualifier("refreshSessionClock") Clock clock) {
        this.applications = applications; this.profiles = profiles; this.users = users; this.accounts = accounts; this.clock = clock;
    }
    @Transactional(readOnly = true)
    public TeacherApplicationDetail detail(Long reviewerId, Long applicationId) {
        requireReviewer(accounts.findById(reviewerId).orElse(null));
        return TeacherApplicationDetail.from(applications.findById(applicationId)
                .orElseThrow(() -> new TeacherReviewFailure(NOT_FOUND)));
    }
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void review(Long reviewerId, Long applicationId, boolean approve, String reason) {
        if (!approve && (reason == null || reason.isBlank() || reason.indexOf(0) >= 0))
            throw new TeacherReviewFailure(INVALID_REQUEST);
        requireReviewer(accounts.findById(reviewerId).orElse(null));
        Long teacherId = applications.findTeacherId(applicationId).orElseThrow(() -> new TeacherReviewFailure(NOT_FOUND));
        // Lock accounts in stable order, then application; scalar account reads avoid stale managed state.
        Map<Long, AccountSnapshot> locked = new HashMap<>();
        for (Long id : new TreeSet<>(List.of(reviewerId, teacherId)))
            locked.put(id, users.lockAccountById(id).orElse(null));
        requireReviewer(locked.get(reviewerId));
        var application = applications.lockById(applicationId).orElseThrow(() -> new TeacherReviewFailure(NOT_FOUND));
        if (!"PENDING".equals(application.getStatus())) throw new TeacherReviewFailure(ALREADY_REVIEWED);
        var teacher = locked.get(teacherId);
        if (!teacherId.equals(application.getTeacherId()) || teacher == null || teacher.role() != Role.TEACHER)
            throw new TeacherReviewFailure(INVALID_APPLICANT);
        var now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        if (approve && profiles.insert(teacherId, application.getSpecialization(), application.getTeachingExperience(),
                application.getIntroduction(), now) != 1) throw new TeacherReviewFailure(PROFILE_EXISTS);
        application.review(reviewerId, approve, reason, now);
        applications.saveAndFlush(application);
        // Spring's transaction interceptor commits before the controller can return success.
    }
    private static void requireReviewer(AccountSnapshot account) {
        if (account == null || account.role() != Role.ADMIN || account.locked() || !account.emailVerified())
            throw new TeacherReviewFailure(DENIED);
    }
}
