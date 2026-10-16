package com.englishlearning.features.auth.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class SmtpVerificationMail implements VerificationMail, PasswordResetMail {
    private final ObjectProvider<JavaMailSender> sender;
    private final String from;
    public SmtpVerificationMail(ObjectProvider<JavaMailSender> sender, @Value("${SMTP_FROM:}") String from) {
        this.sender = sender; this.from = from;
    }
    public void send(String email, String token) {
        deliver(email, "Verify your English Tutoring Platform email", "Your email verification code is:\n" + token
                + "\n\nIt expires in 24 hours. If you did not register, ignore this email.");
    }
    public void sendReset(String email, String token) {
        deliver(email, "Đặt lại mật khẩu", "Mã đặt lại mật khẩu của bạn:\n" + token
                + "\n\nMã có hiệu lực trong 15 phút. Nhập mã trên trang đặt lại mật khẩu. Nếu không yêu cầu, hãy bỏ qua email này.");
    }
    private void deliver(String email, String subject, String body) {
        try {
            var mail = sender.getIfAvailable();
            if (mail == null || from.isBlank() || from.contains("\r") || from.contains("\n")) {
                throw new IllegalStateException();
            }
            var message = new SimpleMailMessage();
            message.setFrom(from); message.setTo(email);
            message.setSubject(subject);
            message.setText(body);
            mail.send(message);
        } catch (RuntimeException failure) {
            // Mail exceptions may contain message bodies, recipients or transport secrets.
            throw new VerificationFailure(VerificationFailure.Reason.DELIVERY);
        }
    }
}
