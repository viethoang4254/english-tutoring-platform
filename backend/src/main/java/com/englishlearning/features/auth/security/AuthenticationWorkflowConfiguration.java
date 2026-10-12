package com.englishlearning.features.auth.security;

import com.englishlearning.features.auth.service.AuthenticationWorkflowService;
import com.englishlearning.features.auth.service.RefreshSessionService;
import com.englishlearning.features.user.service.UserCredentialService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class AuthenticationWorkflowConfiguration {
    @Bean
    AuthenticationWorkflowService authenticationWorkflowService(UserCredentialService users,
            RefreshSessionService sessions, PasswordEncoder passwords, AccessTokenIssuer issuer) {
        return new AuthenticationWorkflowService(users, sessions, passwords, issuer);
    }
}
