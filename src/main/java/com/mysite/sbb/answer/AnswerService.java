package com.mysite.sbb.answer;

import com.mysite.sbb.DataNotFoundException;
import com.mysite.sbb.question.Question;
import com.mysite.sbb.user.SiteUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class AnswerService {
    private static final String DEFAULT_SORT = "latest";
    private static final Set<String> ALLOWED_SORTS = Set.of(DEFAULT_SORT, "old", "recommend");

    private final AnswerRepository answerRepository;

    public Page<Answer> getList(Question question, int page, String sort) {
        sort = normalizeSort(sort);
        if ("recommend".equals(sort)) {
            Pageable pageable = PageRequest.of(page, 5);
            return this.answerRepository.findAllByQuestionOrderByVoterCount(question, pageable);
        }

        List<Sort.Order> sorts = new ArrayList<>();
        if ("old".equals(sort)) {
            sorts.add(Sort.Order.asc("createDate"));
        } else {
            sorts.add(Sort.Order.desc("createDate"));
        }
        Pageable pageable = PageRequest.of(page, 5, Sort.by(sorts));
        return this.answerRepository.findAllByQuestion(question, pageable);
    }

    public Page<Answer> getListByAuthor(SiteUser author, int page) {
        Pageable pageable = PageRequest.of(page, 5);
        return this.answerRepository.findAllByAuthorOrderByCreateDateDesc(author, pageable);
    }

    public Page<Answer> getRecentList(int page) {
        List<Sort.Order> sorts = new ArrayList<>();
        sorts.add(Sort.Order.desc("createDate"));
        sorts.add(Sort.Order.desc("id"));
        Pageable pageable = PageRequest.of(page, 10, Sort.by(sorts));
        return this.answerRepository.findAll(pageable);
    }

    public Answer create(Question question, String content, SiteUser author) {
        Answer answer = new Answer();
        answer.setContent(content);
        answer.setCreateDate(LocalDateTime.now());
        answer.setQuestion(question);
        answer.setAuthor(author);
        this.answerRepository.save(answer);
        return answer;
    }

    public Answer getAnswer(Integer id){
        Optional<Answer> answer = this.answerRepository.findById(id);
        if(answer.isPresent()){
            return answer.get();
        }else{
            throw new DataNotFoundException("answer not found");
        }
    }

    public void modify(Answer answer, String content){
        answer.setContent(content);
        answer.setModifyDate(LocalDateTime.now());
        this.answerRepository.save(answer);
    }

    public void delete(Answer answer){
        this.answerRepository.delete(answer);
    }

    public void vote(Answer answer, SiteUser siteUser){
        answer.getVoter().add(siteUser);
        this.answerRepository.save(answer);
    }

    public static String normalizeSort(String sort) {
        if (sort == null || !ALLOWED_SORTS.contains(sort)) {
            return DEFAULT_SORT;
        }
        return sort;
    }
}
