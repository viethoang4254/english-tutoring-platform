package com.englishlearning.features.googlecalendar;

public final class GoogleFailure extends RuntimeException {
    public final String code;
    public GoogleFailure(String code) { super("Google connection unavailable"); this.code=code; }
}
