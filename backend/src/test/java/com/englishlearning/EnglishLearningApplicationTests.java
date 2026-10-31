package com.englishlearning;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// This skeleton test deliberately excludes database infrastructure.
// DatabaseConnectivityTests verifies the real datasource separately.
@SpringBootTest(properties = "spring.autoconfigure.exclude="
        + "org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration")
class EnglishLearningApplicationTests {
    @org.springframework.test.context.bean.override.mockito.MockitoBean com.englishlearning.features.auth.service.PasswordChangeService passwordChanges;
    @org.springframework.test.context.bean.override.mockito.MockitoBean com.englishlearning.features.user.service.StudentProfileService studentProfiles;
    @org.springframework.test.context.bean.override.mockito.MockitoBean com.englishlearning.features.user.repository.StudentProfileRepository profileRepository;
    @org.springframework.test.context.bean.override.mockito.MockitoBean com.englishlearning.features.category.repository.CategoryRepository categoryRepository;
    @org.springframework.test.context.bean.override.mockito.MockitoBean com.englishlearning.features.course.repository.CourseRepository courseRepository;
    @org.springframework.test.context.bean.override.mockito.MockitoBean com.englishlearning.features.enrollment.repository.EnrollmentRepository enrollmentRepository;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.englishlearning.features.teacher.service.TeacherApplicationReviewService teacherReviews;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.englishlearning.features.teacher.service.TeacherBusinessEligibility teacherEligibility;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.englishlearning.features.teacher.service.TeacherOnboardingService teacherOnboarding;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.englishlearning.features.auth.service.RegistrationService registration;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.englishlearning.features.auth.service.PasswordRecoveryService recovery;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.englishlearning.features.auth.repository.PasswordResetRepository resetRepository;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.englishlearning.features.user.repository.UserCredentialRepository credentialRepository;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.englishlearning.features.auth.security.AccessTokenIssuer accessTokenIssuer;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.englishlearning.features.auth.service.RefreshSessionService refreshSessions;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.englishlearning.features.user.service.CurrentAccountService accounts;

    @Test
    void contextLoads() {
    }
}
