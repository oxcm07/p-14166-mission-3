package com.mysite.sbb.user;

import com.mysite.sbb.answer.Answer;
import com.mysite.sbb.answer.AnswerService;
import com.mysite.sbb.category.CategoryService;
import com.mysite.sbb.comment.Comment;
import com.mysite.sbb.comment.CommentService;
import com.mysite.sbb.question.Question;
import com.mysite.sbb.question.QuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Controller
@RequestMapping("/user")
public class UserController {
    private final UserService userService;
    private final QuestionService questionService;
    private final AnswerService answerService;
    private final CommentService commentService;

    @GetMapping("/signup")
    public String signup(UserCreateForm userCreateForm) {
        return "user/signup_form";
    }

    @PostMapping("/signup")
    public String signup(@Valid UserCreateForm userCreateForm, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "user/signup_form";
        }

        if (!userCreateForm.getPassword1().equals(userCreateForm.getPassword2())) {
            bindingResult.rejectValue("password2", "passwordInCorrect",
                    "2개의 패스워드가 일치하지 않습니다.");
            return "user/signup_form";
        }

        try {
            userService.create(userCreateForm.getUsername(),
                    userCreateForm.getEmail(), userCreateForm.getPassword1());
        } catch (DataIntegrityViolationException e) {
            bindingResult.reject("signupFailed", "이미 등록된 사용자입니다.");
            return "user/signup_form";
        } catch (Exception e) {
            bindingResult.reject("signupFailed", e.getMessage());
            return "user/signup_form";
        }

        return "redirect:/";
    }

    @GetMapping("/login")
    public String login() {
        return "user/login_form";
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/profile")
    public String profile(Model model, Principal principal,
                          @RequestParam(value = "questionPage", defaultValue = "1") int questionPage,
                          @RequestParam(value = "answerPage", defaultValue = "1") int answerPage,
                          @RequestParam(value = "commentPage", defaultValue = "1") int commentPage) {
        SiteUser siteUser = this.userService.getUser(principal.getName());
        Page<Question> questionPaging = this.questionService.getListByAuthor(siteUser, toZeroBasedPage(questionPage));
        Page<Answer> answerPaging = this.answerService.getListByAuthor(siteUser, toZeroBasedPage(answerPage));
        Page<Comment> commentPaging = this.commentService.getListByAuthor(siteUser, toZeroBasedPage(commentPage));

        Map<Integer, Long> questionNumberMap = new HashMap<>();
        Map<Integer, String> questionCategoryCodeMap = new HashMap<>();
        Map<Integer, Question> commentTargetQuestionMap = new HashMap<>();

        questionPaging.getContent()
                .forEach(question -> addQuestionMeta(question, questionNumberMap, questionCategoryCodeMap));
        answerPaging.getContent()
                .forEach(answer -> addQuestionMeta(answer.getQuestion(), questionNumberMap, questionCategoryCodeMap));
        commentPaging.getContent().forEach(comment -> {
            Question targetQuestion = getCommentTargetQuestion(comment);
            if (targetQuestion != null) {
                commentTargetQuestionMap.put(comment.getId(), targetQuestion);
                addQuestionMeta(targetQuestion, questionNumberMap, questionCategoryCodeMap);
            }
        });

        model.addAttribute("siteUser", siteUser);
        model.addAttribute("questionPaging", questionPaging);
        model.addAttribute("answerPaging", answerPaging);
        model.addAttribute("commentPaging", commentPaging);
        model.addAttribute("questionNumberMap", questionNumberMap);
        model.addAttribute("questionCategoryCodeMap", questionCategoryCodeMap);
        model.addAttribute("commentTargetQuestionMap", commentTargetQuestionMap);
        return "user/profile";
    }

    private int toZeroBasedPage(int page) {
        return Math.max(page, 1) - 1;
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

}
