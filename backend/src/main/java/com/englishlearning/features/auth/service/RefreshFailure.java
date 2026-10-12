package com.englishlearning.features.auth.service;

public final class RefreshFailure extends RuntimeException {
    public enum Reason { INVALID_CREDENTIAL, UNAVAILABLE, INTERNAL_FAILURE, COMMIT_UNCERTAIN }
    private final Reason reason;
    public RefreshFailure(Reason reason) {
        super(switch (reason) {
            case INVALID_CREDENTIAL -> "Invalid refresh credential.";
            case UNAVAILABLE -> "Refresh operation unavailable.";
            case INTERNAL_FAILURE -> "Refresh operation failed.";
            case COMMIT_UNCERTAIN -> "Refresh outcome could not be confirmed.";
        });
        this.reason = reason;
    }
    public Reason reason() { return reason; }
}
