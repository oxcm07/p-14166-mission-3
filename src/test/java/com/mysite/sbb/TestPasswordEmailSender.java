package com.mysite.sbb;

import com.mysite.sbb.password.PasswordEmailSender;

public class TestPasswordEmailSender implements PasswordEmailSender {
	private String email;
	private String temporaryPassword;
	private boolean fail;

	@Override
	public void sendTemporaryPassword(String email, String temporaryPassword) {
		this.email = email;
		this.temporaryPassword = temporaryPassword;
		if (this.fail) {
			throw new IllegalStateException("email sending failed");
		}
	}

	public String getEmail() {
		return email;
	}

	public String getTemporaryPassword() {
		return temporaryPassword;
	}

	public void setFail(boolean fail) {
		this.fail = fail;
	}

	void reset() {
		this.email = null;
		this.temporaryPassword = null;
		this.fail = false;
	}
}
