package com.mysite.sbb.question;

import com.mysite.sbb.CommonUtil;
import com.mysite.sbb.answer.Answer;
import com.mysite.sbb.answer.AnswerService;
import com.mysite.sbb.answer.AnswerForm;
import com.mysite.sbb.category.Category;
import com.mysite.sbb.category.CategoryService;
import com.mysite.sbb.comment.CommentForm;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.Map;
import java.util.stream.Collectors;

@RequestMapping("/question")
@RequiredArgsConstructor
@Controller
public class QuestionController {
    private final QuestionService questionService;
    private final AnswerService answerService;
    private final CategoryService categoryService;
    private final UserService userService;
    private final CommonUtil commonUtil;

    @GetMapping("/{categoryCode}/list")
    public String list(Model model, @RequestParam(value = "page", defaultValue = "1") int page,
                       @RequestParam(value = "kw", defaultValue = "") String kw,
                       @PathVariable("categoryCode") String categoryCode) {
        Category category = this.categoryService.getCategory(categoryCode);
        Page<Question> paging = this.questionService.getList(toZeroBasedPage(page), kw, category);
        Map<Integer, Long> questionNumberMap = paging.getContent().stream()
                .collect(Collectors.toMap(Question::getId, this.questionService::getCategoryQuestionNumber));
        model.addAttribute("paging", paging);
        model.addAttribute("kw", kw);
        model.addAttribute("categoryList", this.categoryService.getList());
        model.addAttribute("category", category);
        model.addAttribute("questionNumberMap", questionNumberMap);
        return "question/list";
    }

    private int toZeroBasedPage(int page) {
        return Math.max(page, 1) - 1;
    }

    @GetMapping(value = "/{categoryCode}/detail/{questionNumber}")
    public String detail(Model model, @PathVariable("categoryCode") String categoryCode,
                         @PathVariable("questionNumber") int questionNumber,
                         AnswerForm answerForm, CommentForm commentForm,
                         @RequestParam(value = "answerPage", defaultValue = "1") int answerPage,
                         @RequestParam(value = "answerSort", defaultValue = "latest") String answerSort) {
        Category category = this.categoryService.getCategory(categoryCode);
        Question question = this.questionService.getQuestion(category, questionNumber);
        question = this.questionService.increaseViewCount(question);
        addQuestionDetailAttributes(model, question, answerPage, answerSort);
        return "question/detail";
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

    private String getDetailUrl(Question question) {
        String categoryCode = question.getCategory() != null
                ? question.getCategory().getCode()
                : CategoryService.DEFAULT_CATEGORY_CODE;
        long questionNumber = this.questionService.getCategoryQuestionNumber(question);
        return String.format("/question/%s/detail/%s", categoryCode, questionNumber);
    }

    @PreAuthorize("isAuthenticated()") //로그인 필요
    @GetMapping("/create")
    public String questionCreate(QuestionForm questionForm,
                                 @RequestParam(value = "categoryCode", defaultValue = CategoryService.DEFAULT_CATEGORY_CODE)
                                 String categoryCode, Model model) {
        Category category = this.categoryService.getCategoryOrDefault(categoryCode);
        questionForm.setCategoryCode(category.getCode());
        model.addAttribute("categoryList", this.categoryService.getList());
        model.addAttribute("formAction", "/question/create");
        return "question/form";
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/create")
    public String questionCreate(@Valid QuestionForm questionForm,
                                 BindingResult bindingResult, Principal principal, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categoryList", this.categoryService.getList());
            model.addAttribute("formAction", "/question/create");
            return "question/form";
        }
        SiteUser siteUser = this.userService.getUser(principal.getName());
        Category category = this.categoryService.getCategoryOrDefault(questionForm.getCategoryCode());
        this.questionService.create(questionForm.getSubject(), questionForm.getContent(), siteUser, category);
        return String.format("redirect:/question/%s/list", category.getCode()); // 질문 저장 후 질문목록으로 리다이렉트
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/modify/{id}")
    public String questionModify(QuestionForm questionForm, @PathVariable("id") Integer id,
                                 Principal principal, Model model) {
        Question question = this.questionService.getQuestion(id);
        if (!question.getAuthor().getUsername().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "수정권한이 없습니다.");
        }
        questionForm.setSubject(question.getSubject());
        questionForm.setContent(question.getContent());
        if (question.getCategory() != null) {
            questionForm.setCategoryCode(question.getCategory().getCode());
        }
        model.addAttribute("categoryList", this.categoryService.getList());
        model.addAttribute("formAction", String.format("/question/modify/%s", id));
        return "question/form";
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/modify/{id}")
    public String questionModify(@Valid QuestionForm questionForm, BindingResult bindingResult,
                                 Principal principal, @PathVariable("id") Integer id, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("categoryList", this.categoryService.getList());
            model.addAttribute("formAction", String.format("/question/modify/%s", id));
            return "question/form";
        }
        Question question = this.questionService.getQuestion(id);
        if (!question.getAuthor().getUsername().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "수정권한이 없습니다.");
        }
        Category category = this.categoryService.getCategoryOrDefault(questionForm.getCategoryCode());
        this.questionService.modify(question, questionForm.getSubject(), questionForm.getContent(), category);
        return String.format("redirect:%s", getDetailUrl(question));
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/delete/{id}")
    public String questionDelete(Principal principal, @PathVariable("id") Integer id) {
        Question question = this.questionService.getQuestion(id);
        if (!question.getAuthor().getUsername().equals(principal.getName())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "삭제권한이 없습니다.");
        }
        String categoryCode = question.getCategory() != null
                ? question.getCategory().getCode()
                : CategoryService.DEFAULT_CATEGORY_CODE;
        this.questionService.delete(question);
        return String.format("redirect:/question/%s/list", categoryCode);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/vote/{id}")
    public String questionVote(Principal principal, @PathVariable("id") Integer id) {
        Question question = this.questionService.getQuestion(id);
        SiteUser siteUser = this.userService.getUser(principal.getName()); //로그인 ID를 DB와 매핑 가능한 SiteUser로 승격
        this.questionService.vote(question, siteUser);
        return String.format("redirect:%s", getDetailUrl(question));
    }
}
