package com.mysite.sbb.category;

import com.mysite.sbb.question.QuestionRepository;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CategoryDataInitializerTest {

	@Test
	void initializesDefaultCategoriesAndBackfillsNullQuestionCategories() {
		CategoryService categoryService = mock(CategoryService.class);
		QuestionRepository questionRepository = mock(QuestionRepository.class);
		Category defaultCategory = new Category();
		when(categoryService.getDefaultCategory()).thenReturn(defaultCategory);

		CategoryDataInitializer initializer = new CategoryDataInitializer(categoryService, questionRepository);

		initializer.run(null);

		verify(categoryService).initializeDefaultCategories();
		verify(categoryService).getDefaultCategory();
		verify(questionRepository).updateNullCategory(defaultCategory);
	}
}
