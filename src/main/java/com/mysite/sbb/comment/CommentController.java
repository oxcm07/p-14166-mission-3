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
import org.springframework.web.util.UriComponentsBuilder;

import java.security.Principal;
import java.util.HashMap;
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

    @GetMapping("/list")
    public String list(Model model, @RequestParam(value = "page", defaultValue = "1") int page) {
        Page<Comment> paging = this.commentService.getRecentList(toZeroBasedPage(page));
        Map<Integer, Long> questionNumberMap = new HashMap<>();
        Map<Integer, String> questionCategoryCodeMap = new HashMap<>();
        Map<Integer, Question> commentTargetQuestionMap = new HashMap<>();
        paging.getContent().forEach(comment -> {
            Question targetQuestion = getCommentTargetQuestion(comment);
            if (targetQuestion != null) {
                commentTargetQuestionMap.put(comment.getId(), targetQuestion);
                addQuestionMeta(targetQuestion, questionNumberMap, questionCategoryCodeMap);
            }
        });

        model.addAttribute("paging", paging);
        model.addAttribute("questionNumberMap", questionNumberMap);
        model.addAttribute("questionCategoryCodeMap", questionCategoryCodeMap);
        model.addAttribute("commentTargetQuestionMap", commentTargetQuestionMap);
        return "comment/list";
    }

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
            return "question/detail";
        }
        SiteUser siteUser = this.userService.getUser(principal.getName());
        Comment comment = this.commentService.create(question, commentForm.getContent(), siteUser);
        return redirectToQuestionDetail(question, answerPage, answerSort, "comment_" + comment.getId());
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
        if (question == null) {
            if (!bindingResult.hasErrors()) {
                SiteUser siteUser = this.userService.getUser(principal.getName());
                this.commentService.create(answer, commentForm.getContent(), siteUser);
            }
            return "redirect:/answer/list";
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("answerForm", new AnswerForm());
            addQuestionDetailAttributes(model, question, answerPage, answerSort);
            return "question/detail";
        }
        SiteUser siteUser = this.userService.getUser(principal.getName());
        Comment comment = this.commentService.create(answer, commentForm.getContent(), siteUser);
        return redirectToQuestionDetail(question, answerPage, answerSort, "comment_" + comment.getId());
    }

    private void addQuestionDetailAttributes(Model model, Question question, int answerPage, String answerSort) {
        answerSort = AnswerService.normalizeSort(answerSort);
        Page<Answer> answerPaging = this.answerService.getList(question, toZeroBasedPage(answerPage), answerSort);
        Map<Integer, String> answerContentMap = answerPaging.getContent().stream()
                .collect(Collectors.toMap(Answer::getId, answer -> this.commonUtil.markdown(answer.getContent())));

        model.addAttribute("question", question);
        model.addAttribute("questionContent", this.commonUtil.markdown(question.getContent()));
        model.addAttribute("answerPaging", answerPaging);
        model.addAttribute("answerContentMap", answerContentMap);
        model.addAttribute("answerSort", answerSort);
        model.addAttribute("questionNumber", this.questionService.getCategoryQuestionNumber(question));
        model.addAttribute("questionCategoryCode", getCategoryCode(question));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/modify/{id}")
    public String commentModify(CommentForm commentForm, @PathVariable("id") Integer id, Principal principal,
                                Model model,
                                @RequestParam(value = "answerPage", defaultValue = "1") int answerPage,
                                @RequestParam(value = "answerSort", defaultValue = "latest") String answerSort) {
        Comment comment = this.commentService.getComment(id);
        if (!isOwner(comment.getAuthor(), principal)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "수정권한이 없습니다.");
        }
        commentForm.setContent(comment.getContent());
        model.addAttribute("answerPage", toOneBasedPage(answerPage));
        model.addAttribute("answerSort", AnswerService.normalizeSort(answerSort));
        return "comment/form";
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/modify/{id}")
    public String commentModify(@Valid CommentForm commentForm, BindingResult bindingResult,
                                @PathVariable("id") Integer id, Principal principal, Model model,
                                @RequestParam(value = "answerPage", defaultValue = "1") int answerPage,
                                @RequestParam(value = "answerSort", defaultValue = "latest") String answerSort) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("answerPage", toOneBasedPage(answerPage));
            model.addAttribute("answerSort", AnswerService.normalizeSort(answerSort));
            return "comment/form";
        }
        Comment comment = this.commentService.getComment(id);
        if (!isOwner(comment.getAuthor(), principal)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "수정권한이 없습니다.");
        }
        Question targetQuestion = getCommentTargetQuestion(comment);
        this.commentService.modify(comment, commentForm.getContent());
        if (targetQuestion == null) {
            return "redirect:/comment/list";
        }
        return redirectToQuestionDetail(targetQuestion, answerPage, answerSort, "comment_" + comment.getId());
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/delete/{id}")
    public String commentDelete(Principal principal, @PathVariable("id") Integer id,
                                @RequestParam(value = "answerPage", defaultValue = "1") int answerPage,
                                @RequestParam(value = "answerSort", defaultValue = "latest") String answerSort) {
        Comment comment = this.commentService.getComment(id);
        if (!isOwner(comment.getAuthor(), principal)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "삭제권한이 없습니다.");
        }
        Question targetQuestion = getCommentTargetQuestion(comment);
        this.commentService.delete(comment);
        if (targetQuestion == null) {
            return "redirect:/comment/list";
        }
        return redirectToQuestionDetail(targetQuestion, answerPage, answerSort, null);
    }

    private String getQuestionDetailUrl(Question question) {
        String categoryCode = question.getCategory() != null
                ? question.getCategory().getCode()
                : CategoryService.DEFAULT_CATEGORY_CODE;
        long questionNumber = this.questionService.getCategoryQuestionNumber(question);
        return String.format("/question/%s/detail/%s", categoryCode, questionNumber);
    }

    private void addQuestionMeta(Question question, Map<Integer, Long> questionNumberMap,
                                 Map<Integer, String> questionCategoryCodeMap) {
        if (question == null) {
            return;
        }
        questionNumberMap.putIfAbsent(question.getId(), this.questionService.getCategoryQuestionNumber(question));
        questionCategoryCodeMap.putIfAbsent(question.getId(), getCategoryCode(question));
    }

    private String getCategoryCode(Question question) {
        if (question.getCategory() == null) {
            return CategoryService.DEFAULT_CATEGORY_CODE;
        }
        return question.getCategory().getCode();
    }

    private Question getCommentTargetQuestion(Comment comment) {
        if (comment.getQuestion() != null) {
            return comment.getQuestion();
        }
        Answer answer = comment.getAnswer();
        if (answer != null) {
            return answer.getQuestion();
        }
        return null;
    }

    private int toZeroBasedPage(int page) {
        return Math.max(page, 1) - 1;
    }

    private int toOneBasedPage(int page) {
        return Math.max(page, 1);
    }

    private String redirectToQuestionDetail(Question question, int answerPage, String answerSort, String fragment) {
        return redirectToQuestionDetail(getQuestionDetailUrl(question), answerPage, answerSort, fragment);
    }

    private String redirectToQuestionDetail(String questionDetailUrl, int answerPage, String answerSort, String fragment) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath(questionDetailUrl)
                .queryParam("answerPage", toOneBasedPage(answerPage))
                .queryParam("answerSort", AnswerService.normalizeSort(answerSort));
        if (fragment != null) {
            builder.fragment(fragment);
        }
        return "redirect:" + builder.build().encode().toUriString();
    }

    private boolean isOwner(SiteUser author, Principal principal) {
        return author != null && principal != null && author.getUsername().equals(principal.getName());
    }
}
