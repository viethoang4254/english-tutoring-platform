package com.englishlearning.features.auth.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.englishlearning.features.auth.service.VerificationFailure;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

@SpringBootTest(properties={
        "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
        "spring.flyway.enabled=false", "app.security.browser.local-http=true",
        "app.security.browser.allowed-origins=http://localhost:3000"})
class StudentRegistrationHttpTests extends AuthenticationHttpTestSupport {
    String origin(){return "http://localhost:3000";}
    String api(){return "http://localhost:8080";}
    boolean secure(){return false;}
    String input() { return mapper.writeValueAsString(Map.of("email"," Person@example.invalid ",
            "password"," test password ","fullName","Test Student")); }
    @Test void registrationHasExactTokenFreeProjectionAndNoRefreshCookie() throws Exception {
        var response=mvc.perform(csrf(postAuth("register/student"),bootstrap())
                .contentType(MediaType.APPLICATION_JSON).content(input()))
                .andExpect(status().isCreated()).andExpect(content().json("{\"verificationRequired\":true}"))
                .andExpect(header().string("Cache-Control","no-store")).andReturn().getResponse();
        assertEquals(1,mapper.readTree(response.getContentAsString()).size());
        assertNull(refreshHeader(response.getHeaders("Set-Cookie")));
        verify(registration).register("Person@example.invalid"," test password ","Test Student");
        verifyNoInteractions(workflows);
    }
    @ParameterizedTest @ValueSource(strings={"role","id","emailVerified","locked","extra"})
    void protectedAndUnknownPropertiesRejected(String property) throws Exception {
        var body=mapper.readTree(input());
        ((tools.jackson.databind.node.ObjectNode)body).put(property,"ADMIN");
        mvc.perform(csrf(postAuth("register/student"),bootstrap()).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(body))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verifyNoInteractions(registration);
    }
    @ParameterizedTest @ValueSource(strings={"short"," ","a@",""})
    void invalidPasswordRejected(String password) throws Exception {
        var body=Map.of("email","person@example.invalid","fullName","Test","password",password);
        mvc.perform(csrf(postAuth("register/student"),bootstrap()).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(body))).andExpect(status().isBadRequest());
        verifyNoInteractions(registration);
    }
    @Test void requiredFieldsAndQueryRejected() throws Exception {
        for (String body : new String[]{"{}", "{\"email\":12}",
                mapper.writeValueAsString(Map.of("email","bad","password","valid password","fullName","Test")),
                mapper.writeValueAsString(Map.of("email","p@example.invalid","password","valid password","fullName"," "))}) {
            mvc.perform(csrf(postAuth("register/student"),bootstrap()).contentType(MediaType.APPLICATION_JSON)
                    .content(body)).andExpect(status().isBadRequest());
        }
        mvc.perform(csrf(postAuth("register/student").queryParam("id","2"),bootstrap())
                .contentType(MediaType.APPLICATION_JSON).content(input())).andExpect(status().isBadRequest());
        verifyNoInteractions(registration);
    }
    @ParameterizedTest @ValueSource(strings={"register/student","email-verification","email-verification/resend"})
    void csrfIsRequiredBeforeAnyMutation(String path) throws Exception {
        mvc.perform(postAuth(path).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
        verifyNoInteractions(registration);
    }
    @Test void verificationAndResendHaveEmptyResponses() throws Exception {
        String token=new VerificationTokens().generate();
        mvc.perform(csrf(postAuth("email-verification"),bootstrap()).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("token",token))))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        mvc.perform(csrf(postAuth("email-verification/resend"),bootstrap()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"person@example.invalid\"}"))
                .andExpect(status().isAccepted()).andExpect(content().string(""));
        verify(registration).verify(token); verify(registration).resend("person@example.invalid");
    }
    @Test void safeTypedErrorsNeverExposeCredentialsAndDoNotSetRefreshCookie() throws Exception {
        int[] statuses={409,400,429,503,503,503,500};
        String[] codes={"REGISTRATION_CONFLICT","INVALID_VERIFICATION_CREDENTIAL","VERIFICATION_RATE_LIMITED",
                "VERIFICATION_DELIVERY_UNAVAILABLE","AUTHENTICATION_UNAVAILABLE","AUTHENTICATION_OUTCOME_UNKNOWN","INTERNAL_ERROR"};
        int i=0;
        for (var reason : VerificationFailure.Reason.values()) {
            doThrow(new VerificationFailure(reason)).when(registration).register(anyString(),anyString(),anyString());
            var response=mvc.perform(csrf(postAuth("register/student"),bootstrap()).contentType(MediaType.APPLICATION_JSON)
                    .content(input())).andExpect(status().is(statuses[i]))
                    .andExpect(jsonPath("$.code").value(codes[i++])).andReturn().getResponse();
            assertFalse(response.getContentAsString().contains("test password"));
            assertFalse(response.getContentAsString().contains("example.invalid"));
            assertNull(refreshHeader(response.getHeaders("Set-Cookie")));
        }
    }
    @Test void unapprovedOriginCannotRegister() throws Exception {
        mvc.perform(postAuth("register/student").header("Origin","https://evil.invalid")
                .contentType(MediaType.APPLICATION_JSON).content(input())).andExpect(status().isForbidden());
        verifyNoInteractions(registration);
    }
}
