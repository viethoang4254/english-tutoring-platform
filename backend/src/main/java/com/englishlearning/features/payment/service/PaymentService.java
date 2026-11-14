package com.englishlearning.features.payment.service;

import com.englishlearning.features.payment.dto.PaymentDto;
import com.englishlearning.features.payment.repository.PaymentRepository;
import com.englishlearning.features.enrollment.repository.EnrollmentRepository;
import com.englishlearning.features.paymentaccount.service.*;
import com.englishlearning.features.teacher.service.*;
import com.englishlearning.features.user.service.*;
import com.englishlearning.features.user.entity.User.Role;
import java.security.SecureRandom;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import static com.englishlearning.features.payment.service.PaymentFailure.Reason.*;

@Service
public class PaymentService {
    private final PaymentRepository rows;
    private final EnrollmentRepository enrollments;
    private final UserCredentialService users;
    private final CurrentAccountService accounts;
    private final TeacherBusinessEligibility teachers;
    private final BankDirectory banks;
    private final Clock clock;
    private final SecureRandom random=new SecureRandom();
    public PaymentService(PaymentRepository rows,EnrollmentRepository enrollments,UserCredentialService users,
            CurrentAccountService accounts,TeacherBusinessEligibility teachers,BankDirectory banks,
            @Qualifier("refreshSessionClock") Clock clock) {
        this.rows=rows;this.enrollments=enrollments;this.users=users;this.accounts=accounts;this.teachers=teachers;this.banks=banks;this.clock=clock;
    }
    public record Created(PaymentDto payment,boolean created) {}
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public Created create(long student,long enrollment) {
        student(accounts.findById(student).orElse(null));
        var context=rows.context(student,enrollment).orElseThrow(()->fail(ENROLLMENT_NOT_FOUND));
        // Same ascending user lock order as account reviews, before Course and Enrollment locks.
        for(long id:new TreeSet<>(List.of(student,context.teacher()))) {
            var user=users.lockAccountById(id).orElseThrow(()->fail(ACCESS_DENIED));
            if(id==student)student(user);
        }
        try { teachers.requireEligible(context.teacher()); } catch(TeacherReviewFailure e) { throw fail(RECEIVING_ACCOUNT_UNAVAILABLE); }
        var course=enrollments.course(context.course(),true).orElseThrow(()->fail(COURSE_NOT_PAYABLE));
        Instant now=clock.instant().truncatedTo(ChronoUnit.MICROS);
        var e=enrollments.forCourse(student,context.course(),now).orElseThrow(()->fail(ENROLLMENT_NOT_FOUND));
        if(e.id().value()!=enrollment || !e.status().equals("PENDING"))throw fail(ENROLLMENT_NOT_PAYABLE);
        if(e.reservationExpired())throw fail(RESERVATION_EXPIRED);
        Instant first=enrollments.firstStart(context.course());
        if(!course.status().equals("PUBLISHED") || first==null || !first.isAfter(now))throw fail(COURSE_NOT_PAYABLE);
        if(enrollments.occupied(context.course(),now)>course.capacity())throw fail(COURSE_FULL);
        enrollments.expirePayments(enrollment,now);
        var previous=rows.pending(student,enrollment);
        if(previous.isPresent()) {
            if(previous.get().qrPayload()==null)throw fail(PAYMENT_LEGACY_UNAVAILABLE);
            return new Created(previous.get(),false);
        }
        var destination=rows.destination(context.teacher()).orElseThrow(()->fail(RECEIVING_ACCOUNT_UNAVAILABLE));
        try { banks.require(destination.bin()); } catch(ReceivingFailure ex) { throw fail(RECEIVING_ACCOUNT_UNAVAILABLE); }
        var price=rows.price(context.course());
        var money=PaymentMoney.at(price,now);
        byte[] entropy=new byte[11];random.nextBytes(entropy);
        String reference="ELP"+HexFormat.of().withUpperCase().formatHex(entropy);
        String payload=VietQr.payload(destination.bin(),destination.number(),money.payable().toPlainString(),reference);
        long id=rows.insert(enrollment,destination,money,reference,payload,e.seatReservedUntil(),now);
        return new Created(rows.own(student,id).orElseThrow(),true);
    }
    @Transactional(readOnly=true)
    public Optional<Long> latestId(long student,long enrollment) {
        student(accounts.findById(student).orElse(null));
        enrollments.own(student,enrollment,clock.instant()).orElseThrow(()->fail(ENROLLMENT_NOT_FOUND));
        return rows.latestId(student,enrollment);
    }
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public PaymentDto get(long student,long id) {
        student(users.lockAccountById(student).orElse(null));
        var payment=rows.own(student,id).orElseThrow(()->fail(PAYMENT_NOT_FOUND));
        enrollments.expirePayments(payment.enrollmentId().value(),clock.instant().truncatedTo(ChronoUnit.MICROS));
        return rows.own(student,id).orElseThrow(()->fail(PAYMENT_NOT_FOUND));
    }
    private static void student(AccountSnapshot a) { if(a==null || a.locked() || a.role()!=Role.STUDENT)throw fail(ACCESS_DENIED); }
    private static PaymentFailure fail(PaymentFailure.Reason r) { return new PaymentFailure(r); }
}
