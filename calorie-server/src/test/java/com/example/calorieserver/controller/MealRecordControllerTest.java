package com.example.calorieserver.controller;

import com.example.calorieserver.config.GlobalExceptionHandler;
import com.example.calorieserver.dto.MealRecordRequest;
import com.example.calorieserver.dto.MealRecordResponse;
import com.example.calorieserver.service.MealRecordService;
import com.example.calorieserver.util.TimeUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MealRecordController 切片测试：@Valid、委托、@Positive 校验、默认日期。
 * 业务逻辑在 MealRecordService（已有 MealRecordServiceTest 覆盖），此处只测 controller 切片。
 */
@ExtendWith(MockitoExtension.class)
class MealRecordControllerTest {

    @Mock
    private MealRecordService mealRecordService;

    @InjectMocks
    private MealRecordController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static final String MEAL_JSON =
            "{\"foodId\":1,\"grams\":150,\"mealType\":\"BREAKFAST\",\"mealDate\":\"2026-08-08\"}";

    // ===== POST /api/records/{userId} =====

    @Test
    void addRecord_success_delegates() throws Exception {
        when(mealRecordService.addRecord(eq(1L), any(MealRecordRequest.class)))
                .thenReturn(MealRecordResponse.builder().build());
        mockMvc.perform(post("/api/records/1").contentType(MediaType.APPLICATION_JSON).content(MEAL_JSON))
                .andExpect(status().isOk());
        verify(mealRecordService).addRecord(eq(1L), any(MealRecordRequest.class));
    }

    @Test
    void addRecord_missingGrams_returns400() throws Exception {
        mockMvc.perform(post("/api/records/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"foodId\":1,\"mealType\":\"BREAKFAST\",\"mealDate\":\"2026-08-08\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请填写克数"));
        verifyNoInteractions(mealRecordService);
    }

    @Test
    void addRecord_missingMealType_returns400() throws Exception {
        mockMvc.perform(post("/api/records/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"foodId\":1,\"grams\":150,\"mealDate\":\"2026-08-08\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请选择餐次"));
    }

    // ===== POST /api/records/batch/{userId} =====

    @Test
    void batchAddRecords_success_delegates() throws Exception {
        when(mealRecordService.batchAddRecords(eq(1L), anyList())).thenReturn(List.of());
        mockMvc.perform(post("/api/records/batch/1").contentType(MediaType.APPLICATION_JSON)
                        .content("[" + MEAL_JSON + "]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
        verify(mealRecordService).batchAddRecords(eq(1L), anyList());
    }

    // ===== GET /api/records/{userId} =====

    @Test
    void getByDate_delegatesWithParsedDate() throws Exception {
        when(mealRecordService.getRecordsByDate(1L, LocalDate.of(2026, 8, 8))).thenReturn(List.of());
        mockMvc.perform(get("/api/records/1").param("date", "2026-08-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
        verify(mealRecordService).getRecordsByDate(1L, LocalDate.of(2026, 8, 8));
    }

    // ===== PUT /api/records/{recordId} =====

    @Test
    void updateRecord_success_delegates() throws Exception {
        when(mealRecordService.updateRecord(5L, 200.0)).thenReturn(MealRecordResponse.builder().build());
        mockMvc.perform(put("/api/records/5").param("grams", "200"))
                .andExpect(status().isOk());
        verify(mealRecordService).updateRecord(5L, 200.0);
    }

    // ===== DELETE =====

    @Test
    void deleteRecord_returns204() throws Exception {
        mockMvc.perform(delete("/api/records/5"))
                .andExpect(status().isNoContent());
        verify(mealRecordService).deleteRecord(5L);
    }

    @Test
    void deleteByDate_returns204() throws Exception {
        mockMvc.perform(delete("/api/records/1/date").param("date", "2026-08-08"))
                .andExpect(status().isNoContent());
        verify(mealRecordService).deleteRecordsByDate(1L, LocalDate.of(2026, 8, 8));
    }

    // ===== GET /api/records/{userId}/total =====

    @Test
    void getTotalCalories_returnsDouble() throws Exception {
        when(mealRecordService.getTotalCalories(1L, LocalDate.of(2026, 8, 8))).thenReturn(1500.0);
        mockMvc.perform(get("/api/records/1/total").param("date", "2026-08-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(1500.0));
        verify(mealRecordService).getTotalCalories(1L, LocalDate.of(2026, 8, 8));
    }

    // ===== GET /api/records/summary/{userId}（默认今天） =====

    @Test
    void getDailySummary_defaultsToToday() throws Exception {
        when(mealRecordService.getDailySummary(1L, TimeUtil.today())).thenReturn(null);
        mockMvc.perform(get("/api/records/summary/1"))
                .andExpect(status().isOk());
        verify(mealRecordService).getDailySummary(1L, TimeUtil.today());
    }

    // ===== GET /api/records/{userId}/trend =====

    @Test
    void getWeeklyTrend_clampsDays() throws Exception {
        when(mealRecordService.getWeeklyTrend(1L, LocalDate.of(2026, 8, 8), 366)).thenReturn(Map.of());
        mockMvc.perform(get("/api/records/1/trend").param("endDate", "2026-08-08").param("days", "9999"))
                .andExpect(status().isOk());
        verify(mealRecordService).getWeeklyTrend(1L, LocalDate.of(2026, 8, 8), 366);
    }

    // ===== GET /api/records/nutrition/{userId} =====

    @Test
    void getDailyNutrition_delegates() throws Exception {
        when(mealRecordService.getDailyNutrition(1L, LocalDate.of(2026, 8, 8))).thenReturn(null);
        mockMvc.perform(get("/api/records/nutrition/1").param("date", "2026-08-08"))
                .andExpect(status().isOk());
        verify(mealRecordService).getDailyNutrition(1L, LocalDate.of(2026, 8, 8));
    }
}