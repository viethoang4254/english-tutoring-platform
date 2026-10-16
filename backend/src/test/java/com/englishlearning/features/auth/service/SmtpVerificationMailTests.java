package com.englishlearning.features.auth.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSender;

class SmtpVerificationMailTests {
    @SuppressWarnings("unchecked")
    @Test void resetMessageContainsCodeAndFifteenMinuteExpiry() {
        ObjectProvider<JavaMailSender> provider=mock(ObjectProvider.class);
        JavaMailSender sender=mock(JavaMailSender.class);when(provider.getIfAvailable()).thenReturn(sender);
        new SmtpVerificationMail(provider,"noreply@example.invalid").sendReset("account@example.invalid","test-code");
        var message=ArgumentCaptor.forClass(SimpleMailMessage.class);verify(sender).send(message.capture());
        assertTrue(message.getValue().getText().contains("test-code"));
        assertTrue(message.getValue().getText().contains("15 phút"));
    }
    @SuppressWarnings("unchecked")
    @Test void sendsOnlyThroughMailAdapterAndSanitizesFailures() {
        ObjectProvider<JavaMailSender> provider=mock(ObjectProvider.class);
        JavaMailSender sender=mock(JavaMailSender.class); when(provider.getIfAvailable()).thenReturn(sender);
        var mail=new SmtpVerificationMail(provider,"noreply@example.invalid");
        mail.send("student@example.invalid","test-only-code");
        var message=ArgumentCaptor.forClass(SimpleMailMessage.class); verify(sender).send(message.capture());
        assertArrayEquals(new String[]{"student@example.invalid"},message.getValue().getTo());
        assertTrue(message.getValue().getText().contains("test-only-code"));
        assertTrue(message.getValue().getText().contains("24 hours"));
        doThrow(new MailSendException("secret body")).when(sender).send(any(SimpleMailMessage.class));
        var error=assertThrows(VerificationFailure.class,()->mail.send("student@example.invalid","test-only-code"));
        assertEquals(VerificationFailure.Reason.DELIVERY,error.reason()); assertNull(error.getCause());
        assertFalse(error.toString().contains("secret"));
    }
    @SuppressWarnings("unchecked")
    @Test void missingConfigurationFailsSafely() {
        ObjectProvider<JavaMailSender> provider=mock(ObjectProvider.class);
        assertEquals(VerificationFailure.Reason.DELIVERY,assertThrows(VerificationFailure.class,
                ()->new SmtpVerificationMail(provider,"").send("student@example.invalid","code")).reason());
    }
}
