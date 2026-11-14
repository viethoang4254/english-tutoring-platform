package com.englishlearning.features.enrollment.service;

import com.englishlearning.features.enrollment.dto.CheckoutDto;
import com.englishlearning.features.enrollment.repository.EnrollmentRepository;
import com.englishlearning.features.course.repository.CourseRepository;
import com.englishlearning.features.payment.repository.PaymentRepository;
import com.englishlearning.features.payment.service.PaymentMoney;
import com.englishlearning.features.paymentaccount.service.*;
import com.englishlearning.features.teacher.service.*;
import com.englishlearning.features.user.entity.User.Role;
import com.englishlearning.features.user.service.CurrentAccountService;
import java.time.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import static com.englishlearning.features.enrollment.service.EnrollmentFailure.Reason.*;

@Service
public class CheckoutService {
    private final EnrollmentRepository enrollments;
    private final CourseRepository courses;
    private final PaymentRepository payments;
    private final CurrentAccountService accounts;
    private final TeacherBusinessEligibility teachers;
    private final BankDirectory banks;
    private final Clock clock;
    public CheckoutService(EnrollmentRepository enrollments,CourseRepository courses,PaymentRepository payments,
            CurrentAccountService accounts,TeacherBusinessEligibility teachers,BankDirectory banks,
            @Qualifier("refreshSessionClock") Clock clock) {
        this.enrollments=enrollments;this.courses=courses;this.payments=payments;this.accounts=accounts;
        this.teachers=teachers;this.banks=banks;this.clock=clock;
    }
    // Preview must never expire payments, reserve seats or acquire write locks.
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public CheckoutDto get(long student,long courseId) {
        var actor=accounts.findById(student).orElseThrow(()->new EnrollmentFailure(ACCESS_DENIED));
        if(actor.locked() || actor.role()!=Role.STUDENT)throw new EnrollmentFailure(ACCESS_DENIED);
        var course=courses.publicFind(courseId).orElseThrow(()->new EnrollmentFailure(COURSE_NOT_FOUND));
        var parent=enrollments.course(courseId,false).orElseThrow(()->new EnrollmentFailure(COURSE_NOT_FOUND));
        Instant now=clock.instant();
        var enrollment=enrollments.readForCourse(student,courseId,now).orElse(null);
        var payment=enrollment==null?null:payments.latestId(student,enrollment.id().value())
            .flatMap(id->payments.own(student,id)).orElse(null);
        boolean pending=payment!=null && payment.status().equals("PENDING") && payment.expiresAt().isAfter(now);
        if(pending && payment.qrPayload()==null)throw new com.englishlearning.features.payment.service.PaymentFailure(
            com.englishlearning.features.payment.service.PaymentFailure.Reason.PAYMENT_LEGACY_UNAVAILABLE);
        var price=pending?CheckoutDto.Price.from(payment):CheckoutDto.Price.from(PaymentMoney.at(payments.price(courseId),now));
        String reason=null;
        if(enrollment!=null) {
            if(!enrollment.status().equals("PENDING"))reason="ENROLLMENT_"+enrollment.status();
            else if(enrollment.reservationExpired())reason="ENROLLMENT_RENEWAL_REQUIRED";
        }
        if(reason==null) {
            Instant first=enrollments.firstStart(courseId);
            if(!parent.status().equals("PUBLISHED") || first==null || !first.isAfter(now))reason="COURSE_NOT_ENROLLABLE";
            else if(enrollments.occupied(courseId,now)-(enrollment==null?0:1)>=parent.capacity())reason="COURSE_FULL";
        }
        if(reason==null) {
            try { teachers.requireEligible(parent.teacherId()); }
            catch(TeacherReviewFailure failure) { reason="RECEIVING_ACCOUNT_UNAVAILABLE"; }
            if(reason==null && !pending) {
                var destination=payments.destination(parent.teacherId(),false);
                if(destination.isEmpty())reason="RECEIVING_ACCOUNT_UNAVAILABLE";
                else try { banks.require(destination.get().bin()); }
                catch(ReceivingFailure failure) { reason="RECEIVING_ACCOUNT_UNAVAILABLE"; }
            }
        }
        return new CheckoutDto(course,courses.rules(courseId),price,enrollment,payment==null?null:payment.id(),reason==null,reason,now);
    }
}
