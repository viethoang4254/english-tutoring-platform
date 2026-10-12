package com.englishlearning.features.user.repository;

import com.englishlearning.features.user.entity.User;
import java.util.Optional;
import org.springframework.data.repository.Repository;

public interface UserRepository extends Repository<User, Long> {
    Optional<User> findById(Long id);
}
