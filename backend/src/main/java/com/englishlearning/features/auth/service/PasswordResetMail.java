package com.englishlearning.features.auth.service;

@FunctionalInterface
public interface PasswordResetMail {
    void sendReset(String email, String token);
}
