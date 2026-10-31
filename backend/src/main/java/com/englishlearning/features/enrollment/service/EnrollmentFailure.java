package com.englishlearning.features.enrollment.service;

public class EnrollmentFailure extends RuntimeException {
    public enum Reason { INVALID_REQUEST, ACCESS_DENIED, ENROLLMENT_NOT_FOUND, COURSE_NOT_FOUND,
        COURSE_NOT_ENROLLABLE, COURSE_FULL, ENROLLMENT_RENEWAL_REQUIRED, ENROLLMENT_STATE_CONFLICT,
        PAYMENT_COORDINATION_REQUIRED }
    private final Reason reason;
    public EnrollmentFailure(Reason reason) { super("Enrollment request could not be completed."); this.reason = reason; }
    public Reason reason() { return reason; }
}
