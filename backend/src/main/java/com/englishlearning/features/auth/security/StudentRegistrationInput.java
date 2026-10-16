package com.englishlearning.features.auth.security;

import java.util.Locale;

public final class StudentRegistrationInput {
    private StudentRegistrationInput() {}
    public static String username(String value) {
        if (value == null) throw new IllegalArgumentException("Invalid registration input");
        String canonical = value.replaceAll("^ +| +$", "");
        if (!canonical.matches("[A-Za-z][A-Za-z0-9_]{2,29}"))
            throw new IllegalArgumentException("Invalid registration input");
        return canonical.toLowerCase(Locale.ROOT);
    }
    public static String phone(String value) {
        if (value == null) throw new IllegalArgumentException("Invalid registration input");
        String canonical = value.replaceAll("^ +| +$", "");
        if (!canonical.matches("(?:0|\\+84)[35789][0-9]{8}"))
            throw new IllegalArgumentException("Invalid registration input");
        return canonical.startsWith("0") ? "+84" + canonical.substring(1) : canonical;
    }
}
