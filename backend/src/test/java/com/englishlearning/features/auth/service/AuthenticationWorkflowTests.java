package com.englishlearning.features.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.englishlearning.features.auth.entity.RefreshSession;
import com.englishlearning.features.auth.repository.RefreshSessionRepository;
import com.englishlearning.features.auth.security.*;
import com.englishlearning.features.user.service.*;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.*;
import org.springframework.dao.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.*;
import org.springframework.transaction.support.*;

class AuthenticationWorkflowTests {
    final UserCredentialService users = mock(UserCredentialService.class);
    final RefreshSessionRepository repository = mock(RefreshSessionRepository.class);
    final CurrentAccountService accounts = mock(CurrentAccountService.class);
    final AccessTokenIssuer issuer = mock(AccessTokenIssuer.class);
    final PasswordEncoder passwords = mock(PasswordEncoder.class);
    final PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
    final TransactionStatus tx = new SimpleTransactionStatus();
    final RefreshTokens tokens = new RefreshTokens();
    final Instant now = Instant.parse("2026-10-12T00:00:00Z");
    final CredentialSnapshot account = new CredentialSnapshot(1L, "a@example.invalid", "HASH", false, true);
    final String raw = " valid password ";
    AuthenticationWorkflowService workflow;
    String refresh;

    @BeforeEach void setup() {
        when(manager.getTransaction(any())).thenReturn(tx);
        when(passwords.encode(any())).thenReturn("DUMMY");
        when(passwords.matches(raw, "HASH")).thenReturn(true);
        when(users.findByEmail(any())).thenReturn(Optional.of(account));
        when(users.lockById(1L)).thenReturn(Optional.of(account));
        when(repository.saveAndFlush(any())).thenAnswer(c -> c.getArgument(0));
        when(issuer.issue(1L)).thenReturn(new AccessToken("PRIVATE JWT", now, now.plusSeconds(900)));
        var sessions = new RefreshSessionService(repository, accounts, tokens, Clock.fixed(now, ZoneOffset.UTC),
                Duration.ofDays(7), manager);
        workflow = new AuthenticationWorkflowService(users, sessions, passwords, issuer);
        refresh = tokens.generate();
        when(repository.lockByTokenHash(tokens.hash(refresh))).thenReturn(Optional.of(
                new RefreshSession(1L, tokens.hash(refresh), now.minusSeconds(10), now.plusSeconds(10))));
    }
    AuthenticationFailure failure(AuthenticationFailure.Reason reason, Runnable operation) {
        var e = assertThrows(AuthenticationFailure.class, operation::run);
        assertEquals(reason, e.reason());
        assertNull(e.getCause());
        assertFalse(e.toString().contains("PRIVATE"));
        return e;
    }

    @Test void loginRechecksSignsAndCommitsBeforeReturning() {
        boolean[] committed = {false};
        doAnswer(c -> { committed[0] = true; return null; }).when(manager).commit(tx);
        var result = workflow.login(account.email(), raw);
        assertTrue(committed[0]);
        assertEquals("AuthenticationResult[REDACTED]", result.toString());
        assertEquals("CredentialSnapshot[REDACTED]", account.toString());
        var order = inOrder(users, passwords, issuer, repository, manager);
        order.verify(users).findByEmail(account.email());
        order.verify(passwords).matches(raw, "HASH");
        order.verify(users).lockById(1L);
        order.verify(issuer).issue(1L);
        order.verify(repository).saveAndFlush(any());
        order.verify(manager).commit(tx);
        verifyNoInteractions(accounts);
    }
    @Test void unknownAccountPerformsDummyComparison() {
        when(users.findByEmail(any())).thenReturn(Optional.empty());
        failure(AuthenticationFailure.Reason.INVALID_CREDENTIAL, () -> workflow.login("absent", raw));
        verify(passwords).matches(raw, "DUMMY");
        verifyNoInteractions(issuer, repository, manager);
    }
    @Test void wrongPasswordNeverSignsOrStartsTransaction() {
        failure(AuthenticationFailure.Reason.INVALID_CREDENTIAL, () -> workflow.login("a", "wrong password"));
        verifyNoInteractions(issuer, repository, manager);
    }
    @Test void malformedPasswordNeverLooksUpCredentials() {
        failure(AuthenticationFailure.Reason.INVALID_CREDENTIAL, () -> workflow.login("a", null));
        failure(AuthenticationFailure.Reason.INVALID_CREDENTIAL, () -> workflow.login("a", "short"));
        verifyNoInteractions(users, issuer, repository, manager);
    }
    @Test void missingLockedAndUnverifiedAccountsAreRejectedOnRecheck() {
        for (var current : java.util.List.of(Optional.<CredentialSnapshot>empty(),
                Optional.of(new CredentialSnapshot(1L, account.email(), "HASH", true, true)),
                Optional.of(new CredentialSnapshot(1L, account.email(), "HASH", false, false)))) {
            when(users.lockById(1L)).thenReturn(current);
            failure(current.isPresent() && !current.get().locked() && !current.get().emailVerified()
                    ? AuthenticationFailure.Reason.EMAIL_VERIFICATION_REQUIRED
                    : AuthenticationFailure.Reason.INVALID_CREDENTIAL, () -> workflow.login("a", raw));
        }
        verifyNoInteractions(issuer);
        verify(repository, never()).saveAndFlush(any());
        verify(manager, times(3)).rollback(tx);
    }
    @Test void changedPasswordOrEmailCannotIssueFromStaleCredentialCheck() {
        for (var current : java.util.List.of(
                new CredentialSnapshot(1L, account.email(), "NEW HASH", false, true),
                new CredentialSnapshot(1L, "changed@example.invalid", "HASH", false, true))) {
            when(users.lockById(1L)).thenReturn(Optional.of(current));
            failure(AuthenticationFailure.Reason.INVALID_CREDENTIAL, () -> workflow.login("a", raw));
        }
        verifyNoInteractions(issuer);
    }
    @Test void initiallyIneligibleAccountNeverSigns() {
        for (var current : java.util.List.of(
                new CredentialSnapshot(1L, account.email(), "HASH", true, true),
                new CredentialSnapshot(1L, account.email(), "HASH", false, false))) {
            when(users.findByEmail(any())).thenReturn(Optional.of(current));
            failure(current.locked() ? AuthenticationFailure.Reason.INVALID_CREDENTIAL
                    : AuthenticationFailure.Reason.EMAIL_VERIFICATION_REQUIRED, () -> workflow.login("a", raw));
        }
        verifyNoInteractions(issuer, manager);
    }
    @Test void refreshSignsBeforePersistenceAndCommit() {
        var result = workflow.refresh(refresh);
        assertNotNull(result);
        var order = inOrder(repository, users, issuer, manager);
        order.verify(repository).lockByTokenHash(tokens.hash(refresh));
        order.verify(users).lockById(1L);
        order.verify(issuer).issue(1L);
        order.verify(repository).saveAndFlush(any());
        order.verify(manager).commit(tx);
    }
    @Test void refreshIneligibilityAndMalformedCredentialsNeverSign() {
        when(users.lockById(1L)).thenReturn(Optional.of(
                new CredentialSnapshot(1L, account.email(), "HASH", false, false)));
        failure(AuthenticationFailure.Reason.INVALID_CREDENTIAL, () -> workflow.refresh(refresh));
        failure(AuthenticationFailure.Reason.INVALID_CREDENTIAL, () -> workflow.refresh("bad"));
        verifyNoInteractions(issuer);
        verify(repository, never()).saveAndFlush(any());
    }
    @Test void signingFailureRollsBackLoginAndRefreshWithoutWrites() {
        when(issuer.issue(1L)).thenThrow(new IllegalStateException("PRIVATE KEY"));
        failure(AuthenticationFailure.Reason.INTERNAL_FAILURE, () -> workflow.login("a", raw));
        failure(AuthenticationFailure.Reason.INTERNAL_FAILURE, () -> workflow.refresh(refresh));
        verify(manager, times(2)).rollback(tx);
        verify(manager, never()).commit(any());
        verify(repository, never()).saveAndFlush(any());
    }
    @Test void persistenceFailureRollsBackAndNeverReturnsSignedTokens() {
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("PRIVATE HASH"));
        failure(AuthenticationFailure.Reason.INTERNAL_FAILURE, () -> workflow.refresh(refresh));
        verify(manager).rollback(tx);
        verify(manager, never()).commit(any());
        verify(issuer).issue(1L);
    }
    @Test void uncertainCommitReturnsNoResultAndNeverRetries() {
        doThrow(new TransactionSystemException("PRIVATE CONNECTION")).when(manager).commit(tx);
        failure(AuthenticationFailure.Reason.COMMIT_UNCERTAIN, () -> workflow.refresh(refresh));
        verify(manager).commit(tx);
        verify(repository).saveAndFlush(any());
        verify(issuer).issue(1L);
    }
    @Test void lookupAndTransactionFailuresAreSafe() {
        when(users.findByEmail(any())).thenThrow(new DataAccessResourceFailureException("PRIVATE SQL"));
        failure(AuthenticationFailure.Reason.UNAVAILABLE, () -> workflow.login("a", raw));
        doThrow(new IllegalStateException("PRIVATE SQL")).when(users).findByEmail(any());
        failure(AuthenticationFailure.Reason.INTERNAL_FAILURE, () -> workflow.login("a", raw));
        when(manager.getTransaction(any())).thenThrow(new CannotCreateTransactionException("PRIVATE"));
        failure(AuthenticationFailure.Reason.UNAVAILABLE, () -> workflow.refresh(refresh));
    }
    @Test void ambientTransactionIsRejectedBeforeIssuance() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try { failure(AuthenticationFailure.Reason.INTERNAL_FAILURE, () -> workflow.refresh(refresh)); }
        finally { TransactionSynchronizationManager.setActualTransactionActive(false); }
        verifyNoInteractions(issuer, repository);
    }
    @Test void logoutRevokesTargetWithoutAccountEligibilityLookup() {
        var row=new RefreshSession(1L,tokens.hash(refresh),now.minusSeconds(1),now.plusSeconds(60));
        when(repository.lockByTokenHash(tokens.hash(refresh))).thenReturn(Optional.of(row));
        workflow.logout(refresh);
        assertFalse(row.isUsableAt(now));
        verify(manager).commit(tx);
        verifyNoInteractions(users,issuer,accounts);
        verify(repository,never()).saveAndFlush(any());
    }
    @Test void logoutMissingMalformedAndUnknownCredentialsAreIdempotent() {
        workflow.logout(null); workflow.logout("bad");
        verifyNoInteractions(manager,repository);
        workflow.logout(tokens.generate());
        verify(manager).commit(tx);
        verifyNoInteractions(users,issuer);
    }
    @Test void logoutExpiredOrRevokedCredentialsDoNotCreateReplacement() {
        var expired=new RefreshSession(1L,tokens.hash(refresh),now.minusSeconds(60),now);
        when(repository.lockByTokenHash(tokens.hash(refresh))).thenReturn(Optional.of(expired));
        workflow.logout(refresh);
        var revoked=new RefreshSession(1L,tokens.hash(refresh),now.minusSeconds(60),now.plusSeconds(60));
        revoked.revoke(now.minusSeconds(1));
        when(repository.lockByTokenHash(tokens.hash(refresh))).thenReturn(Optional.of(revoked));
        workflow.logout(refresh);
        verify(manager,times(2)).commit(tx);
        verify(repository,never()).saveAndFlush(any());
        verifyNoInteractions(users,issuer);
    }
    @Test void logoutDatabaseFailureAndUnknownCommitNeverReportSuccess() {
        when(repository.lockByTokenHash(any())).thenThrow(new DataAccessResourceFailureException("PRIVATE"));
        failure(AuthenticationFailure.Reason.UNAVAILABLE,()->workflow.logout(refresh));
        doReturn(Optional.empty()).when(repository).lockByTokenHash(any());
        doThrow(new TransactionSystemException("PRIVATE")).when(manager).commit(tx);
        failure(AuthenticationFailure.Reason.COMMIT_UNCERTAIN,()->workflow.logout(refresh));
        verify(manager).rollback(tx);
        verify(manager).commit(tx);
    }

}
