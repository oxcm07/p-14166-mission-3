package com.mysite.sbb.user;

import com.mysite.sbb.AbstractSbbIntegrationTest;
import com.mysite.sbb.answer.Answer;
import com.mysite.sbb.comment.Comment;
import com.mysite.sbb.question.Question;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class UserControllerTest extends AbstractSbbIntegrationTest {

	@Test
	void profileShowsAuthenticatedUsersActivity() throws Exception {
		SiteUser author = createUser();
		Question question = createQuestion("프로필 질문", "프로필 질문 내용", category("qna"), author, 0);
		Answer answer = answerService.create(question, "프로필 답변 내용", author);
		commentService.create(question, "프로필 질문 댓글", author);
		commentService.create(answer, "프로필 답변 댓글", author);

		mockMvc.perform(get("/user/profile").with(user(TEST_USERNAME)))
				.andExpect(status().isOk())
				.andExpect(view().name("user/profile"))
				.andExpect(model().attributeExists("siteUser", "questionPaging", "answerPaging", "commentPaging",
						"questionNumberMap", "questionCategoryCodeMap", "commentTargetQuestionMap"))
				.andExpect(content().string(containsString("내 프로필")))
				.andExpect(content().string(not(containsString("Page=0"))))
				.andExpect(content().string(containsString("프로필 질문")))
				.andExpect(content().string(containsString("프로필 답변 내용")))
				.andExpect(content().string(containsString("프로필 질문 댓글")))
				.andExpect(content().string(containsString("프로필 답변 댓글")));
	}

	@Test
	void profilePaginatesActivityListsIndependently() throws Exception {
		SiteUser author = createUser();
		Question question = createQuestion("프로필 페이지 질문 원본", "프로필 질문 내용", category("qna"), author, 0);
		for (int i = 1; i <= 6; i++) {
			createQuestion("프로필 페이지 질문 " + i, "프로필 질문 내용 " + i, category("qna"), author, i);
			answerService.create(question, "프로필 페이지 답변 " + i, author);
			commentService.create(question, "프로필 페이지 댓글 " + i, author);
		}

		mockMvc.perform(get("/user/profile")
						.param("questionPage", "2")
						.param("answerPage", "2")
						.param("commentPage", "2")
						.with(user(TEST_USERNAME)))
				.andExpect(status().isOk())
				.andExpect(view().name("user/profile"))
				.andExpect(content().string(containsString("questionPage=2&amp;answerPage=2&amp;commentPage=1")))
				.andExpect(content().string(containsString("questionPage=2&amp;answerPage=1&amp;commentPage=2")))
				.andExpect(content().string(containsString("questionPage=1&amp;answerPage=2&amp;commentPage=2")))
				.andExpect(content().string(containsString("프로필 페이지 질문 원본")))
				.andExpect(content().string(containsString("프로필 페이지 답변 1")))
				.andExpect(content().string(containsString("프로필 페이지 댓글 1")));
	}

	@Test
	void profileHandlesAnswerWithoutQuestion() throws Exception {
		SiteUser author = createUser();
		Answer answer = new Answer();
		answer.setContent("질문 없는 프로필 답변");
		answer.setAuthor(author);
		answer.setCreateDate(LocalDateTime.of(2026, 1, 1, 0, 0));
		answerRepository.save(answer);

		mockMvc.perform(get("/user/profile").with(user(TEST_USERNAME)))
				.andExpect(status().isOk())
				.andExpect(view().name("user/profile"))
				.andExpect(content().string(containsString("연결된 글이 없습니다.")))
				.andExpect(content().string(containsString("질문 없는 프로필 답변")))
				.andExpect(content().string(containsString("href=\"#\"")));
	}

	@Test
	void profileHandlesCommentsWithoutTargetQuestion() throws Exception {
		SiteUser author = createUser();
		Answer answer = new Answer();
		answer.setContent("질문 없는 답변");
		answer.setAuthor(author);
		answer.setCreateDate(LocalDateTime.of(2026, 1, 1, 0, 0));
		answerRepository.save(answer);

		createComment(answer, "질문 없는 답변의 댓글", author, 1);
		Comment orphanComment = new Comment();
		orphanComment.setContent("대상 없는 프로필 댓글");
		orphanComment.setAuthor(author);
		orphanComment.setCreateDate(LocalDateTime.of(2026, 1, 1, 0, 2));
		commentRepository.save(orphanComment);

		mockMvc.perform(get("/user/profile").with(user(TEST_USERNAME)))
				.andExpect(status().isOk())
				.andExpect(view().name("user/profile"))
				.andExpect(content().string(containsString("질문 없는 답변의 댓글")))
				.andExpect(content().string(containsString("대상 없는 프로필 댓글")))
				.andExpect(content().string(containsString("연결된 글이 없습니다.")))
				.andExpect(content().string(containsString("연결 없음")));
	}

	@Test
	void logoutUsesPostWithCsrf() throws Exception {
		createUser();

		mockMvc.perform(get("/user/profile").with(user(TEST_USERNAME)))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("action=\"/user/logout\"")))
				.andExpect(content().string(containsString("method=\"post\"")))
				.andExpect(content().string(not(containsString("href=\"/user/logout\""))));

		mockMvc.perform(get("/user/logout").with(user(TEST_USERNAME)))
				.andExpect(status().isNotFound());

		mockMvc.perform(post("/user/logout")
						.with(user(TEST_USERNAME))
						.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/"));
	}
}
