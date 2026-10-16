package com.englishlearning.features.auth.security;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.englishlearning.features.auth.service.VerificationFailure;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

@SpringBootTest(properties={"spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
        "spring.flyway.enabled=false","app.security.browser.local-http=true","app.security.browser.allowed-origins=http://localhost:3000"})
class PasswordRecoveryHttpTests extends AuthenticationHttpTestSupport {
    String origin(){return "http://localhost:3000";} String api(){return "http://localhost:8080";} boolean secure(){return false;}
    @Test void neutralRequestAndResetHaveNoTokensAndRequireCsrf() throws Exception {
        mvc.perform(postAuth("password/forgot").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"a@example.invalid\"}"))
                .andExpect(status().isForbidden());verifyNoInteractions(recovery);
        mvc.perform(csrf(postAuth("password/forgot"),bootstrap()).contentType(MediaType.APPLICATION_JSON).content("{\"email\":\" a@example.invalid \"}"))
                .andExpect(status().isAccepted()).andExpect(content().string("")).andExpect(header().string("Cache-Control","no-store"));
        verify(recovery).forgot("a@example.invalid");
        mvc.perform(csrf(postAuth("password/reset"),bootstrap()).contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"a@example.invalid\",\"code\":\"000123\",\"password\":\" password \"}"))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(recovery).reset("a@example.invalid","000123"," password ");verifyNoInteractions(workflows);
    }
    @Test void rejectsInvalidProtectedInputAndQueries() throws Exception {
        for(String body:new String[]{"{}","{\"email\":12}","{\"email\":\"bad\"}","{\"email\":\"a@example.invalid\",\"role\":\"ADMIN\"}"})
            mvc.perform(csrf(postAuth("password/forgot"),bootstrap()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        mvc.perform(csrf(postAuth("password/reset"),bootstrap()).contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"a@example.invalid\",\"code\":\"000123\",\"password\":\"short\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(csrf(postAuth("password/forgot").queryParam("id","1"),bootstrap()).contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"a@example.invalid\"}"))
                .andExpect(status().isBadRequest());verifyNoInteractions(recovery);
    }
    @Test void invalidCodeHasSafeError() throws Exception {
        doThrow(new VerificationFailure(VerificationFailure.Reason.INVALID)).when(recovery).reset(any(),any(),any());
        mvc.perform(csrf(postAuth("password/reset"),bootstrap()).contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"a@example.invalid\",\"code\":\"000123\",\"password\":\"password\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_RESET_CREDENTIAL"));
    }
}
