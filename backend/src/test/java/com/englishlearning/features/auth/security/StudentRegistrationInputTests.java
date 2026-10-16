package com.englishlearning.features.auth.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StudentRegistrationInputTests {
    @Test void canonicalUsernameAndBoundaries() {
        assertEquals("student_01", StudentRegistrationInput.username(" Student_01 "));
        assertEquals("abc", StudentRegistrationInput.username("AbC"));
        assertEquals("a".repeat(30), StudentRegistrationInput.username("A".repeat(30)));
        for (String value : new String[]{"", "ab", "a".repeat(31), "1abc", "_abc", "ab-c", "a bc", "ábc", "abc\n", "abc\0"})
            assertThrows(IllegalArgumentException.class, () -> StudentRegistrationInput.username(value));
        assertThrows(IllegalArgumentException.class, () -> StudentRegistrationInput.username(null));
    }
    @Test void vietnameseMobileFormatsAndInvalidValues() {
        for (String prefix : new String[]{"3", "5", "7", "8", "9"}) {
            assertEquals("+84"+prefix+"12345678", StudentRegistrationInput.phone(" 0"+prefix+"12345678 "));
            assertEquals("+84"+prefix+"12345678", StudentRegistrationInput.phone("+84"+prefix+"12345678"));
        }
        for (String value : new String[]{"", "0212345678", "091234567", "09123456789", "+840912345678", "091 2345678", "+12025550123", "0912345678\n"})
            assertThrows(IllegalArgumentException.class, () -> StudentRegistrationInput.phone(value));
        assertThrows(IllegalArgumentException.class, () -> StudentRegistrationInput.phone(null));
    }
}
