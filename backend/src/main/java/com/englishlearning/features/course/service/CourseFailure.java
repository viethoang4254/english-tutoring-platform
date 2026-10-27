package com.englishlearning.features.course.service;

public class CourseFailure extends RuntimeException {
    public enum Reason { INVALID, DENIED, MISSING, CATEGORY_MISSING, CATEGORY_INACTIVE, NOT_DRAFT,
        SCHEDULE_LOCKED, SCHEDULE_OVERLAP, SCHEDULE_TIME, GENERATION_LIMIT, ALREADY_GENERATED,
        SESSION_MISSING, SESSION_NOT_EDITABLE, SESSION_CHANGED, CALENDAR_SYNC_REQUIRED }
    private final Reason reason;
    public CourseFailure(Reason reason) { super("Course request could not be completed."); this.reason = reason; }
    public Reason reason() { return reason; }
    public static CourseFailure invalid() { return new CourseFailure(Reason.INVALID); }
}
