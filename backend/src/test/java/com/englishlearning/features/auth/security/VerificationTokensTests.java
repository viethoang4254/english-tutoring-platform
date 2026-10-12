package com.englishlearning.features.auth.security;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class VerificationTokensTests {
    @Test void randomCanonicalPurposeSeparatedHashOnlyCredentials() {
        var tokens = new VerificationTokens();
        String token = tokens.generate();
        assertEquals(43, token.length());
        assertNotEquals(token, tokens.generate());
        assertTrue(tokens.hash(token).matches("[0-9a-f]{64}"));
        assertEquals(tokens.hash(token), tokens.hash(token));
        assertNotEquals(new RefreshTokens().hash(token), tokens.hash(token));
    }
    @Test void rejectsMalformedTokensSafely() {
        var tokens = new VerificationTokens();
        for (String token : new String[]{null,"","A".repeat(42),"A".repeat(42)+"B","A".repeat(43)+"="}) {
            var error = assertThrows(IllegalArgumentException.class, () -> tokens.hash(token));
            assertEquals("Invalid verification credential.", error.getMessage());
            assertNull(error.getCause());
        }
    }
}
