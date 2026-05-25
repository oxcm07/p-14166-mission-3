package com.mysite.sbb.question;

import com.mysite.sbb.DataNotFoundException;
import com.mysite.sbb.answer.Answer;
import com.mysite.sbb.category.Category;
import com.mysite.sbb.user.SiteUser;
import jakarta.persistence.criteria.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class QuestionService {
    private final QuestionRepository questionRepository;

    private Specification<Question> search(String kw) {
        return new Specification<>() {
            private static final long serialVersionUID = 1L;
            @Override
            public Predicate toPredicate(Root<Question> q, CriteriaQuery<?> query, CriteriaBuilder cb) {
                query.distinct(true);  // 중복을 제거
                Join<Question, SiteUser> u1 = q.join("author", JoinType.LEFT);
                Join<Question, Answer> a = q.join("answerList", JoinType.LEFT);
                Join<Answer, SiteUser> u2 = a.join("author", JoinType.LEFT);
                return cb.or(cb.like(q.get("subject"), "%" + kw + "%"), // 제목
                        cb.like(q.get("content"), "%" + kw + "%"),      // 내용
                        cb.like(u1.get("username"), "%" + kw + "%"),    // 질문 작성자
                        cb.like(a.get("content"), "%" + kw + "%"),      // 답변 내용
                        cb.like(u2.get("username"), "%" + kw + "%"));   // 답변 작성자
            }
        };
    }

    public Page<Question> getList(int page, String kw, Category category) {
        List<Sort.Order> sorts = new ArrayList<>();
        sorts.add(Sort.Order.desc("createDate"));
        sorts.add(Sort.Order.desc("id"));
        Pageable pageable = PageRequest.of(page, 10, Sort.by(sorts));
        Specification<Question> spec = search(kw);
        if (category != null) {
            spec = spec.and((q, query, cb) -> cb.equal(q.get("category"), category));
        }
        return this.questionRepository.findAll(spec, pageable);
    }

    public Page<Question> getListByAuthor(SiteUser author, int page) {
        Pageable pageable = PageRequest.of(page, 5);
        return this.questionRepository.findAllByAuthorOrderByCreateDateDesc(author, pageable);
    }

    public Question getQuestion(Integer id) {
        Optional<Question> question = this.questionRepository.findById(id);
        if (question.isPresent()) {
            return question.get();
        } else {
            throw new DataNotFoundException("question not found");
        }
    }

    public Question getQuestion(Category category, int questionNumber) {
        if (questionNumber < 1) {
            throw new DataNotFoundException("question not found");
        }
        long totalCount = this.questionRepository.countByCategory(category);
        if (questionNumber > totalCount) {
            throw new DataNotFoundException("question not found");
        }
        Pageable pageable = PageRequest.of(questionNumber - 1, 1,
                Sort.by(Sort.Order.asc("createDate"), Sort.Order.asc("id")));
        Page<Question> paging = this.questionRepository.findAllByCategory(category, pageable);
        if (paging.hasContent()) {
            return paging.getContent().get(0);
        }
        throw new DataNotFoundException("question not found");
    }

    public long getCategoryQuestionNumber(Question question) {
        Category category = question.getCategory();
        if (category == null) {
            return question.getId();
        }
        return this.questionRepository.countCategoryQuestionNumber(category, question.getCreateDate(), question.getId());
    }

    public void create(String subject, String content, SiteUser user, Category category) {
        Question q = new Question();
        q.setSubject(subject);
        q.setContent(content);
        q.setCreateDate(LocalDateTime.now());
        q.setAuthor(user);
        q.setCategory(category);
        this.questionRepository.save(q);
    }

    public void modify(Question question, String subject, String content, Category category) {
        question.setSubject(subject);
        question.setContent(content);
        question.setCategory(category);
        question.setModifyDate(LocalDateTime.now());
        this.questionRepository.save(question);
    }

    public void delete(Question question) {
        this.questionRepository.delete(question);
    }

    public void vote(Question question, SiteUser siteUser) {
        question.getVoter().add(siteUser);
        this.questionRepository.save(question);
    }
}
