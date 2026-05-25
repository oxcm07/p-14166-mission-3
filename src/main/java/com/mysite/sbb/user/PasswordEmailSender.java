package com.mysite.sbb.user;

public interface PasswordEmailSender {
    void sendTemporaryPassword(String email, String temporaryPassword);
}
