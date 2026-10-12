package com.englishlearning.features.auth.security;

import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration(proxyBeanMethods = false)
public class PasswordConfiguration {
    private static final Pattern BCRYPT_HASH = Pattern.compile(
            "\\{bcrypt\\}\\$2[aby]\\$(0[4-9]|[12][0-9]|3[01])\\$[./A-Za-z0-9]{53}");

    @Bean
    public PasswordEncoder passwordEncoder() {
        var delegate = new DelegatingPasswordEncoder("bcrypt", Map.of("bcrypt", new BCryptPasswordEncoder(12)));
        return new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                PasswordInput.validate(rawPassword);
                return delegate.encode(rawPassword);
            }

            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                if (!PasswordInput.isValid(rawPassword) || encodedPassword == null
                        || !BCRYPT_HASH.matcher(encodedPassword).matches()) return false;
                try {
                    return delegate.matches(rawPassword, encodedPassword);
                } catch (IllegalArgumentException exception) {
                    return false;
                }
            }
        };
    }
}
