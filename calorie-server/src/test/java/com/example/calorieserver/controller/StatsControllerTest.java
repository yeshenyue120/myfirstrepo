package com.example.calorieserver.controller;

import com.example.calorieserver.config.GlobalExceptionHandler;
import com.example.calorieserver.service.StatsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * StatsController 切片测试：streak/report 委托 + days 钳位。
 * 统计逻辑在 StatsService（已有 StatsServiceTest 覆盖），此处只测 controller 切片。
 */
@ExtendWith(MockitoExtension.class)
class StatsControllerTest {

    @Mock
    private StatsService statsService;

    @InjectMocks
    private StatsController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ===== GET /api/stats/{userId}/streak =====

    @Test
    void getStreak_success_returnsMap() throws Exception {
        when(statsService.getStreak(1L)).thenReturn(Map.of("currentStreak", 3, "longestStreak", 5));
        mockMvc.perform(get("/api/stats/1/streak"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentStreak").value(3))
                .andExpect(jsonPath("$.longestStreak").value(5));
        verify(statsService).getStreak(1L);
    }

    // ===== GET /api/stats/{userId}/report =====

    @Test
    void getReport_success_delegatesWithDays() throws Exception {
        when(statsService.getReport(1L, LocalDate.of(2026, 8, 8), 7))
                .thenReturn(Map.of("days", 7, "checkInDates", List.of()));
        mockMvc.perform(get("/api/stats/1/report").param("endDate", "2026-08-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.days").value(7))
                .andExpect(jsonPath("$.checkInDates").isArray());
        verify(statsService).getReport(1L, LocalDate.of(2026, 8, 8), 7);
    }

    @Test
    void getReport_clampsDaysToMinimum() throws Exception {
        when(statsService.getReport(1L, LocalDate.of(2026, 8, 8), 1)).thenReturn(Map.of());
        mockMvc.perform(get("/api/stats/1/report").param("endDate", "2026-08-08").param("days", "0"))
                .andExpect(status().isOk());
        verify(statsService).getReport(1L, LocalDate.of(2026, 8, 8), 1);
    }

    @Test
    void getReport_clampsDaysToMaximum() throws Exception {
        when(statsService.getReport(1L, LocalDate.of(2026, 8, 8), 366)).thenReturn(Map.of());
        mockMvc.perform(get("/api/stats/1/report").param("endDate", "2026-08-08").param("days", "9999"))
                .andExpect(status().isOk());
        verify(statsService).getReport(1L, LocalDate.of(2026, 8, 8), 366);
    }
}