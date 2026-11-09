package com.englishlearning.features.payment.service;

public class PaymentFailure extends RuntimeException {
    public enum Reason { INVALID_REQUEST, ACCESS_DENIED, PAYMENT_NOT_FOUND, ENROLLMENT_NOT_FOUND,
        ENROLLMENT_NOT_PAYABLE, RESERVATION_EXPIRED, COURSE_NOT_PAYABLE, COURSE_FULL,
        RECEIVING_ACCOUNT_UNAVAILABLE, PAYMENT_AMOUNT_INVALID, PAYMENT_LEGACY_UNAVAILABLE }
    private final Reason reason;
    public PaymentFailure(Reason reason) { super(reason.name()); this.reason=reason; }
    public Reason reason() { return reason; }
}
