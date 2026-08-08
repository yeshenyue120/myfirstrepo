package com.example.calorieserver.controller;

import com.example.calorieserver.config.GlobalExceptionHandler;
import com.example.calorieserver.entity.WeightRecord;
import com.example.calorieserver.exception.BusinessException;
import com.example.calorieserver.security.ForbiddenException;
import com.example.calorieserver.service.WeightService;
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
 * WeightController 切片测试：参数解析（必填/非法/负数）、返回 JSON、service 委托、异常映射。
 * 鉴权/归属(403)在 service 层校验（已有 service 测试覆盖），此处测 controller↔advice 的 403 映射。
 */
@ExtendWith(MockitoExtension.class)
class WeightControllerTest {

    @Mock
    private WeightService weightService;

    @InjectMocks
    private WeightController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static final String JSON = "{\"weightKg\":70.5,\"recordedDate\":\"2026-08-08\",\"bodyFatPct\":20}";

    // ===== POST /api/weights/{userId} 记录体重 =====

    @Test
    void addWeight_success_parsesBodyAndDelegates() throws Exception {
        WeightRecord saved = WeightRecord.builder().id(1L).weightKg(70.5).build();
        when(weightService.addWeight(eq(1L), eq(70.5), eq(LocalDate.of(2026, 8, 8)), eq(20.0), isNull(), isNull()))
                .thenReturn(saved);

        mockMvc.perform(post("/api/weights/1").contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.weightKg").value(70.5));

        verify(weightService).addWeight(eq(1L), eq(70.5), eq(LocalDate.of(2026, 8, 8)), eq(20.0), isNull(), isNull());
    }

    @Test
    void addWeight_missingWeightKg_returns400() throws Exception {
        mockMvc.perform(post("/api/weights/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recordedDate\":\"2026-08-08\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请填写体重"));
        verifyNoInteractions(weightService);
    }

    @Test
    void addWeight_zeroWeightKg_returns400() throws Exception {
        mockMvc.perform(post("/api/weights/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"weightKg\":0,\"recordedDate\":\"2026-08-08\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("体重必须大于 0"));
    }

    @Test
    void addWeight_invalidWeightKg_returns400() throws Exception {
        mockMvc.perform(post("/api/weights/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"weightKg\":\"abc\",\"recordedDate\":\"2026-08-08\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("体重格式不正确"));
    }

    @Test
    void addWeight_missingDate_returns400() throws Exception {
        mockMvc.perform(post("/api/weights/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"weightKg\":70.5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请选择日期"));
    }

    @Test
    void addWeight_invalidDate_returns400() throws Exception {
        mockMvc.perform(post("/api/weights/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"weightKg\":70.5,\"recordedDate\":\"not-a-date\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("日期格式不正确"));
    }

    @Test
    void addWeight_negativeBodyFat_returns400() throws Exception {
        mockMvc.perform(post("/api/weights/1").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"weightKg\":70.5,\"recordedDate\":\"2026-08-08\",\"bodyFatPct\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("体脂率不能为负数"));
    }

    @Test
    void addWeight_serviceThrowsBusiness_returns400() throws Exception {
        when(weightService.addWeight(any(Long.class), anyDouble(), any(), any(), any(), any()))
                .thenThrow(new BusinessException("业务失败"));
        mockMvc.perform(post("/api/weights/1").contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("业务失败"));
    }

    // ===== GET /api/weights/latest/{userId} =====

    @Test
    void getLatest_returnsWeightMap() throws Exception {
        when(weightService.getLatestWeight(1L)).thenReturn(Map.of("weightKg", 70.5, "diff", -0.5));
        mockMvc.perform(get("/api/weights/latest/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.weightKg").value(70.5))
                .andExpect(jsonPath("$.diff").value(-0.5));
        verify(weightService).getLatestWeight(1L);
    }

    // ===== GET /api/weights/history/{userId} =====

    @Test
    void getHistory_delegatesWithDefaultDays() throws Exception {
        when(weightService.getWeightHistory(1L, 30)).thenReturn(List.of());
        mockMvc.perform(get("/api/weights/history/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
        verify(weightService).getWeightHistory(1L, 30);
    }

    @Test
    void getHistory_clampsDays() throws Exception {
        when(weightService.getWeightHistory(1L, 1)).thenReturn(List.of());
        mockMvc.perform(get("/api/weights/history/1").param("days", "0"))
                .andExpect(status().isOk());
        verify(weightService).getWeightHistory(1L, 1);
    }

    // ===== PUT /api/weights/record/{recordId} =====

    @Test
    void updateRecord_success_withoutDate() throws Exception {
        when(weightService.updateWeight(eq(5L), eq(70.0), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(WeightRecord.builder().id(5L).weightKg(70.0).build());
        mockMvc.perform(put("/api/weights/record/5").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"weightKg\":70}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
        verify(weightService).updateWeight(eq(5L), eq(70.0), isNull(), isNull(), isNull(), isNull());
    }

    @Test
    void updateRecord_missingWeightKg_returns400() throws Exception {
        mockMvc.perform(put("/api/weights/record/5").contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("请填写体重"));
    }

    @Test
    void updateRecord_forbidden_returns403() throws Exception {
        when(weightService.updateWeight(any(Long.class), anyDouble(), any(), any(), any(), any()))
                .thenThrow(new ForbiddenException("无权操作该记录"));
        mockMvc.perform(put("/api/weights/record/5").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"weightKg\":70}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("无权操作该记录"));
    }

    // ===== DELETE /api/weights/record/{recordId} =====

    @Test
    void deleteRecord_returns204() throws Exception {
        mockMvc.perform(delete("/api/weights/record/5"))
                .andExpect(status().isNoContent());
        verify(weightService).deleteWeight(5L);
    }
}