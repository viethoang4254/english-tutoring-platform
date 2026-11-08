package com.englishlearning.features.paymentaccount.service;

import com.englishlearning.common.api.ApiId;
import com.englishlearning.features.paymentaccount.dto.ReceivingAccountReview;
import com.englishlearning.features.paymentaccount.repository.ReceivingAccountRepository;
import com.englishlearning.features.teacher.service.TeacherBusinessEligibility;
import com.englishlearning.features.teacher.service.TeacherReviewFailure;
import com.englishlearning.features.user.entity.User.Role;
import com.englishlearning.features.user.service.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import static com.englishlearning.features.paymentaccount.service.ReceivingFailure.Reason.*;

@Service
public class ReceivingAccountReviewService {
    private final ReceivingAccountRepository rows;
    private final CurrentAccountService accounts;
    private final UserCredentialService users;
    private final TeacherBusinessEligibility teachers;
    private final Clock clock;
    public ReceivingAccountReviewService(ReceivingAccountRepository rows, CurrentAccountService accounts,
            UserCredentialService users, TeacherBusinessEligibility teachers, @Qualifier("refreshSessionClock") Clock clock) {
        this.rows=rows; this.accounts=accounts; this.users=users; this.teachers=teachers; this.clock=clock;
    }
    public record Item(ApiId id, ApiId teacherId, String bankName, Instant submittedAt) {}
    public record Page(List<Item> items, int page, int size, boolean hasNext) {}
    @Transactional(readOnly=true)
    public Page pending(long actor, int page, int size) {
        admin(accounts.findById(actor).orElse(null));
        if(page<0 || size<1 || size>100 || (long)page*size>Integer.MAX_VALUE) throw new ReceivingFailure(INVALID);
        var found=rows.pendingPage(page,size);
        return new Page(found.stream().limit(size).map(r -> new Item(r.account().id(),new ApiId(r.teacher()),
                r.account().bankName(),r.account().createdAt())).toList(),page,size,found.size()>size);
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public ReceivingAccountReview detail(long actor, long id) {
        admin(accounts.findById(actor).orElse(null));
        var row=rows.get(id).orElseThrow(() -> new ReceivingFailure(MISSING));
        var audit=rows.audit(id);
        return new ReceivingAccountReview(new ApiId(row.teacher()),row.account(),audit.reviewer(),audit.at(),audit.reason());
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public void review(long actor, long id, boolean approve, String reason) {
        admin(accounts.findById(actor).orElse(null));
        if(approve ? reason!=null : reason==null || reason.isBlank() || reason.length()>2000
                || reason.codePoints().anyMatch(Character::isISOControl)) throw new ReceivingFailure(INVALID);
        long teacher=rows.get(id).orElseThrow(() -> new ReceivingFailure(MISSING)).teacher();
        // Same user-row lock and stable order as onboarding reviews; submissions also lock this Teacher.
        Map<Long,AccountSnapshot> locked=new HashMap<>();
        for(long user : new TreeSet<>(List.of(actor,teacher))) locked.put(user,users.lockAccountById(user).orElse(null));
        admin(locked.get(actor));
        var owner=locked.get(teacher);
        if(owner==null || owner.role()!=Role.TEACHER || owner.locked() || !owner.emailVerified()) throw new ReceivingFailure(DENIED);
        try { teachers.requireEligible(teacher); } catch(TeacherReviewFailure e) { throw new ReceivingFailure(DENIED); }
        var row=rows.lock(id).orElseThrow(() -> new ReceivingFailure(MISSING));
        if(row.teacher()!=teacher || !"PENDING".equals(row.account().reviewStatus())) throw new ReceivingFailure(ALREADY_REVIEWED);
        var now=clock.instant().truncatedTo(ChronoUnit.MICROS);
        if(approve) rows.deactivateSelected(teacher,now);
        if(rows.decide(id,actor,approve,approve?null:reason.strip(),now)!=1) throw new ReceivingFailure(ALREADY_REVIEWED);
    }
    private static void admin(AccountSnapshot account) {
        if(account==null || account.role()!=Role.ADMIN || account.locked() || !account.emailVerified()) throw new ReceivingFailure(DENIED);
    }
}
