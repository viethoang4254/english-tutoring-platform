package com.englishlearning.features.teacher.service;

public final class TeacherReviewFailure extends RuntimeException {
    public enum Reason { DENIED, NOT_FOUND, ONBOARDING_NOT_FOUND, ALREADY_REVIEWED, PROFILE_EXISTS, INVALID_APPLICANT, INVALID_REQUEST }
    private final Reason reason;
    public TeacherReviewFailure(Reason reason) { super("Teacher application request failed."); this.reason = reason; }
    public Reason reason() { return reason; }
}
