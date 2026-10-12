package com.englishlearning.features.auth.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class VerificationTokens {
    private final RefreshTokens random = new RefreshTokens();
    public String generate() { return random.generate(); }
    public String hash(String token) {
        try {
            random.hash(token); // Reuse canonical 32-byte Base64URL validation.
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(("INITIAL_VERIFICATION:" + token).getBytes(StandardCharsets.US_ASCII)));
        } catch (IllegalArgumentException invalid) {
            throw new IllegalArgumentException("Invalid verification credential.");
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("Required digest unavailable.");
        }
    }
}
