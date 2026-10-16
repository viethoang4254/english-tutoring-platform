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
    public Optional<com.englishlearning.features.user.service.AccountSnapshot> lockAccountById(Long id) {
        List<?> rows = entityManager.createNativeQuery(
                "SELECT id,role,locked,email_verified,student_login_without_verification FROM public.users WHERE id=:id FOR UPDATE")
                .setParameter("id", id).getResultList();
        if (rows.isEmpty()) return Optional.empty();
        Object[] row = (Object[]) rows.get(0);
        return Optional.of(new com.englishlearning.features.user.service.AccountSnapshot(
                ((Number) row[0]).longValue(), com.englishlearning.features.user.entity.User.Role.valueOf((String) row[1]),
                (Boolean) row[2], (Boolean) row[3], (Boolean) row[4]));
    }
    public Optional<Long> createStudent(String email, String hash, String fullName, String username, String phone, java.time.Instant now) {
        try {
            List<?> rows = entityManager.createNativeQuery("INSERT INTO public.users "
                    + "(email,password_hash,full_name,username,phone,role,email_verified,locked,student_login_without_verification,created_at,updated_at) "
                    + "VALUES (:email,:hash,:name,:username,:phone,'STUDENT',false,false,true,:now,:now) "
                    + "ON CONFLICT (email) DO NOTHING RETURNING id")
                    .setParameter("email", email).setParameter("hash", hash).setParameter("name", fullName)
                    .setParameter("username", username).setParameter("phone", phone).setParameter("now", now).getResultList();
            return rows.isEmpty() ? Optional.empty() : Optional.of(((Number) rows.get(0)).longValue());
        } catch (RuntimeException failure) {
            for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
                if (cause instanceof org.hibernate.exception.ConstraintViolationException constraint
                        && "23505".equals(constraint.getSQLState())
                        && "uq_users_username".equals(constraint.getConstraintName())) {
                    throw new com.englishlearning.features.user.service.RegistrationConflict();
                }
            }
            throw failure;
        }
    }
    public Optional<Long> createTeacher(String email, String hash, String fullName, java.time.Instant now) {
        return createAccount(email, hash, fullName, "TEACHER", now);
    }
    private Optional<Long> createAccount(String email, String hash, String fullName, String role, java.time.Instant now) {
        List<?> rows = entityManager.createNativeQuery("INSERT INTO public.users "
                + "(email,password_hash,full_name,role,email_verified,locked,created_at,updated_at) "
                + "VALUES (:email,:hash,:name,:role,false,false,:now,:now) "
                + "ON CONFLICT (email) DO NOTHING RETURNING id")
                .setParameter("email", email).setParameter("hash", hash).setParameter("name", fullName)
                .setParameter("role", role).setParameter("now", now).getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(((Number) rows.get(0)).longValue());
    }
    public void changePassword(Long id, String hash, java.time.Instant now) {
        entityManager.createNativeQuery("UPDATE users SET password_hash=:hash,updated_at=:now WHERE id=:id")
                .setParameter("id",id).setParameter("hash",hash).setParameter("now",now).executeUpdate();
    }
    public void verifyEmail(Long id, java.time.Instant now) {
        entityManager.createNativeQuery("UPDATE public.users SET email_verified=true,updated_at=:now WHERE id=:id")
                .setParameter("id", id).setParameter("now", now).executeUpdate();
    }
    private Optional<CredentialSnapshot> query(String predicate, Object value) {
        // Scalar reads avoid returning a stale first-level-cache entity during the locked recheck.
        List<?> rows = entityManager.createNativeQuery(
                "SELECT id,email,password_hash,locked,email_verified,role,student_login_without_verification FROM public.users WHERE " + predicate)
                .setParameter("value", value).getResultList();
        if (rows.isEmpty()) return Optional.empty();
        Object[] row = (Object[]) rows.get(0);
        return Optional.of(new CredentialSnapshot(((Number) row[0]).longValue(),
                (String) row[1], (String) row[2], (Boolean) row[3], (Boolean) row[4],
                com.englishlearning.features.user.entity.User.Role.valueOf((String) row[5]), (Boolean) row[6]));
    }
}
