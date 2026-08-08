package com.example.calorieserver.controller;

import com.example.calorieserver.config.GlobalExceptionHandler;
import com.example.calorieserver.entity.WaterRecord;
import com.example.calorieserver.service.WaterService;
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

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * WaterController 切片测试：委托、@Positive 校验、trend 钳位、默认日期。
 */
@ExtendWith(MockitoExtension.class)
class WaterControllerTest {

    @Mock
    private WaterService waterService;

    @InjectMocks
    private WaterController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void addWater_success_delegates() throws Exception {
        when(waterService.addWater(1L, LocalDate.of(2026, 8, 8), 250))
                .thenReturn(WaterRecord.builder().id(1L).amountMl(250).build());
        mockMvc.perform(post("/api/water/1").param("date", "2026-08-08").param("amountMl", "250"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.amountMl").value(250));
        verify(waterService).addWater(1L, LocalDate.of(2026, 8, 8), 250);
    }

    @Test
    void getByDate_delegates() throws Exception {
        when(waterService.getByDate(1L, LocalDate.of(2026, 8, 8))).thenReturn(List.of());
        mockMvc.perform(get("/api/water/1").param("date", "2026-08-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
        verify(waterService).getByDate(1L, LocalDate.of(2026, 8, 8));
    }

    @Test
    void getSummary_delegates() throws Exception {
        when(waterService.getSummary(1L, LocalDate.of(2026, 8, 8))).thenReturn(Map.of("totalMl", 750));
        mockMvc.perform(get("/api/water/1/summary").param("date", "2026-08-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMl").value(750));
        verify(waterService).getSummary(1L, LocalDate.of(2026, 8, 8));
    }

    @Test
    void getTrend_clampsDays() throws Exception {
        when(waterService.getTrend(1L, LocalDate.of(2026, 8, 8), 366)).thenReturn(Map.of());
        mockMvc.perform(get("/api/water/1/trend").param("endDate", "2026-08-08").param("days", "9999"))
                .andExpect(status().isOk());
        verify(waterService).getTrend(1L, LocalDate.of(2026, 8, 8), 366);
    }

    @Test
    void updateWater_success_delegates() throws Exception {
        when(waterService.updateWater(5L, 300)).thenReturn(WaterRecord.builder().id(5L).build());
        mockMvc.perform(put("/api/water/record/5").param("amountMl", "300"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
        verify(waterService).updateWater(5L, 300);
    }

    @Test
    void deleteWater_returns204() throws Exception {
        mockMvc.perform(delete("/api/water/record/5"))
                .andExpect(status().isNoContent());
        verify(waterService).deleteWater(5L);
    }
}