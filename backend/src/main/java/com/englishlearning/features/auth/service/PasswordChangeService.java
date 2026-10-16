package com.englishlearning.features.auth.service;

import com.englishlearning.features.auth.repository.PasswordResetRepository;
import com.englishlearning.features.auth.repository.RefreshSessionRepository;
import com.englishlearning.features.auth.security.RefreshTokens;
import com.englishlearning.features.auth.security.PasswordInput;
import com.englishlearning.features.user.service.UserCredentialService;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.*;
import org.springframework.transaction.support.*;

@Service
public class PasswordChangeService {
    private final UserCredentialService users;
    private final RefreshSessionRepository sessions;
    private final PasswordResetRepository resets;
    private final PasswordEncoder passwords;
    private final Clock clock;
    private final TransactionTemplate transaction;
    public PasswordChangeService(UserCredentialService users,RefreshSessionRepository sessions,
            PasswordResetRepository resets,PasswordEncoder passwords,
            @Qualifier("refreshSessionClock") Clock clock,PlatformTransactionManager manager) {
        this.users=users;this.sessions=sessions;this.resets=resets;this.passwords=passwords;this.clock=clock;
        transaction=new TransactionTemplate(manager);transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }
    public void change(Long userId,String credential,String currentPassword,String newPassword) {
        final String hash;
        try {hash=new RefreshTokens().hash(credential);}catch(IllegalArgumentException invalid){throw failure(AuthenticationFailure.Reason.INVALID_CREDENTIAL);}
        if(!PasswordInput.isValid(newPassword)) throw failure(AuthenticationFailure.Reason.INVALID_REQUEST);
        if(TransactionSynchronizationManager.isActualTransactionActive()) throw failure(AuthenticationFailure.Reason.INTERNAL_FAILURE);
        boolean[] flushed={false};
        try {
            transaction.executeWithoutResult(status->{
                var user=users.lockById(userId).orElse(null);AuthenticationEligibility.require(user);
                var session=sessions.lockByTokenHash(hash).orElseThrow(()->failure(AuthenticationFailure.Reason.INVALID_CREDENTIAL));
                var now=clock.instant().truncatedTo(ChronoUnit.MICROS);
                if(!userId.equals(session.getUserId()) || !session.isUsableAt(now)) throw failure(AuthenticationFailure.Reason.INVALID_CREDENTIAL);
                if(currentPassword==null || !passwords.matches(currentPassword,user.passwordHash())) throw failure(AuthenticationFailure.Reason.ACCESS_DENIED);
                if(passwords.matches(newPassword,user.passwordHash())) throw failure(AuthenticationFailure.Reason.INVALID_REQUEST);
                users.changePassword(userId,passwords.encode(newPassword),now);
                sessions.revokeOthers(userId,session.getId(),now);
                resets.invalidateCredentials(userId,now);resets.flush();flushed[0]=true;
            });
        } catch(AuthenticationFailure known){throw known;}
        catch(RuntimeException failure){
            if(flushed[0]) throw failure(AuthenticationFailure.Reason.COMMIT_UNCERTAIN);
            if(failure instanceof DataAccessResourceFailureException || failure instanceof TransientDataAccessException || failure instanceof CannotCreateTransactionException || failure instanceof TransactionTimedOutException) throw failure(AuthenticationFailure.Reason.UNAVAILABLE);
            throw failure(AuthenticationFailure.Reason.INTERNAL_FAILURE);
        }
    }
    private static AuthenticationFailure failure(AuthenticationFailure.Reason reason){return new AuthenticationFailure(reason);}
}
