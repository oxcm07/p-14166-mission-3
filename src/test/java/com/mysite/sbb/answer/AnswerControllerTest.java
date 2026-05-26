package com.mysite.sbb.answer;

import com.mysite.sbb.AbstractSbbIntegrationTest;
import com.mysite.sbb.question.Question;
import com.mysite.sbb.user.SiteUser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class AnswerControllerTest extends AbstractSbbIntegrationTest {

	@Test
	void answerCreateRedirectsToCategoryQuestionDetail() throws Exception {
		SiteUser author = createUser();
		Question question = createQuestion("답변 대상 질문", "질문 내용", category("qna"), author, 0);

		mockMvc.perform(post("/answer/create/{id}", question.getId())
						.param("content", "답변 테스트 내용")
						.with(user(TEST_USERNAME))
						.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(header().string("Location",
						containsString("/question/qna/detail/1?answerPage=1&answerSort=latest#answer_")));

		Answer answer = answerRepository.findAll().get(0);
		assertThat(answer.getContent()).isEqualTo("답변 테스트 내용");
		assertThat(answer.getQuestion().getId()).isEqualTo(question.getId());
		assertThat(answer.getAuthor().getUsername()).isEqualTo(TEST_USERNAME);
	}

	@Test
	void recentAnswerListShowsNewestAnswersFirst() throws Exception {
		SiteUser author = createUser();
		Question question = createQuestion("최근 답변 대상 질문", "질문 내용", category("qna"), author, 0);
		createAnswer(question, "오래된 답변", author, 1);
		Answer recentAnswer = createAnswer(question, "최신 답변", author, 2);

		var result = mockMvc.perform(get("/answer/list"))
				.andExpect(status().isOk())
				.andExpect(view().name("answer/list"))
				.andExpect(model().attributeExists("paging", "questionNumberMap", "questionCategoryCodeMap"))
				.andExpect(content().string(containsString("최근 답변")))
				.andExpect(content().string(containsString("최근 답변 대상 질문")))
				.andExpect(content().string(containsString("최신 답변")))
				.andExpect(content().string(containsString("/question/qna/detail/1#answer_" + recentAnswer.getId())))
				.andReturn();

		String html = result.getResponse().getContentAsString();
		assertThat(html.indexOf("최신 답변")).isLessThan(html.indexOf("오래된 답변"));
	}

	@Test
	void answerModifyFormKeepsPagingParameters() throws Exception {
		SiteUser author = createUser();
		Question question = createQuestion("수정 폼 질문", "질문 내용", category("qna"), author, 0);
		Answer answer = answerService.create(question, "수정 폼 답변", author);

		mockMvc.perform(get("/answer/modify/{id}", answer.getId())
						.param("answerPage", "2")
						.param("answerSort", "old")
						.with(user(TEST_USERNAME)))
				.andExpect(status().isOk())
				.andExpect(view().name("answer/form"))
				.andExpect(model().attribute("answerPage", 2))
				.andExpect(model().attribute("answerSort", "old"))
				.andExpect(content().string(containsString("수정 폼 답변")));
	}
}
