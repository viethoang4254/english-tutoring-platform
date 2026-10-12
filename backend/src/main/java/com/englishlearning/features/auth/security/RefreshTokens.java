package com.englishlearning.features.auth.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokens {
    private final SecureRandom random = new SecureRandom();
    public String generate() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    public String hash(String credential) {
        if (credential == null || !credential.matches("[A-Za-z0-9_-]{43}")) {
            throw new IllegalArgumentException("Invalid refresh credential.");
        }
        byte[] decoded = Base64.getUrlDecoder().decode(credential);
        if (decoded.length != 32 || !Base64.getUrlEncoder().withoutPadding().encodeToString(decoded).equals(credential)) {
            throw new IllegalArgumentException("Invalid refresh credential.");
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(credential.getBytes(StandardCharsets.US_ASCII)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Required digest unavailable.");
        }
    }
}
