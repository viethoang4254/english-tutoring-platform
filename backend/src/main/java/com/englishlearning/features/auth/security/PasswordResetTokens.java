package com.englishlearning.features.auth.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PasswordResetTokens {
    private final SecureRandom random = new SecureRandom();
    private final byte[] key;
    public PasswordResetTokens(@Value("${app.security.password-reset.hmac-secret:}") String secret) {
        try { key=Base64.getDecoder().decode(secret); if(key.length<32) throw new IllegalArgumentException(); }
        catch(RuntimeException invalid) { throw new IllegalStateException("Invalid password recovery key configuration."); }
    }
    public String generate() { return String.format(Locale.ROOT,"%06d",random.nextInt(1_000_000)); }
    public String hash(long userId,Instant issuedAt,String code) {
        if(code==null || !code.matches("[0-9]{6}")) throw new IllegalArgumentException("Invalid recovery code.");
        try {
            Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(key,"HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(("PASSWORD_RESET:"+userId+":"+issuedAt+":"+code).getBytes(StandardCharsets.US_ASCII)));
        } catch(java.security.GeneralSecurityException impossible) { throw new IllegalStateException("Recovery signing unavailable."); }
    }
    public boolean matches(long id,Instant issuedAt,String code,String hash) {
        if(code==null || !code.matches("[0-9]{6}")) return false;
        return MessageDigest.isEqual(hash(id,issuedAt,code).getBytes(StandardCharsets.US_ASCII),hash.getBytes(StandardCharsets.US_ASCII));
    }
}
