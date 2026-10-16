package com.englishlearning.features.auth.security;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import com.nimbusds.jwt.SignedJWT;
import java.text.ParseException;
import java.security.interfaces.RSAPublicKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;
import tools.jackson.databind.json.JsonMapper;

@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SecurityConfiguration {
    private static org.springframework.security.authorization.AuthorizationDecision verifiedAdmin(
            java.util.function.Supplier<? extends org.springframework.security.core.Authentication> authentication,
            org.springframework.security.web.access.intercept.RequestAuthorizationContext context) {
        var auth = authentication.get();
        return new org.springframework.security.authorization.AuthorizationDecision(auth.isAuthenticated()
                && auth.getPrincipal() instanceof AccountPrincipal account
                && account.role() == com.englishlearning.features.user.entity.User.Role.ADMIN && account.emailVerified());
    }
    private static org.springframework.security.authorization.AuthorizationDecision verifiedTeacher(
            java.util.function.Supplier<? extends org.springframework.security.core.Authentication> authentication,
            org.springframework.security.web.access.intercept.RequestAuthorizationContext context) {
        var auth = authentication.get();
        return new org.springframework.security.authorization.AuthorizationDecision(auth.isAuthenticated()
                && auth.getPrincipal() instanceof AccountPrincipal account
                && account.role() == com.englishlearning.features.user.entity.User.Role.TEACHER && account.emailVerified());
    }
    @Bean
    JwtDecoder jwtDecoder(@Value("${app.security.jwt.public-key}") Resource publicKey,
            @Value("${app.security.jwt.issuer:english-tutoring-platform}") String issuer,
            @Value("${app.security.jwt.audience:english-tutoring-api}") String audience,
            @Value("${app.security.jwt.clock-tolerance:60s}") Duration tolerance,
            @org.springframework.beans.factory.annotation.Qualifier("jwtClock") Clock clock) throws Exception {
        if (issuer.isBlank() || audience.isBlank() || tolerance.isNegative()) {
            throw new IllegalArgumentException("Invalid JWT validation configuration");
        }
        RSAPublicKey key;
        try (var input = publicKey.getInputStream()) { key = RsaKeyConverters.x509().convert(input); }
        if (key == null || key.getModulus().bitLength() < 2048) {
            throw new IllegalArgumentException("RSA public key must be at least 2048 bits");
        }
        var decoder = NimbusJwtDecoder.withPublicKey(key).signatureAlgorithm(SignatureAlgorithm.RS256).build();
        var timestamps = new JwtTimestampValidator(tolerance);
        timestamps.setClock(clock);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                timestamps, new JwtIssuerValidator(issuer),
                new JwtClaimValidator<List<String>>("aud", values -> values != null && values.contains(audience)),
                new AccessTokenValidator(clock, tolerance)));
        return token -> {
            // Nimbus normalizes subject values before claim conversion; inspect its parsed payload first.
            try {
                var payload = SignedJWT.parse(token).getPayload().toJSONObject();
                if (payload == null || !(payload.get("sub") instanceof String)) {
                    throw new BadJwtException("Invalid access token.");
                }
            } catch (ParseException | IllegalArgumentException exception) {
                throw new BadJwtException("Invalid access token.");
            }
            return decoder.decode(token);
        };
    }

    @Bean
    @org.springframework.core.annotation.Order(1)
    SecurityFilterChain authenticationHttp(HttpSecurity http, BrowserAuthSettings browser, JsonMapper mapper, JwtDecoder decoder, CurrentAccountAuthenticationConverter accounts)
            throws Exception {
        var repository = new org.springframework.security.web.csrf.CookieCsrfTokenRepository();
        repository.setCookiePath(BrowserAuthSettings.PATH);
        repository.setCookieCustomizer(cookie -> cookie.httpOnly(true).secure(browser.secure())
                .sameSite("Lax").path(BrowserAuthSettings.PATH));
        var xor = new org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler();
        var handler = new org.springframework.security.web.csrf.CsrfTokenRequestHandler() {
            @Override public void handle(jakarta.servlet.http.HttpServletRequest request,
                    jakarta.servlet.http.HttpServletResponse response,
                    java.util.function.Supplier<org.springframework.security.web.csrf.CsrfToken> token) {
                xor.handle(request, response, token);
            }
            @Override public String resolveCsrfTokenValue(jakarta.servlet.http.HttpServletRequest request,
                    org.springframework.security.web.csrf.CsrfToken token) {
                // Only the bootstrap token in the explicit header is accepted, never query/form input.
                return request.getHeader(token.getHeaderName()) == null ? null : xor.resolveCsrfTokenValue(request, token);
            }
        };
        http.securityMatcher("/api/v1/auth/**")
                .addFilterAt(BrowserSecurityFilters.cors(browser, mapper), org.springframework.web.filter.CorsFilter.class)
                .addFilterAfter(BrowserSecurityFilters.transport(browser, mapper), org.springframework.web.filter.CorsFilter.class)
                .csrf(csrf -> csrf.csrfTokenRepository(repository).csrfTokenRequestHandler(handler))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .formLogin(form -> form.disable()).httpBasic(basic -> basic.disable()).logout(logout -> logout.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/auth/password/change").hasAnyRole("STUDENT","TEACHER","ADMIN")
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/auth/csrf").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.POST,
                                "/api/v1/auth/login", "/api/v1/auth/refresh", "/api/v1/auth/logout",
                                "/api/v1/auth/register/student", "/api/v1/auth/register/teacher",
                                "/api/v1/auth/email-verification",
                                "/api/v1/auth/email-verification/resend",
                                "/api/v1/auth/password/forgot", "/api/v1/auth/password/reset").permitAll()
                        .anyRequest().denyAll())
                .exceptionHandling(errors -> errors.authenticationEntryPoint(ApiSecurityErrors.unauthorized(mapper))
                        .accessDeniedHandler(ApiSecurityErrors.forbidden(mapper)));
        var bearer=new DefaultBearerTokenResolver();
        var provider = new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider(decoder);
        provider.setJwtAuthenticationConverter(accounts);
        var filter = new org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter(
                new org.springframework.security.authentication.ProviderManager(provider));
        filter.setBearerTokenResolver(request -> "POST".equals(request.getMethod()) && "/api/v1/auth/password/change".equals(request.getRequestURI().substring(request.getContextPath().length())) ? bearer.resolve(request) : null);
        filter.setAuthenticationEntryPoint(ApiSecurityErrors.unauthorized(mapper));
        // Resource Server's DSL exempts Bearer requests from CSRF; this cookie-backed operation must retain it.
        http.addFilterAt(filter, org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    @org.springframework.core.annotation.Order(2)
    SecurityFilterChain apiSecurity(HttpSecurity http, JwtDecoder decoder, JsonMapper mapper,
            CurrentAccountAuthenticationConverter accounts, BrowserAuthSettings browser) throws Exception {
        var unauthorized = ApiSecurityErrors.unauthorized(mapper);
        var forbidden = ApiSecurityErrors.forbidden(mapper);
        // Business APIs accept header Bearer only; cookie-backed auth has its own CSRF-protected chain.
        http.addFilterAt(BrowserSecurityFilters.cors(browser, mapper), org.springframework.web.filter.CorsFilter.class)
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .requestCache(cache -> cache.disable())
                .formLogin(form -> form.disable()).httpBasic(basic -> basic.disable()).logout(logout -> logout.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/admin/teacher-applications/pending",
                                "/api/v1/admin/teacher-applications/{applicationId}")
                        .access(SecurityConfiguration::verifiedAdmin)
                        .requestMatchers(org.springframework.http.HttpMethod.POST,
                                "/api/v1/admin/teacher-applications/{applicationId}/approve",
                                "/api/v1/admin/teacher-applications/{applicationId}/reject")
                        .access(SecurityConfiguration::verifiedAdmin)
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/teacher/onboarding")
                        .access(SecurityConfiguration::verifiedTeacher)
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/v1/teacher/applications/{applicationId}/resubmit")
                        .access(SecurityConfiguration::verifiedTeacher)
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/users/me/profile").hasRole("STUDENT")
                        .requestMatchers(org.springframework.http.HttpMethod.PATCH, "/api/v1/users/me/profile").hasRole("STUDENT")
                        .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/users/me")
                        .hasAnyRole("STUDENT", "TEACHER", "ADMIN")
                        .anyRequest().denyAll())
                .exceptionHandling(errors -> errors.authenticationEntryPoint(unauthorized).accessDeniedHandler(forbidden))
                .oauth2ResourceServer(resource -> resource.bearerTokenResolver(new DefaultBearerTokenResolver())
                        .authenticationEntryPoint(unauthorized).accessDeniedHandler(forbidden)
                        .jwt(jwt -> jwt.decoder(decoder)
                                .jwtAuthenticationConverter(accounts)));
        return http.build();
    }
}
