package com.mysite.sbb.password;

public interface PasswordEmailSender {
    void sendTemporaryPassword(String email, String temporaryPassword);
}
