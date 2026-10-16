package com.englishlearning.features.auth.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.englishlearning.features.auth.service.*;
import jakarta.servlet.http.Cookie;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockCookie;

@SpringBootTest(properties={
        "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
        "spring.flyway.enabled=false",
        "app.security.browser.local-http=true",
        "app.security.browser.allowed-origins=http://localhost:3000"})
class AuthenticationHttpTests extends AuthenticationHttpTestSupport {
    String origin(){return "http://localhost:3000";}
    String api(){return "http://localhost:8080";}
    boolean secure(){return false;}

    @Test void realBootstrapHeaderCookieExchangeLogsInWithExactDto() throws Exception {
        var csrf=bootstrap();
        var response=mvc.perform(csrf(postAuth("login"),csrf).contentType(MediaType.APPLICATION_JSON).content(LOGIN))
                .andExpect(status().isOk()).andReturn().getResponse();
        var json=mapper.readTree(response.getContentAsString());
        assertEquals(Set.of("accessToken","tokenType","issuedAt","expiresAt"),
                mapper.convertValue(json,Map.class).keySet());
        assertEquals("Bearer",json.get("tokenType").asString());
        assertEquals(now.toString(),json.get("issuedAt").asString());
        assertEquals("no-store",response.getHeader("Cache-Control"));
        String header=refreshHeader(response.getHeaders("Set-Cookie"));
        var cookie=MockCookie.parse(header);
        assertEquals(refresh,cookie.getValue());
        assertTrue(cookie.isHttpOnly()); assertFalse(cookie.getSecure());
        assertNull(cookie.getDomain()); assertEquals("/api/v1/auth",cookie.getPath());
        assertEquals(604800,cookie.getMaxAge()); assertTrue(header.contains("SameSite=Lax"));
        assertFalse(response.getContentAsString().contains(refresh));
        verify(workflows).login("person@example.invalid"," test password ");
        assertNull(response.getCookie("JSESSIONID"));
    }
    @ParameterizedTest @ValueSource(strings={"login","refresh","logout"})
    void missingCsrfDoesNotMutate(String path) throws Exception {
        mvc.perform(postAuth(path).contentType(MediaType.APPLICATION_JSON).content(path.equals("login")?LOGIN:""))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        verifyNoInteractions(workflows);
    }
    @Test void invalidCookieHeaderAndParameterFallbackAreRejected() throws Exception {
        var csrf=bootstrap();
        mvc.perform(postAuth("logout").cookie(csrf.cookie()).header("X-XSRF-TOKEN","wrong"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        mvc.perform(postAuth("logout").header("X-XSRF-TOKEN",csrf.token()))
                .andExpect(status().isForbidden());
        mvc.perform(postAuth("logout").cookie(csrf.cookie()).param("_csrf",csrf.token()))
                .andExpect(status().isForbidden());
        verifyNoInteractions(workflows);
    }
    @Test void csrfPrecedesInputValidation() throws Exception {
        mvc.perform(postAuth("login").contentType(MediaType.APPLICATION_JSON).content("{broken"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        verifyNoInteractions(workflows);
    }
    @ParameterizedTest @ValueSource(strings={
            "{}",
            "{\"email\":null,\"password\":\"password\"}",
            "{\"email\":\"bad\",\"password\":\"password\"}",
            "{\"email\":\"a@example.invalid\",\"password\":null}",
            "{\"email\":\"a@example.invalid\",\"password\":12345678}",
            "{\"email\":\"a@example.invalid\",\"password\":\"short\"}",
            "{\"email\":\"a@example.invalid\",\"password\":\"password\",\"role\":\"ADMIN\"}",
            "{\"email\":\"a@example.invalid\",\"password\":\"password\",\"refreshToken\":\"secret\"}",
            "{broken"})
    void invalidLoginDtoIsSafe400(String body) throws Exception {
        mvc.perform(csrf(postAuth("login"),bootstrap()).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verifyNoInteractions(workflows);
    }
    @Test void queryAndBodyAreNotAlternativeCredentialTransports() throws Exception {
        var csrf=bootstrap();
        for(String path:List.of("refresh","logout")) {
            mvc.perform(csrf(postAuth(path).queryParam("refreshToken","secret"),csrf))
                    .andExpect(status().isBadRequest());
            mvc.perform(csrf(postAuth(path),csrf).content(" "))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(csrf(postAuth("login").queryParam("userId","2"),csrf)
                .contentType(MediaType.APPLICATION_JSON).content(LOGIN)).andExpect(status().isBadRequest());
        mvc.perform(getAuth("csrf").queryParam("token","secret")).andExpect(status().isBadRequest());
        verifyNoInteractions(workflows);
    }
    @Test void refreshUsesCookieOnlyAndDoesNotRequireValidAccessJwt() throws Exception {
        var response=mvc.perform(csrf(postAuth("refresh"),bootstrap())
                .cookie(new Cookie("refresh_token",refresh)).header("Authorization","Bearer expired"))
                .andExpect(status().isOk()).andReturn().getResponse();
        assertNotNull(refreshHeader(response.getHeaders("Set-Cookie")));
        verify(workflows).refresh(refresh);
        verifyNoInteractions(decoder,accounts);
    }
    @Test void duplicateRefreshCookiesAreRejected() throws Exception {
        mvc.perform(csrf(postAuth("refresh"),bootstrap()).cookie(
                new Cookie("refresh_token",refresh),new Cookie("refresh_token","other")))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(workflows);
    }
    @Test void logoutClearsCookieWithoutBearerAndMissingCookieIsAllowed() throws Exception {
        var csrf=bootstrap();
        var response=mvc.perform(csrf(postAuth("logout"),csrf).cookie(new Cookie("refresh_token",refresh)))
                .andExpect(status().isNoContent()).andReturn().getResponse();
        var cookie=MockCookie.parse(refreshHeader(response.getHeaders("Set-Cookie")));
        assertEquals(0,cookie.getMaxAge()); assertEquals("",cookie.getValue());
        assertNull(cookie.getDomain()); assertEquals("/api/v1/auth",cookie.getPath());
        verify(workflows).logout(refresh);
        mvc.perform(csrf(postAuth("logout"),csrf)).andExpect(status().isNoContent());
        verify(workflows).logout(null);
        verifyNoInteractions(decoder,accounts);
    }
    @Test void safeFailureMappingNeverSetsOrClearsRefreshCookie() throws Exception {
        var csrf=bootstrap();
        var reasons=AuthenticationFailure.Reason.values();
        for(var reason:reasons) {
            doThrow(new AuthenticationFailure(reason)).when(workflows).login(anyString(),anyString());
            int expected=switch(reason){
                case INVALID_CREDENTIAL->401;
                case EMAIL_VERIFICATION_REQUIRED,ACCESS_DENIED->403;
                case INVALID_REQUEST->400;
                case UNAVAILABLE,COMMIT_UNCERTAIN->503;
                default->500;
            };
            String code=switch(reason){
                case INVALID_CREDENTIAL->"AUTHENTICATION_REQUIRED";
                case EMAIL_VERIFICATION_REQUIRED->"EMAIL_VERIFICATION_REQUIRED";
                case ACCESS_DENIED->"ACCESS_DENIED";
                case INVALID_REQUEST->"INVALID_REQUEST";
                case UNAVAILABLE->"AUTHENTICATION_UNAVAILABLE";
                case COMMIT_UNCERTAIN->"AUTHENTICATION_OUTCOME_UNKNOWN";
                default->"INTERNAL_ERROR";
            };
            var response=mvc.perform(csrf(postAuth("login"),csrf).contentType(MediaType.APPLICATION_JSON).content(LOGIN))
                    .andExpect(status().is(expected)).andExpect(jsonPath("$.code").value(code))
                    .andReturn().getResponse();
            assertNull(refreshHeader(response.getHeaders("Set-Cookie")));
            assertFalse(response.getContentAsString().contains("password"));
            assertEquals(2,mapper.readTree(response.getContentAsString()).size());
        }
    }
    @Test void refreshFailureDoesNotEraseNewerCookieAndLogoutFailureIsNotSuccess() throws Exception {
        var csrf=bootstrap();
        doThrow(new AuthenticationFailure(AuthenticationFailure.Reason.INVALID_CREDENTIAL)).when(workflows).refresh(any());
        var response=mvc.perform(csrf(postAuth("refresh"),csrf).cookie(new Cookie("refresh_token",refresh)))
                .andExpect(status().isUnauthorized()).andReturn().getResponse();
        assertNull(refreshHeader(response.getHeaders("Set-Cookie")));
        doThrow(new AuthenticationFailure(AuthenticationFailure.Reason.UNAVAILABLE)).when(workflows).logout(any());
        response=mvc.perform(csrf(postAuth("logout"),csrf).cookie(new Cookie("refresh_token",refresh)))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("AUTHENTICATION_UNAVAILABLE"))
                .andReturn().getResponse();
        assertNull(refreshHeader(response.getHeaders("Set-Cookie")));
    }
    @Test void unexpectedFailureDoesNotExposeInternalDetails() throws Exception {
        doThrow(new IllegalStateException("PRIVATE SQL password")).when(workflows).login(anyString(),anyString());
        var response=mvc.perform(csrf(postAuth("login"),bootstrap()).contentType(MediaType.APPLICATION_JSON).content(LOGIN))
                .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andReturn().getResponse();
        assertFalse(response.getContentAsString().contains("PRIVATE"));
    }
    @Test void corsAllowsExactLocalCrossOriginPreflightOnly() throws Exception {
        mvc.perform(options(api()+"/api/v1/auth/login").header("Origin",origin())
                .header("Access-Control-Request-Method","POST")
                .header("Access-Control-Request-Headers","content-type,x-xsrf-token"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin",origin()))
                .andExpect(header().string("Access-Control-Allow-Credentials","true"));
        for(String origin:List.of("http://localhost:3001","http://localhost.evil:3000","null")) {
            mvc.perform(options(api()+"/api/v1/auth/login").header("Origin",origin)
                    .header("Access-Control-Request-Method","POST")).andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        }
        mvc.perform(options(api()+"/api/v1/auth/login").header("Origin",origin())
                .header("Access-Control-Request-Method","DELETE")).andExpect(status().isForbidden());
        verifyNoInteractions(workflows);
    }
    @Test void disallowedOriginCannotBootstrapOrMutate() throws Exception {
        mvc.perform(get(api()+"/api/v1/auth/csrf").header("Origin","https://evil.invalid"))
                .andExpect(status().isForbidden());
        mvc.perform(post(api()+"/api/v1/auth/logout").header("Origin","https://evil.invalid"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(workflows);
    }
    @Test void localHttpExceptionCannotServeNonLoopbackRequests() throws Exception {
        mvc.perform(get("http://remote.example/api/v1/auth/csrf")).andExpect(status().isForbidden());
        mvc.perform(getAuth("csrf").with(r->{r.setRemoteAddr("192.0.2.1");return r;}))
                .andExpect(status().isForbidden());
        verifyNoInteractions(workflows);
    }
    @Test void unapprovedAuthOperationsRemainDenied() throws Exception {
        mvc.perform(csrf(postAuth("register"),bootstrap()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(getAuth("login")).andExpect(status().isUnauthorized());
        verifyNoInteractions(workflows);
    }
    @Test void cookieLifetimeUsesRemainingExpiryRatherThanFreshSevenDays() throws Exception {
        when(clock.instant()).thenReturn(now.plusMillis(123750));
        var response=mvc.perform(csrf(postAuth("login"),bootstrap())
                .contentType(MediaType.APPLICATION_JSON).content(LOGIN))
                .andExpect(status().isOk()).andReturn().getResponse();
        assertEquals(604676,MockCookie.parse(refreshHeader(response.getHeaders("Set-Cookie"))).getMaxAge());
    }
    @Test void corsAndRefreshCookieDoNotAuthenticateBusinessRoutes() throws Exception {
        mvc.perform(get(api()+"/api/v1/users/me").header("Origin",origin())
                .cookie(new Cookie("refresh_token",refresh)))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Access-Control-Allow-Origin",origin()))
                .andExpect(jsonPath("$.code").value("AUTHENTICATION_REQUIRED"));
        mvc.perform(options(api()+"/api/v1/users/me").header("Origin",origin())
                .header("Access-Control-Request-Method","GET")
                .header("Access-Control-Request-Headers","authorization"))
                .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin",origin()));
        verifyNoInteractions(workflows,accounts,decoder);
    }

}
