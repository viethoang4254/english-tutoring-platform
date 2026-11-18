package com.englishlearning.features.user.service;

import com.englishlearning.features.user.dto.AdminAnalyticsDto;
import com.englishlearning.features.user.repository.AdminUserRepository;
import com.englishlearning.features.user.entity.User.Role;
import java.time.*;
import java.util.Set;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class AdminAnalyticsService {
    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private final AdminUserRepository repository;
    private final CurrentAccountService accounts;
    private final Clock clock;
    public AdminAnalyticsService(AdminUserRepository repository, CurrentAccountService accounts,
            @Qualifier("refreshSessionClock") Clock clock) {
        this.repository = repository; this.accounts = accounts; this.clock = clock;
    }
    @Transactional(readOnly=true, isolation=Isolation.REPEATABLE_READ)
    public AdminAnalyticsDto overview(Long actor, String range) {
        var account = accounts.findById(actor).orElseThrow(() -> new AdminUserService.Failure(401));
        if (account.locked()) throw new AdminUserService.Failure(401);
        if (account.role()!=Role.ADMIN || !account.emailVerified()) throw new AdminUserService.Failure(403);
        if (range==null || !Set.of("7","30","90","all").contains(range)) throw new AdminUserService.Failure(400);
        Instant now = clock.instant();
        Instant from = range.equals("all") ? null : now.atZone(ZONE).toLocalDate()
                .minusDays(Integer.parseInt(range)-1L).atStartOfDay(ZONE).toInstant();
        return repository.analytics(range, from, now);
    }
}
