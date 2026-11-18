package com.englishlearning.features.auth.security;


import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.*;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import tools.jackson.databind.json.JsonMapper;

@Configuration(proxyBeanMethods=false)
@ConditionalOnWebApplication(type=ConditionalOnWebApplication.Type.SERVLET)
public class AdminManagementSecurity {
    @Bean @Order(0)
    SecurityFilterChain adminManagementHttp(HttpSecurity http,BrowserAuthSettings browser,JsonMapper mapper,JwtDecoder decoder,CurrentAccountAuthenticationConverter accounts)throws Exception{
        var repository=new CookieCsrfTokenRepository();repository.setCookieName("ADMIN-XSRF-TOKEN");
        repository.setCookieCustomizer(c->c.httpOnly(true).secure(browser.secure()).sameSite("Lax").path("/api/v1/admin"));
        var xor=new XorCsrfTokenRequestAttributeHandler();
        var handler=new CsrfTokenRequestHandler(){
            public void handle(jakarta.servlet.http.HttpServletRequest r,jakarta.servlet.http.HttpServletResponse s,java.util.function.Supplier<CsrfToken> t){xor.handle(r,s,t);}
            public String resolveCsrfTokenValue(jakarta.servlet.http.HttpServletRequest r,CsrfToken t){return r.getHeader(t.getHeaderName())==null?null:xor.resolveCsrfTokenValue(r,t);}
        };
        http.securityMatcher("/api/v1/admin/users", "/api/v1/admin/users/**", "/api/v1/admin/teacher-applications", "/api/v1/admin/teacher-applications/**", "/api/v1/admin/csrf", "/api/v1/admin/payment-accounts", "/api/v1/admin/payment-accounts/**")
            .addFilterAt(BrowserSecurityFilters.cors(browser,mapper),org.springframework.web.filter.CorsFilter.class)
            .addFilterAfter(BrowserSecurityFilters.transport(browser,mapper),org.springframework.web.filter.CorsFilter.class)
            .csrf(c->c.csrfTokenRepository(repository).csrfTokenRequestHandler(handler))
            .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)).requestCache(c->c.disable())
            .formLogin(c->c.disable()).httpBasic(c->c.disable()).logout(c->c.disable())
            .authorizeHttpRequests(a->a
                .requestMatchers(org.springframework.http.HttpMethod.GET,"/api/v1/admin/csrf","/api/v1/admin/users","/api/v1/admin/users/{id}","/api/v1/admin/teacher-applications","/api/v1/admin/teacher-applications/pending","/api/v1/admin/teacher-applications/{id}","/api/v1/admin/payment-accounts","/api/v1/admin/payment-accounts/pending","/api/v1/admin/payment-accounts/{id}","/api/v1/admin/payment-accounts/{id}/qr-image").access(AdminManagementSecurity::admin)
                .requestMatchers(org.springframework.http.HttpMethod.PATCH,"/api/v1/admin/users/{id}/lock","/api/v1/admin/users/{id}/profile").access(AdminManagementSecurity::admin)
                .requestMatchers(org.springframework.http.HttpMethod.POST,"/api/v1/admin/users","/api/v1/admin/payment-accounts/{id}/approve","/api/v1/admin/payment-accounts/{id}/reject","/api/v1/admin/teacher-applications/{id}/approve","/api/v1/admin/teacher-applications/{id}/reject").access(AdminManagementSecurity::admin)
                .requestMatchers(org.springframework.http.HttpMethod.DELETE,"/api/v1/admin/users/{id}").access(AdminManagementSecurity::admin)
                .anyRequest().denyAll())
            .exceptionHandling(e->e.authenticationEntryPoint(ApiSecurityErrors.unauthorized(mapper)).accessDeniedHandler(ApiSecurityErrors.forbidden(mapper)));
        var provider=new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider(decoder);provider.setJwtAuthenticationConverter(accounts);
        var filter=new BearerTokenAuthenticationFilter(new org.springframework.security.authentication.ProviderManager(provider));
        var bearer=new DefaultBearerTokenResolver();
        filter.setBearerTokenResolver(bearer);
        filter.setAuthenticationEntryPoint(ApiSecurityErrors.unauthorized(mapper));
        http.addFilterAt(filter,BearerTokenAuthenticationFilter.class);
        return http.build();
    }
    private static org.springframework.security.authorization.AuthorizationDecision admin(java.util.function.Supplier<? extends org.springframework.security.core.Authentication> auth, org.springframework.security.web.access.intercept.RequestAuthorizationContext context) {
        var principal=auth.get().getPrincipal();
        return new org.springframework.security.authorization.AuthorizationDecision(principal instanceof AccountPrincipal a && a.role()==com.englishlearning.features.user.entity.User.Role.ADMIN && a.emailVerified());
    }
}
