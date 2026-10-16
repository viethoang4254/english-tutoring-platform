package com.englishlearning.features.user.service;
public final class ProfileFailure extends RuntimeException {
    public enum Reason { INVALID, UNAUTHENTICATED, DENIED }
    private final Reason reason;
    public ProfileFailure(Reason reason){super("Profile request rejected.");this.reason=reason;}
    public Reason reason(){return reason;}
}
