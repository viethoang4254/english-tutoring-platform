package com.englishlearning.features.auth.service;

import com.englishlearning.features.user.service.CredentialSnapshot;

final class AuthenticationEligibility {
    private AuthenticationEligibility() {}
    static void requireLogin(CredentialSnapshot account) {
        if (account == null || account.locked()) {
            throw new AuthenticationFailure(AuthenticationFailure.Reason.INVALID_CREDENTIAL);
        }
        if (!account.loginEligible()) {
            throw new AuthenticationFailure(account.role() == com.englishlearning.features.user.entity.User.Role.TEACHER
                    ? AuthenticationFailure.Reason.TEACHER_APPROVAL_REQUIRED : AuthenticationFailure.Reason.EMAIL_VERIFICATION_REQUIRED);
        }
    }
    static void require(CredentialSnapshot account) {
        if (account == null || !account.loginEligible()) {
            throw new AuthenticationFailure(AuthenticationFailure.Reason.INVALID_CREDENTIAL);
        }
        // Account authentication does not grant Teacher business eligibility.
    }
}
