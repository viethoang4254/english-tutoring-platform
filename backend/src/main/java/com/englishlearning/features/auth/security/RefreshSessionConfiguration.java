package com.englishlearning.features.auth.security;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class RefreshSessionConfiguration {
    @Bean
    public Clock refreshSessionClock() { return Clock.systemUTC(); }
}
