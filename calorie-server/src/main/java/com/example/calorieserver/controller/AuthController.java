package com.example.calorieserver.controller;

import com.example.calorieserver.dto.ChangePasswordRequest;
import com.example.calorieserver.dto.ForgotPasswordRequest;
import com.example.calorieserver.dto.LoginRequest;
import com.example.calorieserver.dto.RegisterRequest;
import com.example.calorieserver.dto.ResetPasswordRequest;
import com.example.calorieserver.dto.UpdateBodyInfoRequest;
import com.example.calorieserver.dto.UserResponse;
import com.example.calorieserver.service.PasswordResetService;
import com.example.calorieserver.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final PasswordResetService passwordResetService;

    // 注册
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(userService.register(request));
    }

    // 登录
    @PostMapping("/login")
    public ResponseEntity<UserResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(userService.login(request));
    }

    // 忘记密码：发送重置邮件（防枚举，统一成功提示）
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.getEmail());
        return ResponseEntity.ok().build();
    }

    // 重置密码：校验 token 后设置新密码
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }

    // 修改密码：已登录用户改自己的密码（userId 从 token 取）
    @PutMapping("/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request.getOldPassword(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }

    // 14 步信息收集一次性提交
    @PutMapping("/onboarding/{userId}")
    public ResponseEntity<UserResponse> submitOnboarding(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateBodyInfoRequest request) {
        return ResponseEntity.ok(userService.submitOnboarding(userId, request));
    }
}