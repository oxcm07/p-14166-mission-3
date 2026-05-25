package com.mysite.sbb.password;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Primary
@Component
@ConditionalOnProperty(prefix = "spring.mail", name = "host")
public class MailPasswordEmailSender implements PasswordEmailSender {
    private final JavaMailSender mailSender;
    private final String from;

    public MailPasswordEmailSender(JavaMailSender mailSender,
                                   @Value("${app.mail.from:${spring.mail.username:}}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void sendTemporaryPassword(String email, String temporaryPassword) {
        SimpleMailMessage message = new SimpleMailMessage();
        if (StringUtils.hasText(this.from)) {
            message.setFrom(this.from);
        }
        message.setTo(email);
        message.setSubject("SBB 임시 비밀번호 안내");
        message.setText("임시 비밀번호: " + temporaryPassword + System.lineSeparator()
                + "로그인 후 비밀번호를 변경해 주세요.");
        this.mailSender.send(message);
    }
}
