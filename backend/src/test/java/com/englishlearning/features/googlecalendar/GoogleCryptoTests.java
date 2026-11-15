package com.englishlearning.features.googlecalendar;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.Base64;

class GoogleCryptoTests {
    @TempDir Path folder;
    @Test void authenticatedEncryptionIsRandomAndBoundToOwner()throws Exception{
        var crypto=new GoogleCrypto(GoogleTestSettings.create(folder));
        String first=crypto.encrypt("synthetic-refresh","refresh:1:subject"),second=crypto.encrypt("synthetic-refresh","refresh:1:subject");
        assertNotEquals(first,second);assertFalse(first.contains("synthetic-refresh"));
        assertEquals("synthetic-refresh",crypto.decrypt(first,"test-1","refresh:1:subject"));
        assertThrows(GoogleFailure.class,()->crypto.decrypt(first,"test-1","refresh:2:subject"));
        assertThrows(GoogleFailure.class,()->crypto.decrypt(first,"old-key","refresh:1:subject"));
        byte[] changed=Base64.getDecoder().decode(first);changed[changed.length-1]^=1;
        assertThrows(GoogleFailure.class,()->crypto.decrypt(Base64.getEncoder().encodeToString(changed),"test-1","refresh:1:subject"));
    }
    @Test void missingAndInvalidConfigurationSafelyDisableIntegration(){
        var env=new org.springframework.mock.env.MockEnvironment();
        assertFalse(new GoogleSettings(env).enabled);
        env.withProperty("APP_GOOGLE_CALENDAR_ENABLED","true").withProperty("app.security.browser.allowed-origins","https://attacker.example@frontend.example/path");
        var settings=new GoogleSettings(env);assertFalse(settings.enabled);assertEquals("",settings.frontend);
        assertThrows(GoogleFailure.class,settings::key);
    }
    @Test void secretsAndHashesHaveExpectedEntropyAndEncoding()throws Exception{
        var crypto=new GoogleCrypto(GoogleTestSettings.create(folder));String first=crypto.secret();
        assertTrue(first.matches("[A-Za-z0-9_-]{43}"));assertNotEquals(first,crypto.secret());
        assertEquals(43,GoogleCrypto.hash(first).length());assertNotEquals(first,GoogleCrypto.hash(first));
    }
}
