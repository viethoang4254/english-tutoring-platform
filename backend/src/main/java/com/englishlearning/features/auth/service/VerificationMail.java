package com.englishlearning.features.auth.service;

@FunctionalInterface
public interface VerificationMail {
    void send(String email, String token);
}
