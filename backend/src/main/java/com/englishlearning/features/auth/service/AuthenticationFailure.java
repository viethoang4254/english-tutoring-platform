package com.englishlearning.features.auth.service;

public final class AuthenticationFailure extends RuntimeException {
    public enum Reason { INVALID_CREDENTIAL, EMAIL_VERIFICATION_REQUIRED, UNAVAILABLE, INTERNAL_FAILURE, COMMIT_UNCERTAIN }
    private final Reason reason;
    public AuthenticationFailure(Reason reason) {
        super(switch (reason) {
            case INVALID_CREDENTIAL -> "Authentication failed.";
            case EMAIL_VERIFICATION_REQUIRED -> "Email verification required.";
            case UNAVAILABLE -> "Authentication temporarily unavailable.";
            case INTERNAL_FAILURE -> "Authentication operation failed.";
            case COMMIT_UNCERTAIN -> "Authentication outcome could not be confirmed.";
        });
        this.reason = reason;
    }
    public Reason reason() { return reason; }
}
