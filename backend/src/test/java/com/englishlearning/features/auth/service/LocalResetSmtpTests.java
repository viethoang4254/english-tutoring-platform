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
    @Test void resetAdapterDeliversOnlyToAuthenticatedLoopbackSinkWithStartTls() throws Exception {
        System.setProperty("javax.net.ssl.trustStore",System.getenv("TEST_SMTP_TRUST_STORE"));
        System.setProperty("javax.net.ssl.trustStorePassword",System.getenv("TEST_SMTP_TRUST_PASSWORD"));
        var sender=new JavaMailSenderImpl();sender.setHost("127.0.0.1");sender.setPort(1025);
        sender.setUsername(System.getenv("TEST_SMTP_USER"));sender.setPassword(System.getenv("TEST_SMTP_PASSWORD"));
        assertNotNull(sender.getUsername());assertNotNull(sender.getPassword());
        var props=sender.getJavaMailProperties();props.setProperty("mail.smtp.auth","true");props.setProperty("mail.smtp.starttls.enable","true");props.setProperty("mail.smtp.starttls.required","true");
        props.setProperty("mail.smtp.ssl.checkserveridentity","true");props.setProperty("mail.smtp.timeout","5000");props.setProperty("mail.smtp.connectiontimeout","5000");
        ObjectProvider<JavaMailSender> provider=mock(ObjectProvider.class);when(provider.getIfAvailable()).thenReturn(sender);
        String recipient="reset-test-"+UUID.randomUUID()+"@example.invalid";
        String code=new com.englishlearning.features.auth.security.PasswordResetTokens(java.util.Base64.getEncoder().encodeToString(new byte[32])).generate();
        assertDoesNotThrow(()->new SmtpVerificationMail(provider,"noreply@example.invalid").sendReset(recipient,code));
        String captureDirectory=System.getenv("TEST_SMTP_CAPTURE_DIR");
        assertNotNull(captureDirectory,"Loopback capture directory is required");
        int captured=0;
        try(var paths=java.nio.file.Files.list(java.nio.file.Path.of(captureDirectory))) {
            for(var path:paths.filter(p->p.toString().endsWith(".json")).toList()) {
                var capture=new tools.jackson.databind.ObjectMapper().readTree(java.nio.file.Files.readString(path));
                if(capture.get("to").size()!=1 || !recipient.equals(capture.get("to").get(0).asText()))continue;
                var message=new jakarta.mail.internet.MimeMessage(jakarta.mail.Session.getInstance(new java.util.Properties()),
                        new java.io.ByteArrayInputStream(capture.get("message").asText().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                assertTrue("Đặt lại mật khẩu".equals(message.getSubject()),"Expected recovery subject");
                assertTrue(message.getContent() instanceof String body && body.contains(code) && body.contains("15 phút"),
                        "Captured recovery body must contain the issued code and expiry");
                captured++;
            }
        }
        assertEquals(1,captured,"Expected exactly one captured recovery email");
    }
}
