package com.englishlearning.features.auth.service;

import com.englishlearning.features.auth.repository.PasswordResetRepository;
import com.englishlearning.features.auth.security.PasswordResetTokens;
import com.englishlearning.features.auth.security.PasswordInput;
import com.englishlearning.features.user.service.UserCredentialService;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.*;
import org.springframework.transaction.support.*;

@Service
public class PasswordRecoveryService {
    private final UserCredentialService users;
    private final PasswordResetRepository resets;
    private final PasswordResetTokens tokens;
    private final PasswordEncoder passwords;
    private final PasswordResetMail mail;
    private final Clock clock;
    private final TransactionTemplate transaction;
    public PasswordRecoveryService(UserCredentialService users,PasswordResetRepository resets,PasswordResetTokens tokens,
            PasswordEncoder passwords,PasswordResetMail mail,@Qualifier("refreshSessionClock") Clock clock,PlatformTransactionManager manager) {
        this.users=users;this.resets=resets;this.tokens=tokens;this.passwords=passwords;this.mail=mail;this.clock=clock;
        transaction=new TransactionTemplate(manager);transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }
    public void forgot(String email) {
        Delivery delivery=committed(()->{
            var found=users.findByEmail(email).orElse(null);
            if(found==null) return null;
            var user=users.lockById(found.id()).orElse(null);
            var now=clock.instant().truncatedTo(ChronoUnit.MICROS);
            if(user==null || user.locked() || !user.email().equals(found.email()) || !resets.mayIssue(user.id(),now)) return null;
            String token=tokens.generate();resets.invalidateCredentials(user.id(),now);resets.issue(user.id(),tokens.hash(user.id(),now,token),now);
            return new Delivery(user.email(),token);
        });
        if(delivery!=null) {
            try { mail.sendReset(delivery.email(),delivery.token()); }
            catch(RuntimeException ignored) { /* Delivery failure must not disclose account existence. */ }
        }
    }
    public void reset(String email,String code,String password) {
        PasswordInput.validate(password);
        boolean accepted=committed(()->{
            var found=users.findByEmail(email).orElse(null);
            if(found==null) return false;
            var user=users.lockById(found.id()).orElse(null);
            if(user==null || user.locked() || !user.email().equals(found.email())) return false;
            var credential=resets.latest(user.id()).orElse(null);
            var now=clock.instant().truncatedTo(ChronoUnit.MICROS);
            if(credential==null || credential.used() || credential.failures()>=5 || !now.isBefore(credential.expiresAt())) return false;
            if(!tokens.matches(user.id(),credential.issuedAt(),code,credential.hash())) {
                resets.failedAttempt(credential.id());return false;
            }
            users.changePassword(user.id(),passwords.encode(password),now);resets.invalidateAll(user.id(),now);
            return true;
        });
        // Invalid attempts must commit before the safe rejection is raised.
        if(!accepted) throw failure(VerificationFailure.Reason.INVALID);
    }
    private <T> T committed(Supplier<T> work) {
        if(TransactionSynchronizationManager.isActualTransactionActive()) throw failure(VerificationFailure.Reason.INTERNAL);
        boolean[] flushed={false};
        try { return transaction.execute(status->{T result=work.get();resets.flush();flushed[0]=true;return result;}); }
        catch(VerificationFailure known) { throw known; }
        catch(RuntimeException failure) {
            if(flushed[0]) throw failure(VerificationFailure.Reason.OUTCOME_UNKNOWN);
            if(failure instanceof DataAccessResourceFailureException || failure instanceof TransientDataAccessException
                    || failure instanceof CannotCreateTransactionException || failure instanceof TransactionTimedOutException)
                throw failure(VerificationFailure.Reason.UNAVAILABLE);
            throw failure(VerificationFailure.Reason.INTERNAL);
        }
    }
    private static VerificationFailure failure(VerificationFailure.Reason reason) { return new VerificationFailure(reason); }
    private record Delivery(String email,String token) { @Override public String toString(){return "ResetDelivery[REDACTED]";} }
}
