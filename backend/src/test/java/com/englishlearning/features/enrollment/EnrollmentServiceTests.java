package com.englishlearning.features.enrollment;

import com.englishlearning.common.api.ApiId;
import com.englishlearning.features.enrollment.dto.EnrollmentDto;
import com.englishlearning.features.enrollment.repository.EnrollmentRepository;
import com.englishlearning.features.enrollment.service.*;
import com.englishlearning.features.teacher.service.*;
import com.englishlearning.features.user.entity.User.Role;
import com.englishlearning.features.user.service.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EnrollmentServiceTests {
    final Instant now=Instant.parse("2026-10-29T00:00:00Z");
    EnrollmentRepository rows; UserCredentialService users; CurrentAccountService accounts;
    TeacherBusinessEligibility teachers; EnrollmentService service;
    @BeforeEach void setup() {
        rows=mock(EnrollmentRepository.class);users=mock(UserCredentialService.class);accounts=mock(CurrentAccountService.class);teachers=mock(TeacherBusinessEligibility.class);
        service=new EnrollmentService(rows,users,accounts,teachers,Clock.fixed(now,ZoneOffset.UTC));
        var student=new AccountSnapshot(1L,Role.STUDENT,false,false,true);
        when(users.lockAccountById(1L)).thenReturn(Optional.of(student));when(accounts.findById(1L)).thenReturn(Optional.of(student));
        when(rows.course(2,true)).thenReturn(Optional.of(new EnrollmentRepository.Course(9,"PUBLISHED",BigDecimal.ONE,1)));
        when(rows.firstStart(2)).thenReturn(now.plusSeconds(3600));
    }
    EnrollmentDto row(String status,boolean expired) {return new EnrollmentDto(new ApiId(3),new ApiId(2),"Course",status,null,expired?now:now.plusSeconds(900),expired,null,null,now,now);}
    void existing(String status,boolean expired) {var e=row(status,expired);when(rows.own(eq(1L),eq(3L),any())).thenReturn(Optional.of(e));when(rows.forCourse(eq(1L),eq(2L),any())).thenReturn(Optional.of(e));}
    void reason(EnrollmentFailure.Reason reason,Runnable operation) {assertEquals(reason,assertThrows(EnrollmentFailure.class,operation::run).reason());}
    @Test void unverifiedStudentCreatesPendingWithCappedDeadline() {
        when(rows.firstStart(2)).thenReturn(now.plusSeconds(30));when(rows.create(1,2,now.plusSeconds(30),now)).thenReturn(3L);
        when(rows.own(1,3,now)).thenReturn(Optional.of(row("PENDING",false)));
        assertTrue(service.create(1,2).created());verify(rows).create(1,2,now.plusSeconds(30),now);
    }
    @Test void duplicatePreservesDeadlineAndExpiredRequiresRenewal() {
        existing("PENDING",false);assertFalse(service.create(1,2).created());verify(rows,never()).create(anyLong(),anyLong(),any(),any());
        existing("PENDING",true);reason(EnrollmentFailure.Reason.ENROLLMENT_RENEWAL_REQUIRED,()->service.create(1,2));
    }
    @Test void cutoffMissingSessionsPriceAndLifecycleReject() {
        for(Instant start:List.of(now,now.minusSeconds(1))) {when(rows.firstStart(2)).thenReturn(start);reason(EnrollmentFailure.Reason.COURSE_NOT_ENROLLABLE,()->service.create(1,2));}
        when(rows.firstStart(2)).thenReturn(null);reason(EnrollmentFailure.Reason.COURSE_NOT_ENROLLABLE,()->service.create(1,2));
        for(String state:List.of("DRAFT","CANCELLED","COMPLETED","ARCHIVED")) {when(rows.course(2,true)).thenReturn(Optional.of(new EnrollmentRepository.Course(9,state,BigDecimal.ONE,1)));reason(EnrollmentFailure.Reason.COURSE_NOT_ENROLLABLE,()->service.create(1,2));}
        when(rows.course(2,true)).thenReturn(Optional.of(new EnrollmentRepository.Course(9,"PUBLISHED",BigDecimal.ZERO,1)));reason(EnrollmentFailure.Reason.COURSE_NOT_ENROLLABLE,()->service.create(1,2));
    }
    @Test void capacityAndAccountRestrictions() {
        when(rows.occupied(2,now)).thenReturn(1L);reason(EnrollmentFailure.Reason.COURSE_FULL,()->service.create(1,2));
        for(var account:List.of(new AccountSnapshot(1L,Role.STUDENT,true,true,false),new AccountSnapshot(1L,Role.TEACHER,false,true,false),new AccountSnapshot(1L,Role.ADMIN,false,true,false))) {
            when(users.lockAccountById(1L)).thenReturn(Optional.of(account));reason(EnrollmentFailure.Reason.ACCESS_DENIED,()->service.create(1,2));
        }
    }
    @Test void renewIsExplicitAndNeverExtendsValidReservation() {
        existing("PENDING",false);service.renew(1,3);verify(rows,never()).renew(anyLong(),anyLong(),any(),any());
        existing("PENDING",true);service.renew(1,3);verify(rows).renew(1,3,now.plusSeconds(900),now);
    }
    @Test void cancelPreservesHistoryAndPaymentBoundary() {
        existing("PENDING",false);service.cancel(1,3);verify(rows).cancel(1,3,now);
        existing("CANCELLED",false);service.cancel(1,3);verify(rows,times(1)).cancel(1,3,now);
        existing("PENDING",true);when(rows.pendingPayment(3)).thenReturn(true);
        reason(EnrollmentFailure.Reason.PAYMENT_COORDINATION_REQUIRED,()->service.renew(1,3));
        reason(EnrollmentFailure.Reason.PAYMENT_COORDINATION_REQUIRED,()->service.cancel(1,3));
    }
    @Test void activeCompletedAndCancelledCannotRenewOrCancelActive() {
        for(String state:List.of("ACTIVE","COMPLETED","CANCELLED")) {existing(state,false);reason(EnrollmentFailure.Reason.ENROLLMENT_STATE_CONFLICT,()->service.renew(1,3));}
        existing("ACTIVE",false);assertFalse(service.create(1,2).created());reason(EnrollmentFailure.Reason.ENROLLMENT_STATE_CONFLICT,()->service.cancel(1,3));
        for(String state:List.of("COMPLETED","CANCELLED")) {existing(state,false);reason(EnrollmentFailure.Reason.ENROLLMENT_STATE_CONFLICT,()->service.create(1,2));}
    }
    @Test void privateReadsAndTeacherOwnership() {
        reason(EnrollmentFailure.Reason.ENROLLMENT_NOT_FOUND,()->service.get(1,999));
        when(rows.course(2,false)).thenReturn(Optional.of(new EnrollmentRepository.Course(9,"PUBLISHED",BigDecimal.ONE,1)));
        reason(EnrollmentFailure.Reason.COURSE_NOT_FOUND,()->service.roster(8,2,0,20,null));
        verify(rows,never()).roster(anyLong(),anyLong(),anyInt(),anyInt(),any());
        doThrow(new TeacherReviewFailure(TeacherReviewFailure.Reason.DENIED)).when(teachers).requireEligible(9L);
        reason(EnrollmentFailure.Reason.ACCESS_DENIED,()->service.roster(9,2,0,20,null));
    }
    @Test void pagesAreBoundedAndStatusStrict() {
        for(int[] value:new int[][]{{-1,20},{0,0},{0,101},{Integer.MAX_VALUE,100}})assertThrows(EnrollmentFailure.class,()->EnrollmentService.validatePage(value[0],value[1],null));
        assertThrows(EnrollmentFailure.class,()->EnrollmentService.validatePage(0,20,"EXPIRED"));
        when(rows.list(1,0,1,null,now)).thenReturn(List.of(row("PENDING",false),row("ACTIVE",false)));
        var page=service.list(1,0,1,null);assertTrue(page.hasNext());assertEquals(1,page.items().size());
    }
    @Test void writesLockUserThenCourseBeforeCountingSeats() {
        when(rows.create(1,2,now.plusSeconds(900),now)).thenReturn(3L);when(rows.own(1,3,now)).thenReturn(Optional.of(row("PENDING",false)));
        service.create(1,2);var order=inOrder(users,rows);
        order.verify(users).lockAccountById(1L);order.verify(rows).course(2,true);
        order.verify(rows).forCourse(1,2,now);order.verify(rows).firstStart(2);order.verify(rows).occupied(2,now);
        order.verify(rows).create(1,2,now.plusSeconds(900),now);
    }
}
