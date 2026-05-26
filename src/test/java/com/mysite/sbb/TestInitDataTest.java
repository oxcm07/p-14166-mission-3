package com.mysite.sbb;

import com.mysite.sbb.category.Category;
import com.mysite.sbb.category.CategoryService;
import com.mysite.sbb.question.QuestionRepository;
import com.mysite.sbb.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TestInitDataTest {

	@Test
	void disabledInitDataDoesNotCreateQuestionsOrUser() {
		QuestionRepository questionRepository = mock(QuestionRepository.class);
		CategoryService categoryService = mock(CategoryService.class);
		UserRepository userRepository = mock(UserRepository.class);
		PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

		TestInitData testInitData = new TestInitData(
				questionRepository,
				categoryService,
				userRepository,
				passwordEncoder,
				false);

		testInitData.work1();

		verify(categoryService, never()).getDefaultCategory();
		verify(questionRepository, never()).count();
		verify(questionRepository, never()).save(org.mockito.ArgumentMatchers.any());
		verify(userRepository, never()).findByUsername(org.mockito.ArgumentMatchers.anyString());
	}

	@Test
	void enabledInitDataCreatesQuestionsWhenRepositoryIsEmpty() {
		QuestionRepository questionRepository = mock(QuestionRepository.class);
		CategoryService categoryService = mock(CategoryService.class);
		UserRepository userRepository = mock(UserRepository.class);
		PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
		Category defaultCategory = new Category();
		when(categoryService.getDefaultCategory()).thenReturn(defaultCategory);
		when(questionRepository.count()).thenReturn(0L);
		when(userRepository.findByUsername("testuser")).thenReturn(java.util.Optional.empty());
		when(passwordEncoder.encode("1234")).thenReturn("encoded");
		when(userRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(questionRepository.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> invocation.getArgument(0));

		TestInitData testInitData = new TestInitData(
				questionRepository,
				categoryService,
				userRepository,
				passwordEncoder,
				true);

		testInitData.work1();

		verify(questionRepository).count();
		verify(userRepository).findByUsername("testuser");
		verify(questionRepository, org.mockito.Mockito.times(153)).save(org.mockito.ArgumentMatchers.any());
	}
}
