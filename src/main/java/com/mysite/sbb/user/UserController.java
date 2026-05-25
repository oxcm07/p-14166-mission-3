package com.mysite.sbb.user;

import com.mysite.sbb.DataNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

@RequiredArgsConstructor
@Controller
@RequestMapping("/user")
public class UserController {
    private final UserService userService;

    @GetMapping("/signup")
    public String signup(UserCreateForm userCreateForm) {
        return "signup_form";
    }

    @PostMapping("/signup")
    public String signup(@Valid UserCreateForm userCreateForm, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "signup_form";
        }

        if (!userCreateForm.getPassword1().equals(userCreateForm.getPassword2())) {
            bindingResult.rejectValue("password2", "passwordInCorrect",
                    "2개의 패스워드가 일치하지 않습니다.");
            return "signup_form";
        }

        try {
            userService.create(userCreateForm.getUsername(),
                    userCreateForm.getEmail(), userCreateForm.getPassword1());
        } catch (DataIntegrityViolationException e) {
            bindingResult.reject("signupFailed", "이미 등록된 사용자입니다.");
            return "signup_form";
        } catch (Exception e) {
            bindingResult.reject("signupFailed", e.getMessage());
            return "signup_form";
        }

        return "redirect:/";
    }

    @GetMapping("/login")
    public String login() {
        return "login_form";
    }

    @GetMapping("/password/reset")
    public String passwordReset(PasswordResetForm passwordResetForm) {
        return "password_reset_form";
    }

    @PostMapping("/password/reset")
    public String passwordReset(@Valid PasswordResetForm passwordResetForm, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "password_reset_form";
        }

        try {
            this.userService.resetPassword(passwordResetForm.getEmail());
        } catch (DataNotFoundException e) {
            bindingResult.reject("passwordResetFailed", "등록된 이메일을 찾을 수 없습니다.");
            return "password_reset_form";
        } catch (Exception e) {
            bindingResult.reject("passwordResetFailed", "임시 비밀번호 메일 발송에 실패했습니다.");
            return "password_reset_form";
        }

        return "redirect:/user/login?reset";
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/password/change")
    public String passwordChange(PasswordChangeForm passwordChangeForm) {
        return "password_change_form";
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/password/change")
    public String passwordChange(@Valid PasswordChangeForm passwordChangeForm,
                                 BindingResult bindingResult,
                                 Principal principal) {
        if (bindingResult.hasErrors()) {
            return "password_change_form";
        }

        if (!passwordChangeForm.getNewPassword1().equals(passwordChangeForm.getNewPassword2())) {
            bindingResult.rejectValue("newPassword2", "passwordInCorrect",
                    "2개의 새 비밀번호가 일치하지 않습니다.");
            return "password_change_form";
        }

        boolean changed = this.userService.changePassword(principal.getName(),
                passwordChangeForm.getCurrentPassword(), passwordChangeForm.getNewPassword1());
        if (!changed) {
            bindingResult.rejectValue("currentPassword", "passwordMismatch",
                    "기존 비밀번호가 일치하지 않습니다.");
            return "password_change_form";
        }

        return "redirect:/user/password/change?success";
    }
}
