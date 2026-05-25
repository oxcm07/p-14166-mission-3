package com.mysite.sbb.answer;

import com.mysite.sbb.question.Question;
import com.mysite.sbb.user.SiteUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnswerRepository extends JpaRepository<Answer, Integer> {
    Page<Answer> findAllByQuestion(Question question, Pageable pageable);
    Page<Answer> findAllByAuthorOrderByCreateDateDesc(SiteUser author, Pageable pageable);

    @Query(
            value = "select a from Answer a where a.question = :question order by size(a.voter) desc, a.createDate desc",
            countQuery = "select count(a) from Answer a where a.question = :question"
    )
    Page<Answer> findAllByQuestionOrderByVoterCount(@Param("question") Question question, Pageable pageable);
}
