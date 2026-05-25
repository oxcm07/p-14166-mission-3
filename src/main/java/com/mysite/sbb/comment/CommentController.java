package com.mysite.sbb.comment;

import com.mysite.sbb.CommonUtil;
import com.mysite.sbb.answer.Answer;
import com.mysite.sbb.answer.AnswerForm;
import com.mysite.sbb.answer.AnswerService;
import com.mysite.sbb.category.CategoryService;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.Map;
import java.util.stream.Collectors;

@RequestMapping("/comment")
@RequiredArgsConstructor
@Controller
public class CommentController {
    private final QuestionService questionService;
    private final AnswerService answerService;
    private final CommentService commentService;
    private final UserService userService;
    private final CommonUtil commonUtil;

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/create/question/{id}")
    public String createQuestionComment(Model model, @PathVariable("id") Integer id,
                                        @Valid CommentForm commentForm, BindingResult bindingResult,
                                        Principal principal,
                                        @RequestParam(value = "answerPage", defaultValue = "1") int answerPage,
                                        @RequestParam(value = "answerSort", defaultValue = "latest") String answerSort) {
        Question question = this.questionService.getQuestion(id);
        if (bindingResult.hasErrors()) {
            model.addAttribute("answerForm", new AnswerForm());
            addQuestionDetailAttributes(model, question, answerPage, answerSort);
            return "question_detail";
        }
        SiteUser siteUser = this.userService.getUser(principal.getName());
        Comment comment = this.commentService.create(question, commentForm.getContent(), siteUser);
        return String.format("redirect:%s?answerPage=%s&answerSort=%s#comment_%s",
                getQuestionDetailUrl(question), answerPage, answerSort, comment.getId());
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/create/answer/{id}")
    public String createAnswerComment(Model model, @PathVariable("id") Integer id,
                                      @Valid CommentForm commentForm, BindingResult bindingResult,
                                      Principal principal,
                                      @RequestParam(value = "answerPage", defaultValue = "1") int answerPage,
                                      @RequestParam(value = "answerSort", defaultValue = "latest") String answerSort) {
        Answer answer = this.answerService.getAnswer(id);
        Question question = answer.getQuestion();
        if (bindingResult.hasErrors()) {
            model.addAttribute("answerForm", new AnswerForm());
            addQuestionDetailAttributes(model, question, answerPage, answerSort);
            return "question_detail";
        }
        SiteUser siteUser = this.userService.getUser(principal.getName());
        Comment comment = this.commentService.create(answer, commentForm.getContent(), siteUser);
        return String.format("redirect:%s?answerPage=%s&answerSort=%s#comment_%s",
                getQuestionDetailUrl(question), answerPage, answerSort, comment.getId());
    }

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
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/modify/{id}")
    public String commentModify(CommentForm commentForm, @PathVariable("id") Integer id, Principal principal,
                                Model model,
                                @RequestParam(value = "answerPage", defaultValue = "1") int answerPage,
                                @RequestParam(value = "answerSort", defaultValue = "latest") String answerSort) {
        Comment comment = this.commentService.getComment(id);
        if (!comment.getAuthor().getUsername().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "수정권한이 없습니다.");
        }
        commentForm.setContent(comment.getContent());
        model.addAttribute("answerPage", answerPage);
        model.addAttribute("answerSort", answerSort);
        return "comment_form";
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/modify/{id}")
    public String commentModify(@Valid CommentForm commentForm, BindingResult bindingResult,
                                @PathVariable("id") Integer id, Principal principal, Model model,
                                @RequestParam(value = "answerPage", defaultValue = "1") int answerPage,
                                @RequestParam(value = "answerSort", defaultValue = "latest") String answerSort) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("answerPage", answerPage);
            model.addAttribute("answerSort", answerSort);
            return "comment_form";
        }
        Comment comment = this.commentService.getComment(id);
        if (!comment.getAuthor().getUsername().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "수정권한이 없습니다.");
        }
        this.commentService.modify(comment, commentForm.getContent());
        return String.format("redirect:%s?answerPage=%s&answerSort=%s#comment_%s",
                getQuestionDetailUrl(comment), answerPage, answerSort, comment.getId());
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/delete/{id}")
    public String commentDelete(Principal principal, @PathVariable("id") Integer id,
                                @RequestParam(value = "answerPage", defaultValue = "1") int answerPage,
                                @RequestParam(value = "answerSort", defaultValue = "latest") String answerSort) {
        Comment comment = this.commentService.getComment(id);
        if (!comment.getAuthor().getUsername().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "삭제권한이 없습니다.");
        }
        String questionDetailUrl = getQuestionDetailUrl(comment);
        this.commentService.delete(comment);
        return String.format("redirect:%s?answerPage=%s&answerSort=%s",
                questionDetailUrl, answerPage, answerSort);
    }

    private String getQuestionDetailUrl(Comment comment) {
        if (comment.getQuestion() != null) {
            return getQuestionDetailUrl(comment.getQuestion());
        }
        return getQuestionDetailUrl(comment.getAnswer().getQuestion());
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
