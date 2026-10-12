package com.englishlearning.features.auth.repository;

import com.englishlearning.features.auth.entity.RefreshSession;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface RefreshSessionRepository extends Repository<RefreshSession, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from RefreshSession session where session.tokenHash = :hash")
    Optional<RefreshSession> lockByTokenHash(@Param("hash") String hash);
    RefreshSession saveAndFlush(RefreshSession session);
    void flush();
}
