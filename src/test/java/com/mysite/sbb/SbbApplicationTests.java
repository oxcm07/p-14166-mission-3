package com.mysite.sbb;

import com.mysite.sbb.answer.Answer;
import com.mysite.sbb.answer.AnswerRepository;
import com.mysite.sbb.answer.AnswerService;
import com.mysite.sbb.category.Category;
import com.mysite.sbb.category.CategoryService;
import com.mysite.sbb.comment.Comment;
import com.mysite.sbb.comment.CommentRepository;
import com.mysite.sbb.comment.CommentService;
import com.mysite.sbb.question.Question;
import com.mysite.sbb.question.QuestionRepository;
import com.mysite.sbb.password.PasswordEmailSender;
import com.mysite.sbb.user.SiteUser;
import com.mysite.sbb.user.UserRepository;
import com.mysite.sbb.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@Transactional
class SbbApplicationTests {
	private static final String TEST_USERNAME = "testuser";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private QuestionRepository questionRepository;

	@Autowired
	private AnswerRepository answerRepository;

	@Autowired
	private CommentRepository commentRepository;

	@Autowired
	private CategoryService categoryService;

	@Autowired
	private UserService userService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Autowired
	private TestPasswordEmailSender passwordEmailSender;

	@Autowired
	private AnswerService answerService;

	@Autowired
	private CommentService commentService;

	@BeforeEach
	void setUp() {
		passwordEmailSender.reset();
	}

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
	void testInitDataIsNotCreatedDuringTests() {
		assertThat(questionRepository.count()).isZero();
	}

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
				.andExpect(model().attributeExists("paging", "categoryList", "category", "questionNumberMap"))
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
				.andExpect(redirectedUrl("/question/free/list"));

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

	@Test
	void answerCreateRedirectsToCategoryQuestionDetail() throws Exception {
		SiteUser author = createUser();
		Question question = createQuestion("답변 대상 질문", "질문 내용", category("qna"), author, 0);

		mockMvc.perform(post("/answer/create/{id}", question.getId())
						.param("content", "답변 테스트 내용")
						.with(user(TEST_USERNAME))
						.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(header().string("Location", containsString("/question/qna/detail/1?answerPage=1&answerSort=latest#answer_")));

		Answer answer = answerRepository.findAll().get(0);
		assertThat(answer.getContent()).isEqualTo("답변 테스트 내용");
		assertThat(answer.getQuestion().getId()).isEqualTo(question.getId());
		assertThat(answer.getAuthor().getUsername()).isEqualTo(TEST_USERNAME);
	}

	@Test
	void commentCreateOnQuestionRedirectsToCategoryQuestionDetail() throws Exception {
		SiteUser author = createUser();
		Question question = createQuestion("댓글 대상 질문", "질문 내용", category("qna"), author, 0);

		mockMvc.perform(post("/comment/create/question/{id}", question.getId())
						.param("content", "질문 댓글 내용")
						.with(user(TEST_USERNAME))
						.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(header().string("Location", containsString("/question/qna/detail/1?answerPage=1&answerSort=latest#comment_")));

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
				.andExpect(header().string("Location", containsString("/question/qna/detail/1?answerPage=1&answerSort=latest#comment_")));

		Comment comment = commentRepository.findAll().get(0);
		assertThat(comment.getContent()).isEqualTo("답변 댓글 내용");
		assertThat(comment.getAnswer().getId()).isEqualTo(answer.getId());
		assertThat(comment.getAuthor().getUsername()).isEqualTo(TEST_USERNAME);
	}

	@Test
	void passwordResetSendsTemporaryPasswordAndUpdatesStoredPassword() throws Exception {
		createUser();

		mockMvc.perform(post("/user/password/reset")
						.param("email", "testuser@example.com")
						.with(csrf()))
				.andExpect(status().is3xxRedirection())
				.andExpect(redirectedUrl("/user/login?reset"));

		assertThat(passwordEmailSender.email).isEqualTo("testuser@example.com");
		assertThat(passwordEmailSender.temporaryPassword).isNotBlank();

		SiteUser user = userRepository.findByUsername(TEST_USERNAME).orElseThrow();
		assertThat(passwordEncoder.matches(passwordEmailSender.temporaryPassword, user.getPassword())).isTrue();
		assertThat(passwordEncoder.matches("1234", user.getPassword())).isFalse();

		mockMvc.perform(post("/user/login")
						.param("username", TEST_USERNAME)
						.param("password", passwordEmailSender.temporaryPassword)
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

		assertThat(passwordEmailSender.email).isNull();
		assertThat(passwordEmailSender.temporaryPassword).isNull();
	}

	@Test
	void passwordResetKeepsCurrentPasswordWhenEmailSendingFails() throws Exception {
		createUser();
		passwordEmailSender.fail = true;

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
	void answerAndCommentModifyFormsKeepPagingParameters() throws Exception {
		SiteUser author = createUser();
		Question question = createQuestion("수정 폼 질문", "질문 내용", category("qna"), author, 0);
		Answer answer = answerService.create(question, "수정 폼 답변", author);
		Comment comment = commentService.create(question, "수정 폼 댓글", author);

		mockMvc.perform(get("/answer/modify/{id}", answer.getId())
						.param("answerPage", "2")
						.param("answerSort", "old")
						.with(user(TEST_USERNAME)))
				.andExpect(status().isOk())
				.andExpect(view().name("answer/form"))
				.andExpect(model().attribute("answerPage", 2))
				.andExpect(model().attribute("answerSort", "old"))
				.andExpect(content().string(containsString("수정 폼 답변")));

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

	private SiteUser createUser() {
		return userService.create(TEST_USERNAME, "testuser@example.com", "1234");
	}

	private Category category(String code) {
		return categoryService.getCategory(code);
	}

	private Question createQuestion(String subject, String content, Category category, SiteUser author, int seconds) {
		Question question = new Question();
		question.setSubject(subject);
		question.setContent(content);
		question.setCategory(category);
		question.setAuthor(author);
		question.setCreateDate(LocalDateTime.of(2026, 1, 1, 0, 0).plusSeconds(seconds));
		return questionRepository.save(question);
	}

	@TestConfiguration
	static class PasswordTestConfig {
		@Bean
		@Primary
		TestPasswordEmailSender testPasswordEmailSender() {
			return new TestPasswordEmailSender();
		}
	}

	static class TestPasswordEmailSender implements PasswordEmailSender {
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

		void reset() {
			this.email = null;
			this.temporaryPassword = null;
			this.fail = false;
		}
	}
}
