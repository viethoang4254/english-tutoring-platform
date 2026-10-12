package com.englishlearning.features.auth.security;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;

class RefreshTokensTests {
    private final RefreshTokens tokens = new RefreshTokens();
    @Test void generatesCanonicalIndependentCredentialsAndOnlyHashesThem() throws Exception {
        String first = tokens.generate();
        assertEquals(43, first.length());
        assertEquals(32, Base64.getUrlDecoder().decode(first).length);
        assertNotEquals(first, tokens.generate());
        String hash = tokens.hash(first);
        assertTrue(hash.matches("[0-9a-f]{64}"));
        assertEquals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(first.getBytes(StandardCharsets.US_ASCII))), hash);
        assertEquals(hash, tokens.hash(first));
    }
    @Test void rejectsNonCanonicalCredentialsWithoutEchoingThem() {
        for (String token : new String[]{null, "", "a".repeat(42), "a".repeat(44),
                "A".repeat(43) + "=", " " + "A".repeat(43), "A".repeat(42) + "B",
                "+".repeat(43), "/".repeat(43), "é".repeat(43)}) {
            assertEquals("Invalid refresh credential.",
                    assertThrows(IllegalArgumentException.class, () -> tokens.hash(token)).getMessage());
        }
    }
    @Test void canonicalZeroBytesHaveAStableKnownHash() throws Exception {
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[32]);
        assertEquals("A".repeat(43), token);
        assertEquals(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.US_ASCII))), tokens.hash(token));
    }
}
