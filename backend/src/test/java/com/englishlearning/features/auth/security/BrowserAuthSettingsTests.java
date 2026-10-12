package com.englishlearning.features.auth.security;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class BrowserAuthSettingsTests {
    @Test void rejectsWildcardsInsecureProductionAndMalformedOrigins() {
        for(String origin:List.of("*","https://*.example.test","http://localhost:3000",
                "https://example.test/path","https://user:secret@example.test","https://example.test?q=1","null")) {
            var e=assertThrows(IllegalArgumentException.class,()->new BrowserAuthSettings(List.of(origin),false));
            assertEquals("Invalid browser security configuration.",e.getMessage());
            assertNull(e.getCause());
        }
    }
    @Test void localExceptionRequiresOnlyLoopbackHttpOrigins() {
        for(String origin:List.of("http://example.test","https://example.test","http://localhost.evil:3000")) {
            assertThrows(IllegalArgumentException.class,()->new BrowserAuthSettings(List.of(origin),true));
        }
        assertFalse(new BrowserAuthSettings(List.of("http://localhost:3000"),true).secure());
        assertTrue(new BrowserAuthSettings(List.of("https://web.example.test"),false).secure());
        assertTrue(new BrowserAuthSettings(List.of(""),false).origins().isEmpty());
    }
}
