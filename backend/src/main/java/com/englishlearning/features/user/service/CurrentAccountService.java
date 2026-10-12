package com.englishlearning.features.user.service;

import com.englishlearning.features.user.repository.UserRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrentAccountService {
    private final UserRepository users;

    public CurrentAccountService(UserRepository users) { this.users = users; }

    @Transactional(readOnly = true)
    public Optional<AccountSnapshot> findById(Long id) {
        return users.findById(id).map(user -> new AccountSnapshot(
                user.getId(), user.getRole(), user.isLocked(), user.isEmailVerified()));
    }
}
