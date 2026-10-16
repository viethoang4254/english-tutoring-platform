package com.englishlearning.features.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.englishlearning.features.auth.repository.PasswordResetRepository;
import com.englishlearning.features.auth.security.*;
import com.englishlearning.features.user.service.*;
import com.englishlearning.features.user.entity.User.Role;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.*;

class PasswordRecoveryTests {
    final UserCredentialService users=mock(UserCredentialService.class);
    final PasswordResetRepository resets=mock(PasswordResetRepository.class);
    final PasswordEncoder passwords=mock(PasswordEncoder.class);
    final PasswordResetMail mail=mock(PasswordResetMail.class);
    final PlatformTransactionManager manager=mock(PlatformTransactionManager.class);
    final TransactionStatus tx=mock(TransactionStatus.class);
    final PasswordResetTokens tokens=new PasswordResetTokens(java.util.Base64.getEncoder().encodeToString(new byte[32]));
    final Instant now=Instant.parse("2026-10-15T00:00:00Z");
    PasswordRecoveryService service() {
        when(manager.getTransaction(any())).thenReturn(tx);
        var user=new CredentialSnapshot(1L,"a@example.invalid","hash",false,false,Role.STUDENT,true);
        when(users.findByEmail(any())).thenReturn(Optional.of(user));when(users.lockById(1L)).thenReturn(Optional.of(user));
        when(resets.mayIssue(1L,now)).thenReturn(true);
        when(resets.latest(1L)).thenReturn(Optional.of(new PasswordResetRepository.Credential(1L,tokens.hash(1L,now,"000123"),now,now.plusSeconds(900),false,0)));when(passwords.encode(any())).thenReturn("encoded");
        return new PasswordRecoveryService(users,resets,tokens,passwords,mail,Clock.fixed(now,ZoneOffset.UTC),manager);
    }
    @Test void tokenIsCanonicalPurposeBoundAndContainsNoRawSecret() {
        assertTrue(tokens.generate().matches("[0-9]{6}"));
        assertEquals(64,tokens.hash(1L,now,"000123").length());
        assertNotEquals(tokens.hash(1L,now,"000123"),tokens.hash(2L,now,"000123"));
        assertNotEquals(tokens.hash(1L,now,"000123"),tokens.hash(1L,now.plusSeconds(1),"000123"));
        assertThrows(IllegalStateException.class,()->new PasswordResetTokens(""));
        assertFalse(tokens.matches(1L,now,"123",tokens.hash(1L,now,"000123")));
    }
    @Test void wrongAttemptCommitsBeforeRejection() {
        var service=service();
        assertEquals(VerificationFailure.Reason.INVALID,assertThrows(VerificationFailure.class,()->service.reset("a@example.invalid","000124","password")).reason());
        var order=inOrder(resets,manager);order.verify(resets).failedAttempt(1L);order.verify(resets).flush();order.verify(manager).commit(tx);
        verify(users,never()).changePassword(any(),any(),any());
    }
    @Test void mailIsAfterCommitAndFailureIsNeutral() {
        var service=service();doThrow(new IllegalStateException("private body")).when(mail).sendReset(any(),any());
        assertDoesNotThrow(()->service.forgot("a@example.invalid"));
        var order=inOrder(resets,manager,mail);order.verify(resets).issue(eq(1L),anyString(),eq(now));order.verify(resets).flush();order.verify(manager).commit(tx);order.verify(mail).sendReset(any(),any());
    }
    @Test void rollbackDoesNotReturnSuccessOrSendMail() {
        var service=service();doThrow(new IllegalStateException("private SQL")).when(resets).invalidateAll(any(),any());
        var failure=assertThrows(VerificationFailure.class,()->service.reset("a@example.invalid","000123","password"));
        assertEquals(VerificationFailure.Reason.INTERNAL,failure.reason());assertNull(failure.getCause());verify(manager).rollback(tx);verifyNoInteractions(mail);
    }
    @Test void uncertainCommitIsNotSuccessAndNeverSendsMail() {
        var service=service();doThrow(new TransactionSystemException("private connection")).when(manager).commit(tx);
        assertEquals(VerificationFailure.Reason.OUTCOME_UNKNOWN,assertThrows(VerificationFailure.class,()->service.forgot("a@example.invalid")).reason());verifyNoInteractions(mail);
    }
    @Test void databaseFailureIsSafeAndNoResetOccurs() {
        var service=service();when(manager.getTransaction(any())).thenThrow(new CannotCreateTransactionException("private target"));
        assertEquals(VerificationFailure.Reason.UNAVAILABLE,assertThrows(VerificationFailure.class,()->service.forgot("a@example.invalid")).reason());verifyNoInteractions(resets,mail);
    }
}
