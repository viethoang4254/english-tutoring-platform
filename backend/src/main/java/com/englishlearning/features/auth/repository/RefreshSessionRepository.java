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
    @Query("select session.userId from RefreshSession session where session.tokenHash = :hash")
    Optional<Long> findOwner(@Param("hash") String hash);
    @org.springframework.data.jpa.repository.Modifying
    @Query("update RefreshSession s set s.revokedAt=:now where s.userId=:userId and s.id<>:currentId and s.revokedAt is null")
    int revokeOthers(@Param("userId") Long userId,@Param("currentId") Long currentId,@Param("now") java.time.Instant now);
    RefreshSession saveAndFlush(RefreshSession session);
    void flush();
}
