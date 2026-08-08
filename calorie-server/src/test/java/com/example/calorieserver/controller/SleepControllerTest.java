package com.example.calorieserver.controller;

import com.example.calorieserver.config.GlobalExceptionHandler;
import com.example.calorieserver.entity.SleepRecord;
import com.example.calorieserver.service.SleepService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * SleepController 切片测试：委托、@Positive 校验、trend 钳位、默认日期。
 */
@ExtendWith(MockitoExtension.class)
class SleepControllerTest {

    @Mock
    private SleepService sleepService;

    @InjectMocks
    private SleepController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void saveSleep_success_delegates() throws Exception {
        when(sleepService.saveSleep(1L, LocalDate.of(2026, 8, 8), 480))
                .thenReturn(SleepRecord.builder().id(1L).durationMin(480).build());
        mockMvc.perform(post("/api/sleep/1").param("date", "2026-08-08").param("durationMin", "480"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.durationMin").value(480));
        verify(sleepService).saveSleep(1L, LocalDate.of(2026, 8, 8), 480);
    }

    @Test
    void getByDate_delegates() throws Exception {
        when(sleepService.getByDate(1L, LocalDate.of(2026, 8, 8)))
                .thenReturn(SleepRecord.builder().id(1L).build());
        mockMvc.perform(get("/api/sleep/1").param("date", "2026-08-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
        verify(sleepService).getByDate(1L, LocalDate.of(2026, 8, 8));
    }

    @Test
    void getSummary_delegates() throws Exception {
        when(sleepService.getSummary(1L, LocalDate.of(2026, 8, 8))).thenReturn(Map.of("totalMin", 480));
        mockMvc.perform(get("/api/sleep/1/summary").param("date", "2026-08-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMin").value(480));
        verify(sleepService).getSummary(1L, LocalDate.of(2026, 8, 8));
    }

    @Test
    void getTrend_clampsDays() throws Exception {
        when(sleepService.getTrend(1L, LocalDate.of(2026, 8, 8), 366)).thenReturn(Map.of());
        mockMvc.perform(get("/api/sleep/1/trend").param("endDate", "2026-08-08").param("days", "9999"))
                .andExpect(status().isOk());
        verify(sleepService).getTrend(1L, LocalDate.of(2026, 8, 8), 366);
    }

    @Test
    void updateSleep_success_delegates() throws Exception {
        when(sleepService.updateSleep(5L, 420)).thenReturn(SleepRecord.builder().id(5L).build());
        mockMvc.perform(put("/api/sleep/record/5").param("durationMin", "420"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
        verify(sleepService).updateSleep(5L, 420);
    }

    @Test
    void deleteSleep_returns204() throws Exception {
        mockMvc.perform(delete("/api/sleep/record/5"))
                .andExpect(status().isNoContent());
        verify(sleepService).deleteSleep(5L);
    }
}