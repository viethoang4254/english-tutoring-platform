package com.englishlearning.features.auth.service;

import com.englishlearning.features.auth.entity.RefreshSession;
import com.englishlearning.features.auth.security.AccessTokenIssuer;
import com.englishlearning.features.user.service.CredentialSnapshot;
import com.englishlearning.features.user.service.UserCredentialService;
import com.englishlearning.features.auth.repository.RefreshSessionRepository;
import com.englishlearning.features.auth.security.RefreshTokens;
import com.englishlearning.features.user.service.CurrentAccountService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionTimedOutException;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class RefreshSessionService {
    private final RefreshSessionRepository sessions;
    private final CurrentAccountService accounts;
    private final RefreshTokens tokens;
    private final Clock clock;
    private final Duration lifetime;
    private final TransactionTemplate transaction;

    public RefreshSessionService(RefreshSessionRepository sessions, CurrentAccountService accounts,
            RefreshTokens tokens, @Qualifier("refreshSessionClock") Clock clock,
            @Value("${app.security.refresh.lifetime:7d}") Duration lifetime,
            PlatformTransactionManager manager) {
        if (lifetime.isNegative() || lifetime.isZero() || lifetime.toNanosPart() % 1000 != 0) {
            throw new IllegalArgumentException("Invalid refresh lifetime.");
        }
        this.sessions = sessions;
        this.accounts = accounts;
        this.tokens = tokens;
        this.clock = clock;
        this.lifetime = lifetime;
        this.transaction = new TransactionTemplate(manager);
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }
    // Internal persistence primitive; calling workflows must establish full authentication eligibility.
    public RefreshCredential create(Long userId) {
        return committed(() -> {
            checkAccount(userId);
            return insert(userId, now());
        });
    }
    public RefreshCredential rotate(String credential) {
        String hash = incomingHash(credential);
        return committed(() -> {
            RefreshSession old = sessions.lockByTokenHash(hash).orElseThrow(RefreshSessionService::invalid);
            Instant now = now(); // Sample after waiting for the database row lock.
            if (!old.isUsableAt(now)) throw invalid();
            checkAccount(old.getUserId());
            old.revoke(now);
            return insert(old.getUserId(), now);
        });
    }
    public void revoke(String credential) {
        String hash = incomingHash(credential);
        committed(() -> {
            RefreshSession session = sessions.lockByTokenHash(hash).orElseThrow(RefreshSessionService::invalid);
            Instant now = now();
            if (!session.isUsableAt(now)) throw invalid();
            checkAccount(session.getUserId());
            session.revoke(now);
            return null;
        });
    }
    void revokeForLogout(String credential) {
        final String hash;
        try { hash = tokens.hash(credential); }
        catch (IllegalArgumentException invalid) { return; }
        committed(() -> {
            var session = sessions.lockByTokenHash(hash).orElse(null);
            Instant now = now();
            if (session != null && session.isUsableAt(now)) session.revoke(now);
            // Revocation removes authority; locked-account eligibility must not prevent it.
            return null;
        });
    }
    AuthenticationResult createAuthenticated(CredentialSnapshot expected, UserCredentialService users,
            AccessTokenIssuer issuer) {
        return committed(() -> {
            var current = users.lockById(expected.id()).orElse(null);
            if (current == null || current.locked()) {
                throw new AuthenticationFailure(AuthenticationFailure.Reason.INVALID_CREDENTIAL);
            }
            if (!current.email().equals(expected.email())
                    || !current.passwordHash().equals(expected.passwordHash())) {
                throw new AuthenticationFailure(AuthenticationFailure.Reason.INVALID_CREDENTIAL);
            }
            AuthenticationEligibility.requireLogin(current);
            var access = issuer.issue(current.id());
            return new AuthenticationResult(access, insert(current.id(), now()));
        });
    }
    AuthenticationResult rotateAuthenticated(String credential, UserCredentialService users,
            AccessTokenIssuer issuer) {
        String hash = incomingHash(credential);
        return committed(() -> {
            // Lock order: refresh row, then User; future credential-revocation workflows must coordinate.
            var old = sessions.lockByTokenHash(hash).orElseThrow(RefreshSessionService::invalid);
            var current = users.lockById(old.getUserId()).orElse(null);
            Instant now = now();
            if (!old.isUsableAt(now)) throw invalid();
            AuthenticationEligibility.require(current);
            var access = issuer.issue(current.id());
            old.revoke(now);
            return new AuthenticationResult(access, insert(current.id(), now));
        });
    }
    private RefreshCredential insert(Long userId, Instant now) {
        String credential = tokens.generate();
        RefreshSession session = sessions.saveAndFlush(
                new RefreshSession(userId, tokens.hash(credential), now, now.plus(lifetime)));
        return new RefreshCredential(session.getId(), userId, credential, session.getExpiresAt());
    }
    private void checkAccount(Long userId) {
        if (userId == null) throw invalid();
        var account = accounts.findById(userId).orElseThrow(RefreshSessionService::invalid);
        if (account.locked()) throw invalid();
        // Email verification and Teacher onboarding policy remain with future workflows.
    }
    private String incomingHash(String credential) {
        try { return tokens.hash(credential); }
        catch (IllegalArgumentException exception) { throw invalid(); }
    }
    private Instant now() { return clock.instant().truncatedTo(ChronoUnit.MICROS); }
    private <T> T committed(Supplier<T> work) {
        // Joining an outer transaction could expose a replacement before its final commit.
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new RefreshFailure(RefreshFailure.Reason.INTERNAL_FAILURE);
        }
        boolean[] flushed = {false};
        try {
            return transaction.execute(status -> {
                T result = work.get();
                sessions.flush();
                flushed[0] = true;
                return result;
            }); // TransactionTemplate commits before returning to this caller.
        } catch (AuthenticationFailure failure) {
            throw failure;
        } catch (RefreshFailure failure) {
            throw failure;
        } catch (RuntimeException failure) {
            if (flushed[0]) throw new RefreshFailure(RefreshFailure.Reason.COMMIT_UNCERTAIN);
            if (failure instanceof DataAccessResourceFailureException
                    || failure instanceof TransientDataAccessException
                    || failure instanceof CannotCreateTransactionException
                    || failure instanceof TransactionTimedOutException) {
                throw new RefreshFailure(RefreshFailure.Reason.UNAVAILABLE);
            }
            throw new RefreshFailure(RefreshFailure.Reason.INTERNAL_FAILURE);
        }
    }
    private static RefreshFailure invalid() {
        return new RefreshFailure(RefreshFailure.Reason.INVALID_CREDENTIAL);
    }
}
