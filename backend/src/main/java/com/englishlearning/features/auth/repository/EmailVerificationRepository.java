package com.englishlearning.features.auth.repository;

import com.englishlearning.features.auth.entity.EmailVerificationCredential;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface EmailVerificationRepository extends Repository<EmailVerificationCredential, Long> {
    @Query("select c.userId from EmailVerificationCredential c where c.secretHash = :hash and c.purpose = 'INITIAL_VERIFICATION'")
    Optional<Long> findUserId(@Param("hash") String hash);
    Optional<EmailVerificationCredential> findBySecretHash(String hash);
    @Query("select max(c.createdAt) from EmailVerificationCredential c where c.userId = :user and c.purpose = 'INITIAL_VERIFICATION'")
    Instant lastIssued(@Param("user") Long user);
    // Retain credential history: the first initial credential is registration, not a resend.
    @Query(value = "SELECT count(*) FROM public.email_verification_credentials WHERE user_id=:user "
            + "AND purpose='INITIAL_VERIFICATION' AND created_at>:since AND id<>(SELECT min(id) "
            + "FROM public.email_verification_credentials WHERE user_id=:user AND purpose='INITIAL_VERIFICATION')", nativeQuery = true)
    long resendCount(@Param("user") Long user, @Param("since") Instant since);
    EmailVerificationCredential saveAndFlush(EmailVerificationCredential credential);
    void flush();
}
