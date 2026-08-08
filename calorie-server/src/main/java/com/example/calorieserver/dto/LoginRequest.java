package com.example.calorieserver.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    @NotBlank(message = "请输入邮箱或用户名")
    private String email;

    @NotBlank(message = "请输入密码")
    private String password;
}