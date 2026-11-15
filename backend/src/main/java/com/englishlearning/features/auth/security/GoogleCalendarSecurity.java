package com.englishlearning.features.auth.security;

import com.englishlearning.features.googlecalendar.GoogleSettings;
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
public class GoogleCalendarSecurity {
    @Bean @Order(0)
    SecurityFilterChain googleCalendarHttp(HttpSecurity http,BrowserAuthSettings browser,JsonMapper mapper,JwtDecoder decoder,CurrentAccountAuthenticationConverter accounts)throws Exception{
        var repository=new CookieCsrfTokenRepository();repository.setCookieName("GOOGLE-XSRF-TOKEN");
        repository.setCookieCustomizer(c->c.httpOnly(true).secure(browser.secure()).sameSite("Lax").path(GoogleSettings.PATH));
        var xor=new XorCsrfTokenRequestAttributeHandler();
        var handler=new CsrfTokenRequestHandler(){
            public void handle(jakarta.servlet.http.HttpServletRequest r,jakarta.servlet.http.HttpServletResponse s,java.util.function.Supplier<CsrfToken> t){xor.handle(r,s,t);}
            public String resolveCsrfTokenValue(jakarta.servlet.http.HttpServletRequest r,CsrfToken t){return r.getHeader(t.getHeaderName())==null?null:xor.resolveCsrfTokenValue(r,t);}
        };
        http.securityMatcher(GoogleSettings.PATH,GoogleSettings.PATH+"/**")
            .addFilterAt(BrowserSecurityFilters.cors(browser,mapper),org.springframework.web.filter.CorsFilter.class)
            .addFilterAfter(BrowserSecurityFilters.transport(browser,mapper),org.springframework.web.filter.CorsFilter.class)
            .csrf(c->c.csrfTokenRepository(repository).csrfTokenRequestHandler(handler))
            .sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)).requestCache(c->c.disable())
            .formLogin(c->c.disable()).httpBasic(c->c.disable()).logout(c->c.disable())
            .authorizeHttpRequests(a->a.requestMatchers(org.springframework.http.HttpMethod.GET,GoogleSettings.PATH+"/callback").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET,GoogleSettings.PATH).hasAnyRole("STUDENT","TEACHER")
                .requestMatchers(org.springframework.http.HttpMethod.POST,GoogleSettings.PATH+"/connect").hasAnyRole("STUDENT","TEACHER")
                .requestMatchers(org.springframework.http.HttpMethod.DELETE,GoogleSettings.PATH).hasAnyRole("STUDENT","TEACHER").anyRequest().denyAll())
            .exceptionHandling(e->e.authenticationEntryPoint(ApiSecurityErrors.unauthorized(mapper)).accessDeniedHandler(ApiSecurityErrors.forbidden(mapper)));
        var provider=new org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider(decoder);provider.setJwtAuthenticationConverter(accounts);
        var filter=new BearerTokenAuthenticationFilter(new org.springframework.security.authentication.ProviderManager(provider));
        var bearer=new DefaultBearerTokenResolver();
        filter.setBearerTokenResolver(r->r.getRequestURI().substring(r.getContextPath().length()).equals(GoogleSettings.PATH+"/callback")?null:bearer.resolve(r));
        filter.setAuthenticationEntryPoint(ApiSecurityErrors.unauthorized(mapper));
        http.addFilterAt(filter,BearerTokenAuthenticationFilter.class);
        return http.build();
    }
}
