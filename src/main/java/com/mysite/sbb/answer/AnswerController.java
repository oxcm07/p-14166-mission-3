package com.mysite.sbb.answer;

import com.mysite.sbb.CommonUtil;
import com.mysite.sbb.category.CategoryService;
import com.mysite.sbb.comment.CommentForm;
import com.mysite.sbb.question.Question;
import com.mysite.sbb.question.QuestionService;
import com.mysite.sbb.user.SiteUser;
import com.mysite.sbb.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.Map;
import java.util.stream.Collectors;

@RequestMapping("/answer")
@RequiredArgsConstructor
@Controller
public class AnswerController {
    private final QuestionService questionService;
    private final AnswerService answerService;
    private final UserService userService;
    private final CommonUtil commonUtil;

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/create/{id}")
    public String createAnswer(Model model, @PathVariable("id") Integer id,
                               @Valid AnswerForm answerForm, BindingResult bindingResult, Principal principal,
                               @RequestParam(value = "answerPage", defaultValue = "1") int answerPage,
                               @RequestParam(value = "answerSort", defaultValue = "latest") String answerSort) {
        Question question = this.questionService.getQuestion(id);
        SiteUser siteUser = this.userService.getUser(principal.getName());
        if (bindingResult.hasErrors()) {
            addQuestionDetailAttributes(model, question, answerPage, answerSort);
            return "question_detail";
        }
        Answer answer = this.answerService.create(question, answerForm.getContent(), siteUser);
        // 답변 등록 후 앵커로 스크롤을 이동시키기 위해 #answer_%s 추가
        return String.format("redirect:%s?answerPage=%s&answerSort=%s#answer_%s",
                getQuestionDetailUrl(answer.getQuestion()), answerPage, answerSort, answer.getId());
    }
    // 컨트롤러에서 마크다운을 HTML로 변환해서 모델에 담아 넘기는 방식
    private void addQuestionDetailAttributes(Model model, Question question, int answerPage, String answerSort) {
        Page<Answer> answerPaging = this.answerService.getList(question, toZeroBasedPage(answerPage), answerSort);
        Map<Integer, String> answerContentMap = answerPaging.getContent().stream()
                .collect(Collectors.toMap(Answer::getId, answer -> this.commonUtil.markdown(answer.getContent())));

        model.addAttribute("question", question);
        model.addAttribute("questionContent", this.commonUtil.markdown(question.getContent()));
        model.addAttribute("answerPaging", answerPaging);
        model.addAttribute("answerContentMap", answerContentMap);
        model.addAttribute("answerSort", answerSort);
        model.addAttribute("questionNumber", this.questionService.getCategoryQuestionNumber(question));
        if (!model.containsAttribute("commentForm")) {
            model.addAttribute("commentForm", new CommentForm());
        }
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/modify/{id}")
    public String answerModify(AnswerForm answerForm, @PathVariable("id") Integer id, Principal principal,
                               Model model,
                               @RequestParam(value = "answerPage", defaultValue = "1") int answerPage,
                               @RequestParam(value = "answerSort", defaultValue = "latest") String answerSort) {
        Answer answer = this.answerService.getAnswer(id);
        if (!answer.getAuthor().getUsername().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "수정권한이 없습니다.");
        }
        answerForm.setContent(answer.getContent());
        model.addAttribute("answerPage", answerPage);
        model.addAttribute("answerSort", answerSort);
        return "answer_form";
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/modify/{id}")
    public String answerModify(@Valid AnswerForm answerForm, BindingResult bindingResult,
                               @PathVariable("id") Integer id, Principal principal, Model model,
                               @RequestParam(value = "answerPage", defaultValue = "1") int answerPage,
                               @RequestParam(value = "answerSort", defaultValue = "latest") String answerSort) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("answerPage", answerPage);
            model.addAttribute("answerSort", answerSort);
            return "answer_form";
        }
        Answer answer = this.answerService.getAnswer(id);
        if (!answer.getAuthor().getUsername().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "수정권한이 없습니다.");
        }
        this.answerService.modify(answer, answerForm.getContent());
        return String.format("redirect:%s?answerPage=%s&answerSort=%s#answer_%s",
                getQuestionDetailUrl(answer.getQuestion()), answerPage, answerSort, answer.getId());
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/delete/{id}")
    public String answerDelete(Principal principal, @PathVariable("id") Integer id,
                               @RequestParam(value = "answerPage", defaultValue = "1") int answerPage,
                               @RequestParam(value = "answerSort", defaultValue = "latest") String answerSort){
        Answer answer = this.answerService.getAnswer(id);
        if(!answer.getAuthor().getUsername().equals(principal.getName())){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "삭제권한이 없습니다.");
        }
        this.answerService.delete(answer);
        return String.format("redirect:%s?answerPage=%s&answerSort=%s",
                getQuestionDetailUrl(answer.getQuestion()), answerPage, answerSort);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/vote/{id}")
    public String answerVote(Principal principal, @PathVariable("id") Integer id,
                             @RequestParam(value = "answerPage", defaultValue = "1") int answerPage,
                             @RequestParam(value = "answerSort", defaultValue = "latest") String answerSort){
        Answer answer = this.answerService.getAnswer(id);
        SiteUser siteUser = this.userService.getUser(principal.getName());
        this.answerService.vote(answer, siteUser);
        return String.format("redirect:%s?answerPage=%s&answerSort=%s#answer_%s",
                getQuestionDetailUrl(answer.getQuestion()), answerPage, answerSort, answer.getId());
    }

    private String getQuestionDetailUrl(Question question) {
        String categoryCode = question.getCategory() != null
                ? question.getCategory().getCode()
                : CategoryService.DEFAULT_CATEGORY_CODE;
        long questionNumber = this.questionService.getCategoryQuestionNumber(question);
        return String.format("/question/%s/detail/%s", categoryCode, questionNumber);
    }

    private int toZeroBasedPage(int page) {
        return Math.max(page, 1) - 1;
    }
}
