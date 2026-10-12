package com.englishlearning.features.auth.security;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordInfrastructureTests {
    private final PasswordEncoder encoder = new PasswordConfiguration().passwordEncoder();

    @Test void hashesWithCost12AndIndependentSalts() {
        String first = encoder.encode("password example");
        String second = encoder.encode("password example");
        assertTrue(first.matches("\\{bcrypt\\}\\$2a\\$12\\$[./A-Za-z0-9]{53}"));
        assertNotEquals(first, second);
        assertTrue(encoder.matches("password example", first));
        assertFalse(encoder.matches("wrong password", first));
    }

    @Test void enforcesCodePointAndUtf8Boundaries() {
        assertFalse(PasswordInput.isValid("a".repeat(7)));
        assertTrue(PasswordInput.isValid("a".repeat(8)));
        assertTrue(PasswordInput.isValid("a".repeat(64)));
        assertFalse(PasswordInput.isValid("a".repeat(65)));
        assertTrue(PasswordInput.isValid("é".repeat(36)));
        assertFalse(PasswordInput.isValid("é".repeat(36) + "a"));
        assertTrue(PasswordInput.isValid("😀".repeat(18)));
        assertFalse(PasswordInput.isValid("😀".repeat(19)));
        assertFalse(PasswordInput.isValid("😀".repeat(7)));
        assertTrue(PasswordInput.isValid("😀".repeat(8)));
        assertFalse(PasswordInput.isValid(null));
        assertFalse(PasswordInput.isValid("abcdefgh" + (char) 0xD800));
        assertFalse(PasswordInput.isValid("abcdefgh" + (char) 0xDC00));
    }

    @Test void roundTripsBoundaryAndUnicodePasswords() {
        for (String value : new String[]{"a".repeat(8), "a".repeat(64), "😀".repeat(18), "Mật khẩu tốt"}) {
            assertTrue(encoder.matches(value, encoder.encode(value)));
        }
    }

    @Test void preservesWhitespaceCaseAndUnicodeForm() {
        String value = " Password ";
        String hash = encoder.encode(value);
        assertTrue(encoder.matches(value, hash));
        assertFalse(encoder.matches(value.trim(), hash));
        assertFalse(encoder.matches(value.toLowerCase(java.util.Locale.ROOT), hash));
        String composed = "éabcdefg";
        assertFalse(encoder.matches("e\\u0301abcdefg", encoder.encode(composed)));
        assertTrue(encoder.matches("        ", encoder.encode("        ")));
    }

    @Test void rejectsInvalidInputBeforeHashingWithoutEchoingIt() {
        for (String value : new String[]{null, "short", "a".repeat(65), "é".repeat(37)}) {
            var error = assertThrows(IllegalArgumentException.class, () -> encoder.encode(value));
            assertEquals("Invalid password input.", error.getMessage());
        }
    }

    @Test void rejectsMalformedUnsupportedAndPlaintextHashes() {
        for (String hash : new String[]{"", "password example", "{noop}password example",
                "{argon2}invalid", "{bcrypt}invalid", "$2a$12$" + "a".repeat(53),
                "{bcrypt}$2a$99$" + "a".repeat(53), "{bcrypt}$2a$12$" + "!".repeat(53)}) {
            assertFalse(encoder.matches("password example", hash));
        }
        assertFalse(encoder.matches("password example", null));
        String valid = encoder.encode("password example");
        assertFalse(encoder.matches(null, valid));
        assertFalse(encoder.matches("é".repeat(37), valid));
        assertFalse(encoder.matches("password example", valid + "extra"));
    }
}
