package com.englishlearning.features.auth.repository;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class PasswordResetRepository {
    private final EntityManager em;
    public PasswordResetRepository(EntityManager em) { this.em = em; }
    public record Credential(long id,String hash,Instant issuedAt,Instant expiresAt,boolean used,int failures) {}
    public Optional<Credential> latest(Long userId) {
        var rows=em.createNativeQuery("SELECT id,secret_hash,(extract(epoch FROM created_at)*1000000)::bigint,(extract(epoch FROM expires_at)*1000000)::bigint,used_at IS NOT NULL,failed_attempts FROM password_reset_credentials WHERE user_id=:id ORDER BY created_at DESC,id DESC LIMIT 1")
                .setParameter("id",userId).getResultList();
        if(rows.isEmpty()) return Optional.empty();
        Object[] row=(Object[])rows.get(0);
        return Optional.of(new Credential(((Number)row[0]).longValue(),(String)row[1],instant(row[2]),instant(row[3]),(Boolean)row[4],((Number)row[5]).intValue()));
    }
    private static Instant instant(Object value) { long micros=((Number)value).longValue();return Instant.ofEpochSecond(Math.floorDiv(micros,1000000),Math.floorMod(micros,1000000)*1000); }
    public void failedAttempt(long id) {
        em.createNativeQuery("UPDATE password_reset_credentials SET failed_attempts=failed_attempts+1 WHERE id=:id AND failed_attempts<5")
                .setParameter("id",id).executeUpdate();
    }
    public void invalidateCredentials(Long id,Instant now) {
        em.createNativeQuery("UPDATE password_reset_credentials SET used_at=:now WHERE user_id=:id AND used_at IS NULL")
                .setParameter("id",id).setParameter("now",now).executeUpdate();
    }
    public boolean mayIssue(Long id, Instant now) {
        Number count=(Number)em.createNativeQuery("SELECT count(*) FROM password_reset_credentials WHERE user_id=:id AND created_at>:since")
                .setParameter("id",id).setParameter("since",now.minusSeconds(86400)).getSingleResult();
        Number recent=(Number)em.createNativeQuery("SELECT count(*) FROM password_reset_credentials WHERE user_id=:id AND created_at>:since")
                .setParameter("id",id).setParameter("since",now.minusSeconds(60)).getSingleResult();
        return count.longValue()<5 && recent.longValue()==0;
    }
    public void issue(Long id,String hash,Instant now) {
        em.createNativeQuery("INSERT INTO password_reset_credentials(user_id,secret_hash,created_at,expires_at) VALUES (:id,:hash,:now,:expiry)")
                .setParameter("id",id).setParameter("hash",hash).setParameter("now",now).setParameter("expiry",now.plusSeconds(900)).executeUpdate();
    }
    public boolean consume(String hash,Long id,Instant now) {
        return em.createNativeQuery("UPDATE password_reset_credentials SET used_at=:now WHERE secret_hash=:hash AND user_id=:id AND used_at IS NULL AND expires_at>:now")
                .setParameter("hash",hash).setParameter("id",id).setParameter("now",now).executeUpdate()==1;
    }
    public void invalidateAll(Long id,Instant now) {
        em.createNativeQuery("UPDATE password_reset_credentials SET used_at=:now WHERE user_id=:id AND used_at IS NULL")
                .setParameter("id",id).setParameter("now",now).executeUpdate();
        em.createNativeQuery("UPDATE refresh_sessions SET revoked_at=:now WHERE user_id=:id AND revoked_at IS NULL")
                .setParameter("id",id).setParameter("now",now).executeUpdate();
    }
    public void flush() { em.flush(); }
}
