package com.englishlearning.features.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.englishlearning.features.auth.repository.EmailVerificationRepository;
import com.englishlearning.features.auth.security.VerificationTokens;
import com.englishlearning.features.user.service.UserCredentialService;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.*;

class StudentRegistrationServiceTests {
    final UserCredentialService users=mock(UserCredentialService.class);
    final EmailVerificationRepository credentials=mock(EmailVerificationRepository.class);
    final PasswordEncoder passwords=mock(PasswordEncoder.class);
    final VerificationMail mail=mock(VerificationMail.class);
    final PlatformTransactionManager manager=mock(PlatformTransactionManager.class);
    final TransactionStatus status=mock(TransactionStatus.class);
    final Instant now=Instant.parse("2026-10-12T00:00:00Z");
    StudentRegistrationService service() {
        when(manager.getTransaction(any())).thenReturn(status);
        when(passwords.encode(any())).thenReturn("encoded-test-password");
        when(users.createStudent(anyString(),anyString(),anyString(),any())).thenReturn(Optional.of(1L));
        return new StudentRegistrationService(users,credentials,new VerificationTokens(),passwords,mail,
                Clock.fixed(now,ZoneOffset.UTC),manager);
    }
    @Test void persistenceCommitsBeforeDeliveryAndCanonicalizesWithoutChangingPassword() {
        service().register(" Person+tag@Example.invalid "," test password ","Test");
        var order=inOrder(users,credentials,manager,mail);
        order.verify(users).createStudent(eq("person+tag@example.invalid"),anyString(),eq("Test"),eq(now));
        order.verify(credentials).saveAndFlush(any()); order.verify(credentials).flush();
        order.verify(manager).commit(status); order.verify(mail).send(eq("person+tag@example.invalid"),anyString());
        verify(passwords).encode(" test password ");
    }
    @Test void deliveryFailureDoesNotRollbackPersistedAccount() {
        var service=service(); doThrow(new IllegalStateException("private mail content")).when(mail).send(any(),any());
        var error=assertThrows(VerificationFailure.class,()->service.register("a@example.invalid","password","Test"));
        assertEquals(VerificationFailure.Reason.DELIVERY,error.reason()); assertNull(error.getCause());
        verify(manager).commit(status); verify(manager,never()).rollback(any());
    }
    @Test void knownPersistenceFailureRollsBackAndDoesNotSend() {
        var service=service(); doThrow(new IllegalStateException("private SQL")).when(credentials).saveAndFlush(any());
        var error=assertThrows(VerificationFailure.class,()->service.register("a@example.invalid","password","Test"));
        assertEquals(VerificationFailure.Reason.INTERNAL,error.reason()); verify(manager).rollback(status);
        verifyNoInteractions(mail);
    }
    @Test void uncertainCommitDoesNotSendOrClaimSuccess() {
        var service=service(); doThrow(new TransactionSystemException("private SQL")).when(manager).commit(status);
        var error=assertThrows(VerificationFailure.class,()->service.register("a@example.invalid","password","Test"));
        assertEquals(VerificationFailure.Reason.OUTCOME_UNKNOWN,error.reason()); verifyNoInteractions(mail);
    }
    @Test void databaseUnavailableIsSafe() {
        var service=service(); when(manager.getTransaction(any())).thenThrow(new CannotCreateTransactionException("private endpoint"));
        var error=assertThrows(VerificationFailure.class,()->service.register("a@example.invalid","password","Test"));
        assertEquals(VerificationFailure.Reason.UNAVAILABLE,error.reason()); assertNull(error.getCause()); verifyNoInteractions(mail);
    }
    @Test void duplicateDoesNotOverwriteOrSend() {
        var service=service(); when(users.createStudent(any(),any(),any(),any())).thenReturn(Optional.empty());
        assertEquals(VerificationFailure.Reason.CONFLICT,assertThrows(VerificationFailure.class,
                ()->service.register("a@example.invalid","password","Test")).reason());
        verifyNoInteractions(credentials,mail); verify(manager).rollback(status);
    }
}
