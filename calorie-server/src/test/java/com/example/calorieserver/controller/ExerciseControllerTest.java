package com.example.calorieserver.controller;

import com.example.calorieserver.config.GlobalExceptionHandler;
import com.example.calorieserver.dto.ExerciseRequest;
import com.example.calorieserver.entity.ExerciseRecord;
import com.example.calorieserver.service.ExerciseService;
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
 * ExerciseController 切片测试：@Valid、weightKg 透传、默认日期、trend 钳位。
 * 计算逻辑在 ExerciseService（已有 ExerciseServiceTest 覆盖），此处只测 controller 切片。
 */
@ExtendWith(MockitoExtension.class)
class ExerciseControllerTest {

    @Mock
    private ExerciseService exerciseService;

    @InjectMocks
    private ExerciseController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static final String JSON =
            "{\"exerciseType\":\"跑步\",\"durationMin\":30,\"metValue\":8.0}";

    // ===== POST /api/exercises/{userId} =====

    @Test
    void addExercise_success_delegatesWithWeight() throws Exception {
        when(exerciseService.addExercise(eq(1L), any(ExerciseRequest.class), eq(65.0)))
                .thenReturn(ExerciseRecord.builder().id(1L).exerciseType("跑步").build());
        mockMvc.perform(post("/api/exercises/1").param("weightKg", "65")
                        .contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.exerciseType").value("跑步"));
        verify(exerciseService).addExercise(eq(1L), any(ExerciseRequest.class), eq(65.0));
    }

    @Test
    void addExercise_missingDuration_returns400() throws Exception {
        mockMvc.perform(post("/api/exercises/1").param("weightKg", "65")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exerciseType\":\"跑步\",\"metValue\":8.0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请填写运动时长"));
        verifyNoInteractions(exerciseService);
    }

    @Test
    void addExercise_negativeDuration_returns400() throws Exception {
        mockMvc.perform(post("/api/exercises/1").param("weightKg", "65")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"exerciseType\":\"跑步\",\"durationMin\":-5,\"metValue\":8.0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("运动时长需大于 0"));
    }

    // ===== GET /api/exercises/{userId} =====

    @Test
    void getByDate_delegates() throws Exception {
        when(exerciseService.getByDate(1L, "2026-08-08")).thenReturn(List.of());
        mockMvc.perform(get("/api/exercises/1").param("date", "2026-08-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
        verify(exerciseService).getByDate(1L, "2026-08-08");
    }

    // ===== GET /api/exercises/{userId}/summary =====

    @Test
    void getSummary_delegates() throws Exception {
        when(exerciseService.getSummary(1L, LocalDate.of(2026, 8, 8))).thenReturn(Map.of("totalCalories", 300.0));
        mockMvc.perform(get("/api/exercises/1/summary").param("date", "2026-08-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCalories").value(300.0));
        verify(exerciseService).getSummary(1L, LocalDate.of(2026, 8, 8));
    }

    // ===== PUT /api/exercises/record/{recordId} =====

    @Test
    void updateExercise_success_delegates() throws Exception {
        when(exerciseService.updateExercise(eq(5L), any(ExerciseRequest.class), eq(70.0)))
                .thenReturn(ExerciseRecord.builder().id(5L).build());
        mockMvc.perform(put("/api/exercises/record/5").param("weightKg", "70")
                        .contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
        verify(exerciseService).updateExercise(eq(5L), any(ExerciseRequest.class), eq(70.0));
    }

    // ===== DELETE /api/exercises/record/{recordId} =====

    @Test
    void deleteExercise_returns204() throws Exception {
        mockMvc.perform(delete("/api/exercises/record/5"))
                .andExpect(status().isNoContent());
        verify(exerciseService).deleteExercise(5L);
    }

    // ===== GET /api/exercises/{userId}/trend =====

    @Test
    void getExerciseTrend_clampsDays() throws Exception {
        when(exerciseService.getExerciseTrend(1L, LocalDate.of(2026, 8, 8), 366)).thenReturn(Map.of());
        mockMvc.perform(get("/api/exercises/1/trend").param("endDate", "2026-08-08").param("days", "9999"))
                .andExpect(status().isOk());
        verify(exerciseService).getExerciseTrend(1L, LocalDate.of(2026, 8, 8), 366);
    }

    // ===== GET /api/exercises/{userId}/recent-types =====

    @Test
    void getRecentExerciseTypes_delegates() throws Exception {
        when(exerciseService.getRecentExerciseTypes(1L)).thenReturn(List.of("跑步", "游泳"));
        mockMvc.perform(get("/api/exercises/1/recent-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("跑步"));
        verify(exerciseService).getRecentExerciseTypes(1L);
    }
}