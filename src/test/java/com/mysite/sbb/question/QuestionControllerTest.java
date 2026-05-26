package com.mysite.sbb.question;

import com.mysite.sbb.AbstractSbbIntegrationTest;
import com.mysite.sbb.category.Category;
import com.mysite.sbb.user.SiteUser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

class QuestionControllerTest extends AbstractSbbIntegrationTest {

	@Test
	void questionListShowsSelectedCategoryOnly() throws Exception {
		SiteUser author = createUser();
		Category qna = category("qna");
		Category lecture = category("lecture");
		createQuestion("qna 제목", "qna 내용", qna, author, 0);
		createQuestion("lecture 제목", "lecture 내용", lecture, author, 1);

		mockMvc.perform(get("/question/qna/list"))
				.andExpect(status().isOk())
				.andExpect(view().name("question/list"))
				.andExpect(model().attributeExists("paging", "categoryList", "category"))
				.andExpect(content().string(containsString("질문답변")))
				.andExpect(content().string(containsString("강좌")))
				.andExpect(content().string(containsString("qna 제목")))
				.andExpect(content().string(not(containsString("lecture 제목"))));
	}

	@Test
	void questionListSearchesWithinCategory() throws Exception {
		SiteUser author = createUser();
		Category qna = category("qna");
		createQuestion("스프링 검색 제목", "검색 대상", qna, author, 0);
		createQuestion("다른 제목", "다른 내용", qna, author, 1);

		mockMvc.perform(get("/question/qna/list").param("kw", "스프링"))
				.andExpect(status().isOk())
				.andExpect(model().attribute("kw", "스프링"))
				.andExpect(content().string(containsString("스프링 검색 제목")))
				.andExpect(content().string(not(containsString("다른 제목"))));
	}

	@Test
	void questionListPageNumbersStartFromOne() throws Exception {
		SiteUser author = createUser();
		Category qna = category("qna");
		for (int i = 0; i < 11; i++) {
			createQuestion("페이지 번호 질문 " + i, "페이지 번호 내용 " + i, qna, author, i);
		}

		mockMvc.perform(get("/question/qna/list"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("data-page=\"1\">1</a>")))
				.andExpect(content().string(containsString("data-page=\"2\">2</a>")))
				.andExpect(content().string(not(containsString("data-page=\"0\""))));

		mockMvc.perform(get("/question/qna/list").param("page", "2"))
				.andExpect(status().isOk())
				.andExpect(model().attributeExists("paging"))
				.andExpect(content().string(containsString("data-page=\"2\">2</a>")));
	}

	@Test
	void questionDetailUsesCategoryQuestionNumber() throws Exception {
		SiteUser author = createUser();
		Category qna = category("qna");
		createQuestion("첫번째 질문", "첫번째 내용", qna, author, 0);
		Question secondQuestion = createQuestion("두번째 질문", "두번째 내용", qna, author, 1);

		mockMvc.perform(get("/question/qna/detail/2"))
				.andExpect(status().isOk())
				.andExpect(view().name("question/detail"))
				.andExpect(model().attribute("questionNumber", 2L))
				.andExpect(model().attribute("question", secondQuestion))
				.andExpect(content().string(containsString("두번째 질문")))
				.andExpect(content().string(not(containsString("첫번째 질문"))));
	}

	@Test
	void questionDetailIncreasesAndShowsViewCount() throws Exception {
		SiteUser author = createUser();
		Question question = createQuestion("조회수 질문", "조회수 내용", category("qna"), author, 0);

		mockMvc.perform(get("/question/qna/detail/1"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("조회수")))
				.andExpect(content().string(containsString(">1</div>")));

		assertThat(questionRepository.findById(question.getId()).orElseThrow().getViewCount()).isEqualTo(1);
	}

	@Test
	void questionDetailRendersSanitizedMarkdownContent() throws Exception {
		SiteUser author = createUser();
		createQuestion("XSS 질문", """
				<script>alert('xss')</script>
				<img src=x onerror=alert('xss')>
				[click](javascript:alert('xss'))
				![image](data:text/html,<script>alert('xss')</script>)
				""", category("qna"), author, 0);

		mockMvc.perform(get("/question/qna/detail/1"))
				.andExpect(status().isOk())
				.andExpect(content().string(not(containsString("<script>alert"))))
				.andExpect(content().string(not(containsString("<img src=x onerror="))))
				.andExpect(content().string(not(containsString("onerror="))))
				.andExpect(content().string(not(containsString("javascript:alert"))))
				.andExpect(content().string(not(containsString("data:text/html"))))
				.andExpect(content().string(containsString("&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;")))
				.andExpect(content().string(containsString("&lt;img src&#61;x onerror&#61;alert(&#39;xss&#39;)&gt;")))
				.andExpect(content().string(containsString("href=\"\" rel=\"nofollow\">click</a>")))
				.andExpect(content().string(containsString("<img src=\"\"")));
	}

	@Test
	void questionDetailAnswerPageNumbersStartFromOne() throws Exception {
		SiteUser author = createUser();
		Question question = createQuestion("답변 페이지 질문", "질문 내용", category("qna"), author, 0);
		for (int i = 1; i <= 6; i++) {
			answerService.create(question, "답변 페이지 답변 " + i, author);
		}

		mockMvc.perform(get("/question/qna/detail/1"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("data-page=\"1\">1</a>")))
				.andExpect(content().string(containsString("data-page=\"2\">2</a>")))
				.andExpect(content().string(not(containsString("data-page=\"0\""))));

		mockMvc.perform(get("/question/qna/detail/1").param("answerPage", "2"))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("id=\"answerPage\" name=\"answerPage\" value=\"2\"")));
	}

	@Test
	void questionCreateFormUsesRequestedCategory() throws Exception {
		createUser();

		mockMvc.perform(get("/question/create")
						.param("categoryCode", "free")
						.with(user(TEST_USERNAME)))
				.andExpect(status().isOk())
				.andExpect(view().name("question/form"))
				.andExpect(model().attributeExists("categoryList", "questionForm"))
				.andExpect(model().attribute("formAction", "/question/create"))
				.andExpect(content().string(containsString("자유게시판")));
	}

	@Test
	void questionCreateSavesQuestionAndRedirectsToSelectedCategory() throws Exception {
		createUser();

		mockMvc.perform(post("/question/create")
						.param("subject", "등록 테스트 제목")
						.param("content", "등록 테스트 내용")
						.param("categoryCode", "free")
						.with(user(TEST_USERNAME))
						.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(header().string("Location", "/question/free/list"));

		Question question = questionRepository.findAll().get(0);
		assertThat(question.getSubject()).isEqualTo("등록 테스트 제목");
		assertThat(question.getCategory().getCode()).isEqualTo("free");
		assertThat(question.getAuthor().getUsername()).isEqualTo(TEST_USERNAME);
	}

	@Test
	void questionModifyUpdatesQuestionAndCategory() throws Exception {
		SiteUser author = createUser();
		Question question = createQuestion("수정 전 제목", "수정 전 내용", category("qna"), author, 0);

		mockMvc.perform(post("/question/modify/{id}", question.getId())
						.param("subject", "수정 후 제목")
						.param("content", "수정 후 내용")
						.param("categoryCode", "lecture")
						.with(user(TEST_USERNAME))
						.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(header().string("Location", "/question/lecture/detail/1"));

		Question modifiedQuestion = questionRepository.findById(question.getId()).orElseThrow();
		assertThat(modifiedQuestion.getSubject()).isEqualTo("수정 후 제목");
		assertThat(modifiedQuestion.getContent()).isEqualTo("수정 후 내용");
		assertThat(modifiedQuestion.getCategory().getCode()).isEqualTo("lecture");
		assertThat(modifiedQuestion.getModifyDate()).isNotNull();
	}
}
