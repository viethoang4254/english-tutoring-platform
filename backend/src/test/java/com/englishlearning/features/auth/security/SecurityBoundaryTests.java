package com.englishlearning.features.auth.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.englishlearning.features.user.service.CurrentAccountService;
import com.englishlearning.features.user.service.AccountSnapshot;
import com.englishlearning.features.user.entity.User.Role;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import com.englishlearning.EnglishLearningApplication;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.*;
import java.nio.file.*;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(classes = {EnglishLearningApplication.class, SecurityBoundaryTests.Fixtures.class}, properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
        "spring.flyway.enabled=false"})
class SecurityBoundaryTests {
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.englishlearning.features.auth.service.RegistrationService registration;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.englishlearning.features.user.repository.UserCredentialRepository credentialRepository;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.englishlearning.features.auth.security.AccessTokenIssuer accessTokenIssuer;
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    com.englishlearning.features.auth.service.RefreshSessionService refreshSessions;
    static final KeyPair KEY = key();
    static final Path PUBLIC_KEY = publicKey();
    static KeyPair key() {
        try { var generator = KeyPairGenerator.getInstance("RSA"); generator.initialize(2048); return generator.generateKeyPair(); }
        catch (Exception e) { throw new IllegalStateException(e); }
    }
    static Path publicKey() {
        try {
            Path path = Files.createTempFile("task006-public-", ".pem");
            Files.writeString(path, "-----BEGIN PUBLIC KEY-----\n" + Base64.getMimeEncoder(64, new byte[]{10})
                    .encodeToString(KEY.getPublic().getEncoded()) + "\n-----END PUBLIC KEY-----\n");
            path.toFile().deleteOnExit(); return path;
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.security.jwt.public-key", () -> PUBLIC_KEY.toUri().toString());
    }
    @Autowired WebApplicationContext context;
    @Autowired JwtDecoder decoder;
    @MockitoBean CurrentAccountService accounts;
    MockMvc mvc;
    @BeforeEach void setup() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        when(accounts.findById(1L)).thenReturn(Optional.of(new AccountSnapshot(1L, Role.STUDENT, false, false)));
        when(accounts.findById(2L)).thenReturn(Optional.of(new AccountSnapshot(2L, Role.ADMIN, false, true)));
    }

    String token(String variation) throws Exception {
        Instant now = Instant.now();
        var claims = new JWTClaimsSet.Builder().subject("1").issuer("english-tutoring-platform")
                .audience("english-tutoring-api").issueTime(Date.from(now)).expirationTime(Date.from(now.plusSeconds(900)));
        switch (variation) {
            case "sub" -> claims.subject(null);
            case "numericSub" -> claims.claim("sub", 1L);
            case "iss" -> claims.issuer(null);
            case "aud" -> claims.audience((String)null);
            case "iat" -> claims.issueTime(null);
            case "exp" -> claims.expirationTime(null);
            case "wrongIssuer" -> claims.issuer("other");
            case "wrongAudience" -> claims.audience("other");
            case "overflow" -> claims.subject("9223372036854775808");
            case "negative" -> claims.subject("-1");
            case "fraction" -> claims.subject("1.5");
            case "expired" -> claims.issueTime(Date.from(now.minusSeconds(1000))).expirationTime(Date.from(now.minusSeconds(120)));
            case "future" -> claims.issueTime(Date.from(now.plusSeconds(120)));
            case "nbf" -> claims.notBeforeTime(Date.from(now.plusSeconds(120)));
            case "inconsistent" -> claims.expirationTime(Date.from(now.minusSeconds(1)));
            case "forgedRole" -> claims.claim("role", "ADMIN").claim("scope", "ADMIN");
            case "admin" -> claims.subject("2");
            case "maxId" -> claims.subject("9223372036854775807");
            default -> { }
        }
        var jwt = new SignedJWT(new JWSHeader("wrongAlgorithm".equals(variation) ? JWSAlgorithm.RS512 : JWSAlgorithm.RS256), claims.build());
        jwt.sign(new RSASSASigner((RSAPrivateKey)("wrongKey".equals(variation) ? key() : KEY).getPrivate()));
        return jwt.serialize();
    }

    @ParameterizedTest
    @ValueSource(strings={"sub","iss","aud","iat","exp","wrongIssuer","wrongAudience","overflow","negative","fraction","expired","future","nbf","inconsistent","wrongKey","wrongAlgorithm","numericSub"})
    void rejectsInvalidTokensSafely(String variation) throws Exception {
        mvc.perform(get("/__security_test/student").header("Authorization", "Bearer " + token(variation)))
                .andExpect(status().isUnauthorized()).andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(content().json("{\"code\":\"AUTHENTICATION_REQUIRED\",\"message\":\"Authentication required.\"}", org.springframework.test.json.JsonCompareMode.STRICT));
        verifyNoInteractions(accounts);
    }
    @Test void resolvesOnlyValidatedSubjectWithoutRetainingToken() throws Exception {
        mvc.perform(get("/__security_test/student").header("Authorization", "Bearer " + token("valid")))
                .andExpect(status().isOk()).andExpect(content().string("1"));
        verify(accounts).findById(1L);
        verifyNoMoreInteractions(accounts);
    }

    @Test void missingAndLockedAccountsHaveIdenticalSafeErrors() throws Exception {
        for (var account : List.of(Optional.<AccountSnapshot>empty(),
                Optional.of(new AccountSnapshot(1L, Role.STUDENT, true, true)))) {
            when(accounts.findById(1L)).thenReturn(account);
            mvc.perform(get("/__security_test/student").header("Authorization", "Bearer " + token("valid")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().json("{\"code\":\"AUTHENTICATION_REQUIRED\",\"message\":\"Authentication required.\"}", org.springframework.test.json.JsonCompareMode.STRICT));
        }
    }

    @Test void currentRoleAndLockAreRecheckedOnEveryRequest() throws Exception {
        String bearer = "Bearer " + token("forgedRole");
        mvc.perform(get("/__security_test/admin").header("Authorization", bearer)).andExpect(status().isForbidden());
        when(accounts.findById(1L)).thenReturn(Optional.of(new AccountSnapshot(1L, Role.ADMIN, false, true)));
        mvc.perform(get("/__security_test/admin").header("Authorization", bearer)).andExpect(status().isOk());
        when(accounts.findById(1L)).thenReturn(Optional.of(new AccountSnapshot(1L, Role.ADMIN, true, true)));
        mvc.perform(get("/__security_test/admin").header("Authorization", bearer)).andExpect(status().isUnauthorized());
        verify(accounts, times(3)).findById(1L);
    }

    @Test void teacherRoleDoesNotGrantBusinessAccess() throws Exception {
        when(accounts.findById(1L)).thenReturn(Optional.of(new AccountSnapshot(1L, Role.TEACHER, false, true)));
        mvc.perform(get("/api/v1/teacher/courses").header("Authorization", "Bearer " + token("valid")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/__security_test/admin").header("Authorization", "Bearer " + token("valid")))
                .andExpect(status().isForbidden());
    }

    @Test void databaseOutagesAreSafe503WithoutCredentialChallenge() throws Exception {
        for (var failure : List.of(
                new org.springframework.dao.DataAccessResourceFailureException("PRIVATE SQL password"),
                new org.springframework.dao.QueryTimeoutException("PRIVATE SQL"),
                new org.springframework.transaction.CannotCreateTransactionException("PRIVATE connection"),
                new org.springframework.transaction.TransactionTimedOutException("PRIVATE transaction"))) {
            reset(accounts);
            when(accounts.findById(1L)).thenThrow(failure);
            mvc.perform(get("/__security_test/student").header("Authorization", "Bearer " + token("valid")))
                    .andExpect(status().isServiceUnavailable()).andExpect(header().doesNotExist("WWW-Authenticate"))
                    .andExpect(content().json("{\"code\":\"AUTHENTICATION_UNAVAILABLE\",\"message\":\"Authentication temporarily unavailable.\"}", org.springframework.test.json.JsonCompareMode.STRICT));
        }
    }

    @Test void unexpectedLookupFailureIsSafe500() throws Exception {
        when(accounts.findById(1L)).thenThrow(new IllegalStateException("PRIVATE password SQL"));
        mvc.perform(get("/__security_test/student").header("Authorization", "Bearer " + token("valid")))
                .andExpect(status().isInternalServerError()).andExpect(header().doesNotExist("WWW-Authenticate"))
                .andExpect(content().json("{\"code\":\"INTERNAL_ERROR\",\"message\":\"An unexpected error occurred.\"}", org.springframework.test.json.JsonCompareMode.STRICT));
    }

    @Test void rejectsUnsignedAndFormOrBasicCredentials() throws Exception {
        var unsigned = new PlainJWT(new JWTClaimsSet.Builder().subject("1").build()).serialize();
        mvc.perform(get("/__security_test/student").header("Authorization", "Bearer " + unsigned)).andExpect(status().isUnauthorized());
        mvc.perform(post("/__security_test/student").contentType("application/x-www-form-urlencoded")
                .param("access_token",token("valid"))).andExpect(status().isUnauthorized());
        mvc.perform(get("/__security_test/student").header("Authorization", "Basic dGVzdDp0ZXN0"))
                .andExpect(status().isUnauthorized());
    }
    @Test void rejectsMalformedToken() throws Exception {
        mvc.perform(get("/__security_test/student").header("Authorization","Bearer PRIVATE_TOKEN"))
                .andExpect(status().isUnauthorized()).andExpect(content().json("{\"code\":\"AUTHENTICATION_REQUIRED\",\"message\":\"Authentication required.\"}", org.springframework.test.json.JsonCompareMode.STRICT));
    }
    @Test void verifiesRealSignatureAndTestOnlyRoleBoundary() throws Exception {
        var response=mvc.perform(get("/__security_test/student").header("Authorization","Bearer " + token("valid")))
                .andExpect(status().isOk()).andReturn().getResponse();
        assertNull(response.getHeader("Set-Cookie"));
        mvc.perform(get("/__security_test/admin").header("Authorization","Bearer " + token("admin"))).andExpect(status().isOk());
        mvc.perform(get("/__security_test/admin").header("Authorization","Bearer " + token("forgedRole")))
                .andExpect(status().isForbidden()).andExpect(content().json("{\"code\":\"ACCESS_DENIED\",\"message\":\"Access denied.\"}", org.springframework.test.json.JsonCompareMode.STRICT));
        assertEquals("9223372036854775807",decoder.decode(token("maxId")).getSubject());
    }
    @Test void productionChainDeniesAllEvenWithValidToken() throws Exception {
        mvc.perform(get("/api/v1/not-implemented")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/not-implemented").header("Authorization","Bearer " + token("admin"))).andExpect(status().isForbidden());
        mvc.perform(get("/login")).andExpect(status().isUnauthorized()).andExpect(header().doesNotExist("Location"));
    }
    @Test void ignoresQueryAndCookieCredentials() throws Exception {
        String value=token("valid");
        mvc.perform(get("/__security_test/student").param("access_token",value)).andExpect(status().isUnauthorized());
        mvc.perform(get("/__security_test/student").cookie(new jakarta.servlet.http.Cookie("access_token",value))).andExpect(status().isUnauthorized());
    }
    @Test void rejectsInvalidConfiguration() {
        var config=new SecurityConfiguration();
        assertThrows(Exception.class,()->config.jwtDecoder(new ByteArrayResource("invalid".getBytes()),"issuer","audience",Duration.ofSeconds(60), Clock.systemUTC()));
        assertThrows(Exception.class,()->config.jwtDecoder(new ByteArrayResource(new byte[0]),"","audience",Duration.ofSeconds(60), Clock.systemUTC()));
        assertThrows(Exception.class,()->config.jwtDecoder(new ByteArrayResource(new byte[0]),"issuer","audience",Duration.ofSeconds(-1), Clock.systemUTC()));
    }
    @Test void clockToleranceHasExplicitBoundaries() {
        Instant now=Instant.parse("2026-10-11T00:00:00Z");
        var validator=new AccessTokenValidator(Clock.fixed(now,ZoneOffset.UTC),Duration.ofSeconds(60));
        var base=org.springframework.security.oauth2.jwt.Jwt.withTokenValue("test").header("alg","RS256").subject("1");
        assertFalse(validator.validate(base.issuedAt(now.plusSeconds(60)).expiresAt(now.plusSeconds(900)).build()).hasErrors());
        assertTrue(validator.validate(base.issuedAt(now.plusSeconds(61)).expiresAt(now.plusSeconds(900)).build()).hasErrors());
    }

    @Test void currentAccountAllowsAllRolesWithoutOnboardingOrVerifiedEmail() throws Exception {
        for (Role role : Role.values()) {
            when(accounts.findById(1L)).thenReturn(Optional.of(new AccountSnapshot(1L, role, false, false)));
            mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token("forgedRole")))
                    .andExpect(status().isOk())
                    .andExpect(content().json("{\"id\":\"1\",\"role\":\"" + role.name()
                            + "\",\"emailVerified\":false}", org.springframework.test.json.JsonCompareMode.STRICT));
        }
        verify(accounts, times(3)).findById(1L);
    }

    @Test void currentAccountPreservesLargeIdAndFreshDatabaseState() throws Exception {
        when(accounts.findById(Long.MAX_VALUE)).thenReturn(
                Optional.of(new AccountSnapshot(Long.MAX_VALUE, Role.STUDENT, false, false)),
                Optional.of(new AccountSnapshot(Long.MAX_VALUE, Role.ADMIN, false, true)));
        String bearer = "Bearer " + token("maxId");
        mvc.perform(get("/api/v1/users/me").header("Authorization", bearer)).andExpect(status().isOk())
                .andExpect(content().json("{\"id\":\"9223372036854775807\",\"role\":\"STUDENT\",\"emailVerified\":false}", org.springframework.test.json.JsonCompareMode.STRICT));
        mvc.perform(get("/api/v1/users/me").header("Authorization", bearer)).andExpect(status().isOk())
                .andExpect(content().json("{\"id\":\"9223372036854775807\",\"role\":\"ADMIN\",\"emailVerified\":true}", org.springframework.test.json.JsonCompareMode.STRICT));
    }

    @Test void currentAccountRejectsQueryAndAnyBody() throws Exception {
        String bearer = "Bearer " + token("valid");
        mvc.perform(get("/api/v1/users/me?userId=2").header("Authorization", bearer))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("{\"code\":\"INVALID_REQUEST\",\"message\":\"Request could not be processed.\"}", org.springframework.test.json.JsonCompareMode.STRICT));
        for (String body : List.of("{}", " ", "{\"id\":\"2\"}", "not-json")) {
            mvc.perform(get("/api/v1/users/me").header("Authorization", bearer).content(body))
                    .andExpect(status().isBadRequest());
        }
        verify(accounts, never()).findById(2L);
        mvc.perform(get("/api/v1/users/me").header("Authorization", bearer).content(""))
                .andExpect(status().isOk());
    }

    @Test void currentAccountAuthenticationPrecedesForbiddenInput() throws Exception {
        mvc.perform(get("/api/v1/users/me?userId=2").content("{}")).andExpect(status().isUnauthorized());
        for (String variation : List.of("expired", "wrongKey")) {
            mvc.perform(get("/api/v1/users/me?userId=2").content("{}")
                    .header("Authorization", "Bearer " + token(variation))).andExpect(status().isUnauthorized());
        }
        mvc.perform(get("/api/v1/users/me").header("Authorization","Bearer malformed"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(accounts);
    }

    @Test void currentAccountMissingAndLockedPrecedeInputValidation() throws Exception {
        for (var account : List.of(Optional.<AccountSnapshot>empty(),
                Optional.of(new AccountSnapshot(1L, Role.STUDENT, true, true)))) {
            when(accounts.findById(1L)).thenReturn(account);
            mvc.perform(get("/api/v1/users/me?userId=2").content("{}")
                    .header("Authorization", "Bearer " + token("valid"))).andExpect(status().isUnauthorized())
                    .andExpect(content().json("{\"code\":\"AUTHENTICATION_REQUIRED\",\"message\":\"Authentication required.\"}", org.springframework.test.json.JsonCompareMode.STRICT));
        }
    }

    @Test void currentAccountLookupFailuresAreSafe() throws Exception {
        when(accounts.findById(1L)).thenThrow(new org.springframework.dao.DataAccessResourceFailureException("PRIVATE SQL"));
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token("valid")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().json("{\"code\":\"AUTHENTICATION_UNAVAILABLE\",\"message\":\"Authentication temporarily unavailable.\"}", org.springframework.test.json.JsonCompareMode.STRICT));
        reset(accounts);
        when(accounts.findById(1L)).thenThrow(new IllegalStateException("PRIVATE password"));
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token("valid")))
                .andExpect(status().isInternalServerError())
                .andExpect(content().json("{\"code\":\"INTERNAL_ERROR\",\"message\":\"An unexpected error occurred.\"}", org.springframework.test.json.JsonCompareMode.STRICT));
    }

    @Test void onlyExactGetCurrentAccountRouteIsPermitted() throws Exception {
        String bearer = "Bearer " + token("admin");
        mvc.perform(post("/api/v1/users/me").header("Authorization", bearer)).andExpect(status().isForbidden());
        mvc.perform(head("/api/v1/users/me").header("Authorization", bearer)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/users/1").header("Authorization", bearer)).andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/users/me/extra").header("Authorization", bearer)).andExpect(status().isForbidden());
    }

    @TestConfiguration(proxyBeanMethods=false)
    static class Fixtures {
        @Bean FixtureController fixtureController(){return new FixtureController();}
        @Bean @Order(1) SecurityFilterChain fixtureChain(HttpSecurity http,JwtDecoder decoder,JsonMapper mapper, CurrentAccountAuthenticationConverter accounts) throws Exception {
            // Test-only routes use the production converter with mocked User access.
            return http.securityMatcher("/__security_test/**").csrf(c->c.disable())
                    .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .requestCache(c->c.disable())
                    .authorizeHttpRequests(a->a.requestMatchers("/__security_test/admin").hasRole("ADMIN").anyRequest().hasRole("STUDENT"))
                    .exceptionHandling(e->e.authenticationEntryPoint(ApiSecurityErrors.unauthorized(mapper)).accessDeniedHandler(ApiSecurityErrors.forbidden(mapper)))
                    .oauth2ResourceServer(r->r.authenticationEntryPoint(ApiSecurityErrors.unauthorized(mapper)).accessDeniedHandler(ApiSecurityErrors.forbidden(mapper))
                            .jwt(j->j.decoder(decoder).jwtAuthenticationConverter(accounts))).build();
        }
    }
    @RestController static class FixtureController {
        @GetMapping("/__security_test/student") String student(org.springframework.security.core.Authentication authentication){
            assertInstanceOf(AccountPrincipal.class, authentication.getPrincipal());
            assertNull(authentication.getCredentials());
            assertEquals(List.of(new SimpleGrantedAuthority("ROLE_STUDENT")), new ArrayList<>(authentication.getAuthorities()));
            return authentication.getName();
        }
        @GetMapping("/__security_test/admin") String admin(){return "ok";}
    }
}
