package com.mysite.sbb.password;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordResetForm {
    @NotEmpty(message = "이메일은 필수항목입니다.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    private String email;
}
