package com.mysite.sbb.category;

import com.mysite.sbb.DataNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class CategoryService {
    public static final String DEFAULT_CATEGORY_CODE = "qna";
    private static final Map<String, String> DEFAULT_CATEGORIES = new LinkedHashMap<>();

    static {
        DEFAULT_CATEGORIES.put(DEFAULT_CATEGORY_CODE, "질문답변");
        DEFAULT_CATEGORIES.put("lecture", "강좌");
        DEFAULT_CATEGORIES.put("free", "자유게시판");
    }

    private final CategoryRepository categoryRepository;

    public List<Category> getList() {
        initializeDefaultCategories();
        return this.categoryRepository.findAllByOrderByIdAsc();
    }

    public Category getCategory(String code) {
        initializeDefaultCategories();
        return this.categoryRepository.findByCode(code)
                .orElseThrow(() -> new DataNotFoundException("category not found"));
    }

    public Category getCategoryOrDefault(String code) {
        initializeDefaultCategories();
        if (code == null || code.isBlank()) {
            return getDefaultCategory();
        }
        return this.categoryRepository.findByCode(code).orElseGet(this::getDefaultCategory);
    }

    public Category getDefaultCategory() {
        return getCategory(DEFAULT_CATEGORY_CODE);
    }

    public void initializeDefaultCategories() {
        DEFAULT_CATEGORIES.forEach(this::createIfNotExists);
    }

    public Category createIfNotExists(String code, String name) {
        return this.categoryRepository.findByCode(code)
                .orElseGet(() -> {
                    Category category = new Category();
                    category.setCode(code);
                    category.setName(name);
                    return this.categoryRepository.save(category);
                });
    }
}
