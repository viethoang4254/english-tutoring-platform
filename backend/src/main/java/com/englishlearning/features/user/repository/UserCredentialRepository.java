package com.englishlearning.features.user.repository;

import com.englishlearning.features.user.service.CredentialSnapshot;
import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class UserCredentialRepository {
    private final EntityManager entityManager;
    public UserCredentialRepository(EntityManager entityManager) { this.entityManager = entityManager; }

    public Optional<CredentialSnapshot> findByEmail(String email) {
        return query("email = :value", email);
    }
    public Optional<CredentialSnapshot> lockById(Long id) {
        return query("id = :value FOR UPDATE", id);
    }
    public Optional<Long> createStudent(String email, String hash, String fullName, java.time.Instant now) {
        List<?> rows = entityManager.createNativeQuery("INSERT INTO public.users "
                + "(email,password_hash,full_name,role,email_verified,locked,created_at,updated_at) "
                + "VALUES (:email,:hash,:name,'STUDENT',false,false,:now,:now) "
                + "ON CONFLICT (email) DO NOTHING RETURNING id")
                .setParameter("email", email).setParameter("hash", hash).setParameter("name", fullName)
                .setParameter("now", now).getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(((Number) rows.get(0)).longValue());
    }
    public void verifyEmail(Long id, java.time.Instant now) {
        entityManager.createNativeQuery("UPDATE public.users SET email_verified=true,updated_at=:now WHERE id=:id")
                .setParameter("id", id).setParameter("now", now).executeUpdate();
    }
    private Optional<CredentialSnapshot> query(String predicate, Object value) {
        // Scalar reads avoid returning a stale first-level-cache entity during the locked recheck.
        List<?> rows = entityManager.createNativeQuery(
                "SELECT id,email,password_hash,locked,email_verified FROM public.users WHERE " + predicate)
                .setParameter("value", value).getResultList();
        if (rows.isEmpty()) return Optional.empty();
        Object[] row = (Object[]) rows.get(0);
        return Optional.of(new CredentialSnapshot(((Number) row[0]).longValue(),
                (String) row[1], (String) row[2], (Boolean) row[3], (Boolean) row[4]));
    }
}
