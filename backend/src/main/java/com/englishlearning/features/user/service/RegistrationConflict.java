package com.englishlearning.features.user.service;

public final class RegistrationConflict extends RuntimeException {
    public RegistrationConflict() { super("Registration conflict"); }
}
