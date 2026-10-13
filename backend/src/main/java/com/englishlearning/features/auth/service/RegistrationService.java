package com.englishlearning.features.auth.service;

import com.englishlearning.features.auth.entity.EmailVerificationCredential;
import com.englishlearning.features.teacher.entity.TeacherApplication;
import com.englishlearning.features.teacher.repository.TeacherApplicationRepository;
import com.englishlearning.features.auth.repository.EmailVerificationRepository;
import com.englishlearning.features.auth.security.VerificationTokens;
import com.englishlearning.features.auth.security.PasswordInput;
import com.englishlearning.features.user.service.UserCredentialService;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.*;
import org.springframework.transaction.support.*;

@Service
public class RegistrationService {
    private final TeacherApplicationRepository applications;
    private final UserCredentialService users;
    private final EmailVerificationRepository credentials;
    private final VerificationTokens tokens;
    private final PasswordEncoder passwords;
    private final VerificationMail mail;
    private final Clock clock;
    private final TransactionTemplate transaction;
    public RegistrationService(UserCredentialService users, EmailVerificationRepository credentials,
            VerificationTokens tokens, PasswordEncoder passwords, VerificationMail mail,
            @Qualifier("refreshSessionClock") Clock clock, PlatformTransactionManager manager, TeacherApplicationRepository applications) {
        this.applications = applications;
        this.users = users; this.credentials = credentials; this.tokens = tokens; this.passwords = passwords;
        this.mail = mail; this.clock = clock; transaction = new TransactionTemplate(manager);
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }
    public void register(String email, String password, String fullName) {
        PasswordInput.validate(password);
        String canonical = canonical(email);
        String hash = passwords.encode(password);
        Delivery delivery = committed(() -> {
            Instant now = now();
            Long id = users.createStudent(canonical, hash, fullName, now)
                    .orElseThrow(() -> failure(VerificationFailure.Reason.CONFLICT));
            return issue(id, canonical, now);
        });
        deliver(delivery);
    }
    public void registerTeacher(String email, String password, String fullName,
            String specialization, String teachingExperience, String introduction) {
        PasswordInput.validate(password);
        String canonical = canonical(email);
        String hash = passwords.encode(password);
        Delivery delivery = committed(() -> {
            Instant now = now();
            Long id = users.createTeacher(canonical, hash, fullName, now)
                    .orElseThrow(() -> failure(VerificationFailure.Reason.CONFLICT));
            Delivery result = issue(id, canonical, now);
            applications.saveAndFlush(new TeacherApplication(id, specialization, teachingExperience, introduction, now));
            return result;
        });
        deliver(delivery);
    }
    public void verify(String token) {
        final String hash;
        try { hash = tokens.hash(token); }
        catch (IllegalArgumentException invalid) { throw failure(VerificationFailure.Reason.INVALID); }
        committed(() -> {
            Long id = credentials.findUserId(hash).orElseThrow(() -> failure(VerificationFailure.Reason.INVALID));
            // All verification/resend mutations serialize on User before reading mutable credential state.
            var user = users.lockById(id).orElseThrow(() -> failure(VerificationFailure.Reason.INVALID));
            var credential = credentials.findBySecretHash(hash).orElseThrow(() -> failure(VerificationFailure.Reason.INVALID));
            Instant now = now();
            if (user.locked() || user.emailVerified() || !credential.usable(user.email(), now)) {
                throw failure(VerificationFailure.Reason.INVALID);
            }
            credential.consume(now);
            users.verifyEmail(id, now);
            return null;
        });
    }
    public void resend(String email) {
        Delivery delivery = committed(() -> {
            var found = users.findByEmail(canonical(email));
            if (found.isEmpty()) return null;
            var user = users.lockById(found.get().id()).orElse(null);
            if (user == null || user.locked() || user.emailVerified() || !user.email().equals(canonical(email))) return null;
            Instant now = now();
            Instant last = credentials.lastIssued(user.id());
            // This slice resends existing initial-verification credentials; it does not provision other accounts.
            if (last == null) return null;
            if (now.isBefore(last.plusSeconds(60)) || credentials.resendCount(user.id(), now.minusSeconds(86400)) >= 5) {
                throw failure(VerificationFailure.Reason.RATE_LIMITED);
            }
            return issue(user.id(), user.email(), now);
        });
        if (delivery != null) deliver(delivery);
    }
    private Delivery issue(Long id, String email, Instant now) {
        String token = tokens.generate();
        credentials.saveAndFlush(new EmailVerificationCredential(id, email, tokens.hash(token), now));
        return new Delivery(email, token);
    }
    private void deliver(Delivery delivery) {
        try { mail.send(delivery.email(), delivery.token()); }
        catch (RuntimeException failure) { throw failure(VerificationFailure.Reason.DELIVERY); }
    }
    private <T> T committed(Supplier<T> work) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) throw failure(VerificationFailure.Reason.INTERNAL);
        boolean[] flushed = {false};
        try {
            return transaction.execute(status -> {
                T result = work.get(); credentials.flush(); flushed[0] = true; return result;
            });
        } catch (VerificationFailure failure) { throw failure; }
        catch (RuntimeException exception) {
            if (flushed[0]) throw failure(VerificationFailure.Reason.OUTCOME_UNKNOWN);
            if (exception instanceof DataAccessResourceFailureException || exception instanceof TransientDataAccessException
                    || exception instanceof CannotCreateTransactionException || exception instanceof TransactionTimedOutException) {
                throw failure(VerificationFailure.Reason.UNAVAILABLE);
            }
            throw failure(VerificationFailure.Reason.INTERNAL);
        }
    }
    private Instant now() { return clock.instant().truncatedTo(ChronoUnit.MICROS); }
    private static String canonical(String email) { return email.trim().toLowerCase(Locale.ROOT); }
    private static VerificationFailure failure(VerificationFailure.Reason reason) { return new VerificationFailure(reason); }
    private record Delivery(String email, String token) {
        @Override public String toString() { return "VerificationDelivery[REDACTED]"; }
    }
}
