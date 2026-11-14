package com.englishlearning.features.payment.service;

import java.math.*;
import static com.englishlearning.features.payment.service.PaymentFailure.Reason.PAYMENT_AMOUNT_INVALID;

public record PaymentMoney(BigDecimal original, BigDecimal percent, BigDecimal discount,
                           BigDecimal rounding, BigDecimal payable) {
    public static String decimal(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }
    public static PaymentMoney at(com.englishlearning.features.payment.repository.PaymentRepository.Price price, java.time.Instant now) {
        if(!"VND".equals(price.currency()))throw invalid();
        var percent=price.percent()!=null && price.start()!=null && price.end()!=null
            && !now.isBefore(price.start()) && now.isBefore(price.end())?price.percent():BigDecimal.ZERO;
        return calculate(price.tuition(),percent);
    }
    public static PaymentMoney calculate(BigDecimal tuition, BigDecimal percent) {
        try {
            if(tuition==null || tuition.signum()<=0 || tuition.compareTo(new BigDecimal("9999999999999.99"))>0
                || percent==null || percent.signum()<0 || percent.compareTo(new BigDecimal("100"))>=0) throw invalid();
            tuition=tuition.setScale(2,RoundingMode.UNNECESSARY);
            percent=percent.setScale(2,RoundingMode.UNNECESSARY);
            BigDecimal discount=tuition.multiply(percent).movePointLeft(2);
            BigDecimal exact=tuition.subtract(discount);
            BigDecimal payable=exact.setScale(0,RoundingMode.HALF_UP);
            if(payable.signum()<=0 || payable.compareTo(new BigDecimal("9999999999999"))>0) throw invalid();
            return new PaymentMoney(tuition,percent,discount,payable.subtract(exact),payable);
        } catch(ArithmeticException e) { throw invalid(); }
    }
    private static PaymentFailure invalid() { return new PaymentFailure(PAYMENT_AMOUNT_INVALID); }
}
