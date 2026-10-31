package com.englishlearning.features.enrollment.service;

import com.englishlearning.features.enrollment.dto.EnrollmentDto;
import com.englishlearning.features.enrollment.repository.EnrollmentRepository;
import com.englishlearning.features.teacher.service.*;
import com.englishlearning.features.user.entity.User.Role;
import com.englishlearning.features.user.service.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import static com.englishlearning.features.enrollment.service.EnrollmentFailure.Reason.*;

@Service
public class EnrollmentService {
    private final EnrollmentRepository rows;
    private final UserCredentialService users;
    private final CurrentAccountService accounts;
    private final TeacherBusinessEligibility teachers;
    private final Clock clock;
    public EnrollmentService(EnrollmentRepository rows,UserCredentialService users,CurrentAccountService accounts,
            TeacherBusinessEligibility teachers,@Qualifier("refreshSessionClock") Clock clock) {
        this.rows=rows;this.users=users;this.accounts=accounts;this.teachers=teachers;this.clock=clock;
    }
    public record Created(EnrollmentDto enrollment,boolean created) {}
    public record Page<T>(List<T> items,int page,int size,boolean hasNext) {}
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Created create(long actor,long course) {
        student(actor,true);
        var parent=rows.course(course,true).orElseThrow(()->fail(COURSE_NOT_FOUND));
        Instant now=clock.instant();
        var existing=rows.forCourse(actor,course,now);
        if(existing.isPresent()) {
            var e=existing.get();
            if(e.status().equals("ACTIVE"))return new Created(e,false);
            pending(e);
            if(e.reservationExpired())throw fail(ENROLLMENT_RENEWAL_REQUIRED);
            return new Created(e,false);
        }
        Instant deadline=reserve(parent,course,now);
        long id=rows.create(actor,course,deadline,now);
        return new Created(own(actor,id,now),true);
    }
    @Transactional(readOnly=true)
    public EnrollmentDto get(long actor,long id) { student(actor,false);return own(actor,id,clock.instant()); }
    @Transactional(readOnly=true)
    public Page<EnrollmentDto> list(long actor,int page,int size,String status) {
        student(actor,false);validatePage(page,size,status);
        return page(rows.list(actor,page,size,status,clock.instant()),page,size);
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public EnrollmentDto renew(long actor,long id) {
        student(actor,true);var initial=own(actor,id,clock.instant());
        long course=initial.courseId().value();
        var parent=rows.course(course,true).orElseThrow(()->fail(COURSE_NOT_FOUND));
        Instant now=clock.instant();var e=rows.forCourse(actor,course,now).orElseThrow(()->fail(ENROLLMENT_NOT_FOUND));
        pending(e);
        if(!e.reservationExpired())return e;
        noPendingPayment(id);
        rows.renew(actor,id,reserve(parent,course,now),now);
        return own(actor,id,now);
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public EnrollmentDto cancel(long actor,long id) {
        student(actor,true);var initial=own(actor,id,clock.instant());long course=initial.courseId().value();
        rows.course(course,true).orElseThrow(()->fail(COURSE_NOT_FOUND));
        Instant now=clock.instant();var e=rows.forCourse(actor,course,now).orElseThrow(()->fail(ENROLLMENT_NOT_FOUND));
        if(e.status().equals("CANCELLED"))return e;
        pending(e);noPendingPayment(id);rows.cancel(actor,id,now);return own(actor,id,now);
    }
    @Transactional(readOnly=true)
    public Page<EnrollmentRepository.RosterRow> roster(long actor,long course,int page,int size,String status) {
        try { teachers.requireEligible(actor); } catch(TeacherReviewFailure e) { throw fail(ACCESS_DENIED); }
        validatePage(page,size,status);
        var parent=rows.course(course,false).orElseThrow(()->fail(COURSE_NOT_FOUND));
        if(parent.teacherId()!=actor)throw fail(COURSE_NOT_FOUND);
        return page(rows.roster(actor,course,page,size,status),page,size);
    }
    private Instant reserve(EnrollmentRepository.Course course,long id,Instant now) {
        if(!course.status().equals("PUBLISHED")||course.tuition().signum()<=0)throw fail(COURSE_NOT_ENROLLABLE);
        Instant first=rows.firstStart(id);
        if(first==null||!first.isAfter(now))throw fail(COURSE_NOT_ENROLLABLE);
        if(rows.occupied(id,now)>=course.capacity())throw fail(COURSE_FULL);
        Instant limit=now.plusSeconds(900);return first.isBefore(limit)?first:limit;
    }
    private void student(long actor,boolean lock) {
        var account=(lock?users.lockAccountById(actor):accounts.findById(actor)).orElseThrow(()->fail(ACCESS_DENIED));
        if(account.locked()||account.role()!=Role.STUDENT)throw fail(ACCESS_DENIED);
    }
    private EnrollmentDto own(long actor,long id,Instant now) { return rows.own(actor,id,now).orElseThrow(()->fail(ENROLLMENT_NOT_FOUND)); }
    private void pending(EnrollmentDto e) { if(!e.status().equals("PENDING"))throw fail(ENROLLMENT_STATE_CONFLICT); }
    private void noPendingPayment(long id) { if(rows.pendingPayment(id))throw fail(PAYMENT_COORDINATION_REQUIRED); }
    public static void validatePage(int page,int size,String status) {
        if(page<0||size<1||size>100||(long)page*size>Integer.MAX_VALUE||
                (status!=null&&!Set.of("PENDING","ACTIVE","COMPLETED","CANCELLED").contains(status)))throw fail(INVALID_REQUEST);
    }
    private static <T> Page<T> page(List<T> rows,int page,int size) { return new Page<>(List.copyOf(rows.subList(0,Math.min(size,rows.size()))),page,size,rows.size()>size); }
    private static EnrollmentFailure fail(EnrollmentFailure.Reason reason) { return new EnrollmentFailure(reason); }
}
