package com.example.calorieserver.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordRequest {

    @NotBlank(message = "请输入邮箱")
    @Email(message = "邮箱格式不正确")
    private String email;
}
