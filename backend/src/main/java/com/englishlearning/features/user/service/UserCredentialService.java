package com.englishlearning.features.user.service;

import com.englishlearning.features.user.repository.UserCredentialRepository;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserCredentialService {
    private final UserCredentialRepository users;
    public UserCredentialService(UserCredentialRepository users) { this.users = users; }

    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<AccountSnapshot> lockAccountById(Long id) { return users.lockAccountById(id); }

    @Transactional(readOnly = true)
    public Optional<CredentialSnapshot> findByEmail(String input) {
        if (input == null) return Optional.empty();
        return users.findByEmail(input.trim().toLowerCase(Locale.ROOT));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<Long> createStudent(String email, String hash, String name, java.time.Instant now) {
        return users.createStudent(email, hash, name, now);
    }
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<Long> createTeacher(String email, String hash, String name, java.time.Instant now) {
        return users.createTeacher(email, hash, name, now);
    }
    @Transactional(propagation = Propagation.MANDATORY)
    public void verifyEmail(Long id, java.time.Instant now) { users.verifyEmail(id, now); }

    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<CredentialSnapshot> lockById(Long id) {
        return users.lockById(id);
    }
}
