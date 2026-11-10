package com.englishlearning.features.payment.service;

import com.englishlearning.features.payment.dto.*;
import com.englishlearning.features.payment.repository.PaymentRepository;
import com.englishlearning.features.enrollment.repository.EnrollmentRepository;
import com.englishlearning.features.teacher.service.*;
import com.englishlearning.features.user.service.*;
import com.englishlearning.features.user.entity.User.Role;
import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import static com.englishlearning.features.payment.service.PaymentFailure.Reason.*;

@Service
public class ReconciliationService {
    private final PaymentRepository rows;
    private final EnrollmentRepository enrollments;
    private final UserCredentialService users;
    private final TeacherBusinessEligibility teachers;
    private final Clock clock;
    public ReconciliationService(PaymentRepository rows,EnrollmentRepository enrollments,UserCredentialService users,
            TeacherBusinessEligibility teachers,@Qualifier("refreshSessionClock") Clock clock) {
        this.rows=rows;this.enrollments=enrollments;this.users=users;this.teachers=teachers;this.clock=clock;
    }
    public record Page<T>(List<T> items,int page,int size,boolean hasNext) {}
    @Transactional(readOnly=true)
    public Page<PaymentDto> queue(long teacher,long course,int page,int size) {
        eligible(teacher);owned(teacher,course,false);pagination(page,size);
        return page(rows.teacherPage(teacher,course,page,size),page,size);
    }
    @Transactional(readOnly=true)
    public PaymentDto detail(long teacher,long course,long payment) {
        eligible(teacher);var c=rows.teacherContext(teacher,course,payment).orElseThrow(()->fail(PAYMENT_NOT_FOUND));
        return rows.own(c.student(),payment).orElseThrow(()->fail(PAYMENT_NOT_FOUND));
    }
    @Transactional(readOnly=true)
    public Page<ReconciliationAttempt> history(long teacher,long course,long payment,int page,int size) {
        detail(teacher,course,payment);pagination(page,size);return page(rows.history(payment,page,size),page,size);
    }
    // Return business failure instead of throwing: the proxy commits its audit before HTTP maps it to 409.
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public ReconciliationAttempt decide(long teacher,long course,long payment,ReconciliationEvidence input,boolean late) {
        eligible(teacher);
        var context=rows.teacherContext(teacher,course,payment).orElseThrow(()->fail(PAYMENT_NOT_FOUND));
        AccountSnapshot student=null;
        for(long id:new TreeSet<>(List.of(teacher,context.student()))) {
            var account=users.lockAccountById(id).orElseThrow(()->fail(ACCESS_DENIED));
            if(id==teacher && (account.locked() || !account.emailVerified() || account.role()!=Role.TEACHER))throw fail(ACCESS_DENIED);
            if(id==context.student())student=account;
        }
        eligible(teacher);var parent=owned(teacher,course,true);
        var enrollment=enrollments.forCourse(context.student(),course,clock.instant()).orElseThrow(()->fail(PAYMENT_NOT_FOUND));
        var p=rows.lockPayment(context.student(),payment);
        if(input==null)throw fail(INVALID_REQUEST);
        var evidence=input.validated();
        Instant checkedAt=clock.instant().truncatedTo(ChronoUnit.MICROS);
        String mismatch=match(p,evidence,checkedAt);
        if(mismatch!=null)return rows.recordAttempt(payment,teacher,evidence,late,checkedAt,mismatch,null);

        rows.lockBankTransaction(p,evidence.bankTransactionReference());
        Instant now=clock.instant().truncatedTo(ChronoUnit.MICROS);
        boolean reservationExpired=enrollment.seatReservedUntil()==null || !enrollment.seatReservedUntil().isAfter(now);
        var allocation=rows.allocation(p,evidence.bankTransactionReference());
        String failure=null;Long replay=null;
        if(allocation.isPresent()) {
            var previous=allocation.get();
            if(previous.paymentId().value()==payment && p.status().equals("CONFIRMED") && same(previous.evidence(),evidence))replay=previous.id().value();
            else failure="BANK_TRANSACTION_ALREADY_ALLOCATED";
        } else if(p.status().equals("CONFIRMED"))failure="PAYMENT_ALREADY_CONFIRMED";
        if(failure==null && replay==null) {
            if(failure==null && (student==null || student.locked() || student.role()!=Role.STUDENT))failure="STUDENT_INELIGIBLE";
            if(failure==null && rows.conflictingObligation(context.enrollment(),payment))failure="CONFLICTING_PAYMENT";
            if(failure==null && !enrollment.status().equals("PENDING"))failure="ENROLLMENT_NOT_PENDING";
            boolean expired=!p.expiresAt().isAfter(now) || reservationExpired || p.status().equals("EXPIRED");
            if(failure==null && !Set.of("PENDING","EXPIRED").contains(p.status()))failure="PAYMENT_NOT_RECONCILABLE";
            if(failure==null && expired!=late)failure=expired?"LATE_RECONCILIATION_REQUIRED":"PAYMENT_NOT_EXPIRED";
            Instant first=enrollments.firstStart(course);
            if(failure==null && (!parent.status().equals("PUBLISHED") || first==null || !first.isAfter(now)))failure="COURSE_NOT_ELIGIBLE";
            long occupied=enrollments.occupied(course,now);
            long ownSeat=reservationExpired?0:1;
            if(failure==null && occupied-ownSeat>=parent.capacity())failure="MANUAL_RESOLUTION_REQUIRED_NO_CAPACITY";
            if(failure==null)rows.activate(payment,context.enrollment(),now);
        }
        return rows.recordAttempt(payment,teacher,evidence,late,now,failure,replay);
    }
    private static String match(PaymentDto p,ReconciliationEvidence e,Instant now) {
        if(!e.attested())return "ATTESTATION_REQUIRED";
        if(p.bankBin()==null || p.accountNumber()==null)return "LEGACY_PAYMENT_UNSUPPORTED";
        if(!p.bankBin().equals(e.bankBin()) || !p.accountNumber().equals(e.accountNumber()))return "RECEIVER_MISMATCH";
        if(!"VND".equals(e.currency()) || !p.currency().equals(e.currency()))return "CURRENCY_MISMATCH";
        if(new BigDecimal(p.finalAmount()).compareTo(e.observedAmount())!=0)return "AMOUNT_MISMATCH";
        if(!p.transferReference().equals(e.transferReference()) || !Pattern.compile("(?<![A-Za-z0-9])"+Pattern.quote(p.transferReference())+"(?![A-Za-z0-9])").matcher(e.transferDescription()).find())return "REFERENCE_MISMATCH";
        if(e.receivedAt().isBefore(p.createdAt()) || e.receivedAt().isAfter(now))return "RECEIVED_TIME_INVALID";
        return null;
    }
    private static boolean same(ReconciliationEvidence a,ReconciliationEvidence b) {
        return a.bankBin().equals(b.bankBin()) && a.accountNumber().equals(b.accountNumber())
            && a.bankTransactionReference().equals(b.bankTransactionReference()) && a.receivedAt().equals(b.receivedAt())
            && a.observedAmount().compareTo(b.observedAmount())==0 && a.currency().equals(b.currency())
            && a.transferDescription().equals(b.transferDescription()) && a.transferReference().equals(b.transferReference()) && a.attested().equals(b.attested());
    }
    private EnrollmentRepository.Course owned(long teacher,long course,boolean lock) {
        var c=enrollments.course(course,lock).orElseThrow(()->fail(PAYMENT_NOT_FOUND));
        if(c.teacherId()!=teacher)throw fail(PAYMENT_NOT_FOUND);return c;
    }
    private void eligible(long teacher) {try{teachers.requireEligible(teacher);}catch(TeacherReviewFailure e){throw fail(ACCESS_DENIED);}}
    private static void pagination(int page,int size) {if(page<0 || size<1 || size>100 || (long)page*size>Integer.MAX_VALUE)throw fail(INVALID_REQUEST);}
    private static <T> Page<T> page(List<T> rows,int page,int size) {return new Page<>(rows.stream().limit(size).toList(),page,size,rows.size()>size);}
    private static PaymentFailure fail(PaymentFailure.Reason reason){return new PaymentFailure(reason);}
}
