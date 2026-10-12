package com.englishlearning.features.auth.service;

public final class VerificationFailure extends RuntimeException {
    public enum Reason { CONFLICT, INVALID, RATE_LIMITED, DELIVERY, UNAVAILABLE, OUTCOME_UNKNOWN, INTERNAL }
    private final Reason reason;
    public VerificationFailure(Reason reason) { super("Verification request could not be completed."); this.reason = reason; }
    public Reason reason() { return reason; }
}
