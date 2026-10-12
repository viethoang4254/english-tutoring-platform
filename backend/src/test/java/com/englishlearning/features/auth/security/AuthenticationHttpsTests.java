package com.englishlearning.features.auth.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockCookie;

@SpringBootTest(properties={
        "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
        "spring.flyway.enabled=false",
        "app.security.browser.allowed-origins=https://web.example.test"})
class AuthenticationHttpsTests extends AuthenticationHttpTestSupport {
    String origin(){return "https://web.example.test";}
    String api(){return "https://api.example.test";}
    boolean secure(){return true;}
    @Test void httpsSameSiteCrossOriginBootstrapAndLoginUseSecureHostOnlyCookies() throws Exception {
        var csrf=bootstrap();
        var response=mvc.perform(csrf(postAuth("login"),csrf).contentType(MediaType.APPLICATION_JSON).content(LOGIN))
                .andExpect(status().isOk()).andReturn().getResponse();
        var cookie=MockCookie.parse(refreshHeader(response.getHeaders("Set-Cookie")));
        assertTrue(cookie.getSecure()); assertTrue(cookie.isHttpOnly()); assertNull(cookie.getDomain());
        assertTrue(response.getHeaders("Set-Cookie").stream().anyMatch(s->s.contains("SameSite=Lax")));
    }
    @Test void httpsConfigurationDoesNotPermitHttpCredentialTransport() throws Exception {
        mvc.perform(get("http://api.example.test/api/v1/auth/csrf")).andExpect(status().isForbidden());
    }
}
