package com.englishlearning.features.auth.security;

import java.nio.charset.StandardCharsets;

public final class PasswordInput {
    private PasswordInput() {}

    public static boolean isValid(CharSequence input) {
        if (input == null) return false;
        String value = input.toString();
        int count = value.codePointCount(0, value.length());
        if (count < 8 || count > 64) return false;
        // Reject malformed UTF-16 rather than silently replacing it during UTF-8 encoding.
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isHighSurrogate(c)) {
                if (++i >= value.length() || !Character.isLowSurrogate(value.charAt(i))) return false;
            } else if (Character.isLowSurrogate(c)) {
                return false;
            }
        }
        return value.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    public static void validate(CharSequence input) {
        if (!isValid(input)) throw new IllegalArgumentException("Invalid password input.");
    }
}
