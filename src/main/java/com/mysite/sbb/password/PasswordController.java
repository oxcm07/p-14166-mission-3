package com.mysite.sbb.password;

import com.mysite.sbb.DataNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

@RequiredArgsConstructor
@Controller
@RequestMapping("/user/password")
public class PasswordController {
    private final PasswordService passwordService;

    @GetMapping("/reset")
    public String passwordReset(PasswordResetForm passwordResetForm) {
        return "password/reset_form";
    }

    @PostMapping("/reset")
    public String passwordReset(@Valid PasswordResetForm passwordResetForm, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "password/reset_form";
        }

        try {
            this.passwordService.resetPassword(passwordResetForm.getEmail());
        } catch (DataNotFoundException e) {
            bindingResult.reject("passwordResetFailed", "등록된 이메일을 찾을 수 없습니다.");
            return "password/reset_form";
        } catch (Exception e) {
            bindingResult.reject("passwordResetFailed", "임시 비밀번호 메일 발송에 실패했습니다.");
            return "password/reset_form";
        }

        return "redirect:/user/login?reset";
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/change")
    public String passwordChange(PasswordChangeForm passwordChangeForm) {
        return "password/change_form";
    }

    @PreAuthorize("isAuthenticated()")
    @PostMapping("/change")
    public String passwordChange(@Valid PasswordChangeForm passwordChangeForm,
                                 BindingResult bindingResult,
                                 Principal principal) {
        if (bindingResult.hasErrors()) {
            return "password/change_form";
        }

        if (!passwordChangeForm.getNewPassword1().equals(passwordChangeForm.getNewPassword2())) {
            bindingResult.rejectValue("newPassword2", "passwordInCorrect",
                    "2개의 새 비밀번호가 일치하지 않습니다.");
            return "password/change_form";
        }

        boolean changed = this.passwordService.changePassword(principal.getName(),
                passwordChangeForm.getCurrentPassword(), passwordChangeForm.getNewPassword1());
        if (!changed) {
            bindingResult.rejectValue("currentPassword", "passwordMismatch",
                    "기존 비밀번호가 일치하지 않습니다.");
            return "password/change_form";
        }

        return "redirect:/user/password/change?success";
    }
}
