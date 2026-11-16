package com.englishlearning.features.auth.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.englishlearning.features.googlecalendar.*;
import com.englishlearning.features.user.entity.User.Role;
import com.englishlearning.features.user.service.AccountSnapshot;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockCookie;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties={"spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
    "spring.flyway.enabled=false","APP_GOOGLE_CALENDAR_SYNC_ENABLED=true","app.security.browser.local-http=false","app.security.browser.allowed-origins=https://web.example.test"})
class GoogleCalendarHttpTests extends AuthenticationHttpTestSupport {
    @MockitoBean GoogleService google;
    @MockitoBean CalendarSyncStore sync;
    String origin(){return "https://web.example.test";}String api(){return "https://api.example.test";}boolean secure(){return true;}
    final String path=GoogleSettings.PATH;
    void account(Role role,boolean locked){
        when(decoder.decode("access")).thenReturn(Jwt.withTokenValue("access").header("alg","RS256").subject("1").build());
        when(accounts.findById(1L)).thenReturn(Optional.of(new AccountSnapshot(1L,role,locked,true,true,true)));
        when(google.status(1)).thenReturn(new GoogleService.Status("DISCONNECTED",null,false,null,false));
    }
    org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder read(){return get(api()+path).secure(true).header("Origin",origin()).header("Authorization","Bearer access");}
    Csrf googleCsrf()throws Exception{
        var response=mvc.perform(read()).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store")).andReturn().getResponse();
        var cookie=MockCookie.parse(response.getHeaders("Set-Cookie").stream().filter(s->s.startsWith("GOOGLE-XSRF-TOKEN=")).findFirst().orElseThrow());
        assertTrue(cookie.isHttpOnly());assertTrue(cookie.getSecure());assertEquals(path,cookie.getPath());
        return new Csrf(cookie,mapper.readTree(response.getContentAsString()).path("csrfToken").asText());
    }
    @Test void syncIsCsrfProtectedOwnerScopedAndRejectsClientSelectedIdentity()throws Exception {
        account(Role.STUDENT,false);
        var token=googleCsrf();
        when(google.status(1)).thenReturn(new GoogleService.Status("CONNECTED","fixture@example.invalid",false,null,true));
        when(sync.status(1)).thenReturn(new CalendarSyncStore.Status("PENDING",1,0,null));
        mvc.perform(read()).andExpect(status().isOk()).andExpect(jsonPath("$.sync.pending").value(1))
            .andExpect(jsonPath("$.connection.refreshToken").doesNotExist());
        mvc.perform(post(api()+path+"/sync").secure(true).header("Authorization","Bearer access")).andExpect(status().isForbidden());
        mvc.perform(csrf(post(api()+path+"/sync").secure(true).header("Authorization","Bearer access").content("{\"userId\":2}"),token))
            .andExpect(status().isConflict());
        verify(sync,never()).retry(anyLong());
        mvc.perform(csrf(post(api()+path+"/sync").secure(true).header("Authorization","Bearer access"),token))
            .andExpect(status().isAccepted()).andExpect(header().string("Cache-Control","no-store"));
        verify(sync).retry(1);verify(sync,never()).retry(2);
        account(Role.ADMIN,false);
        mvc.perform(csrf(post(api()+path+"/sync").secure(true).header("Authorization","Bearer access"),token))
            .andExpect(status().isForbidden());
        account(Role.STUDENT,true);
        mvc.perform(csrf(post(api()+path+"/sync").secure(true).header("Authorization","Bearer access"),token))
            .andExpect(status().isUnauthorized());
        account(Role.TEACHER,false);
        doThrow(new GoogleFailure("DENIED")).when(google).status(1);
        mvc.perform(csrf(post(api()+path+"/sync").secure(true).header("Authorization","Bearer access"),token))
            .andExpect(status().isForbidden());
        verify(sync,times(1)).retry(anyLong());
    }
    @Test void csrfAndBearerRequiredForBothMutations()throws Exception{
        account(Role.STUDENT,false);var token=googleCsrf();when(google.start(1)).thenReturn(new GoogleService.Started("https://accounts.google.com/o/oauth2/v2/auth","synthetic-binding"));
        mvc.perform(post(api()+path+"/connect").secure(true).header("Authorization","Bearer access")).andExpect(status().isForbidden());
        mvc.perform(csrf(post(api()+path+"/connect").secure(true).header("Origin",origin()).header("Authorization","Bearer access"),token))
            .andExpect(status().isOk()).andExpect(jsonPath("$.browserBinding").doesNotExist()).andExpect(jsonPath("$.refreshToken").doesNotExist())
            .andExpect(header().string("Set-Cookie",org.hamcrest.Matchers.allOf(org.hamcrest.Matchers.containsString("HttpOnly"),org.hamcrest.Matchers.containsString("Secure"),org.hamcrest.Matchers.containsString("SameSite=Lax"))));
        mvc.perform(delete(api()+path).secure(true).header("Authorization","Bearer access")).andExpect(status().isForbidden());
        when(google.disconnect(1)).thenReturn(false);
        mvc.perform(csrf(delete(api()+path).secure(true).header("Authorization","Bearer access"),token)).andExpect(status().isOk()).andExpect(jsonPath("$.disconnected").value(true)).andExpect(jsonPath("$.remoteRevoked").value(false));
    }
    @Test void rolesLockedAndAnonymousAreRejected()throws Exception{
        mvc.perform(get(api()+path).secure(true)).andExpect(status().isUnauthorized());
        account(Role.ADMIN,false);mvc.perform(read()).andExpect(status().isForbidden());
        account(Role.STUDENT,true);mvc.perform(read()).andExpect(status().isUnauthorized());verifyNoInteractions(google);
    }
    @Test void callbackUsesBoundIntentWithoutApplicationJwtAndNeverEchoesInputs()throws Exception{
        mvc.perform(get(api()+path+"/callback").secure(true).param("state","synthetic-state").param("code","synthetic-code")
            .cookie(new jakarta.servlet.http.Cookie("google_oauth_binding","synthetic-binding")))
            .andExpect(status().isSeeOther()).andExpect(header().string("Location","https://web.example.test/account?googleCalendar=CONNECTED"))
            .andExpect(header().string("Cache-Control","no-store")).andExpect(header().string("Referrer-Policy","no-referrer"));
        verify(google).complete("synthetic-state","synthetic-binding","synthetic-code",null);
    }
    @Test void callbackAcceptsCanonicalGoogleIssuerWithConsentParameters()throws Exception{
        mvc.perform(get(api()+path+"/callback").secure(true).param("iss","https://accounts.google.com")
            .param("authuser","0").param("prompt","consent").param("scope","openid email "+GoogleSettings.SCOPE)
            .param("state","synthetic-state").param("code","synthetic-code")
            .cookie(new jakarta.servlet.http.Cookie("google_oauth_binding","synthetic-binding")))
            .andExpect(status().isSeeOther()).andExpect(header().string("Location","https://web.example.test/account?googleCalendar=CONNECTED"));
        verify(google).complete("synthetic-state","synthetic-binding","synthetic-code",null);
    }
    @Test void callbackRejectsInvalidOrDuplicateIssuerBeforeExchange()throws Exception{
        for(String[] issuers:new String[][]{{""},{"https://other.example"},{"https://accounts.google.com","https://accounts.google.com"}}){
            mvc.perform(get(api()+path+"/callback").secure(true).param("iss",issuers))
                .andExpect(status().isSeeOther()).andExpect(header().string("Location","https://web.example.test/account?googleCalendar=INVALID_RESPONSE"));
        }
        verifyNoInteractions(google);
    }
    @Test void callbackRejectsUnapprovedReturnPathAndDuplicateParameters()throws Exception{
        mvc.perform(get(api()+path+"/callback").secure(true).param("returnTo","https://attacker.example"))
            .andExpect(status().isSeeOther()).andExpect(header().string("Location","https://web.example.test/account?googleCalendar=INVALID_RESPONSE"));
        mvc.perform(get(api()+path+"/callback").secure(true).param("state","one","two"))
            .andExpect(status().isSeeOther()).andExpect(header().string("Location","https://web.example.test/account?googleCalendar=INVALID_RESPONSE"));verifyNoInteractions(google);
    }
    @Test void deleteCorsIsRestrictedToConfiguredOrigin()throws Exception{
        mvc.perform(options(api()+path).secure(true).header("Origin",origin()).header("Access-Control-Request-Method","DELETE")
            .header("Access-Control-Request-Headers","authorization,x-xsrf-token"))
            .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin",origin()));
        mvc.perform(options(api()+path).secure(true).header("Origin","https://attacker.example").header("Access-Control-Request-Method","DELETE")).andExpect(status().isForbidden());
    }
}
