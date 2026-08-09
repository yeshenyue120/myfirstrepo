package com.example.calorieserver.controller;

import com.example.calorieserver.config.GlobalExceptionHandler;
import com.example.calorieserver.dto.UpdateBodyInfoRequest;
import com.example.calorieserver.dto.UserResponse;
import com.example.calorieserver.entity.User;
import com.example.calorieserver.service.PasswordResetService;
import com.example.calorieserver.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController 切片测试：@Valid 校验失败、service 委托、返回 JSON。
 * 密码/邮箱校验逻辑在 service 层（已有 UserServiceTest 覆盖），此处只测 controller 切片。
 */
@ExtendWith(MockitoExtension.class)
class
AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private PasswordResetService passwordResetService;

    @InjectMocks
    private AuthController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private UserResponse userResponse() {
        return UserResponse.builder().id(1L).username("alice").email("a@b.com").token("jwt-token").build();
    }

    // ===== POST /api/auth/register =====

    @Test
    void register_success_delegatesAndReturnsUser() throws Exception {
        when(userService.register(any())).thenReturn(userResponse());
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"email\":\"a@b.com\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("a@b.com"))
                .andExpect(jsonPath("$.token").value("jwt-token"));
        verify(userService).register(any());
    }

    @Test
    void register_blankUsername_returns400() throws Exception {
        // 用全空格触发 @NotBlank（长度 3 通过 @Size），避免与 @Size 冲突导致断言不确定
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"   \",\"email\":\"a@b.com\",\"password\":\"123456\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("用户名不能为空"));
        verifyNoInteractions(userService);
    }

    @Test
    void register_invalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"email\":\"not-an-email\",\"password\":\"123456\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("邮箱格式不正确"));
    }

    @Test
    void register_shortPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"email\":\"a@b.com\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("密码长度需在 6-32 位之间"));
    }

    // ===== POST /api/auth/login =====

    @Test
    void login_success_returnsToken() throws Exception {
        when(userService.login(any())).thenReturn(userResponse());
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@b.com\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"));
        verify(userService).login(any());
    }

    @Test
    void login_blankPassword_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@b.com\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请输入密码"));
    }

    // ===== POST /api/auth/forgot-password =====

    @Test
    void forgotPassword_success_returns200() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"a@b.com\"}"))
                .andExpect(status().isOk());
        verify(passwordResetService).requestReset("a@b.com");
    }

    @Test
    void forgotPassword_invalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"bad\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("邮箱格式不正确"));
        verifyNoInteractions(passwordResetService);
    }

    // ===== POST /api/auth/reset-password =====

    @Test
    void resetPassword_success_returns200() throws Exception {
        mockMvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"tok\",\"newPassword\":\"newpass123\"}"))
                .andExpect(status().isOk());
        verify(passwordResetService).resetPassword("tok", "newpass123");
    }

    @Test
    void resetPassword_missingToken_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"newpass123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("重置链接无效"));
    }

    // ===== PUT /api/auth/password（修改密码）=====

    @Test
    void changePassword_success_returns200() throws Exception {
        mockMvc.perform(put("/api/auth/password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"123456\",\"newPassword\":\"newpass123\"}"))
                .andExpect(status().isOk());
        verify(userService).changePassword("123456", "newpass123");
    }

    @Test
    void changePassword_blankOldPassword_returns400() throws Exception {
        mockMvc.perform(put("/api/auth/password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"\",\"newPassword\":\"newpass123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请输入原密码"))
                .andExpect(result -> verifyNoInteractions(userService));
    }

    @Test
    void changePassword_shortNewPassword_returns400() throws Exception {
        mockMvc.perform(put("/api/auth/password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"oldPassword\":\"123456\",\"newPassword\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("密码长度需在 6-32 位之间"));
    }

    // ===== PUT /api/auth/onboarding/{userId} =====

    @Test
    void submitOnboarding_success_delegates() throws Exception {
        when(userService.submitOnboarding(eq(1L), any(UpdateBodyInfoRequest.class))).thenReturn(userResponse());
        mockMvc.perform(put("/api/auth/onboarding/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gender\":\"MALE\",\"heightCm\":175,\"weightKg\":70,\"targetWeightKg\":65}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
        verify(userService).submitOnboarding(eq(1L), any(UpdateBodyInfoRequest.class));
    }

    @Test
    void submitOnboarding_missingGender_returns400() throws Exception {
        mockMvc.perform(put("/api/auth/onboarding/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"heightCm\":175,\"weightKg\":70,\"targetWeightKg\":65}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请选择性别"));
        verifyNoInteractions(userService);
    }
}