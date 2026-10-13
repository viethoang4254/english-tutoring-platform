package com.englishlearning.features.auth.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import com.englishlearning.features.auth.service.*;
import com.englishlearning.features.user.repository.UserCredentialRepository;
import com.englishlearning.features.user.service.CurrentAccountService;
import jakarta.servlet.http.Cookie;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockCookie;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
        "spring.flyway.enabled=false"})
abstract class AuthenticationHttpTestSupport {
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.englishlearning.features.teacher.service.TeacherApplicationReviewService teacherReviews;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.englishlearning.features.teacher.service.TeacherBusinessEligibility teacherEligibility;
    @MockitoBean RegistrationService registration;
    @MockitoBean UserCredentialRepository credentials;
    @MockitoBean CurrentAccountService accounts;
    @MockitoBean RefreshSessionService sessions;
    @MockitoBean AccessTokenIssuer issuer;
    @MockitoBean JwtDecoder decoder;
    @MockitoBean AuthenticationWorkflowService workflows;
    @MockitoBean(name="refreshSessionClock") Clock clock;
    @Autowired WebApplicationContext context;
    @Autowired JsonMapper mapper;
    MockMvc mvc;
    final Instant now=Instant.parse("2026-10-12T00:00:00Z");
    final String refresh=new RefreshTokens().generate();
    static final String LOGIN="{\"email\":\" person@example.invalid \",\"password\":\" test password \"}";
    abstract String origin();
    abstract String api();
    abstract boolean secure();
    @BeforeEach void setupHttp() {
        mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        when(clock.instant()).thenReturn(now);
        var result=new AuthenticationResult(new AccessToken("test-access-jwt",now,now.plusSeconds(900)),
                new RefreshCredential(1L,1L,refresh,now.plusSeconds(604800)));
        when(workflows.login(anyString(),anyString())).thenReturn(result);
        when(workflows.refresh(any())).thenReturn(result);
    }
    MockHttpServletRequestBuilder getAuth(String suffix) {
        return get(api()+"/api/v1/auth/"+suffix).secure(secure()).header("Origin",origin());
    }
    MockHttpServletRequestBuilder postAuth(String suffix) {
        return post(api()+"/api/v1/auth/"+suffix).secure(secure()).header("Origin",origin());
    }
    record Csrf(Cookie cookie,String token) {}
    Csrf bootstrap() throws Exception {
        var response=mvc.perform(getAuth("csrf")).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control","no-store"))
                .andExpect(header().string("Access-Control-Allow-Origin",origin()))
                .andExpect(header().string("Access-Control-Allow-Credentials","true"))
                .andReturn().getResponse();
        Cookie cookie=MockCookie.parse(response.getHeaders("Set-Cookie").stream()
                .filter(s->s.startsWith("XSRF-TOKEN=")).findFirst().orElseThrow());
        assertTrue(cookie.isHttpOnly());
        assertEquals(secure(),cookie.getSecure());
        assertEquals("/api/v1/auth",cookie.getPath());
        assertNull(cookie.getDomain());
        var json=mapper.readTree(response.getContentAsString());
        assertEquals(1,json.size());
        return new Csrf(cookie,json.get("token").asString());
    }
    MockHttpServletRequestBuilder csrf(MockHttpServletRequestBuilder request,Csrf csrf) {
        return request.cookie(csrf.cookie()).header("X-XSRF-TOKEN",csrf.token());
    }
    String refreshHeader(List<String> cookies) {
        return cookies.stream().filter(s->s.startsWith("refresh_token=")).findFirst().orElse(null);
    }
}
