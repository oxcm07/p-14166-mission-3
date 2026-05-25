package com.mysite.sbb.question;

import com.mysite.sbb.category.Category;
import com.mysite.sbb.user.SiteUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Integer> {
    Page<Question> findAll(Specification<Question> spec, Pageable pageable);
    Page<Question> findAllByCategory(Category category, Pageable pageable);
    List<Question> findAllByAuthorOrderByCreateDateDesc(SiteUser author);
    long countByCategory(Category category);

    @Query("""
            select count(q) from Question q
            where q.category = :category
            and (q.createDate < :createDate or (q.createDate = :createDate and q.id <= :id))
            """)
    long countCategoryQuestionNumber(@Param("category") Category category,
                                     @Param("createDate") LocalDateTime createDate,
                                     @Param("id") Integer id);

    @Modifying
    @Query("update Question q set q.category = :category where q.category is null")
    void updateNullCategory(@Param("category") Category category);
}
