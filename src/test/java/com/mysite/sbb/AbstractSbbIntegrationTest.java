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
import com.mysite.sbb.user.SiteUser;
import com.mysite.sbb.user.UserRepository;
import com.mysite.sbb.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@ActiveProfiles("test")
@AutoConfigureMockMvc
@SpringBootTest
@Transactional
@Import(AbstractSbbIntegrationTest.PasswordTestConfig.class)
public abstract class AbstractSbbIntegrationTest {
	protected static final String TEST_USERNAME = "testuser";

	@Autowired
	protected MockMvc mockMvc;

	@Autowired
	protected QuestionRepository questionRepository;

	@Autowired
	protected AnswerRepository answerRepository;

	@Autowired
	protected CommentRepository commentRepository;

	@Autowired
	protected CategoryService categoryService;

	@Autowired
	protected UserService userService;

	@Autowired
	protected UserRepository userRepository;

	@Autowired
	protected PasswordEncoder passwordEncoder;

	@Autowired
	protected TestPasswordEmailSender passwordEmailSender;

	@Autowired
	protected AnswerService answerService;

	@Autowired
	protected CommentService commentService;

	@BeforeEach
	protected void resetPasswordEmailSender() {
		passwordEmailSender.reset();
	}

	protected SiteUser createUser() {
		return userService.create(TEST_USERNAME, "testuser@example.com", "1234");
	}

	protected Category category(String code) {
		return categoryService.getCategory(code);
	}

	protected Question createQuestion(String subject, String content, Category category, SiteUser author, int seconds) {
		Question question = new Question();
		question.setSubject(subject);
		question.setContent(content);
		question.setCategory(category);
		question.setAuthor(author);
		question.setCreateDate(LocalDateTime.of(2026, 1, 1, 0, 0).plusSeconds(seconds));
		return questionRepository.save(question);
	}

	protected Answer createAnswer(Question question, String content, SiteUser author, int seconds) {
		Answer answer = new Answer();
		answer.setQuestion(question);
		answer.setContent(content);
		answer.setAuthor(author);
		answer.setCreateDate(LocalDateTime.of(2026, 1, 1, 0, 0).plusSeconds(seconds));
		return answerRepository.save(answer);
	}

	protected Comment createComment(Question question, String content, SiteUser author, int seconds) {
		Comment comment = new Comment();
		comment.setQuestion(question);
		comment.setContent(content);
		comment.setAuthor(author);
		comment.setCreateDate(LocalDateTime.of(2026, 1, 1, 0, 0).plusSeconds(seconds));
		return commentRepository.save(comment);
	}

	protected Comment createComment(Answer answer, String content, SiteUser author, int seconds) {
		Comment comment = new Comment();
		comment.setAnswer(answer);
		comment.setContent(content);
		comment.setAuthor(author);
		comment.setCreateDate(LocalDateTime.of(2026, 1, 1, 0, 0).plusSeconds(seconds));
		return commentRepository.save(comment);
	}

	@TestConfiguration
	static class PasswordTestConfig {
		@Bean
		@Primary
		TestPasswordEmailSender testPasswordEmailSender() {
			return new TestPasswordEmailSender();
		}
	}
}
