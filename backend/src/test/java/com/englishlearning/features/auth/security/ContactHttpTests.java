package com.englishlearning.features.auth.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.http.MediaType;
import org.mockito.ArgumentCaptor;

@SpringBootTest(properties={"spring.autoconfigure.exclude=org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration",
    "spring.flyway.enabled=false","app.security.browser.local-http=true","app.security.browser.allowed-origins=http://localhost:3000",
    "APP_CONTACT_ENABLED=true","SMTP_FROM=sender@example.invalid","APP_CONTACT_RECIPIENT=support@example.invalid"})
class ContactHttpTests extends AuthenticationHttpTestSupport {
    @MockitoBean JavaMailSender sender;
    String origin(){return "http://localhost:3000";}
    String api(){return "http://localhost:8080";}
    boolean secure(){return false;}
    String body(){return "{\"fullName\":\"Demo User\",\"email\":\"demo@example.invalid\",\"subject\":\"Học trực tuyến\",\"message\":\"Tôi muốn tìm hiểu lịch học.\"}";}
    @Test void publicContactRequiresCsrfAndConfirmsOnlyMailAcceptance() throws Exception {
        mvc.perform(postAuth("contact").contentType(MediaType.APPLICATION_JSON).content(body())).andExpect(status().isForbidden());
        var token=bootstrap();
        mvc.perform(postAuth("contact").cookie(token.cookie()).header("X-XSRF-TOKEN","invalid")
            .contentType(MediaType.APPLICATION_JSON).content(body())).andExpect(status().isForbidden());
        verifyNoInteractions(sender);
        mvc.perform(csrf(postAuth("contact"),bootstrap()).contentType(MediaType.APPLICATION_JSON).content(body()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACCEPTED_BY_MAIL_SERVER"));
        var captured=ArgumentCaptor.forClass(SimpleMailMessage.class);verify(sender).send(captured.capture());
        assertArrayEquals(new String[]{"support@example.invalid"},captured.getValue().getTo());
        assertEquals("demo@example.invalid",captured.getValue().getReplyTo());
        assertTrue(captured.getValue().getText().contains("Tôi muốn tìm hiểu lịch học."));
    }
    @Test void invalidFieldsNeverSendMail() throws Exception {
        for(String invalid:new String[]{body().replace("demo@example.invalid","bad"),body().replace("Demo User",""),body().replace("Học trực tuyến","x".repeat(161)),body().replace("Tôi muốn tìm hiểu lịch học.","x".repeat(5001))})
            mvc.perform(csrf(postAuth("contact"),bootstrap()).contentType(MediaType.APPLICATION_JSON).content(invalid)).andExpect(status().isBadRequest());
        verifyNoInteractions(sender);
    }
    @Test void smtpFailureDoesNotClaimSuccessOrLeakProviderMessage() throws Exception {
        doThrow(new org.springframework.mail.MailSendException("private provider diagnostic")).when(sender).send(any(SimpleMailMessage.class));
        var response=mvc.perform(csrf(postAuth("contact"),bootstrap()).contentType(MediaType.APPLICATION_JSON).content(body()))
            .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value("CONTACT_OUTCOME_UNKNOWN")).andReturn().getResponse();
        assertFalse(response.getContentAsString().contains("private provider diagnostic"));
    }
    @Test void optionalPhoneIsValidatedAndIncludedOnlyInMessageBody() throws Exception {
        String input=body().replace("\"subject\":","\"phone\":\"+84912345678\",\"subject\":");
        mvc.perform(csrf(postAuth("contact"),bootstrap()).contentType(MediaType.APPLICATION_JSON).content(input)).andExpect(status().isOk());
        var captured=ArgumentCaptor.forClass(SimpleMailMessage.class);verify(sender).send(captured.capture());
        assertTrue(captured.getValue().getText().contains("Điện thoại: +84912345678"));
        mvc.perform(csrf(postAuth("contact"),bootstrap()).contentType(MediaType.APPLICATION_JSON).content(input.replace("+84912345678","not-a-phone"))).andExpect(status().isBadRequest());
        verifyNoMoreInteractions(sender);
    }
}
