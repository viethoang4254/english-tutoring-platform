package com.englishlearning.features.googlecalendar;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.*;
import org.springframework.stereotype.Component;

@Component
public final class GoogleCrypto {
    private final GoogleSettings settings;
    private final SecureRandom random=new SecureRandom();
    public GoogleCrypto(GoogleSettings settings){this.settings=settings;}
    public String secret(){byte[] bytes=new byte[32];random.nextBytes(bytes);return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);}
    public static String hash(String value){
        try{return Base64.getUrlEncoder().withoutPadding().encodeToString(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}
        catch(GeneralSecurityException e){throw new IllegalStateException("SHA-256 unavailable");}
    }
    public String encrypt(String value,String context){
        try{
            byte[] iv=new byte[12];random.nextBytes(iv);
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(settings.key(),"AES"),new GCMParameterSpec(128,iv));
            cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));
            byte[] encrypted=cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] result=Arrays.copyOf(iv,iv.length+encrypted.length);System.arraycopy(encrypted,0,result,iv.length,encrypted.length);
            return Base64.getEncoder().encodeToString(result);
        }catch(GeneralSecurityException e){throw new GoogleFailure("UNAVAILABLE");}
    }
    public String decrypt(String value,String keyId,String context){
        if(!settings.keyId.equals(keyId))throw new GoogleFailure("RECONNECT_REQUIRED");
        try{
            byte[] data=Base64.getDecoder().decode(value);if(data.length<29)throw new GeneralSecurityException();
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,new SecretKeySpec(settings.key(),"AES"),new GCMParameterSpec(128,data,0,12));
            cipher.updateAAD(context.getBytes(StandardCharsets.UTF_8));
            return new String(cipher.doFinal(data,12,data.length-12),StandardCharsets.UTF_8);
        }catch(GeneralSecurityException|IllegalArgumentException e){throw new GoogleFailure("RECONNECT_REQUIRED");}
    }
}
