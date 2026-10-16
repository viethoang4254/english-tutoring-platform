package com.englishlearning.features.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@EnabledIfSystemProperty(named="localResetSmtp",matches="true")
class LocalResetSmtpTests {
    @SuppressWarnings("unchecked")
    @Test void resetAdapterDeliversOnlyToAuthenticatedLoopbackSinkWithStartTls() {
        System.setProperty("javax.net.ssl.trustStore",System.getenv("TEST_SMTP_TRUST_STORE"));
        System.setProperty("javax.net.ssl.trustStorePassword",System.getenv("TEST_SMTP_TRUST_PASSWORD"));
        var sender=new JavaMailSenderImpl();sender.setHost("127.0.0.1");sender.setPort(1025);
        sender.setUsername(System.getenv("TEST_SMTP_USER"));sender.setPassword(System.getenv("TEST_SMTP_PASSWORD"));
        assertNotNull(sender.getUsername());assertNotNull(sender.getPassword());
        var props=sender.getJavaMailProperties();props.setProperty("mail.smtp.auth","true");props.setProperty("mail.smtp.starttls.enable","true");props.setProperty("mail.smtp.starttls.required","true");
        props.setProperty("mail.smtp.ssl.checkserveridentity","true");props.setProperty("mail.smtp.timeout","5000");props.setProperty("mail.smtp.connectiontimeout","5000");
        ObjectProvider<JavaMailSender> provider=mock(ObjectProvider.class);when(provider.getIfAvailable()).thenReturn(sender);
        assertDoesNotThrow(()->new SmtpVerificationMail(provider,"noreply@example.invalid")
                .sendReset("reset-test-"+UUID.randomUUID()+"@example.invalid",new com.englishlearning.features.auth.security.PasswordResetTokens(java.util.Base64.getEncoder().encodeToString(new byte[32])).generate()));
    }
}
