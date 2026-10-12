package com.englishlearning.features.auth.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "email_verification_credentials", schema = "public")
public class EmailVerificationCredential {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id", nullable = false) private Long userId;
    @Column(nullable = false) private String purpose;
    @Column(name = "target_email", nullable = false) private String targetEmail;
    @Column(name = "secret_hash", nullable = false) private String secretHash;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "used_at") private Instant usedAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    protected EmailVerificationCredential() {}
    public EmailVerificationCredential(Long userId, String email, String hash, Instant now) {
        this.userId = userId; targetEmail = email; secretHash = hash;
        purpose = "INITIAL_VERIFICATION"; createdAt = now; expiresAt = now.plusSeconds(86400);
    }
    public Long getUserId() { return userId; }
    public boolean usable(String email, Instant now) {
        return "INITIAL_VERIFICATION".equals(purpose) && targetEmail.equals(email)
                && usedAt == null && now.isBefore(expiresAt);
    }
    public void consume(Instant now) { usedAt = now; }
}
