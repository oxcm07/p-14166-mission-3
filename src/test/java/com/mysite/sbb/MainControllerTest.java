package com.mysite.sbb;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class MainControllerTest extends AbstractSbbIntegrationTest {

	@Test
	void sbb() throws Exception {
		mockMvc.perform(get("/sbb"))
				.andExpect(status().isOk())
				.andExpect(content().string("안녕하세요 sbb에 오신 것을 환영합니다."));
	}

	@Test
	void rootRedirectsToQnaQuestionList() throws Exception {
		mockMvc.perform(get("/"))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/question/qna/list"));
	}

	@Test
	void staticResourcesAreServedFromOrganizedPaths() throws Exception {
		mockMvc.perform(get("/css/bootstrap.min.css"))
				.andExpect(status().isOk());
		mockMvc.perform(get("/css/style.css"))
				.andExpect(status().isOk());
		mockMvc.perform(get("/js/bootstrap.min.js"))
				.andExpect(status().isOk());
		mockMvc.perform(get("/js/question/list.js"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("searchForm")));
		mockMvc.perform(get("/js/question/detail.js"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("answerSearchForm")));
	}

	@Test
	void oldRootStaticResourcePathsAreNotServed() throws Exception {
		mockMvc.perform(get("/bootstrap.min.css"))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/style.css"))
				.andExpect(status().isNotFound());
		mockMvc.perform(get("/bootstrap.min.js"))
				.andExpect(status().isNotFound());
	}

	@Test
	void layoutUsesOrganizedStaticResourcePaths() throws Exception {
		mockMvc.perform(get("/user/login"))
				.andExpect(status().isOk())
				.andExpect(view().name("user/login_form"))
				.andExpect(content().string(containsString("/css/bootstrap.min.css")))
				.andExpect(content().string(containsString("/css/style.css")))
				.andExpect(content().string(containsString("/js/bootstrap.min.js")))
				.andExpect(content().string(not(containsString("href=\"/bootstrap.min.css\""))))
				.andExpect(content().string(not(containsString("src=\"/bootstrap.min.js\""))))
				.andExpect(content().string(not(containsString("href=\"/style.css\""))));
	}

	@Test
	void testInitDataIsNotCreatedDuringTests() {
		assertThat(questionRepository.count()).isZero();
	}
}
