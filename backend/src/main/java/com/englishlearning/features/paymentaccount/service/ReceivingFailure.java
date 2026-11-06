package com.englishlearning.features.paymentaccount.service;
public final class ReceivingFailure extends RuntimeException {
    public enum Reason { INVALID, DENIED, MISSING, CONFLICT, IDEMPOTENCY_CONFLICT, UNAVAILABLE }
    private final Reason reason;
    public ReceivingFailure(Reason reason) { super("Receiving account request failed."); this.reason=reason; }
    public Reason reason() { return reason; }
}
