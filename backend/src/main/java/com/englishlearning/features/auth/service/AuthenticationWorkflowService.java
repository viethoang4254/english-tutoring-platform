package com.englishlearning.features.auth.service;

import com.englishlearning.features.auth.security.AccessTokenIssuer;
import com.englishlearning.features.auth.security.PasswordInput;
import com.englishlearning.features.user.service.UserCredentialService;
import java.util.UUID;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionTimedOutException;

public final class AuthenticationWorkflowService {
    private final UserCredentialService users;
    private final RefreshSessionService sessions;
    private final PasswordEncoder passwords;
    private final AccessTokenIssuer issuer;
    private final String dummyHash;

    public AuthenticationWorkflowService(UserCredentialService users, RefreshSessionService sessions,
            PasswordEncoder passwords, AccessTokenIssuer issuer) {
        this.users = users;
        this.sessions = sessions;
        this.passwords = passwords;
        this.issuer = issuer;
        this.dummyHash = passwords.encode(UUID.randomUUID().toString());
    }

    public AuthenticationResult login(String email, String password) {
        try {
            if (!PasswordInput.isValid(password)) throw invalid();
            var account = users.findByEmail(email).orElse(null);
            boolean matches = passwords.matches(password, account == null ? dummyHash : account.passwordHash());
            if (account == null || !matches) throw invalid();
            AuthenticationEligibility.requireLogin(account);
            return sessions.createAuthenticated(account, users, issuer);
        } catch (RuntimeException failure) { throw safe(failure); }
    }

    public AuthenticationResult refresh(String credential) {
        try { return sessions.rotateAuthenticated(credential, users, issuer); }
        catch (RuntimeException failure) { throw safe(failure); }
    }

    public void logout(String credential) {
        try { sessions.revokeForLogout(credential); }
        catch (RuntimeException failure) { throw safe(failure); }
    }

    private static AuthenticationFailure invalid() {
        return new AuthenticationFailure(AuthenticationFailure.Reason.INVALID_CREDENTIAL);
    }
    private static AuthenticationFailure safe(RuntimeException failure) {
        if (failure instanceof AuthenticationFailure known) return known;
        if (failure instanceof RefreshFailure refresh) {
            return new AuthenticationFailure(AuthenticationFailure.Reason.valueOf(refresh.reason().name()));
        }
        if (failure instanceof DataAccessResourceFailureException || failure instanceof TransientDataAccessException
                || failure instanceof CannotCreateTransactionException || failure instanceof TransactionTimedOutException) {
            return new AuthenticationFailure(AuthenticationFailure.Reason.UNAVAILABLE);
        }
        return new AuthenticationFailure(AuthenticationFailure.Reason.INTERNAL_FAILURE);
    }
}
