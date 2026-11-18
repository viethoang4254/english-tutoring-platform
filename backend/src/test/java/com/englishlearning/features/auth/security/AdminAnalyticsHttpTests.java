package com.englishlearning.features.auth.security;

import com.englishlearning.features.user.entity.User.Role;
import com.englishlearning.features.user.service.AccountSnapshot;
import com.englishlearning.features.user.dto.AdminAnalyticsDto;
import com.englishlearning.common.api.ApiMoney;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@org.springframework.boot.test.context.SpringBootTest(properties={"spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration","spring.flyway.enabled=false","app.security.browser.local-http=true","app.security.browser.allowed-origins=http://localhost:3000"})
class AdminAnalyticsHttpTests extends AuthenticationHttpTestSupport {
    String origin(){return "http://localhost:3000";} String api(){return "http://localhost:8080";} boolean secure(){return false;}
    static final String PATH="/api/v1/admin/analytics/overview";
    void actor(Role role,boolean locked,boolean verified){
        when(decoder.decode("access")).thenReturn(Jwt.withTokenValue("access").header("alg","RS256").subject("1").build());
        when(accounts.findById(1L)).thenReturn(Optional.of(new AccountSnapshot(1L,role,locked,verified,true)));
    }
    @Test void onlyVerifiedUnlockedAdminCanRead() throws Exception {
        mvc.perform(get(PATH)).andExpect(status().isUnauthorized());
        for(Role role:List.of(Role.STUDENT,Role.TEACHER)){actor(role,false,true);mvc.perform(get(PATH).header("Authorization","Bearer access")).andExpect(status().isForbidden());}
        actor(Role.ADMIN,true,true);mvc.perform(get(PATH).header("Authorization","Bearer access")).andExpect(status().isUnauthorized());
        actor(Role.ADMIN,false,false);mvc.perform(get(PATH).header("Authorization","Bearer access")).andExpect(status().isForbidden());
        verifyNoInteractions(adminUsers);
    }
    @Test void rangesAreBoundedAndNoMutationIsAuthorized() throws Exception {
        actor(Role.ADMIN,false,true);
        for(String invalid:List.of("?range=0","?range=365","?range=","?range=ALL","?range=7&range=30","?teacherId=1"))
            mvc.perform(get(PATH+invalid).header("Authorization","Bearer access")).andExpect(status().isBadRequest());
        mvc.perform(get(PATH).content("{}").header("Authorization","Bearer access")).andExpect(status().isBadRequest());
        for(var request:List.of(post(PATH),patch(PATH),delete(PATH)))
            mvc.perform(request.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf().asHeader()).header("Authorization","Bearer access")).andExpect(status().isForbidden());
        verifyNoInteractions(adminUsers);
    }
    @Test void snapshotUsesVietnamBoundariesDefaultsAndSafeDto() throws Exception {
        actor(Role.ADMIN,false,true);
        var zero=new ApiMoney("0.00","VND");
        var totals=new AdminAnalyticsDto.Totals(0,0,0,0,0,0,0,0,0,0,0,0,0,0,zero);
        when(adminUsers.analytics(anyString(),nullable(Instant.class),eq(now))).thenAnswer(invocation ->
            new AdminAnalyticsDto(invocation.getArgument(0),"Asia/Ho_Chi_Minh",invocation.getArgument(1),now,totals,
                new AdminAnalyticsDto.Activity(0,0,0,0,zero),List.of(),List.of(),false,List.of(),false));
        mvc.perform(get(PATH+"?range=7").header("Authorization","Bearer access")).andExpect(status().isOk())
            .andExpect(header().string("Cache-Control","no-store")).andExpect(jsonPath("$.totals.confirmedTuition.amount").value("0.00"))
            .andExpect(jsonPath("$.days.length()").value(0)).andExpect(jsonPath("$.totals.payments").value(0))
            .andExpect(jsonPath("$.bankAccount").doesNotExist()).andExpect(jsonPath("$.meetUrl").doesNotExist()).andExpect(jsonPath("$.accessToken").doesNotExist());
        verify(adminUsers).analytics("7",Instant.parse("2026-10-05T17:00:00Z"),now);
        mvc.perform(get(PATH).header("Authorization","Bearer access")).andExpect(status().isOk()).andExpect(jsonPath("$.range").value("30"));
        verify(adminUsers).analytics(eq("30"),any(Instant.class),eq(now));
        mvc.perform(get(PATH+"?range=90").header("Authorization","Bearer access")).andExpect(status().isOk());
        mvc.perform(get(PATH+"?range=all").header("Authorization","Bearer access")).andExpect(status().isOk());
        verify(adminUsers).analytics("all",null,now);
    }
    @Test void infrastructureErrorsAreSafe() throws Exception {
        actor(Role.ADMIN,false,true);when(adminUsers.analytics(anyString(),any(Instant.class),any(Instant.class)))
            .thenThrow(new org.springframework.dao.DataAccessResourceFailureException("PRIVATE database credential"));
        mvc.perform(get(PATH).header("Authorization","Bearer access")).andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.code").value("ANALYTICS_UNAVAILABLE"))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("PRIVATE"))));
    }
}
