package com.englishlearning.features.paymentaccount.service;

import com.englishlearning.features.paymentaccount.repository.ReceivingAccountRepository;
import com.englishlearning.features.teacher.service.TeacherBusinessEligibility;
import com.englishlearning.features.teacher.service.TeacherReviewFailure;
import com.englishlearning.features.user.service.UserCredentialService;
import java.time.Clock;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import static com.englishlearning.features.paymentaccount.service.ReceivingFailure.Reason.*;

@Service
public class ReceivingAccountTransactions {
    private final ReceivingAccountRepository rows;
    private final TeacherBusinessEligibility eligibility;
    private final UserCredentialService users;
    private final Clock clock;

    public ReceivingAccountTransactions(ReceivingAccountRepository rows, TeacherBusinessEligibility eligibility,
            UserCredentialService users, @Qualifier("refreshSessionClock") Clock clock) {
        this.rows = rows; this.eligibility = eligibility; this.users = users; this.clock = clock;
    }

    public record Submission(long actor, BankDirectory.Bank bank, String holder, String number,
            UUID key, String fingerprint, String imageHash, int imageSize) {}

    // Only the object freshly uploaded by this invocation may be reclaimed.
    public static final class Upload {
        final String object;
        private int completion = TransactionSynchronization.STATUS_UNKNOWN;
        private boolean referenced;
        public Upload(String object) { this.object = object; }
        public boolean uncertain() { return completion == TransactionSynchronization.STATUS_UNKNOWN; }
        public boolean reclaimable() {
            return completion == TransactionSynchronization.STATUS_ROLLED_BACK
                || (completion == TransactionSynchronization.STATUS_COMMITTED && !referenced);
        }
    }

    @Transactional(readOnly = true)
    public ReceivingAccountService.Created preflight(Submission input) {
        eligible(input.actor());
        return existing(input);
    }

    @Transactional
    public ReceivingAccountService.Created persist(Submission input, Upload upload) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) throw new ReceivingFailure(UNAVAILABLE);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) { upload.completion = status; }
        });
        if (users.lockAccountById(input.actor()).isEmpty()) throw new ReceivingFailure(DENIED);
        eligible(input.actor());
        var existing = existing(input);
        if (existing != null) return existing;
        // Mark before attempting INSERT: an uncertain commit must always retain the object.
        upload.referenced = true;
        long id = rows.insert(input.actor(), input.bank().bin(), input.bank().code(), input.bank().name(),
                input.holder(), input.number(), upload.object, input.imageHash(), input.imageSize(),
                input.key(), input.fingerprint(), clock.instant());
        return new ReceivingAccountService.Created(rows.get(id).orElseThrow(() -> new ReceivingFailure(UNAVAILABLE)).account(), true);
    }

    private ReceivingAccountService.Created existing(Submission input) {
        var existing = rows.byKey(input.actor(), input.key());
        if (existing.isPresent()) {
            if (!input.fingerprint().equals(existing.get().requestHash())) throw new ReceivingFailure(IDEMPOTENCY_CONFLICT);
            return new ReceivingAccountService.Created(existing.get().account(), false);
        }
        if (rows.pending(input.actor())) throw new ReceivingFailure(CONFLICT);
        return null;
    }

    private void eligible(long actor) {
        try { eligibility.requireEligible(actor); }
        catch (TeacherReviewFailure e) { throw new ReceivingFailure(DENIED); }
    }
}
