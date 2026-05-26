package com.mysite.sbb.password;

import com.mysite.sbb.AbstractSbbIntegrationTest;
import com.mysite.sbb.user.SiteUser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class PasswordControllerTest extends AbstractSbbIntegrationTest {

	@Test
	void passwordResetSendsTemporaryPasswordAndUpdatesStoredPassword() throws Exception {
		createUser();

		mockMvc.perform(post("/user/password/reset")
						.param("email", "testuser@example.com")
						.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/user/login?reset"));

		assertThat(passwordEmailSender.getEmail()).isEqualTo("testuser@example.com");
		assertThat(passwordEmailSender.getTemporaryPassword()).isNotBlank();

		SiteUser user = userRepository.findByUsername(TEST_USERNAME).orElseThrow();
		assertThat(passwordEncoder.matches(passwordEmailSender.getTemporaryPassword(), user.getPassword())).isTrue();
		assertThat(passwordEncoder.matches("1234", user.getPassword())).isFalse();

		mockMvc.perform(post("/user/login")
						.param("username", TEST_USERNAME)
						.param("password", passwordEmailSender.getTemporaryPassword())
						.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/"));
	}

	@Test
	void passwordResetRejectsUnknownEmail() throws Exception {
		mockMvc.perform(post("/user/password/reset")
						.param("email", "unknown@example.com")
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(view().name("password/reset_form"))
				.andExpect(model().hasErrors())
				.andExpect(content().string(containsString("등록된 이메일을 찾을 수 없습니다.")));

		assertThat(passwordEmailSender.getEmail()).isNull();
		assertThat(passwordEmailSender.getTemporaryPassword()).isNull();
	}

	@Test
	void passwordResetKeepsCurrentPasswordWhenEmailSendingFails() throws Exception {
		createUser();
		passwordEmailSender.setFail(true);

		mockMvc.perform(post("/user/password/reset")
						.param("email", "testuser@example.com")
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(view().name("password/reset_form"))
				.andExpect(model().hasErrors())
				.andExpect(content().string(containsString("임시 비밀번호 메일 발송에 실패했습니다.")));

		SiteUser user = userRepository.findByUsername(TEST_USERNAME).orElseThrow();
		assertThat(passwordEncoder.matches("1234", user.getPassword())).isTrue();
	}

	@Test
	void passwordChangeUpdatesPasswordWhenCurrentPasswordMatches() throws Exception {
		createUser();

		mockMvc.perform(post("/user/password/change")
						.param("currentPassword", "1234")
						.param("newPassword1", "5678")
						.param("newPassword2", "5678")
						.with(user(TEST_USERNAME))
						.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/user/password/change?success"));

		SiteUser user = userRepository.findByUsername(TEST_USERNAME).orElseThrow();
		assertThat(passwordEncoder.matches("5678", user.getPassword())).isTrue();
		assertThat(passwordEncoder.matches("1234", user.getPassword())).isFalse();

		mockMvc.perform(post("/user/login")
						.param("username", TEST_USERNAME)
						.param("password", "5678")
						.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/"));
	}

	@Test
	void passwordChangeRejectsWrongCurrentPassword() throws Exception {
		createUser();

		mockMvc.perform(post("/user/password/change")
						.param("currentPassword", "wrong")
						.param("newPassword1", "5678")
						.param("newPassword2", "5678")
						.with(user(TEST_USERNAME))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(view().name("password/change_form"))
				.andExpect(model().hasErrors())
				.andExpect(content().string(containsString("기존 비밀번호가 일치하지 않습니다.")));

		SiteUser user = userRepository.findByUsername(TEST_USERNAME).orElseThrow();
		assertThat(passwordEncoder.matches("1234", user.getPassword())).isTrue();
	}

	@Test
	void passwordChangeRejectsMismatchedNewPasswords() throws Exception {
		createUser();

		mockMvc.perform(post("/user/password/change")
						.param("currentPassword", "1234")
						.param("newPassword1", "5678")
						.param("newPassword2", "9999")
						.with(user(TEST_USERNAME))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(view().name("password/change_form"))
				.andExpect(model().hasErrors())
				.andExpect(content().string(containsString("2개의 새 비밀번호가 일치하지 않습니다.")));
	}
}
