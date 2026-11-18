package com.englishlearning.features.contact;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;

/** Uses the existing browser-auth CSRF cookie scope; no login is required. */
@RestController
public class ContactController {
    public record Input(
        @NotBlank @Size(max=120) @Pattern(regexp="[^\\p{Cntrl}]+") String fullName,
        @NotBlank @Email @Size(max=254) @Pattern(regexp="[^\\s<>]+") String email,
        @Size(max=24) @Pattern(regexp="(?:[+]?[0-9 ]{7,23})?") String phone,
        @NotBlank @Size(max=160) @Pattern(regexp="[^\\p{Cntrl}]+") String subject,
        @NotBlank @Size(max=5000) @Pattern(regexp="[^\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]+") String message) {}

    private final ObjectProvider<JavaMailSender> mail;
    private final boolean enabled;
    private final String from, recipient;
    private long window;
    private int accepted;

    public ContactController(ObjectProvider<JavaMailSender> mail,
            @Value("${APP_CONTACT_ENABLED:false}") boolean enabled,
            @Value("${SMTP_FROM:}") String from,
            @Value("${APP_CONTACT_RECIPIENT:}") String recipient) {
        this.mail=mail; this.enabled=enabled; this.from=from; this.recipient=recipient;
        if(enabled && (!address(from) || !address(recipient)))
            throw new IllegalArgumentException("Contact sender and recipient configuration required");
    }
    private static boolean address(String value) {
        return value.matches("[^\\s@<>;,]+@[^\\s@<>;,]+\\.[^\\s@<>;,]+");
    }
    // shortcut: process-wide 20 attempts/hour; use a shared gateway limit before multiple replicas.
    private synchronized boolean reserve() {
        long hour=System.currentTimeMillis()/3_600_000;
        if(hour!=window){window=hour;accepted=0;}
        if(accepted>=20)return false;
        accepted++;return true;
    }
    @PostMapping("/api/v1/auth/contact")
    public ResponseEntity<?> submit(@Valid @RequestBody Input input,HttpServletRequest request) {
        if(!request.getParameterMap().isEmpty())return failure(400,"INVALID_REQUEST");
        if(!enabled)return failure(503,"CONTACT_UNAVAILABLE");
        var sender=mail.getIfAvailable();
        if(sender==null)return failure(503,"CONTACT_UNAVAILABLE");
        if(!reserve())return failure(429,"CONTACT_RATE_LIMITED");
        var message=new SimpleMailMessage();
        message.setFrom(from);message.setTo(recipient);message.setReplyTo(input.email().trim());
        message.setSubject("Liên hệ: "+input.subject().trim());
        message.setText(input.fullName().trim()+"\n"+input.email().trim()+
            (input.phone()==null || input.phone().isBlank()?"":"\nĐiện thoại: "+input.phone().trim())+"\n\n"+input.message().trim());
        try {sender.send(message);}
        catch(org.springframework.mail.MailException failure){return failure(503,"CONTACT_OUTCOME_UNKNOWN");}
        return ResponseEntity.ok().header("Cache-Control","no-store").body(Map.of("status","ACCEPTED_BY_MAIL_SERVER"));
    }
    private ResponseEntity<?> failure(int status,String code) {
        return ResponseEntity.status(status).header("Cache-Control","no-store").body(Map.of("code",code,"message","Contact request could not be confirmed"));
    }
}
