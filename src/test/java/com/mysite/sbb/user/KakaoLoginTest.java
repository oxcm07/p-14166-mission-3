package com.mysite.sbb.user;

import com.mysite.sbb.AbstractSbbIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class KakaoLoginTest extends AbstractSbbIntegrationTest {

	@Test
	void loginFormShowsKakaoLoginLink() throws Exception {
		mockMvc.perform(get("/user/login"))
				.andExpect(status().isOk())
				.andExpect(view().name("user/login_form"))
				.andExpect(content().string(containsString("카카오로 로그인")))
				.andExpect(content().string(containsString("href=\"/oauth2/authorization/kakao\"")));
	}

	@Test
	void kakaoAuthorizationEndpointRedirectsToKakaoAuthorizeUrl() throws Exception {
		mockMvc.perform(get("/oauth2/authorization/kakao"))
				.andExpect(status().is3xxRedirection())
				.andExpect(header().string("Location", allOf(
						containsString("https://kauth.kakao.com/oauth/authorize"),
						containsString("response_type=code"),
						containsString("client_id=dummy-kakao-client-id"),
						containsString("scope=profile_nickname%20account_email"),
						containsString("redirect_uri=http://localhost/login/oauth2/code/kakao"))));
	}

	@Test
	void findOrCreateKakaoUserCreatesUserWithKakaoProvider() {
		SiteUser user = userService.findOrCreateKakaoUser("12345", "kakao-user@example.com");

		assertThat(user.getUsername()).isEqualTo("kakao_12345");
		assertThat(user.getEmail()).isEqualTo("kakao-user@example.com");
		assertThat(user.getProvider()).isEqualTo("kakao");
		assertThat(user.getProviderId()).isEqualTo("12345");
		assertThat(user.getPassword()).isNotBlank();
	}

	@Test
	void findOrCreateKakaoUserReusesExistingUserForSameProviderId() {
		SiteUser createdUser = userService.findOrCreateKakaoUser("12345", "first@example.com");
		SiteUser reusedUser = userService.findOrCreateKakaoUser("12345", "second@example.com");

		assertThat(reusedUser.getId()).isEqualTo(createdUser.getId());
		assertThat(reusedUser.getEmail()).isEqualTo("first@example.com");
		assertThat(userRepository.findAll()).hasSize(1);
	}

	@Test
	void findOrCreateKakaoUserUsesFallbackEmailWhenKakaoEmailIsMissing() {
		SiteUser user = userService.findOrCreateKakaoUser("12345", null);

		assertThat(user.getUsername()).isEqualTo("kakao_12345");
		assertThat(user.getEmail()).isEqualTo("kakao_12345@kakao.local");
	}

	@Test
	void oauth2LoggedInKakaoUserCanAccessAuthenticatedProfile() throws Exception {
		SiteUser kakaoUser = userService.findOrCreateKakaoUser("12345", "kakao-user@example.com");
		OAuth2UserPrincipal principal = new OAuth2UserPrincipal(
				kakaoUser.getUsername(),
				List.of(new SimpleGrantedAuthority(UserRole.USER.getValue())),
				Map.of(
						"id", "12345",
						"username", kakaoUser.getUsername(),
						"email", kakaoUser.getEmail()));

		mockMvc.perform(get("/user/profile")
						.with(oauth2Login().oauth2User(principal)))
				.andExpect(status().isOk())
				.andExpect(view().name("user/profile"))
				.andExpect(model().attribute("siteUser", kakaoUser))
				.andExpect(content().string(containsString("내 프로필")))
				.andExpect(content().string(containsString(kakaoUser.getUsername())));
	}
}
