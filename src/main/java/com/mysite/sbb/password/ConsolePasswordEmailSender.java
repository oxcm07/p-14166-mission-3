package com.mysite.sbb.password;

import org.springframework.stereotype.Component;

@Component
public class ConsolePasswordEmailSender implements PasswordEmailSender {
    @Override
    public void sendTemporaryPassword(String email, String temporaryPassword) {
        throw new IllegalStateException("SMTP mail settings are not configured.");
    }
}
