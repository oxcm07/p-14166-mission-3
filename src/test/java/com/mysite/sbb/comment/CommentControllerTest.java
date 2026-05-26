package com.mysite.sbb.comment;

import com.mysite.sbb.AbstractSbbIntegrationTest;
import com.mysite.sbb.answer.Answer;
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

class CommentControllerTest extends AbstractSbbIntegrationTest {

	@Test
	void commentCreateOnQuestionRedirectsToCategoryQuestionDetail() throws Exception {
		SiteUser author = createUser();
		Question question = createQuestion("댓글 대상 질문", "질문 내용", category("qna"), author, 0);

		mockMvc.perform(post("/comment/create/question/{id}", question.getId())
						.param("content", "질문 댓글 내용")
						.with(user(TEST_USERNAME))
						.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(header().string("Location",
						containsString("/question/qna/detail/1?answerPage=1&answerSort=latest#comment_")));

		Comment comment = commentRepository.findAll().get(0);
		assertThat(comment.getContent()).isEqualTo("질문 댓글 내용");
		assertThat(comment.getQuestion().getId()).isEqualTo(question.getId());
		assertThat(comment.getAuthor().getUsername()).isEqualTo(TEST_USERNAME);
	}

	@Test
	void commentCreateOnAnswerRedirectsToCategoryQuestionDetail() throws Exception {
		SiteUser author = createUser();
		Question question = createQuestion("답변 댓글 대상 질문", "질문 내용", category("qna"), author, 0);
		Answer answer = answerService.create(question, "답변 내용", author);

		mockMvc.perform(post("/comment/create/answer/{id}", answer.getId())
						.param("content", "답변 댓글 내용")
						.with(user(TEST_USERNAME))
						.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(header().string("Location",
						containsString("/question/qna/detail/1?answerPage=1&answerSort=latest#comment_")));

		Comment comment = commentRepository.findAll().get(0);
		assertThat(comment.getContent()).isEqualTo("답변 댓글 내용");
		assertThat(comment.getAnswer().getId()).isEqualTo(answer.getId());
		assertThat(comment.getAuthor().getUsername()).isEqualTo(TEST_USERNAME);
	}

	@Test
	void recentCommentListShowsNewestCommentsFirst() throws Exception {
		SiteUser author = createUser();
		Question question = createQuestion("최근 댓글 대상 질문", "질문 내용", category("qna"), author, 0);
		Answer answer = createAnswer(question, "댓글 대상 답변", author, 1);
		createComment(question, "오래된 댓글", author, 2);
		Comment recentComment = createComment(answer, "최신 댓글", author, 3);

		var result = mockMvc.perform(get("/comment/list"))
				.andExpect(status().isOk())
				.andExpect(view().name("comment/list"))
				.andExpect(model().attributeExists("paging", "questionNumberMap", "questionCategoryCodeMap",
						"commentTargetQuestionMap"))
				.andExpect(content().string(containsString("최근 댓글")))
				.andExpect(content().string(containsString("최근 댓글 대상 질문")))
				.andExpect(content().string(containsString("최신 댓글")))
				.andExpect(content().string(containsString("답변 댓글")))
				.andExpect(content().string(containsString("/question/qna/detail/1#comment_" + recentComment.getId())))
				.andReturn();

		String html = result.getResponse().getContentAsString();
		assertThat(html.indexOf("최신 댓글")).isLessThan(html.indexOf("오래된 댓글"));
	}

	@Test
	void commentModifyFormKeepsPagingParameters() throws Exception {
		SiteUser author = createUser();
		Question question = createQuestion("수정 폼 질문", "질문 내용", category("qna"), author, 0);
		Comment comment = commentService.create(question, "수정 폼 댓글", author);

		mockMvc.perform(get("/comment/modify/{id}", comment.getId())
						.param("answerPage", "3")
						.param("answerSort", "recommend")
						.with(user(TEST_USERNAME)))
				.andExpect(status().isOk())
				.andExpect(view().name("comment/form"))
				.andExpect(model().attribute("answerPage", 3))
				.andExpect(model().attribute("answerSort", "recommend"))
				.andExpect(content().string(containsString("수정 폼 댓글")));
	}
}
