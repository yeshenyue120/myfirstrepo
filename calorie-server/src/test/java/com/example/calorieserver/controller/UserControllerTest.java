package com.example.calorieserver.controller;

import com.example.calorieserver.config.GlobalExceptionHandler;
import com.example.calorieserver.dto.UserResponse;
import com.example.calorieserver.service.ExportService;
import com.example.calorieserver.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * UserController 切片测试：导出 + 资料查询委托。
 */
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private ExportService exportService;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void exportUser_delegates() throws Exception {
        when(exportService.exportUserData(1L)).thenReturn(Map.of("username", "alice"));
        mockMvc.perform(get("/api/users/1/export"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"));
        verify(exportService).exportUserData(1L);
    }

    @Test
    void getProfile_delegates() throws Exception {
        when(userService.getProfile(1L)).thenReturn(UserResponse.builder().id(1L).username("alice").build());
        mockMvc.perform(get("/api/users/1/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("alice"));
        verify(userService).getProfile(1L);
    }
}