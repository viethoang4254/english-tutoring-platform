package com.englishlearning.features.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.englishlearning.features.auth.repository.RefreshSessionRepository;
import com.englishlearning.features.auth.security.RefreshTokens;
import com.englishlearning.features.user.entity.User.Role;
import com.englishlearning.features.user.service.AccountSnapshot;
import com.englishlearning.features.user.service.CurrentAccountService;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.*;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class RefreshSessionServiceTests {
    final RefreshSessionRepository sessions = mock(RefreshSessionRepository.class);
    final CurrentAccountService accounts = mock(CurrentAccountService.class);
    final RefreshTokens tokens = new RefreshTokens();
    final PlatformTransactionManager manager = mock(PlatformTransactionManager.class);
    final TransactionStatus status = new SimpleTransactionStatus();
    final Clock clock = Clock.fixed(Instant.parse("2026-10-11T00:00:00Z"), ZoneOffset.UTC);

    RefreshSessionService service() {
        when(manager.getTransaction(any())).thenReturn(status);
        when(accounts.findById(1L)).thenReturn(Optional.of(new AccountSnapshot(1L, Role.STUDENT, false, false, false)));
        when(sessions.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        return new RefreshSessionService(sessions, accounts, tokens, clock, Duration.ofDays(7), manager);
    }
    @Test void returnsOnlyAfterCommitAndUsesReadCommitted() {
        var service = service();
        boolean[] committed = {false};
        doAnswer(call -> { committed[0] = true; return null; }).when(manager).commit(status);
        var result = service.create(1L);
        assertTrue(committed[0]);
        assertEquals(clock.instant().plus(Duration.ofDays(7)), result.expiresAt());
        assertEquals("RefreshCredential[REDACTED]", result.toString());
        verify(manager).getTransaction(argThat(def -> def.getIsolationLevel() == TransactionDefinition.ISOLATION_READ_COMMITTED));
        verify(manager).commit(status);
    }
    @Test void commitFailureReturnsNoCredentialAndDoesNotRetry() {
        var service = service();
        doThrow(new TransactionSystemException("PRIVATE connection secret")).when(manager).commit(status);
        var failure = assertThrows(RefreshFailure.class, () -> service.create(1L));
        assertEquals(RefreshFailure.Reason.COMMIT_UNCERTAIN, failure.reason());
        assertNull(failure.getCause());
        assertFalse(failure.toString().contains("PRIVATE"));
        verify(manager).commit(status);
        verify(sessions, times(1)).saveAndFlush(any());
    }
    @Test void knownResourceFailureRollsBackWithoutRetry() {
        var service = service();
        when(accounts.findById(1L)).thenThrow(new DataAccessResourceFailureException("PRIVATE SQL"));
        var failure = assertThrows(RefreshFailure.class, () -> service.create(1L));
        assertEquals(RefreshFailure.Reason.UNAVAILABLE, failure.reason());
        assertNull(failure.getCause());
        verify(manager).rollback(status);
        verify(manager, never()).commit(any());
    }
    @Test void lockAndTransactionTimeoutsFailClosedWithoutRetry() {
        for (RuntimeException cause : new RuntimeException[]{
                new org.springframework.dao.CannotAcquireLockException("PRIVATE lock"),
                new TransactionTimedOutException("PRIVATE timeout")}) {
            reset(accounts, sessions, manager);
            var service = service();
            when(accounts.findById(1L)).thenThrow(cause);
            var failure = assertThrows(RefreshFailure.class, () -> service.create(1L));
            assertEquals(RefreshFailure.Reason.UNAVAILABLE, failure.reason());
            assertNull(failure.getCause());
            verify(manager).rollback(status);
            verify(accounts, times(1)).findById(1L);
        }
    }
    @Test void connectionFailureBeforeTransactionReturnsUnavailable() {
        var service = service();
        when(manager.getTransaction(any())).thenThrow(new CannotCreateTransactionException("PRIVATE password"));
        assertEquals(RefreshFailure.Reason.UNAVAILABLE,
                assertThrows(RefreshFailure.class, () -> service.create(1L)).reason());
        verifyNoInteractions(sessions);
    }
    @Test void insertFailureIsSafeInternalFailure() {
        var service = service();
        when(sessions.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("PRIVATE token_hash"));
        var failure = assertThrows(RefreshFailure.class, () -> service.create(1L));
        assertEquals(RefreshFailure.Reason.INTERNAL_FAILURE, failure.reason());
        assertNull(failure.getCause());
        verify(manager).rollback(status);
    }
    @Test void rejectsAmbientTransactionBeforeAnyWrite() {
        var service = service();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            assertEquals(RefreshFailure.Reason.INTERNAL_FAILURE,
                    assertThrows(RefreshFailure.class, () -> service.create(1L)).reason());
            verifyNoInteractions(sessions);
        } finally { TransactionSynchronizationManager.setActualTransactionActive(false); }
    }
    @Test void malformedCredentialNeverStartsATransaction() {
        var service = service();
        assertEquals(RefreshFailure.Reason.INVALID_CREDENTIAL,
                assertThrows(RefreshFailure.class, () -> service.rotate("PRIVATE malformed")).reason());
        verify(manager, never()).getTransaction(any());
        verifyNoInteractions(sessions);
    }
    @Test void rejectsInvalidLifetime() {
        for (Duration lifetime : new Duration[]{Duration.ZERO, Duration.ofSeconds(-1), Duration.ofNanos(1)}) {
            assertThrows(IllegalArgumentException.class, () ->
                    new RefreshSessionService(sessions, accounts, tokens, clock, lifetime, manager));
        }
    }
}
