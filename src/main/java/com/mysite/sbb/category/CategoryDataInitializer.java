package com.mysite.sbb.category;

import com.mysite.sbb.question.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(0)
@RequiredArgsConstructor
public class CategoryDataInitializer implements ApplicationRunner {
    private final CategoryService categoryService;
    private final QuestionRepository questionRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        this.categoryService.initializeDefaultCategories();
        Category defaultCategory = this.categoryService.getDefaultCategory();
        this.questionRepository.updateNullCategory(defaultCategory);
    }
}
