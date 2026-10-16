package com.englishlearning.features.auth.security;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.englishlearning.features.auth.service.AuthenticationFailure;
import com.englishlearning.features.user.service.AccountSnapshot;
import com.englishlearning.features.user.entity.User.Role;
import java.util.Optional;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
@SpringBootTest(properties={"spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration","spring.flyway.enabled=false","app.security.browser.local-http=true","app.security.browser.allowed-origins=http://localhost:3000"})
class PasswordChangeHttpTests extends AuthenticationHttpTestSupport {
    String origin(){return "http://localhost:3000";}String api(){return "http://localhost:8080";}boolean secure(){return false;}
    String body="{\"currentPassword\":\"old password\",\"newPassword\":\"new password\"}";
    void account(Role role){when(decoder.decode("access")).thenReturn(Jwt.withTokenValue("access").header("alg","RS256").subject("1").build());when(accounts.findById(1L)).thenReturn(Optional.of(new AccountSnapshot(1L,role,false,true,false)));}
    @Test void rolesRequireBearerAndCsrfAndReceiveNoReplacementCookie() throws Exception {
        for(Role role:Role.values()) {account(role);mvc.perform(csrf(postAuth("password/change"),bootstrap()).header("Authorization","Bearer access").cookie(new Cookie("refresh_token",refresh)).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isNoContent()).andExpect(result -> org.junit.jupiter.api.Assertions.assertNull(refreshHeader(result.getResponse().getHeaders("Set-Cookie")))).andExpect(header().string("Cache-Control","no-store"));}
        verify(passwordChanges,times(3)).change(1L,refresh,"old password","new password");
        mvc.perform(csrf(postAuth("password/change"),bootstrap()).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isUnauthorized());
        mvc.perform(postAuth("password/change").header("Authorization","Bearer access").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
    }
    @Test void rejectsUnknownFieldsAndPreservesPublicRecoveryWithStaleBearer() throws Exception {
        account(Role.STUDENT);
        mvc.perform(csrf(postAuth("password/change"),bootstrap()).header("Authorization","Bearer access").contentType(MediaType.APPLICATION_JSON).content(body.substring(0,body.length()-1)+",\"userId\":2}")).andExpect(status().isBadRequest());
        mvc.perform(csrf(postAuth("password/forgot"),bootstrap()).header("Authorization","Bearer stale").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"a@example.invalid\"}")).andExpect(status().isAccepted());verify(decoder,never()).decode("stale");
    }
    @Test void safeFailureMappings() throws Exception {
        account(Role.STUDENT);
        var reasons=new AuthenticationFailure.Reason[]{AuthenticationFailure.Reason.INVALID_CREDENTIAL,AuthenticationFailure.Reason.ACCESS_DENIED,AuthenticationFailure.Reason.INVALID_REQUEST,AuthenticationFailure.Reason.UNAVAILABLE,AuthenticationFailure.Reason.COMMIT_UNCERTAIN,AuthenticationFailure.Reason.INTERNAL_FAILURE};
        int[] statuses={401,403,400,503,503,500};
        for(int i=0;i<reasons.length;i++){doThrow(new AuthenticationFailure(reasons[i])).when(passwordChanges).change(any(),any(),any(),any());mvc.perform(csrf(postAuth("password/change"),bootstrap()).header("Authorization","Bearer access").cookie(new Cookie("refresh_token",refresh)).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().is(statuses[i]));}
    }
}
