package com.englishlearning.features.enrollment.dto;

import com.englishlearning.common.api.ApiId;
import com.englishlearning.features.course.dto.PublicCourseDto;
import com.englishlearning.features.course.dto.ScheduleDto;
import com.englishlearning.features.payment.dto.PaymentDto;
import com.englishlearning.features.payment.service.PaymentMoney;
import java.time.Instant;
import java.util.List;

public record CheckoutDto(PublicCourseDto course, List<ScheduleDto.Rule> rules, Price price,
        EnrollmentDto enrollment, ApiId paymentId, boolean available, String blockedReason, Instant calculatedAt) {
    public record Price(String originalAmount,String discountPercent,String exactDiscountAmount,
                        String roundingAdjustment,String finalAmount,String currency) {
        public static Price from(PaymentMoney m) {
            return new Price(PaymentMoney.decimal(m.original()),PaymentMoney.decimal(m.percent()),PaymentMoney.decimal(m.discount()),
                PaymentMoney.decimal(m.rounding()),PaymentMoney.decimal(m.payable()),"VND");
        }
        public static Price from(PaymentDto p) {
            return new Price(p.originalAmount(),p.discountPercent(),p.exactDiscountAmount(),p.roundingAdjustment(),p.finalAmount(),p.currency());
        }
    }
}
